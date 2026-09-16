package com.example.flagdemo.ServiceLayer.CapitalGlobeSL;

import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobeBlitzEngineBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;

/**
 * Service layer for the capital-location globe game's Blitz format - pure delegation to the
 * engine, mirroring {@link com.example.flagdemo.ServiceLayer.GlobeMatchSL.GlobeBlitzService}.
 */
public class CapitalGlobeBlitzService implements java.io.Serializable {

    private final CapitalGlobeBlitzEngineBL engine;

    public CapitalGlobeBlitzService(CountryController cc, AiSkillLevel aiLevel, double durationSeconds) {
        this.engine = new CapitalGlobeBlitzEngineBL(cc, aiLevel, durationSeconds);
    }

    public void startMatch() {
        engine.startMatch();
    }

    public GuessResultGlobeBL humanGuess(String countryName) {
        return engine.humanGuess(countryName);
    }

    public String humanSkipTarget() {
        return engine.humanSkipTarget();
    }

    public void refreshAiProgress() {
        engine.refreshAiProgress();
    }

    public void pauseMatch() {
        engine.pauseMatch();
    }

    public void resumeMatch() {
        engine.resumeMatch();
    }

    public CapitalGlobeBlitzEngineBL getEngine() {
        return engine;
    }
}
