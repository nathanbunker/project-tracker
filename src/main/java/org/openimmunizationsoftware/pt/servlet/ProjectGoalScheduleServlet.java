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
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class ProjectGoalScheduleServlet extends ClientServlet {

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



      List<Calendar> dayList = new ArrayList<Calendar>();
      {
        int count = 0;
        while (count < 7) {
          Calendar day = webUser.getCalendar();
          day.set(Calendar.HOUR_OF_DAY, 0);
          day.set(Calendar.MINUTE, 0);
          day.set(Calendar.SECOND, 0);
          day.set(Calendar.MILLISECOND, 0);
          day.add(Calendar.DAY_OF_MONTH, count);
          dayList.add(day);
          count++;
        }
      }
      List<Project> projectList = appReq.createProjectList();
      Map<ProjectAction, Map<Calendar, ProjectAction>> projectActionDayMap =
          new HashMap<ProjectAction, Map<Calendar, ProjectAction>>();
      Map<Project, List<ProjectAction>> projectActionGoalMap =
          new HashMap<Project, List<ProjectAction>>();
      {
        for (Project project : projectList) {
          List<ProjectAction> projectActionGoalList;
          {
            Query query = dataSession.createQuery(
                "from ProjectAction where projectId = :projectId and nextDescription <> '' "
                    + "and nextActionId = 0 and nextActionType = :nextActionType order by nextDue asc");
            query.setParameter("projectId", project.getProjectId());
            query.setParameter("nextActionType", ProjectNextActionType.GOAL);
            projectActionGoalList = query.list();
          }
          projectActionGoalMap.put(project, projectActionGoalList);
          for (ProjectAction projectActionGoal : projectActionGoalList) {
            Map<Calendar, ProjectAction> projectActionMap = new HashMap<Calendar, ProjectAction>();
            projectActionDayMap.put(projectActionGoal, projectActionMap);
            for (Calendar day : dayList) {
              Query query = dataSession.createQuery(
                  "from ProjectAction where projectId = :projectId and nextDescription <> '' "
                      + "and nextActionId = 0 and nextActionType <> :nextActionType1 "
                      + "and nextActionType <> :nextActionType2 "
                      + "and nextDue = :nextDue and nextDescription = :nextDescription "
                      + "order by nextDue asc");
              query.setParameter("projectId", project.getProjectId());
              query.setParameter("nextActionType1", ProjectNextActionType.GOAL);
              query.setParameter("nextActionType2", ProjectNextActionType.TASK);
              query.setParameter("nextDue", day.getTime());
              query.setParameter("nextDescription", projectActionGoal.getNextDescription());
              List<ProjectAction> pal = query.list();
              if (pal.size() > 0) {
                projectActionMap.put(day, pal.get(0));
              }
            }
          }
        }
      }

      if (request.getParameter("action") != null) {
        SimpleDateFormat sdfField = webUser.getDateFormat("yyyyMMdd");
        Transaction transaction = dataSession.beginTransaction();
        for (Project project : projectList) {
          List<ProjectAction> projectActionGoalList = projectActionGoalMap.get(project);
          for (ProjectAction projectActionGoal : projectActionGoalList) {
            Map<Calendar, ProjectAction> projectActionMap =
                projectActionDayMap.get(projectActionGoal);
            for (Calendar day : dayList) {
              String fieldName =
                  "s" + projectActionGoal.getActionId() + "." + sdfField.format(day.getTime());
              ProjectAction projectAction = projectActionMap.get(day);
              if (request.getParameter(fieldName) == null) {
                if (projectAction != null) {
                  dataSession.delete(projectAction);
                  projectActionMap.remove(day);
                }
              } else {
                if (projectAction == null) {
                  projectAction = new ProjectAction();
                  projectActionMap.put(day, projectAction);
                  projectAction.setProjectId(project.getProjectId());
                  projectAction.setContactId(webUser.getContactId());
                  projectAction.setContact(webUser.getProjectContact());
                  projectAction.setActionDate(new Date());
                  projectAction.setActionDescription("");
                  projectAction.setNextDescription(projectActionGoal.getNextDescription());
                  projectAction.setNextActionId(0);
                  projectAction.setNextDue(day.getTime());
                  String nextActionType =
                      request.getParameter("na" + projectActionGoal.getActionId());
                  projectAction.setNextActionType(nextActionType);
                  int priorityLevel = project.getPriorityLevel();
                  if (nextActionType != null) {
                    if (nextActionType.equals("T")) {
                      priorityLevel += 1;
                    } else if (nextActionType.equals("N") || nextActionType.equals("W")
                        || nextActionType.equals("E") || nextActionType.equals("A")) {
                      priorityLevel -= 1;
                    }
                  }
                  projectAction.setPriorityLevel(priorityLevel);
                  projectAction.setNextContactId(projectActionGoal.getNextContactId());
                  projectAction.setNextProjectContact(projectActionGoal.getNextProjectContact());
                  projectAction.setProvider(webUser.getProvider());
                  dataSession.save(projectAction);
                }
              }
            }
          }
        }
        transaction.commit();

      }


      appReq.setTitle("Projects");
      printHtmlHead(appReq);

      out.println("<form action=\"ProjectGoalScheduleServlet\" method=\"POST\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Goal</th>");
      {
        SimpleDateFormat sdf1 = new SimpleDateFormat("EEE");
        SimpleDateFormat sdf2 = new SimpleDateFormat("M/d");
        for (Calendar day : dayList) {
          out.println("    <th class=\"boxed\">" + sdf1.format(day.getTime()) + "<br/>"
              + sdf2.format(day.getTime()) + "</th>");
        }
      }
      out.println("    <th class=\"boxed\">Action</th>");
      out.println("  </tr>");
      SimpleDateFormat sdfField = webUser.getDateFormat("yyyyMMdd");
      for (Project project : projectList) {

        List<ProjectAction> projectActionGoalList = projectActionGoalMap.get(project);
        if (projectActionGoalList.size() == 0) {
          out.println("  <tr class=\"boxed\">");
          out.println(
              "    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId()
                  + "\" class=\"button\">" + project.getProjectName() + "</a></td>");
          out.println("    <td class=\"boxed\" colspan=\"9\"></td>");
          out.println("  </tr>");
        } else {
          for (ProjectAction projectActionGoal : projectActionGoalList) {
            Map<Calendar, ProjectAction> projectActionMap =
                projectActionDayMap.get(projectActionGoal);
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId="
                + project.getProjectId() + "\" class=\"button\">" + project.getProjectName()
                + "</a></td>");
            out.println("    <td class=\"boxed\">");
            out.println(projectActionGoal.getNextDescription());
            out.println("    </td>");
            for (Calendar day : dayList) {
              ProjectAction projectAction = projectActionMap.get(day);
              out.println("    <td class=\"boxed\">");
              out.println(
                  "      <input type=\"checkbox\" name=\"s" + projectActionGoal.getActionId() + "."
                      + sdfField.format(day.getTime()) + "\" value=\"" + sdf.format(day.getTime())
                      + "\"" + (projectAction == null ? "" : " checked") + "/>");
              out.println("    </td>");
            }
            out.println("    <td class=\"boxed\">");
            String nextActionType = ProjectNextActionType.WILL;
            if (projectActionGoal.getNextContactId() != null
                && projectActionGoal.getNextContactId() > 0) {
              nextActionType = ProjectNextActionType.ASKS_TO;
            }
            out.println("<select name=\"na" + projectActionGoal.getActionId() + "\">");
            for (String nat : new String[] {ProjectNextActionType.WILL, ProjectNextActionType.MIGHT,
                ProjectNextActionType.WILL_CONTACT, ProjectNextActionType.COMMITTED_TO,
                ProjectNextActionType.WAITING, ProjectNextActionType.ASKS_TO}) {
              String label = ProjectNextActionType.getLabel(nat);
              out.println("  <option value=\"" + nat + "\""
                  + (nat.equals(nextActionType) ? " selected" : "") + ">" + label + "</option>");
            }
            out.println("    </td>");
            out.println("  </tr>");
          }
        }
      }
      out.println("</table>");


      out.println("<input type=\"submit\" name=\"action\" value=\"Schedule\" >");
      out.println("</form>");



      printHtmlFoot(appReq);

    } finally {
      appReq.close();
    }
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
