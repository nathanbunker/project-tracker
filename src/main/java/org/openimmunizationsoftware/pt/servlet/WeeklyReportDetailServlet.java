package org.openimmunizationsoftware.pt.servlet;

import static org.openimmunizationsoftware.pt.util.WebEscaper.escapeHtml;
import static org.openimmunizationsoftware.pt.util.WebEscaper.urlEncode;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel;
import org.dandeliondaily.weeklyreport.service.WeeklyReportDataService;
import org.dandeliondaily.weeklyreport.service.WeeklyTimeSummaryService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.WeeklyReport;

abstract class WeeklyReportDetailServlet extends ClientServlet {
    protected abstract boolean isPublicView();

    protected abstract String detailTitle();

    protected abstract boolean isRequestAuthorized(Session session, WeeklyReport report,
            HttpServletRequest request);

    protected abstract void renderDetail(PrintWriter out, WeeklyReportViewModel model, WebUser owner,
            HttpServletRequest request, String reportQuery, boolean publicView);

    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (isPublicView())
            setPrivacyHeaders(response);
        AppReq appReq = new AppReq(request, response);
        try {
            WeeklyReport report;
            WebUser owner;
            String reportQuery;
            if (isPublicView()) {
                String key = request.getParameter("key");
                report = findPublic(appReq.getDataSession(), key);
                if (report == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                owner = WeeklyReportSupport.loadOwner(appReq.getDataSession(), report);
                reportQuery = "key=" + urlEncode(key);
            } else {
                if (appReq.isLoggedOut()) {
                    forwardToHome(request, response);
                    return;
                }
                owner = appReq.getWebUser();
                report = findOwned(appReq.getDataSession(), request.getParameter("weeklyReportId"),
                        owner.getWebUserId());
                reportQuery = report == null ? "" : "weeklyReportId=" + report.getWeeklyReportId();
            }
            if (report == null || owner == null
                    || !isRequestAuthorized(appReq.getDataSession(), report, request)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            LocalDate week = WeeklyReportSupport.normalizeWeek(request.getParameter("week"), owner.getLocalDateToday());
            WeeklyReportViewModel model = new WeeklyReportDataService().load(
                    appReq.getDataSession(), report, owner, week);
            PrintWriter out;
            if (isPublicView()) {
                response.setContentType("text/html;charset=UTF-8");
                out = response.getWriter();
                out.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"robots\" "
                        + "content=\"noindex,nofollow\"><title>" + escapeHtml(detailTitle())
                        + "</title></head><body>");
            } else {
                appReq.setTitle(detailTitle());
                printHtmlHead(appReq);
                out = appReq.getOut();
            }
            renderDetail(out, model, owner, request, reportQuery, isPublicView());
            if (isPublicView())
                out.println("</body></html>");
            else
                printHtmlFoot(appReq);
        } finally {
            appReq.close();
        }
    }

    protected static boolean billCodeInScope(Session session, WeeklyReport report, String billCode) {
        if (!WeeklyTimeSummaryService.matchesPrefix(billCode, report.getRootBillCode()))
            return false;
        Query query = session.createQuery("select count(*) from BillEntry where workspaceId = :workspaceId "
                + "and webUser.webUserId = :ownerUserId and billable = 'Y' and billMins > 0 and billCode = :billCode");
        query.setInteger("workspaceId", report.getRootWorkspaceId());
        query.setInteger("ownerUserId", report.getOwnerUserId());
        query.setString("billCode", billCode);
        return ((Number) query.uniqueResult()).longValue() > 0;
    }

    protected static boolean projectInScope(Session session, WeeklyReport report, int projectId, String billCode) {
        if (!billCodeInScope(session, report, billCode))
            return false;
        Query query = session.createQuery("select count(*) from BillEntry where workspaceId = :workspaceId "
                + "and webUser.webUserId = :ownerUserId and projectId = :projectId and billable = 'Y' "
                + "and billMins > 0 and billCode = :billCode");
        query.setInteger("workspaceId", report.getRootWorkspaceId());
        query.setInteger("ownerUserId", report.getOwnerUserId());
        query.setInteger("projectId", projectId);
        query.setString("billCode", billCode);
        return ((Number) query.uniqueResult()).longValue() > 0;
    }

    protected static int parseProjectId(String value) {
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    protected static String reportLink(WeeklyReportViewModel model, String reportQuery, boolean publicView) {
        String route = publicView ? "PublicWeeklyReportServlet" : "WeeklyReportServlet";
        return route + "?" + reportQuery + "&amp;week=" + model.getWeekStart();
    }

    protected static String formatMinutes(int minutes) {
        return String.format("%d:%02d", minutes / 60, minutes % 60);
    }

    private static WeeklyReport findOwned(Session session, String value, int ownerUserId) {
        try {
            WeeklyReport report = (WeeklyReport) session.get(WeeklyReport.class, Integer.parseInt(value));
            return report != null && report.getOwnerUserId() == ownerUserId ? report : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static WeeklyReport findPublic(Session session, String key) {
        if (key == null || key.length() < 20)
            return null;
        Query query = session.createQuery("from WeeklyReport where accessKeyHash = :hash and active = 'Y'");
        query.setString("hash", WeeklyReportSupport.hashAccessKey(key));
        List<WeeklyReport> reports = query.list();
        return reports.size() == 1 ? reports.get(0) : null;
    }

    private static void setPrivacyHeaders(HttpServletResponse response) {
        response.setHeader("X-Robots-Tag", "noindex, nofollow");
        response.setHeader("Referrer-Policy", "no-referrer");
    }
}