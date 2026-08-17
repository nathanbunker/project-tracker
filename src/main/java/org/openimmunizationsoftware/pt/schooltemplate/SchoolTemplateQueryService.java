package org.openimmunizationsoftware.pt.schooltemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionNextTemplateConfig;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

/** Loads the Template Scheduler ("School"/"Chores") data for a dependent, independent of any HTML rendering. */
public class SchoolTemplateQueryService {

    public List<Project> loadWorkspaceProjects(Session dataSession, Integer workspaceId) {
        Query query = dataSession.createQuery(
                "from Project where workspaceId = :workspaceId and (projectStatus is null or projectStatus <> :closedStatus) order by projectName");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("closedStatus", ProjectStatus.CLOSED.getDatabaseValue());
        @SuppressWarnings("unchecked")
        List<Project> projectList = query.list();
        return projectList;
    }

    public Map<Integer, Boolean> buildProjectBillableMap(Session dataSession, List<Project> projectList,
            Integer workspaceId) {
        Map<Integer, Boolean> billableMap = new HashMap<Integer, Boolean>();
        if (projectList == null || projectList.isEmpty()) {
            return billableMap;
        }

        Set<String> billCodeSet = new HashSet<String>();
        for (Project project : projectList) {
            if (project.getBillCode() != null && !project.getBillCode().trim().equals("")) {
                billCodeSet.add(project.getBillCode());
            }
        }

        Map<String, BillCode> billCodeMap = new HashMap<String, BillCode>();
        if (!billCodeSet.isEmpty()) {
            Query query = dataSession
                    .createQuery("from BillCode where workspaceId = :workspaceId and id.billCode in (:billCodes)");
            query.setParameter("workspaceId", workspaceId);
            query.setParameterList("billCodes", billCodeSet);
            @SuppressWarnings("unchecked")
            List<BillCode> billCodeList = query.list();
            for (BillCode billCode : billCodeList) {
                billCodeMap.put(billCode.getBillCode(), billCode);
            }
        }

        for (Project project : projectList) {
            BillCode billCode = billCodeMap.get(project.getBillCode());
            boolean billable = billCode != null && "Y".equalsIgnoreCase(billCode.getBillable());
            billableMap.put(Integer.valueOf(project.getProjectId()), Boolean.valueOf(billable));
        }
        return billableMap;
    }

    /** Loads every active template row (and its config, if any) for the dependent, grouped by project, sorted for display. */
    public List<SchoolTemplateSnapshot> loadTemplateSnapshots(Session dataSession, WebUser dependentUser,
            List<Project> projectList, Map<Integer, Boolean> projectBillableMap) {
        List<SchoolTemplateSnapshot> snapshots = new ArrayList<SchoolTemplateSnapshot>();

        List<Project> orderedProjects = new ArrayList<Project>(projectList);
        Collections.sort(orderedProjects, new Comparator<Project>() {
            @Override
            public int compare(Project left, Project right) {
                int leftPriority = left == null ? 0 : left.getPriorityLevel();
                int rightPriority = right == null ? 0 : right.getPriorityLevel();
                if (leftPriority != rightPriority) {
                    return rightPriority - leftPriority;
                }
                String leftName = left == null || left.getProjectName() == null ? "" : left.getProjectName();
                String rightName = right == null || right.getProjectName() == null ? "" : right.getProjectName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });

        for (Project project : orderedProjects) {
            boolean billable = Boolean.TRUE.equals(projectBillableMap.get(Integer.valueOf(project.getProjectId())));

            Query templateQuery = dataSession.createQuery(
                    "from ActionNext where projectId = :projectId "
                            + "and (contactId = :contactId or nextContactId = :nextContactId) "
                            + "and nextDescription <> '' "
                            + "and templateTypeString is NOT NULL and templateTypeString <> '' "
                            + "and nextActionStatusString = :nextActionStatus "
                            + "order by nextActionDate asc, nextDescription");
            templateQuery.setParameter("projectId", project.getProjectId());
            templateQuery.setParameter("contactId", dependentUser.getContactId());
            templateQuery.setParameter("nextContactId", dependentUser.getContactId());
            templateQuery.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
            @SuppressWarnings("unchecked")
            List<ActionNext> templateList = templateQuery.list();

            for (ActionNext templateAction : templateList) {
                ActionNextTemplateConfig config = (ActionNextTemplateConfig) dataSession
                        .get(ActionNextTemplateConfig.class, templateAction.getActionNextId());
                snapshots.add(new SchoolTemplateSnapshot(templateAction, config, project, billable));
            }
        }

        return snapshots;
    }

    public Map<Integer, SchoolTemplateSnapshot> indexById(List<SchoolTemplateSnapshot> snapshots) {
        Map<Integer, SchoolTemplateSnapshot> byId = new HashMap<Integer, SchoolTemplateSnapshot>();
        for (SchoolTemplateSnapshot snapshot : snapshots) {
            byId.put(Integer.valueOf(snapshot.getActionNext().getActionNextId()), snapshot);
        }
        return byId;
    }
}
