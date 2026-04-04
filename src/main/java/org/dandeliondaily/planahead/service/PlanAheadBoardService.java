package org.dandeliondaily.planahead.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.dashboard.service.DashboardTodayColumnService;
import org.dandeliondaily.planahead.model.PlanAheadBoardModel;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeAdder;
import org.openimmunizationsoftware.pt.model.PageMessage;
import org.openimmunizationsoftware.pt.model.PageMessageSeverity;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TemplateType;
import org.openimmunizationsoftware.pt.model.WebUser;

public class PlanAheadBoardService {

    public static final int WINDOW_DAYS = 5;

    public static final String ROW_MEETINGS = "meetings";
    public static final String ROW_COMMITTED = "committed";
    public static final String ROW_WILL = "will";
    public static final String ROW_MIGHT = "might";

    private final PlanAheadDayCapacityService dayCapacityService = new PlanAheadDayCapacityService();
    private final PlanAheadGaugeService gaugeService = new PlanAheadGaugeService();
    private final DashboardTodayColumnService dashboardTodayColumnService = new DashboardTodayColumnService();

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
        Date parsed = parseDay(windowStart.trim());
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

        Date safeStart = stripToDate(webUser, windowStart);
        List<Date> dayList = createDayList(webUser, safeStart, WINDOW_DAYS);
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

        List<ProjectActionNext> movableActions = loadMovableActions(appReq, dayList.get(0),
                nextDay(dayList.get(dayList.size() - 1), webUser));
        List<ProjectActionNext> plannedActions = loadPlannedActions(appReq, dayList.get(0),
                nextDay(dayList.get(dayList.size() - 1), webUser));
        List<ProjectActionNext> templateActions = loadTemplateActions(appReq);
        List<ProjectActionNext> generatedTemplateActions = loadGeneratedTemplateActions(appReq, dayList.get(0),
                nextDay(dayList.get(dayList.size() - 1), webUser));

        Map<String, List<ProjectActionNext>> plannedByDay = bucketByDay(plannedActions);
        Map<String, List<ProjectActionNext>> movableByDayRow = bucketMovableByDayRow(movableActions);

        List<PlanAheadBoardModel.DayHeaderModel> dayHeaders = new ArrayList<PlanAheadBoardModel.DayHeaderModel>();
        String todayKey = toDayKey(stripToDate(webUser, webUser.getToday()));
        for (Date day : dayList) {
            String dayKey = toDayKey(day);
            PlanAheadDayCapacityService.DayCapacity dayCapacity = dayCapacityMap.get(dayKey);
            List<ProjectActionNext> dayPlannedActions = plannedByDay.get(dayKey);
            if (dayPlannedActions == null) {
                dayPlannedActions = new ArrayList<ProjectActionNext>();
            }

            TimeAdder timeAdder;
            if (todayKey.equals(dayKey)) {
                timeAdder = new TimeAdder(dayPlannedActions, appReq);
            } else {
                timeAdder = new TimeAdder(dayPlannedActions, appReq, day);
            }
            int plannedMinutes = timeAdder.getWillAct();
            int availableMinutes = dayCapacity.getBillMins() - plannedMinutes;

            PlanAheadBoardModel.DayHeaderModel dayHeader = new PlanAheadBoardModel.DayHeaderModel();
            dayHeader.setDayKey(dayKey);
            dayHeader.setDayLabel(webUser.getDateFormatService().formatPattern(day, "EEEE", webUser.getTimeZone()));
            dayHeader.setDateLabel(
                    webUser.getDateFormatService().formatPattern(day, "MMM dd, yyyy", webUser.getTimeZone()));
            dayHeader.setBillMins(dayCapacity.getBillMins());
            dayHeader.setPlannedMins(plannedMinutes);
            dayHeader.setAvailableMins(availableMinutes);
            dayHeader.setWorkStatusCode(dayCapacity.getWorkStatusCode());
            dayHeader.setWorkStatusLabel(dayCapacityService.getStatusLabel(dayCapacity.getWorkStatusCode()));
            dayHeader.setGauge(gaugeService.buildDayGauge(plannedMinutes, availableMinutes));
            dayHeaders.add(dayHeader);
        }
        model.setDayHeaders(dayHeaders);

