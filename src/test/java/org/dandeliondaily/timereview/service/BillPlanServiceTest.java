package org.dandeliondaily.timereview.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.BillPlan;
import org.openimmunizationsoftware.pt.model.BillPlanStatus;

public class BillPlanServiceTest {

    private BillPlanService service;

    @Before
    public void setUp() {
        service = new BillPlanService(null, null);
    }

    @Test
    public void selectActiveApprovedPlan_prefersLatestEffectiveDateThenVersion() throws ParseException {
        BillPlan older = plan("2026-01-01", 1, BillPlanStatus.APPROVED.getCode());
        BillPlan sameDateLowerVersion = plan("2026-03-01", 1, BillPlanStatus.APPROVED.getCode());
        BillPlan sameDateHigherVersion = plan("2026-03-01", 2, BillPlanStatus.APPROVED.getCode());
        BillPlan future = plan("2026-04-01", 1, BillPlanStatus.APPROVED.getCode());

        BillPlan selected = service.selectActiveApprovedPlan(
                Arrays.asList(older, sameDateLowerVersion, sameDateHigherVersion, future), parse("2026-03-15"));

        Assert.assertSame(sameDateHigherVersion, selected);
    }

    @Test
    public void selectActiveApprovedPlan_usesSupersededPlanBeforeSuccessorTakesEffect() throws ParseException {
        BillPlan v1 = plan("2026-07-25", 1, BillPlanStatus.SUPERSEDED.getCode());
        v1.setBillPlanId(1);
        BillPlan v2 = plan("2026-10-01", 2, BillPlanStatus.APPROVED.getCode());
        v2.setBillPlanId(2);
        v2.setSupersedesBillPlanId(Integer.valueOf(1));

        Assert.assertSame(v1, service.selectActiveApprovedPlan(Arrays.asList(v1, v2), parse("2026-09-15")));
        Assert.assertSame(v2, service.selectActiveApprovedPlan(Arrays.asList(v1, v2), parse("2026-10-17")));
    }

    @Test
    public void selectActiveApprovedPlan_retroactiveRevisionReplacesSupersededPlan() throws ParseException {
        BillPlan v1 = plan("2026-10-01", 1, BillPlanStatus.SUPERSEDED.getCode());
        v1.setBillPlanId(1);
        BillPlan v2 = plan("2026-08-01", 2, BillPlanStatus.APPROVED.getCode());
        v2.setBillPlanId(2);
        v2.setSupersedesBillPlanId(Integer.valueOf(1));

        Assert.assertSame(v2, service.selectActiveApprovedPlan(Arrays.asList(v1, v2), parse("2026-11-01")));
    }

    @Test
    public void selectActiveApprovedPlan_ignoresDrafts() throws ParseException {
        BillPlan approved = plan("2026-08-01", 1, BillPlanStatus.APPROVED.getCode());
        approved.setBillPlanId(1);
        BillPlan draft = plan("2026-09-01", 2, BillPlanStatus.DRAFT.getCode());
        draft.setBillPlanId(2);
        draft.setSupersedesBillPlanId(Integer.valueOf(1));

        Assert.assertSame(approved,
                service.selectActiveApprovedPlan(Arrays.asList(approved, draft), parse("2026-10-01")));
    }

    private BillPlan plan(String effectiveDate, int versionNum, String status) throws ParseException {
        BillPlan plan = new BillPlan();
        plan.setEffectiveDate(parse(effectiveDate));
        plan.setVersionNum(versionNum);
        plan.setPlanStatus(status);
        return plan;
    }

    private java.util.Date parse(String value) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd").parse(value);
    }
}