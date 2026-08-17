package org.openimmunizationsoftware.pt.schooltemplate;

import java.util.List;

/** One parsed (but not yet validated against the database) record from an uploaded Template Scheduler JSON file. */
public class SchoolTemplateImportEntry {

    private final int recordNumber;
    private Integer id;
    private boolean delete;
    private String project;
    private String description;
    private String scheduleType;
    private List<String> daysOfWeek;
    private List<String> daysOfMonth;
    private List<String> daysOfQuarter;
    private List<String> daysOfYear;
    private String missedActionBehavior;
    private Boolean autoGenerate;
    private String actionType;
    private Integer timeEstimateMinutes;
    private Integer gamePoints;
    private String timeSlot;

    public SchoolTemplateImportEntry(int recordNumber) {
        this.recordNumber = recordNumber;
    }

    public int getRecordNumber() {
        return recordNumber;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public List<String> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<String> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public List<String> getDaysOfMonth() {
        return daysOfMonth;
    }

    public void setDaysOfMonth(List<String> daysOfMonth) {
        this.daysOfMonth = daysOfMonth;
    }

    public List<String> getDaysOfQuarter() {
        return daysOfQuarter;
    }

    public void setDaysOfQuarter(List<String> daysOfQuarter) {
        this.daysOfQuarter = daysOfQuarter;
    }

    public List<String> getDaysOfYear() {
        return daysOfYear;
    }

    public void setDaysOfYear(List<String> daysOfYear) {
        this.daysOfYear = daysOfYear;
    }

    public String getMissedActionBehavior() {
        return missedActionBehavior;
    }

    public void setMissedActionBehavior(String missedActionBehavior) {
        this.missedActionBehavior = missedActionBehavior;
    }

    public Boolean getAutoGenerate() {
        return autoGenerate;
    }

    public void setAutoGenerate(Boolean autoGenerate) {
        this.autoGenerate = autoGenerate;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Integer getTimeEstimateMinutes() {
        return timeEstimateMinutes;
    }

    public void setTimeEstimateMinutes(Integer timeEstimateMinutes) {
        this.timeEstimateMinutes = timeEstimateMinutes;
    }

    public Integer getGamePoints() {
        return gamePoints;
    }

    public void setGamePoints(Integer gamePoints) {
        this.gamePoints = gamePoints;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }
}
