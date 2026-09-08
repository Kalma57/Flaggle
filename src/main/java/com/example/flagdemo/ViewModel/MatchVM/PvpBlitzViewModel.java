package com.example.flagdemo.ViewModel.MatchVM;

import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.BusinessLayer.MatchBL.PvpBlitzEngineBL;
import com.example.flagdemo.BusinessLayer.MatchBL.PvpMatchRoom;

/**
 * ViewModel for one player's perspective on a live "vs Friend" PvP Blitz room.
 *
 * Bridges the View (FlaggleMultiplayerController + Thymeleaf/JS) with the shared
 * {@link PvpMatchRoom}/{@link PvpBlitzEngineBL} that both players' browsers act on
 * concurrently - mirrors {@link PvpMatchViewModel}, just parameterized by which of the
 * two real players ("slots") this particular request/browser belongs to.
 */
public class PvpBlitzViewModel implements java.io.Serializable {

    private final PvpMatchRoom room;
    private final int mySlot;

    public PvpBlitzViewModel(PvpMatchRoom room, int mySlot) {
        this.room = room;
        this.mySlot = mySlot;
    }

    public PvpMatchRoom getRoom() { return room; }
    public int getMySlot() { return mySlot; }
    public PvpBlitzEngineBL getEngine() { return room.getBlitzEngine(); }

    public GuessResultBL submitGuess(String countryName) {
        return room.getBlitzEngine().submitGuess(mySlot, countryName);
    }

    public String giveUpFlag() {
        return room.getBlitzEngine().giveUpFlag(mySlot);
    }
}
