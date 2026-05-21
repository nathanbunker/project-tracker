package org.dandeliondaily.planahead.service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.dandeliondaily.dashboard.service.DashboardTodayColumnService;
import org.dandeliondaily.planahead.model.PlanAheadBoardModel;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeAdder;
import org.openimmunizationsoftware.pt.model.PageMessage;
import org.openimmunizationsoftware.pt.model.PageMessageSeverity;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.WebUser;

public class PlanAheadBoardService {

    public static final int WORK_WINDOW_DAYS = 5;
    public static final int PERSONAL_WINDOW_DAYS = 8;
    public static final String MODE_WORK = "WORK";
    public static final String MODE_PERSONAL = "PERSONAL";

    public static final String ROW_MEETINGS = "meetings";
    public static final String ROW_COMMITTED = "committed";
    public static final String ROW_WILL = "will";
    public static final String ROW_MIGHT = "might";
    public static final String ROW_WAKE = "wake";
    public static final String ROW_MORNING = "morning";
    public static final String ROW_AFTERNOON = "afternoon";
    public static final String ROW_EVENING = "evening";
    public static final String ROW_OVERDUE = "overdue";
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    private final PlanAheadDayCapacityService dayCapacityService = new PlanAheadDayCapacityService();
    private final PlanAheadGaugeService gaugeService = new PlanAheadGaugeService();
    private final DashboardTodayColumnService dashboardTodayColumnService = new DashboardTodayColumnService();

    public String resolveMode(AppReq appReq) {
        String requestedMode = n(appReq.getRequest().getParameter("mode")).trim();
        if (MODE_PERSONAL.equalsIgnoreCase(requestedMode)) {
            appReq.getWebSession().setAttribute("PLAN_AHEAD_MODE", MODE_PERSONAL);
            return MODE_PERSONAL;
        }
        if (MODE_WORK.equalsIgnoreCase(requestedMode)) {
            appReq.getWebSession().setAttribute("PLAN_AHEAD_MODE", MODE_WORK);
            return MODE_WORK;
        }
        Object modeFromSession = appReq.getWebSession().getAttribute("PLAN_AHEAD_MODE");
        if (modeFromSession instanceof String
                && MODE_PERSONAL.equalsIgnoreCase(((String) modeFromSession).trim())) {
            return MODE_PERSONAL;
        }
        return MODE_WORK;
    }

    public void setMode(AppReq appReq, String mode) {
        if (MODE_PERSONAL.equalsIgnoreCase(mode)) {
            appReq.getWebSession().setAttribute("PLAN_AHEAD_MODE", MODE_PERSONAL);
            return;
        }
        appReq.getWebSession().setAttribute("PLAN_AHEAD_MODE", MODE_WORK);
    }

    public Date resolveWindowStart(AppReq appReq) {
        WebUser webUser = appReq.getWebUser();
        Date today = stripToDate(webUser, webUser.getToday());
        String windowStart = appReq.getRequest().getParameter("windowStart");
        if (windowStart == null || windowStart.trim().length() == 0) {
            Object sessionValue = appReq.getWebSession().getAttribute("PLAN_AHEAD_WINDOW_START");
            if (sessionValue instanceof String) {
                windowStart = (String) sessionValue;
            }
        }
        if (windowStart == null || windowStart.trim().length() == 0) {
            appReq.getWebSession().setAttribute("PLAN_AHEAD_WINDOW_START", toDayKey(today));
            return today;
        }
        Date parsed = parseDay(windowStart.trim(), webUser);
        if (parsed == null || parsed.before(today)) {
            appReq.getWebSession().setAttribute("PLAN_AHEAD_WINDOW_START", toDayKey(today));
            return today;
        }
        Date safe = stripToDate(webUser, parsed);
        appReq.getWebSession().setAttribute("PLAN_AHEAD_WINDOW_START", toDayKey(safe));
        return safe;
    }

