package org.dandeliondaily.mcp.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.planahead.service.PlanAheadDayCapacityService;
import org.dandeliondaily.planahead.service.TemplateGenerationService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.BillExpected;
import org.openimmunizationsoftware.pt.model.BillExpectedId;

/**
 * Reads and writes BillExpected (daily availability) for MCP, without the
 * AppReq/web-session coupling of PlanAheadDayCapacityService, and without
 * that class's internal beginTransaction()/commit() calls, which would
 * conflict with the single ambient transaction HibernateSessionFilter
 * already holds open for the whole MCP request (see assessment section 6,
 * decision 16).
 */
public class McpDayAvailabilityService {

    private static final int DEFAULT_WORKING_MINUTES = PlanAheadDayCapacityService.DEFAULT_DAILY_TARGET_MINUTES;

    public static class DayAvailabilityInput {
        private final LocalDate date;
        private final String workStatus;
        private final Integer billMinutes;

        public DayAvailabilityInput(LocalDate date, String workStatus, Integer billMinutes) {
            this.date = date;
            this.workStatus = workStatus;
            this.billMinutes = billMinutes;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getWorkStatus() {
            return workStatus;
        }

        public Integer getBillMinutes() {
            return billMinutes;
        }
    }

    public List<Map<String, Object>> getAvailability(Session session, int webUserId, LocalDate startInclusive,
            LocalDate endInclusive) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (LocalDate day = startInclusive; !day.isAfter(endInclusive); day = day.plusDays(1)) {
            BillExpected billExpected = findBillExpected(session, webUserId, day);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("date", day.toString());
            if (billExpected == null) {
                item.put("billMinutes", defaultMinutes(day));
                item.put("workStatus", defaultStatus(day));
                item.put("isDefault", Boolean.TRUE);
            } else {
                item.put("billMinutes", billExpected.getBillMins());
                item.put("workStatus", normalizeStatus(billExpected.getWorkStatus(), day));
                item.put("isDefault", Boolean.FALSE);
            }
            result.add(item);
        }
        return result;
    }

    /**
     * Upserts each day's availability, then re-runs the same forward-window
     * template regeneration the existing UI triggers after a capacity change
     * (PlanAheadServlet#syncTemplatesAfterCapacityChange), so recurring-action
     * eligibility never drifts out of sync with what was just set.
     */
    public void setAvailability(Session session, int workspaceId, int webUserId, int contactId,
            List<DayAvailabilityInput> days) {
        if (days.isEmpty()) {
            throw new McpToolException("invalid_arguments", "At least one day is required.");
        }
        LocalDate latestDay = null;
        for (DayAvailabilityInput day : days) {
            if (day.getDate() == null) {
                throw new McpToolException("invalid_arguments", "Each day requires a date.");
            }
            String normalizedStatus = normalizeStatusForInput(day.getWorkStatus());
            if (normalizedStatus == null) {
                throw new McpToolException("invalid_arguments",
                        "workStatus must be one of W, N, V, H, T, S for " + day.getDate() + ".");
            }
            int billMinutes = resolveBillMinutes(day, normalizedStatus);
            upsertBillExpected(session, webUserId, day.getDate(), billMinutes, normalizedStatus);
            if (latestDay == null || day.getDate().isAfter(latestDay)) {
                latestDay = day.getDate();
            }
        }

        LocalDate today = LocalDate.now();
        int advanceDays;
        try {
            advanceDays = Integer.parseInt(TrackerKeysManager.getKeyValue(
                    TrackerKeysManager.KEY_TEMPLATE_ADVANCE_DAYS, TrackerKeysManager.KEY_TYPE_GLOBAL,
                    TrackerKeysManager.KEY_ID_GLOBAL, "14", session).trim());
        } catch (NumberFormatException nfe) {
            advanceDays = 14;
        }
        long daysUntilLatest = java.time.temporal.ChronoUnit.DAYS.between(today, latestDay);
        if (daysUntilLatest > advanceDays) {
            advanceDays = daysUntilLatest > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) daysUntilLatest;
        }
        new TemplateGenerationService().generateForwardWindow(session, workspaceId, contactId, today,
                Math.max(advanceDays, 0));
    }

    private int resolveBillMinutes(DayAvailabilityInput day, String normalizedStatus) {
        if (day.getBillMinutes() != null) {
            return Math.max(0, day.getBillMinutes().intValue());
        }
        return PlanAheadDayCapacityService.STATUS_WORKING.equals(normalizedStatus) ? DEFAULT_WORKING_MINUTES : 0;
    }

    private void upsertBillExpected(Session session, int webUserId, LocalDate day, int billMinutes,
            String workStatus) {
        java.util.Date billDate = java.sql.Date.valueOf(day);
        BillExpected billExpected = findBillExpected(session, webUserId, day);
        if (billExpected == null) {
            billExpected = new BillExpected(new BillExpectedId(webUserId, billDate), billMinutes, 0, workStatus);
            session.save(billExpected);
        } else {
            billExpected.setBillMins(billMinutes);
            billExpected.setWorkStatus(workStatus);
            session.update(billExpected);
        }
    }

    private BillExpected findBillExpected(Session session, int webUserId, LocalDate day) {
        Query query = session.createQuery("from BillExpected where id.webUserId = :webUserId and id.billDate = :billDate");
        query.setInteger("webUserId", webUserId);
        query.setParameter("billDate", java.sql.Date.valueOf(day));
        @SuppressWarnings("unchecked")
        List<BillExpected> results = query.list();
        return results.isEmpty() ? null : results.get(0);
    }

    private int defaultMinutes(LocalDate day) {
        return isWeekend(day) ? 0 : DEFAULT_WORKING_MINUTES;
    }

    private String defaultStatus(LocalDate day) {
        return isWeekend(day) ? PlanAheadDayCapacityService.STATUS_NOT_WORKING
                : PlanAheadDayCapacityService.STATUS_WORKING;
    }

    private String normalizeStatus(String value, LocalDate day) {
        String normalized = normalizeStatusForInput(value);
        return normalized == null ? defaultStatus(day) : normalized;
    }

    private String normalizeStatusForInput(String value) {
        if (value == null) {
            return null;
        }
        String upper = value.trim().toUpperCase();
        if (PlanAheadDayCapacityService.STATUS_WORKING.equals(upper)
                || PlanAheadDayCapacityService.STATUS_NOT_WORKING.equals(upper)
                || PlanAheadDayCapacityService.STATUS_VACATION.equals(upper)
                || PlanAheadDayCapacityService.STATUS_HOLIDAY.equals(upper)
                || PlanAheadDayCapacityService.STATUS_TRAVELING.equals(upper)
                || PlanAheadDayCapacityService.STATUS_SICK.equals(upper)) {
            return upper;
        }
        return null;
    }

    private boolean isWeekend(LocalDate day) {
        DayOfWeek dow = day.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}
