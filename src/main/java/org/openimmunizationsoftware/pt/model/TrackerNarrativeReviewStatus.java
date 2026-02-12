package org.openimmunizationsoftware.pt.model;

public enum TrackerNarrativeReviewStatus {
    GENERATING("GENERATING", "Generating"),
    GENERATED("GENERATED", "Generated"),
    APPROVED("APPROVED", "Approved"),
    REJECTED("REJECTED", "Rejected"),
    DELETED("DELETED", "Deleted");

    private final String id;
    private final String label;

    TrackerNarrativeReviewStatus(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public static TrackerNarrativeReviewStatus fromId(String id) {
        if (id == null) {
            return null;
        }
        for (TrackerNarrativeReviewStatus status : TrackerNarrativeReviewStatus.values()) {
            if (status.id.equals(id)) {
                return status;
            }
        }
        return null;
    }
}
