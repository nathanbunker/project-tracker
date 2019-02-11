/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openimmunizationsoftware.pt.CentralControl;
import org.openimmunizationsoftware.pt.manager.MailManager;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ReportProfile;
import org.openimmunizationsoftware.pt.model.ReportSchedule;
import org.openimmunizationsoftware.pt.model.TrackerKeys;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.report.GenerateReport;
import org.openimmunizationsoftware.pt.report.ReportBatch;
import org.openimmunizationsoftware.pt.report.definition.ReportGenerator;
import org.openimmunizationsoftware.pt.report.definition.ReportParameter;

/**
 * 
 * @author nathan
 */
public class ReportRunServlet extends ClientServlet
{

  private static Map<Integer, DailyReportDetails> dailyReportDetailsMap = new HashMap<Integer, ReportRunServlet.DailyReportDetails>();

  private DailyReportRunner dailyReportRunner = null;

  private static class DailyReportDetails
  {
    private Date sentAttempted;
    private String reportText = "";
    private String statusMessage = "";
    private boolean sendingNow = false;

    public Date getSentAttempted()
    {
      return sentAttempted;
    }

    public void setSentAttempted(Date sentAttempted)
    {
      this.sentAttempted = sentAttempted;
    }

    public String getReportText()
    {
      return reportText;
    }

    public void setReportText(String reportText)
    {
      this.reportText = reportText;
    }

    public String getStatusMessage()
    {
      return statusMessage;
    }

    public void setStatusMessage(String statusMessage)
    {
      this.statusMessage = statusMessage;
    }

    public boolean isSendingNow()
    {
      return sendingNow;
    }

    public void setSendingNow(boolean sendingNow)
    {
      this.sendingNow = sendingNow;
    }
  }

  @Override
  public void init() throws ServletException
  {

    // TODO Auto-generated method stub
    super.init();
    dailyReportRunner = new DailyReportRunner();
    dailyReportRunner.start();
  }

  private class DailyReportRunner extends Thread
  {
    Session dataSession = null;
    private Date lastRunTime = new Date();
    private Date nextRunTime;

    public Date getNextRunTime()
    {
      return nextRunTime;
    }

    public DailyReportRunner() {
      init();

    }

