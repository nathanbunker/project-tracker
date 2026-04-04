package org.dandeliondaily.dashboard.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.ProjectActionBlockerManager;
import org.openimmunizationsoftware.pt.model.ProcessStage;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.WebUser;

public class DashboardCurrentActionService {

    private final ActionSentenceImportService actionSentenceImportService = new ActionSentenceImportService();

    private static final String PARAM_ACTION = "action";
    private static final String ACTION_WORK_NEXT = "WorkNext";
    private static final String ACTION_SELECT = "SelectAction";
    private static final String PARAM_NEXT_SUMMARY = "nextSummary";
    private static final String PARAM_WORK_STATUS = "workStatus";
    private static final String PARAM_WORK_FOLLOW_UP = "workFollowUp";
    private static final String PARAM_COMPLETING_ACTION_NEXT_ID = "completingActionNextId";

    private static final String WORK_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String WORK_STATUS_COMPLETE = "COMPLETE";
    private static final String WORK_STATUS_DELETE = "DELETE";
    private static final String WORK_STATUS_BLOCKED = "BLOCKED";

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

    public void handleSelectAction(AppReq appReq) {
        String actionParam = appReq.getRequest().getParameter(PARAM_ACTION);
        if (!ACTION_SELECT.equals(actionParam)) {
            return;
        }
        String actionIdString = appReq.getRequest().getParameter(PARAM_COMPLETING_ACTION_NEXT_ID);
        if (actionIdString == null || actionIdString.trim().length() == 0) {
            return;
        }
        try {
            int actionId = Integer.parseInt(actionIdString.trim());
            Session dataSession = appReq.getDataSession();
            ProjectActionNext selected = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionId);
            if (selected != null) {
                appReq.setCompletingAction(selected);
                if (selected.getProject() != null) {
                    appReq.setProject(selected.getProject());
                }
            }
        } catch (NumberFormatException nfe) {
            // ignore invalid id
        }
    }

    public void ensureCurrentActionSelected(AppReq appReq) {
        ProjectActionNext currentAction = appReq.getCompletingAction();
        if (currentAction != null) {
            return;
        }

        ProjectActionNext nextAction = selectNextActionForWorkFlow(appReq.getWebUser(), appReq.getDataSession(), 0);
        appReq.setCompletingAction(nextAction);
        if (nextAction != null && nextAction.getProject() != null) {
            appReq.setProject(nextAction.getProject());
        }
    }

    public void handleCurrentActionWork(AppReq appReq) {
        String action = appReq.getRequest().getParameter(PARAM_ACTION);
        if (!ACTION_WORK_NEXT.equals(action)) {
            return;
        }

        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();

        ProjectActionNext actionToWork = resolveActionToWork(appReq, dataSession);
        if (actionToWork == null) {
            appReq.addErrorMessage("Current action was not found.");
            return;
        }

        updateSummaryFromRequest(appReq, dataSession, actionToWork);

        String workStatus = normalizeWorkStatus(appReq.getRequest().getParameter(PARAM_WORK_STATUS));
        if (workStatus.length() == 0) {
            workStatus = WORK_STATUS_COMPLETE;
        }

        String followUpText = appReq.getRequest().getParameter(PARAM_WORK_FOLLOW_UP);
        followUpText = followUpText == null ? "" : followUpText.trim();
        boolean hasFollowUp = followUpText.length() > 0;

        if (WORK_STATUS_BLOCKED.equals(workStatus) && !hasFollowUp) {
            appReq.addWarningMessage("Blocked requires a follow-up action.");
            return;
        }

        ProjectActionNext followUpAction = null;
        if (hasFollowUp) {
            followUpAction = createFollowUpActionFromSentence(webUser, dataSession, actionToWork, followUpText);
        }

        if (WORK_STATUS_COMPLETE.equals(workStatus)) {
            closeAction(appReq, actionToWork, actionToWork.getProject(), actionToWork.getNextSummary(),
                    ProjectNextActionStatus.COMPLETED);
        } else if (WORK_STATUS_DELETE.equals(workStatus)) {
            closeAction(appReq, actionToWork, actionToWork.getProject(), "", ProjectNextActionStatus.CANCELLED);
        } else if (WORK_STATUS_BLOCKED.equals(workStatus) && followUpAction != null) {
            Transaction blockTrans = dataSession.beginTransaction();
            actionToWork.setBlockedBy(followUpAction);
            actionToWork.setNextActionDate(null);
            actionToWork.setNextChangeDate(new Date());
            dataSession.update(actionToWork);
            blockTrans.commit();
        } else if (WORK_STATUS_IN_PROGRESS.equals(workStatus)) {
            appReq.addInfoMessage("Progress saved.");
        }

        rationalizeCompletionOrderForCurrentDate(webUser, dataSession);

        int actionToWorkId = actionToWork.getActionNextId();
        ProjectActionNext nextCompletingAction;
        if (WORK_STATUS_BLOCKED.equals(workStatus)
                && followUpAction != null
                && followUpAction.getNextActionDate() != null
                && webUser.isToday(followUpAction.getNextActionDate())) {
            nextCompletingAction = followUpAction;
        } else {
            nextCompletingAction = selectNextActionForWorkFlow(webUser, dataSession, actionToWorkId);
        }

        appReq.setCompletingAction(nextCompletingAction);
        if (nextCompletingAction != null && nextCompletingAction.getProject() != null) {
            appReq.setProject(nextCompletingAction.getProject());
        }

        if (WORK_STATUS_COMPLETE.equals(workStatus)) {
            appReq.addSuccessMessage("Action completed.");
        } else if (WORK_STATUS_DELETE.equals(workStatus)) {
            appReq.addInfoMessage("Action cancelled.");
        } else if (WORK_STATUS_BLOCKED.equals(workStatus)) {
            appReq.addInfoMessage("Action blocked and follow-up saved.");
        }
    }

    private ProjectActionNext resolveActionToWork(AppReq appReq, Session dataSession) {
        ProjectActionNext actionToWork = appReq.getCompletingAction();
        String actionIdString = appReq.getRequest().getParameter(PARAM_COMPLETING_ACTION_NEXT_ID);
        if (actionIdString != null && actionIdString.trim().length() > 0) {
            try {
                int actionId = Integer.parseInt(actionIdString);
                actionToWork = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionId);
            } catch (NumberFormatException nfe) {
                // Keep existing session action if request id is invalid.
            }
        }
        return actionToWork;
    }

    private void updateSummaryFromRequest(AppReq appReq, Session dataSession, ProjectActionNext actionToWork) {
        String nextSummary = appReq.getRequest().getParameter(PARAM_NEXT_SUMMARY);
        if (nextSummary == null) {
            return;
        }
        if (nextSummary.equals(n(actionToWork.getNextSummary()))) {
            return;
        }
        actionToWork.setNextSummary(nextSummary);
        actionToWork.setNextChangeDate(new Date());
        Transaction tx = dataSession.beginTransaction();
        dataSession.update(actionToWork);
        tx.commit();
    }

    private ProjectActionNext createFollowUpActionFromSentence(WebUser webUser, Session dataSession,
            ProjectActionNext sourceAction, String sentenceInput) {
        List<Project> projectList = loadProjectList(webUser, dataSession);
        Project defaultProject = sourceAction == null ? null : sourceAction.getProject();
        return actionSentenceImportService.saveNewActionFromSentence(webUser, dataSession,
                defaultProject, projectList, sentenceInput);
    }

    private ProjectActionNext closeAction(AppReq appReq, ProjectActionNext projectAction, Project project,
            String nextDescription, ProjectNextActionStatus nextActionStatus) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        ProjectActionNext unblockedAction = null;
        Transaction trans = dataSession.beginTransaction();
        if (nextDescription != null && !nextDescription.trim().isEmpty()) {
            ProjectActionTaken actionTaken = new ProjectActionTaken();
            actionTaken.setProject(project);
            actionTaken.setProjectId(project.getProjectId());
            actionTaken.setActionDate(new Date());
            actionTaken.setActionDescription(nextDescription);
            actionTaken.setProvider(webUser.getProvider());
            actionTaken.setContact(webUser.getProjectContact());
            actionTaken.setContactId(webUser.getContactId());
            dataSession.saveOrUpdate(actionTaken);
        }
        projectAction.setNextActionStatus(nextActionStatus);
        projectAction.setCompletionOrder(0);
        projectAction.setNextChangeDate(new Date());
        dataSession.update(projectAction);
        if (nextActionStatus == ProjectNextActionStatus.COMPLETED
                || nextActionStatus == ProjectNextActionStatus.CANCELLED) {
            unblockedAction = ProjectActionBlockerManager.unblockActionsBlockedBy(dataSession, webUser, projectAction);
        }
        trans.commit();
        return unblockedAction;
    }

    private ProjectActionNext selectNextActionForWorkFlow(WebUser webUser, Session dataSession,
            int excludeActionNextId) {
        List<ProjectActionNext> dueTodayList = getProjectActionListForToday(webUser, dataSession, 0);
        List<ProjectActionNext> overdueList = getProjectActionListForToday(webUser, dataSession, -1);
        dueTodayList = filterActionsForDashboardVisibility(dueTodayList, false);
        overdueList = filterActionsForDashboardVisibility(overdueList, true);
        if (excludeActionNextId > 0) {
            dueTodayList.removeIf(pa -> pa.getActionNextId() == excludeActionNextId);
            overdueList.removeIf(pa -> pa.getActionNextId() == excludeActionNextId);
        }

        List<ProjectActionNext> orderedList = new ArrayList<ProjectActionNext>();
        orderedList.addAll(overdueList);
        orderedList.addAll(dueTodayList);
        if (orderedList.isEmpty()) {
            return null;
        }
        sortProjectActionListByCompletionOrder(orderedList, webUser);
        return orderedList.get(0);
    }

    private List<ProjectActionNext> getProjectActionListForToday(WebUser webUser, Session dataSession, int dayOffset) {
        LocalDate today = webUser.getLocalDateToday();
        Query query;
        if (dayOffset < 0) {
            LocalDate oldestDate = today.minusYears(1);
            query = dataSession.createQuery(
                    "select distinct pan from ProjectActionNext pan "
                            + "left join fetch pan.project "
                            + "left join fetch pan.contact "
                            + "left join fetch pan.nextProjectContact "
                            + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                            + "and pan.nextDescription <> '' "
                            + "and pan.nextActionStatusString = :nextActionStatus "
                            + "and pan.nextActionDate >= :oldestDate and pan.nextActionDate < :cutoffDate "
                            + "order by pan.nextActionDate, pan.priorityLevel DESC, pan.nextTimeEstimate, pan.nextChangeDate");
            query.setParameter("oldestDate", java.sql.Date.valueOf(oldestDate));
            query.setParameter("cutoffDate", java.sql.Date.valueOf(today));
        } else {
            LocalDate targetDate = today.plusDays(dayOffset);
            query = dataSession.createQuery(
                    "select distinct pan from ProjectActionNext pan "
                            + "left join fetch pan.project "
                            + "left join fetch pan.contact "
                            + "left join fetch pan.nextProjectContact "
                            + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                            + "and pan.nextDescription <> '' "
                            + "and pan.nextActionStatusString = :nextActionStatus "
                            + "and pan.nextActionDate = :targetDate "
                            + "order by pan.nextActionDate, pan.priorityLevel DESC, pan.nextTimeEstimate, pan.nextChangeDate");
            query.setParameter("targetDate", java.sql.Date.valueOf(targetDate));
        }
        query.setParameter("provider", webUser.getProvider());
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> projectActionList = query.list();
        sortProjectActionList(projectActionList, webUser);
        return projectActionList;
    }

    private List<ProjectActionNext> getProjectActionListForTodayOrEarlier(WebUser webUser, Session dataSession) {
        LocalDate tomorrow = webUser.getLocalDateToday().plusDays(1);
        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan "
                        + "left join fetch pan.project "
                        + "left join fetch pan.contact "
                        + "left join fetch pan.nextProjectContact "
                        + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                        + "and pan.nextDescription <> '' "
                        + "and pan.nextActionStatusString = :nextActionStatus "
                        + "and pan.nextActionDate is not null and pan.nextActionDate < :tomorrow ");
        query.setParameter("provider", webUser.getProvider());
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
        query.setParameter("tomorrow", java.sql.Date.valueOf(tomorrow));
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> projectActionList = query.list();
        return projectActionList;
    }

    private void rationalizeCompletionOrderForCurrentDate(WebUser webUser, Session dataSession) {
        List<ProjectActionNext> candidateList = getProjectActionListForTodayOrEarlier(webUser, dataSession);
        if (candidateList.isEmpty()) {
            return;
        }

        boolean needsRationalization = false;
        for (ProjectActionNext projectAction : candidateList) {
            if (projectAction.getCompletionOrder() <= 0) {
                needsRationalization = true;
                break;
            }
        }
        if (!needsRationalization) {
            return;
        }

        Map<Integer, List<ProjectActionNext>> existingByBucket = new HashMap<Integer, List<ProjectActionNext>>();
        Map<Integer, List<ProjectActionNext>> newByBucket = new HashMap<Integer, List<ProjectActionNext>>();
        for (ProjectActionNext projectAction : candidateList) {
            int bucket = getCompletionBucket(projectAction, webUser);
            if (projectAction.getCompletionOrder() > 0) {
                existingByBucket.computeIfAbsent(bucket, k -> new ArrayList<ProjectActionNext>()).add(projectAction);
            } else {
                newByBucket.computeIfAbsent(bucket, k -> new ArrayList<ProjectActionNext>()).add(projectAction);
            }
        }

        List<ProjectActionNext> orderedList = new ArrayList<ProjectActionNext>();
        for (int bucket = BUCKET_START_OF_WORK_DAY; bucket <= BUCKET_OTHER; bucket++) {
            List<ProjectActionNext> existing = existingByBucket.get(bucket);
            if (existing != null && !existing.isEmpty()) {
                existing.sort((pa1, pa2) -> pa1.getCompletionOrder() - pa2.getCompletionOrder());
                orderedList.addAll(existing);
            }
            List<ProjectActionNext> pending = newByBucket.get(bucket);
            if (pending != null && !pending.isEmpty()) {
                pending.sort((pa1, pa2) -> compareInsideBucket(pa1, pa2));
                orderedList.addAll(pending);
            }
        }

        Transaction trans = dataSession.beginTransaction();
        int completionOrder = 1;
        for (ProjectActionNext projectAction : orderedList) {
            if (projectAction.getCompletionOrder() != completionOrder) {
                projectAction.setCompletionOrder(completionOrder);
                projectAction.setNextChangeDate(new Date());
                dataSession.update(projectAction);
            }
            completionOrder++;
        }
        trans.commit();
    }

    private List<Project> loadProjectList(WebUser webUser, Session dataSession) {
        Query query = dataSession
                .createQuery("from Project where provider = ? and phaseCode <> 'Clos' order by projectName");
        query.setParameter(0, webUser.getProvider());
        @SuppressWarnings("unchecked")
        List<Project> projectList = query.list();
        return projectList;
    }

    private List<ProjectActionNext> filterActionsForDashboardVisibility(List<ProjectActionNext> actions,
            boolean workOnly) {
        List<ProjectActionNext> filtered = new ArrayList<ProjectActionNext>();
        for (ProjectActionNext action : actions) {
            if (action == null) {
                continue;
            }
            if (action.isBillable()) {
                filtered.add(action);
                continue;
            }
            if (workOnly) {
                continue;
            }
            if (action.getTimeSlot() == TimeSlot.MORNING) {
                filtered.add(action);
            }
        }
        return filtered;
    }

    private static void sortProjectActionList(List<ProjectActionNext> projectActionList, WebUser webUser) {
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

    private static void sortProjectActionListByCompletionOrder(List<ProjectActionNext> projectActionList,
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

    private static int getCompletionBucket(ProjectActionNext projectAction, WebUser webUser) {
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

    private String n(String value) {
        if (value == null || value.trim().equals("")) {
            return "";
        }
        return value;
    }

    private String normalizeWorkStatus(String workStatus) {
        String value = n(workStatus).trim();
        if (value.length() == 0) {
            return "";
        }
        String upper = value.toUpperCase();
        if ("INPROGRESS".equals(upper) || "IN_PROGRESS".equals(upper)) {
            return WORK_STATUS_IN_PROGRESS;
        }
        if ("COMPLETE".equals(upper)) {
            return WORK_STATUS_COMPLETE;
        }
        if ("DELETE".equals(upper)) {
            return WORK_STATUS_DELETE;
        }
        if ("BLOCKED".equals(upper)) {
            return WORK_STATUS_BLOCKED;
        }
        return upper;
    }
}
