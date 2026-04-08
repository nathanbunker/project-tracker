package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class Workspace implements java.io.Serializable {

    private static final long serialVersionUID = 3707332874380537788L;

    public static final String TYPE_PERSONAL = "PERSONAL";
    public static final String STATUS_ACTIVE = "ACTIVE";

    private int workspaceId;
    private String workspaceName;
    private String workspaceType;
    private int createdByWebUserId;
    private String workspaceStatus;
    private Date createdDate;

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getWorkspaceType() {
        return workspaceType;
    }

    public void setWorkspaceType(String workspaceType) {
        this.workspaceType = workspaceType;
    }

    public int getCreatedByWebUserId() {
        return createdByWebUserId;
    }

    public void setCreatedByWebUserId(int createdByWebUserId) {
        this.createdByWebUserId = createdByWebUserId;
    }

    public String getWorkspaceStatus() {
        return workspaceStatus;
    }

    public void setWorkspaceStatus(String workspaceStatus) {
        this.workspaceStatus = workspaceStatus;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}