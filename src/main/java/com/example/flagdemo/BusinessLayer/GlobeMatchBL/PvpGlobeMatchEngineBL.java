package com.example.flagdemo.BusinessLayer.GlobeMatchBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.CountryNameHintUtil;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.ProximityLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.PvpRoundResult;
import com.example.flagdemo.BusinessLayer.MatchBL.PvpRoundWinner;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Core engine for a real 1v1 "vs Friend" Globe Match ("Best of N" format), modeled closely
 * on {@link com.example.flagdemo.BusinessLayer.MatchBL.PvpMatchEngineBL} but symmetric: BOTH
 * sides are real players (slot 1 and slot 2), guessing countries by location instead of by
 * flag. Neither player ever sees the other's actual guesses - each browser only ever
 * displays its own guess history; the opponent's live progress is exposed only as a
 * colored badge for the best {@link ProximityLevel} reached so far this round.
 *
 * There's no cap on guesses per player per round - a player can keep trying until they solve
 * it, get overtaken by the other side, or give up (see {@link #giveUpRound(int)}). A hint
 * reveals one more letter of the target's name at a random position (same masking as the
 * single-player {@link com.example.flagdemo.BusinessLayer.GlobeBL.GlobeEngineBL}), personal
 * to whoever takes it. Both players' hint counts are visible to each other live.
 *
 * A hint isn't free: when a player guesses correctly, if they've used MORE hints than their
 * opponent this round, the round isn't decided yet - the opponent gets a "grace period" of
 * {@code 20 * (myHints - opponentHints)} seconds (see {@link #GRACE_SECONDS_PER_HINT}) to
 * still guess correctly and steal the round. Every hint the opponent takes DURING that grace
 * period shaves 20 seconds off their own remaining time, same as it would have before the
 * round ended - a hint always costs 20 seconds of your own runway, wherever that runway
 * currently is. If the leader used no more hints than the opponent, they win immediately with
 * no grace period at all.
 *
 * Every mutating method is {@code synchronized} on this engine instance, since both
 * players' HTTP requests can race to guess at (almost) the same time.
 */
public class PvpGlobeMatchEngineBL implements java.io.Serializable {

    // -------------------- Fields --------------------

    private final CountryController cc;

    /** Rounds needed to win the match (e.g. Best of 3 -> 2, Best of 5 -> 3, Best of 7 -> 4). */
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

    // -------------------- Ready-up state (between rounds) --------------------
    // Once a round ends, neither player is thrown straight into the next one anymore -
    // both browsers show a "ready up" prompt so everyone gets a moment to see the round
    // recap. The next round starts as soon as BOTH players have confirmed ready, or after
    // READY_MAX_MILLIS elapses, whichever comes first (so one AFK player can't stall the
    // match forever).
    private static final long READY_MAX_MILLIS = 7_000;

    private boolean ready1;
    private boolean ready2;
    private long roundOverAtMillis;

    // -------------------- Hints --------------------
    private static final int GRACE_SECONDS_PER_HINT = 20;

    private int hints1UsedThisRound;
    private int hints2UsedThisRound;
    private final Set<Integer> revealedLetterPositions1 = new HashSet<>();
    private final Set<Integer> revealedLetterPositions2 = new HashSet<>();

    private CountryBL currentTarget;
    private int attempts1;
    private int attempts2;

    /** Snapshot of {@link #getRoundElapsedSeconds()} taken the instant the last round ended - unlike the live value, this doesn't keep climbing while the round-over recap is showing. */
    private double lastRoundDurationSeconds;

    // -------------------- Grace period (provisional win) --------------------
    // Once a player guesses correctly, if their hint count was higher than their opponent's,
    // the round isn't decided yet - provisionalWinner names who found it, and the other side
    // has until graceDeadlineElapsedSeconds to steal it back. NONE the rest of the time.
    private PvpRoundWinner provisionalWinner = PvpRoundWinner.NONE;
    private double graceDeadlineElapsedSeconds;

    // Best (lowest-ordinal) ProximityLevel either player has reached so far this round -
    // NONE_ORDINAL means "hasn't guessed yet". This is what the OTHER player's browser is
    // shown as live opponent progress (a colored badge).
    private static final int NONE_ORDINAL = -1;
    private int bestOrdinal1 = NONE_ORDINAL;
    private int bestOrdinal2 = NONE_ORDINAL;

    private PvpRoundWinner currentRoundWinner;

    private final List<PvpRoundResult> roundHistory = new ArrayList<>();

    // -------------------- Constructor --------------------

    public PvpGlobeMatchEngineBL(CountryController cc, int pointsToWin) {
        this.cc = cc;
        this.pointsToWin = pointsToWin;
    }

    // -------------------- Match / Round control --------------------

    public synchronized void startMatch() {
        this.score1 = 0;
        this.score2 = 0;
        this.roundNumber = 0;
        this.matchOver = false;
        this.roundHistory.clear();
        this.matchStartTimeMillis = System.currentTimeMillis();
        startNextRound();
    }

    public synchronized void advanceToNextRound() {
        refreshPauseTimeout();
        refreshGraceTimeout();
        if (matchOver || paused) return;
        if (currentRoundWinner == PvpRoundWinner.NONE) return; // round still in progress
        if (!(ready1 && ready2) && !readyDeadlinePassed()) return; // waiting on a player, and still within the grace window
        startNextRound();
    }

    private void startNextRound() {
        roundNumber++;
        currentTarget = selectRandomCountry();
        attempts1 = 0;
        attempts2 = 0;
        bestOrdinal1 = NONE_ORDINAL;
        bestOrdinal2 = NONE_ORDINAL;
        currentRoundWinner = PvpRoundWinner.NONE;
        provisionalWinner = PvpRoundWinner.NONE;
        roundStartTimeMillis = System.currentTimeMillis();
        pausedMillisThisRound = 0;
        ready1 = false;
        ready2 = false;
        hints1UsedThisRound = 0;
        hints2UsedThisRound = 0;
        revealedLetterPositions1.clear();
        revealedLetterPositions2.clear();
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

    public synchronized GuessResultGlobeBL submitGuess(int slot, String countryName) {
        refreshPauseTimeout();
        refreshGraceTimeout();
        if (matchOver || paused) return null;
        if (currentRoundWinner != PvpRoundWinner.NONE) return null;

        PvpRoundWinner mySlotWinner = (slot == 1) ? PvpRoundWinner.PLAYER1 : PvpRoundWinner.PLAYER2;
        if (provisionalWinner == mySlotWinner) return null; // already found it - waiting out my own grace period

        CountryBL guessedCountry = cc.getCountryByName(countryName);
        GuessResultGlobeBL result = new GuessResultGlobeBL(guessedCountry, currentTarget);
        int ordinal = result.getProximityLevel().ordinal();

        if (slot == 1) {
            attempts1++;
            if (bestOrdinal1 == NONE_ORDINAL || ordinal < bestOrdinal1) bestOrdinal1 = ordinal;
        } else {
            attempts2++;
            if (bestOrdinal2 == NONE_ORDINAL || ordinal < bestOrdinal2) bestOrdinal2 = ordinal;
        }

        if (result.isCorrect()) {
            if (provisionalWinner != PvpRoundWinner.NONE) {
                // The other side already provisionally won, but I caught up within the grace
                // window - I steal the round.
                currentRoundWinner = mySlotWinner;
                if (slot == 1) score1++; else score2++;
                finishRound();
            } else {
                int myHints = (slot == 1) ? hints1UsedThisRound : hints2UsedThisRound;
                int opponentHints = (slot == 1) ? hints2UsedThisRound : hints1UsedThisRound;
                int graceSeconds = GRACE_SECONDS_PER_HINT * (myHints - opponentHints);
                if (graceSeconds <= 0) {
                    currentRoundWinner = mySlotWinner;
                    if (slot == 1) score1++; else score2++;
                    finishRound();
                } else {
                    provisionalWinner = mySlotWinner;
                    graceDeadlineElapsedSeconds = getRoundElapsedSeconds() + graceSeconds;
                }
            }
        }

        return result;
    }

    public synchronized void giveUpRound(int slot) {
        refreshPauseTimeout();
        refreshGraceTimeout();
        if (matchOver || paused || currentRoundWinner != PvpRoundWinner.NONE) return;
        PvpRoundWinner mySlotWinner = (slot == 1) ? PvpRoundWinner.PLAYER1 : PvpRoundWinner.PLAYER2;
        if (provisionalWinner == mySlotWinner) return; // already found it - nothing to give up
        if (slot == 1) {
            currentRoundWinner = PvpRoundWinner.PLAYER2;
            score2++;
        } else {
            currentRoundWinner = PvpRoundWinner.PLAYER1;
            score1++;
        }
        finishRound();
    }

    private void finishRound() {
        lastRoundDurationSeconds = getRoundElapsedSeconds();
        roundHistory.add(new PvpRoundResult(roundNumber, currentRoundWinner, currentTarget.getName(), attempts1, attempts2));

        if (score1 >= pointsToWin || score2 >= pointsToWin) {
            matchOver = true;
        }

        ready1 = false;
        ready2 = false;
        roundOverAtMillis = System.currentTimeMillis();
    }

    // -------------------- Ready-up (between rounds) --------------------

    /**
     * Marks the given player as ready to start the next round. Only takes effect while
     * a round has actually just ended (does nothing before that, or once the match is
     * over) - doesn't advance the round itself, {@link #advanceToNextRound()} still owns
     * that decision (both ready, or the grace window has elapsed).
     */
    public synchronized boolean markReady(int slot) {
        if (matchOver || currentRoundWinner == PvpRoundWinner.NONE) return false;
        if (slot == 1) ready1 = true; else ready2 = true;
        return true;
    }

    public boolean isReady(int slot) { return slot == 1 ? ready1 : ready2; }

    private boolean readyDeadlinePassed() {
        return System.currentTimeMillis() - roundOverAtMillis >= READY_MAX_MILLIS;
    }

    /** Milliseconds left in the ready-up grace window, 0 once it's expired or no round has ended yet. */
    public long getReadyRemainingMillis() {
        if (currentRoundWinner == PvpRoundWinner.NONE) return 0;
        return Math.max(0, READY_MAX_MILLIS - (System.currentTimeMillis() - roundOverAtMillis));
    }

    // -------------------- Hints --------------------

    /**
     * Reveals one more letter of the target country's name to the given player only, at a
     * random position. If the OTHER player is already provisionally winning (their grace
     * period is running against me), this hint also shaves {@link #GRACE_SECONDS_PER_HINT}
     * seconds off my own remaining time - a hint always costs 20 seconds of your current
     * runway, whether that's the head start you're granting or the time you have left to
     * catch up.
     */
    public synchronized String useHint(int slot) {
        refreshGraceTimeout();
        if (matchOver || paused || currentRoundWinner != PvpRoundWinner.NONE) return getHintMaskedName(slot);

        PvpRoundWinner mySlotWinner = (slot == 1) ? PvpRoundWinner.PLAYER1 : PvpRoundWinner.PLAYER2;
        if (provisionalWinner == mySlotWinner) return getHintMaskedName(slot); // already found it

        Set<Integer> myPositions = (slot == 1) ? revealedLetterPositions1 : revealedLetterPositions2;
        int totalLetters = CountryNameHintUtil.countRevealableLetters(currentTarget.getName());
        if (myPositions.size() < totalLetters) {
            CountryNameHintUtil.revealRandomLetter(currentTarget.getName(), myPositions);
            if (slot == 1) hints1UsedThisRound++; else hints2UsedThisRound++;

            PvpRoundWinner opponentWinner = (slot == 1) ? PvpRoundWinner.PLAYER2 : PvpRoundWinner.PLAYER1;
            if (provisionalWinner == opponentWinner) {
                graceDeadlineElapsedSeconds -= GRACE_SECONDS_PER_HINT;
                refreshGraceTimeout(); // may finalize the opponent's win right now if that emptied my runway
            }
        }

        return getHintMaskedName(slot);
    }

    public String getHintMaskedName(int slot) {
        if (currentTarget == null) return "";
        Set<Integer> positions = (slot == 1) ? revealedLetterPositions1 : revealedLetterPositions2;
        return CountryNameHintUtil.maskName(currentTarget.getName(), positions);
    }

    public int getHintsUsedThisRound(int slot) { return slot == 1 ? hints1UsedThisRound : hints2UsedThisRound; }

    /** How long the just-finished round took, frozen at the moment it ended - 0 before any round has ended. */
    public double getLastRoundDurationSeconds() { return lastRoundDurationSeconds; }

    // -------------------- Grace period (provisional win) --------------------

    /** NONE unless someone has found the country but is still in the post-win grace period. */
    public PvpRoundWinner getProvisionalWinner() { return provisionalWinner; }

    /** Seconds left in the grace period, 0 if there isn't one active. */
    public double getGraceRemainingSeconds() {
        if (provisionalWinner == PvpRoundWinner.NONE) return 0;
        return Math.max(0, graceDeadlineElapsedSeconds - getRoundElapsedSeconds());
    }

    /** Finalizes the provisional winner's round once the grace deadline passes unchallenged. */
    public synchronized void refreshGraceTimeout() {
        if (matchOver || paused) return;
        if (provisionalWinner == PvpRoundWinner.NONE) return;
        if (getRoundElapsedSeconds() >= graceDeadlineElapsedSeconds) {
            currentRoundWinner = provisionalWinner;
            if (provisionalWinner == PvpRoundWinner.PLAYER1) score1++; else score2++;
            finishRound();
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

    public double getRoundElapsedSeconds() {
        long effectiveNow = paused ? pauseStartedAtMillis : System.currentTimeMillis();
        return (effectiveNow - roundStartTimeMillis - pausedMillisThisRound) / 1000.0;
    }

    public double getMatchElapsedSeconds() {
        long effectiveNow = paused ? pauseStartedAtMillis : System.currentTimeMillis();
        return (effectiveNow - matchStartTimeMillis - pausedMillisTotalMatch) / 1000.0;
    }

    // -------------------- Getters --------------------

    public int getScore(int slot) { return slot == 1 ? score1 : score2; }
    public int getAttempts(int slot) { return slot == 1 ? attempts1 : attempts2; }

    /** The best (closest) proximity band the given player has reached so far this round, or null if they haven't guessed yet. */
    public ProximityLevel getProgressLevel(int slot) {
        int ordinal = slot == 1 ? bestOrdinal1 : bestOrdinal2;
        return ordinal == NONE_ORDINAL ? null : ProximityLevel.values()[ordinal];
    }

    public int getRoundNumber() { return roundNumber; }
    public int getPointsToWin() { return pointsToWin; }
    public int getBestOf() { return pointsToWin * 2 - 1; }
    public boolean isMatchOver() { return matchOver; }
    public boolean isRoundOver() { return currentRoundWinner != PvpRoundWinner.NONE; }
    public PvpRoundWinner getCurrentRoundWinner() { return currentRoundWinner; }
    public PvpRoundWinner getMatchWinner() {
        if (!matchOver) return PvpRoundWinner.NONE;
        return score1 > score2 ? PvpRoundWinner.PLAYER1 : PvpRoundWinner.PLAYER2;
    }

    public CountryBL getCurrentTarget() { return currentTarget; }
    public List<PvpRoundResult> getRoundHistory() { return roundHistory; }
}
