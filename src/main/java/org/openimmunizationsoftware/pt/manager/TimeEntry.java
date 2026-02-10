package org.openimmunizationsoftware.pt.manager;

import java.util.List;

import org.openimmunizationsoftware.pt.model.ProjectActionTaken;

public class TimeEntry implements Comparable<TimeEntry> {
  private String label;
  private int minutes;
  private String id;
  private List<ProjectActionTaken> projectActionList;
  private int minutesAdjusted = 0;

  public int getMinutesAdjusted() {
    return minutesAdjusted;
  }

  public void setMinutesAdjusted(int minutesAdjusted) {
    this.minutesAdjusted = minutesAdjusted;
  }

  public List<ProjectActionTaken> getProjectActionList() {
    return projectActionList;
  }

  public void setProjectActionList(List<ProjectActionTaken> projectActionList) {
    this.projectActionList = projectActionList;
  }

  public String getId() {
    return id;
  }

  public TimeEntry(String label, int minutes) {
    this.label = label;
    this.minutes = minutes;
  }

  public TimeEntry(String label, int minutes, int id) {
    this.label = label;
    this.minutes = minutes;
    this.id = String.valueOf(id);
    this.minutesAdjusted = minutes;

    if (this.minutes > 0) {
      minutesAdjusted = adjustMinutes(minutes);
    }

  }

  public static int adjustMinutes(int minutes) {
    int minutesAdjusted = minutes;
    if (minutes > 0) {
      int m = minutesAdjusted % 30;
      if (m < 7) {
        minutesAdjusted -= m;
      } else {
        minutesAdjusted += 30 - m;
      }
    }
    return minutesAdjusted;
  }

  public TimeEntry(String label, int minutes, String id) {
    this.label = label;
    this.minutes = minutes;
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public int getMinutes() {
    return minutes;
  }

  public int compareTo(TimeEntry te) {
    return te.minutes - this.minutes;
  }

}
