package org.openimmunizationsoftware.pt.api.v1.resource.dto;

public class SyncContactUpsertItem {

    private String externalContactId;
    private String nameLast;
    private String nameFirst;
    private String nameTitle;
    private String organizationName;
    private String emailAddress;
    private String timeZone;
    private String contactStatus;

    private boolean hasNameLast;
    private boolean hasNameFirst;
    private boolean hasNameTitle;
    private boolean hasOrganizationName;
    private boolean hasEmailAddress;
    private boolean hasTimeZone;
    private boolean hasContactStatus;

    public String getExternalContactId() {
        return externalContactId;
    }

    public void setExternalContactId(String externalContactId) {
        this.externalContactId = externalContactId;
    }

    public String getNameLast() {
        return nameLast;
    }

    public void setNameLast(String nameLast) {
        this.nameLast = nameLast;
        this.hasNameLast = true;
    }

    public String getNameFirst() {
        return nameFirst;
    }

    public void setNameFirst(String nameFirst) {
        this.nameFirst = nameFirst;
        this.hasNameFirst = true;
    }

    public String getNameTitle() {
        return nameTitle;
    }

    public void setNameTitle(String nameTitle) {
        this.nameTitle = nameTitle;
        this.hasNameTitle = true;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
        this.hasOrganizationName = true;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
        this.hasEmailAddress = true;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
        this.hasTimeZone = true;
    }

    public String getContactStatus() {
        return contactStatus;
    }

    public void setContactStatus(String contactStatus) {
        this.contactStatus = contactStatus;
        this.hasContactStatus = true;
    }

    public boolean isHasNameLast() {
        return hasNameLast;
    }

    public boolean isHasNameFirst() {
        return hasNameFirst;
    }

    public boolean isHasNameTitle() {
        return hasNameTitle;
    }

    public boolean isHasOrganizationName() {
        return hasOrganizationName;
    }

    public boolean isHasEmailAddress() {
        return hasEmailAddress;
    }

    public boolean isHasTimeZone() {
        return hasTimeZone;
    }

    public boolean isHasContactStatus() {
        return hasContactStatus;
    }
}
