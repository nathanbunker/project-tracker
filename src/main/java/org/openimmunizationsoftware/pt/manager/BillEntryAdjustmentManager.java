package org.openimmunizationsoftware.pt.manager;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openimmunizationsoftware.pt.model.BillEntry;

public class BillEntryAdjustmentManager {

    public static void applyAdjustedTimes(List<BillEntry> dayEntries, BillEntry editedEntry, Date newStart,
            Date newEnd, Date originalStart, Date originalEnd) {
        if (dayEntries == null || dayEntries.isEmpty()) {
            return;
        }

        int editedIndex = -1;
        BillEntry entryFromList = null;
        for (int i = 0; i < dayEntries.size(); i++) {
            BillEntry entry = dayEntries.get(i);
            if (entry.getBillId() == editedEntry.getBillId()) {
                editedIndex = i;
                entryFromList = entry;
                break;
            }
        }
        if (editedIndex == -1 || entryFromList == null) {
            return;
        }

        BillEntry previous = editedIndex > 0 ? dayEntries.get(editedIndex - 1) : null;
        BillEntry next = editedIndex < dayEntries.size() - 1 ? dayEntries.get(editedIndex + 1) : null;
        boolean previousContiguous = previous != null && isSameMinute(previous.getEndTime(), originalStart);
        boolean nextContiguous = next != null && isSameMinute(next.getStartTime(), originalEnd);

        entryFromList.setStartTime(newStart);
        entryFromList.setEndTime(newEnd);

        Set<BillEntry> changed = new HashSet<BillEntry>();
        changed.add(entryFromList);

        if (previous != null && newStart.after(originalStart) && previousContiguous) {
            previous.setEndTime(newStart);
            if (previous.getStartTime().after(previous.getEndTime())) {
                previous.setStartTime(newStart);
                previous.setEndTime(newStart);
            }
            changed.add(previous);
        }

        if (next != null && newEnd.before(originalEnd) && nextContiguous) {
            next.setStartTime(newEnd);
            if (next.getStartTime().after(next.getEndTime())) {
                next.setStartTime(newEnd);
                next.setEndTime(newEnd);
            }
            changed.add(next);
        }

        Date boundaryStart = entryFromList.getStartTime();
        for (int i = editedIndex - 1; i >= 0; i--) {
            BillEntry entry = dayEntries.get(i);
            if (entry.getEndTime().after(boundaryStart)) {
                entry.setEndTime(boundaryStart);
                if (entry.getStartTime().after(entry.getEndTime())) {
                    entry.setStartTime(boundaryStart);
                    entry.setEndTime(boundaryStart);
                }
                changed.add(entry);
            }
            boundaryStart = entry.getStartTime();
        }

        Date boundaryEnd = entryFromList.getEndTime();
        for (int i = editedIndex + 1; i < dayEntries.size(); i++) {
            BillEntry entry = dayEntries.get(i);
            if (entry.getStartTime().before(boundaryEnd)) {
                entry.setStartTime(boundaryEnd);
                if (entry.getStartTime().after(entry.getEndTime())) {
                    entry.setStartTime(boundaryEnd);
                    entry.setEndTime(boundaryEnd);
                }
                changed.add(entry);
            }
            boundaryEnd = entry.getEndTime();
        }

        for (BillEntry entry : changed) {
            entry.setBillMins(TimeTracker.calculateMins(entry));
        }
    }

    private static boolean isSameMinute(Date first, Date second) {
        if (first == null || second == null) {
            return false;
        }
        long firstMinute = first.getTime() / 60000L;
        long secondMinute = second.getTime() / 60000L;
        return firstMinute == secondMinute;
    }
}