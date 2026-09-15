package com.example.flagdemo.View.CapitalGlobeView;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ViewModel.CapitalGlobeVM.CapitalGlobeViewModel;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * View layer for the capital-location globe game's regular (casual, untimed, single-player)
 * format - mirrors {@link com.example.flagdemo.View.GlobeView.GlobeController} exactly, just
 * showing the target's capital city as the prompt instead of its flag.
 *
 * Uses the same per-window session-isolation pattern as the rest of the app (a UUID gameId
 * stored as "capitalGlobeVM_&lt;gameId&gt;" in the session).
 */
@Controller
@RequestMapping("/Capital/globe")
public class CapitalGlobeController {

    private final CountryController countryController;

    public CapitalGlobeController(CountryController countryController) {
        this.countryController = countryController;
    }

    /** Choose the format: regular, First to N, or Blitz. */
    @GetMapping({"", "/format"})
    public String format() {
        return "CapitalScreens/CapitalGlobeFormatScreen";
    }

    @GetMapping("/start")
    public String start(Model model, HttpSession session) {
        String gameId = UUID.randomUUID().toString();

        CapitalGlobeViewModel viewModel = new CapitalGlobeViewModel(countryController);
        viewModel.startNewGame();

        session.setAttribute("capitalGlobeVM_" + gameId, viewModel);

        model.addAttribute("gameId", gameId);
        model.addAttribute("viewModel", viewModel);

        return "CapitalScreens/CapitalGlobeScreen";
    }

    @RequestMapping(value = "/guess/ajax", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public GuessResultGlobeBL guessAjax(
            @RequestParam("countryName") String countryName,
            @RequestParam("gameId") String gameId,
            HttpSession session) {

        CapitalGlobeViewModel viewModel = (CapitalGlobeViewModel) session.getAttribute("capitalGlobeVM_" + gameId);
        if (viewModel == null) return null;

        return viewModel.guess(countryName);
    }

    @PostMapping("/giveup/ajax")
    @ResponseBody
    public Map<String, Object> giveUpAjax(
            @RequestParam("gameId") String gameId,
            HttpSession session) {

        CapitalGlobeViewModel viewModel = (CapitalGlobeViewModel) session.getAttribute("capitalGlobeVM_" + gameId);
        if (viewModel == null) return Collections.emptyMap();

        viewModel.giveUp();

        CountryBL target = viewModel.getEngine().getTargetCountry();
        String flagFileName = Paths.get(target.getFlagPath()).getFileName().toString();

        Map<String, Object> data = new HashMap<>();
        data.put("success", false);
        data.put("attempts", viewModel.getEngine().getAttempts());
        data.put("countryName", target.getName());
        data.put("flagFileName", flagFileName);
        data.put("latitude", target.getLatitude());
        data.put("longitude", target.getLongitude());
        return data;
    }
}
