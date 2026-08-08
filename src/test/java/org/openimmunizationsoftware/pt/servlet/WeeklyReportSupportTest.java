package org.openimmunizationsoftware.pt.servlet;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

public class WeeklyReportSupportTest {
    @Test
    public void selectsMostRecentlyCompletedSundayThroughSaturdayWeek() {
        Assert.assertEquals(LocalDate.of(2026, 7, 26),
                WeeklyReportSupport.latestCompletedWeekSunday(LocalDate.of(2026, 8, 3)));
        Assert.assertEquals(LocalDate.of(2026, 7, 26),
                WeeklyReportSupport.latestCompletedWeekSunday(LocalDate.of(2026, 8, 2)));
    }

    @Test
    public void normalizesArbitraryDatesAndClampsIncompleteWeeks() {
        LocalDate today = LocalDate.of(2026, 8, 8);
        Assert.assertEquals(LocalDate.of(2026, 7, 26), WeeklyReportSupport.normalizeWeek(null, today));
        Assert.assertEquals(LocalDate.of(2026, 7, 19), WeeklyReportSupport.normalizeWeek("2026-07-23", today));
        Assert.assertEquals(LocalDate.of(2026, 7, 26), WeeklyReportSupport.normalizeWeek("2026-08-03", today));
        Assert.assertEquals(LocalDate.of(2026, 7, 26), WeeklyReportSupport.normalizeWeek("not-a-date", today));
    }

    @Test
    public void accessKeysUseThirtyTwoRandomBytesAndStableSha256Hashes() {
        String first = WeeklyReportSupport.generateAccessKey();
        String second = WeeklyReportSupport.generateAccessKey();
        Assert.assertEquals(43, first.length());
        Assert.assertNotEquals(first, second);
        Assert.assertEquals(64, WeeklyReportSupport.hashAccessKey(first).length());
        Assert.assertEquals(WeeklyReportSupport.hashAccessKey(first), WeeklyReportSupport.hashAccessKey(first));
    }
}