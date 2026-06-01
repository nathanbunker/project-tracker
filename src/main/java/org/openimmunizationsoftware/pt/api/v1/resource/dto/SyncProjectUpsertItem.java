package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import java.util.List;

public class SyncProjectUpsertItem {

    private String externalProjectId;
    private String projectName;
    private String description;
    private String projectHandle;
    private String projectStatus;
    private List<String> projectTags;

    private boolean hasProjectName;
    private boolean hasDescription;
    private boolean hasProjectHandle;
    private boolean hasProjectStatus;
    private boolean hasProjectTags;

    public String getExternalProjectId() {
        return externalProjectId;
    }

    public void setExternalProjectId(String externalProjectId) {
        this.externalProjectId = externalProjectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
        this.hasProjectName = true;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.hasDescription = true;
    }

    public String getProjectHandle() {
        return projectHandle;
    }

    public void setProjectHandle(String projectHandle) {
        this.projectHandle = projectHandle;
        this.hasProjectHandle = true;
    }

    public String getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus) {
        this.projectStatus = projectStatus;
        this.hasProjectStatus = true;
    }

    public boolean isHasProjectName() {
        return hasProjectName;
    }

    public boolean isHasDescription() {
        return hasDescription;
    }

    public boolean isHasProjectHandle() {
        return hasProjectHandle;
    }

    public boolean isHasProjectStatus() {
        return hasProjectStatus;
    }

    public List<String> getProjectTags() {
        return projectTags;
    }

    public void setProjectTags(List<String> projectTags) {
        this.projectTags = projectTags;
        this.hasProjectTags = true;
    }

    public boolean isHasProjectTags() {
        return hasProjectTags;
    }
}
