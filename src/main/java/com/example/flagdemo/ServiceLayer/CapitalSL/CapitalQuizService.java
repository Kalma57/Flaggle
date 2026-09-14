package com.example.flagdemo.ServiceLayer.CapitalSL;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizEngineBL;
import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizMode;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;

/**
 * Service layer for the capital-quiz "Best of N" mode - pure delegation to the engine,
 * mirroring {@link com.example.flagdemo.ServiceLayer.GlobeMatchSL.GlobeMatchService}.
 */
public class CapitalQuizService implements java.io.Serializable {

    private final CapitalQuizEngineBL engine;

    public CapitalQuizService(CountryController cc, CapitalQuizMode mode, AiSkillLevel aiLevel, int pointsToWin) {
        this.engine = new CapitalQuizEngineBL(cc, mode, aiLevel, pointsToWin);
    }

    public void startMatch() {
        engine.startMatch();
    }

    public void advanceToNextRound() {
        engine.advanceToNextRound();
    }

    public boolean submitAnswer(int optionIndex) {
        return engine.submitAnswer(optionIndex);
    }

    public void pauseMatch() {
        engine.pauseMatch();
    }

    public void resumeMatch() {
        engine.resumeMatch();
    }

    public CapitalQuizEngineBL getEngine() {
        return engine;
    }
}
