package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.doa.WeUserDependencyDao;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.GamePointLedger;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.TemplateType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.WeUserDependency;
import org.openimmunizationsoftware.pt.model.WebUser;

import java.time.LocalDate;
import org.dandeliondaily.planahead.service.TemplateGenerationService;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.ActionNextTemplateConfig;

public class ScheduleSchoolServlet extends ClientServlet {

    private static final String PARAM_DEPENDENCY_ID = "dependencyId";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ACTION_NEXT_ID = "actionNextId";

    private static final String ACTION_UPDATE = "Update Template Schedule";
    private static final String ACTION_MOVE_UP = "MoveUp";
    private static final String ACTION_MOVE_DOWN = "MoveDown";
    private static final String ACTION_EDIT_SCHEDULE_ACTION = "EditScheduleAction";
    private static final String ACTION_DELETE_SCHEDULE_ACTION = "DeleteScheduleAction";
    private static final String ACTION_ADD_SCHEDULE_ACTION = "AddScheduleAction";

    private static final String PROJECT_ID = "projectId";
    private static final String TIME_ESTIMATE = "te";
    private static final String TIME_SLOT = "ts";
    private static final String NEXT_DESCRIPTION = "nd";
    private static final String NEXT_ACTION_TYPE = "na";
    private static final String GAME_POINTS = "gp";
    private static final String TEMPLATE_TYPE = "tt";
    private static final String MISSED_ACTION_BEHAVIOR = "mab";
    private static final String AUTO_GENERATE = "ag";
    private static final String SCHEDULE_DAYS_OF_WEEK = "sdow";
    private static final String SCHEDULE_DAYS_OF_MONTH = "sdom";
    private static final String SCHEDULE_DAYS_OF_QUARTER = "sdoq";
    private static final String SCHEDULE_DAYS_OF_YEAR = "sdoy";
    private static final String PARAM_EDIT_NEXT_DESCRIPTION = "editNextDescription";
    private static final String PARAM_EDIT_NEXT_ACTION_DATE = "editNextActionDate";
    private static final String PARAM_EDIT_NEXT_TIME_ESTIMATE = "editNextTimeEstimate";
    private static final String PARAM_EDIT_GAME_POINTS = "editGamePoints";
    private static final String PARAM_EDIT_NEXT_ACTION_TYPE = "editNextActionType";
    private static final String PARAM_EDIT_PROJECT_ID = "editProjectId";

    private static final int ASSIGNMENT_PAST_DAYS = 2;
    private static final int ASSIGNMENT_FUTURE_DAYS = 7;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            WebUser parentUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();
            PrintWriter out = appReq.getOut();

            Integer dependencyId = parseInteger(request.getParameter(PARAM_DEPENDENCY_ID));
            WeUserDependency dependency = loadValidatedDependency(parentUser, dependencyId, dataSession);
            if (dependency == null) {
                appReq.setTitle("Schedule School");
                printHtmlHead(appReq);
                printDandelionLocation(out, "Setup / Dependent Accounts / School Scheduling");
                appReq.setMessageProblem("Dependent account was not found or is not active for this parent account.");
                out.println("<p><a href=\"DependentAccountsServlet\">Back to Dependent Accounts</a></p>");
                printHtmlFoot(appReq);
                return;
            }

            WebUser dependentUser = dependency.getDependentWebUser();
            Integer activeWorkspaceId = WorkspaceRegistry.getWorkspaceIdForWebUserId(dataSession,
                    dependentUser.getWebUserId());
            if (activeWorkspaceId == null) {
                forwardToHome(request, response);
                return;
            }
            ProjectContact dependentContact = (ProjectContact) dataSession.get(ProjectContact.class,
                    dependentUser.getContactId());
            dependentUser.setProjectContact(dependentContact);
            if (dependentContact != null && dependentContact.getTimeZone() != null
                    && !dependentContact.getTimeZone().trim().equals("")) {
                dependentUser.setTimeZone(java.util.TimeZone.getTimeZone(dependentContact.getTimeZone()));
            }

            List<Project> projectList = getProjectListForDependent(dependentUser, dataSession, activeWorkspaceId);
            Map<Integer, Boolean> projectBillableMap = buildProjectBillableMap(projectList, dataSession,
                    activeWorkspaceId);

            Map<Project, List<ActionNext>> templateMap = new HashMap<Project, List<ActionNext>>();
            Map<Integer, ActionNextTemplateConfig> templateConfigMap = new HashMap<Integer, ActionNextTemplateConfig>();
            populateTemplateData(dataSession, dependentUser, projectList, activeWorkspaceId, templateMap,
                    templateConfigMap);

            String action = appReq.getAction();
            if (ACTION_MOVE_UP.equals(action) || ACTION_MOVE_DOWN.equals(action)) {
                handleMoveAction(request, dataSession, dependentUser, activeWorkspaceId,
                        ACTION_MOVE_UP.equals(action) ? -1 : 1);
                response.sendRedirect(buildSelfUrl(dependency.getDependencyId()));
                return;
            }
            if (ACTION_EDIT_SCHEDULE_ACTION.equals(action)) {
                handleScheduleActionEdit(request, dataSession, dependentUser, activeWorkspaceId);
                response.sendRedirect(buildSelfUrl(dependency.getDependencyId()));
                return;
            }
            if (ACTION_DELETE_SCHEDULE_ACTION.equals(action)) {
                handleScheduleActionDelete(request, dataSession, dependentUser, activeWorkspaceId);
                response.sendRedirect(buildSelfUrl(dependency.getDependencyId()));
                return;
            }
            if (ACTION_ADD_SCHEDULE_ACTION.equals(action)) {
                handleScheduleActionAdd(request, dataSession, dependentUser, activeWorkspaceId);
                response.sendRedirect(buildSelfUrl(dependency.getDependencyId()));
                return;
            }
            if (ACTION_UPDATE.equals(action)) {
                handleTemplateUpdate(request, dataSession, dependentUser, projectList, projectBillableMap,
                        templateMap, activeWorkspaceId);
                response.sendRedirect(buildSelfUrl(dependency.getDependencyId()));
                return;
            }

            appReq.setTitle("Schedule School");
            printHtmlHead(appReq);
            printDandelionLocation(out, "Setup / Dependent Accounts / School Scheduling");

            printDependentContext(out, dependency, dependentUser, dependentContact);

            out.println("<form action=\"ScheduleSchoolServlet\" method=\"POST\">");
            out.println("<input type=\"hidden\" name=\"" + PARAM_DEPENDENCY_ID + "\" value=\""
                    + dependency.getDependencyId() + "\">");

            List<Project> schoolProjects = new ArrayList<Project>();
            List<Project> choreProjects = new ArrayList<Project>();
            for (Project project : projectList) {
                Boolean billable = projectBillableMap.get(project.getProjectId());
                if (billable != null && billable.booleanValue()) {
                    schoolProjects.add(project);
                } else {
                    choreProjects.add(project);
                }
            }

            out.println("<h2>Template Scheduler</h2>");
            printTemplateTable(out, dependentUser, schoolProjects, templateMap, templateConfigMap,
                    "School", "School", false);
            printTemplateTable(out, dependentUser, choreProjects, templateMap, templateConfigMap,
                    "Chores", "Chores", true);

            out.println("<br/>");
            out.println("<input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_UPDATE
                    + "\" >");
            out.println("</form>");

            printTemplateEditDialogs(out, dependentUser, dependency.getDependencyId(), templateMap);

            out.println("<br/>");
            out.println("<h2>Daily Assignments</h2>");
            printDailyAssignments(out, dependentUser, dataSession, dependency.getDependencyId(), projectList,
                    projectBillableMap, activeWorkspaceId);

            out.println("<br/>");
            out.println("<p><a href=\"DependentAccountsServlet\">Back to Dependent Accounts</a></p>");

            printHtmlFoot(appReq);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private WeUserDependency loadValidatedDependency(WebUser parentUser, Integer dependencyId, Session dataSession) {
        if (dependencyId == null) {
            return null;
        }
        WeUserDependencyDao dao = new WeUserDependencyDao(dataSession);
        WeUserDependency dependency = dao.getById(dependencyId.intValue());
        if (dependency == null) {
            return null;
        }
        if (dependency.getGuardianWebUser() == null
                || dependency.getGuardianWebUser().getWebUserId() != parentUser.getWebUserId()) {
            return null;
        }
        if (!"active".equals(dependency.getDependencyStatus())) {
            return null;
        }
        return dependency;
    }

    private List<Project> getProjectListForDependent(WebUser dependentUser, Session dataSession, Integer workspaceId) {
        Query query = dataSession
                .createQuery(
                        "from Project where workspaceId = :workspaceId and (projectStatus is null or projectStatus <> :closedStatus) order by projectName");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("closedStatus", ProjectStatus.CLOSED.getDatabaseValue());
        @SuppressWarnings("unchecked")
        List<Project> projectList = query.list();
        return projectList;
    }

    private Map<Integer, Boolean> buildProjectBillableMap(List<Project> projectList, Session dataSession,
            Integer workspaceId) {
        Map<Integer, Boolean> billableMap = new HashMap<Integer, Boolean>();
        if (projectList == null || projectList.isEmpty()) {
            return billableMap;
        }

        Set<String> billCodeSet = new HashSet<String>();
        for (Project project : projectList) {
            if (project.getBillCode() != null && !project.getBillCode().trim().equals("")) {
                billCodeSet.add(project.getBillCode());
            }
        }

        Map<String, BillCode> billCodeMap = new HashMap<String, BillCode>();
        if (!billCodeSet.isEmpty()) {
            Query query = dataSession
                    .createQuery("from BillCode where workspaceId = :workspaceId and id.billCode in (:billCodes)");
            query.setParameter("workspaceId", workspaceId);
            query.setParameterList("billCodes", billCodeSet);
            @SuppressWarnings("unchecked")
            List<BillCode> billCodeList = query.list();
            for (BillCode billCode : billCodeList) {
                billCodeMap.put(billCode.getBillCode(), billCode);
            }
        }

        for (Project project : projectList) {
            BillCode billCode = billCodeMap.get(project.getBillCode());
            boolean billable = billCode != null && "Y".equalsIgnoreCase(billCode.getBillable());
            billableMap.put(Integer.valueOf(project.getProjectId()), Boolean.valueOf(billable));
        }
        return billableMap;
    }

