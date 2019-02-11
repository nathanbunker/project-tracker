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
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillDay;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectClient;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class TrackServlet extends ClientServlet
{

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
  protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    response.setContentType("text/html;charset=UTF-8");
    HttpSession session = request.getSession(true);
    WebUser webUser = (WebUser) session.getAttribute(SESSION_VAR_WEB_USER);
    if (webUser == null)
    {
      RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
      dispatcher.forward(request, response);
      return;
    }

    PrintWriter out = response.getWriter();
    try
    {
      Session dataSession = getDataSession(session);
      TimeTracker timeTracker = (TimeTracker) session.getAttribute(SESSION_VAR_TIME_TRACKER);
      String action = request.getParameter("action");

      if (action != null)
      {
        if (action.equals("StopTimer"))
        {
          if (timeTracker != null)
          {
            timeTracker.stopClock(dataSession);
            if (webUser.getParentWebUser() != null)
            {
              response.sendRedirect("HomeServlet?action=Switch&childWebUserName="
                  + URLEncoder.encode(webUser.getParentWebUser().getUsername(), "UTF-8"));
              return;
            }
          }
        }
      }

      if (webUser.getParentWebUser() != null)
      {
        response.sendRedirect("HomeServlet");
        return;
      }

      String type = request.getParameter("type");
      if (type == null)
      {
        type = "Day";
      }

      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
      String billDateString = request.getParameter("billDate");
      Date billDate = null;
      if ((billDateString != null && billDateString.length() > 0) || !type.equals("Day"))
      {
        try
        {
          billDate = sdf.parse(billDateString);
          if (type.equals("Week"))
          {
            timeTracker = new TimeTracker(webUser, billDate, Calendar.WEEK_OF_YEAR, dataSession);
          } else if (type.equals("Month"))
          {
            timeTracker = new TimeTracker(webUser, billDate, Calendar.MONTH, dataSession);
          } else if (type.equals("Year"))
          {
            timeTracker = new TimeTracker(webUser, billDate, Calendar.YEAR, dataSession);
          } else
          {
            timeTracker = new TimeTracker(webUser, billDate, dataSession);
          }
          Calendar calendar = Calendar.getInstance();
          calendar.setTime(timeTracker.getStartDate());
          while (calendar.getTime().before(timeTracker.getEndDate()))
          {
            updateBillDay(dataSession, webUser, calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
          }
        } catch (ParseException pe)
        {
          request.setAttribute(REQUEST_VAR_MESSAGE, "Unable to parse date: " + pe.getMessage());
        }
      } else
      {
        billDateString = sdf.format(new Date());
      }

      printHtmlHead(out, "Track", request);

      out.println("<form action=\"TrackServlet\" method=\"GET\">");
      if (type.equals("Week"))
      {
        out.println("Date in Week");
      } else if (type.equals("Month"))
      {
        out.println("Date in Month");

      } else if (type.equals("Year"))
      {
        out.println("Date in Year");

      } else
      {
        out.println("Date");
      }
      out.println("<input type=\"text\" name=\"billDate\" value=\"" + billDateString + "\" size=\"10\">");
      out.println("Type ");
      out.println("<select name=\"type\">");
      out.println("<option value=\"Day\"" + (type.equals("Day") ? " selected" : "") + ">Day</option>");
      out.println("<option value=\"Week\"" + (type.equals("Week") ? " selected" : "") + ">Week</option>");
      out.println("<option value=\"Month\"" + (type.equals("Month") ? " selected" : "") + ">Month</option>");
      out.println("<option value=\"Year\"" + (type.equals("Year") ? " selected" : "") + ">Year</option>");
      out.println("</select>");
      out.println("<input type=\"submit\" name=\"action\" value=\"Refresh\">");
      out.println("</form>");

      makeTimeTrackReport(webUser, out, dataSession, timeTracker, type, true);
      printHtmlFoot(out);

    } finally
    {
      out.close();
    }
  }

  public static String makeTimeTrackReport(WebUser webUser, PrintWriter out, Session dataSession, TimeTracker timeTracker, String type,
      boolean showLinks)
  {

    String hours = TimeTracker.formatTime(timeTracker.getTotalMinsBillable());

    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    if (type.equals("Week"))
    {
      out.println("    <th class=\"boxed\">Total Billable Time for Week</th>");
    } else if (type.equals("Month"))
    {
      out.println("    <th class=\"boxed\">Total Billable Time for Month</th>");
    } else if (type.equals("Year"))
    {
      out.println("    <th class=\"boxed\">Total Billable Time for Year</th>");
    } else
    {
      out.println("    <th class=\"boxed\">Total Billable Time</th>");
    }
    out.println("    <td class=\"boxed\">" + hours + "</td>");
    out.println("  </tr>");
    out.println("</table> ");
    out.println("<br/> ");

    List<TimeEntry> timeEntryList;
    timeEntryList = new ArrayList<TrackServlet.TimeEntry>();

    Map<String, Integer> billCodeMap = timeTracker.getTotalMinsForBillCodeMap();
    for (String billCodeString : billCodeMap.keySet())
    {
      timeEntryList.add(new TimeEntry(billCodeString, billCodeMap.get(billCodeString), billCodeString));
    }
    Collections.sort(timeEntryList);

    if (timeEntryList.size() > 0)
    {

      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"6\">Time Tracked by Bill Code</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Bill Code</th>");
      out.println("    <th class=\"boxed\">Actual</th>");
      out.println("    <th class=\"boxed\">Rounded</th>");
      out.println("    <th class=\"boxed\">Billable</th>");
      out.println("  </tr>");
      int totalTime = 0;
      int totalBillable = 0;
      int totalBillableMoney = 0;
      for (TimeEntry timeEntry : timeEntryList)
      {
        String billCodeString = timeEntry.getId();
        BillCode billCode = (BillCode) dataSession.get(BillCode.class, billCodeString);
        int billable;
        if (type.equals("Day"))
        {
          billable = TimeTracker.roundTime(billCodeMap.get(billCodeString), billCode);
        } else
        {
          Query query = dataSession.createQuery("from BillDay where billCode = ? and bill_date >= ? and bill_date < ?");
          query.setParameter(0, billCode);
          query.setParameter(1, timeTracker.getStartDate());
          query.setParameter(2, timeTracker.getEndDate());
          billable = 0;
          List<BillDay> billDayList = query.list();
          for (BillDay billDay : billDayList)
          {
            billable += billDay.getBillMins();
          }
        }
        int billableMoney = (int) (billable * billCode.getBillRate() / 60.0 + 0.5);
        totalTime += billCodeMap.get(billCodeString);
        totalBillable += billable;
        totalBillableMoney += billableMoney;
        out.println("  <tr class=\"boxed\">");
        if (showLinks)
        {
          out.println("    <td class=\"boxed\"><a href=\"BillCodeServlet?billCode=" + billCode.getBillCode() + "\" class=\"button\">"
              + billCode.getBillLabel() + "</a></td>");
        } else
        {
          out.println("    <td class=\"boxed\">" + billCode.getBillLabel() + "</td>");
        }
        out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(billCodeMap.get(billCodeString)) + "</td>");
        out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(billable) + "</td>");
        if (billCode.getBillRate() > 0)
        {
          out.println("    <td class=\"boxed\">" + formatMoney(billableMoney) + "</td>");
        } else
        {
          out.println("    <td class=\"boxed\">&nbsp;</td>");
        }
        out.println("  </tr>");
      }
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Total</th>");
      out.println("    <th class=\"boxed\">Actual</th>");
      out.println("    <th class=\"boxed\">Rounded</th>");
      out.println("    <th class=\"boxed\">Billable</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\">Total</td>");
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalTime) + "</td>");
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalBillable) + "</td>");
      out.println("    <td class=\"boxed\">" + formatMoney(totalBillableMoney) + "</td>");
      out.println("  </tr>");
      out.println("</table> ");
      if (!showLinks)
      {
        out.println("<br/>");
      }
    }
    if (showLinks)
    {
      out.println("<p><a href=\"BillEntriesServlet\" class=\"button\">See all Bill Entries</a></p>");
      out.println("<p><a href=\"BillCodesServlet\" class=\"button\">See all Bill Codes</a></p>");
      out.println("<p><a href=\"BillBudgetsServlet\" class=\"button\">See all Bill Budgets</a></p>");
    }
    timeEntryList = new ArrayList<TrackServlet.TimeEntry>();
    Map<String, Integer> clientMap = timeTracker.getTotalMinsForClientMap();
    for (String clientCode : clientMap.keySet())
    {
      ProjectClient projectClient = getClient(dataSession, clientCode, webUser.getProviderId());
      if (projectClient != null)
      {
        timeEntryList.add(new TimeEntry(projectClient.getClientName(), clientMap.get(clientCode)));
      }
    }
    Collections.sort(timeEntryList);

    if (timeEntryList.size() > 0)
    {
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"6\">Time Tracked by Category</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Category</th>");
      out.println("    <th class=\"boxed\">Total Time</th>");
      out.println("  </tr>");
      for (TimeEntry timeEntry : timeEntryList)
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\">" + timeEntry.getLabel() + "</td>");
        out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(timeEntry.getMinutes()) + "</td>");
        out.println("  </tr>");
      }
      out.println("</table> ");
      out.println("<br/> ");

      timeEntryList = new ArrayList<TrackServlet.TimeEntry>();
      Map<Integer, Integer> projectMap = timeTracker.getTotalMinsForProjectMap();
      for (Integer projectId : projectMap.keySet())
      {
        Project project = (Project) dataSession.get(Project.class, projectId);
        if (project != null)
        {
          timeEntryList.add(new TimeEntry(project.getProjectName(), projectMap.get(projectId), projectId));
        }
      }
      Collections.sort(timeEntryList);
    }

    if (timeEntryList.size() > 0)
    {
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"6\">Time Tracked by Project</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Total Time</th>");
      out.println("  </tr>");
      for (TimeEntry timeEntry : timeEntryList)
      {
        out.println("  <tr class=\"boxed\">");
        if (showLinks)
        {
          out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + timeEntry.getId() + "\" class=\"button\">"
              + timeEntry.getLabel() + "</a></td>");
        } else
        {
          out.println("    <td class=\"boxed\">" + timeEntry.getLabel() + "</td>");
        }
        out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(timeEntry.getMinutes()) + "</td>");
        out.println("  </tr>");
      }
      out.println("</table> ");
      out.println("<br/> ");

    }

    return hours;
  }

  public static void updateBillDay(Session dataSession, WebUser webUser, Date day)
  {
    updateBillDay(dataSession, new TimeTracker(webUser, day, dataSession), day);
  }

  private static void updateBillDay(Session dataSession, TimeTracker timeTracker, Date billDate)
  {
    Map<String, Integer> billCodeMap = timeTracker.getTotalMinsForBillCodeMap();
    for (String billCodeString : billCodeMap.keySet())
    {
      BillCode billCode = (BillCode) dataSession.get(BillCode.class, billCodeString);
      int billMins = TimeTracker.roundTime(billCodeMap.get(billCodeString), billCode);
      Query query = dataSession.createQuery("from BillDay where billCode = ? and billDate = ?");
      query.setParameter(0, billCode);
      query.setParameter(1, billDate);
      List<BillDay> billDayList = query.list();
      BillDay billDay;
      if (billDayList.size() > 0)
      {
        billDay = billDayList.get(0);
      } else
      {
        billDay = new BillDay();
        billDay.setBillCode(billCode);
        billDay.setBillDate(billDate);
      }
      billDay.setBillMins(billMins);
      query = dataSession.createQuery("from BillBudget where billCode = ? and start_date <= ? and end_date > ?");
      query.setParameter(0, billCode);
      query.setParameter(1, billDate);
      query.setParameter(2, billDate);
      List<BillBudget> billBudgetList = query.list();
      if (billBudgetList.size() > 0)
      {
        billDay.setBillBudget(billBudgetList.get(0));
      } else
      {
        billDay.setBillBudget(null);
      }
      Transaction trans = dataSession.beginTransaction();
      try
      {
        dataSession.saveOrUpdate(billDay);
      } finally
      {
        trans.commit();
      }
    }
  }

  protected static ProjectClient getClient(Session dataSession, String clientCode, String providerId)
  {
    Query query;
    query = dataSession.createQuery("from ProjectClient where id.clientCode = ? and id.providerId = ?");
    query.setParameter(0, clientCode);
    query.setParameter(1, providerId);
    List<ProjectClient> projectClientList = query.list();
    ProjectClient projectClient = null;
    if (projectClientList.size() > 0)
    {
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
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
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
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    processRequest(request, response);
  }

  private static String formatMoney(int i)
  {
    if (i == 0)
    {
      return "&nbsp;";
    }
    if (i < 1000)
    {
      return "" + i;
    }
    int thousand = i / 1000;
    String s = "" + i;
    return s.substring(0, s.length() - 3) + "," + s.substring(s.length() - 3);
  }

  private static class TimeEntry implements Comparable<TimeEntry>
  {
    private String label;
    private int minutes;
    private String id;

    public String getId()
    {
      return id;
    }

    public TimeEntry(String label, int minutes) {
      this.label = label;
      this.minutes = minutes;
    }

    public TimeEntry(String label, int minutes, int id) {
      this.label = label;
      this.minutes = minutes;
      this.id = String.valueOf(id);
    }

    public TimeEntry(String label, int minutes, String id) {
      this.label = label;
      this.minutes = minutes;
      this.id = id;
    }

    public String getLabel()
    {
      return label;
    }

    public int getMinutes()
    {
      return minutes;
    }

    public int compareTo(TimeEntry te)
    {
      return te.minutes - this.minutes;
    }

  }

}
