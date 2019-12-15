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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillDay;
import org.openimmunizationsoftware.pt.model.BillMonth;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BillCodeEditServlet extends ClientServlet {

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
      Query query;
      BillCode billCode = null;
      String billCodeString = request.getParameter("billCode");
      if (billCodeString == null) {
        billCode = new BillCode();
        billCode.setProvider(webUser.getProvider());
      } else {
        billCode = (BillCode) dataSession.get(BillCode.class, billCodeString);
        if (billCode == null) {
          billCode = new BillCode();
          billCode.setBillCode(billCodeString);
        }
      }


      if (action != null) {
        if (action.equals("Save")) {
          billCode.setBillLabel(request.getParameter("billLabel"));
          billCode.setBillable(request.getParameter("billable") != null ? "Y" : "N");
          billCode.setVisible(request.getParameter("visible") != null ? "Y" : "N");
          billCode.setEstimateMin(TimeTracker.readTime(request.getParameter("estimateMin")));
          billCode.setBillRate(Integer.parseInt(request.getParameter("billRate")));
          billCode.setBillRound(Integer.parseInt(request.getParameter("billRound")));
          billCode.setProvider(webUser.getProvider());
          Transaction trans = dataSession.beginTransaction();
          try {
            dataSession.saveOrUpdate(billCode);
            trans.commit();
            response.sendRedirect("BillCodeServlet?billCode=" + billCode.getBillCode());
            return;
          } catch (Exception e) {
            appReq.setMessageProblem("Unable to save bill code: " + e.getMessage());
            trans.rollback();
          }
        } else if (action.equals("Save Budget")) {
          Transaction trans = dataSession.beginTransaction();
          try {
            BillBudget billBudget;
            int billBudgetId = Integer.parseInt(request.getParameter("billBudgetId"));
            if (billBudgetId > 0) {
              billBudget = (BillBudget) dataSession.get(BillBudget.class, billBudgetId);
            } else {
              billBudget = new BillBudget();
              billBudget.setBillCode(billCode);
            }
            billBudget.setBillBudgetCode(request.getParameter("billBudgetCode"));
            billBudget.setStartDate(sdf.parse(request.getParameter("startDate")));
            billBudget.setEndDate(sdf.parse(request.getParameter("endDate")));
            billBudget.setBillMins(TimeTracker.readTime(request.getParameter("billMins")));

            if (billBudget.getBillBudgetId() != 0) {
              query = dataSession.createQuery(
                  "from BillMonth where billBudget = ? and billDate >= ? and billDate < ? order by billDate");
              query.setParameter(0, billBudget);
              query.setParameter(1, billBudget.getStartDate());
              query.setParameter(2, billBudget.getEndDate());
              Calendar today = webUser.getCalendar();
              List<BillMonth> billMonthList = query.list();
              if (billMonthList.size() > 0) {
                for (BillMonth billMonth : billMonthList) {
                  Calendar billDateCalendar = webUser.getCalendar();
                  billDateCalendar.setTime(billMonth.getBillDate());

                  boolean isChangable =
                      (billDateCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                          && billDateCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR))
                          || today.before(billDateCalendar);
                  if (isChangable) {
                    billMonth.setBillMinsExpected(TimeTracker.readTime(
                        request.getParameter("billMinsExpected" + billMonth.getBillMonthId())));
                  }
                  out.println("  </tr>");
                }
              }
            }

            dataSession.saveOrUpdate(billBudget);
            trans.commit();
            updateBillMonths(billCode, billBudget, dataSession, webUser);
            response.sendRedirect("BillCodeServlet?billCode=" + billCode.getBillCode());
            return;
          } catch (Exception e) {
            appReq.setMessageProblem("Unable to save bill budget: " + e.getMessage());
            trans.rollback();
          }
        }
      }

      appReq.setTitle("Track");
      printHtmlHead(appReq);

      out.println("<form method=\"POST\" action=\"BillCodeEditServlet\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"2\">Edit Bill Code</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Bill Code</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"billCode\" value=\""
          + n(billCode.getBillCode()) + "\" size=\"\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Bill Label</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"billLabel\" value=\""
          + n(billCode.getBillLabel()) + "\" size=\"\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Billable</th>");
      out.println("    <td class=\"boxed\"><input type=\"checkbox\" name=\"billable\" value=\"Y\""
          + (billCode.getBillable() != null && billCode.getBillable().equals("Y") ? " checked" : "")
          + "></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Visible</th>");
      out.println("    <td class=\"boxed\"><input type=\"checkbox\" name=\"visible\" value=\"Y\""
          + (billCode.getVisible() != null && billCode.getVisible().equals("Y") ? " checked" : "")
          + "></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Estimate Min</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"estimateMin\" value=\""
          + TimeTracker.formatTime(billCode.getEstimateMin()) + "\" size=\"4\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Rate</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"billRate\" value=\""
          + billCode.getBillRate() + "\" size=\"4\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Round</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"billRound\" value=\""
          + billCode.getBillRound() + "\" size=\"4\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save\" size=\"4\"></td>");
      out.println("  </tr>");
      out.println("</table> ");
      out.println("</form> ");
      if (billCode.getBillCode() != null && !billCode.getBillCode().equals("")) {

        query = dataSession.createQuery("from BillBudget where billCode = ? order by startDate");
        query.setParameter(0, billCode);
        List<BillBudget> billBudgetList = query.list();
        billBudgetList.add(new BillBudget());
        for (BillBudget billBudget : billBudgetList) {
          out.println("<form method=\"POST\" action=\"BillCodeEditServlet\">");
          out.println(
              "<input type=\"hidden\" name=\"billCode\" value=\"" + billCode.getBillCode() + "\">");
          out.println("<input type=\"hidden\" name=\"billBudgetId\" value=\""
              + billBudget.getBillBudgetId() + "\">");
          out.println("<table class=\"boxed\">");
          out.println("  <tr>");
          if (billBudget.getBillBudgetId() == 0) {
            out.println("    <th class=\"title\" colspan=\"2\">Edit Budget (new)</th>");
          } else {
            out.println("    <th class=\"title\" colspan=\"2\">Edit Budget for "
                + billBudget.getBillBudgetCode() + "</th>");
          }
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Budget Code</th>");
          out.println(
              "    <td class=\"boxed\"><input type=\"text\" name=\"billBudgetCode\" value=\""
                  + n(billBudget.getBillBudgetCode()) + "\" size=\"30\"></td>");
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Start Date</th>");
          out.println("    <td class=\"boxed\"><input type=\"text\" name=\"startDate\" value=\""
              + (billBudget.getStartDate() == null ? "" : sdf.format(billBudget.getStartDate()))
              + "\" size=\"10\"></td>");
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">End Date</th>");
          out.println("    <td class=\"boxed\"><input type=\"text\" name=\"endDate\" value=\""
              + (billBudget.getEndDate() == null ? "" : sdf.format(billBudget.getEndDate()))
              + "\" size=\"10\"></td>");
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Bill Hours</th>");
          out.println("    <td class=\"boxed\"><input type=\"text\" name=\"billMins\" value=\""
              + TimeTracker.formatTime(billBudget.getBillMins()) + "\" size=\"10\"></td>");
          out.println("  </tr>");
          if (billBudget.getBillBudgetId() != 0) {
            query = dataSession.createQuery(
                "from BillMonth where billBudget = ? and billDate >= ? and billDate < ? order by billDate");
            query.setParameter(0, billBudget);
            query.setParameter(1, billBudget.getStartDate());
            query.setParameter(2, billBudget.getEndDate());
            Calendar today = webUser.getCalendar();
            List<BillMonth> billMonthList = query.list();
            if (billMonthList.size() > 0) {
              out.println("  <tr>");
              out.println("    <th class=\"boxed\">Month</th>");
              out.println("    <th class=\"boxed\">Expected</th>");
              out.println("  </tr>");
              SimpleDateFormat sdfMonth = webUser.getMonthFormat();
              for (BillMonth billMonth : billMonthList) {
                Calendar billDateCalendar = webUser.getCalendar();
                billDateCalendar.setTime(billMonth.getBillDate());

                boolean isChangable =
                    (billDateCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                        && billDateCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR))
                        || today.before(billDateCalendar);
                out.println("  <tr class=\"boxed\">");
                out.println("    <th class=\"boxed\">" + sdfMonth.format(billMonth.getBillDate())
                    + "</th>");
                if (isChangable) {
                  out.println("    <td class=\"boxed\"><input type=\"text\" name=\"billMinsExpected"
                      + billMonth.getBillMonthId() + "\" value=\""
                      + TimeTracker.formatTime(billMonth.getBillMinsExpected())
                      + "\" size=\"10\"></td>");
                } else {
                  out.println("    <td class=\"boxed\">"
                      + TimeTracker.formatTime(billMonth.getBillMinsExpected()) + "</td>");
                }
                out.println("  </tr>");
              }
            }

          }
          out.println("  <tr class=\"boxed\">");
          out.println(
              "    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save Budget\" size=\"4\"></td>");
          out.println("  </tr>");
          out.println("</table> ");
          out.println("</form> ");
        }

      }
      printHtmlFoot(appReq);

    } finally {
      appReq.close();
    }
  }

  public static void updateBillMonths(BillCode billCode, BillBudget billBudget, Session dataSession,
      WebUser webUser) {
    Transaction transaction = dataSession.beginTransaction();
    try {
      Query query;

      List<BillMonth> monthsLeftToWorkList = new ArrayList<BillMonth>();

      Date today = new Date();
      int totalTimeLeft = billBudget.getBillMins();
      Calendar monthCalendar = webUser.getCalendar();
      monthCalendar.setTime(billBudget.getStartDate());
      while (monthCalendar.getTime().before(billBudget.getEndDate())) {
        Date startTime = monthCalendar.getTime();
        query = dataSession.createQuery("from BillMonth where billBudget = ? and billDate = ?");
        query.setParameter(0, billBudget);
        query.setParameter(1, startTime);
        BillMonth billMonth;
        List<BillMonth> billMonthList = query.list();
        if (billMonthList.size() == 0) {
          billMonth = new BillMonth();
          billMonth.setBillBudget(billBudget);
          billMonth.setBillCode(billBudget.getBillCode());
          billMonth.setBillDate(startTime);
          billMonth.setBillMinsExpected(0);
        } else {
          billMonth = billMonthList.get(0);
        }

        monthCalendar.set(Calendar.DAY_OF_MONTH, 1);
        monthCalendar.add(Calendar.MONTH, 1);
        Date endTime = monthCalendar.getTime();
        boolean canWorkMoreThisMonth = today.before(endTime);
        if (canWorkMoreThisMonth) {
          monthsLeftToWorkList.add(billMonth);
        }
        if (!endTime.before(billBudget.getEndDate())) {
          endTime = billBudget.getEndDate();
        }
        query = dataSession.createQuery(
            "from BillDay where billBudget = ? and billDate >= ? and billDate < ? order by billDate asc");
        query.setParameter(0, billBudget);
        query.setParameter(1, startTime);
        query.setParameter(2, endTime);
        List<BillDay> billDayList = query.list();
        int billMinsActual = 0;
        for (BillDay billDay : billDayList) {
          billDay.setBillMinsBudget(totalTimeLeft);
          billDay.setBillMonth(billMonth);
          dataSession.update(billDay);
          totalTimeLeft = totalTimeLeft - billDay.getBillMins();
          if (totalTimeLeft < 0) {
            totalTimeLeft = 0;
          }
          billMinsActual += billDay.getBillMins();
        }
        billMonth.setBillMinsActual(billMinsActual);
        if (!canWorkMoreThisMonth
            || billMonth.getBillMinsActual() > billMonth.getBillMinsExpected()) {
          billMonth.setBillMinsExpected(billMonth.getBillMinsActual());
        }
        dataSession.saveOrUpdate(billMonth);
      }
      billBudget.setBillMinsRemaining(totalTimeLeft);
      dataSession.update(billBudget);
      // first trim down
      for (BillMonth billMonth : monthsLeftToWorkList) {
        int moreToWork = billMonth.getBillMinsExpected() - billMonth.getBillMinsActual();
        if (moreToWork > totalTimeLeft) {
          billMonth.setBillMinsExpected(billMonth.getBillMinsActual() + totalTimeLeft);
          moreToWork = totalTimeLeft;
        }
        totalTimeLeft = totalTimeLeft - moreToWork;
      }
      if (monthsLeftToWorkList.size() > 0) {
        // now add time if there is any left
        while (totalTimeLeft > 0) {
          for (BillMonth billMonth : monthsLeftToWorkList) {
            int addMins = billCode.getBillRound();
            billMonth.setBillMinsExpected(billMonth.getBillMinsExpected() + addMins);
            totalTimeLeft -= addMins;
            if (totalTimeLeft <= 0) {
              break;
            }
          }
        }
      }
      for (BillMonth billMonth : monthsLeftToWorkList) {
        dataSession.update(billMonth);
      }
    } finally {
      transaction.commit();
    }

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
