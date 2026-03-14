package com.example.flagdemo.BusinessLayer.GlobeBL;

import java.util.Arrays;
import java.util.List;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a guess in the "Globe" game mode.
 */
public class GuessResultGlobeBL implements java.io.Serializable {

    // ----------------- Fields -----------------

    private final boolean isCorrect;
    private final CountryBL guessedCountry;
    private final CountryBL targetCountry;
    private final float distance;
    private final ProximityLevel proximityLevel;

    // ----------------- Constants -----------------

    private static final double EARTH_RADIUS_KM = 6371;

    // ----------------- Constructor -----------------

    public GuessResultGlobeBL(CountryBL guessedCountry, CountryBL targetCountry) {
        this.guessedCountry = guessedCountry;
        this.targetCountry = targetCountry;

        this.isCorrect = guessedCountry.equals(targetCountry);

        this.distance = calculateDistance(
                guessedCountry.getLatitude(),
                guessedCountry.getLongitude(),
                targetCountry.getLatitude(),
                targetCountry.getLongitude()
        );

        this.proximityLevel = calculateProximityLevel();
    }

    // ----------------- Getters -----------------

    public boolean isCorrect() {
        return isCorrect;
    }

    public CountryBL getGuessedCountry() {
        return guessedCountry;
    }

    public CountryBL getTargetCountry() {
        return targetCountry;
    }

    public ProximityLevel getProximityLevel() {
        return proximityLevel;
    }

    /**
     * Pulls the color directly from the Enum to the JSON payload,
     * so the Frontend doesn't have to map it manually.
     */
    @JsonProperty("colorHex")
    public String getColorHex() {
        return proximityLevel != null ? proximityLevel.getColorHex() : "#D3D3D3";
    }

    @JsonProperty("distance")
    public float getDistance() {
        return distance;
    }

    @JsonProperty("isCorrect")
    public boolean getIsCorrect() {
        return isCorrect;
    }

    // ----------------- Core Logic -----------------

    private ProximityLevel calculateProximityLevel() {
        if (isCorrect) {
            return ProximityLevel.CORRECT;
        }
        if (isNeighbor()) {
            return ProximityLevel.NEIGHBOR;
        }
        if (distance < 500) {
            return ProximityLevel.VERY_CLOSE;
        }
        if (distance < 1500) {
            return ProximityLevel.CLOSE;
        }
        if (distance < 3000) {
            return ProximityLevel.MEDIUM;
        }
        if (distance < 6000) {
            return ProximityLevel.FAR;
        }
        if (distance < 9000) {
            return ProximityLevel.VERY_FAR;
        }
        return ProximityLevel.EXTREME;
    }

    // ----------------- Neighbor Detection -----------------

    private boolean isNeighbor() {
        String neighbors = targetCountry.getNeighborsList();

        if (neighbors == null || neighbors.isEmpty()) {
            return false;
        }

        List<String> neighborCodes = Arrays.stream(neighbors.split(","))
                .map(String::trim)
                .toList();

        // Directly checks the String ISO3 representation
        return neighborCodes.contains(guessedCountry.getIso3());
    }

    // ----------------- Distance Calculation -----------------

    private float calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (EARTH_RADIUS_KM * c);
    }

    // ----------------- Debug Representation -----------------

    @Override
    public String toString() {
        return "GuessResultGlobe{" +
                "correct=" + isCorrect +
                ", guessedCountry=" + guessedCountry.getName() +
                ", targetCountry=" + targetCountry.getName() +
                ", distance=" + distance +
                ", proximityLevel=" + proximityLevel +
                '}';
    }
}