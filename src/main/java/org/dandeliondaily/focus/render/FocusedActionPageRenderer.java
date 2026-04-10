package org.dandeliondaily.focus.render;

import java.io.PrintWriter;
import java.util.List;

import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.ActionNext;

public class FocusedActionPageRenderer {

        public static class MeetingOption {
                private final int actionNextId;
                private final String label;

                public MeetingOption(int actionNextId, String label) {
                        this.actionNextId = actionNextId;
                        this.label = label;
                }

                public int getActionNextId() {
                        return actionNextId;
                }

                public String getLabel() {
                        return label;
                }
        }

        public static class PreviousActionOption {
                private final int actionNextId;
                private final String label;

                public PreviousActionOption(int actionNextId, String label) {
                        this.actionNextId = actionNextId;
                        this.label = label;
                }

                public int getActionNextId() {
                        return actionNextId;
                }

                public String getLabel() {
                        return label;
                }
        }

        public void render(AppReq appReq, ActionNext action, List<String> notes,
                        List<MeetingOption> meetingOptions,
                        List<PreviousActionOption> previousActions,
                        int spentMinutes, int estimateMinutes, boolean runningClock, int nowMinute,
                        int spentMinutesThisWeek, int todayBillableMinutes,
                        int todayTargetMinutes, int weekTargetMinutes,
                        String nextActionHint, Integer nextActionHintId,
                        String quickCaptureSentenceValue, List<String> quickCaptureProjectNames,
                        boolean quickCaptureFocusRequested) {
                PrintWriter out = appReq.getOut();
                printStyles(out);
                String closeTarget = action == null
                                ? "DandelionDashboardServlet"
                                : "DandelionDashboardServlet?action=SelectAction&completingActionNextId="
                                                + action.getActionNextId();

                out.println("<div class=\"fa-page\">");
                out.println("  <div class=\"fa-quick-capture-box\">");
                out.println("    <div class=\"fa-quick-capture-title\">Quick Capture</div>");
                out.println("    <form class=\"fa-capture-form\" method=\"POST\" action=\"FocusedActionServlet\">");
                out.println("      <div class=\"fa-capture-row\">");
                out.println("        <div class=\"fa-capture-input-container\">");
                out.println("          <input type=\"text\" id=\"sentenceInput\" name=\"sentenceInput\" value=\""
                                + escapeHtml(n(quickCaptureSentenceValue))
                                + "\" placeholder=\"Project Name: I will action details\" autocomplete=\"off\""
                                + (quickCaptureFocusRequested ? " autofocus=\"autofocus\"" : "") + " />");
                out.println("          <div id=\"suggestions\" class=\"fa-capture-suggestions\"></div>");
                out.println("        </div>");
                out.println("        <div class=\"fa-capture-actions\">");
                out.println("          <input type=\"submit\" name=\"action\" value=\"Schedule\" />");
                out.println("          <button type=\"submit\" name=\"action\" value=\"Schedule and Start\" class=\"fa-qc-start-btn\">Start</button>");
                out.println("        </div>");
                out.println("      </div>");
                out.println("    </form>");
                out.println("  </div>");
                out.println(
                                "  <button class=\"fa-close\" title=\"Back to Dandelion Dashboard\" onclick=\"window.location.href='"
                                                + closeTarget + "';\">&times;</button>");
                out.println("  <div class=\"fa-shell\">");
                out.println("    <section class=\"fa-left\">");
                out.println("      <div class=\"fa-left-scroll\">");

                if (action == null) {
                        out.println("        <h1 class=\"fa-title\">No current action selected</h1>");
                        out.println("        <p class=\"fa-subtitle\">Return to dashboard to choose an action.</p>");
                        out.println("      </div>");
                        out.println("    </section>");
                        out.println("    <section class=\"fa-right\">"
                                        + "<div class=\"fa-empty-clock\">No timer context</div>"
                                        + "</section>");
                        out.println("  </div>");
                        out.println("</div>");
                        return;
                }

                String projectName = action.getProject() == null ? "" : n(action.getProject().getProjectName());
                String descriptionForDisplay = action.getNextDescriptionForDisplay(null);

                out.println("        <div class=\"fa-card fa-title-card\">");
                out.println("          <div class=\"fa-main-title\">" + descriptionForDisplay
                                + " <span class=\"fa-project-subtitle\">" + escapeHtml(projectName) + "</span></div>");
                out.println("          <ul id=\"fa-notes-list\" class=\"fa-notes\">");
                for (String note : notes) {
                        out.println("            <li>" + escapeHtml(note) + "</li>");
                }
                out.println("          </ul>");
                out.println("          <div class=\"fa-inline-note\">");
                out.println("            <textarea id=\"fa-note-input\" rows=\"3\" placeholder=\"Add note...\" onkeydown=\"faHandleNoteKeydown(event, "
                                + action.getActionNextId() + ")\"></textarea>");
                out.println("            <div class=\"fa-note-actions\">");
                out.println("              <button type=\"button\" class=\"fa-primary-btn fa-note-submit-btn\" onclick=\"faSubmitNote("
                                + action.getActionNextId() + ")\">Add Note</button>");
                out.println(
                                "              <button type=\"button\" class=\"fa-secondary-btn\" onclick=\"faOpenRescheduleModal(event)\">Reschedule</button>");
                out.println(
                                "              <button type=\"button\" class=\"fa-secondary-btn\" onclick=\"faOpenEditModal(event)\">Edit Action</button>");
                out.println("            </div>");
                out.println("          </div>");
                out.println("        </div>");

                out.println("        <div id=\"faRescheduleModal\" class=\"fa-modal-overlay\" onclick=\"faCloseModalOnOverlay(event, this)\">");
                out.println("          <div class=\"fa-modal fa-reschedule-modal\" onclick=\"event.stopPropagation()\">");
                out.println("            <div class=\"fa-modal-head\">");
                out.println("              <h3 class=\"fa-modal-title\">Reschedule Action</h3>");
                out.println("              <button type=\"button\" class=\"fa-modal-close\" onclick=\"faCloseModal('faRescheduleModal')\">&times;</button>");
                out.println("            </div>");
                out.println("            <div class=\"fa-modal-body\">");
                out.println("              <div class=\"fa-modal-field\">");
                out.println("                <label for=\"faRescheduleDate\">New Date (today or future)</label>");
                out.println("                <input type=\"date\" id=\"faRescheduleDate\" />");
                out.println("              </div>");
                out.println("              <div class=\"fa-modal-actions\">");
                out.println("                <button type=\"button\" class=\"fa-primary-btn\" onclick=\"faSubmitReschedule()\">Apply</button>");
                out.println("                <button type=\"button\" class=\"fa-secondary-btn\" onclick=\"faCloseModal('faRescheduleModal')\">Cancel</button>");
                out.println("              </div>");
                out.println("            </div>");
                out.println("          </div>");
                out.println("        </div>");

                out.println("        <div id=\"faEditModal\" class=\"fa-modal-overlay\" onclick=\"faCloseModalOnOverlay(event, this)\">");
                out.println("          <div class=\"fa-modal fa-edit-modal\" onclick=\"event.stopPropagation()\">");
                out.println("            <div class=\"fa-modal-head\">");
                out.println("              <h3 class=\"fa-modal-title\">Edit Action</h3>");
                out.println("              <button type=\"button\" class=\"fa-modal-close\" onclick=\"faCloseModal('faEditModal')\">&times;</button>");
                out.println("            </div>");
                out.println("            <div class=\"fa-modal-body\">");
                out.println("              <div class=\"fa-modal-field\">");
                out.println("                <label for=\"faEditActionDate\">When</label>");
                out.println("                <input type=\"text\" id=\"faEditActionDate\" />");
                out.println("              </div>");
                out.println("              <div class=\"fa-modal-field\">");
                out.println("                <label for=\"faEditActionDescription\">Description</label>");
                out.println("                <textarea id=\"faEditActionDescription\" rows=\"2\"></textarea>");
                out.println("              </div>");
                out.println("              <div class=\"fa-modal-row\">");
                out.println("                <div class=\"fa-modal-field\">");
                out.println("                  <label for=\"faEditActionTime\">Time Estimate (mins)</label>");
                out.println("                  <input type=\"number\" id=\"faEditActionTime\" min=\"0\" step=\"1\" />");
                out.println("                </div>");
                out.println("                <div class=\"fa-modal-field\">");
                out.println("                  <label for=\"faEditActionType\">Action Type</label>");
                out.println("                  <input type=\"text\" id=\"faEditActionType\" />");
                out.println("                </div>");
                out.println("              </div>");
                out.println("              <div class=\"fa-modal-row\">");
                out.println("                <div class=\"fa-modal-field\">");
                out.println("                  <label for=\"faEditActionTarget\">Target Date</label>");
                out.println("                  <input type=\"text\" id=\"faEditActionTarget\" />");
                out.println("                </div>");
                out.println("                <div class=\"fa-modal-field\">");
                out.println("                  <label for=\"faEditActionDeadline\">Deadline</label>");
                out.println("                  <input type=\"text\" id=\"faEditActionDeadline\" />");
                out.println("                </div>");
                out.println("              </div>");
                out.println("              <div class=\"fa-modal-field\">");
                out.println("                <label for=\"faEditActionLink\">Link URL</label>");
                out.println("                <input type=\"text\" id=\"faEditActionLink\" />");
                out.println("              </div>");
                out.println("              <div class=\"fa-modal-field\">");
                out.println("                <label for=\"faEditActionNote\">Note</label>");
                out.println("                <textarea id=\"faEditActionNote\" rows=\"2\"></textarea>");
                out.println("              </div>");
                out.println("              <div class=\"fa-modal-actions\">");
                out.println("                <button type=\"button\" class=\"fa-primary-btn\" onclick=\"faSubmitEdit('save')\">Save Action</button>");
                out.println("                <button type=\"button\" class=\"fa-primary-btn\" onclick=\"faSubmitEdit('saveAndStart')\">Save and Start</button>");
                out.println("                <button type=\"button\" class=\"fa-secondary-btn\" onclick=\"faCloseModal('faEditModal')\">Cancel</button>");
                out.println("              </div>");
                out.println("            </div>");
                out.println("          </div>");
                out.println("        </div>");

                out.println("        <div class=\"fa-card\">");
                out.println("          <form method=\"POST\" action=\"FocusedActionServlet\" class=\"fa-work-form\">");
                out.println("            <div class=\"fa-work-progress-label\">What action was taken:</div>");
                out.println("            <input type=\"text\" class=\"fa-work-progress-input\" name=\"nextSummary\" value=\"\" autofocus />");
                out.println("            <div class=\"fa-status-row fa-work-status-toggle\">");
                out.println(
                                "              <input type=\"radio\" id=\"faWorkStatusInProgress\" name=\"workStatus\" value=\"IN_PROGRESS\" checked/><label for=\"faWorkStatusInProgress\">In Progress</label>");
                out.println(
                                "              <input type=\"radio\" id=\"faWorkStatusComplete\" name=\"workStatus\" value=\"COMPLETE\"/><label for=\"faWorkStatusComplete\">Complete</label>");
                out.println("              <input type=\"radio\" id=\"faWorkStatusDelete\" name=\"workStatus\" value=\"DELETE\"/><label for=\"faWorkStatusDelete\">Delete</label>");
                out.println(
                                "              <input type=\"radio\" id=\"faWorkStatusBlocked\" name=\"workStatus\" value=\"BLOCKED\"/><label for=\"faWorkStatusBlocked\">Blocked</label>");
                out.println("            </div>");
                out.println("            <div class=\"fa-next-row\">");
                out.println("              <span>Next</span>");
                out.println("              <span class=\"fa-capture-input-container\" style=\"flex:1;\">");
                out.println("                <input type=\"text\" id=\"workFollowUpInput\" name=\"workFollowUp\" style=\"width:100%;\" autocomplete=\"off\" />");
                out.println("                <div id=\"workFollowUpSuggestions\" class=\"fa-capture-suggestions\"></div>");
                out.println("              </span>");
                out.println(
                                "              <button type=\"submit\" name=\"action\" value=\"WorkNext\" class=\"fa-primary-btn\">Next</button>");
                out.println("            </div>");
                out.println(
                                "            <input type=\"hidden\" name=\"completingActionNextId\" value=\""
                                                + action.getActionNextId()
                                                + "\" />");
                out.println("          </form>");
                out.println("        </div>");

                out.println("        <div class=\"fa-card\">");
                out.println("          <h2 class=\"fa-section-title\">Meetings</h2>");
                if (!meetingOptions.isEmpty()) {
                        out.println("          <div id=\"fa-meetings-list\" class=\"fa-meetings-list\">");
                        for (MeetingOption option : meetingOptions) {
                                out.println("            <button type=\"button\" class=\"fa-meeting-item\" onclick=\"faSelectMeeting("
                                                + option.getActionNextId() + ")\">" + escapeHtml(option.getLabel())
                                                + "</button>");
                        }
                        out.println("          </div>");
                }

                out.println("          <h2 class=\"fa-section-title\">Previous Actions</h2>");
                if (previousActions.isEmpty()) {
                        out.println("          <div class=\"fa-next-action-text\">No previous actions.</div>");
                } else {
                        out.println("          <div class=\"fa-previous-actions\">");
                        for (PreviousActionOption option : previousActions) {
                                out.println("            <a class=\"fa-previous-item\" href=\"FocusedActionServlet?completingActionNextId="
                                                + option.getActionNextId() + "\">" + escapeHtml(option.getLabel())
                                                + "</a>");
                        }
                        out.println("          </div>");
                }

                out.println("          <h2 class=\"fa-section-title\">Next Action</h2>");
                if (nextActionHintId != null) {
                        out.println("          <a class=\"fa-next-action-link\" href=\"FocusedActionServlet?completingActionNextId="
                                        + nextActionHintId.intValue() + "\">" + escapeHtml(n(nextActionHint)) + "</a>");
                } else {
                        out.println("          <div class=\"fa-next-action-text\">" + escapeHtml(n(nextActionHint))
                                        + "</div>");
                }
                out.println("        </div>");

                out.println("      </div>");
                out.println("    </section>");

                out.println("    <section class=\"fa-right\">");
                out.println("      <div class=\"fa-clock-wrap\">");
                out.println("        <div class=\"fa-status-left-layer\" id=\"fa-status-left\">Actual<br>"
                                + formatMinutes(spentMinutes) + "</div>");
                out.println("        <div class=\"fa-status-right-layer\" id=\"fa-status-right\">Estimated<br>"
                                + formatMinutes(estimateMinutes) + "</div>");
                out.println(
                                "        <svg id=\"fa-clock\" class=\"fa-clock\" viewBox=\"0 0 760 760\" role=\"img\" aria-label=\"One-hour focus clock\"></svg>");
                out.println("        <div id=\"fa-dandelion-btn\" class=\"fa-dandelion-btn\"><img src=\"dandelion-button.png\" alt=\"Dandelion\" /></div>");
                out.println("        <div class=\"fa-controls\">");
                out.println(
                                "          <button id=\"fa-toggle-timer\" type=\"button\" class=\"fa-primary-btn fa-timer-btn\" onclick=\"faToggleTimer()\">"
                                                + (runningClock ? "Stop" : "Start") + "</button>");
                out.println("        </div>");
                out.println("        <div id=\"fa-time-widget\" class=\"fa-time-widget\">");
                out.println("        <div class=\"fa-time-label\">Today:</div>");
                out.println("        <div id=\"fa-today-time\" class=\"fa-time-value\">"
                                + formatMinutes(todayBillableMinutes)
                                + "</div>");
                out.println("        <div class=\"fa-time-bar\"><div id=\"fa-today-bar\" class=\"fa-time-bar-fill\"></div></div>");
                out.println("        <div class=\"fa-time-label\">Week:</div>");
                out.println("        <div id=\"fa-week-time\" class=\"fa-time-value\">"
                                + formatMinutes(spentMinutesThisWeek) + "</div>");
                out.println("        <div class=\"fa-time-bar\"><div id=\"fa-week-bar\" class=\"fa-time-bar-fill\"></div></div>");
                out.println("        </div>");
                out.println("      </div>");
                out.println("    </section>");
                out.println("  </div>");
                out.println("</div>");

                out.println("<script>");
                out.println("  var faState = {");
                out.println("    actionNextId: " + action.getActionNextId() + ",");
                out.println("    spentMinutes: " + Math.max(0, spentMinutes) + ",");
                out.println("    spentMinutesThisWeek: " + Math.max(0, spentMinutesThisWeek) + ",");
                out.println("    todayBillableMinutes: " + Math.max(0, todayBillableMinutes) + ",");
                out.println("    todayTargetMinutes: " + Math.max(1, todayTargetMinutes) + ",");
                out.println("    weekTargetMinutes: " + Math.max(1, weekTargetMinutes) + ",");
                out.println("    estimateMinutes: " + Math.max(0, estimateMinutes) + ",");
                out.println("    runningClock: " + (runningClock ? "true" : "false") + ",");
                out.println("    nowMinute: " + Math.max(0, Math.min(59, nowMinute)));
                out.println("  };");
                out.println("  var faEditOriginalDate = ''; ");
                out.println("  function faOpenModal(modalId) { var modal = document.getElementById(modalId); if (modal) { modal.classList.add('fa-modal-open'); } }");
                out.println("  function faCloseModal(modalId) { var modal = document.getElementById(modalId); if (modal) { modal.classList.remove('fa-modal-open'); } }");
                out.println("  function faCloseModalOnOverlay(evt, overlay) { if (evt && evt.target === overlay) { overlay.classList.remove('fa-modal-open'); } }");
                out.println("  function faCreateDashboardParams() { return new URLSearchParams(); }");
                out.println("  function faFetchDashboardJson(formData, contextLabel) {");
                out.println("    return fetch('DandelionDashboardServlet', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: formData.toString() })");
                out.println("      .then(function(response) {");
                out.println("        return response.text().then(function(text) {");
                out.println("          if (!response.ok) { throw new Error(contextLabel + ' HTTP ' + response.status + ': ' + text.substring(0, 400)); }");
                out.println("          try { return JSON.parse(text); } catch (parseError) { throw new Error(contextLabel + ' invalid JSON: ' + text.substring(0, 400)); }");
                out.println("        });");
                out.println("      });");
                out.println("  }");
                out.println("  function faGetTodayISO() { var d = new Date(); var y = d.getFullYear(); var m = String(d.getMonth() + 1).padStart(2, '0'); var day = String(d.getDate()).padStart(2, '0'); return y + '-' + m + '-' + day; }");
                out.println("  function faIsoToUserDate(isoDate) { var parts = (isoDate || '').split('-'); if (parts.length !== 3) { return ''; } return parts[1] + '/' + parts[2] + '/' + parts[0]; }");
                out.println("  function faUserDateToIso(userDate) { var parts = (userDate || '').split('/'); if (parts.length !== 3) { return ''; } var m = parts[0].trim().padStart(2, '0'); var d = parts[1].trim().padStart(2, '0'); var y = parts[2].trim(); if (y.length !== 4) { return ''; } return y + '-' + m + '-' + d; }");
                out.println("  function faHandleActionMutationSuccess(data) {");
                out.println("    if (data && data.requiresActionRefresh) { window.location.href = 'FocusedActionServlet'; return; }");
                out.println("    window.location.href = 'FocusedActionServlet?completingActionNextId=' + encodeURIComponent(String(faState.actionNextId));");
                out.println("  }");
                out.println("  function faOpenRescheduleModal(evt) {");
                out.println("    if (evt) { evt.preventDefault(); evt.stopPropagation(); }");
                out.println("    var dateInput = document.getElementById('faRescheduleDate');");
                out.println("    if (dateInput) { dateInput.setAttribute('min', faGetTodayISO()); }");
                out.println("    var formData = faCreateDashboardParams();");
                out.println("    formData.append('action', 'loadRescheduleData');");
                out.println("    formData.append('actionNextId', String(faState.actionNextId));");
                out.println("    faFetchDashboardJson(formData, 'Reschedule preload').then(function(data) {");
                out.println("      if (!data || !data.success) { alert('Unable to load reschedule form.'); return; }");
                out.println("      var loadedIso = faUserDateToIso(data.nextActionDate || '') || faGetTodayISO();");
                out.println("      if (dateInput) { dateInput.value = loadedIso; }");
                out.println("      faOpenModal('faRescheduleModal');");
                out.println("    }).catch(function() { alert('Unable to load reschedule form.'); });");
                out.println("  }");
                out.println("  function faSubmitReschedule() {");
                out.println("    var dateInput = document.getElementById('faRescheduleDate');");
                out.println("    var isoDate = dateInput ? dateInput.value : ''; ");
                out.println("    if (!isoDate) { alert('Please select a date.'); return; }");
                out.println("    var formData = faCreateDashboardParams();");
                out.println("    formData.append('action', 'rescheduleAction');");
                out.println("    formData.append('actionNextId', String(faState.actionNextId));");
                out.println("    formData.append('nextActionDate', faIsoToUserDate(isoDate));");
                out.println("    faFetchDashboardJson(formData, 'Reschedule submit').then(function(data) {");
                out.println("      if (!data || !data.success) { alert('Error: ' + (data && data.message ? data.message : 'Could not reschedule action')); return; }");
                out.println("      faCloseModal('faRescheduleModal');");
                out.println("      faHandleActionMutationSuccess(data);");
                out.println("    }).catch(function() { alert('Error rescheduling action. Please try again.'); });");
                out.println("  }");
                out.println("  function faOpenEditModal(evt) {");
                out.println("    if (evt) { evt.preventDefault(); evt.stopPropagation(); }");
                out.println("    var formData = faCreateDashboardParams();");
                out.println("    formData.append('action', 'loadActionData');");
                out.println("    formData.append('actionNextId', String(faState.actionNextId));");
                out.println("    faFetchDashboardJson(formData, 'Edit preload').then(function(data) {");
                out.println("      if (!data || !data.success) { alert('Unable to load edit form.'); return; }");
                out.println("      faEditOriginalDate = data.nextActionDate || ''; ");
                out.println("      document.getElementById('faEditActionDate').value = data.nextActionDate || ''; ");
                out.println("      document.getElementById('faEditActionDescription').value = data.nextDescription || ''; ");
                out.println("      document.getElementById('faEditActionTime').value = data.nextTimeEstimate || '0'; ");
                out.println("      document.getElementById('faEditActionType').value = data.nextActionType || ''; ");
                out.println("      document.getElementById('faEditActionTarget').value = data.nextTargetDate || ''; ");
                out.println("      document.getElementById('faEditActionDeadline').value = data.nextDeadlineDate || ''; ");
                out.println("      document.getElementById('faEditActionLink').value = data.linkUrl || ''; ");
                out.println("      document.getElementById('faEditActionNote').value = data.nextNote || ''; ");
                out.println("      faOpenModal('faEditModal');");
                out.println("    }).catch(function() { alert('Unable to load edit form.'); });");
                out.println("  }");
                out.println("  function faSubmitEdit(saveMode) {");
                out.println("    var dateValue = (document.getElementById('faEditActionDate').value || '').trim();");
                out.println("    if (!dateValue && faEditOriginalDate) { dateValue = faEditOriginalDate; }");
                out.println("    var formData = faCreateDashboardParams();");
                out.println("    formData.append('action', 'editAction');");
                out.println("    formData.append('actionNextId', String(faState.actionNextId));");
                out.println("    formData.append('nextActionDate', dateValue);");
                out.println("    formData.append('nextActionType', (document.getElementById('faEditActionType').value || '').trim());");
                out.println("    formData.append('nextDescription', document.getElementById('faEditActionDescription').value || '');");
                out.println("    formData.append('nextTimeEstimate', (document.getElementById('faEditActionTime').value || '0').trim());");
                out.println("    formData.append('nextTargetDate', (document.getElementById('faEditActionTarget').value || '').trim());");
                out.println("    formData.append('nextDeadlineDate', (document.getElementById('faEditActionDeadline').value || '').trim());");
                out.println("    formData.append('linkUrl', (document.getElementById('faEditActionLink').value || '').trim());");
                out.println("    formData.append('nextNote', document.getElementById('faEditActionNote').value || '');");
                out.println("    formData.append('saveMode', saveMode || 'save');");
                out.println("    faFetchDashboardJson(formData, 'Edit submit').then(function(data) {");
                out.println("      if (!data || !data.success) { alert('Error saving action: ' + (data && data.message ? data.message : 'Unknown error')); return; }");
                out.println("      faCloseModal('faEditModal');");
                out.println("      faHandleActionMutationSuccess(data);");
                out.println("    }).catch(function() { alert('Error submitting edit. Please try again.'); });");
                out.println("  }");
                out.println("  function faNormalizeMinute(value) { var v = value % 60; if (v < 0) { v += 60; } return v; }");
                out.println(
                                "  function faMinuteToAngle(minute) { return (faNormalizeMinute(minute) / 60) * Math.PI * 2 - (Math.PI / 2); }");
                out.println("  function faPolar(cx, cy, r, minute) {");
                out.println("    var angle = faMinuteToAngle(minute);");
                out.println("    return { x: cx + (Math.cos(angle) * r), y: cy + (Math.sin(angle) * r) }; }");
                out.println(
                                "  function faSvgEl(name) { return document.createElementNS('http://www.w3.org/2000/svg', name); }");
                out.println("  function faAddFullCircle(svg, cx, cy, radius, color) {");
                out.println(
                                "    var c = faSvgEl('circle'); c.setAttribute('cx', cx); c.setAttribute('cy', cy); c.setAttribute('r', radius); c.setAttribute('fill', color); svg.appendChild(c); }");
                out.println("  function faAddSector(svg, cx, cy, radius, startMinute, length, color) {");
                out.println("    if (length <= 0) { return; }");
                out.println("    if (length >= 60) { faAddFullCircle(svg, cx, cy, radius, color); return; }");
                out.println("    var remaining = length;");
                out.println("    var cursor = startMinute;");
                out.println("    while (remaining > 0) {");
                out.println("      var startN = faNormalizeMinute(cursor);");
                out.println("      var untilWrap = 60 - startN;");
                out.println("      var segmentLen = Math.min(remaining, untilWrap);");
                out.println("      var endN = faNormalizeMinute(startN + segmentLen);");
                out.println("      var p0 = faPolar(cx, cy, radius, startN);");
                out.println("      var p1 = faPolar(cx, cy, radius, endN);");
                out.println("      var largeArc = segmentLen > 30 ? 1 : 0;");
                out.println("      var path = faSvgEl('path');");
                out.println(
                                "      path.setAttribute('d', 'M ' + cx + ' ' + cy + ' L ' + p0.x + ' ' + p0.y + ' A ' + radius + ' ' + radius + ' 0 ' + largeArc + ' 1 ' + p1.x + ' ' + p1.y + ' Z');");
                out.println("      path.setAttribute('fill', color);");
                out.println("      svg.appendChild(path);");
                out.println("      remaining -= segmentLen;");
                out.println("      cursor += segmentLen;");
                out.println("    }");
                out.println("  }");
                out.println("  function faDrawFace(svg, cx, cy, radius, palette) {");
                out.println(
                                "    var ring = faSvgEl('circle'); ring.setAttribute('cx', cx); ring.setAttribute('cy', cy); ring.setAttribute('r', radius); ring.setAttribute('fill', 'none'); ring.setAttribute('stroke', palette.ring); ring.setAttribute('stroke-width', '5'); svg.appendChild(ring);");
                out.println("    for (var minute = 0; minute < 60; minute++) {");
                out.println("      var isMajor = minute % 5 === 0;");
                out.println("      var outer = faPolar(cx, cy, radius - 2, minute);");
                out.println("      var inner = faPolar(cx, cy, radius - (isMajor ? 22 : 13), minute);");
                out.println("      var tick = faSvgEl('line');");
                out.println(
                                "      tick.setAttribute('x1', outer.x); tick.setAttribute('y1', outer.y); tick.setAttribute('x2', inner.x); tick.setAttribute('y2', inner.y);");
                out.println("      tick.setAttribute('stroke', isMajor ? palette.majorTick : palette.minorTick);");
                out.println("      tick.setAttribute('stroke-width', isMajor ? 2.6 : 1.2);");
                out.println("      svg.appendChild(tick);");
                out.println("    }");
                out.println("    for (var marker = 0; marker < 60; marker += 5) {");
                out.println("      var labelP = faPolar(cx, cy, radius - 44, marker);");
                out.println("      var t = faSvgEl('text');");
                out.println(
                                "      t.setAttribute('x', labelP.x); t.setAttribute('y', labelP.y + 5); t.setAttribute('text-anchor', 'middle'); t.setAttribute('font-size', '17'); t.setAttribute('fill', palette.label); t.textContent = String(marker);");
                out.println("      svg.appendChild(t);");
                out.println("    }");
                out.println("  }");
                out.println("  function faDrawMinuteHand(svg, cx, cy, radius, minute, palette) {");
                out.println("    var handEnd = faPolar(cx, cy, radius * 0.70, minute);");
                out.println("    var handAngle = faMinuteToAngle(minute);");
                out.println("    var hand = faSvgEl('line');");
                out.println("    hand.setAttribute('x1', cx); hand.setAttribute('y1', cy);");
                out.println("    hand.setAttribute('x2', handEnd.x); hand.setAttribute('y2', handEnd.y);");
                out.println("    hand.setAttribute('stroke', palette.hand);");
                out.println("    hand.setAttribute('stroke-width', '9');");
                out.println("    hand.setAttribute('stroke-linecap', 'round');");
                out.println("    svg.appendChild(hand);");
                out.println("    var wingLength = 26;");
                out.println("    var wingSpread = Math.PI / 6;");
                out.println("    var wing1 = { x: handEnd.x + Math.cos(handAngle + Math.PI - wingSpread) * wingLength, y: handEnd.y + Math.sin(handAngle + Math.PI - wingSpread) * wingLength };");
                out.println("    var wing2 = { x: handEnd.x + Math.cos(handAngle + Math.PI + wingSpread) * wingLength, y: handEnd.y + Math.sin(handAngle + Math.PI + wingSpread) * wingLength };");
                out.println("    var arrow1 = faSvgEl('line');");
                out.println("    arrow1.setAttribute('x1', handEnd.x); arrow1.setAttribute('y1', handEnd.y);");
                out.println("    arrow1.setAttribute('x2', wing1.x); arrow1.setAttribute('y2', wing1.y);");
                out.println("    arrow1.setAttribute('stroke', palette.hand);");
                out.println("    arrow1.setAttribute('stroke-width', '9');");
                out.println("    arrow1.setAttribute('stroke-linecap', 'round');");
                out.println("    svg.appendChild(arrow1);");
                out.println("    var arrow2 = faSvgEl('line');");
                out.println("    arrow2.setAttribute('x1', handEnd.x); arrow2.setAttribute('y1', handEnd.y);");
                out.println("    arrow2.setAttribute('x2', wing2.x); arrow2.setAttribute('y2', wing2.y);");
                out.println("    arrow2.setAttribute('stroke', palette.hand);");
                out.println("    arrow2.setAttribute('stroke-width', '9');");
                out.println("    arrow2.setAttribute('stroke-linecap', 'round');");
                out.println("    svg.appendChild(arrow2);");
                out.println("  }");
                out.println("  function faDrawClock() {");
                out.println("    var svg = document.getElementById('fa-clock'); if (!svg) { return; }");
                out.println("    while (svg.firstChild) { svg.removeChild(svg.firstChild); }");
                out.println("    var cx = 380; var cy = 380; var radius = 300;");
                out.println("    var running = !!faState.runningClock;");
                out.println("    var palette = running ? {");
                out.println(
                                "      background: '#ffffff', spent: '#d9f0d2', remaining: '#2f8c2f', overrun: '#cb2c1f', ring: '#2e342e', minorTick: '#9ba59a', majorTick: '#4d5a4d', label: '#2d3a2d', hand: '#2e342e' }");
                out.println(
                                "      : { background: '#f3f3f3', spent: '#dbdbdb', remaining: '#c7c7c7', overrun: '#bdbdbd', ring: '#8a8a8a', minorTick: '#b5b5b5', majorTick: '#979797', label: '#7a7a7a', hand: '#949494' };");
                out.println("    faAddFullCircle(svg, cx, cy, radius, palette.background);");
                out.println("    var nowMinute = faNormalizeMinute(faState.nowMinute || 0);");
                out.println("    var spent = Math.max(0, Math.min(60, faState.spentMinutes || 0));");
                out.println("    var estimate = Math.max(0, faState.estimateMinutes || 0);");
                out.println("    var remaining = Math.max(0, estimate - (faState.spentMinutes || 0));");
                out.println("    var overrun = Math.max(0, (faState.spentMinutes || 0) - estimate);");
                out.println("    faAddSector(svg, cx, cy, radius - 6, nowMinute - spent, spent, palette.spent);");
                out.println("    faAddSector(svg, cx, cy, radius - 6, nowMinute, Math.min(60, remaining), palette.remaining);");
                out.println(
                                "    faAddSector(svg, cx, cy, radius - 6, nowMinute - overrun, Math.min(60, overrun), palette.overrun);");
                out.println("    faDrawFace(svg, cx, cy, radius, palette);");
                out.println("    faDrawMinuteHand(svg, cx, cy, radius, nowMinute, palette);");
                out.println("  }");
                out.println("  function faHandleNoteKeydown(event, actionNextId) {");
                out.println("    if (event.key === 'Enter') {");
                out.println("      event.preventDefault();");
                out.println("      faSubmitNote(actionNextId);");
                out.println("    }");
                out.println("  }");
                out.println("  function faSelectMeeting(meetingActionNextId) {");
                out.println("    var body = new URLSearchParams();");
                out.println("    body.append('action', 'selectMeetingAction');");
                out.println("    body.append('meetingActionNextId', String(meetingActionNextId));");
                out.println(
                                "    fetch('FocusedActionServlet', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: body.toString() })");
                out.println("      .then(function(response) { return response.json(); })");
                out.println(
                                "      .then(function(data) { if (data && data.success) { window.location.href='FocusedActionServlet?completingActionNextId=' + encodeURIComponent(String(meetingActionNextId)); } else { alert('Unable to select meeting.'); } })");
                out.println("      .catch(function() { alert('Unable to select meeting.'); });");
                out.println("  }");
                out.println("  function faSubmitNote(actionNextId) {");
                out.println("    var input = document.getElementById('fa-note-input');");
                out.println("    if (!input || !input.value || input.value.trim().length === 0) { return; }");
                out.println(
                                "    var body = new URLSearchParams(); body.append('action', 'addCurrentActionNote'); body.append('actionNextId', actionNextId); body.append('nextNote', input.value.trim());");
                out.println(
                                "    fetch('FocusedActionServlet', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: body.toString() })");
                out.println("      .then(function(response) { return response.json(); })");
                out.println("      .then(function(data) {");
                out.println("        if (!data || !data.success) { alert('Unable to add note.'); return; }");
                out.println("        var list = document.getElementById('fa-notes-list'); if (!list) { return; }");
                out.println("        list.innerHTML = '';");
                out.println("        var notes = data.notes || [];");
                out.println(
                                "        for (var i = 0; i < notes.length; i++) { var li = document.createElement('li'); li.textContent = notes[i]; list.appendChild(li); }");
                out.println("        input.value = ''; input.focus();");
                out.println("      })");
                out.println("      .catch(function() { alert('Unable to add note.'); });");
                out.println("  }");
                out.println("  function faInitWorkFollowUpSuggestions() {");
                out.println(
                                "    var actionVerbs = ['I will', 'I have committed', 'I might', 'I would like to', 'I will meet', 'I have set goal to', 'I am waiting'];");
                out.println("    var input = document.getElementById('workFollowUpInput');");
                out.println("    var suggestionsBox = document.getElementById('workFollowUpSuggestions');");
                out.println("    if (!input || !suggestionsBox) { return; }");
                out.println("    var currentSuggestions = []; var selectedIndex = -1;");
                out.println(
                                "    function showSuggestions(suggestions) { suggestionsBox.innerHTML = ''; suggestionsBox.style.display = suggestions.length ? 'block' : 'none'; for (var i=0;i<suggestions.length;i++) { var suggestion = suggestions[i]; var div = document.createElement('div'); div.textContent = suggestion; if (i === selectedIndex) { div.style.backgroundColor = '#e0e0e0'; } div.addEventListener('click', function(evt){ var text = evt.target.textContent || ''; input.value = text + ' '; suggestionsBox.style.display = 'none'; selectedIndex = -1; input.focus(); }); suggestionsBox.appendChild(div); } }");
                out.println(
                                "    input.addEventListener('input', function(){ var text = (input.value || '').trim(); if (text.length === 0) { currentSuggestions = actionVerbs.slice(0); selectedIndex = -1; showSuggestions(currentSuggestions); return; } var suggestions = actionVerbs.filter(function(verb){ return verb.toLowerCase().startsWith(text.toLowerCase()); }); currentSuggestions = suggestions; selectedIndex = -1; showSuggestions(suggestions); });");
                out.println(
                                "    input.addEventListener('focus', function(){ var text = (input.value || '').trim(); if (text.length === 0) { currentSuggestions = actionVerbs.slice(0); selectedIndex = -1; showSuggestions(currentSuggestions); } });");
                out.println(
                                "    input.addEventListener('keydown', function(e){ var visible = suggestionsBox.style.display === 'block'; if (visible && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) { e.preventDefault(); var count = currentSuggestions.length; if (count === 0) { return; } if (e.key === 'ArrowDown') { selectedIndex = (selectedIndex + 1) % count; } else { selectedIndex = (selectedIndex - 1 + count) % count; } showSuggestions(currentSuggestions); } if (visible && (e.key === 'Enter' || e.key === 'Tab')) { if (selectedIndex < 0) { selectedIndex = 0; } if (selectedIndex < currentSuggestions.length) { e.preventDefault(); input.value = currentSuggestions[selectedIndex] + ' '; suggestionsBox.style.display = 'none'; selectedIndex = -1; } } if (e.key === 'Escape') { suggestionsBox.style.display = 'none'; selectedIndex = -1; } });");
                out.println(
                                "    input.addEventListener('blur', function(){ setTimeout(function(){ suggestionsBox.style.display = 'none'; }, 150); });");
                out.println("  }");
                out.println("  function faInitQuickCaptureSuggestions() {");
                out.print("    var projectNames = [");
                for (int i = 0; i < quickCaptureProjectNames.size(); i++) {
                        out.print("\"" + escapeJs(quickCaptureProjectNames.get(i)) + "\"");
                        if (i < quickCaptureProjectNames.size() - 1) {
                                out.print(", ");
                        }
                }
                out.println("]; ");
                out.println(
                                "    var actionVerbs = ['I will', 'I have committed', 'I might', 'I would like to', 'I will meet', 'I have set goal to', 'I am waiting'];");
                out.println("    var input = document.getElementById('sentenceInput');");
                out.println("    var suggestionsBox = document.getElementById('suggestions');");
                out.println("    if (!input || !suggestionsBox) { return; }");
                out.println("    var currentSuggestions = []; var selectedIndex = -1;");
                out.println("    input.addEventListener('input', function() {");
                out.println("      var text = input.value || ''; var colonIndex = text.indexOf(':'); var suggestions = [];");
                out.println("      if (colonIndex === -1) {");
                out.println(
                                "        suggestions = projectNames.filter(function(name) { return name.toLowerCase().startsWith(text.toLowerCase()); });");
                out.println("      } else {");
                out.println("        var beforeColon = text.substring(0, colonIndex).trim();");
                out.println("        var afterColon = text.substring(colonIndex + 1).trim();");
                out.println("        if (projectNames.indexOf(beforeColon) === -1) {");
                out.println(
                                "          suggestions = projectNames.filter(function(name) { return name.toLowerCase().indexOf(beforeColon.toLowerCase()) >= 0; });");
                out.println("        } else if (afterColon.length === 0) {");
                out.println("          suggestions = actionVerbs.slice(0);");
                out.println("        } else {");
                out.println(
                                "          suggestions = actionVerbs.filter(function(verb) { return verb.toLowerCase().startsWith(afterColon.toLowerCase()); });");
                out.println("        }");
                out.println("      }");
                out.println("      currentSuggestions = suggestions; selectedIndex = -1; faShowQuickCaptureSuggestions(suggestionsBox, suggestions, input, text, function(v){ selectedIndex = v; }, function(){ return selectedIndex; });");
                out.println("    });");
                out.println("    input.addEventListener('keydown', function(e) {");
                out.println("      var visible = suggestionsBox.style.display === 'block'; if (!visible) { return; }");
                out.println("      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {");
                out.println("        e.preventDefault(); var count = currentSuggestions.length; if (count === 0) { return; }");
                out.println("        if (e.key === 'ArrowDown') { selectedIndex = (selectedIndex + 1) % count; } else { selectedIndex = (selectedIndex - 1 + count) % count; }");
                out.println("        faShowQuickCaptureSuggestions(suggestionsBox, currentSuggestions, input, input.value, function(v){ selectedIndex = v; }, function(){ return selectedIndex; });");
                out.println("      }");
                out.println("      if (e.key === 'Enter' || e.key === 'Tab') {");
                out.println("        if (selectedIndex < 0) { selectedIndex = 0; }");
                out.println("        if (selectedIndex < currentSuggestions.length) { e.preventDefault(); faAcceptQuickCaptureSuggestion(input, currentSuggestions[selectedIndex], input.value); suggestionsBox.style.display = 'none'; selectedIndex = -1; }");
                out.println("      }");
                out.println("      if (e.key === 'Escape') { suggestionsBox.style.display = 'none'; selectedIndex = -1; }");
                out.println("    });");
                out.println("    input.addEventListener('blur', function(){ setTimeout(function(){ suggestionsBox.style.display = 'none'; }, 150); });");
                out.println("  }");
                out.println("  function faShowQuickCaptureSuggestions(box, suggestions, input, text, setSelectedIndex, getSelectedIndex) {");
                out.println("    box.innerHTML = ''; box.style.display = suggestions.length ? 'block' : 'none';");
                out.println("    suggestions.forEach(function(suggestion, i) {");
                out.println("      var div = document.createElement('div'); div.textContent = suggestion;");
                out.println("      if (i === getSelectedIndex()) { div.style.backgroundColor = '#e0e0e0'; }");
                out.println("      div.addEventListener('click', function() { faAcceptQuickCaptureSuggestion(input, suggestion, text); box.style.display = 'none'; setSelectedIndex(-1); });");
                out.println("      box.appendChild(div);");
                out.println("    });");
                out.println("  }");
                out.println("  function faAcceptQuickCaptureSuggestion(input, suggestion, text) {");
                out.println("    if ((text || '').indexOf(':') === -1) { input.value = suggestion + ': '; return; }");
                out.println("    var beforeColon = (text || '').split(':')[0]; input.value = beforeColon.trim() + ': ' + suggestion + ' '; ");
                out.println("  }");
                out.println("  function faToggleTimer() {");
                out.println(
                                "    var body = new URLSearchParams(); body.append('action', 'toggleTimer'); body.append('completingActionNextId', String(faState.actionNextId || 0));");
                out.println(
                                "    fetch('FocusedActionServlet', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: body.toString() })");
                out.println("      .then(function(response) { return response.json(); })");
                out.println(
                                "      .then(function(data) { if (!data || !data.success) { alert('Unable to toggle timer.'); return; } faState.runningClock = !!data.runningClock; faRefreshClock(); })");
                out.println("      .catch(function() { alert('Unable to toggle timer.'); });");
                out.println("  }");
                out.println("  function faRefreshClock() {");
                out.println(
                                "    var body = new URLSearchParams(); body.append('action', 'refreshFocusClock'); body.append('completingActionNextId', String(faState.actionNextId || 0));");
                out.println(
                                "    fetch('FocusedActionServlet', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: body.toString() })");
                out.println("      .then(function(response) { return response.json(); })");
                out.println("      .then(function(data) {");
                out.println("        if (!data || !data.success) { return; }");
                out.println("        faState.spentMinutes = parseInt(data.spentMinutes || 0, 10) || 0;");
                out.println("        faState.spentMinutesThisWeek = parseInt(data.spentMinutesThisWeek || 0, 10) || 0;");
                out.println("        faState.todayBillableMinutes = parseInt(data.todayBillableMinutes || 0, 10) || 0;");
                out.println("        faState.todayTargetMinutes = parseInt(data.todayTargetMinutes || 0, 10) || 0;");
                out.println("        faState.weekTargetMinutes = parseInt(data.weekTargetMinutes || 0, 10) || 0;");
                out.println("        faState.estimateMinutes = parseInt(data.estimateMinutes || 0, 10) || 0;");
                out.println("        faState.nowMinute = parseInt(data.nowMinute || 0, 10) || 0;");
                out.println("        faState.runningClock = !!data.runningClock;");
                out.println("        var toggle = document.getElementById('fa-toggle-timer');");
                out.println("        if (toggle) { toggle.textContent = faState.runningClock ? 'Stop' : 'Start'; }");
                out.println("        var todayEl = document.getElementById('fa-today-time');");
                out.println("        if (todayEl) { todayEl.textContent = faFormatMinutes(faState.todayBillableMinutes); }");
                out.println("        var weekEl = document.getElementById('fa-week-time');");
                out.println("        if (weekEl) { weekEl.textContent = faFormatMinutes(faState.spentMinutesThisWeek); }");
                out.println("        faUpdateStatusAndGauge();");
                out.println("        faDrawClock();");
                out.println("      });");
                out.println("  }");
                out.println("  function faClampPct(v) { return Math.max(0, Math.min(100, v)); }");
                out.println("  function faUpdateStatusAndGauge() {");
                out.println("    var statusLeftEl = document.getElementById('fa-status-left');");
                out.println("    var statusRightEl = document.getElementById('fa-status-right');");
                out.println("    var spent = Math.max(0, faState.spentMinutes || 0);");
                out.println("    var estimate = Math.max(0, faState.estimateMinutes || 0);");
                out.println("    var overrun = Math.max(0, spent - estimate);");
                out.println("    if (statusLeftEl && statusRightEl) {");
                out.println("      if (overrun > 0) {");
                out.println("        statusLeftEl.innerHTML = 'Over<br>' + faFormatMinutes(overrun);");
                out.println("        statusLeftEl.style.color = '#cb2c1f';");
                out.println("      } else {");
                out.println("        statusLeftEl.innerHTML = 'Actual<br>' + faFormatMinutes(spent);");
                out.println("        statusLeftEl.style.color = '#263226';");
                out.println("      }");
                out.println("      statusRightEl.innerHTML = 'Estimated<br>' + faFormatMinutes(estimate);");
                out.println("    }");
                out.println("    var todayBase = Math.max(1, parseInt(faState.todayTargetMinutes || 0, 10) || (8 * 60));");
                out.println("    var weekBase = Math.max(1, parseInt(faState.weekTargetMinutes || 0, 10) || Math.round(37.5 * 60));");
                out.println("    var todayPct = faClampPct(((Math.max(0, faState.todayBillableMinutes || 0)) / todayBase) * 100);");
                out.println("    var weekPct = faClampPct(((Math.max(0, faState.spentMinutesThisWeek || 0)) / weekBase) * 100);");
                out.println("    var todayBar = document.getElementById('fa-today-bar');");
                out.println("    var weekBar = document.getElementById('fa-week-bar');");
                out.println("    if (todayBar) { todayBar.style.width = todayPct.toFixed(1) + '%'; }");
                out.println("    if (weekBar) { weekBar.style.width = weekPct.toFixed(1) + '%'; }");
                out.println("  }");
                out.println("  function faFormatMinutes(minutes) {");
                out.println("    var safe = Math.max(0, minutes);");
                out.println("    var h = Math.floor(safe / 60);");
                out.println("    var m = safe % 60;");
                out.println("    if (h <= 0) { return m + 'm'; }");
                out.println("    if (m == 0) { return h + 'h'; }");
                out.println("    return h + 'h ' + m + 'm';");
                out.println("  }");
                out.println("  faUpdateStatusAndGauge();");
                out.println("  faDrawClock();");
                out.println("  faInitWorkFollowUpSuggestions();");
                out.println("  faInitQuickCaptureSuggestions();");
                out.println("  window.setInterval(function(){ faRefreshClock(); }, 60000);");
                out.println("</script>");
        }

