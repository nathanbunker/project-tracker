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
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TemplateType;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class TemplateScheduleServlet extends ClientServlet {

  private static final String TEMPLATE_SELECTED = "s";
  private static final String PROJECT_ID = "projectId";
  private static final String TIME_ESTIMATE = "te";
  private static final String NEXT_DESCRIPTION = "nd";

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
        while (count < 8) {
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
      Map<Project, List<ProjectAction>> templateMap =
          new HashMap<Project, List<ProjectAction>>();
      {
        for (Project project : projectList) {
          List<ProjectAction> projectActionTemplateList;
          {
            Query query = dataSession.createQuery(
                "from ProjectAction where projectId = :projectId and nextDescription <> '' "
                    + "and nextActionId = 0 and templateTypeString is NOT NULL and templateTypeString <> '' "
                    + "order by nextDue asc, nextDescription");
            query.setParameter(PROJECT_ID, project.getProjectId());
            projectActionTemplateList = query.list();
          }
          templateMap.put(project, projectActionTemplateList);
          for (ProjectAction projectActionTemplate : projectActionTemplateList) {
            Map<Calendar, ProjectAction> projectActionMap = new HashMap<Calendar, ProjectAction>();
            projectActionDayMap.put(projectActionTemplate, projectActionMap);
            Query query = dataSession.createQuery("from ProjectAction where "
                + "templateActionId = :templateActionId and nextDue >= :nextDue ");
            query.setParameter("templateActionId", projectActionTemplate.getActionId());
            query.setParameter("nextDue", dayList.get(0).getTime());
            List<ProjectAction> pal = query.list();
            for (ProjectAction pa : pal) {

              Calendar calendar = null;
              for (Calendar c : dayList) {
                if (c.getTime().equals(pa.getNextDue())) {
                  calendar = c;
                  break;
                }
              }
              if (calendar != null) {
                projectActionMap.put(calendar, pa);
              }
            }
          }
        }
      }

      if (action != null) {
        SimpleDateFormat sdfField = webUser.getDateFormat("yyyyMMdd");
        Date endOfYear = calculateEndOfYear();
        Transaction transaction = dataSession.beginTransaction();
        ProjectAction templateActionPrevious = null;
        for (Project project : projectList) {
          List<ProjectAction> templateList = templateMap.get(project);
          for (ProjectAction templateAction : templateList) {
            Map<Calendar, ProjectAction> projectActionMap =
                projectActionDayMap.get(templateAction);
            templateActionPrevious = templateAction;
            String nextDescription =
                request.getParameter(NEXT_DESCRIPTION + templateAction.getActionId());
            String timeEstimate =
                request.getParameter(TIME_ESTIMATE + templateAction.getActionId());
            if (!templateAction.getNextDescription().equals(nextDescription)
                || !templateAction.getNextTimeEstimateForDisplay().equals(timeEstimate)) {
              templateAction.setNextDescription(trim(nextDescription, 12000));
              try {
                templateAction.setNextTimeEstimate(Integer.parseInt(timeEstimate));
              } catch (NumberFormatException nfe) {
                // just ignore and keep going
              }
              templateAction.setNextDue(endOfYear);
              dataSession.update(templateAction);
            }
            for (Calendar day : dayList) {
              String fieldName = TEMPLATE_SELECTED + templateAction.getActionId() + "."
                  + sdfField.format(day.getTime());
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
                  projectAction.setNextDescription(templateAction.getNextDescription());
                  projectAction.setNextActionId(0);
                  projectAction.setNextDue(day.getTime());
                  String nextActionType =
                      request.getParameter("na" + templateAction.getActionId());
                  projectAction.setNextActionType(nextActionType);
                  int priorityLevel = project.getPriorityLevel();
                  if (nextActionType != null) {
                    if (nextActionType.equals(ProjectNextActionType.COMMITTED_TO)) {
                      priorityLevel += 1;
                    } else if (nextActionType.equals(ProjectNextActionType.MIGHT)
                        || nextActionType.equals(ProjectNextActionType.WAITING)) {
                      priorityLevel -= 1;
                    }
                  }
                  projectAction.setPriorityLevel(priorityLevel);
                  projectAction.setNextContactId(templateAction.getNextContactId());
                  projectAction.setNextProjectContact(templateAction.getNextProjectContact());
                  projectAction.setProvider(webUser.getProvider());
                  projectAction.setNextTimeEstimate(templateAction.getNextTimeEstimate());
                  projectAction.setTemplateActionId(templateAction.getActionId());
                  projectAction.setPrioritySpecial(templateAction.getPrioritySpecial());
                  dataSession.save(projectAction);
                } else {
                  projectAction.setNextDescription(templateAction.getNextDescription());
                  projectAction.setNextTimeEstimate(templateAction.getNextTimeEstimate());
                  dataSession.update(projectAction);
                }
              }
            }
          }

        }
        if (templateActionPrevious != null) {
          String projectIdString = request.getParameter(PROJECT_ID);
          String nextDescription = request.getParameter(NEXT_DESCRIPTION);
          String timeEstimate = request.getParameter(TIME_ESTIMATE);
          if (!projectIdString.equals("") && !nextDescription.equals("")) {
            Project project =
                (Project) dataSession.get(Project.class, Integer.parseInt(projectIdString));
            ProjectAction templateAction = new ProjectAction();
            templateAction.setContactId(webUser.getContactId());
            templateAction.setContact(webUser.getProjectContact());
            templateAction.setActionDate(new Date());
            templateAction.setActionDescription("");

            templateAction.setProjectId(Integer.parseInt(projectIdString));
            templateAction.setNextDue(endOfYear);
            templateAction.setNextActionType(ProjectNextActionType.WILL);
            templateAction.setNextContactId(templateActionPrevious.getNextContactId());
            templateAction
                .setNextProjectContact(templateActionPrevious.getNextProjectContact());
            templateAction.setNextDescription(trim(nextDescription, 12000));
            templateAction.setProvider(templateActionPrevious.getProvider());
            try {
              templateAction.setNextTimeEstimate(Integer.parseInt(timeEstimate));
            } catch (NumberFormatException nfe) {
              // just ignore and keep going
            }
            templateAction.setNextActionId(0);
            templateAction.setPriorityLevel(project.getPriorityLevel());
            dataSession.save(templateAction);
            List<ProjectAction> templateList = templateMap.get(project);
            if (templateList != null) {
              templateList.add(templateAction);
            }
          }
        }
        transaction.commit();
      }


      appReq.setTitle("Projects");
      printHtmlHead(appReq);

      out.println("<form action=\"TemplateScheduleServlet\" method=\"POST\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Template</th>");
      out.println("    <th class=\"boxed\">Time<br/>(mins)</th>");
      Set<Calendar> onWeekend = new HashSet<Calendar>();
      Map<Calendar, Integer> timeMap = new HashMap<Calendar, Integer>();
      {
        SimpleDateFormat sdf1 = new SimpleDateFormat("EEE");
        SimpleDateFormat sdf2 = new SimpleDateFormat("M/d");
        for (Calendar day : dayList) {
          out.println("    <th class=\"boxed\">" + sdf1.format(day.getTime()) + "<br/>"
              + sdf2.format(day.getTime()) + "</th>");
          timeMap.put(day, 0);
          if (day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
              || day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            onWeekend.add(day);
          }
        }
      }
      out.println("    <th class=\"boxed\">Action</th>");
      out.println("  </tr>");
      SimpleDateFormat sdfField = webUser.getDateFormat("yyyyMMdd");

      for (Project project : projectList) {

        List<ProjectAction> templateScheduleList = templateMap.get(project);
        if (templateScheduleList.size() == 0) {
          //          out.println("  <tr class=\"boxed\">");
          //          out.println(
          //              "    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId()
          //                  + "\" class=\"button\">" + project.getProjectName() + "</a></td>");
          //          out.println("    <td class=\"boxed\" colspan=\"9\"></td>");
          //          out.println("  </tr>");
        } else {
          for (ProjectAction projectActionTemplate : templateScheduleList) {
            Map<Calendar, ProjectAction> projectActionMap =
                projectActionDayMap.get(projectActionTemplate);
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId="
                + project.getProjectId() + "\" class=\"button\">" + project.getProjectName()
                + "</a></td>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"text\" name=\"" + NEXT_DESCRIPTION
                + projectActionTemplate.getActionId() + "\" value=\""
                + projectActionTemplate.getNextDescription() + "\" size=\"30\"/>");
            {
              String link = "ProjectServlet?projectId=" + project.getProjectId() + "&"
                  + ProjectServlet.PARAM_ACTION_ID + "=" + projectActionTemplate.getActionId();
              out.println("      <a href=\"" + link + "\" class=\"button\">edit</a>");
            }
            out.println("    </td>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"text\" name=\"" + TIME_ESTIMATE
                + projectActionTemplate.getActionId() + "\" value=\""
                + projectActionTemplate.getNextTimeEstimateMinsForDisplay() + "\" size=\"3\"/>");
            out.println("    </td>");
            for (Calendar day : dayList) {
              ProjectAction projectAction =
                  projectActionMap == null ? null : projectActionMap.get(day);
              boolean checked = projectAction != null;
              String style = "boxed";
              if (onWeekend.contains(day)) {
                style = "boxed-lowlight";
              }
              out.println("    <td class=\"" + style + "\">");
              out.println("      <input type=\"checkbox\" name=\"" + TEMPLATE_SELECTED
                  + projectActionTemplate.getActionId() + "." + sdfField.format(day.getTime())
                  + "\" value=\"" + sdf.format(day.getTime()) + "\"" + (checked ? " checked" : "")
                  + "/>");
              out.println("    </td>");
              if (checked && projectActionTemplate.getNextTimeEstimate() != null
                  && projectActionTemplate.getNextTimeEstimate() > 0) {
                timeMap.put(day, timeMap.get(day) + projectActionTemplate.getNextTimeEstimate());
              }
            }
            out.println("    <td class=\"boxed\">");
            String nextActionType = ProjectNextActionType.WILL;
            out.println("<select name=\"na" + projectActionTemplate.getActionId() + "\">");
            for (String nat : new String[] {ProjectNextActionType.WILL, ProjectNextActionType.MIGHT,
                ProjectNextActionType.WILL_CONTACT, ProjectNextActionType.COMMITTED_TO}) {
              String label = ProjectNextActionType.getLabel(nat);
              boolean selected = nat.equals(nextActionType);
              out.println("  <option value=\"" + nat + "\"" + (selected ? " selected" : "") + ">"
                  + label + "</option>");
            }
            out.println("      </select>");
            out.println("    </td>");
            out.println("  </tr>");
          }
        }
      }
      out.println("  <tr>");
      out.println("    <td class=\"boxed\">");
      out.println("<select name=\"" + PROJECT_ID + "\">");
      for (Project project : projectList) {
        out.println("  <option value=\"" + project.getProjectId() + "\">" + project.getProjectName()
            + "</option>");
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("    <td class=\"boxed\">");
      out.println(
          "      <input type=\"text\" name=\"" + NEXT_DESCRIPTION + "\" value=\"\" size=\"30\"/>");
      out.println("    </td>");
      out.println("    <td class=\"boxed\">");
      out.println(
          "      <input type=\"text\" name=\"" + TIME_ESTIMATE + "\" value=\"\" size=\"3\"/>");
      out.println("    </td>");

      for (Calendar day : dayList) {
        out.println("    <td class=\"boxed\">");
        out.println(ProjectAction.getTimeForDisplay(timeMap.get(day)));
        out.println("    </td>");
      }
      out.println("  </tr>");

      out.println("</table>");


      out.println("<br/>");
      out.println("<input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"Update\" >");
      out.println("</form>");



      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  private Date calculateEndOfYear() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.MONTH, 1);
    calendar.set(Calendar.MONTH, 11);
    calendar.set(Calendar.DAY_OF_MONTH, 31);
    Date endOfYear = calendar.getTime();
    return endOfYear;
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
