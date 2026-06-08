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
import org.hibernate.exception.ConstraintViolationException;
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

            if ("addMemberByEmail".equals(action)) {
                Integer workspaceId = handleAddMemberByEmail(appReq);
                appReq.setTitle("Dandelion Patches");
                printHtmlHead(appReq);
                renderPage(appReq, workspaceId);
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

        boolean canAdminister = WorkspaceRegistry.canAdministerWorkspace(dataSession, workspaceId.intValue(),
                webUser.getWebUserId());

        out.println("<h2>Patch: " + escapeHtml(selectedWorkspace.getWorkspaceName()) + "</h2>");

        out.println("<a name=\"members\"></a>");
        out.println("<table class=\"boxed\" style=\"margin-bottom:15px;\">");
        out.println("  <tr class=\"boxed\"><th class=\"title\" colspan=\"4\">Members</th></tr>");
        out.println("  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"4\"><strong>Total Members:</strong> "
                + members.size() + "</td></tr>");

        if (canAdminister) {
            out.println("  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"4\">");
            out.println("    <form method=\"POST\" action=\"DandelionPatchServlet#members\">"
                    + "<input type=\"hidden\" name=\"action\" value=\"addMemberByEmail\"/>"
                    + "<input type=\"hidden\" name=\"patchWorkspaceId\" value=\"" + workspaceId + "\"/>"
                    + "<label>Email</label> "
                    + "<input type=\"text\" name=\"memberEmailAddress\" size=\"45\"/> "
                    + "<input type=\"submit\" value=\"Add Member\"/>"
                    + "<div><small>Adds an existing ACTIVE Dandelion user to this patch by email address.</small></div>"
                    + "</form>");
            out.println("  </td></tr>");
        } else {
            out.println(
                    "  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"4\"><small>You have read-only membership access. Only OWNER or ADMIN users can add members.</small></td></tr>");
        }

        out.println(
                "  <tr class=\"boxed\"><th class=\"title\">Email</th><th class=\"title\">Name</th><th class=\"title\">Role</th><th class=\"title\">Added Date</th></tr>");
        if (members.isEmpty()) {
            out.println("  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"4\">No members found.</td></tr>");
        } else {
            for (WorkspaceMember member : members) {
                WebUser memberUser = (WebUser) dataSession.get(WebUser.class, Integer.valueOf(member.getWebUserId()));
                String email = memberUser == null ? "" : valueOrEmpty(memberUser.getEmailAddress());
                String fullName = memberUser == null ? "" : buildDisplayName(memberUser);
                out.println("  <tr class=\"boxed\"><td class=\"boxed\">" + escapeHtml(email)
                        + "</td><td class=\"boxed\">" + escapeHtml(fullName)
                        + "</td><td class=\"boxed\">" + escapeHtml(member.getMemberRole())
                        + "</td><td class=\"boxed\">" + escapeHtml(formatDate(webUser, member.getCreatedDate()))
                        + "</td></tr>");
            }
        }
        out.println("</table>");

        out.println("<table class=\"boxed\" style=\"margin-bottom:15px;\">");
        out.println("  <tr class=\"boxed\"><th class=\"title\" colspan=\"2\">Initial Tags</th></tr>");
        out.println(
                "  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"2\"><small>Default tags seeded for this patch workspace.</small></td></tr>");
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
                out.println("  <tr class=\"boxed\"><td class=\"boxed\">"
                        + escapeHtml(getProjectDisplayName(dataSession, project))
                        + "</td><td class=\"boxed\">"
                        + escapeHtml(loadTagSummaryForProject(dataSession, project.getProjectId()))
                        + "</td></tr>");
            }
        }
        out.println("</table>");
    }

    private Integer handleAddMemberByEmail(AppReq appReq) {
        Session dataSession = appReq.getDataSession();
        WebUser actingUser = appReq.getWebUser();
        Integer workspaceId = parseInteger(appReq.getRequest().getParameter("patchWorkspaceId"));
        String emailAddress = clip(appReq.getRequest().getParameter("memberEmailAddress"), 254).toLowerCase();

        if (workspaceId == null) {
            appReq.setMessageProblem("Patch workspace is required.");
            return null;
        }
        if (emailAddress.length() == 0) {
            appReq.setMessageProblem("Email address is required.");
            return workspaceId;
        }
        if (!WorkspaceRegistry.hasActiveMembership(dataSession, workspaceId.intValue(), actingUser.getWebUserId())) {
            appReq.setMessageProblem("Patch not available.");
            return null;
        }
        if (!WorkspaceRegistry.canAdministerWorkspace(dataSession, workspaceId.intValue(), actingUser.getWebUserId())) {
            appReq.setMessageProblem("Only workspace OWNER or ADMIN can add members.");
            return workspaceId;
        }

        WebUser targetUser = (WebUser) dataSession
                .createQuery(
                        "from WebUser where lower(emailAddress) = :emailAddress and registrationStatus = :registrationStatus")
                .setString("emailAddress", emailAddress)
                .setString("registrationStatus", WebUser.REGISTRATION_STATUS_ACTIVE)
                .uniqueResult();
        if (targetUser == null) {
            appReq.setMessageProblem("No ACTIVE Dandelion user found with that email address.");
            return workspaceId;
        }

        Number existingCount = (Number) dataSession.createQuery(
                "select count(*) from WorkspaceMember where workspaceId = :workspaceId and webUserId = :webUserId and membershipStatus = :membershipStatus")
                .setInteger("workspaceId", workspaceId.intValue())
                .setInteger("webUserId", targetUser.getWebUserId())
                .setString("membershipStatus", WorkspaceMember.STATUS_ACTIVE)
                .uniqueResult();
        if (existingCount != null && existingCount.intValue() > 0) {
            appReq.setMessageProblem("That user is already a member of this patch.");
            return workspaceId;
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            WorkspaceMember workspaceMember = new WorkspaceMember();
            workspaceMember.setWorkspaceId(workspaceId.intValue());
            workspaceMember.setWebUserId(targetUser.getWebUserId());
            workspaceMember.setMemberRole(WorkspaceMember.ROLE_MEMBER);
            workspaceMember.setMembershipStatus(WorkspaceMember.STATUS_ACTIVE);
            workspaceMember.setCreatedDate(new Date());
            dataSession.save(workspaceMember);
            transaction.commit();
            appReq.setMessageConfirmation("Member added: " + targetUser.getEmailAddress());
        } catch (Exception e) {
            transaction.rollback();
            if (isUniqueMembershipViolation(e)) {
                appReq.setMessageProblem("That user is already a member of this patch.");
            } else {
                appReq.setMessageProblem("Unable to add member: " + e.getMessage());
            }
        }
        return workspaceId;
    }

    private boolean isUniqueMembershipViolation(Exception e) {
        Throwable cursor = e;
        while (cursor != null) {
            if (cursor instanceof ConstraintViolationException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private String buildDisplayName(WebUser webUser) {
        String firstName = valueOrEmpty(webUser.getFirstName());
        String lastName = valueOrEmpty(webUser.getLastName());
        String fullName = (firstName + " " + lastName).trim();
        if (fullName.length() > 0) {
            return fullName;
        }
        return valueOrEmpty(webUser.getUsername());
    }

    private String formatDate(WebUser webUser, Date date) {
        if (date == null) {
            return "";
        }
        return webUser.getDateFormat().format(date);
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
