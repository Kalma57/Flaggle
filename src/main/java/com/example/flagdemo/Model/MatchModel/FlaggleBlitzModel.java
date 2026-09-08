package com.example.flagdemo.Model.MatchModel;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.FlaggleBlitzEngineBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ServiceLayer.MatchSL.FlaggleBlitzService;

import java.util.List;

/**
 * Model layer for the Blitz (1-minute time-attack) 1v1 Flaggle mode. Wraps the Service
 * layer and holds whatever extra state the ViewModel/View needs (here: the country list
 * for autocomplete), mirroring the existing FlaggleMatchModel pattern.
 */
public class FlaggleBlitzModel implements java.io.Serializable {

    private final FlaggleBlitzService service;
    private final List<CountryBL> allCountries;

    public FlaggleBlitzModel(CountryController cc, DifficultyLevel difficulty, AiSkillLevel aiLevel, double durationSeconds) {
        this.service = new FlaggleBlitzService(cc, difficulty, aiLevel, durationSeconds);
        this.allCountries = cc.getAllCountries();
    }

    public void startMatch() {
        service.startMatch();
    }

    public GuessResultBL humanGuess(String countryName) {
        return service.humanGuess(countryName);
    }

    public String humanGiveUpFlag() {
        return service.humanGiveUpFlag();
    }

    public void refreshAiProgress() {
        service.refreshAiProgress();
    }

    public void pauseMatch() {
        service.pauseMatch();
    }

    public void resumeMatch() {
        service.resumeMatch();
    }

    public FlaggleBlitzEngineBL getEngine() {
        return service.getEngine();
    }

    public List<CountryBL> getAllCountries() {
        return allCountries;
    }
}
