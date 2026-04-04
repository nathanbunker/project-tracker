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

import org.dandeliondaily.projecthealth.model.ProjectHealthPageModel;
import org.dandeliondaily.projecthealth.model.ProjectListItemModel;
import org.dandeliondaily.projecthealth.render.ProjectHealthPageRenderer;
import org.dandeliondaily.projecthealth.service.ProjectHealthPageService;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class ProjectHealthServlet extends ClientServlet {

    private static final long serialVersionUID = 8700180916236040385L;

    private final ProjectHealthPageService pageService = new ProjectHealthPageService();
    private final ProjectHealthPageRenderer pageRenderer = new ProjectHealthPageRenderer();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            String action = request.getParameter("action");
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

            appReq.setTitle("Project Health");
            ProjectHealthPageModel model = pageService.buildModel(appReq);
            printHtmlHead(appReq);
            pageRenderer.render(appReq, model);
            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
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
            List<ProjectActionNext> actions = pageService.loadUnscheduledReviewActions(appReq);
            Map<Integer, Map<String, Object>> grouped = new LinkedHashMap<Integer, Map<String, Object>>();
            for (ProjectActionNext action : actions) {
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
}
