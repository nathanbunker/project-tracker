package org.openimmunizationsoftware.pt.api.v1.service;

import java.util.Date;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.ApiRequestContext;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionTaken;
import org.openimmunizationsoftware.pt.model.ActionProposal;

public class ActionProposalService {

    public List<Project> listProjectsVisibleToClient(int workspaceId, String username) {
        Session session = HibernateRequestContext.getCurrentSession();
        StringBuilder hql = new StringBuilder(
                "from Project p where p.workspaceId = :workspaceId");
        if (username != null && username.trim().length() > 0) {
            hql.append(" and p.webUser.username = :username");
        }
        hql.append(" order by p.priorityLevel desc, p.projectName");
        Query query = session.createQuery(hql.toString());
        query.setInteger("workspaceId", workspaceId);
        if (username != null && username.trim().length() > 0) {
            query.setString("username", username.trim());
        }
        @SuppressWarnings("unchecked")
        List<Project> results = query.list();
        return results;
    }

    public List<ActionNext> listActionsNextForProject(int workspaceId, int projectId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from ActionNext an where an.projectId = :projectId and an.workspaceId = :workspaceId");
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        @SuppressWarnings("unchecked")
        List<ActionNext> results = query.list();
        return results;
    }

    public List<ActionTaken> listActionsTakenForProject(int workspaceId, int projectId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from ActionTaken atk where atk.projectId = :projectId and atk.workspaceId = :workspaceId");
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        @SuppressWarnings("unchecked")
        List<ActionTaken> results = query.list();
        return results;
    }

    public List<ActionProposal> listProposalsForAction(int workspaceId, int actionNextId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "select ap from ActionProposal ap join ap.project p "
                        + "where ap.actionNextId = :actionNextId and p.workspaceId = :workspaceId "
                        + "order by ap.proposalCreateDate desc");
        query.setInteger("actionNextId", actionNextId);
        query.setInteger("workspaceId", workspaceId);
        @SuppressWarnings("unchecked")
        List<ActionProposal> results = query.list();
        return results;
    }

    public ActionProposal createProposal(int workspaceId, String agentName, int projectId,
            Integer actionNextId, String proposedPatchJson, String summary, String rationale,
            Integer contactId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Project project = requireProject(workspaceId, projectId);
        ActionNext action = null;
        if (actionNextId != null) {
            action = requireAction(workspaceId, projectId, actionNextId.intValue());
        }

        int clientId = ApiRequestContext.getCurrentClient().getClientId();
        Date now = new Date();

        supersedeActiveForTarget(workspaceId, clientId, projectId, actionNextId, now);

        ActionProposal proposal = new ActionProposal();
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
                "select ap from ActionProposal ap join ap.project p "
                        + "where ap.proposalId = :proposalId and ap.clientId = :clientId "
                        + "and p.workspaceId = :workspaceId");
        query.setInteger("proposalId", proposalId);
        query.setInteger("clientId", clientId);
        query.setInteger("workspaceId", workspaceId);
        ActionProposal proposal = (ActionProposal) query.uniqueResult();
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

    private ActionNext requireAction(int workspaceId, int projectId, int actionNextId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from ActionNext an where an.actionNextId = :actionNextId and an.projectId = :projectId "
                        + "and an.workspaceId = :workspaceId");
        query.setInteger("actionNextId", actionNextId);
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        ActionNext action = (ActionNext) query.uniqueResult();
        if (action == null) {
            throw new IllegalStateException("Action not found for workspace.");
        }
        return action;
    }

    private void supersedeActiveForTarget(int workspaceId, int clientId, int projectId,
            Integer actionNextId, Date now) {
        Session session = HibernateRequestContext.getCurrentSession();
        StringBuilder hql = new StringBuilder(
                "select ap from ActionProposal ap join ap.project p "
                        + "where ap.clientId = :clientId and p.workspaceId = :workspaceId "
                        + "and ap.proposalStatusString not in (:rejected, :superseded)");
        if (actionNextId != null) {
            hql.append(" and ap.actionNextId = :actionNextId");
        } else {
            hql.append(" and ap.actionNextId is null and ap.projectId = :projectId");
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
        List<ActionProposal> proposals = query.list();
        for (ActionProposal proposal : proposals) {
            proposal.setProposalStatusString("superseded");
            proposal.setProposalDecideDate(now);
            session.update(proposal);
        }
    }
}
