package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ProjectActionProposal implements java.io.Serializable {

    private static final long serialVersionUID = -2523512022399490700L;

    private int proposalId;
    private int projectId;
    private Integer actionId;
    private Integer contactId;
    private ProposalStatus proposalStatus;
    private Date proposalCreateDate;
    private Date proposalDecideDate;
    private String modelName;
    private String requestId;
    private String proposedSummary;
    private String proposedRationale;
    private String proposedPatch;
    private String inputSnapshot;
    private Project project;
    private ProjectAction action;
    private ProjectContact contact;

    public int getProposalId() {
        return proposalId;
    }

    public void setProposalId(int proposalId) {
        this.proposalId = proposalId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public Integer getActionId() {
        return actionId;
    }

    public void setActionId(Integer actionId) {
        this.actionId = actionId;
    }

    public Integer getContactId() {
        return contactId;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }

    public ProposalStatus getProposalStatus() {
        return proposalStatus;
    }

    public void setProposalStatus(ProposalStatus proposalStatus) {
        this.proposalStatus = proposalStatus;
    }

    public String getProposalStatusString() {
        if (proposalStatus == null) {
            return "";
        }
        return proposalStatus.getId();
    }

    public void setProposalStatusString(String proposalStatusString) {
        this.proposalStatus = ProposalStatus.getProposalStatus(proposalStatusString);
    }

    public Date getProposalCreateDate() {
        return proposalCreateDate;
    }

    public void setProposalCreateDate(Date proposalCreateDate) {
        this.proposalCreateDate = proposalCreateDate;
    }

    public Date getProposalDecideDate() {
        return proposalDecideDate;
    }

    public void setProposalDecideDate(Date proposalDecideDate) {
        this.proposalDecideDate = proposalDecideDate;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getProposedSummary() {
        return proposedSummary;
    }

    public void setProposedSummary(String proposedSummary) {
        this.proposedSummary = proposedSummary;
    }

    public String getProposedRationale() {
        return proposedRationale;
    }

    public void setProposedRationale(String proposedRationale) {
        this.proposedRationale = proposedRationale;
    }

    public String getProposedPatch() {
        return proposedPatch;
    }

    public void setProposedPatch(String proposedPatch) {
        this.proposedPatch = proposedPatch;
    }

    public String getInputSnapshot() {
        return inputSnapshot;
    }

    public void setInputSnapshot(String inputSnapshot) {
        this.inputSnapshot = inputSnapshot;
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

    public ProjectAction getAction() {
        return action;
    }

    public void setAction(ProjectAction action) {
        this.action = action;
        if (action != null) {
            this.actionId = action.getActionId();
        } else {
            this.actionId = null;
        }
    }

    public ProjectContact getContact() {
        return contact;
    }

    public void setContact(ProjectContact contact) {
        this.contact = contact;
        if (contact != null) {
            this.contactId = contact.getContactId();
        } else {
            this.contactId = null;
        }
    }
}
