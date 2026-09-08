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
 * Core engine for the "Blitz" 1v1 Flaggle mode: a fixed-length (1 or 2 minute) time-attack
 * race against an AI opponent. Both sides work through the exact SAME shuffled sequence of
 * flags — but each at their own pace — and whoever has correctly guessed more flags when
 * the clock hits zero wins.
 *
 * Exactly like the "Best of N" format, the human never sees the AI's actual guesses or
 * which flag it's currently on — only a live percentage/attempt-count indicator for
 * whatever flag it's currently working on (see {@link AiOpponentPlan}), plus a running
 * count of how many flags it has fully solved so far. Only once the match ends does the
 * recap reveal each side's per-flag outcome (still never the AI's actual guesses, just
 * whether/how fast it solved each flag in the shared queue).
 *
 * The AI's progress through the queue is not simulated by a background thread: whenever
 * the server needs to know where the AI "is" right now, {@link #refreshAiProgress()}
 * walks it forward through however many flags its precomputed per-flag solve times say
 * it should have finished by now, generating each next plan lazily as it goes.
 */
public class FlaggleBlitzEngineBL implements java.io.Serializable {

    // -------------------- Fields --------------------

    private final CountryController cc;
    private final DifficultyLevel difficulty;
    private final AiSkillLevel aiLevel;

    /** Total length of this Blitz match, in seconds (e.g. 60 or 120). */
    private final double durationSeconds;

    /** The shared sequence of flags both sides race through, fixed for the whole match. */
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
    private double humanCurrentFlagStartElapsed; // elapsed seconds when the human started their current flag
    private BufferedImage humanRevealedFlag; // EASY mode only
    private int humanCorrectCount;
    private final List<BlitzFlagResult> humanHistory = new ArrayList<>();

    // -------------------- AI progress --------------------
    private int aiQueueIndex;
    private double aiCurrentFlagStartElapsed; // elapsed seconds when the AI started its current flag
    private AiOpponentPlan aiPlan;
    private int aiCorrectCount;
    private final List<BlitzFlagResult> aiHistory = new ArrayList<>();

    // -------------------- Constructor --------------------

    public FlaggleBlitzEngineBL(CountryController cc, DifficultyLevel difficulty, AiSkillLevel aiLevel, double durationSeconds) {
        this.cc = cc;
        this.difficulty = difficulty;
        this.aiLevel = aiLevel;
        this.durationSeconds = durationSeconds;
    }

    // -------------------- Match control --------------------

    /**
     * Starts a brand new Blitz match: builds the shared shuffled flag queue and resets
     * both sides' progress and the clock.
     */
    public synchronized void startMatch() {
        queue = new ArrayList<>(cc.getAllCountries());
        Collections.shuffle(queue);

        matchStartTimeMillis = System.currentTimeMillis();
        matchOver = false;
        paused = false;
        pausedMillisTotal = 0;

        humanQueueIndex = 0;
        humanAttemptsThisFlag = 0;
        humanCurrentFlagStartElapsed = 0;
        humanRevealedFlag = null;
        humanCorrectCount = 0;
        humanHistory.clear();

        aiQueueIndex = 0;
        aiCurrentFlagStartElapsed = 0;
        aiCorrectCount = 0;
        aiHistory.clear();
        aiPlan = AiOpponentPlanFactory.createPlan(aiLevel, cc, queue.get(0), difficulty);
    }

    // -------------------- Human guessing --------------------

    /**
     * Processes the human player's guess for whatever flag they're currently on.
     *
     * @return the guess result (for the flag-diff display), or null if the match has
     *         already ended (e.g. the clock ran out a moment earlier) or is paused.
     */
    public synchronized GuessResultBL humanGuess(String countryName) {
        if (paused) return null;
        refreshAiProgress();
        if (matchOver) return null;

        humanAttemptsThisFlag++;
        CountryBL target = queue.get(humanQueueIndex);
        CountryBL guessedCountry = cc.getCountryByName(countryName);

        GuessResultBL result;
        if (difficulty == DifficultyLevel.EASY) {
            result = new GuessResultBL(guessedCountry, target, DifficultyLevel.EASY, humanRevealedFlag);
            humanRevealedFlag = result.getFlagDifferences();
        } else {
            result = new GuessResultBL(guessedCountry, target, DifficultyLevel.HARD);
        }

        if (result.isCorrect()) {
            humanCorrectCount++;
            double timeTaken = getElapsedSeconds() - humanCurrentFlagStartElapsed;
            humanHistory.add(new BlitzFlagResult(humanHistory.size() + 1, target.getName(), true, humanAttemptsThisFlag, timeTaken));
            advanceHumanToNextFlag();
        }

        return result;
    }

    /**
     * The human gives up on their current flag and immediately moves to the next one
     * in the shared queue — it is simply skipped, nobody is credited with it.
     *
     * @return the name of the flag that was given up on (so the UI can reveal it), or
     *         null if the match is already over/paused.
     */
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
        humanRevealedFlag = null;
        humanCurrentFlagStartElapsed = getElapsedSeconds();
    }

    // -------------------- AI progress / clock --------------------

    /**
     * Lazily advances the AI through however many flags in the shared queue it should
     * have already finished by now (per its precomputed per-flag solve times), and
     * ends the match once the clock has run out. Call this before reporting state to
     * the client (status polls and guess/give-up submissions all call it).
     */
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
            aiPlan = AiOpponentPlanFactory.createPlan(aiLevel, cc, queue.get(aiQueueIndex), difficulty);
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

    /** The flag at a given position in the shared queue (same country for both sides). */
    public CountryBL getQueueFlagAt(int index) { return queue.get(index); }

    public AiOpponentPlan getAiPlan() { return aiPlan; }
    /** Elapsed seconds within the AI's *current* flag (i.e. relative to when it started that flag). */
    public double getAiElapsedOnCurrentFlag() {
        return Math.max(0, getElapsedSeconds() - aiCurrentFlagStartElapsed);
    }

    public RoundWinner getMatchWinner() {
        if (!matchOver) return RoundWinner.NONE;
        if (humanCorrectCount > aiCorrectCount) return RoundWinner.HUMAN;
        if (humanCorrectCount < aiCorrectCount) return RoundWinner.AI;
        return RoundWinner.DRAW;
    }

    public DifficultyLevel getDifficulty() { return difficulty; }
    public AiSkillLevel getAiLevel() { return aiLevel; }
    public CountryController getCc() { return cc; }
}
