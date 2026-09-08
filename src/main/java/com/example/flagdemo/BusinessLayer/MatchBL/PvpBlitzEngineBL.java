package com.example.flagdemo.BusinessLayer.MatchBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core engine for a real 1v1 "vs Friend" Blitz match: a fixed-length (1 or 2 minute)
 * time-attack race between TWO real players, modeled closely on
 * {@link FlaggleBlitzEngineBL} but symmetric - there is no AI timeline, both slots work
 * through the exact SAME shuffled sequence of flags, each at their own pace, purely
 * driven by {@link #submitGuess(int, String)} / {@link #giveUpFlag(int)} calls made by
 * the two players' browsers.
 *
 * Neither player ever sees the other's actual guesses or which flag they're currently
 * on - only a live "% of the current flag matched" + attempt count, computed the same
 * way as the vs-Computer Blitz mode's AI progress indicator (see
 * {@link GuessResultBL#calculateMatchPercentage}) and the "Best of N" PvP engine's
 * opponent progress. Only once the match ends does the recap reveal each side's
 * per-flag outcome.
 *
 * Pause mechanic mirrors {@link PvpMatchEngineBL} exactly: each of the two real players
 * gets exactly one pause for the whole match, capped at {@value #PAUSE_MAX_MILLIS} ms
 * (auto-resumes if nobody manually resumes first), and either player may resume early.
 * Unlike the "Best of N" engine there is only one running clock here (no per-round
 * clock), so a single paused-millis accumulator is enough.
 *
 * Every mutating method is {@code synchronized} on this engine instance, since both
 * players' HTTP requests can race to guess/pause/resume at (almost) the same time.
 */
public class PvpBlitzEngineBL implements java.io.Serializable {

    // -------------------- Fields --------------------

    private final CountryController cc;
    private final DifficultyLevel difficulty;

    /** Total length of this Blitz match, in seconds (e.g. 60 or 120). */
    private final double durationSeconds;

    /** The shared sequence of flags both players race through, fixed for the whole match. */
    private List<CountryBL> queue;

    private long matchStartTimeMillis;
    private boolean matchOver;

    // -------------------- Pause state (same policy as PvpMatchEngineBL) --------------------

    private static final long PAUSE_MAX_MILLIS = 60_000;

    private boolean paused;
    private int pausedBySlot; // 0 = not currently paused
    private long pauseStartedAtMillis;
    private long pausedMillisTotal;
    private boolean pause1Used;
    private boolean pause2Used;

    // -------------------- Player 1 progress --------------------

    private int queueIndex1;
    private int attemptsThisFlag1;
    private double currentFlagStartElapsed1; // elapsed seconds when player 1 started their current flag
    private BufferedImage revealedFlag1; // EASY mode only
    private int progressPercent1; // highest % of the current flag player 1 has matched so far
    private int correctCount1;
    private final List<BlitzFlagResult> history1 = new ArrayList<>();

    // -------------------- Player 2 progress --------------------

    private int queueIndex2;
    private int attemptsThisFlag2;
    private double currentFlagStartElapsed2;
    private BufferedImage revealedFlag2;
    private int progressPercent2;
    private int correctCount2;
    private final List<BlitzFlagResult> history2 = new ArrayList<>();

    // -------------------- Constructor --------------------

    public PvpBlitzEngineBL(CountryController cc, DifficultyLevel difficulty, double durationSeconds) {
        this.cc = cc;
        this.difficulty = difficulty;
        this.durationSeconds = durationSeconds;
    }

    // -------------------- Match control --------------------

    /**
     * Starts a brand new Blitz match: builds the shared shuffled flag queue and resets
     * both players' progress and the clock.
     */
    public synchronized void startMatch() {
        queue = new ArrayList<>(cc.getAllCountries());
        Collections.shuffle(queue);

        matchStartTimeMillis = System.currentTimeMillis();
        matchOver = false;

        paused = false;
        pausedBySlot = 0;
        pauseStartedAtMillis = 0;
        pausedMillisTotal = 0;
        pause1Used = false;
        pause2Used = false;

        queueIndex1 = 0;
        attemptsThisFlag1 = 0;
        currentFlagStartElapsed1 = 0;
        revealedFlag1 = null;
        progressPercent1 = 0;
        correctCount1 = 0;
        history1.clear();

        queueIndex2 = 0;
        attemptsThisFlag2 = 0;
        currentFlagStartElapsed2 = 0;
        revealedFlag2 = null;
        progressPercent2 = 0;
        correctCount2 = 0;
        history2.clear();
    }

    // -------------------- Guessing --------------------

    /**
     * Processes a real player's guess for whatever flag they're currently on.
     *
     * @param slot 1 or 2, identifying which of the two real players is guessing
     * @return the guess result (for the flag-diff display), or null if the match has
     *         already ended (e.g. the clock ran out a moment earlier) or is paused
     */
    public synchronized GuessResultBL submitGuess(int slot, String countryName) {
        refreshState();
        if (matchOver || paused) return null;

        CountryBL target = queue.get(slot == 1 ? queueIndex1 : queueIndex2);
        CountryBL guessedCountry = cc.getCountryByName(countryName);
        GuessResultBL result;

        if (slot == 1) {
            attemptsThisFlag1++;
            if (difficulty == DifficultyLevel.EASY) {
                result = new GuessResultBL(guessedCountry, target, DifficultyLevel.EASY, revealedFlag1);
                revealedFlag1 = result.getFlagDifferences();
            } else {
                result = new GuessResultBL(guessedCountry, target, DifficultyLevel.HARD);
            }
            progressPercent1 = Math.max(progressPercent1,
                    GuessResultBL.calculateMatchPercentage(result.getFlagDifferences(), difficulty == DifficultyLevel.EASY));

            if (result.isCorrect()) {
                correctCount1++;
                double timeTaken = getElapsedSeconds() - currentFlagStartElapsed1;
                history1.add(new BlitzFlagResult(history1.size() + 1, target.getName(), true, attemptsThisFlag1, timeTaken));
                advanceToNextFlag(1);
            }
        } else {
            attemptsThisFlag2++;
            if (difficulty == DifficultyLevel.EASY) {
                result = new GuessResultBL(guessedCountry, target, DifficultyLevel.EASY, revealedFlag2);
                revealedFlag2 = result.getFlagDifferences();
            } else {
                result = new GuessResultBL(guessedCountry, target, DifficultyLevel.HARD);
            }
            progressPercent2 = Math.max(progressPercent2,
                    GuessResultBL.calculateMatchPercentage(result.getFlagDifferences(), difficulty == DifficultyLevel.EASY));

            if (result.isCorrect()) {
                correctCount2++;
                double timeTaken = getElapsedSeconds() - currentFlagStartElapsed2;
                history2.add(new BlitzFlagResult(history2.size() + 1, target.getName(), true, attemptsThisFlag2, timeTaken));
                advanceToNextFlag(2);
            }
        }

        return result;
    }

    /**
     * The given player gives up on their current flag and immediately moves to the
     * next one in the shared queue - it is simply skipped, nobody is credited with it.
     *
     * @return the name of the flag that was given up on (so the UI can reveal it), or
     *         null if the match is already over/paused
     */
    public synchronized String giveUpFlag(int slot) {
        refreshState();
        if (matchOver || paused) return null;

        CountryBL target = queue.get(slot == 1 ? queueIndex1 : queueIndex2);
        if (slot == 1) {
            history1.add(new BlitzFlagResult(history1.size() + 1, target.getName(), false, attemptsThisFlag1, 0));
            advanceToNextFlag(1);
        } else {
            history2.add(new BlitzFlagResult(history2.size() + 1, target.getName(), false, attemptsThisFlag2, 0));
            advanceToNextFlag(2);
        }
        return target.getName();
    }

    private void advanceToNextFlag(int slot) {
        if (slot == 1) {
            queueIndex1++;
            if (queueIndex1 >= queue.size()) queueIndex1 = 0; // wrap-around safety net
            attemptsThisFlag1 = 0;
            revealedFlag1 = null;
            progressPercent1 = 0;
            currentFlagStartElapsed1 = getElapsedSeconds();
        } else {
            queueIndex2++;
            if (queueIndex2 >= queue.size()) queueIndex2 = 0;
            attemptsThisFlag2 = 0;
            revealedFlag2 = null;
            progressPercent2 = 0;
            currentFlagStartElapsed2 = getElapsedSeconds();
        }
    }

    // -------------------- Clock --------------------

    /**
     * Lazily auto-resumes an expired pause and ends the match once the clock has run
     * out. Called at the top of every mutating method, plus explicitly on every status
     * poll - there's no background thread simulating anything here, so this is the
     * only place the match-over transition actually happens.
     */
    public synchronized void refreshState() {
        refreshPauseTimeout();
        if (matchOver) return;
        if (getElapsedSeconds() >= durationSeconds) {
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
     *         paused, the match is over, or this player already used their one pause
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
     * paused) - the time spent paused is "erased" from the match clock.
     */
    public synchronized void resumeMatch() {
        if (!paused) return;
        long pausedDuration = System.currentTimeMillis() - pauseStartedAtMillis;
        pausedMillisTotal += pausedDuration;
        paused = false;
        pausedBySlot = 0;
    }

    /**
     * Lazily auto-resumes a pause that's run past its {@value #PAUSE_MAX_MILLIS} budget,
     * so a player can't accidentally freeze the match forever by walking away.
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

    public double getElapsedSeconds() {
        long effectiveNow = paused ? pauseStartedAtMillis : System.currentTimeMillis();
        double elapsed = (effectiveNow - matchStartTimeMillis - pausedMillisTotal) / 1000.0;
        return Math.min(Math.max(0, elapsed), durationSeconds);
    }

    public double getTimeRemainingSeconds() {
        return Math.max(0, durationSeconds - getElapsedSeconds());
    }

    public double getDurationSeconds() { return durationSeconds; }

    // -------------------- Getters --------------------

    public boolean isMatchOver() { return matchOver; }

    public int getCorrectCount(int slot) { return slot == 1 ? correctCount1 : correctCount2; }
    public int getAttemptsThisFlag(int slot) { return slot == 1 ? attemptsThisFlag1 : attemptsThisFlag2; }
    public int getProgressPercent(int slot) { return slot == 1 ? progressPercent1 : progressPercent2; }
    public List<BlitzFlagResult> getHistory(int slot) { return slot == 1 ? history1 : history2; }

    public CountryBL getCurrentTarget(int slot) {
        return queue.get(slot == 1 ? queueIndex1 : queueIndex2);
    }

    /** The flag at a given position in the shared queue (same country for both sides). */
    public CountryBL getQueueFlagAt(int index) { return queue.get(index); }

    public PvpRoundWinner getMatchWinner() {
        if (!matchOver) return PvpRoundWinner.NONE;
        if (correctCount1 > correctCount2) return PvpRoundWinner.PLAYER1;
        if (correctCount2 > correctCount1) return PvpRoundWinner.PLAYER2;
        return PvpRoundWinner.DRAW;
    }

    public DifficultyLevel getDifficulty() { return difficulty; }
}
