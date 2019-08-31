package org.openimmunizationsoftware.pt.model;

import java.util.ArrayList;
import java.util.List;

public class TimeZone {
  private static List<TimeZone> timeZoneList;

  public static List<TimeZone> getTimeZoneList() {
    if (timeZoneList == null) {
      timeZoneList = new ArrayList<TimeZone>();
      timeZoneList.add(new TimeZone("-5", "Eastern"));
      timeZoneList.add(new TimeZone("-6", "Central"));
      timeZoneList.add(new TimeZone("-7", "Mountain"));
      timeZoneList.add(new TimeZone("-8", "Pacific"));
      timeZoneList.add(new TimeZone("-9", "Alaska"));
      timeZoneList.add(new TimeZone("-10", "Hawaii"));
    }
    return timeZoneList;
  }


  private String timeZoneId = "";
  private String timeZoneLabel = "";

  public String getTimeZoneId() {
    return timeZoneId;
  }

  public void setTimeZoneId(String timeZoneId) {
    this.timeZoneId = timeZoneId;
  }

  public String getTimeZoneLabel() {
    return timeZoneLabel;
  }

  public void setTimeZoneLabel(String timeZoneLabel) {
    this.timeZoneLabel = timeZoneLabel;
  }

  public TimeZone(String timeZoneId, String timeZoneLabel) {
    this.timeZoneId = timeZoneId;
    this.timeZoneLabel = timeZoneLabel;
  }


}
