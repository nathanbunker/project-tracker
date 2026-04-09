package org.dandeliondaily.projecthealth.render;

import java.io.PrintWriter;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.dandeliondaily.projecthealth.model.ProjectHealthIssueModel;
import org.dandeliondaily.projecthealth.model.ProjectHealthPageModel;
import org.dandeliondaily.projecthealth.model.ProjectCadenceGroupModel;
import org.dandeliondaily.projecthealth.model.ProjectListItemModel;
import org.dandeliondaily.projecthealth.model.ProjectReportModel;
import org.dandeliondaily.projecthealth.model.ProjectPatchLinkDisplayModel;
import org.openimmunizationsoftware.pt.model.ProjectPatchLink;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.ProjectPhase;
import org.openimmunizationsoftware.pt.model.ReviewInterval;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.Workspace;

public class ProjectHealthPageRenderer {

        // Project Health is intentionally separate from daily execution. It focuses on
        // project triage, maintenance, and review planning.
        private static final boolean DEV_LABELS_ENABLED = false;

        public void render(AppReq appReq, ProjectHealthPageModel model) {
                PrintWriter out = appReq.getOut();
                printStyles(out);

                String rootClass = "ph-page" + (DEV_LABELS_ENABLED ? " ph-dev-labels-enabled" : "");
                out.println("<div class=\"" + rootClass + "\">");
                out.println("  <div class=\"ph-intro\">");
                out.println("    <div style=\"overflow:auto;\">");
                out.println("      <h1 style=\"float:left;\">Project Health</h1>");
                printContextSelector(out, model);
                out.println("    </div>");
                out.println(
                                "    <p>Project triage and maintenance workspace. Use this page to keep projects healthy, not to run daily execution.</p>");
                out.println("  </div>");

                out.println("  <div class=\"ph-shell\">");
                out.println("    <div class=\"ph-col ph-col-left\">");
                printProjectColumn(out, model);
                out.println("    </div>");

                out.println("    <div class=\"ph-col ph-col-center\">");
                printReportColumn(out, model);
                out.println("    </div>");

                out.println("    <div class=\"ph-col ph-col-right\">");
                printIssuesAndActions(out, model);
                out.println("    </div>");
                out.println("  </div>");

                if (model.isSelectedProjectAvailable()) {
                        printProjectEditModal(out, appReq, model, model.getSelectedProjectId());
                }
                printReprioritizeModal(out, model);
                printScheduleReviewModal(out, model);
                printBulkImportModal(out, model);
                printReviewUnscheduledModal(out, model);
                printScripts(out, model);
                out.println("</div>");
        }

        private void printProjectColumn(PrintWriter out, ProjectHealthPageModel model) {
                out.println("<div class=\"ph-section ph-panel\">");
                printDevLabel(out, "PROJECTS WORK");
                out.println("  <div class=\"ph-section-title-row\">");
                out.println("    <h2>Work Projects</h2>");
                out.println("    <span class=\"ph-row-actions\">");
                out.println(
                                "      <button type=\"button\" class=\"ph-emoji-btn\" title=\"Add project\" onclick=\"phOpenProjectCreateModal(event)\">➕</button>");
                if (model.isSelectedProjectAvailable()) {
                        out.println(
                                        "      <button type=\"button\" class=\"ph-emoji-btn\" title=\"Edit selected project\" onclick=\"phOpenProjectEditModal(event)\">✏️</button>");
                        out.println(
                                        "      <button type=\"button\" class=\"ph-emoji-btn\" title=\"Reprioritize selected project\" onclick=\"phOpenReprioritizeProjectModal(event)\">↕️</button>");
                }
                out.println("    </span>");
                out.println("  </div>");
                printProjectCadenceGroups(out, model.getWorkProjectGroups(), "WORK");
                printPhasedSection(out, model.getPausedWorkProjects(), "Paused");
                printPhasedSection(out, model.getCompletedWorkProjects(), "Complete");

                out.println("  <div class=\"ph-divider\"></div>");
                out.println("  <div class=\"ph-section-title-row ph-section-title-row-sub\">");
                out.println("    <h2>Personal Projects</h2>");
                out.println("  </div>");
                printDevLabel(out, "PROJECTS PERSONAL");
                printProjectCadenceGroups(out, model.getPersonalProjectGroups(), "PERSONAL");
                printPhasedSection(out, model.getPausedPersonalProjects(), "Paused");
                printPhasedSection(out, model.getCompletedPersonalProjects(), "Complete");
                out.println("</div>");
        }

        private void printPhasedSection(PrintWriter out, List<ProjectListItemModel> projects, String sectionLabel) {
                if (projects == null || projects.isEmpty()) {
                        return;
                }
                out.println("  <h4 class=\"ph-bucket-title\">" + escapeHtml(sectionLabel) + "</h4>");
                printSimpleProjectTable(out, projects);
        }

        private void printProjectCadenceGroups(PrintWriter out, List<ProjectCadenceGroupModel> groups, String section) {
                boolean renderedAny = false;
                for (ProjectCadenceGroupModel group : groups) {
                        if (group.getProjects() == null || group.getProjects().isEmpty()) {
                                continue;
                        }
                        out.println("  <h4 class=\"ph-bucket-title\">" + escapeHtml(group.getGroupLabel()) + "</h4>");
                        printDevLabel(out, "PROJECTS " + section + " " + group.getGroupKey());
                        printProjectTable(out, group.getProjects());
                        renderedAny = true;
                }
                if (!renderedAny) {
                        out.println("  <p class=\"ph-subtle\">No projects available.</p>");
                }
        }

        private void printProjectTable(PrintWriter out, List<ProjectListItemModel> projects) {
                out.println("  <table class=\"ph-project-table\">");
                out.println("    <tr><th>Project</th><th>Health</th><th>Actions</th></tr>");
                if (projects.isEmpty()) {
                        out.println("    <tr><td colspan=\"3\" class=\"ph-subtle\">No projects available.</td></tr>");
                }
                for (ProjectListItemModel item : projects) {
                        String rowClass = item.isSelected() ? " class=\"ph-selected\"" : "";
                        out.println("    <tr" + rowClass + ">");
                        out.println("      <td><a class=\"ph-project-link\" href=\"ProjectHealthServlet?projectId="
                                        + item.getProjectId()
                                        + "\">" + escapeHtml(item.getProjectName()) + "</a></td>");

                        String healthClass = "ph-health-ok";
                        String healthIcon = "●";
                        if (item.getHealthLevel() == ProjectListItemModel.HealthLevel.ATTENTION_NEEDED) {
                                healthClass = "ph-health-bad";
                                healthIcon = "!";
                        } else if (item.getHealthLevel() == ProjectListItemModel.HealthLevel.NEEDS_REVIEW) {
                                healthClass = "ph-health-warn";
                                healthIcon = "~";
                        }
                        out.println("      <td><span class=\"ph-health " + healthClass + "\" title=\"overdue: "
                                        + item.getOverdueOpenCount() + ", undated: " + item.getUndatedOpenCount()
                                        + "\">"
                                        + healthIcon + " " + escapeHtml(item.getHealthLabel()) + "</span></td>");

                        out.println("      <td><span class=\"ph-row-actions\">");
                        out.println(
                                        "        <button class=\"ph-mini-btn\" type=\"button\" title=\"Reprioritize\" onclick=\"phOpenReprioritizeProjectModalFor("
                                                        + item.getProjectId() + ", event)\">↕️</button>");
                        out.println("        <a class=\"ph-mini-btn-link\" title=\"Select\" href=\"ProjectHealthServlet?projectId="
                                        + item.getProjectId() + "\">→</a>");
                        out.println("      </span></td>");
                        out.println("    </tr>");
                }
                out.println("  </table>");
        }

