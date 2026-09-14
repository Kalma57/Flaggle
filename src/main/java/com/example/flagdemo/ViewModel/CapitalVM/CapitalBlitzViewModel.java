package com.example.flagdemo.ViewModel.CapitalVM;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalBlitzEngineBL;
import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizMode;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.Model.CapitalModel.CapitalBlitzModel;

/**
 * ViewModel for the capital-quiz Blitz (time-attack) mode - thin bridge between the Controller
 * and the Model, mirroring {@link com.example.flagdemo.ViewModel.GlobeMatchVM.GlobeBlitzViewModel}.
 */
public class CapitalBlitzViewModel implements java.io.Serializable {

    private final CapitalBlitzModel cm;

    public CapitalBlitzViewModel(CountryController cc, CapitalQuizMode mode, AiSkillLevel aiLevel, double durationSeconds) {
        this.cm = new CapitalBlitzModel(cc, mode, aiLevel, durationSeconds);
    }

    public void startMatch() {
        cm.startMatch();
    }

    public Boolean submitAnswer(int optionIndex) {
        return cm.submitAnswer(optionIndex);
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

    public CapitalBlitzEngineBL getEngine() {
        return cm.getEngine();
    }
}
