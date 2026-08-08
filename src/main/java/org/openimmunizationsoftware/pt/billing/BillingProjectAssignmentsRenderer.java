package org.openimmunizationsoftware.pt.billing;

import static org.openimmunizationsoftware.pt.util.WebEscaper.escapeHtml;

import java.io.PrintWriter;

import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.AssignmentCell;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.BillCodeColumn;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.CadenceRow;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.ProjectChip;

public class BillingProjectAssignmentsRenderer {

    public void render(PrintWriter out, BillingProjectAssignmentsBoard board) {
        printStyles(out);
        out.println("<main class=\"bpa-page\">");
        out.println("  <div class=\"bpa-title-row\"><div><h1>Billing Project Assignments</h1>"
                + "<div class=\"bpa-count\">" + countProjects(board) + " active projects</div></div>"
                + "<div id=\"bpaStatus\" class=\"bpa-status\" role=\"status\" aria-live=\"polite\"></div></div>");
        if (board.getColumns().isEmpty()) {
            out.println("  <p>No visible billable categories are configured.</p>");
        } else {
            printBoard(out, board);
        }
        out.println("</main>");
        printScript(out);
    }

    private void printBoard(PrintWriter out, BillingProjectAssignmentsBoard board) {
        out.println("  <div class=\"bpa-board-scroll\">");
        out.println("    <table class=\"bpa-board\">");
        out.println("      <thead><tr><th class=\"bpa-corner\" scope=\"col\">Update Every</th>");
        for (BillCodeColumn column : board.getColumns()) {
            out.println("        <th scope=\"col\"><span class=\"bpa-column-label\">"
                    + escapeHtml(column.getLabel()) + "</span>" + renderCode(column) + "</th>");
        }
        out.println("      </tr></thead>");
        out.println("      <tbody>");
        int rowIndex = 0;
        for (CadenceRow row : board.getRows()) {
            out.println("        <tr>");
            out.println("          <th scope=\"row\" class=\"bpa-row-label\">" + escapeHtml(row.getLabel())
                    + "</th>");
            int columnIndex = 0;
            for (AssignmentCell cell : row.getCells()) {
                String cellId = "bpa-cell-" + rowIndex + "-" + columnIndex;
                out.println("          <td id=\"" + cellId + "\" class=\"bpa-cell\" tabindex=\"0\""
                        + " data-bill-code=\"" + escapeHtml(cell.getBillCode()) + "\""
                        + " data-update-every=\"" + cell.getUpdateEveryDays() + "\""
                        + " aria-label=\"" + escapeHtml(row.getLabel() + ", " + columnLabel(board, columnIndex))
                        + "\">");
                for (ProjectChip project : cell.getProjects()) {
                    printProjectChip(out, project, cell);
                }
                out.println("          </td>");
                columnIndex++;
            }
            out.println("        </tr>");
            rowIndex++;
        }
        out.println("      </tbody>");
        out.println("    </table>");
        out.println("  </div>");
    }

    private void printProjectChip(PrintWriter out, ProjectChip project, AssignmentCell cell) {
        out.println("            <div class=\"bpa-chip\" draggable=\"true\" tabindex=\"0\" role=\"button\""
                + " data-project-id=\"" + project.getProjectId() + "\""
                + " data-bill-code=\"" + escapeHtml(cell.getBillCode()) + "\""
                + " data-update-every=\"" + cell.getUpdateEveryDays() + "\""
                + " aria-label=\"Move " + escapeHtml(project.getProjectName()) + "\">"
                + escapeHtml(project.getProjectName()) + "</div>");
    }

    private String renderCode(BillCodeColumn column) {
        if (safe(column.getBillCode()).equals(safe(column.getLabel()))) {
            return "";
        }
        return "<span class=\"bpa-column-code\">" + escapeHtml(column.getBillCode()) + "</span>";
    }

