package org.openimmunizationsoftware.pt.manager;

import java.util.ArrayList;
import java.util.List;

import org.dandeliondaily.dashboard.model.ProjectDashboardSuggestedAction;
import org.dandeliondaily.dashboard.model.ProjectDashboardSuggestedIssue;
import org.dandeliondaily.dashboard.model.ProjectDashboardSuggestedNarrative;

public class ProjectReviewChatResponse {

    private String assistantMessage = "";
    private String proposedDescription = "";
    private String proposedOutcome = "";
    private String proposedSuccessCriteria = "";
    private List<String> followUpQuestions = new ArrayList<String>();
    private List<ProjectDashboardSuggestedAction> proposedActions = new ArrayList<ProjectDashboardSuggestedAction>();
    private List<ProjectDashboardSuggestedIssue> proposedIssues = new ArrayList<ProjectDashboardSuggestedIssue>();
    private List<ProjectDashboardSuggestedNarrative> proposedNarratives = new ArrayList<ProjectDashboardSuggestedNarrative>();

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public void setAssistantMessage(String assistantMessage) {
        this.assistantMessage = assistantMessage;
    }

    public String getProposedDescription() {
        return proposedDescription;
    }

    public void setProposedDescription(String proposedDescription) {
        this.proposedDescription = proposedDescription;
    }

    public String getProposedOutcome() {
        return proposedOutcome;
    }

    public void setProposedOutcome(String proposedOutcome) {
        this.proposedOutcome = proposedOutcome;
    }

    public String getProposedSuccessCriteria() {
        return proposedSuccessCriteria;
    }

    public void setProposedSuccessCriteria(String proposedSuccessCriteria) {
        this.proposedSuccessCriteria = proposedSuccessCriteria;
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
}
