package org.dandeliondaily.mcp.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.dandeliondaily.shared.service.LinkedMeetingService;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionSet;
import org.openimmunizationsoftware.pt.model.ActionSetType;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.ProjectTag;
import org.openimmunizationsoftware.pt.model.ProjectTagMap;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;
import org.openimmunizationsoftware.pt.servlet.HandleValidationSupport;

import com.fasterxml.jackson.databind.JsonNode;

/** Project creation and action-move rules shared by the apply_changes batch. */
final class McpProjectMutationService {

    private static final int MAX_PROJECT_NAME = 100;
    private static final int MAX_PROJECT_HANDLE = 60;
    private static final int MAX_TAGS = 100;

    ProjectDraft prepareProject(Session session, int workspaceId, WebUser webUser, JsonNode item,
            List<ProjectDraft> preceding) {
        ProjectDraft draft = new ProjectDraft();
        try {
            McpProjectAccess.requirePrivate(session, workspaceId, webUser);
            LinkedMeetingService.rejectUnknown(item, setOf("type", "ref", "name", "description", "billCode",
                    "tags", "currentFocus", "allowDuplicate", "projectHandle"));

            draft.ref = requiredText(item, "ref", 100);
            if (draft.ref == null) {
                draft.error = "\"ref\" is required and must be a non-empty string of at most 100 characters.";
                return draft;
            }
            if (findDraft(preceding, draft.ref) != null) {
                draft.error = "Project ref \"" + draft.ref + "\" is already used in this batch.";
                return draft;
            }

            String name = requiredText(item, "name", MAX_PROJECT_NAME);
            if (name == null) {
                draft.error = "\"name\" is required and must be a non-empty string of at most "
                        + MAX_PROJECT_NAME + " characters.";
                return draft;
            }
            draft.name = name;

            if (item.has("allowDuplicate") && !item.get("allowDuplicate").isBoolean()) {
                draft.error = "\"allowDuplicate\" must be a boolean.";
                return draft;
            }
            draft.allowDuplicate = item.path("allowDuplicate").asBoolean(false);
            if (item.has("description")) {
                draft.description = optionalText(item, "description");
                if (item.get("description") != null && !item.get("description").isNull()
                        && draft.description == null) {
                    draft.error = "\"description\" must be a string or null.";
                    return draft;
                }
            }
            if (item.has("currentFocus")) {
                draft.currentFocus = optionalText(item, "currentFocus");
                if (item.get("currentFocus") != null && !item.get("currentFocus").isNull()
                        && draft.currentFocus == null) {
                    draft.error = "\"currentFocus\" must be a string or null.";
                    return draft;
                }
            }

            String billCodeValue = requiredText(item, "billCode", 100);
            if (billCodeValue == null) {
                draft.error = "\"billCode\" is required and must be a non-empty string.";
                return draft;
            }
            BillCode billCode = ClientServlet.resolveBillCode(session, Integer.valueOf(workspaceId), billCodeValue);
            if (billCode == null) {
                draft.error = "Bill code \"" + billCodeValue + "\" was not found in this workspace.";
                return draft;
            }

            String suppliedHandle = item.has("projectHandle") ? optionalText(item, "projectHandle") : "";
            if (item.has("projectHandle") && item.get("projectHandle") != null
                    && !item.get("projectHandle").isNull() && suppliedHandle == null) {
                draft.error = "\"projectHandle\" must be a string or null.";
                return draft;
            }
            String handle = HandleValidationSupport.resolveHandle(suppliedHandle, name, MAX_PROJECT_HANDLE);
            String handleError = HandleValidationSupport.validateHandleCharacters("Project handle", handle);
            if (handleError != null || handle.length() == 0) {
                draft.error = handleError == null ? "\"projectHandle\" must not be empty." : handleError;
                return draft;
            }
            draft.handle = handle;

            draft.tags = resolveTags(session, workspaceId, item, draft);
            if (draft.error != null) {
                return draft;
            }
            for (ProjectDraft previous : preceding) {
                if (previous == null || previous.error != null) {
                    continue;
                }
                if (sameKey(previous.name, draft.name)) {
                    draft.error = "Project name \"" + draft.name + "\" is duplicated in this batch.";
                    return draft;
                }
                if (sameKey(previous.handle, draft.handle)) {
                    draft.error = "Project handle \"" + draft.handle + "\" is duplicated in this batch.";
                    return draft;
                }
            }

            List<Integer> visibleWorkspaceIds = McpProjectAccess.readableWorkspaces(session, workspaceId, webUser);
            @SuppressWarnings("unchecked")
            List<Project> visibleProjects = session.createQuery(
                    "from Project p where p.workspaceId in (:workspaceIds)").setParameterList(
                            "workspaceIds", visibleWorkspaceIds).list();
            for (Project existing : visibleProjects) {
                if (!isOpenProject(existing)) {
                    continue;
                }
                if (sameKey(existing.getProjectHandle(), draft.handle)) {
                    draft.error = "Project handle \"" + draft.handle
                            + "\" is already used by an open project visible to this user.";
                    return draft;
                }
                if (!draft.allowDuplicate && sameKey(existing.getProjectName(), draft.name)) {
                    draft.error = "An open project visible to this user already has the name \""
                            + draft.name + "\". Set allowDuplicate to true to proceed.";
                    return draft;
                }
            }

            Project project = new Project();
            project.setWorkspaceId(Integer.valueOf(workspaceId));
            project.setWebUser(webUser);
            project.setCreatedByWebUserId(Integer.valueOf(webUser.getWebUserId()));
            project.setLastModifiedByWebUserId(Integer.valueOf(webUser.getWebUserId()));
            project.setLastModifiedDate(new Date());
            project.setProjectName(draft.name);
            project.setProjectHandle(draft.handle);
            project.setProjectStatus(ProjectStatus.ACTIVE.getDatabaseValue());
            project.setDescription(draft.description);
            project.setCurrentFocusText(draft.currentFocus);
            project.setBillCode(billCode.getBillCode());
            project.setExternalManaged(false);
            draft.project = project;
            return draft;
        } catch (McpToolException e) {
            draft.error = e.getMessage();
            return draft;
        }
    }

