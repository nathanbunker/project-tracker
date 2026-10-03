package org.dandeliondaily.mcp.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.dandeliondaily.timereview.service.TimeRegularizationService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * get_time_entries / update_time_entries: read and correct one day's time entries
 * (bill_entry). See the assessment, Phase 4 and decisions 24-28.
 */
public class McpTimeEntriesService {

    private static final Logger LOGGER = Logger.getLogger(McpTimeEntriesService.class.getName());
    static final int MAX_CHANGES = 50;
    static final String CURRENT_ENTRY_REASON = "current entry; edit in Dandelion";
    private static final long MAX_DURATION_MILLIS = 12L * 60L * 60L * 1000L;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final TimeRegularizationService regularizationService = new TimeRegularizationService();

    // ---- read ----

    public Map<String, Object> readDay(Session session, WebUser webUser, LocalDate date) {
        List<BillEntry> entries = loadDay(session, webUser, date);
        BillEntry current = currentEntry(entries, date.equals(webUser.getLocalDateToday()));
        return dayToMap(session, webUser, date, entries, current);
    }

    // ---- write ----

    public Map<String, Object> updateDay(Session session, int workspaceId, WebUser webUser, LocalDate date,
            List<JsonNode> changeNodes, String reason) {
        if (changeNodes.isEmpty()) {
            throw new McpToolException("invalid_arguments", "\"changes\" must contain at least one item.");
        }
        if (changeNodes.size() > MAX_CHANGES) {
            throw new McpToolException("invalid_arguments",
                    "Too many changes in one batch; maximum is " + MAX_CHANGES + ".");
        }

        Date dayStart = webUser.toDate(date);
        Date dayEnd = webUser.toDate(date.plusDays(1));
        boolean today = date.equals(webUser.getLocalDateToday());
        Date now = new Date();
        List<BillEntry> entries = loadDay(session, webUser, date);
        BillEntry current = currentEntry(entries, today);
        Map<Integer, BillEntry> byId = new HashMap<Integer, BillEntry>();
        for (BillEntry entry : entries) {
            byId.put(Integer.valueOf(entry.getBillId()), entry);
        }

        // 1. Validate each change on its own; nothing is modified yet.
        List<Prepared> prepared = new ArrayList<Prepared>();
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        boolean allValid = true;
        for (int i = 0; i < changeNodes.size(); i++) {
            Prepared p = validate(session, workspaceId, webUser, date, dayStart, dayEnd, today, now, byId,
                    current, i, changeNodes.get(i));
            prepared.add(p);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("index", Integer.valueOf(i));
            result.put("type", p.type);
            if (p.entry != null) {
                result.put("billId", Integer.valueOf(p.entry.getBillId()));
            }
            if (p.error != null) {
                allValid = false;
                result.put("status", "rejected");
                result.put("reason", p.error);
            } else {
                result.put("status", "valid");
            }
            results.add(result);
        }
        if (allValid) {
            String duplicate = findDuplicateTarget(prepared);
            if (duplicate != null) {
                return rejected(results, duplicate);
            }
        }
        if (!allValid) {
            return rejected(results, null);
        }

        // 2. Simulate the whole day with every change applied and check for overlaps.
        List<Span> simulated = new ArrayList<Span>();
        Map<Integer, Prepared> timeChangeById = new HashMap<Integer, Prepared>();
        for (Prepared p : prepared) {
            if ("adjust".equals(p.type)) {
                timeChangeById.put(Integer.valueOf(p.entry.getBillId()), p);
            }
        }
        for (BillEntry entry : entries) {
            Prepared p = timeChangeById.get(Integer.valueOf(entry.getBillId()));
            simulated.add(p == null
                    ? new Span("entry " + entry.getBillId(), entry.getStartTime(), entry.getEndTime())
                    : new Span("entry " + entry.getBillId(), p.newStart, p.newEnd));
        }
        for (Prepared p : prepared) {
            if ("create".equals(p.type)) {
                simulated.add(new Span("new entry (change " + p.index + ")", p.newStart, p.newEnd));
            }
        }
        String overlap = findOverlap(simulated, webUser);
        if (overlap != null) {
            return rejected(results, "The changes would leave overlapping time: " + overlap
                    + ". Zero-length entries never overlap; shorten or zero one of them.");
        }

        // 3. Apply.
        Integer workspaceForNew = WorkspaceRegistry.getWorkspaceIdForWebUserId(webUser.getWebUserId());
        for (int i = 0; i < prepared.size(); i++) {
            Prepared p = prepared.get(i);
            if ("adjust".equals(p.type)) {
                p.entry.setStartTime(p.newStart);
                p.entry.setEndTime(p.newEnd);
                p.entry.setBillMins(Integer.valueOf(TimeTracker.calculateMins(p.entry)));
                session.update(p.entry);
            } else if ("reassign".equals(p.type)) {
                p.entry.setProjectId(p.project.getProjectId());
                p.entry.setAction(p.action);
                p.entry.setBillCode(p.billCode.getBillCode());
                p.entry.setBillable(p.billCode.getBillable());
                p.entry.setBillBudgetId(p.project.getBillBudgetId());
                session.update(p.entry);
            } else if ("create".equals(p.type)) {
                BillEntry created = TimeTracker.createBillEntry(p.project, p.action, p.billCode,
                        workspaceForNew == null ? p.project.getWorkspaceId() : workspaceForNew, webUser,
                        p.newStart);
                created.setEndTime(p.newEnd);
                created.setBillMins(Integer.valueOf(TimeTracker.calculateMins(created)));
                session.save(created);
                entries.add(created);
                results.get(i).put("billId", Integer.valueOf(created.getBillId()));
            }
            results.get(i).put("status", "applied");
        }

        // 4. Normalize the day exactly as the Review page does, and report what moved.
        sortByStart(entries);
        Map<Integer, Date[]> beforeNormalize = new HashMap<Integer, Date[]>();
        for (BillEntry entry : entries) {
            beforeNormalize.put(Integer.valueOf(entry.getBillId()),
                    new Date[] { entry.getStartTime(), entry.getEndTime() });
        }
        List<BillEntry> normalized = regularizationService.computeNormalizedTimes(webUser, entries, dayStart,
                dayEnd, current == null ? null : Integer.valueOf(current.getBillId()), now);
        List<Map<String, Object>> adjustments = new ArrayList<Map<String, Object>>();
        if (!normalized.isEmpty()) {
            for (BillEntry entry : entries) {
                session.update(entry);
                Date[] before = beforeNormalize.get(Integer.valueOf(entry.getBillId()));
                if (before[0].getTime() != entry.getStartTime().getTime()
                        || before[1].getTime() != entry.getEndTime().getTime()) {
                    Map<String, Object> adjustment = new LinkedHashMap<String, Object>();
                    adjustment.put("billId", Integer.valueOf(entry.getBillId()));
                    adjustment.put("from", hhmm(webUser, before[0]) + "-" + hhmm(webUser, before[1]));
                    adjustment.put("to", hhmm(webUser, entry.getStartTime()) + "-"
                            + hhmm(webUser, entry.getEndTime()));
                    adjustments.add(adjustment);
                }
            }
        }

        LOGGER.info("update_time_entries applied " + prepared.size() + " change(s) for user="
                + webUser.getUsername() + " date=" + date + (reason == null ? "" : " reason=" + reason));

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("applied", Boolean.TRUE);
        response.put("message", "All " + prepared.size() + " changes applied."
                + (adjustments.isEmpty() ? "" : " Day normalization then adjusted " + adjustments.size()
                        + " entr" + (adjustments.size() == 1 ? "y" : "ies") + " (see normalizationAdjustments)."));
        response.put("results", results);
        response.put("normalizationAdjustments", adjustments);
        response.put("day", dayToMap(session, webUser, date, entries, current));
        return response;
    }

