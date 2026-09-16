package com.example.flagdemo.View.CapitalView;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizEngineBL;
import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizMode;
import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ViewModel.CapitalVM.CapitalQuizViewModel;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * View layer for the capital-city quiz "Best of N" mode - one controller serves both quiz
 * directions ({@link CapitalQuizMode}), mirroring {@link com.example.flagdemo.View.GlobeMatchView.GlobeMatchController}
 * in overall shape, but with a {@code mode} path segment instead of a separate class per
 * direction, since the two directions share the exact same screen/flow.
 *
 * "First to 3/5/7" are all supported (race to that many net-correct answers - see
 * {@link CapitalQuizEngineBL}), plus 1/2-minute Blitz (see
 * {@link com.example.flagdemo.View.CapitalView.CapitalBlitzController}).
 */
@Controller
@RequestMapping("/Capital")
public class CapitalController {

    private static final Set<Integer> VALID_TARGETS = Set.of(3, 5, 7);

    private final CountryController countryController;

    public CapitalController(CountryController countryController) {
        this.countryController = countryController;
    }

    /** Hub: choose which of the two quiz directions to play. */
    @GetMapping("")
    public String hub() {
        return "CapitalScreens/CapitalHubScreen";
    }

    /** Top-level hub for this quiz direction: Regular, 1v1 vs Computer, or 1v1 vs Friend. */
    @GetMapping("/{mode}/format")
    public String format(@PathVariable String mode, Model model) {
        CapitalQuizMode quizMode = parseMode(mode);
        if (quizMode == null) return "redirect:/Capital";

        model.addAttribute("mode", mode);
        model.addAttribute("modeLabel", modeLabel(quizMode));
        return "CapitalScreens/CapitalFormatScreen";
    }

    /** The "1v1 vs Computer" bucket: First to N or Blitz, then pick an AI level. */
    @GetMapping("/{mode}/com/format")
    public String comFormat(@PathVariable String mode, Model model) {
        CapitalQuizMode quizMode = parseMode(mode);
        if (quizMode == null) return "redirect:/Capital";

        model.addAttribute("mode", mode);
        model.addAttribute("modeLabel", modeLabel(quizMode));
        return "CapitalScreens/CapitalComFormatScreen";
    }

    /** Step 2 of 2 ("First to N" path): choose the AI's skill level, now that the format is picked. */
    @GetMapping("/{mode}/lobby")
    public String lobby(
            @PathVariable String mode,
            @RequestParam(name = "target", defaultValue = "3") int target,
            Model model) {

        CapitalQuizMode quizMode = parseMode(mode);
        if (quizMode == null) return "redirect:/Capital";

        target = normalizeTarget(target);

        model.addAttribute("mode", mode);
        model.addAttribute("modeLabel", modeLabel(quizMode));
        model.addAttribute("target", target);
        return "CapitalScreens/CapitalLobbyScreen";
    }

    @GetMapping("/{mode}/start")
    public String start(
            @PathVariable String mode,
            @RequestParam(name = "aiLevel", defaultValue = "MEDIUM") AiSkillLevel aiLevel,
            @RequestParam(name = "target", defaultValue = "3") int target,
            Model model,
            HttpSession session) {

        CapitalQuizMode quizMode = parseMode(mode);
        if (quizMode == null) return "redirect:/Capital";

        target = normalizeTarget(target);

        String matchId = UUID.randomUUID().toString();

        CapitalQuizViewModel viewModel = new CapitalQuizViewModel(countryController, quizMode, aiLevel, target);
        viewModel.startMatch();

        session.setAttribute("capitalQuizVM_" + matchId, viewModel);

        model.addAttribute("matchId", matchId);
        model.addAttribute("mode", mode);
        model.addAttribute("modeLabel", modeLabel(quizMode));
        model.addAttribute("aiLevel", aiLevel);
        model.addAttribute("target", target);

        return "CapitalScreens/CapitalMatchScreen";
    }

    private int normalizeTarget(int target) {
        return VALID_TARGETS.contains(target) ? target : 3;
    }

    private CapitalQuizMode parseMode(String mode) {
        if ("flag-to-capital".equals(mode)) return CapitalQuizMode.FLAG_TO_CAPITAL;
        if ("capital-to-country".equals(mode)) return CapitalQuizMode.CAPITAL_TO_COUNTRY;
        return null;
    }

    private String modeLabel(CapitalQuizMode mode) {
        return mode == CapitalQuizMode.FLAG_TO_CAPITAL ? "Flag → Capital" : "Capital → Country";
    }

    @PostMapping("/{mode}/answer/ajax")
    @ResponseBody
    public Map<String, Object> answerAjax(
            @PathVariable String mode,
            @RequestParam("matchId") String matchId,
            @RequestParam("optionIndex") int optionIndex,
            HttpSession session) {

        CapitalQuizViewModel viewModel = (CapitalQuizViewModel) session.getAttribute("capitalQuizVM_" + matchId);
        if (viewModel == null) return Collections.emptyMap();

        viewModel.submitAnswer(optionIndex);
        return buildStatePayload(viewModel);
    }