    void applyProject(Session session, WebUser webUser, ProjectDraft draft) {
        session.save(draft.project);

        ProjectContactAssigned assignment = new ProjectContactAssigned();
        assignment.setId(new ProjectContactAssignedId(webUser.getContactId(), draft.project.getProjectId()));
        assignment.setEmailAlert("Y");
        assignment.setUpdateDue(Integer.valueOf(0));
        session.save(assignment);

        for (ProjectTag tag : draft.tags) {
            ProjectTagMap map = new ProjectTagMap();
            map.setProjectId(draft.project.getProjectId());
            map.setProjectTagId(tag.getProjectTagId());
            map.setCreatedDate(new Date());
            session.save(map);
        }
    }

    String validateMove(Session session, int workspaceId, WebUser webUser, ActionNext action, Project destination) {
        try {
            McpProjectAccess.requirePrivate(session, workspaceId, webUser);
        } catch (McpToolException e) {
            return e.getMessage();
        }
        if (action.getNextActionStatus() != ProjectNextActionStatus.READY) {
            return "Only open actions can be moved.";
        }
        if (action.isTemplate() || action.getTemplateActionNextId() != null) {
            return "template_managed: template-managed actions cannot be moved.";
        }
        ActionSet actionSet = action.getActionSet();
        if (actionSet != null && actionSet.getActionSetType() == ActionSetType.SHARED) {
            return "Shared action sets cannot be moved.";
        }
        Project source = action.getProject();
        if (source == null && action.getProjectId() > 0) {
            source = (Project) session.get(Project.class, Integer.valueOf(action.getProjectId()));
        }
        if (source == null || source.getWorkspaceId() == null
                || source.getWorkspaceId().intValue() != workspaceId) {
            return "Only actions owned by a project in this private workspace can be moved.";
        }
        if (destination == null || destination.getWorkspaceId() == null
                || destination.getWorkspaceId().intValue() != workspaceId || !isOpenProject(destination)) {
            return "The destination must be an open project in this private workspace.";
        }
        if (new McpTimeEntriesService().isCurrentEntryForAction(session, webUser, action)) {
            return "An action with today's current time entry (possibly a running timer) cannot be moved.";
        }
        return null;
    }

    void applyMove(Session session, ActionNext action, Project destination, Date nextChangeDate) {
        BillCode billCode = ClientServlet.resolveBillCode(session, destination);
        action.setProject(destination);
        action.setProjectId(destination.getProjectId());
        // Match ActionSentenceImportService/UI rules: a configured bill code may
        // explicitly be nonbillable. Its presence alone is not sufficient.
        action.setBillable(billCode != null && "Y".equalsIgnoreCase(billCode.getBillable()));
        action.setNextChangeDate(nextChangeDate);
    }

