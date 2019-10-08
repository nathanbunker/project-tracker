package org.openimmunizationsoftware.pt.model;

// Generated Dec 12, 2012 3:50:50 AM by Hibernate Tools 3.4.0.CR1

import java.util.Calendar;
import java.util.Date;

/**
 * ReportSchedule generated by hbm2java
 */
public class ReportSchedule implements java.io.Serializable {

  private static final long serialVersionUID = -6449413293787916523L;

  public static final String STATUS_NEW = "N";
  public static final String STATUS_STOPPED = "S";
  public static final String STATUS_QUEUED = "Q";
  public static final String STATUS_RUNNING = "R";
  public static final String STATUS_DISABLED = "D";
  public static final String STATUS_ERRORED = "E";
  public static final String STATUS_WAITING = "W";

  public static final String[][] STATUSES = {{STATUS_NEW, "New"}, {STATUS_QUEUED, "Queued"},
      {STATUS_DISABLED, "Disabled"}, {STATUS_ERRORED, "Errored"}, {STATUS_STOPPED, "Stopped"},
      {STATUS_WAITING, "Waiting for Trigger"}, {STATUS_RUNNING, "Running"},};

  public static final String METHOD_TCPIP = "T";
  public static final String METHOD_FILE = "F";
  public static final String METHOD_HTTPS = "H";
  public static final String METHOD_EMAIL = "E";
  public static final String METHOD_NONE = "N";

  public boolean isMethodTcpip() {
    return method.equals(METHOD_TCPIP);
  }

  public boolean isMethodFile() {
    return method.equals(METHOD_FILE);
  }

  public boolean isMethodHttps() {
    return method.equals(METHOD_HTTPS);
  }

  public boolean isMethodEmail() {
    return method.equals(METHOD_EMAIL);
  }

  public boolean isMethodNone() {
    return method.equals(METHOD_NONE);
  }



