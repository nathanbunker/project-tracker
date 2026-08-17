package org.dandeliondaily.weeklyreport.render;

import static org.openimmunizationsoftware.pt.util.WebEscaper.escapeHtml;
import static org.openimmunizationsoftware.pt.util.WebEscaper.urlEncode;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel;
import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel.ActivityItem;
import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel.AllocationRow;
import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel.ProjectActivity;
import org.dandeliondaily.weeklyreport.model.WeeklyTimeSummary;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

public class WeeklyReportRenderer {
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US);

    public void render(PrintWriter out, WeeklyReportViewModel model, WebUser owner, LocalDate latestWeek,
            String reportRoute, String accessKey) {
        String reportQuery = accessKey == null
                ? "weeklyReportId=" + model.getReport().getWeeklyReportId()
                : "key=" + urlEncode(accessKey);
        String ownerName = ownerName(owner);
        LocalDate weekStart = model.getWeekStart();

        printStyles(out);
        out.println("<div class=\"wr-page\">");
        out.println("<header class=\"wr-header\">");
        out.println("<div class=\"wr-header-copy\">");
        out.println("<div class=\"wr-eyebrow\">Weekly report</div>");
        out.println("<h1>" + escapeHtml(ownerName) + "</h1>");
        out.println("<p class=\"wr-owner\">Prepared for "
                + escapeHtml(model.getReport().getReportName()) + "</p>");
        out.println("</div>");
        out.println("<div class=\"wr-period\">");
        out.println("<span class=\"wr-period-label\">Reporting period</span>");
        out.println("<strong>" + displayDate(weekStart) + " <span>through</span> "
                + displayDate(weekStart.plusDays(6)) + "</strong>");
        out.println("</div>");
        out.println("<div class=\"wr-total\"><span>Total worked</span><strong>"
                + formatMinutes(model.getSelectedWeek().getAllWorkedMinutes()) + "</strong><small>hours</small></div>");
        out.println("</header>");
        out.println("<nav class=\"wr-week-nav\" aria-label=\"Weekly report navigation\">");
        out.print("<a class=\"wr-nav-link\" href=\"" + reportRoute + "?" + reportQuery + "&amp;week="
                + weekStart.minusWeeks(1) + "\">&larr; Previous week</a>");
        if (weekStart.isBefore(latestWeek)) {
            out.print("<a class=\"wr-nav-link\" href=\"" + reportRoute + "?" + reportQuery + "&amp;week="
                    + weekStart.plusWeeks(1) + "\">Next week &rarr;</a>");
        }
        out.println("</nav>");
        out.println("<main class=\"wr-report\">");

        renderBriefing(out, model);
        renderFundingSources(out, model);
        renderAllocations(out, model, reportQuery, accessKey != null);
        renderProjectActivity(out, model, owner, reportQuery, accessKey != null);
        renderHistory(out, model, reportRoute, reportQuery);
        out.println("</main>");
        out.println("</div>");
    }

    private static void renderBriefing(PrintWriter out, WeeklyReportViewModel model) {
        openSection(out, "Briefing", "Weekly Briefing", "The week's outcomes, priorities, and points for attention.",
                "wr-section-featured");
        if (model.getApprovedNarrativeHtml() == null) {
            out.println("<p class=\"wr-empty\">No approved weekly briefing has been published for this week.</p>");
        } else {
            out.println("<div class=\"weekly-report-briefing\">" + model.getApprovedNarrativeHtml() + "</div>");
        }
        closeSection(out);
    }

    private static void renderFundingSources(PrintWriter out, WeeklyReportViewModel model) {
        Map<String, Totals> bySource = new LinkedHashMap<String, Totals>();
        for (AllocationRow row : model.getAllocationRows().values()) {
            String source = blank(row.getFundingSource()) ? "Unassigned" : row.getFundingSource();
            Totals totals = bySource.get(source);
            if (totals == null) {
                totals = new Totals();
                bySource.put(source, totals);
            }
            totals.week += row.getWeekMinutes();
            totals.fourWeek += row.getFourWeekMinutes();
            totals.fiscal += row.getFiscalYearMinutes();
        }
        openSection(out, "Funding", "Funding Source Summary",
                "Worked time by funding source across the current and longer reporting windows.", "");
        if (bySource.isEmpty()) {
            out.println("<p class=\"wr-empty\">No qualifying billing activity was recorded.</p>");
            closeSection(out);
            return;
        }
        out.println("<div class=\"wr-table-wrap\"><table class=\"wr-table\"><thead><tr>"
                + "<th scope=\"col\">Funding Source</th><th scope=\"col\">Week</th>"
                + "<th scope=\"col\">4 Weeks</th><th scope=\"col\">Fiscal Year</th></tr></thead><tbody>");
        for (Map.Entry<String, Totals> entry : bySource.entrySet()) {
            Totals totals = entry.getValue();
            out.println("<tr><th scope=\"row\">" + escapeHtml(entry.getKey()) + "</th>"
                    + metricCell(totals.week, model.getSelectedWeek().getAllWorkedMinutes())
                    + metricCell(totals.fourWeek, model.getFourWeekAllWorkedMinutes())
                    + metricCell(totals.fiscal, model.getFiscalYearAllWorkedMinutes()) + "</tr>");
        }
        out.println("</tbody></table></div>");
        closeSection(out);
    }

    private static void renderAllocations(PrintWriter out, WeeklyReportViewModel model, String reportQuery,
            boolean publicView) {
        openSection(out, "Allocation", "Billing Allocation",
                "Billing-code distribution compared with annual and steering targets. "
                        + "Click a billing code to see this week's project breakdown.",
                "");
        if (model.getAllocationRows().isEmpty()) {
            out.println("<p class=\"wr-empty\">No billing codes in this report have activity or plan targets.</p>");
            closeSection(out);
            return;
        }
        Map<String, List<ProjectActivity>> activitiesByCode = new LinkedHashMap<String, List<ProjectActivity>>();
        for (ProjectActivity activity : model.getProjectActivities().values()) {
            List<ProjectActivity> activities = activitiesByCode.get(activity.getBillCode());
            if (activities == null) {
                activities = new ArrayList<ProjectActivity>();
                activitiesByCode.put(activity.getBillCode(), activities);
            }
            activities.add(activity);
        }
        String projectRoute = publicView ? "PublicWeeklyReportProjectServlet" : "WeeklyReportProjectServlet";
        out.println("<div class=\"wr-table-wrap\"><table class=\"wr-table\"><thead><tr>"
                + "<th scope=\"col\">Billing Code</th><th scope=\"col\">Funding Source</th>"
                + "<th scope=\"col\">Week</th><th scope=\"col\">4 Weeks</th>"
                + "<th scope=\"col\">Fiscal Year</th><th scope=\"col\">Annual Target</th>"
                + "<th scope=\"col\">Steering Target</th></tr></thead><tbody>");
        int groupIndex = 0;
        for (AllocationRow row : model.getAllocationRows().values()) {
            List<ProjectActivity> activities = activitiesByCode.get(row.getBillCode());
            boolean hasProjects = activities != null && !activities.isEmpty();
            String groupId = "wr-alloc-" + groupIndex++;
            String codeDisplay = hasProjects
                    ? "<button type=\"button\" class=\"wr-code-toggle\" aria-expanded=\"false\" data-target=\""
                            + groupId + "\">" + escapeHtml(row.getBillCode()) + "</button>"
                    : escapeHtml(row.getBillCode());
            out.println("<tr><th scope=\"row\">" + codeDisplay + label(row.getBillLabel()) + "</th>"
                    + "<td>" + escapeHtml(value(row.getFundingSource())) + "</td>"
                    + billingMetric(row.getWeekMinutes(), row.getWeekPercent())
                    + billingMetric(row.getFourWeekMinutes(), row.getFourWeekPercent())
                    + billingMetric(row.getFiscalYearMinutes(), row.getFiscalYearPercent())
                    + percentCell(row.getAnnualTargetPercent()) + percentCell(row.getSteeringTargetPercent())
                    + "</tr>");
            if (hasProjects) {
                for (ProjectActivity activity : activities) {
                    Project project = activity.getProject();
                    String projectName = project == null ? "Project " + activity.getProjectId()
                            : project.getProjectName();
                    String projectLink = projectRoute + "?" + reportQuery + "&amp;week=" + model.getWeekStart()
                            + "&amp;projectId=" + activity.getProjectId() + "&amp;billCode="
                            + urlEncode(activity.getBillCode());
                    out.println("<tr class=\"wr-alloc-detail\" data-group=\"" + groupId + "\" hidden>"
                            + "<th scope=\"row\"></th><td class=\"wr-alloc-project\"><a href=\"" + projectLink
                            + "\">" + escapeHtml(projectName) + "</a></td><td class=\"wr-number\">"
                            + formatMinutes(activity.getRoundedMinutes())
                            + "</td><td></td><td></td><td></td><td></td></tr>");
                }
            }
        }
        out.println("</tbody></table></div>");
        out.println(
                "<script>(function(){var toggles=document.querySelectorAll('.wr-code-toggle');for(var i=0;i<toggles.length;i++){toggles[i].addEventListener('click',function(){var expanded=this.getAttribute('aria-expanded')==='true';this.setAttribute('aria-expanded',expanded?'false':'true');var rows=document.querySelectorAll('tr[data-group=\"'+this.getAttribute('data-target')+'\"]');for(var j=0;j<rows.length;j++){if(expanded){rows[j].setAttribute('hidden','');}else{rows[j].removeAttribute('hidden');}}});}})();</script>");
        closeSection(out);
    }

    private static void renderProjectActivity(PrintWriter out, WeeklyReportViewModel model, WebUser owner,
            String reportQuery, boolean publicView) {
        openSection(out, "Delivery", "Project Activity",
                "Completed work grouped by billing code and project.", "");
        if (model.getProjectActivities().isEmpty()) {
            out.println("<p class=\"wr-empty\">No projects recorded qualifying time for this week.</p>");
            closeSection(out);
            return;
        }
        Map<String, List<ProjectActivity>> byCode = new LinkedHashMap<String, List<ProjectActivity>>();
        for (ProjectActivity activity : model.getProjectActivities().values()) {
            List<ProjectActivity> activities = byCode.get(activity.getBillCode());
            if (activities == null) {
                activities = new ArrayList<ProjectActivity>();
                byCode.put(activity.getBillCode(), activities);
            }
            activities.add(activity);
        }
        for (Map.Entry<String, List<ProjectActivity>> entry : byCode.entrySet()) {
            out.println("<div class=\"wr-activity-group\"><h3>" + escapeHtml(value(entry.getKey())) + "</h3>");
            out.println("<div class=\"wr-project-list\">");
            for (ProjectActivity activity : entry.getValue()) {
                Project project = activity.getProject();
                String projectName = project == null ? "Project " + activity.getProjectId() : project.getProjectName();
                String detailRoute = publicView ? "PublicWeeklyReportProjectServlet" : "WeeklyReportProjectServlet";
                String projectLink = detailRoute + "?" + reportQuery + "&amp;week=" + model.getWeekStart()
                        + "&amp;projectId=" + activity.getProjectId() + "&amp;billCode="
                        + urlEncode(activity.getBillCode());
                out.println("<article class=\"wr-project\"><div class=\"wr-project-heading\"><h4><a href=\""
                        + projectLink + "\">" + escapeHtml(projectName) + "</a></h4><span>"
                        + formatMinutes(activity.getRoundedMinutes()) + " hours</span></div>");
                if (activity.getCompletedItems().isEmpty()) {
                    out.println("<p class=\"wr-empty\">No completed items were recorded.</p>");
                } else {
                    out.println("<ul>");
                    for (ActivityItem item : activity.getCompletedItems()) {
                        out.println("<li>" + owner.toLocalDate(item.getDate()) + ": "
                                + activityDescription(item) + notes(item.getNotes()) + "</li>");
                    }
                    out.println("</ul>");
                }
                out.println("</article>");
            }
            out.println("</div></div>");
        }
        closeSection(out);
    }

    public void renderProjectDetail(PrintWriter out, WeeklyReportViewModel model, WebUser owner, int projectId,
            String billCode, String reportLink) {
        ProjectActivity activity = model.getProjectActivities()
                .get(WeeklyReportViewModel.projectKey(projectId, billCode));
        Project project = activity == null ? null : activity.getProject();
        String projectName = project == null ? "Project " + projectId : project.getProjectName();

        printStyles(out);
        out.println("<div class=\"wr-page\">");
        out.println("<header class=\"wr-header wr-detail-header\">");
        out.println("<div class=\"wr-header-copy\"><div class=\"wr-eyebrow\">Weekly report / Project detail</div>");
        out.println("<h1>" + escapeHtml(projectName) + "</h1><p class=\"wr-owner\">Prepared for "
                + escapeHtml(model.getReport().getReportName()) + "</p></div>");
        out.println("<div class=\"wr-period\"><span class=\"wr-period-label\">Reporting period</span><strong>"
                + displayDate(model.getWeekStart()) + " <span>through</span> "
                + displayDate(model.getWeekStart().plusDays(6)) + "</strong></div>");
        out.println("<div class=\"wr-total\"><span>Project time</span><strong>"
                + formatMinutes(activity == null ? 0 : activity.getRoundedMinutes())
                + "</strong><small>hours</small></div></header>");
        out.println("<nav class=\"wr-week-nav\"><a class=\"wr-nav-link\" href=\"" + reportLink
                + "\">&larr; Back to weekly report</a></nav>");
        out.println("<main class=\"wr-report\">");

        openSection(out, "Overview", "Project Summary", "Current project context and reporting totals.", "");
        out.println("<div class=\"wr-detail-metrics\"><div><span>Historical billing code</span><strong>"
                + escapeHtml(value(billCode)) + "</strong></div><div><span>Selected week</span><strong>"
                + formatMinutes(activity == null ? 0 : activity.getRoundedMinutes()));
        out.println("</strong></div><div><span>Fiscal year</span><strong>"
                + formatMinutes(model.getFiscalProjectMinutes(projectId, billCode)) + "</strong></div></div>");
        if (project != null) {
            out.println("<div class=\"wr-detail-context\">");
            detail(out, "Current Focus", project.getCurrentFocusText());
            detail(out, "Project Outcome", project.getOutcomeText());
            detail(out, "Success Criteria", project.getSuccessCriteriaText());
            out.println("</div>");
        }
        closeSection(out);

        openSection(out, "Delivery", "Completed This Week", "Completed actions recorded during the selected week.",
                "wr-section-featured");
        if (activity == null || activity.getCompletedItems().isEmpty()) {
            out.println("<p class=\"wr-empty\">No completed items were recorded.</p>");
        } else {
            out.println("<ul class=\"wr-detail-activity\">");
            for (ActivityItem item : activity.getCompletedItems()) {
                out.println("<li><time>" + owner.toLocalDate(item.getDate()) + "</time><span>"
                        + activityDescription(item) + notes(item.getNotes()) + "</span></li>");
            }
            out.println("</ul>");
        }
        closeSection(out);
        out.println("</main></div>");
    }

    private static void renderHistory(PrintWriter out, WeeklyReportViewModel model, String reportRoute,
            String reportQuery) {
        openSection(out, "Trend", "Eight-Week History", "A concise view of report scope and total worked time.", "");
        out.println("<div class=\"wr-table-wrap\"><table class=\"wr-table\"><thead><tr>"
                + "<th scope=\"col\">Week</th><th scope=\"col\">Report Scope</th>"
                + "<th scope=\"col\">All Worked Time</th></tr></thead><tbody>");
        for (WeeklyTimeSummary week : model.getHistory()) {
            String marker = week.getWeekStart().equals(model.getWeekStart())
                    ? " <span class=\"wr-selected\">Selected</span>"
                    : "";
            out.println("<tr><th scope=\"row\"><a href=\"" + reportRoute + "?" + reportQuery + "&amp;week="
                    + week.getWeekStart() + "\">" + displayDate(week.getWeekStart()) + " through "
                    + displayDate(week.getWeekStart().plusDays(6)) + "</a>" + marker + "</th><td class=\"wr-number\">"
                    + formatMinutes(week.getScopedMinutes()) + "</td><td class=\"wr-number\">"
                    + formatMinutes(week.getAllWorkedMinutes()) + "</td></tr>");
        }
        out.println("</tbody></table></div>");
        closeSection(out);
    }

    private static String ownerName(WebUser owner) {
        String name = (value(owner.getFirstName()) + " " + value(owner.getLastName())).trim();
        ProjectContact contact = owner.getProjectContact();
        if (name.length() == 0 && contact != null) {
            name = (value(contact.getNameFirst()) + " " + value(contact.getNameLast())).trim();
        }
        return name.length() == 0 ? owner.getUsername() : name;
    }

    private static String allocationMetric(int minutes, BigDecimal percent) {
        return "<td class=\"wr-number\"><strong>" + percent(percent) + "</strong><small>"
                + formatMinutes(minutes) + " hrs</small></td>";
    }

    private static String metricCell(int minutes, int denominator) {
        return allocationMetric(minutes, calculatePercent(minutes, denominator));
    }

    private static String billingMetric(int minutes, BigDecimal percent) {
        return "<td class=\"wr-number\"><strong>" + percentWhole(percent) + "</strong><small>"
                + formatMinutes(minutes) + " hrs</small></td>";
    }

    private static String percentCell(BigDecimal percent) {
        return "<td class=\"wr-number\">" + (percent == null ? "&mdash;" : percentWhole(percent)) + "</td>";
    }

    private static String percent(BigDecimal percent) {
        return percent.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static String percentWhole(BigDecimal percent) {
        return percent.setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static BigDecimal calculatePercent(int numerator, int denominator) {
        if (denominator <= 0)
            return BigDecimal.ZERO.setScale(2);
        return new BigDecimal(numerator).multiply(new BigDecimal("100"))
                .divide(new BigDecimal(denominator), 2, RoundingMode.HALF_UP);
    }

    private static String formatMinutes(int minutes) {
        return String.format("%d:%02d", minutes / 60, minutes % 60);
    }

    private static String label(String label) {
        return blank(label) ? "" : "<small class=\"wr-code-label\">" + escapeHtml(label) + "</small>";
    }

    private static String displayDate(LocalDate date) {
        return DISPLAY_DATE.format(date);
    }

    private static void openSection(PrintWriter out, String eyebrow, String title, String description,
            String extraClass) {
        out.println("<section class=\"wr-section " + extraClass + "\">");
        out.println("<div class=\"wr-section-heading\"><div><span>" + escapeHtml(eyebrow) + "</span><h2>"
                + escapeHtml(title) + "</h2></div><p>" + escapeHtml(description) + "</p></div>");
    }

    private static void closeSection(PrintWriter out) {
        out.println("</section>");
    }

    private static void printStyles(PrintWriter out) {
        out.println("<style>");
        out.println(
                ".wr-page{box-sizing:border-box;min-height:calc(100vh - 70px);padding:18px;background:linear-gradient(180deg,#f4f0e8 0%,#efe7db 40%,#f8f6f1 100%);color:#2d332d;font-family:Georgia,'Times New Roman',serif}");
        out.println(
                ".wr-page *{box-sizing:border-box}.wr-page a{color:#315c3a;text-decoration:none}.wr-page a:hover{color:#1e3d26}");
        out.println(
                ".wr-header{display:grid;grid-template-columns:minmax(0,1fr) auto auto;gap:28px;align-items:end;max-width:1180px;margin:0 auto;background:linear-gradient(90deg,#3c5341 0%,#5b735c 55%,#7a8f70 100%);color:#fffdf8;padding:26px 30px;border:1px solid #354b39;box-shadow:0 10px 30px rgba(94,77,58,.12)}");
        out.println(
                ".wr-eyebrow,.wr-section-heading span{display:block;margin-bottom:7px;font-family:Verdana,sans-serif;font-size:11px;font-weight:bold;letter-spacing:.08em;text-transform:uppercase}.wr-header h1{margin:0;font-size:30px;line-height:1.08;letter-spacing:0}.wr-owner{margin:7px 0 0;color:#e2e9df;font-family:Verdana,sans-serif;font-size:13px}");
        out.println(
                ".wr-period{padding-left:22px;border-left:1px solid rgba(255,255,255,.28);font-family:Verdana,sans-serif}.wr-period-label,.wr-total span{display:block;margin-bottom:7px;font-size:11px;text-transform:uppercase}.wr-period strong{display:block;font-size:14px;line-height:1.5}.wr-period strong span{font-weight:normal;opacity:.75}");
        out.println(
                ".wr-total{min-width:118px;padding-left:22px;border-left:1px solid rgba(255,255,255,.28);font-family:Verdana,sans-serif}.wr-total strong{font-size:31px;line-height:1}.wr-total small{margin-left:5px;font-size:11px;opacity:.8}");
        out.println(
                ".wr-week-nav{display:flex;justify-content:space-between;max-width:1180px;margin:0 auto;padding:10px 14px;background:#e5ecdf;border:1px solid #cbbda7;border-top:0;font-family:Verdana,sans-serif;font-size:12px}.wr-nav-link{padding:4px 7px;text-decoration:none}");
        out.println(
                ".wr-report{max-width:1180px;margin:0 auto;background:rgba(255,252,247,.94);border:1px solid #cbbda7;border-top:0;box-shadow:0 10px 30px rgba(94,77,58,.12)}");
        out.println(
                ".wr-section{padding:30px;border-bottom:1px solid #ddd0bd}.wr-section:last-child{border-bottom:0}.wr-section-featured{background:#fffdf8}.wr-section-heading{display:grid;grid-template-columns:minmax(220px,.7fr) minmax(280px,1fr);gap:30px;align-items:end;margin-bottom:20px}.wr-section-heading span{color:#78866f}.wr-section-heading h2{margin:0;color:#334235;font-size:23px;line-height:1.15}.wr-section-heading p{margin:0;color:#6b6256;font-family:Verdana,sans-serif;font-size:12px;line-height:1.5}");
        out.println(
                ".weekly-report-briefing{max-width:820px;font-size:16px;line-height:1.62}.weekly-report-briefing h2{margin:25px 0 9px;color:#3d4f41;font-size:19px}.weekly-report-briefing h2:first-child{margin-top:0}.weekly-report-briefing ul{padding-left:20px}.weekly-report-briefing li{margin-bottom:8px}.wr-empty{margin:0;color:#766c5f;font-style:italic}");
        out.println(
                ".wr-table-wrap{width:100%;overflow-x:auto;border:1px solid #d9ccb8}.wr-table{width:100%;border-collapse:collapse;font-family:Verdana,sans-serif;font-size:12px;background:#fffdf8}.wr-table th,.wr-table td{padding:11px 12px;border-bottom:1px solid #e4dacb;text-align:left;vertical-align:top}.wr-table thead th{background:#e8eee3;color:#344637;font-size:10px;letter-spacing:.04em;text-transform:uppercase;white-space:nowrap}.wr-table tbody th{color:#354537}.wr-table tbody tr:last-child th,.wr-table tbody tr:last-child td{border-bottom:0}.wr-table tbody tr:hover{background:#faf6ef}.wr-number{white-space:nowrap;font-variant-numeric:tabular-nums}.wr-number strong,.wr-number small{display:block}.wr-number small{margin-top:3px;color:#756b5f;font-size:10px}.wr-code-label{display:block;margin-top:3px;color:#756b5f;font-weight:normal}");
        out.println(
                ".wr-code-toggle{display:inline-flex;align-items:center;gap:6px;padding:0;margin:0;border:0;background:none;font:inherit;color:#354537;cursor:pointer;text-align:left}.wr-code-toggle:hover{color:#1e3d26}.wr-code-toggle::before{content:'\\25B8';font-size:9px;color:#78866f}.wr-code-toggle[aria-expanded=\"true\"]::before{content:'\\25BE'}.wr-alloc-detail{background:#f4efe3}.wr-alloc-detail:hover{background:#f4efe3}.wr-alloc-project{padding-left:26px;color:#5d4b34;font-style:italic}.wr-alloc-project a{font-style:normal}");
        out.println(
                ".wr-activity-group{margin-top:24px}.wr-activity-group:first-of-type{margin-top:0}.wr-activity-group>h3{margin:0;padding:9px 12px;background:#e8eee3;border-left:4px solid #617a60;color:#344637;font-family:Verdana,sans-serif;font-size:12px;letter-spacing:.05em;text-transform:uppercase}.wr-project-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;margin-top:14px}.wr-project{padding:16px;border:1px solid #d9ccb8;background:#fffdf8}.wr-project-heading{display:flex;justify-content:space-between;gap:18px;align-items:baseline;margin-bottom:11px}.wr-project h4{margin:0;font-size:16px}.wr-project-heading span{color:#5d4b34;font-family:Verdana,sans-serif;font-size:11px;font-weight:bold;white-space:nowrap}.wr-project ul{margin:0;padding-left:19px}.wr-project li{margin-bottom:7px;line-height:1.45}.wr-selected{display:inline-block;margin-left:6px;padding:2px 6px;background:#dde9d8;color:#29402c;font-size:9px;text-transform:uppercase}");
        out.println(
                ".wr-detail-header{grid-template-columns:minmax(0,1fr) auto auto}.wr-detail-metrics{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));border:1px solid #d9ccb8;background:#fffdf8}.wr-detail-metrics>div{padding:16px;border-right:1px solid #e4dacb}.wr-detail-metrics>div:last-child{border-right:0}.wr-detail-metrics span,.wr-detail-context h3{display:block;margin:0 0 6px;color:#78866f;font-family:Verdana,sans-serif;font-size:10px;font-weight:bold;letter-spacing:.05em;text-transform:uppercase}.wr-detail-metrics strong{color:#354537;font-size:18px}.wr-detail-context{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px;margin-top:18px}.wr-detail-card{padding:16px;border:1px solid #d9ccb8;background:#fffdf8}.wr-detail-card p{margin:0;line-height:1.5}.wr-detail-activity{list-style:none;margin:0;padding:0;border:1px solid #d9ccb8;background:#fffdf8}.wr-detail-activity li{display:grid;grid-template-columns:110px minmax(0,1fr);gap:16px;padding:13px 15px;border-bottom:1px solid #e4dacb;line-height:1.45}.wr-detail-activity li:last-child{border-bottom:0}.wr-detail-activity time{color:#6b6256;font-family:Verdana,sans-serif;font-size:11px;font-weight:bold}");
        out.println(
                "@media(max-width:760px){.wr-page{padding:8px}.wr-header,.wr-detail-header{grid-template-columns:1fr;gap:18px;padding:22px}.wr-period,.wr-total{padding:14px 0 0;border-left:0;border-top:1px solid rgba(255,255,255,.28)}.wr-section{padding:22px 18px}.wr-section-heading{grid-template-columns:1fr;gap:8px}.wr-project-list,.wr-detail-context,.wr-detail-metrics{grid-template-columns:1fr}.wr-detail-metrics>div{border-right:0;border-bottom:1px solid #e4dacb}.wr-detail-metrics>div:last-child{border-bottom:0}.wr-detail-activity li{grid-template-columns:1fr;gap:5px}.wr-week-nav{padding:8px}.wr-header h1{font-size:26px}} ");
        out.println(
                "@media print{.wr-page{padding:0;background:#fff}.wr-week-nav{display:none}.wr-header,.wr-report{max-width:none;box-shadow:none}.wr-section{break-inside:avoid}.wr-project-list{grid-template-columns:repeat(2,minmax(0,1fr))}}");
        out.println("</style>");
    }

    private static String notes(String notes) {
        return blank(notes) ? "" : " &mdash; " + escapeHtml(notes);
    }

    private static String activityDescription(ActivityItem item) {
        return (item.isCompletedAction() ? "<strong>Completed - </strong>" : "")
                + escapeHtml(item.getDescription());
    }

    private static void detail(PrintWriter out, String label, String text) {
        if (!blank(text)) {
            out.println("<article class=\"wr-detail-card\"><h3>" + escapeHtml(label) + "</h3><p>"
                    + escapeHtml(text) + "</p></article>");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    private static class Totals {
        private int week;
        private int fourWeek;
        private int fiscal;
    }
}