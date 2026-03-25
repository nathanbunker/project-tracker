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
import org.openimmunizationsoftware.pt.doa.WeUserDependencyDao;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.GamePointLedger;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TemplateType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.WeUserDependency;
import org.openimmunizationsoftware.pt.model.WebUser;

public class ScheduleSchoolServlet extends ClientServlet {

    private static final String PARAM_DEPENDENCY_ID = "dependencyId";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ACTION_NEXT_ID = "actionNextId";

    private static final String ACTION_UPDATE = "Update Template Schedule";
    private static final String ACTION_MOVE_UP = "MoveUp";
    private static final String ACTION_MOVE_DOWN = "MoveDown";

    private static final String TEMPLATE_SELECTED = "s";
    private static final String PROJECT_ID = "projectId";
    private static final String TIME_ESTIMATE = "te";
    private static final String TIME_SLOT = "ts";
    private static final String NEXT_DESCRIPTION = "nd";
    private static final String NEXT_ACTION_TYPE = "na";
    private static final String GAME_POINTS = "gp";

    private static final int TEMPLATE_DAY_SPAN = 15;
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
            ProjectContact dependentContact = (ProjectContact) dataSession.get(ProjectContact.class,
                    dependentUser.getContactId());
            dependentUser.setProjectContact(dependentContact);
            if (dependentContact != null && dependentContact.getTimeZone() != null
                    && !dependentContact.getTimeZone().trim().equals("")) {
                dependentUser.setTimeZone(java.util.TimeZone.getTimeZone(dependentContact.getTimeZone()));
            }

            if (dependentUser.getProvider() == null) {
                appReq.setTitle("Schedule School");
                printHtmlHead(appReq);
                printDandelionLocation(out, "Setup / Dependent Accounts / School Scheduling");
                out.println("<h2>Dependent Account Not Ready</h2>");
                out.println("<p>This dependent account has not completed setup yet.</p>");
                out.println("<p><a href=\"DependentAccountsServlet\">Back to Dependent Accounts</a></p>");
                printHtmlFoot(appReq);
                return;
            }

            List<Project> projectList = getProjectListForDependent(dependentUser, dataSession);
            Map<Integer, Boolean> projectBillableMap = buildProjectBillableMap(projectList, dependentUser, dataSession);

            List<Calendar> dayList = buildTemplateDayList(dependentUser);
            Calendar dayRangeEnd = (Calendar) dayList.get(dayList.size() - 1).clone();
            dayRangeEnd.add(Calendar.DAY_OF_MONTH, 1);

            Map<Project, List<ProjectActionNext>> templateMap = new HashMap<Project, List<ProjectActionNext>>();
            Map<ProjectActionNext, Map<Calendar, ProjectActionNext>> projectActionDayMap = new HashMap<ProjectActionNext, Map<Calendar, ProjectActionNext>>();
            populateTemplateData(dataSession, dependentUser, projectList, dayList, dayRangeEnd, templateMap,
                    projectActionDayMap);

