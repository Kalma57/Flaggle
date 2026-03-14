package com.example.flagdemo.View.FlaggleView;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.ViewModel.FlaggleVM.FlaggleViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@Controller
@RequestMapping("/Flaggle")
public class FlaggleController {

    @GetMapping({""})
    public String showStartPage() {
        return "StartScreen";
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
    public String startGame(Model model, HttpSession session) throws SQLException {

        // Generate a unique ID for this specific game window
        String gameId = UUID.randomUUID().toString();

        // Create a fresh ViewModel for this game instance
        FlaggleViewModel viewModel = new FlaggleViewModel();
        viewModel.StartNewGame();

        // Store under a unique key — prevents windows from overwriting each other
        session.setAttribute("flaggleVM_" + gameId, viewModel);

        // Pass gameId to Thymeleaf so it can embed it in the forms
        model.addAttribute("gameId", gameId);
        model.addAttribute("viewModel", viewModel);

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

            return "FlaggleScreens/FlaggleEndScreen";
        }

        List<Map<String, String>> guessList = new ArrayList<>();

        for (GuessResultBL gr : viewModel.getGuesses()) {
            Map<String, String> guessData = new HashMap<>();

            BufferedImage guessedFlag = gr.getGuessedCountry().getFlagImage();
            ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
            ImageIO.write(guessedFlag, "png", baos1);
            guessData.put("guessedImage", Base64.getEncoder().encodeToString(baos1.toByteArray()));
            guessData.put("guessedName",  gr.getGuessedCountry().getName());

            BufferedImage resultImage = gr.getFlagDifferences();
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            ImageIO.write(resultImage, "png", baos2);
            guessData.put("resultImage", Base64.getEncoder().encodeToString(baos2.toByteArray()));

            guessList.add(guessData);
        }

        model.addAttribute("guesses",   guessList);
        model.addAttribute("viewModel", viewModel);

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

        return "FlaggleScreens/FlaggleEndScreen";
    }
}