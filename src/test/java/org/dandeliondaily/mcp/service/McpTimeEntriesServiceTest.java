package org.dandeliondaily.mcp.service;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.dandeliondaily.mcp.service.McpTimeEntriesService.Prepared;
import org.dandeliondaily.mcp.service.McpTimeEntriesService.Span;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillCodeId;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class McpTimeEntriesServiceTest {

    private static final ZoneId DENVER = ZoneId.of("America/Denver");
    private static final LocalDate DAY = LocalDate.of(2026, 9, 29);
    private static final int WORKSPACE = 5;

    private final ObjectMapper mapper = new ObjectMapper();
    private WebUser webUser;
    private Project otherProject;
    private BillCode otherBillCode;
    private final List<Object> updated = new ArrayList<Object>();
    private final List<Object> saved = new ArrayList<Object>();

    @Before
    public void setUp() {
        webUser = new WebUser();
        webUser.setWebUserId(9999);
        webUser.setUsername("tester");
        webUser.setTimeZone(TimeZone.getTimeZone(DENVER));

        otherProject = new Project();
        otherProject.setProjectId(77);
        otherProject.setProjectName("Other");
        otherProject.setWorkspaceId(Integer.valueOf(WORKSPACE));
        otherProject.setBillCode("OTH");
        otherBillCode = new BillCode();
        otherBillCode.setId(new BillCodeId(Integer.valueOf(WORKSPACE), "OTH"));
        otherBillCode.setBillable("Y");
    }

    // ---- pure helpers ----

    @Test
    public void currentEntryIsTodaysLatestStartOnly() {
        BillEntry early = entry(1, 10, at(9, 0), at(10, 0));
        BillEntry late = entry(2, 10, at(10, 0), at(10, 30));
        Assert.assertSame(late, McpTimeEntriesService.currentEntry(Arrays.asList(early, late), true));
        Assert.assertNull(McpTimeEntriesService.currentEntry(Arrays.asList(early, late), false));
        Assert.assertNull(McpTimeEntriesService.currentEntry(Collections.<BillEntry>emptyList(), true));
    }

    @Test
    public void zeroLengthSpansNeverOverlap() {
        List<Span> spans = Arrays.asList(
                new Span("a", at(9, 0), at(10, 0)),
                new Span("zero", at(9, 30), at(9, 30)),
                new Span("b", at(10, 0), at(11, 0)));
        Assert.assertNull(McpTimeEntriesService.findOverlap(spans, webUser));
    }

    @Test
    public void overlapNamesBothSpans() {
        List<Span> spans = Arrays.asList(
                new Span("a", at(9, 0), at(10, 0)),
                new Span("b", at(9, 50), at(10, 30)));
        String overlap = McpTimeEntriesService.findOverlap(spans, webUser);
        Assert.assertEquals("a (09:00-10:00) and b (09:50-10:30)", overlap);
    }

    @Test
    public void overlapIsFoundAcrossANestedSpan() {
        List<Span> spans = Arrays.asList(
                new Span("long", at(9, 0), at(12, 0)),
                new Span("short", at(9, 30), at(9, 45)),
                new Span("late", at(11, 0), at(11, 30)));
        Assert.assertNotNull(McpTimeEntriesService.findOverlap(spans, webUser));
    }

    @Test
    public void duplicateChangeToSameEntryIsRejected() {
        BillEntry e = entry(5, 10, at(9, 0), at(10, 0));
        Prepared first = prepared(0, "adjust", e);
        Prepared second = prepared(1, "adjust", e);
        Prepared reassign = prepared(2, "reassign", e);
        Assert.assertNull(McpTimeEntriesService.findDuplicateTarget(Arrays.asList(first, reassign)));
        Assert.assertNotNull(McpTimeEntriesService.findDuplicateTarget(Arrays.asList(first, second)));
    }

    // ---- full batch flow against a stand-in session ----

    @Test
    public void movesWrongTimeToNeighborsAndZeroesTheWrongEntry() throws Exception {
        BillEntry previous = entry(1, 10, at(9, 0), at(9, 10));
        BillEntry wrong = entry(2, 20, at(9, 10), at(9, 30));
        BillEntry next = entry(3, 30, at(9, 30), at(10, 0));
        List<BillEntry> day = new ArrayList<BillEntry>(Arrays.asList(previous, wrong, next));

        Map<String, Object> result = new McpTimeEntriesService().updateDay(session(day), WORKSPACE, webUser, DAY,
                changes("[{\"type\":\"adjust\",\"billId\":1,\"expectedStart\":\"09:00\",\"expectedEnd\":\"09:10\","
                        + "\"start\":\"09:00\",\"end\":\"09:20\"},"
                        + "{\"type\":\"adjust\",\"billId\":2,\"expectedStart\":\"09:10\",\"expectedEnd\":\"09:30\","
                        + "\"start\":\"09:20\",\"end\":\"09:20\"},"
                        + "{\"type\":\"adjust\",\"billId\":3,\"expectedStart\":\"09:30\",\"expectedEnd\":\"10:00\","
                        + "\"start\":\"09:20\",\"end\":\"10:00\"}]"),
                "wrong task after completing");

        Assert.assertEquals(Boolean.TRUE, result.get("applied"));
        Assert.assertEquals(at(9, 20), previous.getEndTime());
        Assert.assertEquals(at(9, 20), wrong.getStartTime());
        Assert.assertEquals(at(9, 20), wrong.getEndTime());
        Assert.assertEquals(0, wrong.getBillMins().intValue());
        Assert.assertEquals(at(9, 20), next.getStartTime());
        Assert.assertEquals(20, previous.getBillMins().intValue());
        Assert.assertEquals(40, next.getBillMins().intValue());
        Assert.assertTrue(((List<?>) result.get("normalizationAdjustments")).isEmpty());
    }

    @Test
    public void rejectsOverlapWithAnUntouchedEntryAndChangesNothing() throws Exception {
        BillEntry a = entry(1, 10, at(9, 0), at(9, 30));
        BillEntry b = entry(2, 20, at(9, 30), at(10, 0));
        List<BillEntry> day = new ArrayList<BillEntry>(Arrays.asList(a, b));

        Map<String, Object> result = new McpTimeEntriesService().updateDay(session(day), WORKSPACE, webUser, DAY,
                changes("[{\"type\":\"adjust\",\"billId\":1,\"expectedStart\":\"09:00\",\"expectedEnd\":\"09:30\","
                        + "\"start\":\"09:00\",\"end\":\"09:45\"}]"),
                null);

        Assert.assertEquals(Boolean.FALSE, result.get("applied"));
        Assert.assertTrue(String.valueOf(result.get("message")).contains("overlapping"));
        Assert.assertEquals(at(9, 30), a.getEndTime());
        Assert.assertTrue(updated.isEmpty());
    }

    @Test
    public void rejectsStaleExpectedTimes() throws Exception {
        BillEntry a = entry(1, 10, at(9, 0), at(9, 30));
        List<BillEntry> day = new ArrayList<BillEntry>(Arrays.asList(a));

        Map<String, Object> result = new McpTimeEntriesService().updateDay(session(day), WORKSPACE, webUser, DAY,
                changes("[{\"type\":\"adjust\",\"billId\":1,\"expectedStart\":\"09:00\",\"expectedEnd\":\"09:20\","
                        + "\"start\":\"09:00\",\"end\":\"09:10\"}]"),
                null);

        Assert.assertEquals(Boolean.FALSE, result.get("applied"));
        Map<?, ?> item = (Map<?, ?>) ((List<?>) result.get("results")).get(0);
        Assert.assertTrue(String.valueOf(item.get("reason")).contains("has changed"));
        Assert.assertEquals(at(9, 30), a.getEndTime());
    }

    @Test
    public void reassignMovesEntryToAnotherProjectWithItsBillCode() throws Exception {
        BillEntry a = entry(1, 10, at(9, 0), at(9, 30));
        a.setBillCode("OLD");
        List<BillEntry> day = new ArrayList<BillEntry>(Arrays.asList(a));

        Map<String, Object> result = new McpTimeEntriesService().updateDay(session(day), WORKSPACE, webUser, DAY,
                changes("[{\"type\":\"reassign\",\"billId\":1,\"expectedStart\":\"09:00\",\"expectedEnd\":\"09:30\","
                        + "\"projectId\":77}]"),
                null);

        Assert.assertEquals(Boolean.TRUE, result.get("applied"));
        Assert.assertEquals(77, a.getProjectId());
        Assert.assertEquals("OTH", a.getBillCode());
        Assert.assertEquals("Y", a.getBillable());
        Assert.assertNull(a.getAction());
        Assert.assertEquals(at(9, 0), a.getStartTime());
    }

    @Test
    public void reportsWhatNormalizationAdjusted() throws Exception {
        BillEntry a = entry(1, 10, at(9, 0), at(9, 30));
        BillEntry b = entry(2, 20, at(11, 0), at(12, 0));
        List<BillEntry> day = new ArrayList<BillEntry>(Arrays.asList(a, b));

        Map<String, Object> result = new McpTimeEntriesService().updateDay(session(day), WORKSPACE, webUser, DAY,
                changes("[{\"type\":\"adjust\",\"billId\":1,\"expectedStart\":\"09:00\",\"expectedEnd\":\"09:30\","
                        + "\"start\":\"09:03\",\"end\":\"09:33\"}]"),
                null);

        Assert.assertEquals(Boolean.TRUE, result.get("applied"));
        List<?> adjustments = (List<?>) result.get("normalizationAdjustments");
        Assert.assertEquals(1, adjustments.size());
        Map<?, ?> adjustment = (Map<?, ?>) adjustments.get(0);
        Assert.assertEquals("09:03-09:33", adjustment.get("from"));
        Assert.assertEquals("09:00-09:40", adjustment.get("to"));
    }

    @Test
    public void createAddsAnEntryForAProjectWithNoNeighbor() throws Exception {
        BillEntry a = entry(1, 10, at(9, 0), at(9, 30));
        List<BillEntry> day = new ArrayList<BillEntry>(Arrays.asList(a));

        Map<String, Object> result = new McpTimeEntriesService().updateDay(session(day), WORKSPACE, webUser, DAY,
                changes("[{\"type\":\"create\",\"projectId\":77,\"start\":\"13:00\",\"end\":\"13:30\"}]"), null);

        Assert.assertEquals(Boolean.TRUE, result.get("applied"));
        Assert.assertEquals(1, saved.size());
        BillEntry created = (BillEntry) saved.get(0);
        Assert.assertEquals(77, created.getProjectId());
        Assert.assertEquals(at(13, 0), created.getStartTime());
        Assert.assertEquals(30, created.getBillMins().intValue());
    }

    @Test
    public void rejectsUnknownEntryAndBadTimeFormat() throws Exception {
        BillEntry a = entry(1, 10, at(9, 0), at(9, 30));
        List<BillEntry> day = new ArrayList<BillEntry>(Arrays.asList(a));

        Map<String, Object> result = new McpTimeEntriesService().updateDay(session(day), WORKSPACE, webUser, DAY,
                changes("[{\"type\":\"adjust\",\"billId\":42,\"expectedStart\":\"09:00\",\"expectedEnd\":\"09:30\","
                        + "\"start\":\"09:00\",\"end\":\"09:10\"},"
                        + "{\"type\":\"adjust\",\"billId\":1,\"expectedStart\":\"09:00\",\"expectedEnd\":\"09:30\","
                        + "\"start\":\"9am\",\"end\":\"09:10\"}]"),
                null);

        Assert.assertEquals(Boolean.FALSE, result.get("applied"));
        List<?> results = (List<?>) result.get("results");
        Assert.assertTrue(String.valueOf(((Map<?, ?>) results.get(0)).get("reason")).contains("No time entry 42"));
        Assert.assertTrue(String.valueOf(((Map<?, ?>) results.get(1)).get("reason")).contains("HH:mm"));
    }

    // ---- helpers ----

    private List<JsonNode> changes(String json) throws Exception {
        List<JsonNode> list = new ArrayList<JsonNode>();
        for (JsonNode node : mapper.readTree(json)) {
            list.add(node);
        }
        return list;
    }

    private Session session(final List<BillEntry> day) {
        return (Session) Proxy.newProxyInstance(Session.class.getClassLoader(), new Class<?>[] { Session.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("createQuery".equals(name)) {
                        return query((String) args[0], day);
                    }
                    if ("get".equals(name) && args[0] == Project.class) {
                        return Integer.valueOf(77).equals(args[1]) ? otherProject : null;
                    }
                    if ("get".equals(name)) {
                        return null;
                    }
                    if ("update".equals(name)) {
                        updated.add(args[0]);
                        return null;
                    }
                    if ("save".equals(name)) {
                        saved.add(args[0]);
                        return Integer.valueOf(0);
                    }
                    if ("toString".equals(name)) {
                        return "TestSession";
                    }
                    throw new UnsupportedOperationException(name);
                });
    }

    private Query query(final String hql, final List<BillEntry> day) {
        return (Query) Proxy.newProxyInstance(Query.class.getClassLoader(), new Class<?>[] { Query.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("list".equals(name)) {
                        if (hql.startsWith("from BillEntry")) {
                            return new ArrayList<BillEntry>(day);
                        }
                        if (hql.startsWith("from BillCode")) {
                            return Collections.singletonList(otherBillCode);
                        }
                        return Collections.emptyList();
                    }
                    if ("uniqueResult".equals(name)) {
                        return hql.startsWith("from Project") ? otherProject : null;
                    }
                    if (method.getReturnType() == Query.class) {
                        return proxy;
                    }
                    throw new UnsupportedOperationException(name);
                });
    }

    private static Prepared prepared(int index, String type, BillEntry entry) {
        Prepared p = new Prepared();
        p.index = index;
        p.type = type;
        p.entry = entry;
        return p;
    }

    private static BillEntry entry(int id, int projectId, Date start, Date end) {
        BillEntry entry = new BillEntry();
        entry.setBillId(id);
        entry.setProjectId(projectId);
        entry.setStartTime(start);
        entry.setEndTime(end);
        entry.setBillMins(Integer.valueOf((int) ((end.getTime() - start.getTime()) / 60000L)));
        return entry;
    }

    private static Date at(int hour, int minute) {
        return Date.from(ZonedDateTime.of(DAY, LocalTime.of(hour, minute), DENVER).toInstant());
    }
}
