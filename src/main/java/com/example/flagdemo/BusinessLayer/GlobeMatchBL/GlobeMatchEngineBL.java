package com.example.flagdemo.BusinessLayer.GlobeMatchBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.CountryNameHintUtil;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.ProximityLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.MatchRoundResult;
import com.example.flagdemo.BusinessLayer.MatchBL.RoundWinner;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Core engine for a 1v1 Globe Match ("Best of N" format — N is 3, 5, or 7), vs an AI
 * opponent — mirrors {@link com.example.flagdemo.BusinessLayer.MatchBL.FlaggleMatchEngineBL}
 * exactly, just guessing countries by location (via {@link GuessResultGlobeBL}) instead of
 * by flag, and exposing the opponent's live progress as a best-{@link ProximityLevel}
 * reached so far (a colored badge) rather than a match percentage.
 *
 * The AI opponent is not a background thread or an agent loop: at the start of every round
 * a whole "decision plan" is computed once (how many attempts, and exactly when the correct
 * one lands), and every subsequent status check just compares elapsed time against that plan.
 *
 * There's no cap on human guesses per round - a player can keep trying until they solve it,
 * run out the AI's clock against them, or give up (see {@link #humanGiveUpRound()}). A hint
 * reveals one more letter of the target's name at a random position (same masking as the
 * single-player {@link com.example.flagdemo.BusinessLayer.GlobeBL.GlobeEngineBL}).
 *
 * A hint isn't free: every correct human guess is provisional for {@code 20 * hintsUsedThisRound}
 * seconds (see {@link #GRACE_SECONDS_PER_HINT}) - a "grace period" during which the round isn't
 * decided yet and the AI's elapsed-time-driven plan keeps running exactly as before. If the AI's
 * plan resolves before the grace period ends, the AI steals the round; otherwise the human's win
 * becomes final once the grace period elapses. A human who never used a hint wins immediately,
 * with no grace period at all.
 */
public class GlobeMatchEngineBL implements java.io.Serializable {

    // -------------------- Fields --------------------

    private final CountryController cc;
    private final AiSkillLevel aiLevel;

    /** Rounds needed to win the match (e.g. Best of 3 -> 2, Best of 5 -> 3, Best of 7 -> 4). */
    private final int pointsToWin;

    private int humanScore;
    private int aiScore;
    private int roundNumber;
    private boolean matchOver;

    private long matchStartTimeMillis;
    private long roundStartTimeMillis;

    // -------------------- Pause state --------------------
    private boolean paused;
    private long pauseStartedAtMillis;
    private long pausedMillisThisRound;
    private long pausedMillisTotalMatch;

    // -------------------- Hints --------------------
    private static final int GRACE_SECONDS_PER_HINT = 20;

    private int hintsUsedThisRound;
    private final Set<Integer> revealedLetterPositions = new HashSet<>();

    private CountryBL currentTarget;
    private int humanAttemptsThisRound;

    private RoundWinner currentRoundWinner;
    private GlobeAiOpponentPlan aiPlan;

    // -------------------- Grace period (provisional win) --------------------
    // Once the human guesses correctly, if they used hints this round, the round isn't over
    // yet - the AI gets a grace window (proportional to hints used) to still solve it and
    // steal the round. provisionalWinner stays NONE the rest of the time.
    private RoundWinner provisionalWinner = RoundWinner.NONE;
    private double graceDeadlineElapsedSeconds;

    /** Snapshot of {@link #getRoundElapsedSeconds()} taken the instant the last round ended - unlike the live value, this doesn't keep climbing while the round-over recap is showing. */
    private double lastRoundDurationSeconds;

    private final List<MatchRoundResult> roundHistory = new ArrayList<>();

    // -------------------- Constructor --------------------

    public GlobeMatchEngineBL(CountryController cc, AiSkillLevel aiLevel, int pointsToWin) {
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
        if (currentRoundWinner == RoundWinner.NONE) return; // round still in progress
        startNextRound();
    }

    private void startNextRound() {
        roundNumber++;
        currentTarget = selectRandomCountry();
        humanAttemptsThisRound = 0;
        hintsUsedThisRound = 0;
        revealedLetterPositions.clear();
        currentRoundWinner = RoundWinner.NONE;
        provisionalWinner = RoundWinner.NONE;
        roundStartTimeMillis = System.currentTimeMillis();
        pausedMillisThisRound = 0;
        aiPlan = GlobeAiOpponentPlanFactory.createPlan(aiLevel, cc, currentTarget);
    }

    private CountryBL selectRandomCountry() {
        List<CountryBL> valid = new ArrayList<>();
        for (CountryBL c : cc.getAllCountries()) {
            if (c.getLatitude() == 0.0 && c.getLongitude() == 0.0) continue;
            valid.add(c);
        }
        Random rand = new Random();
        return valid.get(rand.nextInt(valid.size()));
    }

    // -------------------- Guessing --------------------

    public synchronized GuessResultGlobeBL humanGuess(String countryName) {
        if (matchOver || paused) return null;

        refreshAiTimeout();
        if (currentRoundWinner != RoundWinner.NONE) return null;
        if (provisionalWinner == RoundWinner.HUMAN) return null; // already found it, waiting out the grace period

        humanAttemptsThisRound++;
        CountryBL guessedCountry = cc.getCountryByName(countryName);
        GuessResultGlobeBL result = new GuessResultGlobeBL(guessedCountry, currentTarget);

        if (result.isCorrect()) {
            int graceSeconds = GRACE_SECONDS_PER_HINT * hintsUsedThisRound;
            if (graceSeconds <= 0) {
                currentRoundWinner = RoundWinner.HUMAN;
                humanScore++;
                finishRound();
            } else {
                provisionalWinner = RoundWinner.HUMAN;
                graceDeadlineElapsedSeconds = getRoundElapsedSeconds() + graceSeconds;
            }
        }

        return result;
    }

    public synchronized void humanGiveUpRound() {
        if (matchOver || paused || currentRoundWinner != RoundWinner.NONE) return;
        if (provisionalWinner == RoundWinner.HUMAN) return; // already found it - nothing to give up
        currentRoundWinner = RoundWinner.AI;
        aiScore++;
        finishRound();
    }

    // -------------------- Hints --------------------

    /**
     * Reveals one more letter of the target country's name at a random position - doesn't
     * touch the round clock directly, but every hint used this round adds
     * {@link #GRACE_SECONDS_PER_HINT} seconds to the grace window the AI gets once the human
     * finds the country (see {@link #humanGuess(String)}).
     */
    public synchronized String useHint() {
        if (matchOver || paused || currentRoundWinner != RoundWinner.NONE) return getHintMaskedName();
        if (provisionalWinner == RoundWinner.HUMAN) return getHintMaskedName(); // already found it

        int totalLetters = CountryNameHintUtil.countRevealableLetters(currentTarget.getName());
        if (revealedLetterPositions.size() < totalLetters) {
            CountryNameHintUtil.revealRandomLetter(currentTarget.getName(), revealedLetterPositions);
            hintsUsedThisRound++;
        }

        return getHintMaskedName();
    }

    public String getHintMaskedName() {
        if (currentTarget == null) return "";
        return CountryNameHintUtil.maskName(currentTarget.getName(), revealedLetterPositions);
    }

    public int getHintsUsedThisRound() { return hintsUsedThisRound; }

    /** How long the just-finished round took, frozen at the moment it ended - 0 before any round has ended. */
    public double getLastRoundDurationSeconds() { return lastRoundDurationSeconds; }

    /** NONE unless the human has found the country but is still in the post-win grace period. */
    public RoundWinner getProvisionalWinner() { return provisionalWinner; }

    /** Seconds left in the grace period, 0 if there isn't one active. */
    public double getGraceRemainingSeconds() {
        if (provisionalWinner == RoundWinner.NONE) return 0;
        return Math.max(0, graceDeadlineElapsedSeconds - getRoundElapsedSeconds());
    }

    public synchronized void refreshAiTimeout() {
        if (matchOver || paused || currentRoundWinner != RoundWinner.NONE) return;
        double elapsed = getRoundElapsedSeconds();

        if (provisionalWinner == RoundWinner.HUMAN) {
            // Grace period active - the AI can still steal the round if its own plan resolves
            // before the deadline; otherwise the human's win becomes final once it passes.
            if (aiPlan.isSolved(elapsed)) {
                currentRoundWinner = RoundWinner.AI;
                aiScore++;
                finishRound();
            } else if (elapsed >= graceDeadlineElapsedSeconds) {
                currentRoundWinner = RoundWinner.HUMAN;
                humanScore++;
                finishRound();
            }
            return;
        }

        if (aiPlan.isSolved(elapsed)) {
            currentRoundWinner = RoundWinner.AI;
            aiScore++;
            finishRound();
        }
    }

    private void finishRound() {
        double elapsed = getRoundElapsedSeconds();
        lastRoundDurationSeconds = elapsed;
        int aiAttemptsAtEnd = aiPlan.getAttemptsSoFar(elapsed);
        roundHistory.add(new MatchRoundResult(roundNumber, currentRoundWinner, currentTarget.getName(), humanAttemptsThisRound, aiAttemptsAtEnd));

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
    public int getBestOf() { return pointsToWin * 2 - 1; }
    public boolean isMatchOver() { return matchOver; }
    public boolean isRoundOver() { return currentRoundWinner != RoundWinner.NONE; }
    public RoundWinner getCurrentRoundWinner() { return currentRoundWinner; }
    public RoundWinner getMatchWinner() {
        if (!matchOver) return RoundWinner.NONE;
        return humanScore > aiScore ? RoundWinner.HUMAN : RoundWinner.AI;
    }

    public CountryBL getCurrentTarget() { return currentTarget; }
    public int getHumanAttemptsThisRound() { return humanAttemptsThisRound; }
    public GlobeAiOpponentPlan getAiPlan() { return aiPlan; }
    /** The best (closest) proximity band the AI has reached so far this round, or null if it hasn't guessed yet. */
    public ProximityLevel getAiProgressLevel() { return aiPlan.getProgressLevel(getRoundElapsedSeconds()); }
    public AiSkillLevel getAiLevel() { return aiLevel; }
    public List<MatchRoundResult> getRoundHistory() { return roundHistory; }
    public CountryController getCc() { return cc; }
}
