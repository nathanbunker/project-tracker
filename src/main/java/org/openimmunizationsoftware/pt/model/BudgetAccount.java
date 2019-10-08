package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class BudgetAccount {

  private int accountId = 0;
  private String accountLabel = "";
  private ProjectProvider provider;
  private int balanceAmount = 0;
  private Date balanceDate = null;
  private int startAmount = 0;
  private Date startDate = null;

  public int getStartAmount() {
    return startAmount;
  }

  public void setStartAmount(int startAmount) {
    this.startAmount = startAmount;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public int getAccountId() {
    return accountId;
  }

  public void setAccountId(int accountId) {
    this.accountId = accountId;
  }

  public String getAccountLabel() {
    return accountLabel;
  }

  public void setAccountLabel(String accountLabel) {
    this.accountLabel = accountLabel;
  }

  public int getBalanceAmount() {
    return balanceAmount;
  }

  public void setBalanceAmount(int balanceAmount) {
    this.balanceAmount = balanceAmount;
  }

  public Date getBalanceDate() {
    return balanceDate;
  }

  public void setBalanceDate(Date balanceDate) {
    this.balanceDate = balanceDate;
  }

  public ProjectProvider getProvider() {
    return provider;
  }

  public void setProvider(ProjectProvider provider) {
    this.provider = provider;
  }

}