    static Date monotonicChangeDate(Date previous) {
        long now = McpActionContextSupport.truncatedNow().getTime();
        if (previous != null && now <= previous.getTime()) {
            now = previous.getTime() + 1000L;
        }
        return new Date(now);
    }

    static boolean isOpenProject(Project project) {
        if (project == null) {
            return false;
        }
        ProjectStatus status = ProjectStatus.fromDatabaseValue(project.getProjectStatus());
        return status == ProjectStatus.ACTIVE || status == ProjectStatus.PAUSED;
    }

    static boolean sameKey(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    static boolean conflictsWithMove(int movedActionId, String otherType, Integer otherActionId,
            List<ActionNext> otherOrderedActions) {
        if ("update_action".equals(otherType) || "reschedule_action".equals(otherType)
                || "split_action".equals(otherType) || "remove_action".equals(otherType)
                || "complete_action".equals(otherType)) {
            return otherActionId != null && otherActionId.intValue() == movedActionId;
        }
        if ("order_day".equals(otherType) && otherOrderedActions != null) {
            for (ActionNext action : otherOrderedActions) {
                if (action != null && action.getActionNextId() == movedActionId) {
                    return true;
                }
            }
        }
        return false;
    }

    static ProjectDraft findDraft(List<ProjectDraft> drafts, String ref) {
        if (drafts == null || ref == null) {
            return null;
        }
        for (ProjectDraft draft : drafts) {
            if (draft != null && draft.ref != null && sameKey(draft.ref, ref)) {
                return draft;
            }
        }
        return null;
    }

    private List<ProjectTag> resolveTags(Session session, int workspaceId, JsonNode item, ProjectDraft draft) {
        List<ProjectTag> resolved = new ArrayList<ProjectTag>();
        if (!item.has("tags") || item.get("tags").isNull()) {
            return resolved;
        }
        JsonNode tagsNode = item.get("tags");
        if (!tagsNode.isArray() || tagsNode.size() > MAX_TAGS) {
            draft.error = "\"tags\" must be an array of at most " + MAX_TAGS + " existing tag names/handles.";
            return resolved;
        }
        @SuppressWarnings("unchecked")
        List<ProjectTag> tags = session.createQuery(
                "from ProjectTag where workspaceId = :workspaceId")
                .setInteger("workspaceId", workspaceId).list();
        Set<Integer> seen = new HashSet<Integer>();
        for (JsonNode tagNode : tagsNode) {
            if (!tagNode.isTextual() || tagNode.textValue().trim().length() == 0) {
                draft.error = "\"tags\" must contain non-empty strings.";
                return resolved;
            }
            String requested = tagNode.textValue().trim();
            ProjectTag match = null;
            for (ProjectTag tag : tags) {
                if (ProjectTag.STATUS_ACTIVE.equalsIgnoreCase(tag.getTagStatus())
                        && (sameKey(tag.getTagName(), requested) || sameKey(tag.getTagHandle(), requested))) {
                    match = tag;
                    break;
                }
            }
            if (match == null) {
                draft.error = "Tag \"" + requested + "\" was not found as an active tag in this workspace.";
                return resolved;
            }
            if (!seen.add(Integer.valueOf(match.getProjectTagId()))) {
                draft.error = "Tag \"" + requested + "\" is repeated.";
                return resolved;
            }
            resolved.add(match);
        }
        return resolved;
    }

    private static Set<String> setOf(String... values) {
        Set<String> result = new HashSet<String>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }

    private static String requiredText(JsonNode item, String field, int maxLength) {
        if (!item.hasNonNull(field) || !item.get(field).isTextual()) {
            return null;
        }
        String value = item.get(field).textValue().trim();
        return value.length() == 0 || value.length() > maxLength ? null : value;
    }

    private static String optionalText(JsonNode item, String field) {
        JsonNode node = item.get(field);
        return node == null || node.isNull() ? null : node.isTextual() ? node.textValue().trim() : null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    static final class ProjectDraft {
        String ref;
        String name;
        String handle;
        String description;
        String currentFocus;
        boolean allowDuplicate;
        Project project;
        List<ProjectTag> tags = new ArrayList<ProjectTag>();
        String error;
    }
}
