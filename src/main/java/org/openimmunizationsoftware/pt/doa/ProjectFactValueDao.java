package org.openimmunizationsoftware.pt.doa;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.ProjectFactValue;

public class ProjectFactValueDao {

    private final Session session;

    public ProjectFactValueDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public ProjectFactValueDao(Session session) {
        this.session = session;
    }

    public ProjectFactValue getByProjectAndFactDefinition(int projectId, int projectFactDefinitionId) {
        Query query = session.createQuery(
                "from ProjectFactValue where projectId = :projectId and projectFactDefinitionId = :projectFactDefinitionId");
        query.setInteger("projectId", projectId);
        query.setInteger("projectFactDefinitionId", projectFactDefinitionId);
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<ProjectFactValue> list = query.list();
        return list.isEmpty() ? null : list.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<ProjectFactValue> listByProjectId(int projectId) {
        Query query = session.createQuery(
                "from ProjectFactValue where projectId = :projectId order by projectFactDefinitionId, projectFactValueId");
        query.setInteger("projectId", projectId);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<ProjectFactValue> listByProjectIds(List<Integer> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        Query query = session.createQuery(
                "from ProjectFactValue where projectId in (:projectIds) order by projectId, projectFactDefinitionId, projectFactValueId");
        query.setParameterList("projectIds", projectIds);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> listByProjectWithDefinitions(int workspaceId, int projectId,
            boolean includeInactiveDefinitions) {
        StringBuilder hql = new StringBuilder(
                "select pfd, pfv from ProjectFactDefinition pfd left join ProjectFactValue pfv "
                        + "on pfv.projectFactDefinitionId = pfd.projectFactDefinitionId and pfv.projectId = :projectId "
                        + "where pfd.workspaceId = :workspaceId ");
        if (!includeInactiveDefinitions) {
            hql.append("and pfd.active = :active ");
        }
        hql.append(
                "order by lower(pfd.factGroup), pfd.displayOrder, lower(pfd.factLabel), pfd.projectFactDefinitionId");

        Query query = session.createQuery(hql.toString());
        query.setInteger("workspaceId", workspaceId);
        query.setInteger("projectId", projectId);
        if (!includeInactiveDefinitions) {
            query.setString("active", "Y");
        }
        return query.list();
    }

    public ProjectFactValue save(ProjectFactValue projectFactValue) {
        session.save(projectFactValue);
        return projectFactValue;
    }

    public void update(ProjectFactValue projectFactValue) {
        session.update(projectFactValue);
    }

    public ProjectFactValue upsertByProjectAndFactDefinition(int projectId, int projectFactDefinitionId,
            ProjectFactValue incomingValue, Integer webUserId, Date now) {
        if (incomingValue == null) {
            throw new IllegalArgumentException("incomingValue is required");
        }
        ProjectFactValue existing = getByProjectAndFactDefinition(projectId, projectFactDefinitionId);
        if (existing == null) {
            incomingValue.setProjectId(projectId);
            incomingValue.setProjectFactDefinitionId(projectFactDefinitionId);
            incomingValue.setCreatedByWebUserId(webUserId);
            incomingValue.setCreatedDate(now);
            incomingValue.setLastModifiedByWebUserId(webUserId);
            incomingValue.setLastModifiedDate(now);
            save(incomingValue);
            return incomingValue;
        }

        existing.setValueBoolean(incomingValue.getValueBoolean());
        existing.setValueText(incomingValue.getValueText());
        existing.setValueDate(incomingValue.getValueDate());
        existing.setValueNumber(incomingValue.getValueNumber());
        existing.setValueCode(incomingValue.getValueCode());
        existing.setNotes(incomingValue.getNotes());
        existing.setLastModifiedByWebUserId(webUserId);
        existing.setLastModifiedDate(now);
        update(existing);
        return existing;
    }

    public void clearByProjectAndFactDefinition(int projectId, int projectFactDefinitionId) {
        Query query = session.createQuery(
                "delete from ProjectFactValue where projectId = :projectId and projectFactDefinitionId = :projectFactDefinitionId");
        query.setInteger("projectId", projectId);
        query.setInteger("projectFactDefinitionId", projectFactDefinitionId);
        query.executeUpdate();
    }
}
