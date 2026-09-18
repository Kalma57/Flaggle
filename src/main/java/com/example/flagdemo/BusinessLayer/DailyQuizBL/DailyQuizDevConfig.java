package com.example.flagdemo.BusinessLayer.DailyQuizBL;

/**
 * Single on/off switch for Daily Quiz testing. While true, {@link com.example.flagdemo.View.DailyQuizView.DailyQuizController}
 * accepts a devCountryId query parameter that forces which country counts as
 * "today's country" and skips the once-per-day cookie gate, so edge cases -
 * island nations, different capitals, repeated runs - can all be tried in one
 * sitting instead of waiting a real day between each one.
 *
 * Flip back to false before shipping the feature for real - every dev-only
 * parameter is silently ignored once this is false, so there is no way to
 * trigger it from the outside by accident.
 */
public final class DailyQuizDevConfig {

    public static final boolean DEV_MODE_ENABLED = false;

    private DailyQuizDevConfig() {}
}
