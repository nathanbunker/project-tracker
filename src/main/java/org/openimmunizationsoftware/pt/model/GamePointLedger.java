package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class GamePointLedger implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ACCOUNT_SPENDABLE = "SPENDABLE";
    public static final String ACCOUNT_SAVINGS = "SAVINGS";

    private Integer gamePointLedgerId;
    private ProjectContact contact;
    private ActionNext projectActionNext;
    private StudentOffer studentOffer;
    private Integer pointChange;
    private String accountBucket;
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

    public ActionNext getActionNext() {
        return projectActionNext;
    }

    public void setActionNext(ActionNext projectActionNext) {
        this.projectActionNext = projectActionNext;
    }

    public StudentOffer getStudentOffer() {
        return studentOffer;
    }

    public void setStudentOffer(StudentOffer studentOffer) {
        this.studentOffer = studentOffer;
    }

    public Integer getPointChange() {
        return pointChange;
    }

    public void setPointChange(Integer pointChange) {
        this.pointChange = pointChange;
    }

    public String getAccountBucket() {
        return accountBucket;
    }

    public void setAccountBucket(String accountBucket) {
        this.accountBucket = accountBucket;
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
