package org.dandeliondaily.focus.render;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import org.openimmunizationsoftware.pt.util.WebEscaper;

/**
 * Drawer holding a filterable list of actions, shared by the Focused Action context strip rows
 * (Up next, and later Meetings and This project). Action rows switch through switchAction; meeting
 * rows go through faSelectMeeting so the pre-meeting action is remembered.
 */
public class FocusActionPickerRenderer {

        private static final String SWITCH_URL_PREFIX = "FocusedActionServlet?action=switchAction&completingActionNextId=";

        public static class Row {
                private final int actionNextId;
                private final String projectName;
                private final String description;
                private final Integer estimateMinutes;
                private final int minutesToday;
                private final String whenLabel;
                private final boolean current;
                private final boolean meeting;

                public Row(int actionNextId, String projectName, String description, Integer estimateMinutes,
                                int minutesToday, String whenLabel, boolean current, boolean meeting) {
                        this.actionNextId = actionNextId;
                        this.projectName = projectName;
                        this.description = description;
                        this.estimateMinutes = estimateMinutes;
                        this.minutesToday = minutesToday;
                        this.whenLabel = whenLabel;
                        this.current = current;
                        this.meeting = meeting;
                }

                public int getActionNextId() {
                        return actionNextId;
                }

                public String getProjectName() {
                        return projectName;
                }

                public String getDescription() {
                        return description;
                }

                public Integer getEstimateMinutes() {
                        return estimateMinutes;
                }

                public int getMinutesToday() {
                        return minutesToday;
                }

                public String getWhenLabel() {
                        return whenLabel;
                }

                public boolean isCurrent() {
                        return current;
                }

                public boolean isMeeting() {
                        return meeting;
                }

                /** "Project - Description (15m)", for one-line previews. */
                public String getLabel() {
                        String text = projectName == null || projectName.length() == 0 ? "" : projectName + " - ";
                        text += description == null ? "" : description;
                        if (estimateMinutes != null) {
                                text += " (" + estimateMinutes + "m)";
                        }
                        return text;
                }
        }

        public static class Group {
                private final String title;
                private final List<Row> rows;

                public Group(String title, List<Row> rows) {
                        this.title = title;
                        this.rows = rows == null ? Collections.<Row>emptyList() : rows;
                }

                public String getTitle() {
                        return title;
                }

                public List<Row> getRows() {
                        return rows;
                }
        }

        public void render(PrintWriter out, String drawerId, String title, List<Group> groups, String emptyText) {
                boolean empty = true;
                for (Group group : groups) {
                        if (!group.getRows().isEmpty()) {
                                empty = false;
                        }
                }
                out.println("      <aside id=\"" + drawerId + "\" class=\"fa-drawer fa-picker\" aria-label=\""
                                + escapeHtml(title) + "\" aria-hidden=\"true\">");
                out.println("        <div class=\"fa-drawer-head\">");
                out.println("          <h2 class=\"fa-section-title\">" + escapeHtml(title) + "</h2>");
                out.println("          <button type=\"button\" class=\"fa-modal-close\" title=\"Close (Esc)\" onclick=\"faCloseDrawer()\">&times;</button>");
                out.println("        </div>");
                out.println("        <div class=\"fa-picker-filter\">");
                out.println("          <input type=\"text\" class=\"fa-picker-filter-input\" placeholder=\"Filter...\" autocomplete=\"off\""
                                + " oninput=\"faPickerFilter(this)\" onkeydown=\"faPickerKeydown(event, this)\" />");
                out.println("        </div>");
                out.println("        <div class=\"fa-drawer-body\">");
                if (empty) {
                        out.println("          <div class=\"fa-drawer-empty\">" + escapeHtml(emptyText) + "</div>");
                }
                for (Group group : groups) {
                        if (group.getRows().isEmpty()) {
                                continue;
                        }
                        out.println("          <div class=\"fa-picker-group\">");
                        out.println("            <div class=\"fa-picker-group-title\">" + escapeHtml(group.getTitle())
                                        + "</div>");
                        for (Row row : group.getRows()) {
                                renderRow(out, row);
                        }
                        out.println("          </div>");
                }
                out.println("          <div class=\"fa-drawer-empty fa-picker-nomatch\" style=\"display:none\">No matches.</div>");
                out.println("        </div>");
                out.println("      </aside>");
        }