        private void printSimpleProjectTable(PrintWriter out, List<ProjectListItemModel> projects) {
                out.println("  <table class=\"ph-project-table\">");
                out.println("    <tr><th>Project</th><th>Actions</th></tr>");
                for (ProjectListItemModel item : projects) {
                        String rowClass = item.isSelected() ? " class=\"ph-selected\"" : "";
                        out.println("    <tr" + rowClass + ">");
                        out.println("      <td><a class=\"ph-project-link\" href=\"ProjectHealthServlet?projectId="
                                        + item.getProjectId()
                                        + "\">" + escapeHtml(item.getProjectName()) + "</a></td>");
                        out.println("      <td><span class=\"ph-row-actions\">");
                        out.println(
                                        "        <button class=\"ph-mini-btn\" type=\"button\" title=\"Reprioritize\" onclick=\"phOpenReprioritizeProjectModalFor("
                                                        + item.getProjectId() + ", event)\">↕️</button>");
                        out.println("        <a class=\"ph-mini-btn-link\" title=\"Select\" href=\"ProjectHealthServlet?projectId="
                                        + item.getProjectId() + "\">→</a>");
                        out.println("      </span></td>");
                        out.println("    </tr>");
                }
                out.println("  </table>");
        }

        private void printReportColumn(PrintWriter out, ProjectHealthPageModel model) {
                out.println("<div class=\"ph-section ph-panel\">");
                printDevLabel(out, "PROJECT REPORT");
                out.println("  <div class=\"ph-section-title-row\">");
                out.println("    <h2>Project Report</h2>");
                out.println(
                                "    <button type=\"button\" class=\"ph-btn\" onclick=\"phCopyReport(event)\">Copy Report</button>");
                out.println("  </div>");

                if (!model.isSelectedProjectAvailable()) {
                        out.println("  <p class=\"ph-subtle\">Select a project to view a report.</p>");
                        out.println("</div>");
                        return;
                }

                ProjectReportModel report = model.getReport();
                out.println("  <div class=\"ph-brief-grid\">");
                out.println("    <div class=\"ph-key\">Project</div><div class=\"ph-val\">"
                                + escapeHtml(report.getProjectName()) + "</div>");
                out.println("    <div class=\"ph-key\">Category</div><div class=\"ph-val\">"
                                + escapeHtml(report.getCategory())
                                + "</div>");
                out.println("    <div class=\"ph-key\">Phase</div><div class=\"ph-val\">"
                                + escapeHtml(report.getPhase())
                                + "</div>");
                out.println("    <div class=\"ph-key\">Description</div><div class=\"ph-val\">"
                                + escapeHtml(report.getDescription()) + "</div>");
                out.println("  </div>");

                out.println("  <h3>Recent Completed Activity</h3>");
                printReportActionList(out, report.getRecentCompleted(), "No recent completed activity.");

                out.println("  <h3>Current Open Scheduled Actions</h3>");
                printReportActionList(out, report.getScheduledOpen(), "No scheduled open actions.");

                out.println("  <h3 id=\"phUnscheduledActions\">Current Unscheduled / Backlog Actions</h3>");
                printReportActionList(out, report.getUnscheduledOpen(), "No unscheduled open actions.");

                out.println("  <h3>Open Project Issues</h3>");
                printReportActionList(out, report.getOpenProjectIssues(), "No open project issues.");

                out.println("  <h3>Project Narrative (Last 90 Days)</h3>");
                printReportActionList(out, report.getRecentNarratives(), "No narrative entries in the last 90 days.");

                out.println("  <h3>What Needs To Be Done Next</h3>");
                out.println("  <ul class=\"ph-list\">");
                for (String recommendation : report.getNextRecommendations()) {
                        out.println("    <li>" + escapeHtml(recommendation) + "</li>");
                }
                out.println("  </ul>");

                out.println("  <h3>Copy-Friendly Briefing</h3>");
                out.println("  <pre id=\"phReportText\" class=\"ph-report-block\">" + escapeHtml(report.getReportText())
                                + "</pre>");
                out.println("</div>");
        }

        private void printReportActionList(PrintWriter out, List<ProjectReportModel.ReportActionLine> lines,
                        String empty) {
                if (lines == null || lines.isEmpty()) {
                        out.println("  <p class=\"ph-subtle\">" + escapeHtml(empty) + "</p>");
                        return;
                }
                out.println("  <ul class=\"ph-list\">");
                for (ProjectReportModel.ReportActionLine line : lines) {
                        out.println("    <li><span class=\"ph-when\">" + escapeHtml(line.getWhenLabel()) + "</span> "
                                        + escapeHtml(line.getDescription()) + "</li>");
                }
                out.println("  </ul>");
        }

        private void printIssuesAndActions(PrintWriter out, ProjectHealthPageModel model) {
                out.println("<div class=\"ph-section ph-panel\">");
                printDevLabel(out, "PROJECT ISSUES");
                out.println("  <h2>Project Health Issues</h2>");
                if (!model.isSelectedProjectAvailable()) {
                        out.println("  <p class=\"ph-subtle\">Select a project to review health issues.</p>");
                } else if (!model.isHealthCheckApplicable()) {
                        out.println(
                                        "  <p class=\"ph-subtle\">Health check not applicable for paused or complete projects.</p>");
                } else {
                        out.println("  <ul class=\"ph-issues\">");
                        for (ProjectHealthIssueModel issue : model.getIssues()) {
                                String severityClass = "ph-issue-info";
                                if (issue.getSeverity() == ProjectHealthIssueModel.Severity.WARNING) {
                                        severityClass = "ph-issue-warn";
                                } else if (issue.getSeverity() == ProjectHealthIssueModel.Severity.CRITICAL) {
                                        severityClass = "ph-issue-critical";
                                }
                                out.println("    <li class=\"" + severityClass + "\"><strong>"
                                                + escapeHtml(issue.getTitle())
                                                + "</strong><br/>"
                                                + escapeHtml(issue.getDetail()) + "</li>");
                        }
                        out.println("  </ul>");
                }

                out.println("  <div class=\"ph-divider\"></div>");
                printDevLabel(out, "PROJECT ACTIONS");
                out.println("  <h2>Quick Actions</h2>");
                // Bulk import is intentionally hosted here as the conceptual home for project
                // maintenance flows, even while this first cut remains scaffolded.
                out.println("  <div class=\"ph-quick-actions\">");
                out.println(
                                "    <button type=\"button\" class=\"ph-btn\" onclick=\"phOpenReviewScheduleModal(event)\">Schedule Project Review</button>");
                out.println(
                                "    <button type=\"button\" class=\"ph-btn\" onclick=\"phOpenBulkImportModal(event)\">Bulk Import Actions</button>");
                out.println(
                                "    <button type=\"button\" class=\"ph-btn\" onclick=\"phOpenReviewUnscheduledModal(event)\">Review Unscheduled Actions</button>");
                out.println(
                                "    <button type=\"button\" class=\"ph-btn\" onclick=\"phOpenReprioritizeProjectModal(event)\">Reprioritize Project</button>");
                out.println(
                                "    <button type=\"button\" class=\"ph-btn\" onclick=\"phOpenProjectEditModal(event)\">Edit Project</button>");
                out.println("  </div>");

                if (model.isPatchLinksVisible()) {
                        out.println("  <div class=\"ph-divider\"></div>");
                        printDevLabel(out, "PROJECT PATCH LINKS");
                        printProjectLinksSection(out, model);
                }
                out.println("</div>");
        }

