package org.openimmunizationsoftware.pt.manager;

import java.util.ArrayList;
import java.util.List;

public class ProjectReviewChatResponse {

    private String assistantMessage = "";
    private String proposedDescription = "";
    private String proposedOutcome = "";
    private String proposedSuccessCriteria = "";
    private List<String> followUpQuestions = new ArrayList<String>();

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
}
