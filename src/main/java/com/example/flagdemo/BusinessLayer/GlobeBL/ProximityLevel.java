package com.example.flagdemo.BusinessLayer.GlobeBL;

/**
 * Represents the proximity level between the guessed country
 * and the target country.
 *
 * Each level is associated with a specific color used by the UI.
 */
public enum ProximityLevel {

    CORRECT("#25C267"),     // green

    NEIGHBOR("#8B0000"),    // dark red

    VERY_CLOSE("#B22222"),  // red

    CLOSE("#FF4C4C"),       // light red

    MEDIUM("#FF8C00"),      // orange

    FAR("#FFA733"),         // light orange

    VERY_FAR("#FFD23F"),    // dark yellow

    EXTREME("#FFF4A3");     // light yellow


    private final String colorHex;

    ProximityLevel(String colorHex) {
        this.colorHex = colorHex;
    }

    /**
     * Returns the hex color associated with the proximity level.
     */
    public String getColorHex() {
        return colorHex;
    }
}