package org.openimmunizationsoftware.pt.servlet;

public class PublicWeeklyReportBillingServlet extends WeeklyReportBillingServlet {
    @Override
    protected boolean isPublicView() {
        return true;
    }
}