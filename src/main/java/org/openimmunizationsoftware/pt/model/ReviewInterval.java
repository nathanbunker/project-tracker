package org.openimmunizationsoftware.pt.model;

/**
 * Enumeration of review intervals for project reviews.
 * Defines standard time periods for reviewing project progress.
 */
public enum ReviewInterval {
    WEEK("Week", 6),
    TWO_WEEKS("Two Weeks", 13),
    MONTH("Month", 26),
    TWO_MONTHS("Two Months", 60),
    FOUR_MONTHS("Four Months", 120),
    YEAR("Year", 360);

    private final String description;
    private final int days;

    ReviewInterval(String description, int days) {
        this.description = description;
        this.days = days;
    }

    public String getDescription() {
        return description;
    }

    public int getDays() {
        return days;
    }

    /**
     * Get a human-readable label for the given number of days.
     * Matches against configured intervals or returns a formatted day count.
     */
    public static String makeLabel(int days) {
        for (ReviewInterval interval : ReviewInterval.values()) {
            if (interval.getDays() == days) {
                return interval.getDescription();
            }
        }
        if (days == 0) {
            return "";
        } else if (days == 1) {
            return "One Day";
        } else if (days == 2) {
            return "Two Days";
        }
        return days + " Days";
    }
}
