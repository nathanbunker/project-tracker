package org.dandeliondaily.timereview.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.dandeliondaily.timereview.model.BudgetUsageSummary;
import org.openimmunizationsoftware.pt.model.BillBudget;

public class BudgetUsageService {

    public BudgetUsageSummary summarizeBudgetUsage(BillBudget billBudget, int usedMinutes) {
        if (billBudget == null) {
            throw new IllegalArgumentException("Bill budget is required.");
        }
        BudgetUsageSummary summary = new BudgetUsageSummary();
        summary.setBudgetId(billBudget.getBillBudgetId());
        summary.setBillCode(billBudget.getBillCode() == null ? null : billBudget.getBillCode().getBillCode());
        summary.setStartDate(billBudget.getStartDate());
        summary.setEndDate(billBudget.getEndDate());
        summary.setAuthorizedMinutes(billBudget.getBillMins());
        summary.setUsedMinutes(usedMinutes);
        int remaining = billBudget.getBillMins() - usedMinutes;
        summary.setRemainingMinutes(remaining);
        summary.setOverBudget(remaining < 0);
        if (billBudget.getBillMins() > 0) {
            BigDecimal percent = new BigDecimal(usedMinutes)
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(billBudget.getBillMins()), 2, RoundingMode.HALF_UP);
            summary.setPercentConsumed(percent);
        } else {
            summary.setPercentConsumed(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        return summary;
    }
}