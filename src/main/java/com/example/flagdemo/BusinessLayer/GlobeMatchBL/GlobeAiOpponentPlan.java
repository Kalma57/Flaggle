package com.example.flagdemo.BusinessLayer.GlobeMatchBL;

import com.example.flagdemo.BusinessLayer.GlobeBL.ProximityLevel;

import java.io.Serializable;
import java.util.List;

/**
 * A precomputed, deterministic timeline for the AI opponent's guesses during a single
 * Globe round — mirrors {@link com.example.flagdemo.BusinessLayer.MatchBL.AiOpponentPlan}
 * exactly, just carrying a {@link ProximityLevel} reached at each attempt instead of a
 * flag-match percentage, since Globe's "how close was that guess" concept is proximity
 * banding rather than a pixel-match percent.
 *
 * Whenever the server needs to know what the AI's status is "right now" (e.g. answering a
 * status poll), it just compares the elapsed time against this plan — no threads, no state
 * machine, no live guessing logic needed.
 */
public class GlobeAiOpponentPlan implements Serializable {

    private final List<Double> attemptTimestamps;
    private final List<ProximityLevel> displayedProximity;

    public GlobeAiOpponentPlan(List<Double> attemptTimestamps, List<ProximityLevel> displayedProximity) {
        this.attemptTimestamps = attemptTimestamps;
        this.displayedProximity = displayedProximity;
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
     * The best (closest) {@link ProximityLevel} the AI has reached so far, based on how
     * many of its planned attempts have already landed by this point in time — or null if
     * it hasn't made a single attempt yet.
     */
    public ProximityLevel getProgressLevel(double elapsedSeconds) {
        int lastReachedIndex = -1;
        for (int i = 0; i < attemptTimestamps.size(); i++) {
            if (attemptTimestamps.get(i) <= elapsedSeconds) {
                lastReachedIndex = i;
            } else {
                break;
            }
        }
        if (lastReachedIndex < 0) return null;
        return displayedProximity.get(lastReachedIndex);
    }
}
