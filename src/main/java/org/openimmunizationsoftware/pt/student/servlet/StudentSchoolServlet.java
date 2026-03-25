package org.openimmunizationsoftware.pt.student.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
import org.openimmunizationsoftware.pt.model.GamePointLedger;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

public class StudentSchoolServlet extends StudentBaseServlet {

    private static final String PARAM_DATE = "date";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ACTION_ID = "actionId";

    private static final String ACTION_COMPLETE = "complete";

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
                response.sendRedirect("../LoginServlet");
                return;
            }

            WebUser webUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();
            Date selectedDate = getSelectedDate(request, webUser, PARAM_DATE);

            String action = request.getParameter(PARAM_ACTION);
            String actionIdString = request.getParameter(PARAM_ACTION_ID);
            if (ACTION_COMPLETE.equals(action) && actionIdString != null && !actionIdString.trim().equals("")) {
                completeStudentAction(actionIdString, selectedDate, webUser, dataSession);
                response.sendRedirect("school?" + PARAM_DATE + "=" + toUserDateKey(selectedDate, webUser));
                return;
            }

            List<ProjectActionNext> actions = fetchDayActions(selectedDate, webUser, dataSession);
            List<ProjectActionNext> schoolActions = new ArrayList<ProjectActionNext>();
            List<ProjectActionNext> choreActions = new ArrayList<ProjectActionNext>();
            for (ProjectActionNext actionRow : actions) {
                if (actionRow.isBillable()) {
                    schoolActions.add(actionRow);
                } else {
                    choreActions.add(actionRow);
                }
            }

            int selectedDayEarnedPoints = getSelectedDayEarnedPoints(selectedDate, webUser, dataSession);

            appReq.setTitle("Student School");
            printHtmlHead(appReq, "School");
            PrintWriter out = appReq.getOut();

            out.println("<h1>Today</h1>");
            out.println("<p><strong>Viewing:</strong> "
                    + webUser.getDateFormatService().formatDate(selectedDate, webUser.getTimeZone()) + "</p>");

            if (!schoolActions.isEmpty()) {
                out.println("<h2>School</h2>");
                printActionTable(out, schoolActions, selectedDate, webUser, true);
            }
            if (!choreActions.isEmpty()) {
                out.println("<h2>Chores</h2>");
                printActionTable(out, choreActions, selectedDate, webUser, false);
            }
            if (schoolActions.isEmpty() && choreActions.isEmpty()) {
                out.println("<table class=\"boxed-mobile\">");
                out.println("  <tr class=\"boxed\"><td class=\"boxed\">No tasks for this day.</td></tr>");
                out.println("</table>");
            }

            printDateNavigation(out, selectedDate, webUser);

            out.println(
                    "<div style=\"position:fixed; left:0; right:0; bottom:0; background:#2B3E42; color:#fff; padding:10px 14px; font-weight:bold; z-index:1000;\">");
            out.println("  Points Earned For This Day: " + selectedDayEarnedPoints);
            out.println("</div>");

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
            if (!response.isCommitted()) {
                response.sendRedirect("../HomeServlet");
            }
        } finally {
            appReq.close();
        }
    }

    private void completeStudentAction(String actionIdString, Date selectedDate, WebUser webUser,
            Session dataSession) {
        int actionId = Integer.parseInt(actionIdString);
        ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionId);
        if (action == null) {
            return;
        }

        if (action.getProvider() == null || webUser.getProvider() == null
                || !webUser.getProvider().getProviderId().equals(action.getProvider().getProviderId())) {
            return;
        }

        String selectedDateKey = toDatabaseDateKey(selectedDate);
        String actionDateKey = toDatabaseDateKey(action.getNextActionDate());
        if (!selectedDateKey.equals(actionDateKey)) {
            return;
        }

        ProjectNextActionStatus currentStatus = action.getNextActionStatus();
        if (currentStatus == ProjectNextActionStatus.COMPLETED || currentStatus == ProjectNextActionStatus.CANCELLED) {
            return;
        }

        Transaction trans = dataSession.beginTransaction();
        try {
            action.setNextActionStatus(ProjectNextActionStatus.COMPLETED);
            action.setNextChangeDate(new Date());
            dataSession.saveOrUpdate(action);
            ProjectActionBlockerManager.unblockActionsBlockedBy(dataSession, webUser, action);

            int points = getAvailablePoints(action);
            if (points != 0) {
                ProjectContact contact = action.getContact();
                if (contact == null) {
                    contact = (ProjectContact) dataSession.get(ProjectContact.class, webUser.getContactId());
                }
                GamePointLedger ledger = new GamePointLedger();
                ledger.setContact(contact);
                ledger.setProjectActionNext(action);
                ledger.setPointChange(points);
                ledger.setEntryType("COMPLETE");
                ledger.setEntryNote(action.getNextDescription());
                ledger.setCreatedDate(new Date());
                ledger.setCreatedBy(webUser.getUsername());
                dataSession.save(ledger);
            }

            trans.commit();
        } catch (Exception e) {
            trans.rollback();
            throw e;
        }
    }

    private List<ProjectActionNext> fetchDayActions(Date selectedDate, WebUser webUser, Session dataSession) {
        String selectedDateKey = toDatabaseDateKey(selectedDate);
        java.sql.Date selectedSqlDate = java.sql.Date.valueOf(selectedDateKey);

        Query query = dataSession.createQuery(
                "select distinct pan from ProjectActionNext pan " +
                        "left join fetch pan.project " +
                        "left join fetch pan.contact " +
                        "where pan.provider = :provider " +
                        "and (pan.contactId = :contactId or pan.nextContactId = :contactId) " +
                        "and pan.nextDescription <> '' " +
                        "and pan.nextActionDate = :selectedDate " +
                        "and pan.nextActionStatusString in (:readyStatus, :completedStatus, :cancelledStatus) " +
                        "order by pan.billable desc, pan.priorityLevel DESC, pan.nextChangeDate");

        query.setParameter("provider", webUser.getProvider());
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("selectedDate", selectedSqlDate);
        query.setParameter("readyStatus", ProjectNextActionStatus.READY.getId());
        query.setParameter("completedStatus", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("cancelledStatus", ProjectNextActionStatus.CANCELLED.getId());

        @SuppressWarnings("unchecked")
        List<ProjectActionNext> results = query.list();

        for (ProjectActionNext action : results) {
            if (action.getProject() == null && action.getProjectId() > 0) {
                action.setProject((Project) dataSession.get(Project.class, action.getProjectId()));
            }
            if (action.getContact() == null && action.getContactId() > 0) {
                action.setContact((ProjectContact) dataSession.get(ProjectContact.class, action.getContactId()));
            }
        }

        return results;
    }

    private int getSelectedDayEarnedPoints(Date selectedDate, WebUser webUser, Session dataSession) {
        Date dayStart = webUser.startOfDay(selectedDate);
        Date dayEnd = webUser.endOfDay(selectedDate);

        Query query = dataSession.createQuery(
                "select sum(gpl.pointChange) from GamePointLedger gpl " +
                        "where gpl.contact.contactId = :contactId " +
                        "and gpl.pointChange > 0 " +
                        "and gpl.createdDate >= :dayStart and gpl.createdDate <= :dayEnd");
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("dayStart", dayStart);
        query.setParameter("dayEnd", dayEnd);
        return intValue((Number) query.uniqueResult());
    }

    private void printActionTable(PrintWriter out, List<ProjectActionNext> actions, Date selectedDate,
            WebUser webUser, boolean schoolSection) {
        String dateParam = toUserDateKey(selectedDate, webUser);

        out.println("<table class=\"boxed-mobile\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Task</th>");
        if (schoolSection) {
            out.println("    <th class=\"boxed\" style=\"text-align:center;\">Points</th>");
        }
        out.println("    <th class=\"boxed\" style=\"text-align:center;\">Action</th>");
        out.println("  </tr>");

        for (ProjectActionNext action : actions) {
            ProjectNextActionStatus status = action.getNextActionStatus();
            boolean isCompleted = status == ProjectNextActionStatus.COMPLETED;
            boolean isCancelled = status == ProjectNextActionStatus.CANCELLED;

            String rowStyle = "";
            if (isCancelled) {
                rowStyle = " style=\"color:#888; text-decoration: line-through;\"";
            } else if (isCompleted) {
                rowStyle = " style=\"background:#e7f6e9;\"";
            }

            String projectName = action.getProject() != null ? action.getProject().getProjectName() : "";
            String projectIcon = action.getProject() != null ? action.getProject().getProjectIcon() : null;
            String projectVisual = (projectIcon != null && !projectIcon.trim().isEmpty()) ? projectIcon.trim() : "";

            String projectDisplay = projectVisual.isEmpty()
                    ? escapeHtml(projectName)
                    : escapeHtml(projectVisual) + " " + escapeHtml(projectName);

            out.println("  <tr class=\"boxed\"" + rowStyle + ">");
            out.println("    <td class=\"boxed\">");
            out.println("      <strong>" + projectDisplay + "</strong>: ");
            String description = action.getNextDescriptionForDisplay(action.getContact());
            out.println("      " + (description == null ? "" : description));
            if (isCompleted) {
                out.println("      <span style=\"margin-left:6px; color:#2a7d2e;\">Completed</span>");
            }
            if (isCancelled) {
                out.println("      <span style=\"margin-left:6px; color:#888;\">Cancelled</span>");
            }
            out.println("    </td>");

            if (schoolSection) {
                out.println("    <td class=\"boxed\" style=\"text-align:center;\">" + getAvailablePoints(action)
                        + "</td>");
            }

            out.println("    <td class=\"boxed\" style=\"text-align:center;\">");
            if (!isCompleted && !isCancelled) {
                out.println("      <a href=\"school?" + PARAM_ACTION_ID + "=" + action.getActionNextId() + "&"
                        + PARAM_ACTION + "=" + ACTION_COMPLETE + "&" + PARAM_DATE + "=" + dateParam
                        + "\" class=\"action-icon\" title=\"Complete\">&#10004;</a>");
            } else {
                out.println("      <span class=\"small\">-</span>");
            }
            out.println("    </td>");
            out.println("  </tr>");
        }

        out.println("</table>");
    }

    private void printDateNavigation(PrintWriter out, Date selectedDate, WebUser webUser) {
        Date previousDay = webUser.addDays(selectedDate, -1);
        Date nextDay = webUser.addDays(selectedDate, 1);
        String prevDate = toUserDateKey(previousDay, webUser);
        String todayDate = toUserDateKey(webUser.getToday(), webUser);
        String nextDate = toUserDateKey(nextDay, webUser);

        out.println("<p>");
        out.println("  <a href=\"school?" + PARAM_DATE + "=" + prevDate + "\" class=\"box\">Previous</a>");
        out.println("  <a href=\"school?" + PARAM_DATE + "=" + todayDate + "\" class=\"button\">Today</a>");
        out.println("  <a href=\"school?" + PARAM_DATE + "=" + nextDate + "\" class=\"box\">Next</a>");
        out.println("</p>");
    }
}
