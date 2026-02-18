package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ProjectActionNext implements java.io.Serializable {

    private static final long serialVersionUID = -6591604788851392516L;

    private int actionNextId;
    private int projectId;
    private int contactId;
    private ProjectProvider provider;
    private ProjectContact contact;
    private Project project;
    private ProjectContact nextProjectContact;
    private String nextActionType;
    private Integer nextTimeEstimate;
    private Integer nextTimeActual;
    private Integer nextContactId;
    private int priorityLevel = 0;
    private PrioritySpecial prioritySpecial;
    private String goalStatus;
    private Integer templateActionNextId;
    private String linkUrl = "";
    private String nextNotes = "";
    private String nextSummary = "";
    private String nextFeedback = "";
    private TemplateType templateType;
    private ProjectNextActionStatus nextActionStatus;
    private Date nextChangeDate;
    private Date nextActionDate;
    private Date nextDeadlineDate;
    private Date nextTargetDate;
    private String nextDescription;
    private boolean billable;

    public int getActionNextId() {
        return actionNextId;
    }

    public void setActionNextId(int actionNextId) {
        this.actionNextId = actionNextId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getContactId() {
        return contactId;
    }

    public void setContactId(int contactId) {
        this.contactId = contactId;
    }

    public ProjectProvider getProvider() {
        return provider;
    }

    public void setProvider(ProjectProvider provider) {
        this.provider = provider;
    }

    public ProjectContact getContact() {
        return contact;
    }

    public void setContact(ProjectContact contact) {
        this.contact = contact;
        if (contact != null) {
            this.contactId = contact.getContactId();
        }
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
        if (project != null) {
            this.projectId = project.getProjectId();
        }
    }

    public ProjectContact getNextProjectContact() {
        return nextProjectContact;
    }

    public void setNextProjectContact(ProjectContact nextProjectContact) {
        this.nextProjectContact = nextProjectContact;
        if (nextProjectContact != null) {
            this.nextContactId = nextProjectContact.getContactId();
        } else {
            this.nextContactId = null;
        }
    }

    public String getNextActionType() {
        return nextActionType;
    }

    public boolean isWill() {
        return nextActionType != null && nextActionType.equals(ProjectNextActionType.WILL);
    }

    public boolean isWillContact() {
        return nextActionType != null && nextActionType.equals(ProjectNextActionType.WILL_CONTACT);
    }

    public boolean isWillMeet() {
        return nextActionType != null && nextActionType.equals(ProjectNextActionType.WILL_MEET);
    }

    public boolean isWillReview() {
        return nextActionType != null && nextActionType.equals(ProjectNextActionType.WILL_REVIEW);
    }

    public boolean isWillDocument() {
        return nextActionType != null && nextActionType.equals(ProjectNextActionType.WILL_DOCUMENT);
    }

    public boolean isMight() {
        return nextActionType != null && nextActionType.equals(ProjectNextActionType.MIGHT);
    }

    public boolean isCommittedTo() {
        return nextActionType != null && nextActionType.equals(ProjectNextActionType.COMMITTED_TO);
    }

    public boolean isGoal() {
        return nextActionType != null && nextActionType.equals(ProjectNextActionType.GOAL);
    }

    public boolean isFollowUp() {
        return nextActionType != null && nextActionType.equals(ProjectNextActionType.WILL_FOLLOW_UP);
    }

    public boolean isWaiting() {
        return nextActionType != null && nextActionType.equals(ProjectNextActionType.WAITING);
    }

    public boolean isOverdueTo() {
        return nextActionType != null && nextActionType.equals(ProjectNextActionType.OVERDUE_TO);
    }

    public void setNextActionType(String nextActionType) {
        this.nextActionType = nextActionType;
    }

    public Integer getNextTimeEstimate() {
        return nextTimeEstimate;
    }

    public void setNextTimeEstimate(Integer nextTimeEstimate) {
        this.nextTimeEstimate = nextTimeEstimate;
    }

    public Integer getNextTimeActual() {
        return nextTimeActual;
    }

    public void setNextTimeActual(Integer nextTimeActual) {
        this.nextTimeActual = nextTimeActual;
    }

    public Integer getNextContactId() {
        return nextContactId;
    }

    public void setNextContactId(Integer nextContactId) {
        this.nextContactId = nextContactId;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public PrioritySpecial getPrioritySpecial() {
        return prioritySpecial;
    }

    public void setPrioritySpecial(PrioritySpecial prioritySpecial) {
        this.prioritySpecial = prioritySpecial;
    }

    public String getPrioritySpecialString() {
        if (prioritySpecial == null) {
            return "";
        }
        return prioritySpecial.getId();
    }

    public void setPrioritySpecialString(String prioritySpecialString) {
        this.prioritySpecial = PrioritySpecial.getPrioritySpecial(prioritySpecialString);
    }

    public String getGoalStatus() {
        return goalStatus;
    }

    public void setGoalStatus(String goalStatus) {
        this.goalStatus = goalStatus;
    }

    public Integer getTemplateActionNextId() {
        return templateActionNextId;
    }

    public void setTemplateActionNextId(Integer templateActionNextId) {
        this.templateActionNextId = templateActionNextId;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getNextNotes() {
        return nextNotes;
    }

    public void setNextNotes(String nextNotes) {
        this.nextNotes = nextNotes;
    }

    public String getNextSummary() {
        return nextSummary;
    }

    public void setNextSummary(String nextSummary) {
        this.nextSummary = nextSummary;
    }

    public String getNextFeedback() {
        return nextFeedback;
    }

    public void setNextFeedback(String nextFeedback) {
        this.nextFeedback = nextFeedback;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public boolean isTemplate() {
        return templateType != null;
    }

    public void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }

    public String getTemplateTypeString() {
        if (templateType == null) {
            return "";
        }
        return templateType.getId();
    }

    public void setTemplateTypeString(String templateTypeString) {
        this.templateType = TemplateType.getTemplateType(templateTypeString);
    }

    public ProjectNextActionStatus getNextActionStatus() {
        return nextActionStatus;
    }

    public void setNextActionStatus(ProjectNextActionStatus nextActionStatus) {
        this.nextActionStatus = nextActionStatus;
    }

    public String getNextActionStatusString() {
        if (nextActionStatus == null) {
            return "";
        }
        return nextActionStatus.getId();
    }

    public void setNextActionStatusString(String nextActionStatusString) {
        this.nextActionStatus = ProjectNextActionStatus.getProjectNextActionStatus(nextActionStatusString);
    }

    public Date getNextChangeDate() {
        return nextChangeDate;
    }

    public void setNextChangeDate(Date nextChangeDate) {
        this.nextChangeDate = nextChangeDate;
    }

    public Date getNextActionDate() {
        return nextActionDate;
    }

    public boolean hasNextActionDate() {
        return nextActionDate != null;
    }

    public void setNextActionDate(Date nextActionDate) {
        this.nextActionDate = nextActionDate;
    }

    public Date getNextDeadlineDate() {
        return nextDeadlineDate;
    }

    public void setNextDeadlineDate(Date nextDeadlineDate) {
        this.nextDeadlineDate = nextDeadlineDate;
    }

    public Date getNextTargetDate() {
        return nextTargetDate;
    }

    public void setNextTargetDate(Date nextTargetDate) {
        this.nextTargetDate = nextTargetDate;
    }

    public String getNextDescription() {
        return nextDescription;
    }

    public boolean hasNextDescription() {
        return nextDescription != null && !nextDescription.equals("");
    }

    public String getNextDescriptionForDisplay(ProjectContact forContact) {
        String i_am = "I am ";
        String i_ = "I ";
        String i_have = "I have";
        if (forContact != null && contact != null && forContact.getContactId() != contact.getContactId()) {
            i_am = contact.getName() + " is ";
            i_ = contact.getName() + " ";
            i_have = contact.getName() + " has ";
        }

        String description = "";
        String type = getNextActionType();
        if (type == null || type.equals("")) {
            description = getNextDescription();
        } else if (type.equals(ProjectNextActionType.WILL_CONTACT)) {
            if (nextProjectContact == null) {
                description = "<i>" + i_ + " will make contact about</i> " + getNextDescription();
            } else {
                description = "<i>" + i_ + " will contact " + nextProjectContact.getName() + " about</i> "
                        + getNextDescription();
            }
        } else if (type.equals(ProjectNextActionType.WILL)) {
            description = "<i>" + i_ + " will</i> " + getNextDescription();
        } else if (type.equals(ProjectNextActionType.WILL_MEET)) {
            description = "<i>" + i_ + " will meet</i> " + getNextDescription();
        } else if (type.equals(ProjectNextActionType.WILL_REVIEW)) {
            description = "<i>" + i_ + " will review</i> " + getNextDescription();
        } else if (type.equals(ProjectNextActionType.WILL_DOCUMENT)) {
            description = "<i>" + i_ + " will document</i> " + getNextDescription();
        } else if (type.equals(ProjectNextActionType.GOAL)) {
            if (nextProjectContact == null) {
                description = "<i>" + i_have + " set a goal to </i> " + getNextDescription();
            } else {
                description = "<i>" + i_have + " set a goal with " + nextProjectContact.getName()
                        + " to</i> " + getNextDescription();
            }
        } else if (type.equals(ProjectNextActionType.MIGHT)) {
            description = "<i>" + i_ + " might</i> " + getNextDescription();
        } else if (type.equals(ProjectNextActionType.COMMITTED_TO)) {
            if (nextProjectContact == null) {
                description = "<i>" + i_have + " committed to </i> " + getNextDescription();
            } else {
                description = "<i>" + i_have + " committed to " + nextProjectContact.getName() + " to</i> "
                        + getNextDescription();
            }
        } else if (type.equals(ProjectNextActionType.OVERDUE_TO)) {
            if (nextProjectContact == null) {
                description = "<i>" + i_am + " overdue to</i> " + getNextDescription();
            } else {
                description = "<i>" + i_am + "overdue in committment to " + nextProjectContact.getName()
                        + " to</i> " + getNextDescription();
            }
        } else if (type.equals(ProjectNextActionType.WAITING)) {
            if (nextProjectContact == null) {
                description = "<i>" + i_am + "waiting for </i> " + getNextDescription();
            } else {
                description = "<i>" + i_am + "waiting for " + nextProjectContact.getName() + " to</i> "
                        + getNextDescription();
            }
        } else if (type.equals(ProjectNextActionType.WILL_FOLLOW_UP)) {
            if (nextProjectContact == null) {
                description = "<i>" + i_ + " will follow up </i> " + getNextDescription();
            } else {
                description = "<i>" + i_ + " will follow up with " + nextProjectContact.getName() + " to</i> "
                        + getNextDescription();
            }
        } else {
            description = "<i>" + i_ + " will " + type + "</i> " + getNextDescription();
        }
        return description;
    }

    public void setNextDescription(String nextDescription) {
        this.nextDescription = nextDescription;
    }

    public boolean isBillable() {
        return billable;
    }

    public void setBillable(boolean billable) {
        this.billable = billable;
    }

    public String getNextTimeEstimateMinsForDisplay() {
        if (nextTimeEstimate == null) {
            return "";
        }
        return "" + nextTimeEstimate;
    }

    public String getNextTimeEstimateForDisplay() {
        if (nextTimeEstimate == null) {
            return "";
        }
        return getTimeForDisplay(getNextTimeEstimate());
    }

    public String getNextTimeActualForDisplay() {
        if (nextTimeActual == null) {
            return "";
        }
        return getTimeForDisplay(getNextTimeActual());
    }

    public static String getTimeForDisplay(int nextTimeEstimateTotal) {
        String time = "";
        if (nextTimeEstimateTotal == 0) {
            time = "-";
        } else if (nextTimeEstimateTotal < 10) {
            time = "0:0" + nextTimeEstimateTotal;
        } else if (nextTimeEstimateTotal < 60) {
            time = "0:" + nextTimeEstimateTotal;
        } else {
            int hour = nextTimeEstimateTotal / 60;
            int minute = nextTimeEstimateTotal % 60;
            if (minute < 10) {
                time = hour + ":0" + minute;
            } else {
                time = hour + ":" + minute;
            }
        }
        return time;
    }
}
