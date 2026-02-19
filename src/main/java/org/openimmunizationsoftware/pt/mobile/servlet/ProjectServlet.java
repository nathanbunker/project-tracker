package org.openimmunizationsoftware.pt.mobile.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
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

            // Handle action processing (Complete/Tomorrow) - works for both GET and POST
            String paramAction = request.getParameter(PARAM_ACTION);
            Integer actionId = parseInteger(request.getParameter(PARAM_ACTION_ID));
            if (paramAction != null && actionId != null) {
                try {
                    ProjectActionNext projectAction = (ProjectActionNext) dataSession.get(ProjectActionNext.class,
                            actionId);
                    if (projectAction != null) {
                        if (ACTION_COMPLETE.equals(paramAction)) {
                            completeAction(projectAction, dataSession);
                        } else if (ACTION_TOMORROW.equals(paramAction)) {
                            postponeToTomorrow(projectAction, dataSession, webUser);
                        }
                    }

                    // Redirect back to the project page
                    response.sendRedirect(buildRedirectUrl(request));
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            boolean showWork = isShowWork(request);
            boolean showPersonal = isShowPersonal(request);

            List<Project> projectList = getProjectList(webUser, dataSession, showWork, showPersonal);
            Integer selectedProjectId = parseInteger(request.getParameter(PARAM_PROJECT_ID));
            Project selectedProject = findProjectById(projectList, selectedProjectId);

            appReq.setTitle("Project");
            printHtmlHead(appReq, "Project");
            PrintWriter out = appReq.getOut();
            if (selectedProject == null) {
                out.println("<h1>Project</h1>");
                Map<Integer, Integer> todoCountMap = getTodayTodoCountByProject(webUser, dataSession, showWork,
                        showPersonal);
                printProjectList(out, projectList, todoCountMap);
            } else {
                printProjectDetail(out, selectedProject, fetchOpenActionsForProject(webUser, dataSession,
                        selectedProject.getProjectId()));
            }
            printHtmlFoot(appReq);
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

    private void printProjectDetail(PrintWriter out, Project project, List<ProjectActionNext> actions) {
        out.println("<h1>" + escapeHtml(project.getProjectName()) + "</h1>");
        out.println("<p><a href=\"project\" class=\"box\">All Projects</a> ");
        out.println("<a href=\"action?" + PARAM_PROJECT_ID + "=" + project.getProjectId()
                + "\" class=\"button\">Add Action</a></p>");
        printProjectActionList(out, actions, project.getProjectId());
    }

    private void printProjectActionList(PrintWriter out, List<ProjectActionNext> actions, int projectId) {
        if (actions.isEmpty()) {
            out.println("<table class=\"boxed-mobile\">");
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">No open items</td>");
            out.println("  </tr>");
            out.println("</table>");
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MM/dd/yyyy");
        out.println("<table class=\"boxed-mobile\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">To Do</th>");
        out.println("    <th class=\"boxed\" style=\"text-align:center;\">Complete</th>");
        out.println("    <th class=\"boxed\" style=\"text-align:center;\">Postpone</th>");
        out.println("    <th class=\"boxed\" style=\"text-align:center;\">Action</th>");
        out.println("  </tr>");

        for (ProjectActionNext action : actions) {
            String description = action.getNextDescriptionForDisplay(action.getContact());

            // Build todo detail link
            String viewUrl = "todo?viewActionId=" + action.getActionNextId();
            if (action.getNextActionDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                viewUrl += "&date=" + sdf.format(action.getNextActionDate());
            }

            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">");
            if (action.getNextActionDate() != null) {
                out.println("      <strong>" + dateFormat.format(action.getNextActionDate()) + ":</strong> ");
            }
            out.println("      <a href=\"" + viewUrl + "\" style=\"text-decoration: none; color: inherit;\">");
            out.println("        " + (description == null ? "" : description));
            out.println("      </a>");
            out.println("    </td>");

            // Complete column
            out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
            out.println("      <a href=\"project?" + PARAM_PROJECT_ID + "=" + projectId + "&" + PARAM_ACTION_ID
                    + "=" + action.getActionNextId() + "&" + PARAM_ACTION + "=" + ACTION_COMPLETE
                    + "\" class=\"action-icon\" title=\"Complete\">&#10004;</a>");
            out.println("    </td>");

            // Postpone column
            out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
            out.println("      <a href=\"project?" + PARAM_PROJECT_ID + "=" + projectId + "&" + PARAM_ACTION_ID
                    + "=" + action.getActionNextId() + "&" + PARAM_ACTION + "=" + ACTION_TOMORROW
                    + "\" class=\"action-icon\" title=\"Postpone\">&#8594;</a>");
            out.println("    </td>");

            // Edit column
            out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
            out.println("      <a href=\"action?actionNextId=" + action.getActionNextId()
                    + "\" class=\"action-icon\" title=\"Edit\">&#9998;</a>");
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

    private List<Project> getProjectList(WebUser webUser, Session dataSession, boolean showWork,
            boolean showPersonal) {
        String queryString = "from Project where provider = ?";
        queryString += " and phaseCode <> 'Clos'";
        queryString += " order by projectName";
        Query query = dataSession.createQuery(queryString);
        query.setParameter(0, webUser.getProvider());
        @SuppressWarnings("unchecked")
        List<Project> allProjects = query.list();

        List<Project> filteredProjects = new ArrayList<Project>();
        for (Project project : allProjects) {
            boolean billable = resolveBillable(dataSession, project);
            if ((billable && showWork) || (!billable && showPersonal)) {
                filteredProjects.add(project);
            }
        }
        return filteredProjects;
    }

    private Map<Integer, Integer> getTodayTodoCountByProject(WebUser webUser, Session dataSession, boolean showWork,
            boolean showPersonal) {
        Calendar cal = webUser.getCalendar();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date today = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrow = cal.getTime();

        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan " +
                        "left join fetch pan.project " +
                        "where pan.provider = :provider " +
                        "and (pan.contactId = :contactId or pan.nextContactId = :contactId) " +
                        "and pan.nextActionStatusString = :status " +
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
            boolean isWork = action.isBillable();
            if ((isWork && !showWork) || (!isWork && !showPersonal)) {
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

    private void completeAction(ProjectActionNext action, Session dataSession) {
        Transaction trans = dataSession.beginTransaction();
        try {
            action.setNextActionStatus(ProjectNextActionStatus.COMPLETED);
            action.setNextChangeDate(new Date());
            dataSession.saveOrUpdate(action);
            trans.commit();
        } catch (Exception e) {
            trans.rollback();
            throw e;
        }
    }

    private void postponeToTomorrow(ProjectActionNext action, Session dataSession, WebUser webUser) {
        Transaction trans = dataSession.beginTransaction();
        try {
            Date tomorrow = TimeTracker.createTomorrow(webUser).getTime();
            action.setNextActionDate(tomorrow);
            action.setNextChangeDate(new Date());
            dataSession.saveOrUpdate(action);
            trans.commit();
        } catch (Exception e) {
            trans.rollback();
            throw e;
        }
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
        BillCode billCode = (BillCode) dataSession.get(BillCode.class, project.getBillCode());
        return billCode != null && "Y".equalsIgnoreCase(billCode.getBillable());
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
