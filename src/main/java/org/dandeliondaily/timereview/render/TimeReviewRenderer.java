package org.dandeliondaily.timereview.render;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.timereview.model.TimeEntryModel;
import org.dandeliondaily.timereview.model.TimeReviewDayModel;
import org.dandeliondaily.timereview.model.TimeSessionModel;
import org.openimmunizationsoftware.pt.model.WebUser;

public class TimeReviewRenderer {

    private static final boolean DEV_LABELS_ENABLED = false;

    public static class EditFormModel {
        private int billId;
        private String startTime = "";
        private String endTime = "";

        public int getBillId() {
            return billId;
        }

        public void setBillId(int billId) {
            this.billId = billId;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime == null ? "" : startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime == null ? "" : endTime;
        }
    }

    public void renderPage(
            PrintWriter out,
            WebUser webUser,
            TimeReviewDayModel dayModel,
            List<Date> trackedDays,
            Map<String, List<Date>> daysByMonth,
            String quickScope,
            EditFormModel editForm,
            String selectedIsoDay,
            String pageMessage) {

        printStyles(out);
        out.println("<div class=\"rd-page\">");
        out.println("  <div class=\"rd-intro\">");
        out.println("    <h1>Review</h1>");
        out.println("    <p>Daily review workspace. Time review and editing are active in this first round.</p>");
        out.println("  </div>");

        if (pageMessage != null && pageMessage.length() > 0) {
            out.println("<div class=\"rd-message\">" + escapeHtml(pageMessage) + "</div>");
        }

        out.println("  <div class=\"rd-shell\">");

        out.println("    <div class=\"rd-col rd-col-left\">");
        printLeftColumn(out, webUser, trackedDays, daysByMonth, selectedIsoDay, quickScope);
        out.println("    </div>");

        out.println("    <div class=\"rd-col rd-col-center\">");
        printCenterColumn(out, quickScope);
        out.println("    </div>");

        out.println("    <div class=\"rd-col rd-col-right\">");
        printRightColumn(out, webUser, dayModel, editForm, quickScope);
        out.println("    </div>");

        out.println("  </div>");
        out.println("</div>");
    }

    private void printLeftColumn(
            PrintWriter out,
            WebUser webUser,
            List<Date> trackedDays,
            Map<String, List<Date>> daysByMonth,
            String selectedIsoDay,
            String quickScope) {

        out.println("<div class=\"rd-panel\">");
        printDevLabel(out, "REVIEW NAV");
        out.println("<h2>Date Navigation</h2>");

        out.println("<div class=\"rd-quick-jump\">");
        out.println("<h3>Quick Jump</h3>");
        printQuickLink(out, "Today", "today", quickScope);
        printQuickLink(out, "This Week", "week", quickScope);
        printQuickLink(out, "This Month", "month", quickScope);
        out.println("</div>");

        out.println("<div class=\"rd-day-nav\">");
        out.println("<h3>Tracked Days</h3>");
        SimpleDateFormat isoFormat = webUser.getDateFormatService().createLegacyFormatter("yyyy-MM-dd",
                webUser.getTimeZone());
        if (trackedDays.isEmpty()) {
            out.println("<p class=\"rd-subtle\">No tracked days yet.</p>");
        } else {
            for (Map.Entry<String, List<Date>> monthEntry : daysByMonth.entrySet()) {
                out.println("<div class=\"rd-month\">");
                out.println("<div class=\"rd-month-label\">" + escapeHtml(monthEntry.getKey()) + "</div>");
                out.println("<div class=\"rd-day-list\">");
                for (Date day : monthEntry.getValue()) {
                    String dayIso = isoFormat.format(day);
                    String css = "rd-day-link";
                    if (dayIso.equals(selectedIsoDay)) {
                        css += " rd-day-link-selected";
                    }
                    String label = webUser.getDateFormatService().formatPattern(day, "EEE dd", webUser.getTimeZone());
                    out.println("<a class=\"" + css + "\" href=\"ReviewDashboardServlet?reviewDate=" + dayIso
                            + "\">" + escapeHtml(label) + "</a>");
                }
                out.println("</div>");
                out.println("</div>");
            }
        }
        out.println("</div>");

        out.println("</div>");
    }

