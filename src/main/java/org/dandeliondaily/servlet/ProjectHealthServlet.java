package org.dandeliondaily.servlet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.dandeliondaily.projecthealth.model.ProjectHealthPageModel;
import org.dandeliondaily.projecthealth.model.ProjectListItemModel;
import org.dandeliondaily.projecthealth.render.ProjectHealthPageRenderer;
import org.dandeliondaily.projecthealth.service.ProjectFactValueService;
import org.dandeliondaily.projecthealth.service.ProjectHealthPageService;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.util.WebEscaper;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.dandeliondaily.projecthealth.service.ProjectPatchLinkService;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.ProjectFactDefinitionDao;
import org.openimmunizationsoftware.pt.doa.ProjectPatchLinkDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectFactDefinition;
import org.openimmunizationsoftware.pt.model.ProjectFactValue;
import org.openimmunizationsoftware.pt.model.ProjectPatchLink;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.Workspace;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

@MultipartConfig
public class ProjectHealthServlet extends ClientServlet {

    private static final long serialVersionUID = 8700180916236040385L;
    private static final String SESSION_PROJECT_HEALTH_CONTEXT_WORKSPACE_ID = "projectHealthContextWorkspaceId";

    private final ProjectHealthPageService pageService = new ProjectHealthPageService();
    private final ProjectFactValueService projectFactValueService = new ProjectFactValueService();
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
            if ("linkPrivateProjectToSharedProject".equals(action)) {
                handleLinkPrivateProjectToSharedProject(appReq,
                        normalizePatchTagKey(request.getParameter("patchTag")));
                return;
            }
            if ("quickCapture".equals(action)) {
                handleQuickCapture(appReq, contextWorkspaceId, normalizePatchTagKey(request.getParameter("patchTag")));
                return;
            }
            if ("saveSharedOpenActionEdit".equals(action)) {
                handleSaveSharedOpenActionEdit(appReq, contextWorkspaceId,
                        normalizePatchTagKey(request.getParameter("patchTag")));
                return;
            }
            if ("adoptSharedOpenAction".equals(action)) {
                handleAdoptSharedOpenAction(appReq, contextWorkspaceId,
                        normalizePatchTagKey(request.getParameter("patchTag")));
                return;
            }
            if ("cancelSharedOpenAction".equals(action)) {
                handleCancelSharedOpenAction(appReq, contextWorkspaceId,
                        normalizePatchTagKey(request.getParameter("patchTag")));
                return;
            }
            if ("saveProjectDefinitionField".equals(action)) {
                handleSaveProjectDefinitionField(appReq, normalizePatchTagKey(request.getParameter("patchTag")));
                return;
            }
            if ("toggleSharedProjectFact".equals(action)) {
                handleToggleSharedProjectFact(appReq, contextWorkspaceId,
                        normalizePatchTagKey(request.getParameter("patchTag")));
                return;
            }
            if ("saveFactDefinition".equals(action)) {
                handleSaveFactDefinition(appReq);
                return;
            }
            if ("downloadFactsCsv".equals(action)) {
                handleDownloadFactsCsv(appReq);
                return;
            }
            if ("uploadFactsCsv".equals(action)) {
                handleUploadFactsCsv(appReq);
                return;
            }
            if ("deactivateFactDefinition".equals(action)) {
                handleDeactivateFactDefinition(appReq);
                return;
            }

            appReq.setTitle("Project Health");
            String selectedPatchTagKey = normalizePatchTagKey(request.getParameter("patchTag"));
            ProjectHealthPageModel model = pageService.buildModel(appReq, contextWorkspaceId, patchWorkspaces,
                    selectedPatchTagKey);
            applyQuickCaptureFlashMessages(appReq, request);
            boolean factsMode = "editFacts".equals(action);
            if (factsMode) {
                Integer selectedFactDefinitionId = parseInteger(request.getParameter("factDefinitionId"));
                String selectedFactGroup = normalizeFactGroup(request.getParameter("factGroup"));
                model.setFactsMode(true);
                model.setFactsMessage(safeText(request.getParameter("factsMessage")));
                model.setFactsMessageError("Y".equalsIgnoreCase(request.getParameter("factsError")));
                pageService.populateFactDefinitions(model, appReq, selectedFactDefinitionId, selectedFactGroup);
            }
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
                    projectRow.put("projectName", getProjectDisplayName(appReq.getDataSession(), action.getProject()));
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

