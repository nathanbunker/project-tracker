package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class MeetingNoteLine implements java.io.Serializable {

    private static final long serialVersionUID = -5693262399324847158L;

    public static final String NARRATIVE_CATEGORY_NOTE = "NOTE";
    public static final String NARRATIVE_CATEGORY_DECISION = "DECISION";
    public static final String NARRATIVE_CATEGORY_INSIGHT = "INSIGHT";
    public static final String NARRATIVE_CATEGORY_RISK = "RISK";
    public static final String NARRATIVE_CATEGORY_OPPORTUNITY = "OPPORTUNITY";
    public static final String NARRATIVE_CATEGORY_QUESTION = "QUESTION";

    public static final String VISIBILITY_INTERNAL = "INTERNAL";
    public static final String VISIBILITY_SHARED = "SHARED";
    public static final String VISIBILITY_PUBLIC = "PUBLIC";

    private Integer meetingNoteLineId;
    private Integer meetingSessionTopicId;
    private Integer workspaceId;
    private Integer parentMeetingNoteLineId;
    private Integer displayOrder;
    private String noteText;
    private String narrativeCategory;
    private String actionType;
    private Integer linkedNextActionId;
    private String visibility = VISIBILITY_INTERNAL;
    private boolean urlDetected = false;
    private Date createdAt;
    private Date updatedAt;
    private Integer createdByUserId;
    private Integer updatedByUserId;

    public static boolean isValidNarrativeCategory(String category) {
        return NARRATIVE_CATEGORY_NOTE.equals(category)
                || NARRATIVE_CATEGORY_DECISION.equals(category)
                || NARRATIVE_CATEGORY_INSIGHT.equals(category)
                || NARRATIVE_CATEGORY_RISK.equals(category)
                || NARRATIVE_CATEGORY_OPPORTUNITY.equals(category)
                || NARRATIVE_CATEGORY_QUESTION.equals(category);
    }

    public static boolean isValidVisibility(String visibility) {
        return VISIBILITY_INTERNAL.equals(visibility)
                || VISIBILITY_SHARED.equals(visibility)
                || VISIBILITY_PUBLIC.equals(visibility);
    }

    public static boolean isValidActionType(String actionType) {
        return ProjectNextActionType.WILL.equals(actionType)
                || ProjectNextActionType.WILL_CONTACT.equals(actionType)
                || ProjectNextActionType.WILL_MEET.equals(actionType)
                || ProjectNextActionType.WILL_REVIEW.equals(actionType)
                || ProjectNextActionType.WILL_DOCUMENT.equals(actionType)
                || ProjectNextActionType.WILL_FOLLOW_UP.equals(actionType)
                || ProjectNextActionType.MIGHT.equals(actionType)
                || ProjectNextActionType.WOULD_LIKE_TO.equals(actionType)
                || ProjectNextActionType.COMMITTED_TO.equals(actionType)
                || ProjectNextActionType.GOAL.equals(actionType)
                || ProjectNextActionType.WAITING.equals(actionType)
                || ProjectNextActionType.OVERDUE_TO.equals(actionType);
    }

    public Integer getMeetingNoteLineId() {
        return meetingNoteLineId;
    }

    public void setMeetingNoteLineId(Integer meetingNoteLineId) {
        this.meetingNoteLineId = meetingNoteLineId;
    }

    public Integer getMeetingSessionTopicId() {
        return meetingSessionTopicId;
    }

    public void setMeetingSessionTopicId(Integer meetingSessionTopicId) {
        this.meetingSessionTopicId = meetingSessionTopicId;
    }

    public Integer getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Integer workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Integer getParentMeetingNoteLineId() {
        return parentMeetingNoteLineId;
    }

    public void setParentMeetingNoteLineId(Integer parentMeetingNoteLineId) {
        this.parentMeetingNoteLineId = parentMeetingNoteLineId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public String getNarrativeCategory() {
        return narrativeCategory;
    }

    public void setNarrativeCategory(String narrativeCategory) {
        this.narrativeCategory = narrativeCategory;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Integer getLinkedNextActionId() {
        return linkedNextActionId;
    }

    public void setLinkedNextActionId(Integer linkedNextActionId) {
        this.linkedNextActionId = linkedNextActionId;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public boolean isUrlDetected() {
        return urlDetected;
    }

    public void setUrlDetected(boolean urlDetected) {
        this.urlDetected = urlDetected;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Integer getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Integer updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }
}
