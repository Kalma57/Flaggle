package com.example.flagdemo.BusinessLayer.DailyQuizBL;

import com.example.flagdemo.BusinessLayer.CapitalGlobeBL.CapitalGlobeTargetPicker;
import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.DataAccessLayer.CountryController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Resolves "today's country" for the Daily Quiz.
 *
 * The real source of truth is a small JSON file a weekly external research
 * agent publishes (web research + the priority/randomness rules agreed on
 * separately - see the routine's own prompt, not this codebase) to the
 * "daily-quiz-data" branch of this repo, fetched here straight from GitHub's
 * raw file host. That branch is dedicated to just this one file and the
 * agent is the only thing that ever pushes to it, so it never interacts with
 * normal app development on develop/main.
 *
 * The fetch is cached for the day (one request per day serves every player,
 * not one per request), and ANY failure - network, missing day, a country
 * name that doesn't match our DB - silently falls back to a deterministic,
 * date-seeded pick from the eligible pool, so the daily quiz always works
 * even if the agent is late, down, or hasn't run yet. {@link DailyQuizDevConfig}
 * lets a caller override the pick entirely for testing, ahead of both paths.
 */
public final class DailyQuizSelectorBL {

    private static final String EVENTS_URL =
            "https://raw.githubusercontent.com/Kalma57/Flaggle/daily-quiz-data/data/daily-quiz-events.json";
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(5);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static volatile LocalDate cachedFetchDate;
    private static volatile JsonNode cachedDays;

    private DailyQuizSelectorBL() {}

    public static DailyQuizSelection getTodaysSelection(CountryController cc, Integer devOverrideCountryId) {
        return getTodaysSelection(cc, devOverrideCountryId, null);
    }

    /**
     * @param devOverrideDate dev-only: pretends "today" is this date instead of the real one,
     *                        so the real agent-feed fetch/resolve path can be exercised against
     *                        a date the agent has actually published for, ahead of the real date
     *                        arriving. Ignored if devOverrideCountryId is also set, or if dev
     *                        mode is off.
     */
    public static DailyQuizSelection getTodaysSelection(CountryController cc, Integer devOverrideCountryId, LocalDate devOverrideDate) {
        LocalDate today = LocalDate.now();

        if (DailyQuizDevConfig.DEV_MODE_ENABLED && devOverrideCountryId != null) {
            CountryBL overridden = cc.getCountryById(devOverrideCountryId);
            if (overridden != null) {
                return new DailyQuizSelection(
                        overridden,
                        "Dev Mode",
                        null,
                        "[DEV MODE] Forced selection for testing: " + overridden.getName() + ".",
                        today,
                        true
                );
            }
        }

        LocalDate effectiveDate = (DailyQuizDevConfig.DEV_MODE_ENABLED && devOverrideDate != null) ? devOverrideDate : today;

        DailyQuizSelection fromAgent = tryAgentSelection(cc, effectiveDate);
        if (fromAgent != null) {
            return fromAgent;
        }

        CountryBL picked = pickDeterministic(cc, effectiveDate);
        return new DailyQuizSelection(
                picked,
                "Today's Pick",
                null,
                "[PLACEHOLDER] The research agent hasn't published today's pick yet - showing a rotating placeholder instead.",
                effectiveDate,
                false
        );
    }

    /** Returns null on anything short of a fully valid, resolvable entry for today - never throws. */
    private static DailyQuizSelection tryAgentSelection(CountryController cc, LocalDate today) {
        try {
            JsonNode days = getDaysMap(today);
            if (days == null) return null;

            JsonNode dayEntry = days.get(today.toString());
            if (dayEntry == null || !dayEntry.hasNonNull("countryName")) return null;

            CountryBL country = cc.getCountryByName(dayEntry.get("countryName").asText());
            if (country == null) return null;

            String eventTitle = dayEntry.hasNonNull("eventTitle") ? dayEntry.get("eventTitle").asText() : "Today's Pick";
            Integer eventYear = dayEntry.hasNonNull("eventYear") ? dayEntry.get("eventYear").asInt() : null;
            String infoText = dayEntry.hasNonNull("infoText") ? dayEntry.get("infoText").asText() : "";
            return new DailyQuizSelection(country, eventTitle, eventYear, infoText, today, false);
        } catch (Exception e) {
            return null;
        }
    }

    /** Fetches and caches the whole "days" object once per calendar day. */
    private static synchronized JsonNode getDaysMap(LocalDate today) throws Exception {
        if (cachedFetchDate != null && cachedFetchDate.equals(today) && cachedDays != null) {
            return cachedDays;
        }

        HttpClient client = HttpClient.newBuilder().connectTimeout(FETCH_TIMEOUT).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(EVENTS_URL))
                .timeout(FETCH_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return cachedDays; // keep serving yesterday's cache rather than nothing, if any
        }

        JsonNode root = MAPPER.readTree(response.body());
        JsonNode days = root.get("days");
        if (days == null || !days.isObject()) {
            return cachedDays;
        }

        cachedDays = days;
        cachedFetchDate = today;
        return cachedDays;
    }

    private static CountryBL pickDeterministic(CountryController cc, LocalDate date) {
        List<CountryBL> pool = CapitalGlobeTargetPicker.eligibleTargets(cc);
        int index = Math.floorMod(date.toString().hashCode(), pool.size());
        return pool.get(index);
    }
}
