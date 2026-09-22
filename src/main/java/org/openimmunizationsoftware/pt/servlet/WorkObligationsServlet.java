package org.openimmunizationsoftware.pt.servlet;

import static org.openimmunizationsoftware.pt.util.WebEscaper.escapeHtml;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.WorkObligationDao;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.WorkObligation;

public class WorkObligationsServlet extends ClientServlet {
    private static final DateTimeFormatter WEEK_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, uuuu");
    private static final double DEFAULT_HOURS = 37.5;
    private static final double MAX_HOURS = 80.0;

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

            LocalDate anchor = owner.getLocalDateToday().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
            LocalDate start = anchor.minusWeeks(8);
            LocalDate end = anchor.plusWeeks(12);
            LocalDate focusWeek = parseWeek(request.getParameter("week"));
            if (focusWeek != null) {
                if (focusWeek.isBefore(start))
                    start = focusWeek;
                if (focusWeek.isAfter(end))
                    end = focusWeek;
            }

            if ("Save".equals(request.getParameter("action"))) {
                LocalDate formStart = parseWeek(request.getParameter("rangeStart"));
                LocalDate formEnd = parseWeek(request.getParameter("rangeEnd"));
                if (formStart != null && formEnd != null) {
                    start = formStart;
                    end = formEnd;
                }
                String problem = applyForm(request, dataSession, owner.getWebUserId(), start, end);
                if (problem == null) {
                    response.sendRedirect("WorkObligationsServlet"
                            + (focusWeek == null ? "" : "?week=" + focusWeek));
                    return;
                }
                appReq.setMessageProblem(problem);
            }

