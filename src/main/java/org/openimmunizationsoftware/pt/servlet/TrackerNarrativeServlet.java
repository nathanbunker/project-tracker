/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.TrackerNarrativeDao;
import org.openimmunizationsoftware.pt.manager.NarrativePeriods;
import org.openimmunizationsoftware.pt.manager.NarrativePeriods.PeriodRange;
import org.openimmunizationsoftware.pt.manager.TrackerNarrativeGenerator;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;
import org.openimmunizationsoftware.pt.model.TrackerNarrativeReviewStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * Tracker narrative list + editor
 * 
 * @author nathan
 */
public class TrackerNarrativeServlet extends ClientServlet {

    private static final long serialVersionUID = 1952130555696282854L;

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final HtmlRenderer MARKDOWN_RENDERER = HtmlRenderer.builder().build();

    private static final String PARAM_ID = "id";
    private static final String PARAM_DATE = "date";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_VIEW = "view";
    private static final String PARAM_MARKDOWN_FINAL = "markdownFinal";

    private static final String ACTION_SAVE = "Save";
    private static final String ACTION_APPROVE = "Approve";
    private static final String ACTION_REGENERATE = "Regenerate";
    private static final String ACTION_REJECT = "Reject";
    private static final String ACTION_DELETE = "Delete";
    private static final String ACTION_GENERATE = "Generate";

    private static final String TYPE_DAILY = "DAILY";
    private static final String TYPE_WEEKLY = "WEEKLY";
    private static final String TYPE_MONTHLY = "MONTHLY";

    private static final String VIEW_EDIT = "edit";

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
            TrackerNarrativeDao narrativeDao = new TrackerNarrativeDao(dataSession);

            if ("POST".equalsIgnoreCase(request.getMethod())) {
                handlePost(request, response, appReq, narrativeDao);
                return;
            }

            String type = resolveType(request);
            LocalDate selectedDate = resolveDate(request, appReq);
            PeriodRange period = resolvePeriod(type, selectedDate);
            long narrativeId = readLong(request.getParameter(PARAM_ID));
            boolean editMode = VIEW_EDIT.equalsIgnoreCase(n(request.getParameter(PARAM_VIEW)));

            appReq.setTitle("Narrative");
            printHtmlHead(appReq);

            PrintWriter out = appReq.getOut();
            if (narrativeId > 0) {
                TrackerNarrative narrative = (TrackerNarrative) dataSession.get(TrackerNarrative.class,
                        (int) narrativeId);
                if (narrative == null) {
                    out.println("<p class=\"fail\">Narrative not found.</p>");
                } else {
                    printEditor(out, webUser, narrative, type, selectedDate, editMode);
                }
            } else {
                printList(out, webUser, narrativeDao, type, selectedDate, period);
            }

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private void handlePost(HttpServletRequest request, HttpServletResponse response, AppReq appReq,
            TrackerNarrativeDao narrativeDao) throws IOException {
        String action = appReq.getAction();
        long narrativeId = readLong(request.getParameter(PARAM_ID));
        String type = resolveType(request);
        LocalDate selectedDate = resolveDate(request, appReq);

        if (action == null || action.trim().length() == 0) {
            response.sendRedirect(buildListLink(type, selectedDate));
            return;
        }

        if (ACTION_APPROVE.equals(action)) {
            if (narrativeId > 0) {
                narrativeDao.approve(narrativeId, LocalDateTime.now(ZoneId.systemDefault()));
                appReq.setMessageConfirmation("Narrative approved.");
            }
            response.sendRedirect(buildEditorLink(narrativeId, type, selectedDate));
            return;
        }

        Session dataSession = appReq.getDataSession();
        Transaction transaction = null;
        String redirect = buildListLink(type, selectedDate);
        long queuedNarrativeId = 0;
        try {
            transaction = dataSession.beginTransaction();

            if (ACTION_SAVE.equals(action)) {
                String markdownFinal = n(request.getParameter(PARAM_MARKDOWN_FINAL));
                narrativeDao.updateFinalText(narrativeId, normalizeMarkdown(markdownFinal));
                appReq.setMessageConfirmation("Narrative updated.");
                redirect = buildEditorLink(narrativeId, type, selectedDate);
            } else if (ACTION_REJECT.equals(action)) {
                narrativeDao.reject(narrativeId);
                appReq.setMessageConfirmation("Narrative rejected.");
                redirect = buildEditorLink(narrativeId, type, selectedDate);
            } else if (ACTION_DELETE.equals(action)) {
                narrativeDao.softDelete(narrativeId);
                appReq.setMessageConfirmation("Narrative deleted.");
                redirect = buildEditorLink(narrativeId, type, selectedDate);
            } else if (ACTION_GENERATE.equals(action)) {
                PeriodRange period = resolvePeriod(type, selectedDate);
                TrackerNarrative newNarrative = buildNewNarrative(type, period);
                queuedNarrativeId = narrativeDao.insert(newNarrative);
                appReq.setMessageConfirmation("Generation requested.");
                redirect = buildEditorLink(queuedNarrativeId, type, selectedDate);
            } else if (ACTION_REGENERATE.equals(action)) {
                TrackerNarrative existing = (TrackerNarrative) dataSession.get(TrackerNarrative.class,
                        (int) narrativeId);
                if (existing == null) {
                    appReq.setMessageProblem("Narrative not found.");
                } else {
                    PeriodRange period = new PeriodRange(toLocalDate(existing.getPeriodStart()),
                            toLocalDate(existing.getPeriodEnd()));
                    TrackerNarrative replacement = buildNewNarrative(existing.getNarrativeType(), period);
                    long newId = narrativeDao.insert(replacement);
                    queuedNarrativeId = newId;
                    appReq.setMessageConfirmation("Generation requested.");
                    redirect = buildEditorLink(newId, type, selectedDate);
                }
            }

            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw exception;
        }

        if (queuedNarrativeId > 0) {
            TrackerNarrativeGenerator.enqueue(queuedNarrativeId);
        }

        response.sendRedirect(redirect);
    }

