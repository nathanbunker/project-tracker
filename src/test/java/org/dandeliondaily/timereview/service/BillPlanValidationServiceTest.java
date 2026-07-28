package org.dandeliondaily.timereview.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillPlan;
import org.openimmunizationsoftware.pt.model.BillPlanStatus;
import org.openimmunizationsoftware.pt.model.BillPlanTarget;
import org.openimmunizationsoftware.pt.model.BillPlanTargetMode;

public class BillPlanValidationServiceTest {

    private BillPlanValidationService service;

    @Before
    public void setUp() {
        service = new BillPlanValidationService();
    }

    @Test
    public void draftPlanMayBeIncompleteWithWarning() {
        BillPlan plan = plan();
        BillPlanTarget target = target("A", BillPlanTargetMode.PERCENT.getCode(), 4000, null, null);
        BillCode billCode = billCode("A");

        BillPlanValidationResult result = service.validateTargets(plan, Arrays.asList(target),
                Arrays.asList(billCode), Collections.<BillBudget>emptyList(), false);

        Assert.assertTrue(result.isValid());
        Assert.assertEquals(1, result.getWarnings().size());
    }

    @Test
    public void approvedPlanRequiresExactHundredPercent() {
        BillPlan plan = plan();
        plan.setPlanStatus(BillPlanStatus.APPROVED.getCode());
        BillPlanTarget target = target("A", BillPlanTargetMode.PERCENT.getCode(), 9000, null, null);

        BillPlanValidationResult result = service.validateTargets(plan, Arrays.asList(target),
                Arrays.asList(billCode("A")), Collections.<BillBudget>emptyList(), true);

        Assert.assertFalse(result.isValid());
    }

    @Test
    public void fixedHoursRequiresBudget() {
        BillPlanValidationResult result = service.validateTargets(plan(), Arrays.asList(
                target("A", BillPlanTargetMode.FIXED_HOURS.getCode(), null, null, null)), Arrays.asList(billCode("A")),
                Collections.<BillBudget>emptyList(), false);

        Assert.assertFalse(result.isValid());
    }

    @Test
    public void annualAndSteeringPercentagesCanDiffer() {
        BillPlanValidationResult result = service.validateTargets(plan(), Arrays.asList(
                target("A", BillPlanTargetMode.PERCENT.getCode(), 4000, 5000, null)), Arrays.asList(billCode("A")),
                Collections.<BillBudget>emptyList(), false);

        Assert.assertTrue(result.isValid());
    }

    private BillPlan plan() {
        BillPlan plan = new BillPlan();
        plan.setWorkspaceId(1);
        plan.setWebUserId(2);
        plan.setBillPlanCode("FY26");
        plan.setPlanLabel("FY26 Plan");
        Date today = new Date();
        plan.setFiscalStartDate(today);
        plan.setFiscalEndDate(today);
        plan.setEffectiveDate(today);
        plan.setVersionNum(1);
        plan.setPlanStatus(BillPlanStatus.DRAFT.getCode());
        return plan;
    }

    private BillPlanTarget target(String billCode, String mode, Integer annualBps, Integer steeringBps,
            Integer budgetId) {
        BillPlanTarget target = new BillPlanTarget();
        target.setBillCode(billCode);
        target.setTargetMode(mode);
        target.setAnnualTargetBps(annualBps);
        target.setSteeringTargetBps(steeringBps);
        target.setBillBudgetId(budgetId);
        return target;
    }

    private BillCode billCode(String code) {
        BillCode billCode = new BillCode();
        billCode.setWorkspaceId(Integer.valueOf(1));
        billCode.setBillCode(code);
        return billCode;
    }
}