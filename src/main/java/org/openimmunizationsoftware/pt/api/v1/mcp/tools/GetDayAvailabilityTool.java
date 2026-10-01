package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpDayAvailabilityService;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class GetDayAvailabilityTool implements McpTool {

    private static final int MAX_RANGE_DAYS = 62;

    private final McpDayAvailabilityService service = new McpDayAvailabilityService();

    @Override
    public String getName() {
        return "get_day_availability";
    }

    @Override
    public String getDescription() {
        return "Reads working-day availability (minutes available and status: Working, Not Working, Vacation, "
                + "Holiday, Traveling, or Sick) for each day in an inclusive date range. Days with no explicit "
                + "setting return the default (8 hours on weekdays, not working on weekends). Read-only.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "startDate", McpSchema.string("Inclusive start date, yyyy-MM-dd."),
                        "endDate", McpSchema.string("Inclusive end date, yyyy-MM-dd. Max 62 days after startDate.")),
                "startDate", "endDate");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        LocalDate startDate = parseDate(McpArgs.requireString(arguments, "startDate"), "startDate");
        LocalDate endDate = parseDate(McpArgs.requireString(arguments, "endDate"), "endDate");
        if (endDate.isBefore(startDate)) {
            throw new McpToolException("invalid_arguments", "endDate must not be before startDate.");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_RANGE_DAYS) {
            throw new McpToolException("invalid_arguments", "Date range too large; maximum is " + MAX_RANGE_DAYS
                    + " days.");
        }
        WebUser owner = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        List<Map<String, Object>> days = service.getAvailability(context.getSession(), owner.getWebUserId(),
                startDate, endDate);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("days", days);
        return response;
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new McpToolException("invalid_arguments", "\"" + field + "\" must be yyyy-MM-dd.");
        }
    }
}
