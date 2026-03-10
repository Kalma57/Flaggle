package com.example.flagdemo.View.GlobeView;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.ViewModel.GlobeVM.GlobeViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/Globe")
public class GlobeController {

    private GlobeViewModel viewModel;

    public GlobeController() throws SQLException {
        this.viewModel = new GlobeViewModel();
    }

    // ------------------ Start Game ------------------
    @GetMapping("/start")
    public String startGame(Model model) throws SQLException {
        viewModel.StartNewGame();
        model.addAttribute("viewModel", viewModel);
        return "GlobeScreens/GlobeGameScreen";
    }

    // ------------------ Handle Guess (AJAX / JSON) ------------------
    @RequestMapping(value = "/guess/ajax", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public GuessResultGlobeBL guessAjax(@RequestParam("countryName") String countryName) {
        try {
            return viewModel.Guess(countryName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ------------------ Give Up (AJAX / JSON) ------------------
    @PostMapping("/giveup/ajax")
    @ResponseBody
    public Map<String,Object> giveUpAjax() throws SQLException {

        int attempts = viewModel.getGm().getAttempts();
        CountryBL targetCountry = viewModel.getTargetCountry();

        String flagPath = targetCountry.getFlagPath();
        String flagFileName = Paths.get(flagPath).getFileName().toString();

        Map<String,Object> data = new HashMap<>();
        data.put("success", false);
        data.put("attempts", attempts);
        data.put("countryName", targetCountry.getName());
        data.put("flagFileName", flagFileName);
        data.put("latitude", targetCountry.getLatitude());
        data.put("longitude", targetCountry.getLongitude());

        return data;
    }

    // ------------------ Get All Countries (Filtered for Globe) ------------------
    @GetMapping("/countries")
    @ResponseBody
    public List<String> getCountries() throws SQLException {
        return viewModel.getGm().getAllCountries().stream()
                .filter(this::isValidForGlobe)
                .map(CountryBL::getName)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to determine if a country has proper data for the Globe game.
     * Excludes countries with missing coordinates (0.0).
     */
    private boolean isValidForGlobe(CountryBL country) {
        if (country.getLatitude() == 0.0 && country.getLongitude() == 0.0) {
            return false;
        }
        return true;
    }
}