    @GetMapping("/{mode}/status/ajax")
    @ResponseBody
    public Map<String, Object> statusAjax(
            @PathVariable String mode,
            @RequestParam("matchId") String matchId,
            HttpSession session) {

        CapitalQuizViewModel viewModel = (CapitalQuizViewModel) session.getAttribute("capitalQuizVM_" + matchId);
        if (viewModel == null) return Collections.emptyMap();

        return buildStatePayload(viewModel);
    }

    @PostMapping("/{mode}/pause/ajax")
    @ResponseBody
    public Map<String, Object> pauseAjax(
            @PathVariable String mode,
            @RequestParam("matchId") String matchId,
            HttpSession session) {

        CapitalQuizViewModel viewModel = (CapitalQuizViewModel) session.getAttribute("capitalQuizVM_" + matchId);
        if (viewModel == null) return Collections.emptyMap();

        viewModel.pauseMatch();
        return buildStatePayload(viewModel);
    }

    @PostMapping("/{mode}/resume/ajax")
    @ResponseBody
    public Map<String, Object> resumeAjax(
            @PathVariable String mode,
            @RequestParam("matchId") String matchId,
            HttpSession session) {

        CapitalQuizViewModel viewModel = (CapitalQuizViewModel) session.getAttribute("capitalQuizVM_" + matchId);
        if (viewModel == null) return Collections.emptyMap();

        viewModel.resumeMatch();
        return buildStatePayload(viewModel);
    }

    @PostMapping("/{mode}/nextRound/ajax")
    @ResponseBody
    public Map<String, Object> nextRoundAjax(
            @PathVariable String mode,
            @RequestParam("matchId") String matchId,
            HttpSession session) {

        CapitalQuizViewModel viewModel = (CapitalQuizViewModel) session.getAttribute("capitalQuizVM_" + matchId);
        if (viewModel == null) return Collections.emptyMap();

        viewModel.advanceToNextRound();
        return buildStatePayload(viewModel);
    }

    /**
     * Builds the JSON payload sent to the frontend after any action. The correct option's
     * index is only ever revealed once the round is actually over - never while it's live.
     */
    private Map<String, Object> buildStatePayload(CapitalQuizViewModel viewModel) {
        CapitalQuizEngineBL engine = viewModel.getEngine();
        Map<String, Object> data = new HashMap<>();

        data.put("mode", engine.getMode().name());
        data.put("humanScore", engine.getHumanScore());
        data.put("aiScore", engine.getAiScore());
        data.put("roundNumber", engine.getRoundNumber());
        data.put("pointsToWin", engine.getPointsToWin());
        data.put("matchElapsedSeconds", engine.getMatchElapsedSeconds());
        data.put("roundElapsedSeconds", engine.getRoundElapsedSeconds());
        data.put("paused", engine.isPaused());
        data.put("aiAnswered", engine.isAiAnswered());

        CountryBL target = engine.getCurrentTarget();
        if (target != null) {
            if (engine.getMode() == CapitalQuizMode.FLAG_TO_CAPITAL) {
                data.put("promptText", target.getName());
                data.put("promptFlagPath", target.getFlagPath());
            } else {
                data.put("promptText", target.getCapital());
                data.put("promptFlagPath", null);
            }
        }

        List<String> options = engine.getCurrentOptions();
        List<CountryBL> optionCountries = engine.getCurrentOptionCountries();
        if (options != null) {
            List<Map<String, Object>> optionData = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) {
                Map<String, Object> opt = new HashMap<>();
                opt.put("text", options.get(i));
                opt.put("flagPath", engine.getMode() == CapitalQuizMode.CAPITAL_TO_COUNTRY ? optionCountries.get(i).getFlagPath() : null);
                optionData.add(opt);
            }
            data.put("options", optionData);
        }

        boolean roundOver = engine.isRoundOver();
        data.put("roundOver", roundOver);
        data.put("humanCorrectThisRound", engine.isLastHumanCorrect());
        data.put("aiCorrectThisRound", engine.isLastAiCorrect());
        data.put("correctOptionIndex", roundOver ? engine.getCorrectOptionIndex() : -1);
        data.put("humanPickedIndex", engine.getHumanPickedIndex());
        data.put("matchOver", engine.isMatchOver());
        data.put("matchWinner", engine.getMatchWinner().name());

        if (engine.isMatchOver()) {
            List<Map<String, Object>> history = new ArrayList<>();
            engine.getRoundHistory().forEach(r -> {
                Map<String, Object> roundData = new HashMap<>();
                roundData.put("roundNumber", r.getRoundNumber());
                roundData.put("humanCorrect", r.isHumanCorrect());
                roundData.put("aiCorrect", r.isAiCorrect());
                roundData.put("promptText", r.getPromptText());
                roundData.put("correctAnswerText", r.getCorrectAnswerText());
                roundData.put("humanPickedText", r.getHumanPickedText());
                roundData.put("targetFlagPath", r.getTargetFlagPath());
                roundData.put("humanTimeSeconds", r.getHumanTimeSeconds());
                roundData.put("aiTimeSeconds", r.getAiTimeSeconds());
                history.add(roundData);
            });
            data.put("roundHistory", history);
        }

        return data;
    }
}
