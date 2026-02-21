/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeEntry;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillDay;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactSupervisor;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectProvider;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * @author nathan
 */
public class TrackServlet extends ClientServlet {

  private static final String TYPE_YEAR = "Year";
  private static final String TYPE_MONTH = "Month";
  private static final String TYPE_WEEK = "Week";
  private static final String TYPE_DAY = "Day";
  public static final float DAILY_HOURS = 7.5f;
  public static final float WEEKLY_HOURS = 37.5f;
  public static final float MONTHLY_HOURS = 150.0f;
  public static final float YEARLY_HOURS = 1800.0f;

  private static final String PARAM_BILL_DATE = "billDate";

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   *
   * @param request  servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException      if an I/O error occurs
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

      TimeTracker timeTracker = appReq.getTimeTracker();

      if (action != null) {
        if (action.equals("StopTimer")) {
          Integer lockedBillEntryId = timeTracker == null ? null : timeTracker.getBillEntryId();
          if (timeTracker != null) {
            timeTracker.stopClock(dataSession);
          }
          Transaction trans = dataSession.beginTransaction();
          try {
            Query cleanupQuery;
            if (lockedBillEntryId != null) {
              cleanupQuery = dataSession.createQuery(
                  "delete from BillEntry where username = :username and billMins = 0 and billId <> :billId");
              cleanupQuery.setParameter("billId", lockedBillEntryId);
            } else {
              cleanupQuery = dataSession
                  .createQuery("delete from BillEntry where username = :username and billMins = 0");
            }
            cleanupQuery.setParameter("username", webUser.getUsername());
            cleanupQuery.executeUpdate();
          } finally {
            trans.commit();
          }
          if (webUser.getParentWebUser() != null) {
            response.sendRedirect(
                "HomeServlet?action=Switch&childWebUserName="
                    + URLEncoder.encode(webUser.getParentWebUser().getUsername(), "UTF-8"));
            return;
          }
          response.sendRedirect("BillEntriesServlet?billDate=" + sdf.format(new Date()));
          return;
        }
      }

      if (webUser.getParentWebUser() != null) {
        response.sendRedirect("HomeServlet");
        return;
      }

      String type = request.getParameter("type");
      if (type == null) {
        type = TYPE_WEEK;
      }

      float targetHours = getTargetHours(request, type);

      int supervisedContactId = 0;
      if (request.getParameter("supervisedContactId") != null) {
        supervisedContactId = Integer.parseInt(request.getParameter("supervisedContactId"));
      }

      WebUser webUserSelected = getWebUserSelected(webUser, dataSession, supervisedContactId);

      String billDateString = request.getParameter(PARAM_BILL_DATE);
      if (billDateString == null || billDateString.length() == 0) {
        billDateString = sdf.format(new Date());
      }

