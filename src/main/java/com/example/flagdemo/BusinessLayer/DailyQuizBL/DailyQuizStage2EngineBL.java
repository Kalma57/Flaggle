package com.example.flagdemo.BusinessLayer.DailyQuizBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Stage 2 of the Daily Quiz: the target country (already revealed from stage
 * 1) is shown highlighted on the globe, and the player has to name every one
 * of its neighbors, typed in one at a time.
 *
 * Countries with no land neighbors (island nations) have an empty
 * {@link CountryBL#getNeighborsList()} - for those, the 3 geographically
 * nearest countries are used instead, computed on the fly with the same
 * Haversine formula already used by {@code GuessResultGlobeBL}. This runs at
 * most once per day's play-through (not per guess), so there is no need to
 * store it as a precomputed column.
 */
public class DailyQuizStage2EngineBL {

    private static final double EARTH_RADIUS_KM = 6371;
    private static final int NEAREST_FALLBACK_COUNT = 3;

    private final CountryBL targetCountry;
    private final List<CountryBL> neighborsToFind;
    private final boolean usedNearestFallback;
    private final Set<CountryBL> found = new LinkedHashSet<>();

    public DailyQuizStage2EngineBL(CountryController cc, CountryBL targetCountry) {
        this.targetCountry = targetCountry;

        String neighborIso3s = targetCountry.getNeighborsList();
        if (neighborIso3s != null && !neighborIso3s.isBlank()) {
            this.neighborsToFind = resolveRealNeighbors(cc, neighborIso3s);
            this.usedNearestFallback = false;
        } else {
            this.neighborsToFind = nearestCountries(cc, targetCountry, NEAREST_FALLBACK_COUNT);
            this.usedNearestFallback = true;
        }
    }

    private static List<CountryBL> resolveRealNeighbors(CountryController cc, String neighborIso3s) {
        Set<String> iso3Codes = Arrays.stream(neighborIso3s.split(","))
                .map(String::trim)
                .collect(java.util.stream.Collectors.toSet());

        List<CountryBL> resolved = new ArrayList<>();
        for (CountryBL c : cc.getAllCountries()) {
            if (iso3Codes.contains(c.getIso3())) {
                resolved.add(c);
            }
        }
        return resolved;
    }

    private static List<CountryBL> nearestCountries(CountryController cc, CountryBL target, int count) {
        return cc.getAllCountries().stream()
                .filter(c -> !c.equals(target))
                .sorted((a, b) -> Double.compare(
                        distanceKm(target, a),
                        distanceKm(target, b)))
                .limit(count)
                .toList();
    }

    private static double distanceKm(CountryBL a, CountryBL b) {
        double latDistance = Math.toRadians(b.getLatitude() - a.getLatitude());
        double lonDistance = Math.toRadians(b.getLongitude() - a.getLongitude());

        double h = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(a.getLatitude()))
                * Math.cos(Math.toRadians(b.getLatitude()))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
        return EARTH_RADIUS_KM * c;
    }

    public DailyQuizNeighborGuessResult guess(String countryName, CountryController cc) {
        CountryBL guessed = cc.getCountryByName(countryName);
        boolean isNeighbor = guessed != null && neighborsToFind.contains(guessed);
        boolean alreadyFound = isNeighbor && found.contains(guessed);

        if (isNeighbor && !alreadyFound) {
            found.add(guessed);
        }

        return new DailyQuizNeighborGuessResult(
                guessed, isNeighbor, alreadyFound,
                neighborsToFind.size() - found.size(),
                neighborsToFind.size(),
                isComplete()
        );
    }

    /** Reveals every neighbor not yet found, and marks the stage complete. */
    public List<CountryBL> giveUp() {
        List<CountryBL> notYetFound = neighborsToFind.stream().filter(c -> !found.contains(c)).toList();
        found.addAll(neighborsToFind);
        return notYetFound;
    }

    public boolean isComplete() {
        return found.size() >= neighborsToFind.size();
    }

    public CountryBL getTargetCountry() { return targetCountry; }
    public List<CountryBL> getNeighborsToFind() { return neighborsToFind; }
    public Set<CountryBL> getFound() { return found; }
    public boolean isUsedNearestFallback() { return usedNearestFallback; }
}