    private void populateTemplateData(Session dataSession, WebUser dependentUser, List<Project> projectList,
            Integer workspaceId, Map<Project, List<ActionNext>> templateMap,
            Map<Integer, ActionNextTemplateConfig> templateConfigMap) {

        for (Project project : projectList) {
            Query templateQuery = dataSession.createQuery(
                    "from ActionNext where projectId = :projectId "
                            + "and (contactId = :contactId or nextContactId = :nextContactId) "
                            + "and nextDescription <> '' "
                            + "and templateTypeString is NOT NULL and templateTypeString <> '' "
                            + "and nextActionStatusString = :nextActionStatus "
                            + "order by nextActionDate asc, nextDescription");
            templateQuery.setParameter("projectId", project.getProjectId());
            templateQuery.setParameter("contactId", dependentUser.getContactId());
            templateQuery.setParameter("nextContactId", dependentUser.getContactId());
            templateQuery.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
            @SuppressWarnings("unchecked")
            List<ActionNext> templateList = templateQuery.list();
            templateMap.put(project, templateList);

            for (ActionNext templateAction : templateList) {
                ActionNextTemplateConfig config = (ActionNextTemplateConfig) dataSession
                        .get(ActionNextTemplateConfig.class, templateAction.getActionNextId());
                if (config != null) {
                    templateConfigMap.put(Integer.valueOf(templateAction.getActionNextId()), config);
                }
            }
        }
    }

    private void handleTemplateUpdate(HttpServletRequest request, Session dataSession, WebUser dependentUser,
            List<Project> projectList, Map<Integer, Boolean> projectBillableMap,
            Map<Project, List<ActionNext>> templateMap,
            Integer workspaceId) {

        Date endOfYear = calculateEndOfYear(dependentUser);
        List<Integer> syncList = new ArrayList<Integer>();

        Transaction transaction = dataSession.beginTransaction();
        try {
            for (Project project : projectList) {
                boolean projectBillable = Boolean.TRUE
                        .equals(projectBillableMap.get(Integer.valueOf(project.getProjectId())));
                List<ActionNext> templateList = templateMap.get(project);
                if (templateList == null) {
                    continue;
                }

                for (ActionNext templateAction : templateList) {
                    String nextDescription = trim(
                            request.getParameter(NEXT_DESCRIPTION + templateAction.getActionNextId()),
                            12000);
                    if (nextDescription == null) {
                        nextDescription = "";
                    }

                    String timeEstimate = request.getParameter(TIME_ESTIMATE + templateAction.getActionNextId());
                    String timeSlotString = request.getParameter(TIME_SLOT + templateAction.getActionNextId());
                    String pointsString = request.getParameter(GAME_POINTS + templateAction.getActionNextId());

                    String nextActionType = safe(
                            request.getParameter(NEXT_ACTION_TYPE + templateAction.getActionNextId())).trim();
                    if (nextActionType.isEmpty()) {
                        nextActionType = ProjectNextActionType.WILL;
                    }

                    String templateTypeId = safe(
                            request.getParameter(TEMPLATE_TYPE + templateAction.getActionNextId())).trim();
                    TemplateType templateType = TemplateType.getTemplateType(templateTypeId);
                    if (templateType == null) {
                        templateType = templateAction.getTemplateType() != null
                                ? templateAction.getTemplateType()
                                : TemplateType.DAILY;
                    }

                    String missedActionBehavior = safe(
                            request.getParameter(MISSED_ACTION_BEHAVIOR + templateAction.getActionNextId())).trim();
                    if (missedActionBehavior.isEmpty()) {
                        missedActionBehavior = "AUTO_CANCEL";
                    }

                    boolean autoGenerate = "Y".equals(
                            safe(request.getParameter(AUTO_GENERATE + templateAction.getActionNextId())).trim());

                    String scheduleDaysOfWeek = trim(
                            safe(request.getParameter(SCHEDULE_DAYS_OF_WEEK + templateAction.getActionNextId())),
                            200);
                    String scheduleDaysOfMonth = trim(
                            safe(request.getParameter(SCHEDULE_DAYS_OF_MONTH + templateAction.getActionNextId())),
                            200);
                    String scheduleDaysOfQuarter = trim(
                            safe(request.getParameter(SCHEDULE_DAYS_OF_QUARTER + templateAction.getActionNextId())),
                            200);
                    String scheduleDaysOfYear = trim(
                            safe(request.getParameter(SCHEDULE_DAYS_OF_YEAR + templateAction.getActionNextId())),
                            200);

                    boolean templateChanged = !safe(templateAction.getNextDescription()).equals(nextDescription)
                            || templateType != templateAction.getTemplateType()
                            || !nextActionType.equals(safe(templateAction.getNextActionType()));

                    if (projectBillable) {
                        Integer currentMinutes = templateAction.getNextTimeEstimate() == null ? Integer.valueOf(0)
                                : templateAction.getNextTimeEstimate();
                        Integer requestedMinutes = parseIntegerSafe(timeEstimate, Integer.valueOf(0));
                        Integer currentPoints = templateAction.getGamePoints() == null ? Integer.valueOf(0)
                                : templateAction.getGamePoints();
                        Integer requestedPoints = parseIntegerSafe(pointsString, Integer.valueOf(0));
                        if (!currentMinutes.equals(requestedMinutes) || !currentPoints.equals(requestedPoints)) {
                            templateChanged = true;
                        }
                        templateAction.setNextTimeEstimate(requestedMinutes);
                        templateAction.setGamePoints(requestedPoints);
                    } else {
                        TimeSlot currentTimeSlot = templateAction.getTimeSlot() == null ? TimeSlot.AFTERNOON
                                : templateAction.getTimeSlot();
                        TimeSlot requestedTimeSlot = TimeSlot.getTimeSlot(timeSlotString);
                        if (requestedTimeSlot == null) {
                            requestedTimeSlot = TimeSlot.AFTERNOON;
                        }
                        if (currentTimeSlot != requestedTimeSlot) {
                            templateChanged = true;
                        }
                        templateAction.setTimeSlot(requestedTimeSlot);
                    }

                    templateAction.setNextDescription(nextDescription);
                    templateAction.setTemplateType(templateType);
                    templateAction.setNextActionType(nextActionType);
                    if (templateChanged) {
                        templateAction.setNextActionDate(endOfYear);
                        templateAction.setNextChangeDate(new Date());
                    }
                    dataSession.update(templateAction);

                    int id = templateAction.getActionNextId();
                    ActionNextTemplateConfig config = (ActionNextTemplateConfig) dataSession
                            .get(ActionNextTemplateConfig.class, id);
                    if (config == null) {
                        config = new ActionNextTemplateConfig();
                        config.setActionNextId(id);
                        config.setAutoGenerate(autoGenerate);
                        config.setMissedActionBehavior(missedActionBehavior);
                        config.setScheduleDaysOfWeek(scheduleDaysOfWeek.length() > 0 ? scheduleDaysOfWeek : null);
                        config.setScheduleDaysOfMonth(scheduleDaysOfMonth.length() > 0 ? scheduleDaysOfMonth : null);
                        config.setScheduleDaysOfQuarter(
                                scheduleDaysOfQuarter.length() > 0 ? scheduleDaysOfQuarter : null);
                        config.setScheduleDaysOfYear(scheduleDaysOfYear.length() > 0 ? scheduleDaysOfYear : null);
                        dataSession.save(config);
                    } else {
                        config.setAutoGenerate(autoGenerate);
                        config.setMissedActionBehavior(missedActionBehavior);
                        config.setScheduleDaysOfWeek(scheduleDaysOfWeek.length() > 0 ? scheduleDaysOfWeek : null);
                        config.setScheduleDaysOfMonth(scheduleDaysOfMonth.length() > 0 ? scheduleDaysOfMonth : null);
                        config.setScheduleDaysOfQuarter(
                                scheduleDaysOfQuarter.length() > 0 ? scheduleDaysOfQuarter : null);
                        config.setScheduleDaysOfYear(scheduleDaysOfYear.length() > 0 ? scheduleDaysOfYear : null);
                        dataSession.update(config);
                    }
                    syncList.add(Integer.valueOf(id));
                }
            }

            String[] addTemplateRequest = readAddTemplateRequest(request);
            if (addTemplateRequest != null) {
                Project project = (Project) dataSession.get(Project.class,
                        Integer.parseInt(addTemplateRequest[0]));
                boolean projectBillable = Boolean.TRUE
                        .equals(projectBillableMap.get(Integer.valueOf(project.getProjectId())));
                ActionNext templateAction = new ActionNext();
                templateAction.setContactId(dependentUser.getContactId());
                templateAction.setContact(dependentUser.getProjectContact());
                templateAction.setNextChangeDate(new Date());
                templateAction.setProjectId(project.getProjectId());
                templateAction.setNextActionDate(endOfYear);
                templateAction.setNextActionType(ProjectNextActionType.WILL);
                TemplateType newTemplateType = TemplateType.getTemplateType(addTemplateRequest[5]);
                if (newTemplateType == null) {
                    newTemplateType = TemplateType.DAILY;
                }
                templateAction.setTemplateType(newTemplateType);
                templateAction.setNextDescription(trim(addTemplateRequest[1], 12000));
                templateAction.setWorkspaceId(workspaceId);
                templateAction.setBillable(projectBillable);
                templateAction.setNextActionStatus(ProjectNextActionStatus.READY);
                templateAction.setPriorityLevel(project.getPriorityLevel());
                if (projectBillable) {
                    templateAction.setNextTimeEstimate(parseIntegerSafe(addTemplateRequest[2], Integer.valueOf(0)));
                    templateAction.setGamePoints(parseIntegerSafe(addTemplateRequest[4], Integer.valueOf(0)));
                } else {
                    TimeSlot timeSlot = TimeSlot.getTimeSlot(addTemplateRequest[3]);
                    if (timeSlot == null) {
                        timeSlot = TimeSlot.AFTERNOON;
                    }
                    templateAction.setTimeSlot(timeSlot);
                }
                dataSession.save(templateAction);

                int newId = templateAction.getActionNextId();
                String newMissedBehavior = addTemplateRequest[6];
                if (newMissedBehavior == null || newMissedBehavior.isEmpty()) {
                    newMissedBehavior = "AUTO_CANCEL";
                }
                boolean newAutoGenerate = "Y".equals(addTemplateRequest[7]);
                String newDaysOfWeek = addTemplateRequest[8];
                String newDaysOfMonth = addTemplateRequest[9];
                String newDaysOfQuarter = addTemplateRequest[10];
                String newDaysOfYear = addTemplateRequest[11];
                ActionNextTemplateConfig newConfig = new ActionNextTemplateConfig();
                newConfig.setActionNextId(newId);
                newConfig.setAutoGenerate(newAutoGenerate);
                newConfig.setMissedActionBehavior(newMissedBehavior);
                newConfig.setScheduleDaysOfWeek(
                        newDaysOfWeek != null && newDaysOfWeek.length() > 0 ? newDaysOfWeek : null);
                newConfig.setScheduleDaysOfMonth(
                        newDaysOfMonth != null && newDaysOfMonth.length() > 0 ? newDaysOfMonth : null);
                newConfig.setScheduleDaysOfQuarter(
                        newDaysOfQuarter != null && newDaysOfQuarter.length() > 0 ? newDaysOfQuarter : null);
                newConfig.setScheduleDaysOfYear(
                        newDaysOfYear != null && newDaysOfYear.length() > 0 ? newDaysOfYear : null);
                dataSession.save(newConfig);
                syncList.add(Integer.valueOf(newId));
            }

            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }

        // Second transaction: call syncAfterEdit for each modified template,
        // using the dependent's workspace and contact — not the parent's.
        if (!syncList.isEmpty()) {
            int advanceDays = 14;
            try {
                advanceDays = Integer.parseInt(TrackerKeysManager.getKeyValue(
                        TrackerKeysManager.KEY_TEMPLATE_ADVANCE_DAYS,
                        TrackerKeysManager.KEY_TYPE_GLOBAL,
                        TrackerKeysManager.KEY_ID_GLOBAL,
                        "14", dataSession).trim());
            } catch (NumberFormatException nfe) {
                advanceDays = 14;
            }
            LocalDate today = LocalDate.now(dependentUser.getZoneId());
            for (Integer templateId : syncList) {
                Transaction syncTx = dataSession.beginTransaction();
                try {
                    ActionNext reloadedTemplate = (ActionNext) dataSession.get(ActionNext.class,
                            templateId.intValue());
                    if (reloadedTemplate != null) {
                        new TemplateGenerationService().syncAfterEdit(
                                dataSession, reloadedTemplate,
                                workspaceId,
                                dependentUser.getContactId(),
                                today, advanceDays);
                    }
                    syncTx.commit();
                } catch (RuntimeException e) {
                    syncTx.rollback();
                    System.err.println("[ScheduleSchool] Schedule sync failed for template "
                            + templateId + ": " + e.getMessage());
                }
            }
        }
    }

