package com.example.flagdemo.ViewModel.GlobeVM;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.Model.GlobeModel.GlobeModel;

import java.sql.SQLException;
import java.util.List;

/**
 * ViewModel for the Globe game mode.
 *
 * Responsible for connecting the View with the Model.
 * The ViewModel exposes only the data that the View needs.
 */
public class GlobeViewModel {

    // -------------------- Fields --------------------

    private GlobeModel gm;

    // -------------------- Constructor --------------------

    public GlobeViewModel() throws SQLException {
        gm = new GlobeModel();
    }

    // -------------------- Game Control --------------------

    /**
     * Starts a new game and resets previous guesses.
     */
    public void StartNewGame() throws SQLException {
        gm.StartNewGame();
        gm.resetGuesses();
    }

    /**
     * Processes a guess and returns the result.
     *
     * The View will use this result to:
     * - zoom to the guessed country
     * - color the country based on proximity
     *
     * @param guessedCountryName name of the guessed country
     * @return GuessResultGlobeBL containing distance and proximity
     */
    public GuessResultGlobeBL Guess(String guessedCountryName) throws SQLException {
        return gm.Guess(guessedCountryName);
    }

    // -------------------- Getters --------------------

    /**
     * Returns the model instance.
     */
    public GlobeModel getGm() {
        return this.gm;
    }

    /**
     * Returns the number of attempts made.
     */
    public int GetAttempts() {
        return this.gm.getAttempts();
    }

    /**
     * Returns all available countries.
     * Used for autocomplete / search in the UI.
     */
    public List<CountryBL> getAllCountries() {
        return this.gm.getAllCountries();
    }

    /**
     * Returns all guesses made so far.
     */
    public List<GuessResultGlobeBL> getGuesses() {
        return this.gm.getGuesses();
    }

    /**
     * Returns the target country.
     * Mainly used for debugging or testing.
     */
    public CountryBL getTargetCountry() {
        return this.gm.getTargetCountry();
    }

    /**
     * Indicates whether the correct country was guessed.
     */
    public boolean isCorrect() {
        return this.gm.getGs().getEngine().IsGameOver();
    }
}