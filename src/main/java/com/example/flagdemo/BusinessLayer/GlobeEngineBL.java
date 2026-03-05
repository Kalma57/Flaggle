package com.example.flagdemo.BusinessLayer;

import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.DataAccessLayer.CountryRepository;

import java.sql.SQLException;
import java.util.Random;

/**
 * Game engine for the Globe game mode.
 *
 * Responsible for:
 * - Selecting a random target country
 * - Processing guesses
 * - Tracking number of attempts
 * - Determining when the game ends
 */
public class GlobeEngineBL {

    // -------------------- Fields --------------------

    private CountryBL targetCountry;
    private int attempts;
    private boolean gameOver;
    private CountryController cc;

    // -------------------- Constructor --------------------

    public GlobeEngineBL() {
        this.attempts = 0;
        this.gameOver = false;
        this.cc = new CountryController(new CountryRepository());
    }

    // -------------------- Game Control --------------------

    /**
     * Starts a new globe game.
     * Selects a random target country and resets the game state.
     */
    public void StartNewGame() throws SQLException {
        this.targetCountry = SelectRandomCountry();
        this.attempts = 0;
        this.gameOver = false;
    }

    /**
     * Processes a guess and returns the result of the guess.
     *
     * @param countryName the name of the guessed country
     * @return GuessResultGlobeBL containing the result of the guess
     */
    public GuessResultGlobeBL Guess(String countryName) throws SQLException {

        if (gameOver) {
            return null;
        }

        // Increase the attempt count
        this.attempts++;

        // Retrieve the guessed country from the database
        CountryBL guessedCountry = cc.getCountryByName(countryName);

        // Create the guess result object
        GuessResultGlobeBL result = new GuessResultGlobeBL(guessedCountry, targetCountry);

        // If the guess is correct, the game ends
        if (result.isCorrect()) {
            this.gameOver = true;
        }

        return result;
    }

    /**
     * Returns whether the game is over.
     */
    public boolean IsGameOver() {
        return gameOver;
    }

    /**
     * Returns the number of attempts made in the current game.
     */
    public int GetAttempts() {
        return attempts;
    }

    // -------------------- Country Selection --------------------

    /**
     * Selects a random country from the database.
     */
    public CountryBL SelectRandomCountry() throws SQLException {

        int numOfCountries = cc.getNumberOfAllCountries();

        Random rand = new Random();

        int randomID = rand.nextInt(numOfCountries) + 1;

        return cc.getCountryById(randomID);
    }

    // -------------------- Getters / Setters --------------------

    /**
     * Returns the target country of the current game.
     */
    public CountryBL getTargetCountry() {
        return targetCountry;
    }

    /**
     * Sets the target country manually.
     * This is mainly used for testing purposes.
     */
    public void setTargetCountry(CountryBL targetCountry) {
        this.targetCountry = targetCountry;
    }

    public CountryController getCountryController(){
        return cc;
    }

}