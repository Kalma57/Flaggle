package com.example.flagdemo.ServiceLayer.GlobeMatchSL;

import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobeMatchEngineBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;

/**
 * Service layer for the 1v1 Globe Match mode.
 *
 * Acts as a bridge between the ViewModel and the GlobeMatchEngineBL, mirroring the
 * existing FlaggleMatchService pattern.
 */
public class GlobeMatchService implements java.io.Serializable {

    private final GlobeMatchEngineBL engine;

    public GlobeMatchService(CountryController cc, AiSkillLevel aiLevel, int pointsToWin) {
        this.engine = new GlobeMatchEngineBL(cc, aiLevel, pointsToWin);
    }

    public void startMatch() {
        engine.startMatch();
    }

    public void advanceToNextRound() {
        engine.advanceToNextRound();
    }

    public GuessResultGlobeBL humanGuess(String countryName) {
        return engine.humanGuess(countryName);
    }

    public void humanGiveUpRound() {
        engine.humanGiveUpRound();
    }

    public void refreshAiTimeout() {
        engine.refreshAiTimeout();
    }

    public void pauseMatch() {
        engine.pauseMatch();
    }

    public void resumeMatch() {
        engine.resumeMatch();
    }

    /** Reveals one more letter of the target's name - see {@link GlobeMatchEngineBL#useHint()}. */
    public String useHint() {
        return engine.useHint();
    }

    public GlobeMatchEngineBL getEngine() {
        return engine;
    }
}
