package org.dandeliondaily.projecthealth.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.dandeliondaily.projecthealth.model.ProjectPatchLinkDisplayModel;
import org.openimmunizationsoftware.pt.doa.ProjectPatchLinkDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.ProjectPatchLink;

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
            } else if (ProjectPatchLink.LINK_TYPE_PATCH_CATEGORY.equals(link.getLinkType())
                    && link.getLinkedPatchCategoryId() != null) {
                ProjectCategory category = (ProjectCategory) session.get(ProjectCategory.class,
                        link.getLinkedPatchCategoryId());
                if (category != null) {
                    display.setCategoryName(category.getClientName());
                    Query projectQuery = session.createQuery(
                            "from Project where workspaceId = :wsId and categoryCode = :code"
                                    + " and (phaseCode is null or phaseCode <> 'Clos')"
                                    + " order by priorityLevel desc, projectName");
                    projectQuery.setParameter("wsId", patchWorkspaceId);
                    projectQuery.setParameter("code", category.getCategoryCode());
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

    public String validateCategoryLink(Session session, int patchCategoryId, int patchWorkspaceId) {
        ProjectCategory category = (ProjectCategory) session.get(ProjectCategory.class, patchCategoryId);
        if (category == null) {
            return "Category not found";
        }
        if (category.getWorkspaceId() == null
                || category.getWorkspaceId().intValue() != patchWorkspaceId) {
            return "Category does not belong to the linked patch workspace";
        }
        return null;
    }
}
