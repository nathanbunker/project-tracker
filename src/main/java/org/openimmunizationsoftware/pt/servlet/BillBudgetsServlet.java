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
public class BillBudgetsServlet extends ClientServlet
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

      runReport(webUser, out, dataSession, true);
      printHtmlFoot(out);

    } finally
    {
      out.close();
    }
  }

  public static void runReport(WebUser webUser, PrintWriter out, Session dataSession, boolean showLinks)
  {
    List<Date> monthDateList = new ArrayList<Date>();
    Date today = new Date();
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    monthDateList.add(calendar.getTime());
    for (int i = 0; i < 6; i++)
    {
      calendar.add(Calendar.MONTH, 1);
      monthDateList.add(calendar.getTime());
    }
    int[] workingDays = new int[monthDateList.size()];
    for (int i = 1; i < monthDateList.size(); i++)
    {
      int workingDayCount = 0;
      Date startDate = monthDateList.get(i - 1);
      Date endDate = monthDateList.get(i);
      Calendar workingDay = Calendar.getInstance();
      workingDay.setTime(startDate);
      while (workingDay.getTime().before(today))
      {
        workingDay.add(Calendar.DAY_OF_MONTH, 1);
      }
      if (workingDay.get(Calendar.DAY_OF_MONTH) > 1)
      {
        workingDay.add(Calendar.DAY_OF_MONTH, -1);
      }

      while (workingDay.getTime().before(endDate))
      {
        if (workingDay.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && workingDay.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY)
        {
          workingDayCount++;
        }
        workingDay.add(Calendar.DAY_OF_MONTH, 1);
      }
      workingDays[i] = workingDayCount;
    }

    int[] totals = new int[monthDateList.size()];

    Query query;
    query = dataSession
        .createQuery("from BillBudget where billCode.providerId = ? and billCode.visible = 'Y'  and startDate <= ? and endDate > ? order by billCode.billLabel, billBudgetCode");
    query.setParameter(0, webUser.getProviderId());
    query.setParameter(1, today);
    query.setParameter(2, today);
    List<BillBudget> billBudgetList = query.list();
    out.println("<table class=\"boxed\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\" colspan=\"" + (monthDateList.size() + 3) + "\">Bill Budgets</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Bill Label</th>");
    out.println("    <th class=\"boxed\">Budget</th>");
    out.println("    <th class=\"boxed\">Hours</th>");
    out.println("    <th class=\"boxed\">Remaining</th>");
    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
    for (int i = 1; i < monthDateList.size(); i++)
    {
      out.println("    <th class=\"boxed\">" + sdfMonth.format(monthDateList.get(i - 1)) + "</th>");
    }
    out.println("  </tr>");
    for (BillBudget billBudget : billBudgetList)
    {
      BillCode billCode = billBudget.getBillCode();
      BillCodeEditServlet.updateBillMonths(billCode, billBudget, dataSession);
      out.println("  <tr class=\"boxed\">");
      if (showLinks)
      {
        out.println("    <td class=\"boxed\"><a href=\"BillCodeServlet?billCode=" + billCode.getBillCode() + "\" class=\"button\">"
            + n(billCode.getBillLabel()) + "</a></td>");
        out.println("    <td class=\"boxed\"><a href=\"BillBudgetServlet?billBudgetId=" + billBudget.getBillBudgetId() + "\" class=\"button\">"
            + billBudget.getBillBudgetCode() + "</a></td>");
      } else
      {
        out.println("    <td class=\"boxed\">" + n(billCode.getBillLabel()) + "</td>");
        out.println("    <td class=\"boxed\">" + billBudget.getBillBudgetCode() + "</td>");
      }
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(billBudget.getBillMins()) + "</td>");
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(billBudget.getBillMinsRemaining()) + "</td>");

      Date startDate = monthDateList.get(0);

      for (int i = 1; i < monthDateList.size(); i++)
      {
        Date endDate = monthDateList.get(i);
        query = dataSession.createQuery("from BillMonth where billBudget = ? and billDate >= ? and billDate < ? order by billDate");
        query.setParameter(0, billBudget);
        query.setParameter(1, startDate);
        query.setParameter(2, endDate);
        List<BillMonth> billMonthList = query.list();
        if (billMonthList.size() == 0)
        {
          out.println("    <td class=\"boxed\">&nbsp;</td>");
        } else
        {
          BillMonth billMonth = billMonthList.get(0);
          int billMinsPending = billMonth.getBillMinsExpected() - billMonth.getBillMinsActual();
          out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(billMinsPending) + "</td>");
          totals[i] += billMinsPending;
        }
        startDate = endDate;
      }

      out.println("  </tr>");
    }

    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\" colspan=\"4\">Total</th>");
    for (int i = 1; i < monthDateList.size(); i++)
    {
      out.println("    <th class=\"boxed\">" + sdfMonth.format(monthDateList.get(i - 1)) + "</th>");
    }
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <td class=\"boxed\" colspan=\"4\">Remaining</td>");

    for (int i = 1; i < monthDateList.size(); i++)
    {
      out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totals[i]) + "</td>");
    }
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <td class=\"boxed\" colspan=\"4\">Working Days Left</td>");

    for (int i = 1; i < monthDateList.size(); i++)
    {
      out.println("    <td class=\"boxed\">" + workingDays[i] + "</td>");
    }

    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <td class=\"boxed\" colspan=\"4\">Per Working Day</td>");

    for (int i = 1; i < monthDateList.size(); i++)
    {
      if (workingDays[i] != 0)
      {
        out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(totals[i] / workingDays[i]) + "</td>");
      } else
      {
        out.println("    <td class=\"boxed\">&nbsp;</td>");
      }
    }

    out.println("  </tr>");
    out.println("</table> ");
    out.println("<br/>");
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
