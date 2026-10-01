package org.dandeliondaily.mcp.service;

import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.doa.ProjectNarrativeDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * add/update/delete_project_narrative (docs/MCP-Feedback.md item 6, I-5):
 * narrative CRUD for any date, distinct from save_work_day_review's
 * day-scoped upsert-or-clear (McpSaveWorkDayReviewService). Shares
 * ProjectNarrativeDao with that path and with ProjectNarrativeReviewServlet,
 * and keeps the same "at most one narrative per project/verb/day" invariant
 * the review flow relies on (ProjectNarrativeDao.findNarrativeForProjectVerbOnDate).
 */
public class McpProjectNarrativeCrudService {

    public Map<String, Object> addNarrative(Session session, int workspaceId, int projectId, WebUser webUser,
            ProjectNarrativeVerb verb, LocalDate date, String text) {
        Project project = requireProjectInWorkspace(session, workspaceId, projectId);
        String trimmed = requireText(text);
        ProjectNarrativeDao dao = new ProjectNarrativeDao(session);
        ProjectNarrative existing = dao.findNarrativeForProjectVerbOnDate(projectId, verb, date);
        if (existing != null) {
            throw new McpToolException("already_exists",
                    "A " + verb.getId() + " narrative already exists for this project on " + date
                            + " (narrativeId " + existing.getNarrativeId() + "); use update_project_narrative "
                            + "instead.");
        }

        ProjectNarrative narrative = new ProjectNarrative();
        narrative.setProject(project);
        narrative.setContact(webUser.getProjectContact());
        narrative.setWorkspaceId(project.getWorkspaceId());
        narrative.setNarrativeVerb(verb);
        narrative.setNarrativeText(trimmed);
        narrative.setNarrativeDate(webUser.toDate(date));
        dao.insert(narrative);
        return toMap(narrative);
    }

    public Map<String, Object> updateNarrative(Session session, int workspaceId, int narrativeId, String lastUpdated,
            WebUser webUser, ProjectNarrativeVerb newVerb, LocalDate newDate, String newText) {
        ProjectNarrative narrative = requireNarrativeInWorkspace(session, workspaceId, narrativeId);
        checkStaleness(narrative, lastUpdated);
        if (newVerb == null && newDate == null && newText == null) {
            throw new McpToolException("invalid_arguments", "At least one of verb, date, text must be provided.");
        }

        ProjectNarrativeDao dao = new ProjectNarrativeDao(session);
        ProjectNarrativeVerb effectiveVerb = newVerb != null ? newVerb : narrative.getNarrativeVerb();
        LocalDate effectiveDate = newDate != null ? newDate : webUser.toLocalDate(narrative.getNarrativeDate());
        if ((newVerb != null || newDate != null)) {
            ProjectNarrative collision = dao.findNarrativeForProjectVerbOnDate(narrative.getProjectId(),
                    effectiveVerb, effectiveDate);
            if (collision != null && collision.getNarrativeId() != narrative.getNarrativeId()) {
                throw new McpToolException("already_exists",
                        "A " + effectiveVerb.getId() + " narrative already exists for this project on "
                                + effectiveDate + " (narrativeId " + collision.getNarrativeId() + ").");
            }
        }

        if (newText != null) {
            narrative.setNarrativeText(requireText(newText));
        }
        narrative.setNarrativeVerb(effectiveVerb);
        if (newDate != null) {
            narrative.setNarrativeDate(rebuildDateKeepingTimeOfDay(narrative.getNarrativeDate(), newDate, webUser));
        }
        dao.update(narrative);
        return toMap(narrative);
    }

    public Map<String, Object> deleteNarrative(Session session, int workspaceId, int narrativeId,
            String lastUpdated) {
        ProjectNarrative narrative = requireNarrativeInWorkspace(session, workspaceId, narrativeId);
        checkStaleness(narrative, lastUpdated);
        new ProjectNarrativeDao(session).delete(narrative);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("narrativeId", Integer.valueOf(narrativeId));
        result.put("deleted", Boolean.TRUE);
        return result;
    }

    private void checkStaleness(ProjectNarrative narrative, String lastUpdated) {
        String currentAsOf = McpActionContextSupport.toIso(narrative.getLastUpdated());
        if (currentAsOf != null) {
            if (lastUpdated == null || lastUpdated.trim().length() == 0) {
                throw new McpToolException("invalid_arguments",
                        "\"lastUpdated\" is required; read it from get_project_context or get_work_day_review "
                                + "first.");
            }
            if (!currentAsOf.equals(lastUpdated)) {
                throw new McpToolException("stale",
                        "This narrative has changed since it was last read (current lastUpdated: " + currentAsOf
                                + ").");
            }
        }
    }

    private Date rebuildDateKeepingTimeOfDay(Date currentNarrativeDate, LocalDate newDate, WebUser webUser) {
        java.time.LocalDateTime currentLocalDateTime = currentNarrativeDate.toInstant()
                .atZone(webUser.getZoneId()).toLocalDateTime();
        java.time.LocalDateTime rebuilt = java.time.LocalDateTime.of(newDate, currentLocalDateTime.toLocalTime());
        return webUser.toDate(rebuilt);
    }

    private String requireText(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.length() == 0) {
            throw new McpToolException("invalid_arguments", "\"text\" is required and cannot be empty.");
        }
        return trimmed;
    }

    private Map<String, Object> toMap(ProjectNarrative narrative) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("narrativeId", Integer.valueOf(narrative.getNarrativeId()));
        map.put("projectId", Integer.valueOf(narrative.getProjectId()));
        map.put("verb", narrative.getNarrativeVerb() == null ? null : narrative.getNarrativeVerb().getId());
        map.put("date", McpActionContextSupport.toIso(narrative.getNarrativeDate()));
        map.put("text", narrative.getNarrativeText());
        map.put("lastUpdated", McpActionContextSupport.toIso(narrative.getLastUpdated()));
        return map;
    }

    private Project requireProjectInWorkspace(Session session, int workspaceId, int projectId) {
        Project project = (Project) session.get(Project.class, projectId);
        if (project == null || project.getWorkspaceId() == null || project.getWorkspaceId().intValue() != workspaceId) {
            throw new McpToolException("not_found", "Project not found.");
        }
        return project;
    }

    private ProjectNarrative requireNarrativeInWorkspace(Session session, int workspaceId, int narrativeId) {
        ProjectNarrative narrative = new ProjectNarrativeDao(session).findById(narrativeId);
        if (narrative == null || narrative.getWorkspaceId() == null
                || narrative.getWorkspaceId().intValue() != workspaceId) {
            throw new McpToolException("not_found", "Narrative not found.");
        }
        return narrative;
    }
}
