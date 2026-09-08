package com.example.flagdemo.BusinessLayer.MatchBL;

import java.io.Serializable;
import java.util.List;

/**
 * A precomputed, deterministic timeline for the AI opponent's guesses during a single round.
 *
 * Rather than running the AI on a background thread or in a real agent loop, the whole
 * round is "decided" up front the moment it starts: a sorted list of timestamps (seconds
 * since the round began) at which the AI makes an attempt, with the very last timestamp
 * being the moment it guesses correctly. Paired with each timestamp is the real "% of the
 * flag matched" the AI has achieved once that attempt lands — computed up front using the
 * exact same pixel-comparison engine ({@code GuessResultBL}) that scores the human player's
 * own guesses, just against real (but never revealed) candidate countries.
 *
 * Whenever the server needs to know what the AI's status is "right now" (e.g. answering a
 * status poll), it just compares the elapsed time against this plan — no threads, no state
 * machine, no live guessing logic needed.
 */
public class AiOpponentPlan implements Serializable {

    private final List<Double> attemptTimestamps;
    private final List<Integer> displayedPercents;

    public AiOpponentPlan(List<Double> attemptTimestamps, List<Integer> displayedPercents) {
        this.attemptTimestamps = attemptTimestamps;
        this.displayedPercents = displayedPercents;
    }

    /**
     * The moment (in seconds since round start) the AI's final, correct guess lands.
     */
    public double getSolveTimeSeconds() {
        return attemptTimestamps.get(attemptTimestamps.size() - 1);
    }

    /**
     * How many attempts the AI has made so far, given how much time has elapsed.
     */
    public int getAttemptsSoFar(double elapsedSeconds) {
        int count = 0;
        for (double t : attemptTimestamps) {
            if (t <= elapsedSeconds) count++;
        }
        return count;
    }

    /**
     * Whether the AI has already reached its correct guess by this point in time.
     */
    public boolean isSolved(double elapsedSeconds) {
        return elapsedSeconds >= getSolveTimeSeconds();
    }

    /**
     * The real "% of the flag matched" the AI has achieved so far, based on how many of
     * its planned attempts have already landed by this point in time. On HARD flag-reveal
     * mode this is the highest single-attempt percentage reached so far (nothing carries
     * over between guesses); on EASY it's the cumulative revealed coverage — either way,
     * the value is precomputed per-attempt in {@link AiOpponentPlanFactory}, so this just
     * looks up the right one.
     */
    public int getProgressPercent(double elapsedSeconds) {
        int lastReachedIndex = -1;
        for (int i = 0; i < attemptTimestamps.size(); i++) {
            if (attemptTimestamps.get(i) <= elapsedSeconds) {
                lastReachedIndex = i;
            } else {
                break;
            }
        }
        if (lastReachedIndex < 0) return 0;
        return displayedPercents.get(lastReachedIndex);
    }
}
