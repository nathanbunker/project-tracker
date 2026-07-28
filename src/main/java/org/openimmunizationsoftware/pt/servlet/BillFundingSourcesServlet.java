package org.openimmunizationsoftware.pt.servlet;

import static org.openimmunizationsoftware.pt.util.WebEscaper.escapeHtml;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.BillFundingSource;
import org.openimmunizationsoftware.pt.model.WebUser;

public class BillFundingSourcesServlet extends ClientServlet {

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

            WebUser webUser = appReq.getWebUser();
            SimpleDateFormat sdf = webUser.getDateFormat();
            Session dataSession = appReq.getDataSession();
            Query query = dataSession.createQuery(
                    "from BillFundingSource where workspaceId = :workspaceId order by fundingSourceCode");
            query.setParameter("workspaceId", activeWorkspaceId);
            @SuppressWarnings("unchecked")
            List<BillFundingSource> sourceList = query.list();

            appReq.setTitle("Funding Sources");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();
            printDandelionLocation(out, "Billing / Funding Sources");
            printBillingAdminNav(out, "Funding Sources");

            out.println("<table class=\"boxed\">");
            out.println("  <tr><th class=\"title\" colspan=\"7\">Funding Sources</th></tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Code</th>");
            out.println("    <th class=\"boxed\">Label</th>");
            out.println("    <th class=\"boxed\">Type</th>");
            out.println("    <th class=\"boxed\">Start Date</th>");
            out.println("    <th class=\"boxed\">End Date</th>");
            out.println("    <th class=\"boxed\">Visible</th>");
            out.println("    <th class=\"boxed\">Edit</th>");
            out.println("  </tr>");
            if (sourceList.isEmpty()) {
                out.println(
                        "  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"7\">No funding sources defined.</td></tr>");
            }
            for (BillFundingSource source : sourceList) {
                out.println("  <tr class=\"boxed\">");
                out.println("    <td class=\"boxed\">" + escapeHtml(n(source.getFundingSourceCode())) + "</td>");
                out.println("    <td class=\"boxed\">" + escapeHtml(n(source.getFundingSourceLabel())) + "</td>");
                out.println("    <td class=\"boxed\">" + escapeHtml(n(source.getFundingSourceType())) + "</td>");
                out.println("    <td class=\"boxed\">" + formatDate(sdf, source.getStartDate()) + "</td>");
                out.println("    <td class=\"boxed\">" + formatDate(sdf, source.getEndDate()) + "</td>");
                out.println("    <td class=\"boxed\">" + escapeHtml(n(source.getVisible())) + "</td>");
                out.println("    <td class=\"boxed\"><a href=\"BillFundingSourceEditServlet?fundingSourceId="
                        + source.getFundingSourceId() + "\">Edit</a></td>");
                out.println("  </tr>");
            }
            out.println("</table>");
            out.println("<p><a href=\"BillFundingSourceEditServlet\">Create a New Funding Source</a></p>");

            printHtmlFoot(appReq);
        } finally {
            appReq.close();
        }
    }

    private String formatDate(SimpleDateFormat sdf, java.util.Date value) {
        return value == null ? "" : sdf.format(value);
    }
}
