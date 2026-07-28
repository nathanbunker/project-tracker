package org.openimmunizationsoftware.pt.doa;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dandeliondaily.timereview.model.AllocationActualMinutes;
import org.dandeliondaily.timereview.model.AllocationProjectDrillDown;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.BillExpected;

public class BillAllocationDao {

    private final Session session;

    public BillAllocationDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public BillAllocationDao(Session session) {
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    public List<AllocationActualMinutes> listActualMinutesByHistoricalBillCode(int workspaceId, int webUserId,
            Date startDate, Date endDateExclusive) {
        Query query = session.createQuery(
                "select be.workspaceId, be.webUser.webUserId, be.billCode, sum(be.billMins) "
                        + "from BillEntry be where be.workspaceId = :workspaceId and be.webUser.webUserId = :webUserId "
                        + "and be.startTime >= :startDate and be.startTime < :endDateExclusive "
                        + "and be.billCode is not null group by be.workspaceId, be.webUser.webUserId, be.billCode "
                        + "order by be.billCode");
        query.setInteger("workspaceId", workspaceId);
        query.setInteger("webUserId", webUserId);
        query.setTimestamp("startDate", startDate);
        query.setTimestamp("endDateExclusive", endDateExclusive);
        List<Object[]> rows = query.list();
        List<AllocationActualMinutes> results = new ArrayList<AllocationActualMinutes>();
        for (Object[] row : rows) {
            AllocationActualMinutes item = new AllocationActualMinutes();
            item.setWorkspaceId(((Number) row[0]).intValue());
            item.setWebUserId(((Number) row[1]).intValue());
            item.setBillCode((String) row[2]);
            item.setTotalMinutes(row[3] == null ? 0 : ((Number) row[3]).intValue());
            results.add(item);
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<AllocationProjectDrillDown> listProjectDrillDownByHistoricalBillCode(int workspaceId, int webUserId,
            Date startDate, Date endDateExclusive) {
        Query query = session.createQuery(
                "select be.billCode, p.projectId, p.projectName, sum(be.billMins), min(be.startTime), max(be.startTime) "
                        + "from BillEntry be, Project p where be.projectId = p.projectId and be.workspaceId = :workspaceId "
                        + "and be.webUser.webUserId = :webUserId and be.startTime >= :startDate and be.startTime < :endDateExclusive "
                        + "and be.billCode is not null group by be.billCode, p.projectId, p.projectName "
                        + "order by be.billCode, sum(be.billMins) desc, p.projectName");
        query.setInteger("workspaceId", workspaceId);
        query.setInteger("webUserId", webUserId);
        query.setTimestamp("startDate", startDate);
        query.setTimestamp("endDateExclusive", endDateExclusive);
        List<Object[]> rows = query.list();
        List<AllocationProjectDrillDown> results = new ArrayList<AllocationProjectDrillDown>();
        for (Object[] row : rows) {
            AllocationProjectDrillDown item = new AllocationProjectDrillDown();
            item.setBillCode((String) row[0]);
            item.setProjectId(((Number) row[1]).intValue());
            item.setProjectLabel((String) row[2]);
            item.setTotalMinutes(row[3] == null ? 0 : ((Number) row[3]).intValue());
            item.setFirstEntryDate((Date) row[4]);
            item.setLastEntryDate((Date) row[5]);
            results.add(item);
        }
        return results;
    }

    public int sumUsedBudgetMinutes(int billBudgetId) {
        Query query = session.createQuery(
                "select sum(be.billMins) from BillEntry be where be.billBudgetId = :billBudgetId");
        query.setInteger("billBudgetId", billBudgetId);
        Number value = (Number) query.uniqueResult();
        return value == null ? 0 : value.intValue();
    }

    @SuppressWarnings("unchecked")
    public List<BillExpected> listBillExpectedBetween(int webUserId, Date startDateInclusive, Date endDateInclusive) {
        Query query = session.createQuery(
                "from BillExpected where id.webUserId = :webUserId and id.billDate >= :startDateInclusive "
                        + "and id.billDate <= :endDateInclusive order by id.billDate asc");
        query.setInteger("webUserId", webUserId);
        query.setDate("startDateInclusive", startDateInclusive);
        query.setDate("endDateInclusive", endDateInclusive);
        return query.list();
    }
}