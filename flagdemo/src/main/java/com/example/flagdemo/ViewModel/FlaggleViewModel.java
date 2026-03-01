package com.example.flagdemo.ViewModel;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GuessResultBL;
import com.example.flagdemo.Model.FlaggleModel;
import com.example.flagdemo.ServiceLayer.GameService;

import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.List;

public class FlaggleViewModel {
    private FlaggleModel fm;

    public FlaggleViewModel() throws SQLException {
        fm = new FlaggleModel();
    }

    public void StartNewGame() throws SQLException {
        fm.StartNewGame();
        fm.resetGuesses();
    }

    public BufferedImage Guess(String guessesCountryName) throws SQLException {
        GuessResultBL gr = fm.Guess(guessesCountryName);
        return gr.getFlagDifferences();
    }

    public FlaggleModel getFm(){
        return this.fm;
    }
    public int getAttemps(){
        return this.fm.getAttemps();
    }

    public List<CountryBL> getAllCountries() {
        return this.fm.getAllCountries();
    }

    public List<GuessResultBL> getGuesses() {
        return this.fm.getGuesses();
    }

    public CountryBL getTargetCountry(){
        return this.fm.getTargetCountry();
    }

    public boolean isCorrect(){
        return this.fm.getGs().getEngine().IsGameOver();
    }
}
