package org.dandeliondaily.mcp.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.doa.ProjectNarrativeDao;
import org.openimmunizationsoftware.pt.doa.ProjectNarrativeDao.Action;
import org.openimmunizationsoftware.pt.doa.ProjectNarrativeDao.ActionWithMinutes;
import org.openimmunizationsoftware.pt.doa.ProjectNarrativeDao.ReviewItemDetail;
import org.openimmunizationsoftware.pt.doa.TrackerNarrativeDao;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;

/**
 * get_work_day_review (docs/MCP-Feedback.md item 2, I-10): mirrors
 * ProjectNarrativeReviewServlet's review screen for a whole day, in
 * structured form, including *why* a project isn't reviewed yet and whether
 * the day's TrackerNarrative report has been generated/approved (item 7).
 * Read-only; pairs with save_work_day_review (McpWorkDayReviewSaveService).
 */
public class McpWorkDayReviewService {

    private static final String DAILY_NARRATIVE_TYPE = "DAILY";

    public Map<String, Object> getWorkDayReview(Session session, int contactId, LocalDate date,
            boolean includeBelowThreshold) {
        ProjectNarrativeDao narrativeDao = new ProjectNarrativeDao(session);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("date", date.toString());
        result.put("minutesReviewThreshold", Integer.valueOf(ProjectNarrativeDao.MINUTES_REVIEW_THRESHOLD));
        result.put("totalMinutes", Integer.valueOf(narrativeDao.getTotalMinutesForDate(date)));

        List<Map<String, Object>> projects = new ArrayList<Map<String, Object>>();
        for (ReviewItemDetail item : narrativeDao.listReviewItemsForDateDetailed(date, contactId,
                includeBelowThreshold)) {
            projects.add(toProjectMap(narrativeDao, item, date));
        }
        result.put("projects", projects);
        result.put("dailyReport", loadDailyReportStatus(session, contactId, date));
        return result;
    }

    private Map<String, Object> toProjectMap(ProjectNarrativeDao narrativeDao, ReviewItemDetail item,
            LocalDate date) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("projectId", Long.valueOf(item.getProjectId()));
        map.put("projectName", item.getProjectName());
        map.put("minutesSpent", Integer.valueOf(item.getMinutesSpent()));
        map.put("reviewed", Boolean.valueOf(item.isReviewed()));
        if (!item.isReviewed()) {
            map.put("notReviewedReason", item.getMissingSetup().isEmpty() ? "no_narrative_yet" : "setup_incomplete");
        }
        map.put("missingSetup", item.getMissingSetup());

        List<Map<String, Object>> completedActions = new ArrayList<Map<String, Object>>();
        for (Action action : narrativeDao.getCompletedActionsForProjectOnDate(item.getProjectId(), date)) {
            Map<String, Object> actionMap = new LinkedHashMap<String, Object>();
            actionMap.put("actionTakenId", Integer.valueOf(action.getActionTakenId()));
            actionMap.put("description", action.getDescription());
            actionMap.put("completedAt", McpActionContextSupport.toIso(action.getCompletedDate()));
            actionMap.put("completionNote", action.getCompletionNote());
            completedActions.add(actionMap);
        }
        map.put("completedActions", completedActions);

        List<Map<String, Object>> deletedActions = new ArrayList<Map<String, Object>>();
        for (ActionWithMinutes action : narrativeDao.getDeletedActionsWithTimeForProjectOnDate(item.getProjectId(),
                date)) {
            Map<String, Object> actionMap = new LinkedHashMap<String, Object>();
            actionMap.put("actionNextId", Integer.valueOf(action.getActionTakenId()));
            actionMap.put("description", action.getDescription());
            actionMap.put("changeDate", McpActionContextSupport.toIso(action.getChangeDate()));
            actionMap.put("minutes", Integer.valueOf(action.getMinutes()));
            deletedActions.add(actionMap);
        }
        map.put("deletedActionsWithTime", deletedActions);

        map.put("narratives", narrativesByVerbMap(narrativeDao, item.getProjectId(), date));

        return map;
    }

    public static Map<String, Object> narrativesByVerbMap(ProjectNarrativeDao narrativeDao, long projectId,
            LocalDate date) {
        Map<String, Object> narrativesByVerb = new LinkedHashMap<String, Object>();
        for (ProjectNarrative narrative : narrativeDao.findByProjectAndDateRange(projectId, date)) {
            ProjectNarrativeVerb verb = narrative.getNarrativeVerb();
            if (verb == null) {
                continue;
            }
            Map<String, Object> narrativeMap = new LinkedHashMap<String, Object>();
            narrativeMap.put("narrativeId", Integer.valueOf(narrative.getNarrativeId()));
            narrativeMap.put("text", narrative.getNarrativeText());
            narrativeMap.put("lastUpdated", McpActionContextSupport.toIso(narrative.getLastUpdated()));
            narrativesByVerb.put(verb.getId(), narrativeMap);
        }
        return narrativesByVerb;
    }

    private Map<String, Object> loadDailyReportStatus(Session session, int contactId, LocalDate date) {
        List<TrackerNarrative> narratives = new TrackerNarrativeDao(session).findByContactTypeAndPeriod(contactId,
                DAILY_NARRATIVE_TYPE, date, date);
        if (narratives.isEmpty()) {
            Map<String, Object> notGenerated = new LinkedHashMap<String, Object>();
            notGenerated.put("exists", Boolean.FALSE);
            return notGenerated;
        }
        TrackerNarrative narrative = narratives.get(0);
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("exists", Boolean.TRUE);
        status.put("narrativeId", Integer.valueOf(narrative.getNarrativeId()));
        status.put("reviewStatus", narrative.getReviewStatusString());
        status.put("generated", Boolean.valueOf(narrative.getMarkdownGenerated() != null));
        status.put("approved", Boolean.valueOf("APPROVED".equals(narrative.getReviewStatusString())));
        status.put("lastUpdated", McpActionContextSupport.toIso(narrative.getLastUpdated()));
        return status;
    }
}
