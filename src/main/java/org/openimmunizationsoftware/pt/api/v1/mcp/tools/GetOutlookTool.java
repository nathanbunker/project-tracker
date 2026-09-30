package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpActionContextSupport;
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

public class GetOutlookTool implements McpTool {

    private final PlanningOutlookService service = new PlanningOutlookService();

    @Override
    public String getName() {
        return "get_outlook";
    }

    @Override
    public String getDescription() {
        return "Reads the durable monthly or weekly outlook for one period: what you intend to accomplish, "
                + "across projects. Not tied to any single project or task. Read-only.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "periodType", McpSchema.stringEnum("WEEK or MONTH.", "WEEK", "MONTH"),
                        "periodStart", McpSchema.string(
                                "The period's start date, yyyy-MM-dd. For WEEK, the Sunday that begins the week. "
                                        + "For MONTH, the first day of the month.")),
                "periodType", "periodStart");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        String periodType = McpArgs.requireString(arguments, "periodType");
        LocalDate periodStart = parseDate(McpArgs.requireString(arguments, "periodStart"));
        WebUser owner = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        OutlookResult result = service.getOutlook(context.getSession(), owner.getWebUserId(), periodType,
                periodStart, LocalDate.now(owner.getZoneId()));
        return toMap(result);
    }

    static Map<String, Object> toMap(OutlookResult result) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("periodType", result.getPeriodType());
        map.put("periodStart", result.getPeriodStart().toString());
        map.put("periodEnd", result.getPeriodEnd().toString());
        map.put("frozen", result.isFrozen());
        PlanningOutlook outlook = result.getOutlook();
        map.put("outlookText", outlook == null ? null : outlook.getOutlookText());
        map.put("updatedAt", outlook == null ? null : McpActionContextSupport.toIso(outlook.getUpdatedAt()));
        return map;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new McpToolException("invalid_arguments", "\"periodStart\" must be yyyy-MM-dd.");
        }
    }
}
