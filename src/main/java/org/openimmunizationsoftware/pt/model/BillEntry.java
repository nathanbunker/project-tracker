package org.openimmunizationsoftware.pt.model;

// Generated Dec 12, 2012 3:50:50 AM by Hibernate Tools 3.4.0.CR1

import java.util.Date;

/**
 * BillEntry generated by hbm2java
 */
public class BillEntry implements java.io.Serializable
{

  private int billId;
  private int projectId;
  private String clientCode;
  private String username;
  private Date startTime;
  private Date endTime;
  private Integer billMins;
  private String billable;
  private String billCode;
  private String providerId;

  public BillEntry() {
  }

  public BillEntry(int billId, int projectId, String clientCode, String username, Date startTime, Date endTime, String billable, String providerId) {
    this.billId = billId;
    this.projectId = projectId;
    this.clientCode = clientCode;
    this.username = username;
    this.startTime = startTime;
    this.endTime = endTime;
    this.billable = billable;
    this.providerId = providerId;
  }

  public BillEntry(int billId, int projectId, String clientCode, String username, Date startTime, Date endTime, Integer billMins, String billable,
      String billCode, String providerId) {
    this.billId = billId;
    this.projectId = projectId;
    this.clientCode = clientCode;
    this.username = username;
    this.startTime = startTime;
    this.endTime = endTime;
    this.billMins = billMins;
    this.billable = billable;
    this.billCode = billCode;
    this.providerId = providerId;
  }

  public int getBillId()
  {
    return this.billId;
  }

  public void setBillId(int billId)
  {
    this.billId = billId;
  }

  public int getProjectId()
  {
    return this.projectId;
  }

  public void setProjectId(int projectId)
  {
    this.projectId = projectId;
  }

  public String getClientCode()
  {
    return this.clientCode;
  }

  public void setClientCode(String clientCode)
  {
    this.clientCode = clientCode;
  }

  public String getUsername()
  {
    return this.username;
  }

  public void setUsername(String username)
  {
    this.username = username;
  }

  public Date getStartTime()
  {
    return this.startTime;
  }

  public void setStartTime(Date startTime)
  {
    this.startTime = startTime;
  }

  public Date getEndTime()
  {
    return this.endTime;
  }

  public void setEndTime(Date endTime)
  {
    this.endTime = endTime;
  }

  public Integer getBillMins()
  {
    return this.billMins;
  }
 

  public void setBillMins(Integer billMins)
  {
    this.billMins = billMins;
  }

  public String getBillable()
  {
    return this.billable;
  }

  public void setBillable(String billable)
  {
    this.billable = billable;
  }

  public String getBillCode()
  {
    return this.billCode;
  }

  public void setBillCode(String billCode)
  {
    this.billCode = billCode;
  }

  public String getProviderId()
  {
    return this.providerId;
  }

  public void setProviderId(String providerId)
  {
    this.providerId = providerId;
  }

}
