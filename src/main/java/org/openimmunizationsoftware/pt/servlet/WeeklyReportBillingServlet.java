package org.openimmunizationsoftware.pt.servlet;

import static org.openimmunizationsoftware.pt.util.WebEscaper.escapeHtml;
import static org.openimmunizationsoftware.pt.util.WebEscaper.urlEncode;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel;
import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel.AllocationRow;
import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel.ProjectActivity;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.WeeklyReport;

public class WeeklyReportBillingServlet extends WeeklyReportDetailServlet {
    @Override
    protected boolean isPublicView() {
        return false;
    }

    @Override
    protected String detailTitle() {
        return "Weekly Report Billing Detail";
    }

    @Override
    protected boolean isRequestAuthorized(Session session, WeeklyReport report,
            HttpServletRequest request) {
        return billCodeInScope(session, report, request.getParameter("billCode"));
    }

    @Override
    protected void renderDetail(PrintWriter out, WeeklyReportViewModel model, WebUser owner,
            HttpServletRequest request, String reportQuery, boolean publicView) {
        String billCode = request.getParameter("billCode");
        AllocationRow row = model.getAllocationRows().get(billCode);
        out.println("<p><a href=\"" + reportLink(model, reportQuery, publicView) + "\">Back to weekly report</a></p>");
        out.println("<h1>" + escapeHtml(billCode) + "</h1>");
        if (row != null) {
            out.println("<p><strong>Week:</strong> " + formatMinutes(row.getWeekMinutes())
                    + " &middot; <strong>Four weeks:</strong> " + formatMinutes(row.getFourWeekMinutes())
                    + " &middot; <strong>Fiscal year:</strong> " + formatMinutes(row.getFiscalYearMinutes()) + "</p>");
        }
        out.println("<h2>Projects This Week</h2>");
        boolean found = false;
        for (ProjectActivity activity : model.getProjectActivities().values()) {
            if (!billCode.equals(activity.getBillCode()))
                continue;
            found = true;
            String route = publicView ? "PublicWeeklyReportProjectServlet" : "WeeklyReportProjectServlet";
            String name = activity.getProject() == null ? "Project " + activity.getProjectId()
                    : activity.getProject().getProjectName();
            out.println("<p><a href=\"" + route + "?" + reportQuery + "&amp;week=" + model.getWeekStart()
                    + "&amp;projectId=" + activity.getProjectId() + "&amp;billCode=" + urlEncode(billCode) + "\">"
                    + escapeHtml(name) + "</a>: " + formatMinutes(activity.getRoundedMinutes()) + "</p>");
        }
        if (!found)
            out.println("<p>No project time was recorded against this code for the selected week.</p>");
    }
}