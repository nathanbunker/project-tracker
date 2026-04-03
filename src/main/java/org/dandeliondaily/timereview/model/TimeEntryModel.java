package org.dandeliondaily.timereview.model;

import java.util.Date;

public class TimeEntryModel {

    private int billId;
    private Date startTime;
    private Date endTime;
    private int durationMinutes;
    private String durationDisplay = "";
    private String projectName = "";
    private int projectId;
    private String actionDescription = "";
    private String categoryName = "";
    private String billCodeLabel = "";
    private String billable = "N";

    public int getBillId() {
        return billId;
    }

    public void setBillId(int billId) {
        this.billId = billId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getDurationDisplay() {
        return durationDisplay;
    }

    public void setDurationDisplay(String durationDisplay) {
        this.durationDisplay = durationDisplay == null ? "" : durationDisplay;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName == null ? "" : projectName;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription == null ? "" : actionDescription;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName == null ? "" : categoryName;
    }

    public String getBillCodeLabel() {
        return billCodeLabel;
    }

    public void setBillCodeLabel(String billCodeLabel) {
        this.billCodeLabel = billCodeLabel == null ? "" : billCodeLabel;
    }

    public String getBillable() {
        return billable;
    }

    public void setBillable(String billable) {
        this.billable = billable == null ? "N" : billable;
    }
}
