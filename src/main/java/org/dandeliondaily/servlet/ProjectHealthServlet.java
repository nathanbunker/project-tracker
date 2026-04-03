package org.dandeliondaily.servlet;

import java.io.IOException;
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

            appReq.setTitle("Projects");
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
        String beforeProjectIdStr = appReq.getRequest().getParameter("beforeProjectId");
        if (projectIdStr == null || beforeProjectIdStr == null) {
            sendJson(appReq, false, "Project ids are required", null);
            return;
        }

        int projectId;
        int beforeProjectId;
        try {
            projectId = Integer.parseInt(projectIdStr.trim());
            beforeProjectId = Integer.parseInt(beforeProjectIdStr.trim());
        } catch (NumberFormatException nfe) {
            sendJson(appReq, false, "Invalid project ids", null);
            return;
        }

        String error = pageService.reprioritizeBefore(appReq, projectId, beforeProjectId);
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

    private void sendJson(AppReq appReq, boolean success, String message, Map<String, Object> data) throws Exception {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\":").append(success);
        json.append(",\"message\":\"").append(escapeJson(message)).append("\"");
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                json.append(",\"").append(escapeJson(entry.getKey())).append("\":");
                appendJsonValue(json, entry.getValue());
            }
        }
        json.append("}");

        appReq.getResponse().setContentType("application/json");
        appReq.getResponse().setCharacterEncoding("UTF-8");
        appReq.getOut().print(json.toString());
    }

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
        if (value instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            json.append("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    json.append(",");
                }
                appendJsonValue(json, item);
                first = false;
            }
            json.append("]");
            return;
        }
        if (value instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            json.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(escapeJson(entry.getKey())).append("\":");
                appendJsonValue(json, entry.getValue());
                first = false;
            }
            json.append("}");
            return;
        }
        json.append("\"").append(escapeJson(value.toString())).append("\"");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
