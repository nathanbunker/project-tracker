package org.dandeliondaily.mcp.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.dashboard.service.DashboardActionOrdering;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * Cross-project, date-range, optionally type-filtered view of scheduled and
 * overdue actions for a workspace. A data-only, filterable analog of
 * org.dandeliondaily.planahead.service.PlanAheadBoardService#buildBoard,
 * which is UI-model-oriented and bound to a fixed window and web session.
 * "Just meetings this week" or "everything on Thursday" are both just a
 * narrower call to this same method (see assessment section 6, decision 17).
 */
public class McpPlanningContextService {

    private static final int MAX_RANGE_DAYS = 62;
    private static final int MAX_ACTIONS = 200;

    public Map<String, Object> getPlanningContext(Session session, int workspaceId, LocalDate startDate,
            LocalDate endDate, List<String> actionTypes, List<Integer> projectIds, WebUser webUser) {
        if (startDate == null || endDate == null) {
            throw new McpToolException("invalid_arguments", "startDate and endDate are required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new McpToolException("invalid_arguments", "endDate must not be before startDate.");
        }
        long rangeDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (rangeDays > MAX_RANGE_DAYS) {
            throw new McpToolException("invalid_arguments",
                    "Date range too large; maximum is " + MAX_RANGE_DAYS + " days.");
        }

        Date start = toDate(startDate);
        Date endExclusive = toDate(endDate.plusDays(1));
        Date today = toDate(LocalDate.now());

        List<ActionNext> scheduled = loadScheduledActions(session, workspaceId, start, endExclusive, actionTypes,
                projectIds);
        List<ActionNext> overdue = loadOverdueActions(session, workspaceId, today, actionTypes, projectIds);
        Map<LocalDate, List<ActionNext>> scheduledByDay = groupByDayInDashboardOrder(scheduled, webUser);
        scheduled = new ArrayList<ActionNext>();
        for (List<ActionNext> dayActions : scheduledByDay.values()) {
            scheduled.addAll(dayActions);
        }
        DashboardActionOrdering.sortByBucketThenCompletionOrder(overdue, webUser);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("scheduledActions", toActionList(session, scheduled, webUser));
        result.put("overdueActions", toActionList(session, overdue, webUser));
        result.put("dayOrder", toDayOrder(scheduledByDay, webUser));
        return result;
    }

    /**
     * Groups the scheduled actions by day and sorts each day the way the
     * dashboard Today column does: by bucket, then completionOrder within the
     * bucket. The query already returns days in date order.
     */
    private Map<LocalDate, List<ActionNext>> groupByDayInDashboardOrder(List<ActionNext> actions,
            WebUser webUser) {
        Map<LocalDate, List<ActionNext>> byDay = new LinkedHashMap<LocalDate, List<ActionNext>>();
        for (ActionNext action : actions) {
            LocalDate day = webUser.toLocalDate(action.getNextActionDate());
            List<ActionNext> dayActions = byDay.get(day);
            if (dayActions == null) {
                dayActions = new ArrayList<ActionNext>();
                byDay.put(day, dayActions);
            }
            dayActions.add(action);
        }
        for (List<ActionNext> dayActions : byDay.values()) {
            DashboardActionOrdering.sortByBucketThenCompletionOrder(dayActions, webUser);
        }
        return byDay;
    }

    /**
     * A compact view of each day's order: the dashboard buckets in display
     * order, each with its action ids in completion order. This is the shape
     * apply_changes order_day works within.
     */
    private List<Map<String, Object>> toDayOrder(Map<LocalDate, List<ActionNext>> scheduledByDay,
            WebUser webUser) {
        List<Map<String, Object>> days = new ArrayList<Map<String, Object>>();
        for (Map.Entry<LocalDate, List<ActionNext>> entry : scheduledByDay.entrySet()) {
            List<Map<String, Object>> groups = new ArrayList<Map<String, Object>>();
            String currentBucket = null;
            List<Integer> currentIds = null;
            for (ActionNext action : entry.getValue()) {
                String bucket = DashboardActionOrdering.getBucketLabel(
                        DashboardActionOrdering.getCompletionBucket(action, webUser));
                if (!bucket.equals(currentBucket)) {
                    currentBucket = bucket;
                    currentIds = new ArrayList<Integer>();
                    Map<String, Object> group = new LinkedHashMap<String, Object>();
                    group.put("bucket", bucket);
                    group.put("actionNextIds", currentIds);
                    groups.add(group);
                }
                currentIds.add(Integer.valueOf(action.getActionNextId()));
            }
            Map<String, Object> day = new LinkedHashMap<String, Object>();
            day.put("date", entry.getKey() == null ? null : entry.getKey().toString());
            day.put("groups", groups);
            days.add(day);
        }
        return days;
    }

    private List<Map<String, Object>> toActionList(Session session, List<ActionNext> actions, WebUser webUser) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (ActionNext action : actions) {
            list.add(McpActionContextSupport.toActionMap(session, action, true, webUser));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<ActionNext> loadScheduledActions(Session session, int workspaceId, Date start, Date endExclusive,
            List<String> actionTypes, List<Integer> projectIds) {
        StringBuilder hql = new StringBuilder(
                "from ActionNext an where an.workspaceId = :workspaceId "
                        + "and an.nextActionDate >= :start and an.nextActionDate < :end "
                        + "and an.nextActionStatusString <> :completed and an.nextActionStatusString <> :cancelled");
        if (!actionTypes.isEmpty()) {
            hql.append(" and an.nextActionType in (:actionTypes)");
        }
        if (!projectIds.isEmpty()) {
            hql.append(" and an.projectId in (:projectIds)");
        }
        hql.append(" order by an.nextActionDate, an.priorityLevel desc");
        Query query = session.createQuery(hql.toString());
        query.setInteger("workspaceId", workspaceId);
        query.setParameter("start", start);
        query.setParameter("end", endExclusive);
        query.setString("completed", ProjectNextActionStatus.COMPLETED.getId());
        query.setString("cancelled", ProjectNextActionStatus.CANCELLED.getId());
        if (!actionTypes.isEmpty()) {
            query.setParameterList("actionTypes", actionTypes);
        }
        if (!projectIds.isEmpty()) {
            query.setParameterList("projectIds", projectIds);
        }
        query.setMaxResults(MAX_ACTIONS);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    private List<ActionNext> loadOverdueActions(Session session, int workspaceId, Date today,
            List<String> actionTypes, List<Integer> projectIds) {
        StringBuilder hql = new StringBuilder(
                "from ActionNext an where an.workspaceId = :workspaceId "
                        + "and an.nextActionDate < :today and an.nextActionStatusString = :ready "
                        + "and an.templateActionNextId is null "
                        + "and (an.templateTypeString is null or an.templateTypeString = '')");
        if (!actionTypes.isEmpty()) {
            hql.append(" and an.nextActionType in (:actionTypes)");
        }
        if (!projectIds.isEmpty()) {
            hql.append(" and an.projectId in (:projectIds)");
        }
        hql.append(" order by an.nextActionDate, an.priorityLevel desc");
        Query query = session.createQuery(hql.toString());
        query.setInteger("workspaceId", workspaceId);
        query.setParameter("today", today);
        query.setString("ready", ProjectNextActionStatus.READY.getId());
        if (!actionTypes.isEmpty()) {
            query.setParameterList("actionTypes", actionTypes);
        }
        if (!projectIds.isEmpty()) {
            query.setParameterList("projectIds", projectIds);
        }
        query.setMaxResults(MAX_ACTIONS);
        return query.list();
    }

    private Date toDate(LocalDate date) {
        return java.sql.Date.valueOf(date);
    }
}
