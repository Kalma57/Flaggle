package com.example.flagdemo.ServiceLayer;

import com.example.flagdemo.BusinessLayer.GlobeEngineBL;
import com.example.flagdemo.BusinessLayer.GuessResultGlobeBL;

import java.sql.SQLException;

/**
 * Service layer for the Globe game mode.
 *
 * Acts as a bridge between the ViewModel and the GlobeEngineBL.
 */
public class GlobeGameService {

    // -------------------- Fields --------------------
    private GlobeEngineBL geb;

    // -------------------- Constructor --------------------
    public GlobeGameService() {
        this.geb = new GlobeEngineBL();
    }

    // -------------------- Game Control --------------------

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
     * @return GuessResultGlobeBL containing the result of the guess
     * @throws SQLException if database access fails
     */
    public GuessResultGlobeBL Guess(String countryName) throws SQLException {
        return geb.Guess(countryName);
    }

    // -------------------- Getter --------------------

    /**
     * Returns the underlying GlobeEngineBL instance.
     *
     * @return the game engine
     */
    public GlobeEngineBL getEngine() {
        return geb;
    }
}