      {
        if (!type.equals(TYPE_DAY)) {
          try {
            Date billDate = sdf.parse(billDateString);
            if (type.equals(TYPE_WEEK)) {
              timeTracker = new TimeTracker(webUserSelected, billDate, Calendar.WEEK_OF_YEAR, dataSession);
            } else if (type.equals(TYPE_MONTH)) {
              timeTracker = new TimeTracker(webUserSelected, billDate, Calendar.MONTH, dataSession);
            } else if (type.equals(TYPE_YEAR)) {
              timeTracker = new TimeTracker(webUserSelected, billDate, Calendar.YEAR, dataSession);
            } else {
              timeTracker = new TimeTracker(webUserSelected, billDate, dataSession);
            }
            Calendar calendar = webUser.getCalendar();
            calendar.setTime(timeTracker.getStartDate());
            while (calendar.getTime().before(timeTracker.getEndDate())) {
              updateBillDay(dataSession, webUserSelected, calendar.getTime());
              calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
          } catch (ParseException pe) {
            appReq.setMessageProblem("Unable to parse date: " + pe.getMessage());
          }
        }
      }

      appReq.setTitle("Track");
      printHtmlHead(appReq);

      printTrackForm(webUser, dataSession, out, type, targetHours, supervisedContactId,
          billDateString);

      makeTimeTrackReport(
          webUserSelected, out, dataSession, timeTracker, type, webUserSelected == webUser, targetHours);

      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  private WebUser getWebUserSelected(WebUser webUser, Session dataSession, int supervisedContactId) {
    WebUser webUserSelected = webUser;
    if (supervisedContactId > 0) {
      ProjectContact projectContactSelected = (ProjectContact) dataSession.get(ProjectContact.class,
          supervisedContactId);
      Query query = dataSession.createQuery(
          "from ProjectContactSupervisor where supervisor = ? and contact = ? order by"
              + " contact.nameFirst, contact.nameLast");
      query.setParameter(0, webUser.getProjectContact());
      query.setParameter(1, projectContactSelected);
      @SuppressWarnings("unchecked")
      List<ProjectContactSupervisor> projectContactSupervisorList = query.list();
      if (projectContactSupervisorList.size() > 0) {
        query = dataSession.createQuery("from WebUser where contactId = ?");
        query.setParameter(0, projectContactSelected.getContactId());
        @SuppressWarnings("unchecked")
        List<WebUser> webUserList = query.list();
        if (webUserList.size() > 0) {
          webUserSelected = webUserList.get(0);
          webUserSelected.setProjectContact(projectContactSelected);
          webUserSelected.setTimeZone(
              TimeZone.getTimeZone(
                  TrackerKeysManager.getKeyValue(
                      TrackerKeysManager.KEY_TIME_ZONE,
                      WebUser.AMERICA_DENVER,
                      webUser,
                      dataSession)));
        }
      }
    }
    return webUserSelected;
  }

  private void printTrackForm(WebUser webUser, Session dataSession, PrintWriter out, String type,
      float targetHours, int supervisedContactId, String billDateString) {
    out.println("<form action=\"TrackServlet\" method=\"GET\">");
    Query query = dataSession.createQuery(
        "from ProjectContactSupervisor where supervisor = ? order by contact.nameFirst,"
            + " contact.nameLast");
    query.setParameter(0, webUser.getProjectContact());
    @SuppressWarnings("unchecked")
    List<ProjectContactSupervisor> projectContactSupervisorList = query.list();
    if (projectContactSupervisorList.size() > 0) {
      out.println("Name");
      out.println("<select name=\"supervisedContactId\">");
      out.println(
          "<option value=\"0\""
              + (supervisedContactId == 0 ? " selected" : "")
              + ">"
              + webUser.getProjectContact().getName()
              + "</option>");
      for (ProjectContactSupervisor projectContactSupervisor : projectContactSupervisorList) {
        int c = projectContactSupervisor.getContact().getContactId();
        String n = projectContactSupervisor.getContact().getName();
        out.println(
            "<option value=\""
                + c
                + "\""
                + (supervisedContactId == c ? " selected" : "")
                + ">"
                + n
                + "</option>");
      }
      out.println("</select>");
    }
    if (type.equals(TYPE_WEEK)) {
      out.println("Date in Week");
    } else if (type.equals(TYPE_MONTH)) {
      out.println("Date in Month");

    } else if (type.equals(TYPE_YEAR)) {
      out.println("Date in Year");
    } else {
      out.println("Date");
    }
    out.println(
        "<input type=\"text\" name=\"" + PARAM_BILL_DATE + "\" value=\"" + billDateString + "\" size=\"10\">");
    out.println("Type ");
    out.println("<select name=\"type\">");
    out.println(
        "<option value=\"Day\"" + (type.equals(TYPE_DAY) ? " selected" : "") + ">Day</option>");
    out.println(
        "<option value=\"Week\"" + (type.equals(TYPE_WEEK) ? " selected" : "") + ">Week</option>");
    out.println(
        "<option value=\"Month\""
            + (type.equals(TYPE_MONTH) ? " selected" : "")
            + ">Month</option>");
    out.println(
        "<option value=\"Year\"" + (type.equals(TYPE_YEAR) ? " selected" : "") + ">Year</option>");
    out.println("</select>");

    out.println("Target Hours");
    out.println(
        "<input type=\"text\" name=\"targetHours\" value=\"" + targetHours + "\" size=\"4\">");

    out.println("<input type=\"submit\" name=\"action\" value=\"Refresh\">");
    out.println("</form>");
  }

  private float getTargetHours(HttpServletRequest request, String type) {
    float targetHours = WEEKLY_HOURS;
    String targetHoursString = request.getParameter(type + "targetHours");
    if (targetHoursString != null) {
      targetHours = Float.parseFloat(targetHoursString);
    }
    if (targetHours == DAILY_HOURS || targetHours == WEEKLY_HOURS || targetHours == MONTHLY_HOURS
        || targetHours == YEARLY_HOURS) {
      if (type.equals(TYPE_DAY)) {
        targetHours = DAILY_HOURS;
      } else if (type.equals(TYPE_WEEK)) {
        targetHours = WEEKLY_HOURS;
      } else if (type.equals(TYPE_MONTH)) {
        targetHours = MONTHLY_HOURS;
      } else if (type.equals(TYPE_YEAR)) {
        targetHours = YEARLY_HOURS;
      }
    }
    return targetHours;
  }

  @SuppressWarnings("unchecked")
  public static String makeTimeTrackReport(
      WebUser webUser,
      PrintWriter out,
      Session dataSession,
      TimeTracker timeTracker,
      String type,
      boolean showLinks,
      float targetHours) {

    int billableMins = timeTracker.getTotalMinsBillable();

    if (type.equals(TYPE_MONTH) || type.equals(TYPE_YEAR)) {
      printTotalWorkingTime(out, type, billableMins, targetHours);
      List<TimeEntry> timeEntryList = setupTimeEntryList(dataSession, timeTracker);
      Collections.sort(timeEntryList);
      List<ProjectActionTaken> projectActionTakenList = getProjectActionTakenList(webUser, dataSession, timeTracker);
      List<ProjectActionNext> projectActionCompletedList = getProjectActionNextCompletedList(webUser, dataSession,
          timeTracker);

      if (timeEntryList.size() > 0 || projectActionTakenList.size() > 0 || projectActionCompletedList.size() > 0) {
        out.println("<table class=\"boxed\">");
        out.println("  <tr>");
        out.println("    <th class=\"title\" colspan=\"3\">Time Tracked by Project</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Project</th>");
        out.println("    <th class=\"boxed\">Total Time</th>");
        out.println("  </tr>");
        for (TimeEntry timeEntry : timeEntryList) {
          printProjectLineWithTimeEntrySummaryOnly(webUser, out, dataSession, timeTracker, type, showLinks,
              projectActionTakenList, projectActionCompletedList, timeEntry);
        }
        out.println("</table> ");
        out.println("<br/> ");
      }
    } else {
      List<TimeEntry> timeEntryList = setupTimeEntryList(dataSession, timeTracker);
      Collections.sort(timeEntryList);
      if (type.equals(TYPE_WEEK)) {
        int totalTimeInMinutes = 0;
        for (TimeEntry timeEntry : timeEntryList) {
          totalTimeInMinutes += timeEntry.getMinutesAdjusted();
        }
        billableMins = totalTimeInMinutes;
      }

      printTotalWorkingTime(out, type, billableMins, targetHours);
      List<ProjectActionTaken> projectActionTakenList = getProjectActionTakenList(webUser, dataSession, timeTracker);
      List<ProjectActionNext> projectActionCompletedList = getProjectActionNextCompletedList(webUser, dataSession,
          timeTracker);

      if (timeEntryList.size() > 0 || projectActionTakenList.size() > 0 || projectActionCompletedList.size() > 0) {
        out.println("<table class=\"boxed\">");
        out.println("  <tr>");
        out.println("    <th class=\"title\" colspan=\"3\">Time Tracked by Project</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Project</th>");
        out.println("    <th class=\"boxed\">Total Time</th>");
        out.println("    <th class=\"boxed\">Actions Taken</th>");
        out.println("  </tr>");
        for (TimeEntry timeEntry : timeEntryList) {
          printProjectLineWithTimeEntry(webUser, out, dataSession, timeTracker, type, showLinks,
              projectActionTakenList, projectActionCompletedList, timeEntry);
        }
        while (projectActionTakenList.size() > 0 || projectActionCompletedList.size() > 0) {
          printProjectLineNoTimeEntered(out, dataSession, type, showLinks, projectActionTakenList,
              projectActionCompletedList);
        }
        out.println("</table> ");
        out.println("<br/> ");
      }
    }
    List<TimeEntry> timeEntryList = new ArrayList<TimeEntry>();
    Map<String, Integer> clientMap = timeTracker.getTotalMinsForClientMap();
    for (String categoryCode : clientMap.keySet()) {
      ProjectCategory projectCategory = getClient(dataSession, categoryCode, webUser.getProvider());
      if (projectCategory != null) {
        timeEntryList.add(
            new TimeEntry(projectCategory.getClientName(), clientMap.get(categoryCode)));
      }
    }
    Collections.sort(timeEntryList);

    if (timeEntryList.size() > 0) {
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"3\">Time Tracked by Category</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Category</th>");
      out.println("    <th class=\"boxed\">Total Time</th>");
      out.println("  </tr>");
      for (TimeEntry timeEntry : timeEntryList) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\">" + timeEntry.getLabel() + "</td>");
        out.println(
            "    <td class=\"boxed\">" + TimeTracker.formatTime(timeEntry.getMinutes()) + "</td>");
        out.println("  </tr>");
      }
      out.println("</table> ");
      out.println("<br/> ");

