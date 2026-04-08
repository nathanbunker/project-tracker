package org.openimmunizationsoftware.pt.api.v1.service;

import java.util.Date;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.ApiRequestContext;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectActionProposal;

public class ProjectActionProposalService {

    public List<Project> listProjectsVisibleToClient(int workspaceId, String username) {
        Session session = HibernateRequestContext.getCurrentSession();
        StringBuilder hql = new StringBuilder(
                "from Project p where p.workspaceId = :workspaceId");
        if (username != null && username.trim().length() > 0) {
            hql.append(" and p.webUser.username = :username");
        }
        hql.append(" order by p.priorityLevel desc, p.categoryCode, p.projectName");
        Query query = session.createQuery(hql.toString());
        query.setInteger("workspaceId", workspaceId);
        if (username != null && username.trim().length() > 0) {
            query.setString("username", username.trim());
        }
        @SuppressWarnings("unchecked")
        List<Project> results = query.list();
        return results;
    }

    public List<ProjectActionNext> listActionsNextForProject(int workspaceId, int projectId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from ProjectActionNext pan where pan.projectId = :projectId and pan.workspaceId = :workspaceId");
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> results = query.list();
        return results;
    }

    public List<ProjectActionTaken> listActionsTakenForProject(int workspaceId, int projectId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from ProjectActionTaken pat where pat.projectId = :projectId and pat.workspaceId = :workspaceId");
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        @SuppressWarnings("unchecked")
        List<ProjectActionTaken> results = query.list();
        return results;
    }

    public List<ProjectActionProposal> listProposalsForAction(int workspaceId, int actionNextId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "select pap from ProjectActionProposal pap join pap.project p "
                        + "where pap.actionNextId = :actionNextId and p.workspaceId = :workspaceId "
                        + "order by pap.proposalCreateDate desc");
        query.setInteger("actionNextId", actionNextId);
        query.setInteger("workspaceId", workspaceId);
        @SuppressWarnings("unchecked")
        List<ProjectActionProposal> results = query.list();
        return results;
    }

    public ProjectActionProposal createProposal(int workspaceId, String agentName, int projectId,
            Integer actionNextId, String proposedPatchJson, String summary, String rationale,
            Integer contactId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Project project = requireProject(workspaceId, projectId);
        ProjectActionNext action = null;
        if (actionNextId != null) {
            action = requireAction(workspaceId, projectId, actionNextId.intValue());
        }

        int clientId = ApiRequestContext.getCurrentClient().getClientId();
        Date now = new Date();

        supersedeActiveForTarget(workspaceId, clientId, projectId, actionNextId, now);

        ProjectActionProposal proposal = new ProjectActionProposal();
        proposal.setClientId(clientId);
        proposal.setProject(project);
        proposal.setProjectId(projectId);
        proposal.setAction(action);
        proposal.setActionNextId(actionNextId);
        proposal.setContactId(contactId);
        proposal.setProposalStatusString("new");
        proposal.setProposalCreateDate(now);
        proposal.setModelName(agentName);
        proposal.setProposedPatch(proposedPatchJson);
        proposal.setProposedSummary(summary);
        proposal.setProposedRationale(rationale);

        session.save(proposal);
        return proposal;
    }

    public void supersedeProposal(int workspaceId, int proposalId) {
        int clientId = ApiRequestContext.getCurrentClient().getClientId();
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "select pap from ProjectActionProposal pap join pap.project p "
                        + "where pap.proposalId = :proposalId and pap.clientId = :clientId "
                        + "and p.workspaceId = :workspaceId");
        query.setInteger("proposalId", proposalId);
        query.setInteger("clientId", clientId);
        query.setInteger("workspaceId", workspaceId);
        ProjectActionProposal proposal = (ProjectActionProposal) query.uniqueResult();
        if (proposal == null) {
            throw new IllegalStateException("Proposal not found for client.");
        }
        if (!"superseded".equalsIgnoreCase(proposal.getProposalStatusString())) {
            proposal.setProposalStatusString("superseded");
            proposal.setProposalDecideDate(new Date());
            session.update(proposal);
        }
    }

    private Project requireProject(int workspaceId, int projectId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from Project p where p.projectId = :projectId and p.workspaceId = :workspaceId");
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        Project project = (Project) query.uniqueResult();
        if (project == null) {
            throw new IllegalStateException("Project not found for workspace.");
        }
        return project;
    }

    private ProjectActionNext requireAction(int workspaceId, int projectId, int actionNextId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from ProjectActionNext pan where pan.actionNextId = :actionNextId and pan.projectId = :projectId "
                        + "and pan.workspaceId = :workspaceId");
        query.setInteger("actionNextId", actionNextId);
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        ProjectActionNext action = (ProjectActionNext) query.uniqueResult();
        if (action == null) {
            throw new IllegalStateException("Action not found for workspace.");
        }
        return action;
    }

    private void supersedeActiveForTarget(int workspaceId, int clientId, int projectId,
            Integer actionNextId, Date now) {
        Session session = HibernateRequestContext.getCurrentSession();
        StringBuilder hql = new StringBuilder(
                "select pap from ProjectActionProposal pap join pap.project p "
                        + "where pap.clientId = :clientId and p.workspaceId = :workspaceId "
                        + "and pap.proposalStatusString not in (:rejected, :superseded)");
        if (actionNextId != null) {
            hql.append(" and pap.actionNextId = :actionNextId");
        } else {
            hql.append(" and pap.actionNextId is null and pap.projectId = :projectId");
        }
        Query query = session.createQuery(hql.toString());
        query.setInteger("clientId", clientId);
        query.setInteger("workspaceId", workspaceId);
        query.setString("rejected", "rejected");
        query.setString("superseded", "superseded");
        if (actionNextId != null) {
            query.setInteger("actionNextId", actionNextId.intValue());
        } else {
            query.setInteger("projectId", projectId);
        }
        @SuppressWarnings("unchecked")
        List<ProjectActionProposal> proposals = query.list();
        for (ProjectActionProposal proposal : proposals) {
            proposal.setProposalStatusString("superseded");
            proposal.setProposalDecideDate(now);
            session.update(proposal);
        }
    }
}
