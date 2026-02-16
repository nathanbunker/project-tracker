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

    public List<Project> listProjectsVisibleToClient(String providerId, String username) {
        Session session = HibernateRequestContext.getCurrentSession();
        StringBuilder hql = new StringBuilder(
                "from Project p where p.provider.providerId = :providerId");
        if (username != null && username.trim().length() > 0) {
            hql.append(" and p.username = :username");
        }
        hql.append(" order by p.priorityLevel desc, p.categoryCode, p.projectName");
        Query query = session.createQuery(hql.toString());
        query.setString("providerId", providerId);
        if (username != null && username.trim().length() > 0) {
            query.setString("username", username.trim());
        }
        @SuppressWarnings("unchecked")
        List<Project> results = query.list();
        return results;
    }

    public List<ProjectActionNext> listActionsNextForProject(String providerId, int projectId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from ProjectActionNext pan where pan.projectId = :projectId and pan.provider.providerId = :providerId");
        query.setInteger("projectId", projectId);
        query.setString("providerId", providerId);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> results = query.list();
        return results;
    }

    public List<ProjectActionTaken> listActionsTakenForProject(String providerId, int projectId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from ProjectActionTaken pat where pat.projectId = :projectId and pat.provider.providerId = :providerId");
        query.setInteger("projectId", projectId);
        query.setString("providerId", providerId);
        @SuppressWarnings("unchecked")
        List<ProjectActionTaken> results = query.list();
        return results;
    }

    public List<ProjectActionProposal> listProposalsForAction(String providerId, int actionId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "select pap from ProjectActionProposal pap join pap.project p "
                        + "where pap.actionId = :actionId and p.provider.providerId = :providerId "
                        + "order by pap.proposalCreateDate desc");
        query.setInteger("actionId", actionId);
        query.setString("providerId", providerId);
        @SuppressWarnings("unchecked")
        List<ProjectActionProposal> results = query.list();
        return results;
    }

    public ProjectActionProposal createProposal(String providerId, String agentName, int projectId,
            Integer actionId, String proposedPatchJson, String summary, String rationale,
            Integer contactId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Project project = requireProject(providerId, projectId);
        ProjectActionNext action = null;
        if (actionId != null) {
            action = requireAction(providerId, projectId, actionId.intValue());
        }

        int clientId = ApiRequestContext.getCurrentClient().getClientId();
        Date now = new Date();

        supersedeActiveForTarget(providerId, clientId, projectId, actionId, now);

        ProjectActionProposal proposal = new ProjectActionProposal();
        proposal.setClientId(clientId);
        proposal.setProject(project);
        proposal.setProjectId(projectId);
        proposal.setAction(action);
        proposal.setActionId(actionId);
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

    public void supersedeProposal(String providerId, int proposalId) {
        int clientId = ApiRequestContext.getCurrentClient().getClientId();
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "select pap from ProjectActionProposal pap join pap.project p "
                        + "where pap.proposalId = :proposalId and pap.clientId = :clientId "
                        + "and p.provider.providerId = :providerId");
        query.setInteger("proposalId", proposalId);
        query.setInteger("clientId", clientId);
        query.setString("providerId", providerId);
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

    private Project requireProject(String providerId, int projectId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from Project p where p.projectId = :projectId and p.provider.providerId = :providerId");
        query.setInteger("projectId", projectId);
        query.setString("providerId", providerId);
        Project project = (Project) query.uniqueResult();
        if (project == null) {
            throw new IllegalStateException("Project not found for provider.");
        }
        return project;
    }

    private ProjectActionNext requireAction(String providerId, int projectId, int actionId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from ProjectActionNext pan where pan.actionNextId = :actionId and pan.projectId = :projectId "
                        + "and pan.provider.providerId = :providerId");
        query.setInteger("actionId", actionId);
        query.setInteger("projectId", projectId);
        query.setString("providerId", providerId);
        ProjectActionNext action = (ProjectActionNext) query.uniqueResult();
        if (action == null) {
            throw new IllegalStateException("Action not found for provider.");
        }
        return action;
    }

    private void supersedeActiveForTarget(String providerId, int clientId, int projectId,
            Integer actionId, Date now) {
        Session session = HibernateRequestContext.getCurrentSession();
        StringBuilder hql = new StringBuilder(
                "select pap from ProjectActionProposal pap join pap.project p "
                        + "where pap.clientId = :clientId and p.provider.providerId = :providerId "
                        + "and pap.proposalStatusString not in (:rejected, :superseded)");
        if (actionId != null) {
            hql.append(" and pap.actionId = :actionId");
        } else {
            hql.append(" and pap.actionId is null and pap.projectId = :projectId");
        }
        Query query = session.createQuery(hql.toString());
        query.setInteger("clientId", clientId);
        query.setString("providerId", providerId);
        query.setString("rejected", "rejected");
        query.setString("superseded", "superseded");
        if (actionId != null) {
            query.setInteger("actionId", actionId.intValue());
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
