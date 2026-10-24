package org.openimmunizationsoftware.pt.doa;

import java.util.Arrays;
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

    /**
     * Returns the plan in effect on the requested date. A superseded plan still
     * covers the dates before its successor's effective date, so history keeps
     * the plan that applied at the time.
     */
    public BillPlan findActiveApprovedPlan(int workspaceId, int webUserId, Date requestedDate) {
        Query query = session.createQuery(
                "from BillPlan p where p.workspaceId = :workspaceId and p.webUserId = :webUserId "
                        + "and p.planStatus in (:statuses) and p.effectiveDate <= :requestedDate "
                        + "and p.fiscalStartDate <= :requestedDate and p.fiscalEndDate >= :requestedDate "
                        + "and not exists (select q.billPlanId from BillPlan q "
                        + "where q.supersedesBillPlanId = p.billPlanId and q.planStatus in (:statuses) "
                        + "and q.effectiveDate <= :requestedDate) "
                        + "order by p.effectiveDate desc, p.versionNum desc");
        query.setInteger("workspaceId", workspaceId);
        query.setInteger("webUserId", webUserId);
        query.setParameterList("statuses",
                Arrays.asList(BillPlanStatus.APPROVED.getCode(), BillPlanStatus.SUPERSEDED.getCode()));
        query.setDate("requestedDate", requestedDate);
        query.setMaxResults(1);
        return (BillPlan) query.uniqueResult();
    }

    public int findMaxVersionNum(int workspaceId, int webUserId, String billPlanCode) {
        Query query = session.createQuery(
                "select max(versionNum) from BillPlan where workspaceId = :workspaceId "
                        + "and webUserId = :webUserId and billPlanCode = :billPlanCode");
        query.setInteger("workspaceId", workspaceId);
        query.setInteger("webUserId", webUserId);
        query.setString("billPlanCode", billPlanCode);
        Number max = (Number) query.uniqueResult();
        return max == null ? 0 : max.intValue();
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