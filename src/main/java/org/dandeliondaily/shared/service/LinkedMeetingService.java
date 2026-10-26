package org.dandeliondaily.shared.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.doa.LinkedMeetingDao;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.LinkedMeeting;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;

import com.fasterxml.jackson.databind.JsonNode;

public class LinkedMeetingService {
    private static final Set<String> SNAPSHOT_FIELDS = new HashSet<String>(Arrays.asList(
            "sourceSystem", "sourceNamespace", "externalEventUid", "externalOccurrenceId",
            "originalSubject", "originalDescription", "organizerName", "organizerAddress",
            "scheduledStart", "scheduledEnd", "sourceTimeZone", "location", "joinUrl", "sourceStatus",
            "sourceLastModifiedAt", "sourceRevision", "sourceFingerprint"));

    public static String actionTypeError(Session session, ActionNext action, String nextActionType) {
        if (action != null && action.isWillMeet() && action.getActionNextId() > 0 && action.getWorkspaceId() != null
                && !ProjectNextActionType.WILL_MEET.equals(nextActionType)) {
            session.refresh(action, LockMode.UPGRADE);
            if (new LinkedMeetingDao(session).findByActionForUpdate(
                    action.getWorkspaceId(), action.getActionNextId()) != null) {
                return "Explicitly unlink the meeting through MCP before changing this action from WILL_MEET.";
            }
        }
        return null;
    }

    public static LinkedMeeting parseSnapshot(JsonNode node) {
        requireObject(node, "meeting");
        rejectUnknown(node, SNAPSHOT_FIELDS);
        LinkedMeeting meeting = parseIdentity(node);
        meeting.setOriginalSubject(text(node, "originalSubject", 512, true));
        meeting.setOriginalDescription(text(node, "originalDescription", 1000000, false));
        meeting.setOrganizerName(text(node, "organizerName", 255, false));
        meeting.setOrganizerAddress(text(node, "organizerAddress", 320, false));
        meeting.setScheduledStartEpochMs(instant(node, "scheduledStart", true));
        meeting.setScheduledEndEpochMs(instant(node, "scheduledEnd", true));
        if (meeting.getScheduledEndEpochMs() <= meeting.getScheduledStartEpochMs()) {
            throw invalid("scheduledEnd must be after scheduledStart.");
        }
        meeting.setSourceTimeZone(text(node, "sourceTimeZone", 128, false));
        meeting.setLocation(text(node, "location", 2048, false));
        meeting.setJoinUrl(text(node, "joinUrl", 1200, false));
        validateJoinUrl(meeting.getJoinUrl());
        meeting.setSourceStatus(text(node, "sourceStatus", 16, true));
        validateStatus(meeting.getSourceStatus());
        meeting.setSourceLastModifiedEpochMs(instant(node, "sourceLastModifiedAt", false));
        meeting.setSourceRevision(text(node, "sourceRevision", 255, false));
        meeting.setSourceFingerprint(text(node, "sourceFingerprint", 255, false));
        return meeting;
    }

    public static LinkedMeeting parseIdentity(JsonNode node) {
        requireObject(node, "identity");
        LinkedMeeting meeting = new LinkedMeeting();
        meeting.setSourceSystem(text(node, "sourceSystem", 64, true));
        String namespace = text(node, "sourceNamespace", 128, false);
        meeting.setSourceNamespace(namespace == null ? "" : namespace);
        meeting.setExternalEventUid(text(node, "externalEventUid", 255, true));
        meeting.setExternalOccurrenceId(text(node, "externalOccurrenceId", 128, true));
        return meeting;
    }

