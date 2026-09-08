package com.example.flagdemo.BusinessLayer.MatchBL;

/**
 * Who won a given round of a real 1v1 "vs Friend" PvP Match. NONE means the round
 * is still in progress. Unlike {@link RoundWinner} (which is HUMAN/AI, for the
 * vs-Computer mode), both PLAYER1 and PLAYER2 are real human players here.
 *
 * DRAW is only meaningful for formats where a tie is actually possible (the Blitz
 * time-attack mode) - the "Best of N" format always resolves to PLAYER1/PLAYER2.
 */
public enum PvpRoundWinner {
    NONE,
    PLAYER1,
    PLAYER2,
    DRAW
}