    private String columnLabel(BillingProjectAssignmentsBoard board, int columnIndex) {
        return columnIndex >= 0 && columnIndex < board.getColumns().size()
                ? board.getColumns().get(columnIndex).getLabel()
                : "";
    }

    private int countProjects(BillingProjectAssignmentsBoard board) {
        int count = 0;
        for (CadenceRow row : board.getRows()) {
            for (AssignmentCell cell : row.getCells()) {
                count += cell.getProjects().size();
            }
        }
        return count;
    }

    private void printStyles(PrintWriter out) {
        out.println("<style>");
        out.println(
                ".bpa-page{max-width:100%;margin:0 0 32px;color:#20262d}.bpa-title-row{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin:0 0 18px}.bpa-title-row h1{margin:0;font-family:Georgia,'Times New Roman',serif;font-size:28px;letter-spacing:0}.bpa-count{margin-top:4px;color:#66717c;font-size:13px}.bpa-status{min-height:20px;color:#24623b;font-weight:bold;text-align:right}.bpa-status.bpa-error{color:#a32929}.bpa-board-scroll{max-width:100%;overflow:auto;border:1px solid #9ca6ad;background:#f4f6f7}.bpa-board{width:max-content;min-width:100%;border-collapse:separate;border-spacing:0;table-layout:fixed}.bpa-board th,.bpa-board td{border-right:1px solid #c5ccd1;border-bottom:1px solid #c5ccd1}.bpa-board thead th{position:sticky;top:0;z-index:3;width:210px;min-width:210px;padding:11px 10px;background:#243746;color:#fff;text-align:left;vertical-align:bottom}.bpa-board .bpa-corner{left:0;z-index:5;width:130px;min-width:130px}.bpa-column-label,.bpa-column-code{display:block;overflow-wrap:anywhere}.bpa-column-label{font-size:14px}.bpa-column-code{margin-top:3px;color:#cbd7de;font-size:11px;font-weight:normal}.bpa-row-label{position:sticky;left:0;z-index:2;width:130px;min-width:130px;padding:10px;background:#e1e6e9;text-align:left;vertical-align:top}.bpa-cell{width:210px;min-width:210px;height:72px;padding:7px;vertical-align:top;background:#fff;outline:none}.bpa-cell:focus,.bpa-cell.bpa-drop-target{box-shadow:inset 0 0 0 3px #d59a24;background:#fff9e8}.bpa-chip{box-sizing:border-box;width:100%;margin:0 0 6px;padding:8px 9px;border:1px solid #87949d;border-left:4px solid #2d748c;border-radius:4px;background:#edf5f7;color:#17252c;cursor:grab;font-size:13px;font-weight:bold;line-height:1.25;overflow-wrap:anywhere}.bpa-chip:focus{outline:2px solid #d59a24;outline-offset:1px}.bpa-chip.bpa-selected{border-color:#b1780a;background:#fff3cf}.bpa-chip.bpa-pending{cursor:wait;opacity:.55}.bpa-chip:last-child{margin-bottom:0}@media(max-width:700px){.bpa-title-row{align-items:flex-start;flex-direction:column}.bpa-status{text-align:left}.bpa-board thead th,.bpa-cell{width:175px;min-width:175px}.bpa-board .bpa-corner,.bpa-row-label{width:105px;min-width:105px}} ");
        out.println("</style>");
    }

