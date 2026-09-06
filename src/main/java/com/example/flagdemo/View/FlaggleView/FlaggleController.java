package com.example.flagdemo.View.FlaggleView;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ViewModel.FlaggleVM.FlaggleViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@Controller
@RequestMapping("/Flaggle")
public class FlaggleController {

    // Singleton, shared across all games/requests (loaded once by Spring)
    private final CountryController countryController;

    public FlaggleController(CountryController countryController) {
        this.countryController = countryController;
    }

    /*
     * FIX: Dedicated per-game lobby.
     *
     * /Flaggle used to render the site-wide game hub (StartScreen). Now that
     * the hub lives at "/" (see HomeController), this route is Flaggle's own
     * lobby page: it only shows Flaggle's mode options (Easy/Hard today, more
     * later) and is reachable straight from the hub's Flaggle card.
     */
    @GetMapping({""})
    public String showLobby() {
        return "FlaggleScreens/FlaggleLobbyScreen";
    }

    /*
     * FIX: Multi-window session isolation.
     *
     * Each call to /Flaggle/start generates a unique gameId.
     * The ViewModel is stored under "flaggleVM_<gameId>" instead of
     * the fixed key "flaggleVM", so two windows in the same browser
     * each get their own independent game state.
     *
     * The gameId is passed to the HTML and embedded as a hidden field
     * in every form, so every subsequent request carries it back.
     */
    @GetMapping("/start")
    public String startGame(
            @RequestParam(name = "difficulty", defaultValue = "HARD") DifficultyLevel difficulty,
            Model model,
            HttpSession session) throws SQLException {

        // Generate a unique ID for this specific game window
        String gameId = UUID.randomUUID().toString();

        // Create a fresh ViewModel for this game instance
        FlaggleViewModel viewModel = new FlaggleViewModel(countryController);
        viewModel.StartNewGame(difficulty);

        // Store under a unique key — prevents windows from overwriting each other
        session.setAttribute("flaggleVM_" + gameId, viewModel);

        // Pass gameId to Thymeleaf so it can embed it in the forms
        model.addAttribute("gameId", gameId);
        model.addAttribute("viewModel", viewModel);
        model.addAttribute("difficulty", difficulty);

        return "FlaggleScreens/FlaggleGameScreen";
    }

    @PostMapping("/guess")
    public String guess(
            @RequestParam("countryName") String countryName,
            @RequestParam("gameId") String gameId,        // received from hidden form field
            Model model,
            HttpSession session) throws SQLException, IOException {

        // Retrieve the ViewModel that belongs to this specific game window
        FlaggleViewModel viewModel =
                (FlaggleViewModel) session.getAttribute("flaggleVM_" + gameId);

        viewModel.Guess(countryName);

        CountryBL targetCountry = viewModel.getTargetCountry();

        if (viewModel.isCorrect()) {
            int attempts = viewModel.getAttemps();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(targetCountry.getFlagImage(), "png", baos);
            String countryImage = Base64.getEncoder().encodeToString(baos.toByteArray());

            model.addAttribute("success",      true);
            model.addAttribute("attempts",     attempts);
            model.addAttribute("countryName",  targetCountry.getName());
            model.addAttribute("countryImage", countryImage);
            model.addAttribute("guesses",      buildGuessList(viewModel));
            model.addAttribute("difficulty",   viewModel.getDifficulty());

            // Game is over — release the ViewModel (and its guess/image history) from the session
            session.removeAttribute("flaggleVM_" + gameId);

            return "FlaggleScreens/FlaggleEndScreen";
        }

        model.addAttribute("guesses",   buildGuessList(viewModel));
        model.addAttribute("viewModel", viewModel);
        model.addAttribute("difficulty", viewModel.getDifficulty());

        // Pass gameId back so the next form submission also carries it
        model.addAttribute("gameId", gameId);

        return "FlaggleScreens/FlaggleGameScreen";
    }

    @PostMapping("/giveup")
    public String giveUp(
            @RequestParam("gameId") String gameId,        // received from hidden form field
            Model model,
            HttpSession session) throws SQLException, IOException {

        // Retrieve the ViewModel that belongs to this specific game window
        FlaggleViewModel viewModel =
                (FlaggleViewModel) session.getAttribute("flaggleVM_" + gameId);

        int attempts = viewModel.getAttemps();
        CountryBL targetCountry = viewModel.getTargetCountry();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(targetCountry.getFlagImage(), "png", baos);
        String countryImage = Base64.getEncoder().encodeToString(baos.toByteArray());

        model.addAttribute("success",      false);
        model.addAttribute("attempts",     attempts);
        model.addAttribute("countryName",  targetCountry.getName());
        model.addAttribute("countryImage", countryImage);
        model.addAttribute("guesses",      buildGuessList(viewModel));
        model.addAttribute("difficulty",   viewModel.getDifficulty());

        // Game is over — release the ViewModel (and its guess/image history) from the session
        session.removeAttribute("flaggleVM_" + gameId);

        return "FlaggleScreens/FlaggleEndScreen";
    }

    /**
     * Builds the list of guess data (guessed flag, name, diff image) for rendering,
     * shared by the in-progress game screen and the end screen's guess history.
     */
    private List<Map<String, String>> buildGuessList(FlaggleViewModel viewModel) {
        List<Map<String, String>> guessList = new ArrayList<>();

        for (GuessResultBL gr : viewModel.getGuesses()) {
            Map<String, String> guessData = new HashMap<>();

            // Images are base64-encoded once when the guess is created (GuessResultBL),
            // so re-rendering the guess history doesn't re-decode/re-encode them every request
            guessData.put("guessedImage", gr.getGuessedFlagBase64());
            guessData.put("guessedName",  gr.getGuessedCountry().getName());
            guessData.put("resultImage",  gr.getFlagDifferencesBase64());

            guessList.add(guessData);
        }

        return guessList;
    }
}