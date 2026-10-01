package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.util.Map;

import org.dandeliondaily.mcp.service.McpProjectNarrativeCrudService;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;

import com.fasterxml.jackson.databind.JsonNode;

public class DeleteProjectNarrativeTool implements McpTool {

    private final McpProjectNarrativeCrudService service = new McpProjectNarrativeCrudService();

    @Override
    public String getName() {
        return "delete_project_narrative";
    }

    @Override
    public String getDescription() {
        return "Deletes an existing project narrative entry, after the user has explicitly approved the removal. "
                + "Requires lastUpdated (from get_project_context, get_work_day_review, or a prior "
                + "add/update_project_narrative call) to avoid deleting a concurrent edit.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "narrativeId", McpSchema.integer("The narrative id."),
                        "lastUpdated", McpSchema.string("The narrative's current lastUpdated, for staleness check.")),
                "narrativeId", "lastUpdated");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        int narrativeId = McpArgs.requireInt(arguments, "narrativeId");
        String lastUpdated = McpArgs.requireString(arguments, "lastUpdated");
        return service.deleteNarrative(context.getSession(), context.getWorkspaceId(), narrativeId, lastUpdated);
    }
}
