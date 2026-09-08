package com.example.flagdemo.BusinessLayer.MatchBL;

/**
 * The lifecycle state of a {@link PvpMatchRoom}.
 */
public enum PvpRoomStatus {
    /** Player 1 has created the room; waiting for a second real player to join. */
    WAITING_FOR_OPPONENT,
    /** Both players are present and the match engine is running. */
    IN_PROGRESS,
    /** The match has been won by one of the two players. */
    FINISHED
}
