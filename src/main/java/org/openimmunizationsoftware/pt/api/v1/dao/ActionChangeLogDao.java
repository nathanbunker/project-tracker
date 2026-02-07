package org.openimmunizationsoftware.pt.api.v1.dao;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.ProjectActionChangeLog;

public class ActionChangeLogDao {

    public List<ProjectActionChangeLog> listByAction(String providerId, int actionId, int limit) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "select cl from ProjectActionChangeLog cl join cl.action a "
                        + "where cl.actionId = :actionId and a.provider.providerId = :providerId "
                        + "order by cl.changeDate desc");
        query.setInteger("actionId", actionId);
        query.setString("providerId", providerId);
        query.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<ProjectActionChangeLog> results = query.list();
        return results;
    }

    public List<ProjectActionChangeLog> listByProject(String providerId, int projectId, int limit) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "select cl from ProjectActionChangeLog cl join cl.project p "
                        + "where cl.projectId = :projectId and p.provider.providerId = :providerId "
                        + "order by cl.changeDate desc");
        query.setInteger("projectId", projectId);
        query.setString("providerId", providerId);
        query.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<ProjectActionChangeLog> results = query.list();
        return results;
    }
}
