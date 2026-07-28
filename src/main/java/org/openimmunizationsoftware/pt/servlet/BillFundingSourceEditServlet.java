package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.BillFundingSource;
import org.openimmunizationsoftware.pt.model.BillFundingSourceType;
import org.openimmunizationsoftware.pt.model.WebUser;

public class BillFundingSourceEditServlet extends ClientServlet {

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
            WebUser webUser = appReq.getWebUser();
            SimpleDateFormat sdf = webUser.getDateFormat();
            String action = appReq.getAction();

            BillFundingSource fundingSource = resolveFundingSource(request, dataSession, activeWorkspaceId.intValue());
            if (fundingSource == null) {
                fundingSource = new BillFundingSource();
                fundingSource.setWorkspaceId(activeWorkspaceId.intValue());
                fundingSource.setVisible("Y");
            }

            if ("Save".equals(action)) {
                String message = applyAndValidate(request, dataSession, webUser, fundingSource,
                        activeWorkspaceId.intValue());
                if (message == null) {
                    Transaction trans = dataSession.beginTransaction();
                    try {
                        dataSession.saveOrUpdate(fundingSource);
                        trans.commit();
                        response.sendRedirect("BillFundingSourcesServlet");
                        return;
                    } catch (Exception e) {
                        trans.rollback();
                        message = "Unable to save funding source: " + e.getMessage();
                    }
                }
                appReq.setMessageProblem(message);
            }

            appReq.setTitle("Funding Source");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();
            printDandelionLocation(out, "Billing / Funding Sources");
            printBillingAdminNav(out, "Funding Sources");

            boolean codeReadonly = isFundingSourceReferenced(dataSession, fundingSource);
            out.println("<form method=\"POST\" action=\"BillFundingSourceEditServlet\">");
            out.println("<input type=\"hidden\" name=\"fundingSourceId\" value=\"" + fundingSource.getFundingSourceId()
                    + "\">");
            out.println("<table class=\"boxed\">");
            out.println("  <tr><th class=\"title\" colspan=\"2\">Edit Funding Source</th></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Code</th><td class=\"boxed\"><input type=\"text\" name=\"fundingSourceCode\" value=\""
                            + escapeHtmlAttribute(n(fundingSource.getFundingSourceCode())) + "\" size=\"30\""
                            + (codeReadonly ? " readonly" : "") + "></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Label</th><td class=\"boxed\"><input type=\"text\" name=\"fundingSourceLabel\" value=\""
                            + escapeHtmlAttribute(n(fundingSource.getFundingSourceLabel()))
                            + "\" size=\"40\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Type</th><td class=\"boxed\"><select name=\"fundingSourceType\">");
            for (BillFundingSourceType type : BillFundingSourceType.values()) {
                String selected = type.getCode().equalsIgnoreCase(n(fundingSource.getFundingSourceType())) ? " selected"
                        : "";
                out.println("<option value=\"" + type.getCode() + "\"" + selected + ">" + type.getCode() + "</option>");
            }
            out.println("</select></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Start Date</th><td class=\"boxed\"><input type=\"text\" name=\"startDate\" value=\""
                            + (fundingSource.getStartDate() == null ? "" : sdf.format(fundingSource.getStartDate()))
                            + "\" size=\"10\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">End Date</th><td class=\"boxed\"><input type=\"text\" name=\"endDate\" value=\""
                            + (fundingSource.getEndDate() == null ? "" : sdf.format(fundingSource.getEndDate()))
                            + "\" size=\"10\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Visible</th><td class=\"boxed\"><input type=\"checkbox\" name=\"visible\" value=\"Y\""
                            + ("Y".equalsIgnoreCase(fundingSource.getVisible()) ? " checked" : "") + "></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save\"></td></tr>");
            out.println("</table>");
            if (codeReadonly) {
                out.println(
                        "<p>Funding source code is locked because it is already referenced by one or more bill codes.</p>");
            }
            out.println("</form>");

