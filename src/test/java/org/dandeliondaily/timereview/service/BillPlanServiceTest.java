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