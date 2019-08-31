/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
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
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.manager.MoneyUtil;
import org.openimmunizationsoftware.pt.model.BudgetAccount;
import org.openimmunizationsoftware.pt.model.BudgetItem;
import org.openimmunizationsoftware.pt.model.BudgetMonth;
import org.openimmunizationsoftware.pt.model.BudgetTrans;
import org.openimmunizationsoftware.pt.model.BudgetTransRecord;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BudgetTransRecordServlet extends ClientServlet {

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
    Session dataSession = getDataSession(session);
    Query query;

    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    int accountId = Integer.parseInt(request.getParameter("accountId"));

    BudgetAccount budgetAccount = (BudgetAccount) dataSession.get(BudgetAccount.class, accountId);

    String action = request.getParameter("action");
    if (action != null) {
      if (action.equals("Link")) {
        int transId = Integer.parseInt(request.getParameter("transId"));
        int transRecordId = Integer.parseInt(request.getParameter("transRecordId"));
        BudgetTrans budgetTrans = (BudgetTrans) dataSession.get(BudgetTrans.class, transId);
        BudgetTransRecord budgetTransRecord =
            (BudgetTransRecord) dataSession.get(BudgetTransRecord.class, transRecordId);
        Transaction transaction = dataSession.beginTransaction();
        try {
          budgetTrans.setBudgetTransRecord(budgetTransRecord);
          budgetTrans.setTransAmount(budgetTransRecord.getTransAmount());
          budgetTrans.setTransDate(budgetTransRecord.getTransDate());
          budgetTransRecord.setBudgetTrans(budgetTrans);
          dataSession.update(budgetTrans);
          dataSession.update(budgetTransRecord);
        } finally {
          transaction.commit();
        }
      } else if (action.equals("Create Trans Record")) {
        String itemIdAndMonthId = request.getParameter("itemIdAndMonthId");
        int pos = itemIdAndMonthId.indexOf("-");
        int itemId = Integer.parseInt(itemIdAndMonthId.substring(0, pos));
        int monthId = Integer.parseInt(itemIdAndMonthId.substring(pos + 1));
        int transRecordId = Integer.parseInt(request.getParameter("transRecordId"));
        BudgetMonth budgetMonth = (BudgetMonth) dataSession.get(BudgetMonth.class, monthId);
        BudgetItem budgetItem = (BudgetItem) dataSession.get(BudgetItem.class, itemId);
        BudgetTransRecord budgetTransRecord =
            (BudgetTransRecord) dataSession.get(BudgetTransRecord.class, transRecordId);

        BudgetTrans budgetTrans = new BudgetTrans();
        budgetTrans.setBudgetItem(budgetItem);
        budgetTrans.setBudgetMonth(budgetMonth);
        budgetTrans.setBudgetTransRecord(budgetTransRecord);
        budgetTrans.setTransAmount(budgetTransRecord.getTransAmount());
        budgetTrans.setTransDate(budgetTransRecord.getTransDate());
        budgetTrans.setTransStatus(BudgetTrans.TRANS_STATUS_PENDING);

        Transaction transaction = dataSession.beginTransaction();
        try {
          dataSession.save(budgetTrans);
          budgetTransRecord.setBudgetTrans(budgetTrans);
          dataSession.update(budgetTransRecord);
        } finally {
          transaction.commit();
        }


      } else if (action.equals("Upload")) {
        String message = null;
        String csv = request.getParameter("csv");
        BufferedReader in = new BufferedReader(new StringReader(csv));
        String line;
        int lineNumber = 0;
        String separator = "\",\"";
        Transaction transaction = dataSession.beginTransaction();
        try {
          while ((line = in.readLine()) != null) {
            lineNumber++;
            int start = 1;
            int end = line.indexOf(separator);
            if (line.startsWith("\"") && line.length() > 30 && end > 0) {
              // Field #1
              String dateString = line.substring(start, end);
              start = end + separator.length();
              end = line.indexOf(separator, start);
              if (end > 0) {
                // Field #2
                String amountString = line.substring(start, end);
                start = end + separator.length();
                end = line.indexOf(separator, start);
                if (end > 0) {
                  // Field #3
                  start = end + separator.length();
                  end = line.indexOf(separator, start);
                  if (end > 0) {
                    // Field #4
                    start = end + separator.length();
                    end = line.length() - 1;
                    if (end > start && line.charAt(end) == '"') {
                      try {
                        String description = line.substring(start, end);
                        int transAmount = MoneyUtil.parse(amountString);
                        Date transDate = sdf.parse(dateString);
                        query = dataSession.createQuery(
                            "from BudgetTransRecord where budgetAccount = ? and transDate = ? and transAmount = ? and description = ? ");
                        query.setParameter(0, budgetAccount);
                        query.setParameter(1, transDate);
                        query.setParameter(2, transAmount);
                        query.setParameter(3, description);
                        if (query.list().size() == 0) {
                          BudgetTransRecord budgetTransRecord = new BudgetTransRecord();
                          budgetTransRecord.setBudgetAccount(budgetAccount);
                          budgetTransRecord.setDescription(description);
                          budgetTransRecord.setTransAmount(transAmount);
                          budgetTransRecord.setTransDate(transDate);
                          dataSession.save(budgetTransRecord);
                        }
                      } catch (Exception e) {
                        message =
                            "Unable to parse on line " + lineNumber + " because " + e.getMessage();
                      }

                    }
                  }
                }
              }
            }
          }
        } finally {
          transaction.commit();
        }
      }
    }

    PrintWriter out = response.getWriter();
    try {
      printHtmlHead(out, "Budget", request);
      out.println("<h1>" + budgetAccount.getAccountLabel() + "</h1>");
      query = dataSession.createQuery(
          "from BudgetTransRecord where budgetAccount = ? and budgetTrans is null order by transDate, transAmount ");
      query.setParameter(0, budgetAccount);

      List<BudgetTransRecord> budgetTransRecordList = query.list();

      if (budgetTransRecordList.size() > 0) {
        out.println("<h2>Unlinked Transactions</h2>");
        List<BudgetTransRecord> budgetTransRecordListUnlinkable =
            new ArrayList<BudgetTransRecord>();

        for (BudgetTransRecord budgetTransRecord : budgetTransRecordList) {
          Calendar startCalendar = Calendar.getInstance();
          startCalendar.setTime(budgetTransRecord.getTransDate());
          startCalendar.add(Calendar.DAY_OF_MONTH, -7);
          Calendar endCalendar = Calendar.getInstance();
          endCalendar.setTime(budgetTransRecord.getTransDate());
          endCalendar.add(Calendar.DAY_OF_MONTH, 7);
          int tenPercent = Math.abs((int) budgetTransRecord.getTransAmount() / 10);
          query = dataSession.createQuery(
              "from BudgetTrans where budgetMonth.budgetAccount = ? and budgetTransRecord is null and transDate >= ? and transDate <= ? and transAmount >= ? and transAmount <= ?");
          query.setParameter(0, budgetAccount);
          query.setParameter(1, startCalendar.getTime());
          query.setParameter(2, endCalendar.getTime());
          query.setParameter(3, budgetTransRecord.getTransAmount() - tenPercent);
          query.setParameter(4, budgetTransRecord.getTransAmount() + tenPercent);

          List<BudgetTrans> budgetTransList = query.list();
          if (budgetTransList.size() == 0) {
            budgetTransRecordListUnlinkable.add(budgetTransRecord);
          } else {
            out.println("<h3>" + budgetTransRecord.getDescription() + "</h3>");
            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Record</th>");
            out.println("    <th class=\"boxed\">Date</th>");
            out.println("    <th class=\"boxed\">Amount</th>");
            out.println("    <th class=\"boxed\" rowspan=\"2\">Choose</th>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">Actual Transaction</td>");
            out.println("    <td class=\"boxed\">" + sdf.format(budgetTransRecord.getTransDate())
                + "</td>");
            out.println("    <td class=\"boxed\"><span class=\"right\">"
                + MoneyUtil.format(budgetTransRecord.getTransAmount()) + "</span></td>");
            out.println("  </tr>");
            for (BudgetTrans budgetTrans : budgetTransList) {
              out.println("  <tr class=\"boxed\">");
              out.println("    <td class=\"boxed\">" + budgetTrans.getBudgetItem().getItemLabel()
                  + "</td>");
              out.println(
                  "    <td class=\"boxed\">" + sdf.format(budgetTrans.getTransDate()) + "</td>");
              out.println("    <td class=\"boxed\"><span class=\"right\">"
                  + MoneyUtil.format(budgetTrans.getTransAmount()) + "</span></td>");
              String link = "BudgetTransRecordServlet?action=Link&accountId="
                  + budgetAccount.getAccountId() + "&transId=" + budgetTrans.getTransId()
                  + "&transRecordId=" + budgetTransRecord.getTransRecordId() + "";
              out.println("    <td class=\"boxed\"><a href=\"" + link
                  + "\" class=\"button\">Link</a></td>");
              out.println("  </tr>");
            }
            out.println("</table>");
          }
        }
        if (budgetTransRecordListUnlinkable.size() > 0) {
          SimpleDateFormat budgetMonthFormat = new SimpleDateFormat("MMM yyyy");
          out.println(
              "<p>The following transactions could not be linked because no matching records were found.</p>");
          query = dataSession.createQuery(
              "from BudgetItem where budgetAccount = ?  and (itemStatus = 'Y' or itemStatus = 'M') order by itemLabel");
          query.setParameter(0, budgetAccount);
          List<BudgetItem> budgetItemList = query.list();
          query = dataSession
              .createQuery("from BudgetMonth where budgetAccount = ? order by monthDate");
          query.setParameter(0, budgetAccount);
          List<BudgetMonth> budgetMonthList = query.list();
          for (BudgetTransRecord budgetTransRecord : budgetTransRecordListUnlinkable) {
            out.println("<h3>" + budgetTransRecord.getDescription() + "</h3>");
            out.println("<form action=\"BudgetTransRecordServlet\" method=\"POST\">");
            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Date</th>");
            out.println("    <th class=\"boxed\">Amount</th>");
            out.println("    <th class=\"boxed\">Description</th>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">" + sdf.format(budgetTransRecord.getTransDate())
                + "</td>");
            out.println("    <td class=\"boxed\"><span class=\"right\">"
                + MoneyUtil.format(budgetTransRecord.getTransAmount()) + "</span></td>");
            out.println("    <td class=\"boxed\">" + budgetTransRecord.getDescription() + "</td>");
            out.println("  </tr>");

            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\" colspan=\"2\">Link With</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <select name=\"itemIdAndMonthId\">");
            out.println("        <option value=\"\">--select--</option>");
            Calendar startCalendar = Calendar.getInstance();
            startCalendar.setTime(budgetTransRecord.getTransDate());
            startCalendar.set(Calendar.DAY_OF_MONTH, 1);
            startCalendar.add(Calendar.MONTH, -1);
            Date startDate = startCalendar.getTime();
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTime(budgetTransRecord.getTransDate());
            endCalendar.set(Calendar.DAY_OF_MONTH, 2);
            endCalendar.add(Calendar.MONTH, 1);
            Date endDate = endCalendar.getTime();
            for (BudgetMonth budgetMonth : budgetMonthList) {
              if (!budgetMonth.getMonthDate().before(startDate)
                  && !budgetMonth.getMonthDate().after(endDate)) {
                for (BudgetItem budgetItem : budgetItemList) {
                  query = dataSession
                      .createQuery("from BudgetTrans where budgetItem = ? and budgetMonth = ?");
                  query.setParameter(0, budgetItem);
                  query.setParameter(1, budgetMonth);
                  List<BudgetTrans> budgetTransList = query.list();
                  if (budgetTransList.size() == 0) {

                    out.println("        <option value=\"" + budgetItem.getItemId() + "-"
                        + budgetMonth.getMonthId() + "\">"
                        + budgetMonthFormat.format(budgetMonth.getMonthDate()) + " - "
                        + budgetItem.getItemLabel() + "</option>");
                  }
                }
              }
            }

            out.println("      </select>");
            out.println(
                "      <input type=\"submit\" name=\"action\" value=\"Create Trans Record\">");
            out.println("      <input type=\"hidden\" name=\"accountId\" value=\""
                + budgetAccount.getAccountId() + "\">");
            out.println("      <input type=\"hidden\" name=\"transRecordId\" value=\""
                + budgetTransRecord.getTransRecordId() + "\">");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("</table>");
            out.println("</form>");

          }
        }
      }

      query = dataSession.createQuery(
          "from BudgetTransRecord where budgetAccount = ? order by transDate, transAmount ");
      query.setParameter(0, budgetAccount);

      budgetTransRecordList = query.list();

      out.println("<h2>All Transactions Recorded</h2>");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"title\" colspan=\"3\">Transaction Records</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Date</th>");
      out.println("    <th class=\"boxed\">Amount</th>");
      out.println("    <th class=\"boxed\">Description</th>");
      out.println("  </tr>");
      for (BudgetTransRecord budgetTransRecord : budgetTransRecordList) {
        out.println("  <tr class=\"boxed\">");
        out.println(
            "    <td class=\"boxed\">" + sdf.format(budgetTransRecord.getTransDate()) + "</td>");
        out.println("    <td class=\"boxed\"><span class=\"right\">"
            + MoneyUtil.format(budgetTransRecord.getTransAmount()) + "</span></td>");
        out.println("    <td class=\"boxed\">" + budgetTransRecord.getDescription() + "</td>");
        out.println("  </tr>");

      }
      out.println("</table>");
      out.println("<h2>Upload CSV</h2>");
      out.println("<form action=\"BudgetTransRecordServlet\" method=\"POST\">");
      out.println("<textarea name=\"csv\" cols=\"40\" rows=\"5\"></textarea><br/>");
      out.println("<input type=\"submit\" name=\"action\" value=\"Upload\">");
      out.println("<input type=\"hidden\" name=\"accountId\" value=\""
          + budgetAccount.getAccountId() + "\">");
      out.println("</form>");
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
