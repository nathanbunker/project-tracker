/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.ProjectGoalStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class GoalReviewServlet extends ClientServlet {

  public static String ACTION_UPDATE_PRIORITY = "Update Priority";
  public static String ACTION_SAVE = "Save";

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   * 
   * @param request
   *          servlet request
   * @param response
   *          servlet response
   * @throws ServletException
   *           if a servlet-specific error occurs
   * @throws IOException
   *           if an I/O error occurs
   */
  @SuppressWarnings("unchecked")
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



      List<ProjectAction> projectActionGoalList = getProjectActionGoalList(dataSession);
      List<Project> projectNeedUpdateList = new ArrayList<Project>();

      if (action == null) {
        Transaction transaction = dataSession.beginTransaction();
        Calendar calendar = Calendar.getInstance();
        boolean splitPriorities = true;
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
          splitPriorities = false;
        }
        calendar.add(Calendar.HOUR, -12);
        Date priorityHigh = calendar.getTime();
        calendar.add(Calendar.HOUR, -24);
        Date priorityMedium = calendar.getTime();
        calendar.add(Calendar.HOUR, -72);
        Date priorityLow = calendar.getTime();
        int count = 0;
        for (ProjectAction projectAction : projectActionGoalList) {
          count++;
          Project project = (Project) dataSession.get(Project.class, projectAction.getProjectId());
          if (projectNeedUpdateList.contains(project)) {
            continue;
          }
          if (projectAction.getGoalStatus() == null || projectAction.getGoalStatus().equals("")) {
            projectNeedUpdateList.add(project);
          } else {
            Date priority = priorityHigh;
            if (splitPriorities && count > 3) {
              priority = priorityMedium;
              if (count > 7) {
                priority = priorityLow;
              }
            }
            Query query = dataSession.createQuery(
                "from ProjectAction where projectId = :projectId and actionDate >= :actionDate");
            query.setParameter("projectId", projectAction.getProjectId());
            query.setParameter("actionDate", priority);
            List<ProjectAction> projectActionList = query.list();
            if (projectActionList.size() == 0) {
              projectNeedUpdateList.add(project);
              projectAction.setGoalStatus(null);
              dataSession.update(projectAction);
            }
          }
        }
        transaction.commit();
      } else {
        if (action.equals(ACTION_UPDATE_PRIORITY)) {
          Transaction transaction = dataSession.beginTransaction();
          for (ProjectAction projectAction : projectActionGoalList) {
            projectAction.setPriorityLevel(Integer
                .parseInt(request.getParameter("priorityLevel" + projectAction.getActionId())));
            projectAction
                .setGoalStatus(request.getParameter("goalStatus" + projectAction.getActionId()));
            Date nextDue = null;
            String s = request.getParameter("nextDue" + projectAction.getActionId());
            if (!s.equals("")) {
              try {
                nextDue = sdf.parse(s);
              } catch (ParseException e) {
                nextDue = projectAction.getNextDue();
              }
            }
            projectAction.setNextDue(nextDue);
            dataSession.update(projectAction);
          }
          transaction.commit();
          projectActionGoalList = getProjectActionGoalList(dataSession);
        } else if (action.equals(ACTION_SAVE)) {
          int projectId = Integer.parseInt(request.getParameter(ProjectServlet.PARAM_PROJECT_ID));
          Project project = ProjectServlet.setupProject(dataSession, projectId);
          ProjectServlet.saveProjectAction(appReq, project, "");
          Transaction transaction = dataSession.beginTransaction();
          for (ProjectAction projectAction : projectActionGoalList) {
            if (projectAction.getProjectId() == projectId) {
              projectAction
                  .setGoalStatus(request.getParameter("goalStatus" + projectAction.getActionId()));
              Date nextDue = null;
              String s = request.getParameter("nextDue" + projectAction.getActionId());
              if (!s.equals("")) {
                try {
                  nextDue = sdf.parse(s);
                } catch (ParseException e) {
                  nextDue = projectAction.getNextDue();
                }
              }
              projectAction.setNextDue(nextDue);
              dataSession.update(projectAction);
            }
          }
          transaction.commit();
          projectActionGoalList = getProjectActionGoalList(dataSession);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -48);
        for (ProjectAction projectAction : projectActionGoalList) {
          Project project = (Project) dataSession.get(Project.class, projectAction.getProjectId());
          if (projectNeedUpdateList.contains(project)) {
            continue;
          }
          if (projectAction.getGoalStatus() == null || projectAction.getGoalStatus().equals("")) {
            projectNeedUpdateList.add(project);
          } else {
            Query query = dataSession.createQuery(
                "from ProjectAction where projectId = :projectId and actionDate >= :actionDate");
            query.setParameter("projectId", projectAction.getProjectId());
            query.setParameter("actionDate", calendar.getTime());
            List<ProjectAction> projectActionList = query.list();
            if (projectActionList.size() == 0) {
              projectNeedUpdateList.add(project);
            }
          }
        }
      }


      appReq.setTitle("Projects");
      printHtmlHead(appReq);

      out.println("<form action=\"GoalReviewServlet\" method=\"POST\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Priority</th>");
      out.println("    <th class=\"boxed\">Goal</th>");
      out.println("    <th class=\"boxed\">Status</th>");
      out.println("    <th class=\"boxed\">Due</th>");
      out.println("  </tr>");
      for (ProjectAction projectAction : projectActionGoalList) {
        projectAction
            .setProject((Project) dataSession.get(Project.class, projectAction.getProjectId()));
        Project project = projectAction.getProject();
        out.println("  <tr class=\"boxed\">");
        out.println(
            "    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId()
                + "\" class=\"button\">" + project.getProjectName() + "</a></td>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"priorityLevel" + projectAction.getActionId()
            + "\" size=\"2\" value=\"" + projectAction.getPriorityLevel() + "\"></input>");
        out.println("    </td>");
        out.println("    <td class=\"boxed\">");
        out.println(projectAction.getNextDescription());
        out.println("    </td>");
        out.println("    <td class=\"boxed\" style=\"background-color: "
            + ProjectGoalStatus.getColor(projectAction.getGoalStatus()) + "\">");
        {
          String id = "goalStatus" + projectAction.getActionId();
          String goalStatus = projectAction.getGoalStatus();
          if (goalStatus == null) {
            goalStatus = "";
          }
          out.println("      <select name=\"" + id + "\">");
          for (String ts : new String[] {ProjectGoalStatus.PROGRESSING, ProjectGoalStatus.DELAYED,
              ProjectGoalStatus.BLOCKED}) {
            out.println(
                "    <option value=\"" + ts + "\"" + (goalStatus.equals(ts) ? " selected" : "")
                    + ">" + ProjectGoalStatus.getLabel(ts) + "</option>");
          }
          out.println("      </select>");
        }
        out.println("    </td>");
        out.println("    <td class=\"boxed\">");
        {
          String id = "nextDue" + projectAction.getActionId();
          if (projectAction.getNextDue() == null) {
            out.println(
                "      <input type=\"text\" name=\"" + id + "\" size=\"10\" value=\"\"></input>");
          } else {
            out.println("      <input type=\"text\" name=\"" + id + "\" size=\"10\" value=\""
                + sdf.format(projectAction.getNextDue()) + "\"></input>");
          }
        }
        out.println("    </td>");
        out.println("  </tr>");
      }
      out.println("</table>");


      out.println(
          "<input type=\"submit\" name=\"action\" value=\"" + ACTION_UPDATE_PRIORITY + "\" >");
      out.println("</form>");

      out.println("<h2>Projects to Update</h2>");
      for (Project project : projectNeedUpdateList) {
        out.println("<h3>" + project.getProjectName() + "</h3>");
        out.println("<form name=\"projectAction" + project.getProjectId()
            + "\" method=\"post\" action=\"GoalReviewServlet\" id=\"saveProjectActionForm"
            + project.getProjectId() + "\">");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Goal</th>");
        out.println("    <th class=\"boxed\">Status</th>");
        out.println("    <th class=\"boxed\">Due</th>");
        out.println("  </tr>");
        for (ProjectAction projectAction : projectActionGoalList) {
          if (!project.equals(projectAction.getProject())) {
            continue;
          }
          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\">");
          out.println(projectAction.getNextDescription());
          out.println("    </td>");
          out.println("    <td class=\"boxed\" style=\"background-color: "
              + ProjectGoalStatus.getColor(projectAction.getGoalStatus()) + "\">");
          {
            String id = "goalStatus" + projectAction.getActionId();
            String goalStatus = projectAction.getGoalStatus();
            if (goalStatus == null) {
              goalStatus = "";
            }
            for (String ts : new String[] {ProjectGoalStatus.PROGRESSING,
                ProjectGoalStatus.DELAYED, ProjectGoalStatus.BLOCKED}) {
              out.println("      <input type=\"radio\" name=\"" + id + "\" id=\"" + id + ts
                  + "\" value=\"" + ts + "\"" + (goalStatus.equals(ts) ? " checked" : "")
                  + "></input> <label for=\"" + id + ts + "\">" + ProjectGoalStatus.getLabel(ts)
                  + "<label>");
            }
          }
          out.println("    </td>");
          out.println("    <td class=\"boxed\">");
          {
            String id = "nextDue" + projectAction.getActionId();
            if (projectAction.getNextDue() == null) {
              out.println(
                  "      <input type=\"text\" name=\"" + id + "\" size=\"10\" value=\"\"></input>");
            } else {
              out.println("      <input type=\"text\" name=\"" + id + "\" size=\"10\" value=\""
                  + sdf.format(projectAction.getNextDue()) + "\"></input>");
            }
          }
          out.println("    </td>");
          out.println("  </tr>");
        }
        out.println("</table>");
        out.println("<br/>");

        List<ProjectContactAssigned> projectContactAssignedList =
            ProjectServlet.getProjectContactAssignedList(dataSession, project.getProjectId());
        List<ProjectContact> projectContactList =
            ProjectServlet.getProjectContactList(dataSession, project, projectContactAssignedList);
        Collections.sort(projectContactAssignedList, new Comparator<ProjectContactAssigned>() {
          public int compare(ProjectContactAssigned arg0, ProjectContactAssigned arg1) {
            if (arg0.getProjectContact().getNameFirst()
                .equals(arg1.getProjectContact().getNameFirst())) {
              return arg0.getProjectContact().getNameLast()
                  .compareTo(arg1.getProjectContact().getNameLast());
            }
            return arg0.getProjectContact().getNameFirst()
                .compareTo(arg1.getProjectContact().getNameFirst());
          }
        });
        project.setProjectContactAssignedList(projectContactAssignedList);

        ProjectServlet.printProjectUpdateForm(appReq, project.getProjectId(), projectContactList,
            null, null);

        out.println("</form>");
      }

      out.println("<h3>Easy Copy Table</h3>");

      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Goal</th>");
      out.println("    <th class=\"boxed\">Status</th>");
      out.println("  </tr>");
      for (ProjectAction projectAction : projectActionGoalList) {
        projectAction
            .setProject((Project) dataSession.get(Project.class, projectAction.getProjectId()));
        Project project = projectAction.getProject();
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\">" + project.getProjectName() + "</td>");
        out.println("    <td class=\"boxed\">" + projectAction.getNextDescription() + "</td>");
        String goalStatus = projectAction.getGoalStatus();
        out.println("    <td class=\"boxed\" style=\"background-color: "
            + ProjectGoalStatus.getColor(goalStatus) + "\">");
        out.println(ProjectGoalStatus.getLabel(goalStatus));
        out.println("    </td>");
        out.println("  </tr>");
      }
      out.println("</table>");

      out.println("<h3>Easy Copy Text</h3>");

      for (ProjectAction projectAction : projectActionGoalList) {
        if (projectAction.getGoalStatus() == null) {
          out.println("    <p>" + projectAction.getNextDescription() + " &mdash; ?????</p>");
        } else if (projectAction.getGoalStatus().equals(ProjectGoalStatus.PROGRESSING)) {
          out.println("    <p>" + projectAction.getNextDescription() + "</p>");
        } else {
          out.println("    <p>" + projectAction.getNextDescription() + " &mdash; "
              + ProjectGoalStatus.getLabel(projectAction.getGoalStatus()) + "</p>");
        }
      }


      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  private List<ProjectAction> getProjectActionGoalList(Session dataSession) {
    List<ProjectAction> projectActionGoalList;
    {
      Query query = dataSession.createQuery("from ProjectAction where nextDescription <> '' "
          + "and nextActionId = 0 and nextActionType = :nextActionType order by priorityLevel desc, projectId, nextDue asc");
      query.setParameter("nextActionType", ProjectNextActionType.GOAL);
      projectActionGoalList = query.list();

    }
    return projectActionGoalList;
  }

  /**
   * Handles the HTTP <code>GET</code> method.
   * 
   * @param request
   *          servlet request
   * @param response
   *          servlet response
   * @throws ServletException
   *           if a servlet-specific error occurs
   * @throws IOException
   *           if an I/O error occurs
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
   *          servlet request
   * @param response
   *          servlet response
   * @throws ServletException
   *           if a servlet-specific error occurs
   * @throws IOException
   *           if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

}
