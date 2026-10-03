package org.dandeliondaily.timereview.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * Pins the day-normalization rules the Review page applies, which the MCP time
 * tools also rely on (assessment Phase 4).
 */
public class TimeRegularizationServiceTest {

    private static final ZoneId DENVER = ZoneId.of("America/Denver");
    private static final LocalDate DAY = LocalDate.of(2026, 9, 29);

    private final TimeRegularizationService service = new TimeRegularizationService();
    private WebUser webUser;
    private Date dayStart;
    private Date dayEnd;
    private Date now;

    @Before
    public void setUp() {
        webUser = new WebUser();
        webUser.setTimeZone(TimeZone.getTimeZone(DENVER));
        dayStart = at(0, 0);
        dayEnd = Date.from(DAY.plusDays(1).atStartOfDay(DENVER).toInstant());
        now = Date.from(DAY.plusDays(3).atTime(12, 0).atZone(DENVER).toInstant());
    }

    @Test
    public void roundsChainStartDownAndEndUpToTenMinutes() {
        BillEntry a = entry(1, at(9, 3), at(9, 47));
        BillEntry b = entry(2, at(9, 47), at(10, 12));

        List<BillEntry> changed = normalize(a, b);

        assertTimes(a, at(9, 0), at(9, 47));
        assertTimes(b, at(9, 47), at(10, 20));
        Assert.assertEquals(47, a.getBillMins().intValue());
        Assert.assertEquals(33, b.getBillMins().intValue());
        Assert.assertEquals(2, changed.size());
    }

    @Test
    public void healsSmallGapByExtendingTheEarlierEntry() {
        BillEntry a = entry(1, at(9, 0), at(9, 42));
        BillEntry b = entry(2, at(9, 45), at(10, 0));

        normalize(a, b);

        assertTimes(a, at(9, 0), at(9, 50));
        assertTimes(b, at(9, 50), at(10, 0));
    }

    @Test
    public void leavesLargeGapsAlone() {
        BillEntry a = entry(1, at(9, 0), at(9, 30));
        BillEntry b = entry(2, at(11, 0), at(12, 0));

        List<BillEntry> changed = normalize(a, b);

        assertTimes(a, at(9, 0), at(9, 30));
        assertTimes(b, at(11, 0), at(12, 0));
        Assert.assertTrue(changed.isEmpty());
    }

    @Test
    public void truncatesSecondsToTheMinute() {
        BillEntry a = entry(1, new Date(at(9, 0).getTime() + 30000L), new Date(at(9, 30).getTime() + 45000L));

        normalize(a);

        assertTimes(a, at(9, 0), at(9, 30));
    }

    @Test
    public void pushesOverlappingEntryToStartAfterThePreviousOne() {
        BillEntry a = entry(1, at(9, 0), at(10, 0));
        BillEntry b = entry(2, at(9, 50), at(10, 30));

        normalize(a, b);

        assertTimes(a, at(9, 0), at(10, 0));
        assertTimes(b, at(10, 0), at(10, 30));
    }

    @Test
    public void keepsZeroLengthEntryInsideAChain() {
        BillEntry a = entry(1, at(9, 0), at(9, 20));
        BillEntry zero = entry(2, at(9, 20), at(9, 20));
        BillEntry b = entry(3, at(9, 20), at(9, 40));

        normalize(a, zero, b);

        assertTimes(a, at(9, 0), at(9, 20));
        assertTimes(zero, at(9, 20), at(9, 20));
        assertTimes(b, at(9, 20), at(9, 40));
        Assert.assertEquals(0, zero.getBillMins() == null ? 0 : zero.getBillMins().intValue());
    }

    @Test
    public void unchangedDayReportsNoChangesAndLeavesBillMinsAlone() {
        BillEntry a = entry(1, at(9, 0), at(9, 30));
        a.setBillMins(Integer.valueOf(999));

        List<BillEntry> changed = normalize(a);

        Assert.assertTrue(changed.isEmpty());
        Assert.assertEquals(999, a.getBillMins().intValue());
    }

    private List<BillEntry> normalize(BillEntry... entries) {
        List<BillEntry> list = new ArrayList<BillEntry>(Arrays.asList(entries));
        return service.computeNormalizedTimes(webUser, list, dayStart, dayEnd, null, now);
    }

    private static BillEntry entry(int id, Date start, Date end) {
        BillEntry entry = new BillEntry();
        entry.setBillId(id);
        entry.setStartTime(start);
        entry.setEndTime(end);
        return entry;
    }

    private static Date at(int hour, int minute) {
        return Date.from(ZonedDateTime.of(DAY, LocalTime.of(hour, minute), DENVER).toInstant());
    }

    private static void assertTimes(BillEntry entry, Date start, Date end) {
        Assert.assertEquals("start of " + entry.getBillId(), start, entry.getStartTime());
        Assert.assertEquals("end of " + entry.getBillId(), end, entry.getEndTime());
    }
}
