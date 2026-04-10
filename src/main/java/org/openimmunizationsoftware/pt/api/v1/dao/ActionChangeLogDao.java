package org.openimmunizationsoftware.pt.api.v1.dao;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.ActionChangeLog;

public class ActionChangeLogDao {

    public List<ActionChangeLog> listByAction(int workspaceId, int actionNextId, int limit) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "select cl from ActionChangeLog cl join cl.action a "
                        + "where cl.actionNextId = :actionNextId and a.workspaceId = :workspaceId "
                        + "order by cl.changeDate desc");
        query.setInteger("actionNextId", actionNextId);
        query.setInteger("workspaceId", workspaceId);
        query.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<ActionChangeLog> results = query.list();
        return results;
    }

    public List<ActionChangeLog> listByProject(int workspaceId, int projectId, int limit) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "select cl from ActionChangeLog cl join cl.project p "
                        + "where cl.projectId = :projectId and p.workspaceId = :workspaceId "
                        + "order by cl.changeDate desc");
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        query.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<ActionChangeLog> results = query.list();
        return results;
    }
}
