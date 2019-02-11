package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class BillBudget
{
  private int billBudgetId = 0;
  private String billBudgetCode = "";
  private BillCode billCode = null;
  private Date startDate = null;
  private Date endDate = null;
  private int billMins = 0;
  private int billMinsRemaining = 0;
  public int getBillMinsRemaining()
  {
    return billMinsRemaining;
  }
  public void setBillMinsRemaining(int billMinsRemaining)
  {
    this.billMinsRemaining = billMinsRemaining;
  }
  public int getBillBudgetId()
  {
    return billBudgetId;
  }
  public void setBillBudgetId(int billBudgetId)
  {
    this.billBudgetId = billBudgetId;
  }
  public String getBillBudgetCode()
  {
    return billBudgetCode;
  }
  public void setBillBudgetCode(String billBudgetCode)
  {
    this.billBudgetCode = billBudgetCode;
  }
  public BillCode getBillCode()
  {
    return billCode;
  }
  public void setBillCode(BillCode billCode)
  {
    this.billCode = billCode;
  }
  public Date getStartDate()
  {
    return startDate;
  }
  public void setStartDate(Date startDate)
  {
    this.startDate = startDate;
  }
  public Date getEndDate()
  {
    return endDate;
  }
  public void setEndDate(Date endDate)
  {
    this.endDate = endDate;
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
