package com.example.flagdemo.ViewModel.CapitalGlobeVM;

import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobeEngineBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.Model.CapitalGlobeModel.CapitalGlobeModel;

/**
 * ViewModel for the capital-location globe game's regular (casual, untimed) format - thin
 * bridge between the Controller and the Model, mirroring {@link com.example.flagdemo.ViewModel.GlobeVM.GlobeViewModel}.
 */
public class CapitalGlobeViewModel implements java.io.Serializable {

    private final CapitalGlobeModel cm;

    public CapitalGlobeViewModel(CountryController cc) {
        this.cm = new CapitalGlobeModel(cc);
    }

    public void startNewGame() {
        cm.startNewGame();
    }

    public GuessResultGlobeBL guess(String countryName) {
        return cm.guess(countryName);
    }

    public void giveUp() {
        cm.giveUp();
    }

    public CapitalGlobeEngineBL getEngine() {
        return cm.getEngine();
    }
}
