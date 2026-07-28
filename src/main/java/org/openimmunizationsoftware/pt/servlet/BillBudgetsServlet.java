package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dandeliondaily.timereview.model.BudgetUsageSummary;
import org.dandeliondaily.timereview.service.BudgetUsageService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.BillAllocationDao;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillBudget;

public class BillBudgetsServlet extends ClientServlet {

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
                    "from BillBudget where workspaceId = :workspaceId order by startDate, billBudgetCode");
            query.setInteger("workspaceId", activeWorkspaceId.intValue());
            @SuppressWarnings("unchecked")
            List<BillBudget> budgetList = query.list();

            BillAllocationDao allocationDao = new BillAllocationDao(dataSession);
            BudgetUsageService usageService = new BudgetUsageService();

            appReq.setTitle("Contract Budgets");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();
            printDandelionLocation(out, "Billing / Contract Budgets");
            printBillingAdminNav(out, "Contract Budgets");

            out.println("<table class=\"boxed\">");
            out.println("  <tr><th class=\"title\" colspan=\"9\">Contract Budgets</th></tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Budget</th>");
            out.println("    <th class=\"boxed\">Bill Code</th>");
            out.println("    <th class=\"boxed\">Start Date</th>");
            out.println("    <th class=\"boxed\">End Date</th>");
            out.println("    <th class=\"boxed\">Authorized</th>");
            out.println("    <th class=\"boxed\">Used</th>");
            out.println("    <th class=\"boxed\">Remaining</th>");
            out.println("    <th class=\"boxed\">Percent</th>");
            out.println("    <th class=\"boxed\">Status</th>");
            out.println("  </tr>");
            if (budgetList.isEmpty()) {
                out.println("  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"9\">No budgets defined.</td></tr>");
            }
            for (BillBudget budget : budgetList) {
                int usedMinutes = allocationDao.sumUsedBudgetMinutes(budget.getBillBudgetId());
                BudgetUsageSummary summary = usageService.summarizeBudgetUsage(budget, usedMinutes);
                out.println("  <tr class=\"boxed\">");
                out.println("    <td class=\"boxed\"><a href=\"BillBudgetEditServlet?billBudgetId="
                        + budget.getBillBudgetId() + "\">"
                        + n(budget.getBillBudgetCode()) + "</a></td>");
                out.println("    <td class=\"boxed\">" + n(summary.getBillCode()) + "</td>");
                out.println(
                        "    <td class=\"boxed\">"
                                + (summary.getStartDate() == null ? ""
                                        : appReq.getWebUser().getDateFormat().format(summary.getStartDate()))
                                + "</td>");
                out.println("    <td class=\"boxed\">" + (summary.getEndDate() == null ? ""
                        : appReq.getWebUser().getDateFormat().format(summary.getEndDate())) + "</td>");
                out.println(
                        "    <td class=\"boxed\">" + TimeTracker.formatTime(summary.getAuthorizedMinutes()) + "</td>");
                out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(summary.getUsedMinutes()) + "</td>");
                out.println(
                        "    <td class=\"boxed\">" + TimeTracker.formatTime(summary.getRemainingMinutes()) + "</td>");
                out.println("    <td class=\"boxed\">" + summary.getPercentConsumed() + "%</td>");
                out.println(
                        "    <td class=\"boxed\">" + (summary.isOverBudget() ? "Over Budget" : "On Track") + "</td>");
                out.println("  </tr>");
            }
            out.println("</table>");
            out.println("<p><a href=\"BillBudgetEditServlet\">Create a New Contract Budget</a></p>");

            printHtmlFoot(appReq);
        } finally {
            appReq.close();
        }
    }
}
