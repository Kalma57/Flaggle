package com.example.flagdemo.ViewModel.MatchVM;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.FlaggleBlitzEngineBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.Model.MatchModel.FlaggleBlitzModel;

import java.util.List;

/**
 * ViewModel for the Blitz (1-minute time-attack) 1v1 Flaggle mode, vs an AI opponent.
 *
 * Bridges the View (FlaggleBlitzController + Thymeleaf/JS) with the Model layer, exposing
 * only what the UI needs while hiding the engine internals — mirrors FlaggleMatchViewModel.
 */
public class FlaggleBlitzViewModel implements java.io.Serializable {

    private final FlaggleBlitzModel fm;

    public FlaggleBlitzViewModel(CountryController cc, DifficultyLevel difficulty, AiSkillLevel aiLevel, double durationSeconds) {
        this.fm = new FlaggleBlitzModel(cc, difficulty, aiLevel, durationSeconds);
    }

    public void startMatch() {
        fm.startMatch();
    }

    public GuessResultBL humanGuess(String countryName) {
        return fm.humanGuess(countryName);
    }

    public String humanGiveUpFlag() {
        return fm.humanGiveUpFlag();
    }

    public void refreshAiProgress() {
        fm.refreshAiProgress();
    }

    public void pauseMatch() {
        fm.pauseMatch();
    }

    public void resumeMatch() {
        fm.resumeMatch();
    }

    public FlaggleBlitzEngineBL getEngine() {
        return fm.getEngine();
    }

    public List<CountryBL> getAllCountries() {
        return fm.getAllCountries();
    }
}
