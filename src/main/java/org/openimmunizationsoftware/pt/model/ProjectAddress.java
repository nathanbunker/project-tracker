package org.openimmunizationsoftware.pt.model;

public class ProjectAddress {
  private int addressId = 0;
  private AddressStatus addressStatus = null;
  private String country = "";
  private String line1 = "";
  private String line2 = "";
  private String line3 = "";
  private String line4 = "";
  private String city = "";
  private String state = "";
  private String zip = "";
  private String timeZone = "";

  public int getAddressId() {
    return addressId;
  }

  public void setAddressId(int addressId) {
    this.addressId = addressId;
  }

  public String getLine3() {
    return line3;
  }

  public void setLine3(String line3) {
    this.line3 = line3;
  }

  public String getLine4() {
    return line4;
  }

  public void setLine4(String line4) {
    this.line4 = line4;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(String timeZone) {
    this.timeZone = timeZone;
  }

  public AddressStatus getAddressStatus() {
    return addressStatus;
  }

  public void setAddressStatus(AddressStatus addressStatus) {
    this.addressStatus = addressStatus;
  }

  public String getAddressStatusString() {
    return addressStatus == null ? "" : addressStatus.getId();
  }

  public void setAddressStatusString(String addressStatusString) {
    this.addressStatus = AddressStatus.get(addressStatusString);
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getLine1() {
    return line1;
  }

  public void setLine1(String line1) {
    this.line1 = line1;
  }

  public String getLine2() {
    return line2;
  }

  public void setLine2(String line2) {
    this.line2 = line2;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getZip() {
    return zip;
  }

  public void setZip(String zip) {
    this.zip = zip;
  }
}
