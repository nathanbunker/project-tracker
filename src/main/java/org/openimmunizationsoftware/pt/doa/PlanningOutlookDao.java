package org.openimmunizationsoftware.pt.doa;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.PlanningOutlook;

public class PlanningOutlookDao {

    private final Session session;

    public PlanningOutlookDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public PlanningOutlookDao(Session session) {
        this.session = session;
    }

    public PlanningOutlook findForPeriod(int ownerUserId, String periodType, LocalDate periodStart) {
        Query query = session.createQuery(
                "from PlanningOutlook where ownerUserId = :ownerUserId and periodType = :periodType "
                        + "and periodStart = :periodStart");
        query.setInteger("ownerUserId", ownerUserId);
        query.setString("periodType", periodType);
        query.setDate("periodStart", java.sql.Date.valueOf(periodStart));
        return (PlanningOutlook) query.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<PlanningOutlook> listForRange(int ownerUserId, String periodType, LocalDate startInclusive,
            LocalDate endExclusive) {
        Query query = session.createQuery(
                "from PlanningOutlook where ownerUserId = :ownerUserId and periodType = :periodType "
                        + "and periodStart >= :start and periodStart < :end order by periodStart");
        query.setInteger("ownerUserId", ownerUserId);
        query.setString("periodType", periodType);
        query.setDate("start", java.sql.Date.valueOf(startInclusive));
        query.setDate("end", java.sql.Date.valueOf(endExclusive));
        return query.list();
    }

    public void save(PlanningOutlook outlook) {
        session.save(outlook);
    }

    public void update(PlanningOutlook outlook) {
        session.update(outlook);
    }
}
