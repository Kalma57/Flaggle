package com.example.flagdemo.View.MatchView;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.BusinessLayer.FlaggleBL.DifficultyLevel;
import com.example.flagdemo.BusinessLayer.FlaggleBL.GuessResultBL;
import com.example.flagdemo.BusinessLayer.MatchBL.AiSkillLevel;
import com.example.flagdemo.BusinessLayer.MatchBL.FlaggleMatchEngineBL;
import com.example.flagdemo.BusinessLayer.MatchBL.MatchRoundResult;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.example.flagdemo.ViewModel.MatchVM.FlaggleMatchViewModel;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.util.*;

/**
 * View layer for the 1v1 Flaggle Match mode.
 *
 * "Best of N" (3, 5, or 7) vs an AI opponent. Both sides guess the same flag every round; the human
 * only ever sees their own guesses in full — the opponent (AI) is shown only as a
 * percentage/attempt-count progress indicator (see /status/ajax), never as real flags
 * or country names. This mirrors how a real vs-friend match will work later: no client
 * ever gets to see the other player's actual guesses, only their live progress.
 *
 * Uses the same per-window session-isolation pattern as Flaggle/Globe (a UUID matchId
 * stored as "matchVM_<matchId>" in the session) since, for now, both sides of the match
 * (human + AI) live inside a single browser session.
 */
@Controller
@RequestMapping("/Flaggle/match")
public class FlaggleMatchController {

    private static final Set<Integer> VALID_BEST_OF = Set.of(3, 5, 7);

    private final CountryController countryController;

    public FlaggleMatchController(CountryController countryController) {
        this.countryController = countryController;
    }

    /** Step 1: choose the game mode — "Best of N" or the 1-minute Blitz time-attack. */
    @GetMapping("")
    public String modeSelect() {
        return "FlaggleScreens/FlaggleMatchModeScreen";
    }

    /** Step 2 (Best of N path): choose the match format (Best of 3 / 5 / 7). */
    @GetMapping("/format")
    public String format() {
        return "FlaggleScreens/FlaggleMatchFormatScreen";
    }

    /** Step 3 (Best of N path): choose flag-reveal difficulty + AI opponent skill, now that the format is picked. */
    @GetMapping("/lobby")
    public String lobby(
            @RequestParam(name = "bestOf", defaultValue = "3") int bestOf,
            Model model) {

        bestOf = normalizeBestOf(bestOf);
        model.addAttribute("bestOf", bestOf);
        model.addAttribute("pointsToWin", (bestOf + 1) / 2);

        return "FlaggleScreens/FlaggleMatchLobbyScreen";
    }

    @GetMapping("/start")
    public String start(
            @RequestParam(name = "difficulty", defaultValue = "HARD") DifficultyLevel difficulty,
            @RequestParam(name = "aiLevel", defaultValue = "MEDIUM") AiSkillLevel aiLevel,
            @RequestParam(name = "bestOf", defaultValue = "3") int bestOf,
            Model model,
            HttpSession session) throws SQLException {

        bestOf = normalizeBestOf(bestOf);
        int pointsToWin = (bestOf + 1) / 2;

        String matchId = UUID.randomUUID().toString();

        FlaggleMatchViewModel viewModel = new FlaggleMatchViewModel(countryController, difficulty, aiLevel, pointsToWin);
        viewModel.startMatch();

        session.setAttribute("matchVM_" + matchId, viewModel);

        model.addAttribute("matchId", matchId);
        model.addAttribute("viewModel", viewModel);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("aiLevel", aiLevel);
        model.addAttribute("bestOf", bestOf);

        return "FlaggleScreens/FlaggleMatchScreen";
    }

    private int normalizeBestOf(int bestOf) {
        return VALID_BEST_OF.contains(bestOf) ? bestOf : 3;
    }

    @PostMapping("/guess/ajax")
    @ResponseBody
    public Map<String, Object> guessAjax(
            @RequestParam("matchId") String matchId,
            @RequestParam("countryName") String countryName,
            HttpSession session) {

        FlaggleMatchViewModel viewModel = (FlaggleMatchViewModel) session.getAttribute("matchVM_" + matchId);
        if (viewModel == null) return Collections.emptyMap();

        GuessResultBL result = viewModel.humanGuess(countryName);
        return buildStatePayload(viewModel, result);
    }

    @PostMapping("/giveup/ajax")
    @ResponseBody
    public Map<String, Object> giveUpAjax(
            @RequestParam("matchId") String matchId,
            HttpSession session) {

        FlaggleMatchViewModel viewModel = (FlaggleMatchViewModel) session.getAttribute("matchVM_" + matchId);
        if (viewModel == null) return Collections.emptyMap();

        viewModel.humanGiveUpRound();
        return buildStatePayload(viewModel, null);
    }

