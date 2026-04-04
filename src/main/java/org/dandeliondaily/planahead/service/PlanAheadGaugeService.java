package org.dandeliondaily.planahead.service;

import org.dandeliondaily.dashboard.model.TimeGaugeModel;
import org.dandeliondaily.dashboard.model.TimeGaugeState;
import org.dandeliondaily.dashboard.model.TimeGaugeVariant;

public class PlanAheadGaugeService {

    private static final int WARNING_PERCENT = 85;

    public TimeGaugeModel buildDayGauge(int plannedMinutes, int availableMinutes) {
        TimeGaugeModel model = new TimeGaugeModel();
        model.setVariant(TimeGaugeVariant.STACKED);
        model.setShowTitle(false);
        model.setShowStatus(false);
        model.setShowTargetRange(true);
        int target = Math.max(plannedMinutes + availableMinutes, 0);
        int current = Math.max(0, plannedMinutes);
        TimeGaugeState state = determineState(current, target);
        model.setCurrentMinutes(current);
        model.setTargetMinutes(target);
        model.setState(state);
        return model;
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
}
