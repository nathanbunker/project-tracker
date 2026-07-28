package org.openimmunizationsoftware.pt.doa;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.BillPlan;
import org.openimmunizationsoftware.pt.model.BillPlanStatus;

public class BillPlanDao {

    private final Session session;

    public BillPlanDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public BillPlanDao(Session session) {
        this.session = session;
    }

    public BillPlan getBillPlan(int billPlanId) {
        return (BillPlan) session.get(BillPlan.class, billPlanId);
    }

    public BillPlan save(BillPlan billPlan) {
        session.save(billPlan);
        return billPlan;
    }

    public void update(BillPlan billPlan) {
        session.update(billPlan);
    }

    @SuppressWarnings("unchecked")
    public List<BillPlan> listPlansForUserAndFiscalPeriod(int workspaceId, int webUserId, Date fiscalStartDate,
            Date fiscalEndDate) {
        Query query = session.createQuery(
                "from BillPlan where workspaceId = :workspaceId and webUserId = :webUserId "
                        + "and fiscalStartDate = :fiscalStartDate and fiscalEndDate = :fiscalEndDate "
                        + "order by effectiveDate desc, versionNum desc");
        query.setInteger("workspaceId", workspaceId);
        query.setInteger("webUserId", webUserId);
        query.setDate("fiscalStartDate", fiscalStartDate);
        query.setDate("fiscalEndDate", fiscalEndDate);
        return query.list();
    }

    public BillPlan findActiveApprovedPlan(int workspaceId, int webUserId, Date requestedDate) {
        Query query = session.createQuery(
                "from BillPlan where workspaceId = :workspaceId and webUserId = :webUserId "
                        + "and planStatus = :planStatus and effectiveDate <= :requestedDate "
                        + "order by effectiveDate desc, versionNum desc");
        query.setInteger("workspaceId", workspaceId);
        query.setInteger("webUserId", webUserId);
        query.setString("planStatus", BillPlanStatus.APPROVED.getCode());
        query.setDate("requestedDate", requestedDate);
        query.setMaxResults(1);
        return (BillPlan) query.uniqueResult();
    }

    public BillPlan findLatestApprovedPlanBefore(int workspaceId, int webUserId, String billPlanCode,
            Date effectiveDate, Integer excludeBillPlanId) {
        StringBuilder hql = new StringBuilder(
                "from BillPlan where workspaceId = :workspaceId and webUserId = :webUserId "
                        + "and billPlanCode = :billPlanCode and planStatus = :planStatus "
                        + "and effectiveDate <= :effectiveDate");
        if (excludeBillPlanId != null) {
            hql.append(" and billPlanId <> :excludeBillPlanId");
        }
        hql.append(" order by effectiveDate desc, versionNum desc");
        Query query = session.createQuery(hql.toString());
        query.setInteger("workspaceId", workspaceId);
        query.setInteger("webUserId", webUserId);
        query.setString("billPlanCode", billPlanCode);
        query.setString("planStatus", BillPlanStatus.APPROVED.getCode());
        query.setDate("effectiveDate", effectiveDate);
        if (excludeBillPlanId != null) {
            query.setInteger("excludeBillPlanId", excludeBillPlanId.intValue());
        }
        query.setMaxResults(1);
        return (BillPlan) query.uniqueResult();
    }
}