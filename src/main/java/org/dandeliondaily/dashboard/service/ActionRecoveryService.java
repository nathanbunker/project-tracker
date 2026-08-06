package org.dandeliondaily.dashboard.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionSetType;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;

public class ActionRecoveryService {

    static final long RECOVERY_WINDOW_MILLIS = 60L * 1000L;
    private static final String SESSION_RECENT_ACTION = "DASHBOARD_RECENT_CLOSED_ACTION";

    public static class RecentAction implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int actionNextId;
        private final String description;
        private final String statusLabel;
        private final long expiresAtMillis;

        RecentAction(int actionNextId, String description, String statusLabel, long expiresAtMillis) {
            this.actionNextId = actionNextId;
            this.description = description;
            this.statusLabel = statusLabel;
            this.expiresAtMillis = expiresAtMillis;
        }

        public int getActionNextId() {
            return actionNextId;
        }

        public String getDescription() {
            return description;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public long getExpiresAtMillis() {
            return expiresAtMillis;
        }
    }

    public void remember(AppReq appReq, ActionNext action, ProjectNextActionStatus status) {
        if (appReq == null || action == null || status == null) {
            return;
        }
        String description = action.getNextDescription();
        if (description == null || description.trim().length() == 0) {
            description = "Untitled action";
        }
        String statusLabel = status == ProjectNextActionStatus.COMPLETED ? "Completed" : "Deleted";
        RecentAction recentAction = new RecentAction(action.getActionNextId(), description.trim(), statusLabel,
                System.currentTimeMillis() + RECOVERY_WINDOW_MILLIS);
        appReq.getWebSession().setAttribute(SESSION_RECENT_ACTION, recentAction);
    }

    public RecentAction getRecentAction(AppReq appReq) {
        if (appReq == null) {
            return null;
        }
        HttpSession webSession = appReq.getWebSession();
        Object value = webSession.getAttribute(SESSION_RECENT_ACTION);
        if (!(value instanceof RecentAction)) {
            return null;
        }
        RecentAction recentAction = (RecentAction) value;
        if (!isAvailable(recentAction.getExpiresAtMillis(), System.currentTimeMillis())) {
            webSession.removeAttribute(SESSION_RECENT_ACTION);
            return null;
        }
        return recentAction;
    }

    public ActionNext restore(AppReq appReq, int actionNextId) {
        RecentAction recentAction = getRecentAction(appReq);
        if (recentAction == null || recentAction.getActionNextId() != actionNextId) {
            throw new IllegalArgumentException("This action is no longer available to restore.");
        }

        Session dataSession = appReq.getDataSession();
        ActionNext action = (ActionNext) dataSession.get(ActionNext.class, actionNextId);
        if (action == null) {
            throw new IllegalArgumentException("Action not found.");
        }
        Integer activeWorkspaceId = appReq.getActiveWorkspaceId();
        if (activeWorkspaceId != null && action.getWorkspaceId() != null
                && activeWorkspaceId.intValue() != action.getWorkspaceId().intValue()) {
            throw new IllegalArgumentException("Action is not available for this workspace.");
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            Date today = java.sql.Date.valueOf(appReq.getWebUser().getLocalDateToday());
            Date now = new Date();
            for (ActionNext sibling : resolveSharedActionSiblings(dataSession, action)) {
                applyRestore(sibling, today, now);
                dataSession.update(sibling);
            }
            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }

        appReq.getWebSession().removeAttribute(SESSION_RECENT_ACTION);
        appReq.setCompletingAction(action);
        if (action.getProject() != null) {
            appReq.setProject(action.getProject());
            TimeTracker timeTracker = appReq.getTimeTracker();
            if (timeTracker != null && timeTracker.isRunningClock()) {
                timeTracker.startClock(action.getProject(), action, dataSession);
            }
        }
        return action;
    }

    boolean isAvailable(long expiresAtMillis, long nowMillis) {
        return expiresAtMillis > nowMillis;
    }

    void applyRestore(ActionNext action, Date today, Date now) {
        action.setNextActionStatus(ProjectNextActionStatus.READY);
        action.setNextActionDate(today);
        action.setCompletionOrder(0);
        action.setNextChangeDate(now);
    }

    private List<ActionNext> resolveSharedActionSiblings(Session dataSession, ActionNext selectedAction) {
        List<ActionNext> singleAction = new ArrayList<ActionNext>();
        singleAction.add(selectedAction);
        if (selectedAction.getActionSet() == null
                || selectedAction.getActionSet().getActionSetType() != ActionSetType.SHARED) {
            return singleAction;
        }
        Query siblingQuery = dataSession.createQuery(
                "from ActionNext an where an.actionSet.actionSetId = :actionSetId order by an.actionNextId");
        siblingQuery.setParameter("actionSetId", selectedAction.getActionSet().getActionSetId());
        @SuppressWarnings("unchecked")
        List<ActionNext> siblings = siblingQuery.list();
        return siblings == null || siblings.isEmpty() ? singleAction : siblings;
    }
}