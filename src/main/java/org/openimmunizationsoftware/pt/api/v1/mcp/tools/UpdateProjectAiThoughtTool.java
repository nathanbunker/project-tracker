package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.util.Map;

import org.dandeliondaily.projectainote.service.ProjectAiNoteService;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.ProjectAiNote;

import com.fasterxml.jackson.databind.JsonNode;

public class UpdateProjectAiThoughtTool implements McpTool {

    private final ProjectAiNoteService service = new ProjectAiNoteService();

    @Override
    public String getName() {
        return "update_project_ai_thought";
    }

    @Override
    public String getDescription() {
        return "Replaces the text of an existing AI thought on a project.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "noteId", McpSchema.integer("The AI thought's id, from add_project_ai_thought or "
                                + "get_project_context."),
                        "noteText", McpSchema.string("The full replacement text.")),
                "noteId", "noteText");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        int noteId = McpArgs.requireInt(arguments, "noteId");
        String noteText = McpArgs.requireString(arguments, "noteText");
        try {
            ProjectAiNote note = service.update(context.getSession(), context.getWorkspaceId(), noteId, noteText);
            return AddProjectAiThoughtTool.toMap(note);
        } catch (IllegalStateException notFound) {
            throw new McpToolException("not_found", notFound.getMessage());
        } catch (IllegalArgumentException invalid) {
            throw new McpToolException("invalid_arguments", invalid.getMessage());
        }
    }
}
