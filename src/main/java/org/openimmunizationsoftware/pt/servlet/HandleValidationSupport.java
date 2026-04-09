package org.openimmunizationsoftware.pt.servlet;

public final class HandleValidationSupport {

    private HandleValidationSupport() {
    }

    public static String clip(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen);
    }

    public static String resolveHandle(String candidate, String fallback, int maxLen) {
        String handle = clip(candidate, maxLen);
        if (handle.length() == 0) {
            handle = clip(fallback, maxLen);
        }
        return handle;
    }

    public static String validateHandleCharacters(String label, String handle) {
        if (handle == null || handle.length() == 0) {
            return null;
        }
        if (handle.indexOf('-') >= 0) {
            return label + " cannot contain '-'";
        }
        if (handle.indexOf(':') >= 0) {
            return label + " cannot contain ':'";
        }
        return null;
    }
}
