package org.dandeliondaily.projecthealth.model;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.pt.model.Project;

public class ProjectPatchLinkDisplayModel {

    private int projectPatchLinkId;
    private String linkType;

    // For DIRECT_PROJECT links
    private Project directLinkedProject;

    // For PATCH_CATEGORY links
    private String categoryName;
    private List<Project> resolvedProjects = new ArrayList<Project>();

    public int getProjectPatchLinkId() {
        return projectPatchLinkId;
    }

    public void setProjectPatchLinkId(int projectPatchLinkId) {
        this.projectPatchLinkId = projectPatchLinkId;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public Project getDirectLinkedProject() {
        return directLinkedProject;
    }

    public void setDirectLinkedProject(Project directLinkedProject) {
        this.directLinkedProject = directLinkedProject;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<Project> getResolvedProjects() {
        return resolvedProjects;
    }

    public void setResolvedProjects(List<Project> resolvedProjects) {
        this.resolvedProjects = resolvedProjects;
    }
}