        private void printProjectLinksSection(PrintWriter out, ProjectHealthPageModel model) {
                out.println("  <h2>Patch Workspace Links</h2>");
                if (model.getSelectedProjectLinkedPatchWorkspace() == null) {
                        out.println(
                                        "  <p class=\"ph-subtle\">Link a patch workspace in Edit Project to add links.</p>");
                        return;
                }
                out.println("  <p class=\"ph-subtle\">Linked workspace: <strong>"
                                + escapeHtml(model.getSelectedProjectLinkedPatchWorkspace().getWorkspaceName())
                                + "</strong></p>");
                List<ProjectPatchLinkDisplayModel> links = model.getProjectPatchLinks();
                if (!links.isEmpty()) {
                        out.println("  <ul class=\"ph-links-list\">");
                        for (ProjectPatchLinkDisplayModel link : links) {
                                out.println("    <li class=\"ph-link-item\">");
                                if (ProjectPatchLink.LINK_TYPE_DIRECT_PROJECT.equals(link.getLinkType())) {
                                        String name = link.getDirectLinkedProject() != null
                                                        ? n(link.getDirectLinkedProject().getProjectName())
                                                        : "(unknown)";
                                        out.println("      <span>&#128196; " + escapeHtml(name) + "</span>");
                                } else {
                                        String catName = link.getCategoryName() != null ? link.getCategoryName()
                                                        : "(unknown)";
                                        out.println("      <span>&#128193; " + escapeHtml(catName) + "</span>");
                                        List<Project> resolved = link.getResolvedProjects();
                                        if (resolved != null && !resolved.isEmpty()) {
                                                out.println("      <ul class=\"ph-link-resolved\">");
                                                for (Project rp : resolved) {
                                                        out.println("        <li>" + escapeHtml(n(rp.getProjectName()))
                                                                        + "</li>");
                                                }
                                                out.println("      </ul>");
                                        }
                                }
                                out.println("      <button type=\"button\" class=\"ph-mini-btn\" title=\"Remove link\""
                                                + " onclick=\"phRemoveProjectPatchLink(event,"
                                                + link.getProjectPatchLinkId() + "," + model.getSelectedProjectId()
                                                + ")\">&times;</button>");
                                out.println("    </li>");
                        }
                        out.println("  </ul>");
                }
                List<Project> patchProjects = model.getAvailablePatchProjects();
                if (!patchProjects.isEmpty()) {
                        out.println("  <div class=\"ph-patch-link-form\">");
                        out.println("    <select id=\"phAddDirectProjectSelect\">");
                        out.println("      <option value=\"\">Add direct project link...</option>");
                        for (Project p : patchProjects) {
                                out.println("      <option value=\"" + p.getProjectId() + "\">"
                                                + escapeHtml(n(p.getProjectName())) + "</option>");
                        }
                        out.println("    </select>");
                        out.println("    <button type=\"button\" class=\"ph-btn\" onclick=\"phAddDirectProjectLink(event,"
                                        + model.getSelectedProjectId() + ")\">Add</button>");
                        out.println("  </div>");
                }
                List<ProjectCategory> patchCategories = model.getAvailablePatchCategories();
                if (!patchCategories.isEmpty()) {
                        out.println("  <div class=\"ph-patch-link-form\">");
                        out.println("    <select id=\"phAddCategorySelect\">");
                        out.println("      <option value=\"\">Add category link...</option>");
                        for (ProjectCategory cat : patchCategories) {
                                out.println("      <option value=\"" + cat.getProjectCategoryId() + "\">"
                                                + escapeHtml(n(cat.getClientName())) + "</option>");
                        }
                        out.println("    </select>");
                        out.println("    <button type=\"button\" class=\"ph-btn\" onclick=\"phAddCategoryLink(event,"
                                        + model.getSelectedProjectId() + ")\">Add</button>");
                        out.println("  </div>");
                }
        }

        private void printReprioritizeModal(PrintWriter out, ProjectHealthPageModel model) {
                out.println(
                                "<div id=\"phReprioritizeModal\" class=\"ph-modal-overlay\" onclick=\"phCloseReprioritizeModal(event)\">");
                out.println("  <div class=\"ph-modal\" onclick=\"event.stopPropagation()\">");
                out.println("    <div class=\"ph-modal-head\">");
                out.println("      <h3 class=\"ph-modal-title\">Reprioritize Project</h3>");
                out.println(
                                "      <button class=\"ph-modal-close\" onclick=\"phCloseReprioritizeModal(event)\">&times;</button>");
                out.println("    </div>");
                out.println(
                                "    <p class=\"ph-subtle\">Choose where to move this project. Reprioritization follows the same interaction pattern used for action reprioritization.</p>");
                out.println("    <div id=\"phReprioritizeChoices\" class=\"ph-reprio-choices\"></div>");
                out.println("  </div>");
                out.println("</div>");
        }

        private void printScheduleReviewModal(PrintWriter out, ProjectHealthPageModel model) {
                out.println(
                                "<div id=\"phScheduleReviewModal\" class=\"ph-modal-overlay\" onclick=\"phCloseScheduleReviewModal(event)\">");
                out.println("  <div class=\"ph-modal\" onclick=\"event.stopPropagation()\">");
                out.println("    <div class=\"ph-modal-head\">");
                out.println("      <h3 class=\"ph-modal-title\">Schedule Project Review</h3>");
                out.println(
                                "      <button class=\"ph-modal-close\" onclick=\"phCloseScheduleReviewModal(event)\">&times;</button>");
                out.println("    </div>");
                out.println("    <form id=\"phScheduleReviewForm\" onsubmit=\"return phSubmitScheduleReview(event)\">");
                out.println("      <input type=\"hidden\" name=\"action\" value=\"scheduleProjectReview\" />");
                out.println(
                                "      <input type=\"hidden\" name=\"projectId\" value=\""
                                                + model.getSelectedProjectId() + "\" />");
                out.println(
                                "      <div class=\"ph-form-field\">\n        <label for=\"phReviewDate\">Review Date (MM/DD/YYYY)</label>\n        <input id=\"phReviewDate\" name=\"reviewDate\" type=\"text\" value=\"\" />\n      </div>");
                out.println(
                                "      <div class=\"ph-form-actions\">\n        <button type=\"submit\" class=\"ph-btn ph-btn-primary\">Schedule</button>\n        <button type=\"button\" class=\"ph-btn\" onclick=\"phCloseScheduleReviewModal(event)\">Cancel</button>\n      </div>");
                out.println("    </form>");
                out.println("  </div>");
                out.println("</div>");
        }

        private void printBulkImportModal(PrintWriter out, ProjectHealthPageModel model) {
                out.println(
                                "<div id=\"phBulkImportModal\" class=\"ph-modal-overlay\" onclick=\"phCloseBulkImportModal(event)\">");
                out.println("  <div class=\"ph-modal\" onclick=\"event.stopPropagation()\">");
                out.println("    <div class=\"ph-modal-head\">");
                out.println("      <h3 class=\"ph-modal-title\">Bulk Import Actions</h3>");
                out.println(
                                "      <button class=\"ph-modal-close\" onclick=\"phCloseBulkImportModal(event)\">&times;</button>");
                out.println("    </div>");
                out.println(
                                "    <p class=\"ph-subtle\">Paste one action per line. Prefix with \"Project Name:\" to route a line to another project.</p>");
                out.println("    <form id=\"phBulkImportForm\" onsubmit=\"return phSubmitBulkImport(event)\">");
                out.println("      <input type=\"hidden\" name=\"action\" value=\"bulkImportActions\" />");
                out.println(
                                "      <input type=\"hidden\" name=\"projectId\" value=\""
                                                + model.getSelectedProjectId() + "\" />");
                out.println("      <div class=\"ph-form-field\">");
                out.println("        <label for=\"phBulkImportText\">Actions</label>");
                out.println(
                                "        <textarea id=\"phBulkImportText\" name=\"bulkImportText\" rows=\"10\" placeholder=\"I will draft timeline tomorrow&#10;Client Project: I would like to review notes friday\"></textarea>");
                out.println("      </div>");
                out.println(
                                "      <div class=\"ph-form-actions\">\n        <button type=\"submit\" class=\"ph-btn ph-btn-primary\">Import Actions</button>\n        <button type=\"button\" class=\"ph-btn\" onclick=\"phCloseBulkImportModal(event)\">Cancel</button>\n      </div>");
                out.println("    </form>");
                out.println("  </div>");
                out.println("</div>");
        }

        private void printReviewUnscheduledModal(PrintWriter out, ProjectHealthPageModel model) {
                out.println(
                                "<div id=\"phReviewUnscheduledModal\" class=\"ph-modal-overlay\" onclick=\"phCloseReviewUnscheduledModal(event)\">");
                out.println("  <div class=\"ph-modal ph-review-unscheduled-modal\" onclick=\"event.stopPropagation()\">");
                out.println("    <div class=\"ph-modal-head\">");
                out.println("      <h3 class=\"ph-modal-title\">Review Unscheduled Actions</h3>");
                out.println(
                                "      <button class=\"ph-modal-close\" onclick=\"phCloseReviewUnscheduledModal(event)\">&times;</button>");
                out.println("    </div>");
                out.println(
                                "    <p class=\"ph-subtle\">Select unscheduled actions to cancel, then paste replacement actions to bulk import.</p>");
                out.println("    <form id=\"phReviewUnscheduledForm\" onsubmit=\"return phSubmitReviewUnscheduled(event)\">");
                out.println("      <input type=\"hidden\" name=\"action\" value=\"replaceUnscheduledActions\" />");
                out.println(
                                "      <input type=\"hidden\" name=\"projectId\" value=\""
                                                + model.getSelectedProjectId() + "\" />");
                out.println("      <div id=\"phReviewUnscheduledList\" class=\"ph-review-list\"></div>");
                out.println("      <div class=\"ph-form-field\">");
                out.println("        <label for=\"phReviewBulkImportText\">Replacement Actions</label>");
                out.println(
                                "        <textarea id=\"phReviewBulkImportText\" name=\"bulkImportText\" rows=\"8\" placeholder=\"I will rewrite proposal tomorrow&#10;Another Project: I would like to call vendor friday\"></textarea>");
                out.println("      </div>");
                out.println(
                                "      <div class=\"ph-form-actions\">\n        <button type=\"submit\" class=\"ph-btn ph-btn-primary\">Delete and Bulk Import</button>\n        <button type=\"button\" class=\"ph-btn\" onclick=\"phCloseReviewUnscheduledModal(event)\">Cancel</button>\n      </div>");
                out.println("    </form>");
                out.println("  </div>");
                out.println("</div>");
        }

