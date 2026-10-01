package org.dandeliondaily.dashboard.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProcessStage;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * The dashboard Today column's day ordering: actions are grouped into buckets
 * (start of work day, overdue, committed, WILL, ...), and within a bucket
 * sorted by completionOrder (set values first), then by process stage, type
 * priority, priority level, and change date. Shared by
 * DashboardTodayColumnService and the MCP planning tools so the order an
 * assistant reads and writes is the order the dashboard shows.
 */
public final class DashboardActionOrdering {

    public static final int BUCKET_START_OF_WORK_DAY = 0;
    public static final int BUCKET_OVERDUE = 1;
    public static final int BUCKET_COMMITTED = 3;
    public static final int BUCKET_WILL = 4;
    public static final int BUCKET_PERSONAL_MORNING = 5;
    public static final int BUCKET_MIGHT = 6;
    public static final int BUCKET_WAITING = 7;
    public static final int BUCKET_WILL_MEET = 8;
    public static final int BUCKET_END_OF_WORK_DAY = 9;
    public static final int BUCKET_OTHER = 11;
    /** Not shown in the Today column (non-billable outside the morning slot, or WOULD_LIKE_TO). */
    public static final int BUCKET_HIDDEN = 99;

    private DashboardActionOrdering() {
    }

    public static String getBucketLabel(int bucket) {
        switch (bucket) {
            case BUCKET_START_OF_WORK_DAY:
                return "Start of Work Day";
            case BUCKET_OVERDUE:
                return "Overdue";
            case BUCKET_COMMITTED:
                return "Committed";
            case BUCKET_WILL:
                return "Will";
            case BUCKET_PERSONAL_MORNING:
                return "Personal (Morning)";
            case BUCKET_MIGHT:
                return "Might";
            case BUCKET_WAITING:
                return "Waiting";
            case BUCKET_WILL_MEET:
                return "Will Meet";
            case BUCKET_END_OF_WORK_DAY:
                return "End of Work Day";
            case BUCKET_OTHER:
                return "Other";
            default:
                return "Not on Dashboard";
        }
    }

    public static void sortByBucketThenCompletionOrder(List<ActionNext> projectActionList, WebUser webUser) {
        Collections.sort(projectActionList, (pa1, pa2) -> {
            int c1 = pa1.getCompletionOrder();
            int c2 = pa2.getCompletionOrder();
            int bucket1 = getCompletionBucket(pa1, webUser);
            int bucket2 = getCompletionBucket(pa2, webUser);
            if (bucket1 != bucket2) {
                return bucket1 - bucket2;
            }
            if (c1 > 0 && c2 <= 0) {
                return -1;
            }
            if (c2 > 0 && c1 <= 0) {
                return 1;
            }
            if (c1 > 0 && c2 > 0 && c1 != c2) {
                return c1 - c2;
            }
            return compareInsideBucket(pa1, pa2);
        });
    }

    /**
     * Returns a day's actions in their new order after moving the requested
     * actions to the front of their own buckets, in the order given. Buckets
     * keep their dashboard order, and the rest of each bucket follows in its
     * current order. The caller numbers completionOrder 1..n down the result,
     * the same way DashboardCurrentActionService rationalizes the day.
     * Requested actions missing from dayActions are ignored.
     */
    public static List<ActionNext> applyRequestedOrder(List<ActionNext> dayActions, List<ActionNext> requested,
            WebUser webUser) {
        List<ActionNext> current = new ArrayList<ActionNext>(dayActions);
        sortByBucketThenCompletionOrder(current, webUser);
        Set<Integer> requestedIds = new HashSet<Integer>();
        for (ActionNext action : requested) {
            requestedIds.add(Integer.valueOf(action.getActionNextId()));
        }
        Map<Integer, List<ActionNext>> requestedByBucket = new LinkedHashMap<Integer, List<ActionNext>>();
        Set<Integer> dayIds = new HashSet<Integer>();
        for (ActionNext action : current) {
            dayIds.add(Integer.valueOf(action.getActionNextId()));
        }
        for (ActionNext action : requested) {
            if (!dayIds.contains(Integer.valueOf(action.getActionNextId()))) {
                continue;
            }
            Integer bucket = Integer.valueOf(getCompletionBucket(action, webUser));
            List<ActionNext> bucketList = requestedByBucket.get(bucket);
            if (bucketList == null) {
                bucketList = new ArrayList<ActionNext>();
                requestedByBucket.put(bucket, bucketList);
            }
            bucketList.add(action);
        }

        List<ActionNext> result = new ArrayList<ActionNext>();
        Integer currentBucket = null;
        for (ActionNext action : current) {
            Integer bucket = Integer.valueOf(getCompletionBucket(action, webUser));
            if (!bucket.equals(currentBucket)) {
                currentBucket = bucket;
                List<ActionNext> first = requestedByBucket.get(bucket);
                if (first != null) {
                    result.addAll(first);
                }
            }
            if (!requestedIds.contains(Integer.valueOf(action.getActionNextId()))) {
                result.add(action);
            }
        }
        return result;
    }

