package org.dandeliondaily.dashboard.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.doa.ProjectPatchLinkDao;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionSetType;
import org.openimmunizationsoftware.pt.model.ActionTaken;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectPatchLink;
import org.openimmunizationsoftware.pt.model.ProjectStatus;

public class ProjectDisplayLabelService {

    public static class ProjectDisplayContext {
        private final Project project;
        private final String displayName;
        private final List<Project> linkedPatchProjects;

        public ProjectDisplayContext(Project project, String displayName, List<Project> linkedPatchProjects) {
            this.project = project;
            this.displayName = displayName;
            this.linkedPatchProjects = linkedPatchProjects;
        }

        public Project getProject() {
            return project;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<Project> getLinkedPatchProjects() {
            return linkedPatchProjects;
        }
    }

    public ProjectDisplayContext buildDisplayContext(Session dataSession, Project project) {
        List<Project> linkedPatchProjects = resolveLinkedPatchProjects(dataSession, project);
        return new ProjectDisplayContext(project, buildDisplayName(project, linkedPatchProjects), linkedPatchProjects);
    }

    public String buildDisplayName(Session dataSession, Project project) {
        return buildDisplayContext(dataSession, project).getDisplayName();
    }

    public Map<Integer, String> buildDisplayNameMap(Session dataSession, List<Project> projects) {
        Map<Integer, String> displayNameByProjectId = new LinkedHashMap<Integer, String>();
        if (projects == null) {
            return displayNameByProjectId;
        }
        for (Project project : projects) {
            if (project == null || project.getProjectId() <= 0
                    || displayNameByProjectId.containsKey(project.getProjectId())) {
                continue;
            }
            displayNameByProjectId.put(project.getProjectId(), buildDisplayName(dataSession, project));
        }
        return displayNameByProjectId;
    }

    public String buildDisplayName(Project project, List<Project> linkedPatchProjects) {
        String projectName = n(project == null ? null : project.getProjectName());
        if (projectName.length() > 0) {
            return projectName;
        }
        if (linkedPatchProjects == null || linkedPatchProjects.isEmpty()) {
            return projectName;
        }
        for (Project linkedPatchProject : linkedPatchProjects) {
            String compositeName = buildCompositeDisplayName(project, linkedPatchProject);
            if (compositeName.length() > 0) {
                return compositeName;
            }
        }
        return projectName;
    }

    public String buildCompositeDisplayName(Project privateProject, Project linkedPatchProject) {
        String privateName = n(privateProject == null ? null : privateProject.getProjectName());
        String sharedName = n(linkedPatchProject == null ? null : linkedPatchProject.getProjectName());
        if (privateName.length() == 0) {
            return sharedName;
        }
        if (sharedName.length() == 0) {
            return privateName;
        }
        return privateName + " / " + sharedName;
    }

    public List<Project> resolveLinkedPatchProjects(Session dataSession, Project privateProject) {
        List<Project> linkedProjects = new ArrayList<Project>();
        if (dataSession == null || privateProject == null || privateProject.getProjectId() <= 0
                || privateProject.getLinkedPatchWorkspaceId() == null) {
            return linkedProjects;
        }

        int patchWorkspaceId = privateProject.getLinkedPatchWorkspaceId().intValue();
        Map<Integer, Project> byProjectId = new LinkedHashMap<Integer, Project>();
        ProjectPatchLinkDao dao = new ProjectPatchLinkDao(dataSession);
        List<ProjectPatchLink> links = dao.listLinksForProject(privateProject.getProjectId());
        for (ProjectPatchLink link : links) {
            if (link == null || link.getPatchWorkspaceId() != patchWorkspaceId) {
                continue;
            }
            if (ProjectPatchLink.LINK_TYPE_DIRECT_PROJECT.equals(link.getLinkType())) {
                if (link.getLinkedPatchProjectId() != null) {
                    Project project = (Project) dataSession.get(Project.class, link.getLinkedPatchProjectId());
                    addIfValidLinkedProject(byProjectId, project, patchWorkspaceId);
                }
            } else if (ProjectPatchLink.LINK_TYPE_PATCH_TAG.equals(link.getLinkType())
                    && link.getLinkedPatchTagId() != null) {
                Query query = dataSession.createQuery(
                        "from Project p where p.workspaceId = :workspaceId"
                                + " and (p.projectStatus is null or p.projectStatus <> :closedStatus)"
                                + " and exists (select 1 from ProjectTagMap ptm where ptm.projectId = p.projectId and ptm.projectTagId = :tagId)"
                                + " order by p.priorityLevel desc, p.projectName");
                query.setParameter("workspaceId", patchWorkspaceId);
                query.setParameter("closedStatus", ProjectStatus.CLOSED.getDatabaseValue());
                query.setParameter("tagId", link.getLinkedPatchTagId());
                @SuppressWarnings("unchecked")
                List<Project> tagProjects = query.list();
                for (Project project : tagProjects) {
                    addIfValidLinkedProject(byProjectId, project, patchWorkspaceId);
                }
            }
        }
        linkedProjects.addAll(byProjectId.values());
        return linkedProjects;
    }

