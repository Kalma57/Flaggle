package com.example.flagdemo.BusinessLayer;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the result of a guess in the "Globe" game mode.
 *
 * In this mode, the player's guess is evaluated based on the
 * geographical distance between the guessed country and the
 * target country.
 *
 * The result includes:
 * - Whether the guess is correct
 * - The distance between the two countries
 * - A proximity level that categorizes how close the guess was
 */
public class GuessResultGlobeBL {

    // ----------------- Fields -----------------

    // Indicates whether the guessed country matches the target country
    private final boolean isCorrect;

    // The country guessed by the player
    private final CountryBL guessedCountry;

    // The correct country the player was supposed to guess
    private final CountryBL targetCountry;

    // Distance between the guessed country and the target country (in kilometers)
    private final float distance;

    // Proximity category based on the calculated distance
    private final ProximityLevel proximityLevel;

    // ----------------- Constants -----------------

    // Earth's average radius in kilometers (used for Haversine distance calculation)
    private static final double EARTH_RADIUS_KM = 6371;

    // ----------------- Constructor -----------------

    /**
     * Creates a GuessResultGlobeBL object and evaluates the guess.
     *
     * The constructor calculates:
     * - Whether the guess is correct
     * - The geographical distance between the two countries
     * - The proximity level based on the calculated distance
     *
     * @param guessedCountry the country guessed by the player
     * @param targetCountry the correct target country
     */
    public GuessResultGlobeBL(CountryBL guessedCountry, CountryBL targetCountry) {

        this.guessedCountry = guessedCountry;
        this.targetCountry = targetCountry;

        // Determine if the guess is exactly correct
        this.isCorrect = guessedCountry.equals(targetCountry);

        // Calculate geographical distance between the two countries
        this.distance = calculateDistance(
                guessedCountry.getLatitude(),
                guessedCountry.getLongitude(),
                targetCountry.getLatitude(),
                targetCountry.getLongitude()
        );

        // Determine the proximity category
        this.proximityLevel = calculateProximityLevel();
    }

    // ----------------- Getters -----------------

    /**
     * Indicates whether the guessed country is correct.
     *
     * @return true if the guessed country equals the target country
     */
    public boolean isCorrect() {
        return isCorrect;
    }

    /**
     * Returns the country guessed by the player.
     *
     * @return guessed country
     */
    public CountryBL getGuessedCountry() {
        return guessedCountry;
    }

    /**
     * Returns the correct target country.
     *
     * @return target country
     */
    public CountryBL getTargetCountry() {
        return targetCountry;
    }

    /**
     * Returns the calculated distance between the guessed country
     * and the target country.
     *
     * @return distance in kilometers
     */
    public float getDistance() {
        return distance;
    }

    /**
     * Returns the proximity level describing how close the guess was.
     *
     * @return proximity level
     */
    public ProximityLevel getProximityLevel() {
        return proximityLevel;
    }

    // ----------------- Core Logic -----------------

    /**
     * Determines the proximity level based on the calculated distance
     * and whether the guessed country is a neighboring country.
     *
     * @return proximity category
     */
    private ProximityLevel calculateProximityLevel() {

        if (isCorrect) {
            return ProximityLevel.CORRECT;
        }

        // If the guessed country borders the target country
        if (isNeighbor()) {
            return ProximityLevel.NEIGHBOR;
        }

        // Distance-based proximity levels
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

    /**
     * Checks whether the guessed country is a direct neighbor
     * of the target country.
     *
     * The neighbors list is stored as a comma-separated list
     * of ISO3 country codes.
     *
     * @return true if the guessed country borders the target country
     */
    private boolean isNeighbor() {

        String neighbors = targetCountry.getNeighborsList();

        if (neighbors == null || neighbors.isEmpty()) {
            return false;
        }

        List<String> neighborCodes = Arrays.stream(neighbors.split(","))
                .map(String::trim)
                .toList();

        return neighborCodes.contains(guessedCountry.getIso3());
    }

    // ----------------- Distance Calculation -----------------

    /**
     * Calculates the geographical distance between two points on Earth
     * using the Haversine formula.
     *
     * The formula accounts for the Earth's spherical shape and provides
     * a reasonably accurate distance between two latitude/longitude points.
     *
     * @param lat1 latitude of the first location
     * @param lon1 longitude of the first location
     * @param lat2 latitude of the second location
     * @param lon2 longitude of the second location
     * @return distance in kilometers
     */
    private float calculateDistance(double lat1, double lon1, double lat2, double lon2) {

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS_KM * c;

        return (float) distance;
    }

    // ----------------- Debug Representation -----------------

    /**
     * Returns a string representation of the guess result,
     * useful for debugging or logging.
     */
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