    private void handleSaveFactDefinition(AppReq appReq) throws Exception {
        Integer workspaceId = appReq.getActiveWorkspaceId();
        if (workspaceId == null) {
            redirectToFacts(appReq, "Workspace is required", true, null, null);
            return;
        }

        Integer factDefinitionId = parseInteger(appReq.getRequest().getParameter("factDefinitionId"));
        String factGroup = normalizeFactGroup(appReq.getRequest().getParameter("factGroup"));
        String factCode = normalizeFactCode(appReq.getRequest().getParameter("factCode"));
        String factLabel = clip(appReq.getRequest().getParameter("factLabel"), 200);
        String factDescription = clipAllowNull(appReq.getRequest().getParameter("factDescription"), 1200);
        String factInputType = normalizeFactInputType(appReq.getRequest().getParameter("factInputType"));
        Integer displayOrder = parseInteger(appReq.getRequest().getParameter("displayOrder"));
        String active = normalizeActive(appReq.getRequest().getParameter("active"));

        if (factGroup.length() == 0) {
            redirectToFacts(appReq, "Fact group is required", true, factDefinitionId, null);
            return;
        }
        if (factCode.length() == 0) {
            redirectToFacts(appReq, "Fact code is required", true, factDefinitionId, factGroup);
            return;
        }
        if (factLabel.length() == 0) {
            redirectToFacts(appReq, "Fact label is required", true, factDefinitionId, factGroup);
            return;
        }
        if (factInputType == null) {
            redirectToFacts(appReq, "Fact input type is invalid", true, factDefinitionId, factGroup);
            return;
        }

        Session dataSession = appReq.getDataSession();
        ProjectFactDefinitionDao dao = new ProjectFactDefinitionDao(dataSession);
        ProjectFactDefinition factDefinition;
        boolean creating = factDefinitionId == null || factDefinitionId.intValue() <= 0;
        if (creating) {
            factDefinition = new ProjectFactDefinition();
            factDefinition.setWorkspaceId(workspaceId.intValue());
            factDefinition.setCreatedByWebUserId(Integer.valueOf(appReq.getWebUser().getWebUserId()));
            factDefinition.setCreatedDate(new java.util.Date());
            if (displayOrder == null) {
                displayOrder = Integer.valueOf(dao.nextDisplayOrderForGroup(workspaceId.intValue(), factGroup));
            }
        } else {
            factDefinition = dao.getById(factDefinitionId.intValue());
            if (factDefinition == null || factDefinition.getWorkspaceId() != workspaceId.intValue()) {
                redirectToFacts(appReq, "Fact definition was not found", true, null, null);
                return;
            }
            if (!factCode.equalsIgnoreCase(safeText(factDefinition.getFactCode()))) {
                redirectToFacts(appReq, "Fact code is stable and cannot be changed", true,
                        Integer.valueOf(factDefinition.getProjectFactDefinitionId()), factGroup);
                return;
            }
            if (displayOrder == null) {
                displayOrder = Integer.valueOf(factDefinition.getDisplayOrder());
            }
        }

        if (dao.existsByWorkspaceAndFactCodeIgnoreCase(workspaceId.intValue(), factCode,
                creating ? null : Integer.valueOf(factDefinition.getProjectFactDefinitionId()))) {
            redirectToFacts(appReq, "Fact code must be unique in this workspace", true,
                    creating ? null : Integer.valueOf(factDefinition.getProjectFactDefinitionId()), factGroup);
            return;
        }

        factDefinition.setFactGroup(factGroup);
        factDefinition.setFactCode(factCode);
        factDefinition.setFactLabel(factLabel);
        factDefinition.setFactDescription(factDescription);
        factDefinition.setFactInputType(factInputType);
        factDefinition.setDisplayOrder(displayOrder == null ? 0 : displayOrder.intValue());
        factDefinition.setActive(active);
        factDefinition.setLastModifiedByWebUserId(Integer.valueOf(appReq.getWebUser().getWebUserId()));
        factDefinition.setLastModifiedDate(new java.util.Date());

        Transaction transaction = dataSession.beginTransaction();
        try {
            if (creating) {
                dao.save(factDefinition);
            } else {
                dao.update(factDefinition);
            }
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            redirectToFacts(appReq, "Unable to save fact definition: " + e.getMessage(), true,
                    creating ? null : Integer.valueOf(factDefinition.getProjectFactDefinitionId()), factGroup);
            return;
        }

        redirectToFacts(appReq, "Fact definition saved", false,
                Integer.valueOf(factDefinition.getProjectFactDefinitionId()), factGroup);
    }

    private void handleDownloadFactsCsv(AppReq appReq) throws Exception {
        Integer workspaceId = appReq.getActiveWorkspaceId();
        if (workspaceId == null) {
            redirectToFacts(appReq, "Workspace is required", true, null, null);
            return;
        }

        ProjectFactDefinitionDao dao = new ProjectFactDefinitionDao(appReq.getDataSession());
        List<ProjectFactDefinition> facts = dao.listByWorkspaceId(workspaceId.intValue(), true);

        HttpServletResponse response = appReq.getResponse();
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=workspace-" + workspaceId.intValue() + "-facts.csv");

        PrintWriter out = response.getWriter();
        out.println("fact_group,fact_code,fact_label,fact_description,fact_input_type,display_order,active");
        for (ProjectFactDefinition fact : facts) {
            out.println(csvCell(fact.getFactGroup())
                    + "," + csvCell(fact.getFactCode())
                    + "," + csvCell(fact.getFactLabel())
                    + "," + csvCell(fact.getFactDescription())
                    + "," + csvCell(fact.getFactInputType())
                    + "," + fact.getDisplayOrder()
                    + "," + csvCell(fact.getActive()));
        }
        out.flush();
    }