  public boolean canRun() {

    if (PERIOD_EVERY_DAY.equals(period)) {
      return true;
    }
    Calendar today = Calendar.getInstance();

    int dayInWeek = today.get(Calendar.DAY_OF_WEEK);
    if (PERIOD_EVERY_WORKING_DAY.equals(period)) {
      return dayInWeek >= 3;
    }
    if (PERIOD_EVERY_SUNDAY.equals(period)) {
      return dayInWeek == 1;
    } else if (PERIOD_EVERY_MONDAY.equals(period)) {
      return dayInWeek == 2;
    } else if (PERIOD_EVERY_TUESDAY.equals(period)) {
      return dayInWeek == 3;
    } else if (PERIOD_EVERY_WEDNESDAY.equals(period)) {
      return dayInWeek == 4;
    } else if (PERIOD_EVERY_THURSDAY.equals(period)) {
      return dayInWeek == 5;
    } else if (PERIOD_EVERY_FRIDAY.equals(period)) {
      return dayInWeek == 6;
    } else if (PERIOD_EVERY_SATURDAY.equals(period)) {
      return dayInWeek == 7;
    }
    int dayInMonth = (today.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1;
    if (PERIOD_FIRST_SATURDAY.equals(period)) {
      return dayInMonth == 1 && dayInWeek == 7;
    }
    if (PERIOD_SECOND_SATURDAY.equals(period)) {
      return dayInMonth == 2 && dayInWeek == 7;
    }
    if (PERIOD_THIRD_SATURDAY.equals(period)) {
      return dayInMonth == 3 && dayInWeek == 7;
    }
    if (PERIOD_FOURTH_SATURDAY.equals(period)) {
      return dayInMonth == 4 && dayInWeek == 7;
    }
    if (PERIOD_LAST_SATURDAY.equals(period)) {
      int thisMonth = today.get(Calendar.MONTH);
      today.add(Calendar.DAY_OF_MONTH, 7);
      return dayInWeek == 7 && thisMonth != today.get(Calendar.MONTH);
    }
    if (PERIOD_LAST_DAY_OF_MONTH.equals(period)) {
      int thisMonth = today.get(Calendar.MONTH);
      today.add(Calendar.DAY_OF_MONTH, 1);
      return thisMonth != today.get(Calendar.MONTH);
    }
    if (PERIOD_FIRST_DAY_OF_MONTH.equals(period)) {
      int thisMonth = today.get(Calendar.MONTH);
      today.add(Calendar.DAY_OF_MONTH, -1);
      return thisMonth != today.get(Calendar.MONTH);
    }
    if (PERIOD_FIRST_MONTH.equals(period)) {
      int thisMonth = today.get(Calendar.MONTH);
      return thisMonth == 1 && dayInMonth == 1 && dayInWeek == 7;
    }
    if (PERIOD_EVERY_MONTH.equals(period)) {
      return dayInMonth == 1 && dayInWeek == 7;
    }
    return false;
  }

  public static final String REPORT_CONTINUOUS = "C";
  public static final String REPORT_DAILY = "D";
  public static final String REPORT_WEEKLY = "W";
  public static final String REPORT_MONTHLY = "M";
  public static final String REPORT_YEARLY = "Y";

  // Continuous Reports
  public static final String PERIOD_EVERY_2_MINS = REPORT_CONTINUOUS + "A";
  public static final String PERIOD_EVERY_5_MINS = REPORT_CONTINUOUS + "B";
  public static final String PERIOD_EVERY_10_MINS = REPORT_CONTINUOUS + "1";
  public static final String PERIOD_EVERY_15_MINS = REPORT_CONTINUOUS + "5";
  public static final String PERIOD_EVERY_20_MINS = REPORT_CONTINUOUS + "2";
  public static final String PERIOD_EVERY_30_MINS = REPORT_CONTINUOUS + "3";
  public static final String PERIOD_EVERY_40_MINS = REPORT_CONTINUOUS + "4";
  public static final String PERIOD_EVERY_60_MINS = REPORT_CONTINUOUS + "6";

  // Daily Reports
  public static final String PERIOD_EVERY_DAY = REPORT_DAILY + "D";
  public static final String PERIOD_EVERY_WORKING_DAY = REPORT_DAILY + "W";

  // Weekly Reports
  public static final String PERIOD_EVERY_SUNDAY = REPORT_WEEKLY + "1";
  public static final String PERIOD_EVERY_MONDAY = REPORT_WEEKLY + "2";
  public static final String PERIOD_EVERY_TUESDAY = REPORT_WEEKLY + "3";
  public static final String PERIOD_EVERY_WEDNESDAY = REPORT_WEEKLY + "4";
  public static final String PERIOD_EVERY_THURSDAY = REPORT_WEEKLY + "5";
  public static final String PERIOD_EVERY_FRIDAY = REPORT_WEEKLY + "6";
  public static final String PERIOD_EVERY_SATURDAY = REPORT_WEEKLY + "7";

  // Monthly Reports
  public static final String PERIOD_FIRST_SATURDAY = REPORT_MONTHLY + "1";
  public static final String PERIOD_SECOND_SATURDAY = REPORT_MONTHLY + "2";
  public static final String PERIOD_THIRD_SATURDAY = REPORT_MONTHLY + "3";
  public static final String PERIOD_FOURTH_SATURDAY = REPORT_MONTHLY + "4";
  public static final String PERIOD_LAST_SATURDAY = REPORT_MONTHLY + "5";
  public static final String PERIOD_LAST_DAY_OF_MONTH = REPORT_MONTHLY + "L";
  public static final String PERIOD_FIRST_DAY_OF_MONTH = REPORT_MONTHLY + "F";

  // Yearly Reports
  public static final String PERIOD_FIRST_MONTH = REPORT_YEARLY + "1";
  public static final String PERIOD_EVERY_MONTH = REPORT_YEARLY + "M";

  public static final String[][] PERIODS = {{PERIOD_EVERY_2_MINS, "Every 2 minutes"},
      {PERIOD_EVERY_5_MINS, "Every 5 minutes"}, {PERIOD_EVERY_10_MINS, "Every 10 minutes"},
      {PERIOD_EVERY_15_MINS, "Every 15 minutes"}, {PERIOD_EVERY_20_MINS, "Every 20 minutes"},
      {PERIOD_EVERY_30_MINS, "Every 30 minutes"}, {PERIOD_EVERY_40_MINS, "Every 40 minutes"},
      {PERIOD_EVERY_60_MINS, "Every 60 minutes"}, {PERIOD_EVERY_DAY, "Every day"},
      {PERIOD_EVERY_WORKING_DAY, "Every working day"}, {PERIOD_EVERY_SUNDAY, "Every Sunday"},
      {PERIOD_EVERY_MONDAY, "Every Monday"}, {PERIOD_EVERY_TUESDAY, "Every Tuesday"},
      {PERIOD_EVERY_WEDNESDAY, "Every Wednesday"}, {PERIOD_EVERY_THURSDAY, "Every Thursday"},
      {PERIOD_EVERY_FRIDAY, "Every Friday"}, {PERIOD_EVERY_SATURDAY, "Every Saturday"},
      {PERIOD_FIRST_SATURDAY, "First Saturday of every month"},
      {PERIOD_SECOND_SATURDAY, "Second Saturday of every month"},
      {PERIOD_THIRD_SATURDAY, "Third Saturday of every month"},
      {PERIOD_FOURTH_SATURDAY, "Fourth Saturday of every month"},
      {PERIOD_LAST_SATURDAY, "Last Saturday of every month"},
      {PERIOD_LAST_DAY_OF_MONTH, "Last day of every month"},
      {PERIOD_FIRST_DAY_OF_MONTH, "First day of every month"},
      {PERIOD_FIRST_MONTH, "First month of every year"}};

  public String getPeriodText() {
    for (int i = 0; i < PERIODS.length; i++) {
      if (PERIODS[i][0].equals(period)) {
        return PERIODS[i][1];
      }
    }
    return "Unknown (" + period + ")";
  }

  public String getMethodText() {
    if (method.equals(METHOD_FILE)) {
      return "File";
    } else if (method.equals(METHOD_HTTPS)) {
      return "HTTPS";
    } else if (method.equals(METHOD_TCPIP)) {
      return "TCP/IP";
    } else if (method.equals(METHOD_EMAIL)) {
      return "Email";
    } else if (method.equals(METHOD_NONE)) {
      return "None";
    }
    return "Unknown (" + method + ")";
  }

  public String getStatusText() {
    if (status.equals(STATUS_NEW)) {
      return "New";
    } else if (status.equals(STATUS_STOPPED)) {
      return "Stopped";
    } else if (status.equals(STATUS_QUEUED)) {
      return "Queued";
    } else if (status.equals(STATUS_RUNNING)) {
      return "Running";
    } else if (status.equals(STATUS_DISABLED)) {
      return "Disabled";
    } else if (status.equals(STATUS_ERRORED)) {
      return "Errored";
    } else if (status.equals(STATUS_WAITING)) {
      return "Waiting for Trigger";
    }
    return "Unknown (" + status + ")";
  }


  public boolean isContinuous() {
    return period.startsWith(REPORT_CONTINUOUS);
  }

  public boolean isDaily() {
    return period.startsWith(REPORT_DAILY);
  }

  public boolean isWeekly() {
    return period.startsWith("D");
  }

  public boolean isMonthly() {
    return period.startsWith("W");
  }

  public boolean isYearly() {
    return period.startsWith("M");
  }

  public static final String DATE_START_DEFAULT = "01/01/1970";


  private int profileId;
  private Date dateStart;
  private String method;
  private String period;
  private String location;
  private String status;
  private String name;

  public ReportSchedule() {}

  public ReportSchedule(int profileId, Date dateStart, String method, String period) {
    this.profileId = profileId;
    this.dateStart = dateStart;
    this.method = method;
    this.period = period;
  }

  public ReportSchedule(int profileId, Date dateStart, String method, String period,
      String location, String status, String name) {
    this.profileId = profileId;
    this.dateStart = dateStart;
    this.method = method;
    this.period = period;
    this.location = location;
    this.status = status;
    this.name = name;
  }

  public int getProfileId() {
    return this.profileId;
  }

  public void setProfileId(int profileId) {
    this.profileId = profileId;
  }

  public Date getDateStart() {
    return this.dateStart;
  }

  public void setDateStart(Date dateStart) {
    this.dateStart = dateStart;
  }

  public String getMethod() {
    return this.method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getPeriod() {
    return this.period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }

  public String getLocation() {
    return this.location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getStatus() {
    return this.status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getContinuouslyScheduledMinutes() {
    if (PERIOD_EVERY_2_MINS.equals(period)) {
      return 2;
    }
    if (PERIOD_EVERY_5_MINS.equals(period)) {
      return 5;
    }
    if (PERIOD_EVERY_10_MINS.equals(period)) {
      return 10;
    }
    if (PERIOD_EVERY_15_MINS.equals(period)) {
      return 15;
    }
    if (PERIOD_EVERY_20_MINS.equals(period)) {
      return 20;
    }
    if (PERIOD_EVERY_30_MINS.equals(period)) {
      return 30;
    }
    if (PERIOD_EVERY_40_MINS.equals(period)) {
      return 40;
    }
    if (PERIOD_EVERY_60_MINS.equals(period)) {
      return 60;
    }
    return -1;
  }

  public boolean isStatusNew() {
    return status.equals(STATUS_NEW);
  }

  public boolean isStatusStopped() {
    return status.equals(STATUS_STOPPED);
  }

  public boolean isStatusQueued() {
    return status.equals(STATUS_QUEUED);
  }

  public boolean isStatusWaiting() {
    return status.equals(STATUS_WAITING);
  }

  public boolean isStatusRunning() {
    return status.equals(STATUS_RUNNING);
  }

  public boolean isStatusDisabled() {
    return status.equals(STATUS_DISABLED);
  }

  public boolean isStatusErrored() {
    return status.equals(STATUS_ERRORED);
  }

}
