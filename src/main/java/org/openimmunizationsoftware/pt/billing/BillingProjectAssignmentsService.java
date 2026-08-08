package org.openimmunizationsoftware.pt.billing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.AssignmentCell;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.BillCodeColumn;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.CadenceRow;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard.ProjectChip;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.ReviewInterval;
import org.openimmunizationsoftware.pt.model.WebUser;

public class BillingProjectAssignmentsService {

    public BillingProjectAssignmentsBoard buildBoard(AppReq appReq) {
        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();
        Integer workspaceId = resolvePrivateWorkspaceId(dataSession, webUser);
        if (workspaceId == null) {
            throw new IllegalArgumentException("Private workspace was not found.");
        }

        List<BillCode> billCodes = loadEligibleBillCodes(dataSession, workspaceId.intValue());
        List<Project> projects = loadProjects(dataSession, workspaceId.intValue(), billCodes);
        Map<Integer, Integer> updateDueByProject = loadUpdateDueByProject(dataSession, webUser, projects);
        return assembleBoard(billCodes, projects, updateDueByProject);
    }

    public MoveResult moveProject(AppReq appReq, int projectId, String targetBillCode, int updateEveryDays) {
        WebUser webUser = appReq.getWebUser();
        if (webUser == null || !webUser.isUserTypeAdmin()) {
            throw new IllegalArgumentException("Administrator access is required.");
        }

        Session dataSession = appReq.getDataSession();
        Integer workspaceId = resolvePrivateWorkspaceId(dataSession, webUser);
        if (workspaceId == null) {
            throw new IllegalArgumentException("Private workspace was not found.");
        }

        BillingProjectAssignmentsBoard board = buildBoard(appReq);
        if (!containsBillCode(board, targetBillCode)) {
            throw new IllegalArgumentException("The selected bill code is not visible and billable.");
        }
        if (!containsCadence(board, updateEveryDays)) {
            throw new IllegalArgumentException("The selected update cadence is not available.");
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            Project project = (Project) dataSession.get(Project.class, projectId);
            if (project == null || project.getWorkspaceId() == null
                    || !workspaceId.equals(project.getWorkspaceId())) {
                throw new IllegalArgumentException("Project was not found in your private workspace.");
            }
            if (ProjectStatus.fromDatabaseValue(project.getProjectStatus()) != ProjectStatus.ACTIVE) {
                throw new IllegalArgumentException("Only active projects can be reassigned.");
            }
            if (!containsBillCode(board, project.getBillCode())) {
                throw new IllegalArgumentException("Project is not currently assigned to a visible billable code.");
            }

            boolean billCodeChanged = !safe(project.getBillCode()).equals(targetBillCode);
            boolean budgetCleared = billCodeChanged && project.getBillBudgetId() != null;
            project.setBillCode(targetBillCode);
            if (budgetCleared) {
                project.setBillBudgetId(null);
            }
            project.setLastModifiedByWebUserId(Integer.valueOf(webUser.getWebUserId()));
            dataSession.saveOrUpdate(project);

            ProjectContactAssignedId assignedId = new ProjectContactAssignedId();
            assignedId.setProjectId(projectId);
            assignedId.setContactId(webUser.getContactId());
            ProjectContactAssigned assigned = (ProjectContactAssigned) dataSession.get(
                    ProjectContactAssigned.class, assignedId);
            if (assigned == null) {
                assigned = new ProjectContactAssigned();
                assigned.setId(assignedId);
                assigned.setEmailAlert("Y");
            }
            assigned.setUpdateDue(Integer.valueOf(updateEveryDays));
            dataSession.saveOrUpdate(assigned);
            transaction.commit();

            MoveResult result = new MoveResult();
            result.setBudgetCleared(budgetCleared);
            return result;
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }
    }

    BillingProjectAssignmentsBoard assembleBoard(List<BillCode> billCodes, List<Project> projects,
            Map<Integer, Integer> updateDueByProject) {
        BillingProjectAssignmentsBoard board = new BillingProjectAssignmentsBoard();
        Map<String, BillCodeColumn> columnByCode = new LinkedHashMap<String, BillCodeColumn>();
        for (BillCode billCode : billCodes) {
            BillCodeColumn column = new BillCodeColumn();
            column.setBillCode(safe(billCode.getBillCode()));
            column.setLabel(firstNonBlank(billCode.getDisplayLabel(), billCode.getBillLabel(), billCode.getBillCode()));
            board.getColumns().add(column);
            columnByCode.put(column.getBillCode(), column);
        }

        List<Integer> persistedCadences = new ArrayList<Integer>();
        for (Project project : projects) {
            Integer updateDue = updateDueByProject.get(Integer.valueOf(project.getProjectId()));
            persistedCadences.add(Integer.valueOf(updateDue == null ? 0 : updateDue.intValue()));
        }
        board.getRows().addAll(createCadenceRows(persistedCadences));

        Map<String, AssignmentCell> cellByKey = new HashMap<String, AssignmentCell>();
        for (CadenceRow row : board.getRows()) {
            for (BillCodeColumn column : board.getColumns()) {
                AssignmentCell cell = new AssignmentCell();
                cell.setBillCode(column.getBillCode());
                cell.setUpdateEveryDays(row.getUpdateEveryDays());
                row.getCells().add(cell);
                cellByKey.put(cellKey(column.getBillCode(), row.getUpdateEveryDays()), cell);
            }
        }

        for (Project project : projects) {
            if (!columnByCode.containsKey(safe(project.getBillCode()))) {
                continue;
            }
            Integer persistedUpdateDue = updateDueByProject.get(Integer.valueOf(project.getProjectId()));
            int updateDue = persistedUpdateDue == null ? 0 : persistedUpdateDue.intValue();
            AssignmentCell cell = cellByKey.get(cellKey(project.getBillCode(), updateDue));
            if (cell == null) {
                continue;
            }
            ProjectChip chip = new ProjectChip();
            chip.setProjectId(project.getProjectId());
            chip.setProjectName(safe(project.getProjectName()));
            cell.getProjects().add(chip);
        }

        Comparator<ProjectChip> byName = new Comparator<ProjectChip>() {
            public int compare(ProjectChip left, ProjectChip right) {
                int nameOrder = safe(left.getProjectName()).compareToIgnoreCase(safe(right.getProjectName()));
                return nameOrder != 0 ? nameOrder : left.getProjectId() - right.getProjectId();
            }
        };
        for (CadenceRow row : board.getRows()) {
            for (AssignmentCell cell : row.getCells()) {
                Collections.sort(cell.getProjects(), byName);
            }
        }
        return board;
    }

