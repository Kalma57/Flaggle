package com.example.flagdemo.BusinessLayer.CapitalGlobeBL;

/**
 * A record of one finished round of the capital-location globe game's "First to N" format,
 * kept for the match recap screen. Mirrors {@link com.example.flagdemo.BusinessLayer.CapitalBL.CapitalRoundResult}'s
 * shape (both sides always answer, so both correctness flags are recorded), plus the attempt
 * count that only makes sense for a click-to-select-and-confirm guess.
 */
public class CapitalGlobeRoundResult implements java.io.Serializable {

    private final int roundNumber;
    private final String targetCountryName;
    private final String targetCapital;
    private final String targetFlagPath;
    private final boolean humanCorrect;
    private final boolean aiCorrect;
    private final int humanAttempts;
    private final double humanTimeSeconds;
    private final double aiTimeSeconds;

    public CapitalGlobeRoundResult(int roundNumber, String targetCountryName, String targetCapital, String targetFlagPath,
                                    boolean humanCorrect, boolean aiCorrect, int humanAttempts,
                                    double humanTimeSeconds, double aiTimeSeconds) {
        this.roundNumber = roundNumber;
        this.targetCountryName = targetCountryName;
        this.targetCapital = targetCapital;
        this.targetFlagPath = targetFlagPath;
        this.humanCorrect = humanCorrect;
        this.aiCorrect = aiCorrect;
        this.humanAttempts = humanAttempts;
        this.humanTimeSeconds = humanTimeSeconds;
        this.aiTimeSeconds = aiTimeSeconds;
    }

    public int getRoundNumber() { return roundNumber; }
    public String getTargetCountryName() { return targetCountryName; }
    public String getTargetCapital() { return targetCapital; }
    public String getTargetFlagPath() { return targetFlagPath; }
    public boolean isHumanCorrect() { return humanCorrect; }
    public boolean isAiCorrect() { return aiCorrect; }
    public int getHumanAttempts() { return humanAttempts; }
    public double getHumanTimeSeconds() { return humanTimeSeconds; }
    public double getAiTimeSeconds() { return aiTimeSeconds; }
}
