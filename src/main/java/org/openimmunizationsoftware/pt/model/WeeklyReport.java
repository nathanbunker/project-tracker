package org.openimmunizationsoftware.pt.model;

import java.io.Serializable;
import java.util.Date;

public class WeeklyReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private int weeklyReportId;
    private String reportName;
    private int ownerUserId;
    private int rootWorkspaceId;
    private String rootBillCode;
    private String active = "Y";
    private String accessKeyHash;
    private Date accessKeyCreatedAt;
    private Date createdAt;
    private Date updatedAt;

    public int getWeeklyReportId() {
        return weeklyReportId;
    }

    public void setWeeklyReportId(int weeklyReportId) {
        this.weeklyReportId = weeklyReportId;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public int getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(int ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public int getRootWorkspaceId() {
        return rootWorkspaceId;
    }

    public void setRootWorkspaceId(int rootWorkspaceId) {
        this.rootWorkspaceId = rootWorkspaceId;
    }

    public String getRootBillCode() {
        return rootBillCode;
    }

    public void setRootBillCode(String rootBillCode) {
        this.rootBillCode = rootBillCode;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getAccessKeyHash() {
        return accessKeyHash;
    }

    public void setAccessKeyHash(String accessKeyHash) {
        this.accessKeyHash = accessKeyHash;
    }

    public Date getAccessKeyCreatedAt() {
        return accessKeyCreatedAt;
    }

    public void setAccessKeyCreatedAt(Date accessKeyCreatedAt) {
        this.accessKeyCreatedAt = accessKeyCreatedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}