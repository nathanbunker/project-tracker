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
import java.util.Date;
import java.util.Enumeration;
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
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillExpected;
import org.openimmunizationsoftware.pt.model.BillExpectedId;
import org.openimmunizationsoftware.pt.model.BillMonth;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BillBudgetsServlet extends ClientServlet {

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
    if (webUser == null || webUser.getParentWebUser() != null) {
      RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
      dispatcher.forward(request, response);
      return;
    }

    PrintWriter out = response.getWriter();
    try {
      Session dataSession = getDataSession(session);

      String action = request.getParameter("action");
      if (action != null) {
        if (action.equals("Update Time")) {
          Transaction transaction = dataSession.beginTransaction();
          SimpleDateFormat formatCompact = new SimpleDateFormat("yyyyMMdd");
          Enumeration<String> enumeration = request.getParameterNames();
          while (enumeration.hasMoreElements()) {
            String paramName = enumeration.nextElement();
            if (paramName.startsWith("wd")) {
              Date date = null;
              String timeString = "0";
              try {
                date = formatCompact.parse(paramName.substring(2));
                timeString = request.getParameter(paramName);
              } catch (ParseException pe) {
                throw new ServletException("Unable to parse date '" + paramName + "'");
              }
              Query query = dataSession
                  .createQuery("from BillExpected where id.username = ? and id.billDate = ?");
              query.setParameter(0, webUser.getUsername());
              query.setParameter(1, date);
              List<BillExpected> billExpectedList = query.list();
              BillExpected billExpected = null;
              if (billExpectedList.size() > 0) {
                billExpected = billExpectedList.get(0);
                billExpected.setBillMins(TimeTracker.readTime(timeString));
                dataSession.update(billExpected);
              } else {
                billExpected = new BillExpected(new BillExpectedId(webUser.getUsername(), date),
                    TimeTracker.readTime(timeString), 0);
                dataSession.save(billExpected);
              }
            }
          }

          transaction.commit();
        }
      }
      printHtmlHead(out, "Track", request);

      runReport(webUser, out, dataSession, true);
      printHtmlFoot(out);

    } finally {
      out.close();
    }
  }

  private static class WorkingDay {
    private Date date = null;
    private boolean isWeekDay = false;
  }

  public static void runReport(WebUser webUser, PrintWriter out, Session dataSession,
      boolean showLinks) {
    List<Date> monthDateList = new ArrayList<Date>();
    Date today = new Date();
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    monthDateList.add(calendar.getTime());
    for (int i = 0; i < 6; i++) {
      calendar.add(Calendar.MONTH, 1);
      monthDateList.add(calendar.getTime());
    }

    List<WorkingDay>[] workingDayList = new ArrayList[monthDateList.size()];
    int[] workingDayCountsPerMonth = new int[monthDateList.size()];
    for (int i = 1; i < monthDateList.size(); i++) {
      workingDayList[i] = new ArrayList<WorkingDay>();
      int workingDayCount = 0;
      Date startDate = monthDateList.get(i - 1);
      Date endDate = monthDateList.get(i);
      Calendar workingDayCalendar = Calendar.getInstance();
      workingDayCalendar.setTime(startDate);
      while (workingDayCalendar.getTime().before(today)) {
        WorkingDay workingDay = new WorkingDay();
        workingDay.date = workingDayCalendar.getTime();
        workingDayCalendar.add(Calendar.DAY_OF_MONTH, 1);
        if (workingDayCalendar.getTime().before(today)) {
          workingDayList[i].add(workingDay);
        }
      }
      if (workingDayCalendar.get(Calendar.DAY_OF_MONTH) > 1) {
        workingDayCalendar.add(Calendar.DAY_OF_MONTH, -1);
      }

      while (workingDayCalendar.getTime().before(endDate)) {
        WorkingDay workingDay = new WorkingDay();
        workingDay.date = workingDayCalendar.getTime();
        workingDayList[i].add(workingDay);

        if (workingDayCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
            && workingDayCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
          workingDayCount++;
          workingDay.isWeekDay = true;
        } else {
          workingDay.isWeekDay = false;
        }
        workingDayCalendar.add(Calendar.DAY_OF_MONTH, 1);
      }
      workingDayCountsPerMonth[i] = workingDayCount;
    }

    int[] totalMins = new int[monthDateList.size()];
    int[] totalBillings = new int[monthDateList.size()];

    Query query;
    query = dataSession.createQuery(
        "from BillBudget where billCode.providerId = ? and billCode.visible = 'Y'  and startDate <= ? and endDate > ? order by billCode.billLabel, billBudgetCode");
    query.setParameter(0, webUser.getProviderId());
    query.setParameter(1, today);
    query.setParameter(2, today);
    List<BillBudget> billBudgetList = query.list();

    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");

    out.println("<table class=\"boxed\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\" colspan=\"4\">Bill Budgets</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Bill Label</th>");
    out.println("    <th class=\"boxed\">Budget</th>");
    out.println("    <th class=\"boxed\">Hours</th>");
    out.println("    <th class=\"boxed\">Remaining</th>");
    out.println("  </tr>");
    for (BillBudget billBudget : billBudgetList) {
      BillCode billCode = billBudget.getBillCode();
      BillCodeEditServlet.updateBillMonths(billCode, billBudget, dataSession);
      out.println("  <tr class=\"boxed\">");
      if (showLinks) {
        out.println(
            "    <td class=\"boxed\"><a href=\"BillCodeServlet?billCode=" + billCode.getBillCode()
                + "\" class=\"button\">" + n(billCode.getBillLabel()) + "</a></td>");
        out.println("    <td class=\"boxed\"><a href=\"BillBudgetServlet?billBudgetId="
            + billBudget.getBillBudgetId() + "\" class=\"button\">" + billBudget.getBillBudgetCode()
            + "</a></td>");
      } else {
        out.println("    <td class=\"boxed\">" + n(billCode.getBillLabel()) + "</td>");
        out.println("    <td class=\"boxed\">" + billBudget.getBillBudgetCode() + "</td>");
      }
      out.println(
          "    <td class=\"boxed\">" + TimeTracker.formatTime(billBudget.getBillMins()) + "</td>");
      out.println("    <td class=\"boxed\">"
          + TimeTracker.formatTime(billBudget.getBillMinsRemaining()) + "</td>");
      out.println("  </tr>");
    }
    out.println("</table>");
    out.println("</br>");

    out.println("<table class=\"boxed\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\" colspan=\"" + (monthDateList.size() + 2)
        + "\">Remaining Hours</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Bill Label</th>");
    out.println(
        "    <th class=\"boxed\">" + sdfMonth.format(monthDateList.get(0)) + " Actual</th>");
    for (int i = 1; i < monthDateList.size(); i++) {
      out.println("    <th class=\"boxed\">" + sdfMonth.format(monthDateList.get(i - 1)) + "</th>");
    }
    out.println("  </tr>");
    for (BillBudget billBudget : billBudgetList) {
      BillCode billCode = billBudget.getBillCode();
      BillCodeEditServlet.updateBillMonths(billCode, billBudget, dataSession);
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\">" + n(billCode.getBillLabel()) + "</td>");

      Date startDate = monthDateList.get(0);

      for (int i = 1; i < monthDateList.size(); i++) {
        Date endDate = monthDateList.get(i);
        query = dataSession.createQuery(
            "from BillMonth where billBudget = ? and billDate >= ? and billDate < ? order by billDate");
        query.setParameter(0, billBudget);
        query.setParameter(1, startDate);
        query.setParameter(2, endDate);
        List<BillMonth> billMonthList = query.list();
        if (billMonthList.size() == 0) {
          out.println("    <td class=\"boxed\">&nbsp;</td>");
        } else {
          BillMonth billMonth = billMonthList.get(0);
          int billMinsPending = billMonth.getBillMinsExpected() - billMonth.getBillMinsActual();
          if (i == 1) {
            out.println("    <td class=\"boxed\">"
                + TimeTracker.formatTime(billMonth.getBillMinsActual()) + "</td>");
          }
          out.println(
              "    <td class=\"boxed\">" + TimeTracker.formatTime(billMinsPending) + "</td>");
          totalMins[i] += billMinsPending;
          totalBillings[i] +=
              (int) (billMinsPending * billMonth.getBillBudget().getBillCode().getBillRate() / 100.0
                  + 0.5);
        }
        startDate = endDate;
      }

      out.println("  </tr>");
    }

    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\" colspan=\"2\">Total</th>");
    for (int i = 1; i < monthDateList.size(); i++) {
      out.println("    <th class=\"boxed\">" + sdfMonth.format(monthDateList.get(i - 1)) + "</th>");
    }
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <td class=\"boxed\" colspan=\"2\">Remaining</td>");
    for (int i = 1; i < monthDateList.size(); i++) {
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalMins[i]) + "</td>");
    }
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <td class=\"boxed\" colspan=\"2\">Working Days Left</td>");

    for (int i = 1; i < monthDateList.size(); i++) {
      out.println("    <td class=\"boxed\">" + workingDayCountsPerMonth[i] + "</td>");
    }

    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <td class=\"boxed\" colspan=\"2\">Per Working Day</td>");
    for (int i = 1; i < monthDateList.size(); i++) {
      if (workingDayCountsPerMonth[i] != 0) {
        out.println("    <td class=\"boxed\">"
            + TimeTracker.formatTime(totalMins[i] / workingDayCountsPerMonth[i]) + "</td>");
      } else {
        out.println("    <td class=\"boxed\">&nbsp;</td>");
      }
    }
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <td class=\"boxed\" colspan=\"2\">Billable Available</td>");
    for (int i = 1; i < monthDateList.size(); i++) {
      if (workingDayCountsPerMonth[i] != 0) {
        out.println(
            "    <td class=\"boxed\">$" + TrackServlet.formatMoney(totalBillings[i]) + "</td>");
      } else {
        out.println("    <td class=\"boxed\">&nbsp;</td>");
      }
    }
    out.println("  </tr>");
    out.println("</table> ");
    out.println("<br/>");

    SimpleDateFormat daySdf = new SimpleDateFormat("MMM dd EEE");
    out.println("<table class=\"boxed\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\" colspan=\"4\">Billable Work Completed</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Date</th>");
    out.println("    <th class=\"boxed\">Hours</th>");
    out.println("    <th class=\"boxed\">Rate</th>");
    out.println("    <th class=\"boxed\">Billable</th>");
    out.println("  </tr>");
    int totalTimeToday = 0;
    int totalBillableRunning = 0;
    int totalTimeRunning = 0;
    int totalBillableMoneyRunning = 0;
    for (WorkingDay workingDay : workingDayList[1]) {
      TimeTracker timeTracker = new TimeTracker(webUser, workingDay.date, dataSession);
      Map<String, Integer> billCodeMap = timeTracker.getTotalMinsForBillCodeMap();
      List<TimeEntry> timeEntryList = timeTracker.createTimeEntryList();
      int totalBillableMoney = 0;
      int totalTime = 0;
      int totalBillable = 0;
      for (TimeEntry timeEntry : timeEntryList) {
        String billCodeString = timeEntry.getId();
        BillCode billCode = (BillCode) dataSession.get(BillCode.class, billCodeString);
        int billable = TimeTracker.roundTime(billCodeMap.get(billCodeString), billCode);
        int billableMoney = (int) (billable * billCode.getBillRate() / 60.0 + 0.5);

        totalTime += billCodeMap.get(billCodeString);
        totalBillable += billable;
        totalBillableMoney += billableMoney;
      }
      totalTimeRunning += totalTime;
      totalBillableRunning += totalBillable;
      totalBillableMoneyRunning += totalBillableMoney;
      totalTimeToday = totalTime;

      out.println("  <tr>");
      out.println("    <td class=\"boxed\">" + daySdf.format(workingDay.date) + "</td>");
      if (timeTracker.getTotalMinsBillable() > 0) {
        out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalTime) + "</td>");
        int rate = ((int) (totalBillableMoney / (totalTime / 60.0) + 0.5));
        out.println("    <td class=\"boxed\">$" + TrackServlet.formatMoney(rate) + "</td>");
        out.println(
            "    <td class=\"boxed\">$" + TrackServlet.formatMoney(totalBillableMoney) + "</td>");
      } else {
        out.println("    <td class=\"boxed\">&nbsp;</td>");
        out.println("    <td class=\"boxed\">&nbsp;</td>");
        out.println("    <td class=\"boxed\">&nbsp;</td>");
      }
      out.println("  </tr>");
      if (workingDay.date.after(today)) {
        break;
      }
    }
    out.println("  <tr>");
    out.println("    <td class=\"boxed\">Total</td>");
    out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalTimeRunning) + "</td>");
    if (totalBillableRunning > 0) {
      int rate = ((int) (totalBillableMoneyRunning / (totalBillableRunning / 60.0) + 0.5));
      out.println("    <td class=\"boxed\">$" + TrackServlet.formatMoney(rate) + "</td>");
    } else {
      out.println("    <td class=\"boxed\">&nbsp;</td>");
    }
    out.println("    <td class=\"boxed\">$" + TrackServlet.formatMoney(totalBillableMoneyRunning)
        + "</td>");
    out.println("  </tr>");

    out.println("</table> ");
    out.println("<br/>");

    SimpleDateFormat formatCompact = new SimpleDateFormat("yyyyMMdd");
    if (showLinks) {
      out.println("<form method=\"POST\" action=\"BillBudgetsServlet\">");
    }
    out.println("<table class=\"boxed\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\" colspan=\"4\">Anticipated Work Schedule</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Date</th>");
    out.println("    <th class=\"boxed\">Hours</th>");
    out.println("  </tr>");
    int totalTime = 0;
    for (WorkingDay workingDay : workingDayList[1]) {
      if (!workingDay.date.before(today)) {
        query =
            dataSession.createQuery("from BillExpected where id.username = ? and id.billDate = ?");
        query.setParameter(0, webUser.getUsername());
        query.setParameter(1, workingDay.date);
        List<BillExpected> billExpectedList = query.list();
        BillExpected billExpected = null;
        if (billExpectedList.size() > 0) {
          billExpected = billExpectedList.get(0);
        } else {
          Transaction transaction = dataSession.beginTransaction();
          billExpected =
              new BillExpected(new BillExpectedId(webUser.getUsername(), workingDay.date),
                  workingDay.isWeekDay ? 8 * 60 : 0, 0);
          dataSession.save(billExpected);
          transaction.commit();
        }
        totalTime += billExpected.getBillMins();
        out.println("  <tr>");
        out.println("    <td class=\"boxed\">" + daySdf.format(workingDay.date) + "</td>");
        if (showLinks) {
          out.println("    <td class=\"boxed\"><input type=\"text\" size=\"5\" name=\"wd"
              + formatCompact.format(workingDay.date) + "\" value=\""
              + TimeTracker.formatTime(billExpected.getBillMins()) + "\"/></td>");
        } else {
          out.println("    <td class=\"boxed\">"
              + TimeTracker.formatTime(billExpected.getBillMins()) + "</td>");
        }
        out.println("  </tr>");

      }
    }
    out.println("  <tr>");
    out.println("    <td class=\"boxed\">Time Remaining</td>");

    int timeLeftToWork = totalTime - totalTimeToday;
    if (timeLeftToWork < 0) {
      timeLeftToWork = 0;
    }
    out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(timeLeftToWork) + "</td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td class=\"boxed\">Planned Work</td>");
    out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalMins[1]) + "</td>");
    out.println("  </tr>");
    if (showLinks) {
      out.println("  <tr>");
      out.println("    <td class=\"boxed\"></td>");
      out.println(
          "    <td class=\"boxed\"><input type=\"submit\" name=\"action\" value=\"Update Time\"/></td>");
      out.println("  </tr>");
    }
    out.println("</table> ");
    if (showLinks) {
      out.println("</form>");
    }
    out.println("<br/>");

    out.println("<table class=\"boxed\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\" colspan=\"4\">Work Progress and Month End Estimate</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">&nbsp;</th>");
    out.println("    <th class=\"boxed\">Hours</th>");
    out.println("    <th class=\"boxed\">Billable</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Completed</th>");
    out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalTimeRunning) + "</td>");
    out.println("    <td class=\"boxed\">$" + TrackServlet.formatMoney(totalBillableMoneyRunning)
        + "</td>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Remaining</th>");
    out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalMins[1]) + "</td>");
    out.println("    <td class=\"boxed\">$" + TrackServlet.formatMoney(totalBillings[1]) + "</td>");
    out.println("  </tr>");
    int additionalBillable = totalBillings[1];
    // 100 > 50
    if (totalBillings[1] > timeLeftToWork) {
      double reduction = ((double) timeLeftToWork) / totalBillings[1];
      additionalBillable = ((int) (totalBillings[1] * reduction + 0.5));
    }
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Scheduled</th>");
    out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(timeLeftToWork) + "</td>");
    out.println(
        "    <td class=\"boxed\">$" + TrackServlet.formatMoney(additionalBillable) + "</td>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Low Estimate</th>");
    out.println("    <td class=\"boxed\">"
        + TimeTracker.formatTime(totalTimeRunning + timeLeftToWork) + "</td>");
    out.println("    <td class=\"boxed\">$"
        + TrackServlet.formatMoney(totalBillableMoneyRunning + additionalBillable) + "</td>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">High Estimate</th>");
    out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalTimeRunning + totalMins[1])
        + "</td>");
    out.println("    <td class=\"boxed\">$"
        + TrackServlet.formatMoney(totalBillableMoneyRunning + totalBillings[1]) + "</td>");
    out.println("  </tr>");
    out.println("</table>");
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

}
