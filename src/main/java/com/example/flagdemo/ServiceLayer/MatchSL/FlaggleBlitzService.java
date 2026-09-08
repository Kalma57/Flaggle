package com.example.flagdemo.ServiceLayer.MatchSL;

import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.FlaggleBlitzEngineBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

/**
 * Service layer for the Blitz (1-minute time-attack) 1v1 Flaggle mode.
 *
 * Acts as a bridge between the ViewModel and the FlaggleBlitzEngineBL, mirroring the
 * existing FlaggleMatchService pattern used by the "Best of N" match mode.
 */
public class FlaggleBlitzService implements java.io.Serializable {

    private final FlaggleBlitzEngineBL engine;

    public FlaggleBlitzService(CountryController cc, DifficultyLevel difficulty, AiSkillLevel aiLevel, double durationSeconds) {
        this.engine = new FlaggleBlitzEngineBL(cc, difficulty, aiLevel, durationSeconds);
    }

    public void startMatch() {
        engine.startMatch();
    }

    public GuessResultBL humanGuess(String countryName) {
        return engine.humanGuess(countryName);
    }

    public String humanGiveUpFlag() {
        return engine.humanGiveUpFlag();
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

    public FlaggleBlitzEngineBL getEngine() {
        return engine;
    }
}
