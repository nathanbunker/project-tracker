package org.dandeliondaily.mcp.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.dandeliondaily.projectnarrative.service.ProjectNarrativeService;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.doa.ProjectNarrativeDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * save_work_day_review (docs/MCP-Feedback.md item 3, I-10): delegates to the
 * same ProjectNarrativeService.applyReviewFields used by
 * ProjectNarrativeReviewServlet, so the review screen and this tool can
 * never drift apart.
 */
public class McpSaveWorkDayReviewService {

    private final ProjectNarrativeService narrativeService = new ProjectNarrativeService();

    public Map<String, Object> save(Session session, int workspaceId, long projectId, LocalDate date,
            WebUser webUser, Map<ProjectNarrativeVerb, String> providedFields,
            Map<ProjectNarrativeVerb, String> expectedLastUpdated) {
        if (providedFields.isEmpty()) {
            throw new McpToolException("invalid_arguments",
                    "\"entries\" must include at least one of note, decision, insight, risk, opportunity.");
        }
        Project project = ProjectNarrativeService.requireProjectForWorkspace(session, Integer.valueOf(workspaceId),
                projectId);
        ProjectNarrativeDao narrativeDao = new ProjectNarrativeDao(session);
        checkStaleness(narrativeDao, projectId, date, expectedLastUpdated);

        narrativeService.applyReviewFields(session, project, date, webUser, providedFields);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("projectId", Long.valueOf(projectId));
        result.put("date", date.toString());
        result.put("narratives", McpWorkDayReviewService.narrativesByVerbMap(narrativeDao, projectId, date));
        return result;
    }

    private void checkStaleness(ProjectNarrativeDao narrativeDao, long projectId, LocalDate date,
            Map<ProjectNarrativeVerb, String> expectedLastUpdated) {
        if (expectedLastUpdated == null || expectedLastUpdated.isEmpty()) {
            return;
        }
        for (Map.Entry<ProjectNarrativeVerb, String> entry : expectedLastUpdated.entrySet()) {
            ProjectNarrative existing = narrativeDao.findNarrativeForProjectVerbOnDate(projectId, entry.getKey(),
                    date);
            String currentAsOf = existing == null ? null : McpActionContextSupport.toIso(existing.getLastUpdated());
            if (currentAsOf != null && !currentAsOf.equals(entry.getValue())) {
                throw new McpToolException("stale",
                        entry.getKey().getId() + " narrative has changed since it was last read (current "
                                + "lastUpdated: " + currentAsOf + ").");
            }
        }
    }
}
