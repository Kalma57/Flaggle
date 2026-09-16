package com.example.flagdemo.BusinessLayer.CapitalBL;

/**
 * A record of one finished round of the capital-quiz's real 1v1 "vs Friend" Match format, kept
 * for the recap screen. Symmetric across both real players (each side's own pick/correctness),
 * unlike {@link CapitalRoundResult} which is human-vs-AI specific.
 */
public class PvpCapitalRoundResult implements java.io.Serializable {

    private final int roundNumber;
    private final String promptText;
    private final String correctAnswerText;
    private final String targetFlagPath;
    private final String player1PickedText;
    private final String player2PickedText;
    private final boolean player1Correct;
    private final boolean player2Correct;

    public PvpCapitalRoundResult(int roundNumber, String promptText, String correctAnswerText, String targetFlagPath,
                                  String player1PickedText, String player2PickedText,
                                  boolean player1Correct, boolean player2Correct) {
        this.roundNumber = roundNumber;
        this.promptText = promptText;
        this.correctAnswerText = correctAnswerText;
        this.targetFlagPath = targetFlagPath;
        this.player1PickedText = player1PickedText;
        this.player2PickedText = player2PickedText;
        this.player1Correct = player1Correct;
        this.player2Correct = player2Correct;
    }

    public int getRoundNumber() { return roundNumber; }
    public String getPromptText() { return promptText; }
    public String getCorrectAnswerText() { return correctAnswerText; }
    public String getTargetFlagPath() { return targetFlagPath; }
    public String getPlayer1PickedText() { return player1PickedText; }
    public String getPlayer2PickedText() { return player2PickedText; }
    public boolean isPlayer1Correct() { return player1Correct; }
    public boolean isPlayer2Correct() { return player2Correct; }
}
