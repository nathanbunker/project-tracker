package org.openimmunizationsoftware.pt.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

public class WorkObligation implements Serializable {
    private static final long serialVersionUID = 1L;

    private int workObligationId;
    private int ownerUserId;
    private Date weekStart;
    private int obligatedMinutes;
    private String note;
    private Date createdAt;
    private Date updatedAt;

    public int getWorkObligationId() {
        return workObligationId;
    }

    public void setWorkObligationId(int workObligationId) {
        this.workObligationId = workObligationId;
    }

    public int getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(int ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Date getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(Date weekStart) {
        this.weekStart = weekStart;
    }

    public LocalDate getWeekStartLocalDate() {
        return weekStart == null ? null : new java.sql.Date(weekStart.getTime()).toLocalDate();
    }

    public void setWeekStartLocalDate(LocalDate weekStart) {
        this.weekStart = weekStart == null ? null : java.sql.Date.valueOf(weekStart);
    }

    public int getObligatedMinutes() {
        return obligatedMinutes;
    }

    public void setObligatedMinutes(int obligatedMinutes) {
        this.obligatedMinutes = obligatedMinutes;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
