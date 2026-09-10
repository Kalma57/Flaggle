package com.example.flagdemo.Model.GlobeMatchModel;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobeBlitzEngineBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ServiceLayer.GlobeMatchSL.GlobeBlitzService;

import java.util.List;

/**
 * Model layer for the Blitz (1/2-minute time-attack) 1v1 Globe mode. Wraps the Service
 * layer and holds whatever extra state the ViewModel/View needs (here: the country list
 * for autocomplete) - mirrors {@link com.example.flagdemo.Model.MatchModel.FlaggleBlitzModel}
 * exactly, minus the difficulty level (Globe has none).
 */
public class GlobeBlitzModel implements java.io.Serializable {

    private final GlobeBlitzService service;
    private final List<CountryBL> allCountries;

    public GlobeBlitzModel(CountryController cc, AiSkillLevel aiLevel, double durationSeconds) {
        this.service = new GlobeBlitzService(cc, aiLevel, durationSeconds);
        this.allCountries = cc.getAllCountries();
    }

    public void startMatch() {
        service.startMatch();
    }

    public GuessResultGlobeBL humanGuess(String countryName) {
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

    public GlobeBlitzEngineBL getEngine() {
        return service.getEngine();
    }

    public List<CountryBL> getAllCountries() {
        return allCountries;
    }
}
