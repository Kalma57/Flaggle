package com.example.flagdemo.BusinessLayer.CapitalBL;

/**
 * Which direction a capital-city round quizzes in - both directions share the exact same
 * engine/screen, just swapping what's shown as the prompt vs. the four answer options.
 */
public enum CapitalQuizMode {
    /** Prompt: flag + country name. Options: four capital cities, one correct. */
    FLAG_TO_CAPITAL,
    /** Prompt: a capital city name. Options: four countries, one correct. */
    CAPITAL_TO_COUNTRY
}
