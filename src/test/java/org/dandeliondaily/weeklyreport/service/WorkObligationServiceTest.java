package org.dandeliondaily.weeklyreport.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.WorkObligation;

public class WorkObligationServiceTest {
    @Test
    public void resolvesDefaultMinutesWhenNoRowExistsForWeek() {
        WorkObligationService service = new WorkObligationService();
        Map<LocalDate, WorkObligation> raw = new LinkedHashMap<LocalDate, WorkObligation>();

        Assert.assertEquals(2250, service.resolveMinutes(raw, LocalDate.of(2026, 9, 6)));
    }

    @Test
    public void resolvesExplicitMinutesWhenRowExistsForWeek() {
        WorkObligationService service = new WorkObligationService();
        LocalDate week = LocalDate.of(2026, 9, 6);
        WorkObligation obligation = new WorkObligation();
        obligation.setObligatedMinutes(1800);
        Map<LocalDate, WorkObligation> raw = new LinkedHashMap<LocalDate, WorkObligation>();
        raw.put(week, obligation);

        Assert.assertEquals(1800, service.resolveMinutes(raw, week));
    }
}
