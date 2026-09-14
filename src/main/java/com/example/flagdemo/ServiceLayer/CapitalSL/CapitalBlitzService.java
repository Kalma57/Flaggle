package com.example.flagdemo.ServiceLayer.CapitalSL;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalBlitzEngineBL;
import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizMode;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;

/**
 * Service layer for the capital-quiz Blitz (time-attack) mode - pure delegation to the engine,
 * mirroring {@link com.example.flagdemo.ServiceLayer.GlobeMatchSL.GlobeBlitzService}.
 */
public class CapitalBlitzService implements java.io.Serializable {

    private final CapitalBlitzEngineBL engine;

    public CapitalBlitzService(CountryController cc, CapitalQuizMode mode, AiSkillLevel aiLevel, double durationSeconds) {
        this.engine = new CapitalBlitzEngineBL(cc, mode, aiLevel, durationSeconds);
    }

    public void startMatch() {
        engine.startMatch();
    }

    public Boolean submitAnswer(int optionIndex) {
        return engine.submitAnswer(optionIndex);
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

    public CapitalBlitzEngineBL getEngine() {
        return engine;
    }
}
