package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class BillDay
{
  private int billDayId = 0;
  private BillCode billCode = null;
  private Date billDate = null;
  private int billMins = 0;
  private int billMinsBudget = 0;
  private BillBudget billBudget = null;
  private BillMonth billMonth = null;

  public int getBillMinsBudget()
  {
    return billMinsBudget;
  }

  public void setBillMinsBudget(int billMinsBudget)
  {
    this.billMinsBudget = billMinsBudget;
  }

  public BillMonth getBillMonth()
  {
    return billMonth;
  }

  public void setBillMonth(BillMonth billMonth)
  {
    this.billMonth = billMonth;
  }

  public BillBudget getBillBudget()
  {
    return billBudget;
  }

  public void setBillBudget(BillBudget billBudget)
  {
    this.billBudget = billBudget;
  }

  public int getBillDayId()
  {
    return billDayId;
  }

  public void setBillDayId(int billDayId)
  {
    this.billDayId = billDayId;
  }

  public BillCode getBillCode()
  {
    return billCode;
  }

  public void setBillCode(BillCode billCode)
  {
    this.billCode = billCode;
  }

  public Date getBillDate()
  {
    return billDate;
  }

  public void setBillDate(Date billDate)
  {
    this.billDate = billDate;
  }

  public int getBillMins()
  {
    return billMins;
  }

  public void setBillMins(int billMins)
  {
    this.billMins = billMins;
  }

}
