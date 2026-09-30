package org.dandeliondaily.mcp.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.shared.service.ActionCompletionService;
import org.dandeliondaily.shared.service.ActionCompletionService.CompletionTime;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.dao.ActionChangeLogDao;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.doa.ActionSetDao;
import org.openimmunizationsoftware.pt.model.ActionChangeLog;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionNextNote;
import org.openimmunizationsoftware.pt.model.ActionSet;
import org.openimmunizationsoftware.pt.model.ActionSetType;
import org.openimmunizationsoftware.pt.model.ActorType;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * apply_changes: the batch write-back operation. Pre-validates the entire
 * batch against current DB state (same shape as ProjectDefinitionImportService's
 * "resolve everything, fail the whole batch on any mismatch" pattern) before
 * mutating anything; if any item fails validation, nothing is applied.
 * Delegates to existing services where one exists (ActionCompletionService
 * for completion) rather than re-implementing their business rules. See
 * docs/Dandelion_Daily_AI_Integration_Assessment.md section 4 and section 6
 * decisions 6, 8, 9, 10, 14.
 */
public class McpApplyChangesService {

    private static final int MAX_CHANGES = 25;
    private static final int MAX_ESTIMATE_MINUTES = 480;
    private static final int DEFAULT_ESTIMATE_MINUTES = 15;

    private final ActionCompletionService actionCompletionService = new ActionCompletionService();

    public Map<String, Object> applyChanges(Session session, int workspaceId, WebUser webUser, String agentName,
            List<JsonNode> changeNodes) {
        if (changeNodes.isEmpty()) {
            throw new McpToolException("invalid_arguments", "\"changes\" must contain at least one item.");
        }
        if (changeNodes.size() > MAX_CHANGES) {
            throw new McpToolException("invalid_arguments",
                    "Too many changes in one batch; maximum is " + MAX_CHANGES + ".");
        }

        List<PreparedChange> prepared = new ArrayList<PreparedChange>();
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        boolean allValid = true;
        for (int i = 0; i < changeNodes.size(); i++) {
            PreparedChange pc = validateItem(session, workspaceId, webUser, i, changeNodes.get(i));
            prepared.add(pc);
            Map<String, Object> resultItem = new LinkedHashMap<String, Object>();
            resultItem.put("index", Integer.valueOf(i));
            resultItem.put("type", pc.type);
            if (pc.actionNextId != null) {
                resultItem.put("actionNextId", pc.actionNextId);
            }
            if (pc.error != null) {
                allValid = false;
                resultItem.put("status", "rejected");
                resultItem.put("reason", pc.error);
            } else {
                resultItem.put("status", "valid");
            }
            results.add(resultItem);
        }

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        if (!allValid) {
            response.put("applied", Boolean.FALSE);
            response.put("message", "Batch rejected: one or more changes failed validation. Nothing was applied.");
            response.put("results", results);
            return response;
        }

        ActionSet batchActionSet = null;
        for (int i = 0; i < prepared.size(); i++) {
            PreparedChange pc = prepared.get(i);
            Map<String, Object> resultItem = results.get(i);
            if ("create_action".equals(pc.type)) {
                batchActionSet = applyCreate(session, webUser, pc, batchActionSet, resultItem);
            } else if ("update_action".equals(pc.type)) {
                applyUpdate(session, webUser, pc);
            } else if ("reschedule_action".equals(pc.type)) {
                applyReschedule(session, pc);
            } else if ("split_action".equals(pc.type)) {
                batchActionSet = applySplit(session, webUser, pc, batchActionSet, resultItem);
            } else if ("remove_action".equals(pc.type)) {
                applyRemove(session, pc);
            } else if ("complete_action".equals(pc.type)) {
                applyComplete(session, webUser, pc);
            }
            resultItem.put("status", "applied");
            logChange(webUser, agentName, pc);
        }

        response.put("applied", Boolean.TRUE);
        response.put("message", "All " + prepared.size() + " changes applied.");
        response.put("results", results);
        return response;
    }

    // ---- validation ----

