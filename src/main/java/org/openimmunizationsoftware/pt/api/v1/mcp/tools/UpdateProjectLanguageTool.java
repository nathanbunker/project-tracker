package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.util.Map;

import org.dandeliondaily.mcp.service.McpProjectLanguageService;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class UpdateProjectLanguageTool implements McpTool {

    private final McpProjectLanguageService service = new McpProjectLanguageService();

    @Override
    public String getName() {
        return "update_project_language";
    }

    @Override
    public String getDescription() {
        return "Updates a project's description, current focus, outcome, and/or success criteria, after the user "
                + "has explicitly approved the wording -- these fields have distinct purposes and should not be "
                + "changed casually. Only fields present in the call are changed; omit a field to leave it alone. "
                + "Never renames the project. Requires asOf (from get_project_context's lastModifiedAt) once the "
                + "project has been modified before, to avoid overwriting a concurrent edit.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "projectId", McpSchema.integer("The Dandelion project id."),
                        "asOf", McpSchema.string(
                                "The project's lastModifiedAt from a prior get_project_context call. Required "
                                        + "unless the project has never been modified (lastModifiedAt was null)."),
                        "description", McpSchema.string("New description. Omit to leave unchanged."),
                        "currentFocus", McpSchema.string("New current focus. Omit to leave unchanged."),
                        "outcome", McpSchema.string("New outcome. Omit to leave unchanged."),
                        "successCriteria", McpSchema.string("New success criteria. Omit to leave unchanged.")),
                "projectId");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        int projectId = McpArgs.requireInt(arguments, "projectId");
        String asOf = McpArgs.optString(arguments, "asOf", null);
        WebUser webUser = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        return service.updateLanguage(context.getSession(), context.getWorkspaceId(), webUser.getWebUserId(),
                projectId, asOf, arguments);
    }
}
