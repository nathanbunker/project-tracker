package org.dandeliondaily.timereview.render;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.dandeliondaily.narrative.model.TrackerNarrativeScope;
import org.dandeliondaily.narrative.model.TrackerNarrativeViewModel;
import org.dandeliondaily.timereview.model.TimeEntryModel;
import org.dandeliondaily.timereview.model.TimeReviewDayModel;
import org.dandeliondaily.timereview.model.TimeSessionModel;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;
import org.openimmunizationsoftware.pt.model.TrackerNarrativeReviewStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

public class TimeReviewRenderer {

    private static final boolean DEV_LABELS_ENABLED = false;
    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final HtmlRenderer MARKDOWN_RENDERER = HtmlRenderer.builder().build();

    private static final String PARAM_REVIEW_DATE = "reviewDate";
    private static final String PARAM_REPORT_TYPE = "reportType";
    private static final String PARAM_NARRATIVE_ID = "narrativeId";
    private static final String PARAM_NARRATIVE_VIEW = "nview";
    private static final String PARAM_HISTORY = "nhistory";

    private static final String ACTION_NARRATIVE_GENERATE = "GenerateNarrative";
    private static final String ACTION_NARRATIVE_REGENERATE = "RegenerateNarrative";
    private static final String ACTION_NARRATIVE_APPROVE = "ApproveNarrative";
    private static final String ACTION_NARRATIVE_REJECT = "RejectNarrative";
    private static final String ACTION_NARRATIVE_SAVE = "SaveNarrative";
    private static final String ACTION_NARRATIVE_DELETE = "DeleteNarrative";

