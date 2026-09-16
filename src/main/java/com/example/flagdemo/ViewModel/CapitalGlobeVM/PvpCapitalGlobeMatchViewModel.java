package com.example.flagdemo.ViewModel.CapitalGlobeVM;

import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobePvpMatchRoom;
import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.PvpCapitalGlobeMatchEngineBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;

/**
 * ViewModel for one player's perspective on a live capital-location globe "vs Friend" PvP
 * match room. Bridges the View (CapitalGlobeMultiplayerController + Thymeleaf/JS) with the
 * shared {@link CapitalGlobePvpMatchRoom}/{@link PvpCapitalGlobeMatchEngineBL} that both
 * players' browsers act on concurrently - mirrors
 * {@link com.example.flagdemo.ViewModel.GlobeMatchVM.PvpGlobeMatchViewModel} exactly, just
 * parameterized by which of the two real players ("slots") this particular request/browser
 * belongs to, and with no hint pass-through since this game has none. Constructed fresh on
 * every request (not stored in session) - there is deliberately no Model or Service layer in
 * between, same as the Globe PvP path this mirrors.
 */
public class PvpCapitalGlobeMatchViewModel implements java.io.Serializable {

    private final CapitalGlobePvpMatchRoom room;
    private final int mySlot;

    public PvpCapitalGlobeMatchViewModel(CapitalGlobePvpMatchRoom room, int mySlot) {
        this.room = room;
        this.mySlot = mySlot;
    }

    public CapitalGlobePvpMatchRoom getRoom() { return room; }
    public int getMySlot() { return mySlot; }
    public PvpCapitalGlobeMatchEngineBL getEngine() { return room.getEngine(); }

    public GuessResultGlobeBL submitGuess(String countryName) {
        return room.getEngine().submitGuess(mySlot, countryName);
    }

    public void giveUpRound() {
        room.getEngine().giveUpRound(mySlot);
    }

    public void advanceToNextRound() {
        room.getEngine().advanceToNextRound();
    }

    /** Marks this player ready for the next round - see {@link PvpCapitalGlobeMatchEngineBL#markReady(int)}. */
    public void markReady() {
        room.getEngine().markReady(mySlot);
    }
}