    public PlanAheadBoardModel buildBoard(AppReq appReq, Date windowStart) {
        PlanAheadBoardModel model = new PlanAheadBoardModel();
        WebUser webUser = appReq.getWebUser();
        String mode = resolveMode(appReq);
        boolean personalMode = MODE_PERSONAL.equalsIgnoreCase(mode);
        model.setMode(mode);

        Date safeStart = stripToDate(webUser, windowStart);
        List<Date> dayList = createDayList(webUser, safeStart, resolveWindowDays(mode));
        model.setWindowStartKey(toDayKey(dayList.get(0)));
        model.setWindowEndKey(toDayKey(dayList.get(dayList.size() - 1)));
        String action = n(appReq.getRequest().getParameter("action"));
        boolean quickCaptureSubmit = "Schedule".equals(action);
        model.setQuickCaptureFocusRequested(quickCaptureSubmit);
        if (quickCaptureSubmit && hasSuccessWithoutBlockingMessages(appReq)) {
            model.setQuickCaptureSentenceValue("");
        } else {
            model.setQuickCaptureSentenceValue(n(appReq.getRequest().getParameter("sentenceInput")));
        }
        model.setQuickCaptureProjectNames(dashboardTodayColumnService.listQuickCaptureProjectNames(appReq));

        Map<String, PlanAheadDayCapacityService.DayCapacity> dayCapacityMap = dayCapacityService
                .ensureAndLoadDayCapacities(appReq, dayList);

        Date startDate = dayList.get(0);
        Date endDateExclusive = nextDay(dayList.get(dayList.size() - 1), webUser);

        List<ActionNext> movableActions = loadMovableActions(appReq, mode, startDate, endDateExclusive);
        List<ActionNext> overdueActions = loadOverdueActions(appReq, mode);
        List<ActionNext> plannedActions = loadPlannedActions(appReq, mode, startDate, endDateExclusive);

        Map<String, List<ActionNext>> plannedByDay = bucketByDay(plannedActions);
        Map<String, List<ActionNext>> movableByDayRow = bucketMovableByDayRow(movableActions, mode);

        List<PlanAheadBoardModel.DayHeaderModel> dayHeaders = new ArrayList<PlanAheadBoardModel.DayHeaderModel>();
        String todayKey = toDayKey(stripToDate(webUser, webUser.getToday()));
        for (Date day : dayList) {
            String dayKey = toDayKey(day);
            PlanAheadDayCapacityService.DayCapacity dayCapacity = dayCapacityMap.get(dayKey);
            List<ActionNext> dayPlannedActions = plannedByDay.get(dayKey);
            if (dayPlannedActions == null) {
                dayPlannedActions = new ArrayList<ActionNext>();
            }

            TimeAdder timeAdder;
            if (todayKey.equals(dayKey)) {
                timeAdder = new TimeAdder(dayPlannedActions, appReq);
            } else {
                timeAdder = new TimeAdder(dayPlannedActions, appReq, day);
            }
            int plannedMinutes = personalMode ? 0 : timeAdder.getWillAct();
            int availableMinutes = personalMode ? 0 : dayCapacity.getBillMins() - plannedMinutes;

            PlanAheadBoardModel.DayHeaderModel dayHeader = new PlanAheadBoardModel.DayHeaderModel();
            dayHeader.setDayKey(dayKey);
            dayHeader.setDayLabel(webUser.getDateFormatService().formatPattern(day, "EEEE", webUser.getTimeZone()));
            dayHeader.setDateLabel(
                    webUser.getDateFormatService().formatPattern(day, "MMM dd, yyyy", webUser.getTimeZone()));
            Calendar cal = Calendar.getInstance(webUser.getTimeZone());
            cal.setTime(day);
            int dow = cal.get(Calendar.DAY_OF_WEEK);
            dayHeader.setWeekend(dow == Calendar.SATURDAY || dow == Calendar.SUNDAY);
            dayHeader.setBillMins(personalMode ? 0 : dayCapacity.getBillMins());
            dayHeader.setPlannedMins(plannedMinutes);
            dayHeader.setAvailableMins(availableMinutes);
            dayHeader.setWorkStatusCode(personalMode ? "" : dayCapacity.getWorkStatusCode());
            dayHeader.setWorkStatusLabel(
                    personalMode ? "" : dayCapacityService.getStatusLabel(dayCapacity.getWorkStatusCode()));
            dayHeader.setGauge(gaugeService.buildDayGauge(plannedMinutes, availableMinutes));
            dayHeaders.add(dayHeader);
        }
        model.setDayHeaders(dayHeaders);

        model.setRows(createRows(appReq, mode, dayHeaders, movableByDayRow));
        model.setOverdueRow(createOverdueRow(appReq, mode, todayKey, overdueActions));
        return model;
    }