            String action = appReq.getAction();
            if (ACTION_MOVE_UP.equals(action) || ACTION_MOVE_DOWN.equals(action)) {
                handleMoveAction(request, dataSession, dependentUser, ACTION_MOVE_UP.equals(action) ? -1 : 1);
                response.sendRedirect(buildSelfUrl(dependency.getDependencyId()));
                return;
            }
            if (ACTION_UPDATE.equals(action)) {
                handleTemplateUpdate(request, dataSession, dependentUser, projectList, projectBillableMap, dayList,
                        templateMap, projectActionDayMap);
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
            printTemplateTable(out, dependentUser, dayList, schoolProjects, templateMap, projectActionDayMap,
                    "School", "School", false);
            printTemplateTable(out, dependentUser, dayList, choreProjects, templateMap, projectActionDayMap,
                    "Chores", "Chores", true);

            out.println("<br/>");
            out.println("<input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_UPDATE
                    + "\" >");
            out.println("</form>");

            out.println("<br/>");
            out.println("<h2>Daily Assignments</h2>");
            printDailyAssignments(out, dependentUser, dataSession, dependency.getDependencyId());

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

    private List<Project> getProjectListForDependent(WebUser dependentUser, Session dataSession) {
        Query query = dataSession
                .createQuery("from Project where provider = :provider and phaseCode <> 'Clos' order by projectName");
        query.setParameter("provider", dependentUser.getProvider());
        @SuppressWarnings("unchecked")
        List<Project> projectList = query.list();
        return projectList;
    }

    private Map<Integer, Boolean> buildProjectBillableMap(List<Project> projectList, WebUser dependentUser,
            Session dataSession) {
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
                .createQuery("from BillCode where provider = :provider and id.billCode in (:billCodes)");
            query.setParameter("provider", dependentUser.getProvider());
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

    private List<Calendar> buildTemplateDayList(WebUser dependentUser) {
        List<Calendar> dayList = new ArrayList<Calendar>();
        for (int i = 0; i < TEMPLATE_DAY_SPAN; i++) {
            Calendar day = dependentUser.getCalendar();
            day.set(Calendar.HOUR_OF_DAY, 0);
            day.set(Calendar.MINUTE, 0);
            day.set(Calendar.SECOND, 0);
            day.set(Calendar.MILLISECOND, 0);
            day.add(Calendar.DAY_OF_MONTH, i);
            dayList.add(day);
        }
        return dayList;
    }

    private void populateTemplateData(Session dataSession, WebUser dependentUser, List<Project> projectList,
            List<Calendar> dayList, Calendar dayRangeEnd,
            Map<Project, List<ProjectActionNext>> templateMap,
            Map<ProjectActionNext, Map<Calendar, ProjectActionNext>> projectActionDayMap) {

        Map<String, Calendar> dayByKey = new HashMap<String, Calendar>();
        for (Calendar day : dayList) {
            dayByKey.put(toDayKey(day), day);
        }

        for (Project project : projectList) {
            Query templateQuery = dataSession.createQuery(
                    "from ProjectActionNext where projectId = :projectId and nextDescription <> '' "
                            + "and templateTypeString is NOT NULL and templateTypeString <> '' "
                            + "and nextActionStatusString = :nextActionStatus "
                            + "order by nextActionDate asc, nextDescription");
            templateQuery.setParameter("projectId", project.getProjectId());
            templateQuery.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
            @SuppressWarnings("unchecked")
            List<ProjectActionNext> templateList = templateQuery.list();
            templateMap.put(project, templateList);

            for (ProjectActionNext templateAction : templateList) {
                Map<Calendar, ProjectActionNext> projectActionMap = new HashMap<Calendar, ProjectActionNext>();
                projectActionDayMap.put(templateAction, projectActionMap);

                Query dayQuery = dataSession.createQuery(
                        "from ProjectActionNext where templateActionNextId = :templateActionNextId "
                                + "and nextActionDate >= :nextActionDate "
                                + "and nextActionDate < :nextActionDateEnd "
                                + "and contactId = :contactId "
                                + "and provider = :provider "
                                + "and nextActionStatusString in (:readyStatus, :completedStatus, :cancelledStatus) "
                                + "order by nextChangeDate desc");
                dayQuery.setParameter("templateActionNextId", templateAction.getActionNextId());
                dayQuery.setParameter("nextActionDate", dayList.get(0).getTime());
                dayQuery.setParameter("nextActionDateEnd", dayRangeEnd.getTime());
                dayQuery.setParameter("contactId", dependentUser.getContactId());
                dayQuery.setParameter("provider", dependentUser.getProvider());
                dayQuery.setParameter("readyStatus", ProjectNextActionStatus.READY.getId());
                dayQuery.setParameter("completedStatus", ProjectNextActionStatus.COMPLETED.getId());
                dayQuery.setParameter("cancelledStatus", ProjectNextActionStatus.CANCELLED.getId());
                @SuppressWarnings("unchecked")
                List<ProjectActionNext> scheduledList = dayQuery.list();
                for (ProjectActionNext scheduledAction : scheduledList) {
                    Calendar calendar = dayByKey.get(toDayKey(scheduledAction.getNextActionDate()));
                    if (calendar != null && !projectActionMap.containsKey(calendar)) {
                        projectActionMap.put(calendar, scheduledAction);
                    }
                }
            }
        }
    }

    private void handleTemplateUpdate(HttpServletRequest request, Session dataSession, WebUser dependentUser,
            List<Project> projectList, Map<Integer, Boolean> projectBillableMap,
            List<Calendar> dayList,
            Map<Project, List<ProjectActionNext>> templateMap,
            Map<ProjectActionNext, Map<Calendar, ProjectActionNext>> projectActionDayMap) {

        SimpleDateFormat sdfField = dependentUser.getDateFormat("yyyyMMdd");
        Date endOfYear = calculateEndOfYear(dependentUser);

        Transaction transaction = dataSession.beginTransaction();
        try {
            for (Project project : projectList) {
                boolean projectBillable = Boolean.TRUE
                        .equals(projectBillableMap.get(Integer.valueOf(project.getProjectId())));
                List<ProjectActionNext> templateList = templateMap.get(project);
                if (templateList == null) {
                    continue;
                }

                for (ProjectActionNext templateAction : templateList) {
                    Map<Calendar, ProjectActionNext> projectActionMap = projectActionDayMap.get(templateAction);
                    String nextDescription = trim(
                            request.getParameter(NEXT_DESCRIPTION + templateAction.getActionNextId()),
                            12000);
                    if (nextDescription == null) {
                        nextDescription = "";
                    }

                    String timeEstimate = request.getParameter(TIME_ESTIMATE + templateAction.getActionNextId());
                    String timeSlotString = request.getParameter(TIME_SLOT + templateAction.getActionNextId());
                    String pointsString = request.getParameter(GAME_POINTS + templateAction.getActionNextId());

                    boolean templateChanged = !safe(templateAction.getNextDescription()).equals(nextDescription);
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

                    if (templateChanged) {
                        templateAction.setNextDescription(nextDescription);
                        templateAction.setNextActionDate(endOfYear);
                        templateAction.setNextChangeDate(new Date());
                        dataSession.update(templateAction);
                    }

                    for (Calendar day : dayList) {
                        String fieldName = TEMPLATE_SELECTED + templateAction.getActionNextId() + "."
                                + sdfField.format(day.getTime());
                        boolean checked = request.getParameter(fieldName) != null;
                        ProjectActionNext assignedAction = projectActionMap == null ? null : projectActionMap.get(day);

                        if (!checked) {
                            if (assignedAction != null
                                    && assignedAction.getNextActionStatus() != ProjectNextActionStatus.CANCELLED) {
                                assignedAction.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
                                assignedAction.setNextChangeDate(new Date());
                                dataSession.update(assignedAction);
                            }
                        } else {
                            if (assignedAction == null) {
                                assignedAction = new ProjectActionNext();
                                assignedAction.setProjectId(project.getProjectId());
                                assignedAction.setContactId(dependentUser.getContactId());
                                assignedAction.setContact(dependentUser.getProjectContact());
                                assignedAction.setProvider(dependentUser.getProvider());
                                assignedAction.setNextActionDate(day.getTime());
                                assignedAction.setBillable(projectBillable);
                                assignedAction.setTemplateActionNextId(templateAction.getActionNextId());
                                assignedAction.setProcessStage(templateAction.getProcessStage());
                                assignedAction.setCompletionOrder(0);
                                String nextActionType = request
                                        .getParameter(NEXT_ACTION_TYPE + templateAction.getActionNextId());
                                if (nextActionType == null || nextActionType.trim().equals("")) {
                                    nextActionType = ProjectNextActionType.WILL;
                                }
                                assignedAction.setNextActionType(nextActionType);
                                int priorityLevel = project.getPriorityLevel();
                                if (ProjectNextActionType.COMMITTED_TO.equals(nextActionType)) {
                                    priorityLevel += 1;
                                } else if (ProjectNextActionType.MIGHT.equals(nextActionType)
                                        || ProjectNextActionType.WAITING.equals(nextActionType)) {
                                    priorityLevel -= 1;
                                }
                                assignedAction.setPriorityLevel(priorityLevel);
                                if (!projectBillable) {
                                    TimeSlot templateSlot = templateAction.getTimeSlot();
                                    assignedAction
                                            .setTimeSlot(templateSlot == null ? TimeSlot.AFTERNOON : templateSlot);
                                }
                                if (projectActionMap != null) {
                                    projectActionMap.put(day, assignedAction);
                                }
                                dataSession.save(assignedAction);
                            }
                            assignedAction.setNextDescription(templateAction.getNextDescription());
                            assignedAction.setNextTimeEstimate(templateAction.getNextTimeEstimate());
                            assignedAction.setGamePoints(templateAction.getGamePoints());
                            if (!projectBillable) {
                                assignedAction.setTimeSlot(templateAction.getTimeSlot());
                            }
                            if (assignedAction.getNextActionStatus() == ProjectNextActionStatus.CANCELLED) {
                                assignedAction.setNextActionStatus(ProjectNextActionStatus.READY);
                            }
                            assignedAction.setNextChangeDate(new Date());
                            dataSession.saveOrUpdate(assignedAction);
                        }
                    }
                }
            }

            String[] addTemplateRequest = readAddTemplateRequest(request);
            if (addTemplateRequest != null) {
                Project project = (Project) dataSession.get(Project.class, Integer.parseInt(addTemplateRequest[0]));
                boolean projectBillable = Boolean.TRUE
                        .equals(projectBillableMap.get(Integer.valueOf(project.getProjectId())));
                ProjectActionNext templateAction = new ProjectActionNext();
                templateAction.setContactId(dependentUser.getContactId());
                templateAction.setContact(dependentUser.getProjectContact());
                templateAction.setNextChangeDate(new Date());
                templateAction.setProjectId(project.getProjectId());
                templateAction.setNextActionDate(endOfYear);
                templateAction.setNextActionType(ProjectNextActionType.WILL);
                templateAction.setTemplateType(TemplateType.DAILY);
                templateAction.setNextDescription(trim(addTemplateRequest[1], 12000));
                templateAction.setProvider(dependentUser.getProvider());
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
            }

            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }
    }

    private void handleMoveAction(HttpServletRequest request, Session dataSession, WebUser dependentUser,
            int direction) {
        Integer actionNextId = parseInteger(request.getParameter(PARAM_ACTION_NEXT_ID));
        if (actionNextId == null) {
            return;
        }

        ProjectActionNext pivot = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId.intValue());
        if (pivot == null || pivot.getNextActionDate() == null) {
            return;
        }
        if (pivot.getProvider() == null || dependentUser.getProvider() == null
                || !safe(pivot.getProvider().getProviderId())
                        .equals(safe(dependentUser.getProvider().getProviderId()))) {
            return;
        }
        if (pivot.getContactId() != dependentUser.getContactId()) {
            return;
        }

        Query query = dataSession.createQuery(
                "from ProjectActionNext where provider = :provider and contactId = :contactId "
                        + "and nextActionDate = :nextActionDate "
                        + "and nextDescription <> '' "
                        + "and nextActionStatusString in (:readyStatus, :completedStatus, :cancelledStatus)");
        query.setParameter("provider", dependentUser.getProvider());
        query.setParameter("contactId", dependentUser.getContactId());
        query.setParameter("nextActionDate", pivot.getNextActionDate());
        query.setParameter("readyStatus", ProjectNextActionStatus.READY.getId());
        query.setParameter("completedStatus", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("cancelledStatus", ProjectNextActionStatus.CANCELLED.getId());
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> dayList = query.list();
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

        ProjectActionNext swap = dayList.get(swapIndex);
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

    private void printTemplateTable(PrintWriter out, WebUser dependentUser, List<Calendar> dayList,
            List<Project> filteredProjectList,
            Map<Project, List<ProjectActionNext>> templateMap,
            Map<ProjectActionNext, Map<Calendar, ProjectActionNext>> projectActionDayMap,
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
        if (choreTable) {
            out.println("    <th class=\"boxed\">Time Slot</th>");
        } else {
            out.println("    <th class=\"boxed\">Time<br/>(mins)</th>");
            out.println("    <th class=\"boxed\">Points</th>");
        }

        Set<Calendar> onWeekend = new HashSet<Calendar>();
        Map<Calendar, Integer> timeMap = new HashMap<Calendar, Integer>();
        SimpleDateFormat sdfField = dependentUser.getDateFormat("yyyyMMdd");
        for (Calendar day : dayList) {
            out.println("    <th class=\"boxed\">"
                    + dependentUser.getDateFormatService().formatWeekdayShort(day.getTime(),
                            dependentUser.getTimeZone())
                    + "<br/>"
                    + dependentUser.getDateFormatService().formatPattern(day.getTime(), "M/d",
                            dependentUser.getTimeZone())
                    + "</th>");
            timeMap.put(day, Integer.valueOf(0));
            if (day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                    || day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                onWeekend.add(day);
            }
        }
        out.println("    <th class=\"boxed\">Action</th>");
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

        List<ProjectActionNext> rowTemplates = new ArrayList<ProjectActionNext>();
        Map<ProjectActionNext, Project> templateProjectMap = new HashMap<ProjectActionNext, Project>();
        for (Project project : orderedProjectList) {
            List<ProjectActionNext> tl = templateMap.get(project);
            if (tl != null) {
                for (ProjectActionNext pan : tl) {
                    rowTemplates.add(pan);
                    templateProjectMap.put(pan, project);
                }
            }
        }

        if (choreTable) {
            Collections.sort(rowTemplates, new Comparator<ProjectActionNext>() {
                @Override
                public int compare(ProjectActionNext left, ProjectActionNext right) {
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

        for (ProjectActionNext templateAction : rowTemplates) {
            Project project = templateProjectMap.get(templateAction);
            Map<Calendar, ProjectActionNext> dayMap = projectActionDayMap.get(templateAction);

            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId()
                    + "\" class=\"button\">" + escapeHtml(project.getProjectName()) + "</a></td>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"text\" name=\"" + NEXT_DESCRIPTION + templateAction.getActionNextId()
                    + "\" value=\"" + escapeHtml(safe(templateAction.getNextDescription())) + "\" size=\"30\"/>");
            String link = "ProjectActionServlet?" + ProjectActionServlet.PARAM_COMPLETING_ACTION_NEXT_ID + "="
                    + templateAction.getActionNextId()
                    + "&editActionNextId=" + templateAction.getActionNextId();
            out.println("      <a href=\"" + link + "\" class=\"button\" title=\"Edit action\">&#9998;</a>");
            out.println("    </td>");

            if (choreTable) {
                out.println("    <td class=\"boxed\">");
                printTimeSlotSelect(out, TIME_SLOT + templateAction.getActionNextId(), templateAction.getTimeSlot());
                out.println("    </td>");
            } else {
                out.println("    <td class=\"boxed\">");
                out.println("      <input type=\"text\" name=\"" + TIME_ESTIMATE + templateAction.getActionNextId()
                        + "\" value=\"" + templateAction.getNextTimeEstimateMinsForDisplay() + "\" size=\"4\"/>");
                out.println("    </td>");
                out.println("    <td class=\"boxed\">");
                out.println("      <input type=\"text\" name=\"" + GAME_POINTS + templateAction.getActionNextId()
                        + "\" value=\""
                        + (templateAction.getGamePoints() == null ? "0" : templateAction.getGamePoints())
                        + "\" size=\"4\"/>");
                out.println("    </td>");
            }

            for (Calendar day : dayList) {
                ProjectActionNext dayAction = dayMap == null ? null : dayMap.get(day);
                boolean checked = dayAction != null
                        && dayAction.getNextActionStatus() != ProjectNextActionStatus.CANCELLED;
                String style = onWeekend.contains(day) ? "boxed-lowlight" : "boxed";
                out.println("    <td class=\"" + style + "\">");
                out.println("      <input type=\"checkbox\" name=\"" + TEMPLATE_SELECTED
                        + templateAction.getActionNextId() + "."
                        + sdfField.format(day.getTime()) + "\" value=\"Y\"" + (checked ? " checked" : "") + "/>");
                out.println("    </td>");

                if (!choreTable && checked && templateAction.getNextTimeEstimate() != null
                        && templateAction.getNextTimeEstimate().intValue() > 0) {
                    timeMap.put(day, Integer
                            .valueOf(timeMap.get(day).intValue() + templateAction.getNextTimeEstimate().intValue()));
                }
            }

            out.println("    <td class=\"boxed\">");
            String nextActionType = safe(templateAction.getNextActionType());
            if (nextActionType.equals("")) {
                nextActionType = ProjectNextActionType.WILL;
            }
            out.println("      <select name=\"" + NEXT_ACTION_TYPE + templateAction.getActionNextId() + "\">");
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

        out.println("  <tr>");
        out.println("    <td class=\"boxed\">");
        out.println("      <select name=\"" + PROJECT_ID + addFieldSuffix + "\">");
        for (Project project : orderedProjectList) {
            out.println(
                    "        <option value=\"" + project.getProjectId() + "\">" + escapeHtml(project.getProjectName())
                            + "</option>");
        }
        out.println("      </select>");
        out.println("    </td>");
        out.println("    <td class=\"boxed\"><input type=\"text\" name=\"" + NEXT_DESCRIPTION + addFieldSuffix
                + "\" value=\"\" size=\"30\"/></td>");

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

        for (Calendar day : dayList) {
            out.println("    <td class=\"boxed\">");
            if (!choreTable) {
                out.println(ProjectActionNext.getTimeForDisplay(timeMap.get(day).intValue()));
            }
            out.println("    </td>");
        }
        out.println("    <td class=\"boxed\">&nbsp;</td>");
        out.println("  </tr>");

        out.println("</table>");
        out.println("<br/>");
    }

    private void printDailyAssignments(PrintWriter out, WebUser dependentUser, Session dataSession,
            int dependencyId) {
        Date rangeStart = dependentUser
                .startOfDay(dependentUser.addDays(dependentUser.getToday(), -ASSIGNMENT_PAST_DAYS));
        Date rangeEnd = dependentUser.endOfDay(dependentUser.addDays(dependentUser.getToday(), ASSIGNMENT_FUTURE_DAYS));

        Query query = dataSession.createQuery(
                "from ProjectActionNext where provider = :provider and contactId = :contactId "
                        + "and nextDescription <> '' "
                        + "and nextActionDate >= :rangeStart and nextActionDate <= :rangeEnd "
                        + "and nextActionStatusString in (:readyStatus, :completedStatus, :cancelledStatus)");
        query.setParameter("provider", dependentUser.getProvider());
        query.setParameter("contactId", dependentUser.getContactId());
        query.setParameter("rangeStart", rangeStart);
        query.setParameter("rangeEnd", rangeEnd);
        query.setParameter("readyStatus", ProjectNextActionStatus.READY.getId());
        query.setParameter("completedStatus", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("cancelledStatus", ProjectNextActionStatus.CANCELLED.getId());
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> assignmentList = query.list();

        Map<Integer, Project> projectMap = new HashMap<Integer, Project>();
        for (ProjectActionNext action : assignmentList) {
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

        Map<String, List<ProjectActionNext>> byDay = new HashMap<String, List<ProjectActionNext>>();
        List<Date> dayOrder = new ArrayList<Date>();
        Calendar day = dependentUser.getCalendar(rangeStart);
        while (!day.getTime().after(rangeEnd)) {
            Date dayDate = day.getTime();
            dayOrder.add(dayDate);
            byDay.put(toDayKey(dayDate), new ArrayList<ProjectActionNext>());
            day.add(Calendar.DAY_OF_MONTH, 1);
        }

        for (ProjectActionNext action : assignmentList) {
            String key = toDayKey(action.getNextActionDate());
            List<ProjectActionNext> rows = byDay.get(key);
            if (rows != null) {
                rows.add(action);
            }
        }

        Map<String, Integer> earnedPointsByDay = loadEarnedPointsByDay(dependentUser, dataSession, rangeStart,
                rangeEnd);

        for (Date dayDate : dayOrder) {
            String dayKey = toDayKey(dayDate);
            List<ProjectActionNext> rows = byDay.get(dayKey);
            if (rows == null || rows.isEmpty()) {
                continue;
            }

            out.println("<h3>"
                    + escapeHtml(dependentUser.getDateFormatService().formatDate(dayDate, dependentUser.getTimeZone()))
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
                ProjectActionNext action = rows.get(i);
                String projectName = action.getProject() == null ? "" : safe(action.getProject().getProjectName());
                String projectIcon = action.getProject() == null ? "" : safe(action.getProject().getProjectIcon());
                String projectLabel = projectIcon.equals("") ? projectName : (projectIcon + " " + projectName);

                int estimate = action.getNextTimeEstimate() == null ? 0 : action.getNextTimeEstimate().intValue();
                int actual = action.getNextTimeActual() == null ? 0 : action.getNextTimeActual().intValue();
                int points = action.getGamePoints() == null ? 0 : action.getGamePoints().intValue();
                totalEstimate += estimate;
                totalActual += actual;

                ProjectNextActionStatus status = action.getNextActionStatus();
                String statusLabel = "Active";
                if (status == ProjectNextActionStatus.COMPLETED) {
                    statusLabel = "Completed";
                } else if (status == ProjectNextActionStatus.CANCELLED) {
                    statusLabel = "Cancelled";
                }

                String sourceLabel = action.getTemplateActionNextId() == null ? "Ad Hoc" : "Template";

                String editUrl = "ProjectActionServlet?" + ProjectActionServlet.PARAM_COMPLETING_ACTION_NEXT_ID + "="
                        + action.getActionNextId() + "&editActionNextId=" + action.getActionNextId()
                        + "&projectId=" + action.getProjectId();
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
                out.println("    <td class=\"boxed\">" + ProjectActionNext.getTimeForDisplay(estimate) + "</td>");
                out.println("    <td class=\"boxed\">" + ProjectActionNext.getTimeForDisplay(actual) + "</td>");
                out.println("    <td class=\"boxed\">" + points + "</td>");
                out.println("    <td class=\"boxed\"><a href=\"" + editUrl + "\" class=\"button\">Edit</a></td>");
                out.println("  </tr>");
            }

            int earnedPoints = intValue(earnedPointsByDay.get(dayKey));
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\" colspan=\"5\"><strong>Totals</strong></td>");
            out.println("    <td class=\"boxed\"><strong>" + ProjectActionNext.getTimeForDisplay(totalEstimate)
                    + "</strong></td>");
            out.println("    <td class=\"boxed\"><strong>" + ProjectActionNext.getTimeForDisplay(totalActual)
                    + "</strong></td>");
            out.println("    <td class=\"boxed\"><strong>" + earnedPoints + "</strong></td>");
            out.println("    <td class=\"boxed\">&nbsp;</td>");
            out.println("  </tr>");
            out.println("</table>");
            out.println("<br/>");
        }
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

    private void sortAssignments(List<ProjectActionNext> actionList) {
        Collections.sort(actionList, new Comparator<ProjectActionNext>() {
            @Override
            public int compare(ProjectActionNext left, ProjectActionNext right) {
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
            String projectId = request.getParameter(projectIdParam);
            String description = request.getParameter(descriptionParam);
            String timeEstimate = request.getParameter(timeEstimateParam);
            String timeSlot = request.getParameter(timeSlotParam);
            String points = request.getParameter(pointsParam);
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
                return new String[] { projectId, description.trim(), timeEstimate.trim(), timeSlot.trim(),
                        points.trim() };
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
        Calendar calendar = Calendar.getInstance();
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
