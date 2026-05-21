package org.dandeliondaily.planahead.render;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.dandeliondaily.dashboard.render.TimeGaugeRenderer;
import org.dandeliondaily.shared.render.EditActionModalRenderer;
import org.dandeliondaily.planahead.model.PlanAheadBoardModel;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;

public class PlanAheadPageRenderer {

        private final TimeGaugeRenderer timeGaugeRenderer = new TimeGaugeRenderer();
        private final EditActionModalRenderer editActionModalRenderer = new EditActionModalRenderer();

        public void render(AppReq appReq, PlanAheadBoardModel boardModel) {
                PrintWriter out = appReq.getOut();
                boolean personalMode = boardModel.isPersonalMode();
                int dayCount = boardModel.getDayHeaders() == null ? 0 : boardModel.getDayHeaders().size();
                String todayKey = appReq.getWebUser().getDateFormatService().formatPattern(
                                appReq.getWebUser().getToday(), "yyyy-MM-dd", appReq.getWebUser().getTimeZone());
                printStyles(out, dayCount);
                out.println("<div class=\"pa-page\">");
                out.println("  <div class=\"pa-intro-bar\">");
                out.println("    <div class=\"pa-intro\">");
                out.println("      <h1>Plan Ahead</h1>");
                out.println("      <p>"
                                + (personalMode
                                                ? "Plan the next seven days for personal flow and weekly visibility."
                                                : "Plan the next five days and rebalance work before the day starts.")
                                + "</p>");
                out.println("    </div>");
                printQuickCapture(out, boardModel);
                out.println("  </div>");

                int viewAllocatedMins = 0;
                if (!personalMode) {
                        for (PlanAheadBoardModel.DayHeaderModel dayHeader : boardModel.getDayHeaders()) {
                                viewAllocatedMins += dayHeader.getBillMins();
                        }
                }
                out.println("  <div class=\"pa-controls\">");
                out.println("    <a class=\"pa-shift\" href=\"PlanAheadServlet?windowStart="
                                + escapeHtml(todayKey)
                                + "\">Today</a>");
                out.println("    <a class=\"pa-shift\" href=\"PlanAheadServlet?action=shiftWindowForward&days=1&windowStart="
                                + escapeHtml(boardModel.getWindowStartKey()) + "\">Next Day &#9654;</a>");
                out.println("    <a class=\"pa-shift\" href=\"TemplateManagementServlet\">Manage Templates</a>");
                out.println("  </div>");

                out.println("  <div class=\"pa-grid\">");
                out.println(renderModeToggleCell(boardModel));
                for (PlanAheadBoardModel.DayHeaderModel dayHeader : boardModel.getDayHeaders()) {
                        out.println(renderDayHeader(dayHeader));
                }

                if (!personalMode) {
                        out.println("    <div class=\"pa-cell pa-cell-label pa-status pa-status-total-cell\">");
                        out.println("      <div class=\"pa-status-main\">");
                        out.println("        <span class=\"pa-status-total-label\">Working</span>");
                        out.println("        <span id=\"pa-view-total\" class=\"pa-card-est-box pa-status-total-box\""
                                        + " title=\"Total Time in View\">"
                                        + escapeHtml(TimeTracker.formatTime(viewAllocatedMins))
                                        + "</span>");
                        out.println("      </div>");
                        out.println("    </div>");
                        for (PlanAheadBoardModel.DayHeaderModel dayHeader : boardModel.getDayHeaders()) {
                                out.println(renderDayStatusCell(dayHeader));
                        }
                }

                if (boardModel.getOverdueRow().isHasItems()) {
                        int overdueTotalMins = 0;
                        for (PlanAheadBoardModel.CardModel c : boardModel.getOverdueRow().getCards()) {
                                overdueTotalMins += c.getEstimateMins();
                        }
                        out.println("    <div class=\"pa-cell pa-cell-label pa-row-label pa-row-overdue-label\""
                                        + " id=\"pa-row-label-overdue\" data-row-key=\"overdue\">"
                                        + "<span class=\"pa-row-label-name\">Overdue</span>"
                                        + "<span class=\"pa-row-total pa-card-est-box\">"
                                        + escapeHtml(TimeTracker.formatTime(overdueTotalMins))
                                        + "</span>"
                                        + "</div>");
                        out.println(renderOverdueTodayCell(boardModel.getOverdueRow(), boardModel.isWorkMode()));
                        for (int i = 1; i < boardModel.getDayHeaders().size(); i++) {
                                out.println("    <div class=\"pa-cell pa-overdue-disabled\"></div>");
                        }
                }

                for (PlanAheadBoardModel.RowModel row : boardModel.getRows()) {
                        int rowTotalMins = 0;
                        for (PlanAheadBoardModel.CellModel rowCell : row.getCells()) {
                                for (PlanAheadBoardModel.CardModel rowCard : rowCell.getCards()) {
                                        rowTotalMins += rowCard.getEstimateMins();
                                }
                        }
                        out.println("    <div class=\"pa-cell pa-cell-label pa-row-label\""
                                        + " id=\"pa-row-label-" + escapeHtml(row.getRowKey()) + "\""
                                        + " data-row-key=\"" + escapeHtml(row.getRowKey()) + "\">"
                                        + "<span class=\"pa-row-label-name\">" + escapeHtml(row.getRowLabel())
                                        + "</span>"
                                        + "<span class=\"pa-row-total pa-card-est-box\">"
                                        + escapeHtml(TimeTracker.formatTime(rowTotalMins)) + "</span>"
                                        + "</div>");
                        for (PlanAheadBoardModel.CellModel cell : row.getCells()) {
                                out.println(renderKanbanCell(cell, boardModel.isWorkMode()));
                        }
                }

                out.println("  </div>");
                printEditModal(out, appReq);
                printStatusModal(out);
                printQuickCaptureScript(out, boardModel);
                printDragDropScript(out, boardModel.getWindowStartKey(), boardModel.getMode());
                out.println("</div>");
        }

        private String renderModeToggleCell(PlanAheadBoardModel boardModel) {
                StringBuilder s = new StringBuilder();
                s.append("    <div class=\"pa-cell pa-cell-label pa-header pa-mode-toggle-cell\">");
                s.append("      <div class=\"pa-mode-toggle\">");
                s.append("        <a class=\"pa-mode-option ");
                if (boardModel.isWorkMode()) {
                        s.append("is-active");
                }
                s.append("\" href=\"PlanAheadServlet?action=setMode&mode=WORK&windowStart=")
                                .append(escapeHtml(boardModel.getWindowStartKey())).append("\">Work</a>");
                s.append("        <a class=\"pa-mode-option ");
                if (boardModel.isPersonalMode()) {
                        s.append("is-active");
                }
                s.append("\" href=\"PlanAheadServlet?action=setMode&mode=PERSONAL&windowStart=")
                                .append(escapeHtml(boardModel.getWindowStartKey())).append("\">Personal</a>");
                s.append("      </div>");
                s.append("    </div>");
                return s.toString();
        }

        private void printQuickCapture(PrintWriter out, PlanAheadBoardModel boardModel) {
                out.println("    <div class=\"pa-quick-capture\">");
                out.println("      <div class=\"pa-quick-capture-title\">Quick Capture</div>");
                out.println("      <form class=\"pa-quick-capture-form\" method=\"POST\" action=\"PlanAheadServlet\">");
                out.println("        <input type=\"hidden\" name=\"windowStart\" value=\""
                                + escapeHtml(boardModel.getWindowStartKey()) + "\" />");
                out.println("        <div class=\"pa-quick-capture-row\">");
                out.println("          <div class=\"pa-quick-capture-input-wrap\">");
                out.println("            <input type=\"text\" id=\"paSentenceInput\" name=\"sentenceInput\" value=\""
                                + escapeHtml(boardModel.getQuickCaptureSentenceValue())
                                + "\" placeholder=\"Project Name: I will action details\" autocomplete=\"off\""
                                + (boardModel.isQuickCaptureFocusRequested() ? " autofocus=\"autofocus\"" : "")
                                + " />");
                out.println("            <div id=\"paSuggestions\" class=\"pa-capture-suggestions\"></div>");
                out.println("          </div>");
                out.println("          <div class=\"pa-quick-capture-actions\">");
                out.println("            <input type=\"submit\" name=\"action\" value=\"Schedule\" />");
                out.println("            <button type=\"submit\" name=\"action\" value=\"Schedule and Start\" class=\"pa-qc-start-btn\">Start</button>");
                out.println("          </div>");
                out.println("        </div>");
                out.println("      </form>");
                out.println("    </div>");
        }

