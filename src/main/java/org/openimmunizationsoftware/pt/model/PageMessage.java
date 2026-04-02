package org.openimmunizationsoftware.pt.model;

/**
 * A single page-level notification message for the multi-message system.
 *
 * <p>
 * This is a new alternative to the legacy {@code messageProblem} /
 * {@code messageConfirmation} fields on AppReq. Those remain unchanged for
 * backward compatibility. Use this class for new work.
 * </p>
 *
 * <p>
 * Usage from a servlet before calling {@code printHtmlHead(..)}:
 * </p>
 * 
 * <pre>
 * appReq.addSuccessMessage("Action saved successfully.");
 * appReq.addErrorMessage("Could not save: " + reason);
 * printHtmlHead(appReq);
 * </pre>
 *
 * <p>
 * SUCCESS and INFO default to auto-dismiss after
 * {@link #DEFAULT_TRANSIENT_MS} milliseconds. WARNING and ERROR default to
 * persistent (no auto-dismiss).
 * </p>
 */
public class PageMessage {

    /** Default auto-dismiss delay for SUCCESS and INFO messages (12 seconds). */
    public static final int DEFAULT_TRANSIENT_MS = 12000;

    private final PageMessageSeverity severity;
    private final String messageText;
    private final boolean autoDismiss;
    private final int dismissAfterMs;

    /**
     * Full constructor.
     *
     * @param severity       the severity/type of the message
     * @param messageText    plain text to display (will be HTML-escaped by the
     *                       renderer)
     * @param autoDismiss    whether the message should dismiss itself after a delay
     * @param dismissAfterMs milliseconds before auto-dismiss (ignored when
     *                       autoDismiss is false)
     */
    public PageMessage(PageMessageSeverity severity, String messageText,
            boolean autoDismiss, int dismissAfterMs) {
        this.severity = severity;
        this.messageText = messageText;
        this.autoDismiss = autoDismiss;
        this.dismissAfterMs = dismissAfterMs;
    }

    /**
     * Convenience constructor using severity-appropriate defaults.
     * SUCCESS/INFO default to auto-dismiss; WARNING/ERROR default to persistent.
     *
     * @param severity    the severity/type of the message
     * @param messageText plain text to display
     */
    public PageMessage(PageMessageSeverity severity, String messageText) {
        this.severity = severity;
        this.messageText = messageText;
        boolean transient_ = (severity == PageMessageSeverity.SUCCESS
                || severity == PageMessageSeverity.INFO);
        this.autoDismiss = transient_;
        this.dismissAfterMs = transient_ ? DEFAULT_TRANSIENT_MS : 0;
    }

    public PageMessageSeverity getSeverity() {
        return severity;
    }

    public String getMessageText() {
        return messageText;
    }

    public boolean isAutoDismiss() {
        return autoDismiss;
    }

    public int getDismissAfterMs() {
        return dismissAfterMs;
    }
}
