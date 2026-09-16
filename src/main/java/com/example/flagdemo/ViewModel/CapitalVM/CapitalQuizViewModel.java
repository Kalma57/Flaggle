package com.example.flagdemo.ViewModel.CapitalVM;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizEngineBL;
import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizMode;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.Model.CapitalModel.CapitalQuizModel;

/**
 * ViewModel for the capital-quiz "Best of N" mode - thin bridge between the Controller and the
 * Model, mirroring {@link com.example.flagdemo.ViewModel.GlobeMatchVM.GlobeMatchViewModel}.
 */
public class CapitalQuizViewModel implements java.io.Serializable {

    private final CapitalQuizModel cm;

    public CapitalQuizViewModel(CountryController cc, CapitalQuizMode mode, AiSkillLevel aiLevel, int pointsToWin) {
        this.cm = new CapitalQuizModel(cc, mode, aiLevel, pointsToWin);
    }

    public void startMatch() {
        cm.startMatch();
    }

    public void advanceToNextRound() {
        cm.advanceToNextRound();
    }

    public boolean submitAnswer(int optionIndex) {
        return cm.submitAnswer(optionIndex);
    }

    public void pauseMatch() {
        cm.pauseMatch();
    }

    public void resumeMatch() {
        cm.resumeMatch();
    }

    public CapitalQuizEngineBL getEngine() {
        return cm.getEngine();
    }
}
