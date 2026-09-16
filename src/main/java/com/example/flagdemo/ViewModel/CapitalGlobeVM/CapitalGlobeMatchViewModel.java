package com.example.flagdemo.ViewModel.CapitalGlobeVM;

import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobeMatchEngineBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.Model.CapitalGlobeModel.CapitalGlobeMatchModel;

/**
 * ViewModel for the capital-location globe game's "First to N" format - thin bridge between
 * the Controller and the Model, mirroring {@link com.example.flagdemo.ViewModel.GlobeMatchVM.GlobeMatchViewModel}.
 */
public class CapitalGlobeMatchViewModel implements java.io.Serializable {

    private final CapitalGlobeMatchModel cm;

    public CapitalGlobeMatchViewModel(CountryController cc, AiSkillLevel aiLevel, int pointsToWin) {
        this.cm = new CapitalGlobeMatchModel(cc, aiLevel, pointsToWin);
    }

    public void startMatch() {
        cm.startMatch();
    }

    public void advanceToNextRound() {
        cm.advanceToNextRound();
    }

    public GuessResultGlobeBL humanGuess(String countryName) {
        return cm.humanGuess(countryName);
    }

    public void giveUpRound() {
        cm.giveUpRound();
    }

    public void pauseMatch() {
        cm.pauseMatch();
    }

    public void resumeMatch() {
        cm.resumeMatch();
    }

    public CapitalGlobeMatchEngineBL getEngine() {
        return cm.getEngine();
    }
}
