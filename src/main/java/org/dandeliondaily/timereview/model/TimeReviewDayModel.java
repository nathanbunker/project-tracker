package org.dandeliondaily.timereview.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TimeReviewDayModel {

    private Date selectedDate;
    private String selectedDateIso = "";
    private String selectedDateLabel = "";
    private int totalMinutes;
    private String totalDisplay = "";
    private boolean hasEntries;
    private Integer lockedBillEntryId;
    private final List<TimeSessionModel> sessions = new ArrayList<TimeSessionModel>();

    public Date getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(Date selectedDate) {
        this.selectedDate = selectedDate;
    }

    public String getSelectedDateIso() {
        return selectedDateIso;
    }

    public void setSelectedDateIso(String selectedDateIso) {
        this.selectedDateIso = selectedDateIso == null ? "" : selectedDateIso;
    }

    public String getSelectedDateLabel() {
        return selectedDateLabel;
    }

    public void setSelectedDateLabel(String selectedDateLabel) {
        this.selectedDateLabel = selectedDateLabel == null ? "" : selectedDateLabel;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public String getTotalDisplay() {
        return totalDisplay;
    }

    public void setTotalDisplay(String totalDisplay) {
        this.totalDisplay = totalDisplay == null ? "" : totalDisplay;
    }

    public boolean isHasEntries() {
        return hasEntries;
    }

    public void setHasEntries(boolean hasEntries) {
        this.hasEntries = hasEntries;
    }

    public Integer getLockedBillEntryId() {
        return lockedBillEntryId;
    }

    public void setLockedBillEntryId(Integer lockedBillEntryId) {
        this.lockedBillEntryId = lockedBillEntryId;
    }

    public List<TimeSessionModel> getSessions() {
        return sessions;
    }
}
