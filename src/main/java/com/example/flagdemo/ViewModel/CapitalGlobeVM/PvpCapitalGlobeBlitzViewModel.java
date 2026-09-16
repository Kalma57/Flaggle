package com.example.flagdemo.ViewModel.CapitalGlobeVM;

import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobePvpMatchRoom;
import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.PvpCapitalGlobeBlitzEngineBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;

/**
 * ViewModel for one player's perspective on a live capital-location globe "vs Friend" PvP
 * Blitz room. Bridges the View with the shared {@link CapitalGlobePvpMatchRoom}/
 * {@link PvpCapitalGlobeBlitzEngineBL} that both players' browsers act on concurrently -
 * mirrors {@link PvpCapitalGlobeMatchViewModel}, just parameterized the same way and wired to
 * the Blitz engine instead.
 */
public class PvpCapitalGlobeBlitzViewModel implements java.io.Serializable {

    private final CapitalGlobePvpMatchRoom room;
    private final int mySlot;

    public PvpCapitalGlobeBlitzViewModel(CapitalGlobePvpMatchRoom room, int mySlot) {
        this.room = room;
        this.mySlot = mySlot;
    }

    public CapitalGlobePvpMatchRoom getRoom() { return room; }
    public int getMySlot() { return mySlot; }
    public PvpCapitalGlobeBlitzEngineBL getEngine() { return room.getBlitzEngine(); }

    public GuessResultGlobeBL submitGuess(String countryName) {
        return room.getBlitzEngine().submitGuess(mySlot, countryName);
    }

    public String skipTarget() {
        return room.getBlitzEngine().skipTarget(mySlot);
    }
}
