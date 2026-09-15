package com.example.flagdemo.ServiceLayer.CapitalGlobeSL;

import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobeMatchEngineBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;

/**
 * Service layer for the capital-location globe game's "First to N" format - pure delegation to
 * the engine, mirroring {@link com.example.flagdemo.ServiceLayer.GlobeMatchSL.GlobeMatchService}.
 */
public class CapitalGlobeMatchService implements java.io.Serializable {

    private final CapitalGlobeMatchEngineBL engine;

    public CapitalGlobeMatchService(CountryController cc, AiSkillLevel aiLevel, int pointsToWin) {
        this.engine = new CapitalGlobeMatchEngineBL(cc, aiLevel, pointsToWin);
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

    public void giveUpRound() {
        engine.giveUpRound();
    }

    public void pauseMatch() {
        engine.pauseMatch();
    }

    public void resumeMatch() {
        engine.resumeMatch();
    }

    public CapitalGlobeMatchEngineBL getEngine() {
        return engine;
    }
}
