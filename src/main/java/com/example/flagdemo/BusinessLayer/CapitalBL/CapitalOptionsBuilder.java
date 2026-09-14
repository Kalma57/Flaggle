package com.example.flagdemo.BusinessLayer.CapitalBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Shared target-picking and answer-option-building logic for the capital-city quiz - factored
 * out so {@link CapitalQuizEngineBL} (Best of N) and {@link CapitalBlitzEngineBL} (time-attack)
 * don't each carry their own copy of the exact same rules.
 */
public final class CapitalOptionsBuilder {

    private static final int OPTION_COUNT = 4;

    private CapitalOptionsBuilder() {}

    /** Only countries with a real capital on record are eligible targets - see {@link CountryBL#hasCapital()}. */
    public static List<CountryBL> eligibleTargets(CountryController cc) {
        List<CountryBL> pool = new ArrayList<>();
        for (CountryBL c : cc.getAllCountries()) {
            if (c.hasCapital()) pool.add(c);
        }
        return pool;
    }

    public static CountryBL pickRandomTarget(CountryController cc) {
        List<CountryBL> pool = eligibleTargets(cc);
        return pool.get(new Random().nextInt(pool.size()));
    }

    /**
     * Builds the four shuffled options (plus the parallel country-per-option list, used for
     * flag icons in CAPITAL_TO_COUNTRY) and records which index is correct.
     * FLAG_TO_CAPITAL: options are capital city names (distractors need their own real capital).
     * CAPITAL_TO_COUNTRY: options are country names (any other country is a valid distractor).
     */
    public static CapitalQuestionOptions build(CountryController cc, CapitalQuizMode mode, CountryBL target) {
        // A country with no real capital on record (Antarctica, the EU, ENG/NIR - see
        // FlagsInserter's specialNames - dependent territories, ...) is never selectable here,
        // as either the target or a decoy: it would otherwise show up as an "Unknown" or
        // otherwise-broken option.
        List<CountryBL> distractorPool = new ArrayList<>();
        for (CountryBL c : cc.getAllCountries()) {
            if (c.equals(target)) continue;
            if (!c.hasCapital()) continue;
            distractorPool.add(c);
        }
        Collections.shuffle(distractorPool);

        List<CountryBL> chosen = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        chosen.add(target);
        labels.add(answerLabel(mode, target));
        for (CountryBL c : distractorPool) {
            if (chosen.size() >= OPTION_COUNT) break;
            String candidate = answerLabel(mode, c);
            if (labels.contains(candidate)) continue; // skip accidental duplicate text
            chosen.add(c);
            labels.add(candidate);
        }

        // Shuffle both lists together so option-text <-> option-country stay paired up.
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < chosen.size(); i++) order.add(i);
        Collections.shuffle(order);

        List<String> optionTexts = new ArrayList<>();
        List<CountryBL> optionCountries = new ArrayList<>();
        for (int i : order) {
            optionTexts.add(labels.get(i));
            optionCountries.add(chosen.get(i));
        }
        int correctIndex = optionCountries.indexOf(target);

        return new CapitalQuestionOptions(optionTexts, optionCountries, correctIndex);
    }

    public static String answerLabel(CapitalQuizMode mode, CountryBL c) {
        return (mode == CapitalQuizMode.FLAG_TO_CAPITAL) ? c.getCapital() : c.getName();
    }
}
