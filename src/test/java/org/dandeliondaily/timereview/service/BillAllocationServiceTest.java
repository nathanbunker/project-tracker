package org.dandeliondaily.timereview.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.timereview.model.AllocationProjectDrillDown;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.Project;

public class BillAllocationServiceTest {

    private BillAllocationService service;

    @Before
    public void setUp() {
        service = new BillAllocationService(null);
    }

    @Test
    public void oneProjectCanAppearUnderTwoHistoricalBillingCodes() throws ParseException {
        Project project = new Project();
        project.setProjectId(7);
        project.setProjectName("Alpha");

        Map<Integer, Project> projectMap = new LinkedHashMap<Integer, Project>();
        projectMap.put(Integer.valueOf(7), project);

        List<AllocationProjectDrillDown> results = service.summarizeProjectDrillDown(Arrays.asList(
                entry(7, "CODE-A", 30, "2026-01-01 09:00"),
                entry(7, "CODE-B", 45, "2026-01-10 09:00"),
                entry(7, "CODE-A", 15, "2026-01-12 09:00")), projectMap);

        Assert.assertEquals(2, results.size());
        Assert.assertEquals("CODE-A", results.get(0).getBillCode());
        Assert.assertEquals(45, results.get(0).getTotalMinutes());
        Assert.assertEquals("CODE-B", results.get(1).getBillCode());
        Assert.assertEquals(45, results.get(1).getTotalMinutes());
    }

    @Test
    public void nullBudgetEntriesRemainLoadable() {
        BillEntry entry = new BillEntry();
        entry.setBillBudgetId(null);
        Assert.assertNull(entry.getBillBudgetId());
    }

    @Test
    public void existingBillableBehaviorIsUnchanged() {
        BillEntry entry = new BillEntry();
        entry.setBillable("N");
        Assert.assertEquals("N", entry.getBillable());
    }

    private BillEntry entry(int projectId, String billCode, int minutes, String when) throws ParseException {
        BillEntry entry = new BillEntry();
        entry.setProjectId(projectId);
        entry.setBillCode(billCode);
        entry.setBillMins(Integer.valueOf(minutes));
        entry.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(when));
        return entry;
    }
}