package com.example.flagdemo.BusinessLayer.FlaggleBL;

import com.example.flagdemo.BusinessLayer.CountryBL;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;

/**
 * Represents the result of a user's guess in the Flaggle game.
 *
 * This class compares the guessed country's flag with the target country's flag
 * and generates an image highlighting the differences between them.
 *
 * The comparison works pixel-by-pixel. The resulting image depends on the
 * {@link DifficultyLevel}:
 *
 * - HARD: a fresh image is produced for every guess. Green pixels represent
 *   similar colors, white pixels represent background/frame areas, and every
 *   other pixel is plain black — no information about the target flag's real
 *   colors is ever leaked.
 *
 * - EASY: the image accumulates across every guess made during the round.
 *   It starts out solid black (white for background/frame areas). Whenever a
 *   pixel matches the target for the first time (in this guess or an earlier
 *   one), it is permanently revealed using the target flag's true, raw color
 *   (not the normalized/bucketed color used only to decide whether it's a
 *   match) — so the revealed shade is exactly what the target flag actually
 *   looks like there. Once revealed, a pixel never reverts to black.
 */
public class GuessResultBL implements java.io.Serializable {

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

    // Base64-encoded PNG of the guessed flag, computed once and cached so repeated
    // renders of the guess history don't re-decode/re-encode the same image every request
    private final String guessedFlagBase64;

    // Base64-encoded PNG of the difference image, computed once and cached for the same reason
    private final String flagDifferencesBase64;

    // ----------------- Constructor -----------------

    /**
     * Creates a GuessResultBL object and computes the visual difference
     * between the guessed country's flag and the target country's flag,
     * using {@link DifficultyLevel#HARD} (the original behavior).
     *
     * @param guessedCountry the country guessed by the user
     * @param targetCountry the correct country
     */
    public GuessResultBL(CountryBL guessedCountry, CountryBL targetCountry) {
        this(guessedCountry, targetCountry, DifficultyLevel.HARD);
    }

    /**
     * Creates a GuessResultBL object and computes the visual difference
     * between the guessed country's flag and the target country's flag,
     * using {@link DifficultyLevel#HARD} rules if difficulty is HARD, or a
     * fresh (non-accumulating) EASY reveal otherwise.
     *
     * @param guessedCountry the country guessed by the user
     * @param targetCountry the correct country
     * @param difficulty controls which comparison algorithm is used
     */
    public GuessResultBL(CountryBL guessedCountry, CountryBL targetCountry, DifficultyLevel difficulty) {
        this(guessedCountry, targetCountry, difficulty, null);
    }

    /**
     * Creates a GuessResultBL object and computes the visual difference
     * between the guessed country's flag and the target country's flag.
     *
     * @param guessedCountry the country guessed by the user
     * @param targetCountry the correct country
     * @param difficulty controls which comparison algorithm is used
     * @param previousEasyReveal only relevant on {@link DifficultyLevel#EASY}: the
     *                           accumulated reveal image from the previous guess this
     *                           round (or null if this is the first guess of the round).
     *                           Ignored on HARD.
     */
    public GuessResultBL(CountryBL guessedCountry, CountryBL targetCountry, DifficultyLevel difficulty, BufferedImage previousEasyReveal) {
        this.guessedCountry = guessedCountry;
        this.targetCountry = targetCountry;

        // Determine if the guess is correct
        this.isCorrect = guessedCountry.equals(targetCountry);

        // Calculate the flag difference/reveal image
        if (difficulty == DifficultyLevel.EASY) {
            this.flagDifferences = calculateAccumulatedEasyReveal(
                    previousEasyReveal,
                    guessedCountry.getFlagImage(),
                    targetCountry.getFlagImage()
            );
        } else {
            this.flagDifferences = calculateFlagDifferences(
                    guessedCountry.getFlagImage(),
                    targetCountry.getFlagImage()
            );
        }

        // Encode both images to base64 once, up front, instead of on every subsequent render
        this.guessedFlagBase64 = encodeToBase64(guessedCountry.getFlagImage());
        this.flagDifferencesBase64 = encodeToBase64(this.flagDifferences);
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

    /**
     * Returns the base64-encoded PNG of the guessed flag (pre-computed in the constructor).
     */
    public String getGuessedFlagBase64() {
        return guessedFlagBase64;
    }

    /**
     * Returns the base64-encoded PNG of the flag difference image (pre-computed in the constructor).
     */
    public String getFlagDifferencesBase64() {
        return flagDifferencesBase64;
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
     * Encodes an image as a base64 PNG string.
     * Wraps IOException as unchecked since encoding an in-memory BufferedImage
     * to a ByteArrayOutputStream does not perform real I/O and cannot realistically fail.
     */
    private static String encodeToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Generates an image that highlights the differences between two flags,
     * using {@link DifficultyLevel#HARD} rules (the original behavior).
     *
     * Pixel comparison rules:
     * - Green pixel: colors are similar
     * - White pixel: background or frame
     * - Every other pixel: plain black — no color information is ever leaked
     *
     * This is a fresh, non-accumulating computation: it depends only on the
     * current guess, not on any earlier guesses made this round.
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

                if (areColorsSimilar(guessedColor, targetColor)) {
                    // Colors match -> green
                    result.setRGB(x, y, new Color(0x4CAF50).getRGB());
                } else {
                    // No match -> plain black, no color information leaked
                    result.setRGB(x, y, BLACK.getRGB());
                }
            }
        }

        return result;
    }

