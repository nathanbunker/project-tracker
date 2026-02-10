package org.openimmunizationsoftware.pt.doa;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;

public class ProjectNarrativeDao {

    private static final int MINUTES_REVIEW_THRESHOLD = 5;

    private final Session session;

    public ProjectNarrativeDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public ProjectNarrativeDao(Session session) {
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    public List<ProjectNarrative> findByProjectAndDateRange(long projectId, LocalDate date) {
        Date start = startOfDay(date);
        Date end = startOfNextDay(date);
        Query query = session.createQuery(
                "from ProjectNarrative where projectId = :projectId and narrativeDate >= :start and narrativeDate < :end order by narrativeDate asc");
        query.setLong("projectId", projectId);
        query.setTimestamp("start", start);
        query.setTimestamp("end", end);
        return query.list();
    }

    public boolean hasNarrativeForProjectOnDate(long projectId, LocalDate date) {
        Date start = startOfDay(date);
        Date end = startOfNextDay(date);
        Query query = session.createQuery(
                "select 1 from ProjectNarrative where projectId = :projectId and narrativeDate >= :start and narrativeDate < :end");
        query.setLong("projectId", projectId);
        query.setTimestamp("start", start);
        query.setTimestamp("end", end);
        query.setMaxResults(1);
        return !query.list().isEmpty();
    }

    public void insert(ProjectNarrative narrative) {
        session.save(narrative);
    }

    public Map<Long, Integer> getMinutesSpentByProjectOnDate(LocalDate date) {
        Date start = startOfDay(date);
        Date end = startOfNextDay(date);
        Query query = session.createQuery(
                "select be.projectId, sum(be.billMins) from BillEntry be where be.startTime >= :start and be.startTime < :end group by be.projectId");
        query.setTimestamp("start", start);
        query.setTimestamp("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.list();
        Map<Long, Integer> results = new LinkedHashMap<Long, Integer>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }
            Number projectId = (Number) row[0];
            Number minutes = (Number) row[1];
            int minuteValue = minutes == null ? 0 : minutes.intValue();
            results.put(projectId.longValue(), minuteValue);
        }
        return results;
    }

    public List<Action> getCompletedActionsForProjectOnDate(long projectId, LocalDate date) {
        Date start = startOfDay(date);
        Date end = startOfNextDay(date);
        Query query = session.createQuery(
                "from ProjectActionNext where projectId = :projectId and nextActionStatusString = :status "
                        + "and nextChangeDate >= :start and nextChangeDate < :end "
                        + "and ((nextSummary is not null and nextSummary <> '') or (nextNotes is not null and nextNotes <> '')) "
                        + "order by nextChangeDate asc");
        query.setLong("projectId", projectId);
        query.setString("status", ProjectNextActionStatus.COMPLETED.getId());
        query.setTimestamp("start", start);
        query.setTimestamp("end", end);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> actions = query.list();
        List<Action> results = new ArrayList<Action>();
        for (ProjectActionNext action : actions) {
            results.add(new Action(
                    action.getActionNextId(),
                    action.getNextDescription(),
                    action.getNextChangeDate(),
                    firstNonEmpty(action.getNextSummary(), action.getNextNotes())));
        }
        return results;
    }

    public List<ReviewItem> listReviewItemsForDate(LocalDate date) {
        Date start = startOfDay(date);
        Date end = startOfNextDay(date);

        Query minutesQuery = session.createQuery(
                "select be.projectId, p.projectName, sum(be.billMins) "
                        + "from BillEntry be, Project p "
                        + "where be.projectId = p.projectId and be.startTime >= :start and be.startTime < :end "
                        + "group by be.projectId, p.projectName "
                        + "having sum(be.billMins) >= :minMinutes "
                        + "order by sum(be.billMins) desc");
        minutesQuery.setTimestamp("start", start);
        minutesQuery.setTimestamp("end", end);
        minutesQuery.setInteger("minMinutes", MINUTES_REVIEW_THRESHOLD);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = minutesQuery.list();

        Query reviewedQuery = session.createQuery(
                "select distinct pn.projectId from ProjectNarrative pn "
                        + "where pn.narrativeDate >= :start and pn.narrativeDate < :end");
        reviewedQuery.setTimestamp("start", start);
        reviewedQuery.setTimestamp("end", end);
        @SuppressWarnings("unchecked")
        List<Number> reviewedProjectIds = reviewedQuery.list();
        Set<Long> reviewedLookup = new HashSet<Long>();
        for (Number projectId : reviewedProjectIds) {
            if (projectId != null) {
                reviewedLookup.add(projectId.longValue());
            }
        }

        List<ReviewItem> results = new ArrayList<ReviewItem>();
        for (Object[] row : rows) {
            if (row == null || row.length < 3) {
                continue;
            }
            Number projectId = (Number) row[0];
            String projectName = (String) row[1];
            Number minutes = (Number) row[2];
            if (projectId == null) {
                continue;
            }
            int minuteValue = minutes == null ? 0 : minutes.intValue();
            boolean reviewed = reviewedLookup.contains(projectId.longValue());
            results.add(new ReviewItem(projectId.longValue(), projectName, minuteValue, reviewed));
        }
        return results;
    }

    private static Date startOfDay(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date startOfNextDay(LocalDate date) {
        return Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static String firstNonEmpty(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        return fallback;
    }

    public static class Action {
        private final int actionId;
        private final String description;
        private final Date completedDate;
        private final String completionNote;

        public Action(int actionId, String description, Date completedDate, String completionNote) {
            this.actionId = actionId;
            this.description = description;
            this.completedDate = completedDate;
            this.completionNote = completionNote;
        }

        public int getActionId() {
            return actionId;
        }

        public String getDescription() {
            return description;
        }

        public Date getCompletedDate() {
            return completedDate;
        }

        public String getCompletionNote() {
            return completionNote;
        }
    }

    public static class ReviewItem {
        private final long projectId;
        private final String projectName;
        private final int minutesSpent;
        private final boolean reviewed;

        public ReviewItem(long projectId, String projectName, int minutesSpent, boolean reviewed) {
            this.projectId = projectId;
            this.projectName = projectName;
            this.minutesSpent = minutesSpent;
            this.reviewed = reviewed;
        }

        public long getProjectId() {
            return projectId;
        }

        public String getProjectName() {
            return projectName;
        }

        public int getMinutesSpent() {
            return minutesSpent;
        }

        public boolean isReviewed() {
            return reviewed;
        }
    }
}
