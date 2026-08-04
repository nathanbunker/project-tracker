package org.dandeliondaily.planahead.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.pt.model.BillExpected;

final class TemplateWorkdayCalendar {

    private final Map<LocalDate, BillExpected> availabilityByDate = new HashMap<LocalDate, BillExpected>();

    TemplateWorkdayCalendar(List<BillExpected> availabilityRows) {
        if (availabilityRows == null) {
            return;
        }
        for (BillExpected row : availabilityRows) {
            if (row != null && row.getId() != null && row.getId().getBillDate() != null) {
                availabilityByDate.put(TemplateGenerationService.toLocalDate(row.getId().getBillDate()), row);
            }
        }
    }

    boolean isEligible(boolean billable, LocalDate date) {
        if (!billable) {
            return true;
        }
        BillExpected availability = availabilityByDate.get(date);
        if (availability == null) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
        }
        return PlanAheadDayCapacityService.STATUS_WORKING.equals(availability.getWorkStatus())
                && availability.getBillMins() > 0;
    }
}