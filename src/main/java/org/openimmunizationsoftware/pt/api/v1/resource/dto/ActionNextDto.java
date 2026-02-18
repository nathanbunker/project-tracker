package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;

@Schema(name = "ProjectActionNext")
public class ActionNextDto {

    @Schema(example = "456")
    private int actionNextId;
    @Schema(example = "123")
    private int projectId;
    private int contactId;
    private String nextDescription;
    private Date nextActionDate;
    private Date nextDeadlineDate;
    private String nextActionType;
    private int priorityLevel;

    public static ActionNextDto from(ProjectActionNext action) {
        ActionNextDto dto = new ActionNextDto();
        dto.setActionNextId(action.getActionNextId());
        dto.setProjectId(action.getProjectId());
        dto.setContactId(action.getContactId());
        dto.setNextDescription(action.getNextDescription());
        dto.setNextActionDate(action.getNextActionDate());
        dto.setNextDeadlineDate(action.getNextDeadlineDate());
        dto.setNextActionType(action.getNextActionType());
        dto.setPriorityLevel(action.getPriorityLevel());
        return dto;
    }

    public int getActionNextId() {
        return actionNextId;
    }

    public void setActionNextId(int actionNextId) {
        this.actionNextId = actionNextId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getContactId() {
        return contactId;
    }

    public void setContactId(int contactId) {
        this.contactId = contactId;
    }

    public String getNextDescription() {
        return nextDescription;
    }

    public void setNextDescription(String nextDescription) {
        this.nextDescription = nextDescription;
    }

    public Date getNextActionDate() {
        return nextActionDate;
    }

    public void setNextActionDate(Date nextActionDate) {
        this.nextActionDate = nextActionDate;
    }

    public Date getNextDeadlineDate() {
        return nextDeadlineDate;
    }

    public void setNextDeadlineDate(Date nextDeadlineDate) {
        this.nextDeadlineDate = nextDeadlineDate;
    }

    public String getNextActionType() {
        return nextActionType;
    }

    public void setNextActionType(String nextActionType) {
        this.nextActionType = nextActionType;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }
}

