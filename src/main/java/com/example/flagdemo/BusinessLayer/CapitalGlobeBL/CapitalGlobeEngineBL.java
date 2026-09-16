package com.example.flagdemo.BusinessLayer.CapitalGlobeBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

/**
 * Game engine for the plain/casual, untimed, single-player capital-location globe game
 * (concept 3, "regular" format) - mirrors {@link com.example.flagdemo.BusinessLayer.GlobeBL.GlobeEngineBL}
 * (pick a random target, click-to-select-and-confirm guessing via {@link GuessResultGlobeBL},
 * attempts), just showing the target's capital city as the prompt instead of its flag, and
 * picking targets via {@link CapitalGlobeTargetPicker} (needs a real capital on record, not
 * just real coordinates). No hints here - the country is meant to be found on the globe itself.
 */
public class CapitalGlobeEngineBL implements java.io.Serializable {

    private final CountryController cc;

    private CountryBL targetCountry;
    private int attempts;
    private boolean gameOver;

    public CapitalGlobeEngineBL(CountryController cc) {
        this.cc = cc;
    }

    public void startNewGame() {
        this.targetCountry = CapitalGlobeTargetPicker.pickRandomTarget(cc);
        this.attempts = 0;
        this.gameOver = false;
    }

    public GuessResultGlobeBL guess(String countryName) {
        if (gameOver) return null;

        // A click can land on a polygon from the map dataset that has no matching row in our
        // own DB (the two are independent sources - odd little territories, disputed regions,
        // etc.) - treat that exactly like a click that missed the globe entirely: a no-op,
        // not a wasted attempt.
        CountryBL guessedCountry = cc.getCountryByName(countryName);
        if (guessedCountry == null) return null;

        this.attempts++;
        GuessResultGlobeBL result = new GuessResultGlobeBL(guessedCountry, targetCountry);

        if (result.isCorrect()) {
            this.gameOver = true;
        }

        return result;
    }

    public void giveUp() {
        this.gameOver = true;
    }

    // -------------------- Getters --------------------

    public boolean isGameOver() { return gameOver; }
    public int getAttempts() { return attempts; }
    public CountryBL getTargetCountry() { return targetCountry; }
}
