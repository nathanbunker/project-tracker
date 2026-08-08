package org.openimmunizationsoftware.pt.servlet;

import static org.openimmunizationsoftware.pt.util.WebEscaper.escapeHtml;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.WeeklyReport;

public class WeeklyReportServlet extends ClientServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }
            Session session = appReq.getDataSession();
            WebUser owner = appReq.getWebUser();
            WeeklyReport report = findOwned(session, request.getParameter("weeklyReportId"), owner.getWebUserId());
            if (report == null) {
                Query query = session
                        .createQuery("from WeeklyReport where ownerUserId = :ownerUserId order by reportName");
                query.setInteger("ownerUserId", owner.getWebUserId());
                @SuppressWarnings("unchecked")
                List<WeeklyReport> reports = query.list();
                if (reports.size() == 1) {
                    response.sendRedirect("WeeklyReportServlet?weeklyReportId=" + reports.get(0).getWeeklyReportId());
                    return;
                }
                renderChooser(appReq, reports);
                return;
            }
            appReq.setTitle(report.getReportName());
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();
            LocalDate week = WeeklyReportSupport.normalizeWeek(request.getParameter("week"), owner.getLocalDateToday());
            WeeklyReportSupport.printReportBody(out, session, report, owner, week, "WeeklyReportServlet", null);
            printHtmlFoot(appReq);
        } finally {
            appReq.close();
        }
    }

    private void renderChooser(AppReq appReq, List<WeeklyReport> reports) {
        appReq.setTitle("Weekly Report");
        printHtmlHead(appReq);
        PrintWriter out = appReq.getOut();
        printDandelionLocation(out, "Time Management & Reporting / Weekly Report");
        if (reports.isEmpty()) {
            out.println(
                    "<p>Create a weekly report series before viewing a weekly report. <a href=\"WeeklyReportsServlet\">Manage Weekly Reports</a></p>");
        } else {
            out.println("<h1>Weekly Reports</h1><ul>");
            for (WeeklyReport report : reports)
                out.println("<li><a href=\"WeeklyReportServlet?weeklyReportId=" + report.getWeeklyReportId() + "\">"
                        + escapeHtml(report.getReportName()) + "</a></li>");
            out.println("</ul>");
        }
        printHtmlFoot(appReq);
    }

    private static WeeklyReport findOwned(Session session, String value, int ownerUserId) {
        if (value == null)
            return null;
        try {
            WeeklyReport report = (WeeklyReport) session.get(WeeklyReport.class, Integer.parseInt(value));
            return report != null && report.getOwnerUserId() == ownerUserId ? report : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}