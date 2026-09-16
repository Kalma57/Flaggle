package com.example.flagdemo.ViewModel.CapitalVM;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalPvpMatchRoom;
import com.example.flagdemo.BusinessLayer.CapitalBL.PvpCapitalBlitzEngineBL;

/**
 * ViewModel for one player's perspective on a live capital-quiz "vs Friend" PvP Blitz room -
 * mirrors {@link PvpCapitalQuizViewModel}, wired to the Blitz engine instead.
 */
public class PvpCapitalBlitzViewModel implements java.io.Serializable {

    private final CapitalPvpMatchRoom room;
    private final int mySlot;

    public PvpCapitalBlitzViewModel(CapitalPvpMatchRoom room, int mySlot) {
        this.room = room;
        this.mySlot = mySlot;
    }

    public CapitalPvpMatchRoom getRoom() { return room; }
    public int getMySlot() { return mySlot; }
    public PvpCapitalBlitzEngineBL getEngine() { return room.getBlitzEngine(); }

    public Boolean submitAnswer(int optionIndex) {
        return room.getBlitzEngine().submitAnswer(mySlot, optionIndex);
    }
}
