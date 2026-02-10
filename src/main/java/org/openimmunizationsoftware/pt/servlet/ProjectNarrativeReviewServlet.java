/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.ProjectNarrativeDao;
import org.openimmunizationsoftware.pt.doa.ProjectNarrativeDao.Action;
import org.openimmunizationsoftware.pt.doa.ProjectNarrativeDao.ReviewItem;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * Review Dashboard + Editor
 * 
 * @author nathan
 */
public class ProjectNarrativeReviewServlet extends ClientServlet {

    private static final long serialVersionUID = 7556432190901997402L;

    private static final String PARAM_DATE = "date";
    private static final String PARAM_PROJECT_ID = "projectId";
    private static final String PARAM_SELECT_PROJECT_ID = "selectProjectId";
    private static final String PARAM_ACTION = "action";

    private static final String PARAM_NOTE = "note";
    private static final String PARAM_DECISION = "decision";
    private static final String PARAM_INSIGHT = "insight";
    private static final String PARAM_RISK = "risk";
    private static final String PARAM_OPPORTUNITY = "opportunity";

    private static final String ACTION_SAVE = "Save";

    private static final String DEFAULT_NOTE_TEXT = "Reviewed/no comments";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            WebUser webUser = appReq.getWebUser();
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            Session dataSession = appReq.getDataSession();
            ProjectNarrativeDao narrativeDao = new ProjectNarrativeDao(dataSession);
            LocalDate reviewDate = resolveReviewDate(request, appReq);

            if ("POST".equalsIgnoreCase(request.getMethod())) {
                handlePost(request, response, appReq, narrativeDao, reviewDate);
                return;
            }

            appReq.setTitle("Review");
            printHtmlHead(appReq);

            PrintWriter out = appReq.getOut();
            List<ReviewItem> reviewItems = narrativeDao.listReviewItemsForDate(reviewDate);
            long selectedProjectId = readLong(request.getParameter(PARAM_PROJECT_ID));
            ReviewItem selectedItem = selectReviewItem(reviewItems, selectedProjectId);

            out.println("<form action=\"ProjectNarrativeReviewServlet\" method=\"POST\">");
            out.println("<input type=\"hidden\" name=\"" + PARAM_DATE + "\" value=\"" + reviewDate
                    + "\">");

            out.println("<table class=\"boxed-full\" width=\"100%\">");
            out.println("  <tr>");
            out.println("    <td class=\"outside\" width=\"70%\">");
            printEditor(out, dataSession, webUser, narrativeDao, reviewDate, selectedItem);
            out.println("    </td>");
            out.println("    <td class=\"outside\" width=\"30%\">");
            printReviewList(out, reviewItems, selectedItem == null ? 0 : selectedItem.getProjectId(), reviewDate);
            out.println("    </td>");
            out.println("  </tr>");
            out.println("</table>");
            out.println("</form>");

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private void handlePost(HttpServletRequest request, HttpServletResponse response, AppReq appReq,
            ProjectNarrativeDao narrativeDao, LocalDate reviewDate) throws IOException {
        Session dataSession = appReq.getDataSession();
        long projectId = readLong(request.getParameter(PARAM_PROJECT_ID));
        String selectProjectIdParam = request.getParameter(PARAM_SELECT_PROJECT_ID);

        if (projectId > 0) {
            Project project = (Project) dataSession.get(Project.class, (int) projectId);
            if (project != null) {
                saveNarratives(request, appReq, narrativeDao, project, reviewDate);
            }
        }

        long redirectProjectId = 0;
        if (selectProjectIdParam != null && selectProjectIdParam.trim().length() > 0) {
            redirectProjectId = readLong(selectProjectIdParam);
        } else {
            List<ReviewItem> reviewItems = narrativeDao.listReviewItemsForDate(reviewDate);
            ReviewItem next = findNextUnreviewed(reviewItems, projectId);
            if (next != null) {
                redirectProjectId = next.getProjectId();
            }
        }

        String redirect = "ProjectNarrativeReviewServlet?" + PARAM_DATE + "=" + reviewDate;
        if (redirectProjectId > 0) {
            redirect += "&" + PARAM_PROJECT_ID + "=" + redirectProjectId;
        }
        response.sendRedirect(redirect);
    }

