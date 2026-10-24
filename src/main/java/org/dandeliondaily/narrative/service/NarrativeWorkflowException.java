package org.dandeliondaily.narrative.service;

public class NarrativeWorkflowException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String code;

    public NarrativeWorkflowException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
