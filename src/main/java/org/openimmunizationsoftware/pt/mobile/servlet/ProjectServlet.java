package org.openimmunizationsoftware.pt.mobile.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * Mobile Project page placeholder.
 * 
 * @author nathan
 */
public class ProjectServlet extends MobileBaseServlet {

    private static final String PARAM_PROJECT_ID = "projectId";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ACTION_ID = "actionId";

    private static final String ACTION_COMPLETE = "complete";
    private static final String ACTION_CANCEL = "cancel";
    private static final String ACTION_RESCHEDULE = "reschedule";
    private static final String ACTION_TOMORROW = "tomorrow";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            WebUser webUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();

            // Handle action processing (Complete/Cancel/Reschedule) - works for both GET
            // and POST
            String paramAction = request.getParameter(PARAM_ACTION);
            Integer actionId = parseInteger(request.getParameter(PARAM_ACTION_ID));
            if (paramAction != null && actionId != null) {
                try {
                    ProjectActionNext projectAction = (ProjectActionNext) dataSession.get(ProjectActionNext.class,
                            actionId);
                    if (projectAction != null) {
                        if (ACTION_COMPLETE.equals(paramAction)) {
                            completeAction(projectAction, dataSession, webUser);
                        } else if (ACTION_CANCEL.equals(paramAction)) {
                            cancelAction(projectAction, dataSession, webUser);
                        } else if (ACTION_RESCHEDULE.equals(paramAction)) {
                            rescheduleAction(projectAction, request, webUser, dataSession);
                        } else if (ACTION_TOMORROW.equals(paramAction)) {
                            postponeToTomorrow(projectAction, dataSession, webUser);
                        }
                    }

                    // Redirect back to the project page
                    response.sendRedirect(buildRedirectUrl(request));
                    return;
                } catch (Exception e) {
                    handleUnexpectedError(response, e);
                    return;
                }
            }

            List<Project> projectList = getProjectList(webUser, dataSession);
            Integer selectedProjectId = parseInteger(request.getParameter(PARAM_PROJECT_ID));
            Project selectedProject = findProjectById(projectList, selectedProjectId);

