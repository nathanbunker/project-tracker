package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;

public class BillBudgetEditServlet extends ClientServlet {

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
            Query billCodeQuery = dataSession.createQuery(
                    "from BillCode where workspaceId = :workspaceId and visible = 'Y' order by billLabel");
            billCodeQuery.setInteger("workspaceId", activeWorkspaceId.intValue());
            @SuppressWarnings("unchecked")
            List<BillCode> billCodeList = billCodeQuery.list();

            BillBudget billBudget = resolveBillBudget(request, dataSession, activeWorkspaceId.intValue());
            if (billBudget == null) {
                billBudget = new BillBudget();
                billBudget.setWorkspaceId(activeWorkspaceId);
            }

            String action = appReq.getAction();
            if ("Save".equals(action)) {
                String message = applyAndValidate(request, appReq, dataSession, billBudget, billCodeList,
                        activeWorkspaceId.intValue());
                if (message == null) {
                    Transaction trans = dataSession.beginTransaction();
                    try {
                        dataSession.saveOrUpdate(billBudget);
                        trans.commit();
                        response.sendRedirect("BillBudgetsServlet");
                        return;
                    } catch (Exception e) {
                        trans.rollback();
                        message = "Unable to save contract budget: " + e.getMessage();
                    }
                }
                appReq.setMessageProblem(message);
            }

            appReq.setTitle("Contract Budget");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();
            printDandelionLocation(out, "Billing / Contract Budgets");
            printBillingAdminNav(out, "Contract Budgets");

            boolean lockBillCode = isBudgetReferenced(dataSession, billBudget);

            out.println("<form method=\"POST\" action=\"BillBudgetEditServlet\">");
            out.println("<input type=\"hidden\" name=\"billBudgetId\" value=\"" + billBudget.getBillBudgetId() + "\">");
            out.println("<table class=\"boxed\">");
            out.println("  <tr><th class=\"title\" colspan=\"2\">Edit Contract Budget</th></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Budget Code</th><td class=\"boxed\"><input type=\"text\" name=\"billBudgetCode\" value=\""
                            + escapeHtmlAttribute(n(billBudget.getBillBudgetCode())) + "\" size=\"30\"></td></tr>");
            out.println("  <tr class=\"boxed\"><th class=\"boxed\">Bill Code</th><td class=\"boxed\">");
            if (lockBillCode && billBudget.getBillCode() != null) {
                out.println("<input type=\"hidden\" name=\"billCode\" value=\""
                        + escapeHtmlAttribute(n(billBudget.getBillCode().getBillCode())) + "\">");
                out.println(n(billBudget.getBillCode().getBillCode()));
            } else {
                out.println("<select name=\"billCode\">");
                out.println("<option value=\"\"></option>");
                for (BillCode billCode : billCodeList) {
                    boolean selected = billBudget.getBillCode() != null
                            && n(billCode.getBillCode()).equals(n(billBudget.getBillCode().getBillCode()));
                    out.println("<option value=\"" + escapeHtmlAttribute(n(billCode.getBillCode())) + "\""
                            + (selected ? " selected" : "") + ">" + n(billCode.getBillLabel()) + "</option>");
                }
                out.println("</select>");
            }
            out.println("</td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Start Date</th><td class=\"boxed\"><input type=\"text\" name=\"startDate\" value=\""
                            + (billBudget.getStartDate() == null ? ""
                                    : appReq.getWebUser().getDateFormat().format(billBudget.getStartDate()))
                            + "\" size=\"10\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">End Date</th><td class=\"boxed\"><input type=\"text\" name=\"endDate\" value=\""
                            + (billBudget.getEndDate() == null ? ""
                                    : appReq.getWebUser().getDateFormat().format(billBudget.getEndDate()))
                            + "\" size=\"10\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Authorized Hours</th><td class=\"boxed\"><input type=\"text\" name=\"billMins\" value=\""
                            + TimeTracker.formatTime(billBudget.getBillMins()) + "\" size=\"10\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save\"></td></tr>");
            out.println("</table>");
            if (lockBillCode) {
                out.println("<p>Bill code is locked because time entries already reference this budget.</p>");
            }
            out.println("</form>");

            printHtmlFoot(appReq);
        } finally {
            appReq.close();
        }
    }

    private BillBudget resolveBillBudget(HttpServletRequest request, Session dataSession, int workspaceId) {
        String billBudgetId = request.getParameter("billBudgetId");
        if (billBudgetId == null || billBudgetId.trim().equals("")) {
            return null;
        }
        BillBudget billBudget = (BillBudget) dataSession.get(BillBudget.class, Integer.parseInt(billBudgetId));
        if (billBudget == null || billBudget.getWorkspaceId() == null
                || billBudget.getWorkspaceId().intValue() != workspaceId) {
            return null;
        }
        return billBudget;
    }

    private String applyAndValidate(HttpServletRequest request, AppReq appReq, Session dataSession,
            BillBudget billBudget, List<BillCode> billCodeList, int workspaceId) {
        String billBudgetCode = trim(request.getParameter("billBudgetCode"), 100);
        String billCodeValue = trim(request.getParameter("billCode"), 15);
        java.util.Date startDate = appReq.getWebUser().parseDate(request.getParameter("startDate"));
        java.util.Date endDate = appReq.getWebUser().parseDate(request.getParameter("endDate"));

        if (billBudgetCode.trim().length() == 0) {
            return "Budget code is required.";
        }
        if (billCodeValue.trim().length() == 0) {
            return "Bill code is required.";
        }
        if (startDate == null || endDate == null) {
            return "Start and end dates are required and must be valid.";
        }
        if (endDate.before(startDate)) {
            return "End date cannot be before start date.";
        }

        BillCode selectedBillCode = null;
        for (BillCode billCode : billCodeList) {
            if (billCodeValue.equals(billCode.getBillCode())) {
                selectedBillCode = billCode;
                break;
            }
        }
        if (selectedBillCode == null) {
            return "Selected bill code was not found in this workspace.";
        }

        if (isBudgetReferenced(dataSession, billBudget)
                && billBudget.getBillCode() != null
                && !billCodeValue.equals(n(billBudget.getBillCode().getBillCode()))) {
            return "Bill code cannot be changed after time is logged to this budget.";
        }

        int billMins;
        try {
            billMins = TimeTracker.readTime(request.getParameter("billMins"));
        } catch (Exception e) {
            return "Authorized hours are not valid.";
        }
        if (billMins < 0) {
            return "Authorized hours cannot be negative.";
        }

        billBudget.setWorkspaceId(Integer.valueOf(workspaceId));
        billBudget.setBillBudgetCode(billBudgetCode);
        billBudget.setBillCode(selectedBillCode);
        billBudget.setStartDate(startDate);
        billBudget.setEndDate(endDate);
        billBudget.setBillMins(billMins);
        return null;
    }

    private boolean isBudgetReferenced(Session dataSession, BillBudget billBudget) {
        if (billBudget == null || billBudget.getBillBudgetId() == 0) {
            return false;
        }
        Query query = dataSession.createQuery("select count(*) from BillEntry where billBudgetId = :billBudgetId");
        query.setInteger("billBudgetId", billBudget.getBillBudgetId());
        Number count = (Number) query.uniqueResult();
        return count != null && count.intValue() > 0;
    }
}