    public Prepared prepare(Session session, int workspaceId, ActionNext action, JsonNode item, boolean unlink) {
        if (action.getWorkspaceId() == null || action.getWorkspaceId().intValue() != workspaceId) {
            throw invalid("Action not found.");
        }
        if (action.isTemplate() || !ProjectNextActionType.WILL_MEET.equals(action.getNextActionType())) {
            throw invalid("A linked meeting requires a non-template-root WILL_MEET action.");
        }
        LinkedMeetingDao dao = new LinkedMeetingDao(session);
        LinkedMeeting current = action.getActionNextId() > 0
                ? dao.findByActionForUpdate(workspaceId, action.getActionNextId()) : null;
        if (current != null) {
            session.refresh(current, LockMode.UPGRADE);
        }
        Long expected = expectedVersion(item);
        if (current == null ? expected != null : expected == null || expected.longValue() != current.getVersion()) {
            throw new McpToolException("stale", "Read the current linkedMeeting.version before changing it; "
                    + "omit expectedMeetingVersion only when creating a link.");
        }
        Prepared prepared = new Prepared();
        prepared.current = current;
        prepared.action = action;
        prepared.unlink = unlink;
        if (unlink) {
            if (current == null) {
                throw invalid("Action has no linked meeting.");
            }
            return prepared;
        }
        prepared.snapshot = parseSnapshot(item.get("meeting"));
        if (current != null && !sameIdentity(current, prepared.snapshot)) {
            throw invalid("Source identity is immutable. Explicitly unlink before linking another occurrence.");
        }
        LinkedMeeting byIdentity = dao.findByIdentity(workspaceId, prepared.snapshot);
        if (byIdentity != null && (current == null
                || !byIdentity.getLinkedMeetingId().equals(current.getLinkedMeetingId()))) {
            throw invalid("Source occurrence is already attached to another action.");
        }
        if (current != null && current.getSourceLastModifiedEpochMs() != null
                && prepared.snapshot.getSourceLastModifiedEpochMs() != null
                && prepared.snapshot.getSourceLastModifiedEpochMs() < current.getSourceLastModifiedEpochMs()) {
            throw new McpToolException("stale", "Source snapshot is older than the stored sourceLastModifiedAt.");
        }
        prepared.snapshot.setWorkspaceId(workspaceId);
        return prepared;
    }

    public LinkedMeeting apply(Session session, Prepared prepared, ActionNext action) {
        if (prepared.unlink) {
            session.delete(prepared.current);
            return null;
        }
        LinkedMeeting snapshot = prepared.snapshot;
        snapshot.setActionNextId(action.getActionNextId());
        if (prepared.current == null) {
            snapshot.setLastSyncedEpochMs(System.currentTimeMillis());
            session.save(snapshot);
            return snapshot;
        }
        LinkedMeeting current = prepared.current;
        Map<String, Object> oldContent = toMap(current);
        Map<String, Object> newContent = toMap(snapshot);
        for (String field : Arrays.asList("linkedMeetingId", "version", "actionNextId", "lastSyncedAt")) {
            oldContent.remove(field);
            newContent.remove(field);
        }
        if (!oldContent.equals(newContent)) {
            copySnapshot(snapshot, current);
            current.setLastSyncedEpochMs(System.currentTimeMillis());
            session.update(current);
        }
        return current;
    }

    private void copySnapshot(LinkedMeeting source, LinkedMeeting target) {
        target.setOriginalSubject(source.getOriginalSubject());
        target.setOriginalDescription(source.getOriginalDescription());
        target.setOrganizerName(source.getOrganizerName());
        target.setOrganizerAddress(source.getOrganizerAddress());
        target.setScheduledStartEpochMs(source.getScheduledStartEpochMs());
        target.setScheduledEndEpochMs(source.getScheduledEndEpochMs());
        target.setSourceTimeZone(source.getSourceTimeZone());
        target.setLocation(source.getLocation());
        target.setJoinUrl(source.getJoinUrl());
        target.setSourceStatus(source.getSourceStatus());
        target.setSourceLastModifiedEpochMs(source.getSourceLastModifiedEpochMs());
        target.setSourceRevision(source.getSourceRevision());
        target.setSourceFingerprint(source.getSourceFingerprint());
    }

