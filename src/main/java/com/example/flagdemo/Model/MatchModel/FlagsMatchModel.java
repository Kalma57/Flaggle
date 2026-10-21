package com.example.flagdemo.Model.MatchModel;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlagsBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlagsBL.GuessResultBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.FlagsMatchEngineBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ServiceLayer.MatchSL.FlagsMatchService;

import java.sql.SQLException;
import java.util.List;

/**
 * Model layer for the 1v1 Flags Match mode. Wraps the Service layer and holds
 * whatever extra state the ViewModel/View needs (here: the country list for autocomplete).
 */
public class FlagsMatchModel implements java.io.Serializable {

    private final FlagsMatchService service;
    private final List<CountryBL> allCountries;

    public FlagsMatchModel(CountryController cc, DifficultyLevel difficulty, AiSkillLevel aiLevel, int pointsToWin) throws SQLException {
        this.service = new FlagsMatchService(cc, difficulty, aiLevel, pointsToWin);
        this.allCountries = cc.getAllCountries();
    }

    public void startMatch() throws SQLException {
        service.startMatch();
    }

    public void advanceToNextRound() throws SQLException {
        service.advanceToNextRound();
    }

    public GuessResultBL humanGuess(String countryName) {
        return service.humanGuess(countryName);
    }

    public void humanGiveUpRound() {
        service.humanGiveUpRound();
    }

    public void refreshAiTimeout() {
        service.refreshAiTimeout();
    }

    public void pauseMatch() {
        service.pauseMatch();
    }

    public void resumeMatch() {
        service.resumeMatch();
    }

    public FlagsMatchEngineBL getEngine() {
        return service.getEngine();
    }

    public List<CountryBL> getAllCountries() {
        return allCountries;
    }
}
