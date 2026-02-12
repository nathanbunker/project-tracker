package org.openimmunizationsoftware.pt.doa;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

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

    public long insert(TrackerNarrative narrative) {
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
        session.update(narrative);
    }

    public void updateFinalText(long id, String markdownFinal) {
        TrackerNarrative narrative = getById(id);
        if (narrative == null) {
            return;
        }
        narrative.setMarkdownFinal(markdownFinal);
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
        session.update(narrative);
    }

    public void softDelete(long id) {
        TrackerNarrative narrative = getById(id);
        if (narrative == null) {
            return;
        }
        narrative.setReviewStatus(TrackerNarrativeReviewStatus.DELETED);
        session.update(narrative);
    }

    public void clearApprovedForPeriod(String type, LocalDate start, LocalDate end) {
        Query query = session.createQuery(
                "update TrackerNarrative set reviewStatusString = :rejected where narrativeType = :type "
                        + "and periodStart = :start and periodEnd = :end and reviewStatusString = :approved");
        query.setString("rejected", TrackerNarrativeReviewStatus.REJECTED.getId());
        query.setString("approved", TrackerNarrativeReviewStatus.APPROVED.getId());
        query.setString("type", type);
        query.setDate("start", toSqlDate(start));
        query.setDate("end", toSqlDate(end));
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
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
