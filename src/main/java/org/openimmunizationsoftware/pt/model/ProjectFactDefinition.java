package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ProjectFactDefinition implements java.io.Serializable {

    private static final long serialVersionUID = 3187413986476368238L;

    public static final String ACTIVE_YES = "Y";
    public static final String ACTIVE_NO = "N";

    public static final String INPUT_TYPE_BOOLEAN = "BOOLEAN";
    public static final String INPUT_TYPE_SELECT = "SELECT";
    public static final String INPUT_TYPE_TEXT = "TEXT";
    public static final String INPUT_TYPE_DATE = "DATE";
    public static final String INPUT_TYPE_NUMBER = "NUMBER";

    private int projectFactDefinitionId;
    private int workspaceId;
    private String factGroup;
    private String factCode;
    private String factLabel;
    private String factDescription;
    private String factInputType = INPUT_TYPE_BOOLEAN;
    private int displayOrder;
    private String active = ACTIVE_YES;
    private Integer createdByWebUserId;
    private Date createdDate;
    private Integer lastModifiedByWebUserId;
    private Date lastModifiedDate;

    public int getProjectFactDefinitionId() {
        return projectFactDefinitionId;
    }

    public void setProjectFactDefinitionId(int projectFactDefinitionId) {
        this.projectFactDefinitionId = projectFactDefinitionId;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getFactGroup() {
        return factGroup;
    }

    public void setFactGroup(String factGroup) {
        this.factGroup = factGroup;
    }

    public String getFactCode() {
        return factCode;
    }

    public void setFactCode(String factCode) {
        this.factCode = factCode;
    }

    public String getFactLabel() {
        return factLabel;
    }

    public void setFactLabel(String factLabel) {
        this.factLabel = factLabel;
    }

    public String getFactDescription() {
        return factDescription;
    }

    public void setFactDescription(String factDescription) {
        this.factDescription = factDescription;
    }

    public String getFactInputType() {
        return factInputType;
    }

    public void setFactInputType(String factInputType) {
        this.factInputType = factInputType;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
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
