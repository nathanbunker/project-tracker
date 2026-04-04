package org.dandeliondaily.dashboard.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.dashboard.model.DashboardNextColumnModel;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeAdder;
import org.openimmunizationsoftware.pt.model.ProcessStage;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.WebUser;

public class DashboardNextColumnService {

    private static final int DAY_CAPACITY_MINUTES = 8 * 60;
    private static final int MAX_SUMMARY_CHIPS = 4;
    private static final String SESSION_SELECTED_DAY_KEY = "DASHBOARD_NEXT_SELECTED_DAY";

    private static final int BUCKET_START_OF_WORK_DAY = 0;
    private static final int BUCKET_OVERDUE = 1;
    private static final int BUCKET_PERSONAL_WAKE = 2;
    private static final int BUCKET_COMMITTED = 3;
    private static final int BUCKET_WILL = 4;
    private static final int BUCKET_PERSONAL_MORNING = 5;
    private static final int BUCKET_MIGHT = 6;
    private static final int BUCKET_WAITING = 7;
    private static final int BUCKET_WILL_MEET = 8;
    private static final int BUCKET_END_OF_WORK_DAY = 9;
    private static final int BUCKET_PERSONAL_LATE = 10;
    private static final int BUCKET_OTHER = 11;

    public DashboardNextColumnModel buildModel(AppReq appReq, DashboardTimeGaugeService gaugeService) {
        DashboardNextColumnModel model = new DashboardNextColumnModel();

        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();

        Calendar planningCalendar = getCalendarForTodayNoTime(webUser);
        planningCalendar.add(Calendar.DAY_OF_MONTH, 1);
        Date planningStartDate = planningCalendar.getTime();
        planningCalendar.add(Calendar.DAY_OF_MONTH, 13);
        Date planningEndDate = planningCalendar.getTime();

        List<ProjectActionNext> planningRangeList = getProjectActionListForPlanningRange(webUser, dataSession,
                planningStartDate, planningEndDate);
        planningRangeList = filterActionsForDashboardVisibility(planningRangeList);
        Map<String, List<ProjectActionNext>> planningBuckets = bucketByDate(planningRangeList);

        List<DashboardNextColumnModel.NextDaySummaryModel> summaries = new ArrayList<DashboardNextColumnModel.NextDaySummaryModel>();
        for (int dayOffset = 1; dayOffset < 14 && summaries.size() < MAX_SUMMARY_CHIPS; dayOffset++) {
            Calendar dayCalendar = getCalendarForTodayNoTime(webUser);
            dayCalendar.add(Calendar.DAY_OF_MONTH, dayOffset);
            Date dayDate = dayCalendar.getTime();
            String dayKey = toDatabaseDateKey(dayDate);
            List<ProjectActionNext> dayActions = planningBuckets.get(dayKey);
            if (dayActions == null || dayActions.isEmpty()) {
                continue;
            }
            sortProjectActionList(dayActions);

            TimeAdder timeAdder = new TimeAdder(dayActions, appReq, dayDate);
            int plannedMinutes = timeAdder.getWillAct();
            if (plannedMinutes <= 0) {
                continue;
            }

            DashboardNextColumnModel.NextDaySummaryModel summary = new DashboardNextColumnModel.NextDaySummaryModel();
            summary.setDayKey(dayKey);
            summary.setDayLabel(webUser.getDateFormatService().formatPattern(dayDate,
                    webUser.getDateDisplayPatternWithWeekdayShort(), webUser.getTimeZone()));
            summary.setDayShortLabel(webUser.getDateFormatService().formatPattern(dayDate, "EEEE",
                    webUser.getTimeZone()));
            summary.setFullDateLabel(webUser.getDateFormatService().formatPattern(dayDate, "EEEE dd MMM yyyy",
                    webUser.getTimeZone()));
            summary.setCommittedDisplay(ProjectActionNext.getTimeForDisplay(timeAdder.getCommittedEst()));
            summary.setWillDisplay(ProjectActionNext.getTimeForDisplay(timeAdder.getWillEst()));
            summary.setWillMeetDisplay(ProjectActionNext.getTimeForDisplay(timeAdder.getWillMeetEst()));
            summary.setPlannedDisplay(ProjectActionNext.getTimeForDisplay(plannedMinutes));
            summary.setPlannedMinutes(plannedMinutes);
            summary.setInlineGauge(gaugeService.buildInlineBarLongGauge(plannedMinutes, DAY_CAPACITY_MINUTES));
            summaries.add(summary);
        }
        model.setDaySummaries(summaries);

        String selectedDayParam = appReq.getRequest().getParameter(DashboardNextColumnModel.getSelectedDayParam());
        if (selectedDayParam == null || selectedDayParam.length() == 0) {
            Object sessionSelectedDay = appReq.getWebSession().getAttribute(SESSION_SELECTED_DAY_KEY);
            if (sessionSelectedDay instanceof String) {
                selectedDayParam = (String) sessionSelectedDay;
            }
        }
        DashboardNextColumnModel.NextDaySummaryModel selectedSummary = resolveSelectedSummary(summaries,
                selectedDayParam);
        if (selectedSummary != null) {
            selectedSummary.setSelected(true);
            appReq.getWebSession().setAttribute(SESSION_SELECTED_DAY_KEY, selectedSummary.getDayKey());
            DashboardNextColumnModel.SelectedDayModel selectedDay = new DashboardNextColumnModel.SelectedDayModel();
            selectedDay.setDayKey(selectedSummary.getDayKey());
            selectedDay.setDayLabel(selectedSummary.getDayLabel());
            selectedDay.setFullDateLabel(selectedSummary.getFullDateLabel());
            selectedDay.setPlannedDisplay(selectedSummary.getPlannedDisplay());
            selectedDay.setPlannedMinutes(selectedSummary.getPlannedMinutes());
            selectedDay.setHeaderGauge(gaugeService.buildPlannedDayGauge(selectedSummary.getPlannedMinutes()));
            List<ProjectActionNext> selectedDayActions = planningBuckets.get(selectedSummary.getDayKey());
            selectedDay.setSections(buildSelectedDaySections(webUser, selectedDayActions));
            model.setSelectedDay(selectedDay);
        }

        // Future refinement: tune how summary days are selected when planning horizon
        // contains sparse data.
        // Future enhancement: optional AJAX day switching can be added without changing
        // this model shape.
        return model;
    }

