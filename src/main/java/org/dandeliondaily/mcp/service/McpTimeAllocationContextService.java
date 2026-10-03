package org.dandeliondaily.mcp.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel;
import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel.ActivityItem;
import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel.AllocationRow;
import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel.ProjectActivity;
import org.dandeliondaily.weeklyreport.model.WeeklyTimeSummary;
import org.dandeliondaily.weeklyreport.service.WeeklyReportDataService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.WeeklyReport;

/**
 * Maps WeeklyReportDataService's already-computed view model (the same data
 * shown at /WeeklyReportServlet) to a structured MCP result: how time is
 * allocated (BillPlan/BillPlanTarget targets per bill code), which projects
 * are under those allocations, and what has happened recently. See
 * docs/Dandelion_Daily_AI_Integration_Assessment.md section 4.
 */
public class McpTimeAllocationContextService {

    private final WeeklyReportDataService dataService = new WeeklyReportDataService();

    public Map<String, Object> getTimeAllocationContext(Session session, int workspaceId, String username,
            LocalDate weekStart) {
        WeeklyReport report = requireWeeklyReport(session, workspaceId);
        WebUser owner = McpWebUserSupport.requireWebUser(session, username);
        LocalDate resolvedWeekStart = weekStart != null ? weekStart
                : LocalDate.now(owner.getZoneId()).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        WeeklyReportViewModel model = dataService.load(session, report, owner, resolvedWeekStart);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("weekStart", resolvedWeekStart.toString());
        result.put("fiscalYear", toFiscalYear(model));
        result.put("obligatedMinutes", model.getObligatedMinutes());
        result.put("obligatedIsDefault", model.isObligatedIsDefault());
        result.put("obligatedNote", model.getObligatedNote());
        result.put("fourWeekObligatedMinutes", model.getFourWeekObligatedMinutes());
        result.put("fiscalYearObligatedMinutes", model.getFiscalYearObligatedMinutes());
        result.put("fourWeekAllWorkedMinutes", model.getFourWeekAllWorkedMinutes());
        result.put("fiscalYearAllWorkedMinutes", model.getFiscalYearAllWorkedMinutes());
        result.put("allocationRows", toAllocationRows(model));
        result.put("projectActivity", toProjectActivity(model));
        result.put("fiscalProjectMinutes", toFiscalProjectMinutes(model));
        result.put("history", toHistory(model));
        return result;
    }

    private List<Map<String, Object>> toAllocationRows(WeeklyReportViewModel model) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (AllocationRow row : model.getAllocationRows().values()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("billCode", row.getBillCode());
            item.put("billLabel", row.getBillLabel());
            item.put("fundingSource", row.getFundingSource());
            item.put("weekMinutes", row.getWeekMinutes());
            item.put("fourWeekMinutes", row.getFourWeekMinutes());
            item.put("fiscalYearMinutes", row.getFiscalYearMinutes());
            item.put("weekPercentOfObligated", row.getWeekPercent());
            item.put("fourWeekPercentOfObligated", row.getFourWeekPercent());
            item.put("fiscalYearPercentOfObligated", row.getFiscalYearPercent());
            item.put("annualTargetPercent", row.getAnnualTargetPercent());
            item.put("steeringTargetPercent", row.getSteeringTargetPercent());
            rows.add(item);
        }
        return rows;
    }

    private List<Map<String, Object>> toProjectActivity(WeeklyReportViewModel model) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (ProjectActivity activity : model.getProjectActivities().values()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("projectId", activity.getProjectId());
            item.put("projectName", activity.getProject() == null ? null : activity.getProject().getProjectName());
            item.put("billCode", activity.getBillCode());
            item.put("weekMinutes", activity.getRoundedMinutes());
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            for (ActivityItem activityItem : activity.getCompletedItems()) {
                Map<String, Object> entry = new LinkedHashMap<String, Object>();
                entry.put("date", McpActionContextSupport.toIso(activityItem.getDate()));
                entry.put("description", activityItem.getDescription());
                entry.put("notes", activityItem.getNotes());
                entry.put("completedAction", activityItem.isCompletedAction());
                items.add(entry);
            }
            item.put("completedItems", items);
            rows.add(item);
        }
        return rows;
    }

    private Map<String, Object> toFiscalYear(WeeklyReportViewModel model) {
        if (model.getFiscalStartDate() == null) {
            return null;
        }
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("planCode", model.getFiscalPlanCode());
        item.put("planLabel", model.getFiscalPlanLabel());
        item.put("startDate", model.getFiscalStartDate().toString());
        item.put("endDate", model.getFiscalEndDate() == null ? null : model.getFiscalEndDate().toString());
        item.put("countedThrough", model.getWeekStart().plusDays(6).toString());
        return item;
    }

    private List<Map<String, Object>> toFiscalProjectMinutes(WeeklyReportViewModel model) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : model.getFiscalProjectMinutes().entrySet()) {
            int minutes = entry.getValue().intValue();
            if (minutes == 0) {
                continue;
            }
            String[] keyParts = entry.getKey().split("\u0000", 2);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("projectId", Integer.valueOf(keyParts[1]));
            item.put("projectName", model.getFiscalProjectNames().get(entry.getKey()));
            item.put("billCode", keyParts[0]);
            item.put("fiscalYearMinutes", minutes);
            rows.add(item);
        }
        Collections.sort(rows, new Comparator<Map<String, Object>>() {
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                int code = ((String) left.get("billCode")).compareTo((String) right.get("billCode"));
                return code != 0 ? code
                        : ((Integer) right.get("fiscalYearMinutes")).compareTo((Integer) left.get("fiscalYearMinutes"));
            }
        });
        return rows;
    }

    private List<Map<String, Object>> toHistory(WeeklyReportViewModel model) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (WeeklyTimeSummary week : model.getHistory()) {
            Integer obligated = model.getObligatedMinutesByWeek().get(week.getWeekStart());
            int obligatedMinutes = obligated == null ? 0 : obligated.intValue();
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("weekStart", week.getWeekStart().toString());
            item.put("allWorkedMinutes", week.getAllWorkedMinutes());
            item.put("obligatedMinutes", obligatedMinutes);
            item.put("varianceMinutes", week.getAllWorkedMinutes() - obligatedMinutes);
            rows.add(item);
        }
        return rows;
    }

    private WeeklyReport requireWeeklyReport(Session session, int workspaceId) {
        Query query = session.createQuery(
                "from WeeklyReport where rootWorkspaceId = :workspaceId and active = 'Y'");
        query.setInteger("workspaceId", workspaceId);
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<WeeklyReport> results = query.list();
        if (results.isEmpty()) {
            throw new McpToolException("weekly_report_not_configured",
                    "No active weekly report is configured for this workspace.");
        }
        return results.get(0);
    }
}
