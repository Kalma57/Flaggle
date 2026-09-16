package com.example.flagdemo.BusinessLayer.CapitalGlobeBL;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalAiOpponentPlan;
import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.RoundWinner;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core engine for the capital-location globe game's Blitz format - same fixed-length,
 * shared-shuffled-queue, independent-pacing race as
 * {@link com.example.flagdemo.BusinessLayer.CapitalBL.CapitalBlitzEngineBL} and net +1/-1
 * scoring, but each question is a type-and-narrow-down guess (unlimited attempts, via
 * {@link GuessResultGlobeBL}) rather than a single click - matching
 * {@link com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobeBlitzEngineBL}, hints are not
 * available in this format (it's about pace, not precision).
 */
public class CapitalGlobeBlitzEngineBL implements java.io.Serializable {

    private final CountryController cc;
    private final AiSkillLevel aiLevel;

    /** Total length of this Blitz match, in seconds (e.g. 60 or 120). */
    private final double durationSeconds;

    /** The shared sequence of question targets both sides race through, fixed for the whole match. */
    private List<CountryBL> queue;

    private long matchStartTimeMillis;
    private boolean matchOver;

    private boolean paused;
    private long pauseStartedAtMillis;
    private long pausedMillisTotal;

    // -------------------- Human progress --------------------
    private int humanQueueIndex;
    private int humanAttemptsThisTarget;
    private double humanCurrentTargetStartElapsed;
    private int humanScore;
    private final List<CapitalGlobeBlitzQuestionResult> humanHistory = new ArrayList<>();

    // -------------------- AI progress --------------------
    private int aiQueueIndex;
    private double aiCurrentTargetStartElapsed;
    private CapitalAiOpponentPlan aiPlan;
    private int aiScore;
    private final List<CapitalGlobeBlitzQuestionResult> aiHistory = new ArrayList<>();

    public CapitalGlobeBlitzEngineBL(CountryController cc, AiSkillLevel aiLevel, double durationSeconds) {
        this.cc = cc;
        this.aiLevel = aiLevel;
        this.durationSeconds = durationSeconds;
    }

    // -------------------- Match control --------------------

    public synchronized void startMatch() {
        queue = new ArrayList<>(CapitalGlobeTargetPicker.eligibleTargets(cc));
        Collections.shuffle(queue);

        matchStartTimeMillis = System.currentTimeMillis();
        matchOver = false;
        paused = false;
        pausedMillisTotal = 0;

        humanQueueIndex = 0;
        humanAttemptsThisTarget = 0;
        humanCurrentTargetStartElapsed = 0;
        humanScore = 0;
        humanHistory.clear();

        aiQueueIndex = 0;
        aiCurrentTargetStartElapsed = 0;
        aiScore = 0;
        aiHistory.clear();
        aiPlan = CapitalGlobeAiOpponentPlanFactory.createPlan(aiLevel);
    }

    // -------------------- Human guessing --------------------

    public synchronized GuessResultGlobeBL humanGuess(String countryName) {
        if (paused) return null;
        refreshAiProgress();
        if (matchOver) return null;

        CountryBL target = queue.get(humanQueueIndex);

        // A click can land on a polygon from the map dataset that has no matching row in our
        // own DB - treat that as a no-op, not a wasted attempt.
        CountryBL guessedCountry = cc.getCountryByName(countryName);
        if (guessedCountry == null) return null;

        humanAttemptsThisTarget++;
        GuessResultGlobeBL result = new GuessResultGlobeBL(guessedCountry, target);

        if (result.isCorrect()) {
            humanScore++;
            double timeTaken = getElapsedSeconds() - humanCurrentTargetStartElapsed;
            humanHistory.add(new CapitalGlobeBlitzQuestionResult(humanHistory.size() + 1, target.getName(), target.getCapital(),
                    target.getFlagPath(), true, humanAttemptsThisTarget, timeTaken));
            advanceHumanToNextTarget();
        }

        return result;
    }

    public synchronized String humanSkipTarget() {
        if (paused) return null;
        refreshAiProgress();
        if (matchOver) return null;

        CountryBL target = queue.get(humanQueueIndex);
        humanScore--;
        humanHistory.add(new CapitalGlobeBlitzQuestionResult(humanHistory.size() + 1, target.getName(), target.getCapital(),
                target.getFlagPath(), false, humanAttemptsThisTarget, 0));
        advanceHumanToNextTarget();
        return target.getName();
    }

    private void advanceHumanToNextTarget() {
        humanQueueIndex++;
        if (humanQueueIndex >= queue.size()) humanQueueIndex = 0; // wrap-around safety net
        humanAttemptsThisTarget = 0;
        humanCurrentTargetStartElapsed = getElapsedSeconds();
    }

    // -------------------- AI progress / clock --------------------

    public synchronized void refreshAiProgress() {
        if (matchOver) return;

        double elapsed = getElapsedSeconds();
        if (elapsed >= durationSeconds) {
            matchOver = true;
            return;
        }

        while (true) {
            double aiTargetAnswerTime = aiCurrentTargetStartElapsed + aiPlan.getAnswerTimeSeconds();
            if (aiTargetAnswerTime > elapsed || aiTargetAnswerTime >= durationSeconds) break;

            CountryBL target = queue.get(aiQueueIndex);
            boolean correct = aiPlan.isCorrect();
            aiScore += correct ? 1 : -1;
            aiHistory.add(new CapitalGlobeBlitzQuestionResult(aiHistory.size() + 1, target.getName(), target.getCapital(),
                    target.getFlagPath(), correct, 1, aiPlan.getAnswerTimeSeconds()));

            aiCurrentTargetStartElapsed = aiTargetAnswerTime;
            aiQueueIndex++;
            if (aiQueueIndex >= queue.size()) aiQueueIndex = 0; // wrap-around safety net
            aiPlan = CapitalGlobeAiOpponentPlanFactory.createPlan(aiLevel);
        }
    }

    // -------------------- Pause / Resume --------------------

    public synchronized void pauseMatch() {
        if (paused || matchOver) return;
        paused = true;
        pauseStartedAtMillis = System.currentTimeMillis();
    }

    public synchronized void resumeMatch() {
        if (!paused) return;
        long pausedDuration = System.currentTimeMillis() - pauseStartedAtMillis;
        pausedMillisTotal += pausedDuration;
        paused = false;
    }

    public boolean isPaused() { return paused; }

    // -------------------- Timing --------------------

    public double getElapsedSeconds() {
        long effectiveNow = paused ? pauseStartedAtMillis : System.currentTimeMillis();
        double elapsed = (effectiveNow - matchStartTimeMillis - pausedMillisTotal) / 1000.0;
        return Math.min(elapsed, durationSeconds);
    }

    public double getTimeRemainingSeconds() {
        return Math.max(0, durationSeconds - getElapsedSeconds());
    }

    public double getDurationSeconds() { return durationSeconds; }

    // -------------------- Getters --------------------

    public boolean isMatchOver() { return matchOver; }

    public int getHumanScore() { return humanScore; }
    public int getAiScore() { return aiScore; }

    public CountryBL getCurrentHumanTarget() { return queue.get(humanQueueIndex); }
    public int getHumanAttemptsThisTarget() { return humanAttemptsThisTarget; }
    public List<CapitalGlobeBlitzQuestionResult> getHumanHistory() { return humanHistory; }
    public List<CapitalGlobeBlitzQuestionResult> getAiHistory() { return aiHistory; }

    public AiSkillLevel getAiLevel() { return aiLevel; }

    public RoundWinner getMatchWinner() {
        if (!matchOver) return RoundWinner.NONE;
        if (humanScore > aiScore) return RoundWinner.HUMAN;
        if (humanScore < aiScore) return RoundWinner.AI;
        return RoundWinner.DRAW;
    }
}
