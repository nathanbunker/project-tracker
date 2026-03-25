package org.openimmunizationsoftware.pt.student.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.WebUser;

public class StudentStoreServlet extends StudentBaseServlet {

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

            int availablePoints = getAvailablePoints(webUser, dataSession);
            int lifetimePointsEarned = getLifetimePointsEarned(webUser, dataSession);
            int pointsEarnedLast7Days = getPointsEarnedLast7Days(webUser, dataSession);

            appReq.setTitle("Student Store");
            printHtmlHead(appReq, "Store");
            PrintWriter out = appReq.getOut();

            out.println("<h1>Store</h1>");
            out.println("<table class=\"boxed-mobile\">");
            out.println("  <tr class=\"boxed\"><th class=\"boxed\">Available Points</th><td class=\"boxed\"><strong>"
                    + availablePoints + "</strong></td></tr>");
            out.println("  <tr class=\"boxed\"><th class=\"boxed\">Total Points Earned</th><td class=\"boxed\">"
                    + lifetimePointsEarned + "</td></tr>");
            out.println("  <tr class=\"boxed\"><th class=\"boxed\">Points Earned Last 7 Days</th><td class=\"boxed\">"
                    + pointsEarnedLast7Days + "</td></tr>");
            out.println("</table>");

            out.println("<p class=\"small\">Store purchasing will be added in a later update.</p>");

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

    private int getAvailablePoints(WebUser webUser, Session dataSession) {
        Query query = dataSession.createQuery(
                "select sum(gpl.pointChange) from GamePointLedger gpl where gpl.contact.contactId = :contactId");
        query.setParameter("contactId", webUser.getContactId());
        return intValue((Number) query.uniqueResult());
    }

    private int getLifetimePointsEarned(WebUser webUser, Session dataSession) {
        Query query = dataSession.createQuery(
                "select sum(gpl.pointChange) from GamePointLedger gpl where gpl.contact.contactId = :contactId and gpl.pointChange > 0");
        query.setParameter("contactId", webUser.getContactId());
        return intValue((Number) query.uniqueResult());
    }

    private int getPointsEarnedLast7Days(WebUser webUser, Session dataSession) {
        Date periodStart = webUser.startOfDay(webUser.addDays(webUser.getToday(), -6));
        Date periodEnd = webUser.endOfDay(webUser.getToday());

        Query query = dataSession.createQuery(
                "select sum(gpl.pointChange) from GamePointLedger gpl " +
                        "where gpl.contact.contactId = :contactId " +
                        "and gpl.pointChange > 0 " +
                        "and gpl.createdDate >= :periodStart and gpl.createdDate <= :periodEnd");
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("periodStart", periodStart);
        query.setParameter("periodEnd", periodEnd);
        return intValue((Number) query.uniqueResult());
    }
}
