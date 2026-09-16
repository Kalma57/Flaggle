package com.example.flagdemo.BusinessLayer.CapitalGlobeBL;

import com.example.flagdemo.BusinessLayer.MatchBL.PvpRoundWinner;

/**
 * A record of one finished round of the capital-location globe game's real 1v1 "vs Friend"
 * Match format, kept for the recap screen. Symmetric across both real players
 * (player1Attempts/player2Attempts), unlike {@link CapitalGlobeRoundResult} which is
 * human-vs-AI specific - mirrors {@link com.example.flagdemo.BusinessLayer.MatchBL.PvpRoundResult}'s
 * symmetric shape, plus the capital/flag context and per-round timing this game's other
 * recap screens already show.
 */
public class PvpCapitalGlobeRoundResult implements java.io.Serializable {

    private final int roundNumber;
    private final String targetCountryName;
    private final String targetCapital;
    private final String targetFlagPath;
    private final PvpRoundWinner winner;
    private final int player1Attempts;
    private final int player2Attempts;
    private final double roundDurationSeconds;

    public PvpCapitalGlobeRoundResult(int roundNumber, String targetCountryName, String targetCapital, String targetFlagPath,
                                       PvpRoundWinner winner, int player1Attempts, int player2Attempts, double roundDurationSeconds) {
        this.roundNumber = roundNumber;
        this.targetCountryName = targetCountryName;
        this.targetCapital = targetCapital;
        this.targetFlagPath = targetFlagPath;
        this.winner = winner;
        this.player1Attempts = player1Attempts;
        this.player2Attempts = player2Attempts;
        this.roundDurationSeconds = roundDurationSeconds;
    }

    public int getRoundNumber() { return roundNumber; }
    public String getTargetCountryName() { return targetCountryName; }
    public String getTargetCapital() { return targetCapital; }
    public String getTargetFlagPath() { return targetFlagPath; }
    public PvpRoundWinner getWinner() { return winner; }
    public int getPlayer1Attempts() { return player1Attempts; }
    public int getPlayer2Attempts() { return player2Attempts; }
    public double getRoundDurationSeconds() { return roundDurationSeconds; }
}
