package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class NathanAccess implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_REQUEST = "REQUEST";
    public static final String TYPE_INVITATION = "INVITATION";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";

    private int nathanAccessId;
    private Date createdAt;
    private Integer createdByUserId;
    private String accessType;
    private String status;
    private String tokenHash;
    private String email;
    private String label;
    private String notes;
    private Date requestedAt;
    private Date approvedAt;
    private Integer approvedByUserId;
    private Date firstUsedAt;
    private Date lastUsedAt;
    private int useCount;
    private Date expiresAt;

    public int getNathanAccessId() { return nathanAccessId; }
    public void setNathanAccessId(int value) { nathanAccessId = value; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date value) { createdAt = value; }
    public Integer getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Integer value) { createdByUserId = value; }
    public String getAccessType() { return accessType; }
    public void setAccessType(String value) { accessType = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String value) { tokenHash = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getLabel() { return label; }
    public void setLabel(String value) { label = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public Date getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Date value) { requestedAt = value; }
    public Date getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Date value) { approvedAt = value; }
    public Integer getApprovedByUserId() { return approvedByUserId; }
    public void setApprovedByUserId(Integer value) { approvedByUserId = value; }
    public Date getFirstUsedAt() { return firstUsedAt; }
    public void setFirstUsedAt(Date value) { firstUsedAt = value; }
    public Date getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Date value) { lastUsedAt = value; }
    public int getUseCount() { return useCount; }
    public void setUseCount(int value) { useCount = value; }
    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date value) { expiresAt = value; }
}