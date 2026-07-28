package org.openimmunizationsoftware.pt.model;

public enum BillPlanStatus {
    DRAFT,
    APPROVED,
    SUPERSEDED,
    INACTIVE;

    public String getCode() {
        return name();
    }

    public static BillPlanStatus fromCode(String value) {
        if (value == null) {
            return null;
        }
        for (BillPlanStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }
}