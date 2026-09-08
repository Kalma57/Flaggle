package com.example.flagdemo.BusinessLayer.FlaggleBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

public class GameEngineBL implements java.io.Serializable {
        private CountryBL targetCountry;
        private int attempts;
        private boolean gameOver;
        private CountryController cc;
        private DifficultyLevel difficulty;

        // EASY-mode only: the accumulated reveal image built up across every guess
        // made this round. Reset to null whenever a new game starts. Unused on HARD.
        private BufferedImage revealedFlag;

        public GameEngineBL(CountryController cc) {
            this.attempts = 0;
            this.gameOver = false;
            this.cc = cc;
        }

    /**
     * Starts a new game using {@link DifficultyLevel#HARD} (the original behavior).
     */
    public void StartNewGame() throws SQLException {
        StartNewGame(DifficultyLevel.HARD);
    }

    /**
     * Starts a new game by selecting a random target country,
     * resetting the number of attempts and the gameOver flag,
     * and locking in the difficulty level for this game round.
     *
     * @param difficulty the difficulty level to play this round with
     */
    public void StartNewGame(DifficultyLevel difficulty) throws SQLException {
        this.targetCountry = selectRandomCountry();
        this.attempts = 0;
        this.gameOver = false;
        this.difficulty = difficulty;
        // Reset the EASY-mode accumulated reveal for the new round
        this.revealedFlag = null;
    }

    /**
     * Returns the difficulty level of the current game round.
     * Defaults to HARD if it was never set (e.g. a game started before
     * this feature existed).
     *
     * @return the current difficulty level
     */
    public DifficultyLevel getDifficulty() {
        return difficulty != null ? difficulty : DifficultyLevel.HARD;
    }

    /**
     * Processes a guess of a country name and returns the result.
     * Increments the attempt count if the game is not over.
     * Sets gameOver to true if the guess is correct.
     *
     * On EASY, the returned result builds on top of every previous guess made
     * this round (see {@link #revealedFlag}); on HARD, each guess is evaluated
     * independently, exactly as before EASY mode existed.
     *
     * @param countryName the name of the country being guessed
     * @return a GuessResultBL object representing the guess result,
     *         or null if the game is already over
     */
    public GuessResultBL Guess(String countryName) throws SQLException {
        this.attempts++;

        CountryBL guessedCountry = cc.getCountryByName(countryName);
        GuessResultBL result;

        if (getDifficulty() == DifficultyLevel.EASY) {
            result = new GuessResultBL(guessedCountry, targetCountry, DifficultyLevel.EASY, revealedFlag);
            // Carry the newly-updated accumulated reveal forward to the next guess
            this.revealedFlag = result.getFlagDifferences();
        } else {
            result = new GuessResultBL(guessedCountry, targetCountry, DifficultyLevel.HARD);
        }

        if (result.isCorrect()) {
            this.gameOver = true;
        }

        return result;
    }

    /**
     * Checks whether the game is over.
     *
     * @return true if the game has ended, false otherwise
     */
    public boolean IsGameOver() {
        return gameOver;
    }

    /**
     * Returns the number of attempts made so far.
     *
     * @return the count of guesses made
     */
    public int GetAttempts() {
        return attempts;
    }

    public CountryController getCountryController(){
        return cc;
    }
    /**
     * Selects a random country from the available countries list.
     *
     * Picks directly from the loaded country list rather than a random ID in
     * [1, count]: DB row IDs are not guaranteed to be a contiguous 1..count range
     * (e.g. duplicate-flag territories have been removed from the table, leaving
     * gaps), so looking up a random ID could silently return null and crash —
     * mirrors the same fix already applied to the Best-of-N and Blitz match engines.
     *
     * @return a randomly selected CountryBL object
     */
    private CountryBL selectRandomCountry() throws SQLException {
        List<CountryBL> allCountries = cc.getAllCountries();
        Random rand = new Random();
        return allCountries.get(rand.nextInt(allCountries.size()));
    }

    public CountryBL getTargetCountry(){
        return this.targetCountry;
    }

    public void setTargetCountry(CountryBL cb){
        this.targetCountry = cb;
    }
    public CountryController getCc(){
        return this.cc;
    }

}