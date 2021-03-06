package org.openimmunizationsoftware.pt.model;

// Generated Dec 12, 2012 3:50:50 AM by Hibernate Tools 3.4.0.CR1

/**
 * BillCode generated by hbm2java
 */
public class BillCode implements java.io.Serializable {

  private static final long serialVersionUID = -6825144623396164007L;
  private String billCode;
  private String billLabel;
  private String billable;
  private String visible;
  private String clientBillCode;
  private String clientBillDescription;
  private ProjectProvider provider;
  private int estimateMin;
  private int billRate;
  private int billRound;


  public int getEstimateMin() {
    return estimateMin;
  }

  public void setEstimateMin(int estimateMin) {
    this.estimateMin = estimateMin;
  }

  public int getBillRate() {
    return billRate;
  }

  public void setBillRate(int billRate) {
    this.billRate = billRate;
  }

  public int getBillRound() {
    return billRound;
  }

  public void setBillRound(int billRound) {
    this.billRound = billRound;
  }

  public String getBillCode() {
    return this.billCode;
  }

  public void setBillCode(String billCode) {
    this.billCode = billCode;
  }

  public String getBillLabel() {
    return this.billLabel;
  }

  public void setBillLabel(String billLabel) {
    this.billLabel = billLabel;
  }

  public String getBillable() {
    return this.billable;
  }

  public void setBillable(String billable) {
    this.billable = billable;
  }

  public String getVisible() {
    return this.visible;
  }

  public void setVisible(String visible) {
    this.visible = visible;
  }

  public String getClientBillCode() {
    return this.clientBillCode;
  }

  public void setClientBillCode(String clientBillCode) {
    this.clientBillCode = clientBillCode;
  }

  public String getClientBillDescription() {
    return this.clientBillDescription;
  }

  public void setClientBillDescription(String clientBillDescription) {
    this.clientBillDescription = clientBillDescription;
  }

  public ProjectProvider getProvider() {
    return provider;
  }

  public void setProvider(ProjectProvider provider) {
    this.provider = provider;
  }

}
