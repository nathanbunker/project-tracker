package org.dandeliondaily.mcp.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.dandeliondaily.narrative.service.TrackerNarrativeService;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.doa.TrackerNarrativeDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;
import org.openimmunizationsoftware.pt.model.TrackerNarrativeReviewStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * get_narratives (docs/MCP-Feedback.md item 8, I-8): read-only access to the
 * generated daily/weekly TrackerNarrative reports, for reviewing what
 * actually happened across a period before setting the next one's outlook.
 */
public class McpNarrativesService {

    public Map<String, Object> listNarratives(Session session, int contactId, String periodType, LocalDate from,
            LocalDate to) {
        return listNarratives(session, contactId, null, periodType, from, to, null, true, true, null);
    }

    public Map<String, Object> listNarratives(Session session, int contactId, Integer workspaceId, String periodType,
            LocalDate from, LocalDate to, String reviewStatus, boolean includeHistory, boolean includeDeleted,
            WebUser owner) {
        List<TrackerNarrative> narratives = new TrackerNarrativeDao(session)
                .findByContactAndTypeInPeriodStartRange(contactId, periodType, from, to);
        List<Map<String, Object>> mapped = new ArrayList<Map<String, Object>>();
        Set<String> periods = new HashSet<String>();
        for (TrackerNarrative narrative : narratives) {
            if (!includeDeleted && narrative.getReviewStatus() == TrackerNarrativeReviewStatus.DELETED) {
                continue;
            }
            if (reviewStatus != null && !reviewStatus.equals(narrative.getReviewStatusString())) {
                continue;
            }
            if (workspaceId != null) {
                Project project = (Project) session.get(Project.class, narrative.getProjectId());
                if (project == null || !workspaceId.equals(project.getWorkspaceId())) {
                    continue;
                }
            }
            String period = narrative.getNarrativeType() + ":" + narrative.getPeriodStart();
            if (!includeHistory && !periods.add(period)) {
                continue;
            }
            mapped.add(toMap(narrative, owner, false));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("narratives", mapped);
        return result;
    }

    public Map<String, Object> toMap(TrackerNarrative narrative, WebUser owner, boolean includePrompt) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("narrativeId", Integer.valueOf(narrative.getNarrativeId()));
        map.put("projectId", Integer.valueOf(narrative.getProjectId()));
        map.put("narrativeType", narrative.getNarrativeType());
        map.put("displayTitle", narrative.getDisplayTitle());
        map.put("periodStart", McpActionContextSupport.toIso(narrative.getPeriodStart()));
        map.put("periodEnd", McpActionContextSupport.toIso(narrative.getPeriodEnd()));
        if (owner != null) {
            map.put("periodStartDate", owner.toLocalDate(narrative.getPeriodStart()).toString());
            map.put("periodEndDate", owner.toLocalDate(narrative.getPeriodEnd()).toString());
        }
        map.put("reviewStatus", narrative.getReviewStatusString());
        String text = narrative.getMarkdownFinal() != null ? narrative.getMarkdownFinal()
                : narrative.getMarkdownGenerated();
        map.put("text", text);
        map.put("isFinal", Boolean.valueOf(narrative.getMarkdownFinal() != null));
        map.put("isApproved", Boolean.valueOf(narrative.getReviewStatus() == TrackerNarrativeReviewStatus.APPROVED));
        map.put("markdownGenerated", narrative.getMarkdownGenerated());
        map.put("markdownFinal", narrative.getMarkdownFinal());
        map.put("dateGenerated", McpActionContextSupport.toIso(narrative.getDateGenerated()));
        map.put("dateApproved", McpActionContextSupport.toIso(narrative.getDateApproved()));
        map.put("modelName", narrative.getModelName());
        map.put("promptVersion", narrative.getPromptVersion());
        map.put("lastUpdated", TrackerNarrativeService.version(narrative));
        if (includePrompt) {
            map.put("promptUsedText", narrative.getPromptUsedText());
        }
        return map;
    }
}
