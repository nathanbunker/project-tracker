package org.dandeliondaily.mcp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dandeliondaily.shared.service.LinkedMeetingService;
import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.LinkedMeeting;

public class LinkedMeetingServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode snapshot() {
        ObjectNode node = mapper.createObjectNode();
        node.put("sourceSystem", "OUTLOOK_ICS");
        node.put("sourceNamespace", "personal-calendar");
        node.put("externalEventUid", "series-123");
        node.put("externalOccurrenceId", "2026-10-26T15:00:00Z");
        node.put("originalSubject", "Original meeting");
        node.put("scheduledStart", "2026-10-26T09:00:00-06:00");
        node.put("scheduledEnd", "2026-10-26T10:00:00-06:00");
        node.put("sourceStatus", "CONFIRMED");
        return node;
    }

    @Test
    public void normalizesOffsetsAndRetainsOriginalIdentityAndContent() {
        ObjectNode node = snapshot();
        node.put("originalDescription", "Agenda and invitation");
        node.put("sourceTimeZone", "America/Denver");
        LinkedMeeting meeting = LinkedMeetingService.parseSnapshot(node);
        Assert.assertEquals("2026-10-26T15:00:00Z", LinkedMeetingService.toMap(meeting).get("scheduledStart"));
        Assert.assertEquals("2026-10-26T15:00:00Z", meeting.getExternalOccurrenceId());
        Assert.assertEquals("Agenda and invitation", meeting.getOriginalDescription());
        Assert.assertEquals("America/Denver", meeting.getSourceTimeZone());
    }

    @Test
    public void renamedRescheduledOccurrenceKeepsSameKey() {
        LinkedMeeting first = LinkedMeetingService.parseSnapshot(snapshot());
        ObjectNode changed = snapshot();
        changed.put("originalSubject", "Renamed");
        changed.put("scheduledStart", "2026-10-27T09:00:00-06:00");
        changed.put("scheduledEnd", "2026-10-27T10:00:00-06:00");
        LinkedMeeting second = LinkedMeetingService.parseSnapshot(changed);
        Assert.assertTrue(LinkedMeetingService.sameIdentity(first, second));
        Assert.assertNotEquals(first.getScheduledStartEpochMs(), second.getScheduledStartEpochMs());
    }

    @Test
    public void rejectsInvalidSnapshots() {
        assertInvalid("scheduledStart", "2026-10-26T09:00:00");
        assertInvalid("scheduledEnd", "2026-10-26T15:00:00Z");
        assertInvalid("scheduledEnd", "2026-10-26T14:00:00Z");
        assertInvalid("joinUrl", "javascript:alert(1)");
        assertInvalid("joinUrl", "/relative");
        assertInvalid("joinUrl", "https://user:password@example.org/join");
        assertInvalid("sourceStatus", "FINALIZED");
        assertInvalid("externalOccurrenceId", "");
        ObjectNode node = snapshot();
        node.put("projectId", 123);
        expectInvalid(node);
    }

    @Test
    public void rejectsWrongTypesOversizedFieldsAndMissingRequiredFields() {
        ObjectNode node = snapshot();
        node.put("scheduledStart", 123);
        expectInvalid(node);
        node = snapshot();
        node.remove("externalEventUid");
        expectInvalid(node);
        node = snapshot();
        node.put("externalEventUid", String.join("", java.util.Collections.nCopies(256, "a")));
        expectInvalid(node);
        node = snapshot();
        node.put("joinUrl", "https://example.org/" + String.join("", java.util.Collections.nCopies(1200, "a")));
        expectInvalid(node);
    }

    @Test
    public void namespaceDefaultIsStableAndIdentityIsCaseSensitive() {
        ObjectNode node = snapshot();
        node.remove("sourceNamespace");
        LinkedMeeting first = LinkedMeetingService.parseSnapshot(node);
        Assert.assertEquals("", first.getSourceNamespace());
        node.put("sourceNamespace", "");
        Assert.assertTrue(LinkedMeetingService.sameIdentity(first, LinkedMeetingService.parseSnapshot(node)));
        node.put("externalEventUid", "SERIES-123");
        Assert.assertFalse(LinkedMeetingService.sameIdentity(first, LinkedMeetingService.parseSnapshot(node)));
    }

    @Test
    public void missingAndCancellationAreExplicitSnapshots() {
        for (String status : new String[] {"CANCELLED", "MISSING", "TENTATIVE"}) {
            ObjectNode node = snapshot();
            node.put("sourceStatus", status);
            Assert.assertEquals(status, LinkedMeetingService.parseSnapshot(node).getSourceStatus());
        }
    }

    private void assertInvalid(String field, String value) {
        ObjectNode node = snapshot();
        node.put(field, value);
        expectInvalid(node);
    }

    private void expectInvalid(ObjectNode node) {
        try {
            LinkedMeetingService.parseSnapshot(node);
            Assert.fail("Expected invalid snapshot: " + node);
        } catch (McpToolException expected) {
            Assert.assertEquals("invalid_arguments", expected.getCode());
        }
    }
}