    private void printCenterColumn(PrintWriter out, String quickScope) {
        out.println("<div class=\"rd-panel\">");
        printDevLabel(out, "REVIEW CENTER");
        out.println("<h2>Narrative Review</h2>");
        out.println("<p class=\"rd-subtle\">Narrative review area will be implemented here.</p>");
        out.println("<div class=\"rd-placeholder\">");
        out.println("<p>Scope selected: <strong>" + escapeHtml(capitalize(quickScope)) + "</strong></p>");
        out.println("<p>This center column remains intentionally scaffold-only in this step.</p>");
        out.println("</div>");
        out.println("</div>");
    }

    private void printRightColumn(
            PrintWriter out,
            WebUser webUser,
            TimeReviewDayModel dayModel,
            EditFormModel editForm,
            String quickScope) {

        out.println("<div class=\"rd-panel\">");
        printDevLabel(out, "TIME REVIEW");
        out.println("<div class=\"rd-section-head\">");
        out.println("<h2>Time Review</h2>");
        out.println("<span class=\"rd-total\">Total: " + escapeHtml(dayModel.getTotalDisplay()) + "</span>");
        out.println("</div>");

        out.println("<p class=\"rd-subtle\">" + escapeHtml(dayModel.getSelectedDateLabel())
                + " (scope: " + escapeHtml(capitalize(quickScope)) + ")</p>");

        if (editForm != null) {
            printEditForm(out, webUser, dayModel, editForm);
        }

        if (!dayModel.isHasEntries()) {
            out.println("<p>No time entries found for this day.</p>");
            out.println("</div>");
            return;
        }

        SimpleDateFormat timeFormat = webUser.getDateFormat(webUser.getTimeDisplayPattern());
        int sessionCount = 0;
        for (TimeSessionModel session : dayModel.getSessions()) {
            sessionCount++;
            String heading = sessionCount == 1 ? "Start Working" : "Continue Working";
            out.println("<h3>" + escapeHtml(timeFormat.format(session.getStartTime())) + " " + heading + "</h3>");
            if (session.getBreakMinutesBefore() != null && session.getBreakMinutesBefore().intValue() > 0) {
                out.println("<p class=\"rd-break\">Break before session: "
                        + escapeHtml(session.getBreakDisplay()) + "</p>");
            }

            out.println("<table class=\"rd-table\">");
            out.println("  <tr>");
            out.println("    <th>Start</th>");
            out.println("    <th>End</th>");
            out.println("    <th>Duration</th>");
            out.println("    <th>Project</th>");
            out.println("    <th>Description</th>");
            out.println("    <th>Edit</th>");
            out.println("  </tr>");

            for (TimeEntryModel entry : session.getEntries()) {
                out.println("  <tr>");
                out.println("    <td>" + escapeHtml(timeFormat.format(entry.getStartTime())) + "</td>");
                out.println("    <td>" + escapeHtml(timeFormat.format(entry.getEndTime())) + "</td>");
                out.println("    <td>" + escapeHtml(entry.getDurationDisplay()) + "</td>");
                String projectLink = entry.getProjectId() > 0
                        ? "<a href=\"ProjectServlet?projectId=" + entry.getProjectId() + "\">"
                                + escapeHtml(entry.getProjectName()) + "</a>"
                        : escapeHtml(entry.getProjectName());
                out.println("    <td>" + projectLink + "</td>");
                out.println("    <td>" + escapeHtml(entry.getActionDescription()) + "</td>");
                if (dayModel.getLockedBillEntryId() != null
                        && dayModel.getLockedBillEntryId().intValue() == entry.getBillId()) {
                    out.println("    <td><span class=\"rd-subtle\">Active</span></td>");
                } else {
                    out.println("    <td><a class=\"rd-edit-link\" href=\"ReviewDashboardServlet?reviewDate="
                            + dayModel.getSelectedDateIso() + "&editBillId=" + entry.getBillId() + "\">Edit</a></td>");
                }
                out.println("  </tr>");
            }
            out.println("</table>");
            out.println("<p class=\"rd-session-total\">Time worked: " + escapeHtml(session.getTotalDisplay()) + "</p>");
        }

        if (!dayModel.getSessions().isEmpty()) {
            TimeSessionModel last = dayModel.getSessions().get(dayModel.getSessions().size() - 1);
            out.println("<h3>" + escapeHtml(timeFormat.format(last.getEndTime())) + " Stop Working</h3>");
        }

        out.println("</div>");
    }

