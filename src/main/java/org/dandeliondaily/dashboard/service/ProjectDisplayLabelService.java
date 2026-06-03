package org.dandeliondaily.dashboard.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.doa.ProjectPatchLinkDao;
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