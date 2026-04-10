package org.dandeliondaily.dashboard.model;

import java.io.Serializable;

public class ProjectDashboardSuggestedAction implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title = "";
    private String description = "";
    private String rationale = "";
    private String suggestedType = "";
    private String suggestedScheduleHint = "";
    private Integer estimateMinutes;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getSuggestedType() {
        return suggestedType;
    }

    public void setSuggestedType(String suggestedType) {
        this.suggestedType = suggestedType;
    }

    public String getSuggestedScheduleHint() {
        return suggestedScheduleHint;
    }

    public void setSuggestedScheduleHint(String suggestedScheduleHint) {
        this.suggestedScheduleHint = suggestedScheduleHint;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }

    public void setEstimateMinutes(Integer estimateMinutes) {
        this.estimateMinutes = estimateMinutes;
    }
}
