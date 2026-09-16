package com.example.flagdemo.BusinessLayer.CapitalBL;

import java.io.Serializable;

/**
 * A record of one question a side (human or AI) personally answered during a capital-quiz
 * Blitz match - kept for the match recap screen. Unlike {@link CapitalRoundResult} (Best of N,
 * one shared result per round), Blitz has each side racing through the queue at its own pace,
 * so human and AI each keep their own independent history list.
 */
public class CapitalBlitzQuestionResult implements Serializable {

    private final int order;
    private final String promptText;
    private final String correctAnswerText;
    private final boolean correct;
    private final double timeTakenSeconds;
    private final String targetFlagPath;

    public CapitalBlitzQuestionResult(int order, String promptText, String correctAnswerText, boolean correct,
                                       double timeTakenSeconds, String targetFlagPath) {
        this.order = order;
        this.promptText = promptText;
        this.correctAnswerText = correctAnswerText;
        this.correct = correct;
        this.timeTakenSeconds = timeTakenSeconds;
        this.targetFlagPath = targetFlagPath;
    }

    public int getOrder() { return order; }
    public String getPromptText() { return promptText; }
    public String getCorrectAnswerText() { return correctAnswerText; }
    public boolean isCorrect() { return correct; }
    public double getTimeTakenSeconds() { return timeTakenSeconds; }
    public String getTargetFlagPath() { return targetFlagPath; }
}
