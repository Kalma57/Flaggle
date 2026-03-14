package com.example.flagdemo.ServiceLayer.FlaggleSL;

import com.example.flagdemo.BusinessLayer.FlaggleBL.GameEngineBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;

import java.sql.SQLException;

public class GameService implements java.io.Serializable {
    private GameEngineBL geb;

    public GameService(){
        this.geb = new GameEngineBL();
    }

    /**
     * Starts a new globe game.
     *
     * @throws SQLException if database access fails
     */
    public void StartNewGame() throws SQLException {
        geb.StartNewGame();
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