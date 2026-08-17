package org.dandeliondaily.dashboard.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Session-scoped state for one project's "Next Actions" suggestion thread. A background thread (kicked
 * off by the servlet) mutates this concurrently with the request thread that renders the page or polls
 * status, so all access goes through synchronized methods rather than exposing the backing lists directly.
 */
public class ProjectNextActionsChatState implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ProjectDashboardChatMessage> messages = new ArrayList<ProjectDashboardChatMessage>();
    private List<ProjectDashboardSuggestedAction> proposedActions = new ArrayList<ProjectDashboardSuggestedAction>();
    private List<String> followUpQuestions = new ArrayList<String>();
    private boolean pending = false;

    public synchronized List<ProjectDashboardChatMessage> snapshotMessages() {
        return new ArrayList<ProjectDashboardChatMessage>(messages);
    }

    public synchronized List<ProjectDashboardSuggestedAction> getProposedActions() {
        return proposedActions;
    }

    public synchronized List<String> getFollowUpQuestions() {
        return followUpQuestions;
    }

    public synchronized boolean isPending() {
        return pending;
    }

    public synchronized boolean hasProposals() {
        return proposedActions != null && !proposedActions.isEmpty();
    }

    /** Records the user's turn and marks the thread as waiting on a background AI call. */
    public synchronized void appendUserMessageAndBeginPending(String prompt) {
        messages.add(new ProjectDashboardChatMessage("user", prompt));
        pending = true;
    }

    /** Called from the background thread once the AI call succeeds. Replaces the whole proposed batch. */
    public synchronized void completePending(String assistantText, List<ProjectDashboardSuggestedAction> actions,
            List<String> followUps, int maxMessages) {
        messages.add(new ProjectDashboardChatMessage("assistant", assistantText));
        trimMessages(maxMessages);
        this.proposedActions = actions == null ? new ArrayList<ProjectDashboardSuggestedAction>() : actions;
        this.followUpQuestions = followUps == null ? new ArrayList<String>() : followUps;
        this.pending = false;
    }

    /** Called from the background thread if the AI call fails. */
    public synchronized void failPending(String errorMessage, int maxMessages) {
        messages.add(new ProjectDashboardChatMessage("assistant", errorMessage));
        trimMessages(maxMessages);
        this.pending = false;
    }

    /** Removes one adopted suggestion so it can't be double-adopted; the rest of the batch is unaffected. */
    public synchronized ProjectDashboardSuggestedAction removeProposedAction(int index) {
        if (proposedActions == null || index < 0 || index >= proposedActions.size()) {
            return null;
        }
        return proposedActions.remove(index);
    }

    public synchronized void clearProposals() {
        proposedActions.clear();
        followUpQuestions.clear();
    }

    private void trimMessages(int maxMessages) {
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }
}