            printHtmlFoot(appReq);
        } finally {
            appReq.close();
        }
    }

    private BillFundingSource resolveFundingSource(HttpServletRequest request, Session dataSession, int workspaceId) {
        String idParam = request.getParameter("fundingSourceId");
        if (idParam == null || idParam.trim().equals("")) {
            return null;
        }
        BillFundingSource source = (BillFundingSource) dataSession.get(BillFundingSource.class,
                Integer.parseInt(idParam));
        if (source == null || source.getWorkspaceId() != workspaceId) {
            return null;
        }
        return source;
    }

    private String applyAndValidate(HttpServletRequest request, Session dataSession, WebUser webUser,
            BillFundingSource fundingSource, int workspaceId) {
        String submittedCode = trim(request.getParameter("fundingSourceCode"), 30).trim();
        String submittedLabel = trim(request.getParameter("fundingSourceLabel"), 150).trim();
        String submittedType = trim(request.getParameter("fundingSourceType"), 20).trim();
        java.util.Date startDate = webUser.parseDate(request.getParameter("startDate"));
        java.util.Date endDate = webUser.parseDate(request.getParameter("endDate"));

        if (submittedCode.length() == 0) {
            return "Funding source code is required.";
        }
        if (submittedLabel.length() == 0) {
            return "Funding source label is required.";
        }
        if (BillFundingSourceType.fromCode(submittedType) == null) {
            return "Funding source type is required.";
        }
        if (request.getParameter("startDate") != null && request.getParameter("startDate").trim().length() > 0
                && startDate == null) {
            return "Unable to parse start date.";
        }
        if (request.getParameter("endDate") != null && request.getParameter("endDate").trim().length() > 0
                && endDate == null) {
            return "Unable to parse end date.";
        }
        if (startDate != null && endDate != null && endDate.before(startDate)) {
            return "End date cannot be before start date.";
        }

        Query uniqueQuery = dataSession.createQuery(
                "select count(*) from BillFundingSource where workspaceId = :workspaceId and upper(fundingSourceCode) = :code and fundingSourceId <> :fundingSourceId");
        uniqueQuery.setInteger("workspaceId", workspaceId);
        uniqueQuery.setString("code", submittedCode.toUpperCase());
        uniqueQuery.setInteger("fundingSourceId", fundingSource.getFundingSourceId());
        Number count = (Number) uniqueQuery.uniqueResult();
        if (count != null && count.intValue() > 0) {
            return "Funding source code must be unique in this workspace.";
        }

        if (fundingSource.getFundingSourceId() > 0 && isFundingSourceReferenced(dataSession, fundingSource)
                && !submittedCode.equalsIgnoreCase(n(fundingSource.getFundingSourceCode()))) {
            return "Funding source code cannot be changed after it is referenced by bill codes.";
        }

        fundingSource.setWorkspaceId(workspaceId);
        fundingSource.setFundingSourceCode(submittedCode);
        fundingSource.setFundingSourceLabel(submittedLabel);
        fundingSource.setFundingSourceType(submittedType.toUpperCase());
        fundingSource.setStartDate(startDate);
        fundingSource.setEndDate(endDate);
        fundingSource.setVisible(request.getParameter("visible") == null ? "N" : "Y");
        return null;
    }

    private boolean isFundingSourceReferenced(Session dataSession, BillFundingSource fundingSource) {
        if (fundingSource == null || fundingSource.getFundingSourceId() <= 0) {
            return false;
        }
        Query query = dataSession.createQuery(
                "select count(*) from BillCode where workspaceId = :workspaceId and fundingSourceId = :fundingSourceId");
        query.setInteger("workspaceId", fundingSource.getWorkspaceId());
        query.setInteger("fundingSourceId", fundingSource.getFundingSourceId());
        Number count = (Number) query.uniqueResult();
        return count != null && count.intValue() > 0;
    }
}
