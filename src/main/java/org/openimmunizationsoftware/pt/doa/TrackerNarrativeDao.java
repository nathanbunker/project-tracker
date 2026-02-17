package org.openimmunizationsoftware.pt.doa;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;
import org.openimmunizationsoftware.pt.model.TrackerNarrativeReviewStatus;

public class TrackerNarrativeDao {

    private final Session session;

    public TrackerNarrativeDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public TrackerNarrativeDao(Session session) {
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    public List<TrackerNarrative> listByContactUpdatedAfter(int contactId, Date updatedAfter) {
        Query query = session.createQuery(
                "from TrackerNarrative where contactId = :contactId and lastUpdated > :updatedAfter "
                        + "order by lastUpdated asc");
        query.setInteger("contactId", contactId);
        query.setTimestamp("updatedAfter", updatedAfter);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<TrackerNarrative> findByTypeAndPeriod(String type, LocalDate start, LocalDate end) {
        Query query = session.createQuery(
                "from TrackerNarrative where narrativeType = :type and periodStart = :start and periodEnd = :end "
                        + "order by dateGenerated desc");
        query.setString("type", type);
        query.setDate("start", toSqlDate(start));
        query.setDate("end", toSqlDate(end));
        return query.list();
    }

    public TrackerNarrative findApprovedByTypeAndPeriod(String type, LocalDate start, LocalDate end) {
        Query query = session.createQuery(
                "from TrackerNarrative where narrativeType = :type and periodStart = :start and periodEnd = :end "
                        + "and reviewStatusString = :status order by dateApproved desc, dateGenerated desc");
        query.setString("type", type);
        query.setDate("start", toSqlDate(start));
        query.setDate("end", toSqlDate(end));
        query.setString("status", TrackerNarrativeReviewStatus.APPROVED.getId());
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<TrackerNarrative> results = query.list();
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    public Map<LocalDate, Integer> sumBillableMinutesByDay(String username, LocalDate startInclusive,
            LocalDate endExclusive) {
        Map<LocalDate, Integer> minutesByDay = new HashMap<LocalDate, Integer>();
        if (username == null || username.trim().length() == 0 || startInclusive == null || endExclusive == null
                || !startInclusive.isBefore(endExclusive)) {
            return minutesByDay;
        }

        Query query = session.createQuery(
                "select startTime, billMins from BillEntry "
                        + "where username = :username and billable = :billable and billMins > 0 "
                        + "and startTime >= :start and startTime < :end");
        query.setString("username", username);
        query.setString("billable", "Y");
        query.setTimestamp("start", toDate(startInclusive.atStartOfDay()));
        query.setTimestamp("end", toDate(endExclusive.atStartOfDay()));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.list();
        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }
            Date startTime = (Date) row[0];
            Number billMinsNumber = (Number) row[1];
            if (startTime == null || billMinsNumber == null) {
                continue;
            }
            LocalDate day = toLocalDate(startTime);
            int billMins = billMinsNumber.intValue();
            Integer existing = minutesByDay.get(day);
            minutesByDay.put(day, (existing == null ? 0 : existing.intValue()) + billMins);
        }
        return minutesByDay;
    }

    public Set<LocalDate> findApprovedPeriodStarts(String type, LocalDate startInclusive, LocalDate endInclusive) {
        Set<LocalDate> approvedStarts = new HashSet<LocalDate>();
        if (type == null || type.trim().length() == 0 || startInclusive == null || endInclusive == null
                || startInclusive.isAfter(endInclusive)) {
            return approvedStarts;
        }

        Query query = session.createQuery(
                "select periodStart from TrackerNarrative where narrativeType = :type "
                        + "and reviewStatusString = :status and periodStart >= :start and periodStart <= :end");
        query.setString("type", type);
        query.setString("status", TrackerNarrativeReviewStatus.APPROVED.getId());
        query.setDate("start", toSqlDate(startInclusive));
        query.setDate("end", toSqlDate(endInclusive));

        @SuppressWarnings("unchecked")
        List<Date> rows = query.list();
        for (Date date : rows) {
            LocalDate localDate = toLocalDate(date);
            if (localDate != null) {
                approvedStarts.add(localDate);
            }
        }
        return approvedStarts;
    }

    public long insert(TrackerNarrative narrative) {
        if (narrative.getLastUpdated() == null) {
            narrative.setLastUpdated(new Date());
        }
        Serializable id = session.save(narrative);
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return narrative.getNarrativeId();
    }

    public void updateGeneratedText(long id, String markdownGenerated, String modelName, String promptVersion,
            LocalDateTime dateGenerated, String status) {
        TrackerNarrative narrative = getById(id);
        if (narrative == null) {
            return;
        }
        narrative.setMarkdownGenerated(markdownGenerated);
        narrative.setModelName(modelName);
        narrative.setPromptVersion(promptVersion);
        narrative.setDateGenerated(toDate(dateGenerated));
        narrative.setReviewStatusString(status);
        narrative.setLastUpdated(new Date());
        session.update(narrative);
    }

    public void updateFinalText(long id, String markdownFinal) {
        TrackerNarrative narrative = getById(id);
        if (narrative == null) {
            return;
        }
        narrative.setMarkdownFinal(markdownFinal);
        narrative.setLastUpdated(new Date());
        session.update(narrative);
    }

    public void approve(long id, LocalDateTime dateApproved) {
        TrackerNarrative narrative = getById(id);
        if (narrative == null) {
            return;
        }
        String type = narrative.getNarrativeType();
        LocalDate start = toLocalDate(narrative.getPeriodStart());
        LocalDate end = toLocalDate(narrative.getPeriodEnd());

        Transaction transaction = session.beginTransaction();
        try {
            if (type != null && start != null && end != null) {
                clearApprovedForPeriod(type, start, end);
            }
            narrative.setReviewStatus(TrackerNarrativeReviewStatus.APPROVED);
            narrative.setDateApproved(toDate(dateApproved));
            narrative.setLastUpdated(new Date());
            session.update(narrative);
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw exception;
        }
    }

    public void reject(long id) {
        TrackerNarrative narrative = getById(id);
        if (narrative == null) {
            return;
        }
        narrative.setReviewStatus(TrackerNarrativeReviewStatus.REJECTED);
        narrative.setLastUpdated(new Date());
        session.update(narrative);
    }

    public void softDelete(long id) {
        TrackerNarrative narrative = getById(id);
        if (narrative == null) {
            return;
        }
        narrative.setReviewStatus(TrackerNarrativeReviewStatus.DELETED);
        narrative.setLastUpdated(new Date());
        session.update(narrative);
    }

    public void clearApprovedForPeriod(String type, LocalDate start, LocalDate end) {
        Query query = session.createQuery(
                "update TrackerNarrative set reviewStatusString = :rejected, lastUpdated = :lastUpdated "
                        + "where narrativeType = :type and periodStart = :start and periodEnd = :end "
                        + "and reviewStatusString = :approved");
        query.setString("rejected", TrackerNarrativeReviewStatus.REJECTED.getId());
        query.setString("approved", TrackerNarrativeReviewStatus.APPROVED.getId());
        query.setString("type", type);
        query.setDate("start", toSqlDate(start));
        query.setDate("end", toSqlDate(end));
        query.setTimestamp("lastUpdated", new Date());
        query.executeUpdate();
    }

    private TrackerNarrative getById(long id) {
        return (TrackerNarrative) session.get(TrackerNarrative.class, (int) id);
    }

    private static java.sql.Date toSqlDate(LocalDate date) {
        return date == null ? null : java.sql.Date.valueOf(date);
    }

    private static Date toDate(LocalDateTime dateTime) {
        return dateTime == null ? null : Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
