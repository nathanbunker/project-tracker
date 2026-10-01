package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpNarrativesService;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class GetNarrativesTool implements McpTool {

    private final McpNarrativesService service = new McpNarrativesService();

    @Override
    public String getName() {
        return "get_narratives";
    }

    @Override
    public String getDescription() {
        return "Reads generated daily or weekly narrative reports (TrackerNarrative) whose period starts within "
                + "an inclusive date range: what actually happened across projects and tasks, recurring themes, "
                + "and where attention went. Returns the approved final text when present, otherwise the "
                + "generated draft, with its review status. Most useful for reviewing a past period before "
                + "setting the next one's outlook. Read-only.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "periodType", McpSchema.stringEnum("DAILY or WEEKLY.", "DAILY", "WEEKLY"),
                        "fromPeriodStart", McpSchema.string("Inclusive lower bound on periodStart, yyyy-MM-dd."),
                        "toPeriodStart", McpSchema.string("Inclusive upper bound on periodStart, yyyy-MM-dd.")),
                "periodType", "fromPeriodStart", "toPeriodStart");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        String periodType = McpArgs.requireString(arguments, "periodType");
        LocalDate from = parseDate(McpArgs.requireString(arguments, "fromPeriodStart"), "fromPeriodStart");
        LocalDate to = parseDate(McpArgs.requireString(arguments, "toPeriodStart"), "toPeriodStart");
        if (to.isBefore(from)) {
            throw new McpToolException("invalid_arguments", "toPeriodStart must not be before fromPeriodStart.");
        }
        WebUser webUser = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        return service.listNarratives(context.getSession(), webUser.getContactId(), periodType, from, to);
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new McpToolException("invalid_arguments", "\"" + field + "\" must be yyyy-MM-dd.");
        }
    }
}
