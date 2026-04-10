package org.dandeliondaily.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.dandeliondaily.dashboard.service.DashboardCurrentActionService;
import org.dandeliondaily.dashboard.service.DashboardTodayColumnService;
import org.dandeliondaily.focus.render.FocusedActionPageRenderer;
import org.dandeliondaily.planahead.service.PlanAheadDayCapacityService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeEntry;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class FocusedActionServlet extends ClientServlet {

    private static final long serialVersionUID = -1832669295224425277L;
    private static final String ACTION_WORK_NEXT = "WorkNext";
    private static final String ACTION_COMPLETE = "COMPLETE";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_WORK_STATUS = "workStatus";
    private static final String ACTION_SCHEDULE = "Schedule";
    private static final String PARAM_COMPLETING_ACTION_NEXT_ID = "completingActionNextId";
    private static final String SESSION_PRE_MEETING_ACTION_ID = "FOCUS_PRE_MEETING_ACTION_ID";
    private static final String SESSION_MEETING_ACTIVE = "FOCUS_MEETING_ACTIVE";
    private static final String SESSION_RECENT_FOCUSED_ACTION_IDS = "FOCUS_RECENT_ACTION_IDS";

    private final DashboardCurrentActionService dashboardCurrentActionService = new DashboardCurrentActionService();
    private final DashboardTodayColumnService dashboardTodayColumnService = new DashboardTodayColumnService();
    private final FocusedActionPageRenderer focusedActionPageRenderer = new FocusedActionPageRenderer();
    private final PlanAheadDayCapacityService dayCapacityService = new PlanAheadDayCapacityService();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            String action = request.getParameter(PARAM_ACTION);
            if ("addCurrentActionNote".equals(action)) {
                handleAddCurrentActionNote(appReq);
                return;
            }
            if ("refreshFocusClock".equals(action)) {
                handleRefreshFocusClock(appReq);
                return;
            }
            if ("toggleTimer".equals(action)) {
                handleToggleTimer(appReq);
                return;
            }
            if ("selectMeetingAction".equals(action)) {
                handleSelectMeetingAction(appReq);
                return;
            }

            selectActionFromRequest(appReq);
            dashboardTodayColumnService.handleQuickCapture(appReq);
            dashboardCurrentActionService.ensureCurrentActionSelected(appReq);

            ActionNext workedAction = appReq.getCompletingAction();
            dashboardCurrentActionService.handleCurrentActionWork(appReq);
            maybeRestorePreMeetingActionAfterCompletion(appReq, action, workedAction);
            dashboardCurrentActionService.ensureCurrentActionSelected(appReq);

            // After completing an action, if no current action is selected, redirect to
            // dashboard
            ActionNext currentAction = reloadCurrentAction(appReq);
            trackFocusedActionSelection(appReq, currentAction);
            if (currentAction == null && ACTION_WORK_NEXT.equals(action)
                    && ACTION_COMPLETE.equalsIgnoreCase(n(appReq.getRequest().getParameter(PARAM_WORK_STATUS)))) {
                appReq.getResponse().sendRedirect("FocusedActionServlet");
                return;
            }
            int spentMinutes = loadCurrentActionMinutesToday(appReq, currentAction);
            int todayBillableMinutes = loadBillableMinutesToday(appReq);
            int spentMinutesThisWeek = loadBillableMinutesThisWeekRounded(appReq);
            int todayTargetMinutes = dayCapacityService.loadTargetMinutesForDay(appReq,
                    appReq.getWebUser().toDate(appReq.getWebUser().getLocalDateToday()));
            int weekTargetMinutes = dayCapacityService.loadTargetMinutesForCurrentWeek(appReq);
            int estimateMinutes = currentAction != null && currentAction.getNextTimeEstimate() != null
                    ? currentAction.getNextTimeEstimate().intValue()
                    : 0;

            List<String> notes = extractNoteLines(currentAction);
            List<FocusedActionPageRenderer.MeetingOption> meetingOptions = loadTodayMeetingOptions(appReq);
            List<FocusedActionPageRenderer.PreviousActionOption> previousActions = loadRecentFocusedActionOptions(
                    appReq,
                    currentAction == null ? 0 : currentAction.getActionNextId());
            boolean runningClock = appReq.getTimeTracker() != null && appReq.getTimeTracker().isRunningClock();
            int nowMinute = appReq.getWebUser().getLocalDateTimeNow().getMinute();
            ActionNext nextActionHintAction = resolveNextActionHintAction(appReq, currentAction);
            String nextActionHint = nextActionHintAction == null
                    ? "No next action available."
                    : buildActionLabel(nextActionHintAction);
            Integer nextActionHintId = nextActionHintAction == null
                    ? null
                    : Integer.valueOf(nextActionHintAction.getActionNextId());
            List<String> quickCaptureProjectNames = dashboardTodayColumnService.listQuickCaptureProjectNames(appReq);
            boolean quickCaptureFocusRequested = ACTION_SCHEDULE.equals(action);

            appReq.setTitle("Focused Action");
            printFocusedHead(appReq);
            focusedActionPageRenderer.render(appReq, currentAction, notes, meetingOptions, previousActions,
                    spentMinutes,
                    estimateMinutes, runningClock, nowMinute, spentMinutesThisWeek, todayBillableMinutes,
                    todayTargetMinutes, weekTargetMinutes,
                    nextActionHint, nextActionHintId,
                    n(appReq.getRequest().getParameter("sentenceInput")), quickCaptureProjectNames,
                    quickCaptureFocusRequested);
            printFocusedFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void selectActionFromRequest(AppReq appReq) {
        String actionIdString = appReq.getRequest().getParameter(PARAM_COMPLETING_ACTION_NEXT_ID);
        if (actionIdString == null || actionIdString.trim().length() == 0) {
            return;
        }
        try {
            int actionId = Integer.parseInt(actionIdString.trim());
            ActionNext selected = (ActionNext) appReq.getDataSession().get(ActionNext.class,
                    actionId);
            if (selected != null) {
                appReq.setCompletingAction(selected);
                if (selected.getProject() != null) {
                    appReq.setProject(selected.getProject());
                }
            }
        } catch (NumberFormatException nfe) {
            // ignore invalid action id
        }
    }

    private void maybeRestorePreMeetingActionAfterCompletion(AppReq appReq, String action,
            ActionNext workedAction) {
        String workStatus = n(appReq.getRequest().getParameter(PARAM_WORK_STATUS));
        if (!ACTION_WORK_NEXT.equals(action) || !ACTION_COMPLETE.equalsIgnoreCase(workStatus)) {
            return;
        }

        if (workedAction != null && !ProjectNextActionType.WILL_MEET.equals(workedAction.getNextActionType())) {
            clearMeetingMemory(appReq);
            return;
        }

        if (workedAction == null || !ProjectNextActionType.WILL_MEET.equals(workedAction.getNextActionType())) {
            return;
        }

        HttpSession session = appReq.getRequest().getSession(false);
        if (session == null) {
            return;
        }
        Object meetingActive = session.getAttribute(SESSION_MEETING_ACTIVE);
        Object preActionIdObj = session.getAttribute(SESSION_PRE_MEETING_ACTION_ID);
        session.removeAttribute(SESSION_MEETING_ACTIVE);
        session.removeAttribute(SESSION_PRE_MEETING_ACTION_ID);
        if (!(meetingActive instanceof Boolean) || !Boolean.TRUE.equals(meetingActive)
                || !(preActionIdObj instanceof Integer)) {
            return;
        }

        Integer preActionId = (Integer) preActionIdObj;
        ActionNext preAction = (ActionNext) appReq.getDataSession().get(ActionNext.class,
                preActionId);
        if (preAction == null || preAction.getNextActionStatus() != ProjectNextActionStatus.READY) {
            return;
        }
        appReq.setCompletingAction(preAction);
        if (preAction.getProject() != null) {
            appReq.setProject(preAction.getProject());
        }
    }

    private void handleSelectMeetingAction(AppReq appReq) throws Exception {
        String meetingActionNextId = appReq.getRequest().getParameter("meetingActionNextId");
        if (meetingActionNextId == null || meetingActionNextId.trim().length() == 0) {
            sendJsonResponse(appReq, false, "Meeting action is required", null);
            return;
        }
        int actionNextId;
        try {
            actionNextId = Integer.parseInt(meetingActionNextId.trim());
        } catch (NumberFormatException nfe) {
            sendJsonResponse(appReq, false, "Invalid meeting action", null);
            return;
        }

        Session dataSession = appReq.getDataSession();
        ActionNext selectedMeeting = (ActionNext) dataSession.get(ActionNext.class, actionNextId);
        if (selectedMeeting == null) {
            sendJsonResponse(appReq, false, "Meeting action not found", null);
            return;
        }

        ActionNext currentAction = appReq.getCompletingAction();
        HttpSession session = appReq.getRequest().getSession(true);
        Integer existingPreMeeting = session.getAttribute(SESSION_PRE_MEETING_ACTION_ID) instanceof Integer
                ? (Integer) session.getAttribute(SESSION_PRE_MEETING_ACTION_ID)
                : null;
        if (existingPreMeeting == null
                && currentAction != null
                && currentAction.getActionNextId() != selectedMeeting.getActionNextId()
                && !ProjectNextActionType.WILL_MEET.equals(currentAction.getNextActionType())
                && currentAction.getNextActionStatus() == ProjectNextActionStatus.READY) {
            session.setAttribute(SESSION_PRE_MEETING_ACTION_ID, Integer.valueOf(currentAction.getActionNextId()));
        }
        session.setAttribute(SESSION_MEETING_ACTIVE, Boolean.TRUE);

        appReq.setCompletingAction(selectedMeeting);
        if (selectedMeeting.getProject() != null) {
            appReq.setProject(selectedMeeting.getProject());
        }

        // Switch timer context to the selected meeting action and start timing it.
        TimeTracker timeTracker = appReq.getTimeTracker();
        if (timeTracker != null && selectedMeeting.getProject() != null) {
            if (timeTracker.isRunningClock()) {
                timeTracker.stopClock(appReq.getDataSession());
            }
            timeTracker.startClock(selectedMeeting.getProject(), selectedMeeting, appReq.getDataSession());
        }
        sendJsonResponse(appReq, true, "Meeting selected", null);
    }

    private void clearMeetingMemory(AppReq appReq) {
        HttpSession session = appReq.getRequest().getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(SESSION_MEETING_ACTIVE);
        session.removeAttribute(SESSION_PRE_MEETING_ACTION_ID);
    }

    private ActionNext resolveNextActionHintAction(AppReq appReq, ActionNext currentAction) {
        ActionNext hinted = null;
        HttpSession session = appReq.getRequest().getSession(false);
        if (session != null
                && currentAction != null
                && ProjectNextActionType.WILL_MEET.equals(currentAction.getNextActionType())) {
            Object preObj = session.getAttribute(SESSION_PRE_MEETING_ACTION_ID);
            if (preObj instanceof Integer) {
                hinted = (ActionNext) appReq.getDataSession().get(ActionNext.class, (Integer) preObj);
                if (hinted != null && hinted.getNextActionStatus() != ProjectNextActionStatus.READY) {
                    hinted = null;
                }
            }
        }

        if (hinted == null) {
            hinted = loadNextReadyAction(appReq, currentAction == null ? 0 : currentAction.getActionNextId());
        }
        return hinted;
    }

    private ActionNext loadNextReadyAction(AppReq appReq, int excludeActionId) {
        WebUser webUser = appReq.getWebUser();
        Integer workspaceId = appReq.getActiveWorkspaceId();
        LocalDate tomorrow = webUser.getLocalDateToday().plusDays(1);
        Query query = appReq.getDataSession().createQuery(
                "select distinct an from ActionNext an "
                        + "left join fetch an.project "
                        + "where an.workspaceId = :workspaceId and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                        + "and an.nextDescription <> '' "
                        + "and an.nextActionStatusString = :nextActionStatus "
                        + "and an.nextActionDate is not null and an.nextActionDate < :tomorrow "
                        + "order by an.nextActionDate, an.completionOrder, an.priorityLevel desc, an.nextChangeDate");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
        query.setParameter("tomorrow", java.sql.Date.valueOf(tomorrow));
        @SuppressWarnings("unchecked")
        List<ActionNext> list = query.list();
        for (ActionNext candidate : list) {
            if (candidate == null || candidate.getActionNextId() == excludeActionId) {
                continue;
            }
            if (candidate.isBillable() || candidate.getTimeSlot() == TimeSlot.MORNING) {
                return candidate;
            }
        }
        return null;
    }

    private String buildActionLabel(ActionNext action) {
        String projectName = action.getProject() == null ? "" : n(action.getProject().getProjectName());
        String description = n(action.getNextDescription());
        String estimate = action.getNextTimeEstimate() == null ? "" : " (" + action.getNextTimeEstimate() + "m)";
        String text = description;
        if (text.length() == 0) {
            text = "[No description]";
        }
        return (projectName.length() > 0 ? projectName + " - " : "") + text + estimate;
    }

    private void handleRefreshFocusClock(AppReq appReq) throws Exception {
        selectActionFromRequest(appReq);
        ActionNext currentAction = reloadCurrentAction(appReq);
        TimeTracker timeTracker = appReq.getTimeTracker();

        if (currentAction != null && timeTracker != null && timeTracker.isRunningClock()) {
            timeTracker.update(currentAction, appReq.getDataSession());
        }

        int spentMinutes = loadCurrentActionMinutesToday(appReq, currentAction);
        int todayBillableMinutes = loadBillableMinutesToday(appReq);
        int spentMinutesThisWeek = loadBillableMinutesThisWeekRounded(appReq);
        int todayTargetMinutes = dayCapacityService.loadTargetMinutesForDay(appReq,
                appReq.getWebUser().toDate(appReq.getWebUser().getLocalDateToday()));
        int weekTargetMinutes = dayCapacityService.loadTargetMinutesForCurrentWeek(appReq);
        int estimateMinutes = currentAction != null && currentAction.getNextTimeEstimate() != null
                ? currentAction.getNextTimeEstimate().intValue()
                : 0;

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("spentMinutes", Integer.valueOf(spentMinutes));
        data.put("todayBillableMinutes", Integer.valueOf(todayBillableMinutes));
        data.put("spentMinutesThisWeek", Integer.valueOf(spentMinutesThisWeek));
        data.put("todayTargetMinutes", Integer.valueOf(todayTargetMinutes));
        data.put("weekTargetMinutes", Integer.valueOf(weekTargetMinutes));
        data.put("estimateMinutes", Integer.valueOf(estimateMinutes));
        data.put("runningClock", Boolean.valueOf(timeTracker != null && timeTracker.isRunningClock()));
        data.put("nowMinute", Integer.valueOf(appReq.getWebUser().getLocalDateTimeNow().getMinute()));
        sendJsonResponse(appReq, true, "OK", data);
    }

    private void handleToggleTimer(AppReq appReq) throws Exception {
        selectActionFromRequest(appReq);
        ActionNext currentAction = reloadCurrentAction(appReq);
        TimeTracker timeTracker = appReq.getTimeTracker();
        if (timeTracker == null) {
            sendJsonResponse(appReq, false, "Timer is not available", null);
            return;
        }
        if (currentAction == null || currentAction.getProject() == null) {
            sendJsonResponse(appReq, false, "Current action is required", null);
            return;
        }

        if (timeTracker.isRunningClock()) {
            timeTracker.stopClock(appReq.getDataSession());
        } else {
            Project project = currentAction.getProject();
            appReq.setProject(project);
            appReq.setCompletingAction(currentAction);
            timeTracker.startClock(project, currentAction, appReq.getDataSession());
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("runningClock", Boolean.valueOf(timeTracker.isRunningClock()));
        sendJsonResponse(appReq, true, "OK", data);
    }

    private void handleAddCurrentActionNote(AppReq appReq) throws Exception {
        String actionNextIdStr = appReq.getRequest().getParameter("actionNextId");
        String nextNote = appReq.getRequest().getParameter("nextNote");
        if (actionNextIdStr == null || nextNote == null || nextNote.trim().length() == 0) {
            sendJsonResponse(appReq, false, "Note is required", null);
            return;
        }

        int actionNextId = Integer.parseInt(actionNextIdStr);
        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        try {
            ActionNext action = (ActionNext) dataSession.get(ActionNext.class, actionNextId);
            if (action == null) {
                transaction.rollback();
                sendJsonResponse(appReq, false, "Action not found", null);
                return;
            }

            String noteToAdd = nextNote.trim();
            String existing = action.getNextNotes();
            String updatedNotes;
            if (existing != null && existing.trim().length() > 0) {
                updatedNotes = existing + "\n - " + noteToAdd;
            } else {
                updatedNotes = " - " + noteToAdd;
            }
            action.setNextNotes(updatedNotes);
            action.setNextChangeDate(new Date());

            dataSession.update(action);
            transaction.commit();

            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("notes", extractNoteLines(action));
            sendJsonResponse(appReq, true, "Note added", data);
        } catch (Exception e) {
            transaction.rollback();
            sendJsonResponse(appReq, false, "Unable to add note", null);
        }
    }

    private ActionNext reloadCurrentAction(AppReq appReq) {
        ActionNext currentAction = appReq.getCompletingAction();
        if (currentAction == null) {
            return null;
        }
        ActionNext persisted = (ActionNext) appReq.getDataSession().get(ActionNext.class,
                currentAction.getActionNextId());
        if (persisted != null) {
            appReq.setCompletingAction(persisted);
            if (persisted.getProject() != null) {
                appReq.setProject(persisted.getProject());
            }
            return persisted;
        }
        return currentAction;
    }

    private List<FocusedActionPageRenderer.MeetingOption> loadTodayMeetingOptions(AppReq appReq) {
        WebUser webUser = appReq.getWebUser();
        Integer workspaceId = appReq.getActiveWorkspaceId();
        LocalDate today = webUser.getLocalDateToday();
        Query query = appReq.getDataSession().createQuery(
                "select distinct an from ActionNext an "
                        + "left join fetch an.project "
                        + "where an.workspaceId = :workspaceId and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                        + "and an.nextActionStatusString = :nextActionStatus "
                        + "and an.nextActionDate = :targetDate "
                        + "and an.nextActionType = :meetingType "
                        + "order by an.completionOrder, an.priorityLevel desc, an.nextChangeDate");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
        query.setParameter("targetDate", java.sql.Date.valueOf(today));
        query.setParameter("meetingType", ProjectNextActionType.WILL_MEET);
        @SuppressWarnings("unchecked")
        List<ActionNext> meetingActions = query.list();

        List<FocusedActionPageRenderer.MeetingOption> options = new ArrayList<FocusedActionPageRenderer.MeetingOption>();
        for (ActionNext action : meetingActions) {
            String projectName = action.getProject() == null ? "" : n(action.getProject().getProjectName());
            String description = n(action.getNextDescription());
            String estimate = action.getNextTimeEstimate() == null ? "" : " (" + action.getNextTimeEstimate() + "m)";
            String title = (projectName.length() > 0 ? projectName + " - " : "")
                    + description
                    + estimate;
            options.add(new FocusedActionPageRenderer.MeetingOption(action.getActionNextId(), title));
        }
        return options;
    }

    private List<FocusedActionPageRenderer.PreviousActionOption> loadRecentFocusedActionOptions(AppReq appReq,
            int excludeActionId) {
        List<FocusedActionPageRenderer.PreviousActionOption> options = new ArrayList<FocusedActionPageRenderer.PreviousActionOption>();
        HttpSession session = appReq.getRequest().getSession(false);
        if (session == null) {
            return options;
        }
        List<Integer> recentActionIds = getRecentFocusedActionIds(session);
        Session dataSession = appReq.getDataSession();

        for (Integer actionId : recentActionIds) {
            if (actionId == null || actionId.intValue() == excludeActionId) {
                continue;
            }
            ActionNext item = (ActionNext) dataSession.get(ActionNext.class, actionId);
            if (item == null) {
                continue;
            }
            String projectName = item.getProject() == null ? "" : n(item.getProject().getProjectName());
            String description = n(item.getNextDescription());
            String text = description;
            if (text.length() == 0) {
                text = "[No description]";
            }
            String suffix;
            if (item.getNextActionStatus() == ProjectNextActionStatus.COMPLETED) {
                suffix = " (completed)";
            } else if (item.getNextTimeEstimate() != null && item.getNextTimeEstimate().intValue() > 0) {
                suffix = " (" + item.getNextTimeEstimate().intValue() + "m)";
            } else {
                suffix = "";
            }
            String label = (projectName.length() > 0 ? projectName + " - " : "") + text + suffix;
            options.add(new FocusedActionPageRenderer.PreviousActionOption(item.getActionNextId(), label));
            if (options.size() >= 2) {
                break;
            }
        }
        return options;
    }

    private void trackFocusedActionSelection(AppReq appReq, ActionNext action) {
        if (action == null) {
            return;
        }
        HttpSession session = appReq.getRequest().getSession(true);
        List<Integer> recentActionIds = getRecentFocusedActionIds(session);
        Integer actionId = Integer.valueOf(action.getActionNextId());
        recentActionIds.remove(actionId);
        recentActionIds.add(0, actionId);
        while (recentActionIds.size() > 10) {
            recentActionIds.remove(recentActionIds.size() - 1);
        }
        session.setAttribute(SESSION_RECENT_FOCUSED_ACTION_IDS, recentActionIds);
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getRecentFocusedActionIds(HttpSession session) {
        Object value = session.getAttribute(SESSION_RECENT_FOCUSED_ACTION_IDS);
        List<Integer> ids = new ArrayList<Integer>();
        if (!(value instanceof List<?>)) {
            return ids;
        }
        for (Object entry : (List<Object>) value) {
            if (entry instanceof Integer) {
                ids.add((Integer) entry);
            }
        }
        return ids;
    }

    private int loadBillableMinutesToday(AppReq appReq) {
        TimeTracker timeTracker = appReq.getTimeTracker();
        if (timeTracker != null) {
            return Math.max(0, timeTracker.getTotalMinsBillable());
        }
        TimeTracker fallback = new TimeTracker(appReq.getWebUser(), appReq.getDataSession());
        return Math.max(0, fallback.getTotalMinsBillable());
    }

    private int loadCurrentActionMinutesToday(AppReq appReq, ActionNext currentAction) {
        if (currentAction == null) {
            return 0;
        }
        Query query = appReq.getDataSession().createQuery(
                "select sum(billMins) from BillEntry where action.actionNextId = :actionNextId "
                        + "and startTime >= :today and startTime < :tomorrow");
        query.setParameter("actionNextId", currentAction.getActionNextId());

        Calendar calendar = appReq.getWebUser().getCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        query.setParameter("today", calendar.getTime());
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        query.setParameter("tomorrow", calendar.getTime());

        @SuppressWarnings("unchecked")
        List<Long> billMinsList = query.list();
        if (billMinsList.size() > 0 && billMinsList.get(0) != null) {
            return Math.max(0, billMinsList.get(0).intValue());
        }
        return 0;
    }

    private int loadBillableMinutesThisWeekRounded(AppReq appReq) {
        TimeTracker timeTrackerForWeek = new TimeTracker(appReq.getWebUser(), new Date(), Calendar.WEEK_OF_YEAR,
                appReq.getDataSession());
        return calculateWeeklyRoundedMinutesLikeTrackServlet(appReq.getDataSession(), timeTrackerForWeek);
    }

    private int calculateWeeklyRoundedMinutesLikeTrackServlet(Session dataSession, TimeTracker timeTrackerForWeek) {
        int totalTimeInMinutes = 0;
        Map<Integer, Integer> projectMap = timeTrackerForWeek.getTotalMinsForProjectMap();
        for (Integer projectId : projectMap.keySet()) {
            Project project = (Project) dataSession.get(Project.class, projectId);
            if (project == null || project.getBillCode() == null) {
                continue;
            }
            BillCode billCode = resolveBillCode(dataSession, project);
            if (billCode == null || !"Y".equals(billCode.getBillable())) {
                continue;
            }
            Integer projectMinutes = projectMap.get(projectId);
            if (projectMinutes != null) {
                totalTimeInMinutes += TimeEntry.adjustMinutes(projectMinutes);
            }
        }
        return totalTimeInMinutes;
    }

    private List<String> extractNoteLines(ActionNext action) {
        List<String> lines = new ArrayList<String>();
        if (action == null || action.getNextNotes() == null || action.getNextNotes().trim().length() == 0) {
            return lines;
        }
        String[] noteLines = action.getNextNotes().split("\\r?\\n");
        for (String line : noteLines) {
            String normalized = n(line).trim();
            if (normalized.startsWith("-")) {
                normalized = normalized.substring(1).trim();
            }
            if (normalized.length() > 0) {
                lines.add(normalized);
            }
        }
        return lines;
    }

    private void printFocusedHead(AppReq appReq) throws IOException {
        HttpServletResponse response = appReq.getResponse();
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = appReq.getOut();
        TimeTracker timeTracker = appReq.getTimeTracker();

        out.println("<html>");
        out.println("  <head>");
        out.println("    <meta charset=\"UTF-8\">");
        if (timeTracker != null && timeTracker.isRunningClock()) {
            out.println("    <title>Focused Action - " + timeTracker.getTotalMinsBillableForDisplay() + "</title>");
        } else {
            out.println("    <title>Focused Action</title>");
        }
        String displayColor = appReq.getDisplayColor();
        String displaySize = appReq.getDisplaySize();
        out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"CssServlet?displaySize=" + displaySize
                + "&displayColor=" + java.net.URLEncoder.encode(displayColor, "UTF-8") + "\" />");
        out.println("  </head>");
        out.println("  <body>");
        renderPageMessages(out, appReq);
    }

    private void printFocusedFoot(AppReq appReq) {
        PrintWriter out = appReq.getOut();
        out.println("  </body>");
        out.println("</html>");
    }

    private void sendJsonResponse(AppReq appReq, boolean success, String message, Map<String, Object> data)
            throws Exception {
        appReq.getResponse().setContentType("application/json; charset=UTF-8");
        PrintWriter out = appReq.getResponse().getWriter();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\":").append(success).append(",");
        json.append("\"message\":\"").append(escapeJson(message)).append("\"");

        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                json.append(",\"").append(entry.getKey()).append("\":");
                appendJsonValue(json, entry.getValue());
            }
        }

        json.append("}");
        out.println(json.toString());
        out.flush();
    }

    @SuppressWarnings("unchecked")
    private void appendJsonValue(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof String) {
            json.append("\"").append(escapeJson((String) value)).append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof Map) {
            json.append("{");
            boolean first = true;
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                appendJsonValue(json, entry.getValue());
                first = false;
            }
            json.append("}");
        } else if (value instanceof Iterable) {
            json.append("[");
            boolean first = true;
            for (Object item : (Iterable<Object>) value) {
                if (!first) {
                    json.append(",");
                }
                appendJsonValue(json, item);
                first = false;
            }
            json.append("]");
        } else {
            json.append("\"").append(escapeJson(value.toString())).append("\"");
        }
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\\') {
                escaped.append("\\\\");
            } else if (ch == '"') {
                escaped.append("\\\"");
            } else if (ch == '\b') {
                escaped.append("\\b");
            } else if (ch == '\f') {
                escaped.append("\\f");
            } else if (ch == '\n') {
                escaped.append("\\n");
            } else if (ch == '\r') {
                escaped.append("\\r");
            } else if (ch == '\t') {
                escaped.append("\\t");
            } else if (ch < 0x20 || ch == '\u2028' || ch == '\u2029') {
                escaped.append(String.format("\\u%04x", Integer.valueOf(ch)));
            } else {
                escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    private void renderPageMessages(PrintWriter out, AppReq appReq) {
        // Messages appear in bottom-left corner with auto-hide functionality
        if (appReq.getPageMessages().isEmpty()) {
            return;
        }
        out.println("<style>");
        out.println(
                "  .pm-stack { display:flex; flex-direction:column; align-items:flex-start; gap:6px; margin:8px 0; position:fixed; bottom:8px; left:8px; z-index:9999; }");
        out.println(
                "  .pm-msg { display:flex; align-items:flex-start; justify-content:space-between; padding:10px 14px; border-radius:4px; font-size:13px; line-height:1.4; width:auto; max-width:min(70ch, calc(100vw - 32px)); animation: fadeOut 4s ease-in-out forwards; }");
        out.println("  @keyframes fadeOut { 0% { opacity: 1; } 85% { opacity: 1; } 100% { opacity: 0; } }");
        out.println("  .pm-success { background:#e8f5e9; border-left:4px solid #388e3c; color:#1b5e20; }");
        out.println("  .pm-info    { background:#e3f2fd; border-left:4px solid #1976d2; color:#0d47a1; }");
        out.println("  .pm-warning { background:#fff8e1; border-left:4px solid #f9a825; color:#6d4c00; }");
        out.println("  .pm-error   { background:#fdecea; border-left:4px solid #c62828; color:#7f0000; }");
        out.println("</style>");
        out.println("<div class=\"pm-stack\">\n");
        for (org.openimmunizationsoftware.pt.model.PageMessage pageMessage : appReq.getPageMessages()) {
            String typeClass = "pm-info";
            if (pageMessage.getSeverity() == org.openimmunizationsoftware.pt.model.PageMessageSeverity.SUCCESS) {
                typeClass = "pm-success";
            } else if (pageMessage
                    .getSeverity() == org.openimmunizationsoftware.pt.model.PageMessageSeverity.WARNING) {
                typeClass = "pm-warning";
            } else if (pageMessage.getSeverity() == org.openimmunizationsoftware.pt.model.PageMessageSeverity.ERROR) {
                typeClass = "pm-error";
            }
            out.println("  <div class=\"pm-msg " + typeClass + "\">" + escapeHtml(pageMessage.getMessageText())
                    + "</div>");
        }
        out.println("</div>");
    }

    private String escapeHtml(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '&') {
                sb.append("&amp;");
            } else if (ch == '<') {
                sb.append("&lt;");
            } else if (ch == '>') {
                sb.append("&gt;");
            } else if (ch == '\"') {
                sb.append("&quot;");
            } else if (ch == '\'') {
                sb.append("&#39;");
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}