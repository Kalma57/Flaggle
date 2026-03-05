package com.example.flagdemo.BusinessLayer;

/**
 * Represents the proximity level between the guessed country
 * and the target country in the globe guessing game.
 */
public enum ProximityLevel {

    CORRECT,        // The guessed country is exactly the target country

    NEIGHBOR,       // The guessed country shares a border with the target country

    VERY_CLOSE,     // Distance is very small (approximately < 500 km)

    CLOSE,          // Close distance (approximately < 1500 km)

    MEDIUM,         // Medium distance (approximately < 3000 km)

    FAR,            // Far distance (approximately < 6000 km)

    VERY_FAR,       // Very far (approximately < 9000 km)

    EXTREME         // Extremely far distance
}