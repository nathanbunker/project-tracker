package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import org.openimmunizationsoftware.pt.model.ProjectActionChangeLog;

@Schema(name = "ActionChangeLog")
public class ActionChangeLogDto {

    @Schema(example = "321")
    private int changeId;
    @Schema(example = "456")
    private int actionId;
    @Schema(example = "123")
    private int projectId;
    private Integer proposalId;
    private Date changeDate;
    private String actorType;
    private String actorId;
    private String sourceType;
    private String changeSummary;
    private String changePatch;
    private String changeReason;

    public static ActionChangeLogDto from(ProjectActionChangeLog log) {
        ActionChangeLogDto dto = new ActionChangeLogDto();
        dto.setChangeId(log.getChangeId());
        dto.setActionId(log.getActionId());
        dto.setProjectId(log.getProjectId());
        dto.setProposalId(log.getProposalId());
        dto.setChangeDate(log.getChangeDate());
        dto.setActorType(log.getActorTypeString());
        dto.setActorId(log.getActorId());
        dto.setSourceType(log.getSourceType());
        dto.setChangeSummary(log.getChangeSummary());
        dto.setChangePatch(log.getChangePatch());
        dto.setChangeReason(log.getChangeReason());
        return dto;
    }

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

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
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
}
