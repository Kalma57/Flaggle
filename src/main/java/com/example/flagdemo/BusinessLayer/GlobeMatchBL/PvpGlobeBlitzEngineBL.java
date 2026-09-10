package com.example.flagdemo.BusinessLayer.GlobeMatchBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.ProximityLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.BlitzFlagResult;
import com.example.flagdemo.BusinessLayer.MatchBL.PvpRoundWinner;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core engine for a real 1v1 "vs Friend" Globe Blitz match: a fixed-length (1 or 2 minute)
 * time-attack race between TWO real players, modeled closely on
 * {@link com.example.flagdemo.BusinessLayer.MatchBL.PvpBlitzEngineBL} but guessing countries
 * by location instead of by flag - both slots work through the exact SAME shuffled sequence
 * of countries, each at their own pace.
 *
 * Neither player ever sees the other's actual guesses or which country they're currently on
 * - only a live colored badge for the best {@link ProximityLevel} reached so far on their
 * current country. Only once the match ends does the recap reveal each side's per-country
 * outcome. Hints are not available in this mode.
 *
 * Pause mechanic mirrors {@link PvpGlobeMatchEngineBL} exactly: each of the two real players
 * gets exactly one pause for the whole match, capped at {@value #PAUSE_MAX_MILLIS} ms.
 *
 * Every mutating method is {@code synchronized} on this engine instance, since both
 * players' HTTP requests can race to guess/pause/resume at (almost) the same time.
 */
public class PvpGlobeBlitzEngineBL implements java.io.Serializable {

    // -------------------- Fields --------------------

    private final CountryController cc;

    /** Total length of this Blitz match, in seconds (e.g. 60 or 120). */
    private final double durationSeconds;

    /** The shared sequence of countries both players race through, fixed for the whole match. */
    private List<CountryBL> queue;

    private long matchStartTimeMillis;
    private boolean matchOver;

    // -------------------- Pause state (same policy as PvpGlobeMatchEngineBL) --------------------

    private static final long PAUSE_MAX_MILLIS = 60_000;

    private boolean paused;
    private int pausedBySlot; // 0 = not currently paused
    private long pauseStartedAtMillis;
    private long pausedMillisTotal;
    private boolean pause1Used;
    private boolean pause2Used;

    private static final int NONE_ORDINAL = -1;

    // -------------------- Player 1 progress --------------------

    private int queueIndex1;
    private int attemptsThisFlag1;
    private double currentFlagStartElapsed1;
    private int bestOrdinal1 = NONE_ORDINAL;
    private int correctCount1;
    private final List<BlitzFlagResult> history1 = new ArrayList<>();

    // -------------------- Player 2 progress --------------------

    private int queueIndex2;
    private int attemptsThisFlag2;
    private double currentFlagStartElapsed2;
    private int bestOrdinal2 = NONE_ORDINAL;
    private int correctCount2;
    private final List<BlitzFlagResult> history2 = new ArrayList<>();

    // -------------------- Constructor --------------------

    public PvpGlobeBlitzEngineBL(CountryController cc, double durationSeconds) {
        this.cc = cc;
        this.durationSeconds = durationSeconds;
    }

    // -------------------- Match control --------------------

    public synchronized void startMatch() {
        queue = new ArrayList<>();
        for (CountryBL c : cc.getAllCountries()) {
            if (c.getLatitude() == 0.0 && c.getLongitude() == 0.0) continue;
            queue.add(c);
        }
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
        bestOrdinal1 = NONE_ORDINAL;
        correctCount1 = 0;
        history1.clear();

        queueIndex2 = 0;
        attemptsThisFlag2 = 0;
        currentFlagStartElapsed2 = 0;
        bestOrdinal2 = NONE_ORDINAL;
        correctCount2 = 0;
        history2.clear();
    }

    // -------------------- Guessing --------------------

    public synchronized GuessResultGlobeBL submitGuess(int slot, String countryName) {
        refreshState();
        if (matchOver || paused) return null;

        CountryBL target = queue.get(slot == 1 ? queueIndex1 : queueIndex2);
        CountryBL guessedCountry = cc.getCountryByName(countryName);
        GuessResultGlobeBL result = new GuessResultGlobeBL(guessedCountry, target);
        int ordinal = result.getProximityLevel().ordinal();

        if (slot == 1) {
            attemptsThisFlag1++;
            if (bestOrdinal1 == NONE_ORDINAL || ordinal < bestOrdinal1) bestOrdinal1 = ordinal;

            if (result.isCorrect()) {
                correctCount1++;
                double timeTaken = getElapsedSeconds() - currentFlagStartElapsed1;
                history1.add(new BlitzFlagResult(history1.size() + 1, target.getName(), true, attemptsThisFlag1, timeTaken));
                advanceToNextFlag(1);
            }
        } else {
            attemptsThisFlag2++;
            if (bestOrdinal2 == NONE_ORDINAL || ordinal < bestOrdinal2) bestOrdinal2 = ordinal;

            if (result.isCorrect()) {
                correctCount2++;
                double timeTaken = getElapsedSeconds() - currentFlagStartElapsed2;
                history2.add(new BlitzFlagResult(history2.size() + 1, target.getName(), true, attemptsThisFlag2, timeTaken));
                advanceToNextFlag(2);
            }
        }

        return result;
    }

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
            bestOrdinal1 = NONE_ORDINAL;
            currentFlagStartElapsed1 = getElapsedSeconds();
        } else {
            queueIndex2++;
            if (queueIndex2 >= queue.size()) queueIndex2 = 0;
            attemptsThisFlag2 = 0;
            bestOrdinal2 = NONE_ORDINAL;
            currentFlagStartElapsed2 = getElapsedSeconds();
        }
    }

    // -------------------- Clock --------------------

    public synchronized void refreshState() {
        refreshPauseTimeout();
        if (matchOver) return;
        if (getElapsedSeconds() >= durationSeconds) {
            matchOver = true;
        }
    }

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
        pausedMillisTotal += pausedDuration;
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
    /** The best (closest) proximity band the given player has reached so far on their current country, or null if they haven't guessed yet. */
    public ProximityLevel getProgressLevel(int slot) {
        int ordinal = slot == 1 ? bestOrdinal1 : bestOrdinal2;
        return ordinal == NONE_ORDINAL ? null : ProximityLevel.values()[ordinal];
    }
    public List<BlitzFlagResult> getHistory(int slot) { return slot == 1 ? history1 : history2; }

    public CountryBL getCurrentTarget(int slot) {
        return queue.get(slot == 1 ? queueIndex1 : queueIndex2);
    }

    public CountryBL getQueueFlagAt(int index) { return queue.get(index); }

    public PvpRoundWinner getMatchWinner() {
        if (!matchOver) return PvpRoundWinner.NONE;
        if (correctCount1 > correctCount2) return PvpRoundWinner.PLAYER1;
        if (correctCount2 > correctCount1) return PvpRoundWinner.PLAYER2;
        return PvpRoundWinner.DRAW;
    }
}
