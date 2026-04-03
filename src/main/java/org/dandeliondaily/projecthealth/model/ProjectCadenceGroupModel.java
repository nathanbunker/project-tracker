package org.dandeliondaily.projecthealth.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectCadenceGroupModel {

    private String groupKey = "";
    private String groupLabel = "";
    private List<ProjectListItemModel> projects = new ArrayList<ProjectListItemModel>();

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public void setGroupLabel(String groupLabel) {
        this.groupLabel = groupLabel;
    }

    public List<ProjectListItemModel> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectListItemModel> projects) {
        this.projects = projects;
    }
}
