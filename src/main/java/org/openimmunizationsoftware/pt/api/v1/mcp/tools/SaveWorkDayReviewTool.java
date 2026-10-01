package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpSaveWorkDayReviewService;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class SaveWorkDayReviewTool implements McpTool {

    private final McpSaveWorkDayReviewService service = new McpSaveWorkDayReviewService();

    @Override
    public String getName() {
        return "save_work_day_review";
    }

    @Override
    public String getDescription() {
        return "Writes a project's NOTE/DECISION/INSIGHT/RISK/OPPORTUNITY narratives for one review date, with "
                + "the same semantics as the Review screen -- requires the user's explicit approval of the wording "
                + "before being called. Only verbs present in \"entries\" are touched: an omitted verb is left "
                + "unchanged, and an empty string removes that day's entry for that verb. NOTE is the exception -- "
                + "if present but blank it gets a default \"Reviewed/no comments\" text rather than being removed, "
                + "since a NOTE is what marks the project reviewed. Saving marks the project reviewed in the "
                + "Dandelion UI once its setup (review cadence, outcome, success criteria) is also complete.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> verbProperties = McpSchema.properties(
                "note", McpSchema.string("Note text. Omit to leave unchanged; empty string defaults the text."),
                "decision", McpSchema.string("Decision text. Omit to leave unchanged; empty string clears it."),
                "insight", McpSchema.string("Insight text. Omit to leave unchanged; empty string clears it."),
                "risk", McpSchema.string("Risk text. Omit to leave unchanged; empty string clears it."),
                "opportunity", McpSchema.string(
                        "Opportunity text. Omit to leave unchanged; empty string clears it."));
        return McpSchema.object(
                McpSchema.properties(
                        "date", McpSchema.string("The review date, yyyy-MM-dd."),
                        "projectId", McpSchema.integer("The Dandelion project id."),
                        "entries", McpSchema.object(verbProperties),
                        "asOf", McpSchema.object(McpSchema.properties(
                                "note", McpSchema.string(
                                        "That verb's current lastUpdated from get_work_day_review, to reject a "
                                                + "concurrent edit. Optional, per verb being changed."),
                                "decision", McpSchema.string("See \"note\"."),
                                "insight", McpSchema.string("See \"note\"."),
                                "risk", McpSchema.string("See \"note\"."),
                                "opportunity", McpSchema.string("See \"note\".")))),
                "date", "projectId", "entries");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        LocalDate date = parseDate(McpArgs.requireString(arguments, "date"));
        int projectId = McpArgs.requireInt(arguments, "projectId");
        JsonNode entriesNode = arguments == null ? null : arguments.get("entries");
        if (entriesNode == null || !entriesNode.isObject()) {
            throw new McpToolException("invalid_arguments", "\"entries\" is required and must be an object.");
        }
        Map<ProjectNarrativeVerb, String> providedFields = readVerbFields(entriesNode);

        JsonNode asOfNode = arguments.get("asOf");
        Map<ProjectNarrativeVerb, String> expectedLastUpdated = asOfNode != null && asOfNode.isObject()
                ? readVerbFields(asOfNode)
                : new LinkedHashMap<ProjectNarrativeVerb, String>();

        WebUser webUser = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        return service.save(context.getSession(), context.getWorkspaceId(), projectId, date, webUser,
                providedFields, expectedLastUpdated);
    }

    private Map<ProjectNarrativeVerb, String> readVerbFields(JsonNode node) {
        Map<ProjectNarrativeVerb, String> fields = new LinkedHashMap<ProjectNarrativeVerb, String>();
        putIfPresent(fields, node, "note", ProjectNarrativeVerb.NOTE);
        putIfPresent(fields, node, "decision", ProjectNarrativeVerb.DECISION);
        putIfPresent(fields, node, "insight", ProjectNarrativeVerb.INSIGHT);
        putIfPresent(fields, node, "risk", ProjectNarrativeVerb.RISK);
        putIfPresent(fields, node, "opportunity", ProjectNarrativeVerb.OPPORTUNITY);
        return fields;
    }

    private void putIfPresent(Map<ProjectNarrativeVerb, String> fields, JsonNode node, String key,
            ProjectNarrativeVerb verb) {
        if (node.has(key)) {
            JsonNode value = node.get(key);
            fields.put(verb, value == null || value.isNull() ? "" : value.asText());
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new McpToolException("invalid_arguments", "\"date\" must be yyyy-MM-dd.");
        }
    }
}