        private void printQuickCaptureScript(PrintWriter out, PlanAheadBoardModel boardModel) {
                out.println("<script>");
                out.print("const paProjectNames = [");
                for (int i = 0; i < boardModel.getQuickCaptureProjectNames().size(); i++) {
                        out.print("\"" + escapeJs(boardModel.getQuickCaptureProjectNames().get(i)) + "\"");
                        if (i < boardModel.getQuickCaptureProjectNames().size() - 1) {
                                out.print(", ");
                        }
                }
                out.println("];\nconst paActionVerbs = [\"I will\", \"I have committed\", \"I might\", \"I would like to\", \"I will meet\", \"I have set goal to\", \"I am waiting\"];");
                out.println("(function(){");
                out.println("  var input = document.getElementById('paSentenceInput');");
                out.println("  var suggestionsBox = document.getElementById('paSuggestions');");
                out.println("  if (!input || !suggestionsBox) { return; }");
                out.println("  var selectedIndex = -1;");
                out.println("  var currentSuggestions = [];");
                out.println("  input.addEventListener('input', function(){");
                out.println("    var text = input.value || '';\n    var colonIndex = text.indexOf(':');\n    var suggestions = [];");
                out.println("    if (colonIndex === -1) {");
                out.println("      suggestions = paProjectNames.filter(function(name){ return name.toLowerCase().indexOf(text.toLowerCase()) === 0; });");
                out.println("    } else {");
                out.println("      var beforeColon = text.substring(0, colonIndex).trim();\n      var afterColon = text.substring(colonIndex + 1).trim();");
                out.println("      if (paProjectNames.indexOf(beforeColon) === -1) {");
                out.println("        suggestions = paProjectNames.filter(function(name){ return name.toLowerCase().indexOf(beforeColon.toLowerCase()) >= 0; });");
                out.println("      } else if (afterColon.length === 0) {");
                out.println("        suggestions = paActionVerbs;");
                out.println("      } else {");
                out.println("        suggestions = paActionVerbs.filter(function(verb){ return verb.toLowerCase().indexOf(afterColon.toLowerCase()) === 0; });");
                out.println("      }");
                out.println("    }");
                out.println("    currentSuggestions = suggestions;\n    selectedIndex = -1;\n    paShowSuggestions(suggestions, text);");
                out.println("  });");
                out.println("  input.addEventListener('keydown', function(e){");
                out.println("    if (!currentSuggestions.length || suggestionsBox.style.display !== 'block') { return; }");
                out.println("    if (e.key === 'ArrowDown') { e.preventDefault(); selectedIndex = (selectedIndex + 1) % currentSuggestions.length; paShowSuggestions(currentSuggestions, input.value || ''); }");
                out.println("    if (e.key === 'ArrowUp') { e.preventDefault(); selectedIndex = (selectedIndex <= 0 ? currentSuggestions.length - 1 : selectedIndex - 1); paShowSuggestions(currentSuggestions, input.value || ''); }");
                out.println("    if (e.key === 'Enter' && selectedIndex >= 0) { e.preventDefault(); paAcceptSuggestion(currentSuggestions[selectedIndex], input.value || ''); }");
                out.println("    if (e.key === 'Tab' && currentSuggestions.length > 0) { e.preventDefault(); if (selectedIndex < 0) { selectedIndex = 0; } paAcceptSuggestion(currentSuggestions[selectedIndex], input.value || ''); }");
                out.println("    if (e.key === 'Escape') { suggestionsBox.style.display = 'none'; }");
                out.println("  });");
                out.println("  window.setTimeout(function(){ if (document.activeElement !== input && input.hasAttribute('autofocus')) { input.focus(); } }, 0);");
                out.println("  document.addEventListener('click', function(e){ if (!suggestionsBox.contains(e.target) && e.target !== input) { suggestionsBox.style.display = 'none'; } });");
                out.println("  function paShowSuggestions(suggestions, text){");
                out.println("    suggestionsBox.innerHTML = '';\n    suggestionsBox.style.display = suggestions.length ? 'block' : 'none';");
                out.println("    suggestions.forEach(function(suggestion, i){");
                out.println("      var div = document.createElement('div');\n      div.textContent = suggestion;\n      if (i === selectedIndex) { div.style.backgroundColor = '#e7efe0'; }");
                out.println("      div.addEventListener('mousedown', function(e){ e.preventDefault(); paAcceptSuggestion(suggestion, text); });");
                out.println("      suggestionsBox.appendChild(div);");
                out.println("    });");
                out.println("  }");
                out.println("  function paAcceptSuggestion(suggestion, text){");
                out.println("    if (text.indexOf(':') === -1) {");
                out.println("      input.value = suggestion + ': ';");
                out.println("    } else {");
                out.println("      var beforeColon = text.substring(0, text.indexOf(':')).trim();");
                out.println("      if (paProjectNames.indexOf(beforeColon) === -1) {");
                out.println("        input.value = suggestion + ': ';");
                out.println("      } else {");
                out.println("        input.value = beforeColon + ': ' + suggestion + ' ';");
                out.println("      }");
                out.println("    }");
                out.println("    suggestionsBox.style.display = 'none';\n    input.focus();");
                out.println("  }");
                out.println("})();");
                out.println("</script>");
        }

        private void printStatusModal(PrintWriter out) {
                out.println("<div id=\"paStatusModal\" class=\"pa-modal-overlay pa-status-modal-overlay\" style=\"display:none;\">");
                out.println("  <div class=\"pa-modal pa-status-modal\">");
                out.println("    <div class=\"pa-modal-head\">");
                out.println("      <h3>Select Work Status</h3>");
                out.println("      <button type=\"button\" class=\"pa-modal-close\" onclick=\"paCloseStatusModal()\">&times;</button>");
                out.println("    </div>");
                out.println("    <div class=\"pa-status-modal-body\">");
                out.println("      <button type=\"button\" class=\"pa-status-option\" data-status-code=\"W\">Working</button>");
                out.println("      <button type=\"button\" class=\"pa-status-option\" data-status-code=\"N\">Not Working</button>");
                out.println("      <button type=\"button\" class=\"pa-status-option\" data-status-code=\"V\">Vacation</button>");
                out.println("      <button type=\"button\" class=\"pa-status-option\" data-status-code=\"H\">Holiday</button>");
                out.println("      <button type=\"button\" class=\"pa-status-option\" data-status-code=\"T\">Traveling</button>");
                out.println("      <button type=\"button\" class=\"pa-status-option\" data-status-code=\"S\">Sick</button>");
                out.println("    </div>");
                out.println("  </div>");
                out.println("</div>");
        }

        private void printEditModal(PrintWriter out, AppReq appReq) {
                EditActionModalRenderer.Config editConfig = new EditActionModalRenderer.Config();
                editConfig.overlayId = "paEditModal";
                editConfig.overlayClass = "pa-modal-overlay pa-edit-modal-overlay";
                editConfig.overlayOnClick = "paCloseModalOnOverlay(event, this)";
                editConfig.modalClass = "pa-modal";
                editConfig.closeOnClick = "paCloseEditModal()";
                editConfig.saveOnClick = "paSaveEditModal()";
                editConfig.saveAndStartOnClick = "paSaveEditModal()";
                editConfig.cancelOnClick = "paCloseEditModal()";
                editConfig.deleteOnClick = "paDeleteCard()";
                editActionModalRenderer.render(out, appReq, editConfig);
        }

        /** Returns the static (non-DB) portion of the edit modal HTML for testing. */
        String buildEditModalBodyHtml() {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println("<div id=\"paEditModal\" class=\"pa-modal-overlay\" onclick=\"paCloseModalOnOverlay(event, this)\">");
                pw.println("  <div class=\"pa-modal\" onclick=\"event.stopPropagation()\">");
                pw.println("    <div class=\"pa-modal-head\">");
                pw.println("      <h3 class=\"pa-modal-title\">Edit Action</h3>");
                pw.println("      <button type=\"button\" class=\"pa-modal-close\" onclick=\"paCloseEditModal()\">&times;</button>");
                pw.println("    </div>");
                pw.println("    <div class=\"pa-modal-body\">");
                pw.println("      <input type=\"hidden\" id=\"paEditActionId\" value=\"\" />");
                pw.println("      <input type=\"hidden\" id=\"paEditActionDateOriginal\" value=\"\" />");
                pw.println("      <label>Date <input type=\"date\" id=\"paEditNextActionDate\" /></label>");
                pw.println("      <div id=\"paEditTypeWrap\">");
                pw.println("        <div class=\"pa-action-type-chips\">");
                pw.println("          <button type=\"button\" class=\"pa-chip pa-action-type-btn\" data-action-type=\"WILL\" onclick=\"paSetEditActionType('WILL')\">Will</button>");
                pw.println("          <button type=\"button\" class=\"pa-chip pa-action-type-btn\" data-action-type=\"MIGHT\" onclick=\"paSetEditActionType('MIGHT')\">Might</button>");
                pw.println("          <button type=\"button\" class=\"pa-chip pa-action-type-btn\" data-action-type=\"WOULD_LIKE_TO\" onclick=\"paSetEditActionType('WOULD_LIKE_TO')\">Would Like To</button>");
                pw.println("          <button type=\"button\" class=\"pa-chip pa-action-type-btn\" data-action-type=\"WILL_CONTACT\" onclick=\"paSetEditActionType('WILL_CONTACT')\">Will Contact</button>");
                pw.println("          <button type=\"button\" class=\"pa-chip pa-action-type-btn\" data-action-type=\"WILL_MEET\" onclick=\"paSetEditActionType('WILL_MEET')\">Will Meet</button>");
                pw.println("          <button type=\"button\" class=\"pa-chip pa-action-type-btn\" data-action-type=\"REVIEW\" onclick=\"paSetEditActionType('REVIEW')\">Review</button>");
                pw.println("          <button type=\"button\" class=\"pa-chip pa-action-type-btn\" data-action-type=\"DOCUMENT\" onclick=\"paSetEditActionType('DOCUMENT')\">Document</button>");
                pw.println("          <button type=\"button\" class=\"pa-chip pa-action-type-btn\" data-action-type=\"WILL_FOLLOW_UP\" onclick=\"paSetEditActionType('WILL_FOLLOW_UP')\">Will Follow Up</button>");
                pw.println("          <button type=\"button\" class=\"pa-chip pa-action-type-btn\" data-action-type=\"COMMITTED_TO\" onclick=\"paSetEditActionType('COMMITTED_TO')\">Committed To</button>");
                pw.println("          <button type=\"button\" class=\"pa-chip pa-action-type-btn\" data-action-type=\"GOAL\" onclick=\"paSetEditActionType('GOAL')\">Goal</button>");
                pw.println("          <button type=\"button\" class=\"pa-chip pa-action-type-btn\" data-action-type=\"WAITING\" onclick=\"paSetEditActionType('WAITING')\">Waiting</button>");
                pw.println("        </div>");
                pw.println("        <input type=\"hidden\" id=\"paEditNextActionType\" value=\"\" />");
                pw.println("      </div>");
                pw.println("      <div id=\"paEditSlotWrap\" style=\"display:none;\">");
                pw.println("        <label>Time Slot <select id=\"paEditTimeSlot\"></select></label>");
                pw.println("      </div>");
                pw.println("      <div id=\"paEditContactWrap\">");
                pw.println("        <label>Contact <select id=\"paEditNextContactId\"><option value=\"\">(none)</option></select></label>");
                pw.println("      </div>");
                pw.println("      <div id=\"paEditEstimateWrap\">");
                pw.println("        <label>Estimate (min) <input type=\"number\" id=\"paEditNextTimeEstimate\" min=\"0\" /></label>");
                pw.println("      </div>");
                pw.println("      <label>Link URL <input type=\"text\" id=\"paEditLinkUrl\" /></label>");
                pw.println("      <div id=\"paEditAdvancedSection\" class=\"pa-advanced-section\">");
                pw.println("        <label>Target Date <input type=\"date\" id=\"paEditNextTargetDate\" /></label>");
                pw.println("        <label>Deadline <input type=\"date\" id=\"paEditNextDeadlineDate\" /></label>");
                pw.println("        <label>Notes <textarea id=\"paEditNextNote\"></textarea></label>");
                pw.println("      </div>");
                pw.println("    </div>");
                pw.println("    <div class=\"pa-modal-foot\">");
                pw.println("      <button type=\"button\" onclick=\"paSaveEditModal()\">Save</button>");
                pw.println("      <button type=\"button\" onclick=\"paCloseEditModal()\">Cancel</button>");
                pw.println("    </div>");
                pw.println("  </div>");
                pw.println("</div>");
                pw.flush();
                return sw.toString();
        }

