package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ActionNextNote implements java.io.Serializable {

    private static final long serialVersionUID = 9204663694949855184L;

    private int actionNextNoteId;
    private int actionNextId;
    private int contactId;
    private String noteLine;
    private Date noteDate;
    private ActionNext actionNext;
    private ProjectContact contact;

    public int getActionNextNoteId() {
        return actionNextNoteId;
    }

    public void setActionNextNoteId(int actionNextNoteId) {
        this.actionNextNoteId = actionNextNoteId;
    }

    public int getActionNextId() {
        return actionNextId;
    }

    public void setActionNextId(int actionNextId) {
        this.actionNextId = actionNextId;
    }

    public int getContactId() {
        return contactId;
    }

    public void setContactId(int contactId) {
        this.contactId = contactId;
    }

    public String getNoteLine() {
        return noteLine;
    }

    public void setNoteLine(String noteLine) {
        this.noteLine = noteLine;
    }

    public Date getNoteDate() {
        return noteDate;
    }

    public void setNoteDate(Date noteDate) {
        this.noteDate = noteDate;
    }

    public ActionNext getActionNext() {
        return actionNext;
    }

    public void setActionNext(ActionNext actionNext) {
        this.actionNext = actionNext;
        if (actionNext != null) {
            this.actionNextId = actionNext.getActionNextId();
        }
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
}
