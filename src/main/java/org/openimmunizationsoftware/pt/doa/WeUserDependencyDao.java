package org.openimmunizationsoftware.pt.doa;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.WeUserDependency;

public class WeUserDependencyDao {

    private final Session session;

    public WeUserDependencyDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public WeUserDependencyDao(Session session) {
        this.session = session;
    }

    public WeUserDependency save(WeUserDependency dependency) {
        session.save(dependency);
        return dependency;
    }

    public void update(WeUserDependency dependency) {
        session.update(dependency);
    }

    public void delete(WeUserDependency dependency) {
        session.delete(dependency);
    }

    public WeUserDependency getById(int dependencyId) {
        return (WeUserDependency) session.get(WeUserDependency.class, dependencyId);
    }

    public List<WeUserDependency> listByGuardianWebUserId(int guardianWebUserId) {
        Query query = session.createQuery(
                "from WeUserDependency wud where wud.guardianWebUser.webUserId = :guardianWebUserId order by wud.createdDate desc");
        query.setInteger("guardianWebUserId", guardianWebUserId);
        @SuppressWarnings("unchecked")
        List<WeUserDependency> results = query.list();
        return results;
    }

    public List<WeUserDependency> listByDependentWebUserId(int dependentWebUserId) {
        Query query = session.createQuery(
                "from WeUserDependency wud where wud.dependentWebUser.webUserId = :dependentWebUserId order by wud.createdDate desc");
        query.setInteger("dependentWebUserId", dependentWebUserId);
        @SuppressWarnings("unchecked")
        List<WeUserDependency> results = query.list();
        return results;
    }

    public List<WeUserDependency> listByGuardianAndStatus(int guardianWebUserId, String dependencyStatus) {
        Query query = session.createQuery(
                "from WeUserDependency wud where wud.guardianWebUser.webUserId = :guardianWebUserId and wud.dependencyStatus = :dependencyStatus order by wud.createdDate desc");
        query.setInteger("guardianWebUserId", guardianWebUserId);
        query.setString("dependencyStatus", dependencyStatus);
        @SuppressWarnings("unchecked")
        List<WeUserDependency> results = query.list();
        return results;
    }

    public WeUserDependency findByInviteTokenHash(String inviteTokenHash) {
        Query query = session.createQuery(
                "from WeUserDependency wud where wud.inviteTokenHash = :inviteTokenHash order by wud.createdDate desc");
        query.setString("inviteTokenHash", inviteTokenHash);
        query.setMaxResults(1);
        return (WeUserDependency) query.uniqueResult();
    }

    public boolean hasActiveDependency(int guardianWebUserId, int dependentWebUserId) {
        Query query = session.createQuery(
                "select count(*) from WeUserDependency wud where wud.guardianWebUser.webUserId = :guardianWebUserId and wud.dependentWebUser.webUserId = :dependentWebUserId and wud.dependencyStatus = :dependencyStatus");
        query.setInteger("guardianWebUserId", guardianWebUserId);
        query.setInteger("dependentWebUserId", dependentWebUserId);
        query.setString("dependencyStatus", "active");
        Number count = (Number) query.uniqueResult();
        return count != null && count.longValue() > 0;
    }

    public int expireInvitesBefore(Date cutoffDate) {
        Query query = session.createQuery(
                "update WeUserDependency wud set wud.dependencyStatus = :expiredStatus, wud.endedDate = :endedDate where wud.dependencyStatus = :invitedStatus and wud.inviteExpiry is not null and wud.inviteExpiry < :cutoffDate");
        query.setString("expiredStatus", "expired");
        query.setTimestamp("endedDate", cutoffDate);
        query.setString("invitedStatus", "invited");
        query.setTimestamp("cutoffDate", cutoffDate);
        return query.executeUpdate();
    }
}