    private void saveNarratives(HttpServletRequest request, AppReq appReq, ProjectNarrativeDao narrativeDao,
            Project project, LocalDate reviewDate) {
        String noteText = n(request.getParameter(PARAM_NOTE)).trim();
        String decisionText = n(request.getParameter(PARAM_DECISION)).trim();
        String insightText = n(request.getParameter(PARAM_INSIGHT)).trim();
        String riskText = n(request.getParameter(PARAM_RISK)).trim();
        String opportunityText = n(request.getParameter(PARAM_OPPORTUNITY)).trim();

        Transaction transaction = null;
        try {
            transaction = appReq.getDataSession().beginTransaction();
            int offsetSeconds = 0;

            if (noteText.length() == 0) {
                noteText = DEFAULT_NOTE_TEXT;
            }
            offsetSeconds = insertNarrative(narrativeDao, appReq, project, reviewDate,
                    ProjectNarrativeVerb.NOTE, noteText, offsetSeconds);

            if (decisionText.length() > 0) {
                offsetSeconds = insertNarrative(narrativeDao, appReq, project, reviewDate,
                        ProjectNarrativeVerb.DECISION, decisionText, offsetSeconds);
            }
            if (insightText.length() > 0) {
                offsetSeconds = insertNarrative(narrativeDao, appReq, project, reviewDate,
                        ProjectNarrativeVerb.INSIGHT, insightText, offsetSeconds);
            }
            if (riskText.length() > 0) {
                offsetSeconds = insertNarrative(narrativeDao, appReq, project, reviewDate,
                        ProjectNarrativeVerb.RISK, riskText, offsetSeconds);
            }
            if (opportunityText.length() > 0) {
                insertNarrative(narrativeDao, appReq, project, reviewDate,
                        ProjectNarrativeVerb.OPPORTUNITY, opportunityText, offsetSeconds);
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    private int insertNarrative(ProjectNarrativeDao narrativeDao, AppReq appReq, Project project,
            LocalDate reviewDate, ProjectNarrativeVerb verb, String text, int offsetSeconds) {
        ProjectNarrative narrative = new ProjectNarrative();
        narrative.setProject(project);
        narrative.setContact(appReq.getWebUser().getProjectContact());
        narrative.setProvider(appReq.getWebUser().getProvider());
        narrative.setNarrativeDate(buildNarrativeDate(reviewDate, offsetSeconds));
        narrative.setNarrativeVerb(verb);
        narrative.setNarrativeText(text);
        narrativeDao.insert(narrative);
        return offsetSeconds + 1;
    }

    private void printEditor(PrintWriter out, Session dataSession, WebUser webUser,
            ProjectNarrativeDao narrativeDao, LocalDate reviewDate, ReviewItem selectedItem) {
        if (selectedItem == null) {
            out.println("<table class=\"boxed\">\n");
            out.println("  <tr class=\"boxed\">\n");
            out.println("    <th class=\"title\" colspan=\"2\">Review Dashboard</th>\n");
            out.println("  </tr>\n");
            out.println("  <tr class=\"boxed\">\n");
            out.println("    <td class=\"boxed\">All projects reviewed for " + reviewDate + ".</td>\n");
            out.println("  </tr>\n");
            out.println("</table>\n");
            return;
        }

        Project project = (Project) dataSession.get(Project.class, (int) selectedItem.getProjectId());
        if (project == null) {
            out.println("<p class=\"fail\">Project not found.</p>");
            return;
        }

        out.println("<input type=\"hidden\" name=\"" + PARAM_PROJECT_ID + "\" value=\""
                + selectedItem.getProjectId() + "\">");

        out.println("<table class=\"boxed\">\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"title\" colspan=\"2\">Review Dashboard</th>\n");
        out.println("  </tr>\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"boxed\">Project</th>\n");
        out.println("    <td class=\"boxed\">" + escapeHtml(project.getProjectName()) + "</td>\n");
        out.println("  </tr>\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"boxed\">Minutes</th>\n");
        out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(selectedItem.getMinutesSpent())
                + "</td>\n");
        out.println("  </tr>\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"boxed\">Date</th>\n");
        out.println("    <td class=\"boxed\">" + reviewDate + "</td>\n");
        out.println("  </tr>\n");
        out.println("</table><br/>\n");

        printCompletedActions(out, narrativeDao, selectedItem.getProjectId(), reviewDate);
        printExistingNarratives(out, narrativeDao, selectedItem.getProjectId(), reviewDate);

        out.println("<table class=\"boxed-full\">\n");
        out.println("  <tr><th class=\"title\" colspan=\"2\">Narrative</th></tr>\n");
        printNarrativeInput(out, "Notes", PARAM_NOTE, 6);
        printNarrativeInput(out, "Decisions made", PARAM_DECISION, 6);
        printNarrativeInput(out, "Insights gained", PARAM_INSIGHT, 6);
        printNarrativeInput(out, "Risks seen", PARAM_RISK, 6);
        printNarrativeInput(out, "Opportunities noticed", PARAM_OPPORTUNITY, 6);
        out.println("  <tr><td class=\"boxed-submit\" colspan=\"2\">\n");
        out.println("    <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_SAVE
                + "\">\n");
        out.println("  </td></tr>\n");
        out.println("</table>\n");
    }

    private void printNarrativeInput(PrintWriter out, String label, String name, int rows) {
        out.println("  <tr>\n");
        out.println("    <th class=\"inside\">" + label + "</th>\n");
        out.println("    <td class=\"inside\">\n");
        out.println("      <textarea name=\"" + name + "\" rows=\"" + rows
                + "\" cols=\"90\" onkeydown=\"resetRefresh()\"></textarea>\n");
        out.println("    </td>\n");
        out.println("  </tr>\n");
    }

    private void printCompletedActions(PrintWriter out, ProjectNarrativeDao narrativeDao, long projectId,
            LocalDate reviewDate) {
        List<Action> actions = narrativeDao.getCompletedActionsForProjectOnDate(projectId, reviewDate);
        out.println("<table class=\"boxed\">\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"title\" colspan=\"2\">Completed actions today</th>\n");
        out.println("  </tr>\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"boxed\">Action</th>\n");
        out.println("    <th class=\"boxed\">Completion note</th>\n");
        out.println("  </tr>\n");
        if (actions.isEmpty()) {
            out.println("  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"2\">No actions completed.</td></tr>\n");
        } else {
            for (Action action : actions) {
                out.println("  <tr class=\"boxed\">\n");
                out.println("    <td class=\"boxed\">" + escapeHtml(action.getDescription()) + "</td>\n");
                out.println("    <td class=\"boxed\">" + escapeHtml(n(action.getCompletionNote())) + "</td>\n");
                out.println("  </tr>\n");
            }
        }
        out.println("</table><br/>\n");
    }

    private void printExistingNarratives(PrintWriter out, ProjectNarrativeDao narrativeDao, long projectId,
            LocalDate reviewDate) {
        List<ProjectNarrative> narratives = narrativeDao.findByProjectAndDateRange(projectId, reviewDate);
        if (narratives.isEmpty()) {
            out.println("<table class=\"boxed\">\n");
            out.println("  <tr class=\"boxed\">\n");
            out.println("    <th class=\"title\" colspan=\"2\">Existing narratives</th>\n");
            out.println("  </tr>\n");
            out.println("  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"2\">None recorded.</td></tr>\n");
            out.println("</table><br/>\n");
            return;
        }

        Map<ProjectNarrativeVerb, List<String>> grouped = new EnumMap<ProjectNarrativeVerb, List<String>>(
                ProjectNarrativeVerb.class);
        for (ProjectNarrative narrative : narratives) {
            ProjectNarrativeVerb verb = narrative.getNarrativeVerb();
            if (verb == null) {
                continue;
            }
            List<String> list = grouped.get(verb);
            if (list == null) {
                list = new ArrayList<String>();
                grouped.put(verb, list);
            }
            list.add(narrative.getNarrativeText());
        }

        out.println("<table class=\"boxed\">\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"title\" colspan=\"2\">Existing narratives</th>\n");
        out.println("  </tr>\n");
        for (ProjectNarrativeVerb verb : ProjectNarrativeVerb.values()) {
            List<String> list = grouped.get(verb);
            if (list == null || list.isEmpty()) {
                continue;
            }
            out.println("  <tr class=\"boxed\">\n");
            out.println("    <th class=\"boxed\">" + escapeHtml(verb.getLabel()) + "</th>\n");
            out.println("    <td class=\"boxed\">\n");
            out.println("      <ul>\n");
            for (String item : list) {
                out.println("        <li>" + escapeHtml(n(item)) + "</li>\n");
            }
            out.println("      </ul>\n");
            out.println("    </td>\n");
            out.println("  </tr>\n");
        }
        out.println("</table><br/>\n");
    }

    private void printReviewList(PrintWriter out, List<ReviewItem> reviewItems, long selectedProjectId,
            LocalDate reviewDate) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate previousDate = reviewDate.minusDays(1);
        LocalDate nextDate = reviewDate.plusDays(1);

        out.println("<table class=\"boxed\">\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"title\" colspan=\"3\">Review List</th>\n");
        out.println("  </tr>\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <td class=\"boxed\" colspan=\"3\">Review Date: " + reviewDate + "</td>\n");
        out.println("  </tr>\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <td class=\"boxed\" colspan=\"3\">\n");
        out.println("      <a class=\"button\" href=\"ProjectNarrativeReviewServlet?" + PARAM_DATE
                + "=" + previousDate + "\">Previous Day</a>\n");
        if (reviewDate.isBefore(today)) {
            out.println("      <a class=\"button\" href=\"ProjectNarrativeReviewServlet?" + PARAM_DATE
                    + "=" + nextDate + "\">Next Day</a>\n");
        }
        out.println("    </td>\n");
        out.println("  </tr>\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"boxed\">Project</th>\n");
        out.println("    <th class=\"boxed\">Minutes</th>\n");
        out.println("    <th class=\"boxed\">Reviewed</th>\n");
        out.println("  </tr>\n");

        if (reviewItems.isEmpty()) {
            out.println("  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"3\">No work recorded.</td></tr>\n");
        } else {
            for (ReviewItem item : reviewItems) {
                boolean selected = item.getProjectId() == selectedProjectId;
                String rowClass = selected ? "inside-highlight" : "inside";
                out.println("  <tr>\n");
                out.println("    <td class=\"" + rowClass + "\">\n");
                out.println("      <button type=\"submit\" name=\"" + PARAM_SELECT_PROJECT_ID
                        + "\" value=\"" + item.getProjectId() + "\" class=\"button\">"
                        + escapeHtml(n(item.getProjectName())) + "</button>\n");
                out.println("    </td>\n");
                out.println("    <td class=\"" + rowClass + "\">" + TimeTracker.formatTime(item.getMinutesSpent())
                        + "</td>\n");
                out.println("    <td class=\"" + rowClass + "\">" + (item.isReviewed() ? "Yes" : "No") + "</td>\n");
                out.println("  </tr>\n");
            }
        }
        out.println("</table>\n");
    }

    private LocalDate resolveReviewDate(HttpServletRequest request, AppReq appReq) {
        String dateParam = request.getParameter(PARAM_DATE);
        if (dateParam == null || dateParam.trim().length() == 0) {
            return LocalDate.now(ZoneId.systemDefault());
        }
        try {
            return LocalDate.parse(dateParam.trim());
        } catch (DateTimeParseException e) {
            appReq.setMessageProblem("Invalid date format. Use YYYY-MM-DD.");
            return LocalDate.now(ZoneId.systemDefault());
        }
    }

    private ReviewItem selectReviewItem(List<ReviewItem> reviewItems, long projectId) {
        if (projectId > 0) {
            for (ReviewItem item : reviewItems) {
                if (item.getProjectId() == projectId) {
                    return item;
                }
            }
        }
        for (ReviewItem item : reviewItems) {
            if (!item.isReviewed()) {
                return item;
            }
        }
        return null;
    }

    private ReviewItem findNextUnreviewed(List<ReviewItem> reviewItems, long currentProjectId) {
        for (ReviewItem item : reviewItems) {
            if (!item.isReviewed() && item.getProjectId() != currentProjectId) {
                return item;
            }
        }
        return null;
    }

    private static Date buildNarrativeDate(LocalDate date, int offsetSeconds) {
        long offsetMillis = offsetSeconds * 1000L;
        Date start = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return new Date(start.getTime() + offsetMillis);
    }

    private static long readLong(String value) {
        if (value == null || value.trim().length() == 0) {
            return 0;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
