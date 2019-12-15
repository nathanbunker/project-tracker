package org.openimmunizationsoftware.pt.model;

public enum AddressStatus {
                           SEND("S", "Send"),
                           ASK_FIRST("A", "Ask First"),
                           DO_NOT_SEND("D", "Do Not Send"),;
  private String id = "";
  private String label = "";


  public String getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  private AddressStatus(String id, String label) {
    this.id = id;
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }

  public static AddressStatus get(String id) {
    if (id != null) {
      for (AddressStatus v : values()) {
        if (v.getId().equals(id)) {
          return v;
        }
      }
    }
    return null;
  }

}