        @SuppressWarnings("unchecked")
        private void printProjectEditModal(PrintWriter out, AppReq appReq, ProjectHealthPageModel model,
                        int selectedProjectId) {
                Session dataSession = appReq.getDataSession();
                WebUser webUser = appReq.getWebUser();

                Project project = (Project) dataSession.get(Project.class, selectedProjectId);
                if (project == null) {
                        return;
                }

                ProjectContactAssigned projectContactAssigned = loadProjectContactAssigned(webUser, dataSession,
                                project);
                int updateEvery = projectContactAssigned != null && projectContactAssigned.getUpdateDue() != null
                                ? projectContactAssigned.getUpdateDue()
                                : 0;

                out.println(
                                "<div id=\"phProjectEditModal\" class=\"ph-modal-overlay\" onclick=\"phCloseProjectEditModal(event)\">");
                out.println("  <div class=\"ph-modal ph-edit-modal\" onclick=\"event.stopPropagation()\">");
                out.println("    <div class=\"ph-modal-head\">");
                out.println("      <h3 id=\"phProjectModalTitle\" class=\"ph-modal-title\">Edit Project</h3>");
                out.println(
                                "      <button class=\"ph-modal-close\" onclick=\"phCloseProjectEditModal(event)\">&times;</button>");
                out.println("    </div>");
                out.println("    <form id=\"phProjectEditForm\" onsubmit=\"return phSubmitProjectEditForm(event)\">");
                out.println(
                                "      <input id=\"phProjectFormAction\" type=\"hidden\" name=\"action\" value=\"saveProjectEdit\" />");
                out.println("      <input id=\"phProjectId\" type=\"hidden\" name=\"projectId\" value=\""
                                + project.getProjectId() + "\" />");
                out.println("      <input type=\"hidden\" name=\"workspaceContextId\" value=\""
                                + (model.getContextWorkspaceId() == null ? "" : model.getContextWorkspaceId())
                                + "\" />");

                out.println("      <div class=\"ph-form-field\"><label>Project Name</label>");
                out.println("      <input id=\"phProjectName\" type=\"text\" name=\"projectName\" value=\""
                                + escapeHtml(n(project.getProjectName())) + "\" /></div>");

                out.println("      <div class=\"ph-form-field\"><label>Project Handle</label>");
                out.println("      <input id=\"phProjectHandle\" type=\"text\" name=\"projectHandle\" value=\""
                                + escapeHtml(n(project.getProjectHandle())) + "\" /></div>");

                out.println("      <div class=\"ph-form-field\"><label>Category</label>");
                out.println("      <select id=\"phProjectCategory\" name=\"categoryCode\">");
                Query categoryQuery = dataSession
                                .createQuery("from ProjectCategory where workspaceId = :workspaceId order by sortOrder, clientName");
                Integer workspaceId = model.getContextWorkspaceId() != null ? model.getContextWorkspaceId()
                                : appReq.getActiveWorkspaceId();
                categoryQuery.setParameter("workspaceId", workspaceId);
                List<ProjectCategory> categoryList = categoryQuery.list();
                for (ProjectCategory category : categoryList) {
                        String categoryCode = category.getCategoryCode();
                        if (categoryCode != null && categoryCode.startsWith("PER-")) {
                                String expectedPersonalCategory = "PER-" + webUser.getContactId();
                                if (!categoryCode.equals(expectedPersonalCategory)) {
                                        continue;
                                }
                        }
                        String selected = categoryCode != null && categoryCode.equals(project.getCategoryCode())
                                        ? " selected"
                                        : "";
                        out.println("        <option value=\"" + escapeHtml(n(categoryCode)) + "\"" + selected + ">"
                                        + escapeHtml(category.getClientNameForDropdown()) + "</option>");
                }
                out.println("      </select></div>");

                out.println("      <div class=\"ph-form-field\"><label>Project Icon</label>");
                out.println("      <input id=\"phProjectIcon\" type=\"text\" name=\"projectIcon\" value=\""
                                + escapeHtml(n(project.getProjectIcon())) + "\" /></div>");

                out.println("      <div class=\"ph-form-field\"><label>Description</label>");
                out.println("      <textarea id=\"phProjectDescription\" name=\"description\" rows=\"3\">"
                                + escapeHtml(n(project.getDescription())) + "</textarea></div>");

                out.println("      <div class=\"ph-form-field\"><label>Project Outcome</label>");
                out.println("      <textarea id=\"phProjectOutcomeText\" name=\"outcomeText\" rows=\"4\">"
                                + escapeHtml(n(project.getOutcomeText())) + "</textarea></div>");

                out.println("      <div class=\"ph-form-field\"><label>Success Criteria</label>");
                out.println("      <textarea id=\"phProjectSuccessCriteriaText\" name=\"successCriteriaText\" rows=\"5\">"
                                + escapeHtml(n(project.getSuccessCriteriaText())) + "</textarea>");
                out.println("      <div class=\"ph-subtle\">Enter one success criterion per line.</div></div>");

                out.println("      <div class=\"ph-form-field\"><label>Phase</label>");
                out.println("      <select id=\"phProjectPhase\" name=\"phaseCode\">");
                Query phaseQuery = dataSession.createQuery("from ProjectPhase");
                List<ProjectPhase> phaseList = phaseQuery.list();
                for (ProjectPhase phase : phaseList) {
                        String selected = phase.getPhaseCode() != null
                                        && phase.getPhaseCode().equals(project.getPhaseCode())
                                                        ? " selected"
                                                        : "";
                        out.println("        <option value=\"" + escapeHtml(n(phase.getPhaseCode())) + "\"" + selected
                                        + ">"
                                        + escapeHtml(phase.getPhaseLabel()) + "</option>");
                }
                out.println("      </select></div>");

                out.println("      <div class=\"ph-form-field\"><label>Bill Code</label>");
                out.println("      <select id=\"phProjectBillCode\" name=\"billCode\"><option value=\"\">(none)</option>");
                workspaceId = model.getContextWorkspaceId() != null ? model.getContextWorkspaceId()
                                : appReq.getActiveWorkspaceId();
                if (workspaceId == null && webUser != null) {
                        workspaceId = org.openimmunizationsoftware.pt.WorkspaceRegistry
                                        .getWorkspaceIdForWebUserId(webUser.getWebUserId());
                }
                Query billCodeQuery = dataSession
                                .createQuery("from BillCode where workspaceId = :workspaceId and visible = 'Y' order by billLabel");
                billCodeQuery.setParameter("workspaceId", workspaceId);
                List<BillCode> billCodes = billCodeQuery.list();
                for (BillCode billCode : billCodes) {
                        String selected = billCode.getBillCode() != null
                                        && billCode.getBillCode().equals(project.getBillCode())
                                                        ? " selected"
                                                        : "";
                        out.println("        <option value=\"" + escapeHtml(n(billCode.getBillCode())) + "\"" + selected
                                        + ">"
                                        + escapeHtml(billCode.getBillLabel()) + "</option>");
                }
                out.println("      </select></div>");

                if (model.isPatchLinksVisible()) {
                        out.println("      <div class=\"ph-form-field\"><label>Linked Patch Workspace</label>");
                        boolean patchDisabled = !model.isCanChangePatchWorkspace();
                        out.println("      <select id=\"phProjectLinkedPatchWorkspaceId\" name=\"linkedPatchWorkspaceId\""
                                        + (patchDisabled ? " disabled" : "") + ">");
                        out.println("        <option value=\"\">(none)</option>");
                        for (Workspace patchWs : model.getAccessiblePatchWorkspaces()) {
                                boolean sel = model.getSelectedProjectLinkedPatchWorkspaceId() != null
                                                && model.getSelectedProjectLinkedPatchWorkspaceId()
                                                                .intValue() == patchWs.getWorkspaceId();
                                out.println("        <option value=\"" + patchWs.getWorkspaceId() + "\""
                                                + (sel ? " selected" : "") + ">"
                                                + escapeHtml(patchWs.getWorkspaceName()) + "</option>");
                        }
                        out.println("      </select>");
                        if (patchDisabled) {
                                out.println(
                                                "      <div class=\"ph-subtle\">Remove all links before changing the patch workspace.</div>");
                        }
                        out.println("      </div>");
                }

                out.println("      <div class=\"ph-form-field\"><label>Update Every</label>");
                out.println(
                                "      <select id=\"phProjectUpdateEvery\" name=\"updateEvery\">\n        <option value=\"0\">none</option>");
                boolean selectedInterval = false;
                if (updateEvery == 0) {
                        selectedInterval = true;
                }
                for (ReviewInterval interval : ReviewInterval.values()) {
                        boolean selected = !selectedInterval && updateEvery > 0 && updateEvery <= interval.getDays();
                        out.println("        <option value=\"" + interval.getDays() + "\""
                                        + (selected ? " selected" : "") + ">"
                                        + escapeHtml(interval.getDescription()) + "</option>");
                        if (selected) {
                                selectedInterval = true;
                        }
                }
                out.println("      </select></div>");

                out.println(
                                "      <div class=\"ph-form-actions\">\n        <button id=\"phProjectSubmitBtn\" type=\"submit\" class=\"ph-btn ph-btn-primary\">Save Project</button>\n        <button type=\"button\" class=\"ph-btn\" onclick=\"phCloseProjectEditModal(event)\">Cancel</button>\n      </div>");
                out.println("    </form>");
                out.println("  </div>");
                out.println("</div>");

                out.println("<script>");
                out.println("window.phProjectDefaults = {");
                out.println("  projectId: '" + project.getProjectId() + "',");
                out.println("  projectName: '" + escapeJsString(n(project.getProjectName())) + "',");
                out.println("  projectHandle: '" + escapeJsString(n(project.getProjectHandle())) + "',");
                out.println("  categoryCode: '" + escapeJsString(n(project.getCategoryCode())) + "',");
                out.println("  projectIcon: '" + escapeJsString(n(project.getProjectIcon())) + "',");
                out.println("  description: '" + escapeJsString(n(project.getDescription())) + "',");
                out.println("  outcomeText: '" + escapeJsString(n(project.getOutcomeText())) + "',");
                out.println("  successCriteriaText: '" + escapeJsString(n(project.getSuccessCriteriaText())) + "',");
                out.println("  phaseCode: '" + escapeJsString(n(project.getPhaseCode())) + "',");
                out.println("  billCode: '" + escapeJsString(n(project.getBillCode())) + "',");
                out.println("  updateEvery: '" + updateEvery + "',");
                String linkedPatchWsId = project.getLinkedPatchWorkspaceId() != null
                                ? project.getLinkedPatchWorkspaceId().toString()
                                : "";
                out.println("  linkedPatchWorkspaceId: '" + linkedPatchWsId + "'\n};");
                out.println(
                                "  function phAddDirectProjectLink(evt, projectId) { if (evt) { evt.preventDefault(); } var sel = document.getElementById('phAddDirectProjectSelect'); if (!sel || !sel.value) { alert('Select a project to link.'); return false; } var formData = new URLSearchParams(); formData.append('action','addDirectProjectLink'); formData.append('projectId', projectId); formData.append('patchProjectId', sel.value); fetch('ProjectHealthServlet', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: formData.toString() }).then(function(r){ return r.json(); }).then(function(data){ if (data && data.success) { window.location.reload(); } else { alert((data && data.message) ? data.message : 'Unable to add link'); } }).catch(function(){ alert('Unable to add link'); }); return false; }");
                out.println(
                                "  function phAddCategoryLink(evt, projectId) { if (evt) { evt.preventDefault(); } var sel = document.getElementById('phAddCategorySelect'); if (!sel || !sel.value) { alert('Select a category to link.'); return false; } var formData = new URLSearchParams(); formData.append('action','addCategoryLink'); formData.append('projectId', projectId); formData.append('patchCategoryId', sel.value); fetch('ProjectHealthServlet', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: formData.toString() }).then(function(r){ return r.json(); }).then(function(data){ if (data && data.success) { window.location.reload(); } else { alert((data && data.message) ? data.message : 'Unable to add link'); } }).catch(function(){ alert('Unable to add link'); }); return false; }");
                out.println(
                                "  function phRemoveProjectPatchLink(evt, linkId, projectId) { if (evt) { evt.preventDefault(); } if (!confirm('Remove this link?')) { return false; } var formData = new URLSearchParams(); formData.append('action','removeProjectPatchLink'); formData.append('projectPatchLinkId', linkId); formData.append('projectId', projectId); fetch('ProjectHealthServlet', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: formData.toString() }).then(function(r){ return r.json(); }).then(function(data){ if (data && data.success) { window.location.reload(); } else { alert((data && data.message) ? data.message : 'Unable to remove link'); } }).catch(function(){ alert('Unable to remove link'); }); return false; }");
                out.println("</script>");
        }

