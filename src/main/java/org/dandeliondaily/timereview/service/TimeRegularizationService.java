package org.dandeliondaily.timereview.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.WebUser;

public class TimeRegularizationService {

    private static final Logger LOGGER = Logger.getLogger(TimeRegularizationService.class.getName());
    private static final long SAFETY_MILLIS = 2L * 60L * 1000L;
    private static final long TEN_MINUTES_MILLIS = 10L * 60L * 1000L;

    public Date toDayStart(WebUser webUser, Date value) {
        Calendar calendar = TimeTracker.createToday(webUser);
        if (value != null) {
            calendar.setTime(value);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public Date toNextDay(WebUser webUser, Date value) {
        Calendar calendar = TimeTracker.createToday(webUser);
        calendar.setTime(toDayStart(webUser, value));
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    @SuppressWarnings("unchecked")
    public List<BillEntry> loadEntriesForDay(WebUser webUser, Session dataSession, Date dayStart) {
        Date dayEnd = toNextDay(webUser, dayStart);
        Query query = dataSession.createQuery(
                "from BillEntry where webUser = :webUser and startTime >= :dayStart and startTime < :dayEnd "
                        + "order by startTime");
        query.setParameter("webUser", webUser);
        query.setParameter("dayStart", dayStart);
        query.setParameter("dayEnd", dayEnd);
        return query.list();
    }

    public int cleanupZeroMinuteEntries(WebUser webUser, Session dataSession, Integer preserveBillEntryId) {
        Transaction trans = dataSession.beginTransaction();
        try {
            Query cleanupQuery;
            if (preserveBillEntryId != null) {
                cleanupQuery = dataSession.createQuery(
                        "delete from BillEntry where webUser = :webUser and billMins = 0 and billId <> :billId");
                cleanupQuery.setParameter("billId", preserveBillEntryId);
            } else {
                cleanupQuery = dataSession.createQuery(
                        "delete from BillEntry where webUser = :webUser and billMins = 0");
            }
            cleanupQuery.setParameter("webUser", webUser);
            int deleted = cleanupQuery.executeUpdate();
            trans.commit();
            return deleted;
        } catch (RuntimeException e) {
            if (trans != null && trans.isActive()) {
                trans.rollback();
            }
            throw e;
        }
    }

    public void normalizeDayEntries(
            WebUser webUser,
            Session dataSession,
            List<BillEntry> billEntryList,
            Date dayStart,
            Date dayEnd,
            Integer lockedBillEntryId) {

        if (billEntryList == null || billEntryList.isEmpty()) {
            return;
        }

        BillEntry activeEntry = resolveActiveEntry(billEntryList, lockedBillEntryId);
        Date now = new Date();
        boolean isToday = now.after(dayStart) && now.before(dayEnd);

        if (activeEntry == null && isToday) {
            BillEntry lastEntry = billEntryList.get(billEntryList.size() - 1);
            if (within10Minutes(lastEntry.getEndTime(), now)) {
                activeEntry = lastEntry;
            }
        }

        List<Chain> chains = buildChains(billEntryList);
        List<BillEntry> changedEntries = new ArrayList<BillEntry>();

        for (BillEntry entry : billEntryList) {
            if (entry == activeEntry) {
                continue;
            }
            updateStartTime(entry, truncateToMinute(webUser, entry.getStartTime()), dayStart, dayEnd, changedEntries);
            updateEndTime(entry, truncateToMinute(webUser, entry.getEndTime()), dayStart, dayEnd, isToday, now,
                    false, changedEntries);
        }

        for (Chain chain : chains) {
            BillEntry firstEntry = billEntryList.get(chain.startIndex);
            BillEntry lastEntryInChain = billEntryList.get(chain.endIndex);

            Date roundedStart = roundDown10(webUser, firstEntry.getStartTime());
            updateStartTime(firstEntry, roundedStart, dayStart, dayEnd, changedEntries);

            boolean allowFutureEnd = activeEntry != null && lastEntryInChain.getBillId() == activeEntry.getBillId();
            if (!allowFutureEnd) {
                Date roundedEnd = roundUp10(webUser, lastEntryInChain.getEndTime());
                updateEndTime(lastEntryInChain, roundedEnd, dayStart, dayEnd, isToday, now, false, changedEntries);
            }
        }

        for (int i = 0; i < chains.size() - 1; i++) {
            BillEntry aLast = billEntryList.get(chains.get(i).endIndex);
            BillEntry bFirst = billEntryList.get(chains.get(i + 1).startIndex);
            Date aEnd = aLast.getEndTime();
            Date bStart = bFirst.getStartTime();

            if (bStart == null || aEnd == null) {
                continue;
            }

            if (!bStart.after(aEnd)) {
                if (bStart.getTime() != aEnd.getTime()) {
                    updateStartTime(bFirst, aEnd, dayStart, dayEnd, changedEntries);
                }
            } else {
                if (sameTenMinuteBucket(webUser, aEnd, bStart)) {
                    updateStartTime(bFirst, aEnd, dayStart, dayEnd, changedEntries);
                } else {
                    Date roundedAEnd = roundUp10(webUser, aEnd);
                    if (roundedAEnd.after(bStart)) {
                        roundedAEnd = bStart;
                    }
                    updateEndTime(aLast, roundedAEnd, dayStart, dayEnd, isToday, now, false, changedEntries);
                    Date newAEnd = aLast.getEndTime();
                    long gapMillis = bStart.getTime() - newAEnd.getTime();
                    if (gapMillis < TEN_MINUTES_MILLIS
                            && (bFirst.getStartTime().after(newAEnd) || bFirst.getStartTime().before(newAEnd))) {
                        updateStartTime(bFirst, newAEnd, dayStart, dayEnd, changedEntries);
                    }
                }
            }
        }

        BillEntry previous = null;
        for (BillEntry entry : billEntryList) {
            Date startTime = clampToDay(entry.getStartTime(), dayStart, dayEnd);
            Date endTime = clampToDay(entry.getEndTime(), dayStart, dayEnd);

            if (previous != null && startTime.before(previous.getEndTime())) {
                startTime = previous.getEndTime();
            }
            if (startTime.after(endTime)) {
                endTime = startTime;
            }

            updateStartTime(entry, startTime, dayStart, dayEnd, changedEntries);
            boolean allowFutureEnd = activeEntry != null && entry.getBillId() == activeEntry.getBillId();
            updateEndTime(entry, endTime, dayStart, dayEnd, isToday, now, allowFutureEnd, changedEntries);
            previous = entry;
        }

        if (changedEntries.isEmpty()) {
            return;
        }

        for (BillEntry entry : billEntryList) {
            entry.setBillMins(TimeTracker.calculateMins(entry));
        }

        Transaction trans = dataSession.beginTransaction();
        try {
            for (BillEntry entry : billEntryList) {
                dataSession.update(entry);
            }
            trans.commit();
        } catch (RuntimeException e) {
            if (trans != null && trans.isActive()) {
                trans.rollback();
            }
            throw e;
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE,
                    "Normalized bill entries for user={0}, dayStart={1}, dayEnd={2}, chains={3}, updates={4}",
                    new Object[] { webUser.getUsername(), dayStart, dayEnd, chains.size(), changedEntries.size() });
        }
    }

    private BillEntry resolveActiveEntry(List<BillEntry> entries, Integer lockedBillEntryId) {
        if (lockedBillEntryId == null) {
            return null;
        }
        for (BillEntry entry : entries) {
            if (entry.getBillId() == lockedBillEntryId.intValue()) {
                return entry;
            }
        }
        return null;
    }

    private List<Chain> buildChains(List<BillEntry> billEntryList) {
        List<Chain> chains = new ArrayList<Chain>();
        int chainStart = 0;
        for (int i = 1; i < billEntryList.size(); i++) {
            BillEntry previous = billEntryList.get(i - 1);
            BillEntry current = billEntryList.get(i);
            if (!isSameMinute(previous.getEndTime(), current.getStartTime())) {
                chains.add(new Chain(chainStart, i - 1));
                chainStart = i;
            }
        }
        chains.add(new Chain(chainStart, billEntryList.size() - 1));
        return chains;
    }

    private void updateStartTime(BillEntry entry, Date newStart, Date dayStart, Date dayEnd,
            List<BillEntry> changedEntries) {
        Date clamped = clampToDay(newStart, dayStart, dayEnd);
        if (entry.getStartTime() == null || entry.getStartTime().getTime() != clamped.getTime()) {
            entry.setStartTime(clamped);
            if (!changedEntries.contains(entry)) {
                changedEntries.add(entry);
            }
        }
    }

    private void updateEndTime(BillEntry entry, Date newEnd, Date dayStart, Date dayEnd,
            boolean isToday, Date now, boolean allowFutureEnd, List<BillEntry> changedEntries) {
        Date adjusted = clampToDay(newEnd, dayStart, dayEnd);
        if (isToday && !allowFutureEnd) {
            Date cap = new Date(now.getTime() + SAFETY_MILLIS);
            if (adjusted.after(cap)) {
                adjusted = now;
            }
        }
        if (entry.getEndTime() == null || entry.getEndTime().getTime() != adjusted.getTime()) {
            entry.setEndTime(adjusted);
            if (!changedEntries.contains(entry)) {
                changedEntries.add(entry);
            }
        }
    }

    private Date clampToDay(Date value, Date dayStart, Date dayEnd) {
        if (value == null) {
            return dayStart;
        }
        if (value.before(dayStart)) {
            return dayStart;
        }
        if (value.after(dayEnd)) {
            return dayEnd;
        }
        return value;
    }

    private Date truncateToMinute(WebUser webUser, Date time) {
        if (time == null) {
            return null;
        }
        Calendar calendar = webUser.getCalendar();
        calendar.setTime(time);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date roundDown10(WebUser webUser, Date time) {
        Calendar calendar = webUser.getCalendar();
        calendar.setTime(time);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        int minute = calendar.get(Calendar.MINUTE);
        int roundedMinute = (minute / 10) * 10;
        calendar.set(Calendar.MINUTE, roundedMinute);
        return calendar.getTime();
    }

    private Date roundUp10(WebUser webUser, Date time) {
        Calendar calendar = webUser.getCalendar();
        calendar.setTime(time);
        int minute = calendar.get(Calendar.MINUTE);
        int mod = minute % 10;
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (mod == 0) {
            return calendar.getTime();
        }
        calendar.add(Calendar.MINUTE, 10 - mod);
        return calendar.getTime();
    }

    private boolean sameTenMinuteBucket(WebUser webUser, Date first, Date second) {
        Calendar calendar = webUser.getCalendar();
        calendar.setTime(first);
        int firstBucket = calendar.get(Calendar.HOUR_OF_DAY) * 6 + calendar.get(Calendar.MINUTE) / 10;
        calendar.setTime(second);
        int secondBucket = calendar.get(Calendar.HOUR_OF_DAY) * 6 + calendar.get(Calendar.MINUTE) / 10;
        return firstBucket == secondBucket;
    }

    public static boolean isSameMinute(Date first, Date second) {
        if (first == null || second == null) {
            return false;
        }
        long firstMinute = first.getTime() / 60000L;
        long secondMinute = second.getTime() / 60000L;
        return firstMinute == secondMinute;
    }

    private boolean within10Minutes(Date first, Date second) {
        if (first == null || second == null) {
            return false;
        }
        long diff = Math.abs(first.getTime() - second.getTime());
        return diff <= TEN_MINUTES_MILLIS;
    }

    private static final class Chain {
        private final int startIndex;
        private final int endIndex;

        private Chain(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }
}