    private void handleMoveAction(HttpServletRequest request, Session dataSession, WebUser dependentUser,
            Integer workspaceId, int direction) {
        Integer actionNextId = parseInteger(request.getParameter(PARAM_ACTION_NEXT_ID));
        if (actionNextId == null) {
            return;
        }

        ActionNext pivot = (ActionNext) dataSession.get(ActionNext.class, actionNextId.intValue());
        if (pivot == null || pivot.getNextActionDate() == null) {
            return;
        }
        if (pivot.getWorkspaceId() == null || !pivot.getWorkspaceId().equals(workspaceId)) {
            return;
        }
        if (!isDependentRelatedAction(pivot, dependentUser)) {
            return;
        }

        Query query = dataSession.createQuery(
                "from ActionNext where workspaceId = :workspaceId "
                        + "and (contactId = :contactId or nextContactId = :nextContactId) "
                        + "and nextActionDate = :nextActionDate "
                        + "and nextDescription <> '' "
                        + "and nextActionStatusString in (:readyStatus, :completedStatus, :cancelledStatus)");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", dependentUser.getContactId());
        query.setParameter("nextContactId", dependentUser.getContactId());
        query.setParameter("nextActionDate", pivot.getNextActionDate());
        query.setParameter("readyStatus", ProjectNextActionStatus.READY.getId());
        query.setParameter("completedStatus", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("cancelledStatus", ProjectNextActionStatus.CANCELLED.getId());
        @SuppressWarnings("unchecked")
        List<ActionNext> dayList = query.list();
        sortAssignments(dayList);

        int index = -1;
        for (int i = 0; i < dayList.size(); i++) {
            if (dayList.get(i).getActionNextId() == pivot.getActionNextId()) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }

        int swapIndex = index + direction;
        if (swapIndex < 0 || swapIndex >= dayList.size()) {
            return;
        }

        ActionNext swap = dayList.get(swapIndex);
        Integer pivotOrder = Integer.valueOf(pivot.getCompletionOrder());
        Integer swapOrder = Integer.valueOf(swap.getCompletionOrder());

        Transaction trans = dataSession.beginTransaction();
        try {
            pivot.setCompletionOrder(swapOrder.intValue());
            swap.setCompletionOrder(pivotOrder.intValue());
            pivot.setNextChangeDate(new Date());
            swap.setNextChangeDate(new Date());
            dataSession.update(pivot);
            dataSession.update(swap);
            trans.commit();
        } catch (RuntimeException e) {
            trans.rollback();
            throw e;
        }
    }

    private void printDependentContext(PrintWriter out, WeUserDependency dependency, WebUser dependentUser,
            ProjectContact dependentContact) {
        String dependentName = "";
        if (dependentContact != null) {
            dependentName = (safe(dependentContact.getNameFirst()) + " " + safe(dependentContact.getNameLast())).trim();
        }
        if (dependentName.equals("")) {
            dependentName = (safe(dependentUser.getFirstName()) + " " + safe(dependentUser.getLastName())).trim();
        }

        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\"><th class=\"title\" colspan=\"2\">Dependent Context</th></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Dependent</th><td class=\"boxed\">"
                + escapeHtml(dependentName) + "</td></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Username</th><td class=\"boxed\">"
                + escapeHtml(safe(dependentUser.getUsername())) + "</td></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Email</th><td class=\"boxed\">"
                + escapeHtml(safe(dependentUser.getEmailAddress())) + "</td></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Workflow Type</th><td class=\"boxed\">"
                + escapeHtml(safe(dependentUser.getWorkflowType())) + "</td></tr>");
        out.println("</table>");
        out.println("<br/>");
    }

