package com.example.flagdemo.BusinessLayer.GlobeBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
public class GlobeEngineBL implements java.io.Serializable {

    // -------------------- Fields --------------------

    private CountryBL targetCountry;
    private int attempts;
    private boolean gameOver;
    private CountryController cc;
    private int hintsUsed;
    private final Set<Integer> revealedLetterPositions = new HashSet<>();

    // -------------------- Constructor --------------------

    public GlobeEngineBL(CountryController cc) {
        this.attempts = 0;
        this.gameOver = false;
        this.cc = cc;
        this.hintsUsed = 0;
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
        this.hintsUsed = 0;
        this.revealedLetterPositions.clear();
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

    // -------------------- Hints --------------------

    /**
     * Reveals one more letter of the target country's name, at a random not-yet-revealed
     * position (not left-to-right, so an early hint can't give away short names by exposing
     * an easy prefix). Non-letter characters (spaces, hyphens, apostrophes) are always shown.
     *
     * Once every letter has been revealed, the word is fully spelled out —
     * this counts as a loss, since the player never actually guessed it.
     *
     * @return the target name with unrevealed letters masked as '_'
     */
    public String useHint() {
        if (targetCountry == null) {
            return "";
        }

        String name = targetCountry.getName();
        int totalLetters = CountryNameHintUtil.countRevealableLetters(name);

        if (revealedLetterPositions.size() < totalLetters) {
            CountryNameHintUtil.revealRandomLetter(name, revealedLetterPositions);
            hintsUsed++;
        }

        return CountryNameHintUtil.maskName(name, revealedLetterPositions);
    }

    /**
     * Returns the number of hints used so far in the current game.
     */
    public int GetHintsUsed() {
        return hintsUsed;
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