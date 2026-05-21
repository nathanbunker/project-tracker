package org.openimmunizationsoftware.pt.model;

import java.io.Serializable;
import java.util.Date;

public class ActionNextTemplateConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private int actionNextId;
    private boolean autoGenerate = true;
    private String missedActionBehavior = "AUTO_CANCEL";
    private String scheduleDaysOfWeek;
    private String scheduleDaysOfMonth;
    private String scheduleDaysOfQuarter;
    private String scheduleDaysOfYear;
    private Date lastGeneratedDate;

    public int getActionNextId() {
        return actionNextId;
    }

    public void setActionNextId(int actionNextId) {
        this.actionNextId = actionNextId;
    }

    public boolean isAutoGenerate() {
        return autoGenerate;
    }

    public void setAutoGenerate(boolean autoGenerate) {
        this.autoGenerate = autoGenerate;
    }

    public String getMissedActionBehavior() {
        return missedActionBehavior;
    }

    public void setMissedActionBehavior(String missedActionBehavior) {
        this.missedActionBehavior = missedActionBehavior;
    }

    public String getScheduleDaysOfWeek() {
        return scheduleDaysOfWeek;
    }

    public void setScheduleDaysOfWeek(String scheduleDaysOfWeek) {
        this.scheduleDaysOfWeek = scheduleDaysOfWeek;
    }

    public String getScheduleDaysOfMonth() {
        return scheduleDaysOfMonth;
    }

    public void setScheduleDaysOfMonth(String scheduleDaysOfMonth) {
        this.scheduleDaysOfMonth = scheduleDaysOfMonth;
    }

    public String getScheduleDaysOfQuarter() {
        return scheduleDaysOfQuarter;
    }

    public void setScheduleDaysOfQuarter(String scheduleDaysOfQuarter) {
        this.scheduleDaysOfQuarter = scheduleDaysOfQuarter;
    }

    public String getScheduleDaysOfYear() {
        return scheduleDaysOfYear;
    }

    public void setScheduleDaysOfYear(String scheduleDaysOfYear) {
        this.scheduleDaysOfYear = scheduleDaysOfYear;
    }

    public Date getLastGeneratedDate() {
        return lastGeneratedDate;
    }

    public void setLastGeneratedDate(Date lastGeneratedDate) {
        this.lastGeneratedDate = lastGeneratedDate;
    }
}
