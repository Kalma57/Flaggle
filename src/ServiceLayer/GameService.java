package ServiceLayer;

import BusinessLayer.GameEngineBL;
import BusinessLayer.GuessResultBL;

public class GameService {
    private GameEngineBL geb;

    public GameService(){
        this.geb = new GameEngineBL();
    }

    public void StartNewGame(){
        geb.StartNewGame();
    }

    public GuessResultBL Guess(String countryName){
        GuessResultBL res = geb.Guess(countryName);
        return res;
    }
}