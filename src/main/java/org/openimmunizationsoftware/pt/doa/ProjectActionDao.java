package org.openimmunizationsoftware.pt.doa;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectProvider;

/**
 * Data Access Object for ProjectActionNext and ProjectActionTaken.
 *
 * This centralizes common queries and persistence operations so servlets
 * and resources do not scatter raw HQL/Session usage.
 */
public class ProjectActionDao {

    private final Session session;

    public ProjectActionDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public ProjectActionDao(Session session) {
        this.session = session;
    }

    public List<ProjectActionNext> listNextActionsForProject(String providerId, int projectId) {
        Query query = session.createQuery(
                "from ProjectActionNext pan where pan.projectId = :projectId and pan.provider.providerId = :providerId order by pan.priorityLevel desc, pan.nextActionDate");
        query.setInteger("projectId", projectId);
        query.setString("providerId", providerId);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> results = query.list();
        return results;
    }

    public List<ProjectActionNext> listReadyNextActionsForContact(ProjectProvider provider, int contactId) {
        Query query = session.createQuery(
                "select distinct pan from ProjectActionNext pan "
                        + "left join fetch pan.project "
                        + "left join fetch pan.contact "
                        + "left join fetch pan.nextProjectContact "
                        + "where pan.provider = :provider "
                        + "and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
                        + "and pan.nextDescription <> '' "
                        + "and pan.nextActionStatusString = :nextActionStatus "
                        + "order by pan.nextActionDate, pan.priorityLevel DESC, pan.nextTimeEstimate, pan.nextChangeDate");
        query.setParameter("provider", provider);
        query.setParameter("contactId", contactId);
        query.setParameter("nextContactId", contactId);
        query.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> results = query.list();
        return results;
    }

    public List<ProjectActionNext> listNextActionsForProjectAndContact(String providerId, int projectId,
            int contactId) {
        Query query = session.createQuery(
                "from ProjectActionNext pan where pan.projectId = :projectId and pan.contactId = :contactId and pan.provider.providerId = :providerId order by pan.nextActionDate");
        query.setInteger("projectId", projectId);
        query.setInteger("contactId", contactId);
        query.setString("providerId", providerId);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> results = query.list();
        return results;
    }

    public List<ProjectActionNext> listTemplateActions(String providerId, int projectId) {
        Query query = session.createQuery(
                "from ProjectActionNext pan where pan.projectId = :projectId and pan.provider.providerId = :providerId and pan.nextDescription <> '' order by pan.priorityLevel desc");
        query.setInteger("projectId", projectId);
        query.setString("providerId", providerId);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> results = query.list();
        return results;
    }

    public ProjectActionNext getNextActionById(String providerId, int projectId, int actionNextId) {
        Query query = session.createQuery(
                "from ProjectActionNext pan where pan.actionNextId = :actionNextId and pan.projectId = :projectId and pan.provider.providerId = :providerId");
        query.setInteger("actionNextId", actionNextId);
        query.setInteger("projectId", projectId);
        query.setString("providerId", providerId);
        return (ProjectActionNext) query.uniqueResult();
    }

    public ProjectActionNext getNextActionById(int actionNextId) {
        return (ProjectActionNext) session.get(ProjectActionNext.class, actionNextId);
    }

    public ProjectActionTaken getTakenActionById(int actionTakenId) {
        return (ProjectActionTaken) session.get(ProjectActionTaken.class, actionTakenId);
    }

    public ProjectActionNext saveNextAction(ProjectActionNext action) {
        validateNextActionStatus(action);
        action.setBillable(resolveBillable(action));
        session.save(action);
        return action;
    }

    public ProjectActionTaken saveTakenAction(ProjectActionTaken action) {
        session.save(action);
        return action;
    }

    public void updateNextAction(ProjectActionNext action) {
        validateNextActionStatus(action);
        action.setBillable(resolveBillable(action));
        session.update(action);
    }

    private boolean resolveBillable(ProjectActionNext action) {
        if (action == null) {
            return false;
        }
        Project project = action.getProject();
        if (project == null && action.getProjectId() > 0) {
            project = (Project) session.get(Project.class, action.getProjectId());
        }
        if (project == null || project.getBillCode() == null || project.getBillCode().equals("")) {
            return false;
        }
        BillCode billCode = (BillCode) session.get(BillCode.class, project.getBillCode());
        return billCode != null && "Y".equalsIgnoreCase(billCode.getBillable());
    }

    private void validateNextActionStatus(ProjectActionNext action) {
        if (action == null) {
            throw new IllegalStateException("ProjectActionNext is required.");
        }
        if (action.getNextActionStatus() == null) {
            throw new IllegalStateException("ProjectActionNext status must be set.");
        }
    }

    public void updateTakenAction(ProjectActionTaken action) {
        session.update(action);
    }

    public void deleteNextAction(ProjectActionNext action) {
        session.delete(action);
    }

    public void deleteTakenAction(ProjectActionTaken action) {
        session.delete(action);
    }

    public List<ProjectActionTaken> listTakenActionsForProject(int projectId) {
        Query query = session.createQuery(
                "from ProjectActionTaken pat where pat.projectId = :projectId order by pat.actionDate desc");
        query.setInteger("projectId", projectId);
        @SuppressWarnings("unchecked")
        List<ProjectActionTaken> results = query.list();
        return results;
    }

    public List<ProjectActionTaken> listTakenActionsForProjectAndContact(int projectId, int contactId) {
        Query query = session.createQuery(
                "from ProjectActionTaken pat where pat.projectId = :projectId and pat.contactId = :contactId "
                        + "order by pat.actionDate desc");
        query.setInteger("projectId", projectId);
        query.setInteger("contactId", contactId);
        @SuppressWarnings("unchecked")
        List<ProjectActionTaken> results = query.list();
        return results;
    }

    public boolean hasTakenActionsForProjectSince(int projectId, Date sinceDate) {
        Query query = session.createQuery(
                "select count(pat.actionTakenId) from ProjectActionTaken pat where pat.projectId = :projectId and pat.actionDate >= :sinceDate");
        query.setInteger("projectId", projectId);
        query.setTimestamp("sinceDate", sinceDate);
        Long count = (Long) query.uniqueResult();
        return count != null && count.longValue() > 0;
    }

    public List<ProjectActionNext> listGoalActions() {
        Query query = session.createQuery(
                "select distinct pan from ProjectActionNext pan "
                        + "left join fetch pan.project "
                        + "where pan.nextDescription <> '' "
                        + "and pan.nextActionType = :nextActionType "
                        + "order by pan.priorityLevel desc, pan.projectId, pan.nextActionDate asc");
        query.setString("nextActionType", org.openimmunizationsoftware.pt.model.ProjectNextActionType.GOAL);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> results = query.list();
        return results;
    }

    public List<ProjectActionNext> findNextActionsByTemplate(int templateActionNextId) {
        Query query = session
                .createQuery("from ProjectActionNext pan where pan.templateActionNextId = :templateActionNextId");
        query.setInteger("templateActionNextId", templateActionNextId);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> results = query.list();
        return results;
    }

    public List<ProjectActionNext> findNextActionsDueBetween(int projectId, Date start, Date end) {
        Query query = session.createQuery(
                "from ProjectActionNext pan where pan.projectId = :projectId and pan.nextActionDate between :start and :end order by pan.nextActionDate");
        query.setInteger("projectId", projectId);
        query.setDate("start", start);
        query.setDate("end", end);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> results = query.list();
        return results;
    }

}
