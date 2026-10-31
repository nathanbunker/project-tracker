package org.dandeliondaily.focus.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * The current action's project for the Focused Action "This project" drawer: the user's READY
 * actions in the project dated today or later plus overdue (within a year, like the dashboard's
 * overdue list), grouped Overdue / Today / Tomorrow / Later, and the ones completed today.
 * Recurring instances (generated from a template) are shown only when dated today; the rest of
 * a series is managed with its template.
 */
public class FocusProjectActionsService {

    public static boolean isRecurring(ActionNext action) {
        return action != null && action.getTemplateActionNextId() != null
                && action.getTemplateActionNextId().intValue() != 0;
    }

    public static class ProjectActions {
        private final List<ActionNext> overdue = new ArrayList<ActionNext>();
        private final List<ActionNext> today = new ArrayList<ActionNext>();
        private final List<ActionNext> tomorrow = new ArrayList<ActionNext>();
        private final List<ActionNext> later = new ArrayList<ActionNext>();
        private List<ActionNext> doneToday = Collections.emptyList();

        public List<ActionNext> getOverdue() {
            return overdue;
        }

        public List<ActionNext> getToday() {
            return today;
        }

        public List<ActionNext> getTomorrow() {
            return tomorrow;
        }

        public List<ActionNext> getLater() {
            return later;
        }

        public List<ActionNext> getDoneToday() {
            return doneToday;
        }

        public int getOpenCount() {
            return overdue.size() + today.size() + tomorrow.size() + later.size();
        }
    }

    public ProjectActions load(Session dataSession, WebUser webUser, Integer workspaceId, int projectId) {
        LocalDate today = webUser.getLocalDateToday();
        Query query = dataSession.createQuery(
                "select distinct an from ActionNext an "
                        + "left join fetch an.project "
                        + "where an.workspaceId = :workspaceId and an.projectId = :projectId "
                        + "and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                        + "and an.nextDescription <> '' "
                        + "and an.nextActionStatusString = :nextActionStatus "
                        + "and (an.templateTypeString is null or an.templateTypeString = '') "
                        + "and an.nextActionDate >= :oldestDate");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("projectId", Integer.valueOf(projectId));
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
        query.setParameter("oldestDate", java.sql.Date.valueOf(today.minusYears(1)));
        @SuppressWarnings("unchecked")
        List<ActionNext> open = query.list();

        ProjectActions actions = group(open, today, webUser.getZoneId());
        actions.doneToday = loadDoneToday(dataSession, webUser, workspaceId, projectId);
        return actions;
    }

    /** Groups by action date, ordered by date then completion order then priority. */
    public ProjectActions group(List<ActionNext> open, LocalDate today, ZoneId zoneId) {
        List<ActionNext> sorted = new ArrayList<ActionNext>(open);
        Collections.sort(sorted, (a, b) -> {
            int byDate = compareDates(toLocalDate(a.getNextActionDate(), zoneId), toLocalDate(b.getNextActionDate(), zoneId));
            if (byDate != 0) {
                return byDate;
            }
            int orderA = a.getCompletionOrder() > 0 ? a.getCompletionOrder() : Integer.MAX_VALUE;
            int orderB = b.getCompletionOrder() > 0 ? b.getCompletionOrder() : Integer.MAX_VALUE;
            if (orderA != orderB) {
                return orderA < orderB ? -1 : 1;
            }
            return Integer.compare(b.getPriorityLevel(), a.getPriorityLevel());
        });
        ProjectActions actions = new ProjectActions();
        LocalDate tomorrow = today.plusDays(1);
        for (ActionNext action : sorted) {
            LocalDate date = toLocalDate(action.getNextActionDate(), zoneId);
            if (date == null || (isRecurring(action) && !date.equals(today))) {
                continue;
            }
            if (date.isBefore(today)) {
                actions.overdue.add(action);
            } else if (date.equals(today)) {
                actions.today.add(action);
            } else if (date.equals(tomorrow)) {
                actions.tomorrow.add(action);
            } else {
                actions.later.add(action);
            }
        }
        return actions;
    }

    /** Same rule as the dashboard's completed-today list, limited to the project. */
    private List<ActionNext> loadDoneToday(Session dataSession, WebUser webUser, Integer workspaceId, int projectId) {
        Date start = TimeTracker.createToday(webUser).getTime();
        Date end = TimeTracker.createTomorrow(webUser).getTime();
        Query query = dataSession.createQuery(
                "select distinct an from ActionNext an "
                        + "left join fetch an.project "
                        + "where an.workspaceId = :workspaceId and an.projectId = :projectId "
                        + "and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                        + "and an.nextActionStatusString = :nextActionStatus and an.nextDescription <> '' "
                        + "and an.nextChangeDate >= :today and an.nextChangeDate < :tomorrow "
                        + "order by an.nextChangeDate");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("projectId", Integer.valueOf(projectId));
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("nextActionStatus", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("today", start);
        query.setParameter("tomorrow", end);
        @SuppressWarnings("unchecked")
        List<ActionNext> done = query.list();
        return done;
    }

    private int compareDates(LocalDate a, LocalDate b) {
        if (a == null || b == null) {
            return a == null ? (b == null ? 0 : 1) : -1;
        }
        return a.compareTo(b);
    }

    private LocalDate toLocalDate(Date date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(zoneId).toLocalDate();
    }
}