    private DashboardNextColumnModel.NextDaySummaryModel resolveSelectedSummary(
            List<DashboardNextColumnModel.NextDaySummaryModel> summaries, String selectedDayParam) {
        if (summaries == null || summaries.isEmpty()) {
            return null;
        }
        if (selectedDayParam != null && selectedDayParam.length() > 0) {
            for (DashboardNextColumnModel.NextDaySummaryModel summary : summaries) {
                if (selectedDayParam.equals(summary.getDayKey())) {
                    return summary;
                }
            }
        }
        return summaries.get(0);
    }

    private List<DashboardNextColumnModel.SelectedDaySectionModel> buildSelectedDaySections(WebUser webUser,
            List<ProjectActionNext> selectedDayActions) {
        List<DashboardNextColumnModel.SelectedDaySectionModel> sections = new ArrayList<DashboardNextColumnModel.SelectedDaySectionModel>();
        if (selectedDayActions == null || selectedDayActions.isEmpty()) {
            return sections;
        }

        Map<Integer, List<ProjectActionNext>> bucketMap = new HashMap<Integer, List<ProjectActionNext>>();
        for (int bucket = BUCKET_START_OF_WORK_DAY; bucket <= BUCKET_OTHER; bucket++) {
            bucketMap.put(bucket, new ArrayList<ProjectActionNext>());
        }

        for (ProjectActionNext action : selectedDayActions) {
            int bucket = getCompletionBucket(action);
            if (bucket > BUCKET_OTHER) {
                continue;
            }
            bucketMap.get(bucket).add(action);
        }

        for (int bucket = BUCKET_START_OF_WORK_DAY; bucket <= BUCKET_OTHER; bucket++) {
            sortProjectActionListByCompletionOrder(bucketMap.get(bucket));
        }

        addSection(sections, "Overdue", toSelectedDayItems(webUser, bucketMap.get(BUCKET_OVERDUE)));
        addSection(sections, "Start of Work Day", toSelectedDayItems(webUser, bucketMap.get(BUCKET_START_OF_WORK_DAY)));
        addSection(sections, "Committed", toSelectedDayItems(webUser, bucketMap.get(BUCKET_COMMITTED)));
        addSection(sections, "Will", toSelectedDayItems(webUser, bucketMap.get(BUCKET_WILL)));
        addSection(sections, "Personal (Morning)", toSelectedDayItems(webUser, bucketMap.get(BUCKET_PERSONAL_MORNING)));
        addSection(sections, "Might", toSelectedDayItems(webUser, bucketMap.get(BUCKET_MIGHT)));
        addSection(sections, "Waiting", toSelectedDayItems(webUser, bucketMap.get(BUCKET_WAITING)));
        addSection(sections, "Will Meet", toSelectedDayItems(webUser, bucketMap.get(BUCKET_WILL_MEET)));
        addSection(sections, "End of Work Day", toSelectedDayItems(webUser, bucketMap.get(BUCKET_END_OF_WORK_DAY)));
        addSection(sections, "Other", toSelectedDayItems(webUser, bucketMap.get(BUCKET_OTHER)));

        // Future: selected-day section rendering can be reused in dedicated planning
        // pages.
        return sections;
    }

