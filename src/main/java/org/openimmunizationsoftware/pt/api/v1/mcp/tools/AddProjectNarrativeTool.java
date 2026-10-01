package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpProjectNarrativeCrudService;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class AddProjectNarrativeTool implements McpTool {

    private final McpProjectNarrativeCrudService service = new McpProjectNarrativeCrudService();

    @Override
    public String getName() {
        return "add_project_narrative";
    }

    @Override
    public String getDescription() {
        return "Adds a dated project narrative entry (NOTE, DECISION, INSIGHT, RISK, or OPPORTUNITY) for any date "
                + "-- not just today -- after the user has explicitly approved the wording. These are durable, "
                + "planning-visible project history, distinct from AI thoughts. Fails if one already exists for "
                + "this project/verb/date; use update_project_narrative instead.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "projectId", McpSchema.integer("The Dandelion project id."),
                        "verb", McpSchema.stringEnum("The narrative type.", "NOTE", "DECISION", "INSIGHT", "RISK",
                                "OPPORTUNITY"),
                        "date", McpSchema.string("The narrative's date, yyyy-MM-dd."),
                        "text", McpSchema.string("The narrative text.")),
                "projectId", "verb", "date", "text");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        int projectId = McpArgs.requireInt(arguments, "projectId");
        ProjectNarrativeVerb verb = parseVerb(McpArgs.requireString(arguments, "verb"));
        LocalDate date = parseDate(McpArgs.requireString(arguments, "date"));
        String text = McpArgs.requireString(arguments, "text");
        WebUser webUser = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        return service.addNarrative(context.getSession(), context.getWorkspaceId(), projectId, webUser, verb, date,
                text);
    }

    static ProjectNarrativeVerb parseVerb(String value) {
        ProjectNarrativeVerb verb = ProjectNarrativeVerb.fromId(value);
        if (verb == null) {
            throw new McpToolException("invalid_arguments",
                    "\"verb\" must be one of NOTE, DECISION, INSIGHT, RISK, OPPORTUNITY.");
        }
        return verb;
    }

    static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new McpToolException("invalid_arguments", "\"date\" must be yyyy-MM-dd.");
        }
    }
}
