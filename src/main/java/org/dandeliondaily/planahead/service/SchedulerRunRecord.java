package org.dandeliondaily.planahead.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Captures the outcome of a single TemplateScheduler generation run.
 * Mutable while the run is in progress; treated as effectively immutable once
 * {@link #finish(Outcome)} has been called.
 */
public class SchedulerRunRecord {

    public enum Outcome {
        RUNNING,
        OK,
        PARTIAL_ERRORS,
        FATAL,
        NO_TEMPLATES
    }

    private final Date startTime;
    private Date endTime;
    private int pairsProcessed;
    private int instancesGenerated;
    private int pairsWithErrors;
    private String lastErrorMessage;
    private String lastErrorStackTrace;
    private Outcome outcome = Outcome.RUNNING;

    public SchedulerRunRecord() {
        this.startTime = new Date();
    }

    /** Called when the run ends (success or failure). Sets endTime and outcome. */
    public void finish(Outcome outcome) {
        this.endTime = new Date();
        this.outcome = outcome;
    }

    public void setPairsProcessed(int count) {
        this.pairsProcessed = count;
    }

    public void addInstancesGenerated(int count) {
        this.instancesGenerated += count;
    }

    /** Records one pair-level error and stores its message and stack trace. */
    public void recordError(Throwable t) {
        this.pairsWithErrors++;
        this.lastErrorMessage = t.getMessage() != null ? t.getMessage() : t.getClass().getName();
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        this.lastErrorStackTrace = sw.toString();
    }

    // ---- read accessors --------------------------------------------------

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public int getPairsProcessed() {
        return pairsProcessed;
    }

    public int getInstancesGenerated() {
        return instancesGenerated;
    }

    public int getPairsWithErrors() {
        return pairsWithErrors;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public String getLastErrorStackTrace() {
        return lastErrorStackTrace;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public long getDurationMs() {
        if (endTime == null) {
            return System.currentTimeMillis() - startTime.getTime();
        }
        return endTime.getTime() - startTime.getTime();
    }

    public String formatStartTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime);
    }

    public String formatStartTimeShort() {
        return new SimpleDateFormat("MM-dd HH:mm").format(startTime);
    }

    public String formatDuration() {
        long ms = getDurationMs();
        if (ms < 1000) {
            return ms + " ms";
        }
        return (ms / 1000) + " s";
    }
}
