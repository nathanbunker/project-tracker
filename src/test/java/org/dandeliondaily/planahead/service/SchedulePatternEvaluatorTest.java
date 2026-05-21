package org.dandeliondaily.planahead.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.TemplateType;

public class SchedulePatternEvaluatorTest {

    private SchedulePatternEvaluator evaluator;

    @Before
    public void setUp() {
        evaluator = new SchedulePatternEvaluator();
    }

    // =========================================================================
    // Null / bad input guards
    // =========================================================================

    @Test
    public void nullType_returnsEmpty() {
        List<LocalDate> result = evaluator.matchingDates(null, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7));
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void nullFrom_returnsEmpty() {
        List<LocalDate> result = evaluator.matchingDates(TemplateType.DAILY, null,
                null, LocalDate.of(2026, 1, 7));
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void nullTo_returnsEmpty() {
        List<LocalDate> result = evaluator.matchingDates(TemplateType.DAILY, null,
                LocalDate.of(2026, 1, 1), null);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void invertedRange_returnsEmpty() {
        List<LocalDate> result = evaluator.matchingDates(TemplateType.DAILY, null,
                LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 1));
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void singleDayRange_returnsOneDate() {
        LocalDate d = LocalDate.of(2026, 5, 20);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.DAILY, null, d, d);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(d, result.get(0));
    }

    // =========================================================================
    // DAILY
    // =========================================================================

    @Test
    public void daily_nullPattern_returnsEveryDay() {
        LocalDate from = LocalDate.of(2026, 5, 18); // Mon
        LocalDate to = LocalDate.of(2026, 5, 22); // Fri
        List<LocalDate> result = evaluator.matchingDates(TemplateType.DAILY, null, from, to);
        Assert.assertEquals(5, result.size());
        Assert.assertEquals(from, result.get(0));
        Assert.assertEquals(to, result.get(4));
    }

    @Test
    public void daily_patternIgnored_returnsEveryDay() {
        // Pattern is meaningless for DAILY but should not cause errors
        LocalDate from = LocalDate.of(2026, 5, 18);
        LocalDate to = LocalDate.of(2026, 5, 19);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.DAILY, "MON", from, to);
        Assert.assertEquals(2, result.size());
    }

    // =========================================================================
    // WEEKLY
    // =========================================================================

