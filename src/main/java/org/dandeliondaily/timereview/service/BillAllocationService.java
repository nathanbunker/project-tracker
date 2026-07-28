package org.dandeliondaily.timereview.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.timereview.model.AllocationActualMinutes;
import org.dandeliondaily.timereview.model.AllocationProjectDrillDown;
import org.openimmunizationsoftware.pt.doa.BillAllocationDao;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.Project;

public class BillAllocationService {

    private final BillAllocationDao billAllocationDao;

    public BillAllocationService() {
        this(new BillAllocationDao());
    }

    public BillAllocationService(BillAllocationDao billAllocationDao) {
        this.billAllocationDao = billAllocationDao;
    }

    public List<AllocationActualMinutes> listActualMinutesByHistoricalBillCode(int workspaceId, int webUserId,
            Date startDate, Date endDateExclusive) {
        return billAllocationDao.listActualMinutesByHistoricalBillCode(workspaceId, webUserId, startDate,
                endDateExclusive);
    }

    public List<AllocationProjectDrillDown> listProjectDrillDownByHistoricalBillCode(int workspaceId, int webUserId,
            Date startDate, Date endDateExclusive) {
        return billAllocationDao.listProjectDrillDownByHistoricalBillCode(workspaceId, webUserId, startDate,
                endDateExclusive);
    }

    public List<AllocationProjectDrillDown> summarizeProjectDrillDown(List<BillEntry> entries,
            Map<Integer, Project> projectsById) {
        LinkedHashMap<String, AllocationProjectDrillDown> totals = new LinkedHashMap<String, AllocationProjectDrillDown>();
        if (entries == null) {
            return new ArrayList<AllocationProjectDrillDown>();
        }
        for (BillEntry entry : entries) {
            if (entry == null || entry.getBillCode() == null) {
                continue;
            }
            String key = entry.getBillCode() + "#" + entry.getProjectId();
            AllocationProjectDrillDown item = totals.get(key);
            if (item == null) {
                item = new AllocationProjectDrillDown();
                item.setBillCode(entry.getBillCode());
                item.setProjectId(entry.getProjectId());
                Project project = projectsById == null ? null : projectsById.get(Integer.valueOf(entry.getProjectId()));
                item.setProjectLabel(project == null ? null : project.getProjectName());
                item.setFirstEntryDate(entry.getStartTime());
                item.setLastEntryDate(entry.getStartTime());
                totals.put(key, item);
            }
            item.setTotalMinutes(
                    item.getTotalMinutes() + (entry.getBillMins() == null ? 0 : entry.getBillMins().intValue()));
            if (entry.getStartTime() != null
                    && (item.getFirstEntryDate() == null || entry.getStartTime().before(item.getFirstEntryDate()))) {
                item.setFirstEntryDate(entry.getStartTime());
            }
            if (entry.getStartTime() != null
                    && (item.getLastEntryDate() == null || entry.getStartTime().after(item.getLastEntryDate()))) {
                item.setLastEntryDate(entry.getStartTime());
            }
        }
        return new ArrayList<AllocationProjectDrillDown>(totals.values());
    }
}