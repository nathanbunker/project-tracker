package org.dandeliondaily.mcp.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;

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
            LocalDate endDate, List<String> actionTypes, List<Integer> projectIds) {
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

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("scheduledActions", toActionList(session, scheduled));
        result.put("overdueActions", toActionList(session, overdue));
        return result;
    }

    private List<Map<String, Object>> toActionList(Session session, List<ActionNext> actions) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (ActionNext action : actions) {
            list.add(McpActionContextSupport.toActionMap(session, action, true));
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
