package org.openimmunizationsoftware.pt.model;

public enum MailStatus {
                        PURCHASED("P", "Purchased"),
                        ASSIGNED("A", "Assigned"),
                        FIRST_NOTICE_SENT("1", "1st Notice Sent"),
                        SECOND_NOTICE_SENT("2", "2nd Notice Sent"),
                        READY("R", "Ready"),
                        MAILED("M", "Mailed"),
                        DO_NOT_MAIL("N", "Do Not Mail"),;
  private String id = "";
  private String label = "";


  public String getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  private MailStatus(String id, String label) {
    this.id = id;
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }

  public static MailStatus get(String id) {
    if (id != null) {
      for (MailStatus v : values()) {
        if (v.getId().equals(id)) {
          return v;
        }
      }
    }
    return null;
  }

}
