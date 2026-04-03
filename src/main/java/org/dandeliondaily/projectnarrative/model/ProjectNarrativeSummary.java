package org.dandeliondaily.projectnarrative.model;

public class ProjectNarrativeSummary {

    private long projectId;
    private String projectName = "";
    private int completedCount;
    private int minutesSpent;
    private boolean reviewed;
    private ProjectNarrativeEntry narrativeEntry = new ProjectNarrativeEntry();

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getMinutesSpent() {
        return minutesSpent;
    }

    public void setMinutesSpent(int minutesSpent) {
        this.minutesSpent = minutesSpent;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }

    public ProjectNarrativeEntry getNarrativeEntry() {
        return narrativeEntry;
    }

    public void setNarrativeEntry(ProjectNarrativeEntry narrativeEntry) {
        this.narrativeEntry = narrativeEntry;
    }
}
