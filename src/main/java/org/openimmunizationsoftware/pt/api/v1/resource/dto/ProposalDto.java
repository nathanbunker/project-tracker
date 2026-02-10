package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import org.openimmunizationsoftware.pt.model.ProjectActionProposal;

@Schema(name = "ProjectActionProposal")
public class ProposalDto {

    @Schema(example = "789")
    private int proposalId;
    @Schema(example = "123")
    private int projectId;
    private Integer actionId;
    private Integer contactId;
    private String proposalStatus;
    private Date proposalCreateDate;
    private Date proposalDecideDate;
    private String modelName;
    private String requestId;
    private String proposedSummary;
    private String proposedRationale;
    private String proposedPatch;

    public static ProposalDto from(ProjectActionProposal proposal) {
        ProposalDto dto = new ProposalDto();
        dto.setProposalId(proposal.getProposalId());
        dto.setProjectId(proposal.getProjectId());
        dto.setActionId(proposal.getActionId());
        dto.setContactId(proposal.getContactId());
        dto.setProposalStatus(proposal.getProposalStatusString());
        dto.setProposalCreateDate(proposal.getProposalCreateDate());
        dto.setProposalDecideDate(proposal.getProposalDecideDate());
        dto.setModelName(proposal.getModelName());
        dto.setRequestId(proposal.getRequestId());
        dto.setProposedSummary(proposal.getProposedSummary());
        dto.setProposedRationale(proposal.getProposedRationale());
        dto.setProposedPatch(proposal.getProposedPatch());
        return dto;
    }

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

    public String getProposalStatus() {
        return proposalStatus;
    }

    public void setProposalStatus(String proposalStatus) {
        this.proposalStatus = proposalStatus;
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
}
