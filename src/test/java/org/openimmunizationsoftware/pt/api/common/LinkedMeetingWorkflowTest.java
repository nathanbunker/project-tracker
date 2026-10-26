package org.openimmunizationsoftware.pt.api.common;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

import javax.ws.rs.core.Response;
import org.dandeliondaily.mcp.service.McpActionContextSupport;
import org.dandeliondaily.shared.service.LinkedMeetingService;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolRegistry;
import org.openimmunizationsoftware.pt.api.v1.resource.McpResource;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionSet;
import org.openimmunizationsoftware.pt.model.LinkedMeeting;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LinkedMeetingWorkflowTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Integer, ActionNext> actions = new LinkedHashMap<Integer, ActionNext>();
    private final Map<Integer, LinkedMeeting> meetings = new LinkedHashMap<Integer, LinkedMeeting>();
    private Session session;
    private WebUser user;
    private Project project;
    private int saved;
    private int updates;
    private int flushes;
    private int rollbacks;
    private boolean failMeetingSave;

    @Before
    public void setUp() {
        user = new WebUser();
        user.setWebUserId(1);
        user.setUsername("tester");
        user.setContactId(2);
        user.setProjectContact(new ProjectContact());
        project = new Project();
        project.setProjectId(3);
        project.setWorkspaceId(4);
        project.setProjectName("Project");
        ActionNext action = action(5, 4);
        actions.put(5, action);
        Transaction transaction = (Transaction) Proxy.newProxyInstance(Transaction.class.getClassLoader(),
                new Class<?>[] {Transaction.class}, (proxy, method, args) -> {
                    if ("isActive".equals(method.getName())) {
                        return true;
                    }
                    if ("rollback".equals(method.getName())) {
                        rollbacks++;
                        actions.entrySet().removeIf(entry -> entry.getKey() >= 100);
                        meetings.clear();
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        session = (Session) Proxy.newProxyInstance(Session.class.getClassLoader(),
                new Class<?>[] {Session.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                    case "getTransaction": return transaction;
                    case "get":
                        if (args[0] == ActionNext.class) {
                            if (args.length == 3) { assertEquals(LockMode.UPGRADE, args[2]); }
                            return actions.get(args[1]);
                        }
                        if (args[0] == Project.class) { return project; }
                        if (args[0] == ProjectContact.class) { return user.getProjectContact(); }
                        throw new UnsupportedOperationException("get " + args[0]);
                    case "refresh":
                        assertEquals(LockMode.UPGRADE, args[1]);
                        return null;
                    case "save":
                        saved++;
                        if (args[0] instanceof LinkedMeeting) {
                            if (failMeetingSave) {
                                throw new McpToolException("conflict", "Simulated uniqueness conflict.");
                            }
                            LinkedMeeting meeting = (LinkedMeeting) args[0];
                            meeting.setLinkedMeetingId(200 + meetings.size());
                            meetings.put(meeting.getLinkedMeetingId(), meeting);
                            return meeting.getLinkedMeetingId();
                        }
                        if (args[0] instanceof ActionNext) {
                            ActionNext created = (ActionNext) args[0];
                            created.setActionNextId(100 + actions.size());
                            actions.put(created.getActionNextId(), created);
                            return created.getActionNextId();
                        }
                        if (args[0] instanceof ActionSet) {
                            ((ActionSet) args[0]).setActionSetId(9);
                            return 9;
                        }
                        return 1;
                    case "update":
                        updates++;
                        if (args[0] instanceof LinkedMeeting) {
                            LinkedMeeting meeting = (LinkedMeeting) args[0];
                            meeting.setVersion(meeting.getVersion() + 1);
                        }
                        return null;
                    case "delete":
                        LinkedMeeting removed = (LinkedMeeting) args[0];
                        meetings.remove(removed.getLinkedMeetingId());
                        return null;
                    case "flush": flushes++; return null;
                    case "createQuery": return query((String) args[0]);
                    default: throw new UnsupportedOperationException(method.getName());
                    }
                });
        HibernateRequestContext.set(session);
        ApiRequestContext.set(new ApiRequestContext.ApiClientInfo(1, "tester", 4, "test-agent"));
    }

    @After
    public void clear() {
        HibernateRequestContext.clear();
        ApiRequestContext.clear();
    }

    private ActionNext action(int id, int workspace) {
        ActionNext action = new ActionNext();
        action.setActionNextId(id);
        action.setWorkspaceId(workspace);
        action.setProject(project);
        action.setNextActionType(ProjectNextActionType.WILL_MEET);
        action.setNextActionStatus(ProjectNextActionStatus.READY);
        action.setNextDescription("Attend part of meeting");
        action.setNextTimeEstimate(30);
        action.setCompletionOrder(7);
        action.setNextActionDate(Date.from(Instant.parse("2026-10-26T00:00:00Z")));
        action.setNextChangeDate(Date.from(Instant.parse("2026-10-26T12:00:00Z")));
        return action;
    }

    private ObjectNode snapshot(String occurrence) {
        ObjectNode node = mapper.createObjectNode();
        node.put("sourceSystem", "OUTLOOK_ICS");
        node.put("sourceNamespace", "calendar");
        node.put("externalEventUid", "series");
        node.put("externalOccurrenceId", occurrence);
        node.put("originalSubject", "Meeting");
        node.put("originalDescription", "Original invitation");
        node.put("scheduledStart", "2026-10-26T15:00:00Z");
        node.put("scheduledEnd", "2026-10-26T16:00:00Z");
        node.put("joinUrl", "https://example.org/join");
        node.put("sourceStatus", "CONFIRMED");
        return node;
    }

    private ObjectNode upsert(String occurrence) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "upsert_linked_meeting");
        node.put("actionNextId", 5);
        node.put("asOf", "2026-10-26T12:00:00Z");
        node.set("meeting", snapshot(occurrence));
        return node;
    }

    private JsonNode apply(ObjectNode... changes) {
        ObjectNode args = mapper.createObjectNode();
        args.set("changes", mapper.valueToTree(Arrays.asList(changes)));
        return call("apply_changes", args).path("result").path("structuredContent");
    }

    private JsonNode call(String name, ObjectNode arguments) {
        ObjectNode request = mapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "tools/call");
        request.putObject("params").put("name", name).set("arguments", arguments);
        Response response = new McpResource().handle(request.toString());
        assertEquals(200, response.getStatus());
        return mapper.valueToTree(response.getEntity());
    }

    @Test
    public void syncRenameRescheduleCancelAndReadThroughMcpPreservesAction() {
        ObjectNode change = upsert("occurrence");
        assertTrue(apply(change).path("applied").asBoolean());
        assertEquals(1, meetings.size());
        LinkedMeeting meeting = meetings.values().iterator().next();
        int writes = updates;
        long lastSynced = meeting.getLastSyncedEpochMs();
        change.put("expectedMeetingVersion", meeting.getVersion());
        assertTrue(apply(change).path("applied").asBoolean());
        assertEquals(writes, updates);
        assertEquals(lastSynced, meeting.getLastSyncedEpochMs());
        ObjectNode source = (ObjectNode) change.get("meeting");
        source.put("originalSubject", "Renamed");
        source.put("scheduledStart", "2026-10-27T15:00:00Z");
        source.put("scheduledEnd", "2026-10-27T16:00:00Z");
        source.put("sourceStatus", "CANCELLED");
        assertTrue(apply(change).path("applied").asBoolean());
        assertEquals(1, meetings.size());
        assertEquals("Renamed", meeting.getOriginalSubject());
        assertEquals("CANCELLED", meeting.getSourceStatus());
        assertEquals("occurrence", meeting.getExternalOccurrenceId());
        assertEquals(1, meeting.getVersion());
        ActionNext action = actions.get(5);
        assertEquals("Attend part of meeting", action.getNextDescription());
        assertEquals(Integer.valueOf(30), action.getNextTimeEstimate());
        assertEquals(7, action.getCompletionOrder());
        assertEquals(ProjectNextActionStatus.READY, action.getNextActionStatus());
        assertEquals("2026-10-26T00:00:00Z", McpActionContextSupport.toIso(action.getNextActionDate()));
        assertEquals("2026-10-26T12:00:00Z", McpActionContextSupport.toIso(action.getNextChangeDate()));
        Map<String, Object> context = McpActionContextSupport.toActionMap(session, action, false);
        assertEquals("CANCELLED", ((Map<?, ?>) context.get("linkedMeeting")).get("sourceStatus"));
        ObjectNode filter = mapper.createObjectNode().put("sourceSystem", "OUTLOOK_ICS")
                .put("sourceNamespace", "calendar").put("externalEventUid", "series");
        JsonNode result = call("get_linked_meetings", filter).path("result").path("structuredContent");
        assertEquals(1, result.path("meetings").size());
        assertEquals(3, result.path("suggestedProjectId").asInt());
        assertEquals("CANCELLED", result.path("meetings").get(0).path("sourceStatus").asText());
        assertTrue(flushes > 0);
    }

    @Test
    public void staleMeetingOrActionRejectsWholeBatchBeforeWrites() {
        apply(upsert("occurrence"));
        int writes = saved;
        ObjectNode update = mapper.createObjectNode().put("type", "update_action").put("actionNextId", 5)
                .put("asOf", "2026-10-26T12:00:00Z").put("description", "Do not apply");
        ObjectNode stale = upsert("occurrence").put("expectedMeetingVersion", 999);
        assertFalse(apply(update, stale).path("applied").asBoolean());
        assertEquals(writes, saved);
        assertEquals("Attend part of meeting", actions.get(5).getNextDescription());
        assertFalse(apply(upsert("occurrence")).path("applied").asBoolean());
        ObjectNode staleAction = upsert("occurrence").put("asOf", "2026-10-25T12:00:00Z")
                .put("expectedMeetingVersion", 0);
        assertFalse(apply(staleAction).path("applied").asBoolean());
    }

    @Test
    public void createAndLinkAreAtomicAndSupportEarlierIndex() {
        ObjectNode create = mapper.createObjectNode().put("type", "create_action").put("projectId", 3)
                .put("description", "New meeting").put("nextActionType", ProjectNextActionType.WILL_MEET);
        ObjectNode link = upsert("new");
        link.remove(Arrays.asList("actionNextId", "asOf"));
        link.put("actionChangeIndex", 0);
        JsonNode result = apply(create, link);
        assertTrue(result.path("applied").asBoolean());
        int created = result.path("results").get(0).path("createdActionNextId").asInt();
        assertTrue(created >= 100);
        assertEquals(created, meetings.values().iterator().next().getActionNextId());
    }

    @Test
    public void runtimeMeetingFailureRollsBackCreatedActionThroughResource() {
        failMeetingSave = true;
        ObjectNode create = mapper.createObjectNode().put("type", "create_action").put("projectId", 3)
                .put("description", "New meeting").put("nextActionType", ProjectNextActionType.WILL_MEET);
        ObjectNode link = upsert("new");
        link.remove(Arrays.asList("actionNextId", "asOf"));
        link.put("actionChangeIndex", 0);
        ObjectNode arguments = mapper.createObjectNode();
        arguments.set("changes", mapper.valueToTree(Arrays.asList(create, link)));
        JsonNode result = call("apply_changes", arguments);
        assertTrue(result.path("result").path("isError").asBoolean());
        assertEquals(1, rollbacks);
        assertEquals(1, actions.size());
        assertTrue(meetings.isEmpty());
    }

    @Test
    public void sourceAndActionCannotBeReassignedOrDuplicated() {
        assertTrue(apply(upsert("first")).path("applied").asBoolean());
        assertFalse(apply(upsert("second").put("expectedMeetingVersion", 0)).path("applied").asBoolean());
        actions.put(6, action(6, 4));
        assertFalse(apply(upsert("first").put("actionNextId", 6)).path("applied").asBoolean());
        assertEquals(1, meetings.size());
    }

    @Test
    public void duplicateAndConflictingBatchLinksAreRejected() {
        ObjectNode first = upsert("first");
        assertFalse(apply(first, first).path("applied").asBoolean());
        assertTrue(meetings.isEmpty());
        ObjectNode edit = mapper.createObjectNode().put("type", "update_action").put("actionNextId", 5)
                .put("asOf", "2026-10-26T12:00:00Z").put("nextActionType", ProjectNextActionType.WILL);
        assertFalse(apply(first, edit).path("applied").asBoolean());
        assertEquals(ProjectNextActionType.WILL_MEET, actions.get(5).getNextActionType());
    }

    @Test
    public void rejectsNonMeetingActionsAndGeneralSourceEdits() {
        actions.get(5).setNextActionType(ProjectNextActionType.WILL);
        assertFalse(apply(upsert("occurrence")).path("applied").asBoolean());
        ObjectNode edit = mapper.createObjectNode().put("type", "update_action").put("actionNextId", 5)
                .put("asOf", "2026-10-26T12:00:00Z").put("description", "Not applied");
        edit.set("linkedMeeting", snapshot("occurrence"));
        assertFalse(apply(edit).path("applied").asBoolean());
        assertEquals("Attend part of meeting", actions.get(5).getNextDescription());
    }

    @Test
    public void workspaceIsolationForReadsAndWrites() {
        actions.put(6, action(6, 7));
        assertFalse(apply(upsert("foreign").put("actionNextId", 6)).path("applied").asBoolean());
        LinkedMeeting foreign = LinkedMeetingService.parseSnapshot(snapshot("foreign"));
        foreign.setWorkspaceId(7);
        foreign.setActionNextId(6);
        foreign.setLinkedMeetingId(99);
        meetings.put(99, foreign);
        JsonNode result = call("get_linked_meetings", mapper.createObjectNode()).path("result").path("structuredContent");
        assertEquals(0, result.path("meetings").size());
        assertTrue(apply(upsert("foreign")).path("applied").asBoolean());
        assertEquals(2, meetings.size());
    }

    @Test
    public void explicitUnlinkPreservesActionAndRequiresCurrentVersion() {
        apply(upsert("occurrence"));
        ObjectNode unlink = mapper.createObjectNode().put("type", "unlink_meeting").put("actionNextId", 5)
                .put("asOf", "2026-10-26T12:00:00Z");
        assertFalse(apply(unlink).path("applied").asBoolean());
        assertTrue(apply(unlink.put("expectedMeetingVersion", 0)).path("applied").asBoolean());
        assertTrue(meetings.isEmpty());
        assertEquals(ProjectNextActionStatus.READY, actions.get(5).getNextActionStatus());
    }

    @Test
    public void olderSourceSnapshotIsRejectedEvenWithCurrentMeetingVersion() {
        ObjectNode first = upsert("occurrence");
        ((ObjectNode) first.get("meeting")).put("sourceLastModifiedAt", "2026-10-26T12:00:00Z");
        assertTrue(apply(first).path("applied").asBoolean());
        first.put("expectedMeetingVersion", 0);
        ((ObjectNode) first.get("meeting")).put("sourceLastModifiedAt", "2026-10-25T12:00:00Z");
        assertFalse(apply(first).path("applied").asBoolean());
    }

    @Test
    public void meetingSnapshotCanBeSynchronizedOnCompletedActionWithoutReopeningIt() {
        actions.get(5).setNextActionStatus(ProjectNextActionStatus.COMPLETED);
        assertTrue(apply(upsert("occurrence")).path("applied").asBoolean());
        assertEquals(ProjectNextActionStatus.COMPLETED, actions.get(5).getNextActionStatus());
        assertEquals(1, meetings.size());
    }

    @Test
    public void newRecurringOccurrenceReturnsPriorProjectEvenWhenNotInReadRange() {
        apply(upsert("prior-occurrence"));
        ObjectNode filter = mapper.createObjectNode().put("sourceSystem", "OUTLOOK_ICS")
                .put("sourceNamespace", "calendar").put("externalEventUid", "series")
                .put("externalOccurrenceId", "new-occurrence")
                .put("rangeStart", "2026-11-01T00:00:00Z").put("rangeEnd", "2026-11-02T00:00:00Z");
        JsonNode result = read(filter);
        assertEquals(0, result.path("meetings").size());
        assertEquals(3, result.path("suggestedProjectId").asInt());
    }

    @Test
    public void actionCancellationRetainsMeetingAndTypeChangeRequiresUnlink() {
        apply(upsert("occurrence"));
        ObjectNode edit = mapper.createObjectNode().put("type", "update_action").put("actionNextId", 5)
                .put("asOf", "2026-10-26T12:00:00Z").put("nextActionType", ProjectNextActionType.WILL);
        assertFalse(apply(edit).path("applied").asBoolean());
        ObjectNode cancel = mapper.createObjectNode().put("type", "remove_action").put("actionNextId", 5)
                .put("asOf", "2026-10-26T12:00:00Z");
        assertTrue(apply(cancel).path("applied").asBoolean());
        assertEquals(1, meetings.size());
        assertEquals("CONFIRMED", meetings.values().iterator().next().getSourceStatus());
    }

    @Test
    public void sharedTypeGuardLeavesOrdinaryActionsAndMeetingEditsAvailable() {
        ActionNext action = actions.get(5);
        assertNull(LinkedMeetingService.actionTypeError(session, action, ProjectNextActionType.WILL));
        apply(upsert("occurrence"));
        assertNull(LinkedMeetingService.actionTypeError(session, action, ProjectNextActionType.WILL_MEET));
        assertNotNull(LinkedMeetingService.actionTypeError(session, action, ProjectNextActionType.WILL));
        assertEquals(ProjectNextActionType.WILL_MEET, action.getNextActionType());
    }

    @Test
    public void rangeBoundariesStatusAndPaginationAreExplicit() {
        apply(upsert("occurrence"));
        ObjectNode filter = mapper.createObjectNode().put("rangeStart", "2026-10-26T16:00:00Z")
                .put("rangeEnd", "2026-10-26T17:00:00Z");
        assertEquals(0, read(filter).path("meetings").size());
        filter.put("rangeStart", "2026-10-26T15:59:00Z");
        assertEquals(1, read(filter).path("meetings").size());
        filter.put("sourceStatus", "MISSING");
        assertEquals(0, read(filter).path("meetings").size());
        actions.put(6, action(6, 4));
        apply(upsert("next").put("actionNextId", 6));
        JsonNode first = read(mapper.createObjectNode().put("limit", 1));
        assertTrue(first.path("hasMore").asBoolean());
        assertEquals(1, first.path("nextOffset").asInt());
        assertEquals(1, read(mapper.createObjectNode().put("limit", 1).put("offset", 1)).path("meetings").size());
    }

    @Test
    public void rejectsMalformedReadFiltersAndSnapshotFields() {
        assertTrue(call("get_linked_meetings", mapper.createObjectNode().put("limit", 0))
                .path("result").path("isError").asBoolean());
        assertTrue(call("get_linked_meetings", mapper.createObjectNode().put("externalEventUid", "series"))
                .path("result").path("isError").asBoolean());
        assertTrue(call("get_linked_meetings", mapper.createObjectNode().put("rangeStart", "2026-10-26T15:00:00Z"))
                .path("result").path("isError").asBoolean());
        ObjectNode change = upsert("occurrence");
        ((ObjectNode) change.get("meeting")).put("scheduledStart", "2026-10-26T15:00:00");
        assertFalse(apply(change).path("applied").asBoolean());
    }

    @Test
    public void hibernateRecognizesVersionedMappingAndRegistryExposesContracts() {
        Configuration configuration = new Configuration().configure();
        configuration.buildMappings();
        org.hibernate.mapping.PersistentClass mapping = configuration.getClassMapping(LinkedMeeting.class.getName());
        assertNotNull(mapping);
        assertTrue(mapping.isVersioned());
        assertEquals("linked_meeting", mapping.getTable().getName());
        McpToolRegistry registry = new McpToolRegistry();
        assertEquals("get_linked_meetings", registry.require("get_linked_meetings").getName());
        String schema = mapper.valueToTree(registry.require("apply_changes").getInputSchema()).toString();
        assertTrue(schema.contains("upsert_linked_meeting"));
        assertTrue(schema.contains("expectedMeetingVersion"));
    }

    private JsonNode read(ObjectNode filter) {
        return call("get_linked_meetings", filter).path("result").path("structuredContent");
    }

    private Query query(String hql) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        int[] pagination = {0, Integer.MAX_VALUE};
        return (Query) Proxy.newProxyInstance(Query.class.getClassLoader(), new Class<?>[] {Query.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("setMaxResults".equals(name)) { pagination[1] = (Integer) args[0]; return proxy; }
                    if ("setFirstResult".equals(name)) { pagination[0] = (Integer) args[0]; return proxy; }
                    if (name.startsWith("set")) { params.put((String) args[0], args[1]); return proxy; }
                    if ("uniqueResult".equals(name)) {
                        if (hql.contains("WebUser")) { return user; }
                        if (hql.contains("from Project")) { return project; }
                        List<LinkedMeeting> matched = matching(params);
                        return matched.isEmpty() ? null : matched.get(0);
                    }
                    if ("list".equals(name)) {
                        List<LinkedMeeting> matched = matching(params);
                        List<Object[]> rows = new ArrayList<Object[]>();
                        if (hql.startsWith("select distinct")) {
                            if (!matched.isEmpty()) { rows.add(new Object[] {3, "Project"}); }
                            return rows;
                        }
                        for (LinkedMeeting meeting : matched) {
                            rows.add(new Object[] {meeting, actions.get(meeting.getActionNextId()), project});
                        }
                        return rows.subList(Math.min(pagination[0], rows.size()),
                                (int) Math.min((long) pagination[0] + pagination[1], rows.size()));
                    }
                    throw new UnsupportedOperationException(name + ": " + hql);
                });
    }

    private List<LinkedMeeting> matching(Map<String, Object> params) {
        List<LinkedMeeting> result = new ArrayList<LinkedMeeting>();
        for (LinkedMeeting meeting : meetings.values()) {
            if (meeting.getWorkspaceId() != ((Number) params.get("workspaceId")).intValue()) { continue; }
            if (params.containsKey("actionNextId")
                    && meeting.getActionNextId() != ((Number) params.get("actionNextId")).intValue()) { continue; }
            if (params.containsKey("sourceSystem") && !meeting.getSourceSystem().equals(params.get("sourceSystem"))) { continue; }
            if (params.containsKey("sourceNamespace") && !meeting.getSourceNamespace().equals(params.get("sourceNamespace"))) { continue; }
            if (params.containsKey("externalEventUid") && !meeting.getExternalEventUid().equals(params.get("externalEventUid"))) { continue; }
            if (params.containsKey("externalOccurrenceId")
                    && !meeting.getExternalOccurrenceId().equals(params.get("externalOccurrenceId"))) { continue; }
            if (params.containsKey("sourceStatus") && !meeting.getSourceStatus().equals(params.get("sourceStatus"))) { continue; }
            if (params.containsKey("rangeStart") && meeting.getScheduledEndEpochMs() <= (Long) params.get("rangeStart")) { continue; }
            if (params.containsKey("rangeEnd") && meeting.getScheduledStartEpochMs() >= (Long) params.get("rangeEnd")) { continue; }
            result.add(meeting);
        }
        Collections.sort(result, Comparator.comparingLong(LinkedMeeting::getScheduledStartEpochMs)
                .thenComparing(LinkedMeeting::getLinkedMeetingId));
        return result;
    }
}
