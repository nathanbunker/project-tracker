package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.openimmunizationsoftware.pt.model.Project;

@Schema(name = "Project")
public class ProjectDto {

    @Schema(example = "123")
    private int projectId;
    @Schema(example = "Upgrade Tracker")
    private String projectName;
    private String description;
    private String projectStatus;
    private String username;
    private int priorityLevel;

    public static ProjectDto from(Project project) {
        ProjectDto dto = new ProjectDto();
        dto.setProjectId(project.getProjectId());
        dto.setProjectName(project.getProjectName());
        dto.setDescription(project.getDescription());
        dto.setProjectStatus(project.getProjectStatus());
        dto.setUsername(project.getUsername());
        dto.setPriorityLevel(project.getPriorityLevel());
        return dto;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus) {
        this.projectStatus = projectStatus;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }
}