    static List<CadenceRow> createCadenceRows(Collection<Integer> persistedCadences) {
        List<CadenceRow> rows = new ArrayList<CadenceRow>();
        Set<Integer> standardDays = new HashSet<Integer>();
        for (ReviewInterval interval : ReviewInterval.values()) {
            standardDays.add(Integer.valueOf(interval.getDays()));
            rows.add(cadenceRow(interval.getDays(), interval.getDescription(), false));
        }

        Set<Integer> legacyDays = new TreeSet<Integer>();
        if (persistedCadences != null) {
            for (Integer days : persistedCadences) {
                if (days != null && days.intValue() > 0 && !standardDays.contains(days)) {
                    legacyDays.add(days);
                }
            }
        }
        for (Integer days : legacyDays) {
            rows.add(cadenceRow(days.intValue(), ReviewInterval.makeLabel(days.intValue()) + " (legacy)", true));
        }
        rows.add(cadenceRow(0, "None", false));
        return rows;
    }

    private static CadenceRow cadenceRow(int days, String label, boolean legacy) {
        CadenceRow row = new CadenceRow();
        row.setUpdateEveryDays(days);
        row.setLabel(label);
        row.setLegacy(legacy);
        return row;
    }

    @SuppressWarnings("unchecked")
    private List<BillCode> loadEligibleBillCodes(Session dataSession, int workspaceId) {
        Query query = dataSession.createQuery(
                "from BillCode where workspaceId = :workspaceId and visible = 'Y' and billable = 'Y' "
                        + "order by billLabel, id.billCode");
        query.setParameter("workspaceId", Integer.valueOf(workspaceId));
        return query.list();
    }

    @SuppressWarnings("unchecked")
    private List<Project> loadProjects(Session dataSession, int workspaceId, List<BillCode> billCodes) {
        if (billCodes.isEmpty()) {
            return new ArrayList<Project>();
        }
        List<String> codes = new ArrayList<String>();
        for (BillCode billCode : billCodes) {
            codes.add(billCode.getBillCode());
        }
        Query query = dataSession.createQuery(
                "from Project where workspaceId = :workspaceId "
                        + "and (projectStatus is null or projectStatus = :activeStatus) "
                        + "and billCode in (:billCodes) order by projectName, projectId");
        query.setParameter("workspaceId", Integer.valueOf(workspaceId));
        query.setParameter("activeStatus", ProjectStatus.ACTIVE.getDatabaseValue());
        query.setParameterList("billCodes", codes);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> loadUpdateDueByProject(Session dataSession, WebUser webUser,
            List<Project> projects) {
        Map<Integer, Integer> updateDueByProject = new HashMap<Integer, Integer>();
        if (projects.isEmpty()) {
            return updateDueByProject;
        }
        List<Integer> projectIds = new ArrayList<Integer>();
        for (Project project : projects) {
            projectIds.add(Integer.valueOf(project.getProjectId()));
        }
        Query query = dataSession.createQuery(
                "select pca.id.projectId, pca.updateDue from ProjectContactAssigned pca "
                        + "where pca.id.contactId = :contactId and pca.id.projectId in (:projectIds)");
        query.setParameter("contactId", Integer.valueOf(webUser.getContactId()));
        query.setParameterList("projectIds", projectIds);
        List<Object[]> rows = query.list();
        for (Object[] row : rows) {
            Number projectId = (Number) row[0];
            Number updateDue = (Number) row[1];
            updateDueByProject.put(Integer.valueOf(projectId.intValue()),
                    Integer.valueOf(updateDue == null ? 0 : updateDue.intValue()));
        }
        return updateDueByProject;
    }

    private Integer resolvePrivateWorkspaceId(Session dataSession, WebUser webUser) {
        return webUser == null ? null
                : WorkspaceRegistry.getWorkspaceIdForWebUserId(dataSession, webUser.getWebUserId());
    }

    private boolean containsBillCode(BillingProjectAssignmentsBoard board, String billCode) {
        for (BillCodeColumn column : board.getColumns()) {
            if (column.getBillCode().equals(safe(billCode))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsCadence(BillingProjectAssignmentsBoard board, int updateEveryDays) {
        for (CadenceRow row : board.getRows()) {
            if (row.getUpdateEveryDays() == updateEveryDays) {
                return true;
            }
        }
        return false;
    }

    private static String cellKey(String billCode, int updateEveryDays) {
        return safe(billCode) + "\n" + updateEveryDays;
    }

    private static String firstNonBlank(String first, String second, String third) {
        if (first != null && first.trim().length() > 0) {
            return first.trim();
        }
        if (second != null && second.trim().length() > 0) {
            return second.trim();
        }
        return safe(third);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static class MoveResult {
        private boolean budgetCleared;

        public boolean isBudgetCleared() {
            return budgetCleared;
        }

        public void setBudgetCleared(boolean budgetCleared) {
            this.budgetCleared = budgetCleared;
        }
    }
}