package com.example.flagdemo.ViewModel.GlobeMatchVM;

import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobePvpMatchRoom;
import com.example.flagdemo.BusinessLayer.GlobeMatchBL.PvpGlobeBlitzEngineBL;

/**
 * ViewModel for one player's perspective on a live "vs Friend" Globe PvP Blitz room.
 *
 * Bridges the View (GlobeMultiplayerController + Thymeleaf/JS) with the shared
 * {@link GlobePvpMatchRoom}/{@link PvpGlobeBlitzEngineBL} that both players' browsers act
 * on concurrently - mirrors {@link PvpGlobeMatchViewModel}, just parameterized by which of
 * the two real players ("slots") this particular request/browser belongs to.
 */
public class PvpGlobeBlitzViewModel implements java.io.Serializable {

    private final GlobePvpMatchRoom room;
    private final int mySlot;

    public PvpGlobeBlitzViewModel(GlobePvpMatchRoom room, int mySlot) {
        this.room = room;
        this.mySlot = mySlot;
    }

    public GlobePvpMatchRoom getRoom() { return room; }
    public int getMySlot() { return mySlot; }
    public PvpGlobeBlitzEngineBL getEngine() { return room.getBlitzEngine(); }

    public GuessResultGlobeBL submitGuess(String countryName) {
        return room.getBlitzEngine().submitGuess(mySlot, countryName);
    }

    public String giveUpFlag() {
        return room.getBlitzEngine().giveUpFlag(mySlot);
    }
}
