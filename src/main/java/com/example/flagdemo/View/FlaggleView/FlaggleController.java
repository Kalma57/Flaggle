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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@Controller
@RequestMapping("/Flaggle")
public class FlaggleController {

    private FlaggleViewModel viewModel;

    public FlaggleController() throws SQLException {
        this.viewModel = new FlaggleViewModel();
    }

    // Start page (remains in the main templates folder)
    @GetMapping({""})
    public String showStartPage() {
        return "StartScreen";
    }

    @GetMapping("/start")
    public String startGame(Model model) throws SQLException {
        viewModel.StartNewGame();
        model.addAttribute("viewModel", viewModel);
        return "FlaggleScreens/FlaggleGameScreen"; // Updated path
    }

    @PostMapping("/guess")
    public String guess(@RequestParam("countryName") String countryName, Model model)
            throws SQLException, IOException {

        // Perform the actual guess
        viewModel.Guess(countryName);

        // ⚡ Check if the user guessed correctly
        CountryBL targetCountry = viewModel.getTargetCountry();

        // ⚡ If guessed correctly — success end screen
        if (viewModel.isCorrect()) {
            int attempts = viewModel.getAttemps();

            // Convert the correct country's flag to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(targetCountry.getFlagImage(), "png", baos);
            String countryImage = Base64.getEncoder().encodeToString(baos.toByteArray());

            // Add information to the model
            model.addAttribute("success", true);
            model.addAttribute("attempts", attempts);
            model.addAttribute("countryName", targetCountry.getName());
            model.addAttribute("countryImage", countryImage);

            return "FlaggleScreens/FlaggleEndScreen"; // Updated path
        }

        // ⚡ If the guess is incorrect — continue as usual

        // Create a list of all guesses made so far
        List<Map<String, String>> guessList = new ArrayList<>();

        for (GuessResultBL gr : viewModel.getGuesses()) {
            Map<String, String> guessData = new HashMap<>();

            // Convert the user's guessed flag to Base64
            BufferedImage guessedFlag = gr.getGuessedCountry().getFlagImage();
            ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
            ImageIO.write(guessedFlag, "png", baos1);
            guessData.put("guessedImage", Base64.getEncoder().encodeToString(baos1.toByteArray()));
            guessData.put("guessedName", gr.getGuessedCountry().getName());

            // Convert the comparison image to Base64
            BufferedImage resultImage = gr.getFlagDifferences();
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            ImageIO.write(resultImage, "png", baos2);
            guessData.put("resultImage", Base64.getEncoder().encodeToString(baos2.toByteArray()));

            guessList.add(guessData);
        }

        // Add to the Model
        model.addAttribute("guesses", guessList);
        model.addAttribute("viewModel", viewModel);

        return "FlaggleScreens/FlaggleGameScreen"; // Updated path
    }

    @PostMapping("/giveup")
    public String giveUp(Model model) throws SQLException, IOException {

        // Number of attempts so far
        int attempts = viewModel.getAttemps();

        // The country that needed to be found
        CountryBL TargetCountry = viewModel.getTargetCountry();

        // Convert the flag to Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(TargetCountry.getFlagImage(), "png", baos);
        String countryImage = Base64.getEncoder().encodeToString(baos.toByteArray());

        // Pass information to the end screen
        model.addAttribute("success", false);
        model.addAttribute("attempts", attempts);
        model.addAttribute("countryName", TargetCountry.getName());
        model.addAttribute("countryImage", countryImage);

        return "FlaggleScreens/FlaggleEndScreen"; // Updated path
    }
}