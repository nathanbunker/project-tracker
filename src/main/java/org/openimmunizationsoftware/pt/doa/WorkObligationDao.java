package org.openimmunizationsoftware.pt.doa;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.WorkObligation;

public class WorkObligationDao {

    private final Session session;

    public WorkObligationDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public WorkObligationDao(Session session) {
        this.session = session;
    }

    public WorkObligation findForWeek(int ownerUserId, LocalDate weekStart) {
        Query query = session.createQuery(
                "from WorkObligation where ownerUserId = :ownerUserId and weekStart = :weekStart");
        query.setInteger("ownerUserId", ownerUserId);
        query.setDate("weekStart", java.sql.Date.valueOf(weekStart));
        return (WorkObligation) query.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<WorkObligation> listForRange(int ownerUserId, LocalDate startInclusive, LocalDate endExclusive) {
        Query query = session.createQuery(
                "from WorkObligation where ownerUserId = :ownerUserId and weekStart >= :start and weekStart < :end order by weekStart");
        query.setInteger("ownerUserId", ownerUserId);
        query.setDate("start", java.sql.Date.valueOf(startInclusive));
        query.setDate("end", java.sql.Date.valueOf(endExclusive));
        return query.list();
    }
}
