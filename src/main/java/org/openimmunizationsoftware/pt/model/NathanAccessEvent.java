package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class NathanAccessEvent implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_PAGE_VIEW = "PAGE_VIEW";
    public static final String TYPE_RESUME_DOWNLOAD = "RESUME_DOWNLOAD";
    public static final String TYPE_STORY_OPEN = "STORY_OPEN";

    private int nathanAccessEventId;
    private int nathanAccessId;
    private Date createdAt;
    private String eventType;
    private String contentKey;

    public int getNathanAccessEventId() { return nathanAccessEventId; }
    public void setNathanAccessEventId(int value) { nathanAccessEventId = value; }
    public int getNathanAccessId() { return nathanAccessId; }
    public void setNathanAccessId(int value) { nathanAccessId = value; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date value) { createdAt = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String value) { eventType = value; }
    public String getContentKey() { return contentKey; }
    public void setContentKey(String value) { contentKey = value; }
}