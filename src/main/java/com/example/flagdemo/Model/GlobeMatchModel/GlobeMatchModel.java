package com.example.flagdemo.Model.GlobeMatchModel;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobeMatchEngineBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ServiceLayer.GlobeMatchSL.GlobeMatchService;

import java.util.List;

/**
 * Model layer for the 1v1 Globe Match mode. Wraps the Service layer and holds whatever
 * extra state the ViewModel/View needs (here: the country list for autocomplete) -
 * mirrors {@link com.example.flagdemo.Model.MatchModel.FlaggleMatchModel} exactly, minus
 * the difficulty level (Globe has none) and without a checked SQLException, since neither
 * {@link GlobeMatchEngineBL} nor the {@link CountryController} calls it relies on throw one.
 */
public class GlobeMatchModel implements java.io.Serializable {

    private final GlobeMatchService service;
    private final List<CountryBL> allCountries;

    public GlobeMatchModel(CountryController cc, AiSkillLevel aiLevel, int pointsToWin) {
        this.service = new GlobeMatchService(cc, aiLevel, pointsToWin);
        this.allCountries = cc.getAllCountries();
    }

    public void startMatch() {
        service.startMatch();
    }

    public void advanceToNextRound() {
        service.advanceToNextRound();
    }

    public GuessResultGlobeBL humanGuess(String countryName) {
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

    public String useHint() {
        return service.useHint();
    }

    public GlobeMatchEngineBL getEngine() {
        return service.getEngine();
    }

    public List<CountryBL> getAllCountries() {
        return allCountries;
    }
}
