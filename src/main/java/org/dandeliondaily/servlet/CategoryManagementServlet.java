package org.dandeliondaily.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.Workspace;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class CategoryManagementServlet extends ClientServlet {

    private static final long serialVersionUID = 2804351768823909949L;
    private static final String SESSION_CATEGORY_CONTEXT_WORKSPACE_ID = "categoryMgmtContextWorkspaceId";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            WebUser webUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();
            List<Workspace> patchWorkspaces = WorkspaceRegistry.getPatchWorkspacesForWebUser(dataSession,
                    webUser.getWebUserId());

            String action = request.getParameter("action");
            if ("setContext".equals(action)) {
                handleSetContext(appReq, patchWorkspaces);
                response.sendRedirect("CategoryManagementServlet");
                return;
            }

            Integer personalWorkspaceId = WorkspaceRegistry.getWorkspaceIdForWebUserId(dataSession,
                    webUser.getWebUserId());
            Integer contextWorkspaceId = resolveContextWorkspaceId(appReq, patchWorkspaces);
            Integer activeWorkspaceId = contextWorkspaceId == null ? personalWorkspaceId : contextWorkspaceId;

            if ("saveCategory".equals(action)) {
                handleSaveCategory(appReq, activeWorkspaceId);
                response.sendRedirect("CategoryManagementServlet");
                return;
            }
            if ("deleteCategory".equals(action)) {
                handleDeleteCategory(appReq, activeWorkspaceId);
                response.sendRedirect("CategoryManagementServlet");
                return;
            }

            appReq.setTitle("Categories");
            printHtmlHead(appReq);
            renderPage(appReq, patchWorkspaces, personalWorkspaceId, contextWorkspaceId, activeWorkspaceId);
            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
            appReq.setMessageProblem("Unable to load category management: " + e.getMessage());
        } finally {
            appReq.close();
        }
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

    private void handleSetContext(AppReq appReq, List<Workspace> patchWorkspaces) {
        HttpSession session = appReq.getRequest().getSession(true);
        String patchWorkspaceIdStr = appReq.getRequest().getParameter("patchWorkspaceId");
        if (patchWorkspaceIdStr == null || patchWorkspaceIdStr.trim().length() == 0) {
            session.removeAttribute(SESSION_CATEGORY_CONTEXT_WORKSPACE_ID);
            return;
        }
        Integer patchWorkspaceId = parseInteger(patchWorkspaceIdStr);
        if (patchWorkspaceId == null) {
            session.removeAttribute(SESSION_CATEGORY_CONTEXT_WORKSPACE_ID);
            return;
        }
        for (Workspace workspace : patchWorkspaces) {
            if (workspace.getWorkspaceId() == patchWorkspaceId.intValue()) {
                session.setAttribute(SESSION_CATEGORY_CONTEXT_WORKSPACE_ID, patchWorkspaceId);
                return;
            }
        }
        session.removeAttribute(SESSION_CATEGORY_CONTEXT_WORKSPACE_ID);
    }

    private void handleSaveCategory(AppReq appReq, Integer activeWorkspaceId) {
        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();

        if (activeWorkspaceId == null
                || !WorkspaceRegistry.canAdministerWorkspace(dataSession, activeWorkspaceId.intValue(),
                        webUser.getWebUserId())) {
            appReq.setMessageProblem("You do not have permission to manage categories in this workspace.");
            return;
        }

        Integer projectCategoryId = parseInteger(appReq.getRequest().getParameter("projectCategoryId"));
        String categoryCode = clip(appReq.getRequest().getParameter("categoryCode"), 15);
        String clientName = clip(appReq.getRequest().getParameter("clientName"), 150);
        String clientAcronym = clip(appReq.getRequest().getParameter("clientAcronym"), 15);
        String visible = clip(appReq.getRequest().getParameter("visible"), 1).toUpperCase();
        Integer sortOrder = parseInteger(appReq.getRequest().getParameter("sortOrder"));

        if (categoryCode.length() == 0) {
            appReq.setMessageProblem("Category code is required.");
            return;
        }
        if (clientName.length() == 0) {
            appReq.setMessageProblem("Category name is required.");
            return;
        }
        if (!"Y".equals(visible) && !"N".equals(visible)) {
            visible = "Y";
        }

        Query duplicateQuery = dataSession.createQuery(
                "select count(*) from ProjectCategory where workspaceId = :workspaceId "
                        + "and lower(categoryCode) = :categoryCode and projectCategoryId <> :projectCategoryId");
        duplicateQuery.setInteger("workspaceId", activeWorkspaceId.intValue());
        duplicateQuery.setString("categoryCode", categoryCode.toLowerCase());
        duplicateQuery.setInteger("projectCategoryId", projectCategoryId == null ? -1 : projectCategoryId.intValue());
        Number duplicateCount = (Number) duplicateQuery.uniqueResult();
        if (duplicateCount != null && duplicateCount.intValue() > 0) {
            appReq.setMessageProblem("Category code must be unique in this workspace.");
            return;
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            ProjectCategory category;
            if (projectCategoryId == null) {
                category = new ProjectCategory();
                category.setWorkspaceId(activeWorkspaceId);
            } else {
                category = (ProjectCategory) dataSession.get(ProjectCategory.class, projectCategoryId.intValue());
                if (category == null || category.getWorkspaceId() == null
                        || category.getWorkspaceId().intValue() != activeWorkspaceId.intValue()) {
                    transaction.rollback();
                    appReq.setMessageProblem("Category not found in selected workspace.");
                    return;
                }
            }

            category.setCategoryCode(categoryCode);
            category.setClientName(clientName);
            category.setClientAcronym(clientAcronym);
            category.setVisible(visible);
            category.setSortOrder(sortOrder == null ? Integer.valueOf(10) : sortOrder);
            dataSession.saveOrUpdate(category);

            transaction.commit();
            appReq.setMessageConfirmation("Category saved.");
        } catch (Exception e) {
            transaction.rollback();
            appReq.setMessageProblem("Unable to save category: " + e.getMessage());
        }
    }

    private void handleDeleteCategory(AppReq appReq, Integer activeWorkspaceId) {
        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();

        if (activeWorkspaceId == null
                || !WorkspaceRegistry.canAdministerWorkspace(dataSession, activeWorkspaceId.intValue(),
                        webUser.getWebUserId())) {
            appReq.setMessageProblem("You do not have permission to manage categories in this workspace.");
            return;
        }

        Integer projectCategoryId = parseInteger(appReq.getRequest().getParameter("projectCategoryId"));
        if (projectCategoryId == null) {
            appReq.setMessageProblem("Category id is required.");
            return;
        }

        ProjectCategory category = (ProjectCategory) dataSession.get(ProjectCategory.class,
                projectCategoryId.intValue());
        if (category == null || category.getWorkspaceId() == null
                || category.getWorkspaceId().intValue() != activeWorkspaceId.intValue()) {
            appReq.setMessageProblem("Category not found in selected workspace.");
            return;
        }

        Query usageQuery = dataSession.createQuery(
                "select count(*) from Project where workspaceId = :workspaceId and categoryCode = :categoryCode "
                        + "and (phaseCode is null or phaseCode <> 'Clos')");
        usageQuery.setInteger("workspaceId", activeWorkspaceId.intValue());
        usageQuery.setString("categoryCode", category.getCategoryCode());
        Number usageCount = (Number) usageQuery.uniqueResult();
        if (usageCount != null && usageCount.intValue() > 0) {
            appReq.setMessageProblem("Category is in use by " + usageCount.intValue() + " project(s).");
            return;
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            dataSession.delete(category);
            transaction.commit();
            appReq.setMessageConfirmation("Category deleted.");
        } catch (Exception e) {
            transaction.rollback();
            appReq.setMessageProblem("Unable to delete category: " + e.getMessage());
        }
    }

    private void renderPage(AppReq appReq, List<Workspace> patchWorkspaces, Integer personalWorkspaceId,
            Integer contextWorkspaceId, Integer activeWorkspaceId) {
        PrintWriter out = appReq.getOut();
        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();

        @SuppressWarnings("unchecked")
        List<ProjectCategory> categories = activeWorkspaceId == null ? new ArrayList<ProjectCategory>()
                : dataSession
                        .createQuery(
                                "from ProjectCategory where workspaceId = :workspaceId order by sortOrder, clientName")
                        .setInteger("workspaceId", activeWorkspaceId.intValue())
                        .list();

        Integer selectedCategoryId = parseInteger(appReq.getRequest().getParameter("projectCategoryId"));
        ProjectCategory selectedCategory = null;
        if (selectedCategoryId != null) {
            for (ProjectCategory category : categories) {
                if (category.getProjectCategoryId() == selectedCategoryId.intValue()) {
                    selectedCategory = category;
                    break;
                }
            }
        }
        if (selectedCategory == null && !categories.isEmpty()) {
            selectedCategory = categories.get(0);
        }

        boolean canManage = activeWorkspaceId != null && WorkspaceRegistry.canAdministerWorkspace(dataSession,
                activeWorkspaceId.intValue(), webUser.getWebUserId());

        out.println("<div class=\"main\">");
        out.println("  <div style=\"overflow:auto;\">");
        out.println("    <h1 style=\"float:left;\">Categories</h1>");
        if (!patchWorkspaces.isEmpty()) {
            out.println(
                    "    <form method=\"GET\" action=\"CategoryManagementServlet\" style=\"float:right;margin-top:10px;\">\n"
                            + "      <input type=\"hidden\" name=\"action\" value=\"setContext\"/>\n"
                            + "      <label for=\"cmPatchWorkspaceId\">Dandelion Patch:</label>\n"
                            + "      <select id=\"cmPatchWorkspaceId\" name=\"patchWorkspaceId\" onchange=\"this.form.submit()\">\n"
                            + "        <option value=\"\">Private</option>");
            for (Workspace workspace : patchWorkspaces) {
                boolean selected = contextWorkspaceId != null
                        && contextWorkspaceId.intValue() == workspace.getWorkspaceId();
                out.println("        <option value=\"" + workspace.getWorkspaceId() + "\""
                        + (selected ? " selected" : "") + ">" + escapeHtml(workspace.getWorkspaceName())
                        + "</option>");
            }
            out.println("      </select>\n"
                    + "    </form>");
        }
        out.println("  </div>");

        String contextLabel = contextWorkspaceId == null ? "Private" : "Patch";
        out.println("  <p>Manage categories for <strong>" + contextLabel + "</strong> workspace.</p>");

        out.println("  <table class=\"boxed\" style=\"width:100%;\"><tr class=\"boxed\">"
                + "<th class=\"boxed\" style=\"width:30%;\">Categories</th>"
                + "<th class=\"boxed\" style=\"width:40%;\">Selected Category</th>"
                + "<th class=\"boxed\" style=\"width:30%;\">Add / Delete</th></tr><tr class=\"boxed\">");

        out.println("<td class=\"boxed\" valign=\"top\">");
        if (categories.isEmpty()) {
            out.println("No categories found.");
        } else {
            for (ProjectCategory category : categories) {
                boolean selected = selectedCategory != null
                        && selectedCategory.getProjectCategoryId() == category.getProjectCategoryId();
                out.println((selected ? "<strong>" : "")
                        + "<a href=\"CategoryManagementServlet?projectCategoryId=" + category.getProjectCategoryId()
                        + "\">" + escapeHtml(category.getClientName()) + "</a>"
                        + (selected ? "</strong>" : "")
                        + "<br/><span style=\"font-size:11px;color:#666;\">" + escapeHtml(category.getCategoryCode())
                        + "</span><br/><br/>");
            }
        }
        out.println("</td>");

        out.println("<td class=\"boxed\" valign=\"top\">");
        if (selectedCategory == null) {
            out.println("Select a category to edit.");
        } else {
            out.println("<form method=\"POST\" action=\"CategoryManagementServlet\">\n"
                    + "  <input type=\"hidden\" name=\"action\" value=\"saveCategory\"/>\n"
                    + "  <input type=\"hidden\" name=\"projectCategoryId\" value=\""
                    + selectedCategory.getProjectCategoryId() + "\"/>\n"
                    + "  <div><label>Category Code</label><br/><input type=\"text\" name=\"categoryCode\" size=\"18\" value=\""
                    + escapeHtml(valueOrEmpty(selectedCategory.getCategoryCode())) + "\"/></div>\n"
                    + "  <div style=\"margin-top:6px;\"><label>Name</label><br/><input type=\"text\" name=\"clientName\" size=\"28\" value=\""
                    + escapeHtml(valueOrEmpty(selectedCategory.getClientName())) + "\"/></div>\n"
                    + "  <div style=\"margin-top:6px;\"><label>Acronym</label><br/><input type=\"text\" name=\"clientAcronym\" size=\"18\" value=\""
                    + escapeHtml(valueOrEmpty(selectedCategory.getClientAcronym())) + "\"/></div>\n"
                    + "  <div style=\"margin-top:6px;\"><label>Sort Order</label><br/><input type=\"text\" name=\"sortOrder\" size=\"8\" value=\""
                    + (selectedCategory.getSortOrder() == null ? "" : selectedCategory.getSortOrder().toString())
                    + "\"/></div>\n"
                    + "  <div style=\"margin-top:6px;\"><label>Visible</label><br/><select name=\"visible\">"
                    + "<option value=\"Y\""
                    + ("Y".equalsIgnoreCase(selectedCategory.getVisible()) ? " selected" : "")
                    + ">Y</option><option value=\"N\""
                    + ("N".equalsIgnoreCase(selectedCategory.getVisible()) ? " selected" : "")
                    + ">N</option></select></div>\n"
                    + "  <div style=\"margin-top:10px;\"><input type=\"submit\" value=\"Save Category\""
                    + (canManage ? "" : " disabled") + "/></div>\n"
                    + "</form>");
        }
        out.println("</td>");

        out.println("<td class=\"boxed\" valign=\"top\">");
        out.println("<form method=\"POST\" action=\"CategoryManagementServlet\">\n"
                + "  <input type=\"hidden\" name=\"action\" value=\"saveCategory\"/>\n"
                + "  <div><label>New Category Code</label><br/><input type=\"text\" name=\"categoryCode\" size=\"18\"/></div>\n"
                + "  <div style=\"margin-top:6px;\"><label>Name</label><br/><input type=\"text\" name=\"clientName\" size=\"24\"/></div>\n"
                + "  <div style=\"margin-top:6px;\"><label>Acronym</label><br/><input type=\"text\" name=\"clientAcronym\" size=\"18\"/></div>\n"
                + "  <div style=\"margin-top:6px;\"><label>Sort Order</label><br/><input type=\"text\" name=\"sortOrder\" size=\"8\" value=\"10\"/></div>\n"
                + "  <div style=\"margin-top:6px;\"><label>Visible</label><br/><select name=\"visible\"><option value=\"Y\">Y</option><option value=\"N\">N</option></select></div>\n"
                + "  <div style=\"margin-top:10px;\"><input type=\"submit\" value=\"Add Category\""
                + (canManage ? "" : " disabled") + "/></div>\n"
                + "</form>");

        if (selectedCategory != null) {
            out.println("<hr/>");
            out.println(
                    "<form method=\"POST\" action=\"CategoryManagementServlet\" onsubmit=\"return confirm('Delete this category?');\">\n"
                            + "  <input type=\"hidden\" name=\"action\" value=\"deleteCategory\"/>\n"
                            + "  <input type=\"hidden\" name=\"projectCategoryId\" value=\""
                            + selectedCategory.getProjectCategoryId() + "\"/>\n"
                            + "  <input type=\"submit\" value=\"Delete Selected\""
                            + (canManage ? "" : " disabled") + "/>\n"
                            + "</form>");
        }
        out.println("</td>");

        out.println("</tr></table>");

        if (!canManage) {
            out.println("<p><em>You can view categories in this workspace but cannot modify them.</em></p>");
        }

        out.println("<p><a href=\"HomeServlet\">Back to Home</a></p>");
        out.println("</div>");
    }

    private Integer resolveContextWorkspaceId(AppReq appReq, List<Workspace> patchWorkspaces) {
        HttpSession session = appReq.getRequest().getSession(true);
        Object stored = session.getAttribute(SESSION_CATEGORY_CONTEXT_WORKSPACE_ID);
        if (!(stored instanceof Integer)) {
            return null;
        }
        Integer contextWorkspaceId = (Integer) stored;
        for (Workspace workspace : patchWorkspaces) {
            if (workspace.getWorkspaceId() == contextWorkspaceId.intValue()) {
                return contextWorkspaceId;
            }
        }
        session.removeAttribute(SESSION_CATEGORY_CONTEXT_WORKSPACE_ID);
        return null;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (Exception e) {
            return null;
        }
    }

    private String clip(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '<') {
                escaped.append("&lt;");
            } else if (c == '>') {
                escaped.append("&gt;");
            } else if (c == '&') {
                escaped.append("&amp;");
            } else if (c == '"') {
                escaped.append("&quot;");
            } else if (c == '\'') {
                escaped.append("&#39;");
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
