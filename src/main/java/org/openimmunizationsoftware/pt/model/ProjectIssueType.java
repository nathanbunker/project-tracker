package org.openimmunizationsoftware.pt.model;

public enum ProjectIssueType {
    BLOCKER, UNKNOWN, NOTE;

    public static ProjectIssueType fromString(String value) {
        if (value == null || value.length() == 0) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public String toDisplayEmoji() {
        switch (this) {
            case BLOCKER: return "⛔";
            case NOTE:    return "📝";
            default:      return "❓";
        }
    }
}
