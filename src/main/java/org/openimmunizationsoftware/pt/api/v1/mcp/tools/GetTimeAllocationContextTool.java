package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpTimeAllocationContextService;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;

import com.fasterxml.jackson.databind.JsonNode;

public class GetTimeAllocationContextTool implements McpTool {

    private final McpTimeAllocationContextService service = new McpTimeAllocationContextService();

    @Override
    public String getName() {
        return "get_time_allocation_context";
    }

    @Override
    public String getDescription() {
        return "Reads the same picture as the weekly report: how time is supposed to be allocated across bill "
                + "codes (annual and steering targets), actual minutes and percent-of-obligated at week/4-week/"
                + "fiscal-year grain, which projects that time went to with recent completed-work descriptions, "
                + "and an 8-week worked-vs-obligated trend. fiscalYear gives the active bill plan's start/end "
                + "dates (null when no plan applies); fiscal-year figures count from startDate through "
                + "countedThrough. fiscalProjectMinutes lists every project/bill code with fiscal-year time, "
                + "so a fiscal-year total can be traced to its projects. Minutes are rounded per project per "
                + "week, as on the report. Read-only.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "weekStart", McpSchema.string(
                                "Optional Sunday date, yyyy-MM-dd, for the week to select. Defaults to the "
                                        + "current week.")));
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        String weekStartText = McpArgs.optString(arguments, "weekStart", null);
        LocalDate weekStart = weekStartText == null ? null : parseDate(weekStartText);
        return service.getTimeAllocationContext(context.getSession(), context.getWorkspaceId(),
                context.getUsername(), weekStart);
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new McpToolException("invalid_arguments", "\"weekStart\" must be yyyy-MM-dd.");
        }
    }
}
