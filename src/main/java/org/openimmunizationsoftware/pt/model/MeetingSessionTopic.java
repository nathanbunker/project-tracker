package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class MeetingSessionTopic implements java.io.Serializable {

    private static final long serialVersionUID = 3188398419887807780L;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PROPOSED = "PROPOSED";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_NEEDS_REVISION = "NEEDS_REVISION";
    public static final String STATUS_COVERED = "COVERED";
    public static final String STATUS_NOT_COVERED = "NOT_COVERED";
    public static final String STATUS_POSTPONED = "POSTPONED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private Integer meetingSessionTopicId;
    private Integer meetingSessionId;
    private Integer workspaceId;
    private Integer interopHubAgendaItemId;
    private Integer interopHubTopicId;
    private Integer projectId;
    private String title;
    private String agendaMarkdown;
    private Integer displayOrder;
    private Integer timeMinutes;
    private String status = STATUS_DRAFT;
    private String statusNote;
    private Integer postponedToInteropHubMeetingId;
    private Date interopHubUpdatedAt;
    private Date lastSyncedAt;
    private Date createdAt;
    private Date updatedAt;

    public static boolean isValidStatus(String status) {
        return STATUS_DRAFT.equals(status)
                || STATUS_PROPOSED.equals(status)
                || STATUS_ACCEPTED.equals(status)
                || STATUS_NEEDS_REVISION.equals(status)
                || STATUS_COVERED.equals(status)
                || STATUS_NOT_COVERED.equals(status)
                || STATUS_POSTPONED.equals(status)
                || STATUS_CANCELLED.equals(status);
    }

    public Integer getMeetingSessionTopicId() {
        return meetingSessionTopicId;
    }

    public void setMeetingSessionTopicId(Integer meetingSessionTopicId) {
        this.meetingSessionTopicId = meetingSessionTopicId;
    }

    public Integer getMeetingSessionId() {
        return meetingSessionId;
    }

    public void setMeetingSessionId(Integer meetingSessionId) {
        this.meetingSessionId = meetingSessionId;
    }

    public Integer getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Integer workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Integer getInteropHubAgendaItemId() {
        return interopHubAgendaItemId;
    }

    public void setInteropHubAgendaItemId(Integer interopHubAgendaItemId) {
        this.interopHubAgendaItemId = interopHubAgendaItemId;
    }

    public Integer getInteropHubTopicId() {
        return interopHubTopicId;
    }

    public void setInteropHubTopicId(Integer interopHubTopicId) {
        this.interopHubTopicId = interopHubTopicId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAgendaMarkdown() {
        return agendaMarkdown;
    }

    public void setAgendaMarkdown(String agendaMarkdown) {
        this.agendaMarkdown = agendaMarkdown;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Integer getTimeMinutes() {
        return timeMinutes;
    }

    public void setTimeMinutes(Integer timeMinutes) {
        this.timeMinutes = timeMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusNote() {
        return statusNote;
    }

    public void setStatusNote(String statusNote) {
        this.statusNote = statusNote;
    }

    public Integer getPostponedToInteropHubMeetingId() {
        return postponedToInteropHubMeetingId;
    }

    public void setPostponedToInteropHubMeetingId(Integer postponedToInteropHubMeetingId) {
        this.postponedToInteropHubMeetingId = postponedToInteropHubMeetingId;
    }

    public Date getInteropHubUpdatedAt() {
        return interopHubUpdatedAt;
    }

    public void setInteropHubUpdatedAt(Date interopHubUpdatedAt) {
        this.interopHubUpdatedAt = interopHubUpdatedAt;
    }

    public Date getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Date lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
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
}
