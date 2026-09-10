package com.example.flagdemo.ServiceLayer.GlobeMatchSL;

import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobeBlitzEngineBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;

/**
 * Service layer for the Blitz (1/2-minute time-attack) 1v1 Globe mode.
 *
 * Acts as a bridge between the ViewModel and the GlobeBlitzEngineBL, mirroring the
 * existing FlaggleBlitzService pattern.
 */
public class GlobeBlitzService implements java.io.Serializable {

    private final GlobeBlitzEngineBL engine;

    public GlobeBlitzService(CountryController cc, AiSkillLevel aiLevel, double durationSeconds) {
        this.engine = new GlobeBlitzEngineBL(cc, aiLevel, durationSeconds);
    }

    public void startMatch() {
        engine.startMatch();
    }

    public GuessResultGlobeBL humanGuess(String countryName) {
        return engine.humanGuess(countryName);
    }

    public String humanGiveUpFlag() {
        return engine.humanGiveUpFlag();
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

    public GlobeBlitzEngineBL getEngine() {
        return engine;
    }
}
