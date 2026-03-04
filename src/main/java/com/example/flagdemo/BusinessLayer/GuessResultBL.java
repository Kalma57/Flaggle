package com.example.flagdemo.BusinessLayer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Predicate;

public class GuessResultBL {

    // ----------------- קבועים -----------------
    private static final int TOLERANCE = 30; // סף להשוואת צבעים
    private static final Color FRAME_COLOR      = new Color(128, 128, 128); // מסגרת
    private static final Color BACKGROUND_COLOR = Color.WHITE;              // רקע

    // צבעי בסיס
    private static final Color RED    = new Color(255, 0, 0);
    private static final Color BLUE   = new Color(0, 0, 255);
    private static final Color YELLOW = new Color(255, 255, 0);
    private static final Color GREEN  = new Color(0, 128, 0);
    private static final Color ORANGE = new Color(255, 165, 0);
    private static final Color PURPLE = new Color(128, 0, 128);
    private static final Color BLACK  = new Color(0, 0, 0);
    private static final Color WHITE  = new Color(255, 255, 255);
    private static final Color GRAY   = new Color(128, 128, 128); // שקוף/לא ברור

    // ----------------- שדות -----------------
    private final boolean isCorrect;
    private final CountryBL guessedCountry;
    private final CountryBL targetCountry;
    private final BufferedImage flagDifferences;

    // ----------------- קונסטרקטור -----------------
    public GuessResultBL(CountryBL guessedCountry, CountryBL targetCountry) {
        this.guessedCountry = guessedCountry;
        this.targetCountry  = targetCountry;

        this.isCorrect = guessedCountry.equals(targetCountry);

        // חישוב הבדלים בדגלים
        this.flagDifferences = calculateFlagDifferences(
                guessedCountry.getFlagImage(),
                targetCountry.getFlagImage()
        );
    }

    // ----------------- פונקציות ציבוריות -----------------
    public boolean isCorrect() {
        return isCorrect;
    }

    public CountryBL getGuessedCountry() {
        return guessedCountry;
    }

    public CountryBL getTargetCountry() {
        return targetCountry;
    }

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

    // ----------------- פונקציות עזר -----------------

    /**
     * מחזיר תמונה עם הבדלים בין דגלים:
     * ירוק = דומה
     * שחור = שונה
     * לבן = רקע / מסגרת
     */
    public static BufferedImage calculateFlagDifferences(BufferedImage guessed, BufferedImage target) {

        int width  = Math.min(guessed.getWidth(), target.getWidth());
        int height = Math.min(guessed.getHeight(), target.getHeight());

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color guessedColor = normalizeColor(new Color(guessed.getRGB(x, y), true));
                Color targetColor  = normalizeColor(new Color(target.getRGB(x, y), true));

                // רקע/מסגרת נשאר לבן
                if (isFrameOrBackground(guessedColor) || isFrameOrBackground(targetColor)) {
                    result.setRGB(x, y, BACKGROUND_COLOR.getRGB());
                    continue;
                }

                // אם הצבעים דומים – ירוק
                if (areColorsSimilar(guessedColor, targetColor)) {
                    result.setRGB(x, y, new Color(0x4CAF50).getRGB());
                } else { // אחרת – שחור
                    result.setRGB(x, y, BLACK.getRGB());
                }
            }
        }

        return result;
    }


    /**
     * מחזיר את הצבע הקרוב ביותר מבין הצבעים הבסיסיים
     */
    private static Color normalizeColor(Color c) {
        Color[] baseColors = {RED, BLUE, YELLOW, GREEN, ORANGE, PURPLE, BLACK, WHITE};

        if (c.getAlpha() < 255) return GRAY;

        Color closest = baseColors[0];
        double minDist = colorDistance(c, closest);

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
     * חישוב מרחק אוקלידי בין צבעים
     */
    private static double colorDistance(Color c1, Color c2) {
        int dr = c1.getRed() - c2.getRed();
        int dg = c1.getGreen() - c2.getGreen();
        int db = c1.getBlue() - c2.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    /**
     * האם שני צבעים דומים לפי סף TOLERANCE
     */
    private static boolean areColorsSimilar(Color c1, Color c2) {
        return Math.abs(c1.getRed()   - c2.getRed())   <= TOLERANCE &&
                Math.abs(c1.getGreen() - c2.getGreen()) <= TOLERANCE &&
                Math.abs(c1.getBlue()  - c2.getBlue())  <= TOLERANCE;
    }

    /**
     * האם פיקסל נחשב מסגרת או רקע
     */
    private static boolean isFrameOrBackground(Color c) {
        return c.equals(FRAME_COLOR);
    }
}
