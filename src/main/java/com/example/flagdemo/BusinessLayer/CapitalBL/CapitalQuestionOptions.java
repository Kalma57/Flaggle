package com.example.flagdemo.BusinessLayer.CapitalBL;

import com.example.flagdemo.BusinessLayer.CountryBL;

import java.util.List;

/**
 * The four shuffled answer options for one capital-quiz question - built once per question and
 * shared by both {@link CapitalQuizEngineBL} (Best of N) and {@link CapitalBlitzEngineBL}
 * (time-attack), see {@link CapitalOptionsBuilder}.
 */
public class CapitalQuestionOptions {

    private final List<String> optionTexts;
    private final List<CountryBL> optionCountries;
    private final int correctIndex;

    public CapitalQuestionOptions(List<String> optionTexts, List<CountryBL> optionCountries, int correctIndex) {
        this.optionTexts = optionTexts;
        this.optionCountries = optionCountries;
        this.correctIndex = correctIndex;
    }

    public List<String> getOptionTexts() { return optionTexts; }
    public List<CountryBL> getOptionCountries() { return optionCountries; }
    public int getCorrectIndex() { return correctIndex; }
}
