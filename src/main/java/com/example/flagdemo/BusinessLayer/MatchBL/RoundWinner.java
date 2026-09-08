package com.example.flagdemo.BusinessLayer.MatchBL;

/**
 * Who won a given round of a 1v1 Match. NONE means the round is still in progress.
 * DRAW is only used by formats where a tie is actually possible (e.g. the Blitz
 * time-attack mode) — the classic "Best of N" format always resolves to HUMAN/AI.
 */
public enum RoundWinner {
    NONE,
    HUMAN,
    AI,
    DRAW
}
