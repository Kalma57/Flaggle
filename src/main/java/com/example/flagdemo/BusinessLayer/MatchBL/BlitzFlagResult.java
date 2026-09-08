package com.example.flagdemo.BusinessLayer.MatchBL;

import java.io.Serializable;

/**
 * A record of one flag the HUMAN player personally passed through during a Blitz
 * (1-minute time-attack) match — kept for the match recap screen.
 *
 * Only the human's own journey is tracked here, on purpose: exactly like the "Best of N"
 * format, the opponent's real flags/guesses are never exposed to the human, only a live
 * percentage/count (see {@link AiOpponentPlan}). So the recap can only ever show what the
 * human themself saw.
 */
public class BlitzFlagResult implements Serializable {

    private final int order;
    private final String countryName;
    private final boolean solved;
    private final int attempts;
    /** How many seconds it took to solve this flag (only meaningful when solved == true). */
    private final double timeTakenSeconds;

    public BlitzFlagResult(int order, String countryName, boolean solved, int attempts, double timeTakenSeconds) {
        this.order = order;
        this.countryName = countryName;
        this.solved = solved;
        this.attempts = attempts;
        this.timeTakenSeconds = timeTakenSeconds;
    }

    public int getOrder() { return order; }
    public String getCountryName() { return countryName; }
    /** True if the flag was guessed correctly; false if it was given up on instead. */
    public boolean isSolved() { return solved; }
    public int getAttempts() { return attempts; }
    public double getTimeTakenSeconds() { return timeTakenSeconds; }
}