        private void printDragDropScript(PrintWriter out, String windowStartKey, String mode) {
                out.println("<script>");
                out.println("(function(){");
                out.println("  var paDraggedActionId = null;");
                out.println("  var paWindowStart = '" + escapeHtml(windowStartKey) + "';");
                out.println("  var paMode = '" + escapeHtml(mode) + "';");
                out.println("  var paIsPersonal = paMode === 'PERSONAL';");
                out.println("  var paReloadAfterEdit = false;");
                out.println("  function paSetEditModeControls(){");
                out.println("    var typeWrap = document.getElementById('eaEditActionTypeSection');");
                out.println("    var slotWrap = document.getElementById('eaEditActionSlotSection');");
                out.println("    var estimateWrap = document.getElementById('eaEditActionTimeSection');");
                out.println("    var typeField = document.getElementById('eaEditActionType');");
                out.println("    var slotField = document.getElementById('eaEditActionTimeSlot');");
                out.println("    var estimateField = document.getElementById('eaEditActionTime');");
                out.println("    if (typeWrap) { typeWrap.style.display = paIsPersonal ? 'none' : ''; }");
                out.println("    if (slotWrap) { slotWrap.style.display = paIsPersonal ? '' : 'none'; }");
                out.println("    if (estimateWrap) { estimateWrap.style.display = paIsPersonal ? 'none' : ''; }");
                out.println("    if (typeField) { typeField.disabled = paIsPersonal; }");
                out.println("    if (slotField) { slotField.disabled = !paIsPersonal; }");
                out.println("    if (estimateField) { estimateField.disabled = paIsPersonal; }");
                out.println("  }");
                out.println("  function paUpdateActionTypeChips(selectedType) {");
                out.println("    var buttons = document.querySelectorAll('.ea-action-type-btn');");
                out.println("    for (var i = 0; i < buttons.length; i++) {");
                out.println("      var btn = buttons[i];");
                out.println("      if (btn.getAttribute('data-action-type') === selectedType) { btn.classList.add('ea-chip-active'); }");
                out.println("      else { btn.classList.remove('ea-chip-active'); }");
                out.println("    }");
                out.println("  }");
                out.println("  function paSetActionTypeChip(type) {");
                out.println("    var hidden = document.getElementById('eaEditActionType');");
                out.println("    if (hidden) { hidden.value = type; }");
                out.println("    paUpdateActionTypeChips(type);");
                out.println("  }");
                out.println("  function eaSetActionType(type) { paSetActionTypeChip(type); }");
                out.println("  document.addEventListener('dragstart', function(e){");
                out.println("    var card = e.target && e.target.closest ? e.target.closest('.pa-card') : null;");
                out.println("    if (!card) { return; }");
                out.println("    if (card.getAttribute('data-reschedule-locked') === 'true') { e.preventDefault(); return; }");
                out.println("    paDraggedActionId = card.getAttribute('data-action-id');");
                out.println("    if (e.dataTransfer) { e.dataTransfer.setData('text/plain', paDraggedActionId || ''); }");
                out.println("  });");
                out.println("  document.addEventListener('dragover', function(e){");
                out.println("    var cell = e.target && e.target.closest ? e.target.closest('.pa-kanban') : null;");
                out.println("    if (!cell) { return; }");
                out.println("    e.preventDefault();");
                out.println("  });");
                out.println("  document.addEventListener('drop', function(e){");
                out.println("    var cell = e.target && e.target.closest ? e.target.closest('.pa-kanban') : null;");
                out.println("    if (!cell) { return; }");
                out.println("    e.preventDefault();");
                out.println(
                                "    var actionId = paDraggedActionId || (e.dataTransfer ? e.dataTransfer.getData('text/plain') : '');");
                out.println("    if (!actionId) { return; }");
                out.println("    var targetDate = cell.getAttribute('data-day');");
                out.println("    var targetRow = cell.getAttribute('data-row');");
                out.println("    var body = 'action=moveCard' +");
                out.println("      '&actionNextId=' + encodeURIComponent(actionId) +");
                out.println("      '&targetDate=' + encodeURIComponent(targetDate || '') +");
                out.println("      '&targetRow=' + encodeURIComponent(targetRow || '') +");
                out.println("      '&windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("    fetch('PlanAheadServlet', {");
                out.println("      method: 'POST',");
                out.println("      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },");
                out.println("      body: body");
                out.println("    })");
                out.println("    .then(function(r){ return r.json(); })");
                out.println("    .then(function(data){");
                out.println("      if (!data || !data.success) {");
                out.println("        console.log('Move failed', data ? data.message : 'Unknown error');");
                out.println("        return;");
                out.println("      }");
                out.println("      paApplyMutationPayload(data);");
                out.println("    })");
                out.println("    .catch(function(err){ console.log('Move request failed', err); });");
                out.println("  });");

                out.println("  window.paOpenEditModal = function(actionNextId, evt, reloadAfterSave){");
                out.println("    if (evt) { evt.preventDefault(); evt.stopPropagation(); }");
                out.println("    paReloadAfterEdit = !!reloadAfterSave;");
                out.println("    var body = 'action=loadCardEdit&actionNextId=' + encodeURIComponent(actionNextId) +");
                out.println("      '&windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("    fetch('PlanAheadServlet', {");
                out.println("      method: 'POST',");
                out.println("      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },");
                out.println("      body: body");
                out.println("    })");
                out.println("    .then(function(r){ return r.json(); })");
                out.println("    .then(function(resp){");
                out.println(
                                "      if (!resp || !resp.success) { console.log('Load edit failed', resp ? resp.message : 'Unknown'); return; }");
                out.println("      var data = resp.data || {};");
                out.println("      document.getElementById('eaEditActionId').value = data.actionNextId || ''; ");
                out.println("      document.getElementById('eaEditActionDate').value = data.nextActionDate || ''; ");
                out.println("      paSetActionTypeChip(data.nextActionType || 'WILL'); ");
                out.println("      document.getElementById('eaEditActionTimeSlot').value = data.timeSlot || 'AFTERNOON'; ");
                out.println("      document.getElementById('eaEditActionDescription').value = data.nextDescription || ''; ");
                out.println("      document.getElementById('eaEditActionTime').value = data.nextTimeEstimate || 0; ");
                out.println("      document.getElementById('eaEditActionTarget').value = data.nextTargetDate || ''; ");
                out.println("      document.getElementById('eaEditActionDeadline').value = data.nextDeadlineDate || ''; ");
                out.println("      document.getElementById('eaEditActionLink').value = data.linkUrl || ''; ");
                out.println("      document.getElementById('eaEditActionContact').value = data.nextContactId || ''; ");
                out.println("      document.getElementById('eaEditActionNote').value = data.nextNote || ''; ");
                out.println("      paSetEditModeControls();");
                out.println("      var modal = document.getElementById('paEditModal'); if (modal) { modal.classList.add('pa-modal-open'); }");
                out.println("    })");
                out.println("    .catch(function(err){ console.log('Load edit request failed', err); });");
                out.println("  };");

                out.println("  window.paCloseEditModal = function(){");
                out.println("    var modal = document.getElementById('paEditModal');");
                out.println("    if (modal) { modal.classList.remove('pa-modal-open'); }");
                out.println("  };");

                out.println("  function paCloseModalOnOverlay(evt, overlay) { if (evt && evt.target === overlay) { overlay.classList.remove('pa-modal-open'); } }");
                out.println("  window.paDeleteCard = function(){");
                out.println("    var actionNextId = document.getElementById('eaEditActionId').value || '0';");
                out.println("    if (!actionNextId || actionNextId === '0') { return; }");
                out.println("    var body = 'action=deleteCardEdit' +");
                out.println("      '&actionNextId=' + encodeURIComponent(actionNextId) +");
                out.println("      '&windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("    paCloseEditModal();");
                out.println("    fetch('PlanAheadServlet', {");
                out.println("      method: 'POST',");
                out.println("      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },");
                out.println("      body: body");
                out.println("    })");
                out.println("    .then(function(r){ return r.json(); })");
                out.println("    .then(function(resp){");
                out.println("      if (!resp || !resp.success) { alert(resp ? resp.message : 'Delete failed'); return; }");
                out.println("      var undoLink = '<a href=\\\"#\\\" onclick=\\\"paUndoDeleteCard(' + actionNextId + '); return false;\\\" style=\\\"cursor:pointer;text-decoration:underline;\\\">undo</a>';");
                out.println("      var notifDiv = document.createElement('div');");
                out.println("      notifDiv.style.cssText = 'position:fixed;bottom:20px;right:20px;background:#fdd;color:#333;padding:12px 16px;border:1px solid #ccc;border-radius:4px;z-index:5000;font-size:13px;';");
                out.println("      notifDiv.innerHTML = 'Action deleted. ' + undoLink;");
                out.println("      document.body.appendChild(notifDiv);");
                out.println("      setTimeout(function(){");
                out.println("        window.location.href = 'PlanAheadServlet?windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("      }, 3000);");
                out.println("    })");
                out.println("    .catch(function(err){ console.log('Delete request failed', err); });");
                out.println("  };");

                out.println("  window.paUndoDeleteCard = function(actionNextId){");
                out.println("    if (!actionNextId) { return; }");
                out.println("    var body = 'action=undoDeleteCard' +");
                out.println("      '&actionNextId=' + encodeURIComponent(actionNextId) +");
                out.println("      '&windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("    fetch('PlanAheadServlet', {");
                out.println("      method: 'POST',");
                out.println("      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },");
                out.println("      body: body");
                out.println("    })");
                out.println("    .then(function(r){ return r.json(); })");
                out.println("    .then(function(resp){");
                out.println("      if (!resp || !resp.success) { alert(resp ? resp.message : 'Undo failed'); return; }");
                out.println("      window.location.href = 'PlanAheadServlet?windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("    })");
                out.println("    .catch(function(err){ console.log('Undo request failed', err); });");
                out.println("  };");

                out.println("  window.paSaveEditModal = function(){");
                out.println("    var actionTypeValue = paIsPersonal ? '' : (document.getElementById('eaEditActionType').value || '');");
                out.println("    var timeSlotValue = paIsPersonal ? (document.getElementById('eaEditActionTimeSlot').value || 'AFTERNOON') : ''; ");
                out.println("    var estimateValue = paIsPersonal ? '0' : (document.getElementById('eaEditActionTime').value || '0');");
                out.println("    var body = 'action=saveCardEdit' +");
                out.println(
                                "      '&actionNextId=' + encodeURIComponent(document.getElementById('eaEditActionId').value || '') +");
                out.println(
                                "      '&nextActionDate=' + encodeURIComponent(document.getElementById('eaEditActionDate').value || '') +");
                out.println("      '&nextActionType=' + encodeURIComponent(actionTypeValue) +");
                out.println("      '&timeSlot=' + encodeURIComponent(timeSlotValue) +");
                out.println(
                                "      '&nextDescription=' + encodeURIComponent(document.getElementById('eaEditActionDescription').value || '') +");
                out.println("      '&nextTimeEstimate=' + encodeURIComponent(estimateValue) +");
                out.println(
                                "      '&nextTargetDate=' + encodeURIComponent(document.getElementById('eaEditActionTarget').value || '') +");
                out.println(
                                "      '&nextDeadlineDate=' + encodeURIComponent(document.getElementById('eaEditActionDeadline').value || '') +");
                out.println("      '&linkUrl=' + encodeURIComponent(document.getElementById('eaEditActionLink').value || '') +");
                out.println(
                                "      '&nextContactId=' + encodeURIComponent(document.getElementById('eaEditActionContact').value || '') +");
                out.println("      '&nextNote=' + encodeURIComponent(document.getElementById('eaEditActionNote').value || '') +");
                out.println("      '&windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("    fetch('PlanAheadServlet', {");
                out.println("      method: 'POST',");
                out.println("      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },");
                out.println("      body: body");
                out.println("    })");
                out.println("    .then(function(r){ return r.json(); })");
                out.println("    .then(function(resp){");
                out.println(
                                "      if (!resp || !resp.success) { console.log('Save edit failed', resp ? resp.message : 'Unknown'); return; }");
                out.println("      if (paReloadAfterEdit) {");
                out.println("        window.location.href = 'PlanAheadServlet?windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("        return;");
                out.println("      }");
                out.println("      paApplyMutationPayload(resp);");
                out.println("      paCloseEditModal();");
                out.println("    })");
                out.println("    .catch(function(err){ console.log('Save edit request failed', err); });");
                out.println("  };");

                out.println("  var paStatusModalContext = { dayKey: '', statusCode: 'W', billMins: 0 }; ");

                out.println("  function paMinutesToClock(mins){");
                out.println("    var total = parseInt(mins, 10);");
                out.println("    if (isNaN(total) || total < 0) { total = 0; }");
                out.println("    var h = Math.floor(total / 60);");
                out.println("    var m = total % 60;");
                out.println("    return h + ':' + (m < 10 ? '0' : '') + m;");
                out.println("  }");

                out.println("  function paParseClockToMinutes(value){");
                out.println("    var text = (value || '').toString().trim();");
                out.println("    if (text.length === 0) { return 0; }");
                out.println("    if (/^\\d+$/.test(text)) { return parseInt(text, 10); }");
                out.println("    var match = text.match(/^(\\d{1,3}):(\\d{1,2})$/);");
                out.println("    if (!match) { return null; }");
                out.println("    var hours = parseInt(match[1], 10);");
                out.println("    var mins = parseInt(match[2], 10);");
                out.println("    if (isNaN(hours) || isNaN(mins) || mins < 0 || mins > 59) { return null; }");
                out.println("    return (hours * 60) + mins;");
                out.println("  }");

                out.println("  function paRecalculateTotals(){");
                out.println("    var grandTotal = 0;");
                out.println("    document.querySelectorAll('.pa-status-time-btn').forEach(function(btn){");
                out.println("      var mins = parseInt(btn.getAttribute('data-bill-mins') || '0', 10);");
                out.println("      if (!isNaN(mins) && mins > 0) { grandTotal += mins; }");
                out.println("    });");
                out.println("    var rowLabelEls = document.querySelectorAll('[data-row-key]');");
                out.println("    rowLabelEls.forEach(function(labelEl){");
                out.println("      var rowKey = labelEl.getAttribute('data-row-key');");
                out.println("      var rowTotal = 0;");
                out.println("      document.querySelectorAll('.pa-kanban[data-row=\"' + rowKey + '\"]').forEach(function(cell){");
                out.println("        cell.querySelectorAll('.pa-card-est-editable').forEach(function(btn){");
                out.println("          var mins = parseInt(btn.getAttribute('data-est-mins') || '0', 10);");
                out.println("          if (!isNaN(mins) && mins > 0) { rowTotal += mins; }");
                out.println("        });");
                out.println("      });");
                out.println("      var totalSpan = labelEl.querySelector('.pa-row-total');");
                out.println("      if (totalSpan) { totalSpan.textContent = paMinutesToClock(rowTotal); }");
                out.println("    });");
                out.println("    var grandEl = document.getElementById('pa-view-total');");
                out.println("    if (grandEl) { grandEl.textContent = paMinutesToClock(grandTotal); }");
                out.println("  }");

                out.println("  function paSaveDayCapacityValue(dayKey, statusCode, billMins, applyHeader){");
                out.println("    var body = 'action=saveDayCapacity' +");
                out.println("      '&billDate=' + encodeURIComponent(dayKey || '') +");
                out.println("      '&workStatus=' + encodeURIComponent(statusCode || 'W') +");
                out.println("      '&billMins=' + encodeURIComponent(billMins || 0) +");
                out.println("      '&windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("    return fetch('PlanAheadServlet', {");
                out.println("      method: 'POST',");
                out.println("      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },");
                out.println("      body: body");
                out.println("    })");
                out.println("    .then(function(r){ return r.json(); })");
                out.println("    .then(function(resp){");
                out.println("      if (!resp || !resp.success) {");
                out.println("        throw new Error(resp ? (resp.message || 'Save failed') : 'Save failed');");
                out.println("      }");
                out.println("      var d = (resp.data && typeof resp.data === 'object') ? resp.data : resp; ");
                out.println("      if (d.dayStatusHtml && d.dayKey) {");
                out.println("        var statusEl = document.getElementById('pa-day-status-' + d.dayKey);");
                out.println("        if (statusEl) { statusEl.outerHTML = d.dayStatusHtml; }");
                out.println("      }");
                out.println("      if (applyHeader && d.dayHeaderHtml && d.dayKey) {");
                out.println("        var headerEl = document.getElementById('pa-day-header-' + d.dayKey);");
                out.println("        if (headerEl) { headerEl.outerHTML = d.dayHeaderHtml; }");
                out.println("      }");
                out.println("      paRecalculateTotals();");
                out.println("      return d;");
                out.println("    });");
                out.println("  }");

                out.println("  function paBeginCardEstimateEdit(button){");
                out.println("    if (!button || button.getAttribute('data-editing') === 'true') { return; }");
                out.println("    var actionId = button.getAttribute('data-action-id') || ''; ");
                out.println("    if (!actionId) { return; }");
                out.println("    var originalMins = parseInt(button.getAttribute('data-est-mins') || '0', 10);");
                out.println("    if (isNaN(originalMins) || originalMins < 0) { originalMins = 0; }");
                out.println("    var originalText = button.textContent || paMinutesToClock(originalMins);");
                out.println("    button.setAttribute('data-editing', 'true');");
                out.println("    var input = document.createElement('input');");
                out.println("    input.type = 'text';");
                out.println("    input.className = 'pa-card-est-input';");
                out.println("    input.value = originalText;");
                out.println("    input.setAttribute('data-action-id', actionId);");
                out.println("    input.setAttribute('data-original-mins', String(originalMins));");
                out.println("    input.setAttribute('data-original-text', originalText);");
                out.println("    button.style.display = 'none';");
                out.println("    button.parentNode.insertBefore(input, button.nextSibling);");
                out.println("    input.focus();");
                out.println("    input.select();");
                out.println("    function finish(save){ paFinishCardEstimateEdit(input, button, save); }");
                out.println("    input.addEventListener('blur', function(){ finish(true); });");
                out.println("    input.addEventListener('keydown', function(e){");
                out.println("      if (e.key === 'Enter') { e.preventDefault(); finish(true); }");
                out.println("      if (e.key === 'Escape') { e.preventDefault(); finish(false); }");
                out.println("    });");
                out.println("  }");

                out.println("  function paFinishCardEstimateEdit(input, button, save){");
                out.println("    if (!input || !button || input.getAttribute('data-finished') === 'true') { return; }");
                out.println("    input.setAttribute('data-finished', 'true');");
                out.println("    var actionId = input.getAttribute('data-action-id') || ''; ");
                out.println("    var originalMins = parseInt(input.getAttribute('data-original-mins') || '0', 10);");
                out.println("    if (isNaN(originalMins) || originalMins < 0) { originalMins = 0; }");
                out.println("    var originalText = input.getAttribute('data-original-text') || paMinutesToClock(originalMins);");
                out.println("    var typed = (input.value || '').trim();");
                out.println("    var parsed = paParseClockToMinutes(typed);");
                out.println("    var hasValidValue = parsed !== null && parsed >= 0;");
                out.println("    if (!save || !hasValidValue) {");
                out.println("      button.textContent = originalText;");
                out.println("      button.setAttribute('data-est-mins', String(originalMins));");
                out.println("      paRestoreCardEstimateEditor(input, button);");
                out.println("      return;");
                out.println("    }");
                out.println("    if (parsed === originalMins) {");
                out.println("      button.textContent = paMinutesToClock(parsed);");
                out.println("      button.setAttribute('data-est-mins', String(parsed));");
                out.println("      paRestoreCardEstimateEditor(input, button);");
                out.println("      return;");
                out.println("    }");
                out.println("    var body = 'action=saveCardEstimate' +");
                out.println("      '&actionNextId=' + encodeURIComponent(actionId) +");
                out.println("      '&nextTimeEstimate=' + encodeURIComponent(parsed) +");
                out.println("      '&windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("    fetch('PlanAheadServlet', {");
                out.println("      method: 'POST',");
                out.println("      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },");
                out.println("      body: body");
                out.println("    })");
                out.println("    .then(function(r){ return r.json(); })");
                out.println("    .then(function(resp){");
                out.println("      if (!resp || !resp.success) {");
                out.println("        button.textContent = originalText;");
                out.println("        button.setAttribute('data-est-mins', String(originalMins));");
                out.println("        alert(resp ? (resp.message || 'Unable to save time estimate') : 'Unable to save time estimate');");
                out.println("        paRestoreCardEstimateEditor(input, button);");
                out.println("        return;");
                out.println("      }");
                out.println("      paApplyMutationPayload(resp);");
                out.println("    })");
                out.println("    .catch(function(err){");
                out.println("      console.log('saveCardEstimate request failed', err);");
                out.println("      button.textContent = originalText;");
                out.println("      button.setAttribute('data-est-mins', String(originalMins));");
                out.println("      alert('Unable to save time estimate right now.');");
                out.println("      paRestoreCardEstimateEditor(input, button);");
                out.println("    });");
                out.println("  }");

                out.println("  function paRestoreCardEstimateEditor(input, button){");
                out.println("    if (input && input.parentNode) { input.parentNode.removeChild(input); }");
                out.println("    button.removeAttribute('data-editing');");
                out.println("    button.style.display = ''; ");
                out.println("  }");

                out.println("  function paBeginCardDescriptionEdit(el){");
                out.println("    if (!el || el.getAttribute('data-editing') === 'true') { return; }");
                out.println("    var actionId = el.getAttribute('data-action-id') || ''; ");
                out.println("    if (!actionId) { return; }");
                out.println("    var originalRaw = el.getAttribute('data-raw-description') || ''; ");
                out.println("    el.setAttribute('data-editing', 'true');");
                out.println("    var input = document.createElement('input');");
                out.println("    input.type = 'text';");
                out.println("    input.className = 'pa-card-desc-input';");
                out.println("    input.value = originalRaw;");
                out.println("    input.setAttribute('data-action-id', actionId);");
                out.println("    input.setAttribute('data-original-raw', originalRaw);");
                out.println("    el.style.display = 'none';");
                out.println("    el.parentNode.insertBefore(input, el.nextSibling);");
                out.println("    input.focus();");
                out.println("    input.select();");
                out.println("    function finish(save){ paFinishCardDescriptionEdit(input, el, save); }");
                out.println("    input.addEventListener('blur', function(){ finish(true); });");
                out.println("    input.addEventListener('keydown', function(e){");
                out.println("      if (e.key === 'Enter') { e.preventDefault(); finish(true); }");
                out.println("      if (e.key === 'Escape') { e.preventDefault(); finish(false); }");
                out.println("    });");
                out.println("  }");

                out.println("  function paFinishCardDescriptionEdit(input, el, save){");
                out.println("    if (!input || !el || input.getAttribute('data-finished') === 'true') { return; }");
                out.println("    input.setAttribute('data-finished', 'true');");
                out.println("    var actionId = input.getAttribute('data-action-id') || ''; ");
                out.println("    var originalRaw = input.getAttribute('data-original-raw') || ''; ");
                out.println("    var typed = (input.value || '').trim();");
                out.println("    if (!save || typed === originalRaw) {");
                out.println("      paRestoreCardDescriptionEditor(input, el);");
                out.println("      return;");
                out.println("    }");
                out.println("    var body = 'action=saveCardDescriptionInline' +");
                out.println("      '&actionNextId=' + encodeURIComponent(actionId) +");
                out.println("      '&nextDescription=' + encodeURIComponent(typed) +");
                out.println("      '&windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("    fetch('PlanAheadServlet', {");
                out.println("      method: 'POST',");
                out.println("      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },");
                out.println("      body: body");
                out.println("    })");
                out.println("    .then(function(r){ return r.json(); })");
                out.println("    .then(function(resp){");
                out.println("      if (!resp || !resp.success) {");
                out.println("        paRestoreCardDescriptionEditor(input, el);");
                out.println("        return;");
                out.println("      }");
                out.println("      paApplyMutationPayload(resp);");
                out.println("    })");
                out.println("    .catch(function(){");
                out.println("      paRestoreCardDescriptionEditor(input, el);");
                out.println("    });");
                out.println("  }");

                out.println("  function paRestoreCardDescriptionEditor(input, el){");
                out.println("    if (input && input.parentNode) { input.parentNode.removeChild(input); }");
                out.println("    el.removeAttribute('data-editing');");
                out.println("    el.style.display = ''; ");
                out.println("  }");

                out.println("  function paBeginTemplateEstimateEdit(button){");
                out.println("    if (!button || button.getAttribute('data-editing') === 'true') { return; }");
                out.println("    var templateActionId = button.getAttribute('data-template-action-id') || ''; ");
                out.println("    if (!templateActionId) { return; }");
                out.println("    var originalMins = parseInt(button.getAttribute('data-est-mins') || '0', 10);");
                out.println("    if (isNaN(originalMins) || originalMins < 0) { originalMins = 0; }");
                out.println("    var originalText = button.textContent || paMinutesToClock(originalMins);");
                out.println("    button.setAttribute('data-editing', 'true');");
                out.println("    var input = document.createElement('input');");
                out.println("    input.type = 'text';");
                out.println("    input.className = 'pa-card-est-input';");
                out.println("    input.value = originalText;");
                out.println("    input.setAttribute('data-template-action-id', templateActionId);");
                out.println("    input.setAttribute('data-original-mins', String(originalMins));");
                out.println("    input.setAttribute('data-original-text', originalText);");
                out.println("    button.style.display = 'none';");
                out.println("    button.parentNode.insertBefore(input, button.nextSibling);");
                out.println("    input.focus();");
                out.println("    input.select();");
                out.println("    function finish(save){ paFinishTemplateEstimateEdit(input, button, save); }");
                out.println("    input.addEventListener('blur', function(){ finish(true); });");
                out.println("    input.addEventListener('keydown', function(e){");
                out.println("      if (e.key === 'Enter') { e.preventDefault(); finish(true); }");
                out.println("      if (e.key === 'Escape') { e.preventDefault(); finish(false); }");
                out.println("    });");
                out.println("  }");

                out.println("  function paFinishTemplateEstimateEdit(input, button, save){");
                out.println("    if (!input || !button || input.getAttribute('data-finished') === 'true') { return; }");
                out.println("    input.setAttribute('data-finished', 'true');");
                out.println("    var templateActionId = input.getAttribute('data-template-action-id') || ''; ");
                out.println("    var originalMins = parseInt(input.getAttribute('data-original-mins') || '0', 10);");
                out.println("    if (isNaN(originalMins) || originalMins < 0) { originalMins = 0; }");
                out.println("    var originalText = input.getAttribute('data-original-text') || paMinutesToClock(originalMins);");
                out.println("    var typed = (input.value || '').trim();");
                out.println("    var parsed = paParseClockToMinutes(typed);");
                out.println("    var hasValidValue = parsed !== null && parsed >= 0;");
                out.println("    if (!save || !hasValidValue) {");
                out.println("      button.textContent = originalText;");
                out.println("      button.setAttribute('data-est-mins', String(originalMins));");
                out.println("      paRestoreCardEstimateEditor(input, button);");
                out.println("      return;");
                out.println("    }");
                out.println("    if (parsed === originalMins) {");
                out.println("      button.textContent = paMinutesToClock(parsed);");
                out.println("      button.setAttribute('data-est-mins', String(parsed));");
                out.println("      paRestoreCardEstimateEditor(input, button);");
                out.println("      return;");
                out.println("    }");
                out.println("    var body = 'action=saveTemplateEstimate' +");
                out.println("      '&templateActionNextId=' + encodeURIComponent(templateActionId) +");
                out.println("      '&nextTimeEstimate=' + encodeURIComponent(parsed) +");
                out.println("      '&windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("    fetch('PlanAheadServlet', {");
                out.println("      method: 'POST',");
                out.println("      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },");
                out.println("      body: body");
                out.println("    })");
                out.println("    .then(function(r){ return r.json(); })");
                out.println("    .then(function(resp){");
                out.println("      if (!resp || !resp.success) {");
                out.println("        button.textContent = originalText;");
                out.println("        button.setAttribute('data-est-mins', String(originalMins));");
                out.println("        alert(resp ? (resp.message || 'Unable to save template estimate') : 'Unable to save template estimate');");
                out.println("        paRestoreCardEstimateEditor(input, button);");
                out.println("        return;");
                out.println("      }");
                out.println("      paApplyMutationPayload(resp);");
                out.println("    })");
                out.println("    .catch(function(err){");
                out.println("      console.log('saveTemplateEstimate request failed', err);");
                out.println("      button.textContent = originalText;");
                out.println("      button.setAttribute('data-est-mins', String(originalMins));");
                out.println("      alert('Unable to save template estimate right now.');");
                out.println("      paRestoreCardEstimateEditor(input, button);");
                out.println("    });");
                out.println("  }");

                out.println("  function paBeginStatusTimeEdit(button){");
                out.println("    if (!button || button.getAttribute('data-editing') === 'true') { return; }");
                out.println("    var dayKey = button.getAttribute('data-day-key') || ''; ");
                out.println("    var statusCode = button.getAttribute('data-status-code') || 'W';");
                out.println("    var originalMins = parseInt(button.getAttribute('data-bill-mins') || '0', 10);");
                out.println("    if (!dayKey || isNaN(originalMins) || originalMins < 0) { return; }");
                out.println("    var originalText = button.textContent || paMinutesToClock(originalMins);");
                out.println("    button.setAttribute('data-editing', 'true');");
                out.println("    var input = document.createElement('input');");
                out.println("    input.type = 'text';");
                out.println("    input.className = 'pa-status-time-input';");
                out.println("    input.value = originalText;");
                out.println("    input.setAttribute('data-day-key', dayKey);");
                out.println("    input.setAttribute('data-status-code', statusCode);");
                out.println("    input.setAttribute('data-original-mins', String(originalMins));");
                out.println("    input.setAttribute('data-original-text', originalText);");
                out.println("    button.style.display = 'none';");
                out.println("    button.parentNode.insertBefore(input, button.nextSibling);");
                out.println("    input.focus();");
                out.println("    input.select();");
                out.println("    function finish(save){ paFinishStatusTimeEdit(input, button, save); }");
                out.println("    input.addEventListener('blur', function(){ finish(true); });");
                out.println("    input.addEventListener('keydown', function(e){");
                out.println("      if (e.key === 'Enter') { e.preventDefault(); finish(true); }");
                out.println("      if (e.key === 'Escape') { e.preventDefault(); finish(false); }");
                out.println("    });");
                out.println("  }");

                out.println("  function paFinishStatusTimeEdit(input, button, save){");
                out.println("    if (!input || !button || input.getAttribute('data-finished') === 'true') { return; }");
                out.println("    input.setAttribute('data-finished', 'true');");
                out.println("    var dayKey = input.getAttribute('data-day-key') || ''; ");
                out.println("    var statusCode = input.getAttribute('data-status-code') || 'W';");
                out.println("    var originalMins = parseInt(input.getAttribute('data-original-mins') || '0', 10);");
                out.println("    if (isNaN(originalMins) || originalMins < 0) { originalMins = 0; }");
                out.println("    var originalText = input.getAttribute('data-original-text') || paMinutesToClock(originalMins);");
                out.println("    var parsed = paParseClockToMinutes((input.value || '').trim());");
                out.println("    if (!save || parsed === null || parsed < 0) {");
                out.println("      button.textContent = originalText;");
                out.println("      button.setAttribute('data-bill-mins', String(originalMins));");
                out.println("      paRestoreStatusTimeEditor(input, button);");
                out.println("      return;");
                out.println("    }");
                out.println("    if (parsed === originalMins) {");
                out.println("      button.textContent = paMinutesToClock(parsed);");
                out.println("      button.setAttribute('data-bill-mins', String(parsed));");
                out.println("      paRestoreStatusTimeEditor(input, button);");
                out.println("      return;");
                out.println("    }");
                out.println("    paSaveDayCapacityValue(dayKey, statusCode, parsed, true)");
                out.println("      .catch(function(err){");
                out.println("        console.log('saveDayCapacity request failed', err);");
                out.println("        button.textContent = originalText;");
                out.println("        button.setAttribute('data-bill-mins', String(originalMins));");
                out.println("        alert(err && err.message ? err.message : 'Unable to save day time right now.');");
                out.println("        paRestoreStatusTimeEditor(input, button);");
                out.println("      });");
                out.println("  }");

                out.println("  function paRestoreStatusTimeEditor(input, button){");
                out.println("    if (input && input.parentNode) { input.parentNode.removeChild(input); }");
                out.println("    button.removeAttribute('data-editing');");
                out.println("    button.style.display = ''; ");
                out.println("  }");

                out.println("  function paOpenStatusModal(dayKey, statusCode, billMins){");
                out.println("    paStatusModalContext.dayKey = dayKey || ''; ");
                out.println("    paStatusModalContext.statusCode = statusCode || 'W';");
                out.println("    paStatusModalContext.billMins = parseInt(billMins || 0, 10); ");
                out.println("    if (isNaN(paStatusModalContext.billMins) || paStatusModalContext.billMins < 0) { paStatusModalContext.billMins = 0; }");
                out.println("    var modal = document.getElementById('paStatusModal');");
                out.println("    if (!modal) { return; }");
                out.println("    modal.style.display = 'flex';");
                out.println("  }");

                out.println("  window.paCloseStatusModal = function(){");
                out.println("    var modal = document.getElementById('paStatusModal');");
                out.println("    if (modal) { modal.style.display = 'none'; }");
                out.println("  }; ");

                out.println("  document.addEventListener('click', function(e){");
                out.println("    var estBtn = e.target && e.target.closest ? e.target.closest('.pa-card-est-editable') : null;");
                out.println("    if (!estBtn) { return; }");
                out.println("    e.preventDefault();");
                out.println("    e.stopPropagation();");
                out.println("    paBeginCardEstimateEdit(estBtn);");
                out.println("  });");

                out.println("  document.addEventListener('click', function(e){");
                out.println("    var templateEstBtn = e.target && e.target.closest ? e.target.closest('.pa-template-est-editable') : null;");
                out.println("    if (!templateEstBtn) { return; }");
                out.println("    e.preventDefault();");
                out.println("    e.stopPropagation();");
                out.println("    paBeginTemplateEstimateEdit(templateEstBtn);");
                out.println("  });");

                out.println("  document.addEventListener('click', function(e){");
                out.println("    var statusBtn = e.target && e.target.closest ? e.target.closest('.pa-status-label-btn') : null;");
                out.println("    if (!statusBtn) { return; }");
                out.println("    e.preventDefault();");
                out.println("    e.stopPropagation();");
                out.println("    paOpenStatusModal(statusBtn.getAttribute('data-day-key'), statusBtn.getAttribute('data-status-code'), statusBtn.getAttribute('data-bill-mins'));");
                out.println("  });");

                out.println("  document.addEventListener('click', function(e){");
                out.println("    var statusOption = e.target && e.target.closest ? e.target.closest('.pa-status-option') : null;");
                out.println("    if (!statusOption) { return; }");
                out.println("    e.preventDefault();");
                out.println("    var statusCode = statusOption.getAttribute('data-status-code') || 'W';");
                out.println("    var nextBillMins = statusCode === 'W' ? paStatusModalContext.billMins : 0;");
                out.println("    paSaveDayCapacityValue(paStatusModalContext.dayKey, statusCode, nextBillMins, true)");
                out.println("      .then(function(){ paCloseStatusModal(); })");
                out.println("      .catch(function(){ paCloseStatusModal(); });");
                out.println("  });");

                out.println("  document.addEventListener('click', function(e){");
                out.println("    var statusTimeBtn = e.target && e.target.closest ? e.target.closest('.pa-status-time-btn') : null;");
                out.println("    if (!statusTimeBtn) { return; }");
                out.println("    e.preventDefault();");
                out.println("    e.stopPropagation();");
                out.println("    paBeginStatusTimeEdit(statusTimeBtn);");
                out.println("  });");

                out.println("  document.addEventListener('click', function(e){");
                out.println("    var descEl = e.target && e.target.closest ? e.target.closest('.pa-card-desc-editable') : null;");
                out.println("    if (!descEl) { return; }");
                out.println("    e.preventDefault();");
                out.println("    e.stopPropagation();");
                out.println("    paBeginCardDescriptionEdit(descEl);");
                out.println("  });");

                out.println("  document.addEventListener('click', function(e){");
                out.println("    var modal = document.getElementById('paStatusModal');");
                out.println("    if (!modal || modal.style.display === 'none') { return; }");
                out.println("    if (e.target === modal) { paCloseStatusModal(); }");
                out.println("  });");

                out.println("  window.paRefreshDayHeaders = function(dayKeysArray){");
                out.println("    if (!dayKeysArray || dayKeysArray.length === 0) { return; }");
                out.println("    var body = 'action=refreshDayHeaders' +");
                out.println("      '&dayKeys=' + encodeURIComponent(dayKeysArray.join(',')) +");
                out.println("      '&windowStart=' + encodeURIComponent(paWindowStart || '');");
                out.println("    fetch('PlanAheadServlet', {");
                out.println("      method: 'POST',");
                out.println("      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },");
                out.println("      body: body");
                out.println("    })");
                out.println("    .then(function(r){ return r.json(); })");
                out.println("    .then(function(resp){");
                out.println("      if (!resp || !resp.success) {");
                out.println("        console.log('refreshDayHeaders failed', resp ? resp.message : 'Unknown');");
                out.println("        return;");
                out.println("      }");
                out.println("      var headers = resp.data && resp.data.dayHeaders ? resp.data.dayHeaders : {};");
                out.println("      Object.keys(headers).forEach(function(dayKey){");
                out.println("        var entry = headers[dayKey];");
                out.println("        if (!entry || !entry.headerHtml) { return; }");
                out.println("        var el = document.getElementById('pa-day-header-' + dayKey);");
                out.println("        if (el) { el.outerHTML = entry.headerHtml; }");
                out.println("      });");
                out.println("    })");
                out.println("    .catch(function(err){ console.log('refreshDayHeaders request failed', err); });");
                out.println("  };");

                out.println("  function paApplyMutationPayload(data){");
                out.println("    if (data && data.affectedCellsHtml) {");
                out.println("      Object.keys(data.affectedCellsHtml).forEach(function(id){");
                out.println("        var el = document.getElementById(id);");
                out.println("        if (el) { el.outerHTML = data.affectedCellsHtml[id]; }");
                out.println("      });");
                out.println("    }");
                out.println("    if (data && data.affectedHeadersHtml) {");
                out.println("      Object.keys(data.affectedHeadersHtml).forEach(function(id){");
                out.println("        var el = document.getElementById(id);");
                out.println("        if (el) { el.outerHTML = data.affectedHeadersHtml[id]; }");
                out.println("      });");
                out.println("    }");
                out.println("    paRecalculateTotals();");
                out.println("  }");
                out.println("  paRecalculateTotals();");
                out.println("})();");
                out.println("</script>");
        }

        public String renderDayHeader(PlanAheadBoardModel.DayHeaderModel dayHeader) {
                StringBuilder s = new StringBuilder();
                boolean showGauge = dayHeader.getBillMins() > 0 || dayHeader.getPlannedMins() > 0;
                String headerClass = dayHeader.isWeekend() ? "pa-cell pa-header pa-header-weekend"
                                : "pa-cell pa-header";
                s.append("    <div class=\"").append(headerClass).append("\" id=\"")
                                .append(dayHeaderDomId(dayHeader.getDayKey()))
                                .append("\">");
                s.append("      <div class=\"pa-header-top\">");
                s.append("        <div class=\"pa-header-left\">");
                s.append("          <div class=\"pa-day-title\">").append(escapeHtml(dayHeader.getDayLabel()))
                                .append("</div>");
                s.append("          <div class=\"pa-day-date\">").append(escapeHtml(dayHeader.getDateLabel()))
                                .append("</div>");
                s.append("        </div>");
                if (showGauge) {
                        s.append("        <div class=\"pa-header-right\">");
                        s.append("          <div class=\"pa-day-gauge\">\n").append(renderGauge(dayHeader))
                                        .append("          </div>");
                        s.append("        </div>");
                }
                s.append("      </div>");
                s.append("    </div>");
                return s.toString();
        }

        public String renderDayStatusCell(PlanAheadBoardModel.DayHeaderModel dayHeader) {
                StringBuilder s = new StringBuilder();
                String statusLabel = dayHeader.getWorkStatusLabel();
                String timeDisplay = TimeTracker.formatTime(dayHeader.getBillMins());
                s.append("    <div class=\"pa-cell pa-status\" id=\"pa-day-status-")
                                .append(escapeHtml(dayHeader.getDayKey()))
                                .append("\">");
                s.append("      <div class=\"pa-status-main\">");
                s.append("<button type=\"button\" class=\"pa-status-label-btn\" data-day-key=\"")
                                .append(escapeHtml(dayHeader.getDayKey()))
                                .append("\" data-status-code=\"")
                                .append(escapeHtml(dayHeader.getWorkStatusCode()))
                                .append("\" data-bill-mins=\"")
                                .append(dayHeader.getBillMins())
                                .append("\">")
                                .append(escapeHtml(statusLabel))
                                .append("</button>");
                s.append("<button type=\"button\" class=\"pa-card-est-box pa-status-time-btn\" data-day-key=\"")
                                .append(escapeHtml(dayHeader.getDayKey()))
                                .append("\" data-status-code=\"")
                                .append(escapeHtml(dayHeader.getWorkStatusCode()))
                                .append("\" data-bill-mins=\"")
                                .append(dayHeader.getBillMins())
                                .append("\" title=\"Click to edit time\">")
                                .append(escapeHtml(timeDisplay))
                                .append("</button>");
                s.append("      </div>");
                s.append("    </div>");
                return s.toString();
        }

        private String renderKanbanCell(PlanAheadBoardModel.CellModel cell, boolean showEstimate) {
                StringBuilder s = new StringBuilder();
                s.append("    <div class=\"pa-cell pa-kanban\" id=\"")
                                .append(kanbanCellDomId(cell.getDayKey(), cell.getRowKey()))
                                .append("\" data-day=\"").append(escapeHtml(cell.getDayKey()))
                                .append("\" data-row=\"").append(escapeHtml(cell.getRowKey())).append("\">");
                if (cell.getCards().isEmpty()) {
                        s.append("<div class=\"pa-empty\">No cards</div>");
                } else {
                        for (PlanAheadBoardModel.CardModel card : cell.getCards()) {
                                s.append("<div class=\"pa-card\" draggable=\"true\" data-action-id=\"")
                                                .append(card.getActionNextId()).append("\"");
                                if (card.isRescheduleLocked()) {
                                        s.append(" data-reschedule-locked=\"true\"");
                                }
                                s.append(">");
                                s.append("<div class=\"pa-card-main\">");
                                s.append("<div class=\"pa-card-body\">");
                                s.append("<div class=\"pa-card-title pa-card-desc-editable\" data-action-id=\"")
                                                .append(card.getActionNextId())
                                                .append("\" data-raw-description=\"")
                                                .append(escapeHtml(card.getRawDescription()))
                                                .append("\" title=\"Click to edit description\">")
                                                .append(card.getDescription())
                                                .append("</div>");
                                s.append("<div class=\"pa-card-subline\">");
                                s.append("<span class=\"pa-card-project\">").append(escapeHtml(card.getProjectName()))
                                                .append("</span>");
                                s.append("<button type=\"button\" class=\"pa-card-edit-link\" onclick=\"paOpenEditModal(")
                                                .append(card.getActionNextId())
                                                .append(", event)\">edit</button>");
                                s.append("</div>");
                                s.append("</div>");
                                if (showEstimate) {
                                        s.append("<button type=\"button\" class=\"pa-card-est-box pa-card-est-editable\" data-action-id=\"")
                                                        .append(card.getActionNextId())
                                                        .append("\" data-est-mins=\"")
                                                        .append(card.getEstimateMins())
                                                        .append("\" title=\"Click to edit estimate\">")
                                                        .append(escapeHtml(card.getEstimateDisplay()))
                                                        .append("</button>");
                                }
                                s.append("</div>");
                                s.append("</div>");
                        }
                }
                s.append("</div>");
                return s.toString();
        }

        public String renderKanbanCellHtml(PlanAheadBoardModel.CellModel cell) {
                return renderKanbanCell(cell, true);
        }

        public String renderKanbanCellHtml(PlanAheadBoardModel.CellModel cell, boolean showEstimate) {
                return renderKanbanCell(cell, showEstimate);
        }

        public String renderOverdueCellHtml(PlanAheadBoardModel.OverdueRowModel overdueRow, boolean showEstimate) {
                return renderOverdueTodayCell(overdueRow, showEstimate);
        }

        public String renderTemplateSelectionCellHtml(PlanAheadBoardModel.TemplateCardModel templateCard,
                        PlanAheadBoardModel.DayHeaderModel dayHeader) {
                StringBuilder sb = new StringBuilder();
                sb.append("<div class=\"pa-template-selection-card\">");
                sb.append("<button type=\"button\" class=\"pa-template-toggle-btn\" onclick=\"paToggleTemplateDay(");
                sb.append(templateCard.getTemplateActionNextId());
                sb.append(",'");
                sb.append(dayHeader.getDayKey());
                sb.append("', this)\">");
                sb.append(templateCard.getDescription());
                sb.append("</button>");
                sb.append("</div>");
                return sb.toString();
        }

        private String renderOverdueTodayCell(PlanAheadBoardModel.OverdueRowModel overdueRow,
                        boolean showEstimate) {
                StringBuilder s = new StringBuilder();
                s.append("    <div class=\"pa-cell pa-overdue-today\" id=\"")
                                .append(kanbanCellDomId(overdueRow.getTodayKey(),
                                                org.dandeliondaily.planahead.service.PlanAheadBoardService.ROW_OVERDUE))
                                .append("\">");
                for (PlanAheadBoardModel.CardModel card : overdueRow.getCards()) {
                        s.append("<div class=\"pa-card pa-card-overdue\" draggable=\"true\" data-action-id=\"")
                                        .append(card.getActionNextId()).append("\"");
                        if (card.isRescheduleLocked()) {
                                s.append(" data-reschedule-locked=\"true\"");
                        }
                        s.append(">");
                        s.append("<div class=\"pa-card-main\">");
                        s.append("<div class=\"pa-card-body\">");
                        s.append("<div class=\"pa-card-title pa-card-desc-editable\" data-action-id=\"")
                                        .append(card.getActionNextId())
                                        .append("\" data-raw-description=\"")
                                        .append(escapeHtml(card.getRawDescription()))
                                        .append("\" title=\"Click to edit description\">")
                                        .append(card.getDescription())
                                        .append("</div>");
                        s.append("<div class=\"pa-card-subline\">");
                        s.append("<span class=\"pa-card-project\">")
                                        .append(escapeHtml(card.getProjectName()))
                                        .append("</span>");
                        s.append("<button type=\"button\" class=\"pa-card-edit-link\" onclick=\"paOpenEditModal(")
                                        .append(card.getActionNextId())
                                        .append(", event, true)\">edit</button>");
                        s.append("</div>");
                        s.append("</div>");
                        if (showEstimate) {
                                s.append("<button type=\"button\" class=\"pa-card-est-box pa-card-est-editable\" data-action-id=\"")
                                                .append(card.getActionNextId())
                                                .append("\" data-est-mins=\"")
                                                .append(card.getEstimateMins())
                                                .append("\" title=\"Click to edit estimate\">")
                                                .append(escapeHtml(card.getEstimateDisplay()))
                                                .append("</button>");
                        }
                        s.append("</div>");
                        s.append("</div>");
                }
                s.append("</div>");
                return s.toString();
        }

        public static String dayHeaderDomId(String dayKey) {
                return "pa-day-header-" + dayKey;
        }

        public static String kanbanCellDomId(String dayKey, String rowKey) {
                return "pa-kanban-" + dayKey + "-" + rowKey;
        }

        private String renderGauge(PlanAheadBoardModel.DayHeaderModel dayHeader) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                timeGaugeRenderer.render(printWriter, dayHeader.getGauge());
                printWriter.flush();
                return stringWriter.toString();
        }

        private void printStyles(PrintWriter out, int dayCount) {
                int safeDayCount = dayCount <= 0 ? 5 : dayCount;
                out.println("<style>");
                out.println(
                                ".pa-page{padding:12px 18px 24px 18px;background:linear-gradient(180deg,#f4f0e8 0%,#efe7db 40%,#f8f6f1 100%);}");
                out.println(".pa-intro-bar{display:flex;align-items:flex-start;justify-content:space-between;gap:18px;flex-wrap:wrap;}");
                out.println(".pa-intro h1{margin:0;color:#2d3a2d;}");
                out.println(".pa-intro p{margin:6px 0 14px 0;color:#425541;}");
                out.println(".pa-quick-capture{margin-left:auto;min-width:360px;max-width:520px;flex:1 1 420px;padding:10px 12px;background:#f8f1e6;border:1px solid #d7c8b1;border-radius:6px;}");
                out.println(".pa-quick-capture-title{font-size:12px;font-weight:bold;letter-spacing:.04em;text-transform:uppercase;color:#52614d;margin-bottom:8px;}");
                out.println(".pa-quick-capture-form{margin:0;}");
                out.println(".pa-quick-capture-row{display:flex;gap:8px;align-items:flex-start;}");
                out.println(".pa-quick-capture-input-wrap{position:relative;flex:1 1 auto;}");
                out.println(".pa-quick-capture-input-wrap input{width:100%;box-sizing:border-box;padding:8px 10px;border:1px solid #cbbda7;font-size:13px;background:#fffdf8;}");
                out.println(".pa-quick-capture-actions input{padding:8px 12px;border:1px solid #9fb1a0;background:#eef5ee;color:#2f4330;cursor:pointer;border-radius:4px;}");
                out.println(".pa-qc-start-btn{padding:8px 12px;border:1px solid #7a9b7a;background:#d6ecd6;color:#1f3a1f;cursor:pointer;border-radius:4px;font-size:inherit;}");
                out.println(".pa-capture-suggestions{display:none;position:absolute;left:0;right:0;top:100%;z-index:50;background:#fff;border:1px solid #cbbda7;border-top:none;box-shadow:0 6px 16px rgba(0,0,0,.12);max-height:180px;overflow-y:auto;}");
                out.println(".pa-capture-suggestions div{padding:7px 10px;cursor:pointer;font-size:13px;color:#2f4330;}");
                out.println(".pa-capture-suggestions div:hover{background:#eef5ee;}");
                out.println(".pa-controls{display:flex;gap:8px;margin-bottom:10px;}");
                out.println(
                                ".pa-shift{display:inline-block;padding:6px 10px;background:#49654a;color:#fff;text-decoration:none;border-radius:4px;}");
                out.println(
                                ".pa-grid{display:grid;grid-template-columns:220px repeat(" + safeDayCount
                                                + ",minmax(220px,1fr));border:1px solid #cbbda7;background:#fffdf8;}");
                out.println(
                                ".pa-cell{border-right:1px solid #dfd3c1;border-bottom:1px solid #dfd3c1;padding:10px;min-height:84px;box-sizing:border-box;}");
                out.println(".pa-cell-label{background:#f8f1e6;font-weight:bold;color:#324532;}");
                out.println(".pa-cell-blank{min-height:0;padding-top:6px;padding-bottom:6px;font-size:0;}");
                out.println(".pa-header{background:#395238;color:#fffdf8;position:sticky;top:0;z-index:30;}");
                out.println(".pa-header-weekend{background:#4e6e50;}");
                out.println(".pa-mode-toggle-cell{display:flex;align-items:center;justify-content:center;}");
                out.println(".pa-mode-toggle{display:inline-flex;background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.45);border-radius:6px;overflow:hidden;}");
                out.println(".pa-mode-option{display:inline-block;padding:6px 10px;color:#fffdf8;text-decoration:none;font-size:12px;font-weight:bold;letter-spacing:.03em;}");
                out.println(".pa-mode-option.is-active{background:#fffdf8;color:#2f4330;}");
                out.println(".pa-mode-option:not(.is-active):hover{background:rgba(255,255,255,.18);}");
                out.println(".pa-row-label{display:flex;align-items:center;justify-content:space-between;gap:6px;}");
                out.println(".pa-row-label-name{flex:1 1 auto;min-width:0;}");
                out.println(".pa-status-total-cell{justify-content:flex-start;}");
                out.println(".pa-status-total-label{border:none;background:none;padding:0;font-size:16px;font-weight:bold;color:#2f4330;line-height:1.1;}");
                out.println(".pa-status-total-box{background:#d9dee3;border-color:#9faab5;color:#25313a;cursor:default;box-shadow:inset 0 1px 0 rgba(255,255,255,.45);}");
                out.println(".pa-header-top{display:flex;flex-wrap:wrap;align-items:flex-start;gap:8px;}");
                out.println(".pa-header-left{min-width:0;}");
                out.println(".pa-header-right{flex:1 0 100%;min-width:0;max-width:none;text-align:left;}");
                out.println(".pa-day-title{font-size:16px;font-weight:bold;}");
                out.println(".pa-day-date{font-size:12px;opacity:.95;margin-top:2px;}");
                out.println(".pa-day-metrics{font-size:12px;margin-top:0;}");
                out.println(".pa-day-gauge{margin-top:4px;display:block;}");
                out.println(".pa-day-gauge .dd-time-gauge{width:90%;}");
                out.println(".pa-status{background:#fbf8f2;min-height:0;padding-top:6px;padding-bottom:6px;}");
                out.println(".pa-status-main{display:flex;align-items:center;justify-content:space-between;gap:8px;}");
                out.println(".pa-status-label-btn{border:none;background:none;padding:0;font-size:16px;font-weight:normal;color:#2f4330;line-height:1.1;cursor:pointer;text-align:left;}");
                out.println(".pa-status-label-btn:hover{text-decoration:underline;color:#223947;}");
                out.println(".pa-status-time-btn{cursor:text;}");
                out.println(".pa-status-time-btn:hover{border-color:#9fb1c2;background:#f7fbff;}");
                out.println(".pa-status-time-input{flex:0 0 auto;width:62px;padding:4px 6px;border:1px solid #6f98bf;border-radius:4px;text-align:center;font-size:14px;font-weight:bold;line-height:1.1;color:#1f2f3a;background:#ffffff;}");
                out.println(".pa-kanban{background:#fff;}");
                out.println(".pa-row-overdue-label{background:#fff0ee;color:#8b2020;}");
                out.println(".pa-row-overdue-label .pa-row-label-name{color:#8b2020;}");
                out.println(".pa-row-overdue-label .pa-card-est-box{background:#fdecea;border-color:#e0a0a0;color:#8b2020;}");
                out.println(".pa-overdue-today{background:#fff0ee;padding:6px 8px;min-height:40px;}");
                out.println(".pa-card-overdue{background:#fdecea;border-color:#e0a0a0;}");
                out.println(".pa-overdue-disabled{background:#f5f5f5;background-image:repeating-linear-gradient(45deg,#e2e2e2 0,#e2e2e2 1px,transparent 0,transparent 50%);background-size:6px 6px;min-height:40px;}");
                out.println(
                                ".pa-card{background:#f6f8fb;border:1px solid #d9e1ea;border-radius:6px;padding:6px 8px;margin-bottom:6px;cursor:grab;}");
                out.println(".pa-card-main{display:flex;align-items:flex-start;gap:8px;}");
                out.println(".pa-card-body{min-width:0;flex:1 1 auto;}");
                out.println(".pa-card-title{font-size:13px;line-height:1.2;color:#1f2f3a;}");
                out.println(".pa-card-title i{font-style:italic;color:#3a4d5b;}");
                out.println(".pa-card-desc-editable{cursor:text;}");
                out.println(".pa-card-subline{margin-top:4px;display:flex;align-items:center;gap:8px;min-width:0;}");
                out.println(".pa-card-project{font-size:11px;font-weight:bold;color:#4b6072;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}");
                out.println(".pa-card-edit-link{border:none;background:none;padding:0;color:#9aa0a6;font-size:11px;cursor:pointer;text-decoration:none;}");
                out.println(".pa-card-edit-link:hover{color:#6f767d;text-decoration:underline;}");
                out.println(".pa-card-est-box{flex:0 0 auto;min-width:56px;padding:4px 6px;background:#ffffff;border:1px solid #cfd8e3;border-radius:4px;text-align:center;font-size:16px;font-weight:bold;line-height:1.1;color:#22313f;}");
                out.println(".pa-card-est-editable{cursor:text;}");
                out.println(".pa-card-est-editable:hover{border-color:#9fb1c2;background:#f7fbff;}");
                out.println(".pa-card-est-input{flex:0 0 auto;width:62px;padding:4px 6px;border:1px solid #6f98bf;border-radius:4px;text-align:center;font-size:14px;font-weight:bold;line-height:1.1;color:#1f2f3a;background:#ffffff;}");
                out.println(".pa-card-desc-input{width:100%;padding:3px 5px;border:1px solid #6f98bf;border-radius:4px;font-size:13px;line-height:1.2;color:#1f2f3a;background:#ffffff;box-sizing:border-box;}");
                out.println(
                                ".pa-card-edit{font-size:11px;padding:2px 8px;border:1px solid #9fb1c2;background:#eef3f8;cursor:pointer;}");
                out.println(".pa-empty{font-size:12px;color:#7b7b7b;font-style:italic;}");
                out.println(".pa-template-label-cell{background:#f8f1e6;color:#324532;font-weight:normal;min-height:0;padding-top:6px;padding-bottom:6px;display:flex;align-items:center;justify-content:space-between;gap:8px;}");
                out.println(".pa-template-label-main{display:flex;align-items:center;gap:8px;min-width:0;flex:1 1 auto;white-space:nowrap;}");
                out.println(".pa-template-inline-text{font-size:13px;line-height:1.1;font-weight:normal;color:#2f3f30;min-width:0;overflow:hidden;text-overflow:ellipsis;flex:1 1 auto;}");
                out.println(".pa-template-inline-edit{border:none;background:none;padding:0;color:#9aa0a6;font-size:11px;cursor:pointer;text-decoration:none;flex:0 0 auto;}");
                out.println(".pa-template-inline-edit:hover{color:#6f767d;text-decoration:underline;}");
                out.println(".pa-template-day-column{background:#fffaf0;min-height:0;padding-top:6px;padding-bottom:6px;display:block;}");
                out.println(".pa-template-day-toggle{display:flex;align-items:flex-start;gap:6px;font-size:11px;color:#4e4e4e;cursor:pointer;line-height:1.2;margin:0;}");
                out.println(".pa-template-day-toggle input{margin-top:2px;}");
                out.println(".pa-template-day-desc{display:block;color:#2f3f30;}");
                out.println(".pa-template-est-editable{cursor:text;}");
                out.println(".pa-template-est-editable:hover{border-color:#9fb1c2;background:#f7fbff;}");
                out.println(".pa-template-add-row{justify-content:flex-start;}");
                out.println(".pa-template-add-btn{font-size:12px;padding:4px 10px;border:1px solid #9fb1a0;background:#eef5ee;cursor:pointer;border-radius:4px;color:#2f4330;}");
                out.println(".pa-template-add-empty{background:#fffaf0;min-height:0;padding-top:6px;padding-bottom:6px;}");
                out.println(
                                ".pa-modal-overlay{position:fixed;inset:0;background:rgba(0,0,0,.35);z-index:1000;display:flex;align-items:center;justify-content:center;}");
                out.println(".pa-edit-modal-overlay{display:none;}");
                out.println(".pa-edit-modal-overlay.pa-modal-open{display:flex;}");
                out.println(
                                ".pa-modal{background:#fff;min-width:520px;max-width:720px;border:1px solid #cbbda7;box-shadow:0 10px 28px rgba(0,0,0,.25);}");
                out.println(
                                ".pa-modal-head{display:flex;justify-content:space-between;align-items:center;padding:10px 14px;background:#f3eadc;border-bottom:1px solid #d7c8b1;}");
                out.println(".pa-modal-head h3{margin:0;font-size:18px;color:#2f4330;}");
                out.println(".pa-modal-close{border:none;background:transparent;font-size:24px;cursor:pointer;line-height:1;}");
                out.println(".pa-modal-body{padding:12px 14px;display:grid;grid-template-columns:1fr 1fr;gap:10px;}");
                out.println(".pa-modal-body label{display:flex;flex-direction:column;font-size:12px;color:#2f3f30;gap:4px;}");
                out.println(".pa-status-modal-overlay{z-index:1100;}");
                out.println(".pa-status-modal{min-width:280px;max-width:360px;}");
                out.println(".pa-status-modal-body{padding:12px 14px;display:flex;flex-direction:column;gap:8px;}");
                out.println(".pa-status-option{padding:7px 10px;border:1px solid #cbbda7;background:#fffdf8;color:#2f4330;cursor:pointer;text-align:left;border-radius:4px;}");
                out.println(".pa-status-option:hover{background:#eef5ee;border-color:#9fb1a0;}");
                out.println("#paTemplateProjectName[readonly]{background:#f7f7f7;color:#46554a;}");
                out.println(
                                ".pa-modal-body input:not([type=radio]):not([type=checkbox]),.pa-modal-body select,.pa-modal-body textarea{padding:6px;border:1px solid #cbbda7;font-size:13px;}");
                out.println(".pa-modal-body textarea{resize:vertical;}");
                out.println(".pa-modal-actions{grid-column:1 / span 2;display:flex;gap:8px;justify-content:flex-end;}");
                out.println(
                                ".pa-modal-actions button{padding:6px 12px;border:1px solid #9fb1a0;background:#eef5ee;cursor:pointer;}");
                out.println("@media (max-width: 900px){.pa-quick-capture{min-width:0;max-width:none;width:100%;}.pa-quick-capture-row{flex-direction:column;}.pa-quick-capture-actions{width:100%;}.pa-quick-capture-actions input{width:100%;}}");
                out.println(".pa-edit-field{display:flex;flex-direction:column;gap:4px;margin-bottom:10px;}");
                out.println(".pa-edit-label{font-size:13px;font-weight:bold;color:#2d3a2d;}");
                out.println(".pa-edit-select{padding:6px;border:1px solid #cbbda7;font-size:13px;}");
                out.println(".pa-advanced-section{margin-bottom:10px;border:1px solid #cbbda7;border-radius:3px;}");
                out.println(".pa-advanced-toggle{padding:5px 8px;cursor:pointer;font-size:13px;font-weight:bold;color:#5b735c;list-style:none;user-select:none;}");
                out.println(".pa-advanced-toggle::-webkit-details-marker{display:none;}");
                out.println(".pa-advanced-toggle::before{content:'\\25B6 ';font-size:10px;}");
                out.println("details.pa-advanced-section[open] .pa-advanced-toggle::before{content:'\\25BC ';}");
                out.println(".pa-advanced-body{padding:6px 8px 2px;}");
                editActionModalRenderer.printSharedStyles(out);
                timeGaugeRenderer.printStyles(out);
                out.println("</style>");
        }

        private String escapeJs(String value) {
                if (value == null) {
                        return "";
                }
                return value.replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\r", "")
                                .replace("\n", "\\n");
        }

        private String escapeHtml(String value) {
                if (value == null) {
                        return "";
                }
                return value.replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;")
                                .replace("'", "&#39;");
        }
}