    private void printEditForm(PrintWriter out, WebUser webUser, TimeReviewDayModel dayModel, EditFormModel editForm) {
        out.println("<div class=\"rd-edit-box\">");
        out.println("<h3>Edit Time Entry</h3>");
        out.println("<form action=\"ReviewDashboardServlet\" method=\"POST\">");
        out.println("<input type=\"hidden\" name=\"action\" value=\"SaveTime\">");
        out.println("<input type=\"hidden\" name=\"reviewDate\" value=\"" + dayModel.getSelectedDateIso() + "\">");
        out.println("<input type=\"hidden\" name=\"billId\" value=\"" + editForm.getBillId() + "\">");
        out.println("<label>Start</label>");
        out.println("<input type=\"text\" name=\"startTime\" value=\"" + escapeHtml(editForm.getStartTime()) + "\">");
        out.println("<label>End</label>");
        out.println("<input type=\"text\" name=\"endTime\" value=\"" + escapeHtml(editForm.getEndTime()) + "\">");
        out.println("<div class=\"rd-edit-actions\">");
        out.println("<input type=\"submit\" value=\"Save\" class=\"rd-btn\">");
        out.println("<a class=\"rd-btn rd-btn-light\" href=\"ReviewDashboardServlet?reviewDate="
                + dayModel.getSelectedDateIso()
                + "\">Cancel</a>");
        out.println("</div>");
        out.println("<p class=\"rd-subtle\">Manual edit updates only this entry, then re-normalizes the day view.</p>");
        out.println("</form>");
        out.println("</div>");
    }

    private void printQuickLink(PrintWriter out, String label, String scope, String quickScope) {
        String css = "rd-quick-link";
        if (scope.equals(quickScope)) {
            css += " rd-quick-link-selected";
        }
        out.println("<a class=\"" + css + "\" href=\"ReviewDashboardServlet?scope=" + scope + "\">"
                + escapeHtml(label) + "</a>");
    }

