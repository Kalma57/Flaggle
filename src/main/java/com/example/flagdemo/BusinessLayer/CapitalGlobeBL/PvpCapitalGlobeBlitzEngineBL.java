package com.example.flagdemo.BusinessLayer.CapitalGlobeBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.MatchBL.PvpRoundWinner;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core engine for the capital-location globe game's real 1v1 "vs Friend" Blitz match - mirrors
 * {@link com.example.flagdemo.BusinessLayer.GlobeMatchBL.PvpGlobeBlitzEngineBL}'s shared-shuffled-
 * queue, independent-pacing race between two real players, but with this game's own net +1/-1
 * scoring (matching {@link CapitalGlobeBlitzEngineBL}'s vs-AI rule: correct=+1, skipped=-1, can
 * go negative) instead of a plain correct-count comparison, and {@link CapitalGlobeTargetPicker}'s
 * target pool instead of a plain lat/lon-only filter. No hints exist in this game, so - unlike
 * the Globe PvP Blitz engine - there's no per-flag proximity badge either; each side's own
 * attempt count is the only live signal of the other's progress. Reuses
 * {@link CapitalGlobeBlitzQuestionResult} for both sides' history (it carries no human/AI
 * labeling of its own, so it's already symmetric-friendly).
 */
public class PvpCapitalGlobeBlitzEngineBL implements java.io.Serializable {

    private final CountryController cc;
    private final double durationSeconds;

    /** The shared sequence of question targets both sides race through, fixed for the whole match. */
    private List<CountryBL> queue;

    private long matchStartTimeMillis;
    private boolean matchOver;

    // -------------------- Pause state (same policy as PvpCapitalGlobeMatchEngineBL) --------------------
    private static final long PAUSE_MAX_MILLIS = 60_000;

    private boolean paused;
    private int pausedBySlot; // 0 = not currently paused
    private long pauseStartedAtMillis;
    private long pausedMillisTotal;
    private boolean pause1Used;
    private boolean pause2Used;

    // -------------------- Player 1 progress --------------------
    private int queueIndex1;
    private int attemptsThisTarget1;
    private double currentTargetStartElapsed1;
    private int score1;
    private final List<CapitalGlobeBlitzQuestionResult> history1 = new ArrayList<>();

    // -------------------- Player 2 progress --------------------
    private int queueIndex2;
    private int attemptsThisTarget2;
    private double currentTargetStartElapsed2;
    private int score2;
    private final List<CapitalGlobeBlitzQuestionResult> history2 = new ArrayList<>();

    public PvpCapitalGlobeBlitzEngineBL(CountryController cc, double durationSeconds) {
        this.cc = cc;
        this.durationSeconds = durationSeconds;
    }

    // -------------------- Match control --------------------

    public synchronized void startMatch() {
        queue = new ArrayList<>(CapitalGlobeTargetPicker.eligibleTargets(cc));
        Collections.shuffle(queue);

        matchStartTimeMillis = System.currentTimeMillis();
        matchOver = false;

        paused = false;
        pausedBySlot = 0;
        pauseStartedAtMillis = 0;
        pausedMillisTotal = 0;
        pause1Used = false;
        pause2Used = false;

        queueIndex1 = 0; attemptsThisTarget1 = 0; currentTargetStartElapsed1 = 0; score1 = 0; history1.clear();
        queueIndex2 = 0; attemptsThisTarget2 = 0; currentTargetStartElapsed2 = 0; score2 = 0; history2.clear();
    }

    // -------------------- Guessing --------------------

    public synchronized GuessResultGlobeBL submitGuess(int slot, String countryName) {
        refreshState();
        if (matchOver || paused) return null;

        CountryBL target = queue.get(slot == 1 ? queueIndex1 : queueIndex2);

        CountryBL guessedCountry = cc.getCountryByName(countryName);
        if (guessedCountry == null) return null;

        GuessResultGlobeBL result = new GuessResultGlobeBL(guessedCountry, target);

        if (slot == 1) {
            attemptsThisTarget1++;
            if (result.isCorrect()) {
                score1++;
                double timeTaken = getElapsedSeconds() - currentTargetStartElapsed1;
                history1.add(new CapitalGlobeBlitzQuestionResult(history1.size() + 1, target.getName(), target.getCapital(),
                        target.getFlagPath(), true, attemptsThisTarget1, timeTaken));
                advanceToNextTarget(1);
            }
        } else {
            attemptsThisTarget2++;
            if (result.isCorrect()) {
                score2++;
                double timeTaken = getElapsedSeconds() - currentTargetStartElapsed2;
                history2.add(new CapitalGlobeBlitzQuestionResult(history2.size() + 1, target.getName(), target.getCapital(),
                        target.getFlagPath(), true, attemptsThisTarget2, timeTaken));
                advanceToNextTarget(2);
            }
        }

        return result;
    }

    public synchronized String skipTarget(int slot) {
        refreshState();
        if (matchOver || paused) return null;

        CountryBL target = queue.get(slot == 1 ? queueIndex1 : queueIndex2);
        if (slot == 1) {
            score1--;
            history1.add(new CapitalGlobeBlitzQuestionResult(history1.size() + 1, target.getName(), target.getCapital(),
                    target.getFlagPath(), false, attemptsThisTarget1, 0));
            advanceToNextTarget(1);
        } else {
            score2--;
            history2.add(new CapitalGlobeBlitzQuestionResult(history2.size() + 1, target.getName(), target.getCapital(),
                    target.getFlagPath(), false, attemptsThisTarget2, 0));
            advanceToNextTarget(2);
        }
        return target.getName();
    }

    private void advanceToNextTarget(int slot) {
        if (slot == 1) {
            queueIndex1++;
            if (queueIndex1 >= queue.size()) queueIndex1 = 0; // wrap-around safety net
            attemptsThisTarget1 = 0;
            currentTargetStartElapsed1 = getElapsedSeconds();
        } else {
            queueIndex2++;
            if (queueIndex2 >= queue.size()) queueIndex2 = 0;
            attemptsThisTarget2 = 0;
            currentTargetStartElapsed2 = getElapsedSeconds();
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

    public int getScore(int slot) { return slot == 1 ? score1 : score2; }
    public int getAttemptsThisTarget(int slot) { return slot == 1 ? attemptsThisTarget1 : attemptsThisTarget2; }
    public List<CapitalGlobeBlitzQuestionResult> getHistory(int slot) { return slot == 1 ? history1 : history2; }
    public CountryBL getCurrentTarget(int slot) { return queue.get(slot == 1 ? queueIndex1 : queueIndex2); }
    public CountryBL getQueueTargetAt(int index) { return queue.get(index); }

    public PvpRoundWinner getMatchWinner() {
        if (!matchOver) return PvpRoundWinner.NONE;
        if (score1 > score2) return PvpRoundWinner.PLAYER1;
        if (score2 > score1) return PvpRoundWinner.PLAYER2;
        return PvpRoundWinner.DRAW;
    }
}
