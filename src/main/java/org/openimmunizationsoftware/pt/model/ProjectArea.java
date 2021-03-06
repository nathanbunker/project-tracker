package org.openimmunizationsoftware.pt.model;

// Generated Dec 12, 2012 3:50:50 AM by Hibernate Tools 3.4.0.CR1

/**
 * ProjectArea generated by hbm2java
 */
public class ProjectArea implements java.io.Serializable {

  private static final long serialVersionUID = 3712027998646830301L;

  private int areaId;
  private String areaLabel;
  private String username;
  private Integer sortOrder;
  private String visible;

  public ProjectArea() {}

  public ProjectArea(int areaId, String areaLabel, String username) {
    this.areaId = areaId;
    this.areaLabel = areaLabel;
    this.username = username;
  }

  public ProjectArea(int areaId, String areaLabel, String username, Integer sortOrder,
      String visible) {
    this.areaId = areaId;
    this.areaLabel = areaLabel;
    this.username = username;
    this.sortOrder = sortOrder;
    this.visible = visible;
  }

  public int getAreaId() {
    return this.areaId;
  }

  public void setAreaId(int areaId) {
    this.areaId = areaId;
  }

  public String getAreaLabel() {
    return this.areaLabel;
  }

  public void setAreaLabel(String areaLabel) {
    this.areaLabel = areaLabel;
  }

  public String getUsername() {
    return this.username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Integer getSortOrder() {
    return this.sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public String getVisible() {
    return this.visible;
  }

  public void setVisible(String visible) {
    this.visible = visible;
  }

}
