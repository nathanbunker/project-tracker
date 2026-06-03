package org.openimmunizationsoftware.pt.model;

import java.math.BigDecimal;
import java.util.Date;

public class ProjectFactValue implements java.io.Serializable {

    private static final long serialVersionUID = -2907255465516389334L;

    public static final String BOOLEAN_YES = "Y";
    public static final String BOOLEAN_NO = "N";

    private int projectFactValueId;
    private int projectId;
    private int projectFactDefinitionId;
    private String valueBoolean;
    private String valueText;
    private Date valueDate;
    private BigDecimal valueNumber;
    private String valueCode;
    private String notes;
    private Integer createdByWebUserId;
    private Date createdDate;
    private Integer lastModifiedByWebUserId;
    private Date lastModifiedDate;

    public int getProjectFactValueId() {
        return projectFactValueId;
    }

    public void setProjectFactValueId(int projectFactValueId) {
        this.projectFactValueId = projectFactValueId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getProjectFactDefinitionId() {
        return projectFactDefinitionId;
    }

    public void setProjectFactDefinitionId(int projectFactDefinitionId) {
        this.projectFactDefinitionId = projectFactDefinitionId;
    }

    public String getValueBoolean() {
        return valueBoolean;
    }

    public void setValueBoolean(String valueBoolean) {
        this.valueBoolean = valueBoolean;
    }

    public Date getValueDate() {
        return valueDate;
    }

    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }

    public String getValueText() {
        return valueText;
    }

    public void setValueText(String valueText) {
        this.valueText = valueText;
    }

    public BigDecimal getValueNumber() {
        return valueNumber;
    }

    public void setValueNumber(BigDecimal valueNumber) {
        this.valueNumber = valueNumber;
    }

    public String getValueCode() {
        return valueCode;
    }

    public void setValueCode(String valueCode) {
        this.valueCode = valueCode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getCreatedByWebUserId() {
        return createdByWebUserId;
    }

    public void setCreatedByWebUserId(Integer createdByWebUserId) {
        this.createdByWebUserId = createdByWebUserId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Integer getLastModifiedByWebUserId() {
        return lastModifiedByWebUserId;
    }

    public void setLastModifiedByWebUserId(Integer lastModifiedByWebUserId) {
        this.lastModifiedByWebUserId = lastModifiedByWebUserId;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
