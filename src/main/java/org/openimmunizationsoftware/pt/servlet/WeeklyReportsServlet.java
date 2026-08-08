package org.openimmunizationsoftware.pt.servlet;

import static org.openimmunizationsoftware.pt.util.WebEscaper.escapeHtml;
import static org.openimmunizationsoftware.pt.util.WebEscaper.urlEncode;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.WeeklyReport;

public class WeeklyReportsServlet extends ClientServlet {
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
            throws IOException, ServletException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut() || appReq.getActiveWorkspaceId() == null) {
                forwardToHome(request, response);
                return;
            }
            Session dataSession = appReq.getDataSession();
            WebUser owner = appReq.getWebUser();
            WeeklyReport report = findOwned(dataSession, request.getParameter("weeklyReportId"), owner.getWebUserId());
            String action = request.getParameter("action");
            String oneTimeUrl = null;

            if ("Save".equals(action)) {
                if (report == null) {
                    report = new WeeklyReport();
                    report.setOwnerUserId(owner.getWebUserId());
                    report.setCreatedAt(new Date());
                }
                String problem = applyForm(request, dataSession, report, appReq.getActiveWorkspaceId());
                if (problem == null) {
                    report.setUpdatedAt(new Date());
                    saveOrUpdate(dataSession, report);
                    response.sendRedirect("WeeklyReportsServlet");
                    return;
                }
                appReq.setMessageProblem(problem);
            } else if (report != null && "Delete".equals(action)) {
                Transaction transaction = dataSession.beginTransaction();
                dataSession.delete(report);
                transaction.commit();
                response.sendRedirect("WeeklyReportsServlet");
                return;
            } else if (report != null && ("Generate Link".equals(action) || "Regenerate Link".equals(action))) {
                String key = WeeklyReportSupport.generateAccessKey();
                report.setAccessKeyHash(WeeklyReportSupport.hashAccessKey(key));
                report.setAccessKeyCreatedAt(new Date());
                report.setUpdatedAt(new Date());
                saveOrUpdate(dataSession, report);
                oneTimeUrl = buildPublicUrl(request, key);
            } else if (report != null && "Disable Link".equals(action)) {
                report.setAccessKeyHash(null);
                report.setAccessKeyCreatedAt(null);
                report.setUpdatedAt(new Date());
                saveOrUpdate(dataSession, report);
                response.sendRedirect("WeeklyReportsServlet");
                return;
            }

            renderPage(appReq, report, oneTimeUrl);
        } finally {
            appReq.close();
        }
    }

    private void renderPage(AppReq appReq, WeeklyReport editing, String oneTimeUrl) {
        Session dataSession = appReq.getDataSession();
        WebUser owner = appReq.getWebUser();
        Query query = dataSession.createQuery("from WeeklyReport where ownerUserId = :ownerUserId order by reportName");
        query.setInteger("ownerUserId", owner.getWebUserId());
        @SuppressWarnings("unchecked")
        List<WeeklyReport> reports = query.list();
        Query billQuery = dataSession
                .createQuery("from BillCode where workspaceId = :workspaceId and visible = 'Y' order by id.billCode");
        billQuery.setInteger("workspaceId", appReq.getActiveWorkspaceId());
        @SuppressWarnings("unchecked")
        List<BillCode> billCodes = billQuery.list();

        appReq.setTitle("Weekly Reports");
        printHtmlHead(appReq);
        PrintWriter out = appReq.getOut();
        printDandelionLocation(out, "Time Management & Reporting / Weekly Reports");
        if (oneTimeUrl != null) {
            out.println(
                    "<p class=\"pass\"><strong>This public URL is shown only once.</strong> Anyone possessing it can view the report.</p>");
            out.println("<p><input type=\"text\" readonly size=\"100\" value=\"" + escapeHtmlAttribute(oneTimeUrl)
                    + "\"></p>");
        }
        out.println("<table class=\"boxed\"><tr><th class=\"title\" colspan=\"8\">Weekly Report Series</th></tr>");
        out.println(
                "<tr class=\"boxed\"><th class=\"boxed\">Name</th><th class=\"boxed\">Root billing code</th><th class=\"boxed\">Active</th><th class=\"boxed\">Access link</th><th class=\"boxed\">Edit</th><th class=\"boxed\">Delete</th><th class=\"boxed\">View</th><th class=\"boxed\">Link management</th></tr>");
        if (reports.isEmpty())
            out.println("<tr><td class=\"boxed\" colspan=\"8\">No weekly report series defined.</td></tr>");
        for (WeeklyReport item : reports) {
            int id = item.getWeeklyReportId();
            out.println("<tr class=\"boxed\"><td class=\"boxed\">" + escapeHtml(item.getReportName())
                    + "</td><td class=\"boxed\">" + escapeHtml(item.getRootBillCode()) + "</td><td class=\"boxed\">"
                    + ("Y".equals(item.getActive()) ? "Yes" : "No") + "</td><td class=\"boxed\">"
                    + (item.getAccessKeyHash() == null ? "No" : "Yes") + "</td>");
            out.println("<td class=\"boxed\"><a href=\"WeeklyReportsServlet?weeklyReportId=" + id + "\">Edit</a></td>");
            out.println(
                    "<td class=\"boxed\"><form method=\"POST\" action=\"WeeklyReportsServlet\" onsubmit=\"return confirm('Delete this weekly report series?');\"><input type=\"hidden\" name=\"weeklyReportId\" value=\""
                            + id + "\"><input type=\"submit\" name=\"action\" value=\"Delete\"></form></td>");
            out.println("<td class=\"boxed\"><a href=\"WeeklyReportServlet?weeklyReportId=" + id + "\">View</a></td>");
            out.println(
                    "<td class=\"boxed\"><form method=\"POST\" action=\"WeeklyReportsServlet\"><input type=\"hidden\" name=\"weeklyReportId\" value=\""
                            + id + "\"><input type=\"submit\" name=\"action\" value=\""
                            + (item.getAccessKeyHash() == null ? "Generate Link" : "Regenerate Link") + "\">"
                            + (item.getAccessKeyHash() == null ? ""
                                    : " <input type=\"submit\" name=\"action\" value=\"Disable Link\">")
                            + "</form></td></tr>");
        }
        out.println("</table>");

        WeeklyReport form = editing == null ? new WeeklyReport() : editing;
        out.println(
                "<form method=\"POST\" action=\"WeeklyReportsServlet\"><input type=\"hidden\" name=\"weeklyReportId\" value=\""
                        + form.getWeeklyReportId() + "\"><table class=\"boxed\"><tr><th class=\"title\" colspan=\"2\">"
                        + (form.getWeeklyReportId() == 0 ? "Create" : "Edit") + " Weekly Report Series</th></tr>");
        out.println(
                "<tr class=\"boxed\"><th class=\"boxed\">Report name</th><td class=\"boxed\"><input type=\"text\" name=\"reportName\" maxlength=\"150\" size=\"50\" value=\""
                        + escapeHtmlAttribute(n(form.getReportName())) + "\"></td></tr>");
        out.println(
                "<tr class=\"boxed\"><th class=\"boxed\">Root billing code</th><td class=\"boxed\"><select name=\"rootBillCode\"><option value=\"\"></option>");
        for (BillCode billCode : billCodes) {
            String code = billCode.getBillCode();
            out.println("<option value=\"" + escapeHtmlAttribute(code) + "\""
                    + (code.equals(form.getRootBillCode()) ? " selected" : "") + ">"
                    + escapeHtml(code + " - " + n(billCode.getBillLabel())) + "</option>");
        }
        out.println(
                "</select></td></tr><tr class=\"boxed\"><th class=\"boxed\">Active</th><td class=\"boxed\"><input type=\"checkbox\" name=\"active\" value=\"Y\""
                        + (form.getWeeklyReportId() == 0 || "Y".equals(form.getActive()) ? " checked" : "")
                        + "></td></tr>");
        out.println(
                "<tr><td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save\"></td></tr></table></form>");
        printHtmlFoot(appReq);
    }

    private String applyForm(HttpServletRequest request, Session dataSession, WeeklyReport report, int workspaceId) {
        String name = n(request.getParameter("reportName")).trim();
        String billCode = n(request.getParameter("rootBillCode")).trim();
        if (name.length() == 0 || name.length() > 150)
            return "A report name of 150 characters or fewer is required.";
        Query query = dataSession.createQuery(
                "select count(*) from BillCode where workspaceId = :workspaceId and id.billCode = :billCode and visible = 'Y'");
        query.setInteger("workspaceId", workspaceId);
        query.setString("billCode", billCode);
        if (((Number) query.uniqueResult()).intValue() != 1)
            return "Select an available root billing code.";
        report.setReportName(name);
        report.setRootWorkspaceId(workspaceId);
        report.setRootBillCode(billCode);
        report.setActive(request.getParameter("active") == null ? "N" : "Y");
        return null;
    }

    private static WeeklyReport findOwned(Session session, String value, int ownerUserId) {
        if (value == null || value.trim().length() == 0)
            return null;
        try {
            WeeklyReport report = (WeeklyReport) session.get(WeeklyReport.class, Integer.parseInt(value));
            return report != null && report.getOwnerUserId() == ownerUserId ? report : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void saveOrUpdate(Session session, WeeklyReport report) {
        Transaction transaction = session.beginTransaction();
        session.saveOrUpdate(report);
        transaction.commit();
    }

    private static String buildPublicUrl(HttpServletRequest request, String key) {
        String url = request.getRequestURL().toString();
        return url.substring(0, url.lastIndexOf('/') + 1) + "PublicWeeklyReportServlet?key=" + urlEncode(key);
    }
}