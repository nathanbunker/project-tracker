package org.openimmunizationsoftware.pt.model;

// Generated Dec 12, 2012 3:50:50 AM by Hibernate Tools 3.4.0.CR1

/**
 * ProjectNextActionType generated by hbm2java
 */
public class ProjectNextActionType implements java.io.Serializable {

  private static final long serialVersionUID = -7007038930961162968L;

  public static String WILL = "D";
  public static String WILL_CONTACT = "C";
  public static String WAITING = "W";
  public static String WILL_RUN_ERRAND = "E";
  public static String COMMITTED_TO = "T";
  public static String ASKS_TO = "A";
  public static String MIGHT = "M";
  public static String GOAL = "G";
  public static String OVERDUE_TO = "O";
  public static String WILL_MEET = "B";

  public static String getLabel(String projectNextActionType) {
    if (projectNextActionType.equals(WILL)) {
      return "Will";
    } else if (projectNextActionType.equals(WILL_CONTACT)) {
      return "Will Contact";
    } else if (projectNextActionType.equals(WAITING)) {
      return "Waiting";
    } else if (projectNextActionType.equals(WILL_RUN_ERRAND)) {
      return "Will Run Errand";
    } else if (projectNextActionType.equals(COMMITTED_TO)) {
      return "Committed To";
    } else if (projectNextActionType.equals(ASKS_TO)) {
      return "Will Ask";
    } else if (projectNextActionType.equals(MIGHT)) {
      return "Might";
    } else if (projectNextActionType.equals(GOAL)) {
      return "Goal";
    } else if (projectNextActionType.equals(OVERDUE_TO)) {
      return "Overdue";
    }
    return projectNextActionType;
  }

  private String nextActionType;
  private String nextActionLabel;

  public ProjectNextActionType() {}

  public ProjectNextActionType(String nextActionType, String nextActionLabel) {
    this.nextActionType = nextActionType;
    this.nextActionLabel = nextActionLabel;
  }

  public String getNextActionType() {
    return this.nextActionType;
  }

  public void setNextActionType(String nextActionType) {
    this.nextActionType = nextActionType;
  }

  public String getNextActionLabel() {
    return this.nextActionLabel;
  }

  public void setNextActionLabel(String nextActionLabel) {
    this.nextActionLabel = nextActionLabel;
  }

}