        private void printContextSelector(PrintWriter out, ProjectHealthPageModel model) {
                if (!model.isShowContextSelector()) {
                        return;
                }
                out.println("      <form method=\"GET\" action=\"ProjectHealthServlet\" style=\"float:right;margin-top:10px;\">");
                out.println("        <input type=\"hidden\" name=\"action\" value=\"setContext\" />");
                out.println("        <label for=\"phPatchWorkspaceId\">Dandelion Patch:</label>");
                out.println("        <select id=\"phPatchWorkspaceId\" name=\"patchWorkspaceId\" onchange=\"this.form.submit()\">\n"
                                + "          <option value=\"\">Private</option>");
                for (Workspace workspace : model.getAccessiblePatchWorkspaces()) {
                        boolean selected = model.getContextWorkspaceId() != null
                                        && model.getContextWorkspaceId().intValue() == workspace.getWorkspaceId();
                        out.println("          <option value=\"" + workspace.getWorkspaceId() + "\""
                                        + (selected ? " selected" : "") + ">"
                                        + escapeHtml(workspace.getWorkspaceName()) + "</option>");
                }
                out.println("        </select>");
                out.println("      </form>");
        }

        private void printScripts(PrintWriter out, ProjectHealthPageModel model) {
                out.println("<script>");
                out.println("  window.phSelectedProjectId = " + model.getSelectedProjectId() + ";");
                out.println("  function phCopyReport(evt) {");
                out.println("    if (evt) { evt.preventDefault(); }");
                out.println("    var reportEl = document.getElementById('phReportText');");
                out.println("    if (!reportEl) { return false; }");
                out.println("    var text = reportEl.textContent || reportEl.innerText || ''; ");
                out.println("    if (navigator.clipboard && navigator.clipboard.writeText) {");
                out.println("      navigator.clipboard.writeText(text).then(function(){ alert('Project report copied.'); });");
                out.println("    } else {");
                out.println(
                                "      var t = document.createElement('textarea'); t.value = text; document.body.appendChild(t); t.select();");
                out.println(
                                "      try { document.execCommand('copy'); alert('Project report copied.'); } catch (e) { alert('Copy not supported in this browser.'); }");
                out.println("      document.body.removeChild(t);");
                out.println("    }");
                out.println("    return false;");
                out.println("  }");

                out.println(
                                "  function phOpenProjectEditModal(evt) { if (evt) { evt.preventDefault(); } phSetProjectFormMode(false); var m = document.getElementById('phProjectEditModal'); if (m) { m.classList.add('ph-modal-open'); } }");
                out.println(
                                "  function phOpenProjectCreateModal(evt) { if (evt) { evt.preventDefault(); } phSetProjectFormMode(true); var m = document.getElementById('phProjectEditModal'); if (m) { m.classList.add('ph-modal-open'); } }");
                out.println(
                                "  function phCloseProjectEditModal(evt) { if (evt) { evt.preventDefault(); evt.stopPropagation(); } var m = document.getElementById('phProjectEditModal'); if (m) { m.classList.remove('ph-modal-open'); } return false; }");
                out.println("  function phSetProjectFormMode(isCreate) {");
                out.println(
                                "    var defaults = window.phProjectDefaults || {}; var actionField = document.getElementById('phProjectFormAction'); var projectId = document.getElementById('phProjectId');");
                out.println(
                                "    var title = document.getElementById('phProjectModalTitle'); var submit = document.getElementById('phProjectSubmitBtn');");
                out.println("    if (!isCreate) {");
                out.println(
                                "      if (actionField) { actionField.value = 'saveProjectEdit'; } if (projectId) { projectId.value = defaults.projectId || ''; }");
                out.println(
                                "      if (title) { title.textContent = 'Edit Project'; } if (submit) { submit.textContent = 'Save Project'; }");
                out.println("      phSetProjectDefaults();");
                out.println("    } else {");
                out.println(
                                "      if (actionField) { actionField.value = 'saveProjectCreate'; } if (projectId) { projectId.value = ''; }");
                out.println(
                                "      if (title) { title.textContent = 'Add Project'; } if (submit) { submit.textContent = 'Add Project'; }");
                out.println("      phClearProjectCreate();");
                out.println("    }");
                out.println("  }");
                out.println("  function phSetProjectDefaults() {");
                out.println("    var d = window.phProjectDefaults || {}; ");
                out.println(
                                "    var map = [['phProjectName','projectName'],['phProjectHandle','projectHandle'],['phProjectCategory','categoryCode'],['phProjectIcon','projectIcon'],['phProjectDescription','description'],['phProjectOutcomeText','outcomeText'],['phProjectSuccessCriteriaText','successCriteriaText'],['phProjectPhase','phaseCode'],['phProjectBillCode','billCode'],['phProjectUpdateEvery','updateEvery'],['phProjectLinkedPatchWorkspaceId','linkedPatchWorkspaceId']];");
                out.println(
                                "    for (var i=0;i<map.length;i++){ var el=document.getElementById(map[i][0]); if (el) { el.value = d[map[i][1]] || ''; } }");
                out.println("  }");
                out.println(
                                "  function phClearProjectCreate() { var f=document.getElementById('phProjectEditForm'); if (f) { f.reset(); } var b=document.getElementById('phProjectBillCode'); if (b) { b.value=''; } var u=document.getElementById('phProjectUpdateEvery'); if (u) { u.value='0'; } var lp=document.getElementById('phProjectLinkedPatchWorkspaceId'); if (lp) { lp.value=''; } }");
                out.println("  function phSubmitProjectEditForm(evt) {");
                out.println(
                                "    evt.preventDefault(); var f=document.getElementById('phProjectEditForm'); if (!f) { return false; }");
                out.println("    var formData = new URLSearchParams(new FormData(f));");
                out.println(
                                "    fetch('DandelionDashboardServlet', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: formData.toString() })");
                out.println("      .then(function(r){ return r.json(); })");
                out.println(
                                "      .then(function(data){ if (data && data.success) { window.location.reload(); } else { alert((data && data.message) ? data.message : 'Unable to save project'); } })");
                out.println("      .catch(function(){ alert('Unable to save project'); });");
                out.println("    return false;");
                out.println("  }");

                out.println(
                                "  function phOpenReprioritizeProjectModal(evt) { phOpenReprioritizeProjectModalFor(window.phSelectedProjectId, evt); }");
                out.println("  function phOpenReprioritizeProjectModalFor(projectId, evt) {");
                out.println("    if (evt) { evt.preventDefault(); evt.stopPropagation(); }");
                out.println("    if (!projectId) { return false; }");
                out.println(
                                "    var m=document.getElementById('phReprioritizeModal'); if (m) { m.classList.add('ph-modal-open'); }");
                out.println(
                                "    var c=document.getElementById('phReprioritizeChoices'); if (c) { c.innerHTML = '<p class=\\\"ph-subtle\\\">Loading...</p>'; }");
                out.println(
                                "    var formData = new URLSearchParams(); formData.append('action','loadProjectReprioritizeData'); formData.append('projectId', projectId);");
                out.println(
                                "    fetch('ProjectHealthServlet', { method:'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: formData.toString() })");
                out.println("      .then(function(r){ return r.json(); })");
                out.println("      .then(function(data){");
                out.println("        if (!c) { return; }");
                out.println(
                                "        if (!data || !data.success) { c.innerHTML = '<p class=\\\"ph-subtle\\\">Could not load options</p>'; return; }");
                out.println(
                                "        var html = ''; html += '<button type=\\\"button\\\" class=\\\"ph-btn ph-btn-block\\\" onclick=\\\"phMoveProject(projectId, null, &quot;FIRST&quot;, event)\\\">Move to first in this review period</button>'; html += '<button type=\\\"button\\\" class=\\\"ph-btn ph-btn-block\\\" onclick=\\\"phMoveProject(projectId, null, &quot;LAST&quot;, event)\\\">Move to last in this review period</button>'; var projects = data.projects || []; if (projects.length > 0) { for (var i=0;i<projects.length;i++) { var p=projects[i]; html += '<button type=\\\"button\\\" class=\\\"ph-btn ph-btn-block\\\" onclick=\\\"phMoveProject(projectId,' + p.id + ', &quot;BEFORE&quot;, event)\\\">Move before ' + phEscapeHtml(p.name || 'project') + '</button>'; } } else { html += '<p class=\\\"ph-subtle\\\">No other projects in this review period.</p>'; }");
                out.println("        c.innerHTML = html;");
                out.println("      })");
                out.println(
                                "      .catch(function(){ if (c) { c.innerHTML = '<p class=\\\"ph-subtle\\\">Could not load options</p>'; } });");
                out.println("    return false;");
                out.println("  }");
                out.println("  function phMoveProject(projectId, beforeProjectId, mode, evt) {");
                out.println("    if (evt) { evt.preventDefault(); evt.stopPropagation(); }");
                out.println(
                                "    var formData = new URLSearchParams(); formData.append('action','reprioritizeProject'); formData.append('projectId', projectId); if (beforeProjectId) { formData.append('beforeProjectId', beforeProjectId); } formData.append('moveMode', mode || 'BEFORE');");
                out.println(
                                "    fetch('ProjectHealthServlet', { method:'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: formData.toString() })");
                out.println("      .then(function(r){ return r.json(); })");
                out.println(
                                "      .then(function(data){ if (data && data.success) { window.location.reload(); } else { alert((data && data.message) ? data.message : 'Unable to reprioritize project'); } })");
                out.println("      .catch(function(){ alert('Unable to reprioritize project'); });");
                out.println("    return false;");
                out.println("  }");
                out.println(
                                "  function phCloseReprioritizeModal(evt) { if (evt) { evt.preventDefault(); evt.stopPropagation(); } var m=document.getElementById('phReprioritizeModal'); if (m) { m.classList.remove('ph-modal-open'); } return false; }");

                out.println(
                                "  function phOpenReviewScheduleModal(evt) { if (evt) { evt.preventDefault(); } var m=document.getElementById('phScheduleReviewModal'); if (m) { m.classList.add('ph-modal-open'); } var d=document.getElementById('phReviewDate'); if (d && (!d.value || d.value.trim() === '')) { var t=new Date(); var mm=('0' + (t.getMonth()+1)).slice(-2); var dd=('0' + t.getDate()).slice(-2); var yyyy=t.getFullYear(); d.value = mm + '/' + dd + '/' + yyyy; } }");
                out.println(
                                "  function phCloseScheduleReviewModal(evt) { if (evt) { evt.preventDefault(); evt.stopPropagation(); } var m=document.getElementById('phScheduleReviewModal'); if (m) { m.classList.remove('ph-modal-open'); } return false; }");
                out.println("  function phSubmitScheduleReview(evt) {");
                out.println(
                                "    evt.preventDefault(); var f = document.getElementById('phScheduleReviewForm'); if (!f) { return false; }");
                out.println("    var formData = new URLSearchParams(new FormData(f));");
                out.println(
                                "    fetch('ProjectHealthServlet', { method:'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: formData.toString() })");
                out.println("      .then(function(r){ return r.json(); })");
                out.println(
                                "      .then(function(data){ if (data && data.success) { window.location.reload(); } else { alert((data && data.message) ? data.message : 'Unable to schedule review'); } })");
                out.println("      .catch(function(){ alert('Unable to schedule review'); });");
                out.println("    return false;");
                out.println("  }");

                out.println(
                                "  function phOpenBulkImportModal(evt) { if (evt) { evt.preventDefault(); } if (!window.phSelectedProjectId) { alert('Select a project before bulk importing actions.'); return false; } var m=document.getElementById('phBulkImportModal'); if (m) { m.classList.add('ph-modal-open'); } var t=document.getElementById('phBulkImportText'); if (t) { t.focus(); } return false; }");
                out.println(
                                "  function phCloseBulkImportModal(evt) { if (evt) { evt.preventDefault(); evt.stopPropagation(); } var m=document.getElementById('phBulkImportModal'); if (m) { m.classList.remove('ph-modal-open'); } return false; }");
                out.println("  function phSubmitBulkImport(evt) {");
                out.println(
                                "    evt.preventDefault(); var f = document.getElementById('phBulkImportForm'); if (!f) { return false; }");
                out.println("    var formData = new URLSearchParams(new FormData(f));");
                out.println(
                                "    fetch('ProjectHealthServlet', { method:'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: formData.toString() })");
                out.println("      .then(function(r){ return r.json(); })");
                out.println(
                                "      .then(function(data){ if (data && data.success) { var count = data.importedCount || 0; alert('Imported ' + count + ' actions.'); window.location.reload(); } else { alert((data && data.message) ? data.message : 'Unable to import actions'); } })");
                out.println("      .catch(function(){ alert('Unable to import actions'); });");
                out.println("    return false;");
                out.println("  }");

                out.println(
                                "  function phOpenReviewUnscheduledModal(evt) { if (evt) { evt.preventDefault(); } if (!window.phSelectedProjectId) { alert('Select a project before reviewing unscheduled actions.'); return false; } var m=document.getElementById('phReviewUnscheduledModal'); if (m) { m.classList.add('ph-modal-open'); } phLoadReviewUnscheduledList(); return false; }");
                out.println(
                                "  function phCloseReviewUnscheduledModal(evt) { if (evt) { evt.preventDefault(); evt.stopPropagation(); } var m=document.getElementById('phReviewUnscheduledModal'); if (m) { m.classList.remove('ph-modal-open'); } return false; }");
                out.println("  function phLoadReviewUnscheduledList() {");
                out.println(
                                "    var list=document.getElementById('phReviewUnscheduledList'); if (!list) { return; } list.innerHTML = '<p class=\\\"ph-subtle\\\">Loading unscheduled actions...</p>'; ");
                out.println(
                                "    var formData = new URLSearchParams(); formData.append('action','loadUnscheduledReviewData');");
                out.println(
                                "    fetch('ProjectHealthServlet', { method:'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: formData.toString() })");
                out.println(
                                "      .then(function(r){ return r.text().then(function(text){ var data = null; try { data = text ? JSON.parse(text) : null; } catch (e) { throw new Error('Unexpected server response (' + r.status + ')'); } if (!r.ok) { var message = (data && data.message) ? data.message : ('Request failed (' + r.status + ')'); throw new Error(message); } return data; }); })");
                out.println("      .then(function(data){");
                out.println(
                                "        if (!data || !data.success) { var message = (data && data.message) ? data.message : 'Could not load unscheduled actions.'; list.innerHTML = '<p class=\\\"ph-subtle\\\">' + phEscapeHtml(message) + '</p>'; return; }");
                out.println(
                                "        var projects = data.projects || []; if (projects.length === 0) { list.innerHTML = '<p class=\\\"ph-subtle\\\">No unscheduled actions found.</p>'; return; }");
                out.println("        var html = ''; ");
                out.println(
                                "        for (var i=0;i<projects.length;i++){ var p=projects[i]; html += '<div class=\\\"ph-review-project\\\">'; html += '<div class=\\\"ph-review-project-title\\\">' + phEscapeHtml(p.projectName || 'Project') + '</div>'; var actions = p.actions || []; for (var j=0;j<actions.length;j++){ var a=actions[j]; html += '<label class=\\\"ph-review-action\\\"><input type=\\\"checkbox\\\" name=\\\"selectedActionId\\\" value=\\\"' + a.actionId + '\\\" /> <span>' + (a.descriptionHtml || '') + '</span></label>'; } html += '</div>'; }");
                out.println("        list.innerHTML = html;");
                out.println("      })");
                out.println(
                                "      .catch(function(err){ var message = (err && err.message) ? err.message : 'Could not load unscheduled actions.'; list.innerHTML = '<p class=\\\"ph-subtle\\\">' + phEscapeHtml(message) + '</p>'; });");
                out.println("  }");
                out.println("  function phSubmitReviewUnscheduled(evt) {");
                out.println(
                                "    evt.preventDefault(); var form = document.getElementById('phReviewUnscheduledForm'); if (!form) { return false; }");
                out.println(
                                "    var checked = form.querySelectorAll('input[name=\\\"selectedActionId\\\"]:checked'); if (!checked || checked.length === 0) { alert('Select at least one unscheduled action to delete.'); return false; }");
                out.println("    var formData = new URLSearchParams();");
                out.println(
                                "    formData.append('action', 'replaceUnscheduledActions'); formData.append('projectId', window.phSelectedProjectId || '0');");
                out.println(
                                "    var bulkText = document.getElementById('phReviewBulkImportText'); formData.append('bulkImportText', bulkText ? (bulkText.value || '') : '');");
                out.println(
                                "    for (var i=0;i<checked.length;i++) { formData.append('selectedActionId', checked[i].value); }");
                out.println(
                                "    fetch('ProjectHealthServlet', { method:'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: formData.toString() })");
                out.println("      .then(function(r){ return r.json(); })");
                out.println(
                                "      .then(function(data){ if (data && data.success) { var cancelled = data.cancelledCount || 0; var imported = data.importedCount || 0; alert('Cancelled ' + cancelled + ' actions and imported ' + imported + ' actions.'); window.location.reload(); } else { alert((data && data.message) ? data.message : 'Unable to replace unscheduled actions'); } })");
                out.println("      .catch(function(){ alert('Unable to replace unscheduled actions'); });");
                out.println("    return false;");
                out.println("  }");

                out.println(
                                "  function phEscapeHtml(value) { return (value || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\"/g, '&quot;').replace(/'/g, '&#39;'); }");
                out.println("</script>");
        }

