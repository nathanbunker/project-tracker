package org.openimmunizationsoftware.pt.model;

public enum PrioritySpecial {
    FIRST("1", "First"), SECOND("2", "Second"), PENULTIMATE("P", "Penultimate"), 
    LAST("L", "Last");
    PrioritySpecial(String id, String label) {
        this.id = id;
        this.label = label;
    }
  private String id = "";
  private String label = "";

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public static PrioritySpecial getPrioritySpecial(String id) {
        for (PrioritySpecial prioritySpecial : PrioritySpecial.values()) {
            if (prioritySpecial.getId().equals(id)) {
                return prioritySpecial;
            }
        }
        return null;
    }
}
