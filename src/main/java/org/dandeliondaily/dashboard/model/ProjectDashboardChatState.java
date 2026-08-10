package org.dandeliondaily.dashboard.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProjectDashboardChatState implements Serializable {

    private static final long serialVersionUID = 2L;

    private List<ProjectDashboardChatMessage> messages = new ArrayList<ProjectDashboardChatMessage>();
    private List<String> followUpQuestions = new ArrayList<String>();
    private List<ProjectDashboardSuggestedAction> proposedActions = new ArrayList<ProjectDashboardSuggestedAction>();
    private List<ProjectDashboardSuggestedIssue> proposedIssues = new ArrayList<ProjectDashboardSuggestedIssue>();
    private List<ProjectDashboardSuggestedNarrative> proposedNarratives = new ArrayList<ProjectDashboardSuggestedNarrative>();

    public List<ProjectDashboardChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ProjectDashboardChatMessage> messages) {
        this.messages = messages;
    }

    public List<String> getFollowUpQuestions() {
        return followUpQuestions;
    }

    public void setFollowUpQuestions(List<String> followUpQuestions) {
        this.followUpQuestions = followUpQuestions;
    }

    public List<ProjectDashboardSuggestedAction> getProposedActions() {
        return proposedActions;
    }

    public void setProposedActions(List<ProjectDashboardSuggestedAction> proposedActions) {
        this.proposedActions = proposedActions;
    }

    public List<ProjectDashboardSuggestedIssue> getProposedIssues() {
        return proposedIssues;
    }

    public void setProposedIssues(List<ProjectDashboardSuggestedIssue> proposedIssues) {
        this.proposedIssues = proposedIssues;
    }

    public List<ProjectDashboardSuggestedNarrative> getProposedNarratives() {
        return proposedNarratives;
    }

    public void setProposedNarratives(List<ProjectDashboardSuggestedNarrative> proposedNarratives) {
        this.proposedNarratives = proposedNarratives;
    }

    public boolean hasSuggestions() {
        return (proposedActions != null && !proposedActions.isEmpty())
                || (proposedIssues != null && !proposedIssues.isEmpty())
                || (proposedNarratives != null && !proposedNarratives.isEmpty());
    }

    public void clearSuggestions() {
        followUpQuestions.clear();
        proposedActions.clear();
        proposedIssues.clear();
        proposedNarratives.clear();
    }

    public static boolean isNonEmpty(String value) {
        return value != null && value.trim().length() > 0;
    }
}
