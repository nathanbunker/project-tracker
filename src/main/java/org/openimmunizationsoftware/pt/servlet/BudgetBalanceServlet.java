/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.manager.MoneyUtil;
import org.openimmunizationsoftware.pt.manager.MonthUtil;
import org.openimmunizationsoftware.pt.model.BudgetAccount;
import org.openimmunizationsoftware.pt.model.BudgetItem;
import org.openimmunizationsoftware.pt.model.BudgetMonth;
import org.openimmunizationsoftware.pt.model.BudgetTrans;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BudgetBalanceServlet extends ClientServlet
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
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

      Query query;

      String message = null;
      BudgetAccount budgetAccount = (BudgetAccount) dataSession.get(BudgetAccount.class, Integer.parseInt(request.getParameter("accountId")));
      Date balanceDate = null;
      if (request.getParameter("balanceDate") != null)
      {
        try
        {
          balanceDate = sdf.parse(request.getParameter("balanceDate"));
        } catch (ParseException pe)
        {
          message = "Balance date can not be parsed: " + pe.getMessage();
        }
      }
      if (balanceDate == null)
      {
        balanceDate = new Date();
      }
      int balanceAmount = 0;
      int balanceAmountCalculated = 0;
      if (request.getParameter("balanceAmount") != null)
      {
        balanceAmount = MoneyUtil.parse(request.getParameter("balanceAmount"));
      }

      List<BudgetTrans> budgetTransList = null;
      {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        Date nextMonth = cal.getTime();
        query = dataSession
            .createQuery("from BudgetTrans where transStatus <> 'X' and budgetItem.budgetAccount = ? and transDate < ? order by transDate, transAmount asc");
        query.setParameter(0, budgetAccount);
        query.setParameter(1, nextMonth);
        budgetTransList = query.list();
      }

      String action = request.getParameter("action");
      if (action != null)
      {
        if (action.equals("Balance"))
        {
          if (message == null)
          {
            balanceAmountCalculated = budgetAccount.getBalanceAmount();
            for (BudgetTrans budgetTrans : budgetTransList)
            {
              if (request.getParameter("paid" + budgetTrans.getTransId()) != null)
              {
                balanceAmountCalculated += budgetTrans.getTransAmount();
              }
            }
            if (balanceAmountCalculated == balanceAmount)
            {
              Transaction trans = dataSession.beginTransaction();
              try
              {
                for (BudgetTrans budgetTrans : budgetTransList)
                {
                  if (request.getParameter("paid" + budgetTrans.getTransId()) != null)
                  {
                    budgetTrans.setTransStatus(BudgetTrans.TRANS_STATUS_PAID);
                  }
                }
                budgetAccount.setBalanceAmount(balanceAmountCalculated);
                budgetAccount.setBalanceDate(balanceDate);
              } finally
              {
                trans.commit();
              }
              response.sendRedirect("BudgetServlet?accountId=" + budgetAccount.getAccountId());
              return;
            } else
            {
              message = "Paid items do not balance the account";
            }
          }
        }
      }

      if (message != null)
      {
        request.setAttribute(REQUEST_VAR_MESSAGE, message);
      }

      printHtmlHead(out, "Budget", request);
      out.println("<div class=\"main\">");
      out.println("<form action=\"BudgetBalanceServlet\" method=\"POST\">");
      out.println("<input type=\"hidden\" name=\"accountId\" value=\"" + budgetAccount.getAccountId() + "\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"title\" colspan=\"7\">Balance Account</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Date</th>");
      out.println("    <th class=\"boxed\">Item</th>");
      out.println("    <th class=\"boxed\">Status</th>");
      out.println("    <th class=\"boxed\">Credit</th>");
      out.println("    <th class=\"boxed\">Debit</th>");
      out.println("    <th class=\"boxed\">Paid</th>");
      out.println("    <th class=\"boxed\">Record</th>");
      out.println("  </tr>");
      SimpleDateFormat shortSdf = new SimpleDateFormat("MMM dd");
      for (BudgetTrans budgetTrans : budgetTransList)
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\" nowrap>" + shortSdf.format(budgetTrans.getTransDate()) + "</td>");
        out.println("    <td class=\"boxed\"><a href=\"BudgetItemEditServlet?accountId=" + budgetAccount.getAccountId() + "&transId="
            + budgetTrans.getTransId() + "\" class=\"button\">" + budgetTrans.getBudgetItem().getItemLabel() + "</a></td>");
        out.println("    <td class=\"boxed\">" + BudgetTrans.getTransStatusLabel(budgetTrans.getTransStatus()) + "</td>");
        if (budgetTrans.getTransAmount() < 0)
        {
          out.println("    <td class=\"boxed\"></td>");
          out.println("    <td class=\"boxed\"><span class=\"right\">" + MoneyUtil.format(-budgetTrans.getTransAmount()) + "</span></td>");
        } else
        {
          out.println("    <td class=\"boxed\"><span class=\"right\">" + MoneyUtil.format(budgetTrans.getTransAmount()) + "</span></td>");
          out.println("    <td class=\"boxed\"></td>");
        }
        if (budgetTrans.getBudgetTransRecord() != null) 
        {
          out.println("    <td class=\"boxed\"><input type=\"checkbox\" name=\"paid" + budgetTrans.getTransId() + "\" value=\"Y\""
              + (request.getParameter("paid" + budgetTrans.getTransId()) != null ? " checked" : "") + "></td>");
          out.println("    <td class=\"boxed\">" + trim(budgetTrans.getBudgetTransRecord().getDescription(), 50) + "</td>");
        }
        else
        {
          out.println("    <td class=\"boxed\"><input type=\"checkbox\" name=\"paid" + budgetTrans.getTransId() + "\" value=\"Y\""
              + (request.getParameter("paid" + budgetTrans.getTransId()) != null ? " checked" : "") + " disabled></td>");
          out.println("    <td class=\"boxed\"></td>");
        }
        out.println("  </tr>");
      }
      out.println("</table>");
      out.println("<br>");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"title\" colspan=\"2\">New Balance</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Date</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"balanceDate\" value=\"" + sdf.format(balanceDate) + "\" size=\"10\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Balance</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"balanceAmount\" value=\"" + MoneyUtil.format(balanceAmount)
          + "\" size=\"10\"></td>");
      out.println("  </tr>");
      if (balanceAmountCalculated > 0)
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Paid Balance</th>");
        out.println("    <td class=\"boxed\">" + MoneyUtil.format(balanceAmountCalculated) + "</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Difference</th>");
        out.println("    <td class=\"boxed\">" + MoneyUtil.format(balanceAmountCalculated - balanceAmount) + "</td>");
        out.println("  </tr>");
      }
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed-submit\" colspan=\"6\"><input type=\"submit\" name=\"action\" value=\"Balance\"></td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</form>");
      out.println("</div>");
      printHtmlFoot(out);

    } finally
    {
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

  /**
   * Returns a short description of the servlet.
   * 
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo()
  {
    return "DQA Tester Home Page";
  }// </editor-fold>

}
