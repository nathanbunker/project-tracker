package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.util.Map;
import org.dandeliondaily.mcp.service.McpLinkedMeetingsService;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import com.fasterxml.jackson.databind.JsonNode;

public class GetLinkedMeetingsTool implements McpTool {
    @Override
    public String getName() { return "get_linked_meetings"; }

    @Override
    public String getDescription() {
        return "Reads linked calendar occurrences in this workspace, including full source snapshots, "
                + "action/project identity, actionAsOf, and meeting version. Filter by actionNextId, source "
                + "occurrence key, series key (omit externalOccurrenceId), date in the user's time zone, "
                + "overlapping half-open rangeStart/rangeEnd, or sourceStatus. Timestamps have explicit offsets. "
                + "Results are chronological, paginated (limit 1-200, default 100; offset default 0). "
                + "Series lookup also returns all seriesProjectCandidates independent of time/status/pagination "
                + "and a suggestedProjectId only if unambiguous; never assigns projects automatically. "
                + "Identity is case-sensitive. Namespace defaults to an empty string. Include CANCELLED and "
                + "MISSING unless filtered; agents should not treat those as confirmed attendance conflicts. Read-only.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(McpSchema.properties(
                "actionNextId", McpSchema.integer("Attached action ID."),
                "sourceSystem", McpSchema.string("Required with any source identity filter."),
                "sourceNamespace", McpSchema.string("Calendar/account namespace; default empty string."),
                "externalEventUid", McpSchema.string("Event or series UID; required with sourceSystem."),
                "externalOccurrenceId", McpSchema.string("Stable occurrence ID; omit to find the series."),
                "date", McpSchema.string("yyyy-MM-dd, in the user's time zone."),
                "rangeStart", McpSchema.string("Inclusive start, ISO-8601 with offset; requires rangeEnd."),
                "rangeEnd", McpSchema.string("Exclusive end, ISO-8601 with offset; requires rangeStart."),
                "sourceStatus", McpSchema.stringEnum(null, "CONFIRMED", "TENTATIVE", "CANCELLED", "MISSING"),
                "limit", McpSchema.integer("1-200; default 100."),
                "offset", McpSchema.integer("Nonnegative; default 0.")));
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        return new McpLinkedMeetingsService().read(context.getSession(), context.getWorkspaceId(),
                McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername()),
                arguments == null ? com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                        : arguments);
    }
}
