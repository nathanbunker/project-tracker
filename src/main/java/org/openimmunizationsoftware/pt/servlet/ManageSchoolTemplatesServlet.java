package org.openimmunizationsoftware.pt.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionNextTemplateConfig;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TemplateType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.WeUserDependency;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.schooltemplate.FieldDiff;
import org.openimmunizationsoftware.pt.schooltemplate.SchoolTemplateDiffService;
import org.openimmunizationsoftware.pt.schooltemplate.SchoolTemplateImportEntry;
import org.openimmunizationsoftware.pt.schooltemplate.SchoolTemplateImportParser;
import org.openimmunizationsoftware.pt.schooltemplate.SchoolTemplateJsonExporter;
import org.openimmunizationsoftware.pt.schooltemplate.SchoolTemplateQueryService;
import org.openimmunizationsoftware.pt.schooltemplate.SchoolTemplateSnapshot;
import org.openimmunizationsoftware.pt.schooltemplate.TemplateChange;

import org.dandeliondaily.planahead.service.TemplateGenerationService;

/**
 * Companion page to {@link ScheduleSchoolServlet}'s Template Scheduler: lets a parent print a human-readable
 * copy of the School/Chores templates for offline review, export them as JSON, and re-import an edited JSON
 * document (additions, updates, and closes) after reviewing it outside the app (e.g. with a spouse and/or an
 * LLM). Import is always validate-all -&gt; preview diff -&gt; explicit confirm -&gt; apply; nothing is written
 * to the database until the user clicks Apply on the preview screen.
 */
@MultipartConfig
public class ManageSchoolTemplatesServlet extends ClientServlet {

    private static final long serialVersionUID = 1L;

    private static final String PARAM_DEPENDENCY_ID = "dependencyId";
    private static final String PARAM_IMPORT_TEXT = "importJson";
    private static final String PARAM_IMPORT_FILE = "importFile";

    private static final String ACTION_PRINT = "Print";
    private static final String ACTION_EXPORT = "ExportJson";
    private static final String ACTION_PREVIEW = "PreviewImport";
    private static final String ACTION_APPLY = "ApplyImport";

    private final SchoolTemplateQueryService queryService = new SchoolTemplateQueryService();
    private final SchoolTemplateJsonExporter jsonExporter = new SchoolTemplateJsonExporter();
    private final SchoolTemplateImportParser importParser = new SchoolTemplateImportParser();
    private final SchoolTemplateDiffService diffService = new SchoolTemplateDiffService();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            WebUser parentUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();
            PrintWriter out = appReq.getOut();

            Integer dependencyId = parseInteger(request.getParameter(PARAM_DEPENDENCY_ID));
            WeUserDependency dependency = ScheduleSchoolServlet.loadValidatedDependency(parentUser, dependencyId,
                    dataSession);
            if (dependency == null) {
                appReq.setTitle("Manage School Templates");
                printHtmlHead(appReq);
                printDandelionLocation(out, "Setup / Dependent Accounts / School Scheduling / Manage Templates");
                appReq.setMessageProblem("Dependent account was not found or is not active for this parent account.");
                out.println("<p><a href=\"DependentAccountsServlet\">Back to Dependent Accounts</a></p>");
                printHtmlFoot(appReq);
                return;
            }

            WebUser dependentUser = dependency.getDependentWebUser();
            Integer activeWorkspaceId = WorkspaceRegistry.getWorkspaceIdForWebUserId(dataSession,
                    dependentUser.getWebUserId());
            if (activeWorkspaceId == null) {
                forwardToHome(request, response);
                return;
            }
            ProjectContact dependentContact = (ProjectContact) dataSession.get(ProjectContact.class,
                    dependentUser.getContactId());
            dependentUser.setProjectContact(dependentContact);
            if (dependentContact != null && dependentContact.getTimeZone() != null
                    && !dependentContact.getTimeZone().trim().equals("")) {
                dependentUser.setTimeZone(java.util.TimeZone.getTimeZone(dependentContact.getTimeZone()));
            }
            String dependentName = resolveDependentName(dependentContact, dependentUser);

