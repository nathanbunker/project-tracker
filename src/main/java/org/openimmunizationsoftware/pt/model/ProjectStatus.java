package org.openimmunizationsoftware.pt.model;

public enum ProjectStatus {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETE("Complete"),
    CLOSED("Closed");

    private final String databaseValue;

    ProjectStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static ProjectStatus fromDatabaseValue(String value) {
        if (value == null || value.trim().length() == 0) {
            return ACTIVE;
        }
        String normalized = value.trim();
        for (ProjectStatus status : values()) {
            if (status.databaseValue.equalsIgnoreCase(normalized)
                    || status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        return ACTIVE;
    }

    public static boolean isKnownStatus(String value) {
        if (value == null || value.trim().length() == 0) {
            return false;
        }
        String normalized = value.trim();
        for (ProjectStatus status : values()) {
            if (status.databaseValue.equalsIgnoreCase(normalized)
                    || status.name().equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    public boolean isClosed() {
        return this == CLOSED;
    }
}