    @GetMapping("/status/ajax")
    @ResponseBody
    public Map<String, Object> statusAjax(
            @RequestParam("matchId") String matchId,
            HttpSession session) {

        FlaggleMatchViewModel viewModel = (FlaggleMatchViewModel) session.getAttribute("matchVM_" + matchId);
        if (viewModel == null) return Collections.emptyMap();

        viewModel.refreshAiTimeout();
        return buildStatePayload(viewModel, null);
    }

    @PostMapping("/pause/ajax")
    @ResponseBody
    public Map<String, Object> pauseAjax(
            @RequestParam("matchId") String matchId,
            HttpSession session) {

        FlaggleMatchViewModel viewModel = (FlaggleMatchViewModel) session.getAttribute("matchVM_" + matchId);
        if (viewModel == null) return Collections.emptyMap();

        viewModel.pauseMatch();
        return buildStatePayload(viewModel, null);
    }

    @PostMapping("/resume/ajax")
    @ResponseBody
    public Map<String, Object> resumeAjax(
            @RequestParam("matchId") String matchId,
            HttpSession session) {

        FlaggleMatchViewModel viewModel = (FlaggleMatchViewModel) session.getAttribute("matchVM_" + matchId);
        if (viewModel == null) return Collections.emptyMap();

        viewModel.resumeMatch();
        return buildStatePayload(viewModel, null);
    }

    @PostMapping("/nextRound/ajax")
    @ResponseBody
    public Map<String, Object> nextRoundAjax(
            @RequestParam("matchId") String matchId,
            HttpSession session) throws SQLException {

        FlaggleMatchViewModel viewModel = (FlaggleMatchViewModel) session.getAttribute("matchVM_" + matchId);
        if (viewModel == null) return Collections.emptyMap();

        viewModel.advanceToNextRound();
        return buildStatePayload(viewModel, null);
    }

    /**
     * Builds the JSON payload sent to the frontend after any Match action. Always includes
     * score/round/timing/opponent-progress info; includes the last human guess's images only
     * when a fresh guess was just made, and reveals the target flag/name only once the round
     * (and thus the answer) is actually over.
     */
    private Map<String, Object> buildStatePayload(FlaggleMatchViewModel viewModel, GuessResultBL lastGuess) {
        FlaggleMatchEngineBL engine = viewModel.getEngine();
        Map<String, Object> data = new HashMap<>();

        data.put("humanScore", engine.getHumanScore());
        data.put("aiScore", engine.getAiScore());
        data.put("roundNumber", engine.getRoundNumber());
        data.put("pointsToWin", engine.getPointsToWin());
        data.put("bestOf", engine.getBestOf());
        data.put("humanAttempts", engine.getHumanAttemptsThisRound());
        data.put("matchElapsedSeconds", engine.getMatchElapsedSeconds());
        data.put("roundElapsedSeconds", engine.getRoundElapsedSeconds());
        data.put("paused", engine.isPaused());

        // Opponent progress — percentage/attempt-count only, never real guesses
        double roundElapsed = engine.getRoundElapsedSeconds();
        boolean hasPlan = engine.getAiPlan() != null;
        data.put("aiAttempts", hasPlan ? engine.getAiPlan().getAttemptsSoFar(roundElapsed) : 0);
        data.put("aiProgressPercent", hasPlan ? engine.getAiPlan().getProgressPercent(roundElapsed) : 0);

        boolean roundOver = engine.isRoundOver();
        data.put("roundOver", roundOver);
        data.put("roundWinner", engine.getCurrentRoundWinner().name());
        data.put("matchOver", engine.isMatchOver());
        data.put("matchWinner", engine.getMatchWinner().name());

        if (engine.isMatchOver()) {
            List<Map<String, Object>> history = new ArrayList<>();
            for (MatchRoundResult round : engine.getRoundHistory()) {
                Map<String, Object> roundData = new HashMap<>();
                roundData.put("roundNumber", round.getRoundNumber());
                roundData.put("winner", round.getWinner().name());
                roundData.put("targetCountryName", round.getTargetCountryName());
                roundData.put("humanAttempts", round.getHumanAttempts());
                roundData.put("aiAttempts", round.getAiAttempts());

                CountryBL roundTarget = countryController.getCountryByName(round.getTargetCountryName());
                if (roundTarget != null) {
                    roundData.put("targetFlagBase64", encodeToBase64(roundTarget.getFlagImage()));
                }

                history.add(roundData);
            }
            data.put("roundHistory", history);
        }

        if (lastGuess != null) {
            data.put("guessedName", lastGuess.getGuessedCountry().getName());
            data.put("guessedFlagBase64", lastGuess.getGuessedFlagBase64());
            data.put("resultFlagBase64", lastGuess.getFlagDifferencesBase64());
            data.put("correct", lastGuess.isCorrect());
        }

        if (roundOver) {
            CountryBL target = engine.getCurrentTarget();
            data.put("targetCountryName", target.getName());
            data.put("targetFlagBase64", encodeToBase64(target.getFlagImage()));
        }

        return data;
    }

    private String encodeToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
