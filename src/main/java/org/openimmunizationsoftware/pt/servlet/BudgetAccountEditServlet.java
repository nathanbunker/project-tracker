/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.manager.MoneyUtil;
import org.openimmunizationsoftware.pt.model.BudgetAccount;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BudgetAccountEditServlet extends ClientServlet {

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

      BudgetAccount budgetAccount = null;
      if (request.getParameter("accountId") != null
          && !request.getParameter("accountId").equals("0")) {
        budgetAccount = (BudgetAccount) dataSession.get(BudgetAccount.class,
            Integer.parseInt(request.getParameter("accountId")));
      }
      if (budgetAccount == null) {
        budgetAccount = new BudgetAccount();
        budgetAccount.setProvider(webUser.getProvider());
      }

      SimpleDateFormat sdf = webUser.getDateFormat();

      String action = request.getParameter("action");
      if (action != null) {
        if (action.equals("Save")) {
          String message = null;
          budgetAccount.setAccountLabel(request.getParameter("accountLabel"));
          if (budgetAccount.getAccountId() == 0) {
            budgetAccount.setStartAmount(MoneyUtil.parse(request.getParameter("startAmount")));
            budgetAccount.setBalanceAmount(budgetAccount.getStartAmount());
            try {
              budgetAccount.setStartDate(sdf.parse(request.getParameter("startDate")));
            } catch (ParseException pe) {
              message = "Unable to parse date: " + pe.getMessage();
            }
            budgetAccount.setBalanceDate(budgetAccount.getStartDate());
          }
          if (message != null) {
            request.setAttribute(REQUEST_VAR_MESSAGE, message);
          } else {
            Transaction trans = dataSession.beginTransaction();
            try {
              dataSession.saveOrUpdate(budgetAccount);
            } finally {
              trans.commit();
            }
            response.sendRedirect("BudgetServlet?accountId=" + budgetAccount.getAccountId());
            return;
          }
        }
      }

      printHtmlHead(out, "Budget", request);
      out.println("<div class=\"main\">");
      out.println("<form action=\"BudgetAccountEditServlet\" method=\"POST\">");
      out.println("<input type=\"hidden\" name=\"accountId\" value=\""
          + budgetAccount.getAccountId() + "\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"title\" colspan=\"3\">Edit Account</th>");
      out.println("  </tr>");

      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Label</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"accountLabel\" value=\""
          + budgetAccount.getAccountLabel() + "\" size=\"30\"></td>");
      out.println("  </tr>");
      if (budgetAccount.getAccountId() == 0) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Start Balance</th>");
        out.println(
            "    <td class=\"boxed\"><input type=\"text\" name=\"startAmount\" value=\"\" size=\"10\"></td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Start Date</th>");
        out.println(
            "    <td class=\"boxed\"><input type=\"text\" name=\"startDate\" value=\"\" size=\"10\"></td>");
        out.println("  </tr>");
      }
      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save\"></td>");
      out.println("  </tr>");

      out.println("</table>");
      out.println("</div>");
      printHtmlFoot(out);

    } finally {
      out.close();
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

  /**
   * Returns a short description of the servlet.
   * 
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "DQA Tester Home Page";
  }// </editor-fold>

}
