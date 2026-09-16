package com.example.flagdemo.BusinessLayer.CapitalBL;

/**
 * A record of one finished capital-quiz round, kept for the match recap screen. Both sides
 * always answer every round now (race-to-N-net-correct, see {@link CapitalQuizEngineBL}), so
 * unlike the old single {@code winner} field, a round records each side's own correctness
 * independently - a round can be a win for both, a loss for both, or a split.
 */
public class CapitalRoundResult implements java.io.Serializable {

    private final int roundNumber;
    private final String promptText;
    private final String correctAnswerText;
    private final String humanPickedText;
    private final boolean humanCorrect;
    private final boolean aiCorrect;
    private final String targetFlagPath;
    private final double humanTimeSeconds;
    private final double aiTimeSeconds;

    public CapitalRoundResult(int roundNumber, String promptText, String correctAnswerText, String humanPickedText,
                               boolean humanCorrect, boolean aiCorrect, String targetFlagPath,
                               double humanTimeSeconds, double aiTimeSeconds) {
        this.roundNumber = roundNumber;
        this.promptText = promptText;
        this.correctAnswerText = correctAnswerText;
        this.humanPickedText = humanPickedText;
        this.humanCorrect = humanCorrect;
        this.aiCorrect = aiCorrect;
        this.targetFlagPath = targetFlagPath;
        this.humanTimeSeconds = humanTimeSeconds;
        this.aiTimeSeconds = aiTimeSeconds;
    }

    public int getRoundNumber() { return roundNumber; }
    public String getPromptText() { return promptText; }
    public String getCorrectAnswerText() { return correctAnswerText; }
    public String getHumanPickedText() { return humanPickedText; }
    public boolean isHumanCorrect() { return humanCorrect; }
    public boolean isAiCorrect() { return aiCorrect; }
    public String getTargetFlagPath() { return targetFlagPath; }
    public double getHumanTimeSeconds() { return humanTimeSeconds; }
    public double getAiTimeSeconds() { return aiTimeSeconds; }
}
