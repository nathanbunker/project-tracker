package org.openimmunizationsoftware.pt.student.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.GamePointLedger;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

public class StudentBankServlet extends StudentBaseServlet {

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_TRANSFER_POINTS = "transferPoints";

    private static final String ACTION_MOVE_TO_SAVINGS = "moveToSavings";
    private static final String ACTION_MOVE_TO_SPENDABLE = "moveToSpendable";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                response.sendRedirect("../LoginServlet");
                return;
            }

            WebUser webUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();

            String action = request.getParameter(PARAM_ACTION);
            Integer transferPoints = parseInteger(request.getParameter(PARAM_TRANSFER_POINTS));
            if (action != null) {
                if (ACTION_MOVE_TO_SAVINGS.equals(action)) {
                    handleTransfer(webUser, dataSession, GamePointLedger.ACCOUNT_SPENDABLE,
                            GamePointLedger.ACCOUNT_SAVINGS, intValue(transferPoints));
                } else if (ACTION_MOVE_TO_SPENDABLE.equals(action)) {
                    handleTransfer(webUser, dataSession, GamePointLedger.ACCOUNT_SAVINGS,
                            GamePointLedger.ACCOUNT_SPENDABLE, intValue(transferPoints));
                }
                response.sendRedirect("bank");
                return;
            }

            int spendablePoints = getAccountPoints(webUser, dataSession, GamePointLedger.ACCOUNT_SPENDABLE);
            int savingsPoints = getAccountPoints(webUser, dataSession, GamePointLedger.ACCOUNT_SAVINGS);

            appReq.setTitle("Student Bank");
            printHtmlHead(appReq, "Bank");
            PrintWriter out = appReq.getOut();

            out.println("<h1>Bank</h1>");
            out.println("<table class=\"boxed-mobile\" style=\"width:100%; margin-bottom:10px;\">");
            out.println("  <tr class=\"boxed\"><th class=\"boxed\">Savings Points</th>");
            out.println("    <td class=\"boxed\" style=\"text-align:right; font-size:18px; font-weight:bold;\">"
                    + savingsPoints + "</td></tr>");
            out.println("  <tr class=\"boxed\"><th class=\"boxed\">Spendable Points</th>");
            out.println("    <td class=\"boxed\" style=\"text-align:right;\">" + spendablePoints + "</td></tr>");
            out.println("</table>");

            out.println("<table class=\"boxed-mobile\" style=\"width:100%; margin-bottom:10px;\">");
            out.println("  <tr class=\"boxed\"><th class=\"boxed\" colspan=\"2\">Move Points</th></tr>");
            out.println("  <tr class=\"boxed\"><td class=\"boxed\">Move to Savings</td><td class=\"boxed\">");
            out.println("    <form action=\"bank\" method=\"POST\" style=\"margin:0;\">");
            out.println("      <input type=\"number\" name=\"" + PARAM_TRANSFER_POINTS
                    + "\" min=\"1\" step=\"1\" style=\"width:90px;\" required>");
            out.println("      <button type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\""
                    + ACTION_MOVE_TO_SAVINGS + "\">Move</button>");
            out.println("    </form>");
            out.println("  </td></tr>");
            out.println("  <tr class=\"boxed\"><td class=\"boxed\">Move to Spendable</td><td class=\"boxed\">");
            out.println("    <form action=\"bank\" method=\"POST\" style=\"margin:0;\">");
            out.println("      <input type=\"number\" name=\"" + PARAM_TRANSFER_POINTS
                    + "\" min=\"1\" step=\"1\" style=\"width:90px;\" required>");
            out.println("      <button type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\""
                    + ACTION_MOVE_TO_SPENDABLE + "\">Move</button>");
            out.println("    </form>");
            out.println("  </td></tr>");
            out.println("</table>");

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
            if (!response.isCommitted()) {
                response.sendRedirect("../HomeServlet");
            }
        } finally {
            appReq.close();
        }
    }

    private int getAccountPoints(WebUser webUser, Session dataSession, String accountBucket) {
        Query query = dataSession.createQuery(
                "select sum(gpl.pointChange) from GamePointLedger gpl where gpl.contact.contactId = :contactId and (gpl.accountBucket = :accountBucket or (:includeLegacy = true and gpl.accountBucket is null))");
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("accountBucket", accountBucket);
        query.setParameter("includeLegacy", Boolean.valueOf(GamePointLedger.ACCOUNT_SPENDABLE.equals(accountBucket)));
        return intValue((Number) query.uniqueResult());
    }

    private void handleTransfer(WebUser webUser, Session dataSession, String fromBucket, String toBucket, int points) {
        if (points <= 0 || fromBucket.equals(toBucket)) {
            return;
        }
        int sourceBalance = getAccountPoints(webUser, dataSession, fromBucket);
        if (sourceBalance < points) {
            return;
        }

        ProjectContact contact = (ProjectContact) dataSession.get(ProjectContact.class, webUser.getContactId());
        if (contact == null) {
            return;
        }

        Date now = new Date();
        Transaction trans = dataSession.beginTransaction();
        try {
            GamePointLedger withdraw = new GamePointLedger();
            withdraw.setContact(contact);
            withdraw.setPointChange(Integer.valueOf(-points));
            withdraw.setAccountBucket(fromBucket);
            withdraw.setEntryType("TRANSFER");
            withdraw.setEntryNote("Moved " + points + " points to " + toBucket.toLowerCase());
            withdraw.setCreatedDate(now);
            withdraw.setCreatedBy(webUser.getUsername());
            dataSession.save(withdraw);

            GamePointLedger deposit = new GamePointLedger();
            deposit.setContact(contact);
            deposit.setPointChange(Integer.valueOf(points));
            deposit.setAccountBucket(toBucket);
            deposit.setEntryType("TRANSFER");
            deposit.setEntryNote("Moved " + points + " points from " + fromBucket.toLowerCase());
            deposit.setCreatedDate(now);
            deposit.setCreatedBy(webUser.getUsername());
            dataSession.save(deposit);

            trans.commit();
        } catch (RuntimeException e) {
            trans.rollback();
            throw e;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().equals("")) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
