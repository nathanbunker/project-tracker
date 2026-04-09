package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ProjectActionSet implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private int actionSetId;
    private String actionSetTypeString;
    private int createdByWebUserId;
    private WebUser createdByWebUser;
    private Date createdDate;

    public int getActionSetId() {
        return actionSetId;
    }

    public void setActionSetId(int actionSetId) {
        this.actionSetId = actionSetId;
    }

    public String getActionSetTypeString() {
        return actionSetTypeString;
    }

    public void setActionSetTypeString(String actionSetTypeString) {
        this.actionSetTypeString = actionSetTypeString;
    }

    public ActionSetType getActionSetType() {
        return ActionSetType.get(actionSetTypeString);
    }

    public void setActionSetType(ActionSetType actionSetType) {
        this.actionSetTypeString = actionSetType == null ? null : actionSetType.getId();
    }

    public int getCreatedByWebUserId() {
        return createdByWebUserId;
    }

    public void setCreatedByWebUserId(int createdByWebUserId) {
        this.createdByWebUserId = createdByWebUserId;
    }

    public WebUser getCreatedByWebUser() {
        return createdByWebUser;
    }

    public void setCreatedByWebUser(WebUser createdByWebUser) {
        this.createdByWebUser = createdByWebUser;
        if (createdByWebUser != null) {
            this.createdByWebUserId = createdByWebUser.getWebUserId();
        }
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
