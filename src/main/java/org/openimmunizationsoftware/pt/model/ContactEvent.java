package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class ContactEvent
{
  public static final String EVENT_TYPE_BIRTH_DAY = "B";
  public static final String EVENT_TYPE_WEDDING_DAY = "W";
  public static final String EVENT_TYPE_OTHER = "O";

  public static final String[] EVENT_TYPE = { EVENT_TYPE_BIRTH_DAY, EVENT_TYPE_WEDDING_DAY, EVENT_TYPE_OTHER };

  public static String getEventTypeLabel(String eventType)
  {
    if (eventType.equals(EVENT_TYPE_BIRTH_DAY))
    {
      return "Birth Day";
    } else if (eventType.equals(EVENT_TYPE_WEDDING_DAY))
    {
      return "Wedding Day";
    } else if (eventType.equals(EVENT_TYPE_OTHER))
    {
      return "Other";
    }
    return eventType;
  }

  private int eventId = 0;
  private ProjectContact projectContact = null;
  private String eventType = "";
  private int eventNum = 0;
  private Date eventDate = null;

  public int getEventId()
  {
    return eventId;
  }

  public void setEventId(int eventId)
  {
    this.eventId = eventId;
  }

  public ProjectContact getProjectContact()
  {
    return projectContact;
  }

  public void setProjectContact(ProjectContact projectContact)
  {
    this.projectContact = projectContact;
  }

  public String getEventType()
  {
    return eventType;
  }

  public String getEventTypeLabel()
  {
    return getEventTypeLabel(eventType);
  }

  public void setEventType(String eventType)
  {
    this.eventType = eventType;
  }

  public int getEventNum()
  {
    return eventNum;
  }

  public void setEventNum(int eventNum)
  {
    this.eventNum = eventNum;
  }

  public Date getEventDate()
  {
    return eventDate;
  }

  public void setEventDate(Date eventDate)
  {
    this.eventDate = eventDate;
  }

}