    private void init()
    {
      if (dataSession == null)
      {
        SessionFactory factory = CentralControl.getSessionFactory();
        dataSession = factory.openSession();
      }
      String dailyTime = TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_REPORT_DAILY_TIME, dataSession);
      if (dailyTime.equals(""))
      {
        dailyTime = "1:30";
      }
      int hour = 1;
      int min = 30;
      try
      {

        int pos = dailyTime.indexOf(":");
        if (pos != -1)
        {
          hour = Integer.parseInt(dailyTime.substring(0, pos));
          min = Integer.parseInt(dailyTime.substring(pos + 1));
        }
      } catch (Exception e)
      {
        hour = 1;
        min = 30;
      }
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.HOUR_OF_DAY, hour);
      cal.set(Calendar.MINUTE, min);
      cal.set(Calendar.SECOND, 0);
      if (cal.getTime().before(lastRunTime))
      {
        // too late for today, will schedule for tomorrow.
        cal.add(Calendar.DAY_OF_MONTH, 1);
      }
      nextRunTime = cal.getTime();
    }

    private boolean keepRunning = true;
    private String statusMessage = "Automatic daily report sender initialized";

    public String getStatusMessage()
    {
      return statusMessage;
    }

    @Override
    public void run()
    {
      while (keepRunning)
      {
        init();
        statusMessage = "Looking to see if it is time to send reports";
        Date now = new Date();
        if (now.after(nextRunTime))
        {
          lastRunTime = now;
          try
          {
            runReportsForToday(dataSession, now);
          } catch (Exception e)
          {
            statusMessage = "Exception ocurred: " + e.getMessage();
            e.printStackTrace();
          }
        }

        synchronized (this)
        {
          try
          {
            statusMessage = "Waiting, will check again soon to see if reports should be sent";
            this.wait(1000 * 60 * 5); // check every 5 minutes
          } catch (InterruptedException ie)
          {
            // continue;
          }
        }

      }
    }

  }

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
    if (webUser == null || !webUser.isUserTypeAdmin())
    {
      RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
      dispatcher.forward(request, response);
      return;
    }

    PrintWriter out = response.getWriter();
    try
    {
      Session dataSession = getDataSession(session);

      String action = request.getParameter("action");
      if (action == null)
      {
        action = "Show All";
      }

      if (action.equals("Run All"))
      {
        runReportsForToday(dataSession, new Date());
        action = "Show All";
      }
      if (action.equals("Run") || action.equals("Email"))
      {

        Query query = dataSession.createQuery("from ReportProfile where profileId = ?");
        query.setParameter(0, Integer.parseInt(request.getParameter("profileId")));
        List<ReportProfile> reportProfileList = query.list();
        ReportProfile reportProfile = reportProfileList.get(0);
        ReportsServlet.loadReportProfileObject(dataSession, reportProfile);

        String period = request.getParameter("period");
        String runDateString = request.getParameter("runDate");
        Date runDate = null;
        String message = null;
        try
        {
          runDate = ReportBatch.createSimpleDateFormat().parse(runDateString);
        } catch (ParseException pe)
        {
          message = "Unable to parse run date: " + pe.getMessage();
        }

        ReportBatch reportBatch = new ReportBatch();
        reportBatch.setProfileId(reportProfile.getProfileId());
        reportBatch.setRunDate(runDateString);
        reportBatch.setPeriod(period);
        Map<String, String> parameterValues = reportBatch.getParameterValues();

        if (message == null)
        {
          try
          {
            GenerateReport.populateStartEndDates(reportBatch);
          } catch (Exception e)
          {
            message = "Unable to set start and end dates for report: " + e;
          }
          if (message == null)
          {
            List<ReportParameter> reportParameters = null;
            try
            {
              reportParameters = reportProfile.getReportDefinition().getReportParameters();
            } catch (Exception e)
            {
              message = "Unable to get report definition: " + e;
            }
            if (message == null)
            {
              for (ReportParameter reportParameter : reportParameters)
              {
                String name = reportParameter.getName();
                String value = request.getParameter(name);
                if (reportParameter.getType().equals(ReportParameter.TYPE_CHECKBOX))
                {
                  value = value != null ? "true" : "false";
                }
                if (value == null)
                {
                  value = "";
                }
                parameterValues.put(name.toUpperCase(), value);
              }
            }
          }
        }

        if (message != null)
        {
          request.setAttribute(REQUEST_VAR_MESSAGE, message);
        }

        String report = "<p><em>Report not run.</em></p>";
        printHtmlHead(out, "Reports", request);

        if (message == null)
        {
          DailyReportDetails dailyReportDetails = new DailyReportDetails();
          dailyReportDetails.setSentAttempted(new Date());
          if (action.equals("Email"))
          {
            dailyReportDetailsMap.put(reportProfile.getProfileId(), dailyReportDetails);
            dailyReportDetails.setSendingNow(true);
          }
          try
          {
            report = runReport(dataSession, action, reportProfile, runDate, parameterValues);
            dailyReportDetails.setReportText(report);
          } catch (Exception e)
          {
            dailyReportDetails.setReportText("Unable to run report: " + e.getMessage());
            out.println("<p>Unable to run report </p>");
            out.println("<pre>");
            e.printStackTrace(out);
            out.println("</pre>");
          } finally
          {
            dailyReportDetails.setSendingNow(false);
          }
        }
        out.print(report);
        printHtmlFoot(out);
      } else if (action.equals("Show All"))
      {

        printHtmlHead(out, "Reports", request);

        List<ReportProfile> reportProfileList = new ArrayList<ReportProfile>();
        for (int profileId : dailyReportDetailsMap.keySet())
        {
          reportProfileList.add((ReportProfile) dataSession.get(ReportProfile.class, profileId));
        }

        Collections.sort(reportProfileList, new Comparator<ReportProfile>() {

          public int compare(ReportProfile o1, ReportProfile o2)
          {
            return o1.getProfileLabel().compareTo(o2.getProfileLabel());
          }
        });

        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"5\">Reports Sent</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Report</th>");
        out.println("    <th class=\"boxed\">Date</th>");
        out.println("    <th class=\"boxed\">Status</th>");
        out.println("    <th class=\"boxed\">Message</th>");
        out.println("    <th class=\"boxed\">&nbsp;</th>");
        out.println("  </tr>");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        for (ReportProfile reportProfile : reportProfileList)
        {
          ReportsServlet.loadReportProfileObject(dataSession, reportProfile);
          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\"><a href=\"ReportServlet?profileId=" + reportProfile.getProfileId() + "\" class=\"button\">"
              + reportProfile.getProfileLabel() + "</a></td>");

          ReportSchedule reportSchedule = reportProfile.getReportSchedule();

          DailyReportDetails dailyReportDetails = dailyReportDetailsMap.get(reportProfile.getProfileId());

          out.println("    <td class=\"boxed\">" + sdf.format(dailyReportDetails.getSentAttempted()) + "</td>");
          if (reportSchedule != null)
          {
            out.println("    <td class=\"boxed\">" + (reportSchedule.getStatus() != null ? reportSchedule.getStatusText() : "") + "</td>");
          } else
          {
            out.println("    <td class=\"boxed\">&nbsp;</td>");
          }
          out.println("    <td class=\"boxed\">" + dailyReportDetails.getStatusMessage() + "</td>");
          if (dailyReportDetails.getReportText().length() > 0)
          {
            out.println("    <td class=\"boxed\"><a href=\"ReportRunServlet?action=Show&profileId=" + reportProfile.getProfileId()
                + "\" class=\"box\">View</td>");
          } else
          {
            out.println("    <td class=\"boxed\">&nbsp;</td>");

          }
          out.println("  </tr>");
        }
        out.println("</table>");
        out.println("<h2>Run All</h2>");
        out.println("<p>The automatic report runner is set to run daily reports at " + sdf.format(dailyReportRunner.getNextRunTime()) + ". ");
        out.println("The last status message was: " + dailyReportRunner.getStatusMessage() + ". </p>");
        out.println("<p><a href=\"ReportRunServlet?action=Run%20All\">Run and send</a> all of today's reports now.</p>");

        printHtmlFoot(out);
      } else if (action.equals("Show"))
      {
        DailyReportDetails dailyReportDetails = dailyReportDetailsMap.get(Integer.parseInt(request.getParameter("profileId")));
        printHtmlHead(out, "Reports", request);
        out.print(dailyReportDetails.getReportText());
        printHtmlFoot(out);
      }

    } finally
    {
      out.close();
    }
  }

  private static void runReportsForToday(Session dataSession, Date runDate)
  {

    Query query;

    query = dataSession.createQuery("from TrackerKeys where id.keyType = ? and id.keyName = ? and keyValue = 'Y'");
    query.setParameter(0, TrackerKeysManager.KEY_TYPE_USER);
    query.setParameter(1, TrackerKeysManager.KEY_TRACK_TIME);
    List<TrackerKeys> trackerKeysList = query.list();
    for (TrackerKeys trackerKeys : trackerKeysList)
    {
      query = dataSession.createQuery("from WebUser where username= ? ");
      query.setParameter(0, trackerKeys.getId().getKeyId());
      List<WebUser> webUserList = query.list();
      if (webUserList.size() > 0)
      {
        WebUser webUser = webUserList.get(0);
        webUser.setProjectContact((ProjectContact) dataSession.get(ProjectContact.class, webUser.getContactId()));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(runDate);
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date billDate = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        while (calendar.getTime().before(endDate))
        {
          TrackServlet.updateBillDay(dataSession, webUser, calendar.getTime());
          calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        printWriter.println("<html>");
        printWriter.println("  <body>");

        printWriter.println("    <h1>Yesterday</h1>");

        TimeTracker timeTracker = new TimeTracker(webUser, billDate, dataSession);
        String hoursWorked = TrackServlet.makeTimeTrackReport(webUser, printWriter, dataSession, timeTracker, "Day", false);

        printWriter.println("    <h1>Work Remaining</h1>");
        BillBudgetsServlet.runReport(webUser, printWriter, dataSession, false);
        
        printWriter.println("    <h1>For Week</h1>");
        timeTracker = new TimeTracker(webUser, billDate, Calendar.WEEK_OF_YEAR, dataSession);
        TrackServlet.makeTimeTrackReport(webUser, printWriter, dataSession, timeTracker, "Week", false);

        SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM yyyy");
        printWriter.println("    <h1>" + sdfMonth.format(billDate) + "</h1>");
        timeTracker = new TimeTracker(webUser, billDate, Calendar.MONTH, dataSession);
        TrackServlet.makeTimeTrackReport(webUser, printWriter, dataSession, timeTracker, "Month", false);

        printWriter.println("  </body>");
        printWriter.println("</html>");
        printWriter.close();

        String report = stringWriter.toString();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

        MailManager mailManager = new MailManager(dataSession);

        try
        {
          printReportToEmail(report, printWriter);
          printWriter.close();
          mailManager.sendEmail("Time Tracker Update: " + hoursWorked + " hours worked", stringWriter.toString(), webUser.getProjectContact()
              .getEmail());
        } catch (Exception e)
        {
          e.printStackTrace();
        }

      }

    }

    query = dataSession.createQuery("from ReportProfile where use_status = 'E'");
    List<ReportProfile> reportProfileList = query.list();
    for (ReportProfile reportProfile : reportProfileList)
    {
      ReportsServlet.loadReportProfileObject(dataSession, reportProfile);
      ReportSchedule reportSchedule = reportProfile.getReportSchedule();
      if (reportSchedule != null && reportSchedule.isStatusQueued()
          && (reportSchedule.isDaily() || reportSchedule.isMonthly() || reportSchedule.isYearly()) && reportSchedule.canRun())
      {
        DailyReportDetails dailyReportDetails = new DailyReportDetails();
        dailyReportDetails.setSentAttempted(new Date());
        dailyReportDetailsMap.put(reportProfile.getProfileId(), dailyReportDetails);
        dailyReportDetails.setSendingNow(true);
        StringBuilder sb = new StringBuilder();
        try
        {
          ReportBatch reportBatch = new ReportBatch();
          reportBatch.setProfileId(reportProfile.getProfileId());
          reportBatch.setRunDate(ReportBatch.createSimpleDateFormat().format(runDate));
          reportBatch.setPeriod(reportSchedule.getPeriod().length() > 1 ? reportSchedule.getPeriod().substring(0, 1) : "");
          Map<String, String> parameterValues = reportBatch.getParameterValues();

          boolean goodToGo = true;
          try
          {
            GenerateReport.populateStartEndDates(reportBatch);

          } catch (Exception e)
          {
            sb.append("Unable to set start and end dates for report: " + e + " ");
            goodToGo = false;
          }
          if (goodToGo)
          {
            List<ReportParameter> reportParameters = null;
            try
            {
              reportParameters = reportProfile.getReportDefinition().getReportParameters();
            } catch (Exception e)
            {
              sb.append("Unable to get report definition: " + e + " ");
              goodToGo = false;
            }
            if (goodToGo)
            {
              for (ReportParameter reportParameter : reportParameters)
              {
                String name = reportParameter.getName();
                String value = TrackerKeysManager.getReportKeyValue(name, reportParameter.getDefaultValue(), reportProfile, dataSession);
                if (value == null)
                {
                  value = "";
                }
                parameterValues.put(name.toUpperCase(), value);
              }
              try
              {
                String report = runReport(dataSession, "Email", reportProfile, runDate, parameterValues);
                dailyReportDetails.setReportText(report);
                sb.append("Report generated and sent");
              } catch (Exception e)
              {
                sb.append("Unable to run report: " + e.getMessage());
              }
            }
          }
        } finally
        {
          dailyReportDetails.setSendingNow(false);
          dailyReportDetails.setStatusMessage(sb.toString());
        }
      }
    }
  }

  private static String runReport(Session dataSession, String action, ReportProfile reportProfile, Date runDate, Map<String, String> parameterValues)
      throws IOException, Exception
  {
    String report;
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    if (reportProfile.getProfileType().equals(ReportProfile.PROFILE_TYPE_XML_DEFINED))
    {
      ReportGenerator.generateReport(printWriter, null, parameterValues, reportProfile.getReportText(), dataSession);
    } else if (reportProfile.getProfileType().equals(ReportProfile.PROFILE_TYPE_PROGRES_REPORT))
    {
      int billBudgetId = Integer.parseInt(parameterValues.get("BILLBUDGETID"));
      BillBudget billBudget = (BillBudget) dataSession.get(BillBudget.class, billBudgetId);
      BillBudgetServlet.generateReport(printWriter, dataSession, billBudget, runDate);
    }
    printWriter.close();
    report = stringWriter.toString();
    if (action.equals("Email"))
    {
      MailManager mailManager = new MailManager(dataSession);
      ReportSchedule reportSchedule = reportProfile.getReportSchedule();
      stringWriter = new StringWriter();
      printWriter = new PrintWriter(stringWriter);
      GenerateReport.doHeader(printWriter, reportProfile);
      printReportToEmail(report, printWriter);
      GenerateReport.doFooter(printWriter, reportProfile, "Email");

      mailManager.sendEmail(reportSchedule.getName(), stringWriter.toString(), reportSchedule.getLocation());

    }
    return report;
  }

  public static void printReportToEmail(String report, PrintWriter printWriter) throws IOException
  {
    InputStream inputStream = new ByteArrayInputStream(report.getBytes());
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

    String line;
    while ((line = bufferedReader.readLine()) != null)
    {
      boolean substitutionFound = false;
      for (String[] sub : CSS_SUBSITUTIONS)
      {
        int pos = line.indexOf(sub[0]);
        if (pos >= 0)
        {
          pos = pos + sub[0].length();
          printWriter.print(line.substring(0, pos));
          printWriter.print(" style=\"");
          printWriter.print(sub[1]);
          printWriter.print("\"");
          printWriter.println(line.substring(pos));
          substitutionFound = true;
          break;

        }
      }
      if (!substitutionFound)
      {
        printWriter.println(line);
      }
    }
    bufferedReader.close();
  }

  private static String[][] CSS_SUBSITUTIONS = {
      { "<table class=\"boxed\"",
          "border-collapse: collapse; border-width: 1px; border-style: solid; padding-left:5px; padding-right:5px; border-color: #2B3E42;" },
      {
          "<th class=\"boxed\"",
          "background-color: #DDDDDD; text-align: left; vertical-align:top; border-collapse: collapse; border-width: 1px; border-style: solid; padding-left:5px; padding-right:5px; border-color: #2B3E42;" },
      { "<th class=\"title\"", "background-color: #990000; color: #DDDDDD; padding-left: 5px;" },
      {
          "<td class=\"boxed\"",
          "text-align: left; vertical-align:top; border-collapse: collapse; border-width: 1px; border-style: solid; padding-left:5px; padding-right:5px; border-color: #2B3E42;" } };

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

}
