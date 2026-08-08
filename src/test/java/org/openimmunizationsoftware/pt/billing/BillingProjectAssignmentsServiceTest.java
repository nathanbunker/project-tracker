package org.openimmunizationsoftware.pt.billing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.AssignmentCell;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.CadenceRow;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ReviewInterval;

public class BillingProjectAssignmentsServiceTest {

    @Test
    public void createCadenceRows_includesStandardLegacyAndNoneRows() {
        List<CadenceRow> rows = BillingProjectAssignmentsService.createCadenceRows(
                Arrays.asList(Integer.valueOf(13), Integer.valueOf(30), Integer.valueOf(0), Integer.valueOf(30)));

        Assert.assertEquals(ReviewInterval.values().length + 2, rows.size());
        Assert.assertEquals("Week", rows.get(0).getLabel());
        Assert.assertEquals(30, rows.get(ReviewInterval.values().length).getUpdateEveryDays());
        Assert.assertEquals("30 Days (legacy)", rows.get(ReviewInterval.values().length).getLabel());
        Assert.assertTrue(rows.get(ReviewInterval.values().length).isLegacy());
        Assert.assertEquals("None", rows.get(rows.size() - 1).getLabel());
        Assert.assertEquals(0, rows.get(rows.size() - 1).getUpdateEveryDays());
    }

    @Test
    public void createCadenceRows_doesNotDuplicateStandardValues() {
        List<CadenceRow> rows = BillingProjectAssignmentsService.createCadenceRows(
                Arrays.asList(Integer.valueOf(6), Integer.valueOf(13), Integer.valueOf(26)));

        Assert.assertEquals(ReviewInterval.values().length + 1, rows.size());
    }

    @Test
    public void assembleBoard_placesProjectsByBillCodeAndCurrentUsersCadence() {
        BillCode billCode = new BillCode();
        billCode.setBillCode("CLIENT");
        billCode.setBillLabel("Client Work");

        Project weekly = project(10, "Weekly Project", "CLIENT");
        Project noCadence = project(11, "No Cadence", "CLIENT");
        Map<Integer, Integer> updateDueByProject = new HashMap<Integer, Integer>();
        updateDueByProject.put(Integer.valueOf(weekly.getProjectId()), Integer.valueOf(6));

        BillingProjectAssignmentsBoard board = new BillingProjectAssignmentsService().assembleBoard(
                Arrays.asList(billCode), Arrays.asList(weekly, noCadence), updateDueByProject);

        Assert.assertEquals("Client Work", board.getColumns().get(0).getLabel());
        AssignmentCell weeklyCell = board.getRows().get(0).getCells().get(0);
        Assert.assertEquals("Weekly Project", weeklyCell.getProjects().get(0).getProjectName());
        AssignmentCell noneCell = board.getRows().get(board.getRows().size() - 1).getCells().get(0);
        Assert.assertEquals("No Cadence", noneCell.getProjects().get(0).getProjectName());
    }

    private Project project(int projectId, String name, String billCode) {
        Project project = new Project();
        project.setProjectId(projectId);
        project.setProjectName(name);
        project.setBillCode(billCode);
        return project;
    }
}