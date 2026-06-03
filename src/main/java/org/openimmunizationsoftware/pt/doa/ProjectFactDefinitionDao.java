package org.openimmunizationsoftware.pt.doa;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.ProjectFactDefinition;

public class ProjectFactDefinitionDao {

    private final Session session;

    public ProjectFactDefinitionDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public ProjectFactDefinitionDao(Session session) {
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    public List<ProjectFactDefinition> listByWorkspaceId(int workspaceId, boolean includeInactive) {
        StringBuilder hql = new StringBuilder("from ProjectFactDefinition pfd where pfd.workspaceId = :workspaceId ");
        if (!includeInactive) {
            hql.append("and pfd.active = :active ");
        }
        hql.append(
                "order by lower(pfd.factGroup), pfd.displayOrder, lower(pfd.factLabel), pfd.projectFactDefinitionId");
        Query query = session.createQuery(hql.toString());
        query.setInteger("workspaceId", workspaceId);
        if (!includeInactive) {
            query.setString("active", ProjectFactDefinition.ACTIVE_YES);
        }
        return query.list();
    }

    public ProjectFactDefinition getById(int projectFactDefinitionId) {
        return (ProjectFactDefinition) session.get(ProjectFactDefinition.class, projectFactDefinitionId);
    }

    public ProjectFactDefinition save(ProjectFactDefinition projectFactDefinition) {
        session.save(projectFactDefinition);
        return projectFactDefinition;
    }

    public void update(ProjectFactDefinition projectFactDefinition) {
        session.update(projectFactDefinition);
    }

    public void deactivate(int workspaceId, int projectFactDefinitionId, Integer webUserId, Date now) {
        Query query = session.createQuery(
                "update ProjectFactDefinition set active = :inactive, lastModifiedByWebUserId = :webUserId, lastModifiedDate = :lastModifiedDate "
                        + "where workspaceId = :workspaceId and projectFactDefinitionId = :projectFactDefinitionId");
        query.setString("inactive", ProjectFactDefinition.ACTIVE_NO);
        query.setParameter("webUserId", webUserId);
        query.setTimestamp("lastModifiedDate", now);
        query.setInteger("workspaceId", workspaceId);
        query.setInteger("projectFactDefinitionId", projectFactDefinitionId);
        query.executeUpdate();
    }

    public boolean existsByWorkspaceAndFactCodeIgnoreCase(int workspaceId, String factCode, Integer excludeId) {
        StringBuilder hql = new StringBuilder(
                "select count(*) from ProjectFactDefinition where workspaceId = :workspaceId and lower(factCode) = :factCode");
        if (excludeId != null) {
            hql.append(" and projectFactDefinitionId <> :excludeId");
        }
        Query query = session.createQuery(hql.toString());
        query.setInteger("workspaceId", workspaceId);
        query.setString("factCode", factCode == null ? "" : factCode.toLowerCase());
        if (excludeId != null) {
            query.setInteger("excludeId", excludeId.intValue());
        }
        Number count = (Number) query.uniqueResult();
        return count != null && count.intValue() > 0;
    }

    public int nextDisplayOrderForGroup(int workspaceId, String factGroup) {
        Query query = session.createQuery(
                "select max(displayOrder) from ProjectFactDefinition where workspaceId = :workspaceId and lower(factGroup) = :factGroup");
        query.setInteger("workspaceId", workspaceId);
        query.setString("factGroup", factGroup == null ? "" : factGroup.toLowerCase());
        Number max = (Number) query.uniqueResult();
        if (max == null) {
            return 0;
        }
        return max.intValue() + 10;
    }
}