    public String buildActionDisplayName(Session dataSession, ActionNext action, Integer privateWorkspaceId) {
        if (action == null) {
            return "";
        }
        List<ActionNext> actions = new ArrayList<ActionNext>();
        actions.add(action);
        Map<Integer, String> byActionId = buildActionDisplayNameMap(dataSession, actions, privateWorkspaceId);
        String displayName = byActionId.get(Integer.valueOf(action.getActionNextId()));
        if (displayName != null) {
            return displayName;
        }
        return action.getProject() == null ? "" : n(action.getProject().getProjectName());
    }

    public Map<Integer, String> buildActionDisplayNameMap(Session dataSession, List<ActionNext> actions,
            Integer privateWorkspaceId) {
        Map<Integer, String> displayNameByActionId = new LinkedHashMap<Integer, String>();
        if (actions == null || actions.isEmpty()) {
            return displayNameByActionId;
        }

        Set<Integer> sharedActionSetIds = new LinkedHashSet<Integer>();
        for (ActionNext action : actions) {
            if (action == null || action.getProject() == null) {
                continue;
            }
            displayNameByActionId.put(Integer.valueOf(action.getActionNextId()),
                    n(action.getProject().getProjectName()));
            ActionSetType actionSetType = action.getActionSet() == null ? null
                    : action.getActionSet().getActionSetType();
            if (isSharedPrivateWorkspaceAction(actionSetType, action.getProject(), privateWorkspaceId,
                    action.getActionSet() == null ? 0 : action.getActionSet().getActionSetId())) {
                sharedActionSetIds.add(Integer.valueOf(action.getActionSet().getActionSetId()));
            }
        }

        if (sharedActionSetIds.isEmpty()) {
            return displayNameByActionId;
        }

        Query siblingQuery = dataSession.createQuery(
                "select distinct an from ActionNext an left join fetch an.project where an.actionSet.actionSetId in (:actionSetIds)");
        siblingQuery.setParameterList("actionSetIds", sharedActionSetIds);
        @SuppressWarnings("unchecked")
        List<ActionNext> siblingActions = siblingQuery.list();

        Map<Integer, List<Project>> sharedProjectByActionSetId = indexSharedProjectsForActionSet(siblingActions,
                privateWorkspaceId, true);
        for (ActionNext action : actions) {
            if (action == null || action.getProject() == null || action.getActionSet() == null) {
                continue;
            }
            ActionSetType actionSetType = action.getActionSet().getActionSetType();
            if (!isSharedPrivateWorkspaceAction(actionSetType, action.getProject(), privateWorkspaceId,
                    action.getActionSet().getActionSetId())) {
                continue;
            }
            Project sharedProject = resolveSingleSharedProject(sharedProjectByActionSetId,
                    action.getActionSet().getActionSetId(), action.getProject().getProjectId());
            if (sharedProject != null) {
                displayNameByActionId.put(Integer.valueOf(action.getActionNextId()),
                        buildCompositeDisplayName(action.getProject(), sharedProject));
            }
        }
        return displayNameByActionId;
    }

    public String buildActionTakenDisplayName(Session dataSession, ActionTaken actionTaken,
            Integer privateWorkspaceId) {
        if (actionTaken == null) {
            return "";
        }
        List<ActionTaken> actionTakenList = new ArrayList<ActionTaken>();
        actionTakenList.add(actionTaken);
        Map<Integer, String> byActionTakenId = buildActionTakenDisplayNameMap(dataSession, actionTakenList,
                privateWorkspaceId);
        String displayName = byActionTakenId.get(Integer.valueOf(actionTaken.getActionTakenId()));
        if (displayName != null) {
            return displayName;
        }
        return actionTaken.getProject() == null ? "" : n(actionTaken.getProject().getProjectName());
    }