    private PreparedChange validateItem(Session session, int workspaceId, WebUser webUser, int index, JsonNode item) {
        PreparedChange pc = new PreparedChange();
        pc.index = index;
        if (item == null || !item.isObject()) {
            pc.error = "Each change must be an object.";
            return pc;
        }
        String type = item.hasNonNull("type") ? item.get("type").asText() : null;
        pc.type = type;
        if (type == null) {
            pc.error = "\"type\" is required.";
            return pc;
        }
        if ("create_action".equals(type)) {
            validateCreate(session, workspaceId, item, pc);
        } else if ("update_action".equals(type)) {
            validateUpdate(session, workspaceId, item, pc);
        } else if ("reschedule_action".equals(type)) {
            validateReschedule(session, workspaceId, item, pc);
        } else if ("split_action".equals(type)) {
            validateSplit(session, workspaceId, item, pc);
        } else if ("remove_action".equals(type)) {
            validateRemove(session, workspaceId, item, pc);
        } else if ("complete_action".equals(type)) {
            validateComplete(session, workspaceId, webUser, item, pc);
        } else {
            pc.error = "Unknown change type \"" + type + "\".";
        }
        return pc;
    }

    private void validateCreate(Session session, int workspaceId, JsonNode item, PreparedChange pc) {
        if (!item.hasNonNull("projectId")) {
            pc.error = "\"projectId\" is required.";
            return;
        }
        Project project = requireProjectInWorkspace(session, workspaceId, item.get("projectId").asInt());
        if (project == null) {
            pc.error = "Project not found.";
            return;
        }
        if (!item.hasNonNull("description") || item.get("description").asText().trim().length() == 0) {
            pc.error = "\"description\" is required.";
            return;
        }
        String actionType = item.hasNonNull("nextActionType") ? item.get("nextActionType").asText()
                : ProjectNextActionType.WILL;
        if (!isValidActionType(actionType)) {
            pc.error = "Invalid nextActionType \"" + actionType + "\".";
            return;
        }
        pc.project = project;
        pc.newDescription = item.get("description").asText().trim();
        pc.newActionType = actionType;
        pc.newScheduledDate = parseNullableDate(item, "scheduledDate", pc);
        if (pc.error != null) {
            return;
        }
        pc.newDeadlineDate = parseNullableDate(item, "deadlineDate", pc);
        if (pc.error != null) {
            return;
        }
        pc.newTargetDate = parseNullableDate(item, "targetDate", pc);
        if (pc.error != null) {
            return;
        }
        pc.newEstimateMinutes = Integer.valueOf(item.hasNonNull("estimateMinutes")
                ? clampEstimate(item.get("estimateMinutes").asInt())
                : DEFAULT_ESTIMATE_MINUTES);
        pc.newNotes = item.hasNonNull("notes") ? item.get("notes").asText() : null;
    }

    private void validateUpdate(Session session, int workspaceId, JsonNode item, PreparedChange pc) {
        if (!resolveAndCheckAction(session, workspaceId, item, pc, false)) {
            return;
        }
        if (item.has("description")) {
            pc.hasDescription = true;
            String description = item.get("description").isNull() ? "" : item.get("description").asText().trim();
            if (description.length() == 0) {
                pc.error = "\"description\" cannot be empty.";
                return;
            }
            pc.description = description;
        }
        if (item.has("nextActionType")) {
            pc.hasActionType = true;
            String type = item.get("nextActionType").asText();
            if (!isValidActionType(type)) {
                pc.error = "Invalid nextActionType \"" + type + "\".";
                return;
            }
            pc.actionType = type;
        }
        if (item.has("estimateMinutes")) {
            pc.hasEstimate = true;
            pc.estimateMinutes = Integer.valueOf(clampEstimate(item.get("estimateMinutes").asInt()));
        }
        if (item.has("addNote")) {
            pc.hasAddNote = true;
            pc.addNote = item.get("addNote").isNull() ? null : item.get("addNote").asText();
        }
        if (!pc.hasDescription && !pc.hasActionType && !pc.hasEstimate && !pc.hasAddNote) {
            pc.error = "At least one of description, nextActionType, estimateMinutes, addNote must be provided.";
        }
    }

