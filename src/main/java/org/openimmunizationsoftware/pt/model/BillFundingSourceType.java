package org.openimmunizationsoftware.pt.model;

public enum BillFundingSourceType {
    FEDERAL,
    AIRA,
    CONTRACT,
    INTERNAL,
    OTHER;

    public String getCode() {
        return name();
    }

    public static BillFundingSourceType fromCode(String value) {
        if (value == null) {
            return null;
        }
        for (BillFundingSourceType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return null;
    }
}