    private List<PlanAheadBoardModel.RowModel> createRows(AppReq appReq,
            String mode,
            List<PlanAheadBoardModel.DayHeaderModel> dayHeaders,
            Map<String, List<ActionNext>> movableByDayRow) {
        List<PlanAheadBoardModel.RowModel> rows = new ArrayList<PlanAheadBoardModel.RowModel>();
        if (MODE_PERSONAL.equalsIgnoreCase(mode)) {
            rows.add(createRow(appReq, ROW_WAKE, "Wake", dayHeaders, movableByDayRow));
            rows.add(createRow(appReq, ROW_MORNING, "Morning", dayHeaders, movableByDayRow));
            rows.add(createRow(appReq, ROW_AFTERNOON, "Afternoon", dayHeaders, movableByDayRow));
            rows.add(createRow(appReq, ROW_EVENING, "Evening", dayHeaders, movableByDayRow));
        } else {
            rows.add(createRow(appReq, ROW_MEETINGS, "Meetings", dayHeaders, movableByDayRow));
            rows.add(createRow(appReq, ROW_COMMITTED, "Committed", dayHeaders, movableByDayRow));
            rows.add(createRow(appReq, ROW_WILL, "Will", dayHeaders, movableByDayRow));
            rows.add(createRow(appReq, ROW_MIGHT, "Might", dayHeaders, movableByDayRow));
        }
        return rows;
    }

    private PlanAheadBoardModel.RowModel createRow(AppReq appReq, String rowKey, String rowLabel,
            List<PlanAheadBoardModel.DayHeaderModel> dayHeaders,
            Map<String, List<ActionNext>> movableByDayRow) {
        PlanAheadBoardModel.RowModel row = new PlanAheadBoardModel.RowModel();
        row.setRowKey(rowKey);
        row.setRowLabel(rowLabel);

        List<PlanAheadBoardModel.CellModel> cells = new ArrayList<PlanAheadBoardModel.CellModel>();
        for (PlanAheadBoardModel.DayHeaderModel dayHeader : dayHeaders) {
            PlanAheadBoardModel.CellModel cell = new PlanAheadBoardModel.CellModel();
            cell.setDayKey(dayHeader.getDayKey());
            cell.setRowKey(rowKey);

            String bucketKey = dayHeader.getDayKey() + "|" + rowKey;
            List<ActionNext> actions = movableByDayRow.get(bucketKey);
            if (actions == null) {
                actions = new ArrayList<ActionNext>();
            }
            actions.sort(Comparator
                    .comparing((ActionNext pa) -> n(pa.getNextTimeEstimate())).reversed()
                    .thenComparing(ActionNext::getPriorityLevel, Comparator.reverseOrder())
                    .thenComparing(ActionNext::getActionNextId));

            List<PlanAheadBoardModel.CardModel> cards = new ArrayList<PlanAheadBoardModel.CardModel>();
            for (ActionNext action : actions) {
                PlanAheadBoardModel.CardModel card = new PlanAheadBoardModel.CardModel();
                card.setActionNextId(action.getActionNextId());
                card.setProjectName(action.getProject() == null ? "" : n(action.getProject().getProjectName()));
                card.setDescription(n(action.getNextDescriptionForDisplay(appReq.getWebUser().getProjectContact())));
                card.setRawDescription(n(action.getNextDescription()));
                card.setEstimateMins(n(action.getNextTimeEstimate()));
                card.setEstimateDisplay(action.getNextTimeEstimateForDisplay());
                card.setNextActionType(n(action.getNextActionType()));
                card.setRescheduleLocked(action.isRescheduleLocked());
                cards.add(card);
            }
            cell.setCards(cards);
            cells.add(cell);
        }
        row.setCells(cells);
        return row;
    }

