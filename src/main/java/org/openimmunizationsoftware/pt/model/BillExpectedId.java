package org.openimmunizationsoftware.pt.model;

// Generated Dec 12, 2012 3:50:50 AM by Hibernate Tools 3.4.0.CR1

import java.util.Date;

/**
 * BillExpectedId generated by hbm2java
 */
public class BillExpectedId implements java.io.Serializable {

  private static final long serialVersionUID = -1668990878557896228L;

  private String username;
  private Date billDate;

  public BillExpectedId() {}

  public BillExpectedId(String username, Date billDate) {
    this.username = username;
    this.billDate = billDate;
  }

  public String getUsername() {
    return this.username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Date getBillDate() {
    return this.billDate;
  }

  public void setBillDate(Date billDate) {
    this.billDate = billDate;
  }

  public boolean equals(Object other) {
    if ((this == other))
      return true;
    if ((other == null))
      return false;
    if (!(other instanceof BillExpectedId))
      return false;
    BillExpectedId castOther = (BillExpectedId) other;

    return ((this.getUsername() == castOther.getUsername()) || (this.getUsername() != null
        && castOther.getUsername() != null && this.getUsername().equals(castOther.getUsername())))
        && ((this.getBillDate() == castOther.getBillDate())
            || (this.getBillDate() != null && castOther.getBillDate() != null
                && this.getBillDate().equals(castOther.getBillDate())));
  }

  public int hashCode() {
    int result = 17;

    result = 37 * result + (getUsername() == null ? 0 : this.getUsername().hashCode());
    result = 37 * result + (getBillDate() == null ? 0 : this.getBillDate().hashCode());
    return result;
  }

}
