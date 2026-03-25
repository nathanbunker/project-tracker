package org.openimmunizationsoftware.pt.doa;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.StudentOffer;

public class StudentOfferDao {

    private final Session session;

    public StudentOfferDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public StudentOfferDao(Session session) {
        this.session = session;
    }

    public StudentOffer getById(int studentOfferId) {
        return (StudentOffer) session.get(StudentOffer.class, studentOfferId);
    }

    public StudentOffer save(StudentOffer studentOffer) {
        session.save(studentOffer);
        return studentOffer;
    }

    public void update(StudentOffer studentOffer) {
        session.update(studentOffer);
    }

    public void delete(StudentOffer studentOffer) {
        session.delete(studentOffer);
    }

    @SuppressWarnings("unchecked")
    public List<StudentOffer> listByContactId(int contactId) {
        Query query = session.createQuery(
                "from StudentOffer so where so.contact.contactId = :contactId order by so.displayOrder, so.studentOfferId");
        query.setInteger("contactId", contactId);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<StudentOffer> listByContactIdAndStatus(int contactId, String status) {
        Query query = session.createQuery(
                "from StudentOffer so where so.contact.contactId = :contactId and so.status = :status order by so.displayOrder, so.studentOfferId");
        query.setInteger("contactId", contactId);
        query.setString("status", status);
        return query.list();
    }
}
