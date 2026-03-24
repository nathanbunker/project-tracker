package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class GamePointLedger implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Integer gamePointLedgerId;
    private ProjectContact contact;
    private ProjectActionNext projectActionNext;
    private Integer pointChange;
    private String entryType;
    private String entryNote;
    private Date createdDate;
    private String createdBy;

    public Integer getGamePointLedgerId() {
        return gamePointLedgerId;
    }

    public void setGamePointLedgerId(Integer gamePointLedgerId) {
        this.gamePointLedgerId = gamePointLedgerId;
    }

    public ProjectContact getContact() {
        return contact;
    }

    public void setContact(ProjectContact contact) {
        this.contact = contact;
    }

    public ProjectActionNext getProjectActionNext() {
        return projectActionNext;
    }

    public void setProjectActionNext(ProjectActionNext projectActionNext) {
        this.projectActionNext = projectActionNext;
    }

    public Integer getPointChange() {
        return pointChange;
    }

    public void setPointChange(Integer pointChange) {
        this.pointChange = pointChange;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getEntryNote() {
        return entryNote;
    }

    public void setEntryNote(String entryNote) {
        this.entryNote = entryNote;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