            String action = appReq.getAction();

            if (ACTION_EXPORT.equals(action)) {
                handleExport(appReq, dataSession, dependentUser, dependency, activeWorkspaceId, dependentName);
                return;
            }
            if (ACTION_PRINT.equals(action)) {
                handlePrint(appReq, dataSession, dependentUser, dependency, activeWorkspaceId, dependentName);
                return;
            }
            if (ACTION_PREVIEW.equals(action)) {
                handlePreview(appReq, dataSession, dependentUser, dependency, activeWorkspaceId);
                return;
            }
            if (ACTION_APPLY.equals(action)) {
                handleApply(appReq, dataSession, dependentUser, dependency, activeWorkspaceId);
                return;
            }

            printMainPage(appReq, dependency, dependentUser, dependentContact);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private String resolveDependentName(ProjectContact dependentContact, WebUser dependentUser) {
        String name = "";
        if (dependentContact != null) {
            name = (safe(dependentContact.getNameFirst()) + " " + safe(dependentContact.getNameLast())).trim();
        }
        if (name.equals("")) {
            name = (safe(dependentUser.getFirstName()) + " " + safe(dependentUser.getLastName())).trim();
        }
        return name;
    }

    // -------------------------------------------------------------------
    // Main page
    // -------------------------------------------------------------------

    private void printMainPage(AppReq appReq, WeUserDependency dependency, WebUser dependentUser,
            ProjectContact dependentContact) {
        PrintWriter out = appReq.getOut();
        int dependencyId = dependency.getDependencyId();

        appReq.setTitle("Manage School Templates");
        printHtmlHead(appReq);
        printDandelionLocation(out, "Setup / Dependent Accounts / School Scheduling / Manage Templates");

        String dependentName = resolveDependentName(dependentContact, dependentUser);
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\"><th class=\"title\" colspan=\"2\">Dependent</th></tr>");
        out.println("  <tr class=\"boxed\"><th class=\"boxed\">Name</th><td class=\"boxed\">"
                + escapeHtmlAttribute(dependentName) + "</td></tr>");
        out.println("</table>");
        out.println("<br/>");

        out.println("<h2>Review This Year, Prepare for Next Year</h2>");
        out.println("<p>Print a plain copy of the current School/Chores templates to review, export the same "
                + "data as JSON, edit it (by hand, with your co-parent, or by having an AI assistant rewrite it "
                + "based on your notes), and re-upload it here. Nothing is saved until you review the changes "
                + "on the preview screen and confirm.</p>");

        out.println("<p>");
        out.println("  <a class=\"button\" target=\"_blank\" href=\"ManageSchoolTemplatesServlet?"
                + PARAM_DEPENDENCY_ID + "=" + dependencyId + "&" + PARAM_ACTION + "=" + ACTION_PRINT
                + "\">Print Templates</a>");
        out.println("  <a class=\"button\" href=\"ManageSchoolTemplatesServlet?" + PARAM_DEPENDENCY_ID + "="
                + dependencyId + "&" + PARAM_ACTION + "=" + ACTION_EXPORT + "\">Export JSON</a>");
        out.println("</p>");

        out.println("<h3>Import Updated JSON</h3>");
        out.println("<p>Paste the edited JSON below, or choose a file — not both.</p>");
        out.println("<form action=\"ManageSchoolTemplatesServlet\" method=\"POST\" enctype=\"multipart/form-data\">");
        out.println("  <input type=\"hidden\" name=\"" + PARAM_DEPENDENCY_ID + "\" value=\"" + dependencyId + "\"/>");
        out.println("  <input type=\"hidden\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_PREVIEW + "\"/>");
        out.println("  <p><textarea name=\"" + PARAM_IMPORT_TEXT
                + "\" rows=\"14\" style=\"width:98%; font-family:monospace;\" "
                + "placeholder=\"Paste updated JSON here\"></textarea></p>");
        out.println("  <p>Or upload a file: <input type=\"file\" name=\"" + PARAM_IMPORT_FILE
                + "\" accept=\"application/json,.json\"/></p>");
        out.println("  <p><button type=\"submit\">Preview Changes</button></p>");
        out.println("</form>");

