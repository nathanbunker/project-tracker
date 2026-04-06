package org.dandeliondaily.planahead.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.BillExpected;
import org.openimmunizationsoftware.pt.model.BillExpectedId;
import org.openimmunizationsoftware.pt.model.WebUser;

public class PlanAheadDayCapacityService {

    public static final int DEFAULT_DAILY_TARGET_MINUTES = 8 * 60;
    public static final int DEFAULT_WEEKLY_TARGET_MINUTES = Math.round(37.5f * 60f);

    public static final String STATUS_WORKING = "W";
    public static final String STATUS_NOT_WORKING = "N";
    public static final String STATUS_VACATION = "V";
    public static final String STATUS_HOLIDAY = "H";
    public static final String STATUS_TRAVELING = "T";
    public static final String STATUS_SICK = "S";

    public static class DayCapacity {
        private Date day;
        private int billMins;
        private String workStatusCode;

        public Date getDay() {
            return day;
        }

        public void setDay(Date day) {
            this.day = day;
        }

        public int getBillMins() {
            return billMins;
        }

        public void setBillMins(int billMins) {
            this.billMins = billMins;
        }

        public String getWorkStatusCode() {
            return workStatusCode;
        }

        public void setWorkStatusCode(String workStatusCode) {
            this.workStatusCode = workStatusCode;
        }
    }

    public Map<String, DayCapacity> ensureAndLoadDayCapacities(AppReq appReq, List<Date> dayList) {
        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();

        Transaction transaction = dataSession.beginTransaction();
        try {
            for (Date day : dayList) {
                BillExpected billExpected = findBillExpected(dataSession, webUser, day);
                if (billExpected == null) {
                    billExpected = new BillExpected(new BillExpectedId(webUser.getWebUserId(), day),
                            defaultMinutes(day, webUser), 0, defaultStatus(day, webUser));
                    dataSession.save(billExpected);
                } else if (billExpected.getWorkStatus() == null || billExpected.getWorkStatus().trim().length() == 0) {
                    billExpected.setWorkStatus(defaultStatus(day, webUser));
                    dataSession.update(billExpected);
                }
            }
            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }

        Map<String, DayCapacity> map = new LinkedHashMap<String, DayCapacity>();
        for (Date day : dayList) {
            BillExpected billExpected = findBillExpected(dataSession, webUser, day);
            DayCapacity dayCapacity = new DayCapacity();
            dayCapacity.setDay(day);
            if (billExpected == null) {
                dayCapacity.setBillMins(defaultMinutes(day, webUser));
                dayCapacity.setWorkStatusCode(defaultStatus(day, webUser));
            } else {
                dayCapacity.setBillMins(billExpected.getBillMins());
                dayCapacity.setWorkStatusCode(normalizeStatusCode(billExpected.getWorkStatus(), day, webUser));
            }
            map.put(toDayKey(day), dayCapacity);
        }
        return map;
    }

    public DayCapacity saveDayCapacity(AppReq appReq, Date day, int billMins, String workStatusCode) {
        String normalizedStatus = normalizeStatusCodeForInput(workStatusCode);
        if (normalizedStatus.length() == 0) {
            throw new IllegalArgumentException("Invalid work status code. Allowed: W,N,V,H,T,S");
        }

        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();
        Date normalizedDay = atDayStart(webUser, day);
        Transaction transaction = dataSession.beginTransaction();
        try {
            BillExpected billExpected = findBillExpected(dataSession, webUser, normalizedDay);
            if (billExpected == null) {
                billExpected = new BillExpected(new BillExpectedId(webUser.getWebUserId(), normalizedDay),
                        Math.max(billMins, 0), 0, normalizedStatus);
                dataSession.save(billExpected);
            } else {
                billExpected.setBillMins(Math.max(billMins, 0));
                billExpected.setWorkStatus(normalizedStatus);
                dataSession.update(billExpected);
            }
            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }

        DayCapacity dayCapacity = new DayCapacity();
        dayCapacity.setDay(normalizedDay);
        dayCapacity.setBillMins(Math.max(billMins, 0));
        dayCapacity.setWorkStatusCode(normalizedStatus);
        return dayCapacity;
    }

    public String getStatusLabel(String workStatusCode) {
        if (STATUS_WORKING.equals(workStatusCode)) {
            return "Working";
        }
        if (STATUS_NOT_WORKING.equals(workStatusCode)) {
            return "Not Working";
        }
        if (STATUS_VACATION.equals(workStatusCode)) {
            return "Vacation";
        }
        if (STATUS_HOLIDAY.equals(workStatusCode)) {
            return "Holiday";
        }
        if (STATUS_TRAVELING.equals(workStatusCode)) {
            return "Traveling";
        }
        if (STATUS_SICK.equals(workStatusCode)) {
            return "Sick";
        }
        return "Working";
    }

    public List<String> getAllowedStatuses() {
        List<String> values = new ArrayList<String>();
        values.add(STATUS_WORKING);
        values.add(STATUS_NOT_WORKING);
        values.add(STATUS_VACATION);
        values.add(STATUS_HOLIDAY);
        values.add(STATUS_TRAVELING);
        values.add(STATUS_SICK);
        return values;
    }

