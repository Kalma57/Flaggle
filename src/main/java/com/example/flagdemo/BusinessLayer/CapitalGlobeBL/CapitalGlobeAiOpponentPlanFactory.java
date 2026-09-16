package com.example.flagdemo.BusinessLayer.CapitalGlobeBL;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalAiOpponentPlan;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;

import java.util.Random;

/**
 * Builds the AI's plan for one capital-location globe round, tiered by {@link AiSkillLevel} -
 * reuses the same {@link CapitalAiOpponentPlan} data holder as
 * {@link com.example.flagdemo.BusinessLayer.CapitalBL.CapitalAiOpponentPlanFactory} (a plain,
 * stateless "when + right-or-wrong" fact, safe to share at that low a layer), but kept as its
 * own factory with its own tuning: finding a country on a rotatable globe is a slower,
 * fundamentally different task than picking one of four multiple-choice options, so this
 * game's own pacing shouldn't be coupled to - or accidentally retuned by - changes meant for
 * the quiz formats, and vice versa.
 */
public class CapitalGlobeAiOpponentPlanFactory {

    private static final Random RANDOM = new Random();

    public static CapitalAiOpponentPlan createPlan(AiSkillLevel level) {
        double minSeconds, maxSeconds, correctProbability;

        switch (level) {
            case EASY -> {
                minSeconds = 5; maxSeconds = 9;
                correctProbability = 0.20;
            }
            case HARD -> {
                // Tuned for ~7-8 answers/minute (average ~8s/answer) - finding a country on the
                // globe takes real looking, so this needed to be noticeably slower than the
                // quiz factory's HARD pace, not just a fast multiple-choice click.
                minSeconds = 6; maxSeconds = 10;
                correctProbability = 0.85;
            }
            default -> { // MEDIUM
                minSeconds = 3; maxSeconds = 6;
                correctProbability = 0.50;
            }
        }

        double answerTime = minSeconds + RANDOM.nextDouble() * (maxSeconds - minSeconds);
        boolean correct = RANDOM.nextDouble() < correctProbability;
        return new CapitalAiOpponentPlan(answerTime, correct);
    }
}
