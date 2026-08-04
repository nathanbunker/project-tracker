package org.dandeliondaily.planahead.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.BillExpected;
import org.openimmunizationsoftware.pt.model.BillExpectedId;

public class TemplateWorkdayCalendarTest {

    @Test
    public void missingAvailabilityUsesWeekdayDefaultsForBillableTemplates() {
        TemplateWorkdayCalendar calendar = new TemplateWorkdayCalendar(Collections.<BillExpected>emptyList());

        Assert.assertTrue(calendar.isEligible(true, LocalDate.of(2026, 8, 3)));
        Assert.assertFalse(calendar.isEligible(true, LocalDate.of(2026, 8, 8)));
    }

    @Test
    public void explicitAvailabilityRequiresWorkingStatusAndPositiveMinutes() {
        LocalDate workingWithNoHours = LocalDate.of(2026, 8, 3);
        LocalDate unavailableWithHours = LocalDate.of(2026, 8, 4);
        LocalDate workingWithHours = LocalDate.of(2026, 8, 8);
        TemplateWorkdayCalendar calendar = new TemplateWorkdayCalendar(Arrays.asList(
                availability(workingWithNoHours, 0, "W"),
                availability(unavailableWithHours, 480, "N"),
                availability(workingWithHours, 480, "W")));

        Assert.assertFalse(calendar.isEligible(true, workingWithNoHours));
        Assert.assertFalse(calendar.isEligible(true, unavailableWithHours));
        Assert.assertTrue(calendar.isEligible(true, workingWithHours));
    }

    @Test
    public void personalTemplatesIgnoreWorkdayAvailability() {
        LocalDate sunday = LocalDate.of(2026, 8, 9);
        TemplateWorkdayCalendar calendar = new TemplateWorkdayCalendar(Arrays.asList(
                availability(sunday, 0, "N")));

        Assert.assertTrue(calendar.isEligible(false, sunday));
    }

    private BillExpected availability(LocalDate date, int minutes, String status) {
        return new BillExpected(new BillExpectedId(7, Date.valueOf(date)), minutes, 0, status);
    }
}