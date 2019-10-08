package org.openimmunizationsoftware.pt.model;

// Generated Dec 12, 2012 3:50:50 AM by Hibernate Tools 3.4.0.CR1

public class ProjectCategory implements java.io.Serializable {

  private static final long serialVersionUID = -5051584400121443510L;

  private int projectCategoryId = 0;
  private int projectContactId;
  private String categoryCode = "";
  private String clientName;
  private Integer sortOrder;
  private String visible;
  private String clientAcronym;
  private ProjectProvider provider = null;

  public String getClientName() {
    return this.clientName;
  }

  public String getClientNameForDropdown() {
    if (getCategoryCode().indexOf('-') > 0) {
      return "&nbsp;&nbsp;-&nbsp;&nbsp;" + getClientName();
    }
    return getClientName();
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
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

  public String getClientAcronym() {
    return this.clientAcronym;
  }

  public void setClientAcronym(String clientAcronym) {
    this.clientAcronym = clientAcronym;
  }

  public int getProjectContactId() {
    return projectContactId;
  }

  public void setProjectContactId(int projectContactId) {
    this.projectContactId = projectContactId;
  }

  public String getCategoryCode() {
    return categoryCode;
  }

  public void setCategoryCode(String categoryCode) {
    this.categoryCode = categoryCode;
  }

  public ProjectProvider getProvider() {
    return provider;
  }

  public void setProvider(ProjectProvider provider) {
    this.provider = provider;
  }

  public int getProjectCategoryId() {
    return projectCategoryId;
  }

  public void setProjectCategoryId(int projectCategoryId) {
    this.projectCategoryId = projectCategoryId;
  }

}