    private List<ActionNext> loadMovableActions(AppReq appReq, String mode, Date startDate,
            Date endDateExclusive) {
        boolean billable = MODE_WORK.equalsIgnoreCase(mode);
        Session dataSession = appReq.getDataSession();
        Query query = dataSession.createQuery(
                "select distinct an from ActionNext an "
                        + "left join fetch an.project "
                        + "where an.workspaceId = :workspaceId and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                        + "and an.billable = :billable "
                        + "and an.nextDescription <> '' "
                        + "and an.nextActionDate >= :startDate and an.nextActionDate < :endDate "
                        + "and an.nextActionStatusString <> :completed and an.nextActionStatusString <> :cancelled "
                        + "and (an.templateTypeString is null or an.templateTypeString = '') "
                        + "order by an.nextActionDate, an.priorityLevel desc, an.nextTimeEstimate desc");
        query.setParameter("workspaceId", appReq.getActiveWorkspaceId());
        query.setParameter("contactId", appReq.getWebUser().getContactId());
        query.setParameter("nextContactId", appReq.getWebUser().getContactId());
        query.setParameter("billable", billable);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDateExclusive);
        query.setParameter("completed", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("cancelled", ProjectNextActionStatus.CANCELLED.getId());
        @SuppressWarnings("unchecked")
        List<ActionNext> list = query.list();
        List<ActionNext> filtered = new ArrayList<ActionNext>();
        for (ActionNext action : list) {
            if (toRowKey(action, mode).length() > 0) {
                filtered.add(action);
            }
        }
        return filtered;
    }

    private List<ActionNext> loadPlannedActions(AppReq appReq, String mode, Date startDate,
            Date endDateExclusive) {
        boolean billable = MODE_WORK.equalsIgnoreCase(mode);
        Session dataSession = appReq.getDataSession();
        Query query = dataSession.createQuery(
                "select distinct an from ActionNext an "
                        + "left join fetch an.project "
                        + "where an.workspaceId = :workspaceId and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                        + "and an.billable = :billable "
                        + "and an.nextDescription <> '' "
                        + "and an.nextActionDate >= :startDate and an.nextActionDate < :endDate "
                        + "and an.nextActionStatusString <> :completed and an.nextActionStatusString <> :cancelled "
                        + "order by an.nextActionDate");
        query.setParameter("workspaceId", appReq.getActiveWorkspaceId());
        query.setParameter("contactId", appReq.getWebUser().getContactId());
        query.setParameter("nextContactId", appReq.getWebUser().getContactId());
        query.setParameter("billable", billable);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDateExclusive);
        query.setParameter("completed", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("cancelled", ProjectNextActionStatus.CANCELLED.getId());
        @SuppressWarnings("unchecked")
        List<ActionNext> list = query.list();
        return list;
    }

    List<ActionNext> loadOverdueActions(AppReq appReq, String mode) {
        boolean billable = MODE_WORK.equalsIgnoreCase(mode);
        Session dataSession = appReq.getDataSession();
        Date today = stripToDate(appReq.getWebUser(), appReq.getWebUser().getToday());
        Query query = dataSession.createQuery(
                "select distinct an from ActionNext an "
                        + "left join fetch an.project "
                        + "where an.workspaceId = :workspaceId and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                        + "and an.billable = :billable "
                        + "and an.nextDescription <> '' "
                        + "and an.nextActionDate < :today "
                        + "and an.nextActionStatusString = :ready "
                        + "and an.templateActionNextId is null "
                        + "and (an.templateTypeString is null or an.templateTypeString = '') "
                        + "order by an.nextActionDate, an.priorityLevel desc, an.nextTimeEstimate desc");
        query.setParameter("workspaceId", appReq.getActiveWorkspaceId());
        query.setParameter("contactId", appReq.getWebUser().getContactId());
        query.setParameter("nextContactId", appReq.getWebUser().getContactId());
        query.setParameter("billable", billable);
        query.setParameter("today", today);
        query.setParameter("ready", ProjectNextActionStatus.READY.getId());
        @SuppressWarnings("unchecked")
        List<ActionNext> list = query.list();
        List<ActionNext> filtered = new ArrayList<ActionNext>();
        for (ActionNext action : list) {
            if (toRowKey(action, mode).length() > 0) {
                filtered.add(action);
            }
        }
        return filtered;
    }

