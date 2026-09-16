package com.example.flagdemo.BusinessLayer.CapitalBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.MatchBL.PvpRoundWinner;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core engine for the capital-quiz's real 1v1 "vs Friend" Blitz match - mirrors
 * {@link com.example.flagdemo.BusinessLayer.CapitalGlobeBL.PvpCapitalGlobeBlitzEngineBL}'s
 * shared-shuffled-queue, independent-pacing race between two real players, but answering a
 * multiple-choice option instead of clicking the globe. Scoring matches
 * {@link CapitalBlitzEngineBL}: a correct answer is +1, a wrong one is -1 (net score can go
 * negative), whoever has the higher net score when time runs out wins - a tie is a draw.
 * Reuses {@link CapitalBlitzQuestionResult} for both sides' history (it carries no human/AI
 * labeling of its own, so it's already symmetric-friendly).
 */
public class PvpCapitalBlitzEngineBL implements java.io.Serializable {

    private final CountryController cc;
    private final CapitalQuizMode mode;
    private final double durationSeconds;

    /** The shared sequence of question targets both sides race through, fixed for the whole match. */
    private List<CountryBL> queue;

    private long matchStartTimeMillis;
    private boolean matchOver;

    // -------------------- Pause state --------------------
    private static final long PAUSE_MAX_MILLIS = 60_000;

    private boolean paused;
    private int pausedBySlot;
    private long pauseStartedAtMillis;
    private long pausedMillisTotal;
    private boolean pause1Used;
    private boolean pause2Used;

    // -------------------- Player 1 progress --------------------
    private int queueIndex1;
    private double questionStartElapsed1;
    private int score1;
    private CapitalQuestionOptions currentOptions1;
    private final List<CapitalBlitzQuestionResult> history1 = new ArrayList<>();

    // -------------------- Player 2 progress --------------------
    private int queueIndex2;
    private double questionStartElapsed2;
    private int score2;
    private CapitalQuestionOptions currentOptions2;
    private final List<CapitalBlitzQuestionResult> history2 = new ArrayList<>();

    public PvpCapitalBlitzEngineBL(CountryController cc, CapitalQuizMode mode, double durationSeconds) {
        this.cc = cc;
        this.mode = mode;
        this.durationSeconds = durationSeconds;
    }

    // -------------------- Match control --------------------

    public synchronized void startMatch() {
        queue = new ArrayList<>(CapitalOptionsBuilder.eligibleTargets(cc));
        Collections.shuffle(queue);

        matchStartTimeMillis = System.currentTimeMillis();
        matchOver = false;
        paused = false;
        pausedBySlot = 0;
        pauseStartedAtMillis = 0;
        pausedMillisTotal = 0;
        pause1Used = false;
        pause2Used = false;

        queueIndex1 = 0; questionStartElapsed1 = 0; score1 = 0; history1.clear();
        currentOptions1 = CapitalOptionsBuilder.build(cc, mode, queue.get(0));

        queueIndex2 = 0; questionStartElapsed2 = 0; score2 = 0; history2.clear();
        currentOptions2 = CapitalOptionsBuilder.build(cc, mode, queue.get(0));
    }

    // -------------------- Answering --------------------

    public synchronized Boolean submitAnswer(int slot, int optionIndex) {
        refreshState();
        if (matchOver || paused) return null;

        if (slot == 1) {
            CountryBL target = queue.get(queueIndex1);
            boolean correct = optionIndex == currentOptions1.getCorrectIndex();
            double timeTaken = getElapsedSeconds() - questionStartElapsed1;
            score1 += correct ? 1 : -1;
            history1.add(new CapitalBlitzQuestionResult(history1.size() + 1, promptText(target),
                    currentOptions1.getOptionTexts().get(currentOptions1.getCorrectIndex()), correct, timeTaken, target.getFlagPath()));
            advanceToNextQuestion(1);
            return correct;
        } else {
            CountryBL target = queue.get(queueIndex2);
            boolean correct = optionIndex == currentOptions2.getCorrectIndex();
            double timeTaken = getElapsedSeconds() - questionStartElapsed2;
            score2 += correct ? 1 : -1;
            history2.add(new CapitalBlitzQuestionResult(history2.size() + 1, promptText(target),
                    currentOptions2.getOptionTexts().get(currentOptions2.getCorrectIndex()), correct, timeTaken, target.getFlagPath()));
            advanceToNextQuestion(2);
            return correct;
        }
    }

    private void advanceToNextQuestion(int slot) {
        if (slot == 1) {
            queueIndex1++;
            if (queueIndex1 >= queue.size()) queueIndex1 = 0;
            questionStartElapsed1 = getElapsedSeconds();
            currentOptions1 = CapitalOptionsBuilder.build(cc, mode, queue.get(queueIndex1));
        } else {
            queueIndex2++;
            if (queueIndex2 >= queue.size()) queueIndex2 = 0;
            questionStartElapsed2 = getElapsedSeconds();
            currentOptions2 = CapitalOptionsBuilder.build(cc, mode, queue.get(queueIndex2));
        }
    }

    private String promptText(CountryBL target) {
        return (mode == CapitalQuizMode.FLAG_TO_CAPITAL) ? target.getName() : target.getCapital();
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

    public double getTimeRemainingSeconds() { return Math.max(0, durationSeconds - getElapsedSeconds()); }
    public double getDurationSeconds() { return durationSeconds; }

    // -------------------- Getters --------------------

    public CapitalQuizMode getMode() { return mode; }
    public boolean isMatchOver() { return matchOver; }
    public int getScore(int slot) { return slot == 1 ? score1 : score2; }
    public List<CapitalBlitzQuestionResult> getHistory(int slot) { return slot == 1 ? history1 : history2; }
    public CountryBL getCurrentTarget(int slot) { return queue.get(slot == 1 ? queueIndex1 : queueIndex2); }
    public CapitalQuestionOptions getCurrentOptions(int slot) { return slot == 1 ? currentOptions1 : currentOptions2; }

    public PvpRoundWinner getMatchWinner() {
        if (!matchOver) return PvpRoundWinner.NONE;
        if (score1 > score2) return PvpRoundWinner.PLAYER1;
        if (score2 > score1) return PvpRoundWinner.PLAYER2;
        return PvpRoundWinner.DRAW;
    }
}
