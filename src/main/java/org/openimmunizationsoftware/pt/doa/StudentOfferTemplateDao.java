package org.openimmunizationsoftware.pt.doa;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.StudentOfferTemplate;

public class StudentOfferTemplateDao {

    private final Session session;

    public StudentOfferTemplateDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public StudentOfferTemplateDao(Session session) {
        this.session = session;
    }

    public StudentOfferTemplate getById(int studentOfferTemplateId) {
        return (StudentOfferTemplate) session.get(StudentOfferTemplate.class, studentOfferTemplateId);
    }

    public StudentOfferTemplate save(StudentOfferTemplate studentOfferTemplate) {
        session.save(studentOfferTemplate);
        return studentOfferTemplate;
    }

    public void update(StudentOfferTemplate studentOfferTemplate) {
        session.update(studentOfferTemplate);
    }

    public void delete(StudentOfferTemplate studentOfferTemplate) {
        session.delete(studentOfferTemplate);
    }

    @SuppressWarnings("unchecked")
    public List<StudentOfferTemplate> listByContactId(int contactId) {
        Query query = session.createQuery(
                "from StudentOfferTemplate sot where sot.contact.contactId = :contactId order by sot.displayOrder, sot.studentOfferTemplateId");
        query.setInteger("contactId", contactId);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<StudentOfferTemplate> listByContactIdAndStatus(int contactId, String status) {
        Query query = session.createQuery(
                "from StudentOfferTemplate sot where sot.contact.contactId = :contactId and sot.status = :status order by sot.displayOrder, sot.studentOfferTemplateId");
        query.setInteger("contactId", contactId);
        query.setString("status", status);
        return query.list();
    }
}