    private static final String VIEW_RENDERED = "rendered";
    private static final String VIEW_RAW = "raw";
    private static final String VIEW_EDIT = "edit";

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
            String quickScope,
            EditFormModel editForm,
            String pageMessage,
            TrackerNarrativeViewModel narrativeModel,
            String narrativeView,
            boolean showHistory) {

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
        printLeftColumn(out, webUser, dayModel, quickScope, narrativeModel);
        out.println("    </div>");

        out.println("    <div class=\"rd-col rd-col-center\">");
        printCenterColumn(out, webUser, dayModel, narrativeModel, narrativeView, showHistory);
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
            TimeReviewDayModel dayModel,
            String quickScope,
            TrackerNarrativeViewModel narrativeModel) {

        String reportType = narrativeModel == null || narrativeModel.getScope() == null
                ? TrackerNarrativeScope.TYPE_DAILY
                : narrativeModel.getScope().getNarrativeType();

        out.println("<div class=\"rd-panel\">");
        printDevLabel(out, "REVIEW NAV");
        out.println("<h2>Date Navigation</h2>");

        out.println("<div class=\"rd-quick-jump\">");
        out.println("<h3>Quick Jump</h3>");
        printQuickLink(out, "Yesterday", "yesterday", quickScope, reportType);
        printQuickLink(out, "Today", "today", quickScope, reportType);
        printQuickLink(out, "This Week", "week", quickScope, reportType);
        printQuickLink(out, "This Month", "month", quickScope, reportType);
        out.println("</div>");

        out.println("<div class=\"rd-day-nav\">");
        out.println("<h3>Select Date</h3>");
        out.println("<form action=\"ReviewDashboardServlet\" method=\"GET\" class=\"rd-date-form\">");
        out.println("<input type=\"hidden\" name=\"" + PARAM_REPORT_TYPE + "\" value=\""
                + escapeHtml(reportType) + "\">");
        out.println("<label for=\"rdReviewDate\">Date</label>");
        out.println("<input type=\"text\" id=\"rdReviewDate\" name=\"reviewDate\" value=\""
                + escapeHtml(formatPreferredDate(webUser, dayModel.getSelectedDate())) + "\">");
        out.println("<div class=\"rd-date-form-actions\">");
        out.println("<input type=\"submit\" value=\"Refresh\" class=\"rd-btn rd-btn-light\">");
        out.println("</div>");
        out.println("<p class=\"rd-subtle\">Use your preferred date format. Current selection: "
                + escapeHtml(dayModel.getSelectedDateLabel()) + "</p>");
        out.println("</form>");
        out.println("</div>");

        out.println("</div>");
    }

    private void printCenterColumn(PrintWriter out, WebUser webUser, TimeReviewDayModel dayModel,
            TrackerNarrativeViewModel narrativeModel, String narrativeView, boolean showHistory) {
        out.println("<div class=\"rd-panel\">");
        printCenterHeader(out, webUser, dayModel, narrativeModel, narrativeView);
        printMainNarrativeArea(out, webUser, dayModel, narrativeModel, narrativeView, showHistory);
        printHistoryArea(out, webUser, dayModel, narrativeModel, narrativeView, showHistory);
        out.println("</div>");
    }

    private void printCenterHeader(PrintWriter out, WebUser webUser, TimeReviewDayModel dayModel,
            TrackerNarrativeViewModel narrativeModel, String narrativeView) {
        TrackerNarrativeScope scope = narrativeModel.getScope();
        TrackerNarrative activeNarrative = narrativeModel.getActiveNarrative();

        printDevLabel(out, "REVIEW HEADER");
        out.println("<div class=\"rd-center-header\">");
        out.println("<h2>" + escapeHtml(buildScopeHeading(scope)) + "</h2>");

        out.println("<form action=\"ReviewDashboardServlet\" method=\"GET\" class=\"rd-type-form\">");
        out.println("<input type=\"hidden\" name=\"" + PARAM_REVIEW_DATE + "\" value=\""
                + escapeHtml(dayModel.getSelectedDateIso()) + "\">");
        out.println("<label for=\"rdReportType\">Report Type</label>");
        out.println("<select id=\"rdReportType\" name=\"" + PARAM_REPORT_TYPE + "\" onchange=\"this.form.submit()\">");
        printTypeOption(out, TrackerNarrativeScope.TYPE_DAILY, scope.getNarrativeType());
        printTypeOption(out, TrackerNarrativeScope.TYPE_WEEKLY, scope.getNarrativeType());
        printTypeOption(out, TrackerNarrativeScope.TYPE_MONTHLY, scope.getNarrativeType());
        out.println("</select>");
        out.println("</form>");

        out.println("<div class=\"rd-narrative-meta\">");
        out.println("<span class=\"rd-status-badge\">" + escapeHtml(resolveStatusLabel(activeNarrative)) + "</span>");
        if (activeNarrative != null && activeNarrative.getDateGenerated() != null) {
            out.println("<span class=\"rd-subtle\">Generated: "
                    + escapeHtml(webUser.getTimeFormat().format(activeNarrative.getDateGenerated())) + "</span>");
        }
        out.println("</div>");

        out.println("<div class=\"rd-narrative-toolbar\">");
        if (activeNarrative == null) {
            printNarrativeActionButton(out, dayModel.getSelectedDateIso(), scope.getNarrativeType(), 0,
                    ACTION_NARRATIVE_GENERATE, "Generate", "rd-btn");
        } else {
            printNarrativeActionButton(out, dayModel.getSelectedDateIso(), scope.getNarrativeType(),
                    activeNarrative.getNarrativeId(), ACTION_NARRATIVE_REGENERATE, "Regenerate", "rd-btn rd-btn-light");
            printNarrativeActionButton(out, dayModel.getSelectedDateIso(), scope.getNarrativeType(),
                    activeNarrative.getNarrativeId(), ACTION_NARRATIVE_APPROVE, "Approve", "rd-btn");
            printNarrativeActionButton(out, dayModel.getSelectedDateIso(), scope.getNarrativeType(),
                    activeNarrative.getNarrativeId(), ACTION_NARRATIVE_REJECT, "Reject", "rd-btn rd-btn-light");

            out.println("<a class=\"rd-btn rd-btn-light\" href=\""
                    + buildCenterLink(dayModel.getSelectedDateIso(), scope.getNarrativeType(),
                            activeNarrative.getNarrativeId(), VIEW_EDIT, false)
                    + "\">Edit</a>");
            out.println("<a class=\"rd-btn rd-btn-light\" href=\""
                    + buildCenterLink(dayModel.getSelectedDateIso(), scope.getNarrativeType(),
                            activeNarrative.getNarrativeId(), VIEW_RAW.equals(narrativeView) ? VIEW_RENDERED : VIEW_RAW,
                            false)
                    + "\">Raw</a>");
        }
        out.println("</div>");
        out.println("</div>");
    }

    private void printMainNarrativeArea(PrintWriter out, WebUser webUser, TimeReviewDayModel dayModel,
            TrackerNarrativeViewModel narrativeModel, String narrativeView, boolean showHistory) {
        printDevLabel(out, "ACTIVE NARRATIVE");
        TrackerNarrativeScope scope = narrativeModel.getScope();
        TrackerNarrative activeNarrative = narrativeModel.getActiveNarrative();

        out.println("<div class=\"rd-narrative-main\">");
        if (activeNarrative == null) {
            out.println("<p>No narrative yet for this period.</p>");
            if (narrativeModel.isGenerationAvailable()) {
                out.println("<p class=\"rd-subtle\">Generate a narrative, make in-place edits, then approve.</p>");
                printNarrativeActionButton(out, dayModel.getSelectedDateIso(), scope.getNarrativeType(), 0,
                        ACTION_NARRATIVE_GENERATE, "Generate", "rd-btn");
            } else {
                out.println(
                        "<p class=\"fail\">" + escapeHtml(narrativeModel.getGenerationUnavailableMessage()) + "</p>");
            }
            out.println("</div>");
            return;
        }

        if (TrackerNarrativeReviewStatus.GENERATING.equals(activeNarrative.getReviewStatus())) {
            out.println("<p class=\"rd-subtle\">Narrative is generating. Refresh to load the completed draft.</p>");
        }

        if (VIEW_EDIT.equals(narrativeView)) {
            out.println("<form action=\"ReviewDashboardServlet\" method=\"POST\" class=\"rd-narrative-edit-form\">");
            out.println("<input type=\"hidden\" name=\"action\" value=\"" + ACTION_NARRATIVE_SAVE + "\">");
            out.println("<input type=\"hidden\" name=\"" + PARAM_REVIEW_DATE + "\" value=\""
                    + escapeHtml(dayModel.getSelectedDateIso()) + "\">");
            out.println("<input type=\"hidden\" name=\"" + PARAM_REPORT_TYPE + "\" value=\""
                    + escapeHtml(scope.getNarrativeType()) + "\">");
            out.println("<input type=\"hidden\" name=\"" + PARAM_NARRATIVE_ID + "\" value=\""
                    + activeNarrative.getNarrativeId() + "\">");
            out.println("<textarea name=\"markdownFinal\" rows=\"20\" class=\"rd-narrative-editor\">"
                    + escapeHtml(n(activeNarrative.getMarkdownFinal())) + "</textarea>");
            out.println("<div class=\"rd-edit-actions\">");
            out.println("<input type=\"submit\" value=\"Save\" class=\"rd-btn\">");
            out.println("<a class=\"rd-btn rd-btn-light\" href=\""
                    + buildCenterLink(dayModel.getSelectedDateIso(), scope.getNarrativeType(),
                            activeNarrative.getNarrativeId(), VIEW_RENDERED, showHistory)
                    + "\">Cancel</a>");
            out.println("</div>");
            out.println("</form>");
            out.println("</div>");
            return;
        }

        if (VIEW_RAW.equals(narrativeView)) {
            out.println(
                    "<pre class=\"rd-raw-markdown\">" + escapeHtml(n(activeNarrative.getMarkdownFinal())) + "</pre>");
            out.println("</div>");
            return;
        }

        out.println("<div class=\"rd-rendered-markdown\">"
                + renderMarkdown(n(activeNarrative.getMarkdownFinal())) + "</div>");
        out.println("</div>");
    }

    private void printHistoryArea(PrintWriter out, WebUser webUser, TimeReviewDayModel dayModel,
            TrackerNarrativeViewModel narrativeModel, String narrativeView, boolean showHistory) {
        TrackerNarrativeScope scope = narrativeModel.getScope();
        TrackerNarrative activeNarrative = narrativeModel.getActiveNarrative();

        // History is intentionally collapsed by default so the active narrative editor
        // stays primary.

        printDevLabel(out, "NARRATIVE HISTORY");
        out.println("<div class=\"rd-history\">");
        out.println("<div class=\"rd-history-head\">");
        out.println("<h3>Previous Attempts</h3>");
        out.println("<a class=\"rd-btn rd-btn-light\" href=\""
                + buildCenterLink(dayModel.getSelectedDateIso(), scope.getNarrativeType(),
                        activeNarrative == null ? 0 : activeNarrative.getNarrativeId(), narrativeView, !showHistory)
                + "\">" + (showHistory ? "Collapse" : "Expand") + "</a>");
        out.println("</div>");

        if (!showHistory) {
            out.println("<p class=\"rd-subtle\">History stays minimized so the active draft remains primary.</p>");
            out.println("</div>");
            return;
        }

        if (narrativeModel.getHistoryItems().isEmpty()) {
            out.println("<p class=\"rd-subtle\">No previous attempts for this scope.</p>");
            out.println("</div>");
            return;
        }

        out.println("<table class=\"rd-history-table\">");
        out.println("<tr><th>Status</th><th>Generated</th><th>Title</th><th>Actions</th></tr>");
        for (TrackerNarrative narrative : narrativeModel.getHistoryItems()) {
            out.println("<tr>");
            out.println("<td>" + escapeHtml(n(narrative.getReviewStatusString())) + "</td>");
            out.println("<td>" + escapeHtml(formatDateTime(webUser, narrative.getDateGenerated())) + "</td>");
            out.println("<td>" + escapeHtml(n(narrative.getDisplayTitle())) + "</td>");
            out.println("<td>");
            out.println("<a class=\"rd-edit-link\" href=\""
                    + buildCenterLink(dayModel.getSelectedDateIso(), scope.getNarrativeType(),
                            narrative.getNarrativeId(), VIEW_RENDERED, true)
                    + "\">View</a>");
            out.println(" ");
            printNarrativeActionButton(out, dayModel.getSelectedDateIso(), scope.getNarrativeType(),
                    narrative.getNarrativeId(), ACTION_NARRATIVE_DELETE, "Delete", "rd-inline-delete");
            out.println("</td>");
            out.println("</tr>");
        }
        out.println("</table>");
        out.println("</div>");
    }

    private void printTypeOption(PrintWriter out, String value, String selectedType) {
        String selected = value.equals(selectedType) ? " selected" : "";
        out.println("<option value=\"" + value + "\"" + selected + ">" + value + "</option>");
    }

    private void printNarrativeActionButton(PrintWriter out, String reviewDateIso, String reportType, long narrativeId,
            String actionValue, String label, String cssClass) {
        out.println("<form action=\"ReviewDashboardServlet\" method=\"POST\" class=\"rd-inline-form\">");
        out.println("<input type=\"hidden\" name=\"action\" value=\"" + actionValue + "\">");
        out.println("<input type=\"hidden\" name=\"" + PARAM_REVIEW_DATE + "\" value=\""
                + escapeHtml(reviewDateIso) + "\">");
        out.println("<input type=\"hidden\" name=\"" + PARAM_REPORT_TYPE + "\" value=\""
                + escapeHtml(reportType) + "\">");
        if (narrativeId > 0) {
            out.println("<input type=\"hidden\" name=\"" + PARAM_NARRATIVE_ID + "\" value=\"" + narrativeId
                    + "\">");
        }
        out.println("<input type=\"submit\" value=\"" + escapeHtml(label) + "\" class=\"" + cssClass + "\">");
        out.println("</form>");
    }

    private String buildCenterLink(String reviewDateIso, String reportType, long narrativeId, String narrativeView,
            boolean showHistory) {
        StringBuilder link = new StringBuilder("ReviewDashboardServlet?");
        link.append(PARAM_REVIEW_DATE).append("=").append(reviewDateIso);
        link.append("&").append(PARAM_REPORT_TYPE).append("=").append(reportType);
        if (narrativeId > 0) {
            link.append("&").append(PARAM_NARRATIVE_ID).append("=").append(narrativeId);
        }
        if (narrativeView != null && narrativeView.length() > 0 && !VIEW_RENDERED.equals(narrativeView)) {
            link.append("&").append(PARAM_NARRATIVE_VIEW).append("=").append(narrativeView);
        }
        if (showHistory) {
            link.append("&").append(PARAM_HISTORY).append("=1");
        }
        return link.toString();
    }

    private String buildScopeHeading(TrackerNarrativeScope scope) {
        if (scope == null || scope.getPeriodStart() == null) {
            return "Narrative Review";
        }
        String type = scope.getNarrativeType();
        if (TrackerNarrativeScope.TYPE_WEEKLY.equals(type)) {
            return "Weekly Review - Week of " + scope.getPeriodStart();
        }
        if (TrackerNarrativeScope.TYPE_MONTHLY.equals(type)) {
            LocalDate monthStart = scope.getPeriodStart();
            return "Monthly Review - " + monthStart.getMonth().getDisplayName(TextStyle.FULL, Locale.US)
                    + " " + monthStart.getYear();
        }
        return "Daily Review - " + scope.getPeriodStart();
    }

    private String resolveStatusLabel(TrackerNarrative activeNarrative) {
        if (activeNarrative == null) {
            return "NONE";
        }
        String status = n(activeNarrative.getReviewStatusString());
        if (status.length() == 0) {
            return "UNKNOWN";
        }
        return status;
    }

    private String formatDateTime(WebUser webUser, Date date) {
        if (date == null) {
            return "";
        }
        return webUser.getTimeFormat().format(date);
    }

    private String renderMarkdown(String markdown) {
        Node document = MARKDOWN_PARSER.parse(markdown == null ? "" : markdown);
        return MARKDOWN_RENDERER.render(document);
    }

    private String n(String value) {
        return value == null ? "" : value;
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
            out.println("    <th>Duration</th>");
            out.println("    <th>Work</th>");
            out.println("    <th>Edit</th>");
            out.println("  </tr>");

            for (TimeEntryModel entry : session.getEntries()) {
                out.println("  <tr>");
                out.println("    <td>" + escapeHtml(timeFormat.format(entry.getStartTime())) + "</td>");
                out.println("    <td>" + escapeHtml(entry.getDurationDisplay()) + "</td>");
                String projectName = entry.getProjectName();
                String actionDescription = entry.getActionDescription();
                String workText = "";
                if (projectName.length() > 0 && actionDescription.length() > 0) {
                    workText = projectName + ": " + actionDescription;
                } else if (projectName.length() > 0) {
                    workText = projectName;
                } else {
                    workText = actionDescription;
                }
                out.println("    <td class=\"rd-work-cell\">" + escapeHtml(workText) + "</td>");
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

    private void printQuickLink(PrintWriter out, String label, String scope, String quickScope, String reportType) {
        String css = "rd-quick-link";
        if (scope.equals(quickScope)) {
            css += " rd-quick-link-selected";
        }
        out.println("<a class=\"" + css + "\" href=\"ReviewDashboardServlet?scope=" + scope
                + "&" + PARAM_REPORT_TYPE + "=" + escapeHtml(reportType) + "\">"
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
                ".rd-center-header { border-bottom: 1px solid #e3d9cb; padding-bottom: 10px; margin-bottom: 10px; }");
        out.println(".rd-center-header h2 { margin-bottom: 6px; }");
        out.println(".rd-type-form { display: inline-flex; align-items: center; gap: 8px; margin-bottom: 8px; }");
        out.println(".rd-type-form label { font-size: 12px; color: #5f564a; }");
        out.println(
                ".rd-type-form select { padding: 4px 6px; border: 1px solid #d2c8ba; border-radius: 4px; background: #fffdfa; }");
        out.println(".rd-narrative-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }");
        out.println(
                ".rd-status-badge { display: inline-block; padding: 3px 8px; border-radius: 999px; background: #e7efe4; border: 1px solid #bdd0b8; font-size: 11px; letter-spacing: 0.03em; }");
        out.println(".rd-narrative-toolbar { display: flex; flex-wrap: wrap; gap: 6px; }");
        out.println(".rd-inline-form { display: inline-block; margin: 0; }");
        out.println(
                ".rd-narrative-main { border: 1px solid #ddd2c3; border-radius: 6px; padding: 10px; background: #fffdfa; margin-bottom: 10px; }");
        out.println(".rd-rendered-markdown { line-height: 1.45; }");
        out.println(
                ".rd-rendered-markdown h1, .rd-rendered-markdown h2, .rd-rendered-markdown h3 { margin-top: 8px; margin-bottom: 6px; }");
        out.println(
                ".rd-raw-markdown { white-space: pre-wrap; background: #f6f2e8; border: 1px solid #dfd4c4; border-radius: 4px; padding: 8px; font-size: 12px; max-height: 420px; overflow: auto; }");
        out.println(
                ".rd-narrative-editor { width: 100%; box-sizing: border-box; border: 1px solid #d2c8ba; border-radius: 4px; padding: 8px; font-family: Consolas, 'Courier New', monospace; font-size: 12px; background: #fffdfa; }");
        out.println(".rd-history { border-top: 1px dashed #d7cab7; padding-top: 8px; }");
        out.println(
                ".rd-history-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }");
        out.println(
                ".rd-history-table { width: 100%; border-collapse: collapse; margin-top: 8px; table-layout: fixed; }");
        out.println(
                ".rd-history-table th, .rd-history-table td { border-bottom: 1px solid #e3d9cb; padding: 5px; font-size: 12px; vertical-align: top; }");
        out.println(
                ".rd-history-table th { text-align: left; color: #596355; font-size: 11px; text-transform: uppercase; }");
        out.println(".rd-history-table th:nth-child(1), .rd-history-table td:nth-child(1) { width: 70px; }");
        out.println(".rd-history-table th:nth-child(2), .rd-history-table td:nth-child(2) { width: 110px; }");
        out.println(".rd-history-table th:nth-child(4), .rd-history-table td:nth-child(4) { width: 100px; }");
        out.println(
                ".rd-inline-delete { background: transparent; border: 0; color: #8b4a3f; font-size: 12px; cursor: pointer; padding: 0; text-decoration: underline; }");
        out.println(
                ".rd-placeholder { border: 1px dashed #c8bea9; background: #fffdf8; border-radius: 6px; padding: 10px; }");
        out.println(".rd-quick-jump { margin-bottom: 12px; }");
        out.println(
                ".rd-quick-link { display: inline-block; margin: 0 6px 6px 0; padding: 4px 9px; border-radius: 999px; border: 1px solid #d2c3ab; text-decoration: none; color: #3a4c37; background: #efe8dc; font-size: 12px; }");
        out.println(".rd-quick-link-selected { background: #dfeeda; border-color: #a8c7a5; }");
        out.println(".rd-date-form label { display: block; margin-bottom: 4px; font-size: 12px; color: #4f4b43; }");
        out.println(
                ".rd-date-form input[type=text] { width: 100%; box-sizing: border-box; padding: 6px 7px; border: 1px solid #d2c8ba; border-radius: 4px; background: #fffdfa; }");
        out.println(".rd-date-form-actions { margin-top: 8px; }");
        out.println(
                ".rd-section-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }");
        out.println(".rd-total { font-size: 12px; color: #5a6557; }");
        out.println(".rd-break { margin: 2px 0 6px 0; color: #6d654f; font-size: 12px; }");
        out.println(".rd-table { width: 100%; border-collapse: collapse; margin-bottom: 6px; table-layout: fixed; }");
        out.println(
                ".rd-table th, .rd-table td { border-bottom: 1px solid #e3d9cb; padding: 6px 5px; font-size: 12px; vertical-align: top; }");
        out.println(
                ".rd-table th { text-align: left; color: #596355; font-size: 11px; text-transform: uppercase; letter-spacing: 0.04em; }");
        out.println(".rd-table th:nth-child(1), .rd-table td:nth-child(1) { width: 58px; white-space: nowrap; }");
        out.println(".rd-table th:nth-child(2), .rd-table td:nth-child(2) { width: 62px; white-space: nowrap; }");
        out.println(".rd-table th:nth-child(4), .rd-table td:nth-child(4) { width: 46px; white-space: nowrap; }");
        out.println(".rd-work-cell { white-space: normal; word-break: break-word; }");
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

    private String formatPreferredDate(WebUser webUser, Date date) {
        if (date == null) {
            return "";
        }
        return webUser.getDateFormat().format(date);
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
