package org.openimmunizationsoftware.pt.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

public class PlanningOutlook implements Serializable {
    private static final long serialVersionUID = 1L;

    private int outlookId;
    private int ownerUserId;
    private String periodType;
    private Date periodStart;
    private String outlookText;
    private Date createdAt;
    private Date updatedAt;

    public int getOutlookId() {
        return outlookId;
    }

    public void setOutlookId(int outlookId) {
        this.outlookId = outlookId;
    }

    public int getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(int ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public Date getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Date periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodStartLocalDate() {
        return periodStart == null ? null : new java.sql.Date(periodStart.getTime()).toLocalDate();
    }

    public void setPeriodStartLocalDate(LocalDate periodStart) {
        this.periodStart = periodStart == null ? null : java.sql.Date.valueOf(periodStart);
    }

    public String getOutlookText() {
        return outlookText;
    }

    public void setOutlookText(String outlookText) {
        this.outlookText = outlookText;
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
