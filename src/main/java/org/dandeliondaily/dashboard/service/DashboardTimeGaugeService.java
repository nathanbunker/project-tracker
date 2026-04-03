package org.dandeliondaily.dashboard.service;

import java.util.Calendar;
import java.util.List;

import org.dandeliondaily.dashboard.model.TimeGaugeModel;
import org.dandeliondaily.dashboard.model.TimeGaugeState;
import org.dandeliondaily.dashboard.model.TimeGaugeVariant;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;

public class DashboardTimeGaugeService {

    private static final int WARNING_PERCENT = 85;
    private static final int DAILY_TARGET_MINUTES = 8 * 60;

    public TimeGaugeModel buildNowGauge(AppReq appReq) {
        TimeGaugeModel model = new TimeGaugeModel();
        model.setVariant(TimeGaugeVariant.STACKED);
        model.setShowTitle(false);

        ProjectActionNext currentAction = appReq.getCompletingAction();
        if (currentAction == null) {
            model.setCurrentMinutes(0);
            model.setTargetMinutes(0);
            model.setState(TimeGaugeState.UNKNOWN);
            model.setStatusText("No current action");
            return model;
        }

        int spentMinutes = loadSpentMinutesToday(appReq.getDataSession(), appReq.getWebUser().getCalendar(),
                currentAction);
        int targetMinutes = currentAction.getNextTimeEstimate() == null ? 0 : currentAction.getNextTimeEstimate();
        model.setCurrentMinutes(spentMinutes);
        model.setTargetMinutes(targetMinutes);

        if (targetMinutes <= 0) {
            model.setState(TimeGaugeState.UNKNOWN);
            model.setStatusText("No estimate");
            return model;
        }

        model.setState(determineState(spentMinutes, targetMinutes));
        int delta = targetMinutes - spentMinutes;
        if (delta > 0) {
            model.setStatusText(formatDuration(delta) + " left");
        } else if (delta == 0) {
            model.setStatusText("On estimate");
        } else {
            model.setStatusText("+" + formatDuration(Math.abs(delta)) + " over");
        }
        return model;
    }

    public TimeGaugeModel buildTodayGauge(AppReq appReq) {
        TimeGaugeModel model = new TimeGaugeModel();
        model.setVariant(TimeGaugeVariant.TODAY_HEADER);
        model.setShowTitle(false);

        // This reuses the same source used by the global app header time display.
        int spentToday = 0;
        TimeTracker timeTracker = appReq.getTimeTracker();
        if (timeTracker != null) {
            spentToday = timeTracker.getTotalMinsBillable();
        }

        // Add Spent row
        TimeGaugeModel.GaugeRow spentRow = new TimeGaugeModel.GaugeRow("Spent", spentToday, DAILY_TARGET_MINUTES);
        spentRow.setState(determineState(spentToday, DAILY_TARGET_MINUTES));
        model.addRow(spentRow);

        // Add Planned row (to be set by servlet with actual planned minutes)
        TimeGaugeModel.GaugeRow plannedRow = new TimeGaugeModel.GaugeRow("Planned", 0, DAILY_TARGET_MINUTES);
        plannedRow.setState(TimeGaugeState.NORMAL);
        model.addRow(plannedRow);

        return model;
    }

    public TimeGaugeModel buildPlannedDayGauge(int plannedMinutes) {
        TimeGaugeModel model = new TimeGaugeModel();
        model.setVariant(TimeGaugeVariant.STACKED);
        model.setShowTitle(false);
        model.setCurrentMinutes(Math.max(0, plannedMinutes));
        model.setTargetMinutes(DAILY_TARGET_MINUTES);
        model.setState(determineState(plannedMinutes, DAILY_TARGET_MINUTES));

        int delta = DAILY_TARGET_MINUTES - plannedMinutes;
        if (plannedMinutes <= 0) {
            model.setStatusText("Nothing planned");
        } else if (delta > 0) {
            model.setStatusText(formatDuration(delta) + " left");
        } else if (delta == 0) {
            model.setStatusText("Full day planned");
        } else {
            model.setStatusText("+" + formatDuration(Math.abs(delta)) + " over");
        }
        return model;
    }

    public void updateTodayGaugePlanned(TimeGaugeModel todayGaugeModel, int plannedMinutes) {
        if (todayGaugeModel.getRows().size() >= 2) {
            // Recreate the row with updated minutes
            TimeGaugeModel.GaugeRow updatedRow = new TimeGaugeModel.GaugeRow("Planned", plannedMinutes,
                    DAILY_TARGET_MINUTES);
            updatedRow.setState(determineState(plannedMinutes, DAILY_TARGET_MINUTES));
            todayGaugeModel.getRows().set(1, updatedRow);
        }
    }

    public TimeGaugeModel buildInlineGauge(int currentMinutes, int targetMinutes, String statusText) {
        TimeGaugeModel model = new TimeGaugeModel();
        model.setVariant(TimeGaugeVariant.INLINE);
        model.setShowTitle(false);
        model.setCurrentMinutes(Math.max(0, currentMinutes));
        model.setTargetMinutes(targetMinutes);
        model.setState(determineState(currentMinutes, targetMinutes));
        model.setStatusText(statusText == null ? "" : statusText);
        model.setShowStatus(false);
        return model;
    }

    public TimeGaugeModel buildInlineBarLongGauge(int currentMinutes, int targetMinutes) {
        TimeGaugeModel model = new TimeGaugeModel();
        model.setVariant(TimeGaugeVariant.INLINE_BAR_LONG);
        model.setShowTitle(false);
        TimeGaugeState state = determineState(currentMinutes, targetMinutes);
        TimeGaugeModel.GaugeRow row = new TimeGaugeModel.GaugeRow(null, Math.max(0, currentMinutes), targetMinutes);
        row.setState(state);
        model.addRow(row);
        return model;
    }

    private int loadSpentMinutesToday(Session dataSession, Calendar userCalendar, ProjectActionNext action) {
        Query query = dataSession.createQuery(
                "select sum(billMins) from BillEntry where action.actionNextId = :actionNextId "
                        + "and startTime >= :today and startTime < :tomorrow");
        query.setParameter("actionNextId", action.getActionNextId());

        Calendar calendar = (Calendar) userCalendar.clone();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        query.setParameter("today", calendar.getTime());
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        query.setParameter("tomorrow", calendar.getTime());

        @SuppressWarnings("unchecked")
        List<Long> billMinsList = query.list();
        if (billMinsList.size() > 0 && billMinsList.get(0) != null) {
            return billMinsList.get(0).intValue();
        }
        return 0;
    }

    private TimeGaugeState determineState(int currentMinutes, int targetMinutes) {
        if (targetMinutes <= 0) {
            return TimeGaugeState.UNKNOWN;
        }
        if (currentMinutes > targetMinutes) {
            return TimeGaugeState.OVER;
        }
        int percent = (int) Math.round((currentMinutes * 100.0) / targetMinutes);
        if (percent >= WARNING_PERCENT) {
            return TimeGaugeState.WARNING;
        }
        return TimeGaugeState.NORMAL;
    }

    private String formatDuration(int totalMinutes) {
        if (totalMinutes <= 0) {
            return "0m";
        }
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours == 0) {
            return minutes + "m";
        }
        if (minutes == 0) {
            return hours + "h";
        }
        return hours + "h " + minutes + "m";
    }
}