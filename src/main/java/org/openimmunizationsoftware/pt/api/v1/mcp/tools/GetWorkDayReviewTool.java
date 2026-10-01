package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.dandeliondaily.mcp.service.McpWorkDayReviewService;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class GetWorkDayReviewTool implements McpTool {

    private final McpWorkDayReviewService service = new McpWorkDayReviewService();

    @Override
    public String getName() {
        return "get_work_day_review";
    }

    @Override
    public String getDescription() {
        return "Mirrors the end-of-day Review screen for one date: per project with billed time, minutes, "
                + "whether it's reviewed and why not (no narrative yet, or which setup fields are missing), "
                + "completed and deleted-with-time actions, and the existing NOTE/DECISION/INSIGHT/RISK/OPPORTUNITY "
                + "narratives with their ids and lastUpdated. Also reports whether the day's narrative report has "
                + "been generated or approved. Read-only.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "date", McpSchema.string("The review date, yyyy-MM-dd."),
                        "includeBelowThreshold", McpSchema.bool(
                                "Include projects with billed time below the review minutes threshold. "
                                        + "Default false.")),
                "date");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        LocalDate date = parseDate(McpArgs.requireString(arguments, "date"));
        boolean includeBelowThreshold = McpArgs.optBool(arguments, "includeBelowThreshold", false);
        WebUser webUser = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        return service.getWorkDayReview(context.getSession(), webUser.getContactId(), date, includeBelowThreshold);
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new McpToolException("invalid_arguments", "\"date\" must be yyyy-MM-dd.");
        }
    }
}
