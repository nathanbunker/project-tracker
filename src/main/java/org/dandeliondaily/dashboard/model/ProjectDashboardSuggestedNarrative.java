package org.dandeliondaily.dashboard.model;

import java.io.Serializable;

public class ProjectDashboardSuggestedNarrative implements Serializable {

    private static final long serialVersionUID = 1L;

    private String verb = "";
    private String text = "";
    private String rationale = "";

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