            renderPage(appReq, owner, start, end, focusWeek);
        } finally {
            appReq.close();
        }
    }

    private String applyForm(HttpServletRequest request, Session dataSession, int ownerUserId, LocalDate start,
            LocalDate end) {
        List<LocalDate> weeks = weekList(start, end);
        Map<LocalDate, Integer> parsedMinutes = new LinkedHashMap<LocalDate, Integer>();
        Map<LocalDate, String> parsedNotes = new LinkedHashMap<LocalDate, String>();
        for (LocalDate week : weeks) {
            String hoursValue = request.getParameter("hours_" + week);
            double hours;
            try {
                hours = hoursValue == null || hoursValue.trim().length() == 0 ? DEFAULT_HOURS
                        : Double.parseDouble(hoursValue.trim());
            } catch (NumberFormatException e) {
                return "Enter a valid number of hours for the week of " + DISPLAY_DATE.format(week) + ".";
            }
            if (hours < 0 || hours > MAX_HOURS) {
                return "Hours for the week of " + DISPLAY_DATE.format(week) + " must be between 0 and " + MAX_HOURS
                        + ".";
            }
            String note = n(request.getParameter("note_" + week)).trim();
            parsedMinutes.put(week, Integer.valueOf((int) Math.round(hours * 60)));
            parsedNotes.put(week, note.length() == 0 ? null : note);
        }

        Map<LocalDate, WorkObligation> existing = new LinkedHashMap<LocalDate, WorkObligation>();
        for (WorkObligation obligation : new WorkObligationDao(dataSession).listForRange(ownerUserId, start,
                end.plusWeeks(1))) {
            existing.put(obligation.getWeekStartLocalDate(), obligation);
        }

        Transaction transaction = dataSession.beginTransaction();
        Date now = new Date();
        for (LocalDate week : weeks) {
            WorkObligation obligation = existing.get(week);
            if (obligation == null) {
                obligation = new WorkObligation();
                obligation.setOwnerUserId(ownerUserId);
                obligation.setWeekStartLocalDate(week);
                obligation.setCreatedAt(now);
            }
            obligation.setObligatedMinutes(parsedMinutes.get(week).intValue());
            obligation.setNote(parsedNotes.get(week));
            obligation.setUpdatedAt(now);
            dataSession.saveOrUpdate(obligation);
        }
        transaction.commit();
        return null;
    }

    private void renderPage(AppReq appReq, WebUser owner, LocalDate start, LocalDate end, LocalDate focusWeek) {
        Session dataSession = appReq.getDataSession();
        Map<LocalDate, WorkObligation> existing = new LinkedHashMap<LocalDate, WorkObligation>();
        for (WorkObligation obligation : new WorkObligationDao(dataSession).listForRange(owner.getWebUserId(), start,
                end.plusWeeks(1))) {
            existing.put(obligation.getWeekStartLocalDate(), obligation);
        }

        appReq.setTitle("Work Obligations");
        printHtmlHead(appReq);
        PrintWriter out = appReq.getOut();
        printDandelionLocation(out, "Time Management & Reporting / Work Obligations");
        out.println("<p>Set the hours you're obligated to work each week. Weeks default to " + DEFAULT_HOURS
                + " hours; lower a week for a holiday, sick day, or vacation. "
                + "The weekly report uses these hours as the denominator for billing-allocation percentages.</p>");
        out.println("<form method=\"POST\" action=\"WorkObligationsServlet\">");
        out.println("<input type=\"hidden\" name=\"rangeStart\" value=\"" + start + "\">");
        out.println("<input type=\"hidden\" name=\"rangeEnd\" value=\"" + end + "\">");
        out.println("<table class=\"boxed\"><tr><th class=\"title\" colspan=\"3\">Weekly Hours Obligated</th></tr>");
        out.println(
                "<tr class=\"boxed\"><th class=\"boxed\">Week</th><th class=\"boxed\">Hours Obligated</th><th class=\"boxed\">Note</th></tr>");
        for (LocalDate week : weekList(start, end)) {
            WorkObligation obligation = existing.get(week);
            double hours = obligation == null ? DEFAULT_HOURS : obligation.getObligatedMinutes() / 60.0;
            String note = obligation == null ? "" : n(obligation.getNote());
            boolean focused = focusWeek != null && focusWeek.equals(week);
            String rowId = "week-" + week;
            out.println("<tr class=\"boxed\"" + (focused ? " style=\"background:#fffbe0\"" : "") + " id=\"" + rowId
                    + "\"><td class=\"boxed\">" + escapeHtml(DISPLAY_DATE.format(week)) + " through "
                    + escapeHtml(DISPLAY_DATE.format(week.plusDays(6)))
                    + (week.equals(owner.getLocalDateToday().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)))
                            ? " <strong>(This week)</strong>"
                            : "")
                    + "</td><td class=\"boxed\"><input type=\"number\" step=\"0.25\" min=\"0\" max=\""
                    + MAX_HOURS + "\" size=\"6\" name=\"hours_" + week + "\" value=\"" + formatHours(hours)
                    + "\"></td><td class=\"boxed\"><input type=\"text\" maxlength=\"150\" size=\"30\" name=\"note_"
                    + week + "\" value=\"" + escapeHtmlAttribute(note) + "\"></td></tr>");
        }
        out.println(
                "<tr><td class=\"boxed-submit\" colspan=\"3\"><input type=\"submit\" name=\"action\" value=\"Save\"></td></tr></table>");
        out.println("</form>");
        printHtmlFoot(appReq);
    }

    private static List<LocalDate> weekList(LocalDate start, LocalDate end) {
        List<LocalDate> weeks = new ArrayList<LocalDate>();
        for (LocalDate week = start; !week.isAfter(end); week = week.plusWeeks(1)) {
            weeks.add(week);
        }
        return weeks;
    }

    private static String formatHours(double hours) {
        if (hours == Math.floor(hours)) {
            return String.valueOf((long) hours);
        }
        return String.valueOf(hours);
    }

    private static LocalDate parseWeek(String value) {
        if (value == null || value.trim().length() == 0)
            return null;
        try {
            return LocalDate.parse(value.trim(), WEEK_FORMAT).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
