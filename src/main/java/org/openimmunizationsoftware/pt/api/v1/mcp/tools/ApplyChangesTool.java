package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.util.List;
import java.util.Map;

import org.dandeliondaily.mcp.service.McpApplyChangesService;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpArgs;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

public class ApplyChangesTool implements McpTool {

    private final McpApplyChangesService service = new McpApplyChangesService();

    @Override
    public String getName() {
        return "apply_changes";
    }

    @Override
    public String getDescription() {
        return "Applies a coherent set of approved action changes together, atomically: either every change "
                + "applies or none do. The whole batch is validated against current data first -- if any item "
                + "references stale data (use the asOf value from a prior get_project_context/get_planning_context "
                + "read) or a template-managed action (edit those via the Dandelion UI instead), the entire batch "
                + "is rejected with a reason per item and nothing is written. Only call this after the user has "
                + "explicitly approved the proposed changes -- never treat your own suggestions as already "
                + "approved. Each entry in \"changes\" has a \"type\" of create_action, update_action, "
                + "reschedule_action, split_action, remove_action, complete_action, or order_day:\n"
                + "- create_action: {type, projectId, description, nextActionType?, scheduledDate?, deadlineDate?, "
                + "targetDate?, estimateMinutes?, notes?, linkUrl?}. Never creates a new project -- projectId must "
                + "already exist.\n"
                + "- update_action: {type, actionNextId, asOf, description?, nextActionType?, estimateMinutes?, "
                + "addNote?, linkUrl?}. Only fields present are changed; addNote appends a new note rather than "
                + "replacing existing ones; linkUrl \"\" or null clears the link. An update that only sets "
                + "linkUrl and/or addNote is also allowed on a template-generated instance (it changes only that "
                + "occurrence, not the template).\n"
                + "- reschedule_action: {type, actionNextId, asOf, scheduledDate?, deadlineDate?, targetDate?} "
                + "(yyyy-MM-dd, or null to clear a date). At least one date field required.\n"
                + "- split_action: {type, actionNextId, asOf, newActions: [{description, nextActionType?, "
                + "scheduledDate?, estimateMinutes?, linkUrl?}, ...]}. Cancels the original and creates the replacements "
                + "fresh, grouped for traceability.\n"
                + "- remove_action: {type, actionNextId, asOf, reason?}. Always a soft cancel, never a hard "
                + "delete.\n"
                + "- complete_action: {type, actionNextId, asOf, completedAt?, durationMinutes?, "
                + "completionDescription?}. Allowed on template-generated instances but not template roots.\n"
                + "- order_day: {type, date, actions: [{actionNextId, asOf}, ...]}. Sets the order of work for a "
                + "day (completionOrder), as the dashboard's up/down buttons do. Order only applies WITHIN a "
                + "dashboard bucket: the dashboard always shows buckets in a fixed order (Overdue, Start of Work "
                + "Day, Committed, Will, Personal (Morning), Might, Waiting, Will Meet, End of Work Day, Other), "
                + "so listing a Might before a Will doesn't move the Might above the Will. The listed actions go "
                + "first in their own bucket in the order given; the rest of each bucket follows in its current "
                + "order. Read the day first with get_planning_context (each action's dashboardBucket, and "
                + "\"dayOrder\") so you know the groups. Each action must be open and scheduled on date (for "
                + "today, overdue actions also count); template-generated instances can be ordered. The result "
                + "lists the new completionOrder and asOf of the ordered actions, plus any other actions on the "
                + "day that were renumbered (their asOf changed too).";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> changeItem = McpSchema.object(
                McpSchema.properties(
                        "type", McpSchema.stringEnum(
                                "Which kind of change this is; see the tool description for each type's fields.",
                                "create_action", "update_action", "reschedule_action", "split_action",
                                "remove_action", "complete_action", "order_day"),
                        "actionNextId", McpSchema.integer(
                                "Required for every type except create_action and order_day."),
                        "asOf", McpSchema.string(
                                "Required alongside actionNextId: the action's current asOf/scheduledDate context "
                                        + "timestamp from a prior read, used to detect concurrent changes."),
                        "projectId", McpSchema.integer("Required for create_action; must be an existing project."),
                        "description", McpSchema.string("Action description."),
                        "nextActionType", McpSchema.stringEnum(null,
                                ProjectNextActionType.WILL, ProjectNextActionType.WILL_CONTACT,
                                ProjectNextActionType.WILL_MEET, ProjectNextActionType.WILL_REVIEW,
                                ProjectNextActionType.WILL_DOCUMENT, ProjectNextActionType.WILL_FOLLOW_UP,
                                ProjectNextActionType.MIGHT, ProjectNextActionType.WOULD_LIKE_TO,
                                ProjectNextActionType.COMMITTED_TO, ProjectNextActionType.GOAL,
                                ProjectNextActionType.WAITING, ProjectNextActionType.OVERDUE_TO),
                        "scheduledDate", McpSchema.string("yyyy-MM-dd, or null to clear."),
                        "deadlineDate", McpSchema.string("yyyy-MM-dd, or null to clear."),
                        "targetDate", McpSchema.string("yyyy-MM-dd, or null to clear."),
                        "estimateMinutes", McpSchema.integer("0-480."),
                        "notes", McpSchema.string("Initial notes, for create_action only."),
                        "addNote", McpSchema.string("A note line to append, for update_action only."),
                        "linkUrl", McpSchema.string(
                                "A link for the action (PR, ticket, document, ...): an http(s) URL of at most "
                                        + "1200 characters. For create_action and update_action; on "
                                        + "update_action, \"\" or null clears it."),
                        "reason", McpSchema.string("Optional reason, for remove_action."),
                        "completedAt", McpSchema.string(
                                "ISO-8601 instant when the work finished (the end of the time entry, not the "
                                        + "start); defaults to now. Combined with durationMinutes to derive when "
                                        + "the work started."),
                        "durationMinutes", McpSchema.integer("Minutes actually spent; defaults to 0."),
                        "completionDescription", McpSchema.string(
                                "What was actually done; defaults to the action's own description."),
                        "newActions", McpSchema.array("For split_action: the replacement actions.",
                                McpSchema.object(McpSchema.properties(
                                        "description", McpSchema.string(null),
                                        "nextActionType", McpSchema.string(null),
                                        "scheduledDate", McpSchema.string(null),
                                        "estimateMinutes", McpSchema.integer(null),
                                        "linkUrl", McpSchema.string(null)))),
                        "date", McpSchema.string("For order_day: the day to order, yyyy-MM-dd."),
                        "actions", McpSchema.array(
                                "For order_day: the actions in the order to do them (within their buckets).",
                                McpSchema.object(McpSchema.properties(
                                        "actionNextId", McpSchema.integer(null),
                                        "asOf", McpSchema.string(null)),
                                        "actionNextId", "asOf"))),
                "type");
        return McpSchema.object(
                McpSchema.properties(
                        "changes", McpSchema.array("Up to 25 changes, applied atomically.", changeItem)),
                "changes");
    }

    @Override
    public Object call(JsonNode arguments, McpToolContext context) {
        List<JsonNode> changes = McpArgs.optNodeList(arguments, "changes");
        if (changes.isEmpty()) {
            throw new McpToolException("invalid_arguments", "\"changes\" must contain at least one item.");
        }
        WebUser webUser = McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername());
        return service.applyChanges(context.getSession(), context.getWorkspaceId(), webUser,
                context.getAgentName(), changes);
    }
}
