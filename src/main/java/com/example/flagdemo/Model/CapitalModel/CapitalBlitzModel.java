package com.example.flagdemo.Model.CapitalModel;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalBlitzEngineBL;
import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizMode;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ServiceLayer.CapitalSL.CapitalBlitzService;

/**
 * Model layer for the capital-quiz Blitz (time-attack) mode - wraps the Service layer,
 * mirroring {@link com.example.flagdemo.Model.GlobeMatchModel.GlobeBlitzModel}.
 */
public class CapitalBlitzModel implements java.io.Serializable {

    private final CapitalBlitzService service;

    public CapitalBlitzModel(CountryController cc, CapitalQuizMode mode, AiSkillLevel aiLevel, double durationSeconds) {
        this.service = new CapitalBlitzService(cc, mode, aiLevel, durationSeconds);
    }

    public void startMatch() {
        service.startMatch();
    }

    public Boolean submitAnswer(int optionIndex) {
        return service.submitAnswer(optionIndex);
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

    public CapitalBlitzEngineBL getEngine() {
        return service.getEngine();
    }
}
