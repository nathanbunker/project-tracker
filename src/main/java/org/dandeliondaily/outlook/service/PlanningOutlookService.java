package org.dandeliondaily.outlook.service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.doa.PlanningOutlookDao;
import org.openimmunizationsoftware.pt.model.PlanningOutlook;

/**
 * Owner-scoped monthly/weekly planning outlooks. Latest-wins per period, no
 * revision history, no links to projects (see docs/Dandelion_Daily_AI_Integration_Assessment.md, section 6).
 */
public class PlanningOutlookService {

    public static final String PERIOD_TYPE_WEEK = "WEEK";
    public static final String PERIOD_TYPE_MONTH = "MONTH";

    public static class OutlookResult {
        private final PlanningOutlook outlook;
        private final String periodType;
        private final LocalDate periodStart;
        private final LocalDate periodEnd;
        private final boolean frozen;

        public OutlookResult(PlanningOutlook outlook, String periodType, LocalDate periodStart,
                LocalDate periodEnd, boolean frozen) {
            this.outlook = outlook;
            this.periodType = periodType;
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
            this.frozen = frozen;
        }

        public PlanningOutlook getOutlook() {
            return outlook;
        }

        public String getPeriodType() {
            return periodType;
        }

        public LocalDate getPeriodStart() {
            return periodStart;
        }

        public LocalDate getPeriodEnd() {
            return periodEnd;
        }

        public boolean isFrozen() {
            return frozen;
        }
    }

    public String normalizePeriodType(String periodType) {
        if (periodType == null) {
            return null;
        }
        String normalized = periodType.trim().toUpperCase();
        if (PERIOD_TYPE_WEEK.equals(normalized) || PERIOD_TYPE_MONTH.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    /** Last day (inclusive) of the given period. */
    public LocalDate periodEnd(String periodType, LocalDate periodStart) {
        if (PERIOD_TYPE_MONTH.equals(periodType)) {
            return periodStart.with(TemporalAdjusters.lastDayOfMonth());
        }
        return periodStart.plusDays(6);
    }

    public boolean isFrozen(String periodType, LocalDate periodStart, LocalDate today) {
        return today.isAfter(periodEnd(periodType, periodStart));
    }

    public OutlookResult getOutlook(Session session, int ownerUserId, String periodType, LocalDate periodStart,
            LocalDate today) {
        String normalizedType = requireValidPeriodType(periodType);
        PlanningOutlook outlook = new PlanningOutlookDao(session).findForPeriod(ownerUserId, normalizedType,
                periodStart);
        LocalDate end = periodEnd(normalizedType, periodStart);
        return new OutlookResult(outlook, normalizedType, periodStart, end, today.isAfter(end));
    }

    public List<OutlookResult> listOutlooks(Session session, int ownerUserId, String periodType,
            LocalDate startInclusive, LocalDate endExclusive, LocalDate today) {
        String normalizedType = requireValidPeriodType(periodType);
        List<PlanningOutlook> rows = new PlanningOutlookDao(session).listForRange(ownerUserId, normalizedType,
                startInclusive, endExclusive);
        List<OutlookResult> results = new ArrayList<OutlookResult>();
        for (PlanningOutlook row : rows) {
            LocalDate periodStart = row.getPeriodStartLocalDate();
            LocalDate end = periodEnd(normalizedType, periodStart);
            results.add(new OutlookResult(row, normalizedType, periodStart, end, today.isAfter(end)));
        }
        return results;
    }

    /**
     * Upserts the outlook text for a period. Throws IllegalStateException if the
     * period has already elapsed (frozen) as of {@code today}.
     */
    public PlanningOutlook setOutlook(Session session, int ownerUserId, String periodType, LocalDate periodStart,
            String outlookText, LocalDate today) {
        String normalizedType = requireValidPeriodType(periodType);
        if (isFrozen(normalizedType, periodStart, today)) {
            throw new IllegalStateException("This " + normalizedType.toLowerCase()
                    + " outlook has already ended (through " + periodEnd(normalizedType, periodStart)
                    + ") and can no longer be edited.");
        }
        PlanningOutlookDao dao = new PlanningOutlookDao(session);
        PlanningOutlook outlook = dao.findForPeriod(ownerUserId, normalizedType, periodStart);
        Date now = new Date();
        if (outlook == null) {
            outlook = new PlanningOutlook();
            outlook.setOwnerUserId(ownerUserId);
            outlook.setPeriodType(normalizedType);
            outlook.setPeriodStartLocalDate(periodStart);
            outlook.setOutlookText(outlookText);
            outlook.setCreatedAt(now);
            outlook.setUpdatedAt(now);
            dao.save(outlook);
        } else {
            outlook.setOutlookText(outlookText);
            outlook.setUpdatedAt(now);
            dao.update(outlook);
        }
        return outlook;
    }

    private String requireValidPeriodType(String periodType) {
        String normalized = normalizePeriodType(periodType);
        if (normalized == null) {
            throw new IllegalArgumentException("periodType must be WEEK or MONTH.");
        }
        return normalized;
    }
}
