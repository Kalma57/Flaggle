package com.example.flagdemo.ViewModel.GlobeMatchVM;

import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobePvpMatchRoom;
import com.example.flagdemo.BusinessLayer.GlobeMatchBL.PvpGlobeMatchEngineBL;

/**
 * ViewModel for one player's perspective on a live "vs Friend" Globe PvP match room.
 *
 * Bridges the View (GlobeMultiplayerController + Thymeleaf/JS) with the shared
 * {@link GlobePvpMatchRoom}/{@link PvpGlobeMatchEngineBL} that both players' browsers act
 * on concurrently - mirrors {@link com.example.flagdemo.ViewModel.MatchVM.PvpMatchViewModel}
 * exactly, just parameterized by which of the two real players ("slots") this particular
 * request/browser belongs to. Unlike its Flaggle counterpart, {@link #advanceToNextRound()}
 * declares no checked exception, since neither {@link PvpGlobeMatchEngineBL} nor the
 * underlying {@link com.example.flagdemo.DataAccessLayer.CountryController} calls it relies
 * on throw one.
 */
public class PvpGlobeMatchViewModel implements java.io.Serializable {

    private final GlobePvpMatchRoom room;
    private final int mySlot;

    public PvpGlobeMatchViewModel(GlobePvpMatchRoom room, int mySlot) {
        this.room = room;
        this.mySlot = mySlot;
    }

    public GlobePvpMatchRoom getRoom() { return room; }
    public int getMySlot() { return mySlot; }
    public PvpGlobeMatchEngineBL getEngine() { return room.getEngine(); }

    public GuessResultGlobeBL submitGuess(String countryName) {
        return room.getEngine().submitGuess(mySlot, countryName);
    }

    public void giveUpRound() {
        room.getEngine().giveUpRound(mySlot);
    }

    public void advanceToNextRound() {
        room.getEngine().advanceToNextRound();
    }

    /** Marks this player ready for the next round - see {@link PvpGlobeMatchEngineBL#markReady(int)}. */
    public void markReady() {
        room.getEngine().markReady(mySlot);
    }

    /** Reveals one more letter of the target's name to this player only - see {@link PvpGlobeMatchEngineBL#useHint(int)}. */
    public String useHint() {
        return room.getEngine().useHint(mySlot);
    }
}
