package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ProjectActionTaken implements java.io.Serializable {

    private static final long serialVersionUID = -5190528304466367460L;

    private int actionTakenId;
    private int projectId;
    private int contactId;
    private Integer workspaceId;
    private Date actionDate;
    private String actionDescription;
    private ProjectContact contact;
    private Project project;
    private ProjectActionSet actionSet;

    public int getActionTakenId() {
        return actionTakenId;
    }

    public void setActionTakenId(int actionTakenId) {
        this.actionTakenId = actionTakenId;
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

    public Integer getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Integer workspaceId) {
        this.workspaceId = workspaceId;
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

    public boolean hasActionDescription() {
        return actionDescription != null && !actionDescription.equals("");
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public ProjectContact getContact() {
        return contact;
    }

    public void setContact(ProjectContact contact) {
        this.contact = contact;
        if (contact != null) {
            this.contactId = contact.getContactId();
        }
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
        if (project != null) {
            this.projectId = project.getProjectId();
        }
    }

    public ProjectActionSet getActionSet() {
        return actionSet;
    }

    public void setActionSet(ProjectActionSet actionSet) {
        this.actionSet = actionSet;
    }
}
