package com.example.flagdemo.ServiceLayer.FlaggleSL;

import com.example.flagdemo.BusinessLayer.FlaggleBL.GameEngineBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;

import java.sql.SQLException;

public class GameService {
    private GameEngineBL geb;

    public GameService(){
        this.geb = new GameEngineBL();
    }

    public void StartNewGame() throws SQLException {
        geb.StartNewGame();
    }

    public GuessResultBL Guess(String countryName) throws SQLException {
        GuessResultBL res = geb.Guess(countryName);
        return res;
    }

    public GameEngineBL getEngine(){
        return this.geb;
    }
}