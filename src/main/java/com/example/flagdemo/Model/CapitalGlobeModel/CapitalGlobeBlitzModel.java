package com.example.flagdemo.Model.CapitalGlobeModel;

import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobeBlitzEngineBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ServiceLayer.CapitalGlobeSL.CapitalGlobeBlitzService;

/**
 * Model layer for the capital-location globe game's Blitz format - wraps the Service layer,
 * mirroring {@link com.example.flagdemo.Model.GlobeMatchModel.GlobeBlitzModel}.
 */
public class CapitalGlobeBlitzModel implements java.io.Serializable {

    private final CapitalGlobeBlitzService service;

    public CapitalGlobeBlitzModel(CountryController cc, AiSkillLevel aiLevel, double durationSeconds) {
        this.service = new CapitalGlobeBlitzService(cc, aiLevel, durationSeconds);
    }

    public void startMatch() {
        service.startMatch();
    }

    public GuessResultGlobeBL humanGuess(String countryName) {
        return service.humanGuess(countryName);
    }

    public String humanSkipTarget() {
        return service.humanSkipTarget();
    }

    public void refreshAiProgress() {
        service.refreshAiProgress();
    }

    public void pauseMatch() {
        service.pauseMatch();
    }

    public void resumeMatch() {
        service.resumeMatch();
    }

    public CapitalGlobeBlitzEngineBL getEngine() {
        return service.getEngine();
    }
}
