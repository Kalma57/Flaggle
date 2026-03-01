package ViewModel;

import BusinessLayer.GuessResultBL;
import ServiceLayer.GameService;

import java.awt.image.BufferedImage;
import java.sql.SQLException;

public class FlaggleViewModel {
    private GameService gs;

    public FlaggleViewModel() throws SQLException {
        gs = new GameService();
    }

    public void StartNewGame() throws SQLException {
        gs.StartNewGame();
    }

    public BufferedImage Guess(String guessesCountryName) throws SQLException {
        GuessResultBL gr = gs.Guess(guessesCountryName);
        return gr.getFlagDifferences();
    }
}
