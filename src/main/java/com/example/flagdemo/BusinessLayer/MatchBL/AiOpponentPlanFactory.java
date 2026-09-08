package com.example.flagdemo.BusinessLayer.MatchBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Builds a randomized-but-bounded {@link AiOpponentPlan} for a round, based on the chosen
 * {@link AiSkillLevel}. No LLM/agent involved — just bounded randomness tuned per level,
 * combined with the real flag pixel-comparison engine so the AI's displayed "% matched"
 * always reflects an actual (never-shown) candidate guess rather than a made-up number.
 */
public class AiOpponentPlanFactory {

    private static final Random RANDOM = new Random();

    /**
     * @param level               how fast/sharp the AI opponent should be
     * @param cc                  used to draw random candidate countries for the AI to "try"
     * @param target              the round's real target country (never exposed to the human
     *                            until the round is actually over)
     * @param flagRevealDifficulty the human's flag-reveal mode for this match — reused here so
     *                            the AI's displayed percentage behaves the same way a human
     *                            guesser's would (EASY accumulates, HARD tracks a running best)
     */
    public static AiOpponentPlan createPlan(AiSkillLevel level, CountryController cc, CountryBL target, DifficultyLevel flagRevealDifficulty) {
        int minAttempts, maxAttempts, sampleSize;
        double minSeconds, maxSeconds;

        switch (level) {
            case EASY -> {
                minAttempts = 6; maxAttempts = 9;
                minSeconds = 45; maxSeconds = 70;
                sampleSize = 1; // picks a random country each time, no attempt to aim well
            }
            case HARD -> {
                minAttempts = 2; maxAttempts = 4;
                minSeconds = 8; maxSeconds = 18;
                sampleSize = 10; // samples several candidates and keeps the closest match
            }
            default -> { // MEDIUM
                minAttempts = 4; maxAttempts = 6;
                minSeconds = 22; maxSeconds = 38;
                sampleSize = 4;
            }
        }

        int attempts = minAttempts + RANDOM.nextInt(maxAttempts - minAttempts + 1);
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

        List<CountryBL> candidates = pickCandidates(cc.getAllCountries(), target, attempts - 1, sampleSize);
        List<Integer> percents = computeDisplayedPercents(candidates, target, flagRevealDifficulty);

        return new AiOpponentPlan(timestamps, percents);
    }

    /**
     * Picks {@code count} distinct non-target candidate countries for the AI to "guess"
     * before it finally lands on the real target. For each slot, {@code sampleSize} random
     * countries are considered and the one with the highest real match percentage against
     * the target is kept — higher sampleSize (tied to AI skill) means a sharper opponent.
     * The final list is sorted ascending by match quality, so the AI's progress feels like
     * it's gradually narrowing in rather than jumping around at random.
     */
    private static List<CountryBL> pickCandidates(List<CountryBL> pool, CountryBL target, int count, int sampleSize) {
        List<CountryBL> remaining = new ArrayList<>(pool);
        remaining.removeIf(c -> c.equals(target));

        List<ScoredCandidate> picked = new ArrayList<>();

        for (int slot = 0; slot < count && !remaining.isEmpty(); slot++) {
            List<CountryBL> shuffled = new ArrayList<>(remaining);
            Collections.shuffle(shuffled, RANDOM);

            int samples = Math.min(sampleSize, shuffled.size());
            CountryBL best = null;
            int bestPercent = -1;

            for (int s = 0; s < samples; s++) {
                CountryBL candidate = shuffled.get(s);
                int percent = hardMatchPercent(candidate, target);
                if (percent > bestPercent) {
                    bestPercent = percent;
                    best = candidate;
                }
            }

            remaining.remove(best);
            picked.add(new ScoredCandidate(best, bestPercent));
        }

        picked.sort(Comparator.comparingInt(ScoredCandidate::percent));

        List<CountryBL> result = new ArrayList<>();
        for (ScoredCandidate sc : picked) result.add(sc.country());
        return result;
    }

    /**
     * Turns the ordered candidate list into the "% matched" value that should be displayed
     * once each attempt lands, mirroring exactly how a human guesser's percentage would
     * behave: cumulative on EASY, running-best-so-far on HARD. The final entry (the AI's
     * winning guess, i.e. the target itself) is always 100.
     */
    private static List<Integer> computeDisplayedPercents(List<CountryBL> candidates, CountryBL target, DifficultyLevel flagRevealDifficulty) {
        List<Integer> percents = new ArrayList<>();

        if (flagRevealDifficulty == DifficultyLevel.EASY) {
            BufferedImage accumulated = null;
            for (CountryBL candidate : candidates) {
                GuessResultBL result = new GuessResultBL(candidate, target, DifficultyLevel.EASY, accumulated);
                accumulated = result.getFlagDifferences();
                percents.add(GuessResultBL.calculateMatchPercentage(accumulated, true));
            }
        } else {
            int runningMax = 0;
            for (CountryBL candidate : candidates) {
                GuessResultBL result = new GuessResultBL(candidate, target, DifficultyLevel.HARD);
                int percent = GuessResultBL.calculateMatchPercentage(result.getFlagDifferences(), false);
                runningMax = Math.max(runningMax, percent);
                percents.add(runningMax);
            }
        }

        percents.add(100); // final attempt: the AI guesses the real target correctly
        return percents;
    }

    private static int hardMatchPercent(CountryBL guessed, CountryBL target) {
        GuessResultBL result = new GuessResultBL(guessed, target, DifficultyLevel.HARD);
        return GuessResultBL.calculateMatchPercentage(result.getFlagDifferences(), false);
    }

    private record ScoredCandidate(CountryBL country, int percent) {}
}
