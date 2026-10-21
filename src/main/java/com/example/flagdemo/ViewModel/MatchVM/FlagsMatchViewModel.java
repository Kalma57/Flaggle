package com.example.flagdemo.ViewModel.MatchVM;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlagsBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlagsBL.GuessResultBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.FlagsMatchEngineBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.Model.MatchModel.FlagsMatchModel;

import java.sql.SQLException;
import java.util.List;

/**
 * ViewModel for the 1v1 Flags Match mode ("Best of 3", vs AI opponent for now).
 *
 * Bridges the View (FlagsMatchController + Thymeleaf/JS) with the Model layer, exposing
 * only what the UI needs while hiding the engine internals.
 */
public class FlagsMatchViewModel implements java.io.Serializable {

    private final FlagsMatchModel fm;

    public FlagsMatchViewModel(CountryController cc, DifficultyLevel difficulty, AiSkillLevel aiLevel, int pointsToWin) throws SQLException {
        this.fm = new FlagsMatchModel(cc, difficulty, aiLevel, pointsToWin);
    }

    public void startMatch() throws SQLException {
        fm.startMatch();
    }

    public void advanceToNextRound() throws SQLException {
        fm.advanceToNextRound();
    }

    public GuessResultBL humanGuess(String countryName) {
        return fm.humanGuess(countryName);
    }

    public void humanGiveUpRound() {
        fm.humanGiveUpRound();
    }

    public void refreshAiTimeout() {
        fm.refreshAiTimeout();
    }

    public void pauseMatch() {
        fm.pauseMatch();
    }

    public void resumeMatch() {
        fm.resumeMatch();
    }

    public FlagsMatchEngineBL getEngine() {
        return fm.getEngine();
    }

    public List<CountryBL> getAllCountries() {
        return fm.getAllCountries();
    }
}
