package com.example.flagdemo.ServiceLayer.CapitalGlobeSL;

import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobeEngineBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

/**
 * Service layer for the capital-location globe game's regular (casual, untimed) format - pure
 * delegation to the engine, mirroring {@link com.example.flagdemo.ServiceLayer.GlobeSL.GlobeGameService}.
 */
public class CapitalGlobeService implements java.io.Serializable {

    private final CapitalGlobeEngineBL engine;

    public CapitalGlobeService(CountryController cc) {
        this.engine = new CapitalGlobeEngineBL(cc);
    }

    public void startNewGame() {
        engine.startNewGame();
    }

    public GuessResultGlobeBL guess(String countryName) {
        return engine.guess(countryName);
    }

    public void giveUp() {
        engine.giveUp();
    }

    public CapitalGlobeEngineBL getEngine() {
        return engine;
    }
}
