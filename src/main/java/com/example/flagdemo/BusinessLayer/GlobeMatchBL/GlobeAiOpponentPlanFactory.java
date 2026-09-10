package com.example.flagdemo.BusinessLayer.GlobeMatchBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.ProximityLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Builds a randomized-but-bounded {@link GlobeAiOpponentPlan} for a round, based on the
 * chosen {@link AiSkillLevel} — mirrors
 * {@link com.example.flagdemo.BusinessLayer.MatchBL.AiOpponentPlanFactory} exactly, just
 * scoring candidate countries by real {@link ProximityLevel}/distance (via
 * {@link GuessResultGlobeBL}) instead of flag pixel-match percentage.
 */
public class GlobeAiOpponentPlanFactory {

    private static final Random RANDOM = new Random();

    /**
     * @param level  how fast/sharp the AI opponent should be
     * @param cc     used to draw random candidate countries for the AI to "try"
     * @param target the round's real target country (never exposed to the human until
     *               the round is actually over)
     */
    public static GlobeAiOpponentPlan createPlan(AiSkillLevel level, CountryController cc, CountryBL target) {
        int minAttempts, maxAttempts, sampleSize;
        double minSeconds, maxSeconds;

        switch (level) {
            case EASY -> {
                minAttempts = 15; maxAttempts = 20;
                minSeconds = 54; maxSeconds = 84;
                sampleSize = 1; // picks a random country each time, no attempt to aim well
            }
            case HARD -> {
                minAttempts = 5; maxAttempts = 7;
                minSeconds = 16; maxSeconds = 35;
                sampleSize = 10; // samples several candidates and keeps the closest match
            }
            default -> { // MEDIUM
                minAttempts = 8; maxAttempts = 13;
                minSeconds = 36; maxSeconds = 60;
                sampleSize = 4;
            }
        }

        int attempts = sampleAttempts(minAttempts, maxAttempts);
        double solveTime = minSeconds + RANDOM.nextDouble() * (maxSeconds - minSeconds);

        List<Double> timestamps = new ArrayList<>();
        for (int i = 1; i < attempts; i++) {
            double base = solveTime * i / attempts;
            double slotWidth = solveTime / attempts;
            double jitter = (RANDOM.nextDouble() - 0.5) * slotWidth * 0.6;
            double t = Math.max(0.5, Math.min(solveTime - 0.5, base + jitter));
            timestamps.add(t);
        }
        Collections.sort(timestamps);
        timestamps.add(solveTime); // final, correct attempt

        List<CountryBL> pool = validPool(cc, target);
        List<CountryBL> candidates = pickCandidates(pool, target, attempts - 1, sampleSize);
        List<ProximityLevel> proximities = computeDisplayedProximity(candidates, target);

        return new GlobeAiOpponentPlan(timestamps, proximities);
    }

    /**
     * Draws the AI's attempt count for the round. {@code [minAttempts, maxAttempts]} isn't a
     * hard cutoff - it's the "typical" band, covering roughly the middle 85% of outcomes
     * around its midpoint. A normal distribution naturally allows rare rounds where the AI is
     * a bit faster or slower than usual, instead of every round feeling equally paced -
     * clamped to a wider absolute floor/ceiling so an unlucky draw never goes to something
     * silly (e.g. 1 attempt, or 40).
     */
    private static int sampleAttempts(int minAttempts, int maxAttempts) {
        double mean = (minAttempts + maxAttempts) / 2.0;
        double stdDev = (maxAttempts - minAttempts) / 3.0;
        int rangeWidth = maxAttempts - minAttempts;

        int lowClamp = Math.max(3, minAttempts - rangeWidth);
        int highClamp = maxAttempts + rangeWidth;

        long draw = Math.round(mean + RANDOM.nextGaussian() * stdDev);
        return (int) Math.max(lowClamp, Math.min(highClamp, draw));
    }

