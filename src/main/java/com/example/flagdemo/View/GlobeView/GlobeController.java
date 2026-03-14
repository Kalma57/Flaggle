package com.example.flagdemo.View.GlobeView;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.ViewModel.GlobeVM.GlobeViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/Globe")
public class GlobeController {

    /*
     * FIX: Multi-window session isolation.
     *
     * Previously all games shared a single session key "globeVM".
     * This meant two windows in the same browser shared one game state.
     *
     * The fix: each game gets a UUID on start. That UUID is passed
     * to the HTML page, and the frontend sends it with every request.
     * The controller looks up the correct ViewModel using "globeVM_<gameId>".
     *
     * Two windows -> two different gameIds -> two isolated game states.
     */

    @GetMapping("/start")
    public String startGame(Model model, HttpSession session) throws SQLException {

        // Generate a unique ID for this specific game window
        String gameId = UUID.randomUUID().toString();

        // Create a fresh ViewModel for this game instance
        GlobeViewModel viewModel = new GlobeViewModel();
        viewModel.StartNewGame();

        // Store the ViewModel under a unique key so multiple windows
        // in the same session don't overwrite each other
        session.setAttribute("globeVM_" + gameId, viewModel);

        // Pass gameId to the HTML — the frontend will include it in every request
        model.addAttribute("gameId", gameId);
        model.addAttribute("viewModel", viewModel);

        return "GlobeScreens/GlobeGameScreen";
    }

    @RequestMapping(value = "/guess/ajax", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public GuessResultGlobeBL guessAjax(
            @RequestParam("countryName") String countryName,
            @RequestParam("gameId") String gameId,
            HttpSession session) {
        try {
            // Retrieve the ViewModel that belongs to this specific game window
            GlobeViewModel viewModel =
                    (GlobeViewModel) session.getAttribute("globeVM_" + gameId);

            if (viewModel == null) return null;

            return viewModel.Guess(countryName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/giveup/ajax")
    @ResponseBody
    public Map<String, Object> giveUpAjax(
            @RequestParam("gameId") String gameId,
            HttpSession session) throws SQLException {

        // Retrieve the ViewModel that belongs to this specific game window
        GlobeViewModel viewModel =
                (GlobeViewModel) session.getAttribute("globeVM_" + gameId);

        if (viewModel == null) return Collections.emptyMap();

        int attempts = viewModel.getGm().getAttempts();
        CountryBL targetCountry = viewModel.getTargetCountry();

        String flagPath     = targetCountry.getFlagPath();
        String flagFileName = Paths.get(flagPath).getFileName().toString();

        Map<String, Object> data = new HashMap<>();
        data.put("success",     false);
        data.put("attempts",    attempts);
        data.put("countryName", targetCountry.getName());
        data.put("flagFileName", flagFileName);
        data.put("latitude",    targetCountry.getLatitude());
        data.put("longitude",   targetCountry.getLongitude());

        return data;
    }

    @GetMapping("/countries")
    @ResponseBody
    public List<String> getCountries(
            @RequestParam("gameId") String gameId,
            HttpSession session) throws SQLException {

        // Retrieve the ViewModel that belongs to this specific game window
        GlobeViewModel viewModel =
                (GlobeViewModel) session.getAttribute("globeVM_" + gameId);

        if (viewModel == null) return Collections.emptyList();

        return viewModel.getGm().getAllCountries().stream()
                .filter(this::isValidForGlobe)
                .map(CountryBL::getName)
                .collect(Collectors.toList());
    }

    private boolean isValidForGlobe(CountryBL country) {
        return !(country.getLatitude() == 0.0 && country.getLongitude() == 0.0);
    }
}