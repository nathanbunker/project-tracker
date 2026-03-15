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
    private static final String PARAM_TARGET_DATE = "targetDate";
    private static final String PARAM_TIME_SLOT = "timeSlotString";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ACTION_ID = "actionId";

    private static final String ACTION_COMPLETE = "complete";
    private static final String ACTION_CANCEL = "cancel";
    private static final String ACTION_TOMORROW = "tomorrow";
    private static final String ACTION_RESCHEDULE = "reschedule";

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
                        } else if (ACTION_CANCEL.equals(paramAction)) {
                            cancelAction(projectAction, dataSession, webUser);
                        } else if (ACTION_TOMORROW.equals(paramAction)) {
                            postponeToTomorrow(projectAction, dataSession, webUser);
                        } else if (ACTION_RESCHEDULE.equals(paramAction)) {
                            rescheduleAction(projectAction, request, webUser, dataSession);
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
                printActionList(out, overdueActions, true, selectedDate, webUser);
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
            printActionTable(out, "Wake", wakeActions, selectedDate, webUser);
            printActionTable(out, "Morning", morningActions, selectedDate, webUser);
            printActionTable(out, "Afternoon", afternoonActions, selectedDate, webUser);
            printActionTable(out, "Evening", eveningActions, selectedDate, webUser);

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
            boolean isOverdue, Date selectedDate, WebUser webUser) {
        if (actions.isEmpty()) {
            out.println("<table class=\"boxed-mobile\">");
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">No items</td>");
            out.println("  </tr>");
            out.println("</table>");
            return;
        }
        printActionTableContent(out, actions, isOverdue, selectedDate, webUser);
    }

    private void printActionTable(PrintWriter out, String title, List<ProjectActionNext> actions,
            Date selectedDate, WebUser webUser) {
        if (actions.isEmpty()) {
            return; // Don't show header or table if no items
        }
        out.println("<h2>" + escapeHtml(title) + "</h2>");
        printActionTableContent(out, actions, false, selectedDate, webUser);
    }

    private void printActionTableContent(PrintWriter out, List<ProjectActionNext> actions,
            boolean isOverdue, Date selectedDate, WebUser webUser) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat daySdf = new SimpleDateFormat("EEE");
        String dateParam = selectedDate != null ? sdf.format(selectedDate) : "";

        out.println("<script>");
        out.println("function showPostponeMenu(actionId){");
        out.println("  var menu = document.getElementById('postpone-menu-' + actionId);");
        out.println("  if (menu) { menu.style.display = 'block'; }");
        out.println("}");
        out.println("function hidePostponeMenu(actionId){");
        out.println("  var menu = document.getElementById('postpone-menu-' + actionId);");
        out.println("  if (menu) { menu.style.display = 'none'; }");
        out.println("}");
        out.println("</script>");

        out.println("<table class=\"boxed-mobile\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">To Do</th>");
        out.println("    <th class=\"boxed\" style=\"text-align:center;\">Action</th>");
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

            // Action column (complete + alternate time slots + postpone + edit)
            String completeUrl = "todo?" + PARAM_ACTION_ID + "=" + action.getActionNextId() + "&"
                    + PARAM_ACTION + "=" + ACTION_COMPLETE;
            if (!dateParam.isEmpty()) {
                completeUrl += "&" + PARAM_DATE + "=" + dateParam;
            }
            TimeSlot currentTimeSlot = action.getTimeSlot();
            if (currentTimeSlot == null) {
                currentTimeSlot = TimeSlot.AFTERNOON;
            }
            String cancelUrl = "todo?" + PARAM_ACTION_ID + "=" + action.getActionNextId() + "&"
                    + PARAM_ACTION + "=" + ACTION_CANCEL;
            if (!dateParam.isEmpty()) {
                cancelUrl += "&" + PARAM_DATE + "=" + dateParam;
            }
            out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
            out.println("      <span style=\"white-space: nowrap;\">");
            out.println("        <a href=\"" + completeUrl
                    + "\" class=\"action-icon\" title=\"Complete\" style=\"margin-right: 8px;\">&#10004;</a>");
            out.println("        <a href=\"javascript:void(0);\" onclick=\"showPostponeMenu(" + action.getActionNextId()
                    + "); return false;\" class=\"action-icon\" title=\"Postpone/Reschedule\" style=\"margin-right: 8px;\">&#8594;</a>");
            out.println("        <a href=\"" + cancelUrl
                    + "\" class=\"action-icon\" title=\"Cancel\" style=\"margin-right: 8px;\">&#10006;</a>");
            out.println("      </span>");

            String actionDay = action.getNextActionDate() == null ? sdf.format(webUser.getToday())
                    : sdf.format(action.getNextActionDate());
            String currentSlotId = currentTimeSlot.getId();
                String popupTitle = description == null ? "" : description;
            if (!projectLabel.isEmpty()) {
                popupTitle = escapeHtml(projectLabel) + " " + popupTitle;
            }
            out.println("      <div id=\"postpone-menu-" + action.getActionNextId()
                    + "\" style=\"display:none; position:fixed; left:10px; right:10px; top:20%; z-index:9999; background:#fff; border:1px solid #666; padding:10px; text-align:left;\">");
            out.println(
                    "        <div style=\"margin-bottom: 8px;\"><strong>Reschedule: " + popupTitle
                            + "</strong> <a href=\"javascript:void(0);\" onclick=\"hidePostponeMenu("
                            + action.getActionNextId()
                            + "); return false;\" style=\"float:right; text-decoration:none;\">&#10005;</a></div>");
            out.println("        <div style=\"margin-bottom: 8px;\">\n");
            printPostponeMenuSlotOption(out, action.getActionNextId(), dateParam, actionDay,
                    TimeSlot.WAKE, "&#9200; Wake", currentSlotId);
            printPostponeMenuSlotOption(out, action.getActionNextId(), dateParam, actionDay,
                    TimeSlot.MORNING, "&#127749; Morning", currentSlotId);
            printPostponeMenuSlotOption(out, action.getActionNextId(), dateParam, actionDay,
                    TimeSlot.AFTERNOON, "&#127774; Afternoon", currentSlotId);
            printPostponeMenuSlotOption(out, action.getActionNextId(), dateParam, actionDay,
                    TimeSlot.EVENING, "&#127769; Evening", currentSlotId);
            out.println("        </div>");
            out.println("        <div>");
            Calendar dayCal = webUser.getCalendar();
            dayCal.setTime(webUser.getToday());
            printPostponeMenuDayOption(out, action.getActionNextId(), dateParam, currentSlotId,
                    actionDay,
                    sdf.format(dayCal.getTime()), "Today");
            for (int i = 0; i < 7; i++) {
                dayCal.add(Calendar.DAY_OF_MONTH, 1);
                printPostponeMenuDayOption(out, action.getActionNextId(), dateParam, currentSlotId,
                        actionDay,
                        sdf.format(dayCal.getTime()), daySdf.format(dayCal.getTime()));
            }
            out.println("        </div>");
            out.println("      </div>");
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

    private void rescheduleAction(ProjectActionNext action, HttpServletRequest request,
            WebUser webUser, Session dataSession) {
        Transaction trans = dataSession.beginTransaction();
        try {
            String targetDateString = request.getParameter(PARAM_TARGET_DATE);
            Date targetDate = action.getNextActionDate();
            if (targetDateString != null && !targetDateString.isEmpty()) {
                try {
                    targetDate = new SimpleDateFormat("yyyy-MM-dd").parse(targetDateString);
                } catch (Exception e) {
                    targetDate = action.getNextActionDate();
                }
            }
            if (targetDate == null) {
                targetDate = webUser.getToday();
            }

            String slotString = request.getParameter(PARAM_TIME_SLOT);
            TimeSlot slot = TimeSlot.getTimeSlot(slotString);
            if (slot == null) {
                slot = action.getTimeSlot() == null ? TimeSlot.AFTERNOON : action.getTimeSlot();
            }

            action.setNextActionDate(targetDate);
            action.setTimeSlot(slot);
            action.setNextChangeDate(new Date());
            dataSession.saveOrUpdate(action);
            trans.commit();
        } catch (Exception e) {
            trans.rollback();
            throw e;
        }
    }

    private void cancelAction(ProjectActionNext action, Session dataSession, WebUser webUser) {
        Transaction trans = dataSession.beginTransaction();
        try {
            action.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
            action.setNextChangeDate(new Date());
            dataSession.saveOrUpdate(action);
            ProjectActionBlockerManager.unblockActionsBlockedBy(dataSession, webUser, action);
            trans.commit();
        } catch (Exception e) {
            trans.rollback();
            throw e;
        }
    }

    private void printPostponeMenuSlotOption(PrintWriter out, int actionNextId,
            String dateParam, String targetDate, TimeSlot slot, String label,
            String currentSlotId) {
        boolean isCurrent = slot.getId().equals(currentSlotId);
        String style = "display:inline-block; margin-right:6px; margin-bottom:6px; padding:4px 6px; text-decoration:none;";
        if (isCurrent) {
            out.println("          <span style=\"" + style + " border:1px solid #666;\">" + label + "</span>");
            return;
        }
        String url = "todo?" + PARAM_ACTION_ID + "=" + actionNextId
                + "&" + PARAM_ACTION + "=" + ACTION_RESCHEDULE
                + "&" + PARAM_TARGET_DATE + "=" + targetDate
                + "&" + PARAM_TIME_SLOT + "=" + slot.getId();
        if (!dateParam.isEmpty()) {
            url += "&" + PARAM_DATE + "=" + dateParam;
        }
        out.println("          <a href=\"" + url + "\" style=\"" + style + " color: inherit;\">"
                + label + "</a>");
    }

    private void printPostponeMenuDayOption(PrintWriter out, int actionNextId,
            String dateParam, String slotId, String currentDate, String targetDate, String label) {
        boolean isCurrent = targetDate.equals(currentDate);
        String style = "display:inline-block; margin-right:6px; margin-bottom:6px; padding:4px 6px; text-decoration:none;";
        if (isCurrent) {
            out.println("          <span style=\"" + style + " border:1px solid #666;\">" + label + "</span>");
            return;
        }
        String url = "todo?" + PARAM_ACTION_ID + "=" + actionNextId
                + "&" + PARAM_ACTION + "=" + ACTION_RESCHEDULE
                + "&" + PARAM_TARGET_DATE + "=" + targetDate
                + "&" + PARAM_TIME_SLOT + "=" + slotId;
        if (!dateParam.isEmpty()) {
            url += "&" + PARAM_DATE + "=" + dateParam;
        }
        out.println("          <a href=\"" + url + "\" style=\"" + style + " color: inherit;\">"
                + label + "</a>");
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
