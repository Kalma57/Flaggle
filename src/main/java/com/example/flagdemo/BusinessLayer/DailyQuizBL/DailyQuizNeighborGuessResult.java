package com.example.flagdemo.BusinessLayer.DailyQuizBL;

import com.example.flagdemo.BusinessLayer.CountryBL;

/** Result of one stage-2 guess: was it a neighbor, and is the whole stage done now? */
public class DailyQuizNeighborGuessResult {

    private final CountryBL guessedCountry;
    private final boolean isNeighbor;
    private final boolean alreadyFound;
    private final int remainingCount;
    private final int totalCount;
    private final boolean stageComplete;

    public DailyQuizNeighborGuessResult(CountryBL guessedCountry, boolean isNeighbor, boolean alreadyFound,
                                         int remainingCount, int totalCount, boolean stageComplete) {
        this.guessedCountry = guessedCountry;
        this.isNeighbor = isNeighbor;
        this.alreadyFound = alreadyFound;
        this.remainingCount = remainingCount;
        this.totalCount = totalCount;
        this.stageComplete = stageComplete;
    }

    public CountryBL getGuessedCountry() { return guessedCountry; }
    public boolean isNeighbor() { return isNeighbor; }
    public boolean isAlreadyFound() { return alreadyFound; }
    public int getRemainingCount() { return remainingCount; }
    public int getTotalCount() { return totalCount; }
    public boolean isStageComplete() { return stageComplete; }
}
