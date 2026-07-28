package org.dandeliondaily.timereview.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import org.dandeliondaily.timereview.model.AllocationActualMinutes;
import org.dandeliondaily.timereview.model.AllocationCalculationResult;
import org.dandeliondaily.timereview.model.AllocationCalculationTarget;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.BillExpected;
import org.openimmunizationsoftware.pt.model.BillExpectedId;
import org.openimmunizationsoftware.pt.model.BillPlan;
import org.openimmunizationsoftware.pt.model.BillPlanTarget;

public class AllocationCalculationServiceTest {

    private AllocationCalculationService service;

    @Before
    public void setUp() {
        service = new AllocationCalculationService();
    }

    @Test
    public void handlesZeroExpectedMinutesSafely() {
        BillPlan plan = plan();
        BillPlanTarget target = target();
        AllocationCalculationResult result = service.calculate(plan, Arrays.asList(target),
                Arrays.asList(actual("A", 100)), Arrays.asList(expected(0, "N")), new Date());

        AllocationCalculationTarget calculated = result.getTargets().get(0);
        Assert.assertEquals(new BigDecimal("0.00"), calculated.getActualYearToDatePercent());
        Assert.assertEquals(new BigDecimal("0.00"), calculated.getRequiredRemainingPeriodPercent());
    }

    @Test
    public void computesProjectedAndRequiredPercentages() {
        BillPlan plan = plan();
        BillPlanTarget target = target();
        AllocationCalculationResult result = service.calculate(plan, Arrays.asList(target),
                Arrays.asList(actual("A", 200)), Arrays.asList(expected(400, "W"), expected(600, "W")), new Date());

        AllocationCalculationTarget calculated = result.getTargets().get(0);
        Assert.assertEquals(new BigDecimal("40.00"), calculated.getAnnualTargetPercent());
        Assert.assertEquals(new BigDecimal("50.00"), calculated.getSteeringTargetPercent());
        Assert.assertEquals(Integer.valueOf(200), calculated.getActualYearToDateMinutes());
    }

    private BillPlan plan() {
        BillPlan plan = new BillPlan();
        Date today = new Date();
        plan.setFiscalStartDate(today);
        plan.setFiscalEndDate(today);
        return plan;
    }

    private BillPlanTarget target() {
        BillPlanTarget target = new BillPlanTarget();
        target.setBillCode("A");
        target.setAnnualTargetBps(Integer.valueOf(4000));
        target.setSteeringTargetBps(Integer.valueOf(5000));
        return target;
    }

    private AllocationActualMinutes actual(String billCode, int minutes) {
        AllocationActualMinutes actual = new AllocationActualMinutes();
        actual.setBillCode(billCode);
        actual.setTotalMinutes(minutes);
        return actual;
    }

    private BillExpected expected(int minutes, String workStatus) {
        BillExpected expected = new BillExpected();
        expected.setId(new BillExpectedId(1, new Date()));
        expected.setBillMins(minutes);
        expected.setWorkStatus(workStatus);
        return expected;
    }
}