    private void validateReschedule(Session session, int workspaceId, JsonNode item, PreparedChange pc) {
        if (!resolveAndCheckAction(session, workspaceId, item, pc, false)) {
            return;
        }
        if (item.has("scheduledDate")) {
            pc.hasScheduledDate = true;
            pc.scheduledDateValue = parseNullableDate(item, "scheduledDate", pc);
            if (pc.error != null) {
                return;
            }
        }
        if (item.has("deadlineDate")) {
            pc.hasDeadlineDate = true;
            pc.deadlineDateValue = parseNullableDate(item, "deadlineDate", pc);
            if (pc.error != null) {
                return;
            }
        }
        if (item.has("targetDate")) {
            pc.hasTargetDate = true;
            pc.targetDateValue = parseNullableDate(item, "targetDate", pc);
            if (pc.error != null) {
                return;
            }
        }
        if (!pc.hasScheduledDate && !pc.hasDeadlineDate && !pc.hasTargetDate) {
            pc.error = "At least one of scheduledDate, deadlineDate, targetDate must be provided.";
        }
    }

    private void validateSplit(Session session, int workspaceId, JsonNode item, PreparedChange pc) {
        if (!resolveAndCheckAction(session, workspaceId, item, pc, false)) {
            return;
        }
        if (!item.has("newActions") || !item.get("newActions").isArray() || item.get("newActions").size() == 0) {
            pc.error = "\"newActions\" must be a non-empty array.";
            return;
        }
        List<NewActionSpec> specs = new ArrayList<NewActionSpec>();
        for (JsonNode newActionNode : item.get("newActions")) {
            NewActionSpec spec = new NewActionSpec();
            if (!newActionNode.hasNonNull("description")
                    || newActionNode.get("description").asText().trim().length() == 0) {
                pc.error = "Each entry in \"newActions\" requires a non-empty \"description\".";
                return;
            }
            spec.description = newActionNode.get("description").asText().trim();
            String type = newActionNode.hasNonNull("nextActionType") ? newActionNode.get("nextActionType").asText()
                    : pc.action.getNextActionType();
            if (!isValidActionType(type)) {
                pc.error = "Invalid nextActionType \"" + type + "\" in newActions.";
                return;
            }
            spec.actionType = type;
            if (newActionNode.hasNonNull("scheduledDate")) {
                try {
                    spec.scheduledDate = java.sql.Date.valueOf(LocalDate.parse(newActionNode.get("scheduledDate")
                            .asText()));
                } catch (DateTimeParseException e) {
                    pc.error = "Invalid \"scheduledDate\" in newActions; must be yyyy-MM-dd.";
                    return;
                }
            }
            spec.estimateMinutes = Integer.valueOf(newActionNode.hasNonNull("estimateMinutes")
                    ? clampEstimate(newActionNode.get("estimateMinutes").asInt())
                    : DEFAULT_ESTIMATE_MINUTES);
            specs.add(spec);
        }
        pc.newActions = specs;
    }

    private void validateRemove(Session session, int workspaceId, JsonNode item, PreparedChange pc) {
        if (!resolveAndCheckAction(session, workspaceId, item, pc, false)) {
            return;
        }
        pc.removeReason = item.hasNonNull("reason") ? item.get("reason").asText() : null;
    }

    private void validateComplete(Session session, int workspaceId, WebUser webUser, JsonNode item,
            PreparedChange pc) {
        if (!resolveAndCheckAction(session, workspaceId, item, pc, true)) {
            return;
        }
        // "completedAt" is when the work finished (the end of the time entry), matching how a
        // person would naturally describe it ("I finished this just now, took me 10 minutes").
        // ActionCompletionService's completionMoment parameter is the START of the entry, so it
        // has to be derived by subtracting the duration -- passing completedAt straight through
        // as the start made any positive duration push the computed end into the future and fail
        // validation unconditionally (found in live testing).
        Date completedAtEnd;
        if (item.hasNonNull("completedAt")) {
            try {
                completedAtEnd = Date.from(java.time.Instant.parse(item.get("completedAt").asText()));
            } catch (java.time.format.DateTimeParseException e) {
                pc.error = "\"completedAt\" must be an ISO-8601 instant, e.g. 2026-10-01T17:00:00Z.";
                return;
            }
        } else {
            completedAtEnd = McpActionContextSupport.truncatedNow();
        }
        int durationMinutes = item.hasNonNull("durationMinutes") ? item.get("durationMinutes").asInt() : 0;
        if (durationMinutes < 0) {
            pc.error = "\"durationMinutes\" cannot be negative.";
            return;
        }
        if (durationMinutes > ActionCompletionService.MAX_DURATION_MINS) {
            pc.error = "\"durationMinutes\" cannot be more than " + ActionCompletionService.MAX_DURATION_MINS + ".";
            return;
        }
        Date completionStart = new Date(completedAtEnd.getTime() - (durationMinutes * 60000L));
        CompletionTime completionTime = CompletionTime.of(completionStart, durationMinutes);
        String validationError = actionCompletionService.validateCompletion(webUser, session, pc.action,
                completionTime);
        if (validationError != null) {
            pc.error = validationError;
            return;
        }
        pc.completedAt = completionStart;
        pc.durationMinutes = durationMinutes;
        pc.completionDescription = item.hasNonNull("completionDescription")
                ? item.get("completionDescription").asText()
                : pc.action.getNextDescription();
    }

