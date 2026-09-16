package com.example.flagdemo.BusinessLayer.CapitalGlobeBL;

import com.example.flagdemo.BusinessLayer.CountryBL;
import com.example.flagdemo.DataAccessLayer.CountryController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Target-picking for the capital-location globe game (concept 3) - a country is only eligible
 * if it has both a real capital ({@link CountryBL#hasCapital()}, same rule as the other two
 * Capital games) AND real map coordinates (same rule the existing Globe game already uses to
 * skip countries with missing lat/lon). Deliberately separate from
 * {@link com.example.flagdemo.BusinessLayer.CapitalBL.CapitalOptionsBuilder} - that one serves
 * the multiple-choice quiz concepts and has no notion of coordinates.
 */
public final class CapitalGlobeTargetPicker {

    /**
     * Real countries smaller in land area than Nauru (~21 km², itself the third-smallest
     * country in the world) - finding them on a rotatable globe is nearly impossible and their
     * polygons are barely visible even when you already know where to look. The DB has no area
     * column, so this is a short, curated list rather than a computed threshold: at this size,
     * only Vatican City (~0.49 km²) and Monaco (~2.1 km²) qualify.
     */
    private static final Set<String> TOO_SMALL_FOR_GLOBE = Set.of("Vatican City", "Monaco");

    /**
     * Countries/territories that simply have no polygon of their own in the world-atlas map
     * data at all (verified against /assets/countries-50m.json) - typed-autocomplete guessing
     * never cared, but a click-to-select target needs a real polygon to land a click on, and
     * these have none to click, marker fallback or not (the point markers in globe.js are only
     * ever drawn to redisplay an ALREADY-guessed country's color - they can't be clicked to make
     * the guess in the first place). Leaving any of these as a possible target would make that
     * round unwinnable no matter how precisely the player clicks.
     */
    private static final Set<String> NO_CLICKABLE_POLYGON = Set.of(
            "Cocos (Keeling) Islands", "Christmas Island", "French Guiana", "Gibraltar",
            "Guadeloupe", "Martinique", "Réunion", "Svalbard & Jan Mayen", "Tuvalu", "Mayotte");

    private CapitalGlobeTargetPicker() {}

    public static List<CountryBL> eligibleTargets(CountryController cc) {
        List<CountryBL> pool = new ArrayList<>();
        for (CountryBL c : cc.getAllCountries()) {
            if (!c.hasCapital()) continue;
            if (c.getLatitude() == 0.0 && c.getLongitude() == 0.0) continue;
            if (TOO_SMALL_FOR_GLOBE.contains(c.getName())) continue;
            if (NO_CLICKABLE_POLYGON.contains(c.getName())) continue;
            pool.add(c);
        }
        return pool;
    }

    public static CountryBL pickRandomTarget(CountryController cc) {
        List<CountryBL> pool = eligibleTargets(cc);
        return pool.get(new Random().nextInt(pool.size()));
    }
}
