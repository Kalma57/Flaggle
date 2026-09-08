package com.example.flagdemo.BusinessLayer.MatchBL;

import java.io.Serializable;

/**
 * A record of one finished round in a real 1v1 "vs Friend" PvP Match - kept for the
 * match history / recap screen. Symmetric across both real players, unlike
 * {@link MatchRoundResult} which is human-vs-AI specific.
 */
public class PvpRoundResult implements Serializable {

    private final int roundNumber;
    private final PvpRoundWinner winner;
    private final String targetCountryName;
    private final int player1Attempts;
    private final int player2Attempts;

    public PvpRoundResult(int roundNumber, PvpRoundWinner winner, String targetCountryName, int player1Attempts, int player2Attempts) {
        this.roundNumber = roundNumber;
        this.winner = winner;
        this.targetCountryName = targetCountryName;
        this.player1Attempts = player1Attempts;
        this.player2Attempts = player2Attempts;
    }

    public int getRoundNumber() { return roundNumber; }
    public PvpRoundWinner getWinner() { return winner; }
    public String getTargetCountryName() { return targetCountryName; }
    public int getPlayer1Attempts() { return player1Attempts; }
    public int getPlayer2Attempts() { return player2Attempts; }
}
