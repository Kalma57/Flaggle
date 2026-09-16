package com.example.flagdemo.BusinessLayer.CapitalGlobeBL;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalAiOpponentPlan;
import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.RoundWinner;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.List;

/**
 * Core engine for the capital-location globe game's "First to N" format vs an AI opponent -
 * same race-to-net-correct rule as {@link com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizEngineBL}
 * (a correct answer is +1, a wrong one is -1, first to {@link #pointsToWin} wins), but a round
 * here is a click-to-select-and-confirm guess (via {@link GuessResultGlobeBL}, unlimited
 * attempts, no hints - the country is meant to be found on the globe itself) rather than a
 * single multiple-choice click - the round simply doesn't resolve until the human gets it
 * right or gives up.
 *
 * The AI side reuses {@link CapitalAiOpponentPlan} (a simple "when, and right-or-wrong" draw) -
 * deliberately not {@link com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobeAiOpponentPlan}'s
 * fancier multi-attempt proximity simulation, to keep this engine's own AI story as simple and
 * self-contained as the other two Capital games'.
 */
public class CapitalGlobeMatchEngineBL implements java.io.Serializable {

    private final CountryController cc;
    private final AiSkillLevel aiLevel;

    /** Net correct answers needed to win the match ("First to N" - e.g. 3, 5, or 7). */
    private final int pointsToWin;

    private int humanScore;
    private int aiScore;
    private int roundNumber;
    private boolean matchOver;

    private long matchStartTimeMillis;
    private long roundStartTimeMillis;

    private boolean paused;
    private long pauseStartedAtMillis;
    private long pausedMillisThisRound;
    private long pausedMillisTotalMatch;

    private CountryBL currentTarget;
    private int humanAttemptsThisRound;
    private boolean currentRoundOver;
    private Boolean lastHumanCorrect;
    private Boolean lastAiCorrect;
    private double lastHumanTimeSeconds;
    private CapitalAiOpponentPlan aiPlan;

    private final List<CapitalGlobeRoundResult> roundHistory = new ArrayList<>();

    public CapitalGlobeMatchEngineBL(CountryController cc, AiSkillLevel aiLevel, int pointsToWin) {
        this.cc = cc;
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
        this.matchStartTimeMillis = System.currentTimeMillis();
        startNextRound();
    }

    public synchronized void advanceToNextRound() {
        if (matchOver) return;
        if (!currentRoundOver) return;
        startNextRound();
    }

    private void startNextRound() {
        roundNumber++;
        currentTarget = CapitalGlobeTargetPicker.pickRandomTarget(cc);
        humanAttemptsThisRound = 0;
        currentRoundOver = false;
        lastHumanCorrect = null;
        lastAiCorrect = null;
        roundStartTimeMillis = System.currentTimeMillis();
        pausedMillisThisRound = 0;
        aiPlan = CapitalGlobeAiOpponentPlanFactory.createPlan(aiLevel);
    }

    // -------------------- Guessing --------------------

    public synchronized GuessResultGlobeBL humanGuess(String countryName) {
        if (matchOver || paused || currentRoundOver) return null;

        // A click can land on a polygon from the map dataset that has no matching row in our
        // own DB - treat that as a no-op, not a wasted attempt.
        CountryBL guessedCountry = cc.getCountryByName(countryName);
        if (guessedCountry == null) return null;

        humanAttemptsThisRound++;
        GuessResultGlobeBL result = new GuessResultGlobeBL(guessedCountry, currentTarget);

        if (result.isCorrect()) {
            resolveRound(true);
        }

        return result;
    }

    public synchronized void giveUpRound() {
        if (matchOver || paused || currentRoundOver) return;
        resolveRound(false);
    }

    /** Resolves both sides at once - the AI's own answer is a fixed fact drawn at round start, not a race. */
    private void resolveRound(boolean humanCorrect) {
        lastHumanTimeSeconds = getRoundElapsedSeconds();
        lastHumanCorrect = humanCorrect;
        humanScore += humanCorrect ? 1 : -1;

        boolean aiCorrect = aiPlan.isCorrect();
        lastAiCorrect = aiCorrect;
        aiScore += aiCorrect ? 1 : -1;

        currentRoundOver = true;
        finishRound();
    }

    private void finishRound() {
        roundHistory.add(new CapitalGlobeRoundResult(roundNumber, currentTarget.getName(), currentTarget.getCapital(),
                currentTarget.getFlagPath(), lastHumanCorrect, lastAiCorrect, humanAttemptsThisRound,
                lastHumanTimeSeconds, aiPlan.getAnswerTimeSeconds()));

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
    public int getHumanAttemptsThisRound() { return humanAttemptsThisRound; }
    public AiSkillLevel getAiLevel() { return aiLevel; }
    public List<CapitalGlobeRoundResult> getRoundHistory() { return roundHistory; }

    /** Whether the AI has (silently) "locked in" its answer yet - purely a live "thinking..." indicator. */
    public boolean isAiAnswered() {
        return aiPlan != null && aiPlan.hasAnswered(getRoundElapsedSeconds());
    }
}
