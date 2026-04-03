package org.dandeliondaily.narrative.model;

import java.time.LocalDate;

public class TrackerNarrativeScope {

    public static final String TYPE_DAILY = "DAILY";
    public static final String TYPE_WEEKLY = "WEEKLY";
    public static final String TYPE_MONTHLY = "MONTHLY";

    private String narrativeType;
    private LocalDate selectedDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    public String getNarrativeType() {
        return narrativeType;
    }

    public void setNarrativeType(String narrativeType) {
        this.narrativeType = narrativeType;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }
}