    private boolean resolveAndCheckAction(Session session, int workspaceId, JsonNode item, PreparedChange pc,
            boolean isComplete) {
        if (!item.hasNonNull("actionNextId") || item.get("actionNextId").asInt() <= 0) {
            pc.error = "\"actionNextId\" is required.";
            return false;
        }
        int actionNextId = item.get("actionNextId").asInt();
        pc.actionNextId = Integer.valueOf(actionNextId);
        ActionNext action = (ActionNext) session.get(ActionNext.class, Integer.valueOf(actionNextId));
        if (action == null || action.getWorkspaceId() == null || action.getWorkspaceId().intValue() != workspaceId) {
            pc.error = "Action not found.";
            return false;
        }
        boolean isRoot = action.isTemplate();
        boolean isInstance = action.getTemplateActionNextId() != null;
        if (isComplete) {
            if (isRoot) {
                pc.error = "template_managed: cannot complete a template root; edit templates via the Dandelion UI.";
                return false;
            }
        } else if (isRoot || isInstance) {
            pc.error = "template_managed: template-managed actions can only be changed via the Dandelion UI "
                    + "(completing a generated instance is the one exception).";
            return false;
        }
        if (!item.hasNonNull("asOf")) {
            pc.error = "\"asOf\" is required.";
            return false;
        }
        String asOf = item.get("asOf").asText();
        String currentAsOf = McpActionContextSupport.toIso(action.getNextChangeDate());
        if (currentAsOf == null || !currentAsOf.equals(asOf)) {
            pc.error = "stale: action has changed since it was last read"
                    + (currentAsOf != null ? " (current asOf: " + currentAsOf + ")" : "") + ".";
            return false;
        }
        pc.action = action;
        Project project = action.getProject();
        if (project == null && action.getProjectId() > 0) {
            project = (Project) session.get(Project.class, Integer.valueOf(action.getProjectId()));
        }
        pc.project = project;
        return true;
    }

    // ---- apply ----

    private ActionSet applyCreate(Session session, WebUser webUser, PreparedChange pc, ActionSet batchActionSet,
            Map<String, Object> resultItem) {
        ActionNext action = new ActionNext();
        action.setProject(pc.project);
        action.setProjectId(pc.project.getProjectId());
        action.setWorkspaceId(pc.project.getWorkspaceId());
        action.setContact(webUser.getProjectContact());
        action.setContactId(webUser.getContactId());
        action.setNextActionType(pc.newActionType);
        action.setNextDescription(pc.newDescription);
        action.setNextSummary(pc.newDescription);
        action.setNextTimeEstimate(pc.newEstimateMinutes);
        action.setNextActionStatus(ProjectNextActionStatus.READY);
        action.setNextActionDate(pc.newScheduledDate);
        action.setNextDeadlineDate(pc.newDeadlineDate);
        action.setNextTargetDate(pc.newTargetDate);
        action.setNextChangeDate(McpActionContextSupport.truncatedNow());
        action.setPriorityLevel(ProjectNextActionType.defaultPriority(pc.newActionType));
        action.setBillable(isBillable(pc.project));
        ActionSet actionSet = batchActionSet != null ? batchActionSet
                : new ActionSetDao(session).createActionSet(webUser, ActionSetType.STANDARD);
        action.setActionSet(actionSet);
        session.save(action);
        if (pc.newNotes != null && pc.newNotes.trim().length() > 0) {
            action.setNextNotes(pc.newNotes);
            session.update(action);
        }
        pc.action = action;
        resultItem.put("createdActionNextId", Integer.valueOf(action.getActionNextId()));
        return actionSet;
    }

