package com.example.flagdemo.BusinessLayer.CapitalBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.RoundWinner;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Core engine for a 1v1 "race to N correct" capital-city quiz vs an AI opponent.
 *
 * Unlike Flaggle/Globe's Best-of-N (where a round is won by whichever side answers first),
 * here BOTH sides always answer every round independently: a correct answer is +1, a wrong
 * answer is -1 (scores can go negative), and the first side whose net score reaches
 * {@link #pointsToWin} wins the match. The AI's "thinking" delay ({@link CapitalAiOpponentPlan})
 * is kept purely for pacing/suspense in the UI - it no longer determines who "gets" the round,
 * since a round is fully resolved the instant the human answers (both sides' correctness are
 * already fixed facts drawn at round start).
 *
 * One engine serves both quiz directions ({@link CapitalQuizMode}) since they're the same
 * state machine with the prompt/options swapped - avoids maintaining two near-identical
 * copies of this class the way Flaggle/Globe's otherwise-unrelated engines are kept separate.
 *
 * Catch-up: a match can in principle run long if the human keeps missing questions (there's no
 * cap on rounds). Rather than secretly buffing the AI to force an ending, once the round count
 * passes {@link #STAGNATION_ROUNDS_PER_POINT} x {@link #pointsToWin} without the match
 * resolving, target selection starts leaning toward capitals the human has personally gotten
 * wrong this match (they were just shown the correct answer, so a repeat is a fair second shot,
 * not free help) - see {@link #pickTarget()}. A capital drops back out of that pool the moment
 * they get it right.
 */
public class CapitalQuizEngineBL implements java.io.Serializable {

    private static final int STAGNATION_ROUNDS_PER_POINT = 5;
    private static final int MISSED_TARGET_WEIGHT = 4;

    // -------------------- Fields --------------------

    private final CountryController cc;
    private final CapitalQuizMode mode;
    private final AiSkillLevel aiLevel;

    /** Net correct answers needed to win the match ("First to N" - e.g. 3, 5, or 7). */
    private final int pointsToWin;

    private int humanScore;
    private int aiScore;
    private int roundNumber;
    private boolean matchOver;

    private long matchStartTimeMillis;
    private long roundStartTimeMillis;

    // -------------------- Pause state (unlimited, vs-computer only - no cap needed) --------------------
    private boolean paused;
    private long pauseStartedAtMillis;
    private long pausedMillisThisRound;
    private long pausedMillisTotalMatch;

    // -------------------- Round state --------------------
    private CountryBL currentTarget;
    private List<String> currentOptions;             // 4 shuffled option strings
    private List<CountryBL> currentOptionCountries;  // parallel list - the country behind each option (for flag icons)
    private int correctOptionIndex;
    private int humanPickedIndex = -1;       // -1 = hasn't answered yet
    private boolean currentRoundOver;
    private Boolean lastHumanCorrect;  // null until the round resolves
    private Boolean lastAiCorrect;
    private double lastHumanTimeSeconds;
    private CapitalAiOpponentPlan aiPlan;

    private final List<CapitalRoundResult> roundHistory = new ArrayList<>();

    /** Capitals the human has gotten wrong so far this match - drives the catch-up mechanism. */
    private final Set<CountryBL> missedTargets = new LinkedHashSet<>();

    // -------------------- Constructor --------------------

    public CapitalQuizEngineBL(CountryController cc, CapitalQuizMode mode, AiSkillLevel aiLevel, int pointsToWin) {
        this.cc = cc;
        this.mode = mode;
        this.aiLevel = aiLevel;
        this.pointsToWin = pointsToWin;
    }

    // -------------------- Match / Round control --------------------

    public synchronized void startMatch() {
        this.humanScore = 0;
        this.aiScore = 0;
        this.roundNumber = 0;
        this.matchOver = false;
        this.roundHistory.clear();
        this.missedTargets.clear();
        this.matchStartTimeMillis = System.currentTimeMillis();
        startNextRound();
    }

    public synchronized void advanceToNextRound() {
        if (matchOver) return;
        if (!currentRoundOver) return; // round still in progress
        startNextRound();
    }

    private void startNextRound() {
        roundNumber++;
        currentTarget = pickTarget();
        buildOptions(currentTarget);
        humanPickedIndex = -1;
        currentRoundOver = false;
        lastHumanCorrect = null;
        lastAiCorrect = null;
        roundStartTimeMillis = System.currentTimeMillis();
        pausedMillisThisRound = 0;
        aiPlan = CapitalAiOpponentPlanFactory.createPlan(aiLevel);
    }

    private CountryBL pickTarget() {
        int stagnationThreshold = STAGNATION_ROUNDS_PER_POINT * pointsToWin;
        if (roundNumber > stagnationThreshold && !missedTargets.isEmpty()) {
            return pickCatchUpTarget();
        }
        return CapitalOptionsBuilder.pickRandomTarget(cc);
    }

    /** Builds the eligible pool with each missed capital repeated {@link #MISSED_TARGET_WEIGHT} times over. */
    private CountryBL pickCatchUpTarget() {
        List<CountryBL> weightedPool = new ArrayList<>(CapitalOptionsBuilder.eligibleTargets(cc));
        for (CountryBL missed : missedTargets) {
            for (int i = 1; i < MISSED_TARGET_WEIGHT; i++) weightedPool.add(missed);
        }
        return weightedPool.get(new Random().nextInt(weightedPool.size()));
    }

    private void buildOptions(CountryBL target) {
        CapitalQuestionOptions options = CapitalOptionsBuilder.build(cc, mode, target);
        currentOptions = options.getOptionTexts();
        currentOptionCountries = options.getOptionCountries();
        correctOptionIndex = options.getCorrectIndex();
    }

    // -------------------- Answering --------------------

    /** @return true if this was the correct answer. */
    public synchronized boolean submitAnswer(int optionIndex) {
        if (matchOver || paused || currentRoundOver) return false;
        if (humanPickedIndex != -1) return false; // already answered this round

        humanPickedIndex = optionIndex;
        boolean correct = optionIndex == correctOptionIndex;
        resolveRound(correct);
        return correct;
    }

    /** Resolves both sides at once - see the class javadoc for why the AI's answer isn't a race anymore. */
    private void resolveRound(boolean humanCorrect) {
        lastHumanTimeSeconds = getRoundElapsedSeconds();
        lastHumanCorrect = humanCorrect;
        humanScore += humanCorrect ? 1 : -1;
        if (humanCorrect) missedTargets.remove(currentTarget);
        else missedTargets.add(currentTarget);

        boolean aiCorrect = aiPlan.isCorrect();
        lastAiCorrect = aiCorrect;
        aiScore += aiCorrect ? 1 : -1;

        currentRoundOver = true;
        finishRound();
    }

    private void finishRound() {
        String prompt = (mode == CapitalQuizMode.FLAG_TO_CAPITAL) ? currentTarget.getName() : currentTarget.getCapital();
        String correctText = currentOptions.get(correctOptionIndex);
        String pickedText = humanPickedIndex >= 0 ? currentOptions.get(humanPickedIndex) : null;
        roundHistory.add(new CapitalRoundResult(roundNumber, prompt, correctText, pickedText, lastHumanCorrect, lastAiCorrect,
                currentTarget.getFlagPath(), lastHumanTimeSeconds, aiPlan.getAnswerTimeSeconds()));

        // Human is checked first: on the rare round where both sides cross the target in the
        // same round, the human's own answer is treated as having "landed" first.
        if (humanScore >= pointsToWin || aiScore >= pointsToWin) {
            matchOver = true;
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
        pausedMillisThisRound += pausedDuration;
        pausedMillisTotalMatch += pausedDuration;
        paused = false;
    }

    public boolean isPaused() { return paused; }

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

    public CapitalQuizMode getMode() { return mode; }
    public int getHumanScore() { return humanScore; }
    public int getAiScore() { return aiScore; }
    public int getRoundNumber() { return roundNumber; }
    public int getPointsToWin() { return pointsToWin; }
    public boolean isMatchOver() { return matchOver; }
    public boolean isRoundOver() { return currentRoundOver; }
    public Boolean isLastHumanCorrect() { return lastHumanCorrect; }
    public Boolean isLastAiCorrect() { return lastAiCorrect; }
    public RoundWinner getMatchWinner() {
        if (!matchOver) return RoundWinner.NONE;
        return humanScore >= pointsToWin ? RoundWinner.HUMAN : RoundWinner.AI;
    }

    public CountryBL getCurrentTarget() { return currentTarget; }
    public List<String> getCurrentOptions() { return currentOptions; }
    public List<CountryBL> getCurrentOptionCountries() { return currentOptionCountries; }
    public int getCorrectOptionIndex() { return correctOptionIndex; }
    public int getHumanPickedIndex() { return humanPickedIndex; }
    public AiSkillLevel getAiLevel() { return aiLevel; }
    public List<CapitalRoundResult> getRoundHistory() { return roundHistory; }

    /** Whether the AI has (silently) "locked in" its answer yet - purely a live "thinking..." indicator; the outcome is already decided regardless. */
    public boolean isAiAnswered() {
        return aiPlan != null && aiPlan.hasAnswered(getRoundElapsedSeconds());
    }
}