    private void printTemplateTable(PrintWriter out, WebUser dependentUser,
            List<Project> filteredProjectList,
            Map<Project, List<ActionNext>> templateMap,
            Map<Integer, ActionNextTemplateConfig> templateConfigMap,
            String heading,
            String addFieldSuffix,
            boolean choreTable) {

        if (filteredProjectList == null || filteredProjectList.isEmpty()) {
            return;
        }

        out.println("<h3>" + heading + "</h3>");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Project</th>");
        out.println("    <th class=\"boxed\">Template</th>");
        out.println("    <th class=\"boxed\">Schedule Type</th>");
        out.println("    <th class=\"boxed\">Schedule Days</th>");
        out.println("    <th class=\"boxed\">Missed Behavior</th>");
        out.println("    <th class=\"boxed\">Auto Gen</th>");
        if (choreTable) {
            out.println("    <th class=\"boxed\">Time Slot</th>");
        } else {
            out.println("    <th class=\"boxed\">Time<br/>(mins)</th>");
            out.println("    <th class=\"boxed\">Points</th>");
        }
        out.println("    <th class=\"boxed\">Action Type</th>");
        out.println("  </tr>");

        List<Project> orderedProjectList = new ArrayList<Project>(filteredProjectList);
        Collections.sort(orderedProjectList, new Comparator<Project>() {
            @Override
            public int compare(Project left, Project right) {
                int leftPriority = left == null ? 0 : left.getPriorityLevel();
                int rightPriority = right == null ? 0 : right.getPriorityLevel();
                if (leftPriority != rightPriority) {
                    return rightPriority - leftPriority;
                }
                String leftName = left == null || left.getProjectName() == null ? "" : left.getProjectName();
                String rightName = right == null || right.getProjectName() == null ? "" : right.getProjectName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });

        List<ActionNext> rowTemplates = new ArrayList<ActionNext>();
        Map<ActionNext, Project> templateProjectMap = new HashMap<ActionNext, Project>();
        for (Project project : orderedProjectList) {
            List<ActionNext> tl = templateMap.get(project);
            if (tl != null) {
                for (ActionNext an : tl) {
                    rowTemplates.add(an);
                    templateProjectMap.put(an, project);
                }
            }
        }

        if (choreTable) {
            Collections.sort(rowTemplates, new Comparator<ActionNext>() {
                @Override
                public int compare(ActionNext left, ActionNext right) {
                    TimeSlot leftSlot = left.getTimeSlot() == null ? TimeSlot.AFTERNOON : left.getTimeSlot();
                    TimeSlot rightSlot = right.getTimeSlot() == null ? TimeSlot.AFTERNOON : right.getTimeSlot();
                    if (leftSlot.ordinal() != rightSlot.ordinal()) {
                        return leftSlot.ordinal() - rightSlot.ordinal();
                    }
                    Project leftProject = templateProjectMap.get(left);
                    Project rightProject = templateProjectMap.get(right);
                    int leftPriority = leftProject == null ? 0 : leftProject.getPriorityLevel();
                    int rightPriority = rightProject == null ? 0 : rightProject.getPriorityLevel();
                    if (leftPriority != rightPriority) {
                        return rightPriority - leftPriority;
                    }
                    String leftName = leftProject == null || leftProject.getProjectName() == null ? ""
                            : leftProject.getProjectName();
                    String rightName = rightProject == null || rightProject.getProjectName() == null ? ""
                            : rightProject.getProjectName();
                    return leftName.compareToIgnoreCase(rightName);
                }
            });
        }

        for (ActionNext templateAction : rowTemplates) {
            Project project = templateProjectMap.get(templateAction);
            int id = templateAction.getActionNextId();
            ActionNextTemplateConfig config = templateConfigMap.get(Integer.valueOf(id));

            TemplateType currentType = templateAction.getTemplateType() != null
                    ? templateAction.getTemplateType()
                    : TemplateType.DAILY;
            String currentMissedBehavior = config != null ? safe(config.getMissedActionBehavior()) : "AUTO_CANCEL";
            if (currentMissedBehavior.isEmpty()) {
                currentMissedBehavior = "AUTO_CANCEL";
            }
            boolean currentAutoGenerate = config == null || config.isAutoGenerate();
            String currentDaysOfWeek = config != null ? safe(config.getScheduleDaysOfWeek()) : "";
            String currentDaysOfMonth = config != null ? safe(config.getScheduleDaysOfMonth()) : "";
            String currentDaysOfQuarter = config != null ? safe(config.getScheduleDaysOfQuarter()) : "";
            String currentDaysOfYear = config != null ? safe(config.getScheduleDaysOfYear()) : "";

            String schedFieldId = "schedDays" + id;

            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId()
                    + "\" class=\"button\">" + escapeHtml(project.getProjectName()) + "</a></td>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"text\" name=\"" + NEXT_DESCRIPTION + id
                    + "\" value=\"" + escapeHtml(safe(templateAction.getNextDescription())) + "\" size=\"30\"/>");
            out.println("      <a href=\"javascript:void(0);\" class=\"button\" title=\"Edit action\" "
                    + "onclick=\"openScheduleEditDialog(" + id + ")\">&#9998;</a>");
            out.println("    </td>");

            // Schedule type dropdown
            out.println("    <td class=\"boxed\">");
            out.println("      <select name=\"" + TEMPLATE_TYPE + id + "\" "
                    + "onchange=\"updateSchedFields" + id + "(this)\">");
            for (TemplateType tt : TemplateType.values()) {
                boolean selected = tt == currentType;
                out.println("        <option value=\"" + tt.getId() + "\""
                        + (selected ? " selected" : "") + ">" + escapeHtml(tt.getLabel()) + "</option>");
            }
            out.println("      </select>");
            out.println("    </td>");

            // Schedule days — one input per pattern type, shown/hidden by JS
            out.println("    <td class=\"boxed\" id=\"" + schedFieldId + "\">");
            out.println("      <span id=\"" + schedFieldId + "_weekly\""
                    + (currentType == TemplateType.WEEKLY ? "" : " style=\"display:none\"") + ">");
            out.println("        <input type=\"text\" name=\"" + SCHEDULE_DAYS_OF_WEEK + id
                    + "\" value=\"" + escapeHtml(currentDaysOfWeek) + "\" size=\"20\""
                    + " placeholder=\"MON,WED,FRI\"/>");
            out.println("      </span>");
            out.println("      <span id=\"" + schedFieldId + "_monthly\""
                    + (currentType == TemplateType.MONTHLY ? "" : " style=\"display:none\"") + ">");
            out.println("        <input type=\"text\" name=\"" + SCHEDULE_DAYS_OF_MONTH + id
                    + "\" value=\"" + escapeHtml(currentDaysOfMonth) + "\" size=\"20\""
                    + " placeholder=\"1,15\"/>");
            out.println("      </span>");
            out.println("      <span id=\"" + schedFieldId + "_quarterly\""
                    + (currentType == TemplateType.QUARTERLY ? "" : " style=\"display:none\"") + ">");
            out.println("        <input type=\"text\" name=\"" + SCHEDULE_DAYS_OF_QUARTER + id
                    + "\" value=\"" + escapeHtml(currentDaysOfQuarter) + "\" size=\"20\"/>");
            out.println("      </span>");
            out.println("      <span id=\"" + schedFieldId + "_yearly\""
                    + (currentType == TemplateType.YEARLY ? "" : " style=\"display:none\"") + ">");
            out.println("        <input type=\"text\" name=\"" + SCHEDULE_DAYS_OF_YEAR + id
                    + "\" value=\"" + escapeHtml(currentDaysOfYear) + "\" size=\"20\"/>");
            out.println("      </span>");
            out.println("    </td>");

            // JS toggle function for this row's schedule fields
            out.println("    <script>");
            out.println("      function updateSchedFields" + id + "(sel) {");
            out.println("        var v = sel.value;");
            out.println("        document.getElementById('" + schedFieldId
                    + "_weekly').style.display   = (v==='W') ? '' : 'none';");
            out.println("        document.getElementById('" + schedFieldId
                    + "_monthly').style.display  = (v==='M') ? '' : 'none';");
            out.println("        document.getElementById('" + schedFieldId
                    + "_quarterly').style.display= (v==='Q') ? '' : 'none';");
            out.println("        document.getElementById('" + schedFieldId
                    + "_yearly').style.display   = (v==='Y') ? '' : 'none';");
            out.println("      }");
            out.println("    </script>");

            // Missed action behavior
            out.println("    <td class=\"boxed\">");
            out.println("      <select name=\"" + MISSED_ACTION_BEHAVIOR + id + "\">");
            for (String[] opt : new String[][] {
                    { "AUTO_CANCEL", "Auto Cancel" },
                    { "CARRY_FORWARD", "Carry Forward" },
                    { "IGNORE", "Ignore" } }) {
                boolean sel = opt[0].equals(currentMissedBehavior);
                out.println("        <option value=\"" + opt[0] + "\""
                        + (sel ? " selected" : "") + ">" + opt[1] + "</option>");
            }
            out.println("      </select>");
            out.println("    </td>");

            // Auto generate checkbox
            out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
            out.println("      <input type=\"checkbox\" name=\"" + AUTO_GENERATE + id + "\" value=\"Y\""
                    + (currentAutoGenerate ? " checked" : "") + "/>");
            out.println("    </td>");

            // Time/points or time slot
            if (choreTable) {
                out.println("    <td class=\"boxed\">");
                printTimeSlotSelect(out, TIME_SLOT + id, templateAction.getTimeSlot());
                out.println("    </td>");
            } else {
                out.println("    <td class=\"boxed\">");
                out.println("      <input type=\"text\" name=\"" + TIME_ESTIMATE + id
                        + "\" value=\"" + templateAction.getNextTimeEstimateMinsForDisplay() + "\" size=\"4\"/>");
                out.println("    </td>");
                out.println("    <td class=\"boxed\">");
                out.println("      <input type=\"text\" name=\"" + GAME_POINTS + id
                        + "\" value=\""
                        + (templateAction.getGamePoints() == null ? "0" : templateAction.getGamePoints())
                        + "\" size=\"4\"/>");
                out.println("    </td>");
            }

            // Action type
            out.println("    <td class=\"boxed\">");
            String nextActionType = safe(templateAction.getNextActionType());
            if (nextActionType.equals("")) {
                nextActionType = ProjectNextActionType.WILL;
            }
            out.println("      <select name=\"" + NEXT_ACTION_TYPE + id + "\">");
            for (String nat : new String[] { ProjectNextActionType.WILL, ProjectNextActionType.MIGHT,
                    ProjectNextActionType.WILL_CONTACT, ProjectNextActionType.COMMITTED_TO }) {
                String label = ProjectNextActionType.getLabel(nat);
                boolean selected = nat.equals(nextActionType);
                out.println("        <option value=\"" + nat + "\"" + (selected ? " selected" : "") + ">"
                        + escapeHtml(label) + "</option>");
            }
            out.println("      </select>");
            out.println("    </td>");
            out.println("  </tr>");
        }

        // Add-template row
        String addSchedFieldId = "schedDaysAdd" + addFieldSuffix;
        out.println("  <tr>");
        out.println("    <td class=\"boxed\">");
        out.println("      <select name=\"" + PROJECT_ID + addFieldSuffix + "\">");
        for (Project project : orderedProjectList) {
            out.println("        <option value=\"" + project.getProjectId() + "\">"
                    + escapeHtml(project.getProjectName()) + "</option>");
        }
        out.println("      </select>");
        out.println("    </td>");
        out.println("    <td class=\"boxed\"><input type=\"text\" name=\"" + NEXT_DESCRIPTION + addFieldSuffix
                + "\" value=\"\" size=\"30\"/></td>");

        // Schedule type for add row
        out.println("    <td class=\"boxed\">");
        out.println("      <select name=\"" + TEMPLATE_TYPE + addFieldSuffix + "\" "
                + "onchange=\"updateSchedFieldsAdd" + addFieldSuffix + "(this)\">");
        for (TemplateType tt : TemplateType.values()) {
            boolean selected = tt == TemplateType.DAILY;
            out.println("        <option value=\"" + tt.getId() + "\""
                    + (selected ? " selected" : "") + ">" + escapeHtml(tt.getLabel()) + "</option>");
        }
        out.println("      </select>");
        out.println("    </td>");

        // Schedule days for add row
        out.println("    <td class=\"boxed\" id=\"" + addSchedFieldId + "\">");
        out.println("      <span id=\"" + addSchedFieldId + "_weekly\" style=\"display:none\">");
        out.println("        <input type=\"text\" name=\"" + SCHEDULE_DAYS_OF_WEEK + addFieldSuffix
                + "\" value=\"\" size=\"20\" placeholder=\"MON,WED,FRI\"/>");
        out.println("      </span>");
        out.println("      <span id=\"" + addSchedFieldId + "_monthly\" style=\"display:none\">");
        out.println("        <input type=\"text\" name=\"" + SCHEDULE_DAYS_OF_MONTH + addFieldSuffix
                + "\" value=\"\" size=\"20\" placeholder=\"1,15\"/>");
        out.println("      </span>");
        out.println("      <span id=\"" + addSchedFieldId + "_quarterly\" style=\"display:none\">");
        out.println("        <input type=\"text\" name=\"" + SCHEDULE_DAYS_OF_QUARTER + addFieldSuffix
                + "\" value=\"\" size=\"20\"/>");
        out.println("      </span>");
        out.println("      <span id=\"" + addSchedFieldId + "_yearly\" style=\"display:none\">");
        out.println("        <input type=\"text\" name=\"" + SCHEDULE_DAYS_OF_YEAR + addFieldSuffix
                + "\" value=\"\" size=\"20\"/>");
        out.println("      </span>");
        out.println("    </td>");

        out.println("    <script>");
        out.println("      function updateSchedFieldsAdd" + addFieldSuffix + "(sel) {");
        out.println("        var v = sel.value;");
        out.println("        document.getElementById('" + addSchedFieldId
                + "_weekly').style.display   = (v==='W') ? '' : 'none';");
        out.println("        document.getElementById('" + addSchedFieldId
                + "_monthly').style.display  = (v==='M') ? '' : 'none';");
        out.println("        document.getElementById('" + addSchedFieldId
                + "_quarterly').style.display= (v==='Q') ? '' : 'none';");
        out.println("        document.getElementById('" + addSchedFieldId
                + "_yearly').style.display   = (v==='Y') ? '' : 'none';");
        out.println("      }");
        out.println("    </script>");

        // Missed behavior for add row
        out.println("    <td class=\"boxed\">");
        out.println("      <select name=\"" + MISSED_ACTION_BEHAVIOR + addFieldSuffix + "\">");
        for (String[] opt : new String[][] {
                { "AUTO_CANCEL", "Auto Cancel" },
                { "CARRY_FORWARD", "Carry Forward" },
                { "IGNORE", "Ignore" } }) {
            boolean sel = "AUTO_CANCEL".equals(opt[0]);
            out.println("        <option value=\"" + opt[0] + "\""
                    + (sel ? " selected" : "") + ">" + opt[1] + "</option>");
        }
        out.println("      </select>");
        out.println("    </td>");

        // Auto gen for add row
        out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
        out.println("      <input type=\"checkbox\" name=\"" + AUTO_GENERATE + addFieldSuffix
                + "\" value=\"Y\" checked/>");
        out.println("    </td>");

        if (choreTable) {
            out.println("    <td class=\"boxed\">");
            printTimeSlotSelect(out, TIME_SLOT + addFieldSuffix, TimeSlot.AFTERNOON);
            out.println("    </td>");
        } else {
            out.println("    <td class=\"boxed\"><input type=\"text\" name=\"" + TIME_ESTIMATE + addFieldSuffix
                    + "\" value=\"\" size=\"4\"/></td>");
            out.println("    <td class=\"boxed\"><input type=\"text\" name=\"" + GAME_POINTS + addFieldSuffix
                    + "\" value=\"0\" size=\"4\"/></td>");
        }
        out.println("    <td class=\"boxed\">&nbsp;</td>");
        out.println("  </tr>");

        out.println("</table>");
        out.println("<br/>");
    }

