package com.example.flagdemo.Model.FlaggleModel;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.ServiceLayer.FlaggleSL.GameService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The FlaggleModel class represents the core game state in the MVVM architecture.
 * It communicates with the Service Layer (GameService) and stores the game data
 * needed by the ViewModel.
 *
 * Responsibilities:
 * - Managing the current game state
 * - Holding the target country
 * - Storing all available countries
 * - Tracking the player's guesses
 * - Tracking the number of attempts
 */
public class FlaggleModel {

    /** Service layer responsible for game logic */
    private GameService gs;

    /** The country the player needs to guess */
    private CountryBL targetCountry;

    /** List of all countries available in the game */
    private List<CountryBL> allCountries;

    /** List of guesses the player has made so far */
    private List<GuessResultBL> guesses;

    /** Number of attempts made by the player */
    private int attemps;

    /**
     * Constructor.
     * Initializes the GameService and loads all countries from the database.
     *
     * @throws SQLException if there is a problem accessing the database
     */
    public FlaggleModel() throws SQLException {
        gs = new GameService();
        allCountries = gs.getEngine().getCountryController().getAllCountries();
        this.allCountries = gs.getEngine().getCc().getAllCountries();
        this.guesses = new ArrayList<>();
    }

    /**
     * Starts a new game by resetting the engine and selecting a new target country.
     *
     * @throws SQLException if there is a database error
     */
    public void StartNewGame() throws SQLException {
        gs.StartNewGame();
        this.targetCountry = gs.getEngine().getTargetCountry();
    }

    /**
     * Performs a guess for the given country name.
     * The guess is processed by the Service Layer and the result is stored.
     *
     * @param guessesCountryName the country guessed by the player
     * @return GuessResultBL object containing the result of the guess
     * @throws SQLException if a database error occurs
     */
    public GuessResultBL Guess(String guessesCountryName) throws SQLException {
        GuessResultBL gr = gs.Guess(guessesCountryName);
        this.attemps = gs.getEngine().GetAttempts();
        this.guesses.add(gr);
        return gr;
    }

    /**
     * Returns the GameService instance.
     *
     * @return GameService object
     */
    public GameService getGs(){
        return this.gs;
    }

    /**
     * Returns the number of attempts made by the player.
     *
     * @return number of attempts
     */
    public int getAttemps(){
        return this.attemps;
    }

    /**
     * Returns a list of all countries available in the game.
     *
     * @return list of CountryBL objects
     */
    public List<CountryBL> getAllCountries() {
        return allCountries;
    }

    /**
     * Returns the list of guesses made by the player.
     *
     * @return list of GuessResultBL objects
     */
    public List<GuessResultBL> getGuesses() {
        return this.guesses;
    }

    /**
     * Clears all previous guesses.
     * Used when starting a new game.
     */
    public void resetGuesses(){
        guesses = new ArrayList<>();
    }

    /**
     * Returns the target country that the player needs to guess.
     *
     * @return CountryBL representing the target country
     */
    public CountryBL getTargetCountry() {
        return targetCountry;
    }
}