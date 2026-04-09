package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ProjectPatchLink implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public static final String LINK_TYPE_DIRECT_PROJECT = "DIRECT_PROJECT";
    public static final String LINK_TYPE_PATCH_TAG = "PATCH_TAG";

    private int projectPatchLinkId;
    private int privateProjectId;
    private int patchWorkspaceId;
    private String linkType;
    private Integer linkedPatchProjectId;
    private Integer linkedPatchTagId;
    private int createdByWebUserId;
    private Date createdDate;

    public int getProjectPatchLinkId() {
        return projectPatchLinkId;
    }

    public void setProjectPatchLinkId(int projectPatchLinkId) {
        this.projectPatchLinkId = projectPatchLinkId;
    }

    public int getPrivateProjectId() {
        return privateProjectId;
    }

    public void setPrivateProjectId(int privateProjectId) {
        this.privateProjectId = privateProjectId;
    }

    public int getPatchWorkspaceId() {
        return patchWorkspaceId;
    }

    public void setPatchWorkspaceId(int patchWorkspaceId) {
        this.patchWorkspaceId = patchWorkspaceId;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public Integer getLinkedPatchProjectId() {
        return linkedPatchProjectId;
    }

    public void setLinkedPatchProjectId(Integer linkedPatchProjectId) {
        this.linkedPatchProjectId = linkedPatchProjectId;
    }

    public Integer getLinkedPatchTagId() {
        return linkedPatchTagId;
    }

    public void setLinkedPatchTagId(Integer linkedPatchTagId) {
        this.linkedPatchTagId = linkedPatchTagId;
    }

    public int getCreatedByWebUserId() {
        return createdByWebUserId;
    }

    public void setCreatedByWebUserId(int createdByWebUserId) {
        this.createdByWebUserId = createdByWebUserId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
