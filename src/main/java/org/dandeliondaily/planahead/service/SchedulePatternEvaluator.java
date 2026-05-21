package org.dandeliondaily.planahead.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.pt.model.TemplateType;

/**
 * Pure logic utility — no database dependency.
 *
 * Given a {@link TemplateType}, an optional schedule pattern string, and a date
 * range,
 * returns every {@link LocalDate} in [from, toInclusive] that matches the
 * schedule.
 *
 * Pattern formats are defined in docs/template-config-domain-model.md.
 *
 * A null or empty pattern means "every applicable day" for
 * Weekly/Monthly/Quarterly/Yearly
 * (i.e., every day in the range, subject to any external working-day filtering
 * by the caller).
 * Daily templates always fire every day regardless of pattern.
 */
public class SchedulePatternEvaluator {

    /**
     * Returns all dates in [from, toInclusive] that match the template's schedule.
     * Returns an empty list for any null argument or inverted range.
     */
    public List<LocalDate> matchingDates(TemplateType type, String pattern,
            LocalDate from, LocalDate toInclusive) {
        List<LocalDate> result = new ArrayList<>();
        if (type == null || from == null || toInclusive == null || from.isAfter(toInclusive)) {
            return result;
        }
        switch (type) {
            case DAILY:
                for (LocalDate d = from; !d.isAfter(toInclusive); d = d.plusDays(1)) {
                    result.add(d);
                }
                break;
            case WEEKLY:
                addWeeklyMatches(pattern, from, toInclusive, result);
                break;
            case MONTHLY:
                addMonthlyMatches(pattern, from, toInclusive, result);
                break;
            case QUARTERLY:
                addQuarterlyMatches(pattern, from, toInclusive, result);
                break;
            case YEARLY:
                addYearlyMatches(pattern, from, toInclusive, result);
                break;
            default:
                break;
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // WEEKLY
    // -------------------------------------------------------------------------

    private void addWeeklyMatches(String pattern, LocalDate from, LocalDate toInclusive,
            List<LocalDate> result) {
        if (isBlank(pattern)) {
            // null/empty = every day in range
            for (LocalDate d = from; !d.isAfter(toInclusive); d = d.plusDays(1)) {
                result.add(d);
            }
            return;
        }
        List<DayOfWeek> days = parseWeeklyPattern(pattern);
        for (LocalDate d = from; !d.isAfter(toInclusive); d = d.plusDays(1)) {
            if (days.contains(d.getDayOfWeek())) {
                result.add(d);
            }
        }
    }

    private List<DayOfWeek> parseWeeklyPattern(String pattern) {
        List<DayOfWeek> days = new ArrayList<>();
        for (String token : pattern.split(",")) {
            DayOfWeek dow = parseDayOfWeek(token.trim());
            if (dow != null && !days.contains(dow)) {
                days.add(dow);
            }
        }
        return days;
    }

    // -------------------------------------------------------------------------
    // MONTHLY
    // -------------------------------------------------------------------------

    private void addMonthlyMatches(String pattern, LocalDate from, LocalDate toInclusive,
            List<LocalDate> result) {
        if (isBlank(pattern)) {
            for (LocalDate d = from; !d.isAfter(toInclusive); d = d.plusDays(1)) {
                result.add(d);
            }
            return;
        }
        // Iterate every month that overlaps the range
        LocalDate monthCursor = from.withDayOfMonth(1);
        LocalDate lastMonth = toInclusive.withDayOfMonth(1);
        while (!monthCursor.isAfter(lastMonth)) {
            for (String token : pattern.split(",")) {
                LocalDate candidate = resolveMonthlyToken(token.trim(), monthCursor);
                if (candidate != null
                        && !candidate.isBefore(from)
                        && !candidate.isAfter(toInclusive)
                        && !result.contains(candidate)) {
                    result.add(candidate);
                }
            }
            monthCursor = monthCursor.plusMonths(1);
        }
        result.sort(null);
    }

    /**
     * Resolve a single monthly token against the first day of a month.
     * Token: numeric day number OR W{n}-{day} OR WL-{day}
     */
    LocalDate resolveMonthlyToken(String token, LocalDate monthFirst) {
        if (isBlank(token)) {
            return null;
        }
        if (token.startsWith("W") || token.startsWith("w")) {
            return resolveWeekPositionInMonth(token, monthFirst);
        }
        // Numeric day of month
        try {
            int day = Integer.parseInt(token);
            if (day < 1) {
                return null;
            }
            int lastDay = monthFirst.lengthOfMonth();
            return monthFirst.withDayOfMonth(Math.min(day, lastDay));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Resolve W{n}-{day} or WL-{day} within the given month.
     * W1-MON = first Monday of the month; WL-FRI = last Friday of the month.
     * Returns null if the requested occurrence does not exist in the month.
     */
    private LocalDate resolveWeekPositionInMonth(String token, LocalDate monthFirst) {
        int dashIdx = token.indexOf('-');
        if (dashIdx < 2) {
            return null; // needs at least "W1-X"
        }
        String weekPart = token.substring(1, dashIdx).trim(); // "1","2","L"
        String dayPart = token.substring(dashIdx + 1).trim();

        DayOfWeek dow = parseDayOfWeek(dayPart);
        if (dow == null) {
            return null;
        }

        if ("L".equalsIgnoreCase(weekPart)) {
            LocalDate lastDay = monthFirst.withDayOfMonth(monthFirst.lengthOfMonth());
            return lastDay.with(TemporalAdjusters.previousOrSame(dow));
        }

        try {
            int weekNum = Integer.parseInt(weekPart);
            if (weekNum < 1 || weekNum > 5) {
                return null;
            }
            // Nth occurrence of DOW in the month
            LocalDate first = monthFirst.with(TemporalAdjusters.nextOrSame(dow));
            if (first.getMonthValue() != monthFirst.getMonthValue()) {
                return null; // should never happen (nextOrSame from 1st of month)
            }
            LocalDate candidate = first.plusWeeks(weekNum - 1);
            if (candidate.getMonthValue() != monthFirst.getMonthValue()) {
                return null; // e.g. W5 when only 4 exist
            }
            return candidate;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // QUARTERLY
    // -------------------------------------------------------------------------

    private void addQuarterlyMatches(String pattern, LocalDate from, LocalDate toInclusive,
            List<LocalDate> result) {
        if (isBlank(pattern)) {
            for (LocalDate d = from; !d.isAfter(toInclusive); d = d.plusDays(1)) {
                result.add(d);
            }
            return;
        }
        // Generate all quarter starts that could contribute dates in range.
        // Quarters start Jan 1, Apr 1, Jul 1, Oct 1.
        int fromYear = from.getYear() - 1;
        int toYear = toInclusive.getYear() + 1;
        List<LocalDate> quarterStarts = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            quarterStarts.add(LocalDate.of(y, 1, 1));
            quarterStarts.add(LocalDate.of(y, 4, 1));
            quarterStarts.add(LocalDate.of(y, 7, 1));
            quarterStarts.add(LocalDate.of(y, 10, 1));
        }
        for (String token : pattern.split(",")) {
            for (LocalDate qStart : quarterStarts) {
                LocalDate qEnd = qStart.plusMonths(3).minusDays(1);
                if (qEnd.isBefore(from) || qStart.isAfter(toInclusive)) {
                    continue;
                }
                LocalDate candidate = resolveQuarterlyToken(token.trim(), qStart);
                if (candidate != null
                        && !candidate.isBefore(from)
                        && !candidate.isAfter(toInclusive)
                        && !result.contains(candidate)) {
                    result.add(candidate);
                }
            }
        }
        result.sort(null);
    }

    /**
     * Resolve a single quarterly token against the first day of a quarter.
     * Token: numeric day of quarter (1-based) OR W{n}-{day} OR WL-{day}
     */
    LocalDate resolveQuarterlyToken(String token, LocalDate quarterStart) {
        if (isBlank(token)) {
            return null;
        }
        if (token.startsWith("W") || token.startsWith("w")) {
            return resolveWeekPositionInQuarter(token, quarterStart);
        }
        try {
            int dayOfQuarter = Integer.parseInt(token);
            if (dayOfQuarter < 1) {
                return null;
            }
            return quarterStart.plusDays(dayOfQuarter - 1);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Resolve W{n}-{day} or WL-{day} within the given quarter (3 months from
     * quarterStart).
     * W1-MON = first Monday of the quarter; WL-FRI = last Friday of the quarter.
     */
    private LocalDate resolveWeekPositionInQuarter(String token, LocalDate quarterStart) {
        int dashIdx = token.indexOf('-');
        if (dashIdx < 2) {
            return null;
        }
        String weekPart = token.substring(1, dashIdx).trim();
        String dayPart = token.substring(dashIdx + 1).trim();

        DayOfWeek dow = parseDayOfWeek(dayPart);
        if (dow == null) {
            return null;
        }

        LocalDate quarterEnd = quarterStart.plusMonths(3).minusDays(1);

        if ("L".equalsIgnoreCase(weekPart)) {
            return quarterEnd.with(TemporalAdjusters.previousOrSame(dow));
        }

        try {
            int weekNum = Integer.parseInt(weekPart);
            if (weekNum < 1 || weekNum > 13) {
                return null;
            }
            // Nth occurrence of DOW in the quarter
            LocalDate first = quarterStart.with(TemporalAdjusters.nextOrSame(dow));
            if (first.isAfter(quarterEnd)) {
                return null;
            }
            LocalDate candidate = first.plusWeeks(weekNum - 1);
            if (candidate.isAfter(quarterEnd)) {
                return null;
            }
            return candidate;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // YEARLY
    // -------------------------------------------------------------------------

    private void addYearlyMatches(String pattern, LocalDate from, LocalDate toInclusive,
            List<LocalDate> result) {
        if (isBlank(pattern)) {
            for (LocalDate d = from; !d.isAfter(toInclusive); d = d.plusDays(1)) {
                result.add(d);
            }
            return;
        }
        for (String token : pattern.split(",")) {
            for (int y = from.getYear(); y <= toInclusive.getYear(); y++) {
                LocalDate candidate = resolveYearlyToken(token.trim(), y);
                if (candidate != null
                        && !candidate.isBefore(from)
                        && !candidate.isAfter(toInclusive)
                        && !result.contains(candidate)) {
                    result.add(candidate);
                }
            }
        }
        result.sort(null);
    }

    /**
     * Resolve a single yearly token for a given year.
     * Token: MMDD OR {MON}-W{n}-{day} OR {MON}-WL-{day}
     *
     * Examples: "0101", "1225", "JAN-W1-MON", "DEC-WL-FRI"
     */
    LocalDate resolveYearlyToken(String token, int year) {
        if (isBlank(token)) {
            return null;
        }
        if (token.contains("-")) {
            // {MON}-W{n}-{day} — split on first dash to get month, then remainder is
            // W{n}-{day}
            int firstDash = token.indexOf('-');
            String monthPart = token.substring(0, firstDash).trim();
            String weekDayPart = token.substring(firstDash + 1).trim(); // e.g. "W1-MON"

            Month month = parseMonth(monthPart);
            if (month == null) {
                return null;
            }
            LocalDate monthFirst = LocalDate.of(year, month, 1);
            return resolveWeekPositionInMonth(weekDayPart, monthFirst);
        }
        // MMDD
        if (token.length() != 4) {
            return null;
        }
        try {
            int mm = Integer.parseInt(token.substring(0, 2));
            int dd = Integer.parseInt(token.substring(2, 4));
            if (mm < 1 || mm > 12 || dd < 1 || dd > 31) {
                return null;
            }
            Month month = Month.of(mm);
            int lastDay = LocalDate.of(year, month, 1).lengthOfMonth();
            return LocalDate.of(year, month, Math.min(dd, lastDay));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    static DayOfWeek parseDayOfWeek(String s) {
        if (s == null) {
            return null;
        }
        switch (s.toUpperCase().trim()) {
            case "MON":
                return DayOfWeek.MONDAY;
            case "TUE":
                return DayOfWeek.TUESDAY;
            case "WED":
                return DayOfWeek.WEDNESDAY;
            case "THU":
                return DayOfWeek.THURSDAY;
            case "FRI":
                return DayOfWeek.FRIDAY;
            case "SAT":
                return DayOfWeek.SATURDAY;
            case "SUN":
                return DayOfWeek.SUNDAY;
            default:
                return null;
        }
    }

    private Month parseMonth(String s) {
        if (s == null) {
            return null;
        }
        switch (s.toUpperCase().trim()) {
            case "JAN":
                return Month.JANUARY;
            case "FEB":
                return Month.FEBRUARY;
            case "MAR":
                return Month.MARCH;
            case "APR":
                return Month.APRIL;
            case "MAY":
                return Month.MAY;
            case "JUN":
                return Month.JUNE;
            case "JUL":
                return Month.JULY;
            case "AUG":
                return Month.AUGUST;
            case "SEP":
                return Month.SEPTEMBER;
            case "OCT":
                return Month.OCTOBER;
            case "NOV":
                return Month.NOVEMBER;
            case "DEC":
                return Month.DECEMBER;
            default:
                return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
