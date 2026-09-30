package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.util.LinkedHashMap;
import java.util.Map;

import org.dandeliondaily.projectainote.service.ProjectAiNoteService;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.ProjectAiNote;

import com.fasterxml.jackson.databind.JsonNode;

public class AddProjectAiThoughtTool implements McpTool {

    private final ProjectAiNoteService service = new ProjectAiNoteService();

    @Override
    public String getName() {
        return "add_project_ai_thought";
    }

    @Override
    public String getDescription() {
        return "Records an observation, question, or idea about a project to retain for a later session. "
                + "Distinct from established facts or decisions the user made (which belong on the project or in "
                + "its narrative) -- this is specifically the assistant's own note to itself. Visible, editable, "
                + "and deletable by the user directly in Dandelion.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "projectId", McpSchema.integer("The Dandelion project id."),
                        "noteText", McpSchema.string("The observation, question, or idea to retain.")),
                "projectId", "noteText");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        int projectId = McpArgs.requireInt(arguments, "projectId");
        String noteText = McpArgs.requireString(arguments, "noteText");
        String source = context.getAgentName() != null ? context.getAgentName() : "mcp";
        try {
            ProjectAiNote note = service.create(context.getSession(), context.getWorkspaceId(), projectId, noteText,
                    source);
            return toMap(note);
        } catch (IllegalStateException notFound) {
            throw new McpToolException("not_found", notFound.getMessage());
        } catch (IllegalArgumentException invalid) {
            throw new McpToolException("invalid_arguments", invalid.getMessage());
        }
    }

    static Map<String, Object> toMap(ProjectAiNote note) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("noteId", note.getNoteId());
        map.put("projectId", note.getProjectId());
        map.put("noteText", note.getNoteText());
        map.put("source", note.getSource());
        return map;
    }
}
