package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class WeUserDependency implements java.io.Serializable {

    private static final long serialVersionUID = -7136156923604951067L;

    private int dependencyId;
    private WebUser guardianWebUser;
    private WebUser dependentWebUser;
    private String relationshipType = "guardian";
    private String dependencyStatus = "invited";
    private boolean canViewTodaySummary = true;
    private boolean canViewNextActions = true;
    private boolean canAddActions = true;
    private boolean canEditActions = false;
    private String inviteEmail;
    private String inviteTokenHash;
    private Date inviteExpiry;
    private Date createdDate;
    private Date acceptedDate;
    private Date endedDate;

    public int getDependencyId() {
        return dependencyId;
    }

    public void setDependencyId(int dependencyId) {
        this.dependencyId = dependencyId;
    }

    public WebUser getGuardianWebUser() {
        return guardianWebUser;
    }

    public void setGuardianWebUser(WebUser guardianWebUser) {
        this.guardianWebUser = guardianWebUser;
    }

    public WebUser getDependentWebUser() {
        return dependentWebUser;
    }

    public void setDependentWebUser(WebUser dependentWebUser) {
        this.dependentWebUser = dependentWebUser;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getDependencyStatus() {
        return dependencyStatus;
    }

    public void setDependencyStatus(String dependencyStatus) {
        this.dependencyStatus = dependencyStatus;
    }

    public boolean isCanViewTodaySummary() {
        return canViewTodaySummary;
    }

    public void setCanViewTodaySummary(boolean canViewTodaySummary) {
        this.canViewTodaySummary = canViewTodaySummary;
    }

    public boolean isCanViewNextActions() {
        return canViewNextActions;
    }

    public void setCanViewNextActions(boolean canViewNextActions) {
        this.canViewNextActions = canViewNextActions;
    }

    public boolean isCanAddActions() {
        return canAddActions;
    }

    public void setCanAddActions(boolean canAddActions) {
        this.canAddActions = canAddActions;
    }

    public boolean isCanEditActions() {
        return canEditActions;
    }

    public void setCanEditActions(boolean canEditActions) {
        this.canEditActions = canEditActions;
    }

    public String getInviteEmail() {
        return inviteEmail;
    }

    public void setInviteEmail(String inviteEmail) {
        this.inviteEmail = inviteEmail;
    }

    public String getInviteTokenHash() {
        return inviteTokenHash;
    }

    public void setInviteTokenHash(String inviteTokenHash) {
        this.inviteTokenHash = inviteTokenHash;
    }

    public Date getInviteExpiry() {
        return inviteExpiry;
    }

    public void setInviteExpiry(Date inviteExpiry) {
        this.inviteExpiry = inviteExpiry;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getAcceptedDate() {
        return acceptedDate;
    }

    public void setAcceptedDate(Date acceptedDate) {
        this.acceptedDate = acceptedDate;
    }

    public Date getEndedDate() {
        return endedDate;
    }

    public void setEndedDate(Date endedDate) {
        this.endedDate = endedDate;
    }
}
