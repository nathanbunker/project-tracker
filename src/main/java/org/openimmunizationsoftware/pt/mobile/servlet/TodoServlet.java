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
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.TimeSlot;
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
    private static final String PARAM_VIEW_ACTION_ID = "viewActionId";
    private static final String PARAM_NEXT_NOTE = "nextNote";

    private static final String ACTION_COMPLETE = "complete";
    private static final String ACTION_TOMORROW = "tomorrow";
    private static final String LIST_START = " - ";

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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            WebUser webUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();

            // Check if we're viewing a detail page
            String viewActionIdStr = request.getParameter(PARAM_VIEW_ACTION_ID);
            if (viewActionIdStr != null && !viewActionIdStr.isEmpty()) {
                try {
                    int viewActionId = Integer.parseInt(viewActionIdStr);
                    ProjectActionNext projectAction = (ProjectActionNext) dataSession.get(
                            ProjectActionNext.class, viewActionId);

                    if (projectAction != null) {
                        // Handle adding a note if submitted
                        String nextNote = request.getParameter(PARAM_NEXT_NOTE);
                        if (nextNote != null && nextNote.trim().length() > 0) {
                            Transaction trans = dataSession.beginTransaction();
                            try {
                                String updatedNotes = nextNote;
                                if (projectAction.getNextNotes() != null
                                        && projectAction.getNextNotes().trim().length() > 0) {
                                    updatedNotes = projectAction.getNextNotes() + "\n - " + nextNote;
                                } else {
                                    updatedNotes = LIST_START + nextNote;
                                }
                                projectAction.setNextNotes(updatedNotes);
                                projectAction.setNextChangeDate(new Date());
                                dataSession.saveOrUpdate(projectAction);
                                trans.commit();
                            } catch (Exception e) {
                                trans.rollback();
                                throw e;
                            }
                        }

                        // Handle action processing (Complete/Postpone)
                        String paramAction = request.getParameter(PARAM_ACTION);
                        if (paramAction != null) {
                            try {
                                if (ACTION_COMPLETE.equals(paramAction)) {
                                    completeAction(projectAction, dataSession, webUser);
                                } else if (ACTION_TOMORROW.equals(paramAction)) {
                                    postponeToTomorrow(projectAction, dataSession, webUser);
                                }
                                // Redirect back to todo main page
                                String redirectUrl = "todo";
                                String dateParam = request.getParameter(PARAM_DATE);
                                if (dateParam != null && !dateParam.isEmpty()) {
                                    redirectUrl += "?" + PARAM_DATE + "=" + dateParam;
                                }
                                response.sendRedirect(redirectUrl);
                                return;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Display detail page
                    appReq.setTitle("Todo Details");
                    printHtmlHead(appReq, "Todo Details");
                    printActionDetail(appReq, projectAction, webUser);
                    printHtmlFoot(appReq);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Handle action processing (Complete/Tomorrow) - works for both GET and POST
            String paramAction = request.getParameter(PARAM_ACTION);
            String actionIdStr = request.getParameter(PARAM_ACTION_ID);

            if (paramAction != null && actionIdStr != null) {
                try {
                    int actionId = Integer.parseInt(actionIdStr);
                    ProjectActionNext projectAction = (ProjectActionNext) dataSession.get(
                            ProjectActionNext.class, actionId);

                    if (projectAction != null) {
                        if (ACTION_COMPLETE.equals(paramAction)) {
                            completeAction(projectAction, dataSession, webUser);
                        } else if (ACTION_TOMORROW.equals(paramAction)) {
                            postponeToTomorrow(projectAction, dataSession, webUser);
                        }
                    }

                    // Redirect back to the todo page with the same date
                    String redirectUrl = buildRedirectUrl(request);
                    response.sendRedirect(redirectUrl);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

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

            // Calculate boundaries for selected day
            Calendar cal = webUser.getCalendar();
            cal.setTime(selectedDate);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date startOfSelectedDay = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            Date endOfSelectedDay = cal.getTime();

            for (ProjectActionNext action : allActions) {
                if (!shouldShow(action, showPersonal, showWork)) {
                    continue;
                }

                if (isToday) {
                    // Today: show overdue (before today) and today's items
                    if (action.getNextActionDate() != null && action.getNextActionDate().before(today)) {
                        overdueActions.add(action);
                    } else {
                        todayActions.add(action);
                    }
                } else {
                    // Other days: only show items for that specific day
                    if (action.getNextActionDate() != null
                            && action.getNextActionDate().compareTo(startOfSelectedDay) >= 0
                            && action.getNextActionDate().before(endOfSelectedDay)) {
                        todayActions.add(action);
                    }
                }
            }

            // Count for filter labels
            int personalCount = countPersonal(allActions);
            int workCount = countWork(allActions);

            // Render page
            appReq.setTitle("Todo");
            printHtmlHead(appReq, "Todo");
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

            // Organize today's actions into categories
            List<ProjectActionNext> wakeActions = new ArrayList<>();
            List<ProjectActionNext> morningActions = new ArrayList<>();
            List<ProjectActionNext> workActions = new ArrayList<>();
            List<ProjectActionNext> afternoonActions = new ArrayList<>();
            List<ProjectActionNext> eveningActions = new ArrayList<>();

            for (ProjectActionNext action : todayActions) {
                if (action.isBillable() && showWork) {
                    workActions.add(action);
                } else if (!action.isBillable() && showPersonal) {
                    TimeSlot ts = action.getTimeSlot();
                    if (ts == TimeSlot.WAKE) {
                        wakeActions.add(action);
                    } else if (ts == TimeSlot.MORNING) {
                        morningActions.add(action);
                    } else if (ts == TimeSlot.AFTERNOON) {
                        afternoonActions.add(action);
                    } else if (ts == TimeSlot.EVENING) {
                        eveningActions.add(action);
                    } else {
                        // Time slot is null or no category - add to afternoon as default
                        afternoonActions.add(action);
                    }
                }
            }

            // Print tables for each category (only if there are items)
            if (showPersonal) {
                printActionTable(out, "Wake", wakeActions, selectedDate);
                printActionTable(out, "Morning", morningActions, selectedDate);
                printActionTable(out, "Afternoon", afternoonActions, selectedDate);
                printActionTable(out, "Evening", eveningActions, selectedDate);
            }
            if (showWork) {
                printActionTable(out, "Work", workActions, selectedDate);
            }

            // Date navigation
            printDateNavigation(out, selectedDate, today, webUser);

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
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

    private void printFilters(PrintWriter out, boolean showPersonal, boolean showWork,
            int personalCount, int workCount, Date selectedDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateParam = selectedDate != null ? sdf.format(selectedDate) : "";

        out.println("<form method=\"get\" action=\"todo\">");
        out.println("  <input type=\"hidden\" name=\"" + PARAM_FILTER_SUBMITTED + "\" value=\"Y\"/>");
        if (!dateParam.isEmpty()) {
            out.println("  <input type=\"hidden\" name=\"" + PARAM_DATE + "\" value=\"" + dateParam + "\" />");
        }
        out.println("  <p>");
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
        out.println("  </p>");
        out.println("</form>");
    }

    private void printActionList(PrintWriter out, List<ProjectActionNext> actions,
            boolean isOverdue, Date selectedDate) {
        if (actions.isEmpty()) {
            out.println("<table class=\"boxed-mobile\">");
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">No items</td>");
            out.println("  </tr>");
            out.println("</table>");
            return;
        }
        printActionTableContent(out, actions, isOverdue, selectedDate);
    }

    private void printActionTable(PrintWriter out, String title, List<ProjectActionNext> actions,
            Date selectedDate) {
        if (actions.isEmpty()) {
            return; // Don't show header or table if no items
        }
        out.println("<h2>" + escapeHtml(title) + "</h2>");
        printActionTableContent(out, actions, false, selectedDate);
    }

    private void printActionTableContent(PrintWriter out, List<ProjectActionNext> actions,
            boolean isOverdue, Date selectedDate) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateParam = selectedDate != null ? sdf.format(selectedDate) : "";

        out.println("<table class=\"boxed-mobile\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">To Do</th>");
        out.println("    <th class=\"boxed\" style=\"text-align:center;\">Complete</th>");
        out.println("    <th class=\"boxed\" style=\"text-align:center;\">Postpone</th>");
        out.println("    <th class=\"boxed\" style=\"text-align:center;\">Details</th>");
        out.println("  </tr>");
        for (ProjectActionNext action : actions) {
            String projectName = action.getProject() != null ? action.getProject().getProjectName() : "";
            String description = action.getNextDescriptionForDisplay(action.getContact());

            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">");
            if (!projectName.isEmpty()) {
                out.println("      <strong>" + escapeHtml(projectName) + ":</strong> ");
            }
            out.println("      " + (description == null ? "" : description));
            if (isOverdue) {
                out.println("      <span class=\"fail\">Overdue</span>");
            }
            out.println("    </td>");

            // Complete column
            String completeUrl = "todo?" + PARAM_ACTION_ID + "=" + action.getActionNextId() + "&"
                    + PARAM_ACTION + "=" + ACTION_COMPLETE;
            if (!dateParam.isEmpty()) {
                completeUrl += "&" + PARAM_DATE + "=" + dateParam;
            }
            out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
            out.println("      <a href=\"" + completeUrl + "\" class=\"action-icon\" title=\"Complete\">&#10004;</a>");
            out.println("    </td>");

            // Postpone column
            String tomorrowUrl = "todo?" + PARAM_ACTION_ID + "=" + action.getActionNextId() + "&"
                    + PARAM_ACTION + "=" + ACTION_TOMORROW;
            if (!dateParam.isEmpty()) {
                tomorrowUrl += "&" + PARAM_DATE + "=" + dateParam;
            }
            out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
            out.println("      <a href=\"" + tomorrowUrl + "\" class=\"action-icon\" title=\"Postpone\">&#8594;</a>");
            out.println("    </td>");

            // View Details column
            String viewUrl = "todo?" + PARAM_VIEW_ACTION_ID + "=" + action.getActionNextId();
            if (!dateParam.isEmpty()) {
                viewUrl += "&" + PARAM_DATE + "=" + dateParam;
            }
            out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
            out.println("      <a href=\"" + viewUrl + "\" class=\"action-icon\" title=\"Details\">&#8505;</a>");
            out.println("    </td>");
            out.println("  </tr>");
        }
        out.println("</table>");
    }

    private void printDateNavigation(PrintWriter out, Date selectedDate, Date today,
            WebUser webUser) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = webUser.getCalendar();

        boolean isToday = isSameDay(selectedDate, today, webUser);

        out.println("<p>");

        // Previous (hidden when viewing today)
        if (!isToday) {
            cal.setTime(selectedDate);
            cal.add(Calendar.DAY_OF_MONTH, -1);
            String prevDate = sdf.format(cal.getTime());
            out.println("  <a href=\"todo?" + PARAM_DATE + "=" + prevDate + "\" class=\"box\">Previous</a>");
        }

        // Today
        String todayDate = sdf.format(today);
        out.println("  <a href=\"todo?" + PARAM_DATE + "=" + todayDate + "\" class=\"button\">Today</a>");

        // Next
        cal.setTime(selectedDate);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        String nextDate = sdf.format(cal.getTime());
        out.println("  <a href=\"todo?" + PARAM_DATE + "=" + nextDate + "\" class=\"box\">Next</a>");

        out.println("</p>");
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

    private void printActionDetail(AppReq appReq, ProjectActionNext action, WebUser webUser) {
        PrintWriter out = appReq.getOut();

        if (action == null) {
            out.println("<p>Action not found</p>");
            return;
        }

        Date today = TimeTracker.createToday(webUser).getTime();
        String title = formatTitle(action.getNextActionDate(), today, webUser);
        out.println("<h1>" + title + "</h1>");

        // Description
        out.println("<h2>Description</h2>");
        String projectName = action.getProject() != null ? action.getProject().getProjectName() : "";
        String description = action.getNextDescriptionForDisplay(action.getContact());
        if (!projectName.isEmpty()) {
            out.println("<strong>" + escapeHtml(projectName) + ":</strong> ");
        }
        out.println("<p>" + (description == null ? "" : description) + "</p>");

        // Link if any
        if (action.getLinkUrl() != null && !action.getLinkUrl().isEmpty()) {
            out.println("<p><a href=\"" + escapeHtml(action.getLinkUrl()) + "\" target=\"_blank\">Link</a></p>");
        }

        // Notes if any
        if (action.getNextNotes() != null && !action.getNextNotes().trim().isEmpty()) {
            out.println("<h2>Notes</h2>");
            out.println(convertToHtmlList(action.getNextNotes()));
        }

        // Add notes form
        out.println("<h2>Add Note</h2>");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateParam = action.getNextActionDate() != null ? sdf.format(action.getNextActionDate()) : "";

        out.println("<form method=\"post\" action=\"todo\">");
        out.println("  <input type=\"hidden\" name=\"" + PARAM_VIEW_ACTION_ID + "\" value=\"" + action.getActionNextId()
                + "\" />");
        if (!dateParam.isEmpty()) {
            out.println("  <input type=\"hidden\" name=\"" + PARAM_DATE + "\" value=\"" + dateParam + "\" />");
        }
        out.println("  <textarea name=\"" + PARAM_NEXT_NOTE + "\" rows=\"5\" style=\"width:100%;\"></textarea>");
        out.println("  <br/>");
        out.println("  <input type=\"submit\" value=\"Add Note\" />");
        out.println("</form>");

        // Action buttons in small table
        out.println("<h2>Actions</h2>");
        out.println("<table class=\"boxed-mobile\">");
        out.println("  <tr class=\"boxed\">");

        // Complete
        String completeUrl = "todo?" + PARAM_VIEW_ACTION_ID + "=" + action.getActionNextId() + "&" +
                PARAM_ACTION + "=" + ACTION_COMPLETE;
        if (!dateParam.isEmpty()) {
            completeUrl += "&" + PARAM_DATE + "=" + dateParam;
        }
        out.println("    <td style=\"text-align:center; padding:10px;\">");
        out.println(
                "      <a href=\"" + completeUrl + "\" class=\"action-icon\" title=\"Complete\">&#10004; Complete</a>");
        out.println("    </td>");

        // Postpone
        String postponeUrl = "todo?" + PARAM_VIEW_ACTION_ID + "=" + action.getActionNextId() + "&" +
                PARAM_ACTION + "=" + ACTION_TOMORROW;
        if (!dateParam.isEmpty()) {
            postponeUrl += "&" + PARAM_DATE + "=" + dateParam;
        }
        out.println("    <td style=\"text-align:center; padding:10px;\">");
        out.println(
                "      <a href=\"" + postponeUrl + "\" class=\"action-icon\" title=\"Postpone\">&#8594; Postpone</a>");
        out.println("    </td>");

        // Edit
        out.println("    <td style=\"text-align:center; padding:10px;\">");
        out.println("      <a href=\"action?actionNextId=" + action.getActionNextId()
                + "\" class=\"action-icon\" title=\"Edit\">&#9998; Edit</a>");
        out.println("    </td>");

        out.println("  </tr>");
        out.println("</table>");
    }

    private static String convertToHtmlList(String input) {
        // Use StringBuilder for efficient string manipulation
        StringBuilder html = new StringBuilder("<ul>\n");

        // Split the input string by new lines
        String[] lines = input.split("\\r?\\n");

        for (String line : lines) {
            // Check if the line starts with " - " and trim whitespace
            if (line.startsWith(" - ")) {
                // Extract the text after " - " and wrap it in <li> tags
                html.append("  <li>").append(escapeHtmlStatic(line.substring(3).trim())).append("</li>\n");
            }
        }

        html.append("</ul>");
        return html.toString();
    }

    private static String escapeHtmlStatic(String text) {
        // Escape HTML special characters to prevent XSS
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
