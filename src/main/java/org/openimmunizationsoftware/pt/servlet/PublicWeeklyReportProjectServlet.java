package org.openimmunizationsoftware.pt.servlet;

public class PublicWeeklyReportProjectServlet extends WeeklyReportProjectServlet {
    @Override
    protected boolean isPublicView() {
        return true;
    }
}