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
import org.openimmunizationsoftware.pt.manager.ProjectActionBlockerManager;
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

    private static final String ACTION_COMPLETE = "complete";
    private static final String ACTION_TOMORROW = "tomorrow";
    private static final String ACTION_SLOT_WAKE = "slotWake";
    private static final String ACTION_SLOT_MORNING = "slotMorning";
    private static final String ACTION_SLOT_AFTERNOON = "slotAfternoon";
    private static final String ACTION_SLOT_EVENING = "slotEvening";

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
            if (appReq.isLoggedOut()) {
                response.sendRedirect("../LoginServlet?uiMode=mobile");
                return;
            }

            WebUser webUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();

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
                        } else if (ACTION_SLOT_WAKE.equals(paramAction)) {
                            updateTimeSlot(projectAction, dataSession, TimeSlot.WAKE);
                        } else if (ACTION_SLOT_MORNING.equals(paramAction)) {
                            updateTimeSlot(projectAction, dataSession, TimeSlot.MORNING);
                        } else if (ACTION_SLOT_AFTERNOON.equals(paramAction)) {
                            updateTimeSlot(projectAction, dataSession, TimeSlot.AFTERNOON);
                        } else if (ACTION_SLOT_EVENING.equals(paramAction)) {
                            updateTimeSlot(projectAction, dataSession, TimeSlot.EVENING);
                        }
                    }

                    // Redirect back to the todo page with the same date
                    String redirectUrl = buildRedirectUrl(request);
                    response.sendRedirect(redirectUrl);
                    return;
                } catch (Exception e) {
                    handleUnexpectedError(request, response, e);
                    return;
                }
            }

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
                if (action.isBillable()) {
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

            // Render page
            appReq.setTitle("Todo");
            printHtmlHead(appReq, "Todo");
            PrintWriter out = appReq.getOut();

            String title = formatTitle(selectedDate, today, webUser);
            out.println("<h1>" + title + "</h1>");

            // Overdue section (only on today)
            if (isToday && !overdueActions.isEmpty()) {
                out.println("<h2>Overdue</h2>");
                printActionList(out, overdueActions, true, selectedDate);
            }

            // Organize today's actions into categories
            List<ProjectActionNext> wakeActions = new ArrayList<>();
            List<ProjectActionNext> morningActions = new ArrayList<>();
            List<ProjectActionNext> afternoonActions = new ArrayList<>();
            List<ProjectActionNext> eveningActions = new ArrayList<>();

            for (ProjectActionNext action : todayActions) {
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

            // Print tables for each category (only if there are items)
            printActionTable(out, "Wake", wakeActions, selectedDate);
            printActionTable(out, "Morning", morningActions, selectedDate);
            printActionTable(out, "Afternoon", afternoonActions, selectedDate);
            printActionTable(out, "Evening", eveningActions, selectedDate);

            // Date navigation
            printDateNavigation(out, selectedDate, today, webUser);

            printHtmlFoot(appReq);
        } catch (Exception e) {
            handleUnexpectedError(request, response, e);
        } finally {
            appReq.close();
        }
    }

    private void handleUnexpectedError(HttpServletRequest request, HttpServletResponse response, Exception e)
            throws IOException {
        e.printStackTrace();
        if (!response.isCommitted()) {
            response.sendRedirect("oops");
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
        out.println("    <th class=\"boxed\" style=\"text-align:center;\">Action</th>");
        out.println("    <th class=\"boxed\" style=\"text-align:center;\">Edit</th>");
        out.println("  </tr>");
        for (ProjectActionNext action : actions) {
            String projectName = action.getProject() != null ? action.getProject().getProjectName() : "";
            String projectIcon = action.getProject() != null ? action.getProject().getProjectIcon() : "";
            boolean hasProjectIcon = projectIcon != null && projectIcon.trim().length() > 0;
            String projectLabel = hasProjectIcon ? projectIcon.trim() : projectName;
            String projectLabelSuffix = hasProjectIcon ? "" : ":";
            String description = action.getNextDescriptionForDisplay(action.getContact());
            Integer projectId = action.getProjectId();

            // Build details link
            String viewUrl = "action?viewActionId=" + action.getActionNextId();
            if (!dateParam.isEmpty()) {
                viewUrl += "&" + PARAM_DATE + "=" + dateParam;
            }

            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">");
            if (!projectLabel.isEmpty()) {
                if (projectId != null && projectId.intValue() > 0) {
                    out.println("      <strong><a href=\"project?projectId=" + projectId
                            + "\" style=\"text-decoration: none;\">" + escapeHtml(projectLabel)
                            + "</a>" + projectLabelSuffix + "</strong> ");
                } else {
                    out.println("      <strong>" + escapeHtml(projectLabel) + projectLabelSuffix + "</strong> ");
                }
            }
            out.println("      <a href=\"" + viewUrl + "\" style=\"text-decoration: none; color: inherit;\">");
            out.println("        " + (description == null ? "" : description));
            out.println("      </a>");
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

            // Action column (3 alternate time slots + postpone)
            TimeSlot currentTimeSlot = action.getTimeSlot();
            if (currentTimeSlot == null) {
                currentTimeSlot = TimeSlot.AFTERNOON;
            }
            String tomorrowUrl = "todo?" + PARAM_ACTION_ID + "=" + action.getActionNextId() + "&"
                    + PARAM_ACTION + "=" + ACTION_TOMORROW;
            if (!dateParam.isEmpty()) {
                tomorrowUrl += "&" + PARAM_DATE + "=" + dateParam;
            }
            out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
            out.println("      <span style=\"white-space: nowrap;\">");
            printTimeSlotActionLink(out, action.getActionNextId(), dateParam, TimeSlot.WAKE,
                    ACTION_SLOT_WAKE, "&#9200;", "Wake", currentTimeSlot);
            printTimeSlotActionLink(out, action.getActionNextId(), dateParam, TimeSlot.MORNING,
                    ACTION_SLOT_MORNING, "&#127749;", "Morning", currentTimeSlot);
            printTimeSlotActionLink(out, action.getActionNextId(), dateParam, TimeSlot.AFTERNOON,
                    ACTION_SLOT_AFTERNOON, "&#127774;", "Afternoon", currentTimeSlot);
            printTimeSlotActionLink(out, action.getActionNextId(), dateParam, TimeSlot.EVENING,
                    ACTION_SLOT_EVENING, "&#127769;", "Evening", currentTimeSlot);
            out.println("        <a href=\"" + tomorrowUrl
                    + "\" class=\"action-icon\" title=\"Postpone\" style=\"margin-right: 8px;\">&#8594;</a>");
            out.println("      </span>");
            out.println("    </td>");

            // Edit column
            out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
            out.println("      <a href=\"action?actionNextId=" + action.getActionNextId()
                    + "\" class=\"action-icon\" title=\"Edit\">&#9998;</a>");
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
            ProjectActionBlockerManager.unblockActionsBlockedBy(dataSession, webUser, action);
            trans.commit();
        } catch (Exception e) {
            trans.rollback();
            throw e;
        }
    }

    private void postponeToTomorrow(ProjectActionNext action, Session dataSession, WebUser webUser) {
        Transaction trans = dataSession.beginTransaction();
        try {
            Date today = webUser.getToday();
            Date tomorrow = webUser.getTomorrow();
            Date nextActionDate = action.getNextActionDate();

            if (nextActionDate != null && nextActionDate.before(today)) {
                action.setNextActionDate(today);
            } else {
                action.setNextActionDate(tomorrow);
            }
            action.setNextChangeDate(new Date());
            dataSession.saveOrUpdate(action);
            trans.commit();
        } catch (Exception e) {
            trans.rollback();
            throw e;
        }
    }

    private void updateTimeSlot(ProjectActionNext action, Session dataSession, TimeSlot timeSlot) {
        Transaction trans = dataSession.beginTransaction();
        try {
            action.setTimeSlot(timeSlot);
            action.setNextChangeDate(new Date());
            dataSession.saveOrUpdate(action);
            trans.commit();
        } catch (Exception e) {
            trans.rollback();
            throw e;
        }
    }

    private void printTimeSlotActionLink(PrintWriter out, int actionNextId, String dateParam,
            TimeSlot targetSlot, String targetAction, String iconEntity, String title,
            TimeSlot currentTimeSlot) {
        if (targetSlot == currentTimeSlot) {
            return;
        }
        String slotUrl = "todo?" + PARAM_ACTION_ID + "=" + actionNextId + "&"
                + PARAM_ACTION + "=" + targetAction;
        if (!dateParam.isEmpty()) {
            slotUrl += "&" + PARAM_DATE + "=" + dateParam;
        }
        out.println("        <a href=\"" + slotUrl
                + "\" class=\"action-icon\" title=\"" + title + "\" style=\"margin-right: 8px;\">"
                + iconEntity + "</a>");
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
