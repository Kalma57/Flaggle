package com.example.flagdemo;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GuessResultBL;
import com.example.flagdemo.ViewModel.FlaggleViewModel;
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

    private com.example.flagdemo.ViewModel.FlaggleViewModel viewModel;

    public FlaggleController() throws SQLException {
        this.viewModel = new FlaggleViewModel();
    }

    // דף פתיחה
    @GetMapping({""})
    public String showStartPage() {
        return "StartScreen";
    }

    @GetMapping("/start")
    public String startGame(Model model) throws SQLException {
        viewModel.StartNewGame();
        model.addAttribute("viewModel", viewModel);
        return "FlaggleGameScreen";
    }

    @PostMapping("/guess")
    public String guess(@RequestParam("countryName") String countryName, Model model)
            throws SQLException, IOException {

        // ביצוע הניחוש בפועל
        viewModel.Guess(countryName);

        // ⚡ בדיקה אם המשתמש ניחש נכון
        CountryBL targetCountry = viewModel.getTargetCountry();

        // ⚡ אם ניחש נכון — מסך סיום הצלחה
        if (viewModel.isCorrect()) {
            int attempts = viewModel.getAttemps();

            // המרה ל-Base64 של דגל המדינה הנכונה
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(targetCountry.getFlagImage(), "png", baos);
            String countryImage = Base64.getEncoder().encodeToString(baos.toByteArray());

            // הוספת מידע ל-model
            model.addAttribute("success", true);
            model.addAttribute("attempts", attempts);
            model.addAttribute("countryName", targetCountry.getName());
            model.addAttribute("countryImage", countryImage);

            return "FlaggleEndScreen";
        }

        // ⚡ אם הניחוש לא נכון — נמשיך כרגיל

        // יצירת רשימה של כל הניחושים שהיו עד כה
        List<Map<String, String>> guessList = new ArrayList<>();

        for (GuessResultBL gr : viewModel.getGuesses()) {
            Map<String, String> guessData = new HashMap<>();

            // המרה ל-Base64 של הדגל שניחש המשתמש
            BufferedImage guessedFlag = gr.getGuessedCountry().getFlagImage();
            ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
            ImageIO.write(guessedFlag, "png", baos1);
            guessData.put("guessedImage", Base64.getEncoder().encodeToString(baos1.toByteArray()));
            guessData.put("guessedName", gr.getGuessedCountry().getName());

            // המרה ל-Base64 של תמונת ההשוואה
            BufferedImage resultImage = gr.getFlagDifferences();
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            ImageIO.write(resultImage, "png", baos2);
            guessData.put("resultImage", Base64.getEncoder().encodeToString(baos2.toByteArray()));

            guessList.add(guessData);
        }

        // מוסיפים ל-Model
        model.addAttribute("guesses", guessList);
        model.addAttribute("viewModel", viewModel);

        return "FlaggleGameScreen";
    }

    @PostMapping("/giveup")
    public String giveUp(Model model) throws SQLException, IOException {

        // מספר הנסיונות עד כה
        int attempts = viewModel.getAttemps();

        // המדינה שהייתה צריך למצוא
        CountryBL TargetCountry = viewModel.getTargetCountry();

        // המרה ל-Base64 של הדגל
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(TargetCountry.getFlagImage(), "png", baos);
        String countryImage = Base64.getEncoder().encodeToString(baos.toByteArray());

        // העברת מידע למסך הסיום
        model.addAttribute("success", false);
        model.addAttribute("attempts", attempts);
        model.addAttribute("countryName", TargetCountry.getName());
        model.addAttribute("countryImage", countryImage);

        return "FlaggleEndScreen";
    }
}
