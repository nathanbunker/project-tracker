package org.dandeliondaily.mcp.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.shared.service.LinkedMeetingService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.LinkedMeeting;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class McpLinkedMeetingsService {
    public Map<String, Object> read(Session session, int workspaceId, WebUser user, JsonNode args) {
        LinkedMeetingService.requireObject(args, "arguments");
        LinkedMeetingService.rejectUnknown(args, new HashSet<String>(Arrays.asList("actionNextId", "sourceSystem",
                "sourceNamespace", "externalEventUid", "externalOccurrenceId", "date", "rangeStart", "rangeEnd",
                "sourceStatus", "limit", "offset")));
        int limit = integer(args, "limit", 100, 1, 200);
        int offset = integer(args, "offset", 0, 0, Integer.MAX_VALUE);
        Map<String, Object> parameters = new LinkedHashMap<String, Object>();
        parameters.put("workspaceId", workspaceId);
        StringBuilder where = new StringBuilder(" where m.workspaceId = :workspaceId "
                + "and a.workspaceId = :workspaceId and p.workspaceId = :workspaceId");
        if (args.has("actionNextId")) {
            parameters.put("actionNextId", integer(args, "actionNextId", 0, 1, Integer.MAX_VALUE));
            where.append(" and m.actionNextId = :actionNextId");
        }
        boolean series = args.has("sourceSystem") || args.has("externalEventUid")
                || args.has("sourceNamespace") || args.has("externalOccurrenceId");
        if (series) {
            parameters.put("sourceSystem", LinkedMeetingService.text(args, "sourceSystem", 64, true));
            String namespace = LinkedMeetingService.text(args, "sourceNamespace", 128, false);
            parameters.put("sourceNamespace", namespace == null ? "" : namespace);
            parameters.put("externalEventUid", LinkedMeetingService.text(args, "externalEventUid", 255, true));
            where.append(" and m.sourceSystem = :sourceSystem and m.sourceNamespace = :sourceNamespace "
                    + "and m.externalEventUid = :externalEventUid");
        }
        String seriesWhere = where.toString();
        Map<String, Object> seriesParameters = new LinkedHashMap<String, Object>(parameters);
        if (args.has("externalOccurrenceId")) {
            parameters.put("externalOccurrenceId",
                    LinkedMeetingService.text(args, "externalOccurrenceId", 128, true));
            where.append(" and m.externalOccurrenceId = :externalOccurrenceId");
        }
        Long start = null;
        Long end = null;
        if (args.has("date")) {
            if (args.has("rangeStart") || args.has("rangeEnd")) {
                throw invalid("Use either date or rangeStart/rangeEnd, not both.");
            }
            try {
                LocalDate date = LocalDate.parse(LinkedMeetingService.text(args, "date", 10, true));
                start = date.atStartOfDay(user.getZoneId()).toInstant().toEpochMilli();
                end = date.plusDays(1).atStartOfDay(user.getZoneId()).toInstant().toEpochMilli();
            } catch (DateTimeParseException e) {
                throw invalid("date must be yyyy-MM-dd.");
            }
        } else if (args.has("rangeStart") || args.has("rangeEnd")) {
            start = LinkedMeetingService.instant(args, "rangeStart", true);
            end = LinkedMeetingService.instant(args, "rangeEnd", true);
        }
        if (start != null) {
            if (end <= start) {
                throw invalid("rangeEnd must be after rangeStart.");
            }
            where.append(" and m.scheduledStartEpochMs < :rangeEnd and m.scheduledEndEpochMs > :rangeStart");
            parameters.put("rangeStart", start);
            parameters.put("rangeEnd", end);
        }
        if (args.has("sourceStatus")) {
            String status = LinkedMeetingService.text(args, "sourceStatus", 16, true);
            LinkedMeetingService.validateStatus(status);
            parameters.put("sourceStatus", status);
            where.append(" and m.sourceStatus = :sourceStatus");
        }
        String from = " from LinkedMeeting m, ActionNext a, Project p";
        String joins = " and a.actionNextId = m.actionNextId and p.projectId = a.projectId";
        Query query = bind(session.createQuery("select m, a, p" + from + where + joins
                + " order by m.scheduledStartEpochMs, m.linkedMeetingId"), parameters);
        query.setFirstResult(offset);
        query.setMaxResults(limit + 1);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.list();
        boolean hasMore = rows.size() > limit;
        List<Map<String, Object>> meetings = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < Math.min(rows.size(), limit); i++) {
            Object[] row = rows.get(i);
            Map<String, Object> meeting = LinkedMeetingService.toMap((LinkedMeeting) row[0]);
            ActionNext action = (ActionNext) row[1];
            Project project = (Project) row[2];
            meeting.put("projectId", project.getProjectId());
            meeting.put("projectName", project.getProjectName());
            meeting.put("actionDescription", action.getNextDescription());
            meeting.put("actionStatus", action.getNextActionStatusString());
            meeting.put("actionAsOf", McpActionContextSupport.toIso(action.getNextChangeDate()));
            meetings.add(meeting);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("meetings", meetings);
        result.put("hasMore", hasMore);
        result.put("nextOffset", hasMore ? offset + limit : null);
        if (series) {
            Query projectsQuery = bind(session.createQuery("select distinct p.projectId, p.projectName"
                    + from + seriesWhere + joins + " order by p.projectId"), seriesParameters);
            @SuppressWarnings("unchecked")
            List<Object[]> projects = projectsQuery.list();
            List<Map<String, Object>> candidates = new ArrayList<Map<String, Object>>();
            for (Object[] project : projects) {
                Map<String, Object> candidate = new LinkedHashMap<String, Object>();
                candidate.put("projectId", project[0]);
                candidate.put("projectName", project[1]);
                candidates.add(candidate);
            }
            result.put("seriesProjectCandidates", candidates);
            result.put("suggestedProjectId", candidates.size() == 1 ? candidates.get(0).get("projectId") : null);
        }
        return result;
    }

    private Query bind(Query query, Map<String, Object> parameters) {
        for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
            query.setParameter(parameter.getKey(), parameter.getValue());
        }
        return query;
    }

    private int integer(JsonNode args, String field, int defaultValue, int min, int max) {
        if (!args.has(field)) {
            return defaultValue;
        }
        JsonNode value = args.get(field);
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < min || value.intValue() > max) {
            throw invalid(field + " must be an integer between " + min + " and " + max + ".");
        }
        return value.intValue();
    }

    private McpToolException invalid(String message) {
        return new McpToolException("invalid_arguments", message);
    }
}
