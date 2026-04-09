/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class ProjectsServlet extends ClientServlet {

  private static final String PARAM_SEARCH_TEXT = "searchText";
  private static final String PARAM_PROJECT_STATUS = "projectStatus";
  private static final String PARAM_SEARCH_FIELD = "searchField";
  private static final String DESCRIPTION = "Description";
  private static final String PROJECT_NAME = "Project Name";
  private static final String NOT_CLOSED = "NOT_CLOSED";
  private static final String ALL = "ALL";

  public static final String ACTION_UP = "up";
  public static final String ACTION_DOWN = "down";
  public static final String PARAM_PROJECT_ID = "projectId";

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   * 
   * @param request
   *                 servlet request
   * @param response
   *                 servlet response
   * @throws ServletException
   *                          if a servlet-specific error occurs
   * @throws IOException
   *                          if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    AppReq appReq = new AppReq(request, response);
    try {
      WebUser webUser = appReq.getWebUser();
      if (appReq.isLoggedOut()) {
        forwardToHome(request, response);
        return;
      }
      Session dataSession = appReq.getDataSession();
      PrintWriter out = appReq.getOut();

      appReq.setTitle("Projects");
      printHtmlHead(appReq);
      printDandelionLocation(out, "Setup / Projects");

      String projectStatus = n(request.getParameter(PARAM_PROJECT_STATUS), NOT_CLOSED);
      String searchField = n(request.getParameter(PARAM_SEARCH_FIELD), PROJECT_NAME);
      String searchText = n(request.getParameter(PARAM_SEARCH_TEXT));

      String action = request.getParameter(PARAM_ACTION);

      if (action != null) {
        if (action.equals(ACTION_UP) || action.equals(ACTION_DOWN)) {
          List<Project> projectList = createProjectList(request, webUser, dataSession, appReq.getActiveWorkspaceId(),
              projectStatus, searchField, searchText);
          Integer projectId = Integer.parseInt(request.getParameter(PARAM_PROJECT_ID));
          Project project = (Project) dataSession.get(Project.class, projectId);
          if (action.equals(ACTION_DOWN)) {
            boolean next = false;
            for (int i = 0; i < projectList.size(); i++) {
              Project p = projectList.get(i);
              int priorityLevel = projectList.size() - i;
              if (p.getProjectId() == project.getProjectId()) {
                next = true;
                p.setPriorityLevel(priorityLevel - 1);
              } else if (next) {
                p.setPriorityLevel(priorityLevel + 1);
                next = false;
              } else {
                p.setPriorityLevel(priorityLevel);
              }
              updateProject(dataSession, p);
            }
          } else if (action.equals(ACTION_UP)) {
            boolean next = false;
            for (int i = projectList.size() - 1; i >= 0; i--) {
              Project p = projectList.get(i);
              int priorityLevel = projectList.size() - i;
              if (p.getProjectId() == project.getProjectId()) {
                next = true;
                p.setPriorityLevel(priorityLevel + 1);
              } else if (next) {
                p.setPriorityLevel(priorityLevel - 1);
                next = false;
              } else {
                p.setPriorityLevel(priorityLevel);
              }
              updateProject(dataSession, p);
            }
          }

        }
      }

      out.println("<form action=\"ProjectsServlet\" method=\"GET\">");
      out.println("Search ");
      out.println("<select name=\"searchField\">");
      out.println("  <option value=\"" + PROJECT_NAME + "\""
          + (searchField.equals(PROJECT_NAME) ? " selected" : "") + ">Project Name</option>");
      out.println("  <option value=\"" + DESCRIPTION + "\""
          + (searchField.equals(DESCRIPTION) ? " selected" : "") + ">Description</option>");
      out.println("</select>");
      out.println("for <input type=\"text\" name=\"searchText\" value=\"" + searchText
          + "\" size=\"15\" >");
      out.println("<br>");
      out.println("Status ");
      out.println("<select name=\"projectStatus\">");
      if (projectStatus.equals(NOT_CLOSED)) {
        out.println("  <option value=\"NOT_CLOSED\" selected>ALL - not closed</option>");
      } else {
        out.println("  <option value=\"NOT_CLOSED\">ALL - not closed</option>");
      }
      if (projectStatus.equals(ALL)) {
        out.println("  <option value=\"ALL\" selected>ALL</option>");
      } else {
        out.println("  <option value=\"ALL\">ALL</option>");
      }
      for (ProjectStatus status : ProjectStatus.values()) {
        if (projectStatus.equals(status.getDatabaseValue())) {
          out.println("  <option value=\"" + status.getDatabaseValue() + "\" selected>"
              + status.getDatabaseValue() + "</option>");
        } else {
          out.println("  <option value=\"" + status.getDatabaseValue() + "\">"
              + status.getDatabaseValue() + "</option>");
        }
      }
      out.println("</select>");
      out.println("<input type=\"submit\" name=\"action\" value=\"Search\" >");
      out.println("</form>");

      Integer workspaceId = appReq.getActiveWorkspaceId();
      List<Project> projectList = createProjectList(request, webUser, dataSession, workspaceId,
          projectStatus, searchField, searchText);
      List<Integer> projectIdList = new ArrayList<Integer>();
      appReq.setProjectIdList(projectIdList);
      appReq.setProjectContactAssignedList(null);

      boolean showPriorityColumn = searchText.equals("") && projectStatus.equals(NOT_CLOSED);

      printProjectSearchResults(out, dataSession, projectStatus, searchField, showPriorityColumn,
          projectList, projectIdList);
      out.println("<h2>Create a New Project</h2>");
      out.println(
          "<p>If you do not see your project in the list above you can <a href=\"ProjectEditServlet\">create</a> one.</p>");
      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  private void updateProject(Session dataSession, Project p) {
    Transaction transaction = dataSession.beginTransaction();
    dataSession.update(p);
    transaction.commit();
  }

  private void printProjectSearchResults(PrintWriter out, Session dataSession, String projectStatus,
      String searchField, boolean showPriority, List<Project> projectList,
      List<Integer> projectIdList) throws UnsupportedEncodingException {
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Project</th>");
    out.println("    <th class=\"boxed\">Status</th>");

    out.println("    <th class=\"boxed\">Priority</th>");
    out.println("  </tr>");
    int position = 0;
    for (Project project : projectList) {
      position++;
      projectIdList.add(project.getProjectId());
      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId()
              + "\" class=\"button\">" + project.getProjectName()
              + (project.getProjectHandle() == null || project.getProjectHandle().equals("")
                  ? ""
                  : " [" + project.getProjectHandle() + "]")
              + "</a></td>");
      out.println("    <td class=\"boxed\">" + n(project.getProjectStatus()) + "</td>");
      out.println("    <td>");
      String link = "ProjectsServlet?" + PARAM_SEARCH_FIELD + "="
          + URLEncoder.encode(searchField, "UTF-8") + "&" + PARAM_PROJECT_STATUS + "=" + projectStatus + "&"
          + PARAM_PROJECT_ID + "="
          + project.getProjectId() + "&" + PARAM_ACTION + "=";
      if (position < projectList.size()) {
        out.println("<a href=\"" + link + ACTION_DOWN + "\" class=\"button\">&#8595; down</a>");
      }
      if (position > 1) {
        out.println("<a href=\"" + link + ACTION_UP + "\" class=\"button\">&#8593; up</a>");
      }
      out.println("    </td>");
      out.println("  </tr>");
    }
    out.println("</table>");
  }

  private List<Project> createProjectList(HttpServletRequest request, WebUser webUser,
      Session dataSession, Integer workspaceId, String projectStatus, String searchField,
      String searchText) {
    Query query;
    {
      String queryString = "from Project where workspaceId = :workspaceId";
      if (!searchText.equals("")) {
        if (searchField.equals(PROJECT_NAME)) {
          queryString += " and projectName like ?";
        } else if (searchField.equals(DESCRIPTION)) {
          queryString += " and description like ?";
        } else {
          queryString += " and projectName like ?";
        }
      }
      if (!projectStatus.equals("") && !projectStatus.equals(ALL)) {
        if (projectStatus.equals(NOT_CLOSED)) {
          queryString += " and (projectStatus is null or projectStatus <> :closedStatus)";
        } else {
          queryString += " and projectStatus = :projectStatus";
        }
      }
      queryString += " order by priorityLevel desc, projectName";
      query = dataSession.createQuery(queryString);
      query.setParameter("workspaceId", workspaceId);
      int i = -1;
      if (!searchText.equals("")) {
        i++;
        query.setParameter(i, searchText + "%");
      }
      if (!projectStatus.equals("") && !projectStatus.equals(ALL)) {
        if (projectStatus.equals(NOT_CLOSED)) {
          query.setParameter("closedStatus", ProjectStatus.CLOSED.getDatabaseValue());
        } else {
          query.setParameter("projectStatus", projectStatus);
        }
      }
    }

    @SuppressWarnings("unchecked")
    List<Project> projectList = query.list();
    return projectList;
  }

  protected static void loadProjectsObject(Session dataSession, Project project) {
    // No-op after hard cutover to status/tags model.
  }

  // <editor-fold defaultstate="collapsed"
  // desc="HttpServlet methods. Click on the + sign on the left to edit the
  // code.">

  /**
   * Handles the HTTP <code>GET</code> method.
   * 
   * @param request
   *                 servlet request
   * @param response
   *                 servlet response
   * @throws ServletException
   *                          if a servlet-specific error occurs
   * @throws IOException
   *                          if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   * 
   * @param request
   *                 servlet request
   * @param response
   *                 servlet response
   * @throws ServletException
   *                          if a servlet-specific error occurs
   * @throws IOException
   *                          if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

}
