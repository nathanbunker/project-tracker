package org.dandeliondaily.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dandeliondaily.patch.service.PatchSeedImportService;
import org.dandeliondaily.patch.service.PatchSeedImportService.SeedImportException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.ProjectTag;
import org.openimmunizationsoftware.pt.model.ProjectTagMap;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.Workspace;
import org.openimmunizationsoftware.pt.model.WorkspaceMember;
import org.openimmunizationsoftware.pt.servlet.HandleValidationSupport;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class DandelionPatchServlet extends ClientServlet {

    private static final long serialVersionUID = 4947375755365109818L;
    private static final int MAX_SEED_JSON_LEN = 60000;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            String action = request.getParameter("action");
            if ("createPatch".equals(action)) {
                Integer createdWorkspaceId = handleCreatePatch(appReq);
                appReq.setTitle("Dandelion Patches");
                printHtmlHead(appReq);
                renderPage(appReq, createdWorkspaceId);
                printHtmlFoot(appReq);
                return;
            }

            appReq.setTitle("Dandelion Patches");
            printHtmlHead(appReq);
            renderPage(appReq, null);
            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
            appReq.setMessageProblem("Unable to load Dandelion Patches: " + e.getMessage());
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

    private Integer handleCreatePatch(AppReq appReq) throws Exception {
        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();
        String patchName = clip(appReq.getRequest().getParameter("patchName"), 100);
        String patchHandle = HandleValidationSupport.resolveHandle(
                appReq.getRequest().getParameter("patchHandle"), patchName, 60);
        String categoriesRaw = clip(appReq.getRequest().getParameter("categories"), 4000);
        String projectsRaw = clip(appReq.getRequest().getParameter("projects"), 4000);
        String jsonSeedPackage = clip(appReq.getRequest().getParameter("jsonSeedPackage"), MAX_SEED_JSON_LEN);
        boolean hasJsonSeed = jsonSeedPackage.length() > 0;

        if (patchName.length() == 0) {
            appReq.setMessageProblem("Patch name is required.");
            return null;
        }

        if (patchHandle.length() == 0) {
            appReq.setMessageProblem("Patch handle is required for active workspaces.");
            return null;
        }

        String handleMessage = HandleValidationSupport.validateHandleCharacters("Patch handle", patchHandle);
        if (handleMessage != null) {
            appReq.setMessageProblem(handleMessage);
            return null;
        }

        Query duplicatePatchQuery = dataSession.createQuery(
                "select count(*) from Workspace w where w.workspaceStatus = :workspaceStatus and lower(w.workspaceHandle) = :workspaceHandle");
        duplicatePatchQuery.setString("workspaceStatus", Workspace.STATUS_ACTIVE);
        duplicatePatchQuery.setString("workspaceHandle", patchHandle.toLowerCase());
        Number duplicatePatchCount = (Number) duplicatePatchQuery.uniqueResult();
        if (duplicatePatchCount != null && duplicatePatchCount.intValue() > 0) {
            appReq.setMessageProblem("Patch handle must be unique among active workspaces.");
            return null;
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            Workspace workspace = new Workspace();
            workspace.setWorkspaceName(patchName);
            workspace.setWorkspaceHandle(patchHandle);
            workspace.setWorkspaceType(Workspace.TYPE_PATCH);
            workspace.setWorkspaceStatus(Workspace.STATUS_ACTIVE);
            workspace.setCreatedByWebUserId(webUser.getWebUserId());
            workspace.setCreatedDate(new Date());
            dataSession.save(workspace);
            dataSession.flush();

            int workspaceId = workspace.getWorkspaceId();

            WorkspaceMember ownerMembership = new WorkspaceMember();
            ownerMembership.setWorkspaceId(workspaceId);
            ownerMembership.setWebUserId(webUser.getWebUserId());
            ownerMembership.setMemberRole(WorkspaceMember.ROLE_OWNER);
            ownerMembership.setMembershipStatus(WorkspaceMember.STATUS_ACTIVE);
            ownerMembership.setCreatedDate(new Date());
            dataSession.save(ownerMembership);

            if (hasJsonSeed) {
                PatchSeedImportService seedImportService = new PatchSeedImportService();
                seedImportService.importSeedPackage(dataSession, webUser, workspaceId, jsonSeedPackage);
            } else {
                List<String> categoryNames = parseCommaSeparatedNames(categoriesRaw, "General", 100);
                List<String> projectNames = parseCommaSeparatedNames(projectsRaw, "Start", 100);
                createManualSeedData(dataSession, webUser, workspaceId, categoryNames, projectNames);
            }

            transaction.commit();
            appReq.setMessageConfirmation("Dandelion Patch created.");
            return Integer.valueOf(workspaceId);
        } catch (SeedImportException e) {
            transaction.rollback();
            appReq.setMessageProblem(e.getMessage());
            return null;
        } catch (Exception e) {
            transaction.rollback();
            appReq.setMessageProblem("Unable to create patch: " + e.getMessage());
            return null;
        }
    }

    private void renderPage(AppReq appReq, Integer createdWorkspaceId) {
        PrintWriter out = appReq.getOut();
        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();

        List<Workspace> patchWorkspaces = WorkspaceRegistry.getPatchWorkspacesForWebUser(dataSession,
                webUser.getWebUserId());

        out.println("<div class=\"main\">");
        out.println("  <h1>Dandelion Patches</h1>");
        out.println("  <p>Create and manage private invite-only patches.</p>");

        String patchNameValue = valueOrEmpty(appReq.getRequest().getParameter("patchName"));
        String patchHandleValue = valueOrEmpty(appReq.getRequest().getParameter("patchHandle"));
        if (patchHandleValue.length() == 0) {
            patchHandleValue = patchNameValue;
        }
        String categoriesValue = valueOrEmpty(appReq.getRequest().getParameter("categories"));
        String projectsValue = valueOrEmpty(appReq.getRequest().getParameter("projects"));
        String jsonSeedValue = valueOrEmpty(appReq.getRequest().getParameter("jsonSeedPackage"));
        if (categoriesValue.length() == 0) {
            categoriesValue = "General";
        }
        if (projectsValue.length() == 0) {
            projectsValue = "Start";
        }

        out.println("  <table class=\"boxed\" style=\"margin-bottom:15px;\">");
        out.println("    <tr class=\"boxed\"><th class=\"title\" colspan=\"2\">Create Patch</th></tr>");
        out.println("    <tr class=\"boxed\"><td class=\"boxed\" colspan=\"2\">");
        out.println("      <form method=\"POST\" action=\"DandelionPatchServlet\">\n"
                + "        <input type=\"hidden\" name=\"action\" value=\"createPatch\"/>\n"
                + "        <div><label>Patch Name</label><br/><input type=\"text\" name=\"patchName\" size=\"45\" value=\""
                + escapeHtml(patchNameValue)
                + "\"/></div>\n"
                + "        <div style=\"margin-top:8px;\"><label>Patch Handle</label><br/><input type=\"text\" name=\"patchHandle\" size=\"45\" value=\""
                + escapeHtml(patchHandleValue)
                + "\"/></div>\n"
                + "        <div style=\"margin-top:8px;\"><label>Categories (comma separated)</label><br/><input type=\"text\" name=\"categories\" size=\"65\" value=\""
                + escapeHtml(categoriesValue)
                + "\"/></div>\n"
                + "        <div style=\"margin-top:8px;\"><label>Projects (comma separated)</label><br/><input type=\"text\" name=\"projects\" size=\"65\" value=\""
                + escapeHtml(projectsValue)
                + "\"/></div>\n"
                + "        <div style=\"margin-top:8px;\"><label>JSON Seed Package</label><br/><textarea name=\"jsonSeedPackage\" rows=\"12\" cols=\"90\">"
                + escapeHtml(jsonSeedValue)
                + "</textarea><br/><small>If JSON seed content is provided, it overrides Categories and Projects for initial workspace data.</small></div>\n"
                + "        <div style=\"margin-top:10px;\"><input type=\"submit\" value=\"Create Patch\"/></div>\n"
                + "      </form>");
        out.println("    </td></tr>");
        out.println("  </table>");

        out.println("  <table class=\"boxed\" style=\"margin-bottom:15px;\">");
        out.println("    <tr class=\"boxed\"><th class=\"title\" colspan=\"2\">Your Patches</th></tr>");
        if (patchWorkspaces.isEmpty()) {
            out.println(
                    "    <tr class=\"boxed\"><td class=\"boxed\" colspan=\"2\">No patch workspaces available yet.</td></tr>");
        } else {
            for (Workspace workspace : patchWorkspaces) {
                out.println("    <tr class=\"boxed\"><td class=\"boxed\">" + escapeHtml(workspace.getWorkspaceName())
                        + "</td><td class=\"boxed\"><a href=\"DandelionPatchServlet?action=viewPatch&patchWorkspaceId="
                        + workspace.getWorkspaceId() + "\">Open</a></td></tr>");
            }
        }
        out.println("  </table>");

        if (createdWorkspaceId != null) {
            renderPatchDetails(appReq, patchWorkspaces, createdWorkspaceId);
        } else {
            String action = appReq.getRequest().getParameter("action");
            if ("viewPatch".equals(action)) {
                Integer requestedWorkspaceId = parseInteger(appReq.getRequest().getParameter("patchWorkspaceId"));
                renderPatchDetails(appReq, patchWorkspaces, requestedWorkspaceId);
            }
        }

        out.println("  <p><a href=\"HomeServlet\">Back to Home</a></p>");
        out.println("</div>");
    }

    private void renderPatchDetails(AppReq appReq, List<Workspace> patchWorkspaces, Integer workspaceId) {
        PrintWriter out = appReq.getOut();
        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();

        if (workspaceId == null || !WorkspaceRegistry.hasActiveMembership(dataSession, workspaceId.intValue(),
                webUser.getWebUserId())) {
            out.println("<p>Patch not available.</p>");
            return;
        }

        Workspace selectedWorkspace = null;
        for (Workspace workspace : patchWorkspaces) {
            if (workspace.getWorkspaceId() == workspaceId.intValue()) {
                selectedWorkspace = workspace;
                break;
            }
        }
        if (selectedWorkspace == null) {
            out.println("<p>Patch not available.</p>");
            return;
        }

        @SuppressWarnings("unchecked")
        List<ProjectTag> tags = dataSession
                .createQuery(
                        "from ProjectTag where workspaceId = :workspaceId and tagStatus = :tagStatus order by sortOrder, tagName")
                .setInteger("workspaceId", workspaceId.intValue())
                .setString("tagStatus", ProjectTag.STATUS_ACTIVE)
                .list();

        @SuppressWarnings("unchecked")
        List<Project> projects = dataSession
                .createQuery(
                        "from Project where workspaceId = :workspaceId and (projectStatus is null or projectStatus <> :closedStatus) order by priorityLevel desc, projectName")
                .setInteger("workspaceId", workspaceId.intValue())
                .setString("closedStatus", ProjectStatus.CLOSED.getDatabaseValue())
                .list();

        @SuppressWarnings("unchecked")
        List<WorkspaceMember> members = dataSession
                .createQuery(
                        "from WorkspaceMember where workspaceId = :workspaceId and membershipStatus = :status order by createdDate")
                .setInteger("workspaceId", workspaceId.intValue())
                .setString("status", WorkspaceMember.STATUS_ACTIVE)
                .list();

        out.println("<h2>Patch: " + escapeHtml(selectedWorkspace.getWorkspaceName()) + "</h2>");
        out.println("<p><strong>Members:</strong> " + members.size()
                + " &nbsp;&nbsp; <a href=\"DandelionPatchServlet?action=viewPatch&patchWorkspaceId=" + workspaceId
                + "#members\">Manage Members</a> (coming soon)</p>");

        out.println("<table class=\"boxed\" style=\"margin-bottom:15px;\">");
        out.println("  <tr class=\"boxed\"><th class=\"title\" colspan=\"2\">Initial Tags</th></tr>");
        if (tags.isEmpty()) {
            out.println("  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"2\">No tags found.</td></tr>");
        } else {
            for (ProjectTag tag : tags) {
                out.println("  <tr class=\"boxed\"><td class=\"boxed\">" + escapeHtml(tag.getTagName())
                        + "</td><td class=\"boxed\">" + escapeHtml(tag.getTagHandle()) + "</td></tr>");
            }
        }
        out.println("</table>");

        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\"><th class=\"title\" colspan=\"2\">Projects</th></tr>");
        if (projects.isEmpty()) {
            out.println("  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"2\">No projects found.</td></tr>");
        } else {
            for (Project project : projects) {
                out.println("  <tr class=\"boxed\"><td class=\"boxed\">" + escapeHtml(project.getProjectName())
                        + "</td><td class=\"boxed\">"
                        + escapeHtml(loadTagSummaryForProject(dataSession, project.getProjectId()))
                        + "</td></tr>");
            }
        }
        out.println("</table>");

        out.println("<a name=\"members\"></a>");
        out.println(
                "<p style=\"margin-top:15px;\"><strong>Membership/Invitations:</strong> Entry point is ready. Invitation workflow arrives in a later phase.</p>");
    }

    private List<String> parseCommaSeparatedNames(String value, String fallback, int maxLen) {
        Map<String, String> deduped = new LinkedHashMap<String, String>();
        if (value != null) {
            String[] parts = value.split(",");
            for (String part : parts) {
                String trimmed = clip(part, maxLen);
                if (trimmed.length() == 0) {
                    continue;
                }
                String key = trimmed.toLowerCase();
                if (!deduped.containsKey(key)) {
                    deduped.put(key, trimmed);
                }
            }
        }
        if (deduped.isEmpty()) {
            deduped.put(fallback.toLowerCase(), fallback);
        }
        return new ArrayList<String>(deduped.values());
    }

    private void createManualSeedData(Session dataSession, WebUser webUser, int workspaceId,
            List<String> categoryNames, List<String> projectNames) {
        List<Integer> createdTagIds = new ArrayList<Integer>();
        for (int i = 0; i < categoryNames.size(); i++) {
            ProjectTag tag = new ProjectTag();
            tag.setWorkspaceId(workspaceId);
            tag.setTagName(categoryNames.get(i));
            tag.setTagHandle(HandleValidationSupport.resolveHandle("", categoryNames.get(i), 60));
            tag.setTagStatus(ProjectTag.STATUS_ACTIVE);
            tag.setSortOrder(Integer.valueOf((i + 1) * 10));
            tag.setCreatedByWebUserId(webUser.getWebUserId());
            tag.setCreatedDate(new Date());
            dataSession.save(tag);
            dataSession.flush();
            createdTagIds.add(Integer.valueOf(tag.getProjectTagId()));
        }

        Integer defaultTagId = createdTagIds.isEmpty() ? null : createdTagIds.get(0);
        for (String projectName : projectNames) {
            Project project = new Project();
            project.setWorkspaceId(Integer.valueOf(workspaceId));
            project.setProjectName(projectName);
            project.setProjectHandle(HandleValidationSupport.resolveHandle("", projectName, 60));
            project.setProjectStatus(ProjectStatus.ACTIVE.getDatabaseValue());
            project.setPriorityLevel(0);
            project.setBillCode(".");
            project.setCreatedByWebUserId(webUser.getWebUserId());
            project.setLastModifiedByWebUserId(webUser.getWebUserId());
            project.setWebUser(webUser);
            dataSession.save(project);
            dataSession.flush();

            if (defaultTagId != null) {
                ProjectTagMap map = new ProjectTagMap();
                map.setProjectId(project.getProjectId());
                map.setProjectTagId(defaultTagId.intValue());
                map.setCreatedDate(new Date());
                dataSession.save(map);
            }
        }
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

    private String loadTagSummaryForProject(Session dataSession, int projectId) {
        @SuppressWarnings("unchecked")
        List<String> tagNames = dataSession.createQuery(
                "select pt.tagName from ProjectTagMap ptm join ProjectTag pt on pt.projectTagId = ptm.projectTagId where ptm.projectId = :projectId order by pt.sortOrder, pt.tagName")
                .setInteger("projectId", projectId)
                .list();
        if (tagNames == null || tagNames.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String tagName : tagNames) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(tagName);
        }
        return sb.toString();
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