    private void addSection(List<DashboardNextColumnModel.SelectedDaySectionModel> sections, String title,
            List<DashboardNextColumnModel.SelectedDayActionItemModel> items) {
        if (items.isEmpty()) {
            return;
        }
        sections.add(new DashboardNextColumnModel.SelectedDaySectionModel(title, items));
    }

    private List<DashboardNextColumnModel.SelectedDayActionItemModel> toSelectedDayItems(WebUser webUser,
            List<ProjectActionNext> actions) {
        List<DashboardNextColumnModel.SelectedDayActionItemModel> items = new ArrayList<DashboardNextColumnModel.SelectedDayActionItemModel>();
        for (ProjectActionNext action : actions) {
            DashboardNextColumnModel.SelectedDayActionItemModel item = new DashboardNextColumnModel.SelectedDayActionItemModel();
            item.setActionNextId(action.getActionNextId());
            item.setProjectName(action.getProject() == null ? "" : n(action.getProject().getProjectName()));
            item.setDescriptionHtml(action.getNextDescriptionForDisplay(webUser.getProjectContact()));
            item.setDescriptionPlain(n(action.getNextDescription()));
            item.setEstimateDisplay(n(action.getNextTimeEstimateForDisplay()));
            item.setActualDisplay(n(action.getNextTimeActualForDisplay()));
            item.setStatusLabel(resolveStatusLabel(action));
            items.add(item);
        }
        return items;
    }

    private Map<String, List<ProjectActionNext>> bucketByDate(List<ProjectActionNext> planningRangeList) {
        Map<String, List<ProjectActionNext>> planningBuckets = new HashMap<String, List<ProjectActionNext>>();
        for (ProjectActionNext projectAction : planningRangeList) {
            String bucketKey = toDatabaseDateKey(projectAction.getNextActionDate());
            List<ProjectActionNext> bucketList = planningBuckets.get(bucketKey);
            if (bucketList == null) {
                bucketList = new ArrayList<ProjectActionNext>();
                planningBuckets.put(bucketKey, bucketList);
            }
            bucketList.add(projectAction);
        }
        return planningBuckets;
    }

