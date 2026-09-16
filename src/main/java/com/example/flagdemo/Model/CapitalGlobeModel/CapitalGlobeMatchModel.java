package com.example.flagdemo.Model.CapitalGlobeModel;

import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobeMatchEngineBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ServiceLayer.CapitalGlobeSL.CapitalGlobeMatchService;

/**
 * Model layer for the capital-location globe game's "First to N" format - wraps the Service
 * layer, mirroring {@link com.example.flagdemo.Model.GlobeMatchModel.GlobeMatchModel}.
 */
public class CapitalGlobeMatchModel implements java.io.Serializable {

    private final CapitalGlobeMatchService service;

    public CapitalGlobeMatchModel(CountryController cc, AiSkillLevel aiLevel, int pointsToWin) {
        this.service = new CapitalGlobeMatchService(cc, aiLevel, pointsToWin);
    }

    public void startMatch() {
        service.startMatch();
    }

    public void advanceToNextRound() {
        service.advanceToNextRound();
    }

    public GuessResultGlobeBL humanGuess(String countryName) {
        return service.humanGuess(countryName);
    }

    public void giveUpRound() {
        service.giveUpRound();
    }

    public void pauseMatch() {
        service.pauseMatch();
    }

    public void resumeMatch() {
        service.resumeMatch();
    }

    public CapitalGlobeMatchEngineBL getEngine() {
        return service.getEngine();
    }
}
