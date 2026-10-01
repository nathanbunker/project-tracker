package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.util.Map;

import org.dandeliondaily.mcp.service.McpProjectContextService;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class GetProjectContextTool implements McpTool {

    private final McpProjectContextService service = new McpProjectContextService();

    @Override
    public String getName() {
        return "get_project_context";
    }

    @Override
    public String getDescription() {
        return "Reads a project's identity, description, current focus, outcome, success criteria, tags, "
                + "recent completed work, open actions, active recurring templates, open issues, and recent "
                + "narratives. Open actions include completionOrder, priorityLevel, and the dashboardBucket they fall "
                + "in on their scheduled day. Read-only.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "projectId", McpSchema.integer("The Dandelion project id.")),
                "projectId");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        int projectId = McpArgs.requireInt(arguments, "projectId");
        WebUser webUser = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        return service.getProjectContext(context.getSession(), context.getWorkspaceId(), projectId, webUser);
    }
}
