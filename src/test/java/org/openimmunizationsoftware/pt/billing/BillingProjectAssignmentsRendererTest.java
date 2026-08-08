package org.openimmunizationsoftware.pt.billing;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.AssignmentCell;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.BillCodeColumn;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.CadenceRow;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.ProjectChip;

public class BillingProjectAssignmentsRendererTest {

    @Test
    public void render_outputsEscapedDraggableProjectAndDropCoordinates() {
        BillingProjectAssignmentsBoard board = board();

        String html = render(board);

        Assert.assertTrue(html.contains("draggable=\"true\""));
        Assert.assertTrue(html.contains("data-project-id=\"17\""));
        Assert.assertTrue(html.contains("data-bill-code=\"CLIENT&amp;ONE\""));
        Assert.assertTrue(html.contains("data-update-every=\"6\""));
        Assert.assertTrue(html.contains("Alpha &lt;Launch&gt;"));
        Assert.assertFalse(html.contains("Alpha <Launch>"));
    }

    @Test
    public void render_postsMoveToAssignmentsServlet() {
        String html = render(board());

        Assert.assertTrue(html.contains("fetch('BillingProjectAssignmentsServlet'"));
        Assert.assertTrue(html.contains("action=moveProject"));
        Assert.assertTrue(html.contains("updateEvery="));
    }

    private BillingProjectAssignmentsBoard board() {
        BillingProjectAssignmentsBoard board = new BillingProjectAssignmentsBoard();
        BillCodeColumn column = new BillCodeColumn();
        column.setBillCode("CLIENT&ONE");
        column.setLabel("Client One");
        board.getColumns().add(column);

        CadenceRow row = new CadenceRow();
        row.setUpdateEveryDays(6);
        row.setLabel("Week");
        AssignmentCell cell = new AssignmentCell();
        cell.setBillCode(column.getBillCode());
        cell.setUpdateEveryDays(row.getUpdateEveryDays());
        ProjectChip chip = new ProjectChip();
        chip.setProjectId(17);
        chip.setProjectName("Alpha <Launch>");
        cell.getProjects().add(chip);
        row.getCells().add(cell);
        board.getRows().add(row);
        return board;
    }

    private String render(BillingProjectAssignmentsBoard board) {
        StringWriter buffer = new StringWriter();
        PrintWriter out = new PrintWriter(buffer);
        new BillingProjectAssignmentsRenderer().render(out, board);
        out.flush();
        return buffer.toString();
    }
}