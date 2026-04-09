package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class WorkspaceMember implements java.io.Serializable {

    private static final long serialVersionUID = 6206499277468475146L;

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String STATUS_ACTIVE = "ACTIVE";

    private int workspaceMemberId;
    private int workspaceId;
    private int webUserId;
    private String memberRole;
    private String membershipStatus;
    private Date createdDate;

    public int getWorkspaceMemberId() {
        return workspaceMemberId;
    }

    public void setWorkspaceMemberId(int workspaceMemberId) {
        this.workspaceMemberId = workspaceMemberId;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public int getWebUserId() {
        return webUserId;
    }

    public void setWebUserId(int webUserId) {
        this.webUserId = webUserId;
    }

    public String getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(String memberRole) {
        this.memberRole = memberRole;
    }

    public String getMembershipStatus() {
        return membershipStatus;
    }

    public void setMembershipStatus(String membershipStatus) {
        this.membershipStatus = membershipStatus;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}