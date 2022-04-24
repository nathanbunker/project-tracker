/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
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
public class BudgetServlet extends ClientServlet {

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
      if (appReq.isLoggedOut()) {
        forwardToHome(request, response);
        return;
      }
      Session dataSession = appReq.getDataSession();
      String action = appReq.getAction();
      PrintWriter out = appReq.getOut();
      SimpleDateFormat sdf = webUser.getDateFormat();

      Query query = dataSession
          .createQuery("from BudgetAccount where provider = :provider order by accountLabel");
      query.setParameter("provider", webUser.getProvider());
      List<BudgetAccount> budgetAccountList = query.list();

      appReq.setTitle("Budget");
      printHtmlHead(appReq);

      out.println("<div class=\"main\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"title\" colspan=\"3\">Accounts</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Label</th>");
      out.println("    <th class=\"boxed\">Date</th>");
      out.println("    <th class=\"boxed\">Balance</th>");
      out.println("  </tr>");

      for (BudgetAccount budgetAccount : budgetAccountList) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\"><a href=\"BudgetServlet?accountId="
            + budgetAccount.getAccountId() + "\" class=\"button\">"
            + budgetAccount.getAccountLabel() + "</a></td>");
        out.println(
            "    <td class=\"boxed\">" + sdf.format(budgetAccount.getBalanceDate()) + "</td>");
        out.println("    <td class=\"boxed\"><span class=\"right\">"
            + MoneyUtil.format(budgetAccount.getBalanceAmount()) + "</span></td>");
        out.println("  </tr>");
      }
      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <td class=\"boxed\" colspan=\"3\"><span class=\"right\"><font size=\"-1\"><a href=\"BudgetAccountEditServlet\" class=\"box\">Add New Account</a></font></span></td>");
      out.println("  </tr>");

      out.println("</table>");

      if (request.getParameter("accountId") != null) {
        BudgetAccount budgetAccount = (BudgetAccount) dataSession.get(BudgetAccount.class,
            Integer.parseInt(request.getParameter("accountId")));
        Date monthDate = null;
        {
          String monthDateString = request.getParameter("monthDate");
          if (monthDateString != null) {
            try {
              monthDate = MonthUtil.getMonthDate(sdf.parse(monthDateString));
            } catch (Exception e) {
              monthDateString = null;
            }
          }
          if (monthDateString == null) {
            monthDate = MonthUtil.getMonthDate(new Date());
          }
        }
        BudgetMonth budgetMonth = getOrCreateBudgetMonth(dataSession, budgetAccount, monthDate);

        query = dataSession.createQuery(
            "from BudgetTrans where budgetMonth = ? and budgetItem.budgetAccount = ? order by transDate, transAmount desc");
        query.setParameter(0, budgetMonth);
        query.setParameter(1, budgetAccount);
        List<BudgetTrans> budgetTransList = query.list();

        Date today = new Date();
        Date nextMonth = MonthUtil.getNextMonth(monthDate);
        Date prevMonth = MonthUtil.getPrevMonth(monthDate);
        if (nextMonth.after(today)) {
          boolean foundChanges = false;
          BudgetMonth prevBudgetMonth = null;
          BudgetMonth prevYearBudgetMonth = null;
          {
            query = dataSession
                .createQuery("from BudgetMonth where budgetAccount = ? and monthDate = ?");
            query.setParameter(0, budgetAccount);
            query.setParameter(1, prevMonth);
            List<BudgetMonth> prevBudgetMonthList = query.list();
            if (prevBudgetMonthList.size() > 0) {
              prevBudgetMonth = prevBudgetMonthList.get(0);
            }
          }
          {
            query = dataSession
                .createQuery("from BudgetMonth where budgetAccount = ? and monthDate = ?");
            query.setParameter(0, budgetAccount);
            query.setParameter(1, MonthUtil.getMonthLastYear(monthDate));
            List<BudgetMonth> prevBudgetMonthList = query.list();
            if (prevBudgetMonthList.size() > 0) {
              prevBudgetMonth = prevBudgetMonthList.get(0);
            }
          }

          if (prevBudgetMonth != null || prevYearBudgetMonth != null) {
            Transaction trans = dataSession.beginTransaction();
            // month is currently still active, need to make sure all items are
            // added to it
            query = dataSession.createQuery(
                "from BudgetItem where budgetAccount = ? and itemStatus = 'M' or itemStatus = 'Y'");
            query.setParameter(0, budgetAccount);
            List<BudgetItem> potentialBudgetItems = query.list();
            for (BudgetItem potentialBudgetItem : potentialBudgetItems) {
              boolean found = false;
              for (BudgetTrans budgetTrans : budgetTransList) {
                if (budgetTrans.getBudgetItem().getItemId() == potentialBudgetItem.getItemId()) {
                  found = true;
                  break;
                }
              }
              if (!found) {
                boolean isMonthly =
                    potentialBudgetItem.getItemStatus().equals(BudgetItem.ITEM_STATUS_MONTHLY);
                if ((prevBudgetMonth != null && isMonthly) || (prevYearBudgetMonth != null
                    && MonthUtil.sameMonthOnly(potentialBudgetItem.getLastDate(), monthDate))) {
                  BudgetTrans prevBudgetTrans = null;
                  query = dataSession
                      .createQuery("from BudgetTrans where budgetItem = ? and budgetMonth = ? ");
                  query.setParameter(0, potentialBudgetItem);
                  query.setParameter(1, isMonthly ? prevBudgetMonth : prevYearBudgetMonth);
                  List<BudgetTrans> prevBudgetTransList = query.list();
                  if (prevBudgetTransList.size() > 0) {
                    prevBudgetTrans = prevBudgetTransList.get(0);
                  }

                  BudgetTrans budgetTrans = new BudgetTrans();
                  budgetTrans.setBudgetItem(potentialBudgetItem);
                  budgetTrans.setBudgetMonth(budgetMonth);

                  if (prevBudgetTrans != null) {
                    budgetTrans.setTransAmount(prevBudgetTrans.getTransAmount());
                    budgetTrans.setTransDate(
                        MonthUtil.thisMonth(monthDate, prevBudgetTrans.getTransDate()));
                    if (prevBudgetTrans.getTransStatus().equals(BudgetTrans.TRANS_STATUS_PAID)) {
                      budgetTrans.setTransStatus(BudgetTrans.TRANS_STATUS_EXPECTED);
                    } else {
                      budgetTrans.setTransStatus(prevBudgetTrans.getTransStatus());
                    }
                  } else {
                    budgetTrans.setTransAmount(0);
                    budgetTrans.setTransDate(
                        MonthUtil.thisMonth(monthDate, potentialBudgetItem.getLastDate()));
                    budgetTrans.setTransStatus(BudgetTrans.TRANS_STATUS_POSSIBLE);
                  }
                  dataSession.save(budgetTrans);
                  foundChanges = true;
                }
              }
            }
            trans.commit();
            if (foundChanges) {
              query = dataSession.createQuery(
                  "from BudgetTrans where budgetMonth = ? and budgetItem.budgetAccount = ? order by transDate, transAmount asc");
              query.setParameter(0, budgetMonth);
              query.setParameter(1, budgetAccount);
              budgetTransList = query.list();
            }
          }
        }

        SimpleDateFormat monthTitleSdf = webUser.getMonthFormat();
        out.println("<br>");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"6\">");
        Date prevDate = MonthUtil.getPrevMonth(monthDate);
        if (!prevDate.before(MonthUtil.getMonthDate(budgetAccount.getStartDate()))) {
          String link = "BudgetServlet?accountId=" + budgetAccount.getAccountId() + "&monthDate="
              + sdf.format(prevDate) + "";
          out.println("<font size=\"-1\"><a href=\"" + link + "\" class=\"box\">Prev</a></font>");
        }
        out.println(budgetAccount.getAccountLabel() + " - " + monthTitleSdf.format(monthDate));
        {
          String link = "BudgetServlet?accountId=" + budgetAccount.getAccountId() + "&monthDate="
              + sdf.format(MonthUtil.getNextMonth(monthDate)) + "";
          out.println("<font size=\"-1\"><a href=\"" + link + "\" class=\"box\">Next</a></font>");
        }
        out.println("</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Date</th>");
        out.println("    <th class=\"boxed\">Item</th>");
        out.println("    <th class=\"boxed\">Status</th>");
        out.println("    <th class=\"boxed\">Credit</th>");
        out.println("    <th class=\"boxed\">Debit</th>");
        out.println("    <th class=\"boxed\">Balance</th>");
        out.println("  </tr>");
        HashMap<String, Integer> totalsByPriorityMap = new HashMap<String, Integer>();
        totalsByPriorityMap.put(BudgetItem.PRIORITY_CODE_BALANCE, 0);
        totalsByPriorityMap.put(BudgetItem.PRIORITY_CODE_COMMITTED, 0);
        totalsByPriorityMap.put(BudgetItem.PRIORITY_CODE_DEBT_PAYOFF, 0);
        totalsByPriorityMap.put(BudgetItem.PRIORITY_CODE_FLEXIBLE, 0);
        totalsByPriorityMap.put(BudgetItem.PRIORITY_CODE_INCOME, 0);
        totalsByPriorityMap.put(BudgetItem.PRIORITY_CODE_SAVINGS, 0);
        totalsByPriorityMap.put(BudgetItem.PRIORITY_CODE_SCHEDULED, 0);
        totalsByPriorityMap.put(BudgetItem.PRIORITY_CODE_DONATIONS, 0);

        SimpleDateFormat shortSdf = webUser.getDateFormat("MMM dd");
        int endingBalance = budgetMonth.getBalanceStart();
        Date startDate =
            budgetAccount.getStartDate().after(monthDate) ? budgetAccount.getStartDate()
                : monthDate;
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\" colspan=\"5\">Starting Balance</td>");
        out.println("    <td class=\"boxed\"><span class=\"right\">"
            + MoneyUtil.format(endingBalance) + "</span></td>");
        out.println("  </tr>");
        for (BudgetTrans budgetTrans : budgetTransList) {
          endingBalance += budgetTrans.getTransAmount();
          String priorityCode = budgetTrans.getBudgetItem().getPriorityCode();
          totalsByPriorityMap.put(priorityCode,
              totalsByPriorityMap.get(priorityCode) + budgetTrans.getTransAmount());
          out.println("  <tr class=\"boxed\">");
          out.println(
              "    <td class=\"boxed\">" + shortSdf.format(budgetTrans.getTransDate()) + "</td>");
          out.println("    <td class=\"boxed\"><a href=\"BudgetItemEditServlet?accountId="
              + budgetAccount.getAccountId() + "&monthDate=" + sdf.format(monthDate) + "&transId="
              + budgetTrans.getTransId() + "\" class=\"button\">"
              + budgetTrans.getBudgetItem().getItemLabel() + "</a></td>");
          out.println("    <td class=\"boxed\">"
              + BudgetTrans.getTransStatusLabel(budgetTrans.getTransStatus()) + "</td>");
          if (budgetTrans.getTransAmount() < 0) {
            out.println("    <td class=\"boxed\"></td>");
            out.println("    <td class=\"boxed\"><span class=\"right\">"
                + MoneyUtil.format(-budgetTrans.getTransAmount()) + "</span></td>");
          } else {
            out.println("    <td class=\"boxed\"><span class=\"right\">"
                + MoneyUtil.format(budgetTrans.getTransAmount()) + "</span></td>");
            out.println("    <td class=\"boxed\"></td>");
          }
          out.println("    <td class=\"boxed\"><span class=\"right\">"
              + MoneyUtil.format(endingBalance) + "<span></td>");
          out.println("  </tr>");
        }
        Transaction trans = dataSession.beginTransaction();
        budgetMonth.setBalanceEnd(endingBalance);
        dataSession.update(budgetMonth);
        trans.commit();
        out.println("  <tr class=\"boxed\">");
        out.println(
            "    <td class=\"boxed\" colspan=\"7\"><span class=\"right\"><font size=\"-1\"><a href=\"BudgetItemEditServlet?accountId="
                + budgetAccount.getAccountId() + "&monthDate=" + sdf.format(monthDate)
                + "\" class=\"box\">New Budget Item</a></font></span></td>");
        out.println("  </tr>");
        out.println("</table>");

        out.println("<br/>");

        out.println("<table class=\"boxed\">");
        String prevPriorityCode = "";
        Collections.sort(budgetTransList, new Comparator<BudgetTrans>() {
          public int compare(BudgetTrans arg0, BudgetTrans arg1) {
            if (arg0.getBudgetItem().getPriorityCode()
                .equals(arg1.getBudgetItem().getPriorityCode())) {
              if (arg0.getTransAmount() > arg1.getTransAmount()) {
                return 1;
              } else if (arg0.getTransAmount() < arg1.getTransAmount()) {
                return -1;
              }
              return 0;
            } else {
              return arg0.getBudgetItem().getPriorityCode()
                  .compareTo(arg1.getBudgetItem().getPriorityCode());
            }
          };
        });
        int priorityTotal = 0;
        for (BudgetTrans budgetTrans : budgetTransList) {
          if (!budgetTrans.getBudgetItem().getPriorityCode().equals(prevPriorityCode)) {
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"title\" colspan=\"6\">"
                + BudgetItem.getPriorityCodeLabel(budgetTrans.getBudgetItem().getPriorityCode())
                + "</th>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Item</th>");
            out.println("    <th class=\"boxed\">Credit</th>");
            out.println("    <th class=\"boxed\">Debit</th>");
            out.println("    <th class=\"boxed\">Total</th>");
            out.println("  </tr>");
            priorityTotal = 0;
          }
          prevPriorityCode = budgetTrans.getBudgetItem().getPriorityCode();
          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\"><a href=\"BudgetItemEditServlet?accountId="
              + budgetAccount.getAccountId() + "&monthDate=" + sdf.format(monthDate) + "&transId="
              + budgetTrans.getTransId() + "\" class=\"button\">"
              + budgetTrans.getBudgetItem().getItemLabel() + "</a></td>");
          if (budgetTrans.getTransAmount() < 0) {
            out.println("    <td class=\"boxed\"></td>");
            out.println("    <td class=\"boxed\"><span class=\"right\">"
                + MoneyUtil.format(-budgetTrans.getTransAmount()) + "</span></td>");
          } else {
            out.println("    <td class=\"boxed\"><span class=\"right\">"
                + MoneyUtil.format(budgetTrans.getTransAmount()) + "</span></td>");
            out.println("    <td class=\"boxed\"></td>");
          }
          priorityTotal += budgetTrans.getTransAmount();
          out.println("    <td class=\"boxed\"><span class=\"right\">"
              + MoneyUtil.format(priorityTotal) + "</span></td>");

          out.println("  </tr>");
        }
        out.println("</table>");

        out.println("<br/>");

        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"2\">Summary Report</th>");
        out.println("  </tr>");
        out.println("  <tr>");
        out.println("    <th class=\"boxed\">Income</th>");
        out.println("    <td class=\"boxed\">"
            + MoneyUtil.format(totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_INCOME)) + "</td>");
        out.println("  </tr>");
        out.println("  <tr>");
        out.println("    <th class=\"boxed\">Expenses</th>");
        out.println("    <td class=\"boxed\">"

            + MoneyUtil.format(totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_DONATIONS)
                + totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_COMMITTED)
                + totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_SCHEDULED)
                + totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_FLEXIBLE)
                + totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_DEBT_PAYOFF)
                + totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_SAVINGS))
            + "</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"2\">Break Down</th>");
        out.println("  </tr>");
        out.println("  <tr>");
        out.println("    <th class=\"boxed\">Committed</th>");
        out.println("    <td class=\"boxed\">"
            + MoneyUtil.format(totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_COMMITTED))
            + "</td>");
        out.println("  </tr>");
        out.println("  <tr>");
        out.println("    <th class=\"boxed\">Flexible</th>");
        out.println("    <td class=\"boxed\">"
            + MoneyUtil.format(totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_FLEXIBLE))
            + "</td>");
        out.println("  <tr>");
        out.println("  <tr>");
        out.println("    <th class=\"boxed\">Scheduled</th>");
        out.println("    <td class=\"boxed\">"
            + MoneyUtil.format(totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_SCHEDULED))
            + "</td>");
        out.println("  </tr>");
        out.println("  </tr>");
        out.println("    <th class=\"boxed\">Donations</th>");
        out.println("    <td class=\"boxed\">"
            + MoneyUtil.format(totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_DONATIONS))
            + "</td>");
        out.println("  </tr>");
        out.println("  <tr>");
        out.println("    <th class=\"boxed\">Debt Payoff</th>");
        out.println("    <td class=\"boxed\">"
            + MoneyUtil.format(totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_DEBT_PAYOFF))
            + "</td>");
        out.println("  </tr>");
        out.println("  <tr>");
        out.println("    <th class=\"boxed\">Savings</th>");
        out.println("    <td class=\"boxed\">"
            + MoneyUtil.format(totalsByPriorityMap.get(BudgetItem.PRIORITY_CODE_SAVINGS))
            + "</td>");
        out.println("  </tr>");
        out.println("</table>");
        out.println("<h2>Balance Account</h2>");
        out.println("<p><a href=\"BudgetBalanceServlet?accountId=" + budgetAccount.getAccountId()
            + "\" class=\"box\">Balance Account</a></p>");
        out.println("<h2>Manage Transaction Records</h2>");
        out.println("<p><a href=\"BudgetTransRecordServlet?accountId="
            + budgetAccount.getAccountId() + "\" class=\"box\">Manage Transaction Records</a></p>");
      }
      out.println("</div>");
      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  protected static BudgetMonth getOrCreateBudgetMonth(Session dataSession,
      BudgetAccount budgetAccount, Date monthDate) {
    Query query;
    BudgetMonth budgetMonth = null;
    query = dataSession.createQuery("from BudgetMonth where budgetAccount = ? and monthDate = ?");
    query.setParameter(0, budgetAccount);
    query.setParameter(1, monthDate);
    List<BudgetMonth> budgetMonthList = query.list();

    if (budgetMonthList.size() == 0) {
      budgetMonth = new BudgetMonth();
      budgetMonth.setBudgetAccount(budgetAccount);
      budgetMonth.setMonthDate(monthDate);

    } else {
      budgetMonth = budgetMonthList.get(0);
    }
    query.setParameter(0, budgetAccount);
    query.setParameter(1, MonthUtil.getPrevMonth(monthDate));
    List<BudgetMonth> prevBudgetMonthList = query.list();
    BudgetMonth prevBudgetMonth = null;
    if (prevBudgetMonthList.size() == 1) {
      prevBudgetMonth = prevBudgetMonthList.get(0);
    }
    Transaction trans = dataSession.beginTransaction();
    try {
      if (prevBudgetMonth == null) {
        budgetMonth.setBalanceStart(budgetAccount.getStartAmount());
        budgetMonth.setBalanceEnd(budgetAccount.getStartAmount());
      } else {
        budgetMonth.setBalanceStart(prevBudgetMonth.getBalanceEnd());
        budgetMonth.setBalanceEnd(prevBudgetMonth.getBalanceEnd());
      }
      dataSession.saveOrUpdate(budgetMonth);
    } finally {
      trans.commit();
    }
    return budgetMonth;
  }

  protected static BudgetMonth getBudgetMonth(Session dataSession, BudgetAccount budgetAccount,
      Date monthDate) {
    Query query;
    BudgetMonth budgetMonth = null;
    query = dataSession.createQuery("from BudgetMonth where budgetAccount = ? and monthDate = ?");
    query.setParameter(0, budgetAccount);
    query.setParameter(1, monthDate);
    List<BudgetMonth> budgetMonthList = query.list();
    if (budgetMonthList.size() == 0) {
      return null;
    } else {
      budgetMonth = budgetMonthList.get(0);
    }
    return budgetMonth;
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
