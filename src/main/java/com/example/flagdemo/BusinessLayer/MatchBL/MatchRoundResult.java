package com.example.flagdemo.BusinessLayer.MatchBL;

import java.io.Serializable;

/**
 * A record of one finished round in a 1v1 Match — kept for the match history / recap screen.
 */
public class MatchRoundResult implements Serializable {

    private final int roundNumber;
    private final RoundWinner winner;
    private final String targetCountryName;
    private final int humanAttempts;
    private final int aiAttempts;

    public MatchRoundResult(int roundNumber, RoundWinner winner, String targetCountryName, int humanAttempts, int aiAttempts) {
        this.roundNumber = roundNumber;
        this.winner = winner;
        this.targetCountryName = targetCountryName;
        this.humanAttempts = humanAttempts;
        this.aiAttempts = aiAttempts;
    }

    public int getRoundNumber() { return roundNumber; }
    public RoundWinner getWinner() { return winner; }
    public String getTargetCountryName() { return targetCountryName; }
    public int getHumanAttempts() { return humanAttempts; }
    public int getAiAttempts() { return aiAttempts; }
}
