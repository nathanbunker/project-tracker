package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpDayAvailabilityService;
import org.dandeliondaily.mcp.service.McpDayAvailabilityService.DayAvailabilityInput;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class SetDayAvailabilityTool implements McpTool {

    private final McpDayAvailabilityService service = new McpDayAvailabilityService();

    @Override
    public String getName() {
        return "set_day_availability";
    }

    @Override
    public String getDescription() {
        return "Sets working-day availability for one or more specific days (e.g. 'not working next Tuesday', "
                + "'half day Wednesday'). Also re-syncs recurring action templates for the affected window, since "
                + "template eligibility depends on this same availability data.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> dayItem = McpSchema.object(
                McpSchema.properties(
                        "date", McpSchema.string("The date, yyyy-MM-dd."),
                        "workStatus", McpSchema.stringEnum(
                                "W=Working, N=Not Working, V=Vacation, H=Holiday, T=Traveling, S=Sick.",
                                "W", "N", "V", "H", "T", "S"),
                        "billMinutes", McpSchema.integer(
                                "Minutes available that day. Optional: defaults to 480 for Working, 0 otherwise.")),
                "date", "workStatus");
        return McpSchema.object(
                McpSchema.properties(
                        "days", McpSchema.array("One or more days to set.", dayItem)),
                "days");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        List<JsonNode> dayNodes = McpArgs.optNodeList(arguments, "days");
        if (dayNodes.isEmpty()) {
            throw new McpToolException("invalid_arguments", "\"days\" must contain at least one entry.");
        }
        List<DayAvailabilityInput> inputs = new ArrayList<DayAvailabilityInput>();
        for (JsonNode dayNode : dayNodes) {
            LocalDate date = parseDate(McpArgs.requireString(dayNode, "date"));
            String workStatus = McpArgs.requireString(dayNode, "workStatus");
            Integer billMinutes = McpArgs.optInt(dayNode, "billMinutes", null);
            inputs.add(new DayAvailabilityInput(date, workStatus, billMinutes));
        }
        WebUser owner = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        service.setAvailability(context.getSession(), context.getWorkspaceId(), owner.getWebUserId(),
                owner.getContactId(), inputs);

        LocalDate min = inputs.get(0).getDate();
        LocalDate max = inputs.get(0).getDate();
        for (DayAvailabilityInput input : inputs) {
            if (input.getDate().isBefore(min)) {
                min = input.getDate();
            }
            if (input.getDate().isAfter(max)) {
                max = input.getDate();
            }
        }
        List<Map<String, Object>> updated = service.getAvailability(context.getSession(), owner.getWebUserId(), min,
                max);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("updated", updated);
        return result;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new McpToolException("invalid_arguments", "\"date\" must be yyyy-MM-dd.");
        }
    }
}
