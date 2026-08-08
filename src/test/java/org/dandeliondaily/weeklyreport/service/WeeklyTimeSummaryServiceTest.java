package org.dandeliondaily.weeklyreport.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.dandeliondaily.weeklyreport.model.WeeklyTimeSummary;
import org.dandeliondaily.weeklyreport.service.WeeklyTimeSummaryService.TimeRow;
import org.junit.Assert;
import org.junit.Test;

public class WeeklyTimeSummaryServiceTest {
    private final WeeklyTimeSummaryService service = new WeeklyTimeSummaryService();

    @Test
    public void roundsEachProjectOncePerSundayThroughSaturdayWeek() {
        LocalDate sunday = LocalDate.of(2026, 7, 26);
        List<TimeRow> rows = Arrays.asList(
                new TimeRow(sunday, 1, "One", "one", "AIRA", 61),
                new TimeRow(sunday.plusDays(2), 1, "One", "one", "AIRA", 5),
                new TimeRow(sunday.plusDays(6), 2, "Two", "two", "OTHER", 23));

        WeeklyTimeSummary result = service.summarizeRows(rows, "AIRA", sunday, 1).get(0);

        Assert.assertEquals(90, result.getAllWorkedMinutes());
        Assert.assertEquals(60, result.getScopedMinutes());
        Assert.assertEquals(Integer.valueOf(60), result.getMinutesByBillCode().get("AIRA"));
    }

    @Test
    public void roundsIndependentlyAcrossWeeksAndTreatsPrefixLiterally() {
        LocalDate sunday = LocalDate.of(2026, 7, 19);
        List<TimeRow> rows = Arrays.asList(
                new TimeRow(sunday, 1, "One", null, "AIRA_1", 7),
                new TimeRow(sunday.plusWeeks(1), 1, "One", null, "AIRA_1", 7),
                new TimeRow(sunday.plusWeeks(1), 2, "Two", null, "AIRA%2", 7));

        List<WeeklyTimeSummary> result = service.summarizeRows(rows, "AIRA_", sunday, 2);

        Assert.assertEquals(30, result.get(0).getScopedMinutes());
        Assert.assertEquals(30, result.get(1).getScopedMinutes());
        Assert.assertEquals(60, result.get(1).getAllWorkedMinutes());
    }

    @Test
    public void distributesOneRoundedProjectTotalAcrossHistoricalCodes() {
        LocalDate sunday = LocalDate.of(2026, 7, 26);
        List<TimeRow> rows = Arrays.asList(
                new TimeRow(sunday, 1, "One", null, "AIRA", 40),
                new TimeRow(sunday.plusDays(1), 1, "One", null, "AIRA-X", 20));

        WeeklyTimeSummary result = service.summarizeRows(rows, "AIRA", sunday, 1).get(0);

        Assert.assertEquals(60, result.getAllWorkedMinutes());
        Assert.assertEquals(60, result.getScopedMinutes());
        Assert.assertEquals(Integer.valueOf(40), result.getMinutesByBillCode().get("AIRA"));
        Assert.assertEquals(Integer.valueOf(20), result.getMinutesByBillCode().get("AIRA-X"));
    }
}