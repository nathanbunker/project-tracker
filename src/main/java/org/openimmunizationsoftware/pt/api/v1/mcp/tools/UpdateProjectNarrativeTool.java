package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpProjectNarrativeCrudService;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;

import com.fasterxml.jackson.databind.JsonNode;

public class UpdateProjectNarrativeTool implements McpTool {

    private final McpProjectNarrativeCrudService service = new McpProjectNarrativeCrudService();

    @Override
    public String getName() {
        return "update_project_narrative";
    }

    @Override
    public String getDescription() {
        return "Updates an existing project narrative's text, verb, and/or date, after the user has explicitly "
                + "approved the wording. Only fields present in the call are changed. Requires lastUpdated (from "
                + "get_project_context, get_work_day_review, or a prior add/update_project_narrative call) to "
                + "avoid overwriting a concurrent edit.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(
                McpSchema.properties(
                        "narrativeId", McpSchema.integer("The narrative id."),
                        "lastUpdated", McpSchema.string("The narrative's current lastUpdated, for staleness check."),
                        "verb", McpSchema.stringEnum("New verb. Omit to leave unchanged.", "NOTE", "DECISION",
                                "INSIGHT", "RISK", "OPPORTUNITY"),
                        "date", McpSchema.string("New date, yyyy-MM-dd. Omit to leave unchanged."),
                        "text", McpSchema.string("New text. Omit to leave unchanged.")),
                "narrativeId", "lastUpdated");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        int narrativeId = McpArgs.requireInt(arguments, "narrativeId");
        String lastUpdated = McpArgs.requireString(arguments, "lastUpdated");
        ProjectNarrativeVerb verb = McpArgs.isPresent(arguments, "verb")
                ? AddProjectNarrativeTool.parseVerb(McpArgs.optString(arguments, "verb", null))
                : null;
        LocalDate date = McpArgs.isPresent(arguments, "date")
                ? AddProjectNarrativeTool.parseDate(McpArgs.optString(arguments, "date", null))
                : null;
        String text = McpArgs.optString(arguments, "text", null);
        return service.updateNarrative(context.getSession(), context.getWorkspaceId(), narrativeId, lastUpdated,
                verb, date, text);
    }
}