    private void handleUploadFactsCsv(AppReq appReq) throws Exception {
        Integer workspaceId = appReq.getActiveWorkspaceId();
        if (workspaceId == null) {
            redirectToFacts(appReq, "Workspace is required", true, null, null);
            return;
        }

        Part csvFilePart = null;
        try {
            csvFilePart = appReq.getRequest().getPart("factsCsvFile");
        } catch (Exception e) {
            redirectToFacts(appReq, "Unable to read uploaded CSV file", true, null, null);
            return;
        }
        if (csvFilePart == null || csvFilePart.getSize() <= 0) {
            redirectToFacts(appReq, "Please choose a CSV file to upload", true, null, null);
            return;
        }

        String csvText = readPartUtf8(csvFilePart);
        List<List<String>> records = parseCsvRecords(csvText);
        if (records.isEmpty()) {
            redirectToFacts(appReq, "CSV file is empty", true, null, null);
            return;
        }

        int headerRowIndex = -1;
        for (int i = 0; i < records.size(); i++) {
            if (!isBlankCsvRow(records.get(i))) {
                headerRowIndex = i;
                break;
            }
        }
        if (headerRowIndex < 0) {
            redirectToFacts(appReq, "CSV file is empty", true, null, null);
            return;
        }

        List<String> headerRow = records.get(headerRowIndex);
        Map<String, Integer> headerMap = new HashMap<String, Integer>();
        for (int i = 0; i < headerRow.size(); i++) {
            String normalizedHeader = normalizeHeaderName(headerRow.get(i));
            if (normalizedHeader.length() > 0) {
                headerMap.put(normalizedHeader, Integer.valueOf(i));
            }
        }

        List<String> missingHeaders = new ArrayList<String>();
        if (!headerMap.containsKey("fact_group")) {
            missingHeaders.add("fact_group");
        }
        if (!headerMap.containsKey("fact_code")) {
            missingHeaders.add("fact_code");
        }
        if (!headerMap.containsKey("fact_label")) {
            missingHeaders.add("fact_label");
        }
        if (!missingHeaders.isEmpty()) {
            redirectToFacts(appReq,
                    "Missing required CSV header(s): " + joinWithComma(missingHeaders),
                    true, null, null);
            return;
        }

        Session dataSession = appReq.getDataSession();
        ProjectFactDefinitionDao dao = new ProjectFactDefinitionDao(dataSession);
        List<ProjectFactDefinition> existingFacts = dao.listByWorkspaceId(workspaceId.intValue(), true);
        Map<String, ProjectFactDefinition> existingByCode = new HashMap<String, ProjectFactDefinition>();
        for (ProjectFactDefinition fact : existingFacts) {
            existingByCode.put(safeText(fact.getFactCode()).trim().toUpperCase(), fact);
        }

        Map<Integer, List<String>> rowByLine = new LinkedHashMap<Integer, List<String>>();
        Map<Integer, String> codeByLine = new HashMap<Integer, String>();
        Map<String, Integer> occurrencesByCode = new HashMap<String, Integer>();
        for (int i = headerRowIndex + 1; i < records.size(); i++) {
            int lineNumber = i + 1;
            List<String> row = records.get(i);
            rowByLine.put(Integer.valueOf(lineNumber), row);

            if (isBlankCsvRow(row)) {
                continue;
            }

            String factCode = normalizeFactCode(getCsvValue(row, headerMap, "fact_code"));
            if (factCode.length() == 0) {
                continue;
            }
            codeByLine.put(Integer.valueOf(lineNumber), factCode);
            Integer count = occurrencesByCode.get(factCode);
            occurrencesByCode.put(factCode, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }

        Set<String> duplicateCodes = new HashSet<String>();
        for (Map.Entry<String, Integer> entry : occurrencesByCode.entrySet()) {
            if (entry.getValue() != null && entry.getValue().intValue() > 1) {
                duplicateCodes.add(entry.getKey());
            }
        }

        int insertedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<String>();

        Transaction transaction = dataSession.beginTransaction();
        try {
            for (Map.Entry<Integer, List<String>> entry : rowByLine.entrySet()) {
                int lineNumber = entry.getKey().intValue();
                List<String> row = entry.getValue();

                if (isBlankCsvRow(row)) {
                    skippedCount++;
                    continue;
                }

                String factGroup = normalizeFactGroup(getCsvValue(row, headerMap, "fact_group"));
                String factCode = normalizeFactCode(getCsvValue(row, headerMap, "fact_code"));
                String factLabel = clip(getCsvValue(row, headerMap, "fact_label"), 200);
                String factDescription = clipAllowNull(getCsvValue(row, headerMap, "fact_description"), 1200);

                if (factGroup.length() == 0 || factCode.length() == 0 || factLabel.length() == 0) {
                    skippedCount++;
                    errors.add("Line " + lineNumber + ": required field(s) missing");
                    continue;
                }

                if (duplicateCodes.contains(factCode)) {
                    skippedCount++;
                    errors.add("Line " + lineNumber + ": duplicate fact_code in CSV (" + factCode + ")");
                    continue;
                }

                String factInputTypeRaw = getCsvValue(row, headerMap, "fact_input_type");
                String factInputType = normalizeFactInputType(factInputTypeRaw);
                if (factInputType == null) {
                    skippedCount++;
                    errors.add("Line " + lineNumber + ": invalid fact_input_type '" + safeText(factInputTypeRaw)
                            + "'");
                    continue;
                }

                int displayOrder = parseIntOrDefault(getCsvValue(row, headerMap, "display_order"), 0);

                String activeRaw = getCsvValue(row, headerMap, "active");
                String activeProvided = normalizeOptionalActive(activeRaw);
                if (activeProvided == null && safeText(activeRaw).trim().length() > 0) {
                    skippedCount++;
                    errors.add("Line " + lineNumber + ": active must be Y or N");
                    continue;
                }

                ProjectFactDefinition existing = existingByCode.get(factCode);
                if (existing == null) {
                    ProjectFactDefinition created = new ProjectFactDefinition();
                    created.setWorkspaceId(workspaceId.intValue());
                    created.setFactGroup(factGroup);
                    created.setFactCode(factCode);
                    created.setFactLabel(factLabel);
                    created.setFactDescription(factDescription);
                    created.setFactInputType(factInputType);
                    created.setDisplayOrder(displayOrder);
                    created.setActive(activeProvided == null ? ProjectFactDefinition.ACTIVE_YES : activeProvided);
                    created.setCreatedByWebUserId(Integer.valueOf(appReq.getWebUser().getWebUserId()));
                    created.setCreatedDate(new java.util.Date());
                    created.setLastModifiedByWebUserId(Integer.valueOf(appReq.getWebUser().getWebUserId()));
                    created.setLastModifiedDate(new java.util.Date());
                    dao.save(created);
                    existingByCode.put(factCode, created);
                    insertedCount++;
                } else {
                    existing.setFactGroup(factGroup);
                    existing.setFactLabel(factLabel);
                    existing.setFactDescription(factDescription);
                    existing.setFactInputType(factInputType);
                    existing.setDisplayOrder(displayOrder);
                    if (activeProvided != null) {
                        existing.setActive(activeProvided);
                    }
                    existing.setLastModifiedByWebUserId(Integer.valueOf(appReq.getWebUser().getWebUserId()));
                    existing.setLastModifiedDate(new java.util.Date());
                    dao.update(existing);
                    updatedCount++;
                }
            }

            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            redirectToFacts(appReq, "Unable to import facts CSV: " + safeText(e.getMessage()), true, null, null);
            return;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Facts CSV import complete. inserted=").append(insertedCount)
                .append(", updated=").append(updatedCount)
                .append(", skipped=").append(skippedCount);
        if (!errors.isEmpty()) {
            int showCount = Math.min(10, errors.size());
            summary.append(". errors (").append(errors.size()).append("): ");
            for (int i = 0; i < showCount; i++) {
                if (i > 0) {
                    summary.append(" | ");
                }
                summary.append(errors.get(i));
            }
            if (errors.size() > showCount) {
                summary.append(" | ...");
            }
        }

        redirectToFacts(appReq, summary.toString(), !errors.isEmpty(), null, null);
    }

    private void handleDeactivateFactDefinition(AppReq appReq) throws Exception {
        Integer workspaceId = appReq.getActiveWorkspaceId();
        if (workspaceId == null) {
            redirectToFacts(appReq, "Workspace is required", true, null, null);
            return;
        }

        Integer factDefinitionId = parseInteger(appReq.getRequest().getParameter("factDefinitionId"));
        if (factDefinitionId == null || factDefinitionId.intValue() <= 0) {
            redirectToFacts(appReq, "Fact definition id is required", true, null, null);
            return;
        }

        Session dataSession = appReq.getDataSession();
        ProjectFactDefinitionDao dao = new ProjectFactDefinitionDao(dataSession);
        ProjectFactDefinition factDefinition = dao.getById(factDefinitionId.intValue());
        if (factDefinition == null || factDefinition.getWorkspaceId() != workspaceId.intValue()) {
            redirectToFacts(appReq, "Fact definition was not found", true, null, null);
            return;
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            dao.deactivate(workspaceId.intValue(), factDefinition.getProjectFactDefinitionId(),
                    Integer.valueOf(appReq.getWebUser().getWebUserId()), new java.util.Date());
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            redirectToFacts(appReq, "Unable to deactivate fact definition: " + e.getMessage(), true,
                    Integer.valueOf(factDefinition.getProjectFactDefinitionId()), factDefinition.getFactGroup());
            return;
        }

        redirectToFacts(appReq, "Fact definition deactivated", false,
                Integer.valueOf(factDefinition.getProjectFactDefinitionId()), factDefinition.getFactGroup());
    }

    private void handleQuickCapture(AppReq appReq, Integer contextWorkspaceId, String patchTagKey) throws IOException {
        String projectIdStr = appReq.getRequest().getParameter("projectId");
        Integer projectId = parseInteger(projectIdStr);
        Integer privateProjectId = parseInteger(appReq.getRequest().getParameter("privateProjectId"));
        String sentenceInput = safeText(appReq.getRequest().getParameter("sentenceInput"));

        if (contextWorkspaceId == null) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Quick capture is only available for shared patch projects.", true);
            return;
        }
        if (projectId == null || projectId.intValue() <= 0) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Project is required for quick capture.", true);
            return;
        }

        try {
            String message = pageService.saveQuickCaptureForSelectedProject(appReq, projectId.intValue(),
                    privateProjectId, sentenceInput);
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, message, false);
        } catch (IllegalArgumentException iae) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, iae.getMessage(), true);
        } catch (Exception e) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Unable to save quick capture: " + e.getMessage(), true);
        }
    }

    private void handleSaveSharedOpenActionEdit(AppReq appReq, Integer contextWorkspaceId, String patchTagKey)
            throws IOException {
        Integer projectId = parseInteger(appReq.getRequest().getParameter("projectId"));
        Integer actionNextId = parseInteger(appReq.getRequest().getParameter("actionNextId"));
        Integer privateProjectId = parseInteger(appReq.getRequest().getParameter("privateProjectId"));
        if (contextWorkspaceId == null) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Open Actions are only available for shared patch projects.", true);
            return;
        }
        if (projectId == null || projectId.intValue() <= 0) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Project is required.", true);
            return;
        }
        if (actionNextId == null || actionNextId.intValue() <= 0) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Action is required.", true);
            return;
        }

        String actorContactId = appReq.getRequest().getParameter("actorContactId");
        String nextActionType = safeText(appReq.getRequest().getParameter("nextActionType"));
        String nextDescription = safeText(appReq.getRequest().getParameter("nextDescription"));
        String nextActionDate = safeText(appReq.getRequest().getParameter("nextActionDate"));
        String nextTimeEstimate = safeText(appReq.getRequest().getParameter("nextTimeEstimate"));
        String priorityLevel = safeText(appReq.getRequest().getParameter("priorityLevel"));
        String completionOrder = safeText(appReq.getRequest().getParameter("completionOrder"));

        try {
            String message = pageService.updateSharedOpenAction(appReq, projectId.intValue(), actionNextId.intValue(),
                    actorContactId, nextActionType, nextDescription, nextActionDate, nextTimeEstimate,
                    priorityLevel, completionOrder);
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, message, false);
        } catch (IllegalArgumentException iae) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, iae.getMessage(), true);
        } catch (Exception e) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Unable to update shared action: " + safeText(e.getMessage()), true);
        }
    }

    private void handleAdoptSharedOpenAction(AppReq appReq, Integer contextWorkspaceId, String patchTagKey)
            throws IOException {
        Integer projectId = parseInteger(appReq.getRequest().getParameter("projectId"));
        Integer actionNextId = parseInteger(appReq.getRequest().getParameter("actionNextId"));
        Integer privateProjectId = parseInteger(appReq.getRequest().getParameter("privateProjectId"));
        if (contextWorkspaceId == null) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Open Actions are only available for shared patch projects.", true);
            return;
        }
        if (projectId == null || projectId.intValue() <= 0) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Project is required.", true);
            return;
        }
        if (actionNextId == null || actionNextId.intValue() <= 0) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Action is required.", true);
            return;
        }

        try {
            String message = pageService.adoptSharedOpenAction(appReq, projectId.intValue(), actionNextId.intValue(),
                    privateProjectId);
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, message, false);
        } catch (IllegalArgumentException iae) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, iae.getMessage(), true);
        } catch (Exception e) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Unable to adopt shared action: " + safeText(e.getMessage()), true);
        }
    }

    private void handleCancelSharedOpenAction(AppReq appReq, Integer contextWorkspaceId, String patchTagKey)
            throws IOException {
        Integer projectId = parseInteger(appReq.getRequest().getParameter("projectId"));
        Integer actionNextId = parseInteger(appReq.getRequest().getParameter("actionNextId"));
        Integer privateProjectId = parseInteger(appReq.getRequest().getParameter("privateProjectId"));
        if (contextWorkspaceId == null) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Open Actions are only available for shared patch projects.", true);
            return;
        }
        if (projectId == null || projectId.intValue() <= 0) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Project is required.", true);
            return;
        }
        if (actionNextId == null || actionNextId.intValue() <= 0) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Action is required.", true);
            return;
        }

        try {
            String message = pageService.cancelSharedOpenAction(appReq, projectId.intValue(), actionNextId.intValue());
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, message, false);
        } catch (IllegalArgumentException iae) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, iae.getMessage(), true);
        } catch (Exception e) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Unable to cancel shared action: " + safeText(e.getMessage()), true);
        }
    }

    private void handleLinkPrivateProjectToSharedProject(AppReq appReq, String patchTagKey) throws IOException {
        Integer projectId = parseInteger(appReq.getRequest().getParameter("projectId"));
        Integer privateProjectId = parseInteger(appReq.getRequest().getParameter("privateProjectId"));
        if (projectId == null || projectId.intValue() <= 0) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, "Project is required.",
                    true);
            return;
        }
        if (privateProjectId == null || privateProjectId.intValue() <= 0) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Choose a private project to link.", true);
            return;
        }
        if (appReq.getActiveWorkspaceId() == null) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, "Workspace is required.",
                    true);
            return;
        }

        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();
        Project sharedProject = (Project) dataSession.get(Project.class, projectId.intValue());
        if (sharedProject == null || sharedProject.getWorkspaceId() == null
                || !sharedProject.getWorkspaceId().equals(appReq.getActiveWorkspaceId())) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Shared project was not found.", true);
            return;
        }

        Integer privateWorkspaceId = WorkspaceRegistry.getWorkspaceIdForWebUserId(dataSession,
                webUser.getWebUserId());
        Project privateProject = (Project) dataSession.get(Project.class, privateProjectId.intValue());
        if (privateWorkspaceId == null || privateProject == null || privateProject.getWorkspaceId() == null
                || !privateProject.getWorkspaceId().equals(privateWorkspaceId)) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Private project was not found.", true);
            return;
        }
        if (privateProject.getLinkedPatchWorkspaceId() != null
                && !privateProject.getLinkedPatchWorkspaceId().equals(appReq.getActiveWorkspaceId())) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Private project is already linked to a different patch workspace.", true);
            return;
        }

        ProjectPatchLinkDao dao = new ProjectPatchLinkDao(dataSession);
        if (dao.directLinkExists(privateProjectId.intValue(), projectId.intValue())) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Private project already linked.", false);
            return;
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            if (privateProject.getLinkedPatchWorkspaceId() == null) {
                privateProject.setLinkedPatchWorkspaceId(appReq.getActiveWorkspaceId());
            }
            if (webUser != null) {
                privateProject.setLastModifiedByWebUserId(Integer.valueOf(webUser.getWebUserId()));
            }
            dataSession.saveOrUpdate(privateProject);

            ProjectPatchLink link = new ProjectPatchLink();
            link.setPrivateProjectId(privateProjectId.intValue());
            link.setPatchWorkspaceId(appReq.getActiveWorkspaceId().intValue());
            link.setLinkType(ProjectPatchLink.LINK_TYPE_DIRECT_PROJECT);
            link.setLinkedPatchProjectId(projectId.intValue());
            link.setCreatedByWebUserId(webUser.getWebUserId());
            link.setCreatedDate(new java.util.Date());
            dao.save(link);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Unable to link private project: " + safeText(e.getMessage()), true);
            return;
        }

        redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, "Private project linked.", false);
    }

    private void handleSaveProjectDefinitionField(AppReq appReq, String patchTagKey) throws IOException {
        Integer projectId = parseInteger(appReq.getRequest().getParameter("projectId"));
        Integer privateProjectId = parseInteger(appReq.getRequest().getParameter("privateProjectId"));
        String fieldName = normalizeProjectDefinitionField(appReq.getRequest().getParameter("fieldName"));
        String fieldValue = clipAllowNull(appReq.getRequest().getParameter("fieldValue"), 12000);

        if (projectId == null || projectId.intValue() <= 0) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, "Project is required.", true);
            return;
        }
        if (fieldName == null) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Project definition field is invalid.", true);
            return;
        }
        if (appReq.getActiveWorkspaceId() == null) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, "Workspace is required.", true);
            return;
        }

        Session dataSession = appReq.getDataSession();
        Project project = (Project) dataSession.get(Project.class, projectId.intValue());
        if (project == null || project.getWorkspaceId() == null
                || !project.getWorkspaceId().equals(appReq.getActiveWorkspaceId())) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, "Project was not found.", true);
            return;
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            applyProjectDefinitionField(project, fieldName, fieldValue);
            if (appReq.getWebUser() != null) {
                project.setLastModifiedByWebUserId(Integer.valueOf(appReq.getWebUser().getWebUserId()));
            }
            dataSession.saveOrUpdate(project);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Unable to save project definition: " + safeText(e.getMessage()), true);
            return;
        }

        redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, "Project definition saved.", false);
    }

    private void handleToggleSharedProjectFact(AppReq appReq, Integer contextWorkspaceId, String patchTagKey)
            throws IOException {
        Integer projectId = parseInteger(appReq.getRequest().getParameter("projectId"));
        Integer privateProjectId = parseInteger(appReq.getRequest().getParameter("privateProjectId"));
        Integer factDefinitionId = parseInteger(appReq.getRequest().getParameter("factDefinitionId"));
        boolean checked = "Y".equalsIgnoreCase(safeText(appReq.getRequest().getParameter("factChecked")))
                || "true".equalsIgnoreCase(safeText(appReq.getRequest().getParameter("factChecked")));

        if (contextWorkspaceId == null) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Project Facts are only available for shared patch projects.", true);
            return;
        }
        if (projectId == null || projectId.intValue() <= 0) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Project is required.", true);
            return;
        }
        if (factDefinitionId == null || factDefinitionId.intValue() <= 0) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Fact definition is required.", true);
            return;
        }

        Session dataSession = appReq.getDataSession();
        Project project = (Project) dataSession.get(Project.class, projectId.intValue());
        if (project == null || project.getWorkspaceId() == null
                || !project.getWorkspaceId().equals(contextWorkspaceId)) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Shared project was not found.", true);
            return;
        }

        ProjectFactDefinitionDao definitionDao = new ProjectFactDefinitionDao(dataSession);
        ProjectFactDefinition definition = definitionDao.getById(factDefinitionId.intValue());
        if (definition == null
                || definition.getWorkspaceId() != contextWorkspaceId.intValue()) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Fact definition was not found in this workspace.", true);
            return;
        }
        if (!ProjectFactDefinition.ACTIVE_YES.equalsIgnoreCase(safeText(definition.getActive()))) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Only active fact definitions can be edited.", true);
            return;
        }
        if (!ProjectFactDefinition.INPUT_TYPE_BOOLEAN.equalsIgnoreCase(safeText(definition.getFactInputType()))) {
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Only boolean facts can be edited in this checklist.", true);
            return;
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            if (checked) {
                ProjectFactValueService.ProjectFactValueUpdate update = new ProjectFactValueService.ProjectFactValueUpdate();
                update.setValueBoolean(ProjectFactValue.BOOLEAN_YES);
                projectFactValueService.setFactValue(dataSession, projectId.intValue(), factDefinitionId.intValue(),
                        update, Integer.valueOf(appReq.getWebUser().getWebUserId()), new java.util.Date());

                Project currentTargetProject = resolveCurrentTargetProjectForFactLog(dataSession, appReq.getWebUser(),
                        privateProjectId);
                Date now = new java.util.Date();
                saveFactCheckedActionTaken(dataSession, appReq.getWebUser(), project, definition, now);
                if (currentTargetProject != null) {
                    saveFactCheckedActionTaken(dataSession, appReq.getWebUser(), currentTargetProject, definition,
                            now);
                }
            } else {
                projectFactValueService.clearFactValue(dataSession, projectId.intValue(), factDefinitionId.intValue());
            }
            transaction.commit();
        } catch (IllegalArgumentException iae) {
            transaction.rollback();
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, safeText(iae.getMessage()),
                    true);
            return;
        } catch (Exception e) {
            transaction.rollback();
            redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId,
                    "Unable to update project fact: " + safeText(e.getMessage()), true);
            return;
        }

        redirectToProjectHealth(appReq, projectId, patchTagKey, privateProjectId, null, false);
    }

    private Project resolveCurrentTargetProjectForFactLog(Session dataSession, WebUser webUser,
            Integer privateProjectId) {
        if (dataSession == null || webUser == null || privateProjectId == null || privateProjectId.intValue() <= 0) {
            return null;
        }
        Integer privateWorkspaceId = WorkspaceRegistry.getWorkspaceIdForWebUserId(dataSession, webUser.getWebUserId());
        if (privateWorkspaceId == null) {
            return null;
        }
        Project privateProject = (Project) dataSession.get(Project.class, privateProjectId.intValue());
        if (privateProject == null || privateProject.getWorkspaceId() == null
                || !privateProject.getWorkspaceId().equals(privateWorkspaceId)) {
            return null;
        }
        return privateProject;
    }

    private void saveFactCheckedActionTaken(Session dataSession, WebUser webUser, Project project,
            ProjectFactDefinition definition, Date now) {
        if (dataSession == null || webUser == null || project == null || definition == null
                || project.getWorkspaceId() == null) {
            return;
        }
        String description = safeText(definition.getFactLabel()).trim();
        if (description.length() == 0) {
            return;
        }

        ActionTaken actionTaken = new ActionTaken();
        actionTaken.setProject(project);
        actionTaken.setProjectId(project.getProjectId());
        actionTaken.setActionDate(now == null ? new Date() : now);
        actionTaken.setActionDescription(description);
        actionTaken.setWorkspaceId(project.getWorkspaceId());
        actionTaken.setContact(webUser.getProjectContact());
        actionTaken.setContactId(webUser.getContactId());
        dataSession.save(actionTaken);
    }

    private void redirectToFacts(AppReq appReq, String message, boolean error, Integer factDefinitionId,
            String factGroup) throws IOException {
        StringBuilder url = new StringBuilder("ProjectHealthServlet?action=editFacts");
        if (message != null && message.trim().length() > 0) {
            url.append("&factsMessage=").append(urlEncode(message));
            url.append("&factsError=").append(error ? "Y" : "N");
        }
        if (factDefinitionId != null && factDefinitionId.intValue() > 0) {
            url.append("&factDefinitionId=").append(factDefinitionId.intValue());
        }
        if (factGroup != null && factGroup.trim().length() > 0) {
            url.append("&factGroup=").append(urlEncode(factGroup.trim()));
        }
        appReq.getResponse().sendRedirect(url.toString());
    }

    private void redirectToProjectHealth(AppReq appReq, Integer projectId, String patchTagKey,
            Integer privateProjectId, String message, boolean error) throws IOException {
        StringBuilder url = new StringBuilder("ProjectHealthServlet");
        if (projectId != null && projectId.intValue() > 0) {
            url.append("?projectId=").append(projectId.intValue());
        }
        if (patchTagKey != null && patchTagKey.trim().length() > 0) {
            url.append(projectId != null && projectId.intValue() > 0 ? "&" : "?");
            url.append("patchTag=").append(urlEncode(patchTagKey.trim()));
        }
        if (privateProjectId != null && privateProjectId.intValue() > 0) {
            url.append(projectId != null && projectId.intValue() > 0
                    || (patchTagKey != null && patchTagKey.trim().length() > 0)
                            ? "&"
                            : "?");
            url.append("privateProjectId=").append(privateProjectId.intValue());
        }
        if (message != null && message.trim().length() > 0) {
            url.append(projectId != null && projectId.intValue() > 0
                    || (patchTagKey != null && patchTagKey.trim().length() > 0)
                    || (privateProjectId != null && privateProjectId.intValue() > 0)
                            ? "&"
                            : "?");
            url.append("quickCaptureMessage=").append(urlEncode(message.trim()));
            url.append("&quickCaptureError=").append(error ? "Y" : "N");
        }
        appReq.getResponse().sendRedirect(url.toString());
    }

    private void applyQuickCaptureFlashMessages(AppReq appReq, HttpServletRequest request) {
        String message = safeText(request.getParameter("quickCaptureMessage"));
        if (message.length() == 0) {
            return;
        }
        if ("Y".equalsIgnoreCase(request.getParameter("quickCaptureError"))) {
            appReq.addErrorMessage(message);
        } else {
            appReq.addSuccessMessage(message);
        }
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

    private String normalizePatchTagKey(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        return value.trim();
    }

    private String normalizeFactGroup(String value) {
        return clip(value, 60);
    }

    private String normalizeFactCode(String value) {
        return clip(value, 80).toUpperCase();
    }

    private String normalizeFactInputType(String value) {
        String normalized = safeText(value).trim().toUpperCase();
        if (normalized.length() == 0) {
            return ProjectFactDefinition.INPUT_TYPE_BOOLEAN;
        }
        if (ProjectFactDefinition.INPUT_TYPE_BOOLEAN.equals(normalized)
                || ProjectFactDefinition.INPUT_TYPE_SELECT.equals(normalized)
                || ProjectFactDefinition.INPUT_TYPE_TEXT.equals(normalized)
                || ProjectFactDefinition.INPUT_TYPE_DATE.equals(normalized)
                || ProjectFactDefinition.INPUT_TYPE_NUMBER.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeActive(String value) {
        return "N".equalsIgnoreCase(safeText(value).trim())
                ? ProjectFactDefinition.ACTIVE_NO
                : ProjectFactDefinition.ACTIVE_YES;
    }

    private String normalizeOptionalActive(String value) {
        String normalized = safeText(value).trim().toUpperCase();
        if (normalized.length() == 0) {
            return null;
        }
        if (ProjectFactDefinition.ACTIVE_YES.equals(normalized)
                || ProjectFactDefinition.ACTIVE_NO.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeProjectDefinitionField(String value) {
        String normalized = safeText(value).trim();
        if ("currentFocusText".equals(normalized)
                || "outcomeText".equals(normalized)
                || "successCriteriaText".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private void applyProjectDefinitionField(Project project, String fieldName, String fieldValue) {
        if ("currentFocusText".equals(fieldName)) {
            project.setCurrentFocusText(fieldValue);
            return;
        }
        if ("outcomeText".equals(fieldName)) {
            project.setOutcomeText(fieldValue);
            return;
        }
        if ("successCriteriaText".equals(fieldName)) {
            project.setSuccessCriteriaText(fieldValue);
            return;
        }
        throw new IllegalArgumentException("Unknown project definition field");
    }

    private String clip(String value, int maxLength) {
        String normalized = safeText(value).trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String clipAllowNull(String value, int maxLength) {
        String normalized = clip(value, maxLength);
        return normalized.length() == 0 ? null : normalized;
    }

    private String csvCell(String value) {
        String text = safeText(value);
        boolean needsQuotes = text.indexOf(',') >= 0 || text.indexOf('"') >= 0
                || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String readPartUtf8(Part part) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream inputStream = part.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            char[] buffer = new char[4096];
            int len;
            while ((len = reader.read(buffer)) >= 0) {
                if (len == 0) {
                    continue;
                }
                sb.append(buffer, 0, len);
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }

    private List<List<String>> parseCsvRecords(String content) {
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> currentRow = new ArrayList<String>();
        StringBuilder currentCell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    currentCell.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (ch == ',' && !inQuotes) {
                currentRow.add(currentCell.toString());
                currentCell.setLength(0);
                continue;
            }
            if ((ch == '\n' || ch == '\r') && !inQuotes) {
                currentRow.add(currentCell.toString());
                currentCell.setLength(0);
                rows.add(currentRow);
                currentRow = new ArrayList<String>();
                if (ch == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                continue;
            }
            currentCell.append(ch);
        }

        if (currentCell.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(currentCell.toString());
            rows.add(currentRow);
        }

        return rows;
    }

    private boolean isBlankCsvRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (String cell : row) {
            if (safeText(cell).trim().length() > 0) {
                return false;
            }
        }
        return true;
    }

    private String normalizeHeaderName(String value) {
        String normalized = safeText(value).trim().toLowerCase();
        normalized = normalized.replace(' ', '_').replace('-', '_');
        return normalized;
    }

    private String getCsvValue(List<String> row, Map<String, Integer> headerMap, String headerName) {
        Integer index = headerMap.get(headerName);
        if (index == null) {
            return "";
        }
        int idx = index.intValue();
        if (idx < 0 || idx >= row.size()) {
            return "";
        }
        return safeText(row.get(idx)).trim();
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(safeText(value).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String joinWithComma(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    private String urlEncode(String value) {
        return WebEscaper.urlEncode(safeText(value));
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
