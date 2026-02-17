package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateProposalRequest")
public class CreateProposalRequest {

    @Schema(description = "JSON patch of proposed fields")
    private String proposedPatchJson;
    private String summary;
    private String rationale;
    private Integer contactId;
    @Schema(description = "Optional action_next_id to scope proposal to a next action")
    private Integer actionNextId;

    public String getProposedPatchJson() {
        return proposedPatchJson;
    }

    public void setProposedPatchJson(String proposedPatchJson) {
        this.proposedPatchJson = proposedPatchJson;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public Integer getContactId() {
        return contactId;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }

    public Integer getActionNextId() {
        return actionNextId;
    }

    public void setActionNextId(Integer actionNextId) {
        this.actionNextId = actionNextId;
    }
}
