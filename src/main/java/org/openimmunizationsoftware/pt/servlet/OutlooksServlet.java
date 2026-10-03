package org.openimmunizationsoftware.pt.servlet;

import static org.openimmunizationsoftware.pt.util.WebEscaper.escapeHtml;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dandeliondaily.outlook.service.PlanningOutlookService;
import org.dandeliondaily.outlook.service.PlanningOutlookService.OutlookResult;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.PlanningOutlook;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * Minimal view/edit page for monthly and weekly planning outlooks. Uses the same
 * PlanningOutlookService as the MCP tools, so the start-date and frozen-period
 * rules can't drift between the two.
 */
public class OutlooksServlet extends ClientServlet {
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, uuuu");
    private static final DateTimeFormatter DISPLAY_MONTH = DateTimeFormatter.ofPattern("MMMM uuuu");
    private static final int PAST_WEEKS = 8;
    private static final int PAST_MONTHS = 6;

    private final PlanningOutlookService service = new PlanningOutlookService();

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

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }
            Session dataSession = appReq.getDataSession();
            WebUser owner = appReq.getWebUser();
            LocalDate today = owner.getLocalDateToday();

            String rejectedType = null;
            LocalDate rejectedStart = null;
            String rejectedText = null;
            if ("Save".equals(request.getParameter("action"))) {
                String periodType = service.normalizePeriodType(request.getParameter("periodType"));
                LocalDate periodStart = parseDate(request.getParameter("periodStart"));
                String text = n(request.getParameter("outlookText")).trim();
                String problem = save(dataSession, owner, periodType, periodStart, text,
                        request.getParameter("loadedUpdatedAt"), today);
                if (problem == null) {
                    response.sendRedirect("OutlooksServlet#" + anchor(periodType, periodStart));
                    return;
                }
                appReq.setMessageProblem(problem);
                rejectedType = periodType;
                rejectedStart = periodStart;
                rejectedText = text;
            }

            renderPage(appReq, owner, today, rejectedType, rejectedStart, rejectedText);
        } finally {
            appReq.close();
        }
    }

    private String save(Session dataSession, WebUser owner, String periodType, LocalDate periodStart, String text,
            String loadedUpdatedAt, LocalDate today) {
        if (periodType == null || periodStart == null) {
            return "The outlook period was missing or invalid. Reload the page and try again.";
        }
        try {
            OutlookResult current = service.getOutlook(dataSession, owner.getWebUserId(), periodType, periodStart,
                    today);
            if (!updatedAtMarker(current.getOutlook()).equals(n(loadedUpdatedAt))) {
                return "This outlook was changed (possibly by the AI assistant) after you opened the page. "
                        + "Your text is shown below but was not saved; review the current version and save again.";
            }
            if (current.getOutlook() == null && text.length() == 0) {
                return null;
            }
            Transaction transaction = dataSession.beginTransaction();
            try {
                service.setOutlook(dataSession, owner.getWebUserId(), periodType, periodStart, text, today);
                transaction.commit();
            } catch (RuntimeException e) {
                transaction.rollback();
                throw e;
            }
            return null;
        } catch (IllegalStateException frozen) {
            return frozen.getMessage();
        } catch (IllegalArgumentException invalid) {
            return invalid.getMessage();
        }
    }

    private void renderPage(AppReq appReq, WebUser owner, LocalDate today, String rejectedType,
            LocalDate rejectedStart, String rejectedText) {
        Session dataSession = appReq.getDataSession();
        int ownerUserId = owner.getWebUserId();
        String week = PlanningOutlookService.PERIOD_TYPE_WEEK;
        String month = PlanningOutlookService.PERIOD_TYPE_MONTH;
        LocalDate thisWeek = service.periodStartFor(week, today);
        LocalDate thisMonth = service.periodStartFor(month, today);

        appReq.setTitle("Outlooks");
        printHtmlHead(appReq);
        PrintWriter out = appReq.getOut();
        printDandelionLocation(out, "Time Management & Reporting / Outlooks");
        out.println("<p>An outlook is what you intend to accomplish over a month or a week, across projects. "
                + "Write it before the period starts. Weeks start on Sunday. Once a period has ended its outlook "
                + "is read-only.</p>");

        out.println("<h2>Month</h2>");
        renderEditor(out, owner, service.getOutlook(dataSession, ownerUserId, month, thisMonth, today), "This month",
                rejectedType, rejectedStart, rejectedText);
        renderEditor(out, owner, service.getOutlook(dataSession, ownerUserId, month, thisMonth.plusMonths(1), today),
                "Next month", rejectedType, rejectedStart, rejectedText);

        out.println("<h2>Week</h2>");
        renderEditor(out, owner, service.getOutlook(dataSession, ownerUserId, week, thisWeek, today), "This week",
                rejectedType, rejectedStart, rejectedText);
        renderEditor(out, owner, service.getOutlook(dataSession, ownerUserId, week, thisWeek.plusWeeks(1), today),
                "Next week", rejectedType, rejectedStart, rejectedText);

        out.println("<h2>Past outlooks</h2>");
        renderPast(out, service.listOutlooks(dataSession, ownerUserId, week, thisWeek.minusWeeks(PAST_WEEKS),
                thisWeek, today), "Past weeks");
        renderPast(out, service.listOutlooks(dataSession, ownerUserId, month, thisMonth.minusMonths(PAST_MONTHS),
                thisMonth, today), "Past months");

        printHtmlFoot(appReq);
    }

    private void renderEditor(PrintWriter out, WebUser owner, OutlookResult result, String label,
            String rejectedType, LocalDate rejectedStart, String rejectedText) {
        PlanningOutlook outlook = result.getOutlook();
        boolean rejected = result.getPeriodType().equals(rejectedType)
                && result.getPeriodStart().equals(rejectedStart);
        String text = rejected ? rejectedText : (outlook == null ? "" : n(outlook.getOutlookText()));

        out.println("<form method=\"POST\" action=\"OutlooksServlet\" id=\""
                + anchor(result.getPeriodType(), result.getPeriodStart()) + "\">");
        out.println("<input type=\"hidden\" name=\"periodType\" value=\"" + result.getPeriodType() + "\">");
        out.println("<input type=\"hidden\" name=\"periodStart\" value=\"" + result.getPeriodStart() + "\">");
        out.println("<input type=\"hidden\" name=\"loadedUpdatedAt\" value=\"" + updatedAtMarker(outlook)
                + "\">");
        out.println("<table class=\"boxed\" style=\"width:100%;max-width:820px\">");
        out.println("<tr><th class=\"title\">" + escapeHtml(label) + ": " + escapeHtml(periodLabel(result))
                + "</th></tr>");
        out.println("<tr class=\"boxed\"><td class=\"boxed\"><textarea name=\"outlookText\" rows=\"8\" "
                + "style=\"width:100%;box-sizing:border-box\">" + escapeHtml(text) + "</textarea>");
        if (rejected && outlook != null && !n(outlook.getOutlookText()).equals(text)) {
            out.println("<div style=\"margin-top:6px;padding:8px;border:1px solid #d9ccb8;background:#fffbe0\">"
                    + "<strong>Currently saved version:</strong><div style=\"white-space:pre-wrap\">"
                    + escapeHtml(n(outlook.getOutlookText())) + "</div></div>");
        }
        if (outlook != null && outlook.getUpdatedAt() != null) {
            out.println("<div style=\"font-size:smaller;color:#6b6256\">Last updated "
                    + escapeHtml(owner.getDateFormatService().formatDateTime(outlook.getUpdatedAt(),
                            owner.getTimeZone()))
                    + "</div>");
        }
        out.println("</td></tr>");
        out.println("<tr><td class=\"boxed-submit\"><input type=\"submit\" name=\"action\" value=\"Save\">"
                + "</td></tr></table>");
        out.println("</form><br>");
    }

    private void renderPast(PrintWriter out, List<OutlookResult> results, String label) {
        out.println("<details><summary>" + escapeHtml(label) + " (" + results.size() + ")</summary>");
        if (results.isEmpty()) {
            out.println("<p><em>None recorded.</em></p>");
        }
        for (int i = results.size() - 1; i >= 0; i--) {
            OutlookResult result = results.get(i);
            out.println("<h3>" + escapeHtml(periodLabel(result)) + "</h3>");
            out.println("<div style=\"white-space:pre-wrap;max-width:820px\">"
                    + escapeHtml(n(result.getOutlook().getOutlookText())) + "</div>");
        }
        out.println("</details>");
    }

    private String periodLabel(OutlookResult result) {
        if (PlanningOutlookService.PERIOD_TYPE_MONTH.equals(result.getPeriodType())) {
            return DISPLAY_MONTH.format(result.getPeriodStart());
        }
        return DISPLAY_DATE.format(result.getPeriodStart()) + " through "
                + DISPLAY_DATE.format(result.getPeriodEnd());
    }

    /**
     * Version marker for the stale-save check. Whole seconds, since MySQL datetime
     * columns drop fractional seconds.
     */
    private static String updatedAtMarker(PlanningOutlook outlook) {
        if (outlook == null || outlook.getUpdatedAt() == null) {
            return "";
        }
        return String.valueOf(outlook.getUpdatedAt().getTime() / 1000);
    }

    private static String anchor(String periodType, LocalDate periodStart) {
        return periodType == null || periodStart == null ? "" : periodType.toLowerCase() + "-" + periodStart;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