    public static Map<String, Object> toMap(LinkedMeeting meeting) {
        if (meeting == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("linkedMeetingId", meeting.getLinkedMeetingId());
        map.put("version", meeting.getVersion());
        map.put("actionNextId", meeting.getActionNextId());
        map.put("sourceSystem", meeting.getSourceSystem());
        map.put("sourceNamespace", meeting.getSourceNamespace());
        map.put("externalEventUid", meeting.getExternalEventUid());
        map.put("externalOccurrenceId", meeting.getExternalOccurrenceId());
        map.put("originalSubject", meeting.getOriginalSubject());
        map.put("originalDescription", meeting.getOriginalDescription());
        map.put("organizerName", meeting.getOrganizerName());
        map.put("organizerAddress", meeting.getOrganizerAddress());
        map.put("scheduledStart", iso(meeting.getScheduledStartEpochMs()));
        map.put("scheduledEnd", iso(meeting.getScheduledEndEpochMs()));
        map.put("sourceTimeZone", meeting.getSourceTimeZone());
        map.put("location", meeting.getLocation());
        map.put("joinUrl", meeting.getJoinUrl());
        map.put("sourceStatus", meeting.getSourceStatus());
        map.put("sourceLastModifiedAt", iso(meeting.getSourceLastModifiedEpochMs()));
        map.put("sourceRevision", meeting.getSourceRevision());
        map.put("sourceFingerprint", meeting.getSourceFingerprint());
        map.put("lastSyncedAt", iso(meeting.getLastSyncedEpochMs()));
        return map;
    }

    public static boolean sameIdentity(LinkedMeeting a, LinkedMeeting b) {
        return a.getSourceSystem().equals(b.getSourceSystem())
                && a.getSourceNamespace().equals(b.getSourceNamespace())
                && a.getExternalEventUid().equals(b.getExternalEventUid())
                && a.getExternalOccurrenceId().equals(b.getExternalOccurrenceId());
    }

    public static Long instant(JsonNode node, String field, boolean required) {
        String value = text(node, field, 64, required);
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant().toEpochMilli();
        } catch (DateTimeParseException | ArithmeticException e) {
            throw invalid(field + " must be an ISO-8601 date/time with Z or an explicit offset.");
        }
    }

    public static String text(JsonNode node, String field, int max, boolean required) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            if (required) {
                throw invalid(field + " is required.");
            }
            return null;
        }
        if (!value.isTextual() || value.textValue().length() > max
                || (required && value.textValue().trim().isEmpty())) {
            throw invalid(field + " must be " + (required ? "a nonblank" : "a") + " string of at most " + max
                    + " characters.");
        }
        return value.textValue();
    }

    public static void requireObject(JsonNode node, String name) {
        if (node == null || !node.isObject()) {
            throw invalid(name + " must be an object.");
        }
    }

    public static void rejectUnknown(JsonNode node, Set<String> fields) {
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String field = names.next();
            if (!fields.contains(field)) {
                throw invalid("Unsupported field: " + field + ".");
            }
        }
    }

    public static void validateStatus(String value) {
        if (!Arrays.asList("CONFIRMED", "TENTATIVE", "CANCELLED", "MISSING").contains(value)) {
            throw invalid("sourceStatus must be CONFIRMED, TENTATIVE, CANCELLED, or MISSING.");
        }
    }

    private static Long expectedVersion(JsonNode node) {
        if (!node.has("expectedMeetingVersion")) {
            return null;
        }
        JsonNode version = node.get("expectedMeetingVersion");
        if (!version.isIntegralNumber() || !version.canConvertToLong() || version.longValue() < 0) {
            throw invalid("expectedMeetingVersion must be a nonnegative integer, or omitted for a new link.");
        }
        return version.longValue();
    }

    private static void validateJoinUrl(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        try {
            URI uri = new URI(url);
            if (!( "https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw invalid("joinUrl must be an absolute http(s) URL without credentials.");
            }
        } catch (URISyntaxException e) {
            throw invalid("joinUrl is not a valid URL.");
        }
    }

    private static String iso(Long epochMs) {
        return epochMs == null ? null : Instant.ofEpochMilli(epochMs).toString();
    }

    private static McpToolException invalid(String message) {
        return new McpToolException("invalid_arguments", message);
    }

    public static class Prepared {
        private LinkedMeeting current;
        private LinkedMeeting snapshot;
        private ActionNext action;
        private boolean unlink;

        public LinkedMeeting getSnapshot() { return snapshot; }
        public ActionNext getAction() { return action; }
    }
}
