package org.openimmunizationsoftware.pt.model;

public enum TimeSlot {
    WAKE("WAKE", "Wake"), MORNING("MORNING", "Morning"), AFTERNOON("AFTERNOON", "Afternoon"),
    EVENING("EVENING", "Evening");

    TimeSlot(String id, String label) {
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

    public static TimeSlot getTimeSlot(String id) {
        if (id == null) {
            return null;
        }
        for (TimeSlot timeSlot : TimeSlot.values()) {
            if (timeSlot.getId().equals(id)) {
                return timeSlot;
            }
        }
        return null;
    }
}