    private void printTemplateEditDialogs(PrintWriter out, WebUser dependentUser, int dependencyId,
            Map<Project, List<ActionNext>> templateMap) {
        for (List<ActionNext> templateList : templateMap.values()) {
            if (templateList == null) {
                continue;
            }
            for (ActionNext templateAction : templateList) {
                printScheduleActionEditDialog(out, dependentUser, dependencyId, templateAction);
            }
        }
    }

    private void printDailyAssignments(PrintWriter out, WebUser dependentUser, Session dataSession,
            int dependencyId, List<Project> projectList, Map<Integer, Boolean> projectBillableMap,
            Integer workspaceId) {
        Date rangeStart = dependentUser
                .startOfDay(dependentUser.addDays(dependentUser.getToday(), -ASSIGNMENT_PAST_DAYS));
        Date rangeEnd = dependentUser.endOfDay(dependentUser.addDays(dependentUser.getToday(), ASSIGNMENT_FUTURE_DAYS));

        Query query = dataSession.createQuery(
                "from ActionNext where workspaceId = :workspaceId "
                        + "and (contactId = :contactId or nextContactId = :nextContactId) "
                        + "and nextDescription <> '' "
                        + "and nextActionDate >= :rangeStart and nextActionDate <= :rangeEnd "
                        + "and nextActionStatusString in (:readyStatus, :completedStatus, :cancelledStatus)");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", dependentUser.getContactId());
        query.setParameter("nextContactId", dependentUser.getContactId());
        query.setParameter("rangeStart", rangeStart);
        query.setParameter("rangeEnd", rangeEnd);
        query.setParameter("readyStatus", ProjectNextActionStatus.READY.getId());
        query.setParameter("completedStatus", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("cancelledStatus", ProjectNextActionStatus.CANCELLED.getId());
        @SuppressWarnings("unchecked")
        List<ActionNext> assignmentList = query.list();

        Map<Integer, Project> projectMap = new HashMap<Integer, Project>();
        for (ActionNext action : assignmentList) {
            if (action.getProject() == null && action.getProjectId() > 0) {
                Project project = projectMap.get(Integer.valueOf(action.getProjectId()));
                if (project == null) {
                    project = (Project) dataSession.get(Project.class, action.getProjectId());
                    projectMap.put(Integer.valueOf(action.getProjectId()), project);
                }
                action.setProject(project);
            }
        }

        sortAssignments(assignmentList);

        Map<String, List<ActionNext>> byDay = new HashMap<String, List<ActionNext>>();
        List<Date> dayOrder = new ArrayList<Date>();
        Calendar day = dependentUser.getCalendar(rangeStart);
        while (!day.getTime().after(rangeEnd)) {
            Date dayDate = day.getTime();
            dayOrder.add(dayDate);
            byDay.put(toDayKey(dayDate), new ArrayList<ActionNext>());
            day.add(Calendar.DAY_OF_MONTH, 1);
        }

        for (ActionNext action : assignmentList) {
            String key = toDayKey(action.getNextActionDate());
            List<ActionNext> rows = byDay.get(key);
            if (rows != null) {
                rows.add(action);
            }
        }

        Map<String, Integer> earnedPointsByDay = loadEarnedPointsByDay(dependentUser, dataSession, rangeStart,
                rangeEnd);

        out.println("<script>");
        out.println("  function openScheduleEditDialog(actionId) {");
        out.println("    var dialog = document.getElementById('scheduleEditDialog' + actionId);");
        out.println("    if (dialog) { dialog.style.display = 'block'; }");
        out.println("  }");
        out.println("  function closeScheduleEditDialog(actionId) {");
        out.println("    var dialog = document.getElementById('scheduleEditDialog' + actionId);");
        out.println("    if (dialog) { dialog.style.display = 'none'; }");
        out.println("  }");
        out.println("</script>");

        for (Date dayDate : dayOrder) {
            String dayKey = toDayKey(dayDate);
            List<ActionNext> rows = byDay.get(dayKey);
            if (rows == null) {
                rows = new ArrayList<ActionNext>();
            }

            List<ActionNext> dialogActions = new ArrayList<ActionNext>();
            String addDialogId = "add" + dayKey.replace("-", "");

            out.println("<h3>"
                    + escapeHtml(dependentUser.getDateFormatService().formatPattern(dayDate, "EEEE MM/dd/yyyy",
                            dependentUser.getTimeZone()))
                    + "</h3>");
            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Sort</th>");
            out.println("    <th class=\"boxed\">Project</th>");
            out.println("    <th class=\"boxed\">Task</th>");
            out.println("    <th class=\"boxed\">Source</th>");
            out.println("    <th class=\"boxed\">Status</th>");
            out.println("    <th class=\"boxed\">Est</th>");
            out.println("    <th class=\"boxed\">Actual</th>");
            out.println("    <th class=\"boxed\">Points</th>");
            out.println("    <th class=\"boxed\">Action</th>");
            out.println("  </tr>");

            int totalEstimate = 0;
            int totalActual = 0;

            for (int i = 0; i < rows.size(); i++) {
                ActionNext action = rows.get(i);
                if (action.getNextActionStatus() == ProjectNextActionStatus.CANCELLED) {
                    continue;
                }
                String projectName = action.getProject() == null ? "" : safe(action.getProject().getProjectName());
                String projectIcon = action.getProject() == null ? "" : safe(action.getProject().getProjectIcon());
                String projectLabel = projectIcon.equals("") ? projectName : (projectIcon + " " + projectName);

                int estimate = action.getNextTimeEstimate() == null ? 0 : action.getNextTimeEstimate().intValue();
                int actual = action.getNextTimeActual() == null ? 0 : action.getNextTimeActual().intValue();
                int points = action.getGamePoints() == null ? 0 : action.getGamePoints().intValue();
                boolean billable = action.isBillable();
                if (billable) {
                    totalEstimate += estimate;
                    totalActual += actual;
                }

                ProjectNextActionStatus status = action.getNextActionStatus();
                String statusLabel = "Active";
                if (status == ProjectNextActionStatus.COMPLETED) {
                    statusLabel = "Completed";
                } else if (status == ProjectNextActionStatus.CANCELLED) {
                    statusLabel = "Cancelled";
                }

                String sourceLabel = action.getTemplateActionNextId() == null ? "Ad Hoc" : "Template";

                String upUrl = "ScheduleSchoolServlet?" + PARAM_DEPENDENCY_ID + "=" + dependencyId + "&" + PARAM_ACTION
                        + "=" + ACTION_MOVE_UP + "&" + PARAM_ACTION_NEXT_ID + "=" + action.getActionNextId();
                String downUrl = "ScheduleSchoolServlet?" + PARAM_DEPENDENCY_ID + "=" + dependencyId + "&"
                        + PARAM_ACTION
                        + "=" + ACTION_MOVE_DOWN + "&" + PARAM_ACTION_NEXT_ID + "=" + action.getActionNextId();

                out.println("  <tr class=\"boxed\">");
                out.println("    <td class=\"boxed\" style=\"white-space:nowrap;\">");
                if (i > 0) {
                    out.println("      <a href=\"" + upUrl + "\" class=\"button\" title=\"Move Up\">&#8593;</a>");
                }
                if (i < rows.size() - 1) {
                    out.println("      <a href=\"" + downUrl + "\" class=\"button\" title=\"Move Down\">&#8595;</a>");
                }
                out.println("    </td>");
                out.println("    <td class=\"boxed\">" + escapeHtml(projectLabel) + "</td>");
                out.println("    <td class=\"boxed\">" + safe(action.getNextDescriptionForDisplay(action.getContact()))
                        + "</td>");
                out.println("    <td class=\"boxed\">" + sourceLabel + "</td>");
                out.println("    <td class=\"boxed\">" + escapeHtml(statusLabel) + "</td>");
                out.println("    <td class=\"boxed\">"
                        + (billable ? ActionNext.getTimeForDisplay(estimate) : "&nbsp;") + "</td>");
                out.println("    <td class=\"boxed\">"
                        + (billable ? ActionNext.getTimeForDisplay(actual) : "&nbsp;") + "</td>");
                out.println("    <td class=\"boxed\">" + points + "</td>");
                out.println("    <td class=\"boxed\"><a href=\"javascript:void(0);\" class=\"button\" "
                        + "onclick=\"openScheduleEditDialog(" + action.getActionNextId()
                        + ")\">Edit</a></td>");
                out.println("  </tr>");
                dialogActions.add(action);
            }

            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">&nbsp;</td>");
            out.println("    <td class=\"boxed\">&nbsp;</td>");
            out.println("    <td class=\"boxed\">&nbsp;</td>");
            out.println("    <td class=\"boxed\">&nbsp;</td>");
            out.println("    <td class=\"boxed\">&nbsp;</td>");
            out.println("    <td class=\"boxed\">&nbsp;</td>");
            out.println("    <td class=\"boxed\">&nbsp;</td>");
            out.println("    <td class=\"boxed\">&nbsp;</td>");
            out.println("    <td class=\"boxed\"><a href=\"javascript:void(0);\" class=\"button\" "
                    + "onclick=\"openScheduleEditDialog('" + addDialogId + "')\">Add</a></td>");
            out.println("  </tr>");

            int earnedPoints = intValue(earnedPointsByDay.get(dayKey));
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\" colspan=\"5\"><strong>Totals</strong></td>");
            out.println("    <td class=\"boxed\"><strong>" + ActionNext.getTimeForDisplay(totalEstimate)
                    + "</strong></td>");
            out.println("    <td class=\"boxed\"><strong>" + ActionNext.getTimeForDisplay(totalActual)
                    + "</strong></td>");
            out.println("    <td class=\"boxed\"><strong>" + earnedPoints + "</strong></td>");
            out.println("    <td class=\"boxed\">&nbsp;</td>");
            out.println("  </tr>");
            out.println("</table>");
            for (ActionNext action : dialogActions) {
                printScheduleActionEditDialog(out, dependentUser, dependencyId, action);
            }
            printScheduleActionAddDialog(out, dependentUser, dependencyId, dayDate, addDialogId, projectList,
                    projectBillableMap);
            out.println("<br/>");
        }
    }

    private void printScheduleActionAddDialog(PrintWriter out, WebUser dependentUser, int dependencyId,
            Date dayDate, String dialogId, List<Project> projectList, Map<Integer, Boolean> projectBillableMap) {
        SimpleDateFormat dateFormat = dependentUser.getDateFormat();
        String dateString = dayDate == null ? "" : dateFormat.format(dayDate);
        String projectSelectId = "addProjectSelect" + dialogId;
        String estimateRowId = "addEstimateRow" + dialogId;
        String pointsRowId = "addPointsRow" + dialogId;

        out.println("<div id=\"scheduleEditDialog" + dialogId
                + "\" style=\"display:none; position:fixed; inset:0; background:rgba(0,0,0,0.5); z-index:9999;\">");
        out.println(
                "  <div style=\"background:#fff; width:680px; max-width:95%; margin:40px auto; padding:14px; border:1px solid #666;\">");
        out.println("    <h3>Add Scheduled Action</h3>");
        out.println("    <form action=\"ScheduleSchoolServlet\" method=\"POST\">");
        out.println("      <input type=\"hidden\" name=\"" + PARAM_DEPENDENCY_ID + "\" value=\"" + dependencyId
                + "\"/>");
        out.println("      <input type=\"hidden\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_ADD_SCHEDULE_ACTION
                + "\"/>");

        out.println("      <table class=\"boxed\" style=\"width:100%;\">");
        out.println("        <tr class=\"boxed\"><th class=\"boxed\">Project</th><td class=\"boxed\"><select name=\""
                + PARAM_EDIT_PROJECT_ID + "\" id=\"" + projectSelectId + "\" onchange=\"toggleAddBillable"
                + dialogId + "()\">");
        if (projectList != null) {
            for (Project project : projectList) {
                boolean billable = Boolean.TRUE.equals(projectBillableMap.get(Integer.valueOf(project.getProjectId())));
                out.println("          <option value=\"" + project.getProjectId() + "\" data-billable=\""
                        + (billable ? "Y" : "N") + "\">"
                        + escapeHtml(safe(project.getProjectName())) + "</option>");
            }
        }
        out.println("        </select></td></tr>");
        out.println("        <tr class=\"boxed\"><th class=\"boxed\">When</th><td class=\"boxed\">"
                + "<input type=\"text\" name=\"" + PARAM_EDIT_NEXT_ACTION_DATE + "\" value=\""
                + escapeHtml(dateString) + "\" size=\"12\"/></td></tr>");
        out.println("        <tr class=\"boxed\"><th class=\"boxed\">What</th><td class=\"boxed\">"
                + "<textarea name=\"" + PARAM_EDIT_NEXT_DESCRIPTION
                + "\" rows=\"3\" style=\"width:98%;\"></textarea></td></tr>");
        out.println("        <tr class=\"boxed\"><th class=\"boxed\">Type</th><td class=\"boxed\"><select name=\""
                + PARAM_EDIT_NEXT_ACTION_TYPE + "\">");
        for (String nat : new String[] { ProjectNextActionType.WILL, ProjectNextActionType.MIGHT,
                ProjectNextActionType.WILL_CONTACT, ProjectNextActionType.WILL_MEET, ProjectNextActionType.WILL_REVIEW,
                ProjectNextActionType.WILL_DOCUMENT, ProjectNextActionType.WILL_FOLLOW_UP,
                ProjectNextActionType.COMMITTED_TO, ProjectNextActionType.WAITING, ProjectNextActionType.GOAL }) {
            out.println("          <option value=\"" + nat + "\">"
                    + escapeHtml(ProjectNextActionType.getLabel(nat)) + "</option>");
        }
        out.println("        </select></td></tr>");
        out.println("        <tr class=\"boxed\" id=\"" + estimateRowId
                + "\"><th class=\"boxed\">Est (mins)</th><td class=\"boxed\">"
                + "<input type=\"text\" name=\"" + PARAM_EDIT_NEXT_TIME_ESTIMATE
                + "\" value=\"\" size=\"6\"/></td></tr>");
        out.println("        <tr class=\"boxed\" id=\"" + pointsRowId
                + "\"><th class=\"boxed\">Points</th><td class=\"boxed\">"
                + "<input type=\"text\" name=\"" + PARAM_EDIT_GAME_POINTS + "\" value=\"0\" size=\"6\"/></td></tr>");
        out.println("      </table>");

        out.println("      <script>");
        out.println("        function toggleAddBillable" + dialogId + "() {");
        out.println("          var select = document.getElementById('" + projectSelectId + "');");
        out.println("          var estRow = document.getElementById('" + estimateRowId + "');");
        out.println("          var pointsRow = document.getElementById('" + pointsRowId + "');");
        out.println("          if (!select || !estRow || !pointsRow) { return; }");
        out.println("          var selected = select.options[select.selectedIndex];");
        out.println("          var isBillable = selected && selected.getAttribute('data-billable') === 'Y';");
        out.println("          estRow.style.display = isBillable ? '' : 'none';");
        out.println("          pointsRow.style.display = isBillable ? '' : 'none';");
        out.println("        }");
        out.println("        toggleAddBillable" + dialogId + "();");
        out.println("      </script>");

        out.println("      <div style=\"margin-top:10px; text-align:right;\">");
        out.println("        <button type=\"submit\">Save</button>");
        out.println("        <button type=\"button\" onclick=\"closeScheduleEditDialog('" + dialogId
                + "')\">Cancel</button>");
        out.println("      </div>");
        out.println("    </form>");
        out.println("  </div>");
        out.println("</div>");
    }

    private Map<String, Integer> loadEarnedPointsByDay(WebUser dependentUser, Session dataSession,
            Date rangeStart, Date rangeEnd) {
        Map<String, Integer> earnedByDay = new HashMap<String, Integer>();

        Query query = dataSession.createQuery(
                "from GamePointLedger where contact.contactId = :contactId "
                        + "and pointChange > 0 "
                        + "and createdDate >= :rangeStart and createdDate <= :rangeEnd");
        query.setParameter("contactId", dependentUser.getContactId());
        query.setParameter("rangeStart", rangeStart);
        query.setParameter("rangeEnd", rangeEnd);

        @SuppressWarnings("unchecked")
        List<GamePointLedger> ledgerList = query.list();

        for (GamePointLedger entry : ledgerList) {
            if (entry.getCreatedDate() == null) {
                continue;
            }
            String key = toDayKey(entry.getCreatedDate());
            Integer current = earnedByDay.get(key);
            if (current == null) {
                current = Integer.valueOf(0);
            }
            earnedByDay.put(key, Integer.valueOf(current.intValue() + intValue(entry.getPointChange())));
        }

        return earnedByDay;
    }

    private void printScheduleActionEditDialog(PrintWriter out, WebUser dependentUser, int dependencyId,
            ActionNext action) {
        SimpleDateFormat dateFormat = dependentUser.getDateFormat();
        String dateString = action.getNextActionDate() == null ? "" : dateFormat.format(action.getNextActionDate());
        String nextActionType = safe(action.getNextActionType());
        if (nextActionType.equals("")) {
            nextActionType = ProjectNextActionType.WILL;
        }

        out.println("<div id=\"scheduleEditDialog" + action.getActionNextId()
                + "\" style=\"display:none; position:fixed; inset:0; background:rgba(0,0,0,0.5); z-index:9999;\">");
        out.println(
                "  <div style=\"background:#fff; width:680px; max-width:95%; margin:40px auto; padding:14px; border:1px solid #666;\">");
        out.println("    <h3>Edit Scheduled Action</h3>");
        out.println("    <form action=\"ScheduleSchoolServlet\" method=\"POST\">");
        out.println("      <input type=\"hidden\" name=\"" + PARAM_DEPENDENCY_ID + "\" value=\"" + dependencyId
                + "\"/>");
        out.println("      <input type=\"hidden\" name=\"" + PARAM_ACTION_NEXT_ID + "\" value=\""
                + action.getActionNextId() + "\"/>");

        out.println("      <table class=\"boxed\" style=\"width:100%;\">");
        out.println("        <tr class=\"boxed\"><th class=\"boxed\">Project</th><td class=\"boxed\">"
                + escapeHtml(action.getProject() == null ? "" : safe(action.getProject().getProjectName()))
                + "</td></tr>");
        out.println("        <tr class=\"boxed\"><th class=\"boxed\">When</th><td class=\"boxed\">"
                + "<input type=\"text\" name=\"" + PARAM_EDIT_NEXT_ACTION_DATE + "\" value=\""
                + escapeHtml(dateString) + "\" size=\"12\"/></td></tr>");
        out.println("        <tr class=\"boxed\"><th class=\"boxed\">What</th><td class=\"boxed\">"
                + "<textarea name=\"" + PARAM_EDIT_NEXT_DESCRIPTION + "\" rows=\"3\" style=\"width:98%;\">"
                + escapeHtml(safe(action.getNextDescription())) + "</textarea></td></tr>");

        out.println("        <tr class=\"boxed\"><th class=\"boxed\">Type</th><td class=\"boxed\"><select name=\""
                + PARAM_EDIT_NEXT_ACTION_TYPE + "\">");
        for (String nat : new String[] { ProjectNextActionType.WILL, ProjectNextActionType.MIGHT,
                ProjectNextActionType.WILL_CONTACT, ProjectNextActionType.WILL_MEET, ProjectNextActionType.WILL_REVIEW,
                ProjectNextActionType.WILL_DOCUMENT, ProjectNextActionType.WILL_FOLLOW_UP,
                ProjectNextActionType.COMMITTED_TO, ProjectNextActionType.WAITING, ProjectNextActionType.GOAL }) {
            boolean selected = nat.equals(nextActionType);
            out.println("          <option value=\"" + nat + "\"" + (selected ? " selected" : "") + ">"
                    + escapeHtml(ProjectNextActionType.getLabel(nat)) + "</option>");
        }
        out.println("        </select></td></tr>");

        if (action.isBillable()) {
            out.println("        <tr class=\"boxed\"><th class=\"boxed\">Est (mins)</th><td class=\"boxed\">"
                    + "<input type=\"text\" name=\"" + PARAM_EDIT_NEXT_TIME_ESTIMATE + "\" value=\""
                    + escapeHtml(action.getNextTimeEstimateMinsForDisplay()) + "\" size=\"6\"/></td></tr>");
            out.println("        <tr class=\"boxed\"><th class=\"boxed\">Points</th><td class=\"boxed\">"
                    + "<input type=\"text\" name=\"" + PARAM_EDIT_GAME_POINTS + "\" value=\""
                    + (action.getGamePoints() == null ? "0" : action.getGamePoints()) + "\" size=\"6\"/></td></tr>");
        }
        out.println("      </table>");

        out.println("      <div style=\"margin-top:10px; text-align:right;\">");
        out.println("        <button type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\""
                + ACTION_DELETE_SCHEDULE_ACTION + "\" "
                + "onclick=\"return confirm('Delete this scheduled action?');\">Delete</button>");
        out.println("        <button type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\""
                + ACTION_EDIT_SCHEDULE_ACTION + "\">Save</button>");
        out.println("        <button type=\"button\" onclick=\"closeScheduleEditDialog(" + action.getActionNextId()
                + ")\">Cancel</button>");
        out.println("      </div>");
        out.println("    </form>");
        out.println("  </div>");
        out.println("</div>");
    }

    private void handleScheduleActionEdit(HttpServletRequest request, Session dataSession, WebUser dependentUser,
            Integer workspaceId) {
        Integer actionNextId = parseInteger(request.getParameter(PARAM_ACTION_NEXT_ID));
        if (actionNextId == null) {
            return;
        }

        ActionNext action = (ActionNext) dataSession.get(ActionNext.class,
                actionNextId.intValue());
        if (action == null || action.getWorkspaceId() == null || !action.getWorkspaceId().equals(workspaceId)
                || !isDependentRelatedAction(action, dependentUser)) {
            return;
        }

        String nextDescription = trim(request.getParameter(PARAM_EDIT_NEXT_DESCRIPTION), 1200);
        if (nextDescription.equals("")) {
            return;
        }

        Date nextActionDate = dependentUser.parseDate(request.getParameter(PARAM_EDIT_NEXT_ACTION_DATE));
        Integer nextTimeEstimate = parseInteger(request.getParameter(PARAM_EDIT_NEXT_TIME_ESTIMATE));
        Integer gamePoints = parseInteger(request.getParameter(PARAM_EDIT_GAME_POINTS));
        String nextActionType = safe(request.getParameter(PARAM_EDIT_NEXT_ACTION_TYPE));
        boolean validType = false;
        for (String nat : new String[] { ProjectNextActionType.WILL, ProjectNextActionType.MIGHT,
                ProjectNextActionType.WILL_CONTACT, ProjectNextActionType.WILL_MEET, ProjectNextActionType.WILL_REVIEW,
                ProjectNextActionType.WILL_DOCUMENT, ProjectNextActionType.WILL_FOLLOW_UP,
                ProjectNextActionType.COMMITTED_TO, ProjectNextActionType.WAITING, ProjectNextActionType.GOAL }) {
            if (nat.equals(nextActionType)) {
                validType = true;
                break;
            }
        }
        if (!validType) {
            nextActionType = action.getNextActionType();
        }

        Transaction trans = dataSession.beginTransaction();
        try {
            action.setNextDescription(nextDescription);
            action.setNextActionDate(nextActionDate);
            action.setNextActionType(nextActionType);
            if (action.isBillable()) {
                action.setNextTimeEstimate(
                        nextTimeEstimate == null || nextTimeEstimate.intValue() <= 0 ? null : nextTimeEstimate);
                action.setGamePoints(gamePoints == null ? Integer.valueOf(0) : gamePoints);
            } else {
                action.setNextTimeEstimate(null);
                action.setGamePoints(null);
                if (action.getNextTimeActual() != null && action.getNextTimeActual().intValue() != 0) {
                    action.setNextTimeActual(Integer.valueOf(0));
                }
            }
            action.setNextChangeDate(new Date());
            dataSession.update(action);
            trans.commit();
        } catch (RuntimeException e) {
            trans.rollback();
            throw e;
        }
    }

    private void handleScheduleActionDelete(HttpServletRequest request, Session dataSession, WebUser dependentUser,
            Integer workspaceId) {
        Integer actionNextId = parseInteger(request.getParameter(PARAM_ACTION_NEXT_ID));
        if (actionNextId == null) {
            return;
        }

        ActionNext action = (ActionNext) dataSession.get(ActionNext.class,
                actionNextId.intValue());
        if (action == null || action.getWorkspaceId() == null || !action.getWorkspaceId().equals(workspaceId)
                || !isDependentRelatedAction(action, dependentUser)) {
            return;
        }

        Transaction trans = dataSession.beginTransaction();
        try {
            action.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
            action.setNextChangeDate(new Date());
            dataSession.update(action);
            trans.commit();
        } catch (RuntimeException e) {
            trans.rollback();
            throw e;
        }
    }

    private void handleScheduleActionAdd(HttpServletRequest request, Session dataSession, WebUser dependentUser,
            Integer workspaceId) {
        Integer projectId = parseInteger(request.getParameter(PARAM_EDIT_PROJECT_ID));
        if (projectId == null) {
            return;
        }

        Project project = (Project) dataSession.get(Project.class, projectId.intValue());
        if (project == null || project.getWorkspaceId() == null || !project.getWorkspaceId().equals(workspaceId)) {
            return;
        }

        String nextDescription = trim(request.getParameter(PARAM_EDIT_NEXT_DESCRIPTION), 1200);
        if (nextDescription.equals("")) {
            return;
        }

        Date nextActionDate = dependentUser.parseDate(request.getParameter(PARAM_EDIT_NEXT_ACTION_DATE));
        if (nextActionDate == null) {
            return;
        }

        Integer nextTimeEstimate = parseInteger(request.getParameter(PARAM_EDIT_NEXT_TIME_ESTIMATE));
        Integer gamePoints = parseInteger(request.getParameter(PARAM_EDIT_GAME_POINTS));
        String nextActionType = safe(request.getParameter(PARAM_EDIT_NEXT_ACTION_TYPE));
        boolean validType = false;
        for (String nat : new String[] { ProjectNextActionType.WILL, ProjectNextActionType.MIGHT,
                ProjectNextActionType.WILL_CONTACT, ProjectNextActionType.WILL_MEET, ProjectNextActionType.WILL_REVIEW,
                ProjectNextActionType.WILL_DOCUMENT, ProjectNextActionType.WILL_FOLLOW_UP,
                ProjectNextActionType.COMMITTED_TO, ProjectNextActionType.WAITING, ProjectNextActionType.GOAL }) {
            if (nat.equals(nextActionType)) {
                validType = true;
                break;
            }
        }
        if (!validType) {
            nextActionType = ProjectNextActionType.WILL;
        }

        Integer completionOrder = Integer.valueOf(1);
        Query orderQuery = dataSession.createQuery(
                "select max(an.completionOrder) from ActionNext an "
                        + "where an.workspaceId = :workspaceId "
                        + "and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                        + "and an.nextActionDate = :nextActionDate");
        orderQuery.setParameter("workspaceId", workspaceId);
        orderQuery.setParameter("contactId", dependentUser.getContactId());
        orderQuery.setParameter("nextContactId", dependentUser.getContactId());
        orderQuery.setParameter("nextActionDate", nextActionDate);
        Number maxOrder = (Number) orderQuery.uniqueResult();
        if (maxOrder != null) {
            completionOrder = Integer.valueOf(maxOrder.intValue() + 1);
        }

        boolean billable = isProjectBillable(project, dataSession, workspaceId);

        Transaction trans = dataSession.beginTransaction();
        try {
            ActionNext action = new ActionNext();
            action.setProjectId(project.getProjectId());
            action.setProject(project);
            action.setContactId(dependentUser.getContactId());
            action.setContact(dependentUser.getProjectContact());
            action.setWorkspaceId(workspaceId);
            action.setNextActionDate(nextActionDate);
            action.setNextDescription(nextDescription);
            action.setNextActionType(nextActionType);
            action.setNextActionStatus(ProjectNextActionStatus.READY);
            action.setBillable(billable);
            action.setCompletionOrder(completionOrder.intValue());
            action.setPriorityLevel(project.getPriorityLevel());
            action.setNextTimeEstimate(billable && nextTimeEstimate != null && nextTimeEstimate.intValue() > 0
                    ? nextTimeEstimate
                    : null);
            action.setGamePoints(billable ? (gamePoints == null ? Integer.valueOf(0) : gamePoints) : null);
            action.setNextChangeDate(new Date());
            dataSession.save(action);
            trans.commit();
        } catch (RuntimeException e) {
            trans.rollback();
            throw e;
        }
    }

    private boolean isProjectBillable(Project project, Session dataSession, Integer workspaceId) {
        if (project == null || project.getBillCode() == null || project.getBillCode().trim().equals("")) {
            return false;
        }
        Query billCodeQuery = dataSession
                .createQuery("from BillCode where workspaceId = :workspaceId and id.billCode = :billCode");
        billCodeQuery.setParameter("workspaceId", workspaceId);
        billCodeQuery.setParameter("billCode", project.getBillCode());
        BillCode billCode = (BillCode) billCodeQuery.uniqueResult();
        return billCode != null && "Y".equalsIgnoreCase(billCode.getBillable());
    }

    private boolean isDependentRelatedAction(ActionNext action, WebUser dependentUser) {
        if (action == null || dependentUser == null) {
            return false;
        }
        int dependentContactId = dependentUser.getContactId();
        return action.getContactId() == dependentContactId
                || (action.getNextContactId() != null && action.getNextContactId().intValue() == dependentContactId);
    }

    private void sortAssignments(List<ActionNext> actionList) {
        Collections.sort(actionList, new Comparator<ActionNext>() {
            @Override
            public int compare(ActionNext left, ActionNext right) {
                String leftDay = toDayKey(left.getNextActionDate());
                String rightDay = toDayKey(right.getNextActionDate());
                int dayCmp = leftDay.compareTo(rightDay);
                if (dayCmp != 0) {
                    return dayCmp;
                }

                int leftProjectPriority = left.getProject() == null ? 0 : left.getProject().getPriorityLevel();
                int rightProjectPriority = right.getProject() == null ? 0 : right.getProject().getPriorityLevel();
                if (leftProjectPriority != rightProjectPriority) {
                    return rightProjectPriority - leftProjectPriority;
                }

                int leftOrder = left.getCompletionOrder();
                int rightOrder = right.getCompletionOrder();
                if (leftOrder != rightOrder) {
                    return leftOrder - rightOrder;
                }

                Date leftChanged = left.getNextChangeDate();
                Date rightChanged = right.getNextChangeDate();
                if (leftChanged == null && rightChanged == null) {
                    return 0;
                }
                if (leftChanged == null) {
                    return 1;
                }
                if (rightChanged == null) {
                    return -1;
                }
                return leftChanged.compareTo(rightChanged);
            }
        });
    }

    private String[] readAddTemplateRequest(HttpServletRequest request) {
        for (String suffix : new String[] { "School", "Chores", "" }) {
            String projectIdParam = suffix.equals("") ? PROJECT_ID : PROJECT_ID + suffix;
            String descriptionParam = suffix.equals("") ? NEXT_DESCRIPTION : NEXT_DESCRIPTION + suffix;
            String timeEstimateParam = suffix.equals("") ? TIME_ESTIMATE : TIME_ESTIMATE + suffix;
            String timeSlotParam = suffix.equals("") ? TIME_SLOT : TIME_SLOT + suffix;
            String pointsParam = suffix.equals("") ? GAME_POINTS : GAME_POINTS + suffix;
            String templateTypeParam = suffix.equals("") ? TEMPLATE_TYPE : TEMPLATE_TYPE + suffix;
            String missedBehaviorParam = suffix.equals("") ? MISSED_ACTION_BEHAVIOR
                    : MISSED_ACTION_BEHAVIOR + suffix;
            String autoGenerateParam = suffix.equals("") ? AUTO_GENERATE : AUTO_GENERATE + suffix;
            String daysOfWeekParam = suffix.equals("") ? SCHEDULE_DAYS_OF_WEEK : SCHEDULE_DAYS_OF_WEEK + suffix;
            String daysOfMonthParam = suffix.equals("") ? SCHEDULE_DAYS_OF_MONTH : SCHEDULE_DAYS_OF_MONTH + suffix;
            String daysOfQuarterParam = suffix.equals("") ? SCHEDULE_DAYS_OF_QUARTER
                    : SCHEDULE_DAYS_OF_QUARTER + suffix;
            String daysOfYearParam = suffix.equals("") ? SCHEDULE_DAYS_OF_YEAR : SCHEDULE_DAYS_OF_YEAR + suffix;

            String projectId = request.getParameter(projectIdParam);
            String description = request.getParameter(descriptionParam);
            String timeEstimate = request.getParameter(timeEstimateParam);
            String timeSlot = request.getParameter(timeSlotParam);
            String points = request.getParameter(pointsParam);
            String templateType = request.getParameter(templateTypeParam);
            String missedBehavior = request.getParameter(missedBehaviorParam);
            String autoGenerate = request.getParameter(autoGenerateParam);
            String daysOfWeek = request.getParameter(daysOfWeekParam);
            String daysOfMonth = request.getParameter(daysOfMonthParam);
            String daysOfQuarter = request.getParameter(daysOfQuarterParam);
            String daysOfYear = request.getParameter(daysOfYearParam);

            if (projectId != null && !projectId.equals("")
                    && description != null && !description.trim().equals("")) {
                if (timeEstimate == null) {
                    timeEstimate = "";
                }
                if (timeSlot == null) {
                    timeSlot = "";
                }
                if (points == null) {
                    points = "0";
                }
                if (templateType == null) {
                    templateType = "D";
                }
                if (missedBehavior == null) {
                    missedBehavior = "AUTO_CANCEL";
                }
                if (autoGenerate == null) {
                    autoGenerate = "";
                }
                if (daysOfWeek == null) {
                    daysOfWeek = "";
                }
                if (daysOfMonth == null) {
                    daysOfMonth = "";
                }
                if (daysOfQuarter == null) {
                    daysOfQuarter = "";
                }
                if (daysOfYear == null) {
                    daysOfYear = "";
                }
                return new String[] {
                        projectId, description.trim(), timeEstimate.trim(), timeSlot.trim(), points.trim(),
                        templateType.trim(), missedBehavior.trim(), autoGenerate.trim(),
                        daysOfWeek.trim(), daysOfMonth.trim(), daysOfQuarter.trim(), daysOfYear.trim()
                };
            }
        }
        return null;
    }

    private Date calculateEndOfYear(WebUser webUser) {
        Calendar calendar = webUser.getCalendar();
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.MONTH, 11);
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        return calendar.getTime();
    }

    private void printTimeSlotSelect(PrintWriter out, String name, TimeSlot selectedTimeSlot) {
        TimeSlot effectiveTimeSlot = selectedTimeSlot == null ? TimeSlot.AFTERNOON : selectedTimeSlot;
        out.println("<select name=\"" + name + "\">");
        for (TimeSlot timeSlot : TimeSlot.values()) {
            out.println("  <option value=\"" + timeSlot.getId() + "\""
                    + (timeSlot == effectiveTimeSlot ? " selected" : "") + ">"
                    + escapeHtml(timeSlot.getLabel()) + "</option>");
        }
        out.println("</select>");
    }

    private static String toDayKey(Calendar calendar) {
        if (calendar == null) {
            return "";
        }
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    private static String toDayKey(Date date) {
        if (date == null) {
            return "";
        }
        Calendar calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        calendar.setTime(date);
        return toDayKey(calendar);
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.trim().equals("")) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseIntegerSafe(String value, Integer fallback) {
        Integer parsed = parseInteger(value);
        return parsed == null ? fallback : parsed;
    }

    private static int intValue(Number number) {
        return number == null ? 0 : number.intValue();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    protected static String trim(String value, int maxLength) {
        String v = value == null ? "" : value.trim();
        if (v.length() > maxLength) {
            v = v.substring(0, maxLength);
        }
        return v;
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String buildSelfUrl(int dependencyId) {
        return "ScheduleSchoolServlet?" + PARAM_DEPENDENCY_ID + "=" + dependencyId;
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
}
