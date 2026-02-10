package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ProjectActionTaken implements java.io.Serializable {

    private static final long serialVersionUID = -5190528304466367460L;

    private int actionTakenId;
    private int projectId;
    private int contactId;
    private Date actionDate;
    private String actionDescription;
    private ProjectProvider provider;
    private ProjectContact contact;
    private Project project;

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

    public ProjectProvider getProvider() {
        return provider;
    }

    public void setProvider(ProjectProvider provider) {
        this.provider = provider;
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
}
