package org.dandeliondaily.projecthealth.model;

public class ProjectListItemModel {

    public enum HealthLevel {
        HEALTHY,
        NEEDS_REVIEW,
        ATTENTION_NEEDED
    }

    private int projectId;
    private String projectName = "";
    private int priorityLevel;
    private boolean selected;
    private HealthLevel healthLevel = HealthLevel.HEALTHY;
    private String healthLabel = "healthy";
    private int undatedOpenCount;
    private int overdueOpenCount;
    private boolean reviewOverdue;

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public HealthLevel getHealthLevel() {
        return healthLevel;
    }

    public void setHealthLevel(HealthLevel healthLevel) {
        this.healthLevel = healthLevel;
    }

    public String getHealthLabel() {
        return healthLabel;
    }

    public void setHealthLabel(String healthLabel) {
        this.healthLabel = healthLabel;
    }

    public int getUndatedOpenCount() {
        return undatedOpenCount;
    }

    public void setUndatedOpenCount(int undatedOpenCount) {
        this.undatedOpenCount = undatedOpenCount;
    }

    public int getOverdueOpenCount() {
        return overdueOpenCount;
    }

    public void setOverdueOpenCount(int overdueOpenCount) {
        this.overdueOpenCount = overdueOpenCount;
    }

    public boolean isReviewOverdue() {
        return reviewOverdue;
    }

    public void setReviewOverdue(boolean reviewOverdue) {
        this.reviewOverdue = reviewOverdue;
    }
}
