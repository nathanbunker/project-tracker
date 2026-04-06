package org.dandeliondaily.dashboard.service;

import org.dandeliondaily.dashboard.model.TimeGaugeModel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DashboardTimeGaugeServiceTest {

    private DashboardTimeGaugeService service;

    @Before
    public void setUp() {
        service = new DashboardTimeGaugeService();
    }

    @Test
    public void buildPlannedDayGauge_usesOverrideTargetWhenPositive() {
        TimeGaugeModel model = service.buildPlannedDayGauge(180, 300);
        Assert.assertEquals(300, model.getTargetMinutes());
        Assert.assertEquals("2h left", model.getStatusText());
    }

    @Test
    public void buildPlannedDayGauge_usesDefaultTargetWhenOverrideInvalid() {
        TimeGaugeModel model = service.buildPlannedDayGauge(120, 0);
        Assert.assertEquals(service.getDefaultDailyTargetMinutes(), model.getTargetMinutes());
    }

    @Test
    public void updateTodayGaugePlanned_usesProvidedTargetOnPlannedRow() {
        TimeGaugeModel model = new TimeGaugeModel();
        model.addRow(new TimeGaugeModel.GaugeRow("Spent", 100, 200));
        model.addRow(new TimeGaugeModel.GaugeRow("Planned", 0, 200));

        service.updateTodayGaugePlanned(model, 150, 300);

        Assert.assertEquals(2, model.getRows().size());
        Assert.assertEquals(150, model.getRows().get(1).getCurrentMinutes());
        Assert.assertEquals(300, model.getRows().get(1).getTargetMinutes());
    }

    @Test
    public void buildInlineBarLongGauge_usesDefaultTargetWhenInvalid() {
        TimeGaugeModel model = service.buildInlineBarLongGauge(50, -1);
        Assert.assertEquals(1, model.getRows().size());
        Assert.assertEquals(service.getDefaultDailyTargetMinutes(), model.getRows().get(0).getTargetMinutes());
    }
}
