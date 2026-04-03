package org.dandeliondaily.timereview.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.dandeliondaily.timereview.model.TimeEntryModel;
import org.dandeliondaily.timereview.model.TimeReviewDayModel;
import org.dandeliondaily.timereview.model.TimeSessionModel;

public class TimeReviewService {

    private final TimeRegularizationService regularizationService = new TimeRegularizationService();

    public TimeRegularizationService getRegularizationService() {
        return regularizationService;
    }

    public Date parseIsoDay(WebUser webUser, String reviewDateText) {
        if (reviewDateText == null || reviewDateText.trim().length() == 0) {
            return null;
        }
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd");
        iso.setLenient(false);
        iso.setTimeZone(webUser.getTimeZone());
        try {
            return regularizationService.toDayStart(webUser, iso.parse(reviewDateText.trim()));
        } catch (ParseException e) {
            return null;
        }
    }

    public Date parseReviewDay(WebUser webUser, String reviewDateText) {
        if (reviewDateText == null || reviewDateText.trim().length() == 0) {
            return null;
        }
        Date parsed = webUser.parseDate(reviewDateText.trim());
        if (parsed != null) {
            return regularizationService.toDayStart(webUser, parsed);
        }
        return parseIsoDay(webUser, reviewDateText);
    }

    public String formatIsoDay(WebUser webUser, Date date) {
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd");
        iso.setTimeZone(webUser.getTimeZone());
        return iso.format(date);
    }

    public Date resolveSelectedDay(WebUser webUser, Date requestedDate, List<Date> trackedDays) {
        if (requestedDate != null) {
            return regularizationService.toDayStart(webUser, requestedDate);
        }

        Date today = regularizationService.toDayStart(webUser, webUser.now());
        for (Date trackedDay : trackedDays) {
            if (webUser.isSameDay(trackedDay, today)) {
                return today;
            }
        }

        if (!trackedDays.isEmpty()) {
            return trackedDays.get(0);
        }

        return today;
    }

    public Date resolveDefaultSelectedDay(WebUser webUser, Session dataSession) {
        Date today = regularizationService.toDayStart(webUser, webUser.now());
        if (hasTrackedTimeOnDay(webUser, dataSession, today)) {
            return today;
        }
        Date mostRecentTrackedDay = findMostRecentTrackedDay(webUser, dataSession);
        if (mostRecentTrackedDay != null) {
            return mostRecentTrackedDay;
        }
        return today;
    }

    public boolean hasTrackedTimeOnDay(WebUser webUser, Session dataSession, Date day) {
        Date dayStart = regularizationService.toDayStart(webUser, day);
        Date dayEnd = regularizationService.toNextDay(webUser, dayStart);
        Query query = dataSession.createQuery(
                "select 1 from BillEntry where webUser = :webUser and startTime >= :dayStart and startTime < :dayEnd and billMins > 0");
        query.setParameter("webUser", webUser);
        query.setParameter("dayStart", dayStart);
        query.setParameter("dayEnd", dayEnd);
        query.setMaxResults(1);
        return !query.list().isEmpty();
    }

    public Date findMostRecentTrackedDay(WebUser webUser, Session dataSession) {
        Query query = dataSession.createQuery(
                "select startTime from BillEntry where webUser = :webUser and billMins > 0 order by startTime desc");
        query.setParameter("webUser", webUser);
        query.setMaxResults(1);
        Object value = query.uniqueResult();
        if (value instanceof Date) {
            return regularizationService.toDayStart(webUser, (Date) value);
        }
        return null;
    }

