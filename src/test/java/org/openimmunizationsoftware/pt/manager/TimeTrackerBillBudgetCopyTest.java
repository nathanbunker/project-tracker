package org.openimmunizationsoftware.pt.manager;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.WebUser;

public class TimeTrackerBillBudgetCopyTest {

    @Test
    public void projectWithoutBudgetCreatesEntryWithNullBudget() {
        Project project = new Project();
        project.setProjectId(10);
        project.setBillCode("WORK");

        BillCode billCode = new BillCode();
        billCode.setBillCode("WORK");
        billCode.setBillable("Y");

        BillEntry billEntry = TimeTracker.createBillEntry(project, null, billCode, Integer.valueOf(1), new WebUser(),
                new Date());

        Assert.assertNull(billEntry.getBillBudgetId());
        Assert.assertEquals("WORK", billEntry.getBillCode());
        Assert.assertEquals("Y", billEntry.getBillable());
    }

    @Test
    public void projectWithBudgetCopiesBudgetIdToNewEntry() {
        Project project = new Project();
        project.setProjectId(11);
        project.setBillCode("CONTRACT");
        project.setBillBudgetId(Integer.valueOf(44));

        BillCode billCode = new BillCode();
        billCode.setBillCode("CONTRACT");
        billCode.setBillable("Y");

        BillEntry billEntry = TimeTracker.createBillEntry(project, null, billCode, Integer.valueOf(2), new WebUser(),
                new Date());

        Assert.assertEquals(Integer.valueOf(44), billEntry.getBillBudgetId());
        Assert.assertEquals("CONTRACT", billEntry.getBillCode());
    }

    @Test
    public void historicalEntryRetainsOriginalBudgetWhenProjectChanges() {
        Project project = new Project();
        project.setProjectId(12);
        project.setBillCode("CONTRACT");
        project.setBillBudgetId(Integer.valueOf(100));

        BillCode billCode = new BillCode();
        billCode.setBillCode("CONTRACT");
        billCode.setBillable("Y");

        BillEntry firstEntry = TimeTracker.createBillEntry(project, null, billCode, Integer.valueOf(2), new WebUser(),
                new Date());
        project.setBillBudgetId(Integer.valueOf(200));
        project.setBillCode("GRANT");
        billCode.setBillCode("GRANT");

        Assert.assertEquals(Integer.valueOf(100), firstEntry.getBillBudgetId());
        Assert.assertEquals("CONTRACT", firstEntry.getBillCode());
    }
}