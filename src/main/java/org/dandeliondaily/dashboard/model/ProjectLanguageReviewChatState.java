package org.dandeliondaily.dashboard.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProjectLanguageReviewChatState implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ProjectDashboardChatMessage> messages = new ArrayList<ProjectDashboardChatMessage>();
    private String proposedDescription = "";
    private String proposedCurrentFocus = "";
    private String proposedOutcome = "";
    private String proposedSuccessCriteria = "";
    private List<String> followUpQuestions = new ArrayList<String>();

    public List<ProjectDashboardChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ProjectDashboardChatMessage> messages) {
        this.messages = messages;
    }

    public String getProposedDescription() {
        return proposedDescription;
    }

    public void setProposedDescription(String proposedDescription) {
        this.proposedDescription = proposedDescription;
    }

    public String getProposedCurrentFocus() {
        return proposedCurrentFocus;
    }

    public void setProposedCurrentFocus(String proposedCurrentFocus) {
        this.proposedCurrentFocus = proposedCurrentFocus;
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

    public boolean hasStarted() {
        return messages != null && !messages.isEmpty();
    }

    public boolean hasProposals() {
        return isNonEmpty(proposedDescription) || isNonEmpty(proposedCurrentFocus)
                || isNonEmpty(proposedOutcome) || isNonEmpty(proposedSuccessCriteria);
    }

    public void clearProposals() {
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
