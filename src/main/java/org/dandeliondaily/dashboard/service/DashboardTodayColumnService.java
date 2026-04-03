package org.dandeliondaily.dashboard.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.dashboard.model.DashboardTodayColumnModel;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeAdder;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.ProcessStage;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class DashboardTodayColumnService {

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_SENTENCE_INPUT = "sentenceInput";
    private static final String ACTION_SCHEDULE = "Schedule";
    private static final String ACTION_SCHEDULE_AND_START = "Schedule and Start";

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
        ProjectActionNext selectedAction = appReq.getCompletingAction();

        List<Project> projectList = loadProjectList(webUser, dataSession);
        ProjectActionNext nextAction = saveNewActionFromSentence(webUser, dataSession, selectedAction, projectList,
                sentenceInput);
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
        List<Project> quickCaptureProjects = loadProjectList(webUser, dataSession);
        List<String> projectNames = new ArrayList<String>();
        for (Project project : quickCaptureProjects) {
            if (project != null && project.getProjectName() != null) {
                projectNames.add(project.getProjectName());
            }
        }
        model.getQuickCapture().setProjectNames(projectNames);

        // Real data wiring starts here for the middle Today column.
        List<ProjectActionNext> dueTodayList = getProjectActionListForToday(webUser, dataSession, 0);
        List<ProjectActionNext> overdueList = getProjectActionListForToday(webUser, dataSession, -1);
        sortProjectActionListByCompletionOrder(dueTodayList);
        sortProjectActionListByCompletionOrder(overdueList);

        model.setActionGroups(buildTodayGroups(webUser, dueTodayList, overdueList));

        List<ProjectActionNext> completedToday = getProjectActionListClosedToday(webUser, dataSession);
        model.setCompletedToday(toActionItems(webUser, completedToday, "Completed"));

        List<ProjectActionNext> todayAndOverdue = new ArrayList<ProjectActionNext>();
        todayAndOverdue.addAll(overdueList);
        todayAndOverdue.addAll(dueTodayList);
        model.setTotals(buildTotals(appReq, webUser, todayAndOverdue));

        return model;
    }

    private DashboardTodayColumnModel.TodayTotalsModel buildTotals(AppReq appReq, WebUser webUser,
            List<ProjectActionNext> todayAndOverdue) {
        DashboardTodayColumnModel.TodayTotalsModel totals = new DashboardTodayColumnModel.TodayTotalsModel();

        TimeAdder timeAdderToday = new TimeAdder(todayAndOverdue, appReq);
        TimeAdder timeAdderScheduled = new TimeAdder(todayAndOverdue, appReq, webUser.getToday());

        int completedAct = timeAdderToday.getCompletedAct();
        int committedEst = timeAdderScheduled.getCommittedEst();
        int willEst = timeAdderScheduled.getWillEst();
        int willMeetEst = timeAdderScheduled.getWillMeetEst();
        int committedWillTotal = committedEst + willEst + willMeetEst;

        totals.setCompletedDisplay(ProjectActionNext.getTimeForDisplay(completedAct));
        totals.setCommittedDisplay(ProjectActionNext.getTimeForDisplay(committedEst));
        totals.setWillDisplay(ProjectActionNext.getTimeForDisplay(willEst));
        totals.setWillMeetDisplay(ProjectActionNext.getTimeForDisplay(willMeetEst));
        totals.setTotalPlannedDisplay(ProjectActionNext.getTimeForDisplay(committedWillTotal));
        totals.setCompletedMinutes(completedAct);
        totals.setPlannedMinutes(committedWillTotal);

        if (committedWillTotal == 0) {
            totals.setGuidanceMessage("You have finished everything you said you would do today.");
            totals.setOverCommitted(false);
        } else if (completedAct > (8 * 60)) {
            totals.setGuidanceMessage("You have already logged more than a full workday.");
            totals.setOverCommitted(true);
        } else if (completedAct + committedWillTotal > (8 * 60)) {
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
            List<ProjectActionNext> dueTodayList, List<ProjectActionNext> overdueList) {
        Map<Integer, List<ProjectActionNext>> bucketMap = new HashMap<Integer, List<ProjectActionNext>>();
        for (int bucket = BUCKET_START_OF_WORK_DAY; bucket <= BUCKET_OTHER; bucket++) {
            bucketMap.put(bucket, new ArrayList<ProjectActionNext>());
        }

        bucketMap.get(BUCKET_OVERDUE).addAll(overdueList);
        for (ProjectActionNext projectAction : dueTodayList) {
            int bucket = getCompletionBucket(projectAction);
            bucketMap.get(bucket).add(projectAction);
        }

        for (int bucket = BUCKET_START_OF_WORK_DAY; bucket <= BUCKET_OTHER; bucket++) {
            sortProjectActionListByCompletionOrder(bucketMap.get(bucket));
        }

        List<DashboardTodayColumnModel.TodayActionGroupModel> groups = new ArrayList<DashboardTodayColumnModel.TodayActionGroupModel>();
        addGroup(groups, "Overdue", toActionItems(webUser, bucketMap.get(BUCKET_OVERDUE), "Overdue"));
        addGroup(groups, "Start of Work Day",
                toActionItems(webUser, bucketMap.get(BUCKET_START_OF_WORK_DAY), "Start of Work Day"));
        addGroup(groups, "Personal (Wake)",
                toActionItems(webUser, bucketMap.get(BUCKET_PERSONAL_WAKE), TimeSlot.WAKE.getLabel()));
        addGroup(groups, "Committed", toActionItems(webUser, bucketMap.get(BUCKET_COMMITTED), "Committed"));
        addGroup(groups, "Will", toActionItems(webUser, bucketMap.get(BUCKET_WILL), "Will"));
        addGroup(groups, "Personal (Morning)",
                toActionItems(webUser, bucketMap.get(BUCKET_PERSONAL_MORNING), TimeSlot.MORNING.getLabel()));
        addGroup(groups, "Might", toActionItems(webUser, bucketMap.get(BUCKET_MIGHT), "Might"));
        addGroup(groups, "Waiting", toActionItems(webUser, bucketMap.get(BUCKET_WAITING), "Waiting"));
        addGroup(groups, "Will Meet", toActionItems(webUser, bucketMap.get(BUCKET_WILL_MEET), "Will Meet"));
        addGroup(groups, "End of Work Day",
                toActionItems(webUser, bucketMap.get(BUCKET_END_OF_WORK_DAY), "End of Work Day"));
        addGroup(groups, "Personal (Afternoon & Evening)",
                toActionItems(webUser, bucketMap.get(BUCKET_PERSONAL_LATE), "Personal"));
        addGroup(groups, "Other", toActionItems(webUser, bucketMap.get(BUCKET_OTHER), "Other"));
        return groups;
    }

    private void addGroup(List<DashboardTodayColumnModel.TodayActionGroupModel> groups, String title,
            List<DashboardTodayColumnModel.TodayActionItemModel> items) {
        if (items.isEmpty()) {
            return;
        }
        groups.add(new DashboardTodayColumnModel.TodayActionGroupModel(title, items));
    }

    private List<DashboardTodayColumnModel.TodayActionItemModel> toActionItems(WebUser webUser,
            List<ProjectActionNext> actions, String contextLabel) {
        List<DashboardTodayColumnModel.TodayActionItemModel> items = new ArrayList<DashboardTodayColumnModel.TodayActionItemModel>();
        for (ProjectActionNext action : actions) {
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

    private List<ProjectActionNext> getProjectActionListClosedToday(WebUser webUser, Session dataSession) {
        Date today = TimeTracker.createToday(webUser).getTime();
        Date tomorrow = TimeTracker.createTomorrow(webUser).getTime();
        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan "
                        + "left join fetch pan.project "
                        + "left join fetch pan.contact "
                        + "left join fetch pan.nextProjectContact "
                        + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                        + "and pan.nextActionStatusString = :nextActionStatus and pan.nextDescription <> '' "
                        + "and pan.nextChangeDate >= :today and pan.nextChangeDate < :tomorrow "
                        + "order by pan.nextTimeActual DESC, pan.nextTimeEstimate DESC");
        query.setParameter("provider", webUser.getProvider());
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("nextActionStatus", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("today", today);
        query.setParameter("tomorrow", tomorrow);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> projectActionList = query.list();
        return projectActionList;
    }

    private List<Project> loadProjectList(WebUser webUser, Session dataSession) {
        Query query = dataSession
                .createQuery("from Project where provider = ? and phaseCode <> 'Clos' order by projectName");
        query.setParameter(0, webUser.getProvider());
        @SuppressWarnings("unchecked")
        List<Project> projectList = query.list();
        return projectList;
    }

    private ProjectActionNext saveNewActionFromSentence(WebUser webUser, Session dataSession,
            ProjectActionNext selectedAction, List<Project> projectList, String sentenceInput) {
        UrlExtractionResult urlResult = extractAndRemoveUrl(sentenceInput);
        String extractedUrl = urlResult.extractedUrl;
        sentenceInput = urlResult.cleanedText;

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
            if (selectedAction == null || selectedAction.getProject() == null) {
                return null;
            }
            foundProject = selectedAction.getProject();
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

    private String resolveStatusLabel(ProjectActionNext projectAction) {
        if (projectAction.getNextActionStatus() != null) {
            return projectAction.getNextActionStatus().getLabel();
        }
        return "Unknown";
    }

    private String displayTime(String value) {
        return n(value, "-");
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

    private String n(String value, String defaultValue) {
        if (value == null || value.trim().equals("")) {
            return defaultValue;
        }
        return value;
    }
}