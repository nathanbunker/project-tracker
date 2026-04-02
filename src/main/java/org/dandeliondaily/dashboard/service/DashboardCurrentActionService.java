package org.dandeliondaily.dashboard.service;

import java.util.ArrayList;
import java.util.Calendar;
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
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.ProcessStage;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class DashboardCurrentActionService {

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
        UrlExtractionResult urlResult = extractAndRemoveUrl(sentenceInput);
        String extractedUrl = urlResult.extractedUrl;
        sentenceInput = urlResult.cleanedText;

        List<Project> projectList = loadProjectList(webUser, dataSession);

        String projectName = "";
        String actionPart = sentenceInput;
        String[] parts = sentenceInput.split(":", 2);
        if (parts.length == 2) {
            projectName = parts[0].trim();
            actionPart = parts[1].trim();
        }

        Project foundProject = null;
        for (Project project : projectList) {
            if (project.getProjectName().equalsIgnoreCase(projectName)) {
                foundProject = project;
                break;
            }
        }
        if (foundProject == null) {
            if (sourceAction == null || sourceAction.getProject() == null) {
                return null;
            }
            foundProject = sourceAction.getProject();
            actionPart = projectName + " " + actionPart;
        }

        String actionVerb = "I will";
        String actionToTake = actionPart;
        String whenToTakeAction = "";
        int nextTimeEstimate = 20;
        if (actionPart.startsWith("I will meet ")) {
            actionVerb = "I will meet";
            actionToTake = actionPart.substring("I will meet ".length()).trim();
            nextTimeEstimate = 60;
        } else if (actionPart.startsWith("I will ")) {
            actionVerb = "I will";
            actionToTake = actionPart.substring("I will ".length()).trim();
        } else if (actionPart.startsWith("I might ")) {
            actionVerb = "I might";
            actionToTake = actionPart.substring("I might ".length()).trim();
        } else if (actionPart.startsWith("I have committed ")) {
            actionVerb = "I have committed";
            actionToTake = actionPart.substring("I have committed ".length()).trim();
        } else if (actionPart.startsWith("I have set goal to")) {
            actionVerb = "I have set goal to";
            actionToTake = actionPart.substring("I have set goal to".length()).trim();
        } else if (actionPart.startsWith("I am waiting ") || actionPart.equals("I am waiting")
                || actionPart.startsWith("I am waiting:")) {
            actionVerb = "I am waiting";
            actionToTake = actionPart.substring("I am waiting".length()).trim();
            if (actionToTake.startsWith(":")) {
                actionToTake = actionToTake.substring(1).trim();
            }
            nextTimeEstimate = 5;
        }

        String[] tokens = actionToTake.trim().split("\\s+");
        if (tokens.length >= 1) {
            String lastToken = tokens[tokens.length - 1];
            String secondLastToken = tokens.length >= 2 ? tokens[tokens.length - 2] : "";
            if (tokens.length > 3) {
                String thirdLastToken = tokens.length >= 3 ? tokens[tokens.length - 3] : "";
                if (thirdLastToken.equals("for") && isNumeric(secondLastToken)) {
                    try {
                        nextTimeEstimate = Integer.parseInt(secondLastToken);
                    } catch (NumberFormatException e) {
                        nextTimeEstimate = 20;
                    }
                    if (lastToken.equals("hours") || lastToken.equals("hour")) {
                        nextTimeEstimate *= 60;
                    }
                    actionToTake = String.join(" ", java.util.Arrays.copyOf(tokens, tokens.length - 3)).trim();
                    tokens = actionToTake.trim().split("\\s+");
                    lastToken = tokens.length >= 1 ? tokens[tokens.length - 1] : "";
                    secondLastToken = tokens.length >= 2 ? tokens[tokens.length - 2] : "";
                }
            }

            boolean foundDate = false;
            if (lastToken.chars().filter(ch -> ch == '/').count() == 2) {
                whenToTakeAction = lastToken;
                foundDate = true;
            } else {
                String lower = lastToken.toLowerCase();
                if (lower.equals("today") || lower.equals("tomorrow")) {
                    whenToTakeAction = lastToken;
                    foundDate = true;
                } else {
                    String[] days = { "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" };
                    for (String day : days) {
                        if (lower.equals(day)) {
                            if (secondLastToken.equalsIgnoreCase("next")) {
                                whenToTakeAction = "next " + lastToken;
                                actionToTake = String.join(" ", java.util.Arrays.copyOf(tokens, tokens.length - 2))
                                        .trim();
                            } else {
                                whenToTakeAction = lastToken;
                                actionToTake = String.join(" ", java.util.Arrays.copyOf(tokens, tokens.length - 1))
                                        .trim();
                            }
                            foundDate = true;
                            break;
                        }
                    }
                }
            }
            if (foundDate && whenToTakeAction != null && actionToTake.endsWith(whenToTakeAction)) {
                actionToTake = actionToTake.substring(0, actionToTake.length() - whenToTakeAction.length()).trim();
            }
        }

        ProjectActionNext nextAction = new ProjectActionNext();
        nextAction.setProject(foundProject);
        nextAction.setProjectId(foundProject.getProjectId());
        nextAction.setContactId(webUser.getContactId());
        Date actionDate = parseWhenToTakeAction(webUser, whenToTakeAction);
        if (actionVerb.equals("I will")) {
            nextAction.setNextActionType(ProjectNextActionType.WILL);
        } else if (actionVerb.equals("I might")) {
            nextAction.setNextActionType(ProjectNextActionType.MIGHT);
        } else if (actionVerb.equals("I have committed")) {
            nextAction.setNextActionType(ProjectNextActionType.COMMITTED_TO);
        } else if (actionVerb.equals("I will meet")) {
            nextAction.setNextActionType(ProjectNextActionType.WILL_MEET);
        } else if (actionVerb.equals("I have set goal to")) {
            nextAction.setNextActionType(ProjectNextActionType.GOAL);
        } else if (actionVerb.equals("I am waiting")) {
            nextAction.setNextActionType(ProjectNextActionType.WAITING);
        } else {
            nextAction.setNextActionType(ProjectNextActionType.WILL);
        }
        nextAction.setNextActionDate(actionDate);
        nextAction.setNextDescription(actionToTake);
        nextAction.setNextTimeEstimate(nextTimeEstimate);
        nextAction.setNextChangeDate(new Date());
        nextAction.setProvider(webUser.getProvider());
        nextAction.setContact(webUser.getProjectContact());
        nextAction.setBillable(resolveBillable(dataSession, foundProject));
        if (extractedUrl != null && extractedUrl.length() > 0) {
            nextAction.setLinkUrl(extractedUrl);
        }
        defaultPersonalTimeSlot(nextAction);
        if (nextAction.getNextActionStatus() == null) {
            if (nextAction.hasNextDescription()) {
                if (nextAction.hasNextActionDate()) {
                    nextAction.setNextActionStatus(ProjectNextActionStatus.READY);
                } else {
                    nextAction.setNextActionStatus(ProjectNextActionStatus.PROPOSED);
                }
            }
        }

        Transaction trans = dataSession.beginTransaction();
        dataSession.saveOrUpdate(nextAction);
        trans.commit();
        return nextAction;
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
        sortProjectActionListByCompletionOrder(orderedList);
        return orderedList.get(0);
    }

    private List<ProjectActionNext> getProjectActionListForToday(WebUser webUser, Session dataSession, int dayOffset) {
        Date today = TimeTracker.createToday(webUser).getTime();
        Date tomorrow = TimeTracker.createTomorrow(webUser).getTime();
        if (dayOffset > 0) {
            Calendar calendar = webUser.getCalendar(today);
            calendar.add(Calendar.DAY_OF_MONTH, dayOffset);
            today = calendar.getTime();
            calendar.setTime(tomorrow);
            calendar.add(Calendar.DAY_OF_MONTH, dayOffset);
            tomorrow = calendar.getTime();
        } else if (dayOffset < 0) {
            Calendar calendar = webUser.getCalendar(today);
            calendar.add(Calendar.YEAR, -1);
            today = calendar.getTime();
            calendar.setTime(tomorrow);
            calendar.add(Calendar.DAY_OF_MONTH, dayOffset);
            tomorrow = calendar.getTime();
        }

        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan "
                        + "left join fetch pan.project "
                        + "left join fetch pan.contact "
                        + "left join fetch pan.nextProjectContact "
                        + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                        + "and pan.nextDescription <> '' "
                        + "and pan.nextActionStatusString = :nextActionStatus "
                        + "and pan.nextActionDate >= :today and pan.nextActionDate < :tomorrow "
                        + "order by pan.nextActionDate, pan.priorityLevel DESC, pan.nextTimeEstimate, pan.nextChangeDate");
        query.setParameter("provider", webUser.getProvider());
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
        query.setParameter("today", today);
        query.setParameter("tomorrow", tomorrow);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> projectActionList = query.list();
        sortProjectActionList(projectActionList);
        return projectActionList;
    }

    private List<ProjectActionNext> getProjectActionListForTodayOrEarlier(WebUser webUser, Session dataSession) {
        Date tomorrow = TimeTracker.createTomorrow(webUser).getTime();
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
        query.setParameter("tomorrow", tomorrow);
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
            int bucket = getCompletionBucket(projectAction);
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

    private boolean resolveBillable(Session dataSession, Project project) {
        if (project == null || project.getBillCode() == null || project.getBillCode().equals("")) {
            return false;
        }
        BillCode billCode = ClientServlet.resolveBillCode(dataSession, project);
        return billCode != null && "Y".equalsIgnoreCase(billCode.getBillable());
    }

    private Date parseWhenToTakeAction(WebUser webUser, String whenToTakeAction) {
        Date actionDate = webUser.getCalendar().getTime();
        if (whenToTakeAction == null || whenToTakeAction.length() == 0) {
            return actionDate;
        }

        Calendar calendar = webUser.getCalendar();
        String lower = whenToTakeAction.trim().toLowerCase();
        String[] days = { "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" };
        boolean isNext = lower.startsWith("next ");
        String dayName = isNext ? lower.substring(5).trim() : lower;
        int dayOfWeek = -1;
        for (int i = 0; i < days.length; i++) {
            if (days[i].equals(dayName)) {
                dayOfWeek = i + 1;
                break;
            }
        }
        if (lower.equals("today")) {
            // keep today
        } else if (lower.equals("tomorrow")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        } else if (dayOfWeek != -1) {
            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int daysUntil = dayOfWeek - currentDayOfWeek;
            if (isNext) {
                if (daysUntil <= 0) {
                    daysUntil += 7;
                }
                daysUntil += 7;
            } else if (daysUntil < 0) {
                daysUntil += 7;
            }
            calendar.add(Calendar.DAY_OF_YEAR, daysUntil);
        } else {
            try {
                Date parsedDate = webUser.getDateFormat().parse(whenToTakeAction);
                calendar.setTime(parsedDate);
            } catch (Exception e) {
                // leave current date as fallback
            }
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        actionDate = calendar.getTime();
        return actionDate;
    }

    private void defaultPersonalTimeSlot(ProjectActionNext projectAction) {
        if (projectAction != null && !projectAction.isBillable() && projectAction.getTimeSlot() == null) {
            projectAction.setTimeSlot(TimeSlot.AFTERNOON);
        }
    }

    private UrlExtractionResult extractAndRemoveUrl(String text) {
        if (text == null || text.isEmpty()) {
            return new UrlExtractionResult(text, null);
        }
        int urlStartIndex = text.indexOf("https://");
        if (urlStartIndex == -1) {
            return new UrlExtractionResult(text, null);
        }
        int urlEndIndex = text.indexOf(' ', urlStartIndex);
        if (urlEndIndex == -1) {
            urlEndIndex = text.length();
        }
        String extractedUrl = text.substring(urlStartIndex, urlEndIndex);
        String cleanedText = text.substring(0, urlStartIndex) + text.substring(urlEndIndex);
        cleanedText = cleanedText.trim().replaceAll("\\s+", " ");
        return new UrlExtractionResult(cleanedText, extractedUrl);
    }

    private static class UrlExtractionResult {
        private final String cleanedText;
        private final String extractedUrl;

        private UrlExtractionResult(String cleanedText, String extractedUrl) {
            this.cleanedText = cleanedText;
            this.extractedUrl = extractedUrl;
        }
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private static void sortProjectActionList(List<ProjectActionNext> projectActionList) {
        Collections.sort(projectActionList, (pa1, pa2) -> {
            int c1 = pa1.getCompletionOrder();
            int c2 = pa2.getCompletionOrder();
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

    private static void sortProjectActionListByCompletionOrder(List<ProjectActionNext> projectActionList) {
        Collections.sort(projectActionList, (pa1, pa2) -> {
            int c1 = pa1.getCompletionOrder();
            int c2 = pa2.getCompletionOrder();
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
        Date actionDate = projectAction.getNextActionDate();
        if (actionDate != null) {
            Calendar actionCal = Calendar.getInstance();
            actionCal.setTime(actionDate);
            setMidnight(actionCal);
            Calendar todayCal = Calendar.getInstance();
            setMidnight(todayCal);
            if (actionCal.before(todayCal)) {
                return BUCKET_OVERDUE;
            }
        }
        if (!projectAction.isBillable()) {
            TimeSlot timeSlot = projectAction.getTimeSlot();
            if (timeSlot == TimeSlot.WAKE) {
                return BUCKET_PERSONAL_WAKE;
            }
            if (timeSlot == TimeSlot.AFTERNOON || timeSlot == TimeSlot.EVENING || timeSlot == null) {
                return BUCKET_PERSONAL_LATE;
            }
            if (timeSlot == TimeSlot.MORNING) {
                return BUCKET_PERSONAL_MORNING;
            }
            return BUCKET_PERSONAL_LATE;
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

    private static void setMidnight(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
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
