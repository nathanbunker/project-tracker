package org.dandeliondaily.timereview.model;

import java.util.Date;

public class AllocationProjectDrillDown {

    private String billCode;
    private int projectId;
    private String projectLabel;
    private int totalMinutes;
    private Date firstEntryDate;
    private Date lastEntryDate;

    public String getBillCode() {
        return billCode;
    }

    public void setBillCode(String billCode) {
        this.billCode = billCode;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getProjectLabel() {
        return projectLabel;
    }

    public void setProjectLabel(String projectLabel) {
        this.projectLabel = projectLabel;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public Date getFirstEntryDate() {
        return firstEntryDate;
    }

    public void setFirstEntryDate(Date firstEntryDate) {
        this.firstEntryDate = firstEntryDate;
    }

    public Date getLastEntryDate() {
        return lastEntryDate;
    }

    public void setLastEntryDate(Date lastEntryDate) {
        this.lastEntryDate = lastEntryDate;
    }
}