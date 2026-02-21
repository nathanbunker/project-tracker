package org.openimmunizationsoftware.pt.manager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.BillEntry;

public class BillEntryAdjustmentManagerTest {

    private static final String BASE_DATE = "2026-02-10 ";

    @Test
    public void shouldShiftMeetingEarlierAndTrimPreviousEntry() {
        List<BillEntry> before = schedule()
                .chainStarts("08:00", "09:15", "11:30").until("12:30").billable(true, true, false)
                .chainStarts("12:30", "15:00", "15:30").until("17:00").billable(true, false, true)
                .build();

        List<BillEntry> expected = schedule()
                .chainStarts("08:00", "09:15", "11:30").until("12:30").billable(true, true, false)
                .chainStarts("12:30", "14:30", "15:30").until("17:00").billable(true, false, true)
                .build();

        assertAdjustmentMatches(before, 4, "14:30", "15:30", expected);
    }

    @Test
    public void shouldShiftMeetingLaterAndExtendPreviousWhenContiguous() {
        List<BillEntry> before = schedule()
                .add("12:30", "14:00", true)
                .add("14:00", "15:00", false)
                .add("15:00", "17:00", true)
                .build();

        List<BillEntry> expected = schedule()
                .add("12:30", "15:00", true)
                .add("15:00", "16:00", false)
                .add("16:00", "17:00", true)
                .build();

        assertAdjustmentMatches(before, 1, "15:00", "16:00", expected);
    }

    @Test
    public void shouldSupportChainEndsDslForReadableFixtures() {
        List<BillEntry> before = schedule()
                .chainEnds("09:00", "10:00", "11:30").from("08:00").billable(true, false, true)
                .build();

        List<BillEntry> expected = schedule()
                .chainEnds("09:00", "09:30", "11:30").from("08:00").billable(true, false, true)
                .build();

        assertAdjustmentMatches(before, 1, "09:00", "09:30", expected);
    }

    private static void assertAdjustmentMatches(List<BillEntry> beforeEntries, int editedIndex,
            String newStart, String newEnd, List<BillEntry> expectedEntries) {
        List<BillEntry> actualEntries = copyEntries(beforeEntries);
        BillEntry editedEntry = actualEntries.get(editedIndex);
        Date originalStart = editedEntry.getStartTime();
        Date originalEnd = editedEntry.getEndTime();

        BillEntryAdjustmentManager.applyAdjustedTimes(actualEntries, editedEntry, at(newStart), at(newEnd),
                originalStart, originalEnd);

        assertTimelineEquals(expectedEntries, actualEntries);
    }

    private static void assertTimelineEquals(List<BillEntry> expectedEntries, List<BillEntry> actualEntries) {
        Assert.assertEquals("Entry count differs", expectedEntries.size(), actualEntries.size());
        for (int i = 0; i < expectedEntries.size(); i++) {
            BillEntry expected = expectedEntries.get(i);
            BillEntry actual = actualEntries.get(i);
            Assert.assertEquals("Start mismatch at index " + i, asMinuteKey(expected.getStartTime()),
                    asMinuteKey(actual.getStartTime()));
            Assert.assertEquals("End mismatch at index " + i, asMinuteKey(expected.getEndTime()),
                    asMinuteKey(actual.getEndTime()));
            Assert.assertEquals("Billable mismatch at index " + i, expected.getBillable(), actual.getBillable());
            Assert.assertEquals("Minutes mismatch at index " + i, expected.getBillMins(), actual.getBillMins());
        }
    }

    private static String asMinuteKey(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private static ScheduleBuilder schedule() {
        return new ScheduleBuilder();
    }

    private static List<BillEntry> copyEntries(List<BillEntry> source) {
        List<BillEntry> copy = new ArrayList<BillEntry>();
        for (BillEntry entry : source) {
            BillEntry cloned = new BillEntry();
            cloned.setBillId(entry.getBillId());
            cloned.setStartTime(entry.getStartTime());
            cloned.setEndTime(entry.getEndTime());
            cloned.setBillable(entry.getBillable());
            cloned.setBillMins(entry.getBillMins());
            copy.add(cloned);
        }
        return copy;
    }

    private static Date at(String time) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(BASE_DATE + time);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid time: " + time, e);
        }
    }

    private static class ScheduleBuilder {
        private final List<BillEntry> entries = new ArrayList<BillEntry>();
        private int nextBillId = 1;

        private ScheduleBuilder add(String start, String end, boolean billable) {
            BillEntry entry = new BillEntry();
            entry.setBillId(nextBillId++);
            entry.setStartTime(at(start));
            entry.setEndTime(at(end));
            entry.setBillable(billable ? "Y" : "N");
            entry.setBillMins(TimeTracker.calculateMins(entry));
            entries.add(entry);
            return this;
        }

        private StartsChain chainStarts(String... starts) {
            return new StartsChain(this, starts);
        }

        private EndsChain chainEnds(String... ends) {
            return new EndsChain(this, ends);
        }

        private List<BillEntry> build() {
            return entries;
        }
    }

    private static class StartsChain {
        private final ScheduleBuilder builder;
        private final String[] starts;
        private String finalEnd;

        private StartsChain(ScheduleBuilder builder, String[] starts) {
            if (starts == null || starts.length == 0) {
                throw new IllegalArgumentException("starts must include at least one time");
            }
            this.builder = builder;
            this.starts = starts;
        }

        private StartsChain until(String finalEnd) {
            this.finalEnd = finalEnd;
            return this;
        }

        private ScheduleBuilder billable(boolean... flags) {
            if (finalEnd == null) {
                throw new IllegalStateException("Call until(finalEnd) before billable(...)");
            }
            if (flags == null || flags.length != starts.length) {
                throw new IllegalArgumentException("billable flags must match starts count");
            }
            for (int i = 0; i < starts.length; i++) {
                String entryEnd = i == starts.length - 1 ? finalEnd : starts[i + 1];
                builder.add(starts[i], entryEnd, flags[i]);
            }
            return builder;
        }
    }

    private static class EndsChain {
        private final ScheduleBuilder builder;
        private final String[] ends;
        private String firstStart;

        private EndsChain(ScheduleBuilder builder, String[] ends) {
            if (ends == null || ends.length == 0) {
                throw new IllegalArgumentException("ends must include at least one time");
            }
            this.builder = builder;
            this.ends = ends;
        }

        private EndsChain from(String firstStart) {
            this.firstStart = firstStart;
            return this;
        }

        private ScheduleBuilder billable(boolean... flags) {
            if (firstStart == null) {
                throw new IllegalStateException("Call from(firstStart) before billable(...)");
            }
            if (flags == null || flags.length != ends.length) {
                throw new IllegalArgumentException("billable flags must match ends count");
            }
            String currentStart = firstStart;
            for (int i = 0; i < ends.length; i++) {
                builder.add(currentStart, ends[i], flags[i]);
                currentStart = ends[i];
            }
            return builder;
        }
    }
}