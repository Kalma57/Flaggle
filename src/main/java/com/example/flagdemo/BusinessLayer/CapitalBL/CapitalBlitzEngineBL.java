package com.example.flagdemo.BusinessLayer.CapitalBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.RoundWinner;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core engine for a fixed-length (1 or 2 minute) capital-quiz time-attack race against an AI
 * opponent - mirrors {@link com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobeBlitzEngineBL}
 * in overall shape (shared shuffled queue, independent per-side pacing), but every question is
 * answered in a single click rather than narrowed down over several attempts, so - just like
 * {@link CapitalQuizEngineBL} - there's no attempts-cap/give-up-and-retry machinery here; a
 * wrong pick simply advances to the next question immediately.
 *
 * Scoring matches {@link CapitalQuizEngineBL}'s "First to N" mode: a correct answer is +1, a
 * wrong one is -1 (net score can go negative), and whoever has the higher net score when time
 * runs out wins - a tie is a draw.
 *
 * The AI's progress through the queue is not simulated by a background thread: whenever the
 * server needs to know where the AI "is" right now, {@link #refreshAiProgress()} walks it
 * forward through however many questions its precomputed per-question answer times say it
 * should have finished by now, generating each next plan lazily as it goes (same trick as
 * GlobeBlitzEngineBL, just with {@link CapitalAiOpponentPlan} standing in for the per-flag plan).
 */
public class CapitalBlitzEngineBL implements java.io.Serializable {

    // -------------------- Fields --------------------

    private final CountryController cc;
    private final CapitalQuizMode mode;
    private final AiSkillLevel aiLevel;

    /** Total length of this Blitz match, in seconds (e.g. 60 or 120). */
    private final double durationSeconds;

    /** The shared sequence of question targets both sides race through, fixed for the whole match. */
    private List<CountryBL> queue;

    private long matchStartTimeMillis;
    private boolean matchOver;

    // -------------------- Pause state --------------------
    private boolean paused;
    private long pauseStartedAtMillis;
    private long pausedMillisTotal;

    // -------------------- Human progress --------------------
    private int humanQueueIndex;
    private double humanCurrentQuestionStartElapsed;
    private int humanScore;
    private CapitalQuestionOptions humanCurrentOptions;
    private final List<CapitalBlitzQuestionResult> humanHistory = new ArrayList<>();

    // -------------------- AI progress --------------------
    private int aiQueueIndex;
    private double aiCurrentQuestionStartElapsed;
    private CapitalAiOpponentPlan aiPlan;
    private int aiScore;
    private final List<CapitalBlitzQuestionResult> aiHistory = new ArrayList<>();

    // -------------------- Constructor --------------------

    public CapitalBlitzEngineBL(CountryController cc, CapitalQuizMode mode, AiSkillLevel aiLevel, double durationSeconds) {
        this.cc = cc;
        this.mode = mode;
        this.aiLevel = aiLevel;
        this.durationSeconds = durationSeconds;
    }

    // -------------------- Match control --------------------

    public synchronized void startMatch() {
        queue = new ArrayList<>(CapitalOptionsBuilder.eligibleTargets(cc));
        Collections.shuffle(queue);

        matchStartTimeMillis = System.currentTimeMillis();
        matchOver = false;
        paused = false;
        pausedMillisTotal = 0;

        humanQueueIndex = 0;
        humanCurrentQuestionStartElapsed = 0;
        humanScore = 0;
        humanHistory.clear();
        humanCurrentOptions = CapitalOptionsBuilder.build(cc, mode, queue.get(0));

        aiQueueIndex = 0;
        aiCurrentQuestionStartElapsed = 0;
        aiScore = 0;
        aiHistory.clear();
        aiPlan = CapitalAiOpponentPlanFactory.createPlan(aiLevel);
    }

    // -------------------- Human answering --------------------

    /** @return true if this was the correct answer, or null if the click couldn't be accepted right now. */
    public synchronized Boolean submitAnswer(int optionIndex) {
        if (paused) return null;
        refreshAiProgress();
        if (matchOver) return null;

        CountryBL target = queue.get(humanQueueIndex);
        boolean correct = optionIndex == humanCurrentOptions.getCorrectIndex();
        double timeTaken = getElapsedSeconds() - humanCurrentQuestionStartElapsed;

        humanScore += correct ? 1 : -1;
        humanHistory.add(new CapitalBlitzQuestionResult(
                humanHistory.size() + 1, promptText(target), humanCurrentOptions.getOptionTexts().get(humanCurrentOptions.getCorrectIndex()),
                correct, timeTaken, target.getFlagPath()));

        advanceHumanToNextQuestion();
        return correct;
    }

    private void advanceHumanToNextQuestion() {
        humanQueueIndex++;
        if (humanQueueIndex >= queue.size()) humanQueueIndex = 0; // wrap-around safety net
        humanCurrentQuestionStartElapsed = getElapsedSeconds();
        humanCurrentOptions = CapitalOptionsBuilder.build(cc, mode, queue.get(humanQueueIndex));
    }

    private String promptText(CountryBL target) {
        return (mode == CapitalQuizMode.FLAG_TO_CAPITAL) ? target.getName() : target.getCapital();
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
            double aiQuestionAnswerTime = aiCurrentQuestionStartElapsed + aiPlan.getAnswerTimeSeconds();
            if (aiQuestionAnswerTime > elapsed || aiQuestionAnswerTime >= durationSeconds) break;

            CountryBL target = queue.get(aiQueueIndex);
            CapitalQuestionOptions options = CapitalOptionsBuilder.build(cc, mode, target);
            boolean correct = aiPlan.isCorrect();
            aiScore += correct ? 1 : -1;
            aiHistory.add(new CapitalBlitzQuestionResult(
                    aiHistory.size() + 1, promptText(target), options.getOptionTexts().get(options.getCorrectIndex()),
                    correct, aiPlan.getAnswerTimeSeconds(), target.getFlagPath()));

            aiCurrentQuestionStartElapsed = aiQuestionAnswerTime;
            aiQueueIndex++;
            if (aiQueueIndex >= queue.size()) aiQueueIndex = 0; // wrap-around safety net
            aiPlan = CapitalAiOpponentPlanFactory.createPlan(aiLevel);
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

    public CapitalQuizMode getMode() { return mode; }
    public boolean isMatchOver() { return matchOver; }

    public int getHumanScore() { return humanScore; }
    public int getAiScore() { return aiScore; }

    public CountryBL getCurrentHumanTarget() { return queue.get(humanQueueIndex); }
    public CapitalQuestionOptions getHumanCurrentOptions() { return humanCurrentOptions; }
    public List<CapitalBlitzQuestionResult> getHumanHistory() { return humanHistory; }
    public List<CapitalBlitzQuestionResult> getAiHistory() { return aiHistory; }

    public CountryBL getQueueTargetAt(int index) { return queue.get(index); }

    public AiSkillLevel getAiLevel() { return aiLevel; }

    public RoundWinner getMatchWinner() {
        if (!matchOver) return RoundWinner.NONE;
        if (humanScore > aiScore) return RoundWinner.HUMAN;
        if (humanScore < aiScore) return RoundWinner.AI;
        return RoundWinner.DRAW;
    }
}