    private void applyUpdate(Session session, WebUser webUser, PreparedChange pc) {
        ActionNext action = pc.action;
        if (pc.hasDescription) {
            action.setNextDescription(pc.description);
        }
        if (pc.hasActionType) {
            action.setNextActionType(pc.actionType);
        }
        if (pc.hasEstimate) {
            action.setNextTimeEstimate(pc.estimateMinutes);
        }
        if (pc.hasAddNote && pc.addNote != null && pc.addNote.trim().length() > 0) {
            ActionNextNote note = new ActionNextNote();
            note.setActionNext(action);
            note.setContactId(webUser.getContactId());
            note.setNoteLine(pc.addNote.trim());
            note.setNoteDate(new Date());
            session.save(note);
        }
        action.setNextChangeDate(McpActionContextSupport.truncatedNow());
        session.update(action);
    }

    private void applyReschedule(Session session, PreparedChange pc) {
        ActionNext action = pc.action;
        if (pc.hasScheduledDate) {
            action.setNextActionDate(pc.scheduledDateValue);
        }
        if (pc.hasDeadlineDate) {
            action.setNextDeadlineDate(pc.deadlineDateValue);
        }
        if (pc.hasTargetDate) {
            action.setNextTargetDate(pc.targetDateValue);
        }
        action.setNextChangeDate(McpActionContextSupport.truncatedNow());
        session.update(action);
    }

    private ActionSet applySplit(Session session, WebUser webUser, PreparedChange pc, ActionSet batchActionSet,
            Map<String, Object> resultItem) {
        ActionNext original = pc.action;
        original.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
        original.setNextChangeDate(McpActionContextSupport.truncatedNow());
        session.update(original);

        ActionSet actionSet = batchActionSet != null ? batchActionSet
                : new ActionSetDao(session).createActionSet(webUser, ActionSetType.STANDARD);
        List<Integer> createdIds = new ArrayList<Integer>();
        List<ActionNext> createdActions = new ArrayList<ActionNext>();
        for (NewActionSpec spec : pc.newActions) {
            ActionNext action = new ActionNext();
            action.setProject(pc.project);
            action.setProjectId(pc.project.getProjectId());
            action.setWorkspaceId(pc.project.getWorkspaceId());
            action.setContact(webUser.getProjectContact());
            action.setContactId(webUser.getContactId());
            action.setNextActionType(spec.actionType);
            action.setNextDescription(spec.description);
            action.setNextSummary(spec.description);
            action.setNextTimeEstimate(spec.estimateMinutes);
            action.setNextActionStatus(ProjectNextActionStatus.READY);
            action.setNextActionDate(spec.scheduledDate);
            action.setNextChangeDate(McpActionContextSupport.truncatedNow());
            action.setPriorityLevel(ProjectNextActionType.defaultPriority(spec.actionType));
            action.setBillable(isBillable(pc.project));
            action.setActionSet(actionSet);
            session.save(action);
            createdIds.add(Integer.valueOf(action.getActionNextId()));
            createdActions.add(action);
        }
        pc.createdActions = createdActions;
        resultItem.put("cancelledActionNextId", Integer.valueOf(original.getActionNextId()));
        resultItem.put("createdActionNextIds", createdIds);
        return actionSet;
    }

    private void applyRemove(Session session, PreparedChange pc) {
        pc.action.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
        pc.action.setNextChangeDate(McpActionContextSupport.truncatedNow());
        session.update(pc.action);
    }

    private void applyComplete(Session session, WebUser webUser, PreparedChange pc) {
        actionCompletionService.applyCompletion(session, webUser, pc.action, pc.completionDescription,
                ProjectNextActionStatus.COMPLETED, pc.completedAt, pc.durationMinutes);
    }

    // ---- audit trail ----

    private void logChange(WebUser webUser, String agentName, PreparedChange pc) {
        if ("split_action".equals(pc.type)) {
            writeChangeLog(webUser, agentName, pc.action, pc.project, "split_action",
                    "Split into actions " + describeCreatedIds(pc.createdActions));
            for (ActionNext created : pc.createdActions) {
                writeChangeLog(webUser, agentName, created, pc.project, "split_action",
                        "Created from split of action #" + pc.actionNextId);
            }
            return;
        }
        String changeReason = "remove_action".equals(pc.type) ? pc.removeReason : null;
        writeChangeLog(webUser, agentName, pc.action, pc.project, pc.type, changeReason);
    }

