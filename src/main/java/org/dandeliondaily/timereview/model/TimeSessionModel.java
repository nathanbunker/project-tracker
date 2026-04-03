package org.dandeliondaily.timereview.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TimeSessionModel {

    private int sequence;
    private Date startTime;
    private Date endTime;
    private int totalMinutes;
    private String totalDisplay = "";
    private Integer breakMinutesBefore;
    private String breakDisplay = "";
    private final List<TimeEntryModel> entries = new ArrayList<TimeEntryModel>();

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
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

    public Integer getBreakMinutesBefore() {
        return breakMinutesBefore;
    }

    public void setBreakMinutesBefore(Integer breakMinutesBefore) {
        this.breakMinutesBefore = breakMinutesBefore;
    }

    public String getBreakDisplay() {
        return breakDisplay;
    }

    public void setBreakDisplay(String breakDisplay) {
        this.breakDisplay = breakDisplay == null ? "" : breakDisplay;
    }

    public List<TimeEntryModel> getEntries() {
        return entries;
    }
}
