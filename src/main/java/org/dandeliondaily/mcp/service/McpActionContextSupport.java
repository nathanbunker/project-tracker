package org.dandeliondaily.mcp.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dandeliondaily.dashboard.service.DashboardActionOrdering;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionSet;
import org.openimmunizationsoftware.pt.model.ActionSetType;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * Shared shaping of an ActionNext row for MCP read tools: exposes the
 * template-origin flags (see docs/Dandelion_Daily_AI_Integration_Assessment.md,
 * section 6, decision 14) and, for actions in a SHARED ActionSet, the identity
 * of the other project(s) the action is shared with, even when those projects
 * live in a different workspace than the calling client (decision 1).
 */
public final class McpActionContextSupport {

    private McpActionContextSupport() {
    }

    public static Map<String, Object> toActionMap(Session session, ActionNext action, boolean includeProjectName) {
        return toActionMap(session, action, includeProjectName, null);
    }

    /**
     * With a webUser, also reports the dashboard Today-column bucket the action
     * falls in (see DashboardActionOrdering); completionOrder only orders actions
     * within a bucket.
     */
    public static Map<String, Object> toActionMap(Session session, ActionNext action, boolean includeProjectName,
            WebUser webUser) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("actionNextId", action.getActionNextId());
        map.put("projectId", action.getProjectId());
        if (includeProjectName) {
            Project project = resolveProject(session, action);
            map.put("projectName", project == null ? null : project.getProjectName());
        }
        map.put("description", action.getNextDescription());
        map.put("nextActionType", action.getNextActionType());
        map.put("status", action.getNextActionStatusString());
        map.put("asOf", toIso(action.getNextChangeDate()));
        map.put("scheduledDate", toIso(action.getNextActionDate()));
        map.put("deadlineDate", toIso(action.getNextDeadlineDate()));
        map.put("targetDate", toIso(action.getNextTargetDate()));
        map.put("estimateMinutes", action.getNextTimeEstimate());
        map.put("actualMinutes", action.getNextTimeActual());
        map.put("notes", action.getNextNotes());
        map.put("linkUrl", action.getLinkUrl());
        map.put("completionOrder", action.getCompletionOrder());
        map.put("priorityLevel", action.getPriorityLevel());
        if (webUser != null) {
            map.put("dashboardBucket", DashboardActionOrdering.getBucketLabel(
                    DashboardActionOrdering.getCompletionBucket(action, webUser)));
        }
        map.put("rescheduleLocked", action.isRescheduleLocked());
        boolean isTemplateRoot = action.isTemplate();
        map.put("isTemplateRoot", isTemplateRoot);
        map.put("generatedFromTemplateId", action.getTemplateActionNextId());
        map.put("templateManaged", isTemplateRoot || action.getTemplateActionNextId() != null);
        List<Map<String, Object>> sharedWith = resolveSharedProjects(session, action);
        if (!sharedWith.isEmpty()) {
            map.put("sharedWithProjects", sharedWith);
        }
        return map;
    }

    private static Project resolveProject(Session session, ActionNext action) {
        Project project = action.getProject();
        if (project == null && action.getProjectId() > 0) {
            project = (Project) session.get(Project.class, action.getProjectId());
        }
        return project;
    }

    private static List<Map<String, Object>> resolveSharedProjects(Session session, ActionNext action) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        ActionSet actionSet = action.getActionSet();
        if (actionSet == null || actionSet.getActionSetType() != ActionSetType.SHARED) {
            return result;
        }
        Query query = session.createQuery(
                "from ActionNext an where an.actionSet.actionSetId = :actionSetId and an.actionNextId <> :selfId");
        query.setInteger("actionSetId", actionSet.getActionSetId());
        query.setInteger("selfId", action.getActionNextId());
        @SuppressWarnings("unchecked")
        List<ActionNext> siblings = query.list();
        Set<Integer> seenProjectIds = new HashSet<Integer>();
        for (ActionNext sibling : siblings) {
            if (!seenProjectIds.add(Integer.valueOf(sibling.getProjectId()))) {
                continue;
            }
            Project project = (Project) session.get(Project.class, sibling.getProjectId());
            if (project == null) {
                continue;
            }
            Map<String, Object> projectMap = new LinkedHashMap<String, Object>();
            projectMap.put("projectId", project.getProjectId());
            projectMap.put("projectName", project.getProjectName());
            projectMap.put("projectHandle", project.getProjectHandle());
            projectMap.put("workspaceId", project.getWorkspaceId());
            result.add(projectMap);
        }
        return result;
    }

    /**
     * Some ActionNext date fields (nextActionDate, nextDeadlineDate,
     * nextTargetDate) are mapped Hibernate type="date" and come back as
     * java.sql.Date, whose toInstant() always throws
     * UnsupportedOperationException. Building the Instant from the epoch
     * millis instead works for java.util.Date, java.sql.Date, and
     * java.sql.Timestamp alike.
     */
    public static String toIso(Date date) {
        return date == null ? null : Instant.ofEpochMilli(date.getTime()).toString();
    }

    /**
     * "Now", truncated to whole-second precision. MySQL's datetime columns
     * (last_modified_date, next_change_date) silently drop fractional
     * seconds on write; a version-marker value that's echoed straight back
     * in the same response -- without a DB round-trip -- must already match
     * what a later read will show, or a client replaying that exact value
     * as "asOf" gets spuriously rejected as stale (found in live testing:
     * McpProjectLanguageService's response carried milliseconds the
     * persisted column never kept).
     */
    public static Date truncatedNow() {
        return new Date((System.currentTimeMillis() / 1000L) * 1000L);
    }
}