      timeEntryList = new ArrayList<TimeEntry>();
      Map<Integer, Integer> projectMap = timeTracker.getTotalMinsForProjectMap();
      for (Integer projectId : projectMap.keySet()) {
        Project project = (Project) dataSession.get(Project.class, projectId);
        if (project != null) {
          timeEntryList.add(
              new TimeEntry(project.getProjectName(), projectMap.get(projectId), projectId));
        }
      }
      Collections.sort(timeEntryList);
    }

    return TimeTracker.formatTime(billableMins);
  }

  private static void printProjectLineNoTimeEntered(PrintWriter out, Session dataSession, String type,
      boolean showLinks, List<ProjectActionTaken> projectActionTakenList,
      List<ProjectActionNext> projectActionCompletedList) {
    Integer projectId = null;
    if (!projectActionTakenList.isEmpty()) {
      projectId = projectActionTakenList.get(0).getProjectId();
    } else if (!projectActionCompletedList.isEmpty()) {
      projectId = projectActionCompletedList.get(0).getProjectId();
    }
    if (projectId == null) {
      return;
    }
    Project project = (Project) dataSession.get(Project.class, projectId);
    out.println("  <tr class=\"boxed\">");
    String link = "<a href=\"ProjectServlet?projectId="
        + project.getProjectId()
        + "\" class=\"button\">";
    if (showLinks) {
      out.println("    <td class=\"boxed\">" + link + project.getProjectName() + "</a></td>");
    } else {
      out.println("    <td class=\"boxed\">" + project.getProjectName() + "</td>");
    }
    out.println("    <td class=\"boxed\">0:00</td>");

    out.println("    <td class=\"boxed\">");
    boolean first = true;
    SimpleDateFormat sdf = new SimpleDateFormat(type.equals(TYPE_WEEK) ? "EEE" : "h:mm aaa");
    for (Iterator<ProjectActionTaken> it = projectActionTakenList.iterator(); it.hasNext();) {
      ProjectActionTaken actionTaken = it.next();
      if (actionTaken.getProjectId() == project.getProjectId()) {
        it.remove();
        if (!first) {
          out.println("    <br/>");
        }
        first = false;
        String dateLabel = actionTaken.getActionDate() == null ? "" : sdf.format(actionTaken.getActionDate());
        out.println("    " + (dateLabel.isEmpty() ? "" : dateLabel + ": ") + actionTaken.getActionDescription());
      }
    }
    for (Iterator<ProjectActionNext> it = projectActionCompletedList.iterator(); it.hasNext();) {
      ProjectActionNext actionNext = it.next();
      if (actionNext.getProjectId() == project.getProjectId()) {
        it.remove();
        if (!first) {
          out.println("    <br/>");
        }
        first = false;
        String dateLabel = actionNext.getNextChangeDate() == null ? "" : sdf.format(actionNext.getNextChangeDate());
        out.println(
            "    " + (dateLabel.isEmpty() ? "" : dateLabel + ": ") + "Completed " + actionNext.getNextDescription());
      }
    }
    out.println("    </td>");
    out.println("  </tr>");
  }

  private static void printProjectLineWithTimeEntry(WebUser webUser, PrintWriter out, Session dataSession,
      TimeTracker timeTracker, String type, boolean showLinks, List<ProjectActionTaken> projectActionTakenList,
      List<ProjectActionNext> projectActionCompletedList, TimeEntry timeEntry) {
    // if displaying for week then time needs to be rounded to 30 minutes.
    // The rounding rule is anything under 7 minutes is rounded down, and everything
    // 7 minutes
    // or over is rounded to the next 30 minutes
    int timeInMinutes = timeEntry.getMinutes();
    if (type.equals(TYPE_WEEK)) {
      timeInMinutes = timeEntry.getMinutesAdjusted();
    }
    out.println("  <tr class=\"boxed\">");
    String link = "<a href=\"ProjectServlet?projectId=" + timeEntry.getId() + "\" class=\"button\">";
    if (showLinks) {
      out.println("    <td class=\"boxed\">" + link + timeEntry.getLabel() + "</a></td>");
    } else {
      out.println("    <td class=\"boxed\">" + timeEntry.getLabel() + "</td>");
    }
    out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(timeInMinutes) + "</td>");

    List<ProjectActionTaken> projectActionList = timeEntry.getProjectActionList();
    int projectId = Integer.parseInt(timeEntry.getId());
    boolean hasCompletedNext = false;
    for (ProjectActionNext actionNext : projectActionCompletedList) {
      if (actionNext.getProjectId() == projectId) {
        hasCompletedNext = true;
        break;
      }
    }
    if ((projectActionList != null && projectActionList.size() > 0) || hasCompletedNext) {
      out.println("    <td class=\"boxed\">");
      SimpleDateFormat sdf = new SimpleDateFormat(type.equals(TYPE_WEEK) ? "EEE" : "h:mm aaa");
      boolean first = true;
      if (projectActionList != null) {
        for (ProjectActionTaken projectAction : projectActionList) {
          projectActionTakenList.remove(projectAction);
          if (!first) {
            out.println("    <br/>");
          }
          first = false;
          String dateLabel = projectAction.getActionDate() == null ? "" : sdf.format(projectAction.getActionDate());
          out.println(
              "    " + (dateLabel.isEmpty() ? "" : dateLabel + ": ") + projectAction.getActionDescription());
        }
      }
      for (Iterator<ProjectActionNext> it = projectActionCompletedList.iterator(); it.hasNext();) {
        ProjectActionNext actionNext = it.next();
        if (actionNext.getProjectId() == projectId) {
          it.remove();
          if (!first) {
            out.println("    <br/>");
          }
          first = false;
          String dateLabel = actionNext.getNextChangeDate() == null ? "" : sdf.format(actionNext.getNextChangeDate());
          out.println(
              "    " + (dateLabel.isEmpty() ? "" : dateLabel + ": ") + "Completed " + actionNext.getNextDescription());
        }
      }
      out.println("    </td>");
    } else {
      if (showLinks) {
        out.println("    <td class=\"inside-highlight\">no actions recorded</td>");
      } else {
        out.println("    <td class=\"inside-highlight\">no actions recorded</td>");
      }
    }
    out.println("  </tr>");
  }

  private static void printProjectNarrative(WebUser webUser, PrintWriter out, Session dataSession,
      TimeTracker timeTracker, String type, List<ProjectActionTaken> projectActionTakenList, TimeEntry timeEntry) {
    // if displaying for week then time needs to be rounded to 30 minutes.
    // The rounding rule is anything under 7 minutes is rounded down, and everything
    // 7 minutes
    // or over is rounded to the next 30 minutes
    int timeInMinutes = timeEntry.getMinutes();
    if (type.equals(TYPE_WEEK)) {
      timeInMinutes = timeEntry.getMinutesAdjusted();
    }
    Query query = dataSession.createQuery(
        "from ProjectActionTaken where contactId = ? and projectId = ? and"
            + " actionDescription <> '' and actionDate >= ? and actionDate < ? order"
            + " by actionDate asc");
    query.setParameter(0, webUser.getContactId());
    query.setParameter(1, Integer.parseInt(timeEntry.getId()));
    query.setParameter(2, timeTracker.getStartDate());
    query.setParameter(3, timeTracker.getEndDate());
    @SuppressWarnings("unchecked")
    List<ProjectActionTaken> projectActionList = query.list();
    timeEntry.setProjectActionList(projectActionList);
    if (timeInMinutes <= 0 && projectActionList.size() <= 0) {
      return;
    }
    out.println("    <h3>Project: " + timeEntry.getLabel() + "</h3>");
    Project project = (Project) dataSession.get(Project.class, Integer.parseInt(timeEntry.getId()));
    if (project.getDescription() != null && project.getDescription().length() > 0) {
      out.println("    <p>Project Description: " + project.getDescription() + "</p>");
    }
    out.println("    <p>Time spent: " + TimeTracker.formatTime(timeInMinutes) + "</p>");

    if (projectActionList.size() > 0) {
      out.println("<p>Actions Taken: ");
      for (ProjectActionTaken projectAction : projectActionList) {
        out.println(" " + projectAction.getActionDescription());
      }
      out.println("    </p>");
    } else {
      out.println("    <p>No actions recorded.</p>");
    }
    return;
  }

  private static void printProjectLineWithTimeEntrySummaryOnly(WebUser webUser, PrintWriter out, Session dataSession,
      TimeTracker timeTracker, String type, boolean showLinks, List<ProjectActionTaken> projectActionTakenList,
      List<ProjectActionNext> projectActionCompletedList, TimeEntry timeEntry) {
    out.println("  <tr class=\"boxed\">");
    String link = "<a href=\"ProjectServlet?projectId=" + timeEntry.getId() + "\" class=\"button\">";
    if (showLinks) {
      out.println("    <td class=\"boxed\">" + link + timeEntry.getLabel() + "</a></td>");
    } else {
      out.println("    <td class=\"boxed\">" + timeEntry.getLabel() + "</td>");
    }
    out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(timeEntry.getMinutes()) + "</td>");

    out.println("  </tr>");
  }

  private static List<TimeEntry> setupTimeEntryList(Session dataSession, TimeTracker timeTracker) {
    List<TimeEntry> timeEntryList = new ArrayList<TimeEntry>();
    Map<Integer, Integer> projectMap = timeTracker.getTotalMinsForProjectMap();
    for (Integer projectId : projectMap.keySet()) {
      Project project = (Project) dataSession.get(Project.class, projectId);
      if (project != null) {
        // Filter out non-billable projects
        String billCodeString = project.getBillCode();
        if (billCodeString != null) {
          BillCode billCode = (BillCode) dataSession.get(BillCode.class, billCodeString);
          if (billCode != null && "Y".equals(billCode.getBillable())) {
            TimeEntry timeEntry = new TimeEntry(project.getProjectName(), projectMap.get(projectId), projectId);
            timeEntryList.add(timeEntry);
          }
        }
      }
    }
    return timeEntryList;
  }

  private static List<ProjectActionTaken> getProjectActionTakenList(WebUser webUser, Session dataSession,
      TimeTracker timeTracker) {
    Query query = dataSession.createQuery(
        "from ProjectActionTaken where contactId = ? and actionDescription <> '' "
            + "and actionDate >= ? and actionDate < ? order by actionDate asc");
    query.setParameter(0, webUser.getContactId());
    query.setParameter(1, timeTracker.getStartDate());
    query.setParameter(2, timeTracker.getEndDate());
    @SuppressWarnings("unchecked")
    List<ProjectActionTaken> projectActionListComplete = query.list();
    return projectActionListComplete;
  }

  private static List<ProjectActionNext> getProjectActionNextCompletedList(WebUser webUser, Session dataSession,
      TimeTracker timeTracker) {
    Query query = dataSession.createQuery(
        "from ProjectActionNext where contactId = ? and nextDescription <> '' "
            + "and nextActionStatusString = ? and nextChangeDate >= ? and nextChangeDate < ? "
            + "order by nextChangeDate asc");
    query.setParameter(0, webUser.getContactId());
    query.setParameter(1, ProjectNextActionStatus.COMPLETED.getId());
    query.setParameter(2, timeTracker.getStartDate());
    query.setParameter(3, timeTracker.getEndDate());
    @SuppressWarnings("unchecked")
    List<ProjectActionNext> projectActionCompletedList = query.list();
    return projectActionCompletedList;
  }

  private static void printTotalWorkingTime(PrintWriter out, String type, int billableMins, float targetHours) {
    String hours = TimeTracker.formatTime(billableMins);
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Total Time</th>");
    out.println("    <td class=\"boxed\">" + hours + "</td>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    int targetMinutes = (int) (targetHours * 60);
    if (billableMins > targetMinutes) {
      out.println("    <th class=\"boxed\">Overage</th>");
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(billableMins - targetMinutes) + "</td>");
    } else {
      out.println("    <th class=\"boxed\">Remaining</th>");
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(targetMinutes - billableMins) + "</td>");
    }
    out.println("  </tr>");
    out.println("</table> ");
    out.println("<br/> ");
  }

  public static void updateBillDay(Session dataSession, WebUser webUser, Date day) {
    updateBillDay(dataSession, new TimeTracker(webUser, day, dataSession), day);
  }

  private static void updateBillDay(Session dataSession, TimeTracker timeTracker, Date billDate) {
    Map<String, Integer> billCodeMap = timeTracker.getTotalMinsForBillCodeMap();
    for (String billCodeString : billCodeMap.keySet()) {
      BillCode billCode = (BillCode) dataSession.get(BillCode.class, billCodeString);
      int billMins = TimeTracker.roundTime(billCodeMap.get(billCodeString), billCode);
      Query query = dataSession.createQuery("from BillDay where billCode = ? and billDate = ?");
      query.setParameter(0, billCode);
      query.setParameter(1, billDate);
      @SuppressWarnings("unchecked")
      List<BillDay> billDayList = query.list();
      BillDay billDay;
      if (billDayList.size() > 0) {
        billDay = billDayList.get(0);
      } else {
        billDay = new BillDay();
        billDay.setBillCode(billCode);
        billDay.setBillDate(billDate);
      }
      billDay.setBillMins(billMins);
      query = dataSession.createQuery(
          "from BillBudget where billCode = ? and start_date <= ? and end_date > ?");
      query.setParameter(0, billCode);
      query.setParameter(1, billDate);
      query.setParameter(2, billDate);
      @SuppressWarnings("unchecked")
      List<BillBudget> billBudgetList = query.list();
      if (billBudgetList.size() > 0) {
        billDay.setBillBudget(billBudgetList.get(0));
      } else {
        billDay.setBillBudget(null);
      }
      Transaction trans = dataSession.beginTransaction();
      try {
        dataSession.saveOrUpdate(billDay);
      } finally {
        trans.commit();
      }
    }
  }

  protected static ProjectCategory getClient(
      Session dataSession, String categoryCode, ProjectProvider provider) {
    Query query;
    query = dataSession.createQuery(
        "from ProjectCategory where categoryCode = :categoryCode and provider = :provider");
    query.setParameter("categoryCode", categoryCode);
    query.setParameter("provider", provider);
    @SuppressWarnings("unchecked")
    List<ProjectCategory> projectCategoryList = query.list();
    ProjectCategory projectCategory = null;
    if (projectCategoryList.size() > 0) {
      projectCategory = projectCategoryList.get(0);
    }
    return projectCategory;
  }

  // <editor-fold defaultstate="collapsed"
  // desc="HttpServlet methods. Click on the + sign on the left to edit the
  // code.">

  /**
   * Handles the HTTP <code>GET</code> method.
   *
   * @param request  servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException      if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   *
   * @param request  servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException      if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  public static String formatMoney(int i) {
    if (i == 0) {
      return "&nbsp;";
    }
    if (i < 1000) {
      return "" + i;
    }
    int thousand = i / 1000;
    String s = "" + thousand;
    return s.substring(0, s.length() - 3) + "," + s.substring(s.length() - 3);
  }
}
