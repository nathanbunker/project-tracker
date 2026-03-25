package org.openimmunizationsoftware.pt.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class StudentOfferTemplate implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Integer studentOfferTemplateId;
    private ProjectContact contact;
    private String title;
    private String description;
    private Integer defaultPricePoints;
    private String imagePath;
    private String status;
    private Integer displayOrder = 0;
    private Date createdDate;
    private Date updatedDate;
    private Set<StudentOffer> studentOffers = new HashSet<StudentOffer>();

    public Integer getStudentOfferTemplateId() {
        return studentOfferTemplateId;
    }

    public void setStudentOfferTemplateId(Integer studentOfferTemplateId) {
        this.studentOfferTemplateId = studentOfferTemplateId;
    }

    public ProjectContact getContact() {
        return contact;
    }

    public void setContact(ProjectContact contact) {
        this.contact = contact;
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

    public Integer getDefaultPricePoints() {
        return defaultPricePoints;
    }

    public void setDefaultPricePoints(Integer defaultPricePoints) {
        this.defaultPricePoints = defaultPricePoints;
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

    public Set<StudentOffer> getStudentOffers() {
        return studentOffers;
    }

    public void setStudentOffers(Set<StudentOffer> studentOffers) {
        this.studentOffers = studentOffers;
    }
}
