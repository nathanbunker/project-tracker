/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class ProjectReviewServlet extends ClientServlet {

  public static enum Interval {
    WEEK("Week", 6),
    TWO_WEEKS("Two Weeks", 13),
    MONTH("Month", 26),
    TWO_MONTHS("Two Months", 60),
    FOUR_MONTHS("Four Months", 120),
    YEAR("Year", 360);

    private String description;
    private int days;

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public int getDays() {
      return days;
    }

    public void setDays(int days) {
      this.days = days;
    }

    private Interval(String description, int days) {
      this.description = description;
      this.days = days;
    }
  }

  public static String makeLabel(int days) {
    for (Interval interval : Interval.values()) {
      if (interval.getDays() == days) {
        return interval.getDescription();
      }
    }
    if (days == 0) {
      return "";
    } else if (days == 1) {
      return "One Day";
    } else if (days == 2) {
      return "Two Days";
    }
    return days + " Days";
  }

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
      String action = appReq.getAction();
      PrintWriter out = appReq.getOut();
      SimpleDateFormat sdf = webUser.getDateFormat();

      Query query;

      if (action != null) {
        if (action.equals("Update")) {
          int updateDue = Integer.parseInt(request.getParameter("updateDue"));
          int projectId = Integer.parseInt(request.getParameter("projectId"));
          query = dataSession.createQuery(
              "from ProjectContactAssigned where id.contactId = ? and id.projectId = ?");
          query.setParameter(0, webUser.getContactId());
          query.setParameter(1, projectId);
          @SuppressWarnings("unchecked")
          List<ProjectContactAssigned> list = query.list();
          if (list.size() > 0) {
            Transaction trans = dataSession.beginTransaction();
            list.get(0).setUpdateDue(updateDue);
            trans.commit();
          }
        }
      }
      appReq.setTitle("Projects");
      printHtmlHead(appReq);

      List<ProjectContactAssigned> projectContactAssignedList = appReq.getProjectContactAssignedList();

      if (projectContactAssignedList == null) {
        projectContactAssignedList = new ArrayList<ProjectContactAssigned>();
        appReq.setProjectContactAssignedList(projectContactAssignedList);
        List<Integer> projectIdList = appReq.getProjectIdList();

        for (int projectId : projectIdList) {
          query = dataSession.createQuery(
              "from ProjectContactAssigned where id.contactId = ? and id.projectId = ?");
          query.setParameter(0, webUser.getContactId());
          query.setParameter(1, projectId);
          @SuppressWarnings("unchecked")
          List<ProjectContactAssigned> list = query.list();
          if (list.size() > 0) {
            projectContactAssignedList.add(list.get(0));
          }
        }
        for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList) {
          query = dataSession.createQuery(
              "from ProjectAction where projectId = ? and contactId = ? order by actionDate desc");
          query.setParameter(0, projectContactAssigned.getId().getProjectId());
          query.setParameter(1, projectContactAssigned.getId().getContactId());
          @SuppressWarnings("unchecked")
          List<ProjectAction> projectActionList = query.list();
          if (projectActionList.size() > 0) {
            projectContactAssigned.setUpdateLast(projectActionList.get(0).getActionDate());
          }
        }
      }

      boolean haveUncategorized = false;

      for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList) {
        if (projectContactAssigned.getUpdateDue() == 0) {
          haveUncategorized = true;
          break;
        }
      }
      if (haveUncategorized) {
        out.println("<h2>Uncategorized</h2>");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Project</th>");
        out.println("    <th class=\"boxed\">Category</th>");
        out.println("    <th class=\"boxed\">Assignment</th>");
        out.println("  </tr>");

        for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList) {
          if (projectContactAssigned.getUpdateDue() == 0) {
            printRow(out, dataSession, projectContactAssigned);
          }
        }
        out.println("</table>");
      }

      out.println("<h2>Projects Overdue</h2>");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Category</th>");
      out.println("    <th class=\"boxed\">Update Every</th>");
      out.println("    <th class=\"boxed\">Last Update</th>");
      out.println("  </tr>");

      Calendar today = webUser.getCalendar();

      for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList) {
        if (projectContactAssigned.getUpdateDue() > 0) {
          boolean due = true;

          if (projectContactAssigned.getUpdateLast() != null) {
            Calendar dueDate = webUser.getCalendar();
            dueDate.setTime(projectContactAssigned.getUpdateLast());
            dueDate.add(Calendar.DAY_OF_MONTH, projectContactAssigned.getUpdateDue());
            due = today.after(dueDate);
          }
          if (due) {
            Project project = (Project) dataSession.get(Project.class,
                projectContactAssigned.getId().getProjectId());
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId="
                + project.getProjectId() + "\" class=\"button\">" + project.getProjectName()
                + "</a></td>");
            out.println("    <td class=\"boxed\">" + (project.getProjectCategory() != null
                ? project.getProjectCategory().getClientName()
                : "") + "</td>");
            out.println("    <td class=\"boxed\">"
                + makeLabel(projectContactAssigned.getUpdateDue()) + "</td>");
            out.println("    <td class=\"boxed\">" + (projectContactAssigned.getUpdateLast() != null
                ? sdf.format(projectContactAssigned.getUpdateLast())
                : "") + "</td>");
            out.println("  </tr>");
          }
        }
      }
      out.println("</table>");

      int lastIntervalDays = 0;

      for (Interval interval : Interval.values()) {

        out.println("<h2>Update every " + interval.getDescription() + "</h2>");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Project</th>");
        out.println("    <th class=\"boxed\">Category</th>");
        out.println("    <th class=\"boxed\">Assignment</th>");
        out.println("  </tr>");

        for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList) {
          if (projectContactAssigned.getUpdateDue() > lastIntervalDays
              && projectContactAssigned.getUpdateDue() <= interval.getDays()) {
            printRow(out, dataSession, projectContactAssigned);
          }
        }
        out.println("</table>");
        lastIntervalDays = interval.getDays();
      }

      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  private void printRow(PrintWriter out, Session dataSession,
      ProjectContactAssigned projectContactAssigned) {
    Project project = (Project) dataSession.get(Project.class, projectContactAssigned.getId().getProjectId());
    ProjectsServlet.loadProjectsObject(dataSession, project);
    out.println("  <tr class=\"boxed\">");
    out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId="
        + project.getProjectId() + "\" class=\"button\">" + project.getProjectName() + "</a></td>");
    out.println("    <td class=\"boxed\">"
        + (project.getProjectCategory() != null ? project.getProjectCategory().getClientName() : "")
        + "</td>");
    out.println("    <td class=\"boxed\">");
    String link = "ProjectReviewServlet?action=Update&projectId="
        + projectContactAssigned.getId().getProjectId() + "&updateDue=";
    for (Interval interval : Interval.values()) {
      out.println("      <a href=\"" + link + interval.getDays() + "\" class=\"button\">"
          + interval.getDescription() + "</a>");
    }
    out.println("    </td>");
    out.println("  </tr>");
  }

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
