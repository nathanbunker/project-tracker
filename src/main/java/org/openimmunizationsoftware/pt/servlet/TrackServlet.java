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
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.manager.TimeEntry;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillDay;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectClient;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactSupervisor;
import org.openimmunizationsoftware.pt.model.TrackerKeys;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class TrackServlet extends ClientServlet {

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
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
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/html;charset=UTF-8");
    HttpSession session = request.getSession(true);
    WebUser webUser = (WebUser) session.getAttribute(SESSION_VAR_WEB_USER);
    if (webUser == null) {
      RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
      dispatcher.forward(request, response);
      return;
    }

    PrintWriter out = response.getWriter();
    try {
      Session dataSession = getDataSession(session);
      TimeTracker timeTracker = (TimeTracker) session.getAttribute(SESSION_VAR_TIME_TRACKER);
      String action = request.getParameter("action");

      if (action != null) {
        if (action.equals("StopTimer")) {
          if (timeTracker != null) {
            timeTracker.stopClock(dataSession);
            if (webUser.getParentWebUser() != null) {
              response.sendRedirect("HomeServlet?action=Switch&childWebUserName="
                  + URLEncoder.encode(webUser.getParentWebUser().getUsername(), "UTF-8"));
              return;
            }
          }
        }
      }

      if (webUser.getParentWebUser() != null) {
        response.sendRedirect("HomeServlet");
        return;
      }

      String type = request.getParameter("type");
      if (type == null) {
        type = "Day";
      }

      int supervisedContactId = 0;
      if (request.getParameter("supervisedContactId") != null) {
        supervisedContactId = Integer.parseInt(request.getParameter("supervisedContactId"));
      }

      WebUser webUserSelected = webUser;
      if (supervisedContactId > 0) {
        ProjectContact projectContactSelected =
            (ProjectContact) dataSession.get(ProjectContact.class, supervisedContactId);
        Query query = dataSession.createQuery(
            "from ProjectContactSupervisor where supervisor = ? and contact = ? order by contact.nameFirst, contact.nameLast");
        query.setParameter(0, webUser.getProjectContact());
        query.setParameter(1, projectContactSelected);
        List<ProjectContactSupervisor> projectContactSupervisorList = query.list();
        if (projectContactSupervisorList.size() > 0) {
          query = dataSession.createQuery("from WebUser where contactId = ?");
          query.setParameter(0, projectContactSelected.getContactId());
          @SuppressWarnings("unchecked")
          List<WebUser> webUserList = query.list();
          if (webUserList.size() > 0) {
            webUserSelected = webUserList.get(0);
            webUserSelected.setProjectContact(projectContactSelected);
          }
        }
      }

      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
      String billDateString = request.getParameter("billDate");
      Date billDate = null;
      if ((billDateString != null && billDateString.length() > 0) || !type.equals("Day")) {
        try {
          billDate = sdf.parse(billDateString);
          if (type.equals("Week")) {
            timeTracker =
                new TimeTracker(webUserSelected, billDate, Calendar.WEEK_OF_YEAR, dataSession);
          } else if (type.equals("Month")) {
            timeTracker = new TimeTracker(webUserSelected, billDate, Calendar.MONTH, dataSession);
          } else if (type.equals("Year")) {
            timeTracker = new TimeTracker(webUserSelected, billDate, Calendar.YEAR, dataSession);
          } else {
            timeTracker = new TimeTracker(webUserSelected, billDate, dataSession);
          }
          Calendar calendar = Calendar.getInstance();
          calendar.setTime(timeTracker.getStartDate());
          while (calendar.getTime().before(timeTracker.getEndDate())) {
            updateBillDay(dataSession, webUserSelected, calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
          }
        } catch (ParseException pe) {
          request.setAttribute(REQUEST_VAR_MESSAGE, "Unable to parse date: " + pe.getMessage());
        }
      } else {
        billDateString = sdf.format(new Date());
      }

      printHtmlHead(out, "Track", request);

      out.println("<form action=\"TrackServlet\" method=\"GET\">");
      Query query = dataSession.createQuery(
          "from ProjectContactSupervisor where supervisor = ? order by contact.nameFirst, contact.nameLast");
      query.setParameter(0, webUser.getProjectContact());
      @SuppressWarnings("unchecked")
      List<ProjectContactSupervisor> projectContactSupervisorList = query.list();
      if (projectContactSupervisorList.size() > 0) {
        out.println("Name");
        out.println("<select name=\"supervisedContactId\">");
        out.println("<option value=\"0\"" + (supervisedContactId == 0 ? " selected" : "") + ">"
            + webUser.getProjectContact().getName() + "</option>");
        for (ProjectContactSupervisor projectContactSupervisor : projectContactSupervisorList) {
          int c = projectContactSupervisor.getContact().getContactId();
          String n = projectContactSupervisor.getContact().getName();
          out.println("<option value=\"" + c + "\"" + (supervisedContactId == c ? " selected" : "")
              + ">" + n + "</option>");
        }
        out.println("</select>");
      }
      if (type.equals("Week")) {
        out.println("Date in Week");
      } else if (type.equals("Month")) {
        out.println("Date in Month");

      } else if (type.equals("Year")) {
        out.println("Date in Year");
      } else {
        out.println("Date");
      }
      out.println(
          "<input type=\"text\" name=\"billDate\" value=\"" + billDateString + "\" size=\"10\">");
      out.println("Type ");
      out.println("<select name=\"type\">");
      out.println(
          "<option value=\"Day\"" + (type.equals("Day") ? " selected" : "") + ">Day</option>");
      out.println(
          "<option value=\"Week\"" + (type.equals("Week") ? " selected" : "") + ">Week</option>");
      out.println("<option value=\"Month\"" + (type.equals("Month") ? " selected" : "")
          + ">Month</option>");
      out.println(
          "<option value=\"Year\"" + (type.equals("Year") ? " selected" : "") + ">Year</option>");
      out.println("</select>");

      out.println("<input type=\"submit\" name=\"action\" value=\"Refresh\">");
      out.println("</form>");

      makeTimeTrackReport(webUserSelected, out, dataSession, timeTracker, type,
          webUserSelected == webUser);

      printHtmlFoot(out);

    } finally {
      out.close();
    }

  }

  @SuppressWarnings("unchecked")
  public static String makeTimeTrackReport(WebUser webUser, PrintWriter out, Session dataSession,
      TimeTracker timeTracker, String type, boolean showLinks) {

    String hours = TimeTracker.formatTime(timeTracker.getTotalMinsBillable());

    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    if (type.equals("Week")) {
      out.println("    <th class=\"boxed\">Total Working Time for Week</th>");
    } else if (type.equals("Month")) {
      out.println("    <th class=\"boxed\">Total Working Time for Month</th>");
    } else if (type.equals("Year")) {
      out.println("    <th class=\"boxed\">Total Working Time for Year</th>");
    } else {
      out.println("    <th class=\"boxed\">Total Working Time</th>");
    }
    out.println("    <td class=\"boxed\">" + hours + "</td>");
    out.println("  </tr>");
    out.println("</table> ");
    out.println("<br/> ");

    if (!type.equals("Month") && !type.equals("Year")) {
      List<TimeEntry> timeEntryList = new ArrayList<TimeEntry>();
      Map<Integer, Integer> projectMap = timeTracker.getTotalMinsForProjectMap();
      for (Integer projectId : projectMap.keySet()) {
        Project project = (Project) dataSession.get(Project.class, projectId);
        if (project != null) {
          timeEntryList
              .add(new TimeEntry(project.getProjectName(), projectMap.get(projectId), projectId));
        }
      }
      List<ProjectAction> projectActionListComplete = null;
      Collections.sort(timeEntryList);
      {
        Query query = dataSession.createQuery("from ProjectAction where contactId = ? "
            + "  and actionDescription <> '' and action_date >= ? and action_date < ? order by actionDate asc");
        query.setParameter(0, webUser.getContactId());
        query.setParameter(1, timeTracker.getStartDate());
        query.setParameter(2, timeTracker.getEndDate());
        projectActionListComplete = query.list();
      }

      if (timeEntryList.size() > 0 || projectActionListComplete.size() > 0) {
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
          out.println("  <tr class=\"boxed\">");
          String link =
              "<a href=\"ProjectServlet?projectId=" + timeEntry.getId() + "\" class=\"button\">";
          if (showLinks) {
            out.println("    <td class=\"boxed\">" + link + timeEntry.getLabel() + "</a></td>");
          } else {
            out.println("    <td class=\"boxed\">" + timeEntry.getLabel() + "</td>");
          }
          out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(timeEntry.getMinutes())
              + "</td>");

          Query query =
              dataSession.createQuery("from ProjectAction where contactId = ? and projectId = ? "
                  + "  and actionDescription <> '' and action_date >= ? and action_date < ? order by actionDate asc");
          query.setParameter(0, webUser.getContactId());
          query.setParameter(1, Integer.parseInt(timeEntry.getId()));
          query.setParameter(2, timeTracker.getStartDate());
          query.setParameter(3, timeTracker.getEndDate());
          List<ProjectAction> projectActionList = query.list();
          if (projectActionList.size() > 0) {
            out.println("    <td class=\"boxed\">");
            SimpleDateFormat sdf = new SimpleDateFormat(type.equals("Week") ? "EEE" : "h:mm aaa");
            boolean first = true;
            for (ProjectAction projectAction : projectActionList) {
              projectActionListComplete.remove(projectAction);
              if (!first) {
                out.println("    <br/>");
              }
              first = false;
              out.println("    " + sdf.format(projectAction.getActionDate()) + ": ");
              out.println("    " + projectAction.getActionDescription());
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
        while (projectActionListComplete.size() > 0) {
          Project project = (Project) dataSession.get(Project.class,
              projectActionListComplete.get(0).getProjectId());
          out.println("  <tr class=\"boxed\">");
          String link = "<a href=\"ProjectServlet?projectId=" + project.getProjectId()
              + "\" class=\"button\">";
          if (showLinks) {
            out.println("    <td class=\"boxed\">" + link + project.getProjectName() + "</a></td>");
          } else {
            out.println("    <td class=\"boxed\">" + project.getProjectName() + "</td>");
          }
          out.println("    <td class=\"boxed\">0:00</td>");

          out.println("    <td class=\"boxed\">");
          boolean first = true;
          for (Iterator<ProjectAction> it = projectActionListComplete.iterator(); it.hasNext();) {
            ProjectAction projectAction = it.next();
            if (projectAction.getProjectId() == project.getProjectId()) {
              SimpleDateFormat sdf = new SimpleDateFormat(type.equals("Week") ? "EEE" : "h:mm aaa");
              it.remove();
              if (!first) {
                out.println("    <br/>");
              }
              first = false;
              out.println("    " + sdf.format(projectAction.getActionDate()) + ": ");
              out.println("    " + projectAction.getActionDescription());
            }
          }
          out.println("    </td>");
          out.println("  </tr>");
        }
        out.println("</table> ");
        out.println("<br/> ");
      }

      HomeServlet.printActionsDue(webUser, out, dataSession, "", timeTracker.getStartDate(),
          showLinks, false);
    }

    out.println("<h2>Additional Information</h2>");

    Map<String, Integer> billCodeMap = timeTracker.getTotalMinsForBillCodeMap();
    List<TimeEntry> timeEntryList = timeTracker.createTimeEntryList();

    if (timeEntryList.size() > 0) {
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"3\">Time Tracked by Funding Source</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Bill Code</th>");
      out.println("    <th class=\"boxed\">Actual</th>");
      out.println("    <th class=\"boxed\">Rounded</th>");
      // out.println("    <th class=\"boxed\">Billable</th>");
      out.println("  </tr>");
      int totalTime = 0;
      int totalBillable = 0;
      int totalBillableMoney = 0;
      for (TimeEntry timeEntry : timeEntryList) {
        String billCodeString = timeEntry.getId();
        BillCode billCode = (BillCode) dataSession.get(BillCode.class, billCodeString);
        int billable;
        if (type.equals("Day")) {
          billable = TimeTracker.roundTime(billCodeMap.get(billCodeString), billCode);
        } else {
          Query query = dataSession
              .createQuery("from BillDay where billCode = ? and bill_date >= ? and bill_date < ?");
          query.setParameter(0, billCode);
          query.setParameter(1, timeTracker.getStartDate());
          query.setParameter(2, timeTracker.getEndDate());
          billable = 0;
          List<BillDay> billDayList = query.list();
          for (BillDay billDay : billDayList) {
            billable += billDay.getBillMins();
          }
        }
        int billableMoney = (int) (billable * billCode.getBillRate() / 60.0 + 0.5);
        totalTime += billCodeMap.get(billCodeString);
        totalBillable += billable;
        totalBillableMoney += billableMoney;
        out.println("  <tr class=\"boxed\">");
        if (showLinks) {
          out.println(
              "    <td class=\"boxed\"><a href=\"BillCodeServlet?billCode=" + billCode.getBillCode()
                  + "\" class=\"button\">" + billCode.getBillLabel() + "</a></td>");
        } else {
          out.println("    <td class=\"boxed\">" + billCode.getBillLabel() + "</td>");
        }
        out.println("    <td class=\"boxed\">"
            + TimeTracker.formatTime(billCodeMap.get(billCodeString)) + "</td>");
        out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(billable) + "</td>");
        //        if (billCode.getBillRate() > 0)
        //        {
        //          out.println("    <td class=\"boxed\">" + formatMoney(billableMoney) + "</td>");
        //        } else
        //        {
        //          out.println("    <td class=\"boxed\">&nbsp;</td>");
        //        }
        out.println("  </tr>");
      }
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Total</th>");
      out.println("    <th class=\"boxed\">Actual</th>");
      out.println("    <th class=\"boxed\">Rounded</th>");
      // out.println("    <th class=\"boxed\">Billable</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\">Total</td>");
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalTime) + "</td>");
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalBillable) + "</td>");
      //      out.println("    <td class=\"boxed\">" + formatMoney(totalBillableMoney) + "</td>");
      out.println("  </tr>");
      out.println("</table> ");
      if (!showLinks) {
        out.println("<br/>");
      }
    }
    if (showLinks) {
      out.println(
          "<p><a href=\"BillEntriesServlet\" class=\"button\">See all Bill Entries</a></p>");
      out.println("<p><a href=\"BillCodesServlet\" class=\"button\">See all Bill Codes</a></p>");
      out.println(
          "<p><a href=\"BillBudgetsServlet\" class=\"button\">See all Bill Budgets</a></p>");
    }
    timeEntryList = new ArrayList<TimeEntry>();
    Map<String, Integer> clientMap = timeTracker.getTotalMinsForClientMap();
    for (String clientCode : clientMap.keySet()) {
      ProjectClient projectClient = getClient(dataSession, clientCode, webUser.getProviderId());
      if (projectClient != null) {
        timeEntryList.add(new TimeEntry(projectClient.getClientName(), clientMap.get(clientCode)));
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
          timeEntryList
              .add(new TimeEntry(project.getProjectName(), projectMap.get(projectId), projectId));
        }
      }
      Collections.sort(timeEntryList);
    }

    return hours;
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
      query = dataSession
          .createQuery("from BillBudget where billCode = ? and start_date <= ? and end_date > ?");
      query.setParameter(0, billCode);
      query.setParameter(1, billDate);
      query.setParameter(2, billDate);
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

  protected static ProjectClient getClient(Session dataSession, String clientCode,
      String providerId) {
    Query query;
    query =
        dataSession.createQuery("from ProjectClient where id.clientCode = ? and id.providerId = ?");
    query.setParameter(0, clientCode);
    query.setParameter(1, providerId);
    List<ProjectClient> projectClientList = query.list();
    ProjectClient projectClient = null;
    if (projectClientList.size() > 0) {
      projectClient = projectClientList.get(0);
    }
    return projectClient;
  }

  // <editor-fold defaultstate="collapsed"
  // desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

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

  public static String formatMoney(int i) {
    if (i == 0) {
      return "&nbsp;";
    }
    if (i < 1000) {
      return "" + i;
    }
    int thousand = i / 1000;
    String s = "" + i;
    return s.substring(0, s.length() - 3) + "," + s.substring(s.length() - 3);
  }

}
