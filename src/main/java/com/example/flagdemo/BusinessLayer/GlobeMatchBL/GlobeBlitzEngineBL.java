package com.example.flagdemo.BusinessLayer.GlobeMatchBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.ProximityLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.BlitzFlagResult;
import com.example.flagdemo.BusinessLayer.MatchBL.RoundWinner;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core engine for the "Blitz" Globe mode: a fixed-length (1 or 2 minute) time-attack race
 * against an AI opponent — mirrors
 * {@link com.example.flagdemo.BusinessLayer.MatchBL.FlaggleBlitzEngineBL} exactly, just
 * guessing countries by location instead of by flag, and exposing the AI's live progress as
 * a best-{@link ProximityLevel} reached on its current country rather than a match
 * percentage. Hints are not available in this mode.
 *
 * The AI's progress through the queue is not simulated by a background thread: whenever the
 * server needs to know where the AI "is" right now, {@link #refreshAiProgress()} walks it
 * forward through however many countries its precomputed per-country solve times say it
 * should have finished by now, generating each next plan lazily as it goes.
 */
public class GlobeBlitzEngineBL implements java.io.Serializable {

    // -------------------- Fields --------------------

    private final CountryController cc;
    private final AiSkillLevel aiLevel;

    /** Total length of this Blitz match, in seconds (e.g. 60 or 120). */
    private final double durationSeconds;

    /** The shared sequence of countries both sides race through, fixed for the whole match. */
    private List<CountryBL> queue;

    private long matchStartTimeMillis;
    private boolean matchOver;

    // -------------------- Pause state --------------------
    private boolean paused;
    private long pauseStartedAtMillis;
    private long pausedMillisTotal;

    // -------------------- Human progress --------------------
    private int humanQueueIndex;
    private int humanAttemptsThisFlag;
    private double humanCurrentFlagStartElapsed;
    private int humanCorrectCount;
    private final List<BlitzFlagResult> humanHistory = new ArrayList<>();

    // -------------------- AI progress --------------------
    private int aiQueueIndex;
    private double aiCurrentFlagStartElapsed;
    private GlobeAiOpponentPlan aiPlan;
    private int aiCorrectCount;
    private final List<BlitzFlagResult> aiHistory = new ArrayList<>();

    // -------------------- Constructor --------------------

    public GlobeBlitzEngineBL(CountryController cc, AiSkillLevel aiLevel, double durationSeconds) {
        this.cc = cc;
        this.aiLevel = aiLevel;
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
        pausedMillisTotal = 0;

        humanQueueIndex = 0;
        humanAttemptsThisFlag = 0;
        humanCurrentFlagStartElapsed = 0;
        humanCorrectCount = 0;
        humanHistory.clear();

        aiQueueIndex = 0;
        aiCurrentFlagStartElapsed = 0;
        aiCorrectCount = 0;
        aiHistory.clear();
        aiPlan = GlobeAiOpponentPlanFactory.createPlan(aiLevel, cc, queue.get(0));
    }

    // -------------------- Human guessing --------------------

    public synchronized GuessResultGlobeBL humanGuess(String countryName) {
        if (paused) return null;
        refreshAiProgress();
        if (matchOver) return null;

        humanAttemptsThisFlag++;
        CountryBL target = queue.get(humanQueueIndex);
        CountryBL guessedCountry = cc.getCountryByName(countryName);
        GuessResultGlobeBL result = new GuessResultGlobeBL(guessedCountry, target);

        if (result.isCorrect()) {
            humanCorrectCount++;
            double timeTaken = getElapsedSeconds() - humanCurrentFlagStartElapsed;
            humanHistory.add(new BlitzFlagResult(humanHistory.size() + 1, target.getName(), true, humanAttemptsThisFlag, timeTaken));
            advanceHumanToNextFlag();
        }

        return result;
    }

    public synchronized String humanGiveUpFlag() {
        if (paused) return null;
        refreshAiProgress();
        if (matchOver) return null;

        CountryBL target = queue.get(humanQueueIndex);
        humanHistory.add(new BlitzFlagResult(humanHistory.size() + 1, target.getName(), false, humanAttemptsThisFlag, 0));
        advanceHumanToNextFlag();
        return target.getName();
    }

    private void advanceHumanToNextFlag() {
        humanQueueIndex++;
        if (humanQueueIndex >= queue.size()) humanQueueIndex = 0; // wrap-around safety net
        humanAttemptsThisFlag = 0;
        humanCurrentFlagStartElapsed = getElapsedSeconds();
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
            double aiFlagSolveTime = aiCurrentFlagStartElapsed + aiPlan.getSolveTimeSeconds();
            if (aiFlagSolveTime > elapsed || aiFlagSolveTime >= durationSeconds) break;

            aiCorrectCount++;
            CountryBL solvedFlag = queue.get(aiQueueIndex);
            aiHistory.add(new BlitzFlagResult(aiHistory.size() + 1, solvedFlag.getName(), true,
                    aiPlan.getAttemptsSoFar(aiPlan.getSolveTimeSeconds()), aiPlan.getSolveTimeSeconds()));

            aiCurrentFlagStartElapsed = aiFlagSolveTime;
            aiQueueIndex++;
            if (aiQueueIndex >= queue.size()) aiQueueIndex = 0; // wrap-around safety net
            aiPlan = GlobeAiOpponentPlanFactory.createPlan(aiLevel, cc, queue.get(aiQueueIndex));
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

    public int getHumanCorrectCount() { return humanCorrectCount; }
    public int getAiCorrectCount() { return aiCorrectCount; }

    public CountryBL getCurrentHumanTarget() { return queue.get(humanQueueIndex); }
    public int getHumanAttemptsThisFlag() { return humanAttemptsThisFlag; }
    public List<BlitzFlagResult> getHumanHistory() { return humanHistory; }
    public List<BlitzFlagResult> getAiHistory() { return aiHistory; }

    public CountryBL getQueueFlagAt(int index) { return queue.get(index); }

    public GlobeAiOpponentPlan getAiPlan() { return aiPlan; }
    public double getAiElapsedOnCurrentFlag() {
        return Math.max(0, getElapsedSeconds() - aiCurrentFlagStartElapsed);
    }
    /** The best (closest) proximity band the AI has reached so far on its current country, or null if it hasn't guessed yet. */
    public ProximityLevel getAiProgressLevel() { return aiPlan.getProgressLevel(getAiElapsedOnCurrentFlag()); }

    public RoundWinner getMatchWinner() {
        if (!matchOver) return RoundWinner.NONE;
        if (humanCorrectCount > aiCorrectCount) return RoundWinner.HUMAN;
        if (humanCorrectCount < aiCorrectCount) return RoundWinner.AI;
        return RoundWinner.DRAW;
    }

    public AiSkillLevel getAiLevel() { return aiLevel; }
    public CountryController getCc() { return cc; }
}