    /**
     * Generates (or extends) the accumulated EASY-mode reveal image for this game round.
     *
     * Unlike {@link #calculateFlagDifferences(BufferedImage, BufferedImage)}, this method's
     * output builds on top of the round's previous reveal image instead of being recomputed
     * from scratch on every guess:
     *
     * - Background/frame areas (determined from the target flag alone) are always white.
     * - Any pixel that was already revealed by an earlier guess this round is copied forward
     *   unchanged — once revealed, a pixel never reverts back to black.
     * - Any pixel not yet revealed is compared (using the same normalized/bucketed color
     *   matching as HARD mode): if it matches, it is revealed using the target flag's true,
     *   raw color (not the flattened bucket color) — so the visible shade is exactly what the
     *   target flag really looks like there. If it still doesn't match, it stays black.
     *
     * @param previousReveal the accumulated reveal image from the previous guess this round,
     *                        or null if this is the first guess of the round
     * @param guessed the guessed flag image
     * @param target the target flag image
     * @return the updated accumulated reveal image
     */
    private static BufferedImage calculateAccumulatedEasyReveal(BufferedImage previousReveal, BufferedImage guessed, BufferedImage target) {

        int width = Math.min(guessed.getWidth(), target.getWidth());
        int height = Math.min(guessed.getHeight(), target.getHeight());

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                // Raw (non-normalized) target color — used when actually revealing a pixel
                Color rawTargetColor = new Color(target.getRGB(x, y), true);

                // Normalized colors — used only to decide background/frame and match/no-match
                Color normalizedTargetColor = normalizeColor(rawTargetColor);

                // Background/frame areas are always white, regardless of any previous reveal
                if (isFrameOrBackground(normalizedTargetColor)) {
                    result.setRGB(x, y, BACKGROUND_COLOR.getRGB());
                    continue;
                }

                // If this pixel was already revealed by an earlier guess, keep it revealed
                if (previousReveal != null
                        && x < previousReveal.getWidth() && y < previousReveal.getHeight()
                        && previousReveal.getRGB(x, y) != BLACK.getRGB()) {
                    result.setRGB(x, y, previousReveal.getRGB(x, y));
                    continue;
                }

                // Not yet revealed -> check if this guess reveals it now
                Color normalizedGuessedColor = normalizeColor(new Color(guessed.getRGB(x, y), true));

                if (areColorsSimilar(normalizedGuessedColor, normalizedTargetColor)) {
                    // Match -> permanently reveal the target's true (raw) color
                    result.setRGB(x, y, rawTargetColor.getRGB());
                } else {
                    // Still no match -> stays hidden behind black
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