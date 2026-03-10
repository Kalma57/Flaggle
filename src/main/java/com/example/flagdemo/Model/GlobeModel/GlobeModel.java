package com.example.flagdemo.Model.GlobeModel;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.ServiceLayer.GlobeSL.GlobeGameService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Model for the Globe game mode.
 *
 * Responsible for:
 * - Managing the game service
 * - Storing the target country
 * - Keeping track of all countries
 * - Storing guess results
 * - Tracking the number of attempts
 */
public class GlobeModel {

    // -------------------- Fields --------------------
    private GlobeGameService gs;
    private CountryBL targetCountry;
    private List<CountryBL> allCountries;
    private List<GuessResultGlobeBL> guesses;
    private int attempts;

    // -------------------- Constructor --------------------
    public GlobeModel() throws SQLException {
        this.gs = new GlobeGameService();
        this.allCountries = gs.getEngine().getCountryController().getAllCountries();
        this.guesses = new ArrayList<>();
        this.attempts = 0;
    }

    // -------------------- Game Control --------------------

    /**
     * Starts a new globe game.
     *
     * @throws SQLException if database access fails
     */
    public void StartNewGame() throws SQLException {
        gs.StartNewGame();
        this.targetCountry = gs.getEngine().getTargetCountry();
        this.guesses.clear();
        this.attempts = 0;
    }

    /**
     * Makes a guess and returns the result.
     *
     * @param guessedCountryName the name of the guessed country
     * @return GuessResultGlobeBL containing the result of the guess
     * @throws SQLException if database access fails
     */
    public GuessResultGlobeBL Guess(String guessedCountryName) throws SQLException {
        GuessResultGlobeBL gr = gs.Guess(guessedCountryName);
        this.attempts = gs.getEngine().GetAttempts();
        this.guesses.add(gr);
        return gr;
    }

    // -------------------- Getters --------------------

    public GlobeGameService getGs() {
        return this.gs;
    }

    public int getAttempts() {
        return this.attempts;
    }

    public List<CountryBL> getAllCountries() {
        return this.allCountries;
    }

    public List<GuessResultGlobeBL> getGuesses() {
        return this.guesses;
    }

    public void resetGuesses() {
        this.guesses = new ArrayList<>();
        this.attempts = 0;
    }

    public CountryBL getTargetCountry() {
        return this.targetCountry;
    }
}