    public static int compareInsideBucket(ActionNext pa1, ActionNext pa2) {
        ProcessStage ps1 = pa1.getProcessStage();
        ProcessStage ps2 = pa2.getProcessStage();
        if ((ps1 != null || ps2 != null) && ps1 != ps2) {
            if (ps1 == ProcessStage.FIRST) {
                return -1;
            } else if (ps2 == ProcessStage.FIRST) {
                return 1;
            }
            if (ps1 == ProcessStage.SECOND) {
                return -1;
            } else if (ps2 == ProcessStage.SECOND) {
                return 1;
            }
            if (ps1 == ProcessStage.LAST) {
                return 1;
            } else if (ps2 == ProcessStage.LAST) {
                return -1;
            }
            if (ps1 == ProcessStage.PENULTIMATE) {
                return 1;
            } else if (ps2 == ProcessStage.PENULTIMATE) {
                return -1;
            }
        }

        int p1 = ProjectNextActionType.defaultPriority(pa1.getNextActionType());
        int p2 = ProjectNextActionType.defaultPriority(pa2.getNextActionType());
        if (p1 != p2) {
            return p2 - p1;
        }
        if (pa2.getPriorityLevel() != pa1.getPriorityLevel()) {
            return pa2.getPriorityLevel() - pa1.getPriorityLevel();
        }
        Date d1 = pa1.getNextChangeDate();
        Date d2 = pa2.getNextChangeDate();
        if (d1 != null && d2 != null) {
            int compare = d1.compareTo(d2);
            if (compare != 0) {
                return compare;
            }
        }
        return pa1.getActionNextId() - pa2.getActionNextId();
    }

    public static int getCompletionBucket(ActionNext projectAction, WebUser webUser) {
        if (projectAction == null) {
            return BUCKET_HIDDEN;
        }
        if (projectAction.isBillable()) {
            ProcessStage processStage = projectAction.getProcessStage();
            if (processStage == ProcessStage.FIRST || processStage == ProcessStage.SECOND) {
                return BUCKET_START_OF_WORK_DAY;
            }
            if (processStage == ProcessStage.PENULTIMATE || processStage == ProcessStage.LAST) {
                return BUCKET_END_OF_WORK_DAY;
            }
        }
        LocalDate actionDate = toStoredLocalDate(projectAction.getNextActionDate(), webUser);
        if (projectAction.isBillable() && actionDate != null && actionDate.isBefore(webUser.getLocalDateToday())) {
            return BUCKET_OVERDUE;
        }
        if (!projectAction.isBillable()) {
            if (projectAction.getTimeSlot() == TimeSlot.MORNING) {
                return BUCKET_PERSONAL_MORNING;
            }
            return BUCKET_HIDDEN;
        }
        String nextActionType = projectAction.getNextActionType();
        if (ProjectNextActionType.OVERDUE_TO.equals(nextActionType)) {
            return BUCKET_OVERDUE;
        }
        if (ProjectNextActionType.COMMITTED_TO.equals(nextActionType)) {
            return BUCKET_COMMITTED;
        }
        if (ProjectNextActionType.WILL.equals(nextActionType)
                || ProjectNextActionType.WILL_CONTACT.equals(nextActionType)
                || ProjectNextActionType.WILL_REVIEW.equals(nextActionType)
                || ProjectNextActionType.WILL_DOCUMENT.equals(nextActionType)
                || ProjectNextActionType.WILL_FOLLOW_UP.equals(nextActionType)) {
            return BUCKET_WILL;
        }
        if (ProjectNextActionType.MIGHT.equals(nextActionType)
                || ProjectNextActionType.GOAL.equals(nextActionType)) {
            return BUCKET_MIGHT;
        }
        if (ProjectNextActionType.WAITING.equals(nextActionType)) {
            return BUCKET_WAITING;
        }
        if (ProjectNextActionType.WILL_MEET.equals(nextActionType)) {
            return BUCKET_WILL_MEET;
        }
        if (ProjectNextActionType.WOULD_LIKE_TO.equals(nextActionType)) {
            return BUCKET_HIDDEN;
        }
        return BUCKET_OTHER;
    }

    private static LocalDate toStoredLocalDate(Date date, WebUser webUser) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return webUser.toLocalDate(date);
    }
}
