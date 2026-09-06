package com.example.flagdemo.ServiceLayer.FlaggleSL;

import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GameEngineBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.sql.SQLException;

public class GameService implements java.io.Serializable {
    private GameEngineBL geb;

    public GameService(CountryController cc){
        this.geb = new GameEngineBL(cc);
    }

    /**
     * Starts a new game using {@link DifficultyLevel#HARD} (the original behavior).
     *
     * @throws SQLException if database access fails
     */
    public void StartNewGame() throws SQLException {
        geb.StartNewGame();
    }

    /**
     * Starts a new game with the given difficulty level.
     *
     * @param difficulty the difficulty level to play this round with
     * @throws SQLException if database access fails
     */
    public void StartNewGame(DifficultyLevel difficulty) throws SQLException {
        geb.StartNewGame(difficulty);
    }

    /**
     * Returns the difficulty level of the current game round.
     *
     * @return the current difficulty level
     */
    public DifficultyLevel getDifficulty() {
        return geb.getDifficulty();
    }

    /**
     * Processes a guess and returns the result.
     *
     * @param countryName the name of the guessed country
     * @return GuessResultBL containing the result of the guess
     * @throws SQLException if database access fails
     */
    public GuessResultBL Guess(String countryName) throws SQLException {
        GuessResultBL res = geb.Guess(countryName);
        return res;
    }

    /**
     * Returns the underlying GlobeEngineBL instance.
     *
     * @return the game engine
     */
    public GameEngineBL getEngine(){
        return this.geb;
    }
}