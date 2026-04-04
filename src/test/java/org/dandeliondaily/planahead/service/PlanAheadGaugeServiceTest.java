package org.dandeliondaily.planahead.service;

import org.dandeliondaily.dashboard.model.TimeGaugeModel;
import org.dandeliondaily.dashboard.model.TimeGaugeState;
import org.dandeliondaily.dashboard.model.TimeGaugeVariant;
import org.junit.Assert;
import org.junit.Test;

public class PlanAheadGaugeServiceTest {

    private final PlanAheadGaugeService service = new PlanAheadGaugeService();

    // ─── state determination ─────────────────────────────────────────────────

    @Test
    public void buildDayGauge_normalState_whenWellUnderWarningThreshold() {
        TimeGaugeModel model = service.buildDayGauge(240, 240);
        Assert.assertEquals(TimeGaugeState.NORMAL, model.getState());
    }

    @Test
    public void buildDayGauge_warningState_whenAtWarningThreshold() {
        // planned=85 of 100 total → 85% → WARNING
        TimeGaugeModel model = service.buildDayGauge(85, 15);
        Assert.assertEquals(TimeGaugeState.WARNING, model.getState());
    }

    @Test
    public void buildDayGauge_warningState_whenAtFullCapacity() {
        // planned=480, available=0 → target=480, current=480 → 100% → WARNING
        TimeGaugeModel model = service.buildDayGauge(480, 0);
        Assert.assertEquals(TimeGaugeState.WARNING, model.getState());
    }

    @Test
    public void buildDayGauge_unknownState_whenTargetIsZero() {
        // planned=0, available=0 → target=0 → UNKNOWN
        TimeGaugeModel model = service.buildDayGauge(0, 0);
        Assert.assertEquals(TimeGaugeState.UNKNOWN, model.getState());
    }

    // ─── model values ────────────────────────────────────────────────────────

    @Test
    public void buildDayGauge_currentEqualsPlannedMinutes() {
        TimeGaugeModel model = service.buildDayGauge(120, 360);
        Assert.assertEquals(120, model.getCurrentMinutes());
    }

    @Test
    public void buildDayGauge_targetEqualsPlannedPlusAvailable() {
        TimeGaugeModel model = service.buildDayGauge(120, 360);
        Assert.assertEquals(480, model.getTargetMinutes());
    }

    @Test
    public void buildDayGauge_negativeAvailableClampsToZero() {
        // available=-60 means day is overbooked; target should stay the configured
        // workday minutes
        TimeGaugeModel model = service.buildDayGauge(540, -60);
        Assert.assertEquals(480, model.getTargetMinutes());
        Assert.assertEquals(TimeGaugeState.OVER, model.getState());
    }

    @Test
    public void buildDayGauge_doesNotUseRowsForStackedVariant() {
        TimeGaugeModel model = service.buildDayGauge(60, 420);
        Assert.assertEquals(0, model.getRows().size());
    }

    // ─── gauge recomputation deltas (Phase 7 contract) ───────────────────────

    @Test
    public void buildDayGauge_planningMoreMinutesIncreasesCurrentAndMaintainsTarget() {
        TimeGaugeModel before = service.buildDayGauge(100, 380);
        TimeGaugeModel after = service.buildDayGauge(200, 280);
        Assert.assertEquals(before.getTargetMinutes(), after.getTargetMinutes());
        Assert.assertTrue(after.getCurrentMinutes() > before.getCurrentMinutes());
    }

    @Test
    public void buildDayGauge_emptyDayProducesNormalStateWithZeroPlanned() {
        // Non-working day: planned=0, available=0 → target=0 → UNKNOWN (no work
        // planned)
        TimeGaugeModel model = service.buildDayGauge(0, 0);
        Assert.assertEquals(TimeGaugeState.UNKNOWN, model.getState());
    }

    @Test
    public void buildDayGauge_usesStackedVariantWithTargetRange() {
        TimeGaugeModel model = service.buildDayGauge(120, 360);
        Assert.assertEquals(TimeGaugeVariant.STACKED, model.getVariant());
        Assert.assertTrue(model.isShowTargetRange());
    }
}
