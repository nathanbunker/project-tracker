package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.dandeliondaily.outlook.service.PlanningOutlookService;
import org.dandeliondaily.outlook.service.PlanningOutlookService.OutlookResult;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class ListOutlooksTool implements McpTool {

    private final PlanningOutlookService service = new PlanningOutlookService();

    @Override
    public String getName() {
        return "list_outlooks";
    }

    @Override
    public String getDescription() {
        return "Lists monthly or weekly outlooks whose period starts within an inclusive date range, most useful "
                + "for reviewing recent history before planning the next period. Read-only.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "periodType", McpSchema.stringEnum("WEEK or MONTH.", "WEEK", "MONTH"),
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
        WebUser owner = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        List<OutlookResult> results = service.listOutlooks(context.getSession(), owner.getWebUserId(), periodType,
                from, to.plusDays(1), LocalDate.now(owner.getZoneId()));
        List<Map<String, Object>> mapped = new ArrayList<Map<String, Object>>();
        for (OutlookResult result : results) {
            mapped.add(GetOutlookTool.toMap(result));
        }
        return mapped;
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new McpToolException("invalid_arguments", "\"" + field + "\" must be yyyy-MM-dd.");
        }
    }
}
