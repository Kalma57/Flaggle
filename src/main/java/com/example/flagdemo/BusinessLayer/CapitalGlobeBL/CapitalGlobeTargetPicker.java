package com.example.flagdemo.BusinessLayer.CapitalGlobeBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Target-picking for the capital-location globe game (concept 3) - a country is only eligible
 * if it has both a real capital ({@link CountryBL#hasCapital()}, same rule as the other two
 * Capital games) AND real map coordinates (same rule the existing Globe game already uses to
 * skip countries with missing lat/lon). Deliberately separate from
 * {@link com.example.flagdemo.BusinessLayer.CapitalBL.CapitalOptionsBuilder} - that one serves
 * the multiple-choice quiz concepts and has no notion of coordinates.
 */
public final class CapitalGlobeTargetPicker {

    private CapitalGlobeTargetPicker() {}

    public static List<CountryBL> eligibleTargets(CountryController cc) {
        List<CountryBL> pool = new ArrayList<>();
        for (CountryBL c : cc.getAllCountries()) {
            if (!c.hasCapital()) continue;
            if (c.getLatitude() == 0.0 && c.getLongitude() == 0.0) continue;
            pool.add(c);
        }
        return pool;
    }

    public static CountryBL pickRandomTarget(CountryController cc) {
        List<CountryBL> pool = eligibleTargets(cc);
        return pool.get(new Random().nextInt(pool.size()));
    }
}
