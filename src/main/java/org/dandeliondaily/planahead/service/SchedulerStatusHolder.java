package org.dandeliondaily.planahead.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * In-memory store of TemplateScheduler run history.
 * Thread-safe. Keeps the last {@link #MAX_RECORDS} completed runs plus a
 * reference to the currently executing run (if any).
 *
 * No persistence: history is lost on app restart, which is intentional — the
 * goal is to show current operational health, not long-term audit data.
 */
public class SchedulerStatusHolder {

    /**
     * Maximum number of completed run records retained. 48 covers ~2 days at hourly
     * cadence.
     */
    private static final int MAX_RECORDS = 48;

    private static volatile SchedulerRunRecord currentRun = null;
    private static final LinkedList<SchedulerRunRecord> completedRuns = new LinkedList<>();

    private SchedulerStatusHolder() {
        // static-only utility class
    }

    /** Called at the start of every generation run. */
    public static synchronized void beginRun(SchedulerRunRecord record) {
        currentRun = record;
    }

    /** Called when a run ends (success or failure). Moves record to history. */
    public static synchronized void completeRun(SchedulerRunRecord record) {
        currentRun = null;
        completedRuns.addFirst(record);
        while (completedRuns.size() > MAX_RECORDS) {
            completedRuns.removeLast();
        }
    }

    /** Returns the currently executing run, or {@code null} if idle. */
    public static SchedulerRunRecord getCurrentRun() {
        return currentRun;
    }

    /** Returns the most recently completed run, or {@code null} if none yet. */
    public static synchronized SchedulerRunRecord getLastCompletedRun() {
        return completedRuns.isEmpty() ? null : completedRuns.getFirst();
    }

    /**
     * Returns a snapshot list of completed runs, most recent first.
     * Never returns {@code null}; may be empty.
     */
    public static synchronized List<SchedulerRunRecord> getRecentRuns() {
        return new ArrayList<>(completedRuns);
    }
}