            appReq.setTitle("Project");
            printHtmlHead(appReq, "Project");
            PrintWriter out = appReq.getOut();
            if (selectedProject == null) {
                out.println("<h1>Project</h1>");
                Map<Integer, Integer> todoCountMap = getTodayTodoCountByProject(webUser, dataSession);
                printProjectList(out, projectList, todoCountMap);
            } else {
                printProjectDetail(out, selectedProject, fetchOpenActionsForProject(webUser, dataSession,
                        selectedProject.getProjectId()), webUser);
            }
            printHtmlFoot(appReq);
        } catch (Exception e) {
            handleUnexpectedError(response, e);
        } finally {
            appReq.close();
        }
    }

    private void handleUnexpectedError(HttpServletResponse response, Exception e) throws IOException {
        e.printStackTrace();
        if (!response.isCommitted()) {
            response.sendRedirect("oops");
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

    private void printProjectList(PrintWriter out, List<Project> projectList, Map<Integer, Integer> todoCountMap) {
        out.println("<table class=\"boxed-mobile\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Project</th>");
        out.println("    <th class=\"boxed\">To Do Today</th>");
        out.println("  </tr>");

        if (projectList.isEmpty()) {
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\" colspan=\"2\">No projects found.</td>");
            out.println("  </tr>");
        } else {
            for (Project project : projectList) {
                int todoCount = todoCountMap.containsKey(project.getProjectId())
                        ? todoCountMap.get(project.getProjectId())
                        : 0;
                out.println("  <tr class=\"boxed\">");
                out.println("    <td class=\"boxed\"><a href=\"project?" + PARAM_PROJECT_ID + "="
                        + project.getProjectId() + "\" class=\"button\">" + escapeHtml(project.getProjectName())
                        + "</a></td>");
                out.println("    <td class=\"boxed\">" + todoCount + "</td>");
                out.println("  </tr>");
            }
        }
        out.println("</table>");
    }

    private void printProjectDetail(PrintWriter out, Project project, List<ProjectActionNext> actions,
            WebUser webUser) {
        out.println("<h1>" + escapeHtml(project.getProjectName()) + "</h1>");
        out.println("<p><a href=\"project\" class=\"box\">All Projects</a> ");
        out.println("<a href=\"action?" + PARAM_PROJECT_ID + "=" + project.getProjectId()
                + "\" class=\"button\">Add Action</a></p>");
        printProjectActionList(out, actions, project.getProjectId(), webUser);
    }

    private void printProjectActionList(PrintWriter out, List<ProjectActionNext> actions, int projectId,
            WebUser webUser) {
        if (actions.isEmpty()) {
            out.println("<table class=\"boxed-mobile\">");
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">No open items</td>");
            out.println("  </tr>");
            out.println("</table>");
            return;
        }

        Date todayStart = webUser.getToday();
        Date tomorrowStart = webUser.getTomorrow();

        List<ProjectActionNext> overdueActions = new ArrayList<ProjectActionNext>();
        List<ProjectActionNext> dueTodayActions = new ArrayList<ProjectActionNext>();
        List<ProjectActionNext> dueLaterActions = new ArrayList<ProjectActionNext>();
        List<ProjectActionNext> unscheduledActions = new ArrayList<ProjectActionNext>();

        for (ProjectActionNext action : actions) {
            Date nextActionDate = action.getNextActionDate();
            if (nextActionDate == null) {
                unscheduledActions.add(action);
            } else {
                Date actionDay = webUser.startOfDay(nextActionDate);
                if (actionDay.before(todayStart)) {
                    overdueActions.add(action);
                } else if (actionDay.before(tomorrowStart)) {
                    dueTodayActions.add(action);
                } else {
                    dueLaterActions.add(action);
                }
            }
        }

        printProjectActionTable(out, "Overdue", overdueActions, projectId, webUser);
        printProjectActionTable(out, "Due Today", dueTodayActions, projectId, webUser);
        printProjectActionTable(out, "Due Later", dueLaterActions, projectId, webUser);
        printProjectActionTable(out, "Unscheduled", unscheduledActions, projectId, webUser);
    }

    private void printProjectActionTable(PrintWriter out, String tableTitle, List<ProjectActionNext> actions,
            int projectId, WebUser webUser) {
        if (actions.isEmpty()) {
            return;
        }

        out.println("<h2>" + tableTitle + "</h2>");
        out.println("<table class=\"boxed-mobile\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">To Do</th>");
        out.println("    <th class=\"boxed\" style=\"text-align:center;\">Action</th>");
        out.println("  </tr>");

        for (ProjectActionNext action : actions) {
            String description = action.getNextDescriptionForDisplay(action.getContact());

            // Build todo detail link
            String viewUrl = "action?viewActionId=" + action.getActionNextId();
            if (action.getNextActionDate() != null) {
                viewUrl += "&date="
                        + webUser.getDateFormatService().formatTransportDate(action.getNextActionDate(),
                                webUser.getTimeZone());
            }

            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">");
            if (action.getNextActionDate() != null) {
                String todoDateUrl = "todo?date=" + webUser.getDateFormatService()
                        .formatTransportDate(action.getNextActionDate(), webUser.getTimeZone());
                out.println("      <strong><a href=\"" + todoDateUrl + "\" style=\"text-decoration: none;\">"
                        + webUser.getDateFormatService().formatPattern(action.getNextActionDate(),
                                webUser.getDateDisplayPatternWithWeekdayShort(), webUser.getTimeZone())
                        + "</a>:</strong> ");
            }
            out.println("      <a href=\"" + viewUrl + "\" style=\"text-decoration: none; color: inherit;\">");
            out.println("        " + (description == null ? "" : description));
            out.println("      </a>");
            out.println("    </td>");

            // Action column (complete + postpone + cancel)
            String completeUrl = "project?" + PARAM_PROJECT_ID + "=" + projectId + "&" + PARAM_ACTION_ID
                    + "=" + action.getActionNextId() + "&" + PARAM_ACTION + "=" + ACTION_COMPLETE;
            String cancelUrl = "project?" + PARAM_PROJECT_ID + "=" + projectId + "&" + PARAM_ACTION_ID
                    + "=" + action.getActionNextId() + "&" + PARAM_ACTION + "=" + ACTION_CANCEL;

            out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
            out.println("      <span style=\"white-space: nowrap;\">");
            out.println("        <a href=\"" + completeUrl
                    + "\" class=\"action-icon\" title=\"Complete\" style=\"margin-right: 8px;\">&#10004;</a>");
            out.println("        <a href=\"javascript:void(0);\" onclick=\"showPostponeMenu(" + action.getActionNextId()
                    + "); return false;\" class=\"action-icon\" title=\"Postpone/Reschedule\" style=\"margin-right: 8px;\">&#8594;</a>");
            out.println("        <a href=\"" + cancelUrl
                    + "\" class=\"action-icon\" title=\"Cancel\" style=\"margin-right: 8px;\">&#10006;</a>");
            out.println("      </span>");

            // Print the shared postpone menu with project context
            printPostponeMenuForProject(out, action, "", webUser, projectId);

            out.println("    </td>");
            out.println("  </tr>");
        }
        out.println("</table>");
    }

    private Project findProjectById(List<Project> projectList, Integer selectedProjectId) {
        if (selectedProjectId == null) {
            return null;
        }
        for (Project project : projectList) {
            if (project.getProjectId() == selectedProjectId.intValue()) {
                return project;
            }
        }
        return null;
    }

    private List<ProjectActionNext> fetchOpenActionsForProject(WebUser webUser, Session dataSession, int projectId) {
        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan " +
                        "left join fetch pan.project " +
                        "left join fetch pan.contact " +
                        "left join fetch pan.nextProjectContact " +
                        "where pan.provider = :provider " +
                        "and (pan.contactId = :contactId or pan.nextContactId = :contactId) " +
                        "and pan.projectId = :projectId " +
                        "and pan.nextActionStatusString = :status " +
                        "and (pan.templateTypeString is null or pan.templateTypeString = '') " +
                        "and pan.nextDescription <> '' " +
                        "order by pan.nextActionDate, pan.priorityLevel DESC, pan.nextChangeDate");
        query.setParameter("provider", webUser.getProvider());
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("projectId", projectId);
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> results = query.list();
        return results;
    }

    private List<Project> getProjectList(WebUser webUser, Session dataSession) {
        String queryString = "from Project where provider = ?";
        queryString += " and phaseCode <> 'Clos'";
        queryString += " order by projectName";
        Query query = dataSession.createQuery(queryString);
        query.setParameter(0, webUser.getProvider());
        @SuppressWarnings("unchecked")
        List<Project> allProjects = query.list();

        List<Project> filteredProjects = new ArrayList<Project>();
        for (Project project : allProjects) {
            if (!resolveBillable(dataSession, project)) {
                filteredProjects.add(project);
            }
        }
        return filteredProjects;
    }

    private Map<Integer, Integer> getTodayTodoCountByProject(WebUser webUser, Session dataSession) {
        Date today = webUser.getToday();
        Date tomorrow = webUser.getTomorrow();

        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan " +
                        "left join fetch pan.project " +
                        "where pan.provider = :provider " +
                        "and (pan.contactId = :contactId or pan.nextContactId = :contactId) " +
                        "and pan.nextActionStatusString = :status " +
                        "and (pan.templateTypeString is null or pan.templateTypeString = '') " +
                        "and pan.nextDescription <> '' " +
                        "and pan.nextActionDate >= :today and pan.nextActionDate < :tomorrow");
        query.setParameter("provider", webUser.getProvider());
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        query.setParameter("today", today);
        query.setParameter("tomorrow", tomorrow);

        @SuppressWarnings("unchecked")
        List<ProjectActionNext> actions = query.list();

        Map<Integer, Integer> countMap = new HashMap<Integer, Integer>();
        for (ProjectActionNext action : actions) {
            if (action.isBillable()) {
                continue;
            }
            Integer projectId = action.getProjectId();
            if (projectId == null || projectId.intValue() <= 0) {
                continue;
            }
            Integer count = countMap.get(projectId);
            countMap.put(projectId, count == null ? 1 : count + 1);
        }
        return countMap;
    }

    private String buildRedirectUrl(HttpServletRequest request) {
        Integer projectId = parseInteger(request.getParameter(PARAM_PROJECT_ID));
        if (projectId != null) {
            return "project?" + PARAM_PROJECT_ID + "=" + projectId;
        }
        return "project";
    }

    private Integer parseInteger(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private boolean resolveBillable(Session dataSession, Project project) {
        if (project == null || project.getBillCode() == null || project.getBillCode().equals("")) {
            return false;
        }
        BillCode billCode = resolveBillCode(dataSession, project);
        return billCode != null && "Y".equalsIgnoreCase(billCode.getBillable());
    }
}
