package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ProjectTag implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    private int projectTagId;
    private int workspaceId;
    private String tagName;
    private String tagHandle;
    private String tagStatus = STATUS_ACTIVE;
    private Integer sortOrder;
    private Integer createdByWebUserId;
    private Date createdDate;

    public int getProjectTagId() {
        return projectTagId;
    }

    public void setProjectTagId(int projectTagId) {
        this.projectTagId = projectTagId;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagHandle() {
        return tagHandle;
    }

    public void setTagHandle(String tagHandle) {
        this.tagHandle = tagHandle;
    }

    public String getTagStatus() {
        return tagStatus;
    }

    public void setTagStatus(String tagStatus) {
        this.tagStatus = tagStatus;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
}
