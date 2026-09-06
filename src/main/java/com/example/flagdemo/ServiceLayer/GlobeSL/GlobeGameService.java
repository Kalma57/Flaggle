package com.example.flagdemo.ServiceLayer.GlobeSL;

import com.example.flagdemo.BusinessLayer.GlobeBL.GlobeEngineBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.sql.SQLException;

/**
 * Service layer for the Globe game mode.
 *
 * Acts as a bridge between the ViewModel and the GlobeEngineBL.
 */
public class GlobeGameService implements java.io.Serializable {

    // -------------------- Fields --------------------
    private GlobeEngineBL geb;

    // -------------------- Constructor --------------------
    public GlobeGameService(CountryController cc) {
        this.geb = new GlobeEngineBL(cc);
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

    /**
     * Reveals one more letter of the target country's name.
     *
     * @return the target name with unrevealed letters masked as '_'
     */
    public String useHint() {
        return geb.useHint();
    }

    /**
     * Returns the number of hints used so far in the current game.
     */
    public int getHintsUsed() {
        return geb.GetHintsUsed();
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