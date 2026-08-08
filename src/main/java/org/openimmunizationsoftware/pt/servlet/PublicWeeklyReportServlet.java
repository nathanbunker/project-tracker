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
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.WeeklyReport;

public class PublicWeeklyReportServlet extends ClientServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("X-Robots-Tag", "noindex, nofollow");
        response.setHeader("Referrer-Policy", "no-referrer");
        AppReq appReq = new AppReq(request, response);
        try {
            String key = request.getParameter("key");
            if (key == null || key.length() < 20) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Query query = appReq.getDataSession()
                    .createQuery("from WeeklyReport where accessKeyHash = :hash and active = 'Y'");
            query.setString("hash", WeeklyReportSupport.hashAccessKey(key));
            @SuppressWarnings("unchecked")
            List<WeeklyReport> reports = query.list();
            if (reports.size() != 1) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            WeeklyReport report = reports.get(0);
            WebUser owner = WeeklyReportSupport.loadOwner(appReq.getDataSession(), report);
            if (owner == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            LocalDate week = WeeklyReportSupport.normalizeWeek(request.getParameter("week"), owner.getLocalDateToday());
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println(
                    "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"robots\" content=\"noindex,nofollow\"><title>"
                            + escapeHtml(report.getReportName()) + "</title></head><body>");
            WeeklyReportSupport.printReportBody(out, appReq.getDataSession(), report, owner, week,
                    "PublicWeeklyReportServlet", key);
            out.println("</body></html>");
        } finally {
            appReq.close();
        }
    }
}