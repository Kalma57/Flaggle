package com.example.flagdemo.BusinessLayer.MatchBL;

/**
 * How fast/accurate the AI opponent is in a 1v1 Match.
 *
 * This is intentionally a separate concept from {@link com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel},
 * which only controls how much visual information the human player receives about their
 * own flag guesses. AiSkillLevel only controls the AI opponent's simulated speed/accuracy.
 */
public enum AiSkillLevel {
    EASY,
    MEDIUM,
    HARD
}
