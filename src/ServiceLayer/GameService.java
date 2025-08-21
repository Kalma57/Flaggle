package ServiceLayer;

import BusinessLayer.GameEngineBL;
import BusinessLayer.GuessResultBL;

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