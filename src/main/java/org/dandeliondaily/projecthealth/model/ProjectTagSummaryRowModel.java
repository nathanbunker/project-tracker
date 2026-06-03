package org.dandeliondaily.projecthealth.model;

public class ProjectTagSummaryRowModel {

    private String tagKey;
    private String tagLabel;
    private int activeProjectCount;

    public String getTagKey() {
        return tagKey;
    }

    public void setTagKey(String tagKey) {
        this.tagKey = tagKey;
    }

    public String getTagLabel() {
        return tagLabel;
    }

    public void setTagLabel(String tagLabel) {
        this.tagLabel = tagLabel;
    }

    public int getActiveProjectCount() {
        return activeProjectCount;
    }

    public void setActiveProjectCount(int activeProjectCount) {
        this.activeProjectCount = activeProjectCount;
    }
}
