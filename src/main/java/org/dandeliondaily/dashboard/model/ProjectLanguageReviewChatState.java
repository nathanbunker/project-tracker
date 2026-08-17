package org.dandeliondaily.dashboard.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Session-scoped state for one project's language review thread. A background thread (kicked off by
 * the servlet) mutates this concurrently with the request thread that renders the page or polls status,
 * so all access goes through synchronized methods rather than exposing the backing fields/lists directly.
 */
public class ProjectLanguageReviewChatState implements Serializable {

    private static final long serialVersionUID = 2L;

    private List<ProjectDashboardChatMessage> messages = new ArrayList<ProjectDashboardChatMessage>();
    private String proposedDescription = "";
    private String proposedCurrentFocus = "";
    private String proposedOutcome = "";
    private String proposedSuccessCriteria = "";
    private List<String> followUpQuestions = new ArrayList<String>();
    private boolean pending = false;

    public synchronized List<ProjectDashboardChatMessage> getMessages() {
        return messages;
    }

    public synchronized List<ProjectDashboardChatMessage> snapshotMessages() {
        return new ArrayList<ProjectDashboardChatMessage>(messages);
    }

    public synchronized String getProposedDescription() {
        return proposedDescription;
    }

    public synchronized String getProposedCurrentFocus() {
        return proposedCurrentFocus;
    }

    public synchronized String getProposedOutcome() {
        return proposedOutcome;
    }

    public synchronized String getProposedSuccessCriteria() {
        return proposedSuccessCriteria;
    }

    public synchronized List<String> getFollowUpQuestions() {
        return followUpQuestions;
    }

    public synchronized boolean isPending() {
        return pending;
    }

    public synchronized boolean hasStarted() {
        return messages != null && !messages.isEmpty();
    }

    public synchronized boolean hasProposals() {
        return isNonEmpty(proposedDescription) || isNonEmpty(proposedCurrentFocus)
                || isNonEmpty(proposedOutcome) || isNonEmpty(proposedSuccessCriteria);
    }

    /** Records the user's turn and marks the thread as waiting on a background AI call. */
    public synchronized void appendUserMessageAndBeginPending(String prompt) {
        messages.add(new ProjectDashboardChatMessage("user", prompt));
        pending = true;
    }

    /** Called from the background thread once the AI call succeeds. */
    public synchronized void completePending(String assistantText, String description, String currentFocus,
            String outcome, String successCriteria, List<String> followUps, int maxMessages) {
        messages.add(new ProjectDashboardChatMessage("assistant", assistantText));
        trimMessages(maxMessages);
        this.proposedDescription = description == null ? "" : description;
        this.proposedCurrentFocus = currentFocus == null ? "" : currentFocus;
        this.proposedOutcome = outcome == null ? "" : outcome;
        this.proposedSuccessCriteria = successCriteria == null ? "" : successCriteria;
        this.followUpQuestions = followUps == null ? new ArrayList<String>() : followUps;
        this.pending = false;
    }

    /** Called from the background thread if the AI call fails. */
    public synchronized void failPending(String errorMessage, int maxMessages) {
        messages.add(new ProjectDashboardChatMessage("assistant", errorMessage));
        trimMessages(maxMessages);
        this.pending = false;
    }

    private void trimMessages(int maxMessages) {
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }

    public synchronized void clearProposals() {
        proposedDescription = "";
        proposedCurrentFocus = "";
        proposedOutcome = "";
        proposedSuccessCriteria = "";
        followUpQuestions.clear();
    }

    public static boolean isNonEmpty(String value) {
        return value != null && value.trim().length() > 0;
    }
}
