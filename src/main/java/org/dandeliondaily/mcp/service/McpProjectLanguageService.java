package org.dandeliondaily.mcp.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.Project;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * update_project_language: a thin, separately-gated wrapper around Project's
 * native language-field setters, mirroring the presence-flag partial-update
 * convention already used by ProjectDefinitionImportService (field omitted
 * vs. explicitly cleared), gated the same way the existing LANGUAGE_REVIEW
 * chat apply path is (WorkspaceRegistry.canAdministerWorkspace), plus a
 * staleness check against Project.lastModifiedDate. Never touches
 * projectName/projectHandle -- renames stay a human-only, explicit action.
 * See docs/Dandelion_Daily_AI_Integration_Assessment.md section 4.
 */
public class McpProjectLanguageService {

    private static final int MAX_FIELD_LENGTH = 12000;

    public Map<String, Object> updateLanguage(Session session, int workspaceId, int webUserId, int projectId,
            String asOf, JsonNode fields) {
        Project project = requireProject(session, workspaceId, projectId);
        if (!WorkspaceRegistry.canAdministerWorkspace(session, workspaceId, webUserId)) {
            throw new McpToolException("forbidden", "This account cannot administer this workspace.");
        }

        String currentAsOf = McpActionContextSupport.toIso(project.getLastModifiedDate());
        if (currentAsOf != null) {
            if (asOf == null || asOf.trim().length() == 0) {
                throw new McpToolException("invalid_arguments",
                        "\"asOf\" is required once a project has been previously modified; read get_project_context "
                                + "first to get the current lastModifiedAt.");
            }
            if (!currentAsOf.equals(asOf)) {
                throw new McpToolException("stale",
                        "Project language has changed since it was last read (current lastModifiedAt: "
                                + currentAsOf + ").");
            }
        }

        boolean changed = false;
        if (fields != null && fields.has("description")) {
            project.setDescription(clip(textOrNull(fields, "description")));
            changed = true;
        }
        if (fields != null && fields.has("currentFocus")) {
            project.setCurrentFocusText(clip(textOrNull(fields, "currentFocus")));
            changed = true;
        }
        if (fields != null && fields.has("outcome")) {
            project.setOutcomeText(clip(textOrNull(fields, "outcome")));
            changed = true;
        }
        if (fields != null && fields.has("successCriteria")) {
            project.setSuccessCriteriaText(clip(textOrNull(fields, "successCriteria")));
            changed = true;
        }
        if (!changed) {
            throw new McpToolException("invalid_arguments",
                    "At least one of description, currentFocus, outcome, successCriteria must be provided.");
        }

        project.setLastModifiedDate(McpActionContextSupport.truncatedNow());
        session.update(project);
        return toMap(project);
    }

    private Map<String, Object> toMap(Project project) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("projectId", project.getProjectId());
        map.put("description", project.getDescription());
        map.put("currentFocus", project.getCurrentFocusText());
        map.put("outcome", project.getOutcomeText());
        map.put("successCriteria", project.getSuccessCriteriaText());
        map.put("lastModifiedAt", McpActionContextSupport.toIso(project.getLastModifiedDate()));
        return map;
    }

    private String textOrNull(JsonNode fields, String field) {
        JsonNode node = fields.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private String clip(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            return null;
        }
        return trimmed.length() > MAX_FIELD_LENGTH ? trimmed.substring(0, MAX_FIELD_LENGTH) : trimmed;
    }

    private Project requireProject(Session session, int workspaceId, int projectId) {
        Query query = session.createQuery(
                "from Project p where p.projectId = :projectId and p.workspaceId = :workspaceId");
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        Project project = (Project) query.uniqueResult();
        if (project == null) {
            throw new McpToolException("not_found", "Project not found.");
        }
        return project;
    }
}
