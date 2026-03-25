package org.openimmunizationsoftware.pt.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class StudentOffer implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Integer studentOfferId;
    private ProjectContact contact;
    private StudentOfferTemplate studentOfferTemplate;
    private String title;
    private String description;
    private Integer pricePoints;
    private String imagePath;
    private String status;
    private Integer displayOrder = 0;
    private Date createdDate;
    private Date updatedDate;
    private Date boughtDate;
    private Date fulfillingDate;
    private Date deliveredDate;
    private Set<GamePointLedger> pointLedgerEntries = new HashSet<GamePointLedger>();

    public Integer getStudentOfferId() {
        return studentOfferId;
    }

    public void setStudentOfferId(Integer studentOfferId) {
        this.studentOfferId = studentOfferId;
    }

    public ProjectContact getContact() {
        return contact;
    }

    public void setContact(ProjectContact contact) {
        this.contact = contact;
    }

    public StudentOfferTemplate getStudentOfferTemplate() {
        return studentOfferTemplate;
    }

    public void setStudentOfferTemplate(StudentOfferTemplate studentOfferTemplate) {
        this.studentOfferTemplate = studentOfferTemplate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPricePoints() {
        return pricePoints;
    }

    public void setPricePoints(Integer pricePoints) {
        this.pricePoints = pricePoints;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Date getBoughtDate() {
        return boughtDate;
    }

    public void setBoughtDate(Date boughtDate) {
        this.boughtDate = boughtDate;
    }

    public Date getFulfillingDate() {
        return fulfillingDate;
    }

    public void setFulfillingDate(Date fulfillingDate) {
        this.fulfillingDate = fulfillingDate;
    }

    public Date getDeliveredDate() {
        return deliveredDate;
    }

    public void setDeliveredDate(Date deliveredDate) {
        this.deliveredDate = deliveredDate;
    }

    public Set<GamePointLedger> getPointLedgerEntries() {
        return pointLedgerEntries;
    }

    public void setPointLedgerEntries(Set<GamePointLedger> pointLedgerEntries) {
        this.pointLedgerEntries = pointLedgerEntries;
    }
}
