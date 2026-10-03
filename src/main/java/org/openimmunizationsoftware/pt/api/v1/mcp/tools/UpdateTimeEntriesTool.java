package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpTimeEntriesService;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class UpdateTimeEntriesTool implements McpTool {

    private final McpTimeEntriesService service = new McpTimeEntriesService();

    @Override
    public String getName() {
        return "update_time_entries";
    }

    @Override
    public String getDescription() {
        return "Corrects one day's tracked time in a single atomic batch: every change applies or none do. Read "
                + "the day with get_time_entries first, propose the corrections, and only call this after the user "
                + "has explicitly approved them. Times are HH:mm (24-hour) in the user's time zone on \"date\". "
                + "Change types:\n"
                + "- adjust: {type, billId, expectedStart, expectedEnd, start, end}. New times for an entry. "
                + "start == end zeroes it (that's how an entry is removed; Dandelion deletes zero-minute entries "
                + "when the timer next stops). Entries are never deleted directly.\n"
                + "- reassign: {type, billId, expectedStart, expectedEnd, projectId, actionNextId?}. Moves an "
                + "entry's time to another project (and optionally one of its actions), keeping its times; the "
                + "bill code follows the project. Can be combined with an adjust of the same entry.\n"
                + "- create: {type, projectId, actionNextId?, start, end}. A new entry, for time that belongs to a "
                + "project with no neighboring entry to extend.\n"
                + "expectedStart/expectedEnd are the entry's current times from get_time_entries; if the entry "
                + "has changed since, the batch is rejected. The batch is also rejected if, after all changes, "
                + "any two entries with time on them would overlap anywhere in the day (zero-length entries never "
                + "overlap), if times leave the day or run into the future, or if it touches today's most recent "
                + "entry (never editable; it may be the running timer). To move time between neighbors, change "
                + "both in the same batch, e.g. extend the previous entry's end and zero the wrong one. After "
                + "applying, the day is normalized exactly as Dandelion's Review page does it: times truncate to "
                + "the minute, each unbroken run of entries starts on a 10-minute mark (rounded down) and ends on "
                + "one (rounded up), and gaps inside one 10-minute window close by extending the earlier entry. "
                + "Anything normalization moved is listed in normalizationAdjustments; tell the user, and follow "
                + "up with another change if the result isn't what they wanted. The result includes the day as "
                + "stored. Up to 50 changes per call; any past day can be corrected.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> changeItem = McpSchema.object(
                McpSchema.properties(
                        "type", McpSchema.stringEnum("adjust, reassign, or create; see the tool description.",
                                "adjust", "reassign", "create"),
                        "billId", McpSchema.integer("The entry to change (adjust, reassign)."),
                        "expectedStart", McpSchema.string(
                                "The entry's current start, HH:mm, from get_time_entries (adjust, reassign)."),
                        "expectedEnd", McpSchema.string(
                                "The entry's current end, HH:mm, from get_time_entries (adjust, reassign)."),
                        "start", McpSchema.string("New start, HH:mm (adjust, create)."),
                        "end", McpSchema.string("New end, HH:mm; equal to start zeroes the entry (adjust, create)."),
                        "projectId", McpSchema.integer("Target project (reassign, create)."),
                        "actionNextId", McpSchema.integer(
                                "Optional action on the target project (reassign, create).")),
                "type");
        return McpSchema.object(
                McpSchema.properties(
                        "date", McpSchema.string("The day being corrected, yyyy-MM-dd."),
                        "changes", McpSchema.array("Up to 50 changes, applied atomically.", changeItem),
                        "reason", McpSchema.string("Optional short note on why (written to the server log).")),
                "date", "changes");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        LocalDate date = GetTimeEntriesTool.parseDate(McpArgs.requireString(arguments, "date"));
        List<JsonNode> changes = McpArgs.optNodeList(arguments, "changes");
        String reason = McpArgs.optString(arguments, "reason", null);
        WebUser webUser = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        return service.updateDay(context.getSession(), context.getWorkspaceId(), webUser, date, changes, reason);
    }
}
