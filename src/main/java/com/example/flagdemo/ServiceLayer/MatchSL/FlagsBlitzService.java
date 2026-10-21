package com.example.flagdemo.ServiceLayer.MatchSL;

import com.example.flagdemo.BusinessLayer.FlagsBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlagsBL.GuessResultBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.FlagsBlitzEngineBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

/**
 * Service layer for the Blitz (1-minute time-attack) 1v1 Flags mode.
 *
 * Acts as a bridge between the ViewModel and the FlagsBlitzEngineBL, mirroring the
 * existing FlagsMatchService pattern used by the "Best of N" match mode.
 */
public class FlagsBlitzService implements java.io.Serializable {

    private final FlagsBlitzEngineBL engine;

    public FlagsBlitzService(CountryController cc, DifficultyLevel difficulty, AiSkillLevel aiLevel, double durationSeconds) {
        this.engine = new FlagsBlitzEngineBL(cc, difficulty, aiLevel, durationSeconds);
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

    public FlagsBlitzEngineBL getEngine() {
        return engine;
    }
}
