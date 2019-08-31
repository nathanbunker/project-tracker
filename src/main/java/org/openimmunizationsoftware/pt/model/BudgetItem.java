package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class BudgetItem {
  public static final String ITEM_STATUS_MONTHLY = "M";
  public static final String ITEM_STATUS_YEARLY = "Y";
  public static final String ITEM_STATUS_SPORADIC = "S";
  public static final String ITEM_STATUS_ONE_TIME = "O";
  public static final String ITEM_STATUS_CLOSED = "C";

  public static final String[][] ITEM_STATUS = {{ITEM_STATUS_MONTHLY, "Monthly"},
      {ITEM_STATUS_YEARLY, "Yearly"}, {ITEM_STATUS_SPORADIC, "Sporadic"},
      {ITEM_STATUS_ONE_TIME, "One Time"}, {ITEM_STATUS_CLOSED, "Closed"}};

  public static final String PRIORITY_CODE_COMMITTED = "A";
  public static final String PRIORITY_CODE_SCHEDULED = "B";
  public static final String PRIORITY_CODE_FLEXIBLE = "C";
  public static final String PRIORITY_CODE_DEBT_PAYOFF = "P";
  public static final String PRIORITY_CODE_SAVINGS = "S";
  public static final String PRIORITY_CODE_BALANCE = "O";
  public static final String PRIORITY_CODE_INCOME = "I";
  public static final String PRIORITY_CODE_DONATIONS = "D";

  public static final String[][] PRIORITY_CODE =
      {{PRIORITY_CODE_COMMITTED, "Committed"}, {PRIORITY_CODE_SCHEDULED, "Scheduled"},
          {PRIORITY_CODE_FLEXIBLE, "Flexible"}, {PRIORITY_CODE_DEBT_PAYOFF, "Debt Payoff"},
          {PRIORITY_CODE_DONATIONS, "Donations"}, {PRIORITY_CODE_SAVINGS, "Savings"},
          {PRIORITY_CODE_BALANCE, "Balance"}, {PRIORITY_CODE_INCOME, "Income"}};

  public static String getPriorityCodeLabel(String priorityCode) {
    for (String[] pair : PRIORITY_CODE) {
      if (pair[0].equals(priorityCode)) {
        return pair[1];
      }
    }
    return priorityCode;
  }

  private int itemId = 0;
  private String itemLabel = "";
  private BudgetAccount budgetAccount = null;
  private String itemStatus = "";
  private int lastAmount = 0;
  private Date lastDate = null;
  private String priorityCode = "";
  private BudgetItem relatedBudgetItem = null;

  public int getItemId() {
    return itemId;
  }

  public void setItemId(int itemId) {
    this.itemId = itemId;
  }

  public String getItemLabel() {
    return itemLabel;
  }

  public void setItemLabel(String itemLabel) {
    this.itemLabel = itemLabel;
  }

  public String getItemStatus() {
    return itemStatus;
  }

  public void setItemStatus(String itemStatus) {
    this.itemStatus = itemStatus;
  }

  public int getLastAmount() {
    return lastAmount;
  }

  public void setLastAmount(int lastAmount) {
    this.lastAmount = lastAmount;
  }

  public Date getLastDate() {
    return lastDate;
  }

  public void setLastDate(Date lastDate) {
    this.lastDate = lastDate;
  }

  public String getPriorityCode() {
    return priorityCode;
  }

  public void setPriorityCode(String priorityCode) {
    this.priorityCode = priorityCode;
  }

  public BudgetItem getRelatedBudgetItem() {
    return relatedBudgetItem;
  }

  public void setRelatedBudgetItem(BudgetItem relatedBudgetItem) {
    this.relatedBudgetItem = relatedBudgetItem;
  }

  public BudgetAccount getBudgetAccount() {
    return budgetAccount;
  }

  public void setBudgetAccount(BudgetAccount budgetAccount) {
    this.budgetAccount = budgetAccount;
  }

}
