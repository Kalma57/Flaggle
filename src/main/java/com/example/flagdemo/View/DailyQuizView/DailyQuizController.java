package com.example.flagdemo.View.DailyQuizView;

import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalOptionsBuilder;
import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuestionOptions;
import com.example.flagdemo.BusinessLayer.CapitalBL.CapitalQuizMode;
import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.DailyQuizBL.DailyQuizDevConfig;
import com.example.flagdemo.BusinessLayer.DailyQuizBL.DailyQuizNeighborGuessResult;
import com.example.flagdemo.BusinessLayer.DailyQuizBL.DailyQuizPlayedGuardBL;
import com.example.flagdemo.BusinessLayer.DailyQuizBL.DailyQuizSelection;
import com.example.flagdemo.BusinessLayer.DailyQuizBL.DailyQuizSelectorBL;
import com.example.flagdemo.BusinessLayer.DailyQuizBL.DailyQuizStage2EngineBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.GlobeBL.GuessResultGlobeBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ViewModel.FlaggleVM.FlaggleViewModel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * View layer for the Daily Quiz. The whole play-through lives on one page
 * (see DailyQuizPlayScreen.html) that reveals each stage in place rather than
 * navigating - stage 1 reuses the Flaggle guess/diff mechanic pinned to
 * today's country (see DailyQuizSelectorBL), stage 2 shows that country
 * highlighted on the globe and has the player name every neighbor (see
 * DailyQuizStage2EngineBL), stage 3 is a 4-option capital pick reusing
 * {@link CapitalOptionsBuilder} exactly as-is (the target is already known
 * by this point, so no new options logic was needed), and stage 4 shows the
 * research agent's own write-up of why this country was picked (see
 * DailyQuizSelectorBL/DailyQuizSelection). Like Capital's own Regular mode,
 * stage 3's correct index - and stage 4's whole content - ships up front
 * alongside the stage 3 question, and grading/reveal happens client-side -
 * there's nothing at stake worth a second server round trip for (stage 3's
 * pick itself is the one exception - see /complete).
 *
 * gameId is deterministic ("daily-" + the selection's date), not a random
 * UUID, specifically so the session attributes below are addressable again
 * on a later visit the same day - that is the entire resume mechanism: no
 * database involved, just reusing whatever the session already has in
 * memory. It naturally stops working the same way the once-per-day cookie's
 * enforcement does - a different browser, an expired session, or the server
 * restarting all lose it - see DailyQuizPlayedGuardBL's own note.
 */
@Controller
@RequestMapping("/DailyQuiz")
public class DailyQuizController {

    private final CountryController countryController;

    public DailyQuizController(CountryController countryController) {
        this.countryController = countryController;
    }

    /**
     * Entry point. Three outcomes:
     *  - brand new attempt today: creates fresh state, marks the cookie, renders stage 1.
     *  - an attempt is already in progress in THIS session (resumeStage's session
     *    key holds view/engine objects, no /complete call yet): renders the exact
     *    same page, but with resuming=true so the client fetches /resume and jumps
     *    back to wherever it left off instead of starting stage 1 over.
     *  - already fully completed, or the cookie says played but this session has
     *    no memory of it (expired session / different browser): the recap screen.
     */
    @GetMapping("/start")
    public String start(@RequestParam(required = false) Integer devCountryId,
                         @RequestParam(required = false) String devDate,
                         Model model,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         HttpSession session) throws SQLException {

        LocalDate parsedDevDate = devDate != null ? LocalDate.parse(devDate) : null;
        DailyQuizSelection selection = DailyQuizSelectorBL.getTodaysSelection(countryController, devCountryId, parsedDevDate);
        String gameId = "daily-" + selection.getDate();

        if (Boolean.TRUE.equals(session.getAttribute("dailyQuizCompleted_" + gameId))) {
            addAlreadyPlayedModel(model, devCountryId, parsedDevDate);
            return "DailyQuizScreens/DailyQuizAlreadyPlayedScreen";
        }

        FlaggleViewModel viewModel = (FlaggleViewModel) session.getAttribute("dailyQuizFlaggleVM_" + gameId);
        boolean resuming = viewModel != null;

        if (!resuming) {
            if (DailyQuizPlayedGuardBL.hasPlayedToday(request)) {
                addAlreadyPlayedModel(model, devCountryId, parsedDevDate);
                return "DailyQuizScreens/DailyQuizAlreadyPlayedScreen";
            }

            viewModel = new FlaggleViewModel(countryController);
            viewModel.StartNewGame(DifficultyLevel.HARD);
            viewModel.setTargetCountry(selection.getCountry());
            session.setAttribute("dailyQuizFlaggleVM_" + gameId, viewModel);
            session.setAttribute("dailyQuizSelection_" + gameId, selection);

            // Marked the instant today's attempt begins, not once some later stage
            // completes - so leaving mid-quiz and coming back resumes in place
            // (or, once the session itself is gone, lands on the recap screen)
            // rather than silently starting a second attempt over.
            DailyQuizPlayedGuardBL.markPlayedToday(response);
        }

        model.addAttribute("gameId", gameId);
        model.addAttribute("viewModel", viewModel);
        model.addAttribute("resuming", resuming);
        return "DailyQuizScreens/DailyQuizPlayScreen";
    }

    /**
     * Everything the client needs to jump straight back to wherever this
     * session's attempt left off, fetched on load when start() said resuming=true.
     * Stage 1's guess history replays exactly (FlaggleViewModel already keeps it
     * in order); stage 2 can only report which neighbors are found so far, not
     * the original click-by-click order, since the engine only tracks a Set -
     * good enough to resume progress correctly, just not a frame-perfect replay.
     * Stage 3, if reached, is shown unanswered again - it was never graded
     * server-side in the first place (see the class doc), so there is nothing
     * to restore there beyond the question itself.
     */
    @GetMapping("/resume")
    @ResponseBody
    public Map<String, Object> resume(@RequestParam("gameId") String gameId, HttpSession session) {
        FlaggleViewModel viewModel = (FlaggleViewModel) session.getAttribute("dailyQuizFlaggleVM_" + gameId);
        Map<String, Object> result = new LinkedHashMap<>();
        if (viewModel == null) {
            result.put("resumeStage", 0);
            return result;
        }

        CountryBL target = viewModel.getTargetCountry();
        result.put("targetCountry", countryPayload(target));
        result.put("stage1Guesses", viewModel.getGuesses().stream().map(g -> {
            Map<String, Object> guessPayload = new LinkedHashMap<>();
            guessPayload.put("guessedName", g.getGuessedCountry().getName());
            guessPayload.put("guessedFlagBase64", g.getGuessedFlagBase64());
            guessPayload.put("resultFlagBase64", g.getFlagDifferencesBase64());
            return guessPayload;
        }).toList());

        DailyQuizStage2EngineBL stage2 = (DailyQuizStage2EngineBL) session.getAttribute("dailyQuizStage2Engine_" + gameId);
        if (stage2 == null) {
            result.put("resumeStage", 1);
            return result;
        }

        result.put("stage1Success", viewModel.isCorrect());
        result.put("neighborTotalCount", stage2.getNeighborsToFind().size());
        result.put("usedNearestFallback", stage2.isUsedNearestFallback());
        result.put("stage2Found", stage2.getFound().stream().map(c -> {
            Map<String, Object> foundPayload = countryPayload(c);
            GuessResultGlobeBL proximity = new GuessResultGlobeBL(c, target);
            foundPayload.put("colorHex", proximity.getColorHex());
            foundPayload.put("distance", proximity.getDistance());
            return foundPayload;
        }).toList());

        if (!stage2.isComplete()) {
            result.put("resumeStage", 2);
            return result;
        }

        result.put("resumeStage", 3);
        result.putAll(beginStage3(target, gameId, session));
        return result;
    }

    /**
     * Marks today's attempt as fully finished - called once stage 3 is
     * answered (right or wrong, grading is client-side either way, see the
     * class doc). Without this, the server has no way to tell "finished" apart
     * from "still somewhere in stage 3", since nothing about stage 3 or 4 was
     * ever reported back before this endpoint existed.
     */
    @PostMapping("/complete")
    @ResponseBody
    public Map<String, Object> complete(@RequestParam("gameId") String gameId, HttpSession session) {
        session.setAttribute("dailyQuizCompleted_" + gameId, true);
        return Map.of("ok", true);
    }

    /** Populates the recap screen shown on any revisit once today's attempt has begun. */
    private void addAlreadyPlayedModel(Model model, Integer devCountryId, LocalDate devDate) throws SQLException {
        DailyQuizSelection selection = DailyQuizSelectorBL.getTodaysSelection(countryController, devCountryId, devDate);
        CountryBL country = selection.getCountry();
        model.addAttribute("countryName", country.getName());
        model.addAttribute("countryFlagPath", country.getFlagPath());
        model.addAttribute("countryCapital", country.getCapital());
        model.addAttribute("eventTitle", selection.getEventTitle());
        model.addAttribute("infoText", selection.getInfoText());
        if (selection.getEventYear() != null) {
            model.addAttribute("yearsAgo", selection.getDate().getYear() - selection.getEventYear());
        }
    }

    // ------------------------------------------------------------- Stage 1

    @PostMapping("/guess")
    @ResponseBody
    public Map<String, Object> guess(@RequestParam("countryName") String countryName,
                                      @RequestParam("gameId") String gameId,
                                      HttpSession session) throws SQLException {

        FlaggleViewModel viewModel = (FlaggleViewModel) session.getAttribute("dailyQuizFlaggleVM_" + gameId);
        viewModel.Guess(countryName);

        List<GuessResultBL> guesses = viewModel.getGuesses();
        GuessResultBL last = guesses.get(guesses.size() - 1);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("correct", last.isCorrect());
        result.put("attempts", viewModel.getAttemps());
        result.put("guessedName", last.getGuessedCountry().getName());
        result.put("guessedFlagBase64", last.getGuessedFlagBase64());
        result.put("resultFlagBase64", last.getFlagDifferencesBase64());

        if (last.isCorrect()) {
            result.put("stage1Complete", true);
            result.putAll(beginStage2(viewModel.getTargetCountry(), gameId, session));
        } else {
            result.put("stage1Complete", false);
        }
        return result;
    }

    @PostMapping("/giveup")
    @ResponseBody
    public Map<String, Object> giveUp(@RequestParam("gameId") String gameId,
                                       HttpSession session) {

        FlaggleViewModel viewModel = (FlaggleViewModel) session.getAttribute("dailyQuizFlaggleVM_" + gameId);
        CountryBL target = viewModel.getTargetCountry();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attempts", viewModel.getAttemps());
        result.put("stage1Complete", true);
        result.putAll(beginStage2(target, gameId, session));
        return result;
    }

    /** Creates the stage-2 engine and returns the payload the client needs to reveal it. */
    private Map<String, Object> beginStage2(CountryBL target, String gameId, HttpSession session) {
        DailyQuizStage2EngineBL stage2 = new DailyQuizStage2EngineBL(countryController, target);
        session.setAttribute("dailyQuizStage2Engine_" + gameId, stage2);

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> targetPayload = new LinkedHashMap<>();
        targetPayload.put("name", target.getName());
        targetPayload.put("flagPath", target.getFlagPath());
        targetPayload.put("latitude", target.getLatitude());
        targetPayload.put("longitude", target.getLongitude());
        payload.put("targetCountry", targetPayload);
        payload.put("neighborTotalCount", stage2.getNeighborsToFind().size());
        payload.put("usedNearestFallback", stage2.isUsedNearestFallback());
        return payload;
    }

    // ------------------------------------------------------------- Stage 2

    @PostMapping("/stage2/guess")
    @ResponseBody
    public Map<String, Object> stage2Guess(@RequestParam("countryName") String countryName,
                                            @RequestParam("gameId") String gameId,
                                            HttpSession session) {

        DailyQuizStage2EngineBL stage2 = (DailyQuizStage2EngineBL) session.getAttribute("dailyQuizStage2Engine_" + gameId);
        DailyQuizNeighborGuessResult guessResult = stage2.guess(countryName, countryController);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isNeighbor", guessResult.isNeighbor());
        result.put("alreadyFound", guessResult.isAlreadyFound());
        result.put("remainingCount", guessResult.getRemainingCount());
        result.put("totalCount", guessResult.getTotalCount());
        result.put("stageComplete", guessResult.isStageComplete());

        if (guessResult.getGuessedCountry() != null) {
            Map<String, Object> guessedPayload = countryPayload(guessResult.getGuessedCountry());
            // Every guess gets colored on the globe by real distance to the target,
            // exactly like the regular Globe game - not just a binary neighbor/not,
            // so a near-miss still reads as a near-miss.
            GuessResultGlobeBL proximity = new GuessResultGlobeBL(guessResult.getGuessedCountry(), stage2.getTargetCountry());
            guessedPayload.put("colorHex", proximity.getColorHex());
            guessedPayload.put("distance", proximity.getDistance());
            result.put("guessedCountry", guessedPayload);
        }

        if (guessResult.isStageComplete()) {
            result.putAll(beginStage3(stage2.getTargetCountry(), gameId, session));
        }
        return result;
    }

    @PostMapping("/stage2/giveup")
    @ResponseBody
    public Map<String, Object> stage2GiveUp(@RequestParam("gameId") String gameId,
                                             HttpSession session) {

        DailyQuizStage2EngineBL stage2 = (DailyQuizStage2EngineBL) session.getAttribute("dailyQuizStage2Engine_" + gameId);
        List<CountryBL> revealed = stage2.giveUp();

        // Same shape as a guess result (flag + distance/color) so the client
        // can drop these straight into the same guesses list as real guesses.
        List<Map<String, Object>> revealedPayload = revealed.stream().map(c -> {
            Map<String, Object> payload = countryPayload(c);
            GuessResultGlobeBL proximity = new GuessResultGlobeBL(c, stage2.getTargetCountry());
            payload.put("colorHex", proximity.getColorHex());
            payload.put("distance", proximity.getDistance());
            return payload;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("revealed", revealedPayload);
        result.put("stageComplete", true);

        result.putAll(beginStage3(stage2.getTargetCountry(), gameId, session));
        return result;
    }

    private Map<String, Object> countryPayload(CountryBL c) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", c.getName());
        payload.put("flagPath", c.getFlagPath());
        payload.put("latitude", c.getLatitude());
        payload.put("longitude", c.getLongitude());
        return payload;
    }

    // ------------------------------------------------------------- Stage 3

    /**
     * Builds the 4-option capital question for the already-known target -
     * same option-building rules as Capital's FLAG_TO_CAPITAL mode, just
     * skipping the flag reveal since the country is already on screen. Also
     * attaches stage 4's content (the "why this country" info text) up
     * front, same reasoning as stage 3 itself: nothing here needs grading or
     * hiding, so there is no reason to make the client wait for a second
     * round trip just to reveal it once stage 3 is answered.
     */
    private Map<String, Object> beginStage3(CountryBL target, String gameId, HttpSession session) {
        CapitalQuestionOptions options = CapitalOptionsBuilder.build(countryController, CapitalQuizMode.FLAG_TO_CAPITAL, target);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stage3OptionTexts", options.getOptionTexts());
        payload.put("stage3CorrectIndex", options.getCorrectIndex());
        payload.put("stage4CountryName", target.getName());
        payload.put("stage4CountryFlagPath", target.getFlagPath());
        payload.put("stage4Capital", target.getCapital());

        DailyQuizSelection selection = (DailyQuizSelection) session.getAttribute("dailyQuizSelection_" + gameId);
        if (selection != null) {
            payload.put("stage4EventTitle", selection.getEventTitle());
            payload.put("stage4InfoText", selection.getInfoText());
            if (selection.getEventYear() != null) {
                payload.put("stage4YearsAgo", selection.getDate().getYear() - selection.getEventYear());
            }
        }
        return payload;
    }

    // ---------------------------------------------------------------------
    // Debug surface for the selection layer - see DailyQuizSelectorBL/
    // DailyQuizDevConfig. Stays inert once DEV_MODE_ENABLED is false.
    // ---------------------------------------------------------------------

    @GetMapping("/debug/today")
    @ResponseBody
    public Map<String, Object> debugToday(@RequestParam(required = false) Integer devCountryId,
                                           @RequestParam(required = false) String devDate,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("devModeEnabled", DailyQuizDevConfig.DEV_MODE_ENABLED);

        boolean alreadyPlayed = DailyQuizPlayedGuardBL.hasPlayedToday(request);
        result.put("alreadyPlayedToday", alreadyPlayed);
        if (alreadyPlayed) {
            return result;
        }

        LocalDate parsedDevDate = devDate != null ? LocalDate.parse(devDate) : null;
        DailyQuizSelection selection = DailyQuizSelectorBL.getTodaysSelection(countryController, devCountryId, parsedDevDate);
        result.put("countryId", selection.getCountry().getCode());
        result.put("countryName", selection.getCountry().getName());
        result.put("capital", selection.getCountry().getCapital());
        result.put("eventTitle", selection.getEventTitle());
        result.put("eventYear", selection.getEventYear());
        result.put("infoText", selection.getInfoText());
        result.put("date", selection.getDate().toString());
        result.put("devOverrideUsed", selection.isDevOverride());

        DailyQuizPlayedGuardBL.markPlayedToday(response);
        return result;
    }

    @PostMapping("/debug/reset")
    @ResponseBody
    public Map<String, Object> debugReset(HttpServletResponse response) {
        DailyQuizPlayedGuardBL.clearPlayed(response);
        return Map.of("reset", true);
    }
}
