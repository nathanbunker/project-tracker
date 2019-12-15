/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillMonth;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BillCodeServlet extends ClientServlet {

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
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    AppReq appReq = new AppReq(request, response);
    try {
      WebUser webUser = appReq.getWebUser();
      if (appReq.isLoggedOut() || appReq.isDependentWebUser()) {
        forwardToHome(request, response);
        return;
      }
      Session dataSession = appReq.getDataSession();
      String action = appReq.getAction();
      PrintWriter out = appReq.getOut();
      SimpleDateFormat sdf = webUser.getDateFormat();
      Query query;
      appReq.setTitle("Track");
      printHtmlHead(appReq);

      BillCode billCode = null;
      String billCodeString = request.getParameter("billCode");
      billCode = (BillCode) dataSession.get(BillCode.class, billCodeString);
      if (billCode == null) {
        billCode = new BillCode();
        billCode.setBillCode(billCodeString);
      }

      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println(
          "    <th class=\"title\" colspan=\"2\"><span class=\"right\"><font size=\"-1\"><a href=\"BillCodeEditServlet?billCode="
              + billCode.getBillCode() + "\" class=\"box\">Edit</a></font></span>Bill Code</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Bill Code</th>");
      out.println("    <td class=\"boxed\">" + n(billCode.getBillCode()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Bill Label</th>");
      out.println("    <td class=\"boxed\">" + n(billCode.getBillLabel()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Billable</th>");
      out.println("    <td class=\"boxed\">"
          + (billCode.getBillable() != null && billCode.getBillable().equals("Y") ? "Yes" : "No")
          + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Visible</th>");
      out.println("    <td class=\"boxed\">"
          + (billCode.getVisible() != null && billCode.getVisible().equals("Y") ? "Yes" : "No")
          + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Estimate Min</th>");
      out.println(
          "    <td class=\"boxed\">" + TimeTracker.formatTime(billCode.getEstimateMin()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Rate</th>");
      out.println("    <td class=\"boxed\">" + billCode.getBillRate() + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Round</th>");
      out.println("    <td class=\"boxed\">" + billCode.getBillRound() + "</td>");
      out.println("  </tr>");
      out.println("</table> ");
      out.println("<br/>");
      if (billCode.getBillCode() != null && !billCode.getBillCode().equals("")) {

        query = dataSession.createQuery("from BillBudget where billCode = ? order by startDate");
        query.setParameter(0, billCode);
        List<BillBudget> billBudgetList = query.list();
        for (BillBudget billBudget : billBudgetList) {
          out.println("<table class=\"boxed\">");
          out.println("  <tr>");
          out.println("    <th class=\"title\" colspan=\"3\">Budget for "
              + billBudget.getBillBudgetCode() + "</th>");
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Budget Code</th>");
          out.println("    <td class=\"boxed\" colspan=\"2\">" + n(billBudget.getBillBudgetCode())
              + "</td>");
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Start Date</th>");
          out.println("    <td class=\"boxed\" colspan=\"2\">"
              + (billBudget.getStartDate() == null ? "" : sdf.format(billBudget.getStartDate()))
              + "</td>");
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">End Date</th>");
          out.println("    <td class=\"boxed\" colspan=\"2\">"
              + (billBudget.getEndDate() == null ? "" : sdf.format(billBudget.getEndDate()))
              + "</td>");
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Bill Hours</th>");
          out.println("    <td class=\"boxed\" colspan=\"2\">"
              + TimeTracker.formatTime(billBudget.getBillMins()) + "</td>");
          out.println("  </tr>");
          BillCodeEditServlet.updateBillMonths(billCode, billBudget, dataSession, webUser);
          query = dataSession.createQuery(
              "from BillMonth where billBudget = ? and billDate >= ? and billDate < ? order by billDate");
          query.setParameter(0, billBudget);
          query.setParameter(1, billBudget.getStartDate());
          query.setParameter(2, billBudget.getEndDate());
          List<BillMonth> billMonthList = query.list();
          if (billMonthList.size() > 0) {
            out.println("  <tr>");
            out.println("    <th class=\"boxed\">Month</th>");
            out.println("    <th class=\"boxed\">Actual</th>");
            out.println("    <th class=\"boxed\">Expected</th>");
            out.println("  </tr>");
            SimpleDateFormat sdfMonth = webUser.getMonthFormat();
            for (BillMonth billMonth : billMonthList) {
              out.println("  <tr class=\"boxed\">");
              out.println(
                  "    <th class=\"boxed\">" + sdfMonth.format(billMonth.getBillDate()) + "</th>");
              out.println("    <td class=\"boxed\">"
                  + TimeTracker.formatTime(billMonth.getBillMinsActual()) + "</td>");
              out.println("    <td class=\"boxed\">"
                  + TimeTracker.formatTime(billMonth.getBillMinsExpected()) + "</td>");
              out.println("  </tr>");
            }
          }

          out.println("</table> ");
          out.println("<p><a href=\"BillBudgetServlet?billBudgetId=" + billBudget.getBillBudgetId()
              + "\">Bill Budget Report</a></p>");
        }
      }
      printHtmlFoot(appReq);

    } finally {
      appReq.close();
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
