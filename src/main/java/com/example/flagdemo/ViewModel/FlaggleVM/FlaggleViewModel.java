package com.example.flagdemo.ViewModel.FlaggleVM;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.Model.FlaggleModel.FlaggleModel;

import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.List;

/**
 * ViewModel layer of the Flaggle game.
 *
 * This class acts as a bridge between the View (UI) and the Model layer.
 * It exposes methods that the UI can call while hiding the internal
 * business logic and database interactions.
 */
public class FlaggleViewModel {

    /** Main model object that contains the game logic and state */
    private FlaggleModel fm;

    /**
     * Constructor that initializes the Flaggle model.
     *
     * @throws SQLException if there is a problem accessing the database
     */
    public FlaggleViewModel() throws SQLException {
        fm = new FlaggleModel();
    }

    /**
     * Starts a new game.
     *
     * This resets the current guesses and generates a new target country.
     *
     * @throws SQLException if a database error occurs
     */
    public void StartNewGame() throws SQLException {
        fm.StartNewGame();
        fm.resetGuesses();
    }

    /**
     * Performs a guess in the game.
     *
     * @param guessesCountryName the name of the country guessed by the user
     * @return a BufferedImage representing the visual differences between
     *         the guessed country and the target country
     * @throws SQLException if a database error occurs
     */
    public BufferedImage Guess(String guessesCountryName) throws SQLException {
        GuessResultBL gr = fm.Guess(guessesCountryName);
        return gr.getFlagDifferences();
    }

    /**
     * Returns the current model instance.
     *
     * @return the FlaggleModel object
     */
    public FlaggleModel getFm(){
        return this.fm;
    }

    /**
     * Returns the number of attempts made by the player.
     *
     * @return number of attempts
     */
    public int getAttemps(){
        return this.fm.getAttemps();
    }

    /**
     * Returns a list of all available countries in the game.
     *
     * @return list of CountryBL objects
     */
    public List<CountryBL> getAllCountries() {
        return this.fm.getAllCountries();
    }

    /**
     * Returns all guesses made so far in the current game.
     *
     * @return list of GuessResultBL objects representing previous guesses
     */
    public List<GuessResultBL> getGuesses() {
        return this.fm.getGuesses();
    }

    /**
     * Returns the target country of the current game.
     *
     * @return the CountryBL object representing the correct answer
     */
    public CountryBL getTargetCountry(){
        return this.fm.getTargetCountry();
    }

    /**
     * Checks if the game has ended.
     *
     * @return true if the correct country was guessed, otherwise false
     */
    public boolean isCorrect(){
        return this.fm.getGs().getEngine().IsGameOver();
    }
}