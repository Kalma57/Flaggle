package com.example.flagdemo.BusinessLayer.DailyQuizBL;

import com.example.flagdemo.BusinessLayer.CountryBL;

import java.time.LocalDate;

/** Holds the resolved "country of the day" plus the stage-4 info text and how it was picked. */
public class DailyQuizSelection {

    private final CountryBL country;
    private final String eventTitle;
    /** Year the underlying event originally happened, when there is one clear origin year - null otherwise (see DailyQuizSelectorBL). */
    private final Integer eventYear;
    private final String infoText;
    private final LocalDate date;
    private final boolean devOverride;

    public DailyQuizSelection(CountryBL country, String eventTitle, Integer eventYear, String infoText, LocalDate date, boolean devOverride) {
        this.country = country;
        this.eventTitle = eventTitle;
        this.eventYear = eventYear;
        this.infoText = infoText;
        this.date = date;
        this.devOverride = devOverride;
    }

    public CountryBL getCountry() { return country; }
    public String getEventTitle() { return eventTitle; }
    public Integer getEventYear() { return eventYear; }
    public String getInfoText() { return infoText; }
    public LocalDate getDate() { return date; }
    public boolean isDevOverride() { return devOverride; }
}
