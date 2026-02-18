package org.openimmunizationsoftware.pt.mobile.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * Mobile Todo landing page showing READY ProjectActionNext items.
 * 
 * @author nathan
 */
public class TodoServlet extends MobileBaseServlet {

    private static final String PARAM_DATE = "date";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ACTION_ID = "actionId";
    private static final String PARAM_SHOW_PERSONAL = "showPersonal";
    private static final String PARAM_SHOW_WORK = "showWork";

    private static final String ACTION_COMPLETE = "complete";
    private static final String ACTION_TOMORROW = "tomorrow";

    private static final String SESSION_SHOW_PERSONAL = "mobile_show_personal";
    private static final String SESSION_SHOW_WORK = "mobile_show_work";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            String action = request.getParameter(PARAM_ACTION);
            String actionIdStr = request.getParameter(PARAM_ACTION_ID);

            if (action != null && actionIdStr != null) {
                int actionId = Integer.parseInt(actionIdStr);
                Session dataSession = appReq.getDataSession();
                ProjectActionNext projectAction = (ProjectActionNext) dataSession.get(
                        ProjectActionNext.class, actionId);

                if (projectAction != null) {
                    if (ACTION_COMPLETE.equals(action)) {
                        completeAction(projectAction, dataSession, appReq.getWebUser());
                    } else if (ACTION_TOMORROW.equals(action)) {
                        postponeToTomorrow(projectAction, dataSession, appReq.getWebUser());
                    }
                }
            }

            // POST-redirect-GET pattern
            String redirectUrl = buildRedirectUrl(request);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            WebUser webUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();

            // Resolve filter preferences
            resolveFilterPreferences(request);
            boolean showPersonal = isShowPersonal(request);
            boolean showWork = isShowWork(request);

            // Get selected date (default to today)
            Date selectedDate = getSelectedDate(request, webUser);
            Date today = TimeTracker.createToday(webUser).getTime();

            // Fetch READY actions for the selected date
            List<ProjectActionNext> allActions = fetchReadyActions(selectedDate, webUser, dataSession);

            // Filter by personal/work
            List<ProjectActionNext> overdueActions = new ArrayList<>();
            List<ProjectActionNext> todayActions = new ArrayList<>();

            boolean isToday = isSameDay(selectedDate, today, webUser);

            for (ProjectActionNext action : allActions) {
                if (!shouldShow(action, showPersonal, showWork)) {
                    continue;
                }

                if (isToday && action.getNextActionDate() != null && action.getNextActionDate().before(today)) {
                    overdueActions.add(action);
                } else {
                    todayActions.add(action);
                }
            }

            // Count for filter labels
            int personalCount = countPersonal(allActions);
            int workCount = countWork(allActions);

            // Render page
            appReq.setTitle("Todo");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();

            String title = formatTitle(selectedDate, today, webUser);
            out.println("<h1>" + title + "</h1>");

            // Filter checkboxes
            printFilters(out, showPersonal, showWork, personalCount, workCount, selectedDate);

            // Overdue section (only on today)
            if (isToday && !overdueActions.isEmpty()) {
                out.println("<h2>Overdue</h2>");
                printActionList(out, overdueActions, true, selectedDate);
            }

            // Today/selected date section
            if (!isToday) {
                out.println("<h2>" + formatDateSection(selectedDate, webUser) + "</h2>");
            }
            printActionList(out, todayActions, false, selectedDate);

            // Date navigation
            printDateNavigation(out, selectedDate, today, showPersonal, showWork, webUser);

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private void resolveFilterPreferences(HttpServletRequest request) {
        HttpSession session = request.getSession();

        String personalParam = request.getParameter(PARAM_SHOW_PERSONAL);
        String workParam = request.getParameter(PARAM_SHOW_WORK);

        if (personalParam != null || workParam != null) {
            // Explicit filter change
            boolean personal = "on".equals(personalParam);
            boolean work = "on".equals(workParam);

            // Ensure at least one is checked
            if (!personal && !work) {
                personal = true;
            }

            session.setAttribute(SESSION_SHOW_PERSONAL, personal);
            session.setAttribute(SESSION_SHOW_WORK, work);
        } else if (session.getAttribute(SESSION_SHOW_PERSONAL) == null) {
            // First time - defaults
            session.setAttribute(SESSION_SHOW_PERSONAL, true);
            session.setAttribute(SESSION_SHOW_WORK, false);
        }
    }

    private boolean isShowPersonal(HttpServletRequest request) {
        Boolean val = (Boolean) request.getSession().getAttribute(SESSION_SHOW_PERSONAL);
        return val != null ? val : true;
    }

    private boolean isShowWork(HttpServletRequest request) {
        Boolean val = (Boolean) request.getSession().getAttribute(SESSION_SHOW_WORK);
        return val != null ? val : false;
    }

