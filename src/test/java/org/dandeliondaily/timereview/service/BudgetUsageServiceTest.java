package org.dandeliondaily.timereview.service;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.dandeliondaily.timereview.model.BudgetUsageSummary;
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;

public class BudgetUsageServiceTest {

    private BudgetUsageService service;

    @Before
    public void setUp() {
        service = new BudgetUsageService();
    }

    @Test
    public void remainingHoursAreDerivedFromEntries() {
        BudgetUsageSummary summary = service.summarizeBudgetUsage(budget(600), 150);
        Assert.assertEquals(450, summary.getRemainingMinutes());
        Assert.assertEquals(new BigDecimal("25.00"), summary.getPercentConsumed());
        Assert.assertFalse(summary.isOverBudget());
    }

    @Test
    public void overBudgetCanGoNegative() {
        BudgetUsageSummary summary = service.summarizeBudgetUsage(budget(600), 650);
        Assert.assertEquals(-50, summary.getRemainingMinutes());
        Assert.assertTrue(summary.isOverBudget());
    }

    private BillBudget budget(int authorizedMinutes) {
        BillCode billCode = new BillCode();
        billCode.setBillCode("CONTRACT");
        BillBudget budget = new BillBudget();
        budget.setBillBudgetId(10);
        budget.setBillCode(billCode);
        budget.setBillMins(authorizedMinutes);
        return budget;
    }
}