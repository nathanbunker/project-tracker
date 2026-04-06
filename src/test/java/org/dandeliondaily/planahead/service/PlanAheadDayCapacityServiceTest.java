package org.dandeliondaily.planahead.service;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PlanAheadDayCapacityServiceTest {

    private PlanAheadDayCapacityService service;

    @Before
    public void setUp() {
        service = new PlanAheadDayCapacityService();
    }

    // --- getStatusLabel ---

    @Test
    public void getStatusLabel_working() {
        Assert.assertEquals("Working", service.getStatusLabel("W"));
    }

    @Test
    public void getStatusLabel_notWorking() {
        Assert.assertEquals("Not Working", service.getStatusLabel("N"));
    }

    @Test
    public void getStatusLabel_vacation() {
        Assert.assertEquals("Vacation", service.getStatusLabel("V"));
    }

    @Test
    public void getStatusLabel_holiday() {
        Assert.assertEquals("Holiday", service.getStatusLabel("H"));
    }

    @Test
    public void getStatusLabel_traveling() {
        Assert.assertEquals("Traveling", service.getStatusLabel("T"));
    }

    @Test
    public void getStatusLabel_sick() {
        Assert.assertEquals("Sick", service.getStatusLabel("S"));
    }

    @Test
    public void getStatusLabel_unknownCodeDefaultsToWorking() {
        Assert.assertEquals("Working", service.getStatusLabel("X"));
    }

    @Test
    public void getStatusLabel_nullDefaultsToWorking() {
        Assert.assertEquals("Working", service.getStatusLabel(null));
    }

    // --- getAllowedStatuses ---

    @Test
    public void getAllowedStatuses_returnsSixCodes() {
        List<String> statuses = service.getAllowedStatuses();
        Assert.assertEquals(6, statuses.size());
    }

    @Test
    public void getAllowedStatuses_containsAllSixCodes() {
        List<String> statuses = service.getAllowedStatuses();
        Assert.assertTrue(statuses.contains("W"));
        Assert.assertTrue(statuses.contains("N"));
        Assert.assertTrue(statuses.contains("V"));
        Assert.assertTrue(statuses.contains("H"));
        Assert.assertTrue(statuses.contains("T"));
        Assert.assertTrue(statuses.contains("S"));
    }

    // --- saveDayCapacity validation (exception thrown before any DB access) ---

    @Test(expected = IllegalArgumentException.class)
    public void saveDayCapacity_throwsForInvalidStatusCode() {
        // AppReq is null; exception fires before getDataSession() is called
        service.saveDayCapacity(null, new Date(), 480, "X");
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveDayCapacity_throwsForEmptyStatusCode() {
        service.saveDayCapacity(null, new Date(), 480, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveDayCapacity_throwsForNullStatusCode() {
        service.saveDayCapacity(null, new Date(), 480, null);
    }

    @Test
    public void loadTargetMinutesForDay_nullAppReqFallsBackToDefaultDailyTarget() {
        int minutes = service.loadTargetMinutesForDay(null, new Date());
        Assert.assertEquals(PlanAheadDayCapacityService.DEFAULT_DAILY_TARGET_MINUTES, minutes);
    }

    @Test
    public void loadTargetMinutesForCurrentWeek_nullAppReqFallsBackToDefaultWeeklyTarget() {
        int minutes = service.loadTargetMinutesForCurrentWeek(null);
        Assert.assertEquals(PlanAheadDayCapacityService.DEFAULT_WEEKLY_TARGET_MINUTES, minutes);
    }

    @Test
    public void loadTargetMinutesForWeek_nullAppReqFallsBackToDefaultWeeklyTarget() {
        int minutes = service.loadTargetMinutesForWeek(null, new Date());
        Assert.assertEquals(PlanAheadDayCapacityService.DEFAULT_WEEKLY_TARGET_MINUTES, minutes);
    }
}