    public Date findMostRecentTrackedDayInRange(WebUser webUser, Session dataSession, Date rangeStart, Date rangeEnd) {
        Date start = regularizationService.toDayStart(webUser, rangeStart);
        Date endExclusive = regularizationService.toNextDay(webUser, rangeEnd);
        Query query = dataSession.createQuery(
                "select startTime from BillEntry where webUser = :webUser and billMins > 0 and startTime >= :rangeStart and startTime < :rangeEnd order by startTime desc");
        query.setParameter("webUser", webUser);
        query.setParameter("rangeStart", start);
        query.setParameter("rangeEnd", endExclusive);
        query.setMaxResults(1);
        Object value = query.uniqueResult();
        if (value instanceof Date) {
            return regularizationService.toDayStart(webUser, (Date) value);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<Date> listTrackedDays(WebUser webUser, Session dataSession) {
        Query query = dataSession.createQuery(
                "select startTime from BillEntry where webUser = :webUser and billMins > 0 order by startTime desc");
        query.setParameter("webUser", webUser);
        List<Date> dateTimes = query.list();

        List<Date> days = new ArrayList<Date>();
        Set<Long> dayMillisSet = new HashSet<Long>();
        for (Date dateTime : dateTimes) {
            Date day = regularizationService.toDayStart(webUser, dateTime);
            long millis = day.getTime();
            if (!dayMillisSet.contains(millis)) {
                dayMillisSet.add(millis);
                days.add(day);
            }
        }
        return days;
    }

    public Map<String, List<Date>> groupTrackedDaysByMonth(WebUser webUser, List<Date> trackedDaysDesc) {
        LinkedHashMap<String, List<Date>> grouped = new LinkedHashMap<String, List<Date>>();
        SimpleDateFormat monthFormat = webUser.getDateFormatService().createLegacyFormatter("MMMM yyyy",
                webUser.getTimeZone());

        for (Date day : trackedDaysDesc) {
            String key = monthFormat.format(day);
            List<Date> bucket = grouped.get(key);
            if (bucket == null) {
                bucket = new ArrayList<Date>();
                grouped.put(key, bucket);
            }
            bucket.add(day);
        }

        for (List<Date> bucket : grouped.values()) {
            bucket.sort(null);
        }

        return grouped;
    }

    public TimeReviewDayModel buildDayModel(WebUser webUser, Session dataSession, Date selectedDay,
            Integer lockedBillEntryId) {
        Date dayStart = regularizationService.toDayStart(webUser, selectedDay);
        Date dayEnd = regularizationService.toNextDay(webUser, selectedDay);

        List<BillEntry> dayEntries = regularizationService.loadEntriesForDay(webUser, dataSession, dayStart);
        if (!dayEntries.isEmpty()) {
            regularizationService.normalizeDayEntries(webUser, dataSession, dayEntries, dayStart, dayEnd,
                    lockedBillEntryId);
            dayEntries = regularizationService.loadEntriesForDay(webUser, dataSession, dayStart);
        }

        TimeReviewDayModel dayModel = new TimeReviewDayModel();
        dayModel.setSelectedDate(dayStart);
        dayModel.setSelectedDateIso(formatIsoDay(webUser, dayStart));
        dayModel.setSelectedDateLabel(webUser.getDateFormatService().formatPattern(dayStart, "EEEE MM-dd-yyyy",
                webUser.getTimeZone()));
        dayModel.setLockedBillEntryId(lockedBillEntryId);
        dayModel.setHasEntries(!dayEntries.isEmpty());

        Map<Integer, Project> projectMap = loadProjectMap(webUser, dataSession);
        Map<String, ProjectCategory> categoryMap = loadCategoryMap(webUser, dataSession);
        Map<String, BillCode> billCodeMap = loadBillCodeMap(webUser, dataSession);

        TimeSessionModel currentSession = null;
        BillEntry previous = null;
        int sessionIndex = 0;

        for (BillEntry entry : dayEntries) {
            if (currentSession == null
                    || previous == null
                    || !TimeRegularizationService.isSameMinute(previous.getEndTime(), entry.getStartTime())) {
                TimeSessionModel nextSession = new TimeSessionModel();
                nextSession.setSequence(++sessionIndex);
                nextSession.setStartTime(entry.getStartTime());
                if (previous != null) {
                    int breakMinutes = calculateMins(previous.getEndTime(), entry.getStartTime());
                    if (breakMinutes > 0) {
                        nextSession.setBreakMinutesBefore(Integer.valueOf(breakMinutes));
                        nextSession.setBreakDisplay(TimeTracker.formatTime(breakMinutes));
                    }
                }
                dayModel.getSessions().add(nextSession);
                currentSession = nextSession;
            }

            TimeEntryModel line = toEntryModel(entry, projectMap, categoryMap, billCodeMap);
            currentSession.getEntries().add(line);
            currentSession.setEndTime(entry.getEndTime());
            currentSession.setTotalMinutes(currentSession.getTotalMinutes() + line.getDurationMinutes());
            currentSession.setTotalDisplay(TimeTracker.formatTime(currentSession.getTotalMinutes()));

            dayModel.setTotalMinutes(dayModel.getTotalMinutes() + line.getDurationMinutes());
            previous = entry;
        }

        dayModel.setTotalDisplay(TimeTracker.formatTime(dayModel.getTotalMinutes()));
        return dayModel;
    }

    public String updateEntryTime(
            WebUser webUser,
            Session dataSession,
            int billId,
            String startText,
            String endText,
            Date selectedDay,
            Integer lockedBillEntryId) {

        BillEntry billEntry = (BillEntry) dataSession.get(BillEntry.class, billId);
        if (billEntry == null) {
            return "Bill entry was not found.";
        }
        if (billEntry.getWebUser() == null || billEntry.getWebUser().getWebUserId() != webUser.getWebUserId()) {
            return "Bill entry does not belong to the signed-in user.";
        }
        if (lockedBillEntryId != null && lockedBillEntryId.intValue() == billId) {
            return "The active timer entry cannot be edited while the timer is running.";
        }

        Date dayStart = regularizationService.toDayStart(webUser, selectedDay);
        Date dayEnd = regularizationService.toNextDay(webUser, selectedDay);

        Date newStart = parseTimeForDay(webUser, startText, dayStart);
        if (newStart == null) {
            return "Unable to parse start time.";
        }
        Date newEnd = parseTimeForDay(webUser, endText, dayStart);
        if (newEnd == null) {
            return "Unable to parse end time.";
        }

        if (newStart.after(newEnd)) {
            return "Start time must be before end time.";
        }
        if (newStart.before(dayStart) || !newStart.before(dayEnd) || newEnd.before(dayStart)
                || !newEnd.before(dayEnd)) {
            return "Times must stay within the selected day.";
        }
        if (newEnd.getTime() - newStart.getTime() > (12L * 60L * 60L * 1000L)) {
            return "Duration cannot exceed 12 hours.";
        }

        billEntry.setStartTime(newStart);
        billEntry.setEndTime(newEnd);
        billEntry.setBillMins(TimeTracker.calculateMins(billEntry));

        Transaction trans = dataSession.beginTransaction();
        try {
            dataSession.update(billEntry);
            trans.commit();
        } catch (RuntimeException e) {
            if (trans != null && trans.isActive()) {
                trans.rollback();
            }
            throw e;
        }

        List<BillEntry> dayEntries = regularizationService.loadEntriesForDay(webUser, dataSession, dayStart);
        regularizationService.normalizeDayEntries(webUser, dataSession, dayEntries, dayStart, dayEnd,
                lockedBillEntryId);
        return null;
    }

    private Date parseTimeForDay(WebUser webUser, String text, Date baseDay) {
        if (text == null || text.trim().length() == 0) {
            return null;
        }
        String[] patterns = new String[] {
                webUser.getTimeDisplayPattern(),
                webUser.getTimeEntryPattern(),
                "HH:mm",
                "hh:mm a",
                "h:mm a"
        };

        for (String pattern : patterns) {
            SimpleDateFormat sdf = webUser.getDateFormatService().createLegacyFormatter(pattern, webUser.getTimeZone());
            sdf.setLenient(false);
            try {
                Date parsedTime = sdf.parse(text.trim());
                Calendar base = webUser.getCalendar(baseDay);
                Calendar parsed = webUser.getCalendar(parsedTime);
                base.set(Calendar.HOUR_OF_DAY, parsed.get(Calendar.HOUR_OF_DAY));
                base.set(Calendar.MINUTE, parsed.get(Calendar.MINUTE));
                base.set(Calendar.SECOND, 0);
                base.set(Calendar.MILLISECOND, 0);
                return base.getTime();
            } catch (ParseException e) {
                // Try next pattern.
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Project> loadProjectMap(WebUser webUser, Session dataSession) {
        Query query = dataSession.createQuery("from Project where provider = :provider");
        query.setParameter("provider", webUser.getProvider());
        List<Project> projects = query.list();
        Map<Integer, Project> map = new HashMap<Integer, Project>();
        for (Project project : projects) {
            map.put(project.getProjectId(), project);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, ProjectCategory> loadCategoryMap(WebUser webUser, Session dataSession) {
        Query query = dataSession.createQuery("from ProjectCategory where provider = :provider");
        query.setParameter("provider", webUser.getProvider());
        List<ProjectCategory> categories = query.list();
        Map<String, ProjectCategory> map = new HashMap<String, ProjectCategory>();
        for (ProjectCategory category : categories) {
            map.put(category.getCategoryCode(), category);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, BillCode> loadBillCodeMap(WebUser webUser, Session dataSession) {
        Query query = dataSession.createQuery("from BillCode where provider = :provider");
        query.setParameter("provider", webUser.getProvider());
        List<BillCode> billCodes = query.list();
        Map<String, BillCode> map = new HashMap<String, BillCode>();
        for (BillCode billCode : billCodes) {
            map.put(billCode.getBillCode(), billCode);
        }
        return map;
    }

    private TimeEntryModel toEntryModel(
            BillEntry billEntry,
            Map<Integer, Project> projectMap,
            Map<String, ProjectCategory> categoryMap,
            Map<String, BillCode> billCodeMap) {
        TimeEntryModel model = new TimeEntryModel();
        model.setBillId(billEntry.getBillId());
        model.setStartTime(billEntry.getStartTime());
        model.setEndTime(billEntry.getEndTime());

        int mins = billEntry.getBillMins() == null ? TimeTracker.calculateMins(billEntry) : billEntry.getBillMins();
        model.setDurationMinutes(mins);
        model.setDurationDisplay(TimeTracker.formatTime(mins));
        model.setBillable(billEntry.getBillable());

        Project project = projectMap.get(billEntry.getProjectId());
        if (project != null) {
            model.setProjectId(project.getProjectId());
            model.setProjectName(project.getProjectName());
        }

        ProjectCategory category = categoryMap.get(billEntry.getCategoryCode());
        if (category != null) {
            model.setCategoryName(category.getClientName());
        }

        if (billEntry.getBillCode() != null) {
            BillCode billCode = billCodeMap.get(billEntry.getBillCode());
            if (billCode != null) {
                model.setBillCodeLabel(billCode.getBillLabel());
            }
        }

        ProjectActionNext action = billEntry.getAction();
        if (action != null) {
            String desc = action.getNextDescriptionForDisplay(null);
            model.setActionDescription(desc == null ? "" : desc.replaceAll("<[^>]+>", ""));
        }

        return model;
    }

    private int calculateMins(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        long elapsedTime = endTime.getTime() - startTime.getTime();
        if (elapsedTime <= 0) {
            return 0;
        }
        return (int) (elapsedTime / 60000.0 + 0.5);
    }
}
