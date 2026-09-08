package com.example.flagdemo.BusinessLayer.MatchBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Core engine for a real 1v1 "vs Friend" Match ("Best of N" format - N is 3, 5, or 7),
 * modeled closely on {@link FlaggleMatchEngineBL} but symmetric: BOTH sides are real
 * players (slot 1 and slot 2), so there is no precomputed AI timeline - every attempt,
 * reveal, and round outcome comes from an actual {@link #submitGuess(int, String)} call
 * made by one of the two players' browsers.
 *
 * Both players try to guess the SAME target flag every round. Whoever calls
 * {@link #submitGuess(int, String)} with the correct answer first wins the round.
 * Neither player ever sees the other's actual guesses - each browser only ever
 * displays its own guess history; the opponent's live progress is exposed only as a
 * "% of the flag matched" + attempt count, computed the same way the vs-Computer
 * mode's AI opponent progress is (see {@link GuessResultBL#calculateMatchPercentage}).
 *
 * Every mutating method is {@code synchronized} on this engine instance, since both
 * players' HTTP requests can race to guess at (almost) the same time and this is the
 * one shared object standing between them.
 */
public class PvpMatchEngineBL implements java.io.Serializable {

    // -------------------- Fields --------------------

    private final CountryController cc;
    private final DifficultyLevel difficulty;

    /** Rounds needed to win the match (e.g. Best of 3 -> 2, Best of 5 -> 3, Best of 7 -> 4). */
    private final int pointsToWin;

    private int score1;
    private int score2;
    private int roundNumber;
    private boolean matchOver;

    private long matchStartTimeMillis;
    private long roundStartTimeMillis;

    // -------------------- Pause state --------------------
    // Unlike the vs-Computer match (one human, unlimited personal pause), here TWO real
    // players share the same clock, so pausing has to be a fair, visible-to-both action:
    // each player gets exactly one pause for the whole match, it freezes the clock for
    // both browsers (via polling), and it auto-resumes on its own after PAUSE_MAX_MILLIS
    // in case whoever paused it walks away instead of resuming.
    private static final long PAUSE_MAX_MILLIS = 60_000;

    private boolean paused;
    private int pausedBySlot; // 0 = not currently paused
    private long pauseStartedAtMillis;
    private long pausedMillisThisRound;
    private long pausedMillisTotalMatch;
    private boolean pause1Used;
    private boolean pause2Used;

    private CountryBL currentTarget;
    private int attempts1;
    private int attempts2;

    // EASY-mode only: each player's own accumulated reveal image for the current round.
    private BufferedImage revealedFlag1;
    private BufferedImage revealedFlag2;

    // Highest "% of the flag matched" either player has reached so far this round -
    // this is what the OTHER player's browser is shown as live opponent progress.
    private int progressPercent1;
    private int progressPercent2;

    private PvpRoundWinner currentRoundWinner;

    private final List<PvpRoundResult> roundHistory = new ArrayList<>();

    // -------------------- Constructor --------------------

    public PvpMatchEngineBL(CountryController cc, DifficultyLevel difficulty, int pointsToWin) {
        this.cc = cc;
        this.difficulty = difficulty;
        this.pointsToWin = pointsToWin;
    }

    // -------------------- Match / Round control --------------------

    /**
     * Starts a brand new match: resets scores/history and begins round 1.
     */
    public synchronized void startMatch() throws SQLException {
        this.score1 = 0;
        this.score2 = 0;
        this.roundNumber = 0;
        this.matchOver = false;
        this.roundHistory.clear();
        this.matchStartTimeMillis = System.currentTimeMillis();
        startNextRound();
    }

    /**
     * Advances to the next round. Only valid once the current round has a winner
     * and the match itself isn't over yet. Safe to call from both players' browsers
     * independently (e.g. both auto-advance timers firing) - the second call simply
     * sees the round already in progress and does nothing.
     */
    public synchronized void advanceToNextRound() throws SQLException {
        refreshPauseTimeout();
        if (matchOver || paused) return;
        if (currentRoundWinner == PvpRoundWinner.NONE) return; // round still in progress
        startNextRound();
    }

    private void startNextRound() throws SQLException {
        roundNumber++;
        currentTarget = selectRandomCountry();
        attempts1 = 0;
        attempts2 = 0;
        revealedFlag1 = null;
        revealedFlag2 = null;
        progressPercent1 = 0;
        progressPercent2 = 0;
        currentRoundWinner = PvpRoundWinner.NONE;
        roundStartTimeMillis = System.currentTimeMillis();
        pausedMillisThisRound = 0;
    }

    private CountryBL selectRandomCountry() throws SQLException {
        // Pick directly from the loaded country list rather than a random ID in
        // [1, count] - see FlaggleMatchEngineBL for why (DB row IDs aren't contiguous).
        List<CountryBL> allCountries = cc.getAllCountries();
        Random rand = new Random();
        return allCountries.get(rand.nextInt(allCountries.size()));
    }

    // -------------------- Guessing --------------------

    /**
     * Processes a real player's guess for the current round.
     *
     * @param slot 1 or 2, identifying which of the two real players is guessing
     * @return the guess result (for the flag-diff display), or null if the round is
     *         already over or the match itself has ended
     */
    public synchronized GuessResultBL submitGuess(int slot, String countryName) {
        refreshPauseTimeout();
        if (matchOver || paused) return null;
        if (currentRoundWinner != PvpRoundWinner.NONE) return null;

        CountryBL guessedCountry = cc.getCountryByName(countryName);
        GuessResultBL result;

        if (slot == 1) {
            attempts1++;
            if (difficulty == DifficultyLevel.EASY) {
                result = new GuessResultBL(guessedCountry, currentTarget, DifficultyLevel.EASY, revealedFlag1);
                revealedFlag1 = result.getFlagDifferences();
            } else {
                result = new GuessResultBL(guessedCountry, currentTarget, DifficultyLevel.HARD);
            }
            progressPercent1 = Math.max(progressPercent1,
                    GuessResultBL.calculateMatchPercentage(result.getFlagDifferences(), difficulty == DifficultyLevel.EASY));
        } else {
            attempts2++;
            if (difficulty == DifficultyLevel.EASY) {
                result = new GuessResultBL(guessedCountry, currentTarget, DifficultyLevel.EASY, revealedFlag2);
                revealedFlag2 = result.getFlagDifferences();
            } else {
                result = new GuessResultBL(guessedCountry, currentTarget, DifficultyLevel.HARD);
            }
            progressPercent2 = Math.max(progressPercent2,
                    GuessResultBL.calculateMatchPercentage(result.getFlagDifferences(), difficulty == DifficultyLevel.EASY));
        }

        if (result.isCorrect()) {
            currentRoundWinner = (slot == 1) ? PvpRoundWinner.PLAYER1 : PvpRoundWinner.PLAYER2;
            if (slot == 1) score1++; else score2++;
            finishRound();
        }

        return result;
    }

    /**
     * The given player gives up on the current round - the other real player is
     * credited with the round win.
     */
    public synchronized void giveUpRound(int slot) {
        refreshPauseTimeout();
        if (matchOver || paused || currentRoundWinner != PvpRoundWinner.NONE) return;
        if (slot == 1) {
            currentRoundWinner = PvpRoundWinner.PLAYER2;
            score2++;
        } else {
            currentRoundWinner = PvpRoundWinner.PLAYER1;
            score1++;
        }
        finishRound();
    }

    private void finishRound() {
        roundHistory.add(new PvpRoundResult(roundNumber, currentRoundWinner, currentTarget.getName(), attempts1, attempts2));

        if (score1 >= pointsToWin || score2 >= pointsToWin) {
            matchOver = true;
        }
    }

    // -------------------- Pause / Resume --------------------

    /**
     * Pauses the match on behalf of the given player - freezes the clock for BOTH
     * browsers (each polls the shared room, so the other side picks it up within a
     * second). Each of the two real players only gets to do this once per match.
     *
     * @return true if this call actually paused the match; false if it was already
     *         paused, the match is over, or this player already used their one pause.
     */
    public synchronized boolean pauseMatch(int slot) {
        if (paused || matchOver) return false;
        if (hasUsedPause(slot)) return false;
        paused = true;
        pausedBySlot = slot;
        pauseStartedAtMillis = System.currentTimeMillis();
        if (slot == 1) pause1Used = true; else pause2Used = true;
        return true;
    }

    /**
     * Resumes a paused match. Either player may call this (it's cooperative once
     * paused) - the time spent paused is "erased" from both the round and match
     * clocks, exactly like the vs-Computer match.
     */
    public synchronized void resumeMatch() {
        if (!paused) return;
        long pausedDuration = System.currentTimeMillis() - pauseStartedAtMillis;
        pausedMillisThisRound += pausedDuration;
        pausedMillisTotalMatch += pausedDuration;
        paused = false;
        pausedBySlot = 0;
    }

    /**
     * Lazily auto-resumes a pause that's run past its {@value #PAUSE_MAX_MILLIS} budget,
     * so a player can't accidentally freeze the match forever by walking away. Called at
     * the top of every mutating method, plus explicitly on every status poll.
     */
    public synchronized void refreshPauseTimeout() {
        if (!paused) return;
        if (System.currentTimeMillis() - pauseStartedAtMillis >= PAUSE_MAX_MILLIS) {
            resumeMatch();
        }
    }

    public boolean isPaused() { return paused; }
    public int getPausedBySlot() { return pausedBySlot; }
    public boolean hasUsedPause(int slot) { return slot == 1 ? pause1Used : pause2Used; }

    public long getPauseRemainingMillis() {
        if (!paused) return 0;
        return Math.max(0, PAUSE_MAX_MILLIS - (System.currentTimeMillis() - pauseStartedAtMillis));
    }

    // -------------------- Timing --------------------

    public double getRoundElapsedSeconds() {
        long effectiveNow = paused ? pauseStartedAtMillis : System.currentTimeMillis();
        return (effectiveNow - roundStartTimeMillis - pausedMillisThisRound) / 1000.0;
    }

    public double getMatchElapsedSeconds() {
        long effectiveNow = paused ? pauseStartedAtMillis : System.currentTimeMillis();
        return (effectiveNow - matchStartTimeMillis - pausedMillisTotalMatch) / 1000.0;
    }

    // -------------------- Getters --------------------

    public int getScore(int slot) { return slot == 1 ? score1 : score2; }
    public int getAttempts(int slot) { return slot == 1 ? attempts1 : attempts2; }
    public int getProgressPercent(int slot) { return slot == 1 ? progressPercent1 : progressPercent2; }

    public int getRoundNumber() { return roundNumber; }
    public int getPointsToWin() { return pointsToWin; }
    /** The full match format as a "best of N" number, e.g. pointsToWin=3 -> Best of 5. */
    public int getBestOf() { return pointsToWin * 2 - 1; }
    public boolean isMatchOver() { return matchOver; }
    public boolean isRoundOver() { return currentRoundWinner != PvpRoundWinner.NONE; }
    public PvpRoundWinner getCurrentRoundWinner() { return currentRoundWinner; }
    public PvpRoundWinner getMatchWinner() {
        if (!matchOver) return PvpRoundWinner.NONE;
        return score1 > score2 ? PvpRoundWinner.PLAYER1 : PvpRoundWinner.PLAYER2;
    }

    public CountryBL getCurrentTarget() { return currentTarget; }
    public DifficultyLevel getDifficulty() { return difficulty; }
    public List<PvpRoundResult> getRoundHistory() { return roundHistory; }
}
