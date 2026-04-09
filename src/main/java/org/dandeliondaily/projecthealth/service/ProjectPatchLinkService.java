package org.dandeliondaily.projecthealth.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.dandeliondaily.projecthealth.model.ProjectPatchLinkDisplayModel;
import org.openimmunizationsoftware.pt.doa.ProjectPatchLinkDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.ProjectPatchLink;
import org.openimmunizationsoftware.pt.model.ProjectTag;

public class ProjectPatchLinkService {

    @SuppressWarnings("unchecked")
    public List<ProjectPatchLinkDisplayModel> buildLinkDisplayModels(Session session, int privateProjectId,
            int patchWorkspaceId) {
        ProjectPatchLinkDao dao = new ProjectPatchLinkDao(session);
        List<ProjectPatchLink> links = dao.listLinksForProject(privateProjectId);
        List<ProjectPatchLinkDisplayModel> result = new ArrayList<ProjectPatchLinkDisplayModel>();
        for (ProjectPatchLink link : links) {
            ProjectPatchLinkDisplayModel display = new ProjectPatchLinkDisplayModel();
            display.setProjectPatchLinkId(link.getProjectPatchLinkId());
            display.setLinkType(link.getLinkType());
            if (ProjectPatchLink.LINK_TYPE_DIRECT_PROJECT.equals(link.getLinkType())
                    && link.getLinkedPatchProjectId() != null) {
                Project patchProject = (Project) session.get(Project.class, link.getLinkedPatchProjectId());
                display.setDirectLinkedProject(patchProject);
            } else if (ProjectPatchLink.LINK_TYPE_PATCH_TAG.equals(link.getLinkType())
                    && link.getLinkedPatchTagId() != null) {
                ProjectTag tag = (ProjectTag) session.get(ProjectTag.class,
                        link.getLinkedPatchTagId());
                if (tag != null) {
                    display.setTagName(tag.getTagName());
                    Query projectQuery = session.createQuery(
                            "from Project p where p.workspaceId = :wsId"
                                    + " and p.projectStatus <> :closedStatus"
                                    + " and exists (select 1 from ProjectTagMap ptm where ptm.projectId = p.projectId and ptm.projectTagId = :tagId)"
                                    + " order by p.priorityLevel desc, p.projectName");
                    projectQuery.setParameter("wsId", patchWorkspaceId);
                    projectQuery.setParameter("closedStatus", ProjectStatus.CLOSED.getDatabaseValue());
                    projectQuery.setParameter("tagId", tag.getProjectTagId());
                    display.setResolvedProjects((List<Project>) projectQuery.list());
                }
            }
            result.add(display);
        }
        return result;
    }

    public String validateDirectLink(Session session, int patchProjectId, int patchWorkspaceId) {
        Project patchProject = (Project) session.get(Project.class, patchProjectId);
        if (patchProject == null) {
            return "Patch project not found";
        }
        if (patchProject.getWorkspaceId() == null
                || patchProject.getWorkspaceId().intValue() != patchWorkspaceId) {
            return "Patch project does not belong to the linked patch workspace";
        }
        return null;
    }

    public String validateTagLink(Session session, int patchTagId, int patchWorkspaceId) {
        ProjectTag tag = (ProjectTag) session.get(ProjectTag.class, patchTagId);
        if (tag == null) {
            return "Tag not found";
        }
        if (tag.getWorkspaceId() != patchWorkspaceId) {
            return "Tag does not belong to the linked patch workspace";
        }
        return null;
    }
}