        private void renderRow(PrintWriter out, Row row) {
                String classes = "fa-picker-row" + (row.isCurrent() ? " fa-picker-current" : "")
                                + (row.isMeeting() ? " fa-picker-meeting" : "");
                String filterText = (n(row.getProjectName()) + " " + n(row.getDescription())).toLowerCase();
                String target = row.isMeeting()
                                ? " href=\"#\" onclick=\"event.preventDefault(); faSelectMeeting(" + row.getActionNextId()
                                                + ");\""
                                : " href=\"" + escapeHtml(SWITCH_URL_PREFIX) + row.getActionNextId() + "\"";
                StringBuilder meta = new StringBuilder();
                if (row.getEstimateMinutes() != null) {
                        meta.append("est ").append(formatMinutes(row.getEstimateMinutes().intValue()));
                }
                if (row.getMinutesToday() > 0) {
                        appendSeparator(meta).append(formatMinutes(row.getMinutesToday())).append(" today");
                }
                out.println("            <a class=\"" + classes + "\"" + target + " data-filter=\""
                                + escapeHtml(filterText) + "\">");
                out.println("              <span class=\"fa-picker-main\">");
                out.println("                <span class=\"fa-picker-project\">"
                                + (row.isMeeting() ? "<span class=\"fa-picker-tag\">Meeting</span> " : "")
                                + escapeHtml(row.getProjectName())
                                + (row.isCurrent() ? " <span class=\"fa-picker-tag fa-picker-tag-current\">Current</span>" : "")
                                + "</span>");
                out.println("                <span class=\"fa-picker-desc\">" + escapeHtml(row.getDescription()) + "</span>");
                out.println("              </span>");
                out.println("              <span class=\"fa-picker-side\">");
                if (n(row.getWhenLabel()).length() > 0) {
                        out.println("                <span class=\"fa-picker-when\">" + escapeHtml(row.getWhenLabel())
                                        + "</span>");
                }
                if (meta.length() > 0) {
                        out.println("                <span class=\"fa-picker-meta\">" + meta + "</span>");
                }
                out.println("              </span>");
                out.println("            </a>");
        }

        public void printStyles(PrintWriter out) {
                out.println("  .fa-picker-filter { flex: 0 0 auto; padding: 8px 12px; border-bottom: 1px solid #e3e9e1; }");
                out.println("  .fa-picker-filter-input { width: 100%; box-sizing: border-box; padding: 7px 9px; border: 1px solid #bfcdbb; border-radius: 6px; font: inherit; font-size: 14px; background: #fcfffb; }");
                out.println("  .fa-picker-group { margin-bottom: 10px; }");
                out.println("  .fa-picker-group-title { font-size: 11px; font-weight: 700; letter-spacing: .05em; text-transform: uppercase; color: #6a7a68; margin: 4px 2px 4px; }");
                out.println("  .fa-picker-row { display: flex; align-items: center; gap: 10px; padding: 7px 9px; margin-bottom: 3px; border: 1px solid transparent; border-radius: 7px; color: #233423; text-decoration: none; }");
                out.println("  .fa-picker-row:hover { background: #edf5ea; }");
                out.println("  .fa-picker-row.fa-picker-active { background: #e3eee0; border-color: #7f9b7f; }");
                out.println("  .fa-picker-current { border-color: #2a6f2a; background: #f1f8ee; }");
                out.println("  .fa-picker-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }");
                out.println("  .fa-picker-project { font-size: 11px; letter-spacing: .03em; text-transform: uppercase; color: #4a6350; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }");
                out.println("  .fa-picker-desc { font-size: 14px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }");
                out.println("  .fa-picker-side { flex: 0 0 auto; display: flex; flex-direction: column; align-items: flex-end; font-size: 12px; color: #5b6b58; }");
                out.println("  .fa-picker-when { font-weight: 600; }");
                out.println("  .fa-picker-tag { display: inline-block; padding: 0 5px; border-radius: 4px; background: #e6ece4; color: #3d553d; font-size: 10px; font-weight: 700; letter-spacing: .04em; }");
                out.println("  .fa-picker-tag-current { background: #2f8c2f; color: #fff; }");
                out.println("  .fa-picker-meeting .fa-picker-tag { background: #e4e9f3; color: #34466b; }");
        }

