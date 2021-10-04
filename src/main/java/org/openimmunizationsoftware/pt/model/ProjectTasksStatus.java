package org.openimmunizationsoftware.pt.model;

import java.io.Serializable;

public class ProjectTasksStatus implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static String PROGRESSING = "P";
  public static String DELAYED = "D";
  public static String BLOCKED = "B";

  public static String getLabel(String taskStatus) {
    if (taskStatus != null) {
      if (taskStatus.equals(PROGRESSING)) {
        return "Progressing";
      }
      if (taskStatus.equals(DELAYED)) {
        return "Delayed";
      }
      if (taskStatus.equals(BLOCKED)) {
        return "Blocked";
      }
    }
    return "";
  }

  public static String getColor(String taskStatus) {
    if (taskStatus != null) {
      if (taskStatus.equals(PROGRESSING)) {
        return "GreenYellow";
      }
      if (taskStatus.equals(DELAYED)) {
        return "LightYellow";
      }
      if (taskStatus.equals(BLOCKED)) {
        return "OrangeRed";
      }
    }
    return "LightGray";
  }
}

