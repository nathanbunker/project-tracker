package org.openimmunizationsoftware.pt.doa;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.BillEntry;

/**
 * DAO for BillEntry operations used by servlets.
 */
public class BillEntryDao {

    private final Session session;

    public BillEntryDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public BillEntryDao(Session session) {
        this.session = session;
    }

    public BillEntry getBillEntry(int billId) {
        return (BillEntry) session.get(BillEntry.class, billId);
    }

    public BillEntry saveBillEntry(BillEntry billEntry) {
        session.save(billEntry);
        return billEntry;
    }

    public void updateBillEntry(BillEntry billEntry) {
        session.update(billEntry);
    }

    public void deleteBillEntry(BillEntry billEntry) {
        session.delete(billEntry);
    }

    @SuppressWarnings("unchecked")
    public List<BillEntry> listBillEntriesForUserBetween(String username, Date start, Date end) {
        Query query = session.createQuery(
                "from BillEntry where username = :username and startTime >= :start and startTime < :end order by startTime");
        query.setString("username", username);
        query.setTimestamp("start", start);
        query.setTimestamp("end", end);
        return query.list();
    }

}
