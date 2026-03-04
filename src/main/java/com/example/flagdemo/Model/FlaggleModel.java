package com.example.flagdemo.Model;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GuessResultBL;
import com.example.flagdemo.ServiceLayer.GameService;

import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FlaggleModel {
    private GameService gs;
    private CountryBL targetCountry;
    private List<CountryBL> allCountries;
    private  List<GuessResultBL> guesses;
    private int attemps;

    public FlaggleModel() throws SQLException {
        gs = new GameService();
        allCountries = gs.getEngine().getCountryController().getAllCountries();
        this.allCountries = gs.getEngine().getCc().getAllCountries();
        this.guesses = new ArrayList<>();
    }

    public void StartNewGame() throws SQLException {
        gs.StartNewGame();
        this.targetCountry = gs.getEngine().getTargetCountry();
    }

    public GuessResultBL Guess(String guessesCountryName) throws SQLException {
        GuessResultBL gr = gs.Guess(guessesCountryName);
        this.attemps = gs.getEngine().GetAttempts();
        this.guesses.add(gr);
        return gr;
    }

    public GameService getGs(){
        return this.gs;
    }

    public int getAttemps(){
        return this.attemps;
    }

    public List<CountryBL> getAllCountries() {
        return allCountries;
    }

    public List<GuessResultBL> getGuesses() {
        return this.guesses;
    }

    public void resetGuesses(){
        guesses = new ArrayList<>();
    }

    public CountryBL getTargetCountry() {
        return targetCountry;
    }
}
