package org.openimmunizationsoftware.pt.model;

public enum ProjectIssueStatus {
    OPEN, RESOLVED;

    public static ProjectIssueStatus fromString(String value) {
        if (value == null || value.length() == 0) {
            return OPEN;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OPEN;
        }
    }
}
