package org.dandeliondaily.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.dandeliondaily.projecthealth.model.ProjectHealthPageModel;
import org.dandeliondaily.projecthealth.model.ProjectListItemModel;
import org.dandeliondaily.projecthealth.render.ProjectHealthPageRenderer;
import org.dandeliondaily.projecthealth.service.ProjectHealthPageService;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.dandeliondaily.projecthealth.service.ProjectPatchLinkService;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.ProjectPatchLinkDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectPatchLink;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.Workspace;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class ProjectHealthServlet extends ClientServlet {

    private static final long serialVersionUID = 8700180916236040385L;
    private static final String SESSION_PROJECT_HEALTH_CONTEXT_WORKSPACE_ID = "projectHealthContextWorkspaceId";

    private final ProjectHealthPageService pageService = new ProjectHealthPageService();
    private final ProjectHealthPageRenderer pageRenderer = new ProjectHealthPageRenderer();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        Integer originalActiveWorkspaceId = appReq.getActiveWorkspaceId();
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            WebUser webUser = appReq.getWebUser();
            List<Workspace> patchWorkspaces = WorkspaceRegistry.getPatchWorkspacesForWebUser(appReq.getDataSession(),
                    webUser.getWebUserId());
            String action = request.getParameter("action");
            if ("setContext".equals(action)) {
                handleSetContext(appReq, patchWorkspaces);
                action = null;
            }

            Integer contextWorkspaceId = resolveContextWorkspaceId(appReq, patchWorkspaces);
            if (contextWorkspaceId != null) {
                appReq.setActiveWorkspaceId(contextWorkspaceId);
            }
            if ("loadProjectReprioritizeData".equals(action)) {
                handleLoadProjectReprioritizeData(appReq);
                return;
            }
            if ("reprioritizeProject".equals(action)) {
                handleReprioritizeProject(appReq);
                return;
            }
            if ("scheduleProjectReview".equals(action)) {
                handleScheduleProjectReview(appReq);
                return;
            }
            if ("markProjectReviewedNow".equals(action)) {
                handleMarkProjectReviewedNow(appReq);
                return;
            }
            if ("bulkImportActions".equals(action)) {
                handleBulkImportActions(appReq);
                return;
            }
            if ("loadUnscheduledReviewData".equals(action)) {
                handleLoadUnscheduledReviewData(appReq);
                return;
            }
            if ("replaceUnscheduledActions".equals(action)) {
                handleReplaceUnscheduledActions(appReq);
                return;
            }
            if ("addDirectProjectLink".equals(action)) {
                handleAddDirectProjectLink(appReq);
                return;
            }
            if ("addTagLink".equals(action)) {
                handleAddTagLink(appReq);
                return;
            }
            if ("removeProjectPatchLink".equals(action)) {
                handleRemoveProjectPatchLink(appReq);
                return;
            }

            appReq.setTitle("Project Health");
            ProjectHealthPageModel model = pageService.buildModel(appReq, contextWorkspaceId, patchWorkspaces);
            printHtmlHead(appReq);
            pageRenderer.render(appReq, model);
            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.setActiveWorkspaceId(originalActiveWorkspaceId);
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

    private void handleLoadProjectReprioritizeData(AppReq appReq) throws Exception {
        String projectIdStr = appReq.getRequest().getParameter("projectId");
        if (projectIdStr == null || projectIdStr.trim().length() == 0) {
            sendJson(appReq, false, "Project id is required", null);
            return;
        }

        int projectId;
        try {
            projectId = Integer.parseInt(projectIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid project id", null);
            return;
        }

        List<ProjectListItemModel> candidates = pageService.loadReprioritizeCandidates(appReq, projectId);
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (ProjectListItemModel candidate : candidates) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", candidate.getProjectId());
            row.put("name", candidate.getProjectName());
            rows.add(row);
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("projects", rows);
        sendJson(appReq, true, "OK", data);
    }

    private void handleReprioritizeProject(AppReq appReq) throws Exception {
        String projectIdStr = appReq.getRequest().getParameter("projectId");
        String mode = appReq.getRequest().getParameter("moveMode");
        String beforeProjectIdStr = appReq.getRequest().getParameter("beforeProjectId");
        if (projectIdStr == null) {
            sendJson(appReq, false, "Project id is required", null);
            return;
        }

        int projectId;
        Integer beforeProjectId = null;
        try {
            projectId = Integer.parseInt(projectIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid project id", null);
            return;
        }

        if (beforeProjectIdStr != null && beforeProjectIdStr.trim().length() > 0) {
            try {
                beforeProjectId = Integer.valueOf(Integer.parseInt(beforeProjectIdStr.trim()));
            } catch (NumberFormatException nfe) {
                sendJson(appReq, false, "Invalid target project id", null);
                return;
            }
        }

        String error = pageService.reprioritizeProject(appReq, projectId, beforeProjectId, mode);
        if (error != null) {
            sendJson(appReq, false, error, null);
            return;
        }

        sendJson(appReq, true, "Project reprioritized", null);
    }

    private void handleScheduleProjectReview(AppReq appReq) throws Exception {
        String projectIdStr = appReq.getRequest().getParameter("projectId");
        String reviewDate = appReq.getRequest().getParameter("reviewDate");
        if (projectIdStr == null || projectIdStr.trim().length() == 0) {
            sendJson(appReq, false, "Project id is required", null);
            return;
        }

        int projectId;
        try {
            projectId = Integer.parseInt(projectIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid project id", null);
            return;
        }

        java.util.Date parsed = pageService.parseReviewDate(reviewDate);
        if (parsed == null) {
            sendJson(appReq, false, "Review date must be in MM/DD/YYYY format", null);
            return;
        }

        String error = pageService.scheduleProjectReview(appReq, projectId, parsed);
        if (error != null) {
            sendJson(appReq, false, error, null);
            return;
        }

        sendJson(appReq, true, "Project review scheduled", null);
    }

    private void handleMarkProjectReviewedNow(AppReq appReq) throws Exception {
        String projectIdStr = appReq.getRequest().getParameter("projectId");
        if (projectIdStr == null || projectIdStr.trim().length() == 0) {
            sendJson(appReq, false, "Project id is required", null);
            return;
        }
        int projectId;
        try {
            projectId = Integer.parseInt(projectIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid project id", null);
            return;
        }

        String error = pageService.updateLastReviewNow(appReq, projectId);
        if (error != null) {
            sendJson(appReq, false, error, null);
            return;
        }
        sendJson(appReq, true, "Project review timestamp updated", null);
    }

    private void handleBulkImportActions(AppReq appReq) throws Exception {
        String projectIdStr = appReq.getRequest().getParameter("projectId");
        String bulkImportText = appReq.getRequest().getParameter("bulkImportText");
        if (projectIdStr == null || projectIdStr.trim().length() == 0) {
            sendJson(appReq, false, "Project id is required", null);
            return;
        }

        int projectId;
        try {
            projectId = Integer.parseInt(projectIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid project id", null);
            return;
        }

        int importedCount;
        try {
            importedCount = pageService.bulkImportActions(appReq, projectId, bulkImportText);
        } catch (IllegalArgumentException iae) {
            sendJson(appReq, false, iae.getMessage(), null);
            return;
        }

        if (importedCount <= 0) {
            sendJson(appReq, false, "No actions were imported", null);
            return;
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("importedCount", importedCount);
        sendJson(appReq, true, "Imported " + importedCount + " actions", data);
    }

    private void handleLoadUnscheduledReviewData(AppReq appReq) throws Exception {
        try {
            List<ActionNext> actions = pageService.loadUnscheduledReviewActions(appReq);
            Map<Integer, Map<String, Object>> grouped = new LinkedHashMap<Integer, Map<String, Object>>();
            for (ActionNext action : actions) {
                if (action.getProject() == null) {
                    continue;
                }
                int projectId = action.getProject().getProjectId();
                Map<String, Object> projectRow = grouped.get(projectId);
                if (projectRow == null) {
                    projectRow = new LinkedHashMap<String, Object>();
                    projectRow.put("projectId", projectId);
                    projectRow.put("projectName", action.getProject().getProjectName());
                    projectRow.put("actions", new ArrayList<Map<String, Object>>());
                    grouped.put(projectId, projectRow);
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> projectActions = (List<Map<String, Object>>) projectRow.get("actions");
                Map<String, Object> actionRow = new LinkedHashMap<String, Object>();
                actionRow.put("actionId", action.getActionNextId());
                String descriptionHtml;
                try {
                    descriptionHtml = action.getNextDescriptionForDisplay(appReq.getWebUser().getProjectContact());
                } catch (Exception e) {
                    descriptionHtml = escapeHtml(action.getNextDescription());
                }
                actionRow.put("descriptionHtml", descriptionHtml);
                projectActions.add(actionRow);
            }

            List<Map<String, Object>> projectRows = new ArrayList<Map<String, Object>>(grouped.values());
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("projects", projectRows);
            sendJson(appReq, true, "OK", data);
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(appReq, false, "Unable to load unscheduled actions: " + e.getMessage(), null);
        }
    }

    private void handleReplaceUnscheduledActions(AppReq appReq) throws Exception {
        String projectIdStr = appReq.getRequest().getParameter("projectId");
        String[] selectedActionIdsRaw = appReq.getRequest().getParameterValues("selectedActionId");
        String bulkImportText = appReq.getRequest().getParameter("bulkImportText");

        if (projectIdStr == null || projectIdStr.trim().length() == 0) {
            sendJson(appReq, false, "Project id is required", null);
            return;
        }
        int projectId;
        try {
            projectId = Integer.parseInt(projectIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid project id", null);
            return;
        }

        List<Integer> selectedActionIds = new ArrayList<Integer>();
        if (selectedActionIdsRaw != null) {
            for (String idRaw : selectedActionIdsRaw) {
                if (idRaw == null || idRaw.trim().length() == 0) {
                    continue;
                }
                try {
                    selectedActionIds.add(Integer.parseInt(idRaw.trim()));
                } catch (NumberFormatException nfe) {
                    // skip invalid id
                }
            }
        }

        ProjectHealthPageService.ReplaceUnscheduledResult result;
        try {
            result = pageService.replaceUnscheduledActions(appReq, projectId, selectedActionIds, bulkImportText);
        } catch (IllegalArgumentException iae) {
            sendJson(appReq, false, iae.getMessage(), null);
            return;
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("cancelledCount", result.getCancelledCount());
        data.put("importedCount", result.getImportedCount());
        sendJson(appReq, true,
                "Cancelled " + result.getCancelledCount() + " actions and imported " + result.getImportedCount()
                        + " actions",
                data);
    }

    private void handleAddDirectProjectLink(AppReq appReq) throws Exception {
        String projectIdStr = appReq.getRequest().getParameter("projectId");
        String patchProjectIdStr = appReq.getRequest().getParameter("patchProjectId");
        if (projectIdStr == null || projectIdStr.trim().length() == 0) {
            sendJson(appReq, false, "Project id is required", null);
            return;
        }
        if (patchProjectIdStr == null || patchProjectIdStr.trim().length() == 0) {
            sendJson(appReq, false, "Patch project is required", null);
            return;
        }
        int projectId;
        int patchProjectId;
        try {
            projectId = Integer.parseInt(projectIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid project id", null);
            return;
        }
        try {
            patchProjectId = Integer.parseInt(patchProjectIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid patch project id", null);
            return;
        }
        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();
        Project project = (Project) dataSession.get(Project.class, projectId);
        if (project == null || !Integer.valueOf(appReq.getActiveWorkspaceId()).equals(project.getWorkspaceId())) {
            sendJson(appReq, false, "Project not found", null);
            return;
        }
        if (project.getLinkedPatchWorkspaceId() == null) {
            sendJson(appReq, false, "Project has no linked patch workspace", null);
            return;
        }
        int linkedPatchWorkspaceId = project.getLinkedPatchWorkspaceId().intValue();
        ProjectPatchLinkDao dao = new ProjectPatchLinkDao(dataSession);
        if (dao.directLinkExists(projectId, patchProjectId)) {
            sendJson(appReq, false, "Link already exists", null);
            return;
        }
        ProjectPatchLinkService patchLinkService = new ProjectPatchLinkService();
        String error = patchLinkService.validateDirectLink(dataSession, patchProjectId, linkedPatchWorkspaceId);
        if (error != null) {
            sendJson(appReq, false, error, null);
            return;
        }
        Transaction transaction = dataSession.beginTransaction();
        try {
            ProjectPatchLink link = new ProjectPatchLink();
            link.setPrivateProjectId(projectId);
            link.setPatchWorkspaceId(linkedPatchWorkspaceId);
            link.setLinkType(ProjectPatchLink.LINK_TYPE_DIRECT_PROJECT);
            link.setLinkedPatchProjectId(patchProjectId);
            link.setCreatedByWebUserId(webUser.getWebUserId());
            link.setCreatedDate(new java.util.Date());
            dao.save(link);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            sendJson(appReq, false, "Unable to add link: " + e.getMessage(), null);
            return;
        }
        sendJson(appReq, true, "Link added", null);
    }

    private void handleAddTagLink(AppReq appReq) throws Exception {
        String projectIdStr = appReq.getRequest().getParameter("projectId");
        String patchTagIdStr = appReq.getRequest().getParameter("patchTagId");
        if (projectIdStr == null || projectIdStr.trim().length() == 0) {
            sendJson(appReq, false, "Project id is required", null);
            return;
        }
        if (patchTagIdStr == null || patchTagIdStr.trim().length() == 0) {
            sendJson(appReq, false, "Patch tag is required", null);
            return;
        }
        int projectId;
        int patchTagId;
        try {
            projectId = Integer.parseInt(projectIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid project id", null);
            return;
        }
        try {
            patchTagId = Integer.parseInt(patchTagIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid patch tag id", null);
            return;
        }
        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();
        Project project = (Project) dataSession.get(Project.class, projectId);
        if (project == null || !Integer.valueOf(appReq.getActiveWorkspaceId()).equals(project.getWorkspaceId())) {
            sendJson(appReq, false, "Project not found", null);
            return;
        }
        if (project.getLinkedPatchWorkspaceId() == null) {
            sendJson(appReq, false, "Project has no linked patch workspace", null);
            return;
        }
        int linkedPatchWorkspaceId = project.getLinkedPatchWorkspaceId().intValue();
        ProjectPatchLinkDao dao = new ProjectPatchLinkDao(dataSession);
        if (dao.tagLinkExists(projectId, patchTagId)) {
            sendJson(appReq, false, "Link already exists", null);
            return;
        }
        ProjectPatchLinkService patchLinkService = new ProjectPatchLinkService();
        String error = patchLinkService.validateTagLink(dataSession, patchTagId, linkedPatchWorkspaceId);
        if (error != null) {
            sendJson(appReq, false, error, null);
            return;
        }
        Transaction transaction = dataSession.beginTransaction();
        try {
            ProjectPatchLink link = new ProjectPatchLink();
            link.setPrivateProjectId(projectId);
            link.setPatchWorkspaceId(linkedPatchWorkspaceId);
            link.setLinkType(ProjectPatchLink.LINK_TYPE_PATCH_TAG);
            link.setLinkedPatchTagId(patchTagId);
            link.setCreatedByWebUserId(webUser.getWebUserId());
            link.setCreatedDate(new java.util.Date());
            dao.save(link);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            sendJson(appReq, false, "Unable to add link: " + e.getMessage(), null);
            return;
        }
        sendJson(appReq, true, "Link added", null);
    }

    private void handleRemoveProjectPatchLink(AppReq appReq) throws Exception {
        String linkIdStr = appReq.getRequest().getParameter("projectPatchLinkId");
        String projectIdStr = appReq.getRequest().getParameter("projectId");
        if (linkIdStr == null || linkIdStr.trim().length() == 0) {
            sendJson(appReq, false, "Link id is required", null);
            return;
        }
        if (projectIdStr == null || projectIdStr.trim().length() == 0) {
            sendJson(appReq, false, "Project id is required", null);
            return;
        }
        int linkId;
        int projectId;
        try {
            linkId = Integer.parseInt(linkIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid link id", null);
            return;
        }
        try {
            projectId = Integer.parseInt(projectIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid project id", null);
            return;
        }
        Session dataSession = appReq.getDataSession();
        Project project = (Project) dataSession.get(Project.class, projectId);
        if (project == null || !Integer.valueOf(appReq.getActiveWorkspaceId()).equals(project.getWorkspaceId())) {
            sendJson(appReq, false, "Project not found", null);
            return;
        }
        ProjectPatchLinkDao dao = new ProjectPatchLinkDao(dataSession);
        ProjectPatchLink link = dao.getById(linkId);
        if (link == null || link.getPrivateProjectId() != projectId) {
            sendJson(appReq, false, "Link not found", null);
            return;
        }
        Transaction transaction = dataSession.beginTransaction();
        try {
            dao.delete(link);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            sendJson(appReq, false, "Unable to remove link: " + e.getMessage(), null);
            return;
        }
        sendJson(appReq, true, "Link removed", null);
    }

    private void sendJson(AppReq appReq, boolean success, String message, Map<String, Object> data) throws Exception {
        appReq.getResponse().setContentType("application/json; charset=UTF-8");
        PrintWriter out = appReq.getResponse().getWriter();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\":").append(success).append(",");
        json.append("\"message\":\"").append(escapeJson(message)).append("\"");
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                json.append(",\"").append(escapeJson(entry.getKey())).append("\":");
                appendJsonValue(json, entry.getValue());
            }
        }
        json.append("}");

        out.println(json.toString());
        out.flush();
    }

    @SuppressWarnings("unchecked")
    private void appendJsonValue(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
            return;
        }
        if (value instanceof String) {
            json.append("\"").append(escapeJson((String) value)).append("\"");
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            json.append(value.toString());
            return;
        }
        if (value instanceof Map<?, ?>) {
            Map<Object, Object> map = (Map<Object, Object>) value;
            json.append("{");
            boolean first = true;
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                appendJsonValue(json, entry.getValue());
                first = false;
            }
            json.append("}");
            return;
        }
        if (value instanceof Iterable<?>) {
            json.append("[");
            boolean first = true;
            for (Object item : (Iterable<Object>) value) {
                if (!first) {
                    json.append(",");
                }
                appendJsonValue(json, item);
                first = false;
            }
            json.append("]");
            return;
        }
        if (value.getClass().isArray()) {
            json.append("[");
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    json.append(",");
                }
                appendJsonValue(json, java.lang.reflect.Array.get(value, i));
            }
            json.append("]");
            return;
        }
        json.append("\"").append(escapeJson(value.toString())).append("\"");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                    break;
            }
        }
        return escaped.toString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&#39;");
                    break;
                default:
                    escaped.append(ch);
                    break;
            }
        }
        return escaped.toString();
    }

    private void handleSetContext(AppReq appReq, List<Workspace> patchWorkspaces) {
        HttpSession session = appReq.getRequest().getSession(true);
        String patchWorkspaceIdStr = appReq.getRequest().getParameter("patchWorkspaceId");
        if (patchWorkspaceIdStr == null || patchWorkspaceIdStr.trim().length() == 0) {
            session.removeAttribute(SESSION_PROJECT_HEALTH_CONTEXT_WORKSPACE_ID);
            return;
        }
        Integer patchWorkspaceId = parseInteger(patchWorkspaceIdStr);
        if (patchWorkspaceId == null) {
            session.removeAttribute(SESSION_PROJECT_HEALTH_CONTEXT_WORKSPACE_ID);
            return;
        }
        for (Workspace workspace : patchWorkspaces) {
            if (workspace.getWorkspaceId() == patchWorkspaceId.intValue()) {
                session.setAttribute(SESSION_PROJECT_HEALTH_CONTEXT_WORKSPACE_ID, patchWorkspaceId);
                return;
            }
        }
        session.removeAttribute(SESSION_PROJECT_HEALTH_CONTEXT_WORKSPACE_ID);
    }

    private Integer resolveContextWorkspaceId(AppReq appReq, List<Workspace> patchWorkspaces) {
        HttpSession session = appReq.getRequest().getSession(true);
        Object stored = session.getAttribute(SESSION_PROJECT_HEALTH_CONTEXT_WORKSPACE_ID);
        if (!(stored instanceof Integer)) {
            return null;
        }
        Integer contextWorkspaceId = (Integer) stored;
        for (Workspace workspace : patchWorkspaces) {
            if (workspace.getWorkspaceId() == contextWorkspaceId.intValue()) {
                return contextWorkspaceId;
            }
        }
        session.removeAttribute(SESSION_PROJECT_HEALTH_CONTEXT_WORKSPACE_ID);
        return null;
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (Exception e) {
            return null;
        }
    }
}
