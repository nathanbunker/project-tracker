package org.dandeliondaily.timereview.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AllocationCalculationResult {

    private Date asOfDate;
    private Date fiscalStartDate;
    private Date fiscalEndDate;
    private int expectedWorkedMinutesYearToDate;
    private int expectedWorkedMinutesRemaining;
    private List<AllocationCalculationTarget> targets = new ArrayList<AllocationCalculationTarget>();

    public Date getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(Date asOfDate) {
        this.asOfDate = asOfDate;
    }

    public Date getFiscalStartDate() {
        return fiscalStartDate;
    }

    public void setFiscalStartDate(Date fiscalStartDate) {
        this.fiscalStartDate = fiscalStartDate;
    }

    public Date getFiscalEndDate() {
        return fiscalEndDate;
    }

    public void setFiscalEndDate(Date fiscalEndDate) {
        this.fiscalEndDate = fiscalEndDate;
    }

    public int getExpectedWorkedMinutesYearToDate() {
        return expectedWorkedMinutesYearToDate;
    }

    public void setExpectedWorkedMinutesYearToDate(int expectedWorkedMinutesYearToDate) {
        this.expectedWorkedMinutesYearToDate = expectedWorkedMinutesYearToDate;
    }

    public int getExpectedWorkedMinutesRemaining() {
        return expectedWorkedMinutesRemaining;
    }

    public void setExpectedWorkedMinutesRemaining(int expectedWorkedMinutesRemaining) {
        this.expectedWorkedMinutesRemaining = expectedWorkedMinutesRemaining;
    }

    public List<AllocationCalculationTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<AllocationCalculationTarget> targets) {
        this.targets = targets;
    }
}