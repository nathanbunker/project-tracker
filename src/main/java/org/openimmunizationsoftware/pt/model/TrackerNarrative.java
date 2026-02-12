package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class TrackerNarrative implements java.io.Serializable {

    private static final long serialVersionUID = 6256647617619498403L;

    private int narrativeId;
    private String displayTitle;
    private String narrativeType;
    private Date periodStart;
    private Date periodEnd;
    private TrackerNarrativeReviewStatus reviewStatus;
    private String markdownGenerated;
    private String markdownFinal;
    private Date dateGenerated;
    private Date dateApproved;
    private String promptVersion;
    private String modelName;

    public int getNarrativeId() {
        return narrativeId;
    }

    public void setNarrativeId(int narrativeId) {
        this.narrativeId = narrativeId;
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

    public TrackerNarrativeReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(TrackerNarrativeReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewStatusString() {
        return reviewStatus == null ? null : reviewStatus.getId();
    }

    public void setReviewStatusString(String reviewStatusString) {
        this.reviewStatus = TrackerNarrativeReviewStatus.fromId(reviewStatusString);
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
}