        private void printStyles(PrintWriter out) {
                out.println("<style>");
                out.println(
                                "  .fa-page { height: 100vh; overflow: hidden; background: linear-gradient(160deg, #f6f8f5 0%, #edf2ec 48%, #f8fbf7 100%); box-sizing: border-box; padding: 8px 10px 10px 10px; position: relative; }");
                out.println(
                                "  .fa-close { position: fixed; right: 14px; top: 10px; border: 1px solid #7f8f7f; background: #fff; width: 38px; height: 38px; border-radius: 8px; font-size: 26px; line-height: 30px; cursor: pointer; z-index: 60; }");
                out.println(
                                "  .fa-shell { display: grid; grid-template-columns: minmax(380px, 39%) minmax(520px, 61%); gap: 12px; height: calc(100vh - 20px); }");
                out.println("  .fa-left { min-height: 0; }");
                out.println("  .fa-left-scroll { height: 100%; overflow-y: auto; padding-right: 6px; }");
                out.println(
                                "  .fa-right { min-height: 0; display: flex; flex-direction: column; justify-content: center; align-items: center; position: relative; }");
                out.println("  .fa-clock-wrap { width: min(82vh, 760px); max-width: 100%; position: relative; }");
                out.println("  .fa-clock { width: 100%; height: auto; }");
                out.println("  .fa-status-left-layer, .fa-status-right-layer { position: absolute; z-index: 7; font-size: 30px; font-weight: 700; text-shadow: 0 1px 1px rgba(255,255,255,0.55); width: 180px; line-height: 1.1; text-align: center; pointer-events: none; }");
                out.println("  .fa-status-left-layer { left: 15%; top: 16%; transform: translate(-50%, -50%); color: #263226; text-align: left; }");
                out.println("  .fa-status-right-layer { left: 85%; top: 16%; transform: translate(-50%, -50%); color: #2f8c2f; text-align: right; }");
                out.println("  .fa-dandelion-btn { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 106px; height: 106px; border-radius: 50%; border: 3px solid #1f2f1f; background-color: #f0f0f0; z-index: 8; display: flex; align-items: center; justify-content: center; overflow: hidden; }");
                out.println("  .fa-dandelion-btn img { width: 90%; height: 90%; object-fit: contain; display: block; }");
                out.println("  .fa-time-widget { position: absolute; left: 95%; top: 84%; transform: translate(-50%, -50%); background: rgba(255,255,255,0.95); border: 2px solid #2a6f2a; border-radius: 12px; padding: 6px 8px; display: grid; grid-template-columns: auto auto 72px; gap: 4px 6px; align-items: center; font-weight: 600; font-size: 12px; z-index: 10; width: 228px; }");
                out.println("  .fa-time-label { color: #555; text-align: right; margin: 0; }");
                out.println("  .fa-time-value { color: #2f8c2f; font-size: 16px; }");
                out.println("  .fa-time-bar { height: 10px; border-radius: 999px; background: #d8dfd8; overflow: hidden; border: 1px solid #b3beb3; }");
                out.println("  .fa-time-bar-fill { height: 100%; width: 0; background: linear-gradient(90deg, #2f8c2f 0%, #6fb56f 100%); transition: width 0.3s ease; }");
                out.println("  .fa-controls { position: absolute; left: 7%; top: 84%; transform: translate(-50%, -50%); z-index: 10; text-align: left; }");
                out.println(
                                "  .fa-card { background: #ffffffd4; border: 1px solid #ced8cb; border-radius: 10px; padding: 12px; margin-bottom: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }");
                out.println("  .fa-title-card { border: 2px solid #2a6f2a; }");
                out.println("  .fa-main-title { font-size: 36px; font-weight: 700; color: #1f2f1f; line-height: 1.3; margin-bottom: 8px; }");
                out.println("  .fa-main-title i { font-style: italic; }");
                out.println("  .fa-project-subtitle { font-size: 14px; letter-spacing: .02em; text-transform: uppercase; color: #4a6350; }");
                out.println("  .fa-main-title .fa-project-subtitle { margin-left: 10px; vertical-align: baseline; }");
                out.println(
                                "  .fa-secondary-btn { border: 1px solid #7f9b7f; background: #eef6ec; color: #274127; padding: 8px 12px; border-radius: 7px; cursor: pointer; }");
                out.println(
                                "  .fa-primary-btn { border: 1px solid #2a6f2a; background: #2f8c2f; color: #fff; padding: 8px 14px; border-radius: 7px; cursor: pointer; font-size: 14px; font-weight: 500; }");
                out.println("  .fa-timer-btn { padding: 16px 32px; border-radius: 10px; font-size: 18px; font-weight: 700; min-width: 140px; }");
                out.println("  .fa-section-title { margin: 0 0 10px 0; font-size: 18px; color: #293829; }");
                out.println("  .fa-notes { margin: 8px 0 10px 18px; padding: 0; }");
                out.println("  .fa-notes li { margin-bottom: 6px; white-space: pre-wrap; word-break: break-word; }");
                out.println("  .fa-inline-note textarea { width: 100%; box-sizing: border-box; margin-bottom: 8px; padding: 8px; border: 1px solid #bbb; border-radius: 4px; font-family: inherit; }");
                out.println("  .fa-note-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }");
                out.println("  .fa-note-submit-btn { width: auto; }");
                out.println("  .fa-work-progress-label { margin-bottom: 6px; color: #324532; font-size: 16px; font-weight: 700; }");
                out.println("  .fa-work-progress-input { width: 100%; box-sizing: border-box; font-size: 18px; padding: 8px 10px; border: 1px solid #cfbea6; background: #fffdf8; }");
                out.println("  .fa-status-row { display: flex; gap: 10px; margin: 10px 0; flex-wrap: wrap; }");
                out.println("  .fa-work-status-toggle input[type='radio'] { position: absolute; opacity: 0; pointer-events: none; }");
                out.println("  .fa-work-status-toggle label { display: inline-block; padding: 6px 10px; border: 1px solid #d5d5d5; background: #efefef; color: #5e5e5e; border-radius: 4px; cursor: pointer; font-weight: bold; user-select: none; }");
                out.println("  .fa-work-status-toggle input[type='radio']:checked + label { border: 2px solid #58734f; background: #e6f1df; color: #23361e; padding: 5px 9px; }");
                out.println("  .fa-next-row { display: flex; align-items: center; gap: 8px; }");
                out.println("  .fa-next-action-text { font-size: 15px; color: #233423; min-height: 24px; }");
                out.println("  .fa-next-action-link { display: inline-block; width: 100%; box-sizing: border-box; border: 1px solid #7f9b7f; background: #eef6ec; color: #274127; padding: 10px 12px; border-radius: 7px; text-decoration: none; }");
                out.println("  .fa-next-action-link:hover { background: #dde9db; }");
                out.println("  .fa-meetings-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 10px; }");
                out.println("  .fa-meeting-item { border: 1px solid #7f9b7f; background: #eef6ec; color: #274127; padding: 10px 12px; border-radius: 7px; cursor: pointer; text-align: left; }");
                out.println("  .fa-meeting-item:hover { background: #dde9db; }");
                out.println("  .fa-previous-actions { display: flex; flex-direction: column; gap: 8px; margin-bottom: 10px; }");
                out.println("  .fa-previous-item { display: inline-block; width: 100%; box-sizing: border-box; border: 1px solid #d6dfd4; background: #f6faf5; color: #274127; padding: 10px 12px; border-radius: 7px; text-decoration: none; }");
                out.println("  .fa-previous-item:hover { background: #edf5ea; }");
                out.println("  .fa-capture-input-container { position: relative; display: inline-block; }");
                out.println("  .fa-capture-suggestions { position: absolute; left: 0; right: 0; top: calc(100% + 2px); z-index: 40; display: none; background: #fffdf8; border: 1px solid #d9ccb8; box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08); max-height: 220px; overflow-y: auto; }");
                out.println("  .fa-capture-suggestions div { padding: 6px 8px; cursor: pointer; }");
                out.println("  .fa-capture-suggestions div:hover { background: #efe7db; }");
                out.println("  .fa-quick-capture-box { position: absolute; right: 58px; top: 10px; z-index: 50; min-width: 360px; max-width: 520px; padding: 10px 12px; background: #f8f1e6; border: 1px solid #d7c8b1; border-radius: 6px; box-shadow: 0 4px 12px rgba(0,0,0,0.10); }");
                out.println("  .fa-quick-capture-title { font-size: 12px; font-weight: bold; letter-spacing: .04em; text-transform: uppercase; color: #52614d; margin-bottom: 8px; }");
                out.println("  .fa-capture-form { margin: 0; }");
                out.println("  .fa-capture-row { display: flex; align-items: center; gap: 8px; }");
                out.println("  .fa-capture-row #sentenceInput { width: 100%; box-sizing: border-box; padding: 8px; border: 1px solid #d3c2aa; background: #fffaf1; }");
                out.println("  .fa-capture-actions { display: flex; gap: 8px; white-space: nowrap; }");
                out.println("  .fa-qc-start-btn { border: 1px solid #7a9b7a; background: #d6ecd6; color: #1f3a1f; padding: 6px 10px; cursor: pointer; }");
                out.println("  .fa-empty-clock { color: #7d7d7d; font-size: 20px; }");
                out.println("  .fa-modal-overlay { position: fixed; inset: 0; background: rgba(30, 40, 30, 0.35); display: none; align-items: center; justify-content: center; z-index: 90; }");
                out.println("  .fa-modal-overlay.fa-modal-open { display: flex; }");
                out.println("  .fa-modal { width: min(92vw, 760px); max-height: 84vh; overflow-y: auto; background: #fffdf8; border: 1px solid #c8d4c4; border-radius: 10px; box-shadow: 0 10px 30px rgba(0,0,0,0.22); }");
                out.println("  .fa-reschedule-modal { width: min(92vw, 420px); }");
                out.println("  .fa-modal-head { display: flex; align-items: center; justify-content: space-between; padding: 12px 14px; border-bottom: 1px solid #d8e0d6; }");
                out.println("  .fa-modal-title { margin: 0; font-size: 18px; color: #243624; }");
                out.println("  .fa-modal-close { border: 1px solid #8ca08c; background: #f5faf4; color: #274127; width: 34px; height: 34px; border-radius: 7px; font-size: 24px; line-height: 28px; cursor: pointer; }");
                out.println("  .fa-modal-body { padding: 14px; }");
                out.println("  .fa-modal-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }");
                out.println("  .fa-modal-field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }");
                out.println("  .fa-modal-field label { font-size: 12px; font-weight: 700; color: #314631; letter-spacing: .02em; text-transform: uppercase; }");
                out.println("  .fa-modal-field input, .fa-modal-field textarea { width: 100%; box-sizing: border-box; border: 1px solid #bfcdbb; border-radius: 6px; padding: 8px 9px; font-family: inherit; font-size: 14px; background: #fcfffb; }");
                out.println("  .fa-modal-actions { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 6px; }");
                out.println("  @media (max-width: 900px) {");
                out.println("    .fa-shell { grid-template-columns: 1fr; }");
                out.println("    .fa-left-scroll { height: auto; max-height: none; }");
                out.println("    .fa-right { min-height: 420px; }");
                out.println("    .fa-clock-wrap { width: min(92vw, 620px); }");
                out.println("    .fa-dandelion-btn { width: 78px; height: 78px; }");
                out.println("    .fa-status-left-layer, .fa-status-right-layer { font-size: 18px; width: 120px; }");
                out.println("    .fa-status-left-layer { left: 16%; top: 18%; }");
                out.println("    .fa-status-right-layer { left: 84%; top: 18%; }");
                out.println("    .fa-controls { left: 11%; top: 82%; }");
                out.println("    .fa-time-widget { left: 90%; top: 82%; width: min(52vw, 236px); grid-template-columns: auto auto 1fr; }");
                out.println("    .fa-timer-btn { min-width: 112px; padding: 12px 20px; font-size: 16px; }");
                out.println("    .fa-quick-capture-box { position: static; min-width: 0; max-width: none; width: 100%; margin: 0 0 8px 0; }");
                out.println("    .fa-modal-row { grid-template-columns: 1fr; }");
                out.println("  }");
                out.println("</style>");
        }

        private String escapeHtml(String value) {
                if (value == null || value.length() == 0) {
                        return "";
                }
                StringBuilder sb = new StringBuilder(value.length() + 16);
                for (int i = 0; i < value.length(); i++) {
                        char ch = value.charAt(i);
                        if (ch == '&') {
                                sb.append("&amp;");
                        } else if (ch == '<') {
                                sb.append("&lt;");
                        } else if (ch == '>') {
                                sb.append("&gt;");
                        } else if (ch == '\"') {
                                sb.append("&quot;");
                        } else if (ch == '\'') {
                                sb.append("&#39;");
                        } else {
                                sb.append(ch);
                        }
                }
                return sb.toString();
        }

        private String escapeJs(String value) {
                if (value == null || value.length() == 0) {
                        return "";
                }
                return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }

        private String n(String value) {
                return value == null ? "" : value;
        }

        private String formatMinutes(int minutes) {
                int safe = Math.max(0, minutes);
                int h = safe / 60;
                int m = safe % 60;
                if (h <= 0) {
                        return m + "m";
                }
                if (m == 0) {
                        return h + "h";
                }
                return h + "h " + m + "m";
        }
}