    private Date getSelectedDate(HttpServletRequest request, WebUser webUser) {
        String dateParam = request.getParameter(PARAM_DATE);
        if (dateParam != null && !dateParam.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse(dateParam);
            } catch (Exception e) {
                // Fall through to today
            }
        }
        return TimeTracker.createToday(webUser).getTime();
    }

    private List<ProjectActionNext> fetchReadyActions(Date selectedDate, WebUser webUser, Session dataSession) {
        Calendar cal = webUser.getCalendar();
        cal.setTime(selectedDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date nextDayStart = cal.getTime();

        // Fetch all READY actions for this date OR overdue (if viewing today)
        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan " +
                        "left join fetch pan.project " +
                        "left join fetch pan.contact " +
                        "left join fetch pan.nextProjectContact " +
                        "where pan.provider = :provider " +
                        "and (pan.contactId = :contactId or pan.nextContactId = :contactId) " +
                        "and pan.nextActionStatusString = :status " +
                        "and pan.nextDescription <> '' " +
                        "and pan.nextActionDate < :nextDayStart " +
                        "order by pan.nextActionDate, pan.priorityLevel DESC, pan.nextChangeDate");

        query.setParameter("provider", webUser.getProvider());
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        query.setParameter("nextDayStart", nextDayStart);

        @SuppressWarnings("unchecked")
        List<ProjectActionNext> results = query.list();

        // Filter to only include items due before next day (date-only semantics)
        List<ProjectActionNext> filtered = new ArrayList<>();
        for (ProjectActionNext action : results) {
            if (action.getNextActionDate() != null && action.getNextActionDate().before(nextDayStart)) {
                // Load lazy associations
                if (action.getProject() == null && action.getProjectId() > 0) {
                    action.setProject((Project) dataSession.get(Project.class, action.getProjectId()));
                }
                if (action.getContact() == null && action.getContactId() > 0) {
                    action.setContact((ProjectContact) dataSession.get(ProjectContact.class, action.getContactId()));
                }
                if (action.getNextProjectContact() == null && action.getNextContactId() != null
                        && action.getNextContactId() > 0) {
                    action.setNextProjectContact(
                            (ProjectContact) dataSession.get(ProjectContact.class, action.getNextContactId()));
                }
                filtered.add(action);
            }
        }

        return filtered;
    }

    private boolean shouldShow(ProjectActionNext action, boolean showPersonal, boolean showWork) {
        boolean isWork = action.isBillable();
        if (isWork) {
            return showWork;
        } else {
            return showPersonal;
        }
    }

    private int countPersonal(List<ProjectActionNext> actions) {
        int count = 0;
        for (ProjectActionNext action : actions) {
            if (!action.isBillable()) {
                count++;
            }
        }
        return count;
    }

    private int countWork(List<ProjectActionNext> actions) {
        int count = 0;
        for (ProjectActionNext action : actions) {
            if (action.isBillable()) {
                count++;
            }
        }
        return count;
    }

    private boolean isSameDay(Date date1, Date date2, WebUser webUser) {
        if (date1 == null || date2 == null)
            return false;
        Calendar cal1 = webUser.getCalendar();
        cal1.setTime(date1);
        Calendar cal2 = webUser.getCalendar();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private String formatTitle(Date selectedDate, Date today, WebUser webUser) {
        if (isSameDay(selectedDate, today, webUser)) {
            return "Todo Today";
        }

        Calendar cal = webUser.getCalendar();
        cal.setTime(selectedDate);
        Calendar todayCal = webUser.getCalendar();
        todayCal.setTime(today);

        long daysDiff = (cal.getTimeInMillis() - todayCal.getTimeInMillis()) / (1000 * 60 * 60 * 24);

        if (daysDiff >= 1 && daysDiff <= 7) {
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return "Todo " + dayFormat.format(selectedDate);
        } else if (daysDiff >= 8 && daysDiff <= 14) {
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return "Todo Next " + dayFormat.format(selectedDate);
        } else {
            SimpleDateFormat dateFormat = webUser.getDateFormat();
            return "Todo " + dateFormat.format(selectedDate);
        }
    }

    private String formatDateSection(Date date, WebUser webUser) {
        SimpleDateFormat sdf = webUser.getDateFormat();
        return sdf.format(date);
    }

    private void printFilters(PrintWriter out, boolean showPersonal, boolean showWork,
            int personalCount, int workCount, Date selectedDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateParam = selectedDate != null ? sdf.format(selectedDate) : "";

        out.println("<form method=\"get\" action=\"todo\">");
        if (!dateParam.isEmpty()) {
            out.println("  <input type=\"hidden\" name=\"" + PARAM_DATE + "\" value=\"" + dateParam + "\" />");
        }
        out.println("  <div class=\"filters\">");
        out.println("    <label>");
        out.println("      <input type=\"checkbox\" name=\"" + PARAM_SHOW_PERSONAL + "\" value=\"on\" " +
                (showPersonal ? "checked" : "") + " onchange=\"this.form.submit()\" />");
        out.println("      Personal (" + personalCount + ")");
        out.println("    </label>");
        out.println("    <label>");
        out.println("      <input type=\"checkbox\" name=\"" + PARAM_SHOW_WORK + "\" value=\"on\" " +
                (showWork ? "checked" : "") + " onchange=\"this.form.submit()\" />");
        out.println("      Work (" + workCount + ")");
        out.println("    </label>");
        out.println("  </div>");
        out.println("</form>");
    }

    private void printActionList(PrintWriter out, List<ProjectActionNext> actions,
            boolean isOverdue, Date selectedDate) {
        if (actions.isEmpty()) {
            out.println("<p>No items</p>");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateParam = selectedDate != null ? sdf.format(selectedDate) : "";

        out.println("<ul class=\"todo-list\">");
        for (ProjectActionNext action : actions) {
            out.println("  <li class=\"todo-item\">");
            out.println("    <form method=\"post\" action=\"todo\" style=\"display:inline;\">");
            out.println("      <input type=\"hidden\" name=\"" + PARAM_ACTION_ID + "\" value=\""
                    + action.getActionNextId() + "\" />");
            out.println(
                    "      <input type=\"hidden\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_COMPLETE + "\" />");
            if (!dateParam.isEmpty()) {
                out.println("      <input type=\"hidden\" name=\"" + PARAM_DATE + "\" value=\"" + dateParam + "\" />");
            }
            out.println("      <button type=\"submit\" class=\"checkbox-btn\">&#x2610;</button>");
            out.println("    </form>");

            String projectName = action.getProject() != null ? action.getProject().getProjectName() : "";
            String description = action.getNextDescriptionForDisplay(action.getContact());
            String badge = action.isBillable() ? "W" : "P";
            String badgeClass = action.isBillable() ? "badge-work" : "badge-personal";

            out.println("    <div class=\"todo-content\">");
            out.println("      <div class=\"todo-text\">");
            if (!projectName.isEmpty()) {
                out.println("        <strong>" + escapeHtml(projectName) + ":</strong> ");
            }
            out.println(escapeHtml(description));
            out.println("        <span class=\"badge " + badgeClass + "\">" + badge + "</span>");
            if (isOverdue) {
                out.println("        <span class=\"badge badge-overdue\">Overdue</span>");
            }
            out.println("      </div>");
            out.println("      <div class=\"todo-actions\">");

            // Tomorrow button
            out.println("        <form method=\"post\" action=\"todo\" style=\"display:inline;\">");
            out.println("          <input type=\"hidden\" name=\"" + PARAM_ACTION_ID + "\" value=\""
                    + action.getActionNextId() + "\" />");
            out.println("          <input type=\"hidden\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_TOMORROW
                    + "\" />");
            if (!dateParam.isEmpty()) {
                out.println(
                        "          <input type=\"hidden\" name=\"" + PARAM_DATE + "\" value=\"" + dateParam + "\" />");
            }
            out.println("          <button type=\"submit\" class=\"btn-small\">Tomorrow</button>");
            out.println("        </form>");

            // Edit link (placeholder)
            out.println("        <a href=\"#\" class=\"btn-small\">Edit</a>");
            out.println("      </div>");
            out.println("    </div>");
            out.println("  </li>");
        }
        out.println("</ul>");
    }

    private void printDateNavigation(PrintWriter out, Date selectedDate, Date today,
            boolean showPersonal, boolean showWork, WebUser webUser) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = webUser.getCalendar();

        boolean isToday = isSameDay(selectedDate, today, webUser);

        out.println("<div class=\"date-nav\">");

        // Previous (hidden when viewing today)
        if (!isToday) {
            cal.setTime(selectedDate);
            cal.add(Calendar.DAY_OF_MONTH, -1);
            String prevDate = sdf.format(cal.getTime());
            out.println("  <a href=\"todo?" + PARAM_DATE + "=" + prevDate + "\" class=\"btn\">Previous</a>");
        }

        // Today
        String todayDate = sdf.format(today);
        out.println("  <a href=\"todo?" + PARAM_DATE + "=" + todayDate + "\" class=\"btn\">Today</a>");

        // Next
        cal.setTime(selectedDate);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        String nextDate = sdf.format(cal.getTime());
        out.println("  <a href=\"todo?" + PARAM_DATE + "=" + nextDate + "\" class=\"btn\">Next</a>");

        out.println("</div>");
    }

    private void completeAction(ProjectActionNext action, Session dataSession, WebUser webUser) {
        Transaction trans = dataSession.beginTransaction();
        try {
            action.setNextActionStatus(ProjectNextActionStatus.COMPLETED);
            action.setNextChangeDate(new Date());
            dataSession.saveOrUpdate(action);
            trans.commit();
        } catch (Exception e) {
            trans.rollback();
            throw e;
        }
    }

    private void postponeToTomorrow(ProjectActionNext action, Session dataSession, WebUser webUser) {
        Transaction trans = dataSession.beginTransaction();
        try {
            Date tomorrow = TimeTracker.createTomorrow(webUser).getTime();
            action.setNextActionDate(tomorrow);
            action.setNextChangeDate(new Date());
            dataSession.saveOrUpdate(action);
            trans.commit();
        } catch (Exception e) {
            trans.rollback();
            throw e;
        }
    }

    private String buildRedirectUrl(HttpServletRequest request) {
        String dateParam = request.getParameter(PARAM_DATE);
        if (dateParam != null && !dateParam.isEmpty()) {
            return "todo?" + PARAM_DATE + "=" + dateParam;
        }
        return "todo";
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
