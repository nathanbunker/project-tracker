package org.dandeliondaily.weeklyreport.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.weeklyreport.model.WeeklyTimeSummary;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.manager.TimeEntry;
import org.openimmunizationsoftware.pt.model.WebUser;

public class WeeklyTimeSummaryService {

    @SuppressWarnings("unchecked")
    public List<WeeklyTimeSummary> load(Session session, WebUser owner, int workspaceId, String billCodePrefix,
            LocalDate firstWeekStart, int weekCount) {
        return load(session, owner, workspaceId, billCodePrefix, firstWeekStart, weekCount,
                firstWeekStart, firstWeekStart.plusWeeks(weekCount));
    }

    @SuppressWarnings("unchecked")
    public List<WeeklyTimeSummary> load(Session session, WebUser owner, int workspaceId, String billCodePrefix,
            LocalDate firstWeekStart, int weekCount, LocalDate dataStartInclusive, LocalDate dataEndExclusive) {
        if (weekCount <= 0) {
            return Collections.emptyList();
        }
        LocalDate endExclusive = firstWeekStart.plusWeeks(weekCount);
        LocalDate queryStart = dataStartInclusive.isAfter(firstWeekStart) ? dataStartInclusive : firstWeekStart;
        LocalDate queryEnd = dataEndExclusive.isBefore(endExclusive) ? dataEndExclusive : endExclusive;
        Query query = session.createQuery(
                "select be.startTime, be.projectId, p.projectName, p.projectHandle, be.billCode, be.billMins "
                        + "from BillEntry be, Project p where be.projectId = p.projectId "
                        + "and be.workspaceId = :workspaceId and be.webUser.webUserId = :webUserId "
                        + "and be.billable = 'Y' and be.billMins > 0 and be.startTime >= :start "
                        + "and be.startTime < :end order by be.startTime, be.projectId");
        query.setInteger("workspaceId", workspaceId);
        query.setInteger("webUserId", owner.getWebUserId());
        query.setTimestamp("start", toDate(queryStart, owner.getZoneId()));
        query.setTimestamp("end", toDate(queryEnd, owner.getZoneId()));
        List<Object[]> rows = query.list();
        return summarizeRows(toRows(rows, owner.getZoneId()), billCodePrefix, firstWeekStart, weekCount);
    }

    public List<WeeklyTimeSummary> summarizeRows(List<TimeRow> rows, String billCodePrefix,
            LocalDate firstWeekStart, int weekCount) {
        List<WeeklyTimeSummary> summaries = new ArrayList<WeeklyTimeSummary>();
        Map<LocalDate, Map<Integer, ProjectBucket>> bucketsByWeek = new LinkedHashMap<LocalDate, Map<Integer, ProjectBucket>>();
        for (int index = 0; index < weekCount; index++) {
            LocalDate week = firstWeekStart.plusWeeks(index);
            summaries.add(new WeeklyTimeSummary(week));
            bucketsByWeek.put(week, new LinkedHashMap<Integer, ProjectBucket>());
        }
        if (rows != null) {
            for (TimeRow row : rows) {
                if (row == null || row.getDate() == null || row.getMinutes() <= 0)
                    continue;
                long dayOffset = ChronoUnit.DAYS.between(firstWeekStart, row.getDate());
                if (dayOffset < 0 || dayOffset >= weekCount * 7L)
                    continue;
                LocalDate week = firstWeekStart.plusWeeks(dayOffset / 7L);
                Map<Integer, ProjectBucket> weekBuckets = bucketsByWeek.get(week);
                ProjectBucket project = weekBuckets.get(Integer.valueOf(row.getProjectId()));
                if (project == null) {
                    project = new ProjectBucket(row.getProjectId(), row.getProjectName(), row.getProjectHandle());
                    weekBuckets.put(Integer.valueOf(row.getProjectId()), project);
                }
                project.add(row.getBillCode(), row.getMinutes());
            }
        }
        for (WeeklyTimeSummary summary : summaries) {
            buildSummary(summary, bucketsByWeek.get(summary.getWeekStart()), billCodePrefix);
        }
        return summaries;
    }

