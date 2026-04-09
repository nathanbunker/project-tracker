package org.openimmunizationsoftware.pt.model;

public enum ActionSetType {
    STANDARD("S", "Standard"),
    SHARED("H", "Shared"),
    ASK("A", "Ask");

    ActionSetType(String id, String label) {
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

    public static ActionSetType get(String id) {
        if (id != null) {
            for (ActionSetType t : values()) {
                if (t.getId().equals(id)) {
                    return t;
                }
            }
        }
        return null;
    }
}
