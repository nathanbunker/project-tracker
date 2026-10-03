package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.dandeliondaily.outlook.service.PlanningOutlookService;
import org.dandeliondaily.outlook.service.PlanningOutlookService.OutlookResult;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.PlanningOutlook;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class SetOutlookTool implements McpTool {

    private final PlanningOutlookService service = new PlanningOutlookService();

    @Override
    public String getName() {
        return "set_outlook";
    }

    @Override
    public String getDescription() {
        return "Replaces the outlook text for one monthly or weekly period with the latest version (no revision "
                + "history is kept within a period). Weeks start on Sunday and months on the 1st. Fails if the "
                + "period has already ended. Only apply this after "
                + "the user has explicitly approved the wording.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "periodType", McpSchema.stringEnum("WEEK or MONTH.", "WEEK", "MONTH"),
                        "periodStart", McpSchema.string(
                                "The period's start date, yyyy-MM-dd. For WEEK, it must be the Sunday that begins the "
                                        + "week; for MONTH, the first day of the month. Any other date is "
                                        + "rejected."),
                        "outlookText", McpSchema.string("The full outlook text, replacing any prior text.")),
                "periodType", "periodStart", "outlookText");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        String periodType = McpArgs.requireString(arguments, "periodType");
        LocalDate periodStart = parseDate(McpArgs.requireString(arguments, "periodStart"));
        String outlookText = McpArgs.requireString(arguments, "outlookText");
        WebUser owner = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        try {
            LocalDate today = LocalDate.now(owner.getZoneId());
            PlanningOutlook outlook = service.setOutlook(context.getSession(), owner.getWebUserId(), periodType,
                    periodStart, outlookText, today);
            String normalizedType = outlook.getPeriodType();
            OutlookResult result = new OutlookResult(outlook, normalizedType, periodStart,
                    service.periodEnd(normalizedType, periodStart), false);
            return GetOutlookTool.toMap(result);
        } catch (IllegalStateException frozen) {
            throw new McpToolException("period_frozen", frozen.getMessage());
        } catch (IllegalArgumentException invalid) {
            throw new McpToolException("invalid_arguments", invalid.getMessage());
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new McpToolException("invalid_arguments", "\"periodStart\" must be yyyy-MM-dd.");
        }
    }
}
