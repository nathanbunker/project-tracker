package org.openimmunizationsoftware.pt.doa;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.BillFundingSource;

public class BillFundingSourceDao {

    private final Session session;

    public BillFundingSourceDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public BillFundingSourceDao(Session session) {
        this.session = session;
    }

    public BillFundingSource getFundingSource(int fundingSourceId) {
        return (BillFundingSource) session.get(BillFundingSource.class, fundingSourceId);
    }

    public BillFundingSource save(BillFundingSource fundingSource) {
        session.save(fundingSource);
        return fundingSource;
    }

    public void update(BillFundingSource fundingSource) {
        session.update(fundingSource);
    }

    @SuppressWarnings("unchecked")
    public List<BillFundingSource> listForWorkspace(int workspaceId) {
        Query query = session.createQuery(
                "from BillFundingSource where workspaceId = :workspaceId order by fundingSourceLabel, fundingSourceCode");
        query.setInteger("workspaceId", workspaceId);
        return query.list();
    }
}