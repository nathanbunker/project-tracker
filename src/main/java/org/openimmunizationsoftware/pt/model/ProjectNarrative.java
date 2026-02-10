package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ProjectNarrative implements java.io.Serializable {

    private static final long serialVersionUID = 3918484321671882606L;

    private int narrativeId;
    private int projectId;
    private int contactId;
    private String providerId;
    private Date narrativeDate;
    private ProjectNarrativeVerb narrativeVerb;
    private String narrativeText;
    private Project project;
    private ProjectContact contact;
    private ProjectProvider provider;

    public int getNarrativeId() {
        return narrativeId;
    }

    public void setNarrativeId(int narrativeId) {
        this.narrativeId = narrativeId;
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

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Date getNarrativeDate() {
        return narrativeDate;
    }

    public void setNarrativeDate(Date narrativeDate) {
        this.narrativeDate = narrativeDate;
    }

    public ProjectNarrativeVerb getNarrativeVerb() {
        return narrativeVerb;
    }

    public void setNarrativeVerb(ProjectNarrativeVerb narrativeVerb) {
        this.narrativeVerb = narrativeVerb;
    }

    public String getNarrativeVerbString() {
        return narrativeVerb == null ? null : narrativeVerb.getId();
    }

    public void setNarrativeVerbString(String narrativeVerbString) {
        this.narrativeVerb = ProjectNarrativeVerb.fromId(narrativeVerbString);
    }

    public String getNarrativeText() {
        return narrativeText;
    }

    public void setNarrativeText(String narrativeText) {
        this.narrativeText = narrativeText;
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

    public ProjectContact getContact() {
        return contact;
    }

    public void setContact(ProjectContact contact) {
        this.contact = contact;
        if (contact != null) {
            this.contactId = contact.getContactId();
        }
    }

    public ProjectProvider getProvider() {
        return provider;
    }

    public void setProvider(ProjectProvider provider) {
        this.provider = provider;
        if (provider != null) {
            this.providerId = provider.getProviderId();
        }
    }
}
