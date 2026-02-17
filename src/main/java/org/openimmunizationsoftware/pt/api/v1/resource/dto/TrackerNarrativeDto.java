package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;

@Schema(name = "TrackerNarrative")
public class TrackerNarrativeDto {

    @Schema(example = "101")
    private int narrativeId;
    @Schema(example = "12")
    private int projectId;
    @Schema(example = "66747")
    private int contactId;
    private String displayTitle;
    @Schema(example = "DAILY")
    private String narrativeType;
    private Date periodStart;
    private Date periodEnd;
    @Schema(example = "GENERATED")
    private String reviewStatus;
    private String markdownGenerated;
    private String markdownFinal;
    private Date dateGenerated;
    private Date dateApproved;
    private String promptVersion;
    private String modelName;
    private Date lastUpdated;

    public static TrackerNarrativeDto from(TrackerNarrative narrative) {
        TrackerNarrativeDto dto = new TrackerNarrativeDto();
        dto.setNarrativeId(narrative.getNarrativeId());
        dto.setProjectId(narrative.getProjectId());
        dto.setContactId(narrative.getContactId());
        dto.setDisplayTitle(narrative.getDisplayTitle());
        dto.setNarrativeType(narrative.getNarrativeType());
        dto.setPeriodStart(narrative.getPeriodStart());
        dto.setPeriodEnd(narrative.getPeriodEnd());
        dto.setReviewStatus(narrative.getReviewStatusString());
        dto.setMarkdownGenerated(narrative.getMarkdownGenerated());
        dto.setMarkdownFinal(narrative.getMarkdownFinal());
        dto.setDateGenerated(narrative.getDateGenerated());
        dto.setDateApproved(narrative.getDateApproved());
        dto.setPromptVersion(narrative.getPromptVersion());
        dto.setModelName(narrative.getModelName());
        dto.setLastUpdated(narrative.getLastUpdated());
        return dto;
    }

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

    public String getDisplayTitle() {
        return displayTitle;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    public String getNarrativeType() {
        return narrativeType;
    }

    public void setNarrativeType(String narrativeType) {
        this.narrativeType = narrativeType;
    }

    public Date getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Date periodStart) {
        this.periodStart = periodStart;
    }

    public Date getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Date periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getMarkdownGenerated() {
        return markdownGenerated;
    }

    public void setMarkdownGenerated(String markdownGenerated) {
        this.markdownGenerated = markdownGenerated;
    }

    public String getMarkdownFinal() {
        return markdownFinal;
    }

    public void setMarkdownFinal(String markdownFinal) {
        this.markdownFinal = markdownFinal;
    }

    public Date getDateGenerated() {
        return dateGenerated;
    }

    public void setDateGenerated(Date dateGenerated) {
        this.dateGenerated = dateGenerated;
    }

    public Date getDateApproved() {
        return dateApproved;
    }

    public void setDateApproved(Date dateApproved) {
        this.dateApproved = dateApproved;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
