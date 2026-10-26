package org.openimmunizationsoftware.pt.model;

public class LinkedMeeting implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Integer linkedMeetingId;
    private long version;
    private int workspaceId;
    private int actionNextId;
    private String sourceSystem;
    private String sourceNamespace;
    private String externalEventUid;
    private String externalOccurrenceId;
    private String originalSubject;
    private String originalDescription;
    private String organizerName;
    private String organizerAddress;
    private long scheduledStartEpochMs;
    private long scheduledEndEpochMs;
    private String sourceTimeZone;
    private String location;
    private String joinUrl;
    private String sourceStatus;
    private Long sourceLastModifiedEpochMs;
    private String sourceRevision;
    private String sourceFingerprint;
    private long lastSyncedEpochMs;

    public Integer getLinkedMeetingId() { return linkedMeetingId; }
    public void setLinkedMeetingId(Integer value) { linkedMeetingId = value; }
    public long getVersion() { return version; }
    public void setVersion(long value) { version = value; }
    public int getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(int value) { workspaceId = value; }
    public int getActionNextId() { return actionNextId; }
    public void setActionNextId(int value) { actionNextId = value; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String value) { sourceSystem = value; }
    public String getSourceNamespace() { return sourceNamespace; }
    public void setSourceNamespace(String value) { sourceNamespace = value; }
    public String getExternalEventUid() { return externalEventUid; }
    public void setExternalEventUid(String value) { externalEventUid = value; }
    public String getExternalOccurrenceId() { return externalOccurrenceId; }
    public void setExternalOccurrenceId(String value) { externalOccurrenceId = value; }
    public String getOriginalSubject() { return originalSubject; }
    public void setOriginalSubject(String value) { originalSubject = value; }
    public String getOriginalDescription() { return originalDescription; }
    public void setOriginalDescription(String value) { originalDescription = value; }
    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String value) { organizerName = value; }
    public String getOrganizerAddress() { return organizerAddress; }
    public void setOrganizerAddress(String value) { organizerAddress = value; }
    public long getScheduledStartEpochMs() { return scheduledStartEpochMs; }
    public void setScheduledStartEpochMs(long value) { scheduledStartEpochMs = value; }
    public long getScheduledEndEpochMs() { return scheduledEndEpochMs; }
    public void setScheduledEndEpochMs(long value) { scheduledEndEpochMs = value; }
    public String getSourceTimeZone() { return sourceTimeZone; }
    public void setSourceTimeZone(String value) { sourceTimeZone = value; }
    public String getLocation() { return location; }
    public void setLocation(String value) { location = value; }
    public String getJoinUrl() { return joinUrl; }
    public void setJoinUrl(String value) { joinUrl = value; }
    public String getSourceStatus() { return sourceStatus; }
    public void setSourceStatus(String value) { sourceStatus = value; }
    public Long getSourceLastModifiedEpochMs() { return sourceLastModifiedEpochMs; }
    public void setSourceLastModifiedEpochMs(Long value) { sourceLastModifiedEpochMs = value; }
    public String getSourceRevision() { return sourceRevision; }
    public void setSourceRevision(String value) { sourceRevision = value; }
    public String getSourceFingerprint() { return sourceFingerprint; }
    public void setSourceFingerprint(String value) { sourceFingerprint = value; }
    public long getLastSyncedEpochMs() { return lastSyncedEpochMs; }
    public void setLastSyncedEpochMs(long value) { lastSyncedEpochMs = value; }
}
