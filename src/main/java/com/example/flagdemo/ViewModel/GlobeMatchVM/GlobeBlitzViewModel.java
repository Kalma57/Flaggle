package com.example.flagdemo.ViewModel.GlobeMatchVM;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobeBlitzEngineBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.Model.GlobeMatchModel.GlobeBlitzModel;

import java.util.List;

/**
 * ViewModel for the Blitz (1/2-minute time-attack) 1v1 Globe mode, vs an AI opponent.
 *
 * Bridges the View (GlobeBlitzController + Thymeleaf/JS) with the Model layer, exposing
 * only what the UI needs while hiding the engine internals - mirrors GlobeMatchViewModel.
 */
public class GlobeBlitzViewModel implements java.io.Serializable {

    private final GlobeBlitzModel gm;

    public GlobeBlitzViewModel(CountryController cc, AiSkillLevel aiLevel, double durationSeconds) {
        this.gm = new GlobeBlitzModel(cc, aiLevel, durationSeconds);
    }

    public void startMatch() {
        gm.startMatch();
    }

    public GuessResultGlobeBL humanGuess(String countryName) {
        return gm.humanGuess(countryName);
    }

    public String humanGiveUpFlag() {
        return gm.humanGiveUpFlag();
    }

    public void refreshAiProgress() {
        gm.refreshAiProgress();
    }

    public void pauseMatch() {
        gm.pauseMatch();
    }

    public void resumeMatch() {
        gm.resumeMatch();
    }

    public GlobeBlitzEngineBL getEngine() {
        return gm.getEngine();
    }

    public List<CountryBL> getAllCountries() {
        return gm.getAllCountries();
    }
}
