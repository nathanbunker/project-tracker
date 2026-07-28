package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.BillPlan;

public class BillPlansServlet extends ClientServlet {

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
            if (appReq.isLoggedOut() || !appReq.isAdmin()) {
                forwardToHome(request, response);
                return;
            }
            Integer activeWorkspaceId = appReq.getActiveWorkspaceId();
            if (activeWorkspaceId == null) {
                forwardToHome(request, response);
                return;
            }

            Session dataSession = appReq.getDataSession();
            Query query = dataSession.createQuery(
                    "from BillPlan where workspaceId = :workspaceId order by webUserId, fiscalStartDate desc, billPlanCode, versionNum desc");
            query.setInteger("workspaceId", activeWorkspaceId.intValue());
            @SuppressWarnings("unchecked")
            List<BillPlan> planList = query.list();

            appReq.setTitle("Allocation Plans");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();
            printDandelionLocation(out, "Billing / Allocation Plans");
            printBillingAdminNav(out, "Allocation Plans");

            out.println("<table class=\"boxed\">");
            out.println("  <tr><th class=\"title\" colspan=\"8\">Allocation Plans</th></tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Plan Code</th>");
            out.println("    <th class=\"boxed\">Label</th>");
            out.println("    <th class=\"boxed\">User ID</th>");
            out.println("    <th class=\"boxed\">Fiscal Start</th>");
            out.println("    <th class=\"boxed\">Fiscal End</th>");
            out.println("    <th class=\"boxed\">Version</th>");
            out.println("    <th class=\"boxed\">Status</th>");
            out.println("    <th class=\"boxed\">Effective</th>");
            out.println("  </tr>");
            if (planList.isEmpty()) {
                out.println(
                        "  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"8\">No allocation plans defined.</td></tr>");
            }
            for (BillPlan plan : planList) {
                out.println("  <tr class=\"boxed\">");
                out.println("    <td class=\"boxed\"><a href=\"BillPlanEditServlet?billPlanId=" + plan.getBillPlanId()
                        + "\">"
                        + n(plan.getBillPlanCode()) + "</a></td>");
                out.println("    <td class=\"boxed\">" + n(plan.getPlanLabel()) + "</td>");
                out.println("    <td class=\"boxed\">" + plan.getWebUserId() + "</td>");
                out.println(
                        "    <td class=\"boxed\">"
                                + (plan.getFiscalStartDate() == null ? ""
                                        : appReq.getWebUser().getDateFormat().format(plan.getFiscalStartDate()))
                                + "</td>");
                out.println(
                        "    <td class=\"boxed\">"
                                + (plan.getFiscalEndDate() == null ? ""
                                        : appReq.getWebUser().getDateFormat().format(plan.getFiscalEndDate()))
                                + "</td>");
                out.println("    <td class=\"boxed\">" + plan.getVersionNum() + "</td>");
                out.println("    <td class=\"boxed\">" + n(plan.getPlanStatus()) + "</td>");
                out.println(
                        "    <td class=\"boxed\">"
                                + (plan.getEffectiveDate() == null ? ""
                                        : appReq.getWebUser().getDateFormat().format(plan.getEffectiveDate()))
                                + "</td>");
                out.println("  </tr>");
            }
            out.println("</table>");
            out.println("<p><a href=\"BillPlanEditServlet\">Create a New Allocation Plan</a></p>");

            printHtmlFoot(appReq);
        } finally {
            appReq.close();
        }
    }
}