    // ---- validation ----

    private Prepared validate(Session session, int workspaceId, WebUser webUser, LocalDate date, Date dayStart,
            Date dayEnd, boolean today, Date now, Map<Integer, BillEntry> byId, BillEntry current, int index,
            JsonNode item) {
        Prepared p = new Prepared();
        p.index = index;
        p.type = item.hasNonNull("type") ? item.get("type").asText() : null;
        if (!"adjust".equals(p.type) && !"reassign".equals(p.type) && !"create".equals(p.type)) {
            p.error = "\"type\" must be adjust, reassign, or create.";
            return p;
        }

        if (!"create".equals(p.type)) {
            if (!item.hasNonNull("billId")) {
                p.error = "\"billId\" is required for " + p.type + ".";
                return p;
            }
            p.entry = byId.get(Integer.valueOf(item.get("billId").asInt()));
            if (p.entry == null) {
                p.error = "No time entry " + item.get("billId").asInt() + " of yours starts on " + date + ".";
                return p;
            }
            if (current != null && current.getBillId() == p.entry.getBillId()) {
                p.error = "Entry " + p.entry.getBillId() + " is today's most recent entry, which may be the "
                        + "running timer (" + CURRENT_ENTRY_REASON + ").";
                return p;
            }
            String expectedStart = item.hasNonNull("expectedStart") ? item.get("expectedStart").asText() : null;
            String expectedEnd = item.hasNonNull("expectedEnd") ? item.get("expectedEnd").asText() : null;
            if (expectedStart == null || expectedEnd == null) {
                p.error = "\"expectedStart\" and \"expectedEnd\" (HH:mm, from get_time_entries) are required.";
                return p;
            }
            if (!expectedStart.equals(hhmm(webUser, p.entry.getStartTime()))
                    || !expectedEnd.equals(hhmm(webUser, p.entry.getEndTime()))) {
                p.error = "Entry " + p.entry.getBillId() + " has changed: it is now "
                        + hhmm(webUser, p.entry.getStartTime()) + "-" + hhmm(webUser, p.entry.getEndTime())
                        + ", not " + expectedStart + "-" + expectedEnd + ". Read the day again.";
                return p;
            }
        }

        if ("adjust".equals(p.type) || "create".equals(p.type)) {
            p.newStart = parseTime(webUser, date, item, "start", p);
            if (p.error != null) {
                return p;
            }
            p.newEnd = parseTime(webUser, date, item, "end", p);
            if (p.error != null) {
                return p;
            }
            if (p.newStart.after(p.newEnd)) {
                p.error = "start must not be after end.";
                return p;
            }
            if (p.newEnd.getTime() - p.newStart.getTime() > MAX_DURATION_MILLIS) {
                p.error = "An entry can't be longer than 12 hours.";
                return p;
            }
            if (p.newStart.before(dayStart) || !p.newEnd.before(dayEnd)) {
                p.error = "Times must stay within " + date + ".";
                return p;
            }
            if (today && p.newEnd.after(now)) {
                p.error = "end can't be in the future.";
                return p;
            }
            if ("create".equals(p.type) && !p.newEnd.after(p.newStart)) {
                p.error = "A created entry needs some time (end after start).";
                return p;
            }
        }

        if ("reassign".equals(p.type) || "create".equals(p.type)) {
            if (!item.hasNonNull("projectId")) {
                p.error = "\"projectId\" is required for " + p.type + ".";
                return p;
            }
            p.project = requireProjectInWorkspace(session, workspaceId, item.get("projectId").asInt());
            if (p.project == null) {
                p.error = "Project " + item.get("projectId").asInt() + " not found in this workspace.";
                return p;
            }
            p.billCode = ClientServlet.resolveBillCode(session, p.project);
            if (p.billCode == null) {
                p.error = "Project " + p.project.getProjectId() + " (" + p.project.getProjectName()
                        + ") has no bill code, so time can't be recorded on it.";
                return p;
            }
            if (item.hasNonNull("actionNextId")) {
                ActionNext action = (ActionNext) session.get(ActionNext.class,
                        Integer.valueOf(item.get("actionNextId").asInt()));
                if (action == null || action.getProjectId() != p.project.getProjectId()) {
                    p.error = "Action " + item.get("actionNextId").asInt() + " doesn't belong to project "
                            + p.project.getProjectId() + ".";
                    return p;
                }
                p.action = action;
            }
        }
        return p;
    }

