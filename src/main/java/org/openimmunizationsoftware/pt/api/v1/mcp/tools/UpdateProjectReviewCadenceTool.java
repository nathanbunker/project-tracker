package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.util.Map;

import org.dandeliondaily.mcp.service.McpProjectReviewCadenceService;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class UpdateProjectReviewCadenceTool implements McpTool {

    private final McpProjectReviewCadenceService service = new McpProjectReviewCadenceService();

    @Override
    public String getName() {
        return "update_project_review_cadence";
    }

    @Override
    public String getDescription() {
        return "Changes how often a project should come up for review on the Project Health page, after the user "
                + "has explicitly approved the change -- the same grouping shown there (Week, Two Weeks, Month, "
                + "Two Months, Four Months, Year, or None). Only affects review cadence; does not touch the "
                + "project's priority or week focus.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "projectId", McpSchema.integer("The Dandelion project id."),
                        "reviewInterval", McpSchema.stringEnum(
                                "The new review cadence. NONE means the project is never flagged for review.",
                                "NONE", "WEEK", "TWO_WEEKS", "MONTH", "TWO_MONTHS", "FOUR_MONTHS", "YEAR")),
                "projectId", "reviewInterval");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        int projectId = McpArgs.requireInt(arguments, "projectId");
        String reviewInterval = McpArgs.requireString(arguments, "reviewInterval");
        WebUser webUser = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        return service.updateCadence(context.getSession(), context.getWorkspaceId(), webUser.getContactId(),
                projectId, reviewInterval);
    }
}
