package org.openimmunizationsoftware.pt.manager;


public class TimeEntry implements Comparable<TimeEntry>
{
  private String label;
  private int minutes;
  private String id;

  public String getId()
  {
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
  }

  public TimeEntry(String label, int minutes, String id) {
    this.label = label;
    this.minutes = minutes;
    this.id = id;
  }

  public String getLabel()
  {
    return label;
  }

  public int getMinutes()
  {
    return minutes;
  }

  public int compareTo(TimeEntry te)
  {
    return te.minutes - this.minutes;
  }

}