        private void printStyles(PrintWriter out) {
                out.println("<style>");
                out.println(
                                "  .ph-page { padding: 12px 18px 24px 18px; background: linear-gradient(180deg, #f4f0e8 0%, #efe7db 40%, #f8f6f1 100%); min-height: calc(100vh - 70px); box-sizing: border-box; }");
                out.println("  .ph-intro { margin-bottom: 12px; }");
                out.println("  .ph-intro h1 { margin: 0 0 4px 0; font-size: 30px; color: #344636; }");
                out.println("  .ph-intro p { margin: 0; color: #5f685d; }");
                out.println(
                                "  .ph-shell { display: grid; grid-template-columns: 28% 44% 28%; gap: 14px; align-items: start; }");
                out.println(
                                "  .ph-panel { background: #fbf8f2; border: 1px solid #e4dbcd; border-radius: 8px; padding: 12px; box-shadow: 0 1px 0 rgba(255, 255, 255, 0.75) inset; }");
                out.println("  .ph-section { margin-bottom: 12px; }");
                out.println(
                                "  .ph-section-title-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }");
                out.println("  .ph-section-title-row-sub { margin-top: 8px; }");
                out.println("  .ph-section h2 { margin: 0 0 8px 0; font-size: 17px; color: #2f3a2f; }");
                out.println(
                                "  .ph-subtitle { margin: 10px 0 6px 0; font-size: 12px; color: #5f6d5f; text-transform: uppercase; letter-spacing: 0.04em; }");
                out.println(
                                "  .ph-bucket-title { margin: 10px 0 4px 0; font-size: 11px; color: #6a6053; text-transform: uppercase; letter-spacing: 0.04em; }");
                out.println("  .ph-subtle { color: #807667; font-size: 12px; }");

                out.println("  .ph-project-table { width: 100%; border-collapse: collapse; margin-bottom: 8px; }");
                out.println(
                                "  .ph-project-table th, .ph-project-table td { border-bottom: 1px solid #e3d9cb; padding: 6px 4px; font-size: 12px; vertical-align: middle; }");
                out.println(
                                "  .ph-project-table th { text-align: left; color: #5b6558; font-size: 11px; text-transform: uppercase; letter-spacing: 0.04em; }");
                out.println("  .ph-project-link { color: #2e4a30; text-decoration: none; }");
                out.println("  .ph-project-link:hover { text-decoration: underline; }");
                out.println("  .ph-selected { background: #f1f8ea; }");

                out.println(
                                "  .ph-health { font-size: 11px; border-radius: 999px; padding: 3px 7px; border: 1px solid #d5cdc0; display: inline-block; }");
                out.println("  .ph-health-ok { background: #e9f4e9; color: #2d5b34; border-color: #c8dcc9; }");
                out.println("  .ph-health-warn { background: #fff7e5; color: #7c5d18; border-color: #e8d5a6; }");
                out.println("  .ph-health-bad { background: #fdeceb; color: #8d2f2f; border-color: #e6b4b4; }");

                out.println("  .ph-row-actions { display: inline-flex; gap: 6px; align-items: center; }");
                out.println(
                                "  .ph-mini-btn, .ph-mini-btn-link, .ph-emoji-btn { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: 4px; border: 1px solid #cfbea6; background: #f0ebe0; color: #2d3a2d; text-decoration: none; cursor: pointer; font-size: 13px; }");
                out.println(
                                "  .ph-mini-btn:hover, .ph-mini-btn-link:hover, .ph-emoji-btn:hover { background: #e8e1d3; border-color: #bfa982; }");

                out.println(
                                "  .ph-brief-grid { display: grid; grid-template-columns: 125px 1fr; gap: 7px 10px; margin-bottom: 10px; }");
                out.println("  .ph-key { color: #556154; font-weight: bold; font-size: 12px; }");
                out.println("  .ph-val { color: #3f453d; font-size: 12px; }");
                out.println("  .ph-list { margin: 6px 0 10px 18px; padding: 0; }");
                out.println("  .ph-list li { margin-bottom: 5px; color: #364033; font-size: 12px; }");
                out.println("  .ph-when { color: #6f675a; font-weight: bold; margin-right: 6px; }");
                out.println(
                                "  .ph-report-block { white-space: pre-wrap; border: 1px solid #d9ccb8; border-radius: 6px; background: #fffdfa; padding: 10px; font-family: Consolas, 'Courier New', monospace; font-size: 12px; max-height: 360px; overflow: auto; }");

                out.println(
                                "  .ph-issues { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }");
                out.println(
                                "  .ph-issues li { border: 1px solid #dfd5c8; border-radius: 6px; padding: 8px; font-size: 12px; }");
                out.println("  .ph-issue-info { background: #f6f7f5; color: #455045; }");
                out.println("  .ph-issue-warn { background: #fff8e8; color: #71581b; }");
                out.println("  .ph-issue-critical { background: #feefed; color: #7d2c2c; }");

                out.println("  .ph-divider { height: 1px; background: #ddd3c4; margin: 12px 0; }");
                out.println("  .ph-quick-actions { display: flex; flex-direction: column; gap: 8px; }");
                out.println(
                                "  .ph-btn { background: #efe8dc; border: 1px solid #d2c2ab; color: #334233; border-radius: 4px; padding: 7px 10px; cursor: pointer; text-align: left; }");
                out.println("  .ph-btn:hover { background: #e6ddcf; border-color: #bfa982; }");
                out.println("  .ph-btn-primary { background: #dcebd8; border-color: #a9c9a7; }");
                out.println("  .ph-btn-primary:hover { background: #d2e4cf; border-color: #92b990; }");
                out.println("  .ph-btn-block { width: 100%; text-align: left; margin-bottom: 6px; }");

                out.println(
                                "  .ph-modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: none; align-items: center; justify-content: center; z-index: 12000; }");
                out.println("  .ph-modal-overlay.ph-modal-open { display: flex; }");
                out.println(
                                "  .ph-modal { width: min(460px, calc(100vw - 30px)); max-height: calc(100vh - 40px); overflow: auto; background: #fff; border: 1px solid #e4dbcd; border-radius: 6px; box-shadow: 0 10px 28px rgba(0,0,0,0.2); padding: 12px 14px; }");
                out.println(
                                "  .ph-modal-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }");
                out.println("  .ph-modal-title { margin: 0; font-size: 16px; color: #2f3a2f; }");
                out.println(
                                "  .ph-modal-close { background: none; border: none; font-size: 20px; line-height: 1; cursor: pointer; color: #6f6659; }");
                out.println("  .ph-form-field { margin-bottom: 8px; }");
                out.println("  .ph-form-field label { display: block; margin-bottom: 4px; font-size: 12px; color: #4f4b43; }");
                out.println(
                                "  .ph-form-field input, .ph-form-field select, .ph-form-field textarea { width: 100%; box-sizing: border-box; padding: 6px 7px; border: 1px solid #d2c8ba; border-radius: 4px; }");
                out.println("  .ph-form-actions { display: flex; gap: 8px; margin-top: 10px; }");
                out.println("  .ph-reprio-choices { margin-top: 8px; }");
                out.println("  .ph-review-unscheduled-modal { width: min(860px, calc(100vw - 30px)); }");
                out.println(
                                "  .ph-review-list { border: 1px solid #d9ccb8; border-radius: 6px; background: #fffdfa; padding: 8px; margin-bottom: 10px; max-height: 300px; overflow: auto; }");
                out.println("  .ph-review-project { margin-bottom: 10px; }");
                out.println("  .ph-review-project-title { font-weight: bold; color: #415341; margin: 4px 0 6px 0; }");
                out.println("  .ph-review-action { display: block; margin: 4px 0; font-size: 12px; color: #364033; }");
                out.println("  .ph-review-action input { margin-right: 6px; }");

                out.println(
                                "  .ph-dev-label { display: none; font-size: 10px; font-weight: bold; letter-spacing: 0.04em; text-transform: uppercase; color: #796f62; margin-bottom: 4px; }");
                out.println("  .ph-dev-labels-enabled .ph-dev-label { display: block; }");

                out.println("  @media (max-width: 1180px) { .ph-shell { grid-template-columns: 1fr; } }");
                out.println(
                                "  .ph-links-list { list-style: none; margin: 6px 0 10px 0; padding: 0; display: flex; flex-direction: column; gap: 6px; }");
                out.println(
                                "  .ph-link-item { display: flex; align-items: flex-start; gap: 6px; font-size: 12px; border: 1px solid #e0d8cb; border-radius: 4px; padding: 5px 7px; background: #faf8f4; }");
                out.println(
                                "  .ph-link-item span { flex: 1; color: #364033; }");
                out.println(
                                "  .ph-link-resolved { list-style: disc; margin: 4px 0 0 16px; padding: 0; }");
                out.println(
                                "  .ph-link-resolved li { color: #5f6d5f; font-size: 11px; margin-bottom: 2px; }");
                out.println(
                                "  .ph-patch-link-form { display: flex; gap: 6px; align-items: center; margin-bottom: 6px; }");
                out.println(
                                "  .ph-patch-link-form select { flex: 1; padding: 5px 6px; border: 1px solid #d2c8ba; border-radius: 4px; font-size: 12px; }");
                out.println("</style>");
        }

        private void printDevLabel(PrintWriter out, String label) {
                out.println("<div class=\"ph-dev-label\">" + escapeHtml(label) + "</div>");
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

        private String escapeJsString(String value) {
                if (value == null) {
                        return "";
                }
                return value.replace("\\", "\\\\")
                                .replace("'", "\\'")
                                .replace("\"", "\\\"")
                                .replace("\r", "\\r")
                                .replace("\n", "\\n")
                                .replace("</", "<\\/");
        }

        private String n(String value) {
                return value == null ? "" : value;
        }

        private ProjectContactAssigned loadProjectContactAssigned(WebUser webUser, Session dataSession,
                        Project project) {
                if (webUser == null || project == null) {
                        return null;
                }
                ProjectContactAssignedId id = new ProjectContactAssignedId();
                id.setContactId(webUser.getContactId());
                id.setProjectId(project.getProjectId());
                return (ProjectContactAssigned) dataSession.get(ProjectContactAssigned.class, id);
        }
}