    private static void buildSummary(WeeklyTimeSummary summary, Map<Integer, ProjectBucket> projects,
            String billCodePrefix) {
        for (ProjectBucket project : projects.values()) {
            int roundedProjectMinutes = TimeEntry.adjustMinutes(project.rawMinutes);
            summary.setAllWorkedMinutes(summary.getAllWorkedMinutes() + roundedProjectMinutes);
            Map<String, Integer> allocated = allocateRoundedMinutes(project.byBillCode, roundedProjectMinutes);
            for (Map.Entry<String, Integer> entry : allocated.entrySet()) {
                if (!matchesPrefix(entry.getKey(), billCodePrefix))
                    continue;
                int roundedMinutes = entry.getValue().intValue();
                summary.setScopedMinutes(summary.getScopedMinutes() + roundedMinutes);
                Integer existing = summary.getMinutesByBillCode().get(entry.getKey());
                summary.getMinutesByBillCode().put(entry.getKey(), Integer.valueOf(
                        (existing == null ? 0 : existing.intValue()) + roundedMinutes));
                summary.getProjects().add(new WeeklyTimeSummary.ProjectTime(project.projectId, project.projectName,
                        project.projectHandle, entry.getKey(), project.byBillCode.get(entry.getKey()).intValue(),
                        roundedMinutes));
            }
        }
        summary.sort();
    }

    static Map<String, Integer> allocateRoundedMinutes(Map<String, Integer> rawByCode, int roundedTotal) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        int rawTotal = 0;
        for (Integer value : rawByCode.values())
            rawTotal += value.intValue();
        if (rawTotal <= 0 || roundedTotal <= 0) {
            for (String code : rawByCode.keySet())
                result.put(code, Integer.valueOf(0));
            return result;
        }
        List<AllocationRemainder> remainders = new ArrayList<AllocationRemainder>();
        int allocated = 0;
        for (Map.Entry<String, Integer> entry : rawByCode.entrySet()) {
            long numerator = (long) roundedTotal * entry.getValue().intValue();
            int base = (int) (numerator / rawTotal);
            result.put(entry.getKey(), Integer.valueOf(base));
            allocated += base;
            remainders.add(new AllocationRemainder(entry.getKey(), numerator % rawTotal));
        }
        Collections.sort(remainders, new Comparator<AllocationRemainder>() {
            public int compare(AllocationRemainder left, AllocationRemainder right) {
                int remainder = Long.compare(right.remainder, left.remainder);
                return remainder != 0 ? remainder : left.billCode.compareTo(right.billCode);
            }
        });
        for (int index = 0; allocated < roundedTotal; index++, allocated++) {
            String code = remainders.get(index % remainders.size()).billCode;
            result.put(code, Integer.valueOf(result.get(code).intValue() + 1));
        }
        return result;
    }

    public static boolean matchesPrefix(String billCode, String prefix) {
        return billCode != null && prefix != null && billCode.startsWith(prefix);
    }

    private static List<TimeRow> toRows(List<Object[]> rows, ZoneId zoneId) {
        List<TimeRow> result = new ArrayList<TimeRow>();
        for (Object[] row : rows) {
            Date start = (Date) row[0];
            result.add(new TimeRow(start.toInstant().atZone(zoneId).toLocalDate(), ((Number) row[1]).intValue(),
                    (String) row[2], (String) row[3], (String) row[4], ((Number) row[5]).intValue()));
        }
        return result;
    }

    private static Date toDate(LocalDate date, ZoneId zoneId) {
        return Date.from(date.atStartOfDay(zoneId).toInstant());
    }

    private static class ProjectBucket {
        private final int projectId;
        private final String projectName;
        private final String projectHandle;
        private int rawMinutes;
        private final Map<String, Integer> byBillCode = new HashMap<String, Integer>();

        private ProjectBucket(int projectId, String projectName, String projectHandle) {
            this.projectId = projectId;
            this.projectName = projectName;
            this.projectHandle = projectHandle;
        }

        private void add(String billCode, int minutes) {
            rawMinutes += minutes;
            Integer existing = byBillCode.get(billCode);
            byBillCode.put(billCode, Integer.valueOf((existing == null ? 0 : existing.intValue()) + minutes));
        }
    }

    private static class AllocationRemainder {
        private final String billCode;
        private final long remainder;

        private AllocationRemainder(String billCode, long remainder) {
            this.billCode = billCode;
            this.remainder = remainder;
        }
    }

    public static class TimeRow {
        private final LocalDate date;
        private final int projectId;
        private final String projectName;
        private final String projectHandle;
        private final String billCode;
        private final int minutes;

        public TimeRow(LocalDate date, int projectId, String projectName, String projectHandle, String billCode,
                int minutes) {
            this.date = date;
            this.projectId = projectId;
            this.projectName = projectName;
            this.projectHandle = projectHandle;
            this.billCode = billCode;
            this.minutes = minutes;
        }

        public LocalDate getDate() {
            return date;
        }

        public int getProjectId() {
            return projectId;
        }

        public String getProjectName() {
            return projectName;
        }

        public String getProjectHandle() {
            return projectHandle;
        }

        public String getBillCode() {
            return billCode;
        }

        public int getMinutes() {
            return minutes;
        }
    }
}