    private void printList(PrintWriter out, WebUser webUser, TrackerNarrativeDao narrativeDao, String type,
            LocalDate selectedDate, PeriodRange period) {
        out.println("<h2>Tracker Narratives</h2>");

        out.println("<form method=\"GET\" action=\"TrackerNarrativeServlet\">");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"2\">Filter</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Narrative Type</th>");
        out.println("    <td class=\"boxed\">" + buildTypeSelect(type) + "</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Date</th>");
        out.println("    <td class=\"boxed\"><input type=\"date\" name=\"" + PARAM_DATE
                + "\" value=\"" + selectedDate + "\"></td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\" colspan=\"2\">"
                + "<input type=\"submit\" value=\"Apply\"></td>");
        out.println("  </tr>");
        out.println("</table>");
        out.println("</form><br/>");

        out.println("<form method=\"POST\" action=\"TrackerNarrativeServlet\">\n");
        out.println("<input type=\"hidden\" name=\"" + PARAM_TYPE + "\" value=\"" + type + "\">\n");
        out.println("<input type=\"hidden\" name=\"" + PARAM_DATE + "\" value=\"" + selectedDate + "\">\n");
        out.println("<input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_GENERATE
                + "\">\n");
        out.println("</form><br/>\n");

        String periodLabel = formatPeriod(webUser, period);
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"4\">Period: " + periodLabel + "</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Status</th>");
        out.println("    <th class=\"boxed\">Title</th>");
        out.println("    <th class=\"boxed\">Generated</th>");
        out.println("    <th class=\"boxed\">Edit</th>");
        out.println("  </tr>");

        List<TrackerNarrative> narratives = narrativeDao.findByTypeAndPeriod(type, period.getStart(), period.getEnd());
        TrackerNarrative approved = narrativeDao.findApprovedByTypeAndPeriod(type, period.getStart(), period.getEnd());
        int approvedId = approved == null ? 0 : approved.getNarrativeId();

        if (narratives.isEmpty()) {
            out.println("  <tr class=\"boxed\"><td class=\"boxed\" colspan=\"4\">No narratives found.</td></tr>");
        } else {
            for (TrackerNarrative narrative : narratives) {
                boolean isApproved = narrative.getNarrativeId() == approvedId;
                String rowClass = isApproved ? "boxed-highlight" : "boxed";
                out.println("  <tr class=\"boxed\">");
                out.println("    <td class=\"" + rowClass + "\">"
                        + escapeHtml(n(narrative.getReviewStatusString())) + (isApproved ? " (approved)" : "")
                        + "</td>");
                out.println("    <td class=\"" + rowClass + "\">" + escapeHtml(n(narrative.getDisplayTitle()))
                        + "</td>");
                out.println(
                        "    <td class=\"" + rowClass + "\">" + formatDateTime(webUser, narrative.getDateGenerated())
                                + "</td>");
                out.println("    <td class=\"" + rowClass + "\"><a class=\"button\" href=\""
                        + buildEditorLink(narrative.getNarrativeId(), type, selectedDate) + "\">Edit</a></td>");
                out.println("  </tr>");
            }
        }
        out.println("</table>");
    }

    private void printEditor(PrintWriter out, WebUser webUser, TrackerNarrative narrative, String type,
            LocalDate selectedDate, boolean editMode) {
        out.println("<h2>Tracker Narrative</h2>");
        out.println("<p><a class=\"button\" href=\"" + buildListLink(type, selectedDate)
                + "\">Back to list</a></p>");

        out.println("<form method=\"POST\" action=\"TrackerNarrativeServlet\">\n");
        out.println("<input type=\"hidden\" name=\"" + PARAM_ID + "\" value=\""
                + narrative.getNarrativeId() + "\">\n");
        out.println("<input type=\"hidden\" name=\"" + PARAM_TYPE + "\" value=\"" + escapeHtml(type)
                + "\">\n");
        out.println("<input type=\"hidden\" name=\"" + PARAM_DATE + "\" value=\"" + selectedDate
                + "\">\n");

        out.println("<table class=\"boxed\">\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"title\" colspan=\"2\">Details</th>\n");
        out.println("  </tr>\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"boxed\">Title</th>\n");
        out.println("    <td class=\"boxed\">" + escapeHtml(n(narrative.getDisplayTitle())) + "</td>\n");
        out.println("  </tr>\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"boxed\">Type</th>\n");
        out.println("    <td class=\"boxed\">" + escapeHtml(n(narrative.getNarrativeType())) + "</td>\n");
        out.println("  </tr>\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"boxed\">Status</th>\n");
        out.println("    <td class=\"boxed\">" + escapeHtml(n(narrative.getReviewStatusString())) + "</td>\n");
        out.println("  </tr>\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"boxed\">Generated</th>\n");
        out.println("    <td class=\"boxed\">" + formatDateTime(webUser, narrative.getDateGenerated()) + "</td>\n");
        out.println("  </tr>\n");
        out.println("</table><br/>\n");

        if (TrackerNarrativeReviewStatus.GENERATING.equals(narrative.getReviewStatus())) {
            out.println("<p class=\"fail\">Generating... "
                    + "<a class=\"button\" href=\"" + buildEditorLink(narrative.getNarrativeId(), type,
                            selectedDate, editMode ? VIEW_EDIT : null)
                    + "\">Refresh</a></p>\n");
        }

        if (editMode) {
            out.println("<p><a class=\"button\" href=\""
                    + buildEditorLink(narrative.getNarrativeId(), type, selectedDate)
                    + "\">Preview</a></p>\n");
            out.println("<table class=\"boxed-fill\">\n");
            out.println("  <tr class=\"boxed\">\n");
            out.println("    <th class=\"title\">Final Markdown</th>\n");
            out.println("  </tr>\n");
            out.println("  <tr class=\"boxed\">\n");
            out.println("    <td class=\"boxed\"><textarea name=\"" + PARAM_MARKDOWN_FINAL
                    + "\" rows=\"20\" cols=\"100\" onkeydown=\"resetRefresh()\">"
                    + escapeHtml(n(narrative.getMarkdownFinal())) + "</textarea></td>\n");
            out.println("  </tr>\n");
            out.println("</table><br/>\n");
        } else {
            out.println("<p><a class=\"button\" href=\""
                    + buildEditorLink(narrative.getNarrativeId(), type, selectedDate, VIEW_EDIT)
                    + "\">Edit</a></p>\n");
            out.println("<table class=\"boxed-fill\">\n");
            out.println("  <tr class=\"boxed\">\n");
            out.println("    <th class=\"title\">Final Markdown</th>\n");
            out.println("  </tr>\n");
            out.println("  <tr class=\"boxed\">\n");
            out.println("    <td class=\"boxed\"><div class=\"scrollbox\">"
                    + renderMarkdown(n(narrative.getMarkdownFinal())) + "</div></td>\n");
            out.println("  </tr>\n");
            out.println("</table><br/>\n");
        }

        String generatedId = "generatedMarkdown" + narrative.getNarrativeId();
        out.println("<p><a class=\"button\" href=\"javascript:toggleLayer('" + generatedId
                + "')\">Toggle Generated Markdown</a></p>\n");
        out.println("<div id=\"" + generatedId + "\" style=\"display:none;\">\n");
        out.println("<table class=\"boxed-fill\">\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <th class=\"title\">Generated Markdown (read-only)</th>\n");
        out.println("  </tr>\n");
        out.println("  <tr class=\"boxed\">\n");
        out.println("    <td class=\"boxed\"><div class=\"scrollbox\"><pre>"
                + escapeHtml(n(narrative.getMarkdownGenerated())) + "</pre></div></td>\n");
        out.println("  </tr>\n");
        out.println("</table></div><br/>\n");

        out.println("<p>");
        if (editMode) {
            out.println("  <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_SAVE
                    + "\">\n");
        }
        out.println("  <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_APPROVE
                + "\">\n");
        out.println("  <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_REGENERATE
                + "\">\n");
        out.println("  <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_REJECT + "\">\n");
        out.println("  <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_DELETE + "\">\n");
        out.println("</p>\n");
        out.println("</form>\n");
    }

    private static String buildTypeSelect(String selected) {
        StringBuilder sb = new StringBuilder();
        sb.append("<select name=\"").append(PARAM_TYPE).append("\">");
        sb.append(option(TYPE_DAILY, selected));
        sb.append(option(TYPE_WEEKLY, selected));
        sb.append(option(TYPE_MONTHLY, selected));
        sb.append("</select>");
        return sb.toString();
    }

    private static String option(String value, String selected) {
        String mark = value.equals(selected) ? " selected" : "";
        return "<option value=\"" + value + "\"" + mark + ">" + value + "</option>";
    }

    private static String formatPeriod(WebUser webUser, PeriodRange period) {
        return formatDate(webUser, period.getStart()) + " - " + formatDate(webUser, period.getEnd());
    }

    private static String formatDate(WebUser webUser, LocalDate date) {
        if (date == null) {
            return "";
        }
        Date asDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return webUser.getDateFormat().format(asDate);
    }

    private static String formatDateTime(WebUser webUser, Date date) {
        if (date == null) {
            return "";
        }
        return webUser.getTimeFormat().format(date);
    }

    private static TrackerNarrative buildNewNarrative(String type, PeriodRange period) {
        TrackerNarrative narrative = new TrackerNarrative();
        narrative.setNarrativeType(type);
        narrative.setPeriodStart(toDate(period.getStart()));
        narrative.setPeriodEnd(toDate(period.getEnd()));
        narrative.setDisplayTitle(buildDisplayTitle(type, period));
        narrative.setReviewStatus(TrackerNarrativeReviewStatus.GENERATING);
        narrative.setMarkdownGenerated(null);
        narrative.setMarkdownFinal(null);
        return narrative;
    }

    private static String buildDisplayTitle(String type, PeriodRange period) {
        if (TYPE_WEEKLY.equals(type)) {
            return "Weekly Summary - " + period.getStart() + " to " + period.getEnd();
        }
        if (TYPE_MONTHLY.equals(type)) {
            return "Monthly Summary - " + period.getStart().getYear() + "-"
                    + String.format("%02d", period.getStart().getMonthValue());
        }
        return "Daily Summary - " + period.getStart();
    }

    private static PeriodRange resolvePeriod(String type, LocalDate date) {
        if (TYPE_WEEKLY.equals(type)) {
            return NarrativePeriods.forWeekly(date);
        }
        if (TYPE_MONTHLY.equals(type)) {
            return NarrativePeriods.forMonthly(YearMonth.from(date));
        }
        return NarrativePeriods.forDaily(date);
    }

    private static String resolveType(HttpServletRequest request) {
        String type = request.getParameter(PARAM_TYPE);
        if (type == null || type.trim().length() == 0) {
            return TYPE_DAILY;
        }
        String normalized = type.trim().toUpperCase();
        if (TYPE_WEEKLY.equals(normalized) || TYPE_MONTHLY.equals(normalized) || TYPE_DAILY.equals(normalized)) {
            return normalized;
        }
        return TYPE_DAILY;
    }

    private static LocalDate resolveDate(HttpServletRequest request, AppReq appReq) {
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

    private static String renderMarkdown(String markdown) {
        String value = markdown == null ? "" : markdown;
        Node document = MARKDOWN_PARSER.parse(value);
        return MARKDOWN_RENDERER.render(document);
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

    private static String normalizeMarkdown(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : value;
    }

    private static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date toDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static String buildListLink(String type, LocalDate date) {
        return "TrackerNarrativeServlet?" + PARAM_TYPE + "=" + type + "&" + PARAM_DATE + "=" + date;
    }

    private static String buildEditorLink(long id, String type, LocalDate date) {
        return buildEditorLink(id, type, date, null);
    }

    private static String buildEditorLink(long id, String type, LocalDate date, String view) {
        String link = "TrackerNarrativeServlet?" + PARAM_ID + "=" + id + "&" + PARAM_TYPE + "=" + type
                + "&" + PARAM_DATE + "=" + date;
        if (view != null && view.trim().length() > 0) {
            link += "&" + PARAM_VIEW + "=" + view;
        }
        return link;
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
