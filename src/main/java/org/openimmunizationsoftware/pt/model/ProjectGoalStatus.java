package org.openimmunizationsoftware.pt.model;

import java.io.Serializable;

public class ProjectGoalStatus implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static String PROGRESSING = "P";
  public static String DELAYED = "D";
  public static String BLOCKED = "B";

  public static String getLabel(String goalStatus) {
    if (goalStatus != null) {
      if (goalStatus.equals(PROGRESSING)) {
        return "Progressing";
      }
      if (goalStatus.equals(DELAYED)) {
        return "Delayed";
      }
      if (goalStatus.equals(BLOCKED)) {
        return "Blocked";
      }
    }
    return "";
  }

  public static String getColor(String goalStatus) {
    if (goalStatus != null) {
      if (goalStatus.equals(PROGRESSING)) {
        return "MediumSeaGreen";
      }
      if (goalStatus.equals(DELAYED)) {
        return "Gold";
      }
      if (goalStatus.equals(BLOCKED)) {
        return "OrangeRed";
      }
    }
    return "LightGray";
  }
}

