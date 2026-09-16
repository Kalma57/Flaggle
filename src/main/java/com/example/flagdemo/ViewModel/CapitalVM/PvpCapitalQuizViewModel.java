package com.example.flagdemo.ViewModel.CapitalVM;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalPvpMatchRoom;
import com.example.flagdemo.BusinessLayer.CapitalBL.PvpCapitalQuizEngineBL;

/**
 * ViewModel for one player's perspective on a live capital-quiz "vs Friend" PvP match room.
 * Bridges the View with the shared {@link CapitalPvpMatchRoom}/{@link PvpCapitalQuizEngineBL}
 * that both players' browsers act on concurrently - mirrors
 * {@link com.example.flagdemo.ViewModel.CapitalGlobeVM.PvpCapitalGlobeMatchViewModel}, just
 * parameterized the same way and wired to the quiz engine instead. Constructed fresh on every
 * request (not stored in session) - no Model or Service layer in between, same as every other
 * PvP path in this codebase.
 */
public class PvpCapitalQuizViewModel implements java.io.Serializable {

    private final CapitalPvpMatchRoom room;
    private final int mySlot;

    public PvpCapitalQuizViewModel(CapitalPvpMatchRoom room, int mySlot) {
        this.room = room;
        this.mySlot = mySlot;
    }

    public CapitalPvpMatchRoom getRoom() { return room; }
    public int getMySlot() { return mySlot; }
    public PvpCapitalQuizEngineBL getEngine() { return room.getEngine(); }

    public void submitAnswer(int optionIndex) {
        room.getEngine().submitAnswer(mySlot, optionIndex);
    }
}
