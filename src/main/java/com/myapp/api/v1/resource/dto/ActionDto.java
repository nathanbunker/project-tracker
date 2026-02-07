package com.myapp.api.v1.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import org.openimmunizationsoftware.pt.model.ProjectAction;

@Schema(name = "ProjectAction")
public class ActionDto {

    @Schema(example = "456")
    private int actionId;
    @Schema(example = "123")
    private int projectId;
    private int contactId;
    private Date actionDate;
    private String actionDescription;
    private String nextDescription;
    private Date nextDue;
    private Date nextDeadline;
    private String nextActionType;
    private int priorityLevel;

    public static ActionDto from(ProjectAction action) {
        ActionDto dto = new ActionDto();
        dto.setActionId(action.getActionId());
        dto.setProjectId(action.getProjectId());
        dto.setContactId(action.getContactId());
        dto.setActionDate(action.getActionDate());
        dto.setActionDescription(action.getActionDescription());
        dto.setNextDescription(action.getNextDescription());
        dto.setNextDue(action.getNextDue());
        dto.setNextDeadline(action.getNextDeadline());
        dto.setNextActionType(action.getNextActionType());
        dto.setPriorityLevel(action.getPriorityLevel());
        return dto;
    }

    public int getActionId() {
        return actionId;
    }

    public void setActionId(int actionId) {
        this.actionId = actionId;
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

    public Date getActionDate() {
        return actionDate;
    }

    public void setActionDate(Date actionDate) {
        this.actionDate = actionDate;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public String getNextDescription() {
        return nextDescription;
    }

    public void setNextDescription(String nextDescription) {
        this.nextDescription = nextDescription;
    }

    public Date getNextDue() {
        return nextDue;
    }

    public void setNextDue(Date nextDue) {
        this.nextDue = nextDue;
    }

    public Date getNextDeadline() {
        return nextDeadline;
    }

    public void setNextDeadline(Date nextDeadline) {
        this.nextDeadline = nextDeadline;
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
