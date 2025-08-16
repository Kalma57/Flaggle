package BusinessLayer;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;


public class GuessResultBL {
    private boolean correct;
    private CountryBL guessedCountry;
    private CountryBL targetCountry;
    private Map<String, Boolean> flagDifferences;

    public GuessResultBL(CountryBL guessedCountry, CountryBL targetCountry) {
        this.guessedCountry = guessedCountry;
        this.targetCountry = targetCountry;

        this.correct = guessedCountry.equals(targetCountry);

        this.flagDifferences = CalculateFlagDifferences(guessedCountry, targetCountry);
    }

    /**
     * Calculates a map of differences between the flags of two countries.
     * The map contains pixel positions as keys ("x,y") and a boolean value indicating
     * whether the pixels overlap (are identical).
     *
     * @param guessed the guessed country's flag
     * @param target the target country's flag
     * @return a map with pixel positions and a boolean indicating pixel overlap
     */
    private Map<String, Boolean> CalculateFlagDifferences(CountryBL guessed, CountryBL target) {
        Map<String, Boolean> differencesMap = new HashMap<>();

        BufferedImage guessedFlag = guessed.getFlagImage();
        BufferedImage targetFlag = target.getFlagImage();

        int width = Math.min(guessedFlag.getWidth(), targetFlag.getWidth());
        int height = Math.min(guessedFlag.getHeight(), targetFlag.getHeight());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int guessedRGB = guessedFlag.getRGB(x, y);
                int targetRGB = targetFlag.getRGB(x, y);

                boolean isOverlap = (guessedRGB == targetRGB);
                differencesMap.put(x + "," + y, isOverlap);
            }
        }

        return differencesMap;
    }

    /**
     * Checks whether the guess is correct.
     *
     * @return true if the guess matches the target country, false otherwise
     */
    public boolean isCorrect() {
        return correct;
    }

    /**
     * Returns the guessed country.
     *
     * @return the guessed CountryBL object
     */
    public CountryBL getGuessedCountry() {
        return guessedCountry;
    }

    /**
     * Returns the target country.
     *
     * @return the target CountryBL object
     */
    public CountryBL getTargetCountry() {
        return targetCountry;
    }

    /**
     * Returns a string representation of the guess result,
     * including correctness and the involved countries.
     *
     * @return a descriptive string of the guess result
     */
    @Override
    public String toString() {
        return "GuessResult{" +
                "correct=" + correct +
                ", guessedCountry=" + guessedCountry.getName() +
                ", targetCountry=" + targetCountry.getName() +
                '}';
    }
}