    public Map<Integer, String> buildActionTakenDisplayNameMap(Session dataSession, List<ActionTaken> actionTakenList,
            Integer privateWorkspaceId) {
        Map<Integer, String> displayNameByActionTakenId = new LinkedHashMap<Integer, String>();
        if (actionTakenList == null || actionTakenList.isEmpty()) {
            return displayNameByActionTakenId;
        }

        Set<Integer> sharedActionSetIds = new LinkedHashSet<Integer>();
        for (ActionTaken actionTaken : actionTakenList) {
            if (actionTaken == null || actionTaken.getProject() == null) {
                continue;
            }
            displayNameByActionTakenId.put(Integer.valueOf(actionTaken.getActionTakenId()),
                    n(actionTaken.getProject().getProjectName()));
            ActionSetType actionSetType = actionTaken.getActionSet() == null ? null
                    : actionTaken.getActionSet().getActionSetType();
            if (isSharedPrivateWorkspaceAction(actionSetType, actionTaken.getProject(), privateWorkspaceId,
                    actionTaken.getActionSet() == null ? 0 : actionTaken.getActionSet().getActionSetId())) {
                sharedActionSetIds.add(Integer.valueOf(actionTaken.getActionSet().getActionSetId()));
            }
        }

        if (sharedActionSetIds.isEmpty()) {
            return displayNameByActionTakenId;
        }

        Query siblingQuery = dataSession.createQuery(
                "select distinct at from ActionTaken at left join fetch at.project where at.actionSet.actionSetId in (:actionSetIds)");
        siblingQuery.setParameterList("actionSetIds", sharedActionSetIds);
        @SuppressWarnings("unchecked")
        List<ActionTaken> siblingTaken = siblingQuery.list();

        Map<Integer, List<Project>> sharedProjectByActionSetId = indexSharedProjectsForActionSet(siblingTaken,
                privateWorkspaceId, false);
        for (ActionTaken actionTaken : actionTakenList) {
            if (actionTaken == null || actionTaken.getProject() == null || actionTaken.getActionSet() == null) {
                continue;
            }
            ActionSetType actionSetType = actionTaken.getActionSet().getActionSetType();
            if (!isSharedPrivateWorkspaceAction(actionSetType, actionTaken.getProject(), privateWorkspaceId,
                    actionTaken.getActionSet().getActionSetId())) {
                continue;
            }
            Project sharedProject = resolveSingleSharedProject(sharedProjectByActionSetId,
                    actionTaken.getActionSet().getActionSetId(), actionTaken.getProject().getProjectId());
            if (sharedProject != null) {
                displayNameByActionTakenId.put(Integer.valueOf(actionTaken.getActionTakenId()),
                        buildCompositeDisplayName(actionTaken.getProject(), sharedProject));
            }
        }

        return displayNameByActionTakenId;
    }

    private boolean isSharedPrivateWorkspaceAction(ActionSetType actionSetType, Project project,
            Integer privateWorkspaceId, int actionSetId) {
        if (actionSetType == null || project == null || project.getWorkspaceId() == null || privateWorkspaceId == null
                || actionSetId <= 0) {
            return false;
        }
        return ActionSetType.SHARED.equals(actionSetType)
                && privateWorkspaceId.equals(project.getWorkspaceId());
    }

    private Map<Integer, List<Project>> indexSharedProjectsForActionSet(List<?> actions,
            Integer privateWorkspaceId, boolean actionNextRows) {
        Map<Integer, List<Project>> sharedProjectsByActionSetId = new LinkedHashMap<Integer, List<Project>>();
        if (actions == null || privateWorkspaceId == null) {
            return sharedProjectsByActionSetId;
        }
        for (Object row : actions) {
            if (row == null) {
                continue;
            }
            int actionSetId = 0;
            Project project = null;
            if (actionNextRows && row instanceof ActionNext) {
                ActionNext action = (ActionNext) row;
                if (action.getActionSet() != null) {
                    actionSetId = action.getActionSet().getActionSetId();
                }
                project = action.getProject();
            } else if (!actionNextRows && row instanceof ActionTaken) {
                ActionTaken actionTaken = (ActionTaken) row;
                if (actionTaken.getActionSet() != null) {
                    actionSetId = actionTaken.getActionSet().getActionSetId();
                }
                project = actionTaken.getProject();
            }
            if (actionSetId <= 0 || project == null || project.getWorkspaceId() == null
                    || privateWorkspaceId.equals(project.getWorkspaceId())) {
                continue;
            }
            List<Project> sharedProjects = sharedProjectsByActionSetId.get(Integer.valueOf(actionSetId));
            if (sharedProjects == null) {
                sharedProjects = new ArrayList<Project>();
                sharedProjectsByActionSetId.put(Integer.valueOf(actionSetId), sharedProjects);
            }
            if (!containsProject(sharedProjects, project.getProjectId())) {
                sharedProjects.add(project);
            }
        }
        return sharedProjectsByActionSetId;
    }

    private Project resolveSingleSharedProject(Map<Integer, List<Project>> sharedProjectsByActionSetId,
            int actionSetId, int privateProjectId) {
        List<Project> sharedProjects = sharedProjectsByActionSetId.get(Integer.valueOf(actionSetId));
        if (sharedProjects == null || sharedProjects.isEmpty()) {
            return null;
        }
        Project only = null;
        for (Project project : sharedProjects) {
            if (project == null || project.getProjectId() == privateProjectId) {
                continue;
            }
            if (only != null && only.getProjectId() != project.getProjectId()) {
                return null;
            }
            only = project;
        }
        return only;
    }

    private boolean containsProject(List<Project> projects, int projectId) {
        for (Project project : projects) {
            if (project != null && project.getProjectId() == projectId) {
                return true;
            }
        }
        return false;
    }

    private void addIfValidLinkedProject(Map<Integer, Project> byProjectId, Project project, int patchWorkspaceId) {
        if (project == null || project.getProjectId() <= 0 || project.getWorkspaceId() == null
                || project.getWorkspaceId().intValue() != patchWorkspaceId) {
            return;
        }
        ProjectStatus status = project.getProjectStatusEnum();
        if (status != null && status.isClosed()) {
            return;
        }
        if (!byProjectId.containsKey(project.getProjectId())) {
            byProjectId.put(project.getProjectId(), project);
        }
    }

    private String n(String value) {
        return value == null ? "" : value.trim();
    }
}