        model.setRows(createRows(appReq, dayHeaders, movableByDayRow));
        model.setTemplateRow(
                createTemplateRow(appReq, templateActions, dayHeaders, dayCapacityMap, generatedTemplateActions));
        return model;
    }

    private List<PlanAheadBoardModel.RowModel> createRows(AppReq appReq,
            List<PlanAheadBoardModel.DayHeaderModel> dayHeaders,
            Map<String, List<ProjectActionNext>> movableByDayRow) {
        List<PlanAheadBoardModel.RowModel> rows = new ArrayList<PlanAheadBoardModel.RowModel>();
        rows.add(createRow(appReq, ROW_MEETINGS, "Meetings", dayHeaders, movableByDayRow));
        rows.add(createRow(appReq, ROW_COMMITTED, "Committed", dayHeaders, movableByDayRow));
        rows.add(createRow(appReq, ROW_WILL, "Will", dayHeaders, movableByDayRow));
        rows.add(createRow(appReq, ROW_MIGHT, "Might", dayHeaders, movableByDayRow));
        return rows;
    }

    private PlanAheadBoardModel.RowModel createRow(AppReq appReq, String rowKey, String rowLabel,
            List<PlanAheadBoardModel.DayHeaderModel> dayHeaders,
            Map<String, List<ProjectActionNext>> movableByDayRow) {
        PlanAheadBoardModel.RowModel row = new PlanAheadBoardModel.RowModel();
        row.setRowKey(rowKey);
        row.setRowLabel(rowLabel);

        List<PlanAheadBoardModel.CellModel> cells = new ArrayList<PlanAheadBoardModel.CellModel>();
        for (PlanAheadBoardModel.DayHeaderModel dayHeader : dayHeaders) {
            PlanAheadBoardModel.CellModel cell = new PlanAheadBoardModel.CellModel();
            cell.setDayKey(dayHeader.getDayKey());
            cell.setRowKey(rowKey);

            String bucketKey = dayHeader.getDayKey() + "|" + rowKey;
            List<ProjectActionNext> actions = movableByDayRow.get(bucketKey);
            if (actions == null) {
                actions = new ArrayList<ProjectActionNext>();
            }
            actions.sort(Comparator
                    .comparing((ProjectActionNext pa) -> n(pa.getNextTimeEstimate())).reversed()
                    .thenComparing(ProjectActionNext::getPriorityLevel, Comparator.reverseOrder())
                    .thenComparing(ProjectActionNext::getActionNextId));

            List<PlanAheadBoardModel.CardModel> cards = new ArrayList<PlanAheadBoardModel.CardModel>();
            for (ProjectActionNext action : actions) {
                PlanAheadBoardModel.CardModel card = new PlanAheadBoardModel.CardModel();
                card.setActionNextId(action.getActionNextId());
                card.setProjectName(action.getProject() == null ? "" : n(action.getProject().getProjectName()));
                card.setDescription(n(action.getNextDescriptionForDisplay(appReq.getWebUser().getProjectContact())));
                card.setEstimateMins(n(action.getNextTimeEstimate()));
                card.setEstimateDisplay(action.getNextTimeEstimateForDisplay());
                card.setNextActionType(n(action.getNextActionType()));
                cards.add(card);
            }
            cell.setCards(cards);
            cells.add(cell);
        }
        row.setCells(cells);
        return row;
    }

    private PlanAheadBoardModel.TemplateRowModel createTemplateRow(AppReq appReq,
            List<ProjectActionNext> templateActions,
            List<PlanAheadBoardModel.DayHeaderModel> dayHeaders,
            Map<String, PlanAheadDayCapacityService.DayCapacity> dayCapacityMap,
            List<ProjectActionNext> generatedTemplateActions) {
        PlanAheadBoardModel.TemplateRowModel row = new PlanAheadBoardModel.TemplateRowModel();
        Map<String, ProjectActionNext> generatedByTemplateDay = new HashMap<String, ProjectActionNext>();
        for (ProjectActionNext generatedAction : generatedTemplateActions) {
            if (generatedAction.getTemplateActionNextId() == null || generatedAction.getNextActionDate() == null) {
                continue;
            }
            generatedByTemplateDay.put(
                    generatedAction.getTemplateActionNextId() + "|" + toDayKey(generatedAction.getNextActionDate()),
                    generatedAction);
        }

        List<PlanAheadBoardModel.TemplateCardModel> cards = new ArrayList<PlanAheadBoardModel.TemplateCardModel>();
        for (ProjectActionNext action : templateActions) {
            PlanAheadBoardModel.TemplateCardModel card = new PlanAheadBoardModel.TemplateCardModel();
            card.setTemplateActionNextId(action.getActionNextId());
            card.setProjectName(action.getProject() == null ? "" : n(action.getProject().getProjectName()));
            card.setDescription(n(action.getNextDescriptionForDisplay(appReq.getWebUser().getProjectContact())));
            card.setEstimateDisplay(action.getNextTimeEstimateForDisplay());
            TemplateType templateType = action.getTemplateType();
            card.setTemplateTypeLabel(templateType == null ? "" : templateType.getLabel());
            card.setTemplateTypeId(templateType == null ? "" : templateType.getId());

            List<PlanAheadBoardModel.TemplateDaySelectionModel> selections = new ArrayList<PlanAheadBoardModel.TemplateDaySelectionModel>();
            for (PlanAheadBoardModel.DayHeaderModel dayHeader : dayHeaders) {
                PlanAheadBoardModel.TemplateDaySelectionModel selection = new PlanAheadBoardModel.TemplateDaySelectionModel();
                selection.setDayKey(dayHeader.getDayKey());
                selection.setDayLabel(dayHeader.getDayLabel().length() > 3 ? dayHeader.getDayLabel().substring(0, 3)
                        : dayHeader.getDayLabel());

                String key = action.getActionNextId() + "|" + dayHeader.getDayKey();
                ProjectActionNext generatedAction = generatedByTemplateDay.get(key);
                if (generatedAction != null) {
                    boolean active = generatedAction.getNextActionStatus() != null
                            && generatedAction.getNextActionStatus() != ProjectNextActionStatus.CANCELLED;
                    selection.setSelected(active);
                } else {
                    boolean defaultSelected = TemplateType.DAILY.equals(templateType);
                    PlanAheadDayCapacityService.DayCapacity dayCapacity = dayCapacityMap.get(dayHeader.getDayKey());
                    if (defaultSelected && dayCapacity != null) {
                        defaultSelected = PlanAheadDayCapacityService.STATUS_WORKING
                                .equals(dayCapacity.getWorkStatusCode());
                    }
                    selection.setSelected(defaultSelected);
                }
                selections.add(selection);
            }
            card.setDaySelections(selections);
            cards.add(card);
        }
        row.setTemplateCards(cards);
        return row;
    }

    private List<ProjectActionNext> loadGeneratedTemplateActions(AppReq appReq, Date startDate, Date endDateExclusive) {
        Session dataSession = appReq.getDataSession();
        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan "
                        + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                        + "and pan.billable = true "
                        + "and pan.templateActionNextId is not null "
                        + "and pan.nextActionDate >= :startDate and pan.nextActionDate < :endDate "
                        + "and pan.nextActionStatusString <> :completed "
                        + "order by pan.nextActionDate");
        query.setParameter("provider", appReq.getWebUser().getProvider());
        query.setParameter("contactId", appReq.getWebUser().getContactId());
        query.setParameter("nextContactId", appReq.getWebUser().getContactId());
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDateExclusive);
        query.setParameter("completed", ProjectNextActionStatus.COMPLETED.getId());
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> list = query.list();
        return list;
    }

    private List<ProjectActionNext> loadMovableActions(AppReq appReq, Date startDate, Date endDateExclusive) {
        Session dataSession = appReq.getDataSession();
        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan "
                        + "left join fetch pan.project "
                        + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                        + "and pan.billable = true "
                        + "and pan.nextDescription <> '' "
                        + "and pan.nextActionDate >= :startDate and pan.nextActionDate < :endDate "
                        + "and pan.nextActionStatusString <> :completed and pan.nextActionStatusString <> :cancelled "
                        + "and pan.templateActionNextId is null "
                        + "and (pan.templateTypeString is null or pan.templateTypeString = '') "
                        + "order by pan.nextActionDate, pan.priorityLevel desc, pan.nextTimeEstimate desc");
        query.setParameter("provider", appReq.getWebUser().getProvider());
        query.setParameter("contactId", appReq.getWebUser().getContactId());
        query.setParameter("nextContactId", appReq.getWebUser().getContactId());
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDateExclusive);
        query.setParameter("completed", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("cancelled", ProjectNextActionStatus.CANCELLED.getId());
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> list = query.list();
        List<ProjectActionNext> filtered = new ArrayList<ProjectActionNext>();
        for (ProjectActionNext action : list) {
            if (toRowKey(action.getNextActionType()).length() > 0) {
                filtered.add(action);
            }
        }
        return filtered;
    }

    private List<ProjectActionNext> loadPlannedActions(AppReq appReq, Date startDate, Date endDateExclusive) {
        Session dataSession = appReq.getDataSession();
        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan "
                        + "left join fetch pan.project "
                        + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                        + "and pan.billable = true "
                        + "and pan.nextDescription <> '' "
                        + "and pan.nextActionDate >= :startDate and pan.nextActionDate < :endDate "
                        + "and pan.nextActionStatusString <> :completed and pan.nextActionStatusString <> :cancelled "
                        + "order by pan.nextActionDate");
        query.setParameter("provider", appReq.getWebUser().getProvider());
        query.setParameter("contactId", appReq.getWebUser().getContactId());
        query.setParameter("nextContactId", appReq.getWebUser().getContactId());
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDateExclusive);
        query.setParameter("completed", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("cancelled", ProjectNextActionStatus.CANCELLED.getId());
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> list = query.list();
        return list;
    }

    private List<ProjectActionNext> loadTemplateActions(AppReq appReq) {
        Session dataSession = appReq.getDataSession();
        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan "
                        + "left join fetch pan.project "
                        + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                        + "and pan.billable = true "
                        + "and (pan.templateTypeString is not null and pan.templateTypeString <> '') "
                        + "and pan.nextActionStatusString <> :completed and pan.nextActionStatusString <> :cancelled "
                        + "order by pan.priorityLevel desc, pan.nextTimeEstimate desc");
        query.setParameter("provider", appReq.getWebUser().getProvider());
        query.setParameter("contactId", appReq.getWebUser().getContactId());
        query.setParameter("nextContactId", appReq.getWebUser().getContactId());
        query.setParameter("completed", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("cancelled", ProjectNextActionStatus.CANCELLED.getId());
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> list = query.list();
        return list;
    }

    private Map<String, List<ProjectActionNext>> bucketMovableByDayRow(List<ProjectActionNext> actions) {
        Map<String, List<ProjectActionNext>> map = new LinkedHashMap<String, List<ProjectActionNext>>();
        for (ProjectActionNext action : actions) {
            String rowKey = toRowKey(action.getNextActionType());
            if (rowKey.length() == 0 || action.getNextActionDate() == null) {
                continue;
            }
            String key = toDayKey(action.getNextActionDate()) + "|" + rowKey;
            List<ProjectActionNext> bucket = map.get(key);
            if (bucket == null) {
                bucket = new ArrayList<ProjectActionNext>();
                map.put(key, bucket);
            }
            bucket.add(action);
        }
        return map;
    }

    private Map<String, List<ProjectActionNext>> bucketByDay(List<ProjectActionNext> actions) {
        Map<String, List<ProjectActionNext>> map = new HashMap<String, List<ProjectActionNext>>();
        for (ProjectActionNext action : actions) {
            if (action.getNextActionDate() == null) {
                continue;
            }
            String key = toDayKey(action.getNextActionDate());
            List<ProjectActionNext> bucket = map.get(key);
            if (bucket == null) {
                bucket = new ArrayList<ProjectActionNext>();
                map.put(key, bucket);
            }
            bucket.add(action);
        }
        return map;
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
        if (ProjectNextActionType.MIGHT.equals(actionType)) {
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

    private Date parseDay(String dayKey) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        try {
            return sdf.parse(dayKey);
        } catch (ParseException e) {
            return null;
        }
    }

    private String toDayKey(Date day) {
        return new SimpleDateFormat("yyyy-MM-dd").format(day);
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
