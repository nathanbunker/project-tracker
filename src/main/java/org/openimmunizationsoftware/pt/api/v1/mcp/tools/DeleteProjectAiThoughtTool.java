package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.util.LinkedHashMap;
import java.util.Map;

import org.dandeliondaily.projectainote.service.ProjectAiNoteService;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;

import com.fasterxml.jackson.databind.JsonNode;

public class DeleteProjectAiThoughtTool implements McpTool {

    private final ProjectAiNoteService service = new ProjectAiNoteService();

    @Override
    public String getName() {
        return "delete_project_ai_thought";
    }

    @Override
    public String getDescription() {
        return "Deletes an AI thought on a project that's no longer useful.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "noteId", McpSchema.integer("The AI thought's id, from add_project_ai_thought or "
                                + "get_project_context.")),
                "noteId");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        int noteId = McpArgs.requireInt(arguments, "noteId");
        try {
            service.delete(context.getSession(), context.getWorkspaceId(), noteId);
        } catch (IllegalStateException notFound) {
            throw new McpToolException("not_found", notFound.getMessage());
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("deleted", true);
        result.put("noteId", noteId);
        return result;
    }
}
