package org.dandeliondaily.projecthealth.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectReportModel {

    public static class ReportActionLine {
        private int actionId;
        private String description = "";
        private String whenLabel = "";

        public int getActionId() {
            return actionId;
        }

        public void setActionId(int actionId) {
            this.actionId = actionId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getWhenLabel() {
            return whenLabel;
        }

        public void setWhenLabel(String whenLabel) {
            this.whenLabel = whenLabel;
        }
    }

    private int projectId;
    private String projectName = "";
    private String description = "";
    private String category = "";
    private String phase = "";
    private String reportText = "";
    private int undatedOpenCount;
    private int overdueOpenCount;
    private int updateDueDays;
    private String lastReviewLabel = "";

    private List<ReportActionLine> recentCompleted = new ArrayList<ReportActionLine>();
    private List<ReportActionLine> scheduledOpen = new ArrayList<ReportActionLine>();
    private List<ReportActionLine> unscheduledOpen = new ArrayList<ReportActionLine>();
    private List<String> nextRecommendations = new ArrayList<String>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getReportText() {
        return reportText;
    }

    public void setReportText(String reportText) {
        this.reportText = reportText;
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

    public int getUpdateDueDays() {
        return updateDueDays;
    }

    public void setUpdateDueDays(int updateDueDays) {
        this.updateDueDays = updateDueDays;
    }

    public String getLastReviewLabel() {
        return lastReviewLabel;
    }

    public void setLastReviewLabel(String lastReviewLabel) {
        this.lastReviewLabel = lastReviewLabel;
    }

    public List<ReportActionLine> getRecentCompleted() {
        return recentCompleted;
    }

    public void setRecentCompleted(List<ReportActionLine> recentCompleted) {
        this.recentCompleted = recentCompleted;
    }

    public List<ReportActionLine> getScheduledOpen() {
        return scheduledOpen;
    }

    public void setScheduledOpen(List<ReportActionLine> scheduledOpen) {
        this.scheduledOpen = scheduledOpen;
    }

    public List<ReportActionLine> getUnscheduledOpen() {
        return unscheduledOpen;
    }

    public void setUnscheduledOpen(List<ReportActionLine> unscheduledOpen) {
        this.unscheduledOpen = unscheduledOpen;
    }

    public List<String> getNextRecommendations() {
        return nextRecommendations;
    }

    public void setNextRecommendations(List<String> nextRecommendations) {
        this.nextRecommendations = nextRecommendations;
    }
}