    public int loadTargetMinutesForDay(AppReq appReq, Date day) {
        if (appReq == null || day == null) {
            return DEFAULT_DAILY_TARGET_MINUTES;
        }
        BillExpected billExpected = findBillExpected(appReq.getDataSession(), appReq.getWebUser(),
                atDayStart(appReq.getWebUser(), day));
        if (billExpected == null) {
            return DEFAULT_DAILY_TARGET_MINUTES;
        }
        return Math.max(0, billExpected.getBillMins());
    }

    public int loadTargetMinutesForCurrentWeek(AppReq appReq) {
        if (appReq == null || appReq.getWebUser() == null) {
            return DEFAULT_WEEKLY_TARGET_MINUTES;
        }
        Date today = atDayStart(appReq.getWebUser(),
                appReq.getWebUser().toDate(appReq.getWebUser().getLocalDateToday()));
        return loadTargetMinutesForWeek(appReq, today);
    }

    public int loadTargetMinutesForWeek(AppReq appReq, Date dayInWeek) {
        if (appReq == null || appReq.getWebUser() == null || dayInWeek == null) {
            return DEFAULT_WEEKLY_TARGET_MINUTES;
        }

        WebUser webUser = appReq.getWebUser();
        Date weekStart = getWeekStartSunday(webUser, dayInWeek);
        Date weekEndExclusive = getWeekEndExclusive(webUser, weekStart);

        List<BillExpected> rows = findBillExpectedForWeek(appReq.getDataSession(), webUser, weekStart,
                weekEndExclusive);
        if (rows.size() < 7) {
            return DEFAULT_WEEKLY_TARGET_MINUTES;
        }

        Map<String, Integer> dayMinutesMap = new LinkedHashMap<String, Integer>();
        for (BillExpected row : rows) {
            if (row == null || row.getId() == null || row.getId().getBillDate() == null) {
                continue;
            }
            dayMinutesMap.put(toDayKey(row.getId().getBillDate()), Integer.valueOf(Math.max(0, row.getBillMins())));
        }
        if (dayMinutesMap.size() < 7) {
            return DEFAULT_WEEKLY_TARGET_MINUTES;
        }

        Calendar cursor = webUser.getCalendar();
        cursor.setTime(weekStart);
        int total = 0;
        for (int i = 0; i < 7; i++) {
            String dayKey = toDayKey(cursor.getTime());
            Integer dayMins = dayMinutesMap.get(dayKey);
            if (dayMins == null) {
                return DEFAULT_WEEKLY_TARGET_MINUTES;
            }
            total += dayMins.intValue();
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
        return total;
    }

    private BillExpected findBillExpected(Session dataSession, WebUser webUser, Date day) {
        Query query = dataSession.createQuery("from BillExpected where id.webUserId = ? and id.billDate = ?");
        query.setParameter(0, webUser.getWebUserId());
        query.setParameter(1, day);
        @SuppressWarnings("unchecked")
        List<BillExpected> results = query.list();
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }

    private List<BillExpected> findBillExpectedForWeek(Session dataSession, WebUser webUser, Date weekStart,
            Date weekEndExclusive) {
        Query query = dataSession.createQuery("from BillExpected where id.webUserId = :webUserId "
                + "and id.billDate >= :weekStart and id.billDate < :weekEndExclusive");
        query.setParameter("webUserId", webUser.getWebUserId());
        query.setParameter("weekStart", weekStart);
        query.setParameter("weekEndExclusive", weekEndExclusive);
        @SuppressWarnings("unchecked")
        List<BillExpected> results = query.list();
        return results;
    }

    private Date atDayStart(WebUser webUser, Date day) {
        Calendar calendar = webUser.getCalendar();
        calendar.setTime(day);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getWeekStartSunday(WebUser webUser, Date day) {
        Calendar calendar = webUser.getCalendar();
        calendar.setTime(atDayStart(webUser, day));
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        return calendar.getTime();
    }

    private Date getWeekEndExclusive(WebUser webUser, Date weekStart) {
        Calendar calendar = webUser.getCalendar();
        calendar.setTime(weekStart);
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        return calendar.getTime();
    }

    private int defaultMinutes(Date day, WebUser webUser) {
        return isWeekend(day, webUser) ? 0 : 8 * 60;
    }

    private String defaultStatus(Date day, WebUser webUser) {
        return isWeekend(day, webUser) ? STATUS_NOT_WORKING : STATUS_WORKING;
    }

    private boolean isWeekend(Date day, WebUser webUser) {
        Calendar calendar = webUser.getCalendar();
        calendar.setTime(day);
        int dow = calendar.get(Calendar.DAY_OF_WEEK);
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY;
    }

    private String normalizeStatusCodeForInput(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim().toUpperCase();
        for (String allowed : getAllowedStatuses()) {
            if (allowed.equals(v)) {
                return v;
            }
        }
        return "";
    }

    private String normalizeStatusCode(String value, Date day, WebUser webUser) {
        String normalized = normalizeStatusCodeForInput(value);
        if (normalized.length() == 0) {
            return defaultStatus(day, webUser);
        }
        return normalized;
    }

    private String toDayKey(Date day) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(day);
    }
}