        out.println("<br/>");
        out.println("<p><a href=\"ScheduleSchoolServlet?" + PARAM_DEPENDENCY_ID + "=" + dependencyId
                + "\">Back to Template Scheduler</a></p>");

        printHtmlFoot(appReq);
    }

    // -------------------------------------------------------------------
    // Export
    // -------------------------------------------------------------------

    private void handleExport(AppReq appReq, Session dataSession, WebUser dependentUser, WeUserDependency dependency,
            Integer workspaceId, String dependentName) throws Exception {
        List<Project> projectList = queryService.loadWorkspaceProjects(dataSession, workspaceId);
        Map<Integer, Boolean> billableMap = queryService.buildProjectBillableMap(dataSession, projectList,
                workspaceId);
        List<SchoolTemplateSnapshot> snapshots = queryService.loadTemplateSnapshots(dataSession, dependentUser,
                projectList, billableMap);

        String json = jsonExporter.toJson(dependency.getDependencyId(), dependentName, snapshots);

        String fileName = "school-templates-" + slug(dependentName) + "-"
                + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".json";

        HttpServletResponse response = appReq.getResponse();
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        appReq.getOut().print(json);
    }

    private String slug(String value) {
        String v = safe(value).trim().toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '-') {
                sb.append('-');
            }
        }
        return sb.length() == 0 ? "dependent" : sb.toString();
    }

    // -------------------------------------------------------------------
    // Print (human-readable)
    // -------------------------------------------------------------------

    private void handlePrint(AppReq appReq, Session dataSession, WebUser dependentUser, WeUserDependency dependency,
            Integer workspaceId, String dependentName) {
        List<Project> projectList = queryService.loadWorkspaceProjects(dataSession, workspaceId);
        Map<Integer, Boolean> billableMap = queryService.buildProjectBillableMap(dataSession, projectList,
                workspaceId);
        List<SchoolTemplateSnapshot> snapshots = queryService.loadTemplateSnapshots(dataSession, dependentUser,
                projectList, billableMap);

        HttpServletResponse response = appReq.getResponse();
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = appReq.getOut();

        out.println("<html><head><meta charset=\"UTF-8\">");
        out.println("<title>School Templates - " + escapeHtmlAttribute(dependentName) + "</title>");
        out.println("<style>");
        out.println("  body { font-family: sans-serif; margin: 24px; color: #222; }");
        out.println("  h1 { font-size: 20px; } h2 { font-size: 16px; margin-top: 28px; }");
        out.println("  table { border-collapse: collapse; width: 100%; margin-bottom: 12px; }");
        out.println("  th, td { border: 1px solid #999; padding: 4px 8px; text-align: left; font-size: 13px; }");
        out.println("  th { background: #eee; }");
        out.println("  .noprint { margin-bottom: 16px; }");
        out.println("  @media print { .noprint { display: none; } }");
        out.println("</style></head><body>");
        out.println("<div class=\"noprint\"><button onclick=\"window.print()\">Print</button> "
                + "<a href=\"ManageSchoolTemplatesServlet?" + PARAM_DEPENDENCY_ID + "=" + dependency.getDependencyId()
                + "\">Back</a></div>");
        out.println("<h1>School Templates for " + escapeHtmlAttribute(dependentName) + "</h1>");
        out.println("<p>As of " + new SimpleDateFormat("EEEE, MMMM d, yyyy").format(new Date()) + "</p>");

        printPrintableCategory(out, snapshots, true, "School");
        printPrintableCategory(out, snapshots, false, "Chores");

        out.println("</body></html>");
    }

    private void printPrintableCategory(PrintWriter out, List<SchoolTemplateSnapshot> snapshots, boolean billable,
            String heading) {
        out.println("<h2>" + heading + "</h2>");
        List<SchoolTemplateSnapshot> filtered = new ArrayList<SchoolTemplateSnapshot>();
        for (SchoolTemplateSnapshot snapshot : snapshots) {
            if (snapshot.isBillable() == billable) {
                filtered.add(snapshot);
            }
        }
        if (filtered.isEmpty()) {
            out.println("<p>(none)</p>");
            return;
        }

        // loadTemplateSnapshots already orders snapshots project-by-project, so a simple
        // "did the project change" check is enough to group them without re-sorting here.
        Project currentProject = null;
        for (SchoolTemplateSnapshot snapshot : filtered) {
            if (currentProject == null || snapshot.getProject() == null
                    || currentProject.getProjectId() != snapshot.getProject().getProjectId()) {
                if (currentProject != null) {
                    out.println("</table>");
                }
                currentProject = snapshot.getProject();
                out.println("<h3>" + escapeHtmlAttribute(currentProject == null ? "" : currentProject.getProjectName())
                        + "</h3>");
                out.println("<table>");
                out.println("<tr><th>Description</th><th>Schedule</th><th>Missed Behavior</th><th>"
                        + (billable ? "Time / Points" : "Time Slot") + "</th><th>Action Type</th></tr>");
            }

            ActionNext actionNext = snapshot.getActionNext();
            ActionNextTemplateConfig config = snapshot.getConfig();
            TemplateType type = actionNext.getTemplateType() != null ? actionNext.getTemplateType() : TemplateType.DAILY;
            String missedBehavior = config != null && !safe(config.getMissedActionBehavior()).isEmpty()
                    ? config.getMissedActionBehavior() : "AUTO_CANCEL";
            String actionType = safe(actionNext.getNextActionType());
            if (actionType.isEmpty()) {
                actionType = ProjectNextActionType.WILL;
            }
            String timeOrSlot;
            if (billable) {
                int estimate = actionNext.getNextTimeEstimate() == null ? 0 : actionNext.getNextTimeEstimate().intValue();
                int points = actionNext.getGamePoints() == null ? 0 : actionNext.getGamePoints().intValue();
                timeOrSlot = estimate + " min / " + points + " pts";
            } else {
                TimeSlot slot = actionNext.getTimeSlot() != null ? actionNext.getTimeSlot() : TimeSlot.AFTERNOON;
                timeOrSlot = slot.getLabel();
            }

            out.println("<tr>");
            out.println("<td>" + escapeHtmlAttribute(safe(actionNext.getNextDescription())) + "</td>");
            out.println("<td>" + escapeHtmlAttribute(printableScheduleLabel(type, config)) + "</td>");
            out.println("<td>" + escapeHtmlAttribute(missedBehavior) + "</td>");
            out.println("<td>" + escapeHtmlAttribute(timeOrSlot) + "</td>");
            out.println("<td>" + escapeHtmlAttribute(ProjectNextActionType.getLabel(actionType)) + "</td>");
            out.println("</tr>");
        }
        out.println("</table>");
    }

    private String printableScheduleLabel(TemplateType type, ActionNextTemplateConfig config) {
        if (type == TemplateType.DAILY) {
            return "Daily";
        }
        String csv = null;
        if (config != null) {
            switch (type) {
                case WEEKLY:
                    csv = config.getScheduleDaysOfWeek();
                    break;
                case MONTHLY:
                    csv = config.getScheduleDaysOfMonth();
                    break;
                case QUARTERLY:
                    csv = config.getScheduleDaysOfQuarter();
                    break;
                case YEARLY:
                    csv = config.getScheduleDaysOfYear();
                    break;
                default:
                    break;
            }
        }
        if (csv == null || csv.trim().isEmpty()) {
            return type.getLabel() + " (every occurrence)";
        }
        return type.getLabel() + " (" + csv.trim() + ")";
    }

    // -------------------------------------------------------------------
    // Import: preview
    // -------------------------------------------------------------------

    private void handlePreview(AppReq appReq, Session dataSession, WebUser dependentUser, WeUserDependency dependency,
            Integer workspaceId) throws Exception {
        String importText;
        try {
            importText = readImportText(appReq.getRequest());
        } catch (IllegalArgumentException e) {
            printImportErrorPage(appReq, dependency, "", e.getMessage());
            return;
        }

        List<TemplateChange> changes;
        try {
            List<SchoolTemplateImportEntry> entries = importParser.parse(importText);
            List<Project> projectList = queryService.loadWorkspaceProjects(dataSession, workspaceId);
            Map<Integer, Boolean> billableMap = queryService.buildProjectBillableMap(dataSession, projectList,
                    workspaceId);
            List<SchoolTemplateSnapshot> snapshots = queryService.loadTemplateSnapshots(dataSession, dependentUser,
                    projectList, billableMap);
            changes = diffService.computeDiff(projectList, billableMap, snapshots, entries);
        } catch (IllegalArgumentException e) {
            printImportErrorPage(appReq, dependency, importText, e.getMessage());
            return;
        }

        printPreviewPage(appReq, dependency, importText, changes);
    }

    private String readImportText(HttpServletRequest request) throws IOException, ServletException {
        String pastedJson = safe(request.getParameter(PARAM_IMPORT_TEXT)).trim();
        Part filePart = null;
        try {
            filePart = request.getPart(PARAM_IMPORT_FILE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to read the uploaded file.");
        }
        boolean hasFile = filePart != null && filePart.getSize() > 0;
        if (hasFile && pastedJson.length() > 0) {
            throw new IllegalArgumentException("Choose a JSON file or paste JSON, not both.");
        }
        if (!hasFile && pastedJson.length() == 0) {
            throw new IllegalArgumentException("Paste JSON or choose a file to import.");
        }
        if (hasFile && filePart.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("The JSON file must be 2 MB or smaller.");
        }
        return hasFile ? readPartUtf8(filePart) : pastedJson;
    }

    private String readPartUtf8(Part part) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream inputStream = part.getInputStream();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            char[] buffer = new char[4096];
            int len;
            while ((len = reader.read(buffer)) >= 0) {
                if (len > 0) {
                    sb.append(buffer, 0, len);
                }
            }
        } finally {
            inputStream.close();
        }
        return sb.toString();
    }

    private void printImportErrorPage(AppReq appReq, WeUserDependency dependency, String importText, String errorMessage) {
        PrintWriter out = appReq.getOut();
        appReq.setTitle("Manage School Templates");
        printHtmlHead(appReq);
        printDandelionLocation(out, "Setup / Dependent Accounts / School Scheduling / Manage Templates");

        out.println("<h2>Import Could Not Be Validated</h2>");
        out.println("<pre style=\"white-space:pre-wrap; background:#fdecea; border-left:4px solid #c62828; padding:10px;\">"
                + escapeHtmlAttribute(errorMessage) + "</pre>");
        out.println("<p>Fix the JSON (or send the message above back to your AI assistant to fix) and re-upload. "
                + "Nothing has been changed.</p>");

        out.println("<form action=\"ManageSchoolTemplatesServlet\" method=\"POST\" enctype=\"multipart/form-data\">");
        out.println("  <input type=\"hidden\" name=\"" + PARAM_DEPENDENCY_ID + "\" value=\""
                + dependency.getDependencyId() + "\"/>");
        out.println("  <input type=\"hidden\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_PREVIEW + "\"/>");
        out.println("  <p><textarea name=\"" + PARAM_IMPORT_TEXT
                + "\" rows=\"14\" style=\"width:98%; font-family:monospace;\">" + escapeHtmlAttribute(importText)
                + "</textarea></p>");
        out.println("  <p>Or upload a file: <input type=\"file\" name=\"" + PARAM_IMPORT_FILE + "\"/></p>");
        out.println("  <p><button type=\"submit\">Preview Changes</button></p>");
        out.println("</form>");

        out.println("<p><a href=\"ManageSchoolTemplatesServlet?" + PARAM_DEPENDENCY_ID + "="
                + dependency.getDependencyId() + "\">Cancel</a></p>");

        printHtmlFoot(appReq);
    }

    private void printPreviewPage(AppReq appReq, WeUserDependency dependency, String importText,
            List<TemplateChange> changes) {
        PrintWriter out = appReq.getOut();
        appReq.setTitle("Manage School Templates");
        printHtmlHead(appReq);
        printDandelionLocation(out, "Setup / Dependent Accounts / School Scheduling / Manage Templates");

        int addCount = 0;
        int updateCount = 0;
        int deleteCount = 0;
        for (TemplateChange change : changes) {
            if (change.getChangeType() == TemplateChange.ChangeType.ADD) {
                addCount++;
            } else if (change.getChangeType() == TemplateChange.ChangeType.UPDATE) {
                updateCount++;
            } else {
                deleteCount++;
            }
        }

        out.println("<h2>Review Changes Before Applying</h2>");
        out.println("<p><strong>" + addCount + "</strong> to add, <strong>" + updateCount
                + "</strong> to update, <strong>" + deleteCount + "</strong> to close. Nothing has been saved yet.</p>");

        if (changes.isEmpty()) {
            out.println("<p>No changes detected between the uploaded JSON and the current templates.</p>");
        }

        out.println("<table class=\"boxed\">");
        out.println("<tr class=\"boxed\"><th class=\"boxed\">Change</th><th class=\"boxed\">Project</th>"
                + "<th class=\"boxed\">Description</th><th class=\"boxed\">Details</th></tr>");
        for (TemplateChange change : changes) {
            out.println("<tr class=\"boxed\">");
            out.println("<td class=\"boxed\"><strong>" + changeLabel(change) + "</strong></td>");
            out.println("<td class=\"boxed\">" + escapeHtmlAttribute(change.getDisplayProjectName()) + "</td>");
            out.println("<td class=\"boxed\">" + escapeHtmlAttribute(change.getDisplayDescription()) + "</td>");
            out.println("<td class=\"boxed\">");
            if (change.getChangeType() == TemplateChange.ChangeType.UPDATE) {
                if (change.getFieldDiffs().isEmpty()) {
                    out.println("(no field changes)");
                } else {
                    for (FieldDiff diff : change.getFieldDiffs()) {
                        out.println("<div><em>" + escapeHtmlAttribute(diff.getLabel()) + ":</em> "
                                + escapeHtmlAttribute(diff.getOldValue()) + " &rarr; "
                                + escapeHtmlAttribute(diff.getNewValue()) + "</div>");
                    }
                }
            } else if (change.getChangeType() == TemplateChange.ChangeType.DELETE) {
                out.println("Will be closed (id " + change.getExistingSnapshot().getActionNext().getActionNextId() + ")");
            } else {
                out.println("New template");
            }
            out.println("</td>");
            out.println("</tr>");
        }
        out.println("</table>");

        out.println("<form action=\"ManageSchoolTemplatesServlet\" method=\"POST\">");
        out.println("  <input type=\"hidden\" name=\"" + PARAM_DEPENDENCY_ID + "\" value=\""
                + dependency.getDependencyId() + "\"/>");
        out.println("  <input type=\"hidden\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_APPLY + "\"/>");
        out.println("  <textarea name=\"" + PARAM_IMPORT_TEXT + "\" style=\"display:none;\">"
                + escapeHtmlAttribute(importText) + "</textarea>");
        out.println("  <p><button type=\"submit\" " + (changes.isEmpty() ? "disabled" : "")
                + ">Apply These Changes</button> "
                + "<a href=\"ManageSchoolTemplatesServlet?" + PARAM_DEPENDENCY_ID + "=" + dependency.getDependencyId()
                + "\">Cancel</a></p>");
        out.println("</form>");

        printHtmlFoot(appReq);
    }

    private String changeLabel(TemplateChange change) {
        switch (change.getChangeType()) {
            case ADD:
                return "Add";
            case DELETE:
                return "Close";
            default:
                return "Update";
        }
    }

    // -------------------------------------------------------------------
    // Import: apply
    // -------------------------------------------------------------------

    private void handleApply(AppReq appReq, Session dataSession, WebUser dependentUser, WeUserDependency dependency,
            Integer workspaceId) {
        String importText = safe(appReq.getRequest().getParameter(PARAM_IMPORT_TEXT));

        List<TemplateChange> changes;
        try {
            List<SchoolTemplateImportEntry> entries = importParser.parse(importText);
            List<Project> projectList = queryService.loadWorkspaceProjects(dataSession, workspaceId);
            Map<Integer, Boolean> billableMap = queryService.buildProjectBillableMap(dataSession, projectList,
                    workspaceId);
            List<SchoolTemplateSnapshot> snapshots = queryService.loadTemplateSnapshots(dataSession, dependentUser,
                    projectList, billableMap);
            changes = diffService.computeDiff(projectList, billableMap, snapshots, entries);
        } catch (IllegalArgumentException e) {
            printImportErrorPage(appReq, dependency,
                    importText, "The templates changed since you previewed this import, so it needs to be "
                            + "re-checked:\n" + e.getMessage());
            return;
        }

        List<Integer> syncList = new ArrayList<Integer>();
        Date endOfYear = ScheduleSchoolServlet.calculateEndOfYear(dependentUser);

        Transaction transaction = dataSession.beginTransaction();
        try {
            for (TemplateChange change : changes) {
                if (change.getChangeType() == TemplateChange.ChangeType.DELETE) {
                    ActionNext actionNext = change.getExistingSnapshot().getActionNext();
                    actionNext.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
                    actionNext.setNextChangeDate(new Date());
                    dataSession.update(actionNext);
                    continue;
                }

                SchoolTemplateImportEntry entry = change.getSourceEntry();
                TemplateType templateType = TemplateType.valueOf(entry.getScheduleType());
                boolean billable = change.isBillable();

                ActionNext actionNext;
                boolean isNew = change.getChangeType() == TemplateChange.ChangeType.ADD;
                if (isNew) {
                    actionNext = new ActionNext();
                    actionNext.setContactId(dependentUser.getContactId());
                    actionNext.setContact(dependentUser.getProjectContact());
                    actionNext.setWorkspaceId(workspaceId);
                    actionNext.setNextActionStatus(ProjectNextActionStatus.READY);
                    actionNext.setCompletionOrder(0);
                    actionNext.setNextActionDate(endOfYear);
                } else {
                    actionNext = change.getExistingSnapshot().getActionNext();
                }

                actionNext.setProjectId(change.getResolvedProject().getProjectId());
                actionNext.setProject(change.getResolvedProject());
                actionNext.setPriorityLevel(change.getResolvedProject().getPriorityLevel());
                actionNext.setNextDescription(trim(entry.getDescription(), 12000));
                actionNext.setTemplateType(templateType);
                actionNext.setNextActionType(entry.getActionType());
                actionNext.setBillable(billable);
                if (billable) {
                    actionNext.setNextTimeEstimate(
                            Integer.valueOf(entry.getTimeEstimateMinutes() == null ? 0 : entry.getTimeEstimateMinutes()));
                    actionNext.setGamePoints(
                            Integer.valueOf(entry.getGamePoints() == null ? 0 : entry.getGamePoints()));
                    actionNext.setTimeSlot(null);
                } else {
                    TimeSlot timeSlot = entry.getTimeSlot() != null ? TimeSlot.valueOf(entry.getTimeSlot())
                            : TimeSlot.AFTERNOON;
                    actionNext.setTimeSlot(timeSlot);
                    actionNext.setNextTimeEstimate(null);
                    actionNext.setGamePoints(null);
                }
                if (!isNew && !change.getFieldDiffs().isEmpty()) {
                    actionNext.setNextActionDate(endOfYear);
                }
                actionNext.setNextChangeDate(new Date());

                if (isNew) {
                    dataSession.save(actionNext);
                } else {
                    dataSession.update(actionNext);
                }

                int id = actionNext.getActionNextId();
                ActionNextTemplateConfig config = (ActionNextTemplateConfig) dataSession
                        .get(ActionNextTemplateConfig.class, id);
                boolean newConfig = config == null;
                if (newConfig) {
                    config = new ActionNextTemplateConfig();
                    config.setActionNextId(id);
                }
                config.setAutoGenerate(entry.getAutoGenerate() == null || entry.getAutoGenerate().booleanValue());
                config.setMissedActionBehavior(entry.getMissedActionBehavior());
                config.setScheduleDaysOfWeek(joinCsv(entry.getDaysOfWeek()));
                config.setScheduleDaysOfMonth(joinCsv(entry.getDaysOfMonth()));
                config.setScheduleDaysOfQuarter(joinCsv(entry.getDaysOfQuarter()));
                config.setScheduleDaysOfYear(joinCsv(entry.getDaysOfYear()));
                if (newConfig) {
                    dataSession.save(config);
                } else {
                    dataSession.update(config);
                }

                syncList.add(Integer.valueOf(id));
            }
            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }

        if (!syncList.isEmpty()) {
            int advanceDays = 14;
            try {
                advanceDays = Integer.parseInt(TrackerKeysManager.getKeyValue(
                        TrackerKeysManager.KEY_TEMPLATE_ADVANCE_DAYS,
                        TrackerKeysManager.KEY_TYPE_GLOBAL,
                        TrackerKeysManager.KEY_ID_GLOBAL,
                        "14", dataSession).trim());
            } catch (NumberFormatException nfe) {
                advanceDays = 14;
            }
            LocalDate today = LocalDate.now(dependentUser.getZoneId());
            for (Integer templateId : syncList) {
                Transaction syncTx = dataSession.beginTransaction();
                try {
                    ActionNext reloadedTemplate = (ActionNext) dataSession.get(ActionNext.class,
                            templateId.intValue());
                    if (reloadedTemplate != null) {
                        new TemplateGenerationService().syncAfterEdit(
                                dataSession, reloadedTemplate, workspaceId, dependentUser.getContactId(), today,
                                advanceDays);
                    }
                    syncTx.commit();
                } catch (RuntimeException e) {
                    syncTx.rollback();
                    System.err.println("[ManageSchoolTemplates] Schedule sync failed for template "
                            + templateId + ": " + e.getMessage());
                }
            }
        }

        int addCount = 0;
        int updateCount = 0;
        int deleteCount = 0;
        for (TemplateChange change : changes) {
            if (change.getChangeType() == TemplateChange.ChangeType.ADD) {
                addCount++;
            } else if (change.getChangeType() == TemplateChange.ChangeType.UPDATE) {
                updateCount++;
            } else {
                deleteCount++;
            }
        }

        appReq.setTitle("Manage School Templates");
        appReq.setMessageConfirmation("Import applied: " + addCount + " added, " + updateCount + " updated, "
                + deleteCount + " closed.");
        printHtmlHead(appReq);
        PrintWriter out = appReq.getOut();
        printDandelionLocation(out, "Setup / Dependent Accounts / School Scheduling / Manage Templates");
        out.println("<p><a href=\"ManageSchoolTemplatesServlet?" + PARAM_DEPENDENCY_ID + "="
                + dependency.getDependencyId() + "\">Back to Manage Templates</a> | "
                + "<a href=\"ScheduleSchoolServlet?" + PARAM_DEPENDENCY_ID + "=" + dependency.getDependencyId()
                + "\">Back to Template Scheduler</a></p>");
        printHtmlFoot(appReq);
    }

    private String joinCsv(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(token);
        }
        return sb.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.trim().equals("")) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
