package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ProjectTagMap implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private int projectTagMapId;
    private int projectId;
    private int projectTagId;
    private Date createdDate;

    public int getProjectTagMapId() {
        return projectTagMapId;
    }

    public void setProjectTagMapId(int projectTagMapId) {
        this.projectTagMapId = projectTagMapId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getProjectTagId() {
        return projectTagId;
    }

    public void setProjectTagId(int projectTagId) {
        this.projectTagId = projectTagId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
