package com.example.flagdemo.ViewModel.MatchVM;

import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.BusinessLayer.MatchBL.PvpMatchEngineBL;
import com.example.flagdemo.BusinessLayer.MatchBL.PvpMatchRoom;

import java.sql.SQLException;

/**
 * ViewModel for one player's perspective on a live "vs Friend" PvP match room.
 *
 * Bridges the View (FlaggleMultiplayerController + Thymeleaf/JS) with the shared
 * {@link PvpMatchRoom}/{@link PvpMatchEngineBL} that both players' browsers act on
 * concurrently, the same way {@link FlaggleMatchViewModel} bridges the vs-Computer
 * controller to its engine - just parameterized by which of the two real players
 * ("slots") this particular request/browser belongs to.
 */
public class PvpMatchViewModel implements java.io.Serializable {

    private final PvpMatchRoom room;
    private final int mySlot;

    public PvpMatchViewModel(PvpMatchRoom room, int mySlot) {
        this.room = room;
        this.mySlot = mySlot;
    }

    public PvpMatchRoom getRoom() { return room; }
    public int getMySlot() { return mySlot; }
    public PvpMatchEngineBL getEngine() { return room.getEngine(); }

    public GuessResultBL submitGuess(String countryName) {
        return room.getEngine().submitGuess(mySlot, countryName);
    }

    public void giveUpRound() {
        room.getEngine().giveUpRound(mySlot);
    }

    public void advanceToNextRound() throws SQLException {
        room.getEngine().advanceToNextRound();
    }
}
