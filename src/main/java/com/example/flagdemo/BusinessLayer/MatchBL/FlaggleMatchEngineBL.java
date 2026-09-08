package com.example.flagdemo.BusinessLayer.MatchBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Core engine for a 1v1 Flaggle Match ("Best of N" format — N is 3, 5, or 7, i.e. first to
 * 2, 3, or 4 round wins respectively), vs an AI opponent.
 *
 * Both sides try to guess the SAME target flag every round. Whoever guesses it correctly
 * first wins the round. Neither side ever sees the other's actual guesses — the human only
 * ever sees their own guess history; the AI opponent's "progress" is exposed to the human
 * only as a time-based percentage/attempt-count (see {@link AiOpponentPlan}), never as real
 * flags or country names.
 *
 * The AI opponent is not a background thread or an agent loop: at the start of every round a
 * whole "decision plan" is computed once (how many attempts, and exactly when the correct one
 * lands), and every subsequent status check just compares elapsed time against that plan.
 */
public class FlaggleMatchEngineBL implements java.io.Serializable {

    // -------------------- Fields --------------------

    private final CountryController cc;
    private final DifficultyLevel difficulty;
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
    // Pausing must freeze BOTH the match clock and the AI's precomputed timeline (which is
    // driven purely by wall-clock elapsed time), so we simply track how many milliseconds
    // have been "spent paused" and subtract that from every elapsed-time computation below.
    private boolean paused;
    private long pauseStartedAtMillis;
    private long pausedMillisThisRound;   // resets every round — used for round-elapsed calc
    private long pausedMillisTotalMatch;  // accumulates all match — used for match-elapsed calc

    private CountryBL currentTarget;
    private int humanAttemptsThisRound;

    // EASY-mode only: the human's accumulated reveal image for the current round.
    private BufferedImage humanRevealedFlag;

    private RoundWinner currentRoundWinner;
    private AiOpponentPlan aiPlan;

    private final List<MatchRoundResult> roundHistory = new ArrayList<>();

    // -------------------- Constructor --------------------

    public FlaggleMatchEngineBL(CountryController cc, DifficultyLevel difficulty, AiSkillLevel aiLevel, int pointsToWin) {
        this.cc = cc;
        this.difficulty = difficulty;
        this.aiLevel = aiLevel;
        this.pointsToWin = pointsToWin;
    }

    // -------------------- Match / Round control --------------------

    /**
     * Starts a brand new match: resets scores/history and begins round 1.
     */
    public synchronized void startMatch() throws SQLException {
        this.humanScore = 0;
        this.aiScore = 0;
        this.roundNumber = 0;
        this.matchOver = false;
        this.roundHistory.clear();
        this.matchStartTimeMillis = System.currentTimeMillis();
        startNextRound();
    }

    /**
     * Advances to the next round. Only valid once the current round has a winner
     * and the match itself isn't over yet.
     */
    public synchronized void advanceToNextRound() throws SQLException {
        if (matchOver) return;
        if (currentRoundWinner == RoundWinner.NONE) return; // round still in progress
        startNextRound();
    }

    private void startNextRound() throws SQLException {
        roundNumber++;
        currentTarget = selectRandomCountry();
        humanAttemptsThisRound = 0;
        humanRevealedFlag = null;
        currentRoundWinner = RoundWinner.NONE;
        roundStartTimeMillis = System.currentTimeMillis();
        pausedMillisThisRound = 0;
        aiPlan = AiOpponentPlanFactory.createPlan(aiLevel, cc, currentTarget, difficulty);
    }

    private CountryBL selectRandomCountry() throws SQLException {
        // Pick directly from the loaded country list rather than a random ID in
        // [1, count]: DB row IDs are not guaranteed to be a contiguous 1..count range
        // (e.g. countries with missing flag images have been removed from the table,
        // leaving gaps), so looking up a random ID could silently return null and
        // crash later rounds — more rounds (Best of 7) meant more chances to hit a gap.
        List<CountryBL> allCountries = cc.getAllCountries();
        Random rand = new Random();
        return allCountries.get(rand.nextInt(allCountries.size()));
    }

    // -------------------- Guessing --------------------

    /**
     * Processes the human player's guess for the current round.
     *
     * @return the guess result (for the flag-diff display), or null if the round is
     *         already over (e.g. the AI's timeout was reached a moment earlier) or the
     *         match itself has ended.
     */
    public synchronized GuessResultBL humanGuess(String countryName) {
        if (matchOver || paused) return null;

        // Resolve any pending AI timeout first, in case it already happened
        // (e.g. two nearly-simultaneous requests) before crediting a human win.
        refreshAiTimeout();
        if (currentRoundWinner != RoundWinner.NONE) return null;

        humanAttemptsThisRound++;
        CountryBL guessedCountry = cc.getCountryByName(countryName);

        GuessResultBL result;
        if (difficulty == DifficultyLevel.EASY) {
            result = new GuessResultBL(guessedCountry, currentTarget, DifficultyLevel.EASY, humanRevealedFlag);
            humanRevealedFlag = result.getFlagDifferences();
        } else {
            result = new GuessResultBL(guessedCountry, currentTarget, DifficultyLevel.HARD);
        }

        if (result.isCorrect()) {
            currentRoundWinner = RoundWinner.HUMAN;
            humanScore++;
            finishRound();
        }

        return result;
    }

    /**
     * The human gives up on the current round — the AI is credited with the round win.
     */
    public synchronized void humanGiveUpRound() {
        if (matchOver || paused || currentRoundWinner != RoundWinner.NONE) return;
        currentRoundWinner = RoundWinner.AI;
        aiScore++;
        finishRound();
    }

    /**
     * Lazily resolves an AI win-by-timeout: call this before reporting state to the client
     * (status polls and guess submissions both call it) so the AI's precomputed solve time
     * gets credited even if nobody happened to be actively guessing at that exact moment.
     */
    public synchronized void refreshAiTimeout() {
        if (matchOver || paused || currentRoundWinner != RoundWinner.NONE) return;
        double elapsed = getRoundElapsedSeconds();
        if (aiPlan.isSolved(elapsed)) {
            currentRoundWinner = RoundWinner.AI;
            aiScore++;
            finishRound();
        }
    }

    private void finishRound() {
        double elapsed = getRoundElapsedSeconds();
        int aiAttemptsAtEnd = aiPlan.getAttemptsSoFar(elapsed);
        roundHistory.add(new MatchRoundResult(roundNumber, currentRoundWinner, currentTarget.getName(), humanAttemptsThisRound, aiAttemptsAtEnd));

        if (humanScore >= pointsToWin || aiScore >= pointsToWin) {
            matchOver = true;
        }
    }

    // -------------------- Pause / Resume --------------------

    /**
     * Pauses the match: freezes both the match clock and the AI's precomputed timeline
     * (which is otherwise driven purely by wall-clock elapsed time) at their current values.
     */
    public synchronized void pauseMatch() {
        if (paused || matchOver) return;
        paused = true;
        pauseStartedAtMillis = System.currentTimeMillis();
    }

    /**
     * Resumes a paused match: the time spent paused is "erased" from both the round and
     * match clocks, so nothing (including the AI) silently advances while paused.
     */
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
    /** The full match format as a "best of N" number, e.g. pointsToWin=3 -> Best of 5. */
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
    public AiOpponentPlan getAiPlan() { return aiPlan; }
    public DifficultyLevel getDifficulty() { return difficulty; }
    public AiSkillLevel getAiLevel() { return aiLevel; }
    public List<MatchRoundResult> getRoundHistory() { return roundHistory; }
    public CountryController getCc() { return cc; }
}
