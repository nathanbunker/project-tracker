package org.openimmunizationsoftware.pt.model;

public enum BillPlanVariancePolicy {
    CARRY_FORWARD,
    FORWARD_ONLY,
    MANUAL;

    public String getCode() {
        return name();
    }

    public static BillPlanVariancePolicy fromCode(String value) {
        if (value == null) {
            return null;
        }
        for (BillPlanVariancePolicy policy : values()) {
            if (policy.name().equalsIgnoreCase(value.trim())) {
                return policy;
            }
        }
        return null;
    }
}