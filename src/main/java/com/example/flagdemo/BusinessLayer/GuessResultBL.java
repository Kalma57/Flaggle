package com.example.flagdemo.BusinessLayer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Predicate;

/**
 * Represents the result of a user's guess in the Flaggle game.
 *
 * This class compares the guessed country's flag with the target country's flag
 * and generates an image highlighting the differences between them.
 *
 * The comparison works pixel-by-pixel and produces an output image where:
 * - Green pixels represent similar colors
 * - Black pixels represent different colors
 * - White pixels represent background or frame areas
 */
public class GuessResultBL {

    // ----------------- Constants -----------------

    // Maximum allowed difference between RGB values for two colors to be considered similar
    private static final int TOLERANCE = 30;

    // Color representing the frame of the flag image
    private static final Color FRAME_COLOR = new Color(128, 128, 128);

    // Background color used in the result image
    private static final Color BACKGROUND_COLOR = Color.WHITE;

    // Base colors used to normalize flag colors
    private static final Color RED = new Color(255, 0, 0);
    private static final Color BLUE = new Color(0, 0, 255);
    private static final Color YELLOW = new Color(255, 255, 0);
    private static final Color GREEN = new Color(0, 128, 0);
    private static final Color ORANGE = new Color(255, 165, 0);
    private static final Color PURPLE = new Color(128, 0, 128);
    private static final Color BLACK = new Color(0, 0, 0);
    private static final Color WHITE = new Color(255, 255, 255);

    // Used when a pixel has transparency or cannot be clearly classified
    private static final Color GRAY = new Color(128, 128, 128);

    // ----------------- Fields -----------------

    // Indicates whether the guessed country matches the target country
    private final boolean isCorrect;

    // The country guessed by the user
    private final CountryBL guessedCountry;

    // The actual target country
    private final CountryBL targetCountry;

    // Image showing the visual differences between the two flags
    private final BufferedImage flagDifferences;

    // ----------------- Constructor -----------------

    /**
     * Creates a GuessResultBL object and computes the visual difference
     * between the guessed country's flag and the target country's flag.
     *
     * @param guessedCountry the country guessed by the user
     * @param targetCountry the correct country
     */
    public GuessResultBL(CountryBL guessedCountry, CountryBL targetCountry) {
        this.guessedCountry = guessedCountry;
        this.targetCountry = targetCountry;

        // Determine if the guess is correct
        this.isCorrect = guessedCountry.equals(targetCountry);

        // Calculate the flag difference image
        this.flagDifferences = calculateFlagDifferences(
                guessedCountry.getFlagImage(),
                targetCountry.getFlagImage()
        );
    }

    // ----------------- Public Methods -----------------

    /**
     * Indicates whether the guessed country is correct.
     *
     * @return true if the guess matches the target country
     */
    public boolean isCorrect() {
        return isCorrect;
    }

    /**
     * Returns the country guessed by the user.
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
     * Returns the generated image highlighting the differences
     * between the guessed flag and the target flag.
     *
     * @return difference image
     */
    public BufferedImage getFlagDifferences() {
        return flagDifferences;
    }

    @Override
    public String toString() {
        return "GuessResult{" +
                "correct=" + isCorrect +
                ", guessedCountry=" + guessedCountry.getName() +
                ", targetCountry=" + targetCountry.getName() +
                '}';
    }

    // ----------------- Helper Methods -----------------

    /**
     * Generates an image that highlights the differences between two flags.
     *
     * Pixel comparison rules:
     * - Green pixel: colors are similar
     * - Black pixel: colors are different
     * - White pixel: background or frame
     *
     * @param guessed the guessed flag image
     * @param target the target flag image
     * @return image representing the differences between the two flags
     */
    public static BufferedImage calculateFlagDifferences(BufferedImage guessed, BufferedImage target) {

        int width = Math.min(guessed.getWidth(), target.getWidth());
        int height = Math.min(guessed.getHeight(), target.getHeight());

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Compare pixels one by one
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                // Normalize colors to one of the base colors
                Color guessedColor = normalizeColor(new Color(guessed.getRGB(x, y), true));
                Color targetColor = normalizeColor(new Color(target.getRGB(x, y), true));

                // Ignore frame or background pixels
                if (isFrameOrBackground(guessedColor) || isFrameOrBackground(targetColor)) {
                    result.setRGB(x, y, BACKGROUND_COLOR.getRGB());
                    continue;
                }

                // If colors are similar -> green
                if (areColorsSimilar(guessedColor, targetColor)) {
                    result.setRGB(x, y, new Color(0x4CAF50).getRGB());
                } else { // Otherwise -> black
                    result.setRGB(x, y, BLACK.getRGB());
                }
            }
        }

        return result;
    }

    /**
     * Maps a given color to the closest predefined base color.
     *
     * This helps reduce noise when comparing pixels between two flags.
     *
     * @param c the original color
     * @return the closest base color
     */
    private static Color normalizeColor(Color c) {

        Color[] baseColors = {RED, BLUE, YELLOW, GREEN, ORANGE, PURPLE, BLACK, WHITE};

        // Treat transparent pixels as undefined
        if (c.getAlpha() < 255) return GRAY;

        Color closest = baseColors[0];
        double minDist = colorDistance(c, closest);

        // Find the base color with the minimal distance
        for (Color base : baseColors) {
            double dist = colorDistance(c, base);
            if (dist < minDist) {
                minDist = dist;
                closest = base;
            }
        }

        return closest;
    }

    /**
     * Computes the Euclidean distance between two colors in RGB space.
     *
     * @param c1 first color
     * @param c2 second color
     * @return distance between the colors
     */
    private static double colorDistance(Color c1, Color c2) {
        int dr = c1.getRed() - c2.getRed();
        int dg = c1.getGreen() - c2.getGreen();
        int db = c1.getBlue() - c2.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    /**
     * Checks whether two colors are similar within the defined tolerance.
     *
     * @param c1 first color
     * @param c2 second color
     * @return true if the colors are considered similar
     */
    private static boolean areColorsSimilar(Color c1, Color c2) {
        return Math.abs(c1.getRed() - c2.getRed()) <= TOLERANCE &&
                Math.abs(c1.getGreen() - c2.getGreen()) <= TOLERANCE &&
                Math.abs(c1.getBlue() - c2.getBlue()) <= TOLERANCE;
    }

    /**
     * Determines whether a pixel belongs to the flag frame or background.
     *
     * @param c pixel color
     * @return true if the pixel is part of the frame/background
     */
    private static boolean isFrameOrBackground(Color c) {
        return c.equals(FRAME_COLOR);
    }
}