    private void printScript(PrintWriter out) {
        out.println("<script>");
        out.println("(function(){");
        out.println("  var selectedChip = null;");
        out.println("  var statusEl = document.getElementById('bpaStatus');");
        out.println(
                "  function setStatus(message, error){ if(!statusEl){return;} statusEl.textContent=message||''; statusEl.className='bpa-status'+(error?' bpa-error':''); }");
        out.println(
                "  function selectChip(chip){ if(selectedChip){selectedChip.classList.remove('bpa-selected');} selectedChip=chip; if(chip){chip.classList.add('bpa-selected');chip.focus();} }");
        out.println(
                "  function insertSorted(cell, chip){ var chips=cell.querySelectorAll('.bpa-chip'); var name=(chip.textContent||'').toLowerCase(); for(var i=0;i<chips.length;i++){if(chips[i]!==chip&&(chips[i].textContent||'').toLowerCase()>name){cell.insertBefore(chip,chips[i]);return;}} cell.appendChild(chip); }");
        out.println("  function moveChip(chip, cell){");
        out.println("    if(!chip||!cell||chip.classList.contains('bpa-pending')){return;}");
        out.println("    var billCode=cell.getAttribute('data-bill-code')||'';");
        out.println("    var updateEvery=cell.getAttribute('data-update-every')||'0';");
        out.println(
                "    if(chip.getAttribute('data-bill-code')===billCode&&chip.getAttribute('data-update-every')===updateEvery){selectChip(null);return;}");
        out.println("    chip.classList.add('bpa-pending'); setStatus('Saving...',false);");
        out.println(
                "    var body='action=moveProject&projectId='+encodeURIComponent(chip.getAttribute('data-project-id')||'')+'&billCode='+encodeURIComponent(billCode)+'&updateEvery='+encodeURIComponent(updateEvery);");
        out.println(
                "    fetch('BillingProjectAssignmentsServlet',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},body:body})");
        out.println("      .then(function(response){return response.json();})");
        out.println(
                "      .then(function(response){if(!response||!response.success){throw new Error(response&&response.message?response.message:'Unable to move project.');} chip.setAttribute('data-bill-code',billCode);chip.setAttribute('data-update-every',updateEvery);insertSorted(cell,chip);selectChip(null);setStatus(response.message||'Project moved.',false);})");
        out.println(
                "      .catch(function(error){setStatus(error&&error.message?error.message:'Unable to move project.',true);})");
        out.println("      .then(function(){chip.classList.remove('bpa-pending');});");
        out.println("  }");
        out.println(
                "  document.addEventListener('dragstart',function(event){var chip=event.target&&event.target.closest?event.target.closest('.bpa-chip'):null;if(!chip){return;}selectedChip=chip;if(event.dataTransfer){event.dataTransfer.effectAllowed='move';event.dataTransfer.setData('text/plain',chip.getAttribute('data-project-id')||'');}});");
        out.println(
                "  document.addEventListener('dragover',function(event){var cell=event.target&&event.target.closest?event.target.closest('.bpa-cell'):null;if(!cell){return;}event.preventDefault();cell.classList.add('bpa-drop-target');});");
        out.println(
                "  document.addEventListener('dragleave',function(event){var cell=event.target&&event.target.closest?event.target.closest('.bpa-cell'):null;if(cell&&!cell.contains(event.relatedTarget)){cell.classList.remove('bpa-drop-target');}});");
        out.println(
                "  document.addEventListener('drop',function(event){var cell=event.target&&event.target.closest?event.target.closest('.bpa-cell'):null;if(!cell){return;}event.preventDefault();cell.classList.remove('bpa-drop-target');moveChip(selectedChip,cell);});");
        out.println(
                "  document.addEventListener('dragend',function(){document.querySelectorAll('.bpa-drop-target').forEach(function(cell){cell.classList.remove('bpa-drop-target');});selectedChip=null;});");
        out.println(
                "  document.addEventListener('keydown',function(event){var chip=event.target&&event.target.closest?event.target.closest('.bpa-chip'):null;if(chip&&(event.key==='Enter'||event.key===' ')){event.preventDefault();selectChip(selectedChip===chip?null:chip);return;}var cell=event.target&&event.target.closest?event.target.closest('.bpa-cell'):null;if(cell&&selectedChip&&event.key==='Enter'){event.preventDefault();moveChip(selectedChip,cell);}if(event.key==='Escape'){selectChip(null);}});");
        out.println("})();");
        out.println("</script>");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}