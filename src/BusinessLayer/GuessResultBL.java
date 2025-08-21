package BusinessLayer;

import org.mozilla.javascript.json.JsonParser;

import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.lang.annotation.RetentionPolicy;
import java.lang.classfile.Attributes;

public class GuessResultBL {
    private static final int TOLERANCE = 30; // ← קבוע

    private boolean correct;
    private CountryBL guessedCountry;
    private CountryBL targetCountry;
    private BufferedImage flagDifferences;

    public GuessResultBL(CountryBL guessedCountry, CountryBL targetCountry) {
        this.guessedCountry = guessedCountry;
        this.targetCountry = targetCountry;

        this.correct = guessedCountry.equals(targetCountry);

        this.flagDifferences = calculateFlagDifferences(
                guessedCountry.getFlagImage(),
                targetCountry.getFlagImage()
        );
    }

    /**
     * Calculates differences between two flags pixel by pixel.
     * Green = similar pixel, Black = different.
     */
    public static BufferedImage calculateFlagDifferences(BufferedImage guessed, BufferedImage target) {
        int width = Math.min(guessed.getWidth(), target.getWidth());
        int height = Math.min(guessed.getHeight(), target.getHeight());

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int guessedRGB = guessed.getRGB(x, y);
                int targetRGB = target.getRGB(x, y);

                if (areColorsSimilar(guessedRGB, targetRGB)) {
                    result.setRGB(x, y, Color.GREEN.getRGB());
                } else {
                    result.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        return result;
    }

    private static boolean areColorsSimilar(int rgb1, int rgb2) {
        Color c1 = new Color(rgb1);
        Color c2 = new Color(rgb2);

        int redDiff   = Math.abs(c1.getRed()   - c2.getRed());
        int greenDiff = Math.abs(c1.getGreen() - c2.getGreen());
        int blueDiff  = Math.abs(c1.getBlue()  - c2.getBlue());

        return (redDiff <= TOLERANCE &&
                greenDiff <= TOLERANCE &&
                blueDiff <= TOLERANCE);
    }

    // ---- Getters ----

    public boolean isCorrect() {
        return correct;
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
                "correct=" + correct +
                ", guessedCountry=" + guessedCountry.getName() +
                ", targetCountry=" + targetCountry.getName() +
                '}';
    }
}