    private Date parseTime(WebUser webUser, LocalDate date, JsonNode item, String field, Prepared p) {
        if (!item.hasNonNull(field)) {
            p.error = "\"" + field + "\" (HH:mm) is required for " + p.type + ".";
            return null;
        }
        String text = item.get(field).asText().trim();
        try {
            LocalTime time = LocalTime.parse(text, HH_MM);
            return Date.from(ZonedDateTime.of(date, time, webUser.getZoneId()).toInstant());
        } catch (DateTimeParseException e) {
            p.error = "\"" + field + "\" must be HH:mm (24-hour), e.g. 09:20; got \"" + text + "\".";
            return null;
        }
    }

    /** At most one adjust and one reassign per entry, so the result of a batch is unambiguous. */
    static String findDuplicateTarget(List<Prepared> prepared) {
        Map<String, Integer> seen = new HashMap<String, Integer>();
        for (Prepared p : prepared) {
            if (p.entry == null) {
                continue;
            }
            String key = p.type + ":" + p.entry.getBillId();
            if (seen.containsKey(key)) {
                return "Entry " + p.entry.getBillId() + " has more than one " + p.type
                        + " change; combine them into one.";
            }
            seen.put(key, Integer.valueOf(p.index));
        }
        return null;
    }

    // ---- pure helpers (unit-tested) ----

    /**
     * Today's most recent entry (latest start, then highest id), which may be the
     * running timer and so is never editable through the MCP (decision 24).
     */
    static BillEntry currentEntry(List<BillEntry> entries, boolean today) {
        if (!today || entries.isEmpty()) {
            return null;
        }
        BillEntry latest = null;
        for (BillEntry entry : entries) {
            if (latest == null || entry.getStartTime().after(latest.getStartTime())
                    || (entry.getStartTime().equals(latest.getStartTime())
                            && entry.getBillId() > latest.getBillId())) {
                latest = entry;
            }
        }
        return latest;
    }

