package org.dandeliondaily.outlook.service;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

public class PlanningOutlookServiceTest {

    @Test
    public void normalizesValidPeriodTypesCaseInsensitively() {
        PlanningOutlookService service = new PlanningOutlookService();
        Assert.assertEquals("WEEK", service.normalizePeriodType("week"));
        Assert.assertEquals("MONTH", service.normalizePeriodType(" Month "));
    }

    @Test
    public void rejectsInvalidPeriodType() {
        PlanningOutlookService service = new PlanningOutlookService();
        Assert.assertNull(service.normalizePeriodType("QUARTER"));
        Assert.assertNull(service.normalizePeriodType(null));
    }

    @Test
    public void weekPeriodEndsSixDaysAfterStart() {
        PlanningOutlookService service = new PlanningOutlookService();
        LocalDate sunday = LocalDate.of(2026, 9, 20);
        Assert.assertEquals(LocalDate.of(2026, 9, 26), service.periodEnd("WEEK", sunday));
    }

    @Test
    public void monthPeriodEndsOnLastCalendarDay() {
        PlanningOutlookService service = new PlanningOutlookService();
        Assert.assertEquals(LocalDate.of(2026, 2, 28), service.periodEnd("MONTH", LocalDate.of(2026, 2, 1)));
        Assert.assertEquals(LocalDate.of(2026, 9, 30), service.periodEnd("MONTH", LocalDate.of(2026, 9, 1)));
    }

    @Test
    public void weekIsEditableThroughItsLastDayThenFrozen() {
        PlanningOutlookService service = new PlanningOutlookService();
        LocalDate sunday = LocalDate.of(2026, 9, 20);
        Assert.assertFalse(service.isFrozen("WEEK", sunday, LocalDate.of(2026, 9, 26)));
        Assert.assertTrue(service.isFrozen("WEEK", sunday, LocalDate.of(2026, 9, 27)));
    }

    @Test
    public void monthIsEditableThroughItsLastDayThenFrozen() {
        PlanningOutlookService service = new PlanningOutlookService();
        LocalDate firstOfMonth = LocalDate.of(2026, 9, 1);
        Assert.assertFalse(service.isFrozen("MONTH", firstOfMonth, LocalDate.of(2026, 9, 30)));
        Assert.assertTrue(service.isFrozen("MONTH", firstOfMonth, LocalDate.of(2026, 10, 1)));
    }
}
