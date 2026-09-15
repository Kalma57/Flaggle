package com.example.flagdemo.Model.CapitalGlobeModel;

import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobeEngineBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ServiceLayer.CapitalGlobeSL.CapitalGlobeService;

/**
 * Model layer for the capital-location globe game's regular (casual, untimed) format - wraps
 * the Service layer, mirroring {@link com.example.flagdemo.Model.GlobeModel.GlobeModel}.
 */
public class CapitalGlobeModel implements java.io.Serializable {

    private final CapitalGlobeService service;

    public CapitalGlobeModel(CountryController cc) {
        this.service = new CapitalGlobeService(cc);
    }

    public void startNewGame() {
        service.startNewGame();
    }

    public GuessResultGlobeBL guess(String countryName) {
        return service.guess(countryName);
    }

    public void giveUp() {
        service.giveUp();
    }

    public CapitalGlobeEngineBL getEngine() {
        return service.getEngine();
    }
}
