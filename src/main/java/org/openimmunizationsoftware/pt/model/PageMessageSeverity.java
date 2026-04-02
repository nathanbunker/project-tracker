package org.openimmunizationsoftware.pt.model;

/**
 * Severity levels for the multi-message page notification system.
 *
 * <p>
 * New alternative to the legacy single {@code messageProblem} /
 * {@code messageConfirmation} fields on AppReq. Register messages via
 * {@code AppReq.addSuccessMessage(..)} etc. before calling
 * {@code printHtmlHead(..)}. SUCCESS and INFO auto-dismiss by default;
 * WARNING and ERROR persist until manually dismissed.
 * </p>
 */
public enum PageMessageSeverity {

    /** Operation completed successfully. Auto-dismisses. */
    SUCCESS,

    /** General informational notice. Auto-dismisses. */
    INFO,

    /** Something may require attention. Persists. */
    WARNING,

    /** An error occurred that the user must acknowledge. Persists. */
    ERROR
}
