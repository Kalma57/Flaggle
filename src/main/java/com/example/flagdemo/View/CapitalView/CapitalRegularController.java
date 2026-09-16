package com.example.flagdemo.View.CapitalView;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalOptionsBuilder;
import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuestionOptions;
import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizMode;
import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * View layer for the capital-city quiz's casual "Regular" mode - one question after another,
 * no score, no AI, no time limit; the player just leaves whenever they're done. One controller
 * serves both quiz directions ({@link CapitalQuizMode}), same as {@link CapitalController}.
 *
 * Deliberately stateless: unlike every other Capital format, there's no session-stored engine
 * here at all. A single AJAX call builds one random question via {@link CapitalOptionsBuilder}
 * and returns it, correct answer included - there's no AI or opponent to keep it secret from,
 * so a second round-trip to "submit" an answer would only add latency for no benefit.
 */
@Controller
@RequestMapping("/Capital")
public class CapitalRegularController {

    private final CountryController countryController;

    public CapitalRegularController(CountryController countryController) {
        this.countryController = countryController;
    }

    @GetMapping("/{mode}/regular")
    public String regular(@PathVariable String mode, Model model) {
        CapitalQuizMode quizMode = parseMode(mode);
        if (quizMode == null) return "redirect:/Capital";

        model.addAttribute("mode", mode);
        model.addAttribute("modeLabel", modeLabel(quizMode));
        return "CapitalScreens/CapitalRegularScreen";
    }

    @GetMapping("/{mode}/regular/question/ajax")
    @ResponseBody
    public Map<String, Object> questionAjax(@PathVariable String mode) {
        CapitalQuizMode quizMode = parseMode(mode);
        if (quizMode == null) return Map.of();

        CountryBL target = CapitalOptionsBuilder.pickRandomTarget(countryController);
        CapitalQuestionOptions options = CapitalOptionsBuilder.build(countryController, quizMode, target);

        Map<String, Object> data = new HashMap<>();
        if (quizMode == CapitalQuizMode.FLAG_TO_CAPITAL) {
            data.put("promptText", target.getName());
            data.put("promptFlagPath", target.getFlagPath());
        } else {
            data.put("promptText", target.getCapital());
            data.put("promptFlagPath", null);
        }

        List<Map<String, Object>> optionData = new ArrayList<>();
        List<String> texts = options.getOptionTexts();
        List<CountryBL> optionCountries = options.getOptionCountries();
        for (int i = 0; i < texts.size(); i++) {
            Map<String, Object> opt = new HashMap<>();
            opt.put("text", texts.get(i));
            opt.put("flagPath", quizMode == CapitalQuizMode.CAPITAL_TO_COUNTRY ? optionCountries.get(i).getFlagPath() : null);
            optionData.add(opt);
        }
        data.put("options", optionData);
        data.put("correctOptionIndex", options.getCorrectIndex());

        return data;
    }

    private CapitalQuizMode parseMode(String mode) {
        if ("flag-to-capital".equals(mode)) return CapitalQuizMode.FLAG_TO_CAPITAL;
        if ("capital-to-country".equals(mode)) return CapitalQuizMode.CAPITAL_TO_COUNTRY;
        return null;
    }

    private String modeLabel(CapitalQuizMode mode) {
        return mode == CapitalQuizMode.FLAG_TO_CAPITAL ? "Flag → Capital" : "Capital → Country";
    }
}
