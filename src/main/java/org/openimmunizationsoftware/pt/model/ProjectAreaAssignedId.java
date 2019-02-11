package org.openimmunizationsoftware.pt.model;

// Generated Dec 12, 2012 3:50:50 AM by Hibernate Tools 3.4.0.CR1

/**
 * ProjectAreaAssignedId generated by hbm2java
 */
public class ProjectAreaAssignedId implements java.io.Serializable
{

  private int areaId;
  private int projectId;

  public ProjectAreaAssignedId() {
  }

  public ProjectAreaAssignedId(int areaId, int projectId) {
    this.areaId = areaId;
    this.projectId = projectId;
  }

  public int getAreaId()
  {
    return this.areaId;
  }

  public void setAreaId(int areaId)
  {
    this.areaId = areaId;
  }

  public int getProjectId()
  {
    return this.projectId;
  }

  public void setProjectId(int projectId)
  {
    this.projectId = projectId;
  }

  public boolean equals(Object other)
  {
    if ((this == other))
      return true;
    if ((other == null))
      return false;
    if (!(other instanceof ProjectAreaAssignedId))
      return false;
    ProjectAreaAssignedId castOther = (ProjectAreaAssignedId) other;

    return (this.getAreaId() == castOther.getAreaId()) && (this.getProjectId() == castOther.getProjectId());
  }

  public int hashCode()
  {
    int result = 17;

    result = 37 * result + this.getAreaId();
    result = 37 * result + this.getProjectId();
    return result;
  }

}
