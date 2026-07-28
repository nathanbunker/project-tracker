package org.openimmunizationsoftware.pt.model;

public enum BillPlanTargetMode {
    PERCENT,
    FIXED_HOURS,
    BOTH;

    public String getCode() {
        return name();
    }

    public static BillPlanTargetMode fromCode(String value) {
        if (value == null) {
            return null;
        }
        for (BillPlanTargetMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        return null;
    }
}