        /** Filter, arrow-key and Enter handling for every picker drawer on the page. */
        public void printScript(PrintWriter out) {
                out.println("  function faPickerRows(drawer) { return Array.prototype.slice.call(drawer.querySelectorAll('.fa-picker-row')).filter(function(r) { return r.style.display !== 'none'; }); }");
                out.println("  function faPickerSetActive(drawer, row) {");
                out.println("    var old = drawer.querySelector('.fa-picker-active'); if (old) { old.classList.remove('fa-picker-active'); }");
                out.println("    if (row) { row.classList.add('fa-picker-active'); row.scrollIntoView({ block: 'nearest' }); }");
                out.println("  }");
                out.println("  function faPickerOpen(drawerId) {");
                out.println("    faOpenDrawer(drawerId);");
                out.println("    var drawer = document.getElementById(drawerId); if (!drawer) { return; }");
                out.println("    var input = drawer.querySelector('.fa-picker-filter-input');");
                out.println("    if (input && input.value) { input.value = ''; faPickerFilter(input); }");
                out.println("    var body = drawer.querySelector('.fa-drawer-body'); if (body) { body.scrollTop = 0; }");
                out.println("    var start = drawer.querySelector('.fa-picker-current') || faPickerRows(drawer)[0];");
                out.println("    faPickerSetActive(drawer, start);");
                out.println("    if (start) { start.scrollIntoView({ block: 'center' }); }");
                out.println("    if (input) { setTimeout(function() { input.focus(); }, 0); }");
                out.println("  }");
                out.println("  function faPickerFilter(input) {");
                out.println("    var drawer = input.closest('.fa-picker'); var text = (input.value || '').trim().toLowerCase();");
                out.println("    var rows = drawer.querySelectorAll('.fa-picker-row'); var shown = 0;");
                out.println("    for (var i = 0; i < rows.length; i++) { var match = !text || (rows[i].getAttribute('data-filter') || '').indexOf(text) >= 0; rows[i].style.display = match ? '' : 'none'; if (match) { shown++; } }");
                out.println("    var groups = drawer.querySelectorAll('.fa-picker-group');");
                out.println("    for (var g = 0; g < groups.length; g++) { groups[g].style.display = faPickerRows(groups[g]).length ? '' : 'none'; }");
                out.println("    var nomatch = drawer.querySelector('.fa-picker-nomatch'); if (nomatch) { nomatch.style.display = (rows.length && !shown) ? '' : 'none'; }");
                out.println("    var active = drawer.querySelector('.fa-picker-active');");
                out.println("    if (!active || active.style.display === 'none') { faPickerSetActive(drawer, faPickerRows(drawer)[0]); }");
                out.println("  }");
                out.println("  function faPickerKeydown(e, input) {");
                out.println("    var drawer = input.closest('.fa-picker'); var rows = faPickerRows(drawer); if (!rows.length) { return; }");
                out.println("    var index = rows.indexOf(drawer.querySelector('.fa-picker-active'));");
                out.println("    if (e.key === 'ArrowDown') { e.preventDefault(); faPickerSetActive(drawer, rows[Math.min(rows.length - 1, index + 1)]); }");
                out.println("    else if (e.key === 'ArrowUp') { e.preventDefault(); faPickerSetActive(drawer, rows[Math.max(0, index - 1)]); }");
                out.println("    else if (e.key === 'Enter') { e.preventDefault(); if (index >= 0) { rows[index].click(); } }");
                out.println("  }");
        }

        private StringBuilder appendSeparator(StringBuilder sb) {
                if (sb.length() > 0) {
                        sb.append(" &middot; ");
                }
                return sb;
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

        private String escapeHtml(String value) {
                return WebEscaper.escapeHtml(value);
        }

        private String n(String value) {
                return value == null ? "" : value;
        }
}
