package org.openimmunizationsoftware.pt.doa;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.BillPlanTarget;

public class BillPlanTargetDao {

    private final Session session;

    public BillPlanTargetDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public BillPlanTargetDao(Session session) {
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    public List<BillPlanTarget> listTargetsForPlan(int billPlanId) {
        Query query = session.createQuery(
                "from BillPlanTarget where billPlan.billPlanId = :billPlanId order by displayOrder, billCode");
        query.setInteger("billPlanId", billPlanId);
        return query.list();
    }
}