    private List<ProjectActionNext> getProjectActionListForPlanningRange(WebUser webUser, Session dataSession,
            Date startDate, Date endDate) {
        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan "
                        + "left join fetch pan.project "
                        + "left join fetch pan.contact "
                        + "left join fetch pan.nextProjectContact "
                        + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                        + "and pan.nextDescription <> '' "
                        + "and pan.nextActionDate >= :startDate and pan.nextActionDate < :endDate "
                        + "order by pan.nextActionDate, pan.priorityLevel DESC, pan.nextTimeEstimate, pan.nextChangeDate");
        query.setParameter("provider", webUser.getProvider());
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> projectActionList = query.list();
        return projectActionList;
    }

    private List<ProjectActionNext> filterActionsForDashboardVisibility(List<ProjectActionNext> actions) {
        List<ProjectActionNext> filtered = new ArrayList<ProjectActionNext>();
        for (ProjectActionNext action : actions) {
            if (action == null) {
                continue;
            }
            if (action.isBillable() || action.getTimeSlot() == TimeSlot.MORNING) {
                filtered.add(action);
            }
        }
        return filtered;
    }

    private Calendar getCalendarForTodayNoTime(WebUser webUser) {
        Calendar c = webUser.getCalendar();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private static String toDatabaseDateKey(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private static void sortProjectActionList(List<ProjectActionNext> projectActionList) {
        projectActionList.sort((pa1, pa2) -> {
            int bucket1 = getCompletionBucket(pa1);
            int bucket2 = getCompletionBucket(pa2);
            if (bucket1 != bucket2) {
                return bucket1 - bucket2;
            }
            return compareInsideBucket(pa1, pa2);
        });
    }

    private static void sortProjectActionListByCompletionOrder(List<ProjectActionNext> projectActionList) {
        projectActionList.sort((pa1, pa2) -> {
            int c1 = pa1 == null ? 0 : pa1.getCompletionOrder();
            int c2 = pa2 == null ? 0 : pa2.getCompletionOrder();
            if (c1 > 0 && c2 > 0 && c1 != c2) {
                return c1 - c2;
            }
            if (c1 > 0 && c2 <= 0) {
                return -1;
            }
            if (c2 > 0 && c1 <= 0) {
                return 1;
            }
            int bucket1 = getCompletionBucket(pa1);
            int bucket2 = getCompletionBucket(pa2);
            if (bucket1 != bucket2) {
                return bucket1 - bucket2;
            }
            return compareInsideBucket(pa1, pa2);
        });
    }

    private static int compareInsideBucket(ProjectActionNext pa1, ProjectActionNext pa2) {
        ProcessStage ps1 = pa1.getProcessStage();
        ProcessStage ps2 = pa2.getProcessStage();
        if ((ps1 != null || ps2 != null) && ps1 != ps2) {
            if (ps1 == ProcessStage.FIRST) {
                return -1;
            } else if (ps2 == ProcessStage.FIRST) {
                return 1;
            }
            if (ps1 == ProcessStage.SECOND) {
                return -1;
            } else if (ps2 == ProcessStage.SECOND) {
                return 1;
            }
            if (ps1 == ProcessStage.LAST) {
                return 1;
            } else if (ps2 == ProcessStage.LAST) {
                return -1;
            }
            if (ps1 == ProcessStage.PENULTIMATE) {
                return 1;
            } else if (ps2 == ProcessStage.PENULTIMATE) {
                return -1;
            }
        }
        int p1 = ProjectNextActionType.defaultPriority(pa1.getNextActionType());
        int p2 = ProjectNextActionType.defaultPriority(pa2.getNextActionType());
        if (p1 != p2) {
            return p2 - p1;
        }
        if (pa2.getPriorityLevel() != pa1.getPriorityLevel()) {
            return pa2.getPriorityLevel() - pa1.getPriorityLevel();
        }
        Date d1 = pa1.getNextChangeDate();
        Date d2 = pa2.getNextChangeDate();
        if (d1 != null && d2 != null) {
            int compare = d1.compareTo(d2);
            if (compare != 0) {
                return compare;
            }
        }
        return pa1.getActionNextId() - pa2.getActionNextId();
    }

    private static int getCompletionBucket(ProjectActionNext projectAction) {
        if (projectAction == null) {
            return 99;
        }
        if (projectAction.isBillable()) {
            ProcessStage processStage = projectAction.getProcessStage();
            if (processStage == ProcessStage.FIRST || processStage == ProcessStage.SECOND) {
                return BUCKET_START_OF_WORK_DAY;
            }
            if (processStage == ProcessStage.PENULTIMATE || processStage == ProcessStage.LAST) {
                return BUCKET_END_OF_WORK_DAY;
            }
        }
        if (!projectAction.isBillable()) {
            if (projectAction.getTimeSlot() == TimeSlot.MORNING) {
                return BUCKET_PERSONAL_MORNING;
            }
            return 99;
        }
        String nextActionType = projectAction.getNextActionType();
        if (ProjectNextActionType.OVERDUE_TO.equals(nextActionType)) {
            return BUCKET_OVERDUE;
        }
        if (ProjectNextActionType.COMMITTED_TO.equals(nextActionType)) {
            return BUCKET_COMMITTED;
        }
        if (ProjectNextActionType.WILL.equals(nextActionType)
                || ProjectNextActionType.WILL_CONTACT.equals(nextActionType)
                || ProjectNextActionType.WILL_REVIEW.equals(nextActionType)
                || ProjectNextActionType.WILL_DOCUMENT.equals(nextActionType)
                || ProjectNextActionType.WILL_FOLLOW_UP.equals(nextActionType)) {
            return BUCKET_WILL;
        }
        if (ProjectNextActionType.MIGHT.equals(nextActionType)
                || ProjectNextActionType.GOAL.equals(nextActionType)) {
            return BUCKET_MIGHT;
        }
        if (ProjectNextActionType.WAITING.equals(nextActionType)) {
            return BUCKET_WAITING;
        }
        if (ProjectNextActionType.WILL_MEET.equals(nextActionType)) {
            return BUCKET_WILL_MEET;
        }
        return BUCKET_OTHER;
    }

    private String resolveStatusLabel(ProjectActionNext projectAction) {
        if (projectAction.getNextActionStatus() != null) {
            return projectAction.getNextActionStatus().getLabel();
        }
        return "Unknown";
    }

    private String n(String value) {
        if (value == null || value.trim().length() == 0) {
            return "-";
        }
        return value;
    }
}