    private String describeCreatedIds(List<ActionNext> createdActions) {
        StringBuilder sb = new StringBuilder();
        for (ActionNext action : createdActions) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("#").append(action.getActionNextId());
        }
        return sb.toString();
    }

    private void writeChangeLog(WebUser webUser, String agentName, ActionNext action, Project project,
            String changeSummary, String changeReason) {
        ActionChangeLog log = new ActionChangeLog();
        log.setAction(action);
        log.setProject(project);
        log.setChangeDate(new Date());
        log.setActorType(ActorType.AI);
        log.setActorId(clip(agentName != null ? agentName : "mcp", 60));
        log.setSourceType(clip("mcp_apply_changes", 24));
        log.setChangeSummary(clip(changeSummary, 4000));
        log.setChangeReason(changeReason);
        new ActionChangeLogDao().save(log);
    }

    // ---- shared helpers ----

    private boolean isBillable(Project project) {
        return project.getBillCode() != null && project.getBillCode().trim().length() > 0;
    }

    private boolean isValidActionType(String value) {
        return ProjectNextActionType.WILL.equals(value)
                || ProjectNextActionType.WILL_CONTACT.equals(value)
                || ProjectNextActionType.WILL_MEET.equals(value)
                || ProjectNextActionType.WILL_REVIEW.equals(value)
                || ProjectNextActionType.WILL_DOCUMENT.equals(value)
                || ProjectNextActionType.WILL_FOLLOW_UP.equals(value)
                || ProjectNextActionType.MIGHT.equals(value)
                || ProjectNextActionType.WOULD_LIKE_TO.equals(value)
                || ProjectNextActionType.COMMITTED_TO.equals(value)
                || ProjectNextActionType.GOAL.equals(value)
                || ProjectNextActionType.WAITING.equals(value)
                || ProjectNextActionType.OVERDUE_TO.equals(value);
    }

    private int clampEstimate(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > MAX_ESTIMATE_MINUTES) {
            return MAX_ESTIMATE_MINUTES;
        }
        return value;
    }

    private Date parseNullableDate(JsonNode item, String field, PreparedChange pc) {
        if (!item.has(field)) {
            return null;
        }
        JsonNode node = item.get(field);
        if (node.isNull()) {
            return null;
        }
        try {
            return java.sql.Date.valueOf(LocalDate.parse(node.asText()));
        } catch (DateTimeParseException e) {
            pc.error = "\"" + field + "\" must be yyyy-MM-dd or null.";
            return null;
        }
    }

    private Project requireProjectInWorkspace(Session session, int workspaceId, int projectId) {
        Query query = session.createQuery(
                "from Project p where p.projectId = :projectId and p.workspaceId = :workspaceId");
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        return (Project) query.uniqueResult();
    }

    private String clip(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    // ---- internal structures ----

    private static class PreparedChange {
        int index;
        String type;
        String error;
        Integer actionNextId;
        ActionNext action;
        Project project;

        // create_action
        String newDescription;
        String newActionType;
        Date newScheduledDate;
        Date newDeadlineDate;
        Date newTargetDate;
        Integer newEstimateMinutes;
        String newNotes;

        // update_action (presence flags)
        boolean hasDescription;
        String description;
        boolean hasActionType;
        String actionType;
        boolean hasEstimate;
        Integer estimateMinutes;
        boolean hasAddNote;
        String addNote;

        // reschedule_action (presence flags; null value means clear)
        boolean hasScheduledDate;
        Date scheduledDateValue;
        boolean hasDeadlineDate;
        Date deadlineDateValue;
        boolean hasTargetDate;
        Date targetDateValue;

        // remove_action
        String removeReason;

        // complete_action
        Date completedAt;
        int durationMinutes;
        String completionDescription;

        // split_action
        List<NewActionSpec> newActions;
        List<ActionNext> createdActions;
    }

    private static class NewActionSpec {
        String description;
        String actionType;
        Date scheduledDate;
        Integer estimateMinutes;
    }
}
