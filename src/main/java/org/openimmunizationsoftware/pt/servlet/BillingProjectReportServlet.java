package org.openimmunizationsoftware.pt.servlet;

import static org.openimmunizationsoftware.pt.util.WebEscaper.escapeHtml;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.billing.BillingProjectReportFormatter;
import org.openimmunizationsoftware.pt.billing.BillingProjectReportFormatter.ProjectReportItem;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.ReviewInterval;
import org.openimmunizationsoftware.pt.model.WebUser;

public class BillingProjectReportServlet extends ClientServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }
            Integer workspaceId = appReq.getActiveWorkspaceId();
            if (workspaceId == null) {
                forwardToHome(request, response);
                return;
            }

            Session dataSession = appReq.getDataSession();
            PrintWriter out = appReq.getOut();
            WebUser webUser = appReq.getWebUser();
            String selectedBillCode = trimToEmpty(request.getParameter("billCode"));
            boolean excludeClosed = shouldExcludeClosed(request.getParameter("submitted"),
                    request.getParameter("excludeClosed"));

            appReq.setTitle("Billing Project Report");
            printHtmlHead(appReq);
            printDandelionLocation(out, "Billing / Project Report");
            printBillingAdminNav(out, "Project Report");
            printStyles(out);

            List<BillCode> billCodes = loadBillCodes(dataSession, workspaceId);
            printFilters(out, billCodes, selectedBillCode, excludeClosed);
            if (selectedBillCode.length() == 0) {
                out.println("<p>Select a bill code to generate a project report.</p>");
            } else {
                List<ProjectReportItem> items = loadReportItems(dataSession, workspaceId, webUser,
                        selectedBillCode, excludeClosed);
                printReport(out, selectedBillCode, items);
            }
            printCopyScript(out);
            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
            if (!response.isCommitted()) {
                appReq.setTitle("Billing Project Report");
                printHtmlHead(appReq);
            }
            appReq.getOut().println("<p class=\"fail\">Unable to load the billing project report: "
                    + escapeHtml(n(e.getMessage())) + "</p>");
            printHtmlFoot(appReq);
        } finally {
            appReq.close();
        }
    }

    static boolean shouldExcludeClosed(String submitted, String excludeClosed) {
        return !"Y".equals(submitted) || excludeClosed != null;
    }

    @SuppressWarnings("unchecked")
    private List<BillCode> loadBillCodes(Session dataSession, Integer workspaceId) {
        Query query = dataSession.createQuery(
                "from BillCode where workspaceId = :workspaceId and visible = 'Y' order by id.billCode");
        query.setParameter("workspaceId", workspaceId);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    private List<ProjectReportItem> loadReportItems(Session dataSession, Integer workspaceId,
            WebUser webUser, String billCode, boolean excludeClosed) {
        String hql = "from Project where workspaceId = :workspaceId and billCode = :billCode";
        if (excludeClosed) {
            hql += " and (projectStatus is null or projectStatus <> :closedStatus)";
        }
        hql += " order by projectName, projectHandle";
        Query query = dataSession.createQuery(hql);
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("billCode", billCode);
        if (excludeClosed) {
            query.setParameter("closedStatus", ProjectStatus.CLOSED.getDatabaseValue());
        }
        List<Project> projects = query.list();
        List<ProjectReportItem> items = new ArrayList<ProjectReportItem>();
        for (Project project : projects) {
            ProjectReportItem item = new ProjectReportItem();
            item.setProject(project);
            item.setTags(loadProjectTags(dataSession, project.getProjectId()));
            item.setUpdateEveryDays(loadUpdateEveryDays(dataSession, project.getProjectId(),
                    webUser.getContactId()));
            items.add(item);
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private List<String> loadProjectTags(Session dataSession, int projectId) {
        Query query = dataSession.createQuery(
                "select pt.tagName from ProjectTagMap ptm, ProjectTag pt "
                        + "where ptm.projectId = :projectId and pt.projectTagId = ptm.projectTagId "
                        + "order by pt.sortOrder, pt.tagName");
        query.setParameter("projectId", projectId);
        return query.list();
    }

    private int loadUpdateEveryDays(Session dataSession, int projectId, int contactId) {
        ProjectContactAssignedId id = new ProjectContactAssignedId();
        id.setProjectId(projectId);
        id.setContactId(contactId);
        ProjectContactAssigned assigned = (ProjectContactAssigned) dataSession.get(ProjectContactAssigned.class, id);
        return assigned == null || assigned.getUpdateDue() == null ? 0 : assigned.getUpdateDue();
    }

    private void printFilters(PrintWriter out, List<BillCode> billCodes, String selectedBillCode,
            boolean excludeClosed) {
        out.println("<form method=\"GET\" action=\"BillingProjectReportServlet\" class=\"bpr-filters\">");
        out.println("  <input type=\"hidden\" name=\"submitted\" value=\"Y\" />");
        out.println("  <label for=\"bprBillCode\"><strong>Bill Code</strong></label>");
        out.println("  <select id=\"bprBillCode\" name=\"billCode\">");
        out.println("    <option value=\"\">Select a bill code</option>");
        for (BillCode billCode : billCodes) {
            String code = n(billCode.getBillCode());
            String label = n(billCode.getBillLabel());
            String selected = code.equals(selectedBillCode) ? " selected" : "";
            out.println("    <option value=\"" + escapeHtmlAttribute(code) + "\"" + selected + ">"
                    + escapeHtml(code + (label.length() == 0 ? "" : " - " + label)) + "</option>");
        }
        out.println("  </select>");
        out.println("  <label><input type=\"checkbox\" name=\"excludeClosed\" value=\"Y\""
                + (excludeClosed ? " checked" : "") + " /> Exclude closed projects</label>");
        out.println("  <button type=\"submit\">Generate Report</button>");
        out.println("</form>");
    }

    private void printReport(PrintWriter out, String billCode, List<ProjectReportItem> items) {
        BillingProjectReportFormatter formatter = new BillingProjectReportFormatter();
        out.println("<div class=\"bpr-heading\"><div><h1>Projects for " + escapeHtml(billCode)
                + "</h1><p>" + items.size() + " project" + (items.size() == 1 ? "" : "s")
                + " found.</p></div>");
        out.println("<button type=\"button\" onclick=\"bprCopy('bprMarkdownAll')\">Copy Full Report</button></div>");
        printMarkdownSource(out, "bprMarkdownAll", formatter.formatReport(billCode, items));
        if (items.isEmpty()) {
            out.println("<p>No projects match this bill code and filter.</p>");
            return;
        }
        int index = 0;
        for (ProjectReportItem item : items) {
            Project project = item.getProject();
            String markdownId = "bprMarkdown" + index++;
            out.println("<section class=\"bpr-project\">");
            out.println("  <div class=\"bpr-project-heading\"><h2>" + escapeHtml(n(project.getProjectName()))
                    + "</h2><button type=\"button\" onclick=\"bprCopy('" + markdownId
                    + "')\">Copy Markdown</button></div>");
            out.println("  <dl class=\"bpr-details\">");
            printDetail(out, "Project Name", project.getProjectName(), false);
            printDetail(out, "Project Handle", project.getProjectHandle(), false);
            printDetail(out, "Tags", join(item.getTags()), false);
            printDetail(out, "Description", project.getDescription(), true);
            printDetail(out, "Current Focus", project.getCurrentFocusText(), true);
            printDetail(out, "Project Outcome", project.getOutcomeText(), true);
            printDetail(out, "Success Criteria", project.getSuccessCriteriaText(), true);
            printDetail(out, "Status", project.getProjectStatus(), false);
            printDetail(out, "Bill Code", project.getBillCode(), false);
            printDetail(out, "Update Every", ReviewInterval.makeLabel(item.getUpdateEveryDays()), false);
            out.println("  </dl>");
            printMarkdownSource(out, markdownId, formatter.formatProject(item));
            out.println("</section>");
        }
    }

    private void printDetail(PrintWriter out, String label, String value, boolean multiline) {
        String display = value == null || value.trim().length() == 0 ? "Not provided" : value.trim();
        out.println("    <dt>" + escapeHtml(label) + "</dt><dd"
                + (multiline ? " class=\"bpr-multiline\"" : "") + ">" + escapeHtml(display) + "</dd>");
    }

    private void printMarkdownSource(PrintWriter out, String id, String markdown) {
        out.println("<textarea id=\"" + id + "\" class=\"bpr-markdown-source\" aria-hidden=\"true\">"
                + escapeHtml(markdown) + "</textarea>");
    }

    private void printStyles(PrintWriter out) {
        out.println("<style>");
        out.println(
                ".bpr-filters{display:flex;align-items:center;gap:12px;flex-wrap:wrap;margin:0 0 24px;padding:14px;border:1px solid #aaa;background:#f7f7f7}.bpr-filters select{min-width:260px;padding:5px}.bpr-heading,.bpr-project-heading{display:flex;justify-content:space-between;align-items:center;gap:16px}.bpr-heading h1,.bpr-project-heading h2{margin-bottom:4px}.bpr-project{max-width:960px;margin:18px 0;padding:18px;border:1px solid #aaa;background:#fff}.bpr-details{display:grid;grid-template-columns:minmax(130px,180px) minmax(0,1fr);margin:16px 0 0}.bpr-details dt,.bpr-details dd{margin:0;padding:8px;border-top:1px solid #ddd}.bpr-details dt{font-weight:bold}.bpr-multiline{white-space:pre-wrap}.bpr-markdown-source{display:none}@media(max-width:650px){.bpr-details{grid-template-columns:1fr}.bpr-details dd{padding-top:0;border-top:0}.bpr-heading,.bpr-project-heading{align-items:flex-start;flex-direction:column}}");
        out.println("</style>");
    }

    private void printCopyScript(PrintWriter out) {
        out.println(
                "<script>function bprCopy(id){var source=document.getElementById(id);if(!source){return;}var text=source.value||'';if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(text).then(function(){alert('Markdown copied.');});return;}source.style.display='block';source.select();try{document.execCommand('copy');alert('Markdown copied.');}finally{source.style.display='none';}}</script>");
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(n(value));
        }
        return result.toString();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

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
}