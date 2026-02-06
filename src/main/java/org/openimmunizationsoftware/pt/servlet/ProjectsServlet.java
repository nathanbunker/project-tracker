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
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.ProjectPhase;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class ProjectsServlet extends ClientServlet {

  private static final String PARAM_SEARCH_TEXT = "searchText";
  private static final String PARAM_CATEGORY_CODE = "categoryCode";
  private static final String PARAM_PHASE_CODE = "phaseCode";
  private static final String PARAM_SEARCH_FIELD = "searchField";
  private static final String DESCRIPTION = "Description";
  private static final String PROVIDER_NAME = "Provider Name";
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

      String categoryCode = n(request.getParameter(PARAM_CATEGORY_CODE));
      String phaseCode = n(request.getParameter(PARAM_PHASE_CODE), NOT_CLOSED);
      String searchField = n(request.getParameter(PARAM_SEARCH_FIELD), PROJECT_NAME);
      String searchText = n(request.getParameter(PARAM_SEARCH_TEXT));

      String action = request.getParameter(PARAM_ACTION);

      if (action != null) {
        if (action.equals(ACTION_UP) || action.equals(ACTION_DOWN)) {
          List<Project> projectList = createProjectList(request, webUser, dataSession, categoryCode,
              phaseCode, searchField, searchText);
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
      out.println("  <option value=\"" + PROVIDER_NAME + "\""
          + (searchField.equals(PROVIDER_NAME) ? " selected" : "") + ">Provider Name</option>");
      out.println("  <option value=\"" + DESCRIPTION + "\""
          + (searchField.equals(DESCRIPTION) ? " selected" : "") + ">Description</option>");
      out.println("</select>");
      out.println("for <input type=\"text\" name=\"searchText\" value=\"" + searchText
          + "\" size=\"15\" >");
      out.println("<br>");
      out.println("Limit by Category ");
      out.println("<select name=\"categoryCode\">");
      out.println("  <option value=\"\">ALL</option>");
      Query query = dataSession.createQuery(
          "from ProjectCategory where provider = :provider and visible = 'Y' order by sortOrder, clientName");
      query.setParameter("provider", webUser.getProvider());
      @SuppressWarnings("unchecked")
      List<ProjectCategory> projectCategoryList = query.list();
      for (ProjectCategory projectCategory : projectCategoryList) {
        if (projectCategory.getCategoryCode().startsWith("PER-")) {
          if (!projectCategory.getCategoryCode().equals("PER-" + webUser.getContactId())) {
            continue;
          }
        }
        if (categoryCode.equals(projectCategory.getCategoryCode())) {
          out.println("  <option value=\"" + projectCategory.getCategoryCode() + "\" selected>"
              + projectCategory.getClientNameForDropdown() + "</option>");
        } else {
          out.println("  <option value=\"" + projectCategory.getCategoryCode() + "\">"
              + projectCategory.getClientNameForDropdown() + "</option>");
        }
      }
      out.println("</select>");
      out.println("&nbsp; Phase ");
      out.println("<select name=\"phaseCode\">");
      if (phaseCode.equals(NOT_CLOSED)) {
        out.println("  <option value=\"NOT_CLOSED\" selected>ALL - not closed</option>");
      } else {
        out.println("  <option value=\"NOT_CLOSED\">ALL - not closed</option>");
      }
      if (phaseCode.equals(ALL)) {
        out.println("  <option value=\"ALL\" selected>ALL</option>");
      } else {
        out.println("  <option value=\"ALL\">ALL</option>");
      }
      query = dataSession.createQuery("from ProjectPhase order by phase_label");
      @SuppressWarnings("unchecked")
      List<ProjectPhase> projectPhaseList = query.list();
      for (ProjectPhase projectPhase : projectPhaseList) {
        if (phaseCode.equals(projectPhase.getPhaseCode())) {
          out.println("  <option value=\"" + projectPhase.getPhaseCode() + "\" selected>"
              + projectPhase.getPhaseLabel() + "</option>");
        } else {
          out.println("  <option value=\"" + projectPhase.getPhaseCode() + "\">"
              + projectPhase.getPhaseLabel() + "</option>");
        }
      }
      out.println("</select>");
      out.println("<input type=\"submit\" name=\"action\" value=\"Search\" >");
      out.println("</form>");

      List<Project> projectList = createProjectList(request, webUser, dataSession, categoryCode,
          phaseCode, searchField, searchText);
      List<Integer> projectIdList = new ArrayList<Integer>();
      appReq.setProjectIdList(projectIdList);
      appReq.setProjectContactAssignedList(null);

      boolean showPriorityColumn = searchText.equals("") && categoryCode.equals("") && phaseCode.equals(NOT_CLOSED);

      printProjectSearchResults(out, dataSession, phaseCode, searchField, showPriorityColumn,
          categoryCode, projectList, projectIdList);
      out.println("<h2>Create a New Project</h2>");
      out.println(
          "<p>If you do not see your project in the list above you can <a href=\"ProjectEditServlet\">create</a> one.</p>");
      out.println("<h2>Project Review</h2>");
      out.println(
          "<p>On a regular basis you should <a href=\"ProjectReviewServlet\">review your projects</a> to ensure that each is getting the attention it deserves. </p>");
      out.println("<h2>Schedule from Templates</h2>");
      out.println(
          "<p><a href=\"TemplateScheduleServlet\">Review all the templated</a> in the selected projects above and schedule next steps. </p>");
      out.println("<h2>Goal Review</h2>");
      out.println(
          "<p><a href=\"GoalReviewServlet\">Review all goals</a> in the selected projects above and indicate current status/p>");
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

  private void printProjectSearchResults(PrintWriter out, Session dataSession, String phaseCode,
      String searchField, boolean showPriority, String categoryCode, List<Project> projectList,
      List<Integer> projectIdList) throws UnsupportedEncodingException {
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Project</th>");
    out.println("    <th class=\"boxed\">Category</th>");
    out.println("    <th class=\"boxed\">Phase</th>");

    out.println("    <th class=\"boxed\">Priority</th>");
    out.println("  </tr>");
    int position = 0;
    for (Project project : projectList) {
      position++;
      projectIdList.add(project.getProjectId());
      loadProjectsObject(dataSession, project);
      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId()
              + "\" class=\"button\">" + project.getProjectName() + "</a></td>");
      out.println("    <td class=\"boxed\">"
          + (project.getProjectCategory() != null ? project.getProjectCategory().getClientName()
              : "")
          + "</td>");
      out.println("    <td class=\"boxed\">"
          + (project.getProjectPhase() != null ? project.getProjectPhase().getPhaseLabel() : "")
          + "</td>");
      out.println("    <td>");
      String link = "ProjectsServlet?" + PARAM_SEARCH_FIELD + "="
          + URLEncoder.encode(searchField, "UTF-8") + "&" + PARAM_PHASE_CODE + "=" + phaseCode + "&"
          + PARAM_CATEGORY_CODE + "=" + categoryCode + "&" + PARAM_PROJECT_ID + "="
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

  @SuppressWarnings("unchecked")
  private List<Project> createProjectList(HttpServletRequest request, WebUser webUser,
      Session dataSession, String categoryCode, String phaseCode, String searchField,
      String searchText) {
    Query query;
    List<Project> projectList;
    {
      String queryString = "from Project where provider = ?";
      if (!searchText.equals("")) {
        if (searchField.equals(PROJECT_NAME)) {
          queryString += " and projectName like ?";
        } else if (searchField.equals(PROVIDER_NAME)) {
          queryString += " and providerName like ?";
        } else if (searchField.equals(DESCRIPTION)) {
          queryString += " and description like ?";
        } else {
          queryString += " and projectName like ?";
        }
      }
      if (!phaseCode.equals("") && !phaseCode.equals(ALL)) {
        if (phaseCode.equals(NOT_CLOSED)) {
          queryString += " and phaseCode <> 'Clos'";
        } else {
          queryString += " and phaseCode = ?";
        }
      }
      if (!categoryCode.equals("")) {
        queryString += " and (categoryCode = ? or categoryCode LIKE ? )";
      }
      queryString += " order by priorityLevel desc, categoryCode, projectName";
      query = dataSession.createQuery(queryString);
      query.setParameter(0, webUser.getProvider());
      int i = 0;
      if (!searchText.equals("")) {
        i++;
        query.setParameter(i, searchText + "%");
      }
      if (!phaseCode.equals("") && !phaseCode.equals(NOT_CLOSED) && !phaseCode.equals(ALL)) {
        i++;
        query.setParameter(i, phaseCode);
      }
      if (!categoryCode.equals("")) {
        i++;
        query.setParameter(i, categoryCode);
        i++;
        query.setParameter(i, categoryCode + "-%");
      }
    }

    projectList = query.list();
    return projectList;
  }

  protected static void loadProjectsObject(Session dataSession, Project project) {
    Query query1 = dataSession.createQuery(
        "from ProjectCategory where categoryCode = :categoryCode and provider = :provider");
    query1.setParameter(PARAM_CATEGORY_CODE, project.getCategoryCode());
    query1.setParameter("provider", project.getProvider());
    @SuppressWarnings("unchecked")
    List<ProjectCategory> projectCategoryList = query1.list();
    project.setProjectCategory(projectCategoryList.size() > 0 ? projectCategoryList.get(0) : null);
    ProjectPhase projectPhase = (ProjectPhase) dataSession.get(ProjectPhase.class, project.getPhaseCode());
    project.setProjectPhase(projectPhase);
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
