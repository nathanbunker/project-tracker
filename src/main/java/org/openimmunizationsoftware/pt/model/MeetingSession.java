package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class MeetingSession implements java.io.Serializable {

    private static final long serialVersionUID = 6756766938788970025L;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PROPOSED = "PROPOSED";
    public static final String STATUS_FINALIZED = "FINALIZED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private Integer meetingSessionId;
    private Integer workspaceId;
    private Integer interopHubMeetingId;
    private String meetingKey;
    private String meetingName;
    private String meetingDescription;
    private Date scheduledStart;
    private Date scheduledEnd;
    private String timezoneId;
    private String status = STATUS_DRAFT;
    private Integer parentProjectId;
    private String onlineMeetingUrl;
    private String onlineMeetingDetails;
    private String cancellationReason;
    private Date interopHubUpdatedAt;
    private Date lastSyncedAt;
    private Date createdAt;
    private Date updatedAt;
    private Integer createdByUserId;
    private Integer updatedByUserId;

    public static boolean isValidStatus(String status) {
        return STATUS_DRAFT.equals(status)
                || STATUS_PROPOSED.equals(status)
                || STATUS_FINALIZED.equals(status)
                || STATUS_COMPLETED.equals(status)
                || STATUS_CANCELLED.equals(status);
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

    public Integer getInteropHubMeetingId() {
        return interopHubMeetingId;
    }

    public void setInteropHubMeetingId(Integer interopHubMeetingId) {
        this.interopHubMeetingId = interopHubMeetingId;
    }

    public String getMeetingKey() {
        return meetingKey;
    }

    public void setMeetingKey(String meetingKey) {
        this.meetingKey = meetingKey;
    }

    public String getMeetingName() {
        return meetingName;
    }

    public void setMeetingName(String meetingName) {
        this.meetingName = meetingName;
    }

    public String getMeetingDescription() {
        return meetingDescription;
    }

    public void setMeetingDescription(String meetingDescription) {
        this.meetingDescription = meetingDescription;
    }

    public Date getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(Date scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public Date getScheduledEnd() {
        return scheduledEnd;
    }

    public void setScheduledEnd(Date scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public void setTimezoneId(String timezoneId) {
        this.timezoneId = timezoneId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getParentProjectId() {
        return parentProjectId;
    }

    public void setParentProjectId(Integer parentProjectId) {
        this.parentProjectId = parentProjectId;
    }

    public String getOnlineMeetingUrl() {
        return onlineMeetingUrl;
    }

    public void setOnlineMeetingUrl(String onlineMeetingUrl) {
        this.onlineMeetingUrl = onlineMeetingUrl;
    }

    public String getOnlineMeetingDetails() {
        return onlineMeetingDetails;
    }

    public void setOnlineMeetingDetails(String onlineMeetingDetails) {
        this.onlineMeetingDetails = onlineMeetingDetails;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
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
