package org.dandeliondaily.dashboard.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.dashboard.model.DashboardTodayColumnModel;
import org.dandeliondaily.projectnarrative.model.ProjectNarrativeSummary;
import org.dandeliondaily.projectnarrative.service.ProjectNarrativeService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeAdder;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.ProcessStage;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.WebUser;

public class DashboardTodayColumnService {

    private final ActionSentenceImportService actionSentenceImportService = new ActionSentenceImportService();
    private final ProjectNarrativeService projectNarrativeService = new ProjectNarrativeService();

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_SENTENCE_INPUT = "sentenceInput";
    private static final String ACTION_SCHEDULE = "Schedule";
    private static final String ACTION_SCHEDULE_AND_START = "Schedule and Start";

    private static final int BUCKET_START_OF_WORK_DAY = 0;
    private static final int BUCKET_OVERDUE = 1;
    private static final int BUCKET_COMMITTED = 3;
    private static final int BUCKET_WILL = 4;
    private static final int BUCKET_PERSONAL_MORNING = 5;
    private static final int BUCKET_MIGHT = 6;
    private static final int BUCKET_WAITING = 7;
    private static final int BUCKET_WILL_MEET = 8;
    private static final int BUCKET_END_OF_WORK_DAY = 9;
    private static final int BUCKET_OTHER = 11;

    public void handleQuickCapture(AppReq appReq) {
        String action = appReq.getRequest().getParameter(PARAM_ACTION);
        if (!ACTION_SCHEDULE.equals(action) && !ACTION_SCHEDULE_AND_START.equals(action)) {
            return;
        }

        String sentenceInput = appReq.getRequest().getParameter(PARAM_SENTENCE_INPUT);
        if (sentenceInput == null || sentenceInput.trim().length() == 0) {
            appReq.addWarningMessage("Quick capture requires text before scheduling.");
            return;
        }

        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        ActionNext selectedAction = appReq.getCompletingAction();
        Project selectedProject = selectedAction == null ? null : selectedAction.getProject();

        List<Project> projectList = loadProjectList(webUser, dataSession);
        ActionNext nextAction = actionSentenceImportService.saveNewActionFromSentence(webUser, dataSession,
                selectedProject, projectList, sentenceInput);
        if (nextAction == null) {
            appReq.addErrorMessage("Unable to create action from quick capture sentence.");
            return;
        }

        if (ACTION_SCHEDULE_AND_START.equals(action)) {
            appReq.setCompletingAction(nextAction);
            appReq.setProject(nextAction.getProject());
        }
        appReq.addSuccessMessage("Saved quick capture action.");
    }

    public DashboardTodayColumnModel buildModel(AppReq appReq) {
        DashboardTodayColumnModel model = new DashboardTodayColumnModel();

        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();

        model.getQuickCapture().setSentenceValue("");
        String actionParam = appReq.getRequest().getParameter(PARAM_ACTION);
        if (ACTION_SCHEDULE.equals(actionParam)) {
            model.getQuickCapture().setFocusRequested(true);
        }
        List<Project> quickCaptureProjects = loadProjectList(webUser, dataSession);
        List<String> projectNames = new ArrayList<String>();
        for (Project project : quickCaptureProjects) {
            if (project != null && project.getProjectName() != null) {
                projectNames.add(project.getProjectName());
            }
        }
        model.getQuickCapture().setProjectNames(projectNames);

        // Real data wiring starts here for the middle Today column.
        List<ActionNext> dueTodayList = getProjectActionListForToday(webUser, dataSession, 0);
        List<ActionNext> overdueList = getProjectActionListForToday(webUser, dataSession, -1);
        dueTodayList = filterActionsForDashboardVisibility(dueTodayList, false);
        overdueList = filterActionsForDashboardVisibility(overdueList, true);
        List<ActionNext> ideasList = getWouldLikeToIdeasList(webUser, dataSession);
        sortProjectActionListByCompletionOrder(dueTodayList, webUser);
        sortProjectActionListByCompletionOrder(overdueList, webUser);

        model.setActionGroups(buildTodayGroups(webUser, dueTodayList, overdueList, ideasList));

        List<ActionNext> completedToday = getProjectActionListClosedToday(webUser, dataSession);
        model.setCompletedToday(toActionItems(webUser, completedToday, "Completed"));
        model.setWorkdayReview(buildWorkdayReviewModel(webUser, dataSession, model.getActionGroups(), completedToday));

        List<ActionNext> todayAndOverdue = new ArrayList<ActionNext>();
        todayAndOverdue.addAll(overdueList);
        todayAndOverdue.addAll(dueTodayList);
        model.setTotals(buildTotals(appReq, webUser, todayAndOverdue));

        return model;
    }

