package com.example.flagdemo.BusinessLayer.CapitalBL;

import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;

import java.util.Random;

/**
 * Builds the AI's plan for one quiz round, tiered by {@link AiSkillLevel} - reuses the same
 * skill-level enum as Flaggle/Globe's match modes rather than inventing a parallel one.
 *
 * A harder AI answers both faster and more accurately - a quiz bot doesn't need the
 * candidate-sampling machinery the flag/globe factories use (there's no "closer guess" to
 * simulate), so this is intentionally the simplest of the three AI-plan factories in the app.
 */
public class CapitalAiOpponentPlanFactory {

    private static final Random RANDOM = new Random();

    public static CapitalAiOpponentPlan createPlan(AiSkillLevel level) {
        double minSeconds, maxSeconds, correctProbability;

        switch (level) {
            case EASY -> {
                minSeconds = 5; maxSeconds = 9;
                correctProbability = 0.20;
            }
            case HARD -> {
                minSeconds = 1.5; maxSeconds = 3.5;
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
