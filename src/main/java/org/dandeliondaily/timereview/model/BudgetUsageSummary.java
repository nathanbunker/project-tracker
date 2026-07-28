package org.dandeliondaily.timereview.model;

import java.math.BigDecimal;
import java.util.Date;

public class BudgetUsageSummary {

    private int budgetId;
    private String billCode;
    private Date startDate;
    private Date endDate;
    private int authorizedMinutes;
    private int usedMinutes;
    private int remainingMinutes;
    private BigDecimal percentConsumed;
    private boolean overBudget;

    public int getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(int budgetId) {
        this.budgetId = budgetId;
    }

    public String getBillCode() {
        return billCode;
    }

    public void setBillCode(String billCode) {
        this.billCode = billCode;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getAuthorizedMinutes() {
        return authorizedMinutes;
    }

    public void setAuthorizedMinutes(int authorizedMinutes) {
        this.authorizedMinutes = authorizedMinutes;
    }

    public int getUsedMinutes() {
        return usedMinutes;
    }

    public void setUsedMinutes(int usedMinutes) {
        this.usedMinutes = usedMinutes;
    }

    public int getRemainingMinutes() {
        return remainingMinutes;
    }

    public void setRemainingMinutes(int remainingMinutes) {
        this.remainingMinutes = remainingMinutes;
    }

    public BigDecimal getPercentConsumed() {
        return percentConsumed;
    }

    public void setPercentConsumed(BigDecimal percentConsumed) {
        this.percentConsumed = percentConsumed;
    }

    public boolean isOverBudget() {
        return overBudget;
    }

    public void setOverBudget(boolean overBudget) {
        this.overBudget = overBudget;
    }
}