    public List<String> listQuickCaptureProjectNames(AppReq appReq) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        List<Project> quickCaptureProjects = loadProjectList(webUser, dataSession);
        List<String> projectNames = new ArrayList<String>();
        for (Project project : quickCaptureProjects) {
            if (project != null && project.getProjectName() != null) {
                projectNames.add(project.getProjectName());
            }
        }
        return projectNames;
    }

    private DashboardTodayColumnModel.WorkdayReviewModel buildWorkdayReviewModel(WebUser webUser, Session dataSession,
            List<DashboardTodayColumnModel.TodayActionGroupModel> actionGroups,
            List<ActionNext> completedToday) {
        DashboardTodayColumnModel.WorkdayReviewModel model = new DashboardTodayColumnModel.WorkdayReviewModel();
        model.setTopPriority(isWorkdayComplete(actionGroups));

        if (completedToday == null || completedToday.isEmpty()) {
            model.setRenderSection(false);
            model.setAllReviewed(false);
            return model;
        }

        List<Integer> completedActionProjectIds = new ArrayList<Integer>();
        for (ActionNext action : completedToday) {
            if (action == null || action.getProject() == null || action.getProject().getProjectId() <= 0) {
                continue;
            }
            completedActionProjectIds.add(action.getProject().getProjectId());
        }

        List<ProjectNarrativeSummary> summaries = projectNarrativeService.listNarrativeSummariesForCompletedProjects(
                webUser, dataSession, webUser.getLocalDateToday(), completedActionProjectIds);

        List<DashboardTodayColumnModel.WorkdayReviewItemModel> reviewItems = new ArrayList<DashboardTodayColumnModel.WorkdayReviewItemModel>();
        boolean allReviewed = !summaries.isEmpty();
        for (ProjectNarrativeSummary summary : summaries) {
            // Filter using summed bill-entry minutes for the day (source of truth for
            // tracked time).
            if (summary.getMinutesSpent() < 5) {
                continue;
            }

            DashboardTodayColumnModel.WorkdayReviewItemModel item = new DashboardTodayColumnModel.WorkdayReviewItemModel();
            item.setProjectId(summary.getProjectId());
            item.setProjectName(summary.getProjectName());
            item.setCompletedCount(summary.getCompletedCount());
            item.setMinutesSpent(summary.getMinutesSpent());
            item.setMinutesDisplay(ActionNext.getTimeForDisplay(summary.getMinutesSpent()));
            item.setReviewed(summary.isReviewed());
            item.setNote(summary.getNarrativeEntry().getNote());
            item.setDecision(summary.getNarrativeEntry().getDecision());
            item.setInsight(summary.getNarrativeEntry().getInsight());
            item.setRisk(summary.getNarrativeEntry().getRisk());
            item.setOpportunity(summary.getNarrativeEntry().getOpportunity());

            // Add completed actions for this project
            List<String> projectCompletedActions = new ArrayList<>();
            for (ActionNext action : completedToday) {
                if (action != null && action.getProject() != null &&
                        action.getProject().getProjectId() == summary.getProjectId()) {
                    if (action.getNextDescription() != null && !action.getNextDescription().isEmpty()) {
                        projectCompletedActions.add(action.getNextDescription());
                    }
                }
            }
            item.setCompletedActions(projectCompletedActions);

            reviewItems.add(item);
            if (!summary.isReviewed()) {
                allReviewed = false;
            }
        }

        model.setProjectItems(reviewItems);
        model.setRenderSection(!reviewItems.isEmpty());
        model.setAllReviewed(allReviewed && !reviewItems.isEmpty());
        return model;
    }

    private boolean isWorkdayComplete(List<DashboardTodayColumnModel.TodayActionGroupModel> actionGroups) {
        if (actionGroups == null || actionGroups.isEmpty()) {
            return true;
        }

        return !hasItemsForSection(actionGroups, "Committed")
                && !hasItemsForSection(actionGroups, "Will Meet")
                && !hasItemsForSection(actionGroups, "Will")
                && !hasItemsForSection(actionGroups, "Start of Work Day");
    }

    private boolean hasItemsForSection(List<DashboardTodayColumnModel.TodayActionGroupModel> actionGroups,
            String title) {
        for (DashboardTodayColumnModel.TodayActionGroupModel group : actionGroups) {
            if (group == null || group.getTitle() == null) {
                continue;
            }
            if (title.equals(group.getTitle()) && group.getItems() != null && !group.getItems().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private DashboardTodayColumnModel.TodayTotalsModel buildTotals(AppReq appReq, WebUser webUser,
            List<ActionNext> todayAndOverdue) {
        DashboardTodayColumnModel.TodayTotalsModel totals = new DashboardTodayColumnModel.TodayTotalsModel();

        TimeAdder timeAdderToday = new TimeAdder(todayAndOverdue, appReq);
        TimeAdder timeAdderScheduled = new TimeAdder(todayAndOverdue, appReq, webUser.getToday());

        int completedAct = timeAdderToday.getCompletedAct();
        int committedEst = timeAdderScheduled.getCommittedEst();
        int willEst = timeAdderScheduled.getWillEst();
        int willMeetEst = timeAdderScheduled.getWillMeetEst();
        int remainingPlannedTotal = committedEst + willEst + willMeetEst;
        int plannedTotal = completedAct + remainingPlannedTotal;

        totals.setCompletedDisplay(ActionNext.getTimeForDisplay(completedAct));
        totals.setCommittedDisplay(ActionNext.getTimeForDisplay(committedEst));
        totals.setWillDisplay(ActionNext.getTimeForDisplay(willEst));
        totals.setWillMeetDisplay(ActionNext.getTimeForDisplay(willMeetEst));
        totals.setTotalPlannedDisplay(ActionNext.getTimeForDisplay(plannedTotal));
        totals.setCompletedMinutes(completedAct);
        totals.setPlannedMinutes(plannedTotal);

        if (plannedTotal == 0) {
            totals.setGuidanceMessage("You have finished everything you said you would do today.");
            totals.setOverCommitted(false);
        } else if (completedAct > (8 * 60)) {
            totals.setGuidanceMessage("You have already logged more than a full workday.");
            totals.setOverCommitted(true);
        } else if (plannedTotal > (8 * 60)) {
            totals.setGuidanceMessage("You are over committed for today. Consider re-planning.");
            totals.setOverCommitted(true);
        } else if (completedAct < 30) {
            totals.setGuidanceMessage("Good morning. You are just getting started.");
            totals.setOverCommitted(false);
        } else {
            totals.setGuidanceMessage("You are on track for today.");
            totals.setOverCommitted(false);
        }
        return totals;
    }

    private List<DashboardTodayColumnModel.TodayActionGroupModel> buildTodayGroups(WebUser webUser,
            List<ActionNext> dueTodayList, List<ActionNext> overdueList,
            List<ActionNext> ideasList) {
        Map<Integer, List<ActionNext>> bucketMap = new HashMap<Integer, List<ActionNext>>();
        for (int bucket = BUCKET_START_OF_WORK_DAY; bucket <= BUCKET_OTHER; bucket++) {
            bucketMap.put(bucket, new ArrayList<ActionNext>());
        }

        bucketMap.get(BUCKET_OVERDUE).addAll(overdueList);
        for (ActionNext projectAction : dueTodayList) {
            int bucket = getCompletionBucket(projectAction, webUser);
            if (bucket > BUCKET_OTHER) {
                continue;
            }
            bucketMap.get(bucket).add(projectAction);
        }

        for (int bucket = BUCKET_START_OF_WORK_DAY; bucket <= BUCKET_OTHER; bucket++) {
            sortProjectActionListByCompletionOrder(bucketMap.get(bucket), webUser);
        }

        List<DashboardTodayColumnModel.TodayActionGroupModel> groups = new ArrayList<DashboardTodayColumnModel.TodayActionGroupModel>();
        addGroup(groups, "Overdue", toActionItems(webUser, bucketMap.get(BUCKET_OVERDUE), "Overdue"));
        addGroup(groups, "Start of Work Day",
                toActionItems(webUser, bucketMap.get(BUCKET_START_OF_WORK_DAY), "Start of Work Day"));
        addGroup(groups, "Committed", toActionItems(webUser, bucketMap.get(BUCKET_COMMITTED), "Committed"));
        addGroup(groups, "Will", toActionItems(webUser, bucketMap.get(BUCKET_WILL), "Will"));
        addGroup(groups, "Personal (Morning)",
                toActionItems(webUser, bucketMap.get(BUCKET_PERSONAL_MORNING), TimeSlot.MORNING.getLabel()));
        addGroup(groups, "Might", toActionItems(webUser, bucketMap.get(BUCKET_MIGHT), "Might"));
        addGroup(groups, "Waiting", toActionItems(webUser, bucketMap.get(BUCKET_WAITING), "Waiting"));
        addGroup(groups, "Will Meet", toActionItems(webUser, bucketMap.get(BUCKET_WILL_MEET), "Will Meet"));
        addGroup(groups, "End of Work Day",
                toActionItems(webUser, bucketMap.get(BUCKET_END_OF_WORK_DAY), "End of Work Day"));
        addGroup(groups, "Ideas", toActionItems(webUser, ideasList, "Ideas"));
        addGroup(groups, "Other", toActionItems(webUser, bucketMap.get(BUCKET_OTHER), "Other"));
        return groups;
    }

    private List<ActionNext> getWouldLikeToIdeasList(WebUser webUser, Session dataSession) {
        Integer workspaceId = WorkspaceRegistry.getWorkspaceIdForWebUserId(webUser.getWebUserId());
        Query query = dataSession.createQuery(
                "select distinct an from ActionNext an "
                        + "left join fetch an.project "
                        + "left join fetch an.contact "
                        + "left join fetch an.nextProjectContact "
                        + "where an.workspaceId = :workspaceId and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                        + "and an.nextDescription <> '' "
                        + "and an.nextActionStatusString = :nextActionStatus "
                        + "and an.nextActionType = :nextActionType "
                        + "and an.billable = :billable "
                        + "order by an.actionNextId DESC");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
        query.setParameter("nextActionType", ProjectNextActionType.WOULD_LIKE_TO);
        query.setParameter("billable", true);
        @SuppressWarnings("unchecked")
        List<ActionNext> projectActionList = query.list();
        return projectActionList;
    }

    private List<ActionNext> filterActionsForDashboardVisibility(List<ActionNext> actions,
            boolean workOnly) {
        List<ActionNext> filtered = new ArrayList<ActionNext>();
        for (ActionNext action : actions) {
            if (action == null) {
                continue;
            }
            if (action.isBillable() || action.getTimeSlot() == TimeSlot.MORNING) {
                filtered.add(action);
            }
        }
        return filtered;
    }

    private void addGroup(List<DashboardTodayColumnModel.TodayActionGroupModel> groups, String title,
            List<DashboardTodayColumnModel.TodayActionItemModel> items) {
        if (items.isEmpty()) {
            return;
        }
        groups.add(new DashboardTodayColumnModel.TodayActionGroupModel(title, items));
    }

    private List<DashboardTodayColumnModel.TodayActionItemModel> toActionItems(WebUser webUser,
            List<ActionNext> actions, String contextLabel) {
        List<DashboardTodayColumnModel.TodayActionItemModel> items = new ArrayList<DashboardTodayColumnModel.TodayActionItemModel>();
        for (ActionNext action : actions) {
            DashboardTodayColumnModel.TodayActionItemModel item = new DashboardTodayColumnModel.TodayActionItemModel();
            item.setProjectName(action.getProject() == null ? "" : n(action.getProject().getProjectName(), ""));
            item.setDescriptionText(n(action.getNextDescription(), ""));
            item.setDescriptionHtml(action.getNextDescriptionForDisplay(webUser.getProjectContact()));
            item.setActionNextId(action.getActionNextId());
            item.setEstimateDisplay(displayTime(action.getNextTimeEstimateForDisplay()));
            item.setActualDisplay(displayTime(action.getNextTimeActualForDisplay()));
            item.setEstimateMinutes(action.getNextTimeEstimate() == null ? 0 : action.getNextTimeEstimate());
            item.setActualMinutes(action.getNextTimeActual() == null ? 0 : action.getNextTimeActual());
            if (action.getTimeSlot() != null && action.getTimeSlot().getLabel() != null
                    && action.getTimeSlot().getLabel().trim().length() > 0) {
                item.setContextLabel(action.getTimeSlot().getLabel());
            } else {
                item.setContextLabel(contextLabel);
            }
            item.setStatusLabel(resolveStatusLabel(action));
            items.add(item);
        }
        return items;
    }

    private List<ActionNext> getProjectActionListForToday(WebUser webUser, Session dataSession, int dayOffset) {
        Integer workspaceId = WorkspaceRegistry.getWorkspaceIdForWebUserId(webUser.getWebUserId());
        LocalDate today = webUser.getLocalDateToday();
        Query query;
        if (dayOffset < 0) {
            LocalDate oldestDate = today.minusYears(1);
            query = dataSession.createQuery(
                    "select distinct an from ActionNext an "
                            + "left join fetch an.project "
                            + "left join fetch an.contact "
                            + "left join fetch an.nextProjectContact "
                            + "where an.workspaceId = :workspaceId and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                            + "and an.nextDescription <> '' "
                            + "and an.nextActionStatusString = :nextActionStatus "
                            + "and an.nextActionDate >= :oldestDate and an.nextActionDate < :cutoffDate "
                            + "order by an.nextActionDate, an.priorityLevel DESC, an.nextTimeEstimate, an.nextChangeDate");
            query.setParameter("oldestDate", java.sql.Date.valueOf(oldestDate));
            query.setParameter("cutoffDate", java.sql.Date.valueOf(today));
        } else {
            LocalDate targetDate = today.plusDays(dayOffset);
            query = dataSession.createQuery(
                    "select distinct an from ActionNext an "
                            + "left join fetch an.project "
                            + "left join fetch an.contact "
                            + "left join fetch an.nextProjectContact "
                            + "where an.workspaceId = :workspaceId and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                            + "and an.nextDescription <> '' "
                            + "and an.nextActionStatusString = :nextActionStatus "
                            + "and an.nextActionDate = :targetDate "
                            + "order by an.nextActionDate, an.priorityLevel DESC, an.nextTimeEstimate, an.nextChangeDate");
            query.setParameter("targetDate", java.sql.Date.valueOf(targetDate));
        }
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
        @SuppressWarnings("unchecked")
        List<ActionNext> projectActionList = query.list();
        sortProjectActionList(projectActionList, webUser);
        return projectActionList;
    }

    private List<ActionNext> getProjectActionListClosedToday(WebUser webUser, Session dataSession) {
        Integer workspaceId = WorkspaceRegistry.getWorkspaceIdForWebUserId(webUser.getWebUserId());
        Date today = TimeTracker.createToday(webUser).getTime();
        Date tomorrow = TimeTracker.createTomorrow(webUser).getTime();
        Query query = dataSession.createQuery(
                "select distinct an from ActionNext an "
                        + "left join fetch an.project "
                        + "left join fetch an.contact "
                        + "left join fetch an.nextProjectContact "
                        + "where an.workspaceId = :workspaceId and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                        + "and an.nextActionStatusString = :nextActionStatus and an.nextDescription <> '' "
                        + "and an.nextChangeDate >= :today and an.nextChangeDate < :tomorrow "
                        + "order by an.nextTimeActual DESC, an.nextTimeEstimate DESC");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("nextActionStatus", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("today", today);
        query.setParameter("tomorrow", tomorrow);
        @SuppressWarnings("unchecked")
        List<ActionNext> projectActionList = query.list();
        return projectActionList;
    }

    private List<Project> loadProjectList(WebUser webUser, Session dataSession) {
        Query query = dataSession
                .createQuery(
                        "from Project where workspaceId = ? and (projectStatus is null or projectStatus = :activeStatus) order by projectName");
        query.setParameter(0, WorkspaceRegistry.getWorkspaceIdForWebUserId(webUser.getWebUserId()));
        query.setParameter("activeStatus", ProjectStatus.ACTIVE.getDatabaseValue());
        @SuppressWarnings("unchecked")
        List<Project> projectList = query.list();
        return projectList;
    }

    private String resolveStatusLabel(ActionNext projectAction) {
        if (projectAction.getNextActionStatus() != null) {
            return projectAction.getNextActionStatus().getLabel();
        }
        return "Unknown";
    }

    private String displayTime(String value) {
        return n(value, "-");
    }

    private static void sortProjectActionList(List<ActionNext> projectActionList, WebUser webUser) {
        Collections.sort(projectActionList, (pa1, pa2) -> {
            int c1 = pa1.getCompletionOrder();
            int c2 = pa2.getCompletionOrder();
            if (c1 > 0 && c2 <= 0) {
                return -1;
            }
            if (c2 > 0 && c1 <= 0) {
                return 1;
            }
            int bucket1 = getCompletionBucket(pa1, webUser);
            int bucket2 = getCompletionBucket(pa2, webUser);
            if (bucket1 != bucket2) {
                return bucket1 - bucket2;
            }
            return compareInsideBucket(pa1, pa2);
        });
    }

    private static void sortProjectActionListByCompletionOrder(List<ActionNext> projectActionList,
            WebUser webUser) {
        Collections.sort(projectActionList, (pa1, pa2) -> {
            int c1 = pa1.getCompletionOrder();
            int c2 = pa2.getCompletionOrder();
            if (c1 > 0 && c2 <= 0) {
                return -1;
            }
            if (c2 > 0 && c1 <= 0) {
                return 1;
            }
            int bucket1 = getCompletionBucket(pa1, webUser);
            int bucket2 = getCompletionBucket(pa2, webUser);
            if (bucket1 != bucket2) {
                return bucket1 - bucket2;
            }
            if (c1 > 0 || c2 > 0) {
                if (c1 <= 0) {
                    return 1;
                }
                if (c2 <= 0) {
                    return -1;
                }
                if (c1 != c2) {
                    return c1 - c2;
                }
            }
            return compareInsideBucket(pa1, pa2);
        });
    }

    private static int compareInsideBucket(ActionNext pa1, ActionNext pa2) {
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

    private static int getCompletionBucket(ActionNext projectAction, WebUser webUser) {
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
        LocalDate actionDate = toStoredLocalDate(projectAction.getNextActionDate(), webUser);
        if (projectAction.isBillable() && actionDate != null && actionDate.isBefore(webUser.getLocalDateToday())) {
            return BUCKET_OVERDUE;
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
        if (ProjectNextActionType.WOULD_LIKE_TO.equals(nextActionType)) {
            return 99;
        }
        return BUCKET_OTHER;
    }

    private static LocalDate toStoredLocalDate(Date date, WebUser webUser) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return webUser.toLocalDate(date);
    }

    private String n(String value, String defaultValue) {
        if (value == null || value.trim().equals("")) {
            return defaultValue;
        }
        return value;
    }
}