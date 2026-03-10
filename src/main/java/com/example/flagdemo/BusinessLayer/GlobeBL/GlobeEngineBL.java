package com.example.flagdemo.BusinessLayer.GlobeBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.DataAccessLayer.CountryRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Game engine for the Globe game mode.
 *
 * Responsible for:
 * - Selecting a random target country (Filtered for valid globe data)
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
     * Selects a random valid target country and resets the game state.
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

    // -------------------- Country Selection & Filtering --------------------

    /**
     * Selects a random country from the database, ensuring it has valid data for the Globe.
     */
    public CountryBL SelectRandomCountry() throws SQLException {

        // 1. Fetch all countries from DB
        List<CountryBL> allCountries = cc.getAllCountries();

        // 2. Filter out countries with missing coordinates
        List<CountryBL> validCountries = allCountries.stream()
                .filter(this::isValidForGlobe)
                .collect(Collectors.toList());

        // 3. Pick a random country from the valid list
        Random rand = new Random();
        int randomIndex = rand.nextInt(validCountries.size());

        return validCountries.get(randomIndex);
    }

    /**
     * Helper method to determine if a country has proper data for the Globe game.
     * Excludes countries with missing coordinates (0.0).
     */
    private boolean isValidForGlobe(CountryBL country) {
        if (country == null) return false;
        // If both Lat and Lon are exactly 0.0, it means data is missing.
        if (country.getLatitude() == 0.0 && country.getLongitude() == 0.0) {
            return false;
        }
        return true;
    }

    /**
     * Retrieves all countries from the database.
     * Required by the GlobeController to populate the dropdown list.
     */
    public List<CountryBL> getAllCountries() throws SQLException {
        return cc.getAllCountries();
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