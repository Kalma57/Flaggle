package com.example.flagdemo.BusinessLayer.CapitalGlobeBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.MatchBL.PvpRoundWinner;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.List;

/**
 * Core engine for the capital-location globe game's real 1v1 "vs Friend" Match ("First to N")
 * format - mirrors {@link com.example.flagdemo.BusinessLayer.GlobeMatchBL.PvpGlobeMatchEngineBL}'s
 * symmetric two-real-player shape (both sides are real, addressed by slot 1/2, every mutator
 * {@code synchronized} since two independent HTTP threads can race), but simplified for this
 * game's own rules: no hints exist here at all, so there's no grace-period/provisional-win
 * mechanic either - whichever player's guess is confirmed correct FIRST wins the round
 * immediately, full stop. Target selection reuses {@link CapitalGlobeTargetPicker} (same
 * capital+coordinates+clickability rules as the single-player and vs-AI Capital Globe formats),
 * and guesses reuse {@link GuessResultGlobeBL} just like every other Capital Globe engine.
 *
 * Between rounds, both players get a short "ready up" window (mirroring the Globe PvP engine)
 * so nobody gets thrown straight into the next round before they've seen the recap - the round
 * starts once both are ready, or the window simply expires.
 */
public class PvpCapitalGlobeMatchEngineBL implements java.io.Serializable {

    private final CountryController cc;

    /** Rounds needed to win the match (e.g. Best of 3 -> 2, Best of 5 -> 3, Best of 7 -> 4). */
    private final int pointsToWin;

    private int score1;
    private int score2;
    private int roundNumber;
    private boolean matchOver;

    private long matchStartTimeMillis;
    private long roundStartTimeMillis;

    // -------------------- Pause state --------------------
    private static final long PAUSE_MAX_MILLIS = 60_000;

    private boolean paused;
    private int pausedBySlot; // 0 = not currently paused
    private long pauseStartedAtMillis;
    private long pausedMillisThisRound;
    private long pausedMillisTotalMatch;
    private boolean pause1Used;
    private boolean pause2Used;

    // -------------------- Ready-up state (between rounds) --------------------
    private static final long READY_MAX_MILLIS = 7_000;

    private boolean ready1;
    private boolean ready2;
    private long roundOverAtMillis;

    private CountryBL currentTarget;
    private int attempts1;
    private int attempts2;

    /** Snapshot of {@link #getRoundElapsedSeconds()} taken the instant the last round ended. */
    private double lastRoundDurationSeconds;

    private PvpRoundWinner currentRoundWinner;

    private final List<PvpCapitalGlobeRoundResult> roundHistory = new ArrayList<>();

    public PvpCapitalGlobeMatchEngineBL(CountryController cc, int pointsToWin) {
        this.cc = cc;
        this.pointsToWin = pointsToWin;
    }

    // -------------------- Match / Round control --------------------

    public synchronized void startMatch() {
        score1 = 0;
        score2 = 0;
        roundNumber = 0;
        matchOver = false;
        roundHistory.clear();
        matchStartTimeMillis = System.currentTimeMillis();
        startNextRound();
    }

    public synchronized void advanceToNextRound() {
        refreshPauseTimeout();
        if (matchOver || paused) return;
        if (currentRoundWinner == PvpRoundWinner.NONE) return; // round still in progress
        if (!(ready1 && ready2) && !readyDeadlinePassed()) return;
        startNextRound();
    }

    private void startNextRound() {
        roundNumber++;
        currentTarget = CapitalGlobeTargetPicker.pickRandomTarget(cc);
        attempts1 = 0;
        attempts2 = 0;
        currentRoundWinner = PvpRoundWinner.NONE;
        roundStartTimeMillis = System.currentTimeMillis();
        pausedMillisThisRound = 0;
        ready1 = false;
        ready2 = false;
    }

    // -------------------- Guessing --------------------

    public synchronized GuessResultGlobeBL submitGuess(int slot, String countryName) {
        refreshPauseTimeout();
        if (matchOver || paused) return null;
        if (currentRoundWinner != PvpRoundWinner.NONE) return null;

        // A click can land on a polygon from the map dataset that has no matching row in our
        // own DB - treat that as a no-op, not a wasted attempt.
        CountryBL guessedCountry = cc.getCountryByName(countryName);
        if (guessedCountry == null) return null;

        if (slot == 1) attempts1++; else attempts2++;
        GuessResultGlobeBL result = new GuessResultGlobeBL(guessedCountry, currentTarget);

        if (result.isCorrect()) {
            currentRoundWinner = (slot == 1) ? PvpRoundWinner.PLAYER1 : PvpRoundWinner.PLAYER2;
            if (slot == 1) score1++; else score2++;
            finishRound();
        }

        return result;
    }

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
        lastRoundDurationSeconds = getRoundElapsedSeconds();
        roundHistory.add(new PvpCapitalGlobeRoundResult(roundNumber, currentTarget.getName(), currentTarget.getCapital(),
                currentTarget.getFlagPath(), currentRoundWinner, attempts1, attempts2, lastRoundDurationSeconds));

        if (score1 >= pointsToWin || score2 >= pointsToWin) {
            matchOver = true;
        }

        ready1 = false;
        ready2 = false;
        roundOverAtMillis = System.currentTimeMillis();
    }

    // -------------------- Ready-up (between rounds) --------------------

    public synchronized boolean markReady(int slot) {
        if (matchOver || currentRoundWinner == PvpRoundWinner.NONE) return false;
        if (slot == 1) ready1 = true; else ready2 = true;
        return true;
    }

    public boolean isReady(int slot) { return slot == 1 ? ready1 : ready2; }

    private boolean readyDeadlinePassed() {
        return System.currentTimeMillis() - roundOverAtMillis >= READY_MAX_MILLIS;
    }

    public long getReadyRemainingMillis() {
        if (currentRoundWinner == PvpRoundWinner.NONE) return 0;
        return Math.max(0, READY_MAX_MILLIS - (System.currentTimeMillis() - roundOverAtMillis));
    }

    public double getLastRoundDurationSeconds() { return lastRoundDurationSeconds; }

    // -------------------- Pause / Resume --------------------

    public synchronized boolean pauseMatch(int slot) {
        if (paused || matchOver) return false;
        if (hasUsedPause(slot)) return false;
        paused = true;
        pausedBySlot = slot;
        pauseStartedAtMillis = System.currentTimeMillis();
        if (slot == 1) pause1Used = true; else pause2Used = true;
        return true;
    }

    public synchronized void resumeMatch() {
        if (!paused) return;
        long pausedDuration = System.currentTimeMillis() - pauseStartedAtMillis;
        pausedMillisThisRound += pausedDuration;
        pausedMillisTotalMatch += pausedDuration;
        paused = false;
        pausedBySlot = 0;
    }

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

    public int getRoundNumber() { return roundNumber; }
    public int getPointsToWin() { return pointsToWin; }
    public int getBestOf() { return pointsToWin * 2 - 1; }
    public boolean isMatchOver() { return matchOver; }
    public boolean isRoundOver() { return currentRoundWinner != PvpRoundWinner.NONE; }
    public PvpRoundWinner getCurrentRoundWinner() { return currentRoundWinner; }
    public PvpRoundWinner getMatchWinner() {
        if (!matchOver) return PvpRoundWinner.NONE;
        return score1 > score2 ? PvpRoundWinner.PLAYER1 : PvpRoundWinner.PLAYER2;
    }

    public CountryBL getCurrentTarget() { return currentTarget; }
    public List<PvpCapitalGlobeRoundResult> getRoundHistory() { return roundHistory; }
}
