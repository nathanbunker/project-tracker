package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class BudgetTransRecord {
  private int transRecordId = 0;
  private BudgetTrans budgetTrans = null;
  private Date transDate = null;
  private int transAmount = 0;
  private String description = "";
  private BudgetAccount budgetAccount = null;

  public BudgetAccount getBudgetAccount() {
    return budgetAccount;
  }

  public void setBudgetAccount(BudgetAccount budgetAccount) {
    this.budgetAccount = budgetAccount;
  }

  public int getTransRecordId() {
    return transRecordId;
  }

  public void setTransRecordId(int transRecordId) {
    this.transRecordId = transRecordId;
  }

  public BudgetTrans getBudgetTrans() {
    return budgetTrans;
  }

  public void setBudgetTrans(BudgetTrans budgetTrans) {
    this.budgetTrans = budgetTrans;
  }

  public Date getTransDate() {
    return transDate;
  }

  public void setTransDate(Date transDate) {
    this.transDate = transDate;
  }

  public int getTransAmount() {
    return transAmount;
  }

  public void setTransAmount(int transAmount) {
    this.transAmount = transAmount;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
