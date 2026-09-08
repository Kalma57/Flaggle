package com.example.flagdemo.BusinessLayer.MatchBL;

/**
 * Which real 1v1 "vs Friend" format a given {@link PvpMatchRoom} hosts. A room is
 * created for exactly one mode and keeps whichever engine matches it (never both).
 */
public enum PvpGameMode {
    /** Same flag every round, first to guess it right wins the round (3/5/7 rounds). */
    BEST_OF_N,
    /** Shared shuffled flag queue, fixed running clock (1 or 2 minutes), most correct flags wins. */
    BLITZ
}
