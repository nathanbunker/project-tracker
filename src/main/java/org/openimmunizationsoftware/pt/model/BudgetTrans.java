package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class BudgetTrans
{

  public static final String TRANS_STATUS_EXPECTED = "E";
  public static final String TRANS_STATUS_POSSIBLE = "B";
  public static final String TRANS_STATUS_SCHEDULED = "S";
  public static final String TRANS_STATUS_PENDING = "P";
  public static final String TRANS_STATUS_DUE = "D";
  public static final String TRANS_STATUS_PAID = "X";

  public static final String[][] TRANS_STATUS = { { TRANS_STATUS_EXPECTED, "Expected" }, { TRANS_STATUS_POSSIBLE, "Possible" },
      { TRANS_STATUS_SCHEDULED, "Scheduled" }, { TRANS_STATUS_PENDING, "Pending" }, { TRANS_STATUS_DUE, "Due" }, { TRANS_STATUS_PAID, "Paid" } };

  public static String getTransStatusLabel(String transStatus)
  {
    for (String[] pair : TRANS_STATUS)
    {
      if (pair[0].equals(transStatus))
      {
        return pair[1];
      }
    }
    return transStatus;
  }
  
  public String getTransStatusLabel()
  {
    return getTransStatusLabel(transStatus);
  }

  private int transId = 0;
  private BudgetItem budgetItem = null;
  private BudgetMonth budgetMonth = null;
  private Date transDate = null;
  private String transStatus = "";
  private int transAmount = 0;
  private BudgetTrans relatedBudgetTrans = null;
  private BudgetTransRecord budgetTransRecord = null;

  public BudgetTransRecord getBudgetTransRecord()
  {
    return budgetTransRecord;
  }

  public void setBudgetTransRecord(BudgetTransRecord budgetTransRecord)
  {
    this.budgetTransRecord = budgetTransRecord;
  }

  public int getTransId()
  {
    return transId;
  }

  public void setTransId(int transId)
  {
    this.transId = transId;
  }

  public BudgetItem getBudgetItem()
  {
    return budgetItem;
  }

  public void setBudgetItem(BudgetItem budgetItem)
  {
    this.budgetItem = budgetItem;
  }

  public BudgetMonth getBudgetMonth()
  {
    return budgetMonth;
  }

  public void setBudgetMonth(BudgetMonth budgetMonth)
  {
    this.budgetMonth = budgetMonth;
  }

  public Date getTransDate()
  {
    return transDate;
  }

  public void setTransDate(Date transDate)
  {
    this.transDate = transDate;
  }

  public String getTransStatus()
  {
    return transStatus;
  }

  public void setTransStatus(String transStatus)
  {
    this.transStatus = transStatus;
  }

  public int getTransAmount()
  {
    return transAmount;
  }

  public void setTransAmount(int transAmount)
  {
    this.transAmount = transAmount;
  }

  public BudgetTrans getRelatedBudgetTrans()
  {
    return relatedBudgetTrans;
  }

  public void setRelatedBudgetTrans(BudgetTrans relatedBudgetTrans)
  {
    this.relatedBudgetTrans = relatedBudgetTrans;
  }

}
