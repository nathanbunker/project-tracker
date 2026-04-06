package org.dandeliondaily.focus.render;

import java.io.PrintWriter;
import java.util.List;

import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;

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

    public void render(AppReq appReq, ProjectActionNext action, List<String> notes, List<MeetingOption> meetingOptions,
            int spentMinutes, int estimateMinutes, boolean runningClock, int nowMinute, int spentMinutesThisWeek) {
        PrintWriter out = appReq.getOut();
        printStyles(out);

        out.println("<div class=\"fa-page\">");
        out.println(
                "  <button class=\"fa-close\" title=\"Back to Dandelion Dashboard\" onclick=\"window.location.href='DandelionDashboardServlet';\">&times;</button>");
        out.println("  <div class=\"fa-shell\">");
        out.println("    <section class=\"fa-left\">");
        out.println("      <div class=\"fa-left-scroll\">");

        if (action == null) {
            out.println("        <h1 class=\"fa-title\">No current action selected</h1>");
            out.println("        <p class=\"fa-subtitle\">Return to dashboard to choose an action.</p>");
            out.println("      </div>");
            out.println("    </section>");
            out.println("    <section class=\"fa-right\">" + "<div class=\"fa-empty-clock\">No timer context</div>"
                    + "</section>");
            out.println("  </div>");
            out.println("</div>");
            return;
        }

        String projectName = action.getProject() == null ? "" : n(action.getProject().getProjectName());
        String descriptionForDisplay = action.getNextDescriptionForDisplay(null);

        out.println("        <div class=\"fa-card fa-title-card\">");
        out.println("          <div class=\"fa-main-title\">" + descriptionForDisplay + "</div>");
        out.println("          <div class=\"fa-project-subtitle\">" + escapeHtml(projectName) + "</div>");
        out.println("          <div class=\"fa-secondary-actions\">");
        out.println(
                "            <button type=\"button\" class=\"fa-secondary-btn\" onclick=\"window.location.href='DandelionDashboardServlet?action=SelectAction&completingActionNextId="
                        + action.getActionNextId() + "';\">Reschedule</button>");
        out.println(
                "            <button type=\"button\" class=\"fa-secondary-btn\" onclick=\"window.location.href='DandelionDashboardServlet?action=SelectAction&completingActionNextId="
                        + action.getActionNextId() + "';\">Edit Action</button>");
        out.println("          </div>");
        out.println("        </div>");

        out.println("        <div class=\"fa-card\">");
        out.println("          <h2 class=\"fa-section-title\">Notes</h2>");
        out.println("          <ul id=\"fa-notes-list\" class=\"fa-notes\">");
        if (notes.isEmpty()) {
            out.println("            <li class=\"fa-empty\">No notes yet.</li>");
        } else {
            for (String note : notes) {
                out.println("            <li>" + escapeHtml(note) + "</li>");
            }
        }
        out.println("          </ul>");
        out.println("          <div class=\"fa-inline-note\">");
        out.println("            <textarea id=\"fa-note-input\" rows=\"4\" placeholder=\"Add note...\" onkeydown=\"faHandleNoteKeydown(event, " 
                + action.getActionNextId() + ")\"></textarea>");
        out.println("          </div>");
        out.println("        </div>");

        out.println("        <div class=\"fa-card\">");
        out.println("          <h2 class=\"fa-section-title\">Work</h2>");
        out.println("          <form method=\"POST\" action=\"FocusedActionServlet\" class=\"fa-work-form\">");
        out.println("            <div class=\"fa-field-label\">What action was taken:</div>");
        out.println("            <input type=\"text\" name=\"nextSummary\" value=\""
                + escapeHtml(n(action.getNextSummary()))
                + "\" style=\"width:100%;\" autofocus />");
        out.println("            <div class=\"fa-status-row\">");
        out.println(
                "              <label><input type=\"radio\" name=\"workStatus\" value=\"IN_PROGRESS\"/> In Progress</label>");
        out.println(
                "              <label><input type=\"radio\" name=\"workStatus\" value=\"COMPLETE\" checked/> Complete</label>");
        out.println("              <label><input type=\"radio\" name=\"workStatus\" value=\"DELETE\"/> Delete</label>");
        out.println(
                "              <label><input type=\"radio\" name=\"workStatus\" value=\"BLOCKED\"/> Blocked</label>");
        out.println("            </div>");
        out.println("            <div class=\"fa-next-row\">");
        out.println("              <span>Next</span>");
        out.println("              <input type=\"text\" name=\"workFollowUp\" style=\"flex:1;\" />");
        out.println(
                "              <button type=\"submit\" name=\"action\" value=\"WorkNext\" class=\"fa-primary-btn\">Next</button>");
        out.println("            </div>");
        out.println(
                "            <input type=\"hidden\" name=\"completingActionNextId\" value=\"" + action.getActionNextId()
                        + "\" />");
        out.println("          </form>");
        out.println("        </div>");

        if (!meetingOptions.isEmpty()) {
            out.println("        <div class=\"fa-card\">");
            out.println("          <h2 class=\"fa-section-title\">Meetings</h2>");
            out.println("          <div id=\"fa-meetings-list\" class=\"fa-meetings-list\">");
            for (MeetingOption option : meetingOptions) {
                out.println("            <button type=\"button\" class=\"fa-meeting-item\" onclick=\"faSelectMeeting("
                        + option.getActionNextId() + ")\">" + escapeHtml(option.getLabel()) + "</button>");
            }
            out.println("          </div>");
            out.println("        </div>");
        }

        out.println("      </div>");
        out.println("    </section>");

        out.println("    <section class=\"fa-right\">");
        out.println("      <div id=\"fa-status-display\" class=\"fa-status-display\">");
        out.println("        <div class=\"fa-status-left\" id=\"fa-status-left\">0m Actual</div>");
        out.println("        <div class=\"fa-status-right\" id=\"fa-status-right\">" + formatMinutes(estimateMinutes) + " Estimated</div>");
        out.println("      </div>");
        out.println("      <div class=\"fa-clock-wrap\">");
        out.println(
                "        <svg id=\"fa-clock\" class=\"fa-clock\" viewBox=\"0 0 760 760\" role=\"img\" aria-label=\"One-hour focus clock\"></svg>");
        out.println("        <div id=\"fa-dandelion-btn\" class=\"fa-dandelion-btn\"></div>");
        out.println("      </div>");
        out.println("      <div id=\"fa-time-widget\" class=\"fa-time-widget\">");
        out.println("        <div class=\"fa-time-label\">Today:</div>");
        out.println("        <div id=\"fa-today-time\" class=\"fa-time-value\">" + formatMinutes(spentMinutes) + "</div>");
        out.println("        <div class=\"fa-time-label\">Week:</div>");
        out.println("        <div id=\"fa-week-time\" class=\"fa-time-value\">" + formatMinutes(spentMinutesThisWeek) + "</div>");
        out.println("      </div>");
        out.println("      <div class=\"fa-controls\">");
        out.println(
                "        <button id=\"fa-toggle-timer\" type=\"button\" class=\"fa-primary-btn\" onclick=\"faToggleTimer()\">"
                        + (runningClock ? "Stop" : "Start") + "</button>");
        out.println("      </div>");
        out.println("    </section>");
        out.println("  </div>");
        out.println("</div>");

        out.println("<script>");
        out.println("  var faState = {");
        out.println("    actionNextId: " + action.getActionNextId() + ",");
        out.println("    spentMinutes: " + Math.max(0, spentMinutes) + ",");
        out.println("    spentMinutesThisWeek: " + Math.max(0, spentMinutesThisWeek) + ",");
        out.println("    estimateMinutes: " + Math.max(0, estimateMinutes) + ",");
        out.println("    runningClock: " + (runningClock ? "true" : "false") + ",");
        out.println("    nowMinute: " + Math.max(0, Math.min(59, nowMinute)));
        out.println("  };");
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
        out.println("  function faDrawClock() {");
        out.println("    var svg = document.getElementById('fa-clock'); if (!svg) { return; }");
        out.println("    while (svg.firstChild) { svg.removeChild(svg.firstChild); }");
        out.println("    var cx = 380; var cy = 380; var radius = 300;");
        out.println("    var running = !!faState.runningClock;");
        out.println("    var palette = running ? {");
        out.println(
                "      background: '#ffffff', spent: '#d9f0d2', remaining: '#2f8c2f', overrun: '#cb2c1f', ring: '#2e342e', minorTick: '#9ba59a', majorTick: '#4d5a4d', label: '#2d3a2d' }");
        out.println(
                "      : { background: '#f3f3f3', spent: '#dbdbdb', remaining: '#c7c7c7', overrun: '#bdbdbd', ring: '#8a8a8a', minorTick: '#b5b5b5', majorTick: '#979797', label: '#7a7a7a' };");
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
        out.println("  }");
        out.println("  function faHandleNoteKeydown(event, actionNextId) {");
        out.println("    if (event.key === 'Enter' && event.ctrlKey) {");
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
                "      .then(function(data) { if (data && data.success) { window.location.reload(); } else { alert('Unable to select meeting.'); } })");
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
                "        if (!notes.length) { var emptyLi = document.createElement('li'); emptyLi.className = 'fa-empty'; emptyLi.textContent = 'No notes yet.'; list.appendChild(emptyLi); }");
        out.println(
                "        for (var i = 0; i < notes.length; i++) { var li = document.createElement('li'); li.textContent = notes[i]; list.appendChild(li); }");
        out.println("        input.value = ''; input.focus();");
        out.println("      })");
        out.println("      .catch(function() { alert('Unable to add note.'); });");
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
        out.println("        faState.estimateMinutes = parseInt(data.estimateMinutes || 0, 10) || 0;");
        out.println("        faState.nowMinute = parseInt(data.nowMinute || 0, 10) || 0;");
        out.println("        faState.runningClock = !!data.runningClock;");
        out.println("        var toggle = document.getElementById('fa-toggle-timer');");
        out.println("        if (toggle) { toggle.textContent = faState.runningClock ? 'Stop' : 'Start'; }");
        out.println("        var todayEl = document.getElementById('fa-today-time');");
        out.println("        if (todayEl) { todayEl.textContent = faFormatMinutes(faState.spentMinutes); }");
        out.println("        var weekEl = document.getElementById('fa-week-time');");
        out.println("        if (weekEl) { weekEl.textContent = faFormatMinutes(faState.spentMinutesThisWeek); }");
        out.println("        var statusLeftEl = document.getElementById('fa-status-left');");
        out.println("        var statusRightEl = document.getElementById('fa-status-right');");
        out.println("        if (statusLeftEl && statusRightEl) {");
        out.println("          var spent = Math.max(0, faState.spentMinutes || 0);");
        out.println("          var estimate = Math.max(0, faState.estimateMinutes || 0);");
        out.println("          var overrun = Math.max(0, spent - estimate);");
        out.println("          if (overrun > 0) {");
        out.println("            statusLeftEl.textContent = faFormatMinutes(overrun) + ' Over Estimate';");
        out.println("            statusLeftEl.style.color = '#cb2c1f';");
        out.println("          } else {");
        out.println("            statusLeftEl.textContent = faFormatMinutes(spent) + ' Actual';");
        out.println("            statusLeftEl.style.color = '#333';");
        out.println("          }");
        out.println("          statusRightEl.textContent = faFormatMinutes(estimate) + ' Estimated';");
        out.println("        }");
        out.println("        faDrawClock();");
        out.println("      });");
        out.println("  }");
        out.println("  function faFormatMinutes(minutes) {");
        out.println("    var safe = Math.max(0, minutes);");
        out.println("    var h = Math.floor(safe / 60);");
        out.println("    var m = safe % 60;");
        out.println("    if (h <= 0) { return m + 'm'; }");
        out.println("    if (m == 0) { return h + 'h'; }");
        out.println("    return h + 'h ' + m + 'm';");
        out.println("  }");
        out.println("  faDrawClock();");
        out.println("  window.setInterval(function(){ if (faState.runningClock) { faRefreshClock(); } }, 60000);");
        out.println("</script>");
    }

    private void printStyles(PrintWriter out) {
        out.println("<style>");
        out.println(
                "  .fa-page { min-height: 100vh; background: linear-gradient(160deg, #f6f8f5 0%, #edf2ec 48%, #f8fbf7 100%); box-sizing: border-box; padding: 16px; position: relative; }");
        out.println(
                "  .fa-close { position: fixed; right: 14px; top: 10px; border: 1px solid #7f8f7f; background: #fff; width: 38px; height: 38px; border-radius: 8px; font-size: 26px; line-height: 30px; cursor: pointer; z-index: 9; }");
        out.println(
                "  .fa-shell { display: grid; grid-template-columns: minmax(380px, 40%) minmax(540px, 60%); gap: 16px; min-height: calc(100vh - 32px); }");
        out.println("  .fa-left { min-height: 0; }");
        out.println("  .fa-left-scroll { height: calc(100vh - 32px); overflow-y: auto; padding-right: 8px; }");
        out.println(
                "  .fa-right { min-height: 0; display: flex; flex-direction: column; justify-content: center; align-items: center; position: relative; }");
        out.println("  .fa-clock-wrap { width: min(90vh, 850px); max-width: 100%; position: relative; }");
        out.println("  .fa-clock { width: 100%; height: auto; }");
        out.println("  .fa-status-display { display: flex; justify-content: space-between; width: 100%; margin-bottom: 16px; font-weight: 600; font-size: 16px; }");
        out.println("  .fa-status-left { color: #333; text-align: left; }");
        out.println("  .fa-status-right { color: #2f8c2f; text-align: right; }");
        out.println("  .fa-dandelion-btn { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 90px; height: 90px; border-radius: 50%; border: 3px solid #000; background: url('dandelion-button.png') center / contain no-repeat; background-color: #f0f0f0; z-index: 5; }");
        out.println("  .fa-time-widget { position: absolute; bottom: 20px; right: 20px; background: rgba(255,255,255,0.9); border: 2px solid #2a6f2a; border-radius: 12px; padding: 12px 16px; display: grid; grid-template-columns: auto auto; gap: 12px; font-weight: 600; font-size: 14px; z-index: 10; }");
        out.println("  .fa-time-label { color: #555; text-align: right; }");
        out.println("  .fa-time-value { color: #2f8c2f; font-size: 16px; }");
        out.println("  .fa-controls { margin-top: 14px; }");
        out.println(
                "  .fa-card { background: #ffffffd4; border: 1px solid #ced8cb; border-radius: 10px; padding: 14px; margin-bottom: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }");
        out.println("  .fa-title-card { border: 2px solid #2a6f2a; }");
        out.println("  .fa-main-title { font-size: 36px; font-weight: 700; color: #1f2f1f; line-height: 1.3; margin-bottom: 8px; }");
        out.println("  .fa-main-title i { font-style: italic; }");
        out.println("  .fa-project-subtitle { font-size: 14px; letter-spacing: .02em; text-transform: uppercase; color: #4a6350; }");
        out.println("  .fa-secondary-actions { display: flex; gap: 8px; margin-top: 12px; }");
        out.println(
                "  .fa-secondary-btn { border: 1px solid #7f9b7f; background: #eef6ec; color: #274127; padding: 8px 12px; border-radius: 7px; cursor: pointer; }");
        out.println(
                "  .fa-primary-btn { border: 1px solid #2a6f2a; background: #2f8c2f; color: #fff; padding: 16px 32px; border-radius: 7px; cursor: pointer; font-size: 18px; font-weight: 600; min-width: 120px; }");;
        out.println("  .fa-section-title { margin: 0 0 10px 0; font-size: 18px; color: #293829; }");
        out.println("  .fa-notes { margin: 0 0 10px 18px; padding: 0; }");
        out.println("  .fa-notes li { margin-bottom: 6px; white-space: pre-wrap; word-break: break-word; }");
        out.println("  .fa-empty { color: #6d7a6d; list-style: none; margin-left: -18px; }");
        out.println("  .fa-inline-note textarea { width: 100%; box-sizing: border-box; margin-bottom: 8px; padding: 8px; border: 1px solid #bbb; border-radius: 4px; font-family: inherit; }");
        out.println("  .fa-work-form .fa-field-label { margin-bottom: 6px; color: #324532; }");
        out.println("  .fa-status-row { display: flex; gap: 10px; margin: 10px 0; flex-wrap: wrap; }");
        out.println("  .fa-next-row { display: flex; align-items: center; gap: 8px; }");
        out.println("  .fa-meetings-list { display: flex; flex-direction: column; gap: 8px; }");
        out.println("  .fa-meeting-item { border: 1px solid #7f9b7f; background: #eef6ec; color: #274127; padding: 10px 12px; border-radius: 7px; cursor: pointer; text-align: left; }");
        out.println("  .fa-meeting-item:hover { background: #dde9db; }");
        out.println("  .fa-empty-clock { color: #7d7d7d; font-size: 20px; }");
        out.println("  @media (max-width: 900px) {");
        out.println("    .fa-shell { grid-template-columns: 1fr; }");
        out.println("    .fa-left-scroll { height: auto; max-height: none; }");
        out.println("    .fa-right { min-height: 420px; }");
        out.println("    .fa-clock-wrap { width: min(92vw, 620px); }");
        out.println("    .fa-dandelion-btn { width: 70px; height: 70px; }");
        out.println("    .fa-status-display { font-size: 14px; }");
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
