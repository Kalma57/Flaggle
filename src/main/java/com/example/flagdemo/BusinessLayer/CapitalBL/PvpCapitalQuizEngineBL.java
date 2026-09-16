package com.example.flagdemo.BusinessLayer.CapitalBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.MatchBL.PvpRoundWinner;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.List;

/**
 * Core engine for the capital-quiz's real 1v1 "vs Friend" Match ("First to N") format.
 *
 * Unlike the globe game's PvP (a genuine race, since clicking is open-ended), this mirrors
 * {@link CapitalQuizEngineBL}'s own "both sides always answer" philosophy: both real players
 * see the SAME question and options, each picks independently (their choice hidden from the
 * other until both have answered), and the round resolves the moment BOTH have picked - or
 * after {@link #ANSWER_TIMEOUT_MILLIS} if one player stalls, treating a non-answer as wrong so
 * an AFK opponent can't stall the match forever. A correct answer is +1, a wrong one (including
 * a timeout) is -1, first to {@link #pointsToWin} wins.
 *
 * The next round starts automatically {@link #NEXT_ROUND_DELAY_MILLIS} after a round resolves -
 * there's no explicit "ready up" step, since resolution already only happens once both sides
 * have acted (or timed out), so there's nothing left to wait on.
 *
 * Every mutating method is {@code synchronized}, since both players' HTTP requests can race to
 * answer/pause at (almost) the same time.
 */
public class PvpCapitalQuizEngineBL implements java.io.Serializable {

    private static final long ANSWER_TIMEOUT_MILLIS = 15_000;
    private static final long NEXT_ROUND_DELAY_MILLIS = 3_000;

    private final CountryController cc;
    private final CapitalQuizMode mode;
    private final int pointsToWin;

    private int score1;
    private int score2;
    private int roundNumber;
    private boolean matchOver;

    private long matchStartTimeMillis;
    private long roundStartTimeMillis;

    // -------------------- Pause state --------------------
    private static final long PAUSE_MAX_MILLIS = 60_000;

    private boolean paused;
    private int pausedBySlot; // 0 = not currently paused
    private long pauseStartedAtMillis;
    private long pausedMillisThisRound;
    private long pausedMillisTotalMatch;
    private boolean pause1Used;
    private boolean pause2Used;

    // -------------------- Round state --------------------
    private CountryBL currentTarget;
    private CapitalQuestionOptions currentOptions;
    private Integer picked1; // null = hasn't answered yet
    private Integer picked2;
    private boolean roundResolved;
    private long roundResolvedAtMillis;
    private Boolean lastPlayer1Correct;
    private Boolean lastPlayer2Correct;

    private final List<PvpCapitalRoundResult> roundHistory = new ArrayList<>();

    public PvpCapitalQuizEngineBL(CountryController cc, CapitalQuizMode mode, int pointsToWin) {
        this.cc = cc;
        this.mode = mode;
        this.pointsToWin = pointsToWin;
    }

    // -------------------- Match / Round control --------------------

    public synchronized void startMatch() {
        score1 = 0;
        score2 = 0;
        roundNumber = 0;
        matchOver = false;
        roundHistory.clear();
        matchStartTimeMillis = System.currentTimeMillis();
        startNextRound();
    }

    /** Called on every poll/action from either player - auto-advances once the delay has elapsed. */
    public synchronized void refreshRound() {
        refreshPauseTimeout();
        if (matchOver || paused) return;
        if (!roundResolved) {
            if (getRoundElapsedMillis() >= ANSWER_TIMEOUT_MILLIS) resolveRound();
            return;
        }
        if (System.currentTimeMillis() - roundResolvedAtMillis >= NEXT_ROUND_DELAY_MILLIS) {
            startNextRound();
        }
    }

    private void startNextRound() {
        roundNumber++;
        currentTarget = CapitalOptionsBuilder.pickRandomTarget(cc);
        currentOptions = CapitalOptionsBuilder.build(cc, mode, currentTarget);
        picked1 = null;
        picked2 = null;
        roundResolved = false;
        lastPlayer1Correct = null;
        lastPlayer2Correct = null;
        roundStartTimeMillis = System.currentTimeMillis();
        pausedMillisThisRound = 0;
    }