    /**
     * Describes the first pair of spans with time on them that overlap, or null.
     * Zero-length spans never overlap anything.
     */
    static String findOverlap(List<Span> spans, WebUser webUser) {
        List<Span> positive = new ArrayList<Span>();
        for (Span span : spans) {
            if (span.end.after(span.start)) {
                positive.add(span);
            }
        }
        Collections.sort(positive, new Comparator<Span>() {
            @Override
            public int compare(Span a, Span b) {
                return a.start.compareTo(b.start);
            }
        });
        Span latestEnding = null;
        for (Span span : positive) {
            if (latestEnding != null && span.start.before(latestEnding.end)) {
                return latestEnding.label + " (" + hhmm(webUser, latestEnding.start) + "-"
                        + hhmm(webUser, latestEnding.end) + ") and " + span.label + " ("
                        + hhmm(webUser, span.start) + "-" + hhmm(webUser, span.end) + ")";
            }
            if (latestEnding == null || span.end.after(latestEnding.end)) {
                latestEnding = span;
            }
        }
        return null;
    }

    static String hhmm(WebUser webUser, Date date) {
        return HH_MM.format(date.toInstant().atZone(webUser.getZoneId()));
    }

    // ---- rendering and loading ----

    private Map<String, Object> dayToMap(Session session, WebUser webUser, LocalDate date, List<BillEntry> entries,
            BillEntry current) {
        sortByStart(entries);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        int total = 0;
        Map<Integer, Project> projects = new HashMap<Integer, Project>();
        for (BillEntry entry : entries) {
            int minutes = entry.getBillMins() == null ? TimeTracker.calculateMins(entry)
                    : entry.getBillMins().intValue();
            total += minutes;
            Integer projectId = Integer.valueOf(entry.getProjectId());
            if (!projects.containsKey(projectId)) {
                projects.put(projectId, (Project) session.get(Project.class, projectId));
            }
            Project project = projects.get(projectId);
            ActionNext action = entry.getAction();
            boolean editable = current == null || current.getBillId() != entry.getBillId();

            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("billId", Integer.valueOf(entry.getBillId()));
            map.put("start", hhmm(webUser, entry.getStartTime()));
            map.put("end", hhmm(webUser, entry.getEndTime()));
            map.put("minutes", Integer.valueOf(minutes));
            map.put("projectId", projectId);
            map.put("projectName", project == null ? null : project.getProjectName());
            map.put("actionNextId", action == null ? null : Integer.valueOf(action.getActionNextId()));
            map.put("actionDescription", action == null ? null : action.getNextDescription());
            map.put("billCode", entry.getBillCode());
            map.put("billable", entry.getBillable());
            map.put("editable", Boolean.valueOf(editable));
            if (!editable) {
                map.put("notEditableReason", CURRENT_ENTRY_REASON);
            }
            list.add(map);
        }
        Map<String, Object> day = new LinkedHashMap<String, Object>();
        day.put("date", date.toString());
        day.put("timeZone", webUser.getZoneId().getId());
        day.put("totalMinutes", Integer.valueOf(total));
        day.put("entries", list);
        return day;
    }

    private List<BillEntry> loadDay(Session session, WebUser webUser, LocalDate date) {
        List<BillEntry> entries = new ArrayList<BillEntry>(
                regularizationService.loadEntriesForDay(webUser, session, webUser.toDate(date)));
        sortByStart(entries);
        return entries;
    }

    private static void sortByStart(List<BillEntry> entries) {
        Collections.sort(entries, new Comparator<BillEntry>() {
            @Override
            public int compare(BillEntry a, BillEntry b) {
                int byStart = a.getStartTime().compareTo(b.getStartTime());
                return byStart != 0 ? byStart : Integer.compare(a.getBillId(), b.getBillId());
            }
        });
    }

    private static Map<String, Object> rejected(List<Map<String, Object>> results, String batchReason) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("applied", Boolean.FALSE);
        response.put("message", "Batch rejected; nothing was changed."
                + (batchReason == null ? " See the reason on each rejected change." : " " + batchReason));
        response.put("results", results);
        return response;
    }

    private Project requireProjectInWorkspace(Session session, int workspaceId, int projectId) {
        Query query = session.createQuery(
                "from Project p where p.projectId = :projectId and p.workspaceId = :workspaceId");
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        return (Project) query.uniqueResult();
    }

    // ---- internal structures ----

    static class Prepared {
        int index;
        String type;
        String error;
        BillEntry entry;
        Date newStart;
        Date newEnd;
        Project project;
        ActionNext action;
        BillCode billCode;
    }

    static class Span {
        final String label;
        final Date start;
        final Date end;

        Span(String label, Date start, Date end) {
            this.label = label;
            this.start = start;
            this.end = end;
        }
    }
}
