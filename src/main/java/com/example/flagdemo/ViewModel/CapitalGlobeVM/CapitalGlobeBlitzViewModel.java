package com.example.flagdemo.ViewModel.CapitalGlobeVM;

import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobeBlitzEngineBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.Model.CapitalGlobeModel.CapitalGlobeBlitzModel;

/**
 * ViewModel for the capital-location globe game's Blitz format - thin bridge between the
 * Controller and the Model, mirroring {@link com.example.flagdemo.ViewModel.GlobeMatchVM.GlobeBlitzViewModel}.
 */
public class CapitalGlobeBlitzViewModel implements java.io.Serializable {

    private final CapitalGlobeBlitzModel cm;

    public CapitalGlobeBlitzViewModel(CountryController cc, AiSkillLevel aiLevel, double durationSeconds) {
        this.cm = new CapitalGlobeBlitzModel(cc, aiLevel, durationSeconds);
    }

    public void startMatch() {
        cm.startMatch();
    }

    public GuessResultGlobeBL humanGuess(String countryName) {
        return cm.humanGuess(countryName);
    }

    public String humanSkipTarget() {
        return cm.humanSkipTarget();
    }

    public void refreshAiProgress() {
        cm.refreshAiProgress();
    }

    public void pauseMatch() {
        cm.pauseMatch();
    }

    public void resumeMatch() {
        cm.resumeMatch();
    }

    public CapitalGlobeBlitzEngineBL getEngine() {
        return cm.getEngine();
    }
}