    // -------------------- Answering --------------------

    public synchronized void submitAnswer(int slot, int optionIndex) {
        refreshRound();
        if (matchOver || paused || roundResolved) return;
        if (slot == 1) {
            if (picked1 != null) return;
            picked1 = optionIndex;
        } else {
            if (picked2 != null) return;
            picked2 = optionIndex;
        }
        if (picked1 != null && picked2 != null) resolveRound();
    }

    private void resolveRound() {
        boolean correct1 = picked1 != null && picked1 == currentOptions.getCorrectIndex();
        boolean correct2 = picked2 != null && picked2 == currentOptions.getCorrectIndex();
        lastPlayer1Correct = correct1;
        lastPlayer2Correct = correct2;
        score1 += correct1 ? 1 : -1;
        score2 += correct2 ? 1 : -1;

        String prompt = (mode == CapitalQuizMode.FLAG_TO_CAPITAL) ? currentTarget.getName() : currentTarget.getCapital();
        String correctText = currentOptions.getOptionTexts().get(currentOptions.getCorrectIndex());
        String picked1Text = picked1 != null ? currentOptions.getOptionTexts().get(picked1) : null;
        String picked2Text = picked2 != null ? currentOptions.getOptionTexts().get(picked2) : null;
        roundHistory.add(new PvpCapitalRoundResult(roundNumber, prompt, correctText, currentTarget.getFlagPath(),
                picked1Text, picked2Text, correct1, correct2));

        roundResolved = true;
        roundResolvedAtMillis = System.currentTimeMillis();

        if (score1 >= pointsToWin || score2 >= pointsToWin) {
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
        pausedMillisThisRound += pausedDuration;
        pausedMillisTotalMatch += pausedDuration;
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

    private long getRoundElapsedMillis() {
        long effectiveNow = paused ? pauseStartedAtMillis : System.currentTimeMillis();
        return effectiveNow - roundStartTimeMillis - pausedMillisThisRound;
    }

    public double getRoundElapsedSeconds() { return getRoundElapsedMillis() / 1000.0; }

    public double getMatchElapsedSeconds() {
        long effectiveNow = paused ? pauseStartedAtMillis : System.currentTimeMillis();
        return (effectiveNow - matchStartTimeMillis - pausedMillisTotalMatch) / 1000.0;
    }

    /** Seconds left before an unanswered round is force-resolved (treating silence as wrong). */
    public double getAnswerTimeoutRemainingSeconds() {
        if (roundResolved) return 0;
        return Math.max(0, (ANSWER_TIMEOUT_MILLIS - getRoundElapsedMillis()) / 1000.0);
    }

    /** Seconds left before the next round auto-starts, 0 if the round isn't resolved yet. */
    public double getNextRoundRemainingSeconds() {
        if (!roundResolved) return 0;
        return Math.max(0, (NEXT_ROUND_DELAY_MILLIS - (System.currentTimeMillis() - roundResolvedAtMillis)) / 1000.0);
    }

    // -------------------- Getters --------------------

    public CapitalQuizMode getMode() { return mode; }
    public int getScore(int slot) { return slot == 1 ? score1 : score2; }
    public boolean hasAnswered(int slot) { return slot == 1 ? picked1 != null : picked2 != null; }
    public Integer getPicked(int slot) { return slot == 1 ? picked1 : picked2; }
    public Boolean isCorrect(int slot) { return slot == 1 ? lastPlayer1Correct : lastPlayer2Correct; }

    public int getRoundNumber() { return roundNumber; }
    public int getPointsToWin() { return pointsToWin; }
    public boolean isMatchOver() { return matchOver; }
    public boolean isRoundOver() { return roundResolved; }
    public PvpRoundWinner getMatchWinner() {
        if (!matchOver) return PvpRoundWinner.NONE;
        return score1 > score2 ? PvpRoundWinner.PLAYER1 : PvpRoundWinner.PLAYER2;
    }

    public CountryBL getCurrentTarget() { return currentTarget; }
    public CapitalQuestionOptions getCurrentOptions() { return currentOptions; }
    public List<PvpCapitalRoundResult> getRoundHistory() { return roundHistory; }
}
