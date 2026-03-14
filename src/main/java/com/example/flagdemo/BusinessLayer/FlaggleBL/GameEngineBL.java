package com.example.flagdemo.BusinessLayer.FlaggleBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.DataAccessLayer.CountryRepository;

import java.sql.SQLException;
import java.util.Random;

public class GameEngineBL implements java.io.Serializable {
        private CountryBL targetCountry;
        private int attempts;
        private boolean gameOver;
        private CountryController cc;

        public GameEngineBL() {
            this.attempts = 0;
            this.gameOver = false;
            this.cc = new CountryController(new CountryRepository());
        }

    /**
     * Starts a new game by selecting a random target country,
     * resetting the number of attempts and the gameOver flag.
     */
    public void StartNewGame() throws SQLException {
        this.targetCountry = selectRandomCountry();
        this.attempts = 0;
        this.gameOver = false;
    }

    /**
     * Processes a guess of a country name and returns the result.
     * Increments the attempt count if the game is not over.
     * Sets gameOver to true if the guess is correct.
     *
     * @param countryName the name of the country being guessed
     * @return a GuessResultBL object representing the guess result,
     *         or null if the game is already over
     */
    public GuessResultBL Guess(String countryName) throws SQLException {
        this.attempts++;

        CountryBL guessedCountry = cc.getCountryByName(countryName);
        GuessResultBL result = new GuessResultBL(guessedCountry, targetCountry);

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
     * @return a randomly selected CountryBL object
     */
    private CountryBL selectRandomCountry() throws SQLException {
        int numOfCountries = cc.getNumberOfAllCountries();
        Random rand = new Random();
        int randomID = rand.nextInt(numOfCountries) + 1;
        CountryBL randomCountry = cc.getCountryById(randomID);
        return randomCountry;
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