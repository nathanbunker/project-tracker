package org.openimmunizationsoftware.pt.model;

public enum BillPlanPercentBasis {
    ALL_WORKED_TIME;

    public String getCode() {
        return name();
    }

    public static BillPlanPercentBasis fromCode(String value) {
        if (value == null) {
            return null;
        }
        for (BillPlanPercentBasis basis : values()) {
            if (basis.name().equalsIgnoreCase(value.trim())) {
                return basis;
            }
        }
        return null;
    }
}