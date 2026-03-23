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
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TemplateType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class TemplateScheduleServlet extends ClientServlet {

  private static final String TEMPLATE_SELECTED = "s";
  private static final String PROJECT_ID = "projectId";
  private static final String TIME_ESTIMATE = "te";
  private static final String TIME_SLOT = "ts";
  private static final String NEXT_DESCRIPTION = "nd";
  private static final String PARAM_SHOW_WORK = "showWork";
  private static final String PARAM_SHOW_PERSONAL = "showPersonal";
  private static final String PARAM_FILTER_SUBMITTED = "filterSubmitted";
  private static final String SESSION_SHOW_WORK = "projectAction.showWork";
  private static final String SESSION_SHOW_PERSONAL = "projectAction.showPersonal";
  private static final String REQUEST_SHOW_WORK = "projectAction.requestShowWork";
  private static final String REQUEST_SHOW_PERSONAL = "projectAction.requestShowPersonal";

  private boolean resolveBillable(Session dataSession, Project project) {
    if (project == null || project.getBillCode() == null || project.getBillCode().equals("")) {
      return false;
    }
    BillCode billCode = resolveBillCode(dataSession, project);
    return billCode != null && "Y".equalsIgnoreCase(billCode.getBillable());
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
      boolean showWork = isShowWork(request);
      boolean showPersonal = isShowPersonal(request);

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
      if (projectList == null || projectList.isEmpty()) {
        projectList = getProjectList(webUser, dataSession);
      }
      Map<ProjectActionNext, Map<Calendar, ProjectActionNext>> projectActionDayMap = new HashMap<ProjectActionNext, Map<Calendar, ProjectActionNext>>();
      Map<Project, List<ProjectActionNext>> templateMap = new HashMap<Project, List<ProjectActionNext>>();
      {
        for (Project project : projectList) {
          Query query = dataSession.createQuery(
              "from ProjectActionNext where projectId = :projectId and nextDescription <> '' "
                  + "and templateTypeString is NOT NULL and templateTypeString <> '' "
                  + "and nextActionStatusString = :nextActionStatus "
                  + "order by nextActionDate asc, nextDescription");
          query.setParameter(PROJECT_ID, project.getProjectId());
          query.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
          @SuppressWarnings("unchecked")
          List<ProjectActionNext> projectActionTemplateList = query.list();
          templateMap.put(project, projectActionTemplateList);
          for (ProjectActionNext projectActionTemplate : projectActionTemplateList) {
            Map<Calendar, ProjectActionNext> projectActionMap = new HashMap<Calendar, ProjectActionNext>();
            projectActionDayMap.put(projectActionTemplate, projectActionMap);
            query = dataSession.createQuery("from ProjectActionNext where "
                + "templateActionNextId = :templateActionNextId and nextActionDate >= :nextActionDate ");
            query.setParameter("templateActionNextId", projectActionTemplate.getActionNextId());
            query.setParameter("nextActionDate", dayList.get(0).getTime());
            @SuppressWarnings("unchecked")
            List<ProjectActionNext> pal = query.list();
            for (ProjectActionNext pa : pal) {

              Calendar calendar = null;
              for (Calendar c : dayList) {
                if (c.getTime().equals(pa.getNextActionDate())) {
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
        Date endOfYear = calculateEndOfYear(webUser);
        Transaction transaction = dataSession.beginTransaction();
        for (Project project : projectList) {
          boolean projectBillable = resolveBillable(dataSession, project);
          if (!shouldIncludeProject(projectBillable, showWork, showPersonal)) {
            continue;
          }
          List<ProjectActionNext> templateList = templateMap.get(project);
          for (ProjectActionNext templateAction : templateList) {
            Map<Calendar, ProjectActionNext> projectActionMap = projectActionDayMap.get(templateAction);
            String nextDescription = request.getParameter(NEXT_DESCRIPTION + templateAction.getActionNextId());
            String timeEstimate = request.getParameter(TIME_ESTIMATE + templateAction.getActionNextId());
            String timeSlotString = request.getParameter(TIME_SLOT + templateAction.getActionNextId());
            TimeSlot currentTimeSlot = templateAction.getTimeSlot() == null ? TimeSlot.AFTERNOON
                : templateAction.getTimeSlot();
            TimeSlot requestedTimeSlot = TimeSlot.getTimeSlot(timeSlotString);
            if (requestedTimeSlot == null) {
              requestedTimeSlot = TimeSlot.AFTERNOON;
            }
            boolean templateChanged = !templateAction.getNextDescription().equals(nextDescription);
            if (projectBillable) {
              templateChanged = templateChanged
                  || !templateAction.getNextTimeEstimateForDisplay().equals(timeEstimate);
            } else {
              templateChanged = templateChanged || currentTimeSlot != requestedTimeSlot;
            }
            if (templateChanged) {
              templateAction.setNextDescription(trim(nextDescription, 12000));
              if (projectBillable) {
                try {
                  templateAction.setNextTimeEstimate(Integer.parseInt(timeEstimate));
                } catch (NumberFormatException nfe) {
                  // just ignore and keep going
                }
              } else {
                templateAction.setTimeSlot(requestedTimeSlot);
              }
              templateAction.setNextActionDate(endOfYear);
              dataSession.update(templateAction);
            }
            for (Calendar day : dayList) {
              String fieldName = TEMPLATE_SELECTED + templateAction.getActionNextId() + "."
                  + sdfField.format(day.getTime());
              ProjectActionNext projectAction = projectActionMap.get(day);

              if (request.getParameter(fieldName) == null) {
                if (projectAction != null) {
                  dataSession.delete(projectAction);
                  projectActionMap.remove(day);
                }
              } else {
                if (projectAction == null) {
                  projectAction = new ProjectActionNext();
                  projectActionMap.put(day, projectAction);
                  projectAction.setProjectId(project.getProjectId());
                  projectAction.setContactId(webUser.getContactId());
                  projectAction.setContact(webUser.getProjectContact());
                  projectAction.setNextChangeDate(new Date());
                  projectAction.setNextDescription(templateAction.getNextDescription());
                  projectAction.setNextActionDate(day.getTime());
                  projectAction.setBillable(projectBillable);
                  String nextActionType = request.getParameter("na" + templateAction.getActionNextId());
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
                  projectAction.setTimeSlot(templateAction.getTimeSlot());
                  projectAction.setTemplateActionNextId(templateAction.getActionNextId());
                  projectAction.setProcessStage(templateAction.getProcessStage());
                  projectAction.setNextActionStatus(ProjectNextActionStatus.READY);
                  dataSession.save(projectAction);
                } else {
                  projectAction.setNextDescription(templateAction.getNextDescription());
                  projectAction.setNextTimeEstimate(templateAction.getNextTimeEstimate());
                  projectAction.setTimeSlot(templateAction.getTimeSlot());
                  dataSession.update(projectAction);
                }
              }
            }
          }

        }
        String[] addTemplateRequest = readAddTemplateRequest(request);
        if (addTemplateRequest != null) {
          String projectIdString = addTemplateRequest[0];
          String nextDescription = addTemplateRequest[1];
          String timeEstimate = addTemplateRequest[2];
          String timeSlotString = addTemplateRequest[3];
          Project project = (Project) dataSession.get(Project.class, Integer.parseInt(projectIdString));
          boolean projectBillable = resolveBillable(dataSession, project);
          if (shouldIncludeProject(projectBillable, showWork, showPersonal)) {
            ProjectActionNext templateAction = new ProjectActionNext();
            templateAction.setContactId(webUser.getContactId());
            templateAction.setContact(webUser.getProjectContact());
            templateAction.setNextChangeDate(new Date());

            templateAction.setProjectId(Integer.parseInt(projectIdString));
            templateAction.setNextActionDate(endOfYear);
            templateAction.setNextActionType(ProjectNextActionType.WILL);
            templateAction.setTemplateType(TemplateType.DAILY);
            templateAction.setNextDescription(trim(nextDescription, 12000));
            templateAction.setProvider(webUser.getProvider());
            templateAction.setBillable(projectBillable);
            if (projectBillable) {
              try {
                templateAction.setNextTimeEstimate(Integer.parseInt(timeEstimate));
              } catch (NumberFormatException nfe) {
                // just ignore and keep going
              }
            } else {
              TimeSlot timeSlot = TimeSlot.getTimeSlot(timeSlotString);
              if (timeSlot == null) {
                timeSlot = TimeSlot.AFTERNOON;
              }
              templateAction.setTimeSlot(timeSlot);
            }
            templateAction.setNextActionStatus(ProjectNextActionStatus.READY);
            templateAction.setPriorityLevel(project.getPriorityLevel());
            dataSession.save(templateAction);
            List<ProjectActionNext> templateList = templateMap.get(project);
            if (templateList != null) {
              templateList.add(templateAction);
            }
          }
        }
        transaction.commit();
      }

      appReq.setTitle("Projects");
      printHtmlHead(appReq);

      List<Project> workProjectList = new ArrayList<Project>();
      List<Project> personalProjectList = new ArrayList<Project>();
      for (Project project : projectList) {
        if (resolveBillable(dataSession, project)) {
          workProjectList.add(project);
        } else {
          personalProjectList.add(project);
        }
      }

      out.println("<form action=\"TemplateScheduleServlet\" method=\"POST\">");
      if (showWork) {
        printTemplateTable(out, webUser, dayList, workProjectList, templateMap, projectActionDayMap, sdf, "Work",
            "Work", false);
      }
      if (showPersonal) {
        printTemplateTable(out, webUser, dayList, personalProjectList, templateMap, projectActionDayMap, sdf,
            "Personal", "Personal", true);
      }

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

  private Date calculateEndOfYear(WebUser webUser) {
    Calendar calendar = webUser.getCalendar();
    calendar.add(Calendar.MONTH, 1);
    calendar.set(Calendar.MONTH, 11);
    calendar.set(Calendar.DAY_OF_MONTH, 31);
    Date endOfYear = calendar.getTime();
    return endOfYear;
  }

  private boolean shouldIncludeProject(boolean projectBillable, boolean showWork, boolean showPersonal) {
    if (projectBillable) {
      return showWork;
    }
    return showPersonal;
  }

  private void resolveAndStoreShowPreferences(HttpServletRequest request) {
    if (request.getAttribute(REQUEST_SHOW_WORK) instanceof Boolean
        && request.getAttribute(REQUEST_SHOW_PERSONAL) instanceof Boolean) {
      return;
    }

    HttpSession session = request.getSession();
    boolean hasFilterSubmitted = request.getParameter(PARAM_FILTER_SUBMITTED) != null;
    boolean hasShowWorkParam = request.getParameter(PARAM_SHOW_WORK) != null;
    boolean hasShowPersonalParam = request.getParameter(PARAM_SHOW_PERSONAL) != null;

    boolean showWork;
    boolean showPersonal;
    if (hasFilterSubmitted) {
      showWork = hasShowWorkParam;
      showPersonal = hasShowPersonalParam;
    } else {
      Boolean sessionShowWork = (Boolean) session.getAttribute(SESSION_SHOW_WORK);
      Boolean sessionShowPersonal = (Boolean) session.getAttribute(SESSION_SHOW_PERSONAL);
      if (sessionShowWork == null && sessionShowPersonal == null) {
        showWork = true;
        showPersonal = true;
      } else {
        showWork = sessionShowWork != null ? sessionShowWork.booleanValue() : true;
        showPersonal = sessionShowPersonal != null ? sessionShowPersonal.booleanValue() : true;
      }
    }

    if (!showWork && !showPersonal) {
      showWork = true;
      showPersonal = true;
    }

    session.setAttribute(SESSION_SHOW_WORK, Boolean.valueOf(showWork));
    session.setAttribute(SESSION_SHOW_PERSONAL, Boolean.valueOf(showPersonal));
    request.setAttribute(REQUEST_SHOW_WORK, Boolean.valueOf(showWork));
    request.setAttribute(REQUEST_SHOW_PERSONAL, Boolean.valueOf(showPersonal));
  }

  private boolean isShowWork(HttpServletRequest request) {
    resolveAndStoreShowPreferences(request);
    return ((Boolean) request.getAttribute(REQUEST_SHOW_WORK)).booleanValue();
  }

  private boolean isShowPersonal(HttpServletRequest request) {
    resolveAndStoreShowPreferences(request);
    return ((Boolean) request.getAttribute(REQUEST_SHOW_PERSONAL)).booleanValue();
  }

  private List<Project> getProjectList(WebUser webUser, Session dataSession) {
    String queryString = "from Project where provider = ?";
    queryString += " and phaseCode <> 'Clos'";
    queryString += " order by projectName";
    Query query = dataSession.createQuery(queryString);
    query.setParameter(0, webUser.getProvider());
    @SuppressWarnings("unchecked")
    List<Project> projectList = query.list();
    return projectList;
  }

  private void printTemplateTable(PrintWriter out, WebUser webUser, List<Calendar> dayList,
      List<Project> filteredProjectList,
      Map<Project, List<ProjectActionNext>> templateMap,
      Map<ProjectActionNext, Map<Calendar, ProjectActionNext>> projectActionDayMap,
      SimpleDateFormat sdf,
      String heading,
      String addFieldSuffix,
      boolean personalTable) {
    if (filteredProjectList == null || filteredProjectList.isEmpty()) {
      return;
    }

    out.println("<h2>" + heading + "</h2>");
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Project</th>");
    out.println("    <th class=\"boxed\">Template</th>");
    out.println("    <th class=\"boxed\">" + (personalTable ? "Time Slot" : "Time<br/>(mins)") + "</th>");

    Set<Calendar> onWeekend = new HashSet<Calendar>();
    Map<Calendar, Integer> timeMap = new HashMap<Calendar, Integer>();
    for (Calendar day : dayList) {
      out.println("    <th class=\"boxed\">"
          + webUser.getDateFormatService().formatWeekdayShort(day.getTime(), webUser.getTimeZone()) + "<br/>"
          + webUser.getDateFormatService().formatPattern(day.getTime(), "M/d", webUser.getTimeZone()) + "</th>");
      timeMap.put(day, 0);
      if (day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
          || day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
        onWeekend.add(day);
      }
    }
    out.println("    <th class=\"boxed\">Action</th>");
    out.println("  </tr>");

    SimpleDateFormat sdfField = webUser.getDateFormat("yyyyMMdd");
    for (Project project : filteredProjectList) {
      List<ProjectActionNext> templateScheduleList = templateMap.get(project);
      if (templateScheduleList == null || templateScheduleList.size() == 0) {
        continue;
      }
      if (personalTable) {
        templateScheduleList = sortByTimeSlot(templateScheduleList);
      }
      for (ProjectActionNext projectActionTemplate : templateScheduleList) {
        Map<Calendar, ProjectActionNext> projectActionMap = projectActionDayMap.get(projectActionTemplate);
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId="
            + project.getProjectId() + "\" class=\"button\">" + project.getProjectName()
            + "</a></td>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"" + NEXT_DESCRIPTION
            + projectActionTemplate.getActionNextId() + "\" value=\""
            + projectActionTemplate.getNextDescription() + "\" size=\"30\"/>");
        String link = "ProjectActionServlet?"
            + ProjectActionServlet.PARAM_COMPLETING_ACTION_NEXT_ID + "="
            + projectActionTemplate.getActionNextId()
            + "&editActionNextId=" + projectActionTemplate.getActionNextId();
        out.println("      <a href=\"" + link + "\" class=\"button\" title=\"Edit action\">&#9998;</a>");
        out.println("    </td>");
        out.println("    <td class=\"boxed\">");
        if (personalTable) {
          printTimeSlotSelect(out, TIME_SLOT + projectActionTemplate.getActionNextId(),
              projectActionTemplate.getTimeSlot());
        } else {
          out.println("      <input type=\"text\" name=\"" + TIME_ESTIMATE
              + projectActionTemplate.getActionNextId() + "\" value=\""
              + projectActionTemplate.getNextTimeEstimateMinsForDisplay() + "\" size=\"3\"/>");
        }
        out.println("    </td>");
        for (Calendar day : dayList) {
          ProjectActionNext projectAction = projectActionMap == null ? null : projectActionMap.get(day);
          boolean checked = projectAction != null;
          String style = onWeekend.contains(day) ? "boxed-lowlight" : "boxed";
          out.println("    <td class=\"" + style + "\">");
          out.println("      <input type=\"checkbox\" name=\"" + TEMPLATE_SELECTED
              + projectActionTemplate.getActionNextId() + "." + sdfField.format(day.getTime())
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
        out.println("<select name=\"na" + projectActionTemplate.getActionNextId() + "\">");
        for (String nat : new String[] { ProjectNextActionType.WILL, ProjectNextActionType.MIGHT,
            ProjectNextActionType.WILL_CONTACT, ProjectNextActionType.COMMITTED_TO }) {
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

    out.println("  <tr>");
    out.println("    <td class=\"boxed\">");
    out.println("<select name=\"" + PROJECT_ID + addFieldSuffix + "\">");
    for (Project project : filteredProjectList) {
      out.println("  <option value=\"" + project.getProjectId() + "\">" + project.getProjectName()
          + "</option>");
    }
    out.println("      </select>");
    out.println("    </td>");
    out.println("    <td class=\"boxed\">");
    out.println("      <input type=\"text\" name=\"" + NEXT_DESCRIPTION + addFieldSuffix
        + "\" value=\"\" size=\"30\"/>");
    out.println("    </td>");
    out.println("    <td class=\"boxed\">");
    if (personalTable) {
      printTimeSlotSelect(out, TIME_SLOT + addFieldSuffix, TimeSlot.AFTERNOON);
    } else {
      out.println("      <input type=\"text\" name=\"" + TIME_ESTIMATE + addFieldSuffix
          + "\" value=\"\" size=\"3\"/>");
    }
    out.println("    </td>");
    for (Calendar day : dayList) {
      out.println("    <td class=\"boxed\">");
      if (!personalTable) {
        out.println(ProjectActionNext.getTimeForDisplay(timeMap.get(day)));
      }
      out.println("    </td>");
    }
    out.println("  </tr>");
    out.println("</table>");
  }

  private List<ProjectActionNext> sortByTimeSlot(List<ProjectActionNext> templateScheduleList) {
    List<ProjectActionNext> sortedTemplateScheduleList = new ArrayList<ProjectActionNext>(templateScheduleList);
    Collections.sort(sortedTemplateScheduleList, new Comparator<ProjectActionNext>() {
      @Override
      public int compare(ProjectActionNext left, ProjectActionNext right) {
        int leftOrder = resolveTimeSlotOrder(left);
        int rightOrder = resolveTimeSlotOrder(right);
        if (leftOrder != rightOrder) {
          return leftOrder - rightOrder;
        }
        String leftDescription = left.getNextDescription() == null ? "" : left.getNextDescription();
        String rightDescription = right.getNextDescription() == null ? "" : right.getNextDescription();
        return leftDescription.compareToIgnoreCase(rightDescription);
      }
    });
    return sortedTemplateScheduleList;
  }

  private int resolveTimeSlotOrder(ProjectActionNext projectActionNext) {
    TimeSlot timeSlot = projectActionNext.getTimeSlot();
    if (timeSlot == null) {
      timeSlot = TimeSlot.AFTERNOON;
    }
    return timeSlot.ordinal();
  }

  private String[] readAddTemplateRequest(HttpServletRequest request) {
    for (String suffix : new String[] { "Work", "Personal", "" }) {
      String projectIdParam = suffix.equals("") ? PROJECT_ID : PROJECT_ID + suffix;
      String descriptionParam = suffix.equals("") ? NEXT_DESCRIPTION : NEXT_DESCRIPTION + suffix;
      String timeEstimateParam = suffix.equals("") ? TIME_ESTIMATE : TIME_ESTIMATE + suffix;
      String timeSlotParam = suffix.equals("") ? TIME_SLOT : TIME_SLOT + suffix;
      String projectId = request.getParameter(projectIdParam);
      String description = request.getParameter(descriptionParam);
      String timeEstimate = request.getParameter(timeEstimateParam);
      String timeSlot = request.getParameter(timeSlotParam);
      if (projectId != null && !projectId.equals("")
          && description != null && !description.trim().equals("")) {
        if (timeEstimate == null) {
          timeEstimate = "";
        }
        if (timeSlot == null) {
          timeSlot = "";
        }
        return new String[] { projectId, description.trim(), timeEstimate.trim(), timeSlot.trim() };
      }
    }
    return null;
  }

  private void printTimeSlotSelect(PrintWriter out, String name, TimeSlot selectedTimeSlot) {
    TimeSlot effectiveTimeSlot = selectedTimeSlot == null ? TimeSlot.AFTERNOON : selectedTimeSlot;
    out.println("      <select name=\"" + name + "\">");
    for (TimeSlot timeSlot : TimeSlot.values()) {
      out.println("        <option value=\"" + timeSlot.getId() + "\""
          + (timeSlot == effectiveTimeSlot ? " selected" : "") + ">"
          + timeSlot.getLabel() + "</option>");
    }
    out.println("      </select>");
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
