package com.example.flagdemo.BusinessLayer.CapitalGlobeBL;

/**
 * A record of one country a side (human or AI) personally resolved during the capital-location
 * globe game's Blitz format - kept for the match recap screen. Mirrors
 * {@link com.example.flagdemo.BusinessLayer.CapitalBL.CapitalBlitzQuestionResult}'s shape, plus
 * the attempt count a type-and-narrow-down guess can rack up that a multiple-choice click can't.
 */
public class CapitalGlobeBlitzQuestionResult implements java.io.Serializable {

    private final int order;
    private final String targetCountryName;
    private final String targetCapital;
    private final String targetFlagPath;
    private final boolean correct;
    private final int attempts;
    private final double timeTakenSeconds;

    public CapitalGlobeBlitzQuestionResult(int order, String targetCountryName, String targetCapital, String targetFlagPath,
                                            boolean correct, int attempts, double timeTakenSeconds) {
        this.order = order;
        this.targetCountryName = targetCountryName;
        this.targetCapital = targetCapital;
        this.targetFlagPath = targetFlagPath;
        this.correct = correct;
        this.attempts = attempts;
        this.timeTakenSeconds = timeTakenSeconds;
    }

    public int getOrder() { return order; }
    public String getTargetCountryName() { return targetCountryName; }
    public String getTargetCapital() { return targetCapital; }
    public String getTargetFlagPath() { return targetFlagPath; }
    public boolean isCorrect() { return correct; }
    public int getAttempts() { return attempts; }
    public double getTimeTakenSeconds() { return timeTakenSeconds; }
}
