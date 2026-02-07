package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ProjectActionChangeLog implements java.io.Serializable {

    private static final long serialVersionUID = 2038137430999366165L;

    private int changeId;
    private int actionId;
    private int projectId;
    private Integer proposalId;
    private Date changeDate;
    private ActorType actorType;
    private String actorId;
    private String sourceType;
    private String changeSummary;
    private String changePatch;
    private String changeReason;
    private ProjectAction action;
    private Project project;
    private ProjectActionProposal proposal;

    public int getChangeId() {
        return changeId;
    }

    public void setChangeId(int changeId) {
        this.changeId = changeId;
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

    public Integer getProposalId() {
        return proposalId;
    }

    public void setProposalId(Integer proposalId) {
        this.proposalId = proposalId;
    }

    public Date getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(Date changeDate) {
        this.changeDate = changeDate;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public void setActorType(ActorType actorType) {
        this.actorType = actorType;
    }

    public String getActorTypeString() {
        if (actorType == null) {
            return "";
        }
        return actorType.getId();
    }

    public void setActorTypeString(String actorTypeString) {
        this.actorType = ActorType.getActorType(actorTypeString);
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public String getChangePatch() {
        return changePatch;
    }

    public void setChangePatch(String changePatch) {
        this.changePatch = changePatch;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public ProjectAction getAction() {
        return action;
    }

    public void setAction(ProjectAction action) {
        this.action = action;
        if (action != null) {
            this.actionId = action.getActionId();
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

    public ProjectActionProposal getProposal() {
        return proposal;
    }

    public void setProposal(ProjectActionProposal proposal) {
        this.proposal = proposal;
        if (proposal != null) {
            this.proposalId = proposal.getProposalId();
        } else {
            this.proposalId = null;
        }
    }
}
