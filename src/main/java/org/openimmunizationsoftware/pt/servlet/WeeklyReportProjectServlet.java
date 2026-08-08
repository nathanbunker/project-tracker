package org.openimmunizationsoftware.pt.servlet;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel;
import org.dandeliondaily.weeklyreport.render.WeeklyReportRenderer;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.WeeklyReport;

public class WeeklyReportProjectServlet extends WeeklyReportDetailServlet {
    @Override
    protected boolean isPublicView() {
        return false;
    }

    @Override
    protected String detailTitle() {
        return "Weekly Report Project Detail";
    }

    @Override
    protected boolean isRequestAuthorized(Session session, WeeklyReport report,
            HttpServletRequest request) {
        return projectInScope(session, report, parseProjectId(request.getParameter("projectId")),
                request.getParameter("billCode"));
    }

    @Override
    protected void renderDetail(PrintWriter out, WeeklyReportViewModel model, WebUser owner,
            HttpServletRequest request, String reportQuery, boolean publicView) {
        int projectId = parseProjectId(request.getParameter("projectId"));
        String billCode = request.getParameter("billCode");
        new WeeklyReportRenderer().renderProjectDetail(out, model, owner, projectId, billCode,
                reportLink(model, reportQuery, publicView));
    }
}