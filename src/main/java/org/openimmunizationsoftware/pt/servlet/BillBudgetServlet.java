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
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillDay;
import org.openimmunizationsoftware.pt.model.BillMonth;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BillBudgetServlet extends ClientServlet
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
    if (webUser == null || webUser.getParentWebUser() != null)
    {
      RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
      dispatcher.forward(request, response);
      return;
    }

    PrintWriter out = response.getWriter();
    try
    {
      Session dataSession = getDataSession(session);
      printHtmlHead(out, "Track", request);

      BillBudget billBudget = (BillBudget) dataSession.get(BillBudget.class, Integer.parseInt(request.getParameter("billBudgetId")));
      Date today = new Date();

      generateReport(out, dataSession, billBudget, today);

      printHtmlFoot(out);

    } finally
    {
      out.close();
    }
  }

  public static void generateReport(PrintWriter out, Session dataSession, BillBudget billBudget, Date today)
  {
    BillCode billCode = billBudget.getBillCode();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

    Query query;
    out.println("<table class=\"boxed\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\" colspan=\"2\">Details</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Work for</th>");
    out.println("    <td class=\"boxed\">" + n(billCode.getBillLabel()) + "</td>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Budget</th>");
    out.println("    <td class=\"boxed\">" + n(billBudget.getBillBudgetCode()) + "</td>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Round to</th>");
    String billRound = (billCode.getBillRound() == 1 ? billCode.getBillRound() + " min" : billCode.getBillRound() + " mins");
    out.println("    <td class=\"boxed\">" + billRound + "</td>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Start Date</th>");
    out.println("    <td class=\"boxed\">" + (billBudget.getStartDate() == null ? "" : sdf.format(billBudget.getStartDate())) + "</td>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">End Date</th>");
    out.println("    <td class=\"boxed\">" + (billBudget.getEndDate() == null ? "" : sdf.format(billBudget.getEndDate())) + "</td>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Hours Allocated</th>");
    out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(billBudget.getBillMins()) + "</td>");
    out.println("  </tr>");
    out.println("</table> ");
    out.println("<br/>");
    out.println("<table class=\"boxed\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\" colspan=\"4\">Hours Worked</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Month</th>");
    out.println("    <th class=\"boxed\">Date</th>");
    out.println("    <th class=\"boxed\">Hours</th>");
    out.println("    <th class=\"boxed\">Remaining</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <td class=\"boxed\">Budget Start</td>");
    out.println("    <td class=\"boxed\">" + sdf.format(billBudget.getStartDate()) + "</td>");
    out.println("    <td class=\"boxed\">&nbsp;</td>");
    out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(billBudget.getBillMins()) + "</td>");
    out.println("  </tr>");

    List<Date[]> actionMonthDates = new ArrayList<Date[]>();
    Date prevStartDate = billBudget.getStartDate();

    int prevMonth = getMonth(billBudget.getStartDate());
    int totalMins = billBudget.getBillMins();
    int mins = 0;
    Date prevDate = billBudget.getStartDate();
    SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy");
    query = dataSession.createQuery("from BillDay where billBudget = ? and bill_date >= ? and bill_date < ? order by billDate asc");
    query.setParameter(0, billBudget);
    query.setParameter(1, billBudget.getStartDate());
    query.setParameter(2, billBudget.getEndDate());

    List<BillDay> billDayList = query.list();

    for (BillDay billDay : billDayList)
    {
      int month = getMonth(billDay.getBillDate());
      if (prevMonth != month)
      {
        if (mins > 0)
        {
          totalMins = totalMins - mins;
          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\">" + monthFormat.format(prevDate) + "</td>");
          out.println("    <td class=\"boxed\">" + sdf.format(prevDate) + "</td>");
          out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(mins) + "</td>");
          out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalMins) + "</td>");
          out.println("  </tr>");
          mins = 0;
          actionMonthDates.add(new Date[] { prevStartDate, billDay.getBillDate() });
          prevStartDate = billDay.getBillDate();
        }
      }
      mins += billDay.getBillMins();
      prevDate = billDay.getBillDate();
      prevMonth = month;
    }
    if (mins > 0)
    {
      totalMins = totalMins - mins;
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\">" + monthFormat.format(prevDate) + "</td>");
      out.println("    <td class=\"boxed\">" + sdf.format(prevDate) + "</td>");
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(mins) + "</td>");
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalMins) + "</td>");
      out.println("  </tr>");
      actionMonthDates.add(new Date[] { prevStartDate, billBudget.getEndDate() });
    }
    out.println("</table> ");
    out.println("<br/>");

    if (today.before(billBudget.getEndDate()))
    {
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"2\">Estimated Effort Remaining</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Month</th>");
      out.println("    <th class=\"boxed\">Estimate</th>");
      out.println("  </tr>");
      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.DAY_OF_MONTH, 1);
      Date startDate = calendar.getTime();
      while (startDate.before(billBudget.getEndDate()))
      {
        calendar.add(Calendar.MONTH, 1);
        Date endDate = calendar.getTime();
        query = dataSession.createQuery("from BillMonth where billBudget = ? and billDate >= ? and billDate < ? order by billDate");
        query.setParameter(0, billBudget);
        query.setParameter(1, startDate);
        query.setParameter(2, endDate);
        List<BillMonth> billMonthList = query.list();
        if (billMonthList.size() > 0)
        {
          BillMonth billMonth = billMonthList.get(0);
          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\">" + monthFormat.format(startDate) + "</td>");
          out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(billMonth.getBillMinsExpected()) + "</td>");
          out.println("  </tr>");
        }
        startDate = endDate;
      }
      out.println("</table> ");
      out.println("<br/>");

    }

    query = dataSession.createQuery("from Project where billCode = ? order by projectName");
    query.setParameter(0, billCode.getBillCode());
    List<Project> projectList = query.list();

    int stop = actionMonthDates.size() - 2;
    if (stop < 0)
    {
      stop = 0;
    }
    for (int i = actionMonthDates.size() - 1; i >= stop; i--)
    {

      Date start = actionMonthDates.get(i)[0];
      Date end = actionMonthDates.get(i)[1];

      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"4\">Actions Taken in " + monthFormat.format(start) + "</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      if (projectList.size() > 1)
      {
        out.println("    <th class=\"boxed\">Project</th>");
      }
      out.println("    <th class=\"boxed\">Date &amp; Time</th>");
      out.println("    <th class=\"boxed\">Name</th>");
      out.println("    <th class=\"boxed\">Action</th>");
      out.println("  </tr>");

      for (Project project : projectList)
      {

        query = dataSession
            .createQuery("from ProjectAction where projectId = ? and actionDescription <> '' and actionDate >= ? and actionDate < ? order by actionDate asc");
        query.setParameter(0, project.getProjectId());
        query.setParameter(1, start);
        query.setParameter(2, end);
        List<ProjectAction> projectActionList = query.list();

        for (ProjectAction projectAction : projectActionList)
        {
          ProjectContact projectContact = (ProjectContact) dataSession.get(ProjectContact.class, projectAction.getContactId());
          out.println("  <tr class=\"boxed\">");
          if (projectList.size() > 1)
          {
            out.println("    <td class=\"boxed\">" + project.getProjectName() + "</td>");
          }
          out.println("    <td class=\"boxed\">" + sdf.format(projectAction.getActionDate()) + "</td>");
          out.println("    <td class=\"boxed\">" + projectContact.getNameFirst() + " " + projectContact.getNameLast() + "</td>");
          out.println("    <td class=\"boxed\">" + nbsp(projectAction.getActionDescription()));
          out.println("  </tr>");
        }
      }
      out.println("</table> ");
      out.println("<br/>");
    }
    if (today.before(billBudget.getEndDate()))
    {

      // still time left
      int countWorkingDays = 0;
      int countMonths = -1;
      int countWorkingDaysForPartialMonth = 0;
      int countPartialMonth = 0;
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(today);
      while (calendar.getTime().before(billBudget.getEndDate()))
      {
        if (calendar.get(Calendar.DAY_OF_MONTH) == 1)
        {
          // new month
          if (countMonths == -1)
          {
            countPartialMonth = countWorkingDaysForPartialMonth;
          }
          countMonths++;
          countWorkingDaysForPartialMonth = 0;
        }
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (dayOfWeek == Calendar.MONDAY || dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.WEDNESDAY || dayOfWeek == Calendar.THURSDAY
            || dayOfWeek == Calendar.FRIDAY)
        {
          countWorkingDays++;
          countWorkingDaysForPartialMonth++;
        }
        calendar.add(Calendar.DAY_OF_MONTH, 1);
      }
      if (countMonths == -1)
      {
        countMonths = 0;
        countPartialMonth = countWorkingDaysForPartialMonth;
      } else
      {
        countPartialMonth += countWorkingDaysForPartialMonth;
      }
      if (countPartialMonth >= 36)
      {
        countMonths += 2;
      } else if (countPartialMonth >= 15)
      {
        countMonths++;
      }
      int countWeeks = (countWorkingDays + 1) / 5;
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"4\">Time Remaining</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">By</th>");
      out.println("    <th class=\"boxed\">Count</th>");
      out.println("    <th class=\"boxed\">Hours</th>");
      out.println("    <th class=\"boxed\">Rate</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\">Months</td>");
      out.println("    <td class=\"boxed\">" + countMonths + "</td>");
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalMins) + "</td>");
      out.println("    <td class=\"boxed\">" + (countMonths == 0 ? "-" : TimeTracker.formatTime(totalMins / countMonths)) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\">Weeks</td>");
      out.println("    <td class=\"boxed\">" + countWeeks + "</td>");
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalMins) + "</td>");
      out.println("    <td class=\"boxed\">" + (countWeeks == 0 ? "-" : TimeTracker.formatTime(totalMins / countWeeks)) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\">Working Days</td>");
      out.println("    <td class=\"boxed\">" + countWorkingDays + "</td>");
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totalMins) + "</td>");
      out.println("    <td class=\"boxed\">" + (countWorkingDays == 0 ? "-" : TimeTracker.formatTime(totalMins / countWorkingDays)) + "</td>");
      out.println("  </tr>");
      out.println("</table> ");
      out.println("<br/>");
    }
  }

  private static int getMonth(Date d)
  {
    Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    int month = cal.get(Calendar.MONTH);
    return month;
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

}
