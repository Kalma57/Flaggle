package com.example.flagdemo.ViewModel.GlobeMatchVM;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.GlobeMatchBL.GlobeMatchEngineBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.Model.GlobeMatchModel.GlobeMatchModel;

import java.util.List;

/**
 * ViewModel for the 1v1 Globe Match mode ("Best of N", vs AI opponent).
 *
 * Bridges the View (GlobeMatchController + Thymeleaf/JS) with the Model layer, exposing
 * only what the UI needs while hiding the engine internals - mirrors FlaggleMatchViewModel.
 */
public class GlobeMatchViewModel implements java.io.Serializable {

    private final GlobeMatchModel gm;

    public GlobeMatchViewModel(CountryController cc, AiSkillLevel aiLevel, int pointsToWin) {
        this.gm = new GlobeMatchModel(cc, aiLevel, pointsToWin);
    }

    public void startMatch() {
        gm.startMatch();
    }

    public void advanceToNextRound() {
        gm.advanceToNextRound();
    }

    public GuessResultGlobeBL humanGuess(String countryName) {
        return gm.humanGuess(countryName);
    }

    public void humanGiveUpRound() {
        gm.humanGiveUpRound();
    }

    public void refreshAiTimeout() {
        gm.refreshAiTimeout();
    }

    public void pauseMatch() {
        gm.pauseMatch();
    }

    public void resumeMatch() {
        gm.resumeMatch();
    }

    public String useHint() {
        return gm.useHint();
    }

    public GlobeMatchEngineBL getEngine() {
        return gm.getEngine();
    }

    public List<CountryBL> getAllCountries() {
        return gm.getAllCountries();
    }
}
