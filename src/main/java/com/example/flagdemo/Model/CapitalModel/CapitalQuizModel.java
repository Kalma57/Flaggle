package com.example.flagdemo.Model.CapitalModel;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizEngineBL;
import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizMode;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ServiceLayer.CapitalSL.CapitalQuizService;

/**
 * Model layer for the capital-quiz "Best of N" mode - wraps the Service layer, mirroring
 * {@link com.example.flagdemo.Model.GlobeMatchModel.GlobeMatchModel} (minus the country list,
 * which this mode's screens don't need for autocomplete since answers are clickable options).
 */
public class CapitalQuizModel implements java.io.Serializable {

    private final CapitalQuizService service;

    public CapitalQuizModel(CountryController cc, CapitalQuizMode mode, AiSkillLevel aiLevel, int pointsToWin) {
        this.service = new CapitalQuizService(cc, mode, aiLevel, pointsToWin);
    }

    public void startMatch() {
        service.startMatch();
    }

    public void advanceToNextRound() {
        service.advanceToNextRound();
    }

    public boolean submitAnswer(int optionIndex) {
        return service.submitAnswer(optionIndex);
    }

    public void pauseMatch() {
        service.pauseMatch();
    }

    public void resumeMatch() {
        service.resumeMatch();
    }

    public CapitalQuizEngineBL getEngine() {
        return service.getEngine();
    }
}
