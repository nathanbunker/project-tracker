package org.openimmunizationsoftware.pt.model;

// Generated Dec 12, 2012 3:50:50 AM by Hibernate Tools 3.4.0.CR1

/**
 * ProjectBookmark generated by hbm2java
 */
public class ProjectBookmark implements java.io.Serializable {

  private ProjectBookmarkId id;
  private String bookmarkUrl;

  public ProjectBookmark() {}

  public ProjectBookmark(ProjectBookmarkId id, String bookmarkUrl) {
    this.id = id;
    this.bookmarkUrl = bookmarkUrl;
  }

  public ProjectBookmarkId getId() {
    return this.id;
  }

  public void setId(ProjectBookmarkId id) {
    this.id = id;
  }

  public String getBookmarkUrl() {
    return this.bookmarkUrl;
  }

  public void setBookmarkUrl(String bookmarkUrl) {
    this.bookmarkUrl = bookmarkUrl;
  }

}