    private void printStyles(PrintWriter out) {
        out.println("<style>");
        out.println(".rd-page { font-family: 'Trebuchet MS', Verdana, sans-serif; color: #2f3a2f; }");
        out.println(".rd-intro h1 { margin: 0 0 6px 0; font-size: 28px; color: #324631; }");
        out.println(".rd-intro p { margin: 0 0 10px 0; color: #5e6a5a; }");
        out.println(
                ".rd-message { margin: 0 0 10px 0; padding: 8px 10px; border: 1px solid #cfc39f; background: #fbf7ea; border-radius: 6px; }");
        out.println(".rd-shell { display: grid; grid-template-columns: 28% 34% 38%; gap: 14px; align-items: start; }");
        out.println(
                ".rd-panel { background: #fbf8f2; border: 1px solid #e4dbcd; border-radius: 8px; padding: 12px; box-shadow: 0 1px 0 rgba(255,255,255,0.75) inset; }");
        out.println(".rd-panel h2 { margin: 0 0 8px 0; font-size: 18px; color: #2f3a2f; }");
        out.println(".rd-panel h3 { margin: 10px 0 6px 0; font-size: 14px; color: #435043; }");
        out.println(".rd-subtle { color: #7a705f; font-size: 12px; }");
        out.println(
                ".rd-placeholder { border: 1px dashed #c8bea9; background: #fffdf8; border-radius: 6px; padding: 10px; }");
        out.println(".rd-quick-jump { margin-bottom: 12px; }");
        out.println(
                ".rd-quick-link { display: inline-block; margin: 0 6px 6px 0; padding: 4px 9px; border-radius: 999px; border: 1px solid #d2c3ab; text-decoration: none; color: #3a4c37; background: #efe8dc; font-size: 12px; }");
        out.println(".rd-quick-link-selected { background: #dfeeda; border-color: #a8c7a5; }");
        out.println(".rd-month { margin-bottom: 8px; }");
        out.println(
                ".rd-month-label { font-weight: bold; font-size: 12px; color: #5a6356; margin-bottom: 4px; text-transform: uppercase; letter-spacing: 0.04em; }");
        out.println(".rd-day-list { display: flex; flex-wrap: wrap; gap: 6px; }");
        out.println(
                ".rd-day-link { display: inline-block; padding: 4px 8px; border: 1px solid #d8ccba; border-radius: 6px; text-decoration: none; color: #364234; font-size: 12px; background: #fffdfa; }");
        out.println(".rd-day-link-selected { background: #e8f2e2; border-color: #a9c79f; font-weight: bold; }");
        out.println(
                ".rd-section-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }");
        out.println(".rd-total { font-size: 12px; color: #5a6557; }");
        out.println(".rd-break { margin: 2px 0 6px 0; color: #6d654f; font-size: 12px; }");
        out.println(".rd-table { width: 100%; border-collapse: collapse; margin-bottom: 6px; }");
        out.println(
                ".rd-table th, .rd-table td { border-bottom: 1px solid #e3d9cb; padding: 6px 5px; font-size: 12px; vertical-align: top; }");
        out.println(
                ".rd-table th { text-align: left; color: #596355; font-size: 11px; text-transform: uppercase; letter-spacing: 0.04em; }");
        out.println(".rd-edit-link { text-decoration: none; color: #2e5732; }");
        out.println(".rd-edit-link:hover { text-decoration: underline; }");
        out.println(".rd-session-total { margin: 0 0 10px 0; font-size: 12px; color: #4d5949; }");
        out.println(
                ".rd-edit-box { border: 1px solid #d9ccb8; border-radius: 6px; background: #fffdfa; padding: 10px; margin-bottom: 10px; }");
        out.println(".rd-edit-box label { display: block; font-size: 12px; margin: 6px 0 4px 0; color: #4f4b43; }");
        out.println(
                ".rd-edit-box input[type=text] { width: 100%; box-sizing: border-box; padding: 6px 7px; border: 1px solid #d2c8ba; border-radius: 4px; }");
        out.println(".rd-edit-actions { display: flex; gap: 8px; margin-top: 10px; }");
        out.println(
                ".rd-btn { background: #dcebd8; border: 1px solid #a9c9a7; color: #334233; border-radius: 4px; padding: 7px 10px; cursor: pointer; text-decoration: none; font-size: 12px; }");
        out.println(".rd-btn-light { background: #efe8dc; border-color: #d2c2ab; }");
        out.println(
                ".rd-dev-label { display: none; font-size: 10px; font-weight: bold; letter-spacing: 0.04em; text-transform: uppercase; color: #796f62; margin-bottom: 4px; }");
        out.println(".rd-dev-labels-enabled .rd-dev-label { display: block; }");
        out.println("@media (max-width: 1180px) { .rd-shell { grid-template-columns: 1fr; } }");
        out.println("</style>");
    }

    private void printDevLabel(PrintWriter out, String label) {
        if (!DEV_LABELS_ENABLED) {
            return;
        }
        out.println("<span class=\"rd-dev-label\">" + escapeHtml(label) + "</span>");
    }

    private String capitalize(String value) {
        if (value == null || value.length() == 0) {
            return "Today";
        }
        if (value.length() == 1) {
            return value.toUpperCase();
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
