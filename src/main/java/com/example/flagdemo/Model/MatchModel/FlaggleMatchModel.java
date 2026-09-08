package com.example.flagdemo.Model.MatchModel;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.FlaggleMatchEngineBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ServiceLayer.MatchSL.FlaggleMatchService;

import java.sql.SQLException;
import java.util.List;

/**
 * Model layer for the 1v1 Flaggle Match mode. Wraps the Service layer and holds
 * whatever extra state the ViewModel/View needs (here: the country list for autocomplete).
 */
public class FlaggleMatchModel implements java.io.Serializable {

    private final FlaggleMatchService service;
    private final List<CountryBL> allCountries;

    public FlaggleMatchModel(CountryController cc, DifficultyLevel difficulty, AiSkillLevel aiLevel, int pointsToWin) throws SQLException {
        this.service = new FlaggleMatchService(cc, difficulty, aiLevel, pointsToWin);
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

    public FlaggleMatchEngineBL getEngine() {
        return service.getEngine();
    }

    public List<CountryBL> getAllCountries() {
        return allCountries;
    }
}
