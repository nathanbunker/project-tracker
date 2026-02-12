package org.openimmunizationsoftware.pt.manager;

import java.time.LocalDate;
import java.time.YearMonth;

public class NarrativePeriods {

    public static PeriodRange forDaily(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        return new PeriodRange(date, date);
    }

    public static PeriodRange forWeekly(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        int daysSinceSunday = date.getDayOfWeek().getValue() % 7;
        LocalDate start = date.minusDays(daysSinceSunday);
        LocalDate end = start.plusDays(6);
        return new PeriodRange(start, end);
    }

    public static PeriodRange forMonthly(YearMonth yearMonth) {
        if (yearMonth == null) {
            throw new IllegalArgumentException("yearMonth is required");
        }
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return new PeriodRange(start, end);
    }

    public static final class PeriodRange {
        private final LocalDate start;
        private final LocalDate end;

        public PeriodRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        public LocalDate getStart() {
            return start;
        }

        public LocalDate getEnd() {
            return end;
        }
    }
}