    private PlanAheadBoardModel.OverdueRowModel createOverdueRow(AppReq appReq, String mode,
            String todayKey, List<ActionNext> overdueActions) {
        PlanAheadBoardModel.OverdueRowModel row = new PlanAheadBoardModel.OverdueRowModel();
        row.setTodayKey(todayKey);
        List<PlanAheadBoardModel.CardModel> cards = new ArrayList<PlanAheadBoardModel.CardModel>();
        for (ActionNext action : overdueActions) {
            PlanAheadBoardModel.CardModel card = new PlanAheadBoardModel.CardModel();
            card.setActionNextId(action.getActionNextId());
            card.setProjectName(action.getProject() == null ? "" : n(action.getProject().getProjectName()));
            card.setDescription(n(action.getNextDescriptionForDisplay(appReq.getWebUser().getProjectContact())));
            card.setRawDescription(n(action.getNextDescription()));
            card.setEstimateMins(n(action.getNextTimeEstimate()));
            card.setEstimateDisplay(action.getNextTimeEstimateForDisplay());
            card.setNextActionType(n(action.getNextActionType()));
            card.setRescheduleLocked(action.isRescheduleLocked());
            cards.add(card);
        }
        cards.sort(Comparator
                .comparing((PlanAheadBoardModel.CardModel c) -> c.getEstimateMins()).reversed()
                .thenComparing(PlanAheadBoardModel.CardModel::getActionNextId));
        row.setCards(cards);
        return row;
    }

    private Map<String, List<ActionNext>> bucketMovableByDayRow(List<ActionNext> actions, String mode) {
        Map<String, List<ActionNext>> map = new LinkedHashMap<String, List<ActionNext>>();
        for (ActionNext action : actions) {
            String rowKey = toRowKey(action, mode);
            if (rowKey.length() == 0 || action.getNextActionDate() == null) {
                continue;
            }
            String key = toDayKey(action.getNextActionDate()) + "|" + rowKey;
            List<ActionNext> bucket = map.get(key);
            if (bucket == null) {
                bucket = new ArrayList<ActionNext>();
                map.put(key, bucket);
            }
            bucket.add(action);
        }
        return map;
    }

    private Map<String, List<ActionNext>> bucketByDay(List<ActionNext> actions) {
        Map<String, List<ActionNext>> map = new HashMap<String, List<ActionNext>>();
        for (ActionNext action : actions) {
            if (action.getNextActionDate() == null) {
                continue;
            }
            String key = toDayKey(action.getNextActionDate());
            List<ActionNext> bucket = map.get(key);
            if (bucket == null) {
                bucket = new ArrayList<ActionNext>();
                map.put(key, bucket);
            }
            bucket.add(action);
        }
        return map;
    }

    private String toRowKey(ActionNext action, String mode) {
        if (MODE_PERSONAL.equalsIgnoreCase(mode)) {
            TimeSlot timeSlot = action.getTimeSlot();
            if (timeSlot == null) {
                timeSlot = TimeSlot.AFTERNOON;
            }
            if (timeSlot == TimeSlot.WAKE) {
                return ROW_WAKE;
            }
            if (timeSlot == TimeSlot.MORNING) {
                return ROW_MORNING;
            }
            if (timeSlot == TimeSlot.EVENING) {
                return ROW_EVENING;
            }
            return ROW_AFTERNOON;
        }
        return toRowKey(action.getNextActionType());
    }