    /**
     * Countries with valid (non-zero) coordinates, minus the target itself — the same pool
     * a real target/candidate has to come from (see {@code GlobeEngineBL.isValidForGlobe}).
     */
    private static List<CountryBL> validPool(CountryController cc, CountryBL target) {
        List<CountryBL> pool = new ArrayList<>();
        for (CountryBL c : cc.getAllCountries()) {
            if (c.equals(target)) continue;
            if (c.getLatitude() == 0.0 && c.getLongitude() == 0.0) continue;
            pool.add(c);
        }
        return pool;
    }

    /**
     * Picks {@code count} distinct candidate countries for the AI to "guess" before it
     * finally lands on the real target. For each slot, {@code sampleSize} random countries
     * are considered and the closest one (by real proximity band, then raw distance) is
     * kept — higher sampleSize (tied to AI skill) means a sharper opponent. The final list
     * is ordered worst-to-best so the AI's progress feels like it's gradually narrowing in
     * rather than jumping around at random.
     */
    private static List<CountryBL> pickCandidates(List<CountryBL> pool, CountryBL target, int count, int sampleSize) {
        List<CountryBL> remaining = new ArrayList<>(pool);
        List<ScoredCandidate> picked = new ArrayList<>();

        for (int slot = 0; slot < count && !remaining.isEmpty(); slot++) {
            List<CountryBL> shuffled = new ArrayList<>(remaining);
            Collections.shuffle(shuffled, RANDOM);

            int samples = Math.min(sampleSize, shuffled.size());
            CountryBL best = null;
            GuessResultGlobeBL bestResult = null;

            for (int s = 0; s < samples; s++) {
                CountryBL candidate = shuffled.get(s);
                GuessResultGlobeBL result = new GuessResultGlobeBL(candidate, target);
                if (bestResult == null || isCloser(result, bestResult)) {
                    bestResult = result;
                    best = candidate;
                }
            }

            remaining.remove(best);
            picked.add(new ScoredCandidate(best, bestResult.getProximityLevel(), bestResult.getDistance()));
        }

        // Worst (highest ordinal / farthest) first, best (lowest ordinal / closest) last.
        picked.sort(Comparator.comparingInt((ScoredCandidate sc) -> sc.level().ordinal())
                .reversed()
                .thenComparing(ScoredCandidate::distance, Comparator.reverseOrder()));

        List<CountryBL> result = new ArrayList<>();
        for (ScoredCandidate sc : picked) result.add(sc.country());
        return result;
    }

    private static boolean isCloser(GuessResultGlobeBL a, GuessResultGlobeBL b) {
        int cmp = Integer.compare(a.getProximityLevel().ordinal(), b.getProximityLevel().ordinal());
        if (cmp != 0) return cmp < 0;
        return a.getDistance() < b.getDistance();
    }

    /**
     * Turns the ordered candidate list into the {@link ProximityLevel} that should be
     * displayed once each attempt lands — a running best-so-far, exactly like a real
     * player's opponent-progress badge. The final entry (the AI's winning guess, i.e. the
     * target itself) is always {@link ProximityLevel#CORRECT}.
     */
    private static List<ProximityLevel> computeDisplayedProximity(List<CountryBL> candidates, CountryBL target) {
        List<ProximityLevel> levels = new ArrayList<>();
        ProximityLevel runningBest = null;

        for (CountryBL candidate : candidates) {
            GuessResultGlobeBL result = new GuessResultGlobeBL(candidate, target);
            ProximityLevel level = result.getProximityLevel();
            if (runningBest == null || level.ordinal() < runningBest.ordinal()) {
                runningBest = level;
            }
            levels.add(runningBest);
        }

        levels.add(ProximityLevel.CORRECT); // final attempt: the AI guesses the real target correctly
        return levels;
    }

    private record ScoredCandidate(CountryBL country, ProximityLevel level, float distance) {}
}
