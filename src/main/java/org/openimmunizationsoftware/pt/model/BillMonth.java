package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class BillMonth
{
  private int billMonthId = 0;
  private BillCode billCode = null;
  private Date billDate = null;
  private int billMinsExpected = 0;
  private int billMinsActual = 0;
  private BillBudget billBudget = null;
  
  public int getBillMonthId()
  {
    return billMonthId;
  }
  public void setBillMonthId(int billMonthId)
  {
    this.billMonthId = billMonthId;
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
  public int getBillMinsExpected()
  {
    return billMinsExpected;
  }
  public void setBillMinsExpected(int billMinsExpected)
  {
    this.billMinsExpected = billMinsExpected;
  }
  public int getBillMinsActual()
  {
    return billMinsActual;
  }
  public void setBillMinsActual(int billMinsActual)
  {
    this.billMinsActual = billMinsActual;
  }
  public BillBudget getBillBudget()
  {
    return billBudget;
  }
  public void setBillBudget(BillBudget billBudget)
  {
    this.billBudget = billBudget;
  }

}