    private String toRowKey(String actionType) {
        if (ProjectNextActionType.WILL_MEET.equals(actionType)) {
            return ROW_MEETINGS;
        }
        if (ProjectNextActionType.COMMITTED_TO.equals(actionType)) {
            return ROW_COMMITTED;
        }
        if (ProjectNextActionType.WILL.equals(actionType)
                || ProjectNextActionType.WILL_CONTACT.equals(actionType)
                || ProjectNextActionType.WILL_DOCUMENT.equals(actionType)
                || ProjectNextActionType.WILL_FOLLOW_UP.equals(actionType)
                || ProjectNextActionType.WILL_REVIEW.equals(actionType)) {
            return ROW_WILL;
        }
        if (ProjectNextActionType.MIGHT.equals(actionType)
                || ProjectNextActionType.WOULD_LIKE_TO.equals(actionType)) {
            return ROW_MIGHT;
        }
        return "";
    }

    public String resolveRowKeyForActionType(String actionType) {
        return toRowKey(actionType);
    }

    public String resolveActionTypeForRowKey(String rowKey) {
        if (ROW_MEETINGS.equals(rowKey)) {
            return ProjectNextActionType.WILL_MEET;
        }
        if (ROW_COMMITTED.equals(rowKey)) {
            return ProjectNextActionType.COMMITTED_TO;
        }
        if (ROW_WILL.equals(rowKey)) {
            return ProjectNextActionType.WILL;
        }
        if (ROW_MIGHT.equals(rowKey)) {
            return ProjectNextActionType.MIGHT;
        }
        return "";
    }

    public TimeSlot resolveTimeSlotForRowKey(String rowKey) {
        if (ROW_WAKE.equals(rowKey)) {
            return TimeSlot.WAKE;
        }
        if (ROW_MORNING.equals(rowKey)) {
            return TimeSlot.MORNING;
        }
        if (ROW_EVENING.equals(rowKey)) {
            return TimeSlot.EVENING;
        }
        if (ROW_AFTERNOON.equals(rowKey)) {
            return TimeSlot.AFTERNOON;
        }
        return null;
    }

    public String resolveRowKeyForTimeSlot(TimeSlot timeSlot) {
        if (timeSlot == null || timeSlot == TimeSlot.AFTERNOON) {
            return ROW_AFTERNOON;
        }
        if (timeSlot == TimeSlot.WAKE) {
            return ROW_WAKE;
        }
        if (timeSlot == TimeSlot.MORNING) {
            return ROW_MORNING;
        }
        if (timeSlot == TimeSlot.EVENING) {
            return ROW_EVENING;
        }
        return ROW_AFTERNOON;
    }

    private List<Date> createDayList(WebUser webUser, Date startDate, int count) {
        List<Date> dates = new ArrayList<Date>();
        Calendar calendar = webUser.getCalendar();
        calendar.setTime(startDate);
        for (int i = 0; i < count; i++) {
            dates.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dates;
    }

    private int resolveWindowDays(String mode) {
        return MODE_PERSONAL.equalsIgnoreCase(mode) ? PERSONAL_WINDOW_DAYS : WORK_WINDOW_DAYS;
    }

    private Date nextDay(Date date, WebUser webUser) {
        Calendar calendar = webUser.getCalendar();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    private Date stripToDate(WebUser webUser, Date date) {
        Calendar calendar = webUser.getCalendar();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date parseDay(String dayKey, WebUser webUser) {
        try {
            LocalDate localDate = LocalDate.parse(dayKey);
            return webUser.toDate(localDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String toDayKey(Date day) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(UTC_TIME_ZONE);
        return sdf.format(day);
    }

    private String n(String value) {
        return value == null ? "" : value;
    }

    private Integer n(Integer value) {
        return value == null ? Integer.valueOf(0) : value;
    }

    private boolean hasSuccessWithoutBlockingMessages(AppReq appReq) {
        boolean hasSuccess = false;
        for (PageMessage message : appReq.getPageMessages()) {
            if (message == null || message.getSeverity() == null) {
                continue;
            }
            if (message.getSeverity() == PageMessageSeverity.ERROR
                    || message.getSeverity() == PageMessageSeverity.WARNING) {
                return false;
            }
            if (message.getSeverity() == PageMessageSeverity.SUCCESS) {
                hasSuccess = true;
            }
        }
        return hasSuccess;
    }

}
