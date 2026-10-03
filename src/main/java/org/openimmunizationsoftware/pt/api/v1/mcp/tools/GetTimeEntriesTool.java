package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpTimeEntriesService;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class GetTimeEntriesTool implements McpTool {

    private final McpTimeEntriesService service = new McpTimeEntriesService();

    @Override
    public String getName() {
        return "get_time_entries";
    }

    @Override
    public String getDescription() {
        return "Reads one day's tracked time entries in order: billId, start and end (HH:mm, 24-hour, in the "
                + "user's time zone), minutes, project, action, bill code, and whether each entry can be "
                + "corrected with update_time_entries. Today's most recent entry is never editable (it may be the "
                + "running timer). Zero-minute entries are ones that were zeroed out; Dandelion deletes them the "
                + "next time the timer stops. Use this before proposing any time correction. Read-only.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "date", McpSchema.string("The day, yyyy-MM-dd.")),
                "date");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        LocalDate date = parseDate(McpArgs.requireString(arguments, "date"));
        WebUser webUser = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        return service.readDay(context.getSession(), webUser, date);
    }

    static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new McpToolException("invalid_arguments", "\"date\" must be yyyy-MM-dd.");
        }
    }
}
