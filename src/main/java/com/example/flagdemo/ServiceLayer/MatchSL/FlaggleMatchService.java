package com.example.flagdemo.ServiceLayer.MatchSL;

import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.FlaggleMatchEngineBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.sql.SQLException;

/**
 * Service layer for the 1v1 Flaggle Match mode.
 *
 * Acts as a bridge between the ViewModel and the FlaggleMatchEngineBL, mirroring the
 * existing GameService / GlobeGameService pattern used by the single-player modes.
 */
public class FlaggleMatchService implements java.io.Serializable {

    private final FlaggleMatchEngineBL engine;

    public FlaggleMatchService(CountryController cc, DifficultyLevel difficulty, AiSkillLevel aiLevel, int pointsToWin) {
        this.engine = new FlaggleMatchEngineBL(cc, difficulty, aiLevel, pointsToWin);
    }

    public void startMatch() throws SQLException {
        engine.startMatch();
    }

    public void advanceToNextRound() throws SQLException {
        engine.advanceToNextRound();
    }

    public GuessResultBL humanGuess(String countryName) {
        return engine.humanGuess(countryName);
    }

    public void humanGiveUpRound() {
        engine.humanGiveUpRound();
    }

    public void refreshAiTimeout() {
        engine.refreshAiTimeout();
    }

    public void pauseMatch() {
        engine.pauseMatch();
    }

    public void resumeMatch() {
        engine.resumeMatch();
    }

    public FlaggleMatchEngineBL getEngine() {
        return engine;
    }
}
