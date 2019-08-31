package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class BudgetMonth {

  private int monthId = 0;
  private Date monthDate = null;
  private BudgetAccount budgetAccount = null;
  private int balanceStart = 0;
  private int balanceEnd = 0;

  public int getBalanceStart() {
    return balanceStart;
  }

  public void setBalanceStart(int balanceStart) {
    this.balanceStart = balanceStart;
  }

  public int getBalanceEnd() {
    return balanceEnd;
  }

  public void setBalanceEnd(int balanceEnd) {
    this.balanceEnd = balanceEnd;
  }

  public BudgetAccount getBudgetAccount() {
    return budgetAccount;
  }

  public void setBudgetAccount(BudgetAccount budgetAccount) {
    this.budgetAccount = budgetAccount;
  }

  public int getMonthId() {
    return monthId;
  }

  public void setMonthId(int monthId) {
    this.monthId = monthId;
  }

  public Date getMonthDate() {
    return monthDate;
  }

  public void setMonthDate(Date monthDate) {
    this.monthDate = monthDate;
  }

}