    @Test
    public void weekly_nullPattern_returnsEveryDay() {
        LocalDate from = LocalDate.of(2026, 5, 18);
        LocalDate to = LocalDate.of(2026, 5, 24);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.WEEKLY, null, from, to);
        Assert.assertEquals(7, result.size());
    }

    @Test
    public void weekly_emptyPattern_returnsEveryDay() {
        LocalDate from = LocalDate.of(2026, 5, 18);
        LocalDate to = LocalDate.of(2026, 5, 24);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.WEEKLY, "  ", from, to);
        Assert.assertEquals(7, result.size());
    }

    @Test
    public void weekly_mondayOnly_returnsOnlyMondays() {
        // Week of May 18 (Mon) through May 31 (Sun) — 2 Mondays
        LocalDate from = LocalDate.of(2026, 5, 18);
        LocalDate to = LocalDate.of(2026, 5, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.WEEKLY, "MON", from, to);
        Assert.assertEquals(2, result.size());
        for (LocalDate d : result) {
            Assert.assertEquals(DayOfWeek.MONDAY, d.getDayOfWeek());
        }
        Assert.assertEquals(LocalDate.of(2026, 5, 18), result.get(0));
        Assert.assertEquals(LocalDate.of(2026, 5, 25), result.get(1));
    }

    @Test
    public void weekly_monWedFri_returnsThreeDaysPerWeek() {
        // May 18 (Mon) - May 24 (Sun)
        LocalDate from = LocalDate.of(2026, 5, 18);
        LocalDate to = LocalDate.of(2026, 5, 24);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.WEEKLY, "MON,WED,FRI", from, to);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(LocalDate.of(2026, 5, 18), result.get(0)); // Mon
        Assert.assertEquals(LocalDate.of(2026, 5, 20), result.get(1)); // Wed
        Assert.assertEquals(LocalDate.of(2026, 5, 22), result.get(2)); // Fri
    }

    @Test
    public void weekly_tueThu_twoWeeks() {
        // May 18 (Mon) - May 31 (Sun) — 2 full weeks
        LocalDate from = LocalDate.of(2026, 5, 18);
        LocalDate to = LocalDate.of(2026, 5, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.WEEKLY, "TUE,THU", from, to);
        Assert.assertEquals(4, result.size());
        Assert.assertEquals(DayOfWeek.TUESDAY, result.get(0).getDayOfWeek());
        Assert.assertEquals(DayOfWeek.THURSDAY, result.get(1).getDayOfWeek());
        Assert.assertEquals(DayOfWeek.TUESDAY, result.get(2).getDayOfWeek());
        Assert.assertEquals(DayOfWeek.THURSDAY, result.get(3).getDayOfWeek());
    }

    @Test
    public void weekly_caseInsensitiveTokens() {
        LocalDate from = LocalDate.of(2026, 5, 18);
        LocalDate to = LocalDate.of(2026, 5, 24);
        List<LocalDate> upper = evaluator.matchingDates(TemplateType.WEEKLY, "MON,FRI", from, to);
        List<LocalDate> lower = evaluator.matchingDates(TemplateType.WEEKLY, "mon,fri", from, to);
        Assert.assertEquals(upper, lower);
    }

    // =========================================================================
    // MONTHLY — numeric day
    // =========================================================================

    @Test
    public void monthly_day1_returnsFirstOfEachMonth() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.MONTHLY, "1", from, to);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(LocalDate.of(2026, 1, 1), result.get(0));
        Assert.assertEquals(LocalDate.of(2026, 2, 1), result.get(1));
        Assert.assertEquals(LocalDate.of(2026, 3, 1), result.get(2));
    }

    @Test
    public void monthly_day1And15_returnsTwicePerMonth() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.MONTHLY, "1,15", from, to);
        Assert.assertEquals(4, result.size());
        Assert.assertTrue(result.contains(LocalDate.of(2026, 5, 1)));
        Assert.assertTrue(result.contains(LocalDate.of(2026, 5, 15)));
        Assert.assertTrue(result.contains(LocalDate.of(2026, 6, 1)));
        Assert.assertTrue(result.contains(LocalDate.of(2026, 6, 15)));
    }

    @Test
    public void monthly_day31_clampedToLastDayInFebruary() {
        LocalDate from = LocalDate.of(2026, 2, 1);
        LocalDate to = LocalDate.of(2026, 2, 28);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.MONTHLY, "31", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 2, 28), result.get(0));
    }

    @Test
    public void monthly_day29_clampedForFebruaryNonLeap() {
        LocalDate from = LocalDate.of(2026, 2, 1);
        LocalDate to = LocalDate.of(2026, 2, 28);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.MONTHLY, "29", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 2, 28), result.get(0));
    }

    @Test
    public void monthly_day29_leapYear_returnsActualDay29() {
        LocalDate from = LocalDate.of(2024, 2, 1);
        LocalDate to = LocalDate.of(2024, 2, 29);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.MONTHLY, "29", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2024, 2, 29), result.get(0));
    }

    // =========================================================================
    // MONTHLY — week-position tokens
    // =========================================================================

    @Test
    public void monthly_w1Mon_returnsFirstMondayOfMonth() {
        // May 2026: first Monday is May 4
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.MONTHLY, "W1-MON", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 5, 4), result.get(0));
    }

    @Test
    public void monthly_w2Fri_returnsSecondFridayOfMonth() {
        // May 2026: Fridays are May 1, 8, 15, 22, 29 → 2nd Friday = May 8
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.MONTHLY, "W2-FRI", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 5, 8), result.get(0));
    }

    @Test
    public void monthly_wlFri_returnsLastFridayOfMonth() {
        // May 2026: last Friday = May 29
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.MONTHLY, "WL-FRI", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 5, 29), result.get(0));
    }

    @Test
    public void monthly_w5Mon_returnsNullWhenOnlyFourMondays() {
        // Feb 2026 starts on Sunday. Mondays: Feb 2, 9, 16, 23 — no 5th Monday.
        LocalDate from = LocalDate.of(2026, 2, 1);
        LocalDate to = LocalDate.of(2026, 2, 28);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.MONTHLY, "W5-MON", from, to);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void monthly_combinedNumericAndWeekPosition() {
        // "W1-MON,WL-FRI" for May 2026: May 4 (first Mon) and May 29 (last Fri)
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.MONTHLY, "W1-MON,WL-FRI", from, to);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(LocalDate.of(2026, 5, 4)));
        Assert.assertTrue(result.contains(LocalDate.of(2026, 5, 29)));
    }

    @Test
    public void monthly_rangeFilterApplied_onlyReturnsMatchesInRange() {
        // 15th of each month, range starts mid-month so first hit is excluded
        LocalDate from = LocalDate.of(2026, 5, 16); // after the 15th
        LocalDate to = LocalDate.of(2026, 6, 30);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.MONTHLY, "15", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 6, 15), result.get(0));
    }

    // =========================================================================
    // QUARTERLY — numeric day of quarter
    // =========================================================================

    @Test
    public void quarterly_day1_returnsFirstDayOfEachQuarter() {
        // Q1 2026 = Jan 1; Q2 = Apr 1; Q3 = Jul 1; Q4 = Oct 1
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.QUARTERLY, "1", from, to);
        Assert.assertEquals(4, result.size());
        Assert.assertEquals(LocalDate.of(2026, 1, 1), result.get(0));
        Assert.assertEquals(LocalDate.of(2026, 4, 1), result.get(1));
        Assert.assertEquals(LocalDate.of(2026, 7, 1), result.get(2));
        Assert.assertEquals(LocalDate.of(2026, 10, 1), result.get(3));
    }

    @Test
    public void quarterly_day46_midQuarter() {
        // Q1 2026: Jan 1 + 45 days = Feb 15
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.QUARTERLY, "46", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 2, 15), result.get(0));
    }

    @Test
    public void quarterly_day1And46_twoMatchesPerQuarter() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.QUARTERLY, "1,46", from, to);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(LocalDate.of(2026, 1, 1)));
        Assert.assertTrue(result.contains(LocalDate.of(2026, 2, 15)));
    }

    // =========================================================================
    // QUARTERLY — week-position tokens
    // =========================================================================

    @Test
    public void quarterly_w1Mon_returnsFirstMondayOfQuarter() {
        // Q2 2026 starts Apr 1 (Wed). First Monday of Q2 = Apr 6.
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.QUARTERLY, "W1-MON", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 4, 6), result.get(0));
    }

    @Test
    public void quarterly_wlFri_returnsLastFridayOfQuarter() {
        // Q1 2026 ends Mar 31 (Tue). Last Friday before/on Mar 31 = Mar 27.
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.QUARTERLY, "WL-FRI", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 3, 27), result.get(0));
    }

    @Test
    public void quarterly_w13Fri_lastWeekOfQuarter() {
        // Q1 2026: first Friday = Jan 2. W13-FRI = Jan 2 + 12*7 = Jan 2 + 84 = Mar 27.
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.QUARTERLY, "W13-FRI", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 3, 27), result.get(0));
    }

    @Test
    public void quarterly_multipleQuartersInRange() {
        // W1-MON for full year: one Monday per quarter
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.QUARTERLY, "W1-MON", from, to);
        Assert.assertEquals(4, result.size());
    }

    // =========================================================================
    // YEARLY — MMDD format
    // =========================================================================

    @Test
    public void yearly_0101_returnsJan1() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.YEARLY, "0101", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 1, 1), result.get(0));
    }

    @Test
    public void yearly_1225_returnsDec25() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.YEARLY, "1225", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 12, 25), result.get(0));
    }

    @Test
    public void yearly_multipleTokens_twoMatchesPerYear() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2027, 12, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.YEARLY, "0101,0701", from, to);
        Assert.assertEquals(4, result.size());
        Assert.assertTrue(result.contains(LocalDate.of(2026, 1, 1)));
        Assert.assertTrue(result.contains(LocalDate.of(2026, 7, 1)));
        Assert.assertTrue(result.contains(LocalDate.of(2027, 1, 1)));
        Assert.assertTrue(result.contains(LocalDate.of(2027, 7, 1)));
    }

    @Test
    public void yearly_0229_nonLeapYear_clampedToFeb28() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.YEARLY, "0229", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 2, 28), result.get(0));
    }

    @Test
    public void yearly_0229_leapYear_returnsActualDate() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.YEARLY, "0229", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2024, 2, 29), result.get(0));
    }

    @Test
    public void yearly_rangeExcludesBeforeFrom() {
        // From Jun 1 — should not return Jan 1
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.YEARLY, "0101,1225", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 12, 25), result.get(0));
    }

    // =========================================================================
    // YEARLY — week-position in month format
    // =========================================================================

    @Test
    public void yearly_janW1Mon_returnsFirstMondayOfJanuary() {
        // Jan 2026: first Monday = Jan 5
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.YEARLY, "JAN-W1-MON", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 1, 5), result.get(0));
    }

    @Test
    public void yearly_decWlFri_returnsLastFridayOfDecember() {
        // Dec 2026: last Friday = Dec 25 (Fri)
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.YEARLY, "DEC-WL-FRI", from, to);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(LocalDate.of(2026, 12, 25), result.get(0));
    }

    @Test
    public void yearly_marW1MonAndJunWlFri_twoMatchesPerYear() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.YEARLY,
                "MAR-W1-MON,JUN-WL-FRI", from, to);
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void yearly_multipleYears_returnsOneMatchPerYear() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2027, 12, 31);
        List<LocalDate> result = evaluator.matchingDates(TemplateType.YEARLY, "0701", from, to);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(LocalDate.of(2025, 7, 1), result.get(0));
        Assert.assertEquals(LocalDate.of(2026, 7, 1), result.get(1));
        Assert.assertEquals(LocalDate.of(2027, 7, 1), result.get(2));
    }

    // =========================================================================
    // parseDayOfWeek helper
    // =========================================================================

    @Test
    public void parseDayOfWeek_validTokens() {
        Assert.assertEquals(DayOfWeek.MONDAY, SchedulePatternEvaluator.parseDayOfWeek("MON"));
        Assert.assertEquals(DayOfWeek.TUESDAY, SchedulePatternEvaluator.parseDayOfWeek("TUE"));
        Assert.assertEquals(DayOfWeek.WEDNESDAY, SchedulePatternEvaluator.parseDayOfWeek("WED"));
        Assert.assertEquals(DayOfWeek.THURSDAY, SchedulePatternEvaluator.parseDayOfWeek("THU"));
        Assert.assertEquals(DayOfWeek.FRIDAY, SchedulePatternEvaluator.parseDayOfWeek("FRI"));
        Assert.assertEquals(DayOfWeek.SATURDAY, SchedulePatternEvaluator.parseDayOfWeek("SAT"));
        Assert.assertEquals(DayOfWeek.SUNDAY, SchedulePatternEvaluator.parseDayOfWeek("SUN"));
    }

    @Test
    public void parseDayOfWeek_invalidToken_returnsNull() {
        Assert.assertNull(SchedulePatternEvaluator.parseDayOfWeek("MONDAY"));
        Assert.assertNull(SchedulePatternEvaluator.parseDayOfWeek(""));
        Assert.assertNull(SchedulePatternEvaluator.parseDayOfWeek(null));
    }

    // =========================================================================
    // resolveMonthlyToken direct tests
    // =========================================================================

    @Test
    public void resolveMonthlyToken_numericDay_basic() {
        LocalDate may2026 = LocalDate.of(2026, 5, 1);
        Assert.assertEquals(LocalDate.of(2026, 5, 15), evaluator.resolveMonthlyToken("15", may2026));
    }

    @Test
    public void resolveMonthlyToken_w1Mon() {
        // May 2026: first Monday = May 4
        Assert.assertEquals(LocalDate.of(2026, 5, 4),
                evaluator.resolveMonthlyToken("W1-MON", LocalDate.of(2026, 5, 1)));
    }

    @Test
    public void resolveMonthlyToken_wlFri() {
        // May 2026: last Friday = May 29
        Assert.assertEquals(LocalDate.of(2026, 5, 29),
                evaluator.resolveMonthlyToken("WL-FRI", LocalDate.of(2026, 5, 1)));
    }

    @Test
    public void resolveMonthlyToken_badToken_returnsNull() {
        Assert.assertNull(evaluator.resolveMonthlyToken("BADTOKEN", LocalDate.of(2026, 5, 1)));
        Assert.assertNull(evaluator.resolveMonthlyToken("", LocalDate.of(2026, 5, 1)));
        Assert.assertNull(evaluator.resolveMonthlyToken(null, LocalDate.of(2026, 5, 1)));
    }

    // =========================================================================
    // resolveQuarterlyToken direct tests
    // =========================================================================

    @Test
    public void resolveQuarterlyToken_day1() {
        // Q2 2026 = Apr 1
        Assert.assertEquals(LocalDate.of(2026, 4, 1),
                evaluator.resolveQuarterlyToken("1", LocalDate.of(2026, 4, 1)));
    }

    @Test
    public void resolveQuarterlyToken_w1Mon() {
        // Q2 2026 starts Apr 1 (Wed). First Mon of Q2 = Apr 6.
        Assert.assertEquals(LocalDate.of(2026, 4, 6),
                evaluator.resolveQuarterlyToken("W1-MON", LocalDate.of(2026, 4, 1)));
    }

    @Test
    public void resolveQuarterlyToken_wlFri() {
        // Q1 2026 ends Mar 31 (Tue). Last Fri on/before Mar 31 = Mar 27.
        Assert.assertEquals(LocalDate.of(2026, 3, 27),
                evaluator.resolveQuarterlyToken("WL-FRI", LocalDate.of(2026, 1, 1)));
    }

    // =========================================================================
    // resolveYearlyToken direct tests
    // =========================================================================

    @Test
    public void resolveYearlyToken_mmdd() {
        Assert.assertEquals(LocalDate.of(2026, 7, 4),
                evaluator.resolveYearlyToken("0704", 2026));
    }

    @Test
    public void resolveYearlyToken_janW1Mon() {
        // Jan 2026: first Monday = Jan 5
        Assert.assertEquals(LocalDate.of(2026, 1, 5),
                evaluator.resolveYearlyToken("JAN-W1-MON", 2026));
    }

    @Test
    public void resolveYearlyToken_decWlFri() {
        Assert.assertEquals(LocalDate.of(2026, 12, 25),
                evaluator.resolveYearlyToken("DEC-WL-FRI", 2026));
    }

    @Test
    public void resolveYearlyToken_badFormat_returnsNull() {
        Assert.assertNull(evaluator.resolveYearlyToken("13010", 2026)); // 5 chars, invalid
        Assert.assertNull(evaluator.resolveYearlyToken("BADMON-W1-MON", 2026));
        Assert.assertNull(evaluator.resolveYearlyToken("", 2026));
        Assert.assertNull(evaluator.resolveYearlyToken(null, 2026));
    }
}
