package org.dandeliondaily.projecthealth.service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.dandeliondaily.dashboard.service.ActionSentenceImportService;
import org.dandeliondaily.dashboard.service.ActionSentenceImportService.QuickCaptureActorKind;
import org.dandeliondaily.dashboard.service.ActionSentenceImportService.QuickCaptureActorResolution;
import org.dandeliondaily.projecthealth.model.ProjectHealthIssueModel;
import org.dandeliondaily.projecthealth.model.ProjectHealthPageModel;
import org.dandeliondaily.projecthealth.model.ProjectCadenceGroupModel;
import org.dandeliondaily.projecthealth.model.ProjectListItemModel;
import org.dandeliondaily.projecthealth.model.ProjectTagSummaryRowModel;
import org.dandeliondaily.projecthealth.model.ProjectReportModel;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.ActionSetDao;
import org.openimmunizationsoftware.pt.doa.ProjectFactDefinitionDao;
import org.openimmunizationsoftware.pt.doa.ProjectFactValueDao;
import org.openimmunizationsoftware.pt.doa.ProjectIssueDao;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionSet;
import org.openimmunizationsoftware.pt.model.ActionSetType;
import org.openimmunizationsoftware.pt.model.ProjectFactDefinition;
import org.openimmunizationsoftware.pt.model.ProjectFactValue;
import org.openimmunizationsoftware.pt.model.ProjectPatchLink;
import org.openimmunizationsoftware.pt.model.ProjectTag;
import org.openimmunizationsoftware.pt.model.Workspace;
import org.openimmunizationsoftware.pt.doa.ProjectPatchLinkDao;
import org.openimmunizationsoftware.pt.model.ActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.ProjectIssue;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.ReviewInterval;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class ProjectHealthPageService {

    public static final String PARAM_PROJECT_ID = "projectId";
    private static final String SESSION_PRIVATE_SELECTED_PROJECT_ID = "projectHealthPrivateSelectedProjectId";
    private static final String SESSION_PATCH_SELECTED_TAG_PREFIX = "projectHealthPatchSelectedTag.";
    private static final String SESSION_PATCH_SELECTED_PROJECT_PREFIX = "projectHealthPatchSelectedProject.";
    private static final String SESSION_SHARED_COCKPIT_PRIVATE_PROJECT_PREFIX = "projectHealthSharedCockpitPrivateProject.";
    private static final String BUCKET_NONE = "NONE";
    private static final String STATUS_ACTIVE = ProjectStatus.ACTIVE.getDatabaseValue();
    private static final String STATUS_PAUSED = ProjectStatus.PAUSED.getDatabaseValue();
    private static final String STATUS_COMPLETE = ProjectStatus.COMPLETE.getDatabaseValue();
    private static final String STATUS_CLOSED = ProjectStatus.CLOSED.getDatabaseValue();

    private final ActionSentenceImportService actionSentenceImportService = new ActionSentenceImportService();

    private enum ReprioritizeMode {
        BEFORE,
        FIRST,
        LAST
    }

    private static class ProjectStats {
        private int undatedOpen;
        private int overdueOpen;
        private Date lastReview;
        private int updateDue;
        private boolean reviewOverdue;
        private boolean reviewScheduledToday;
        private boolean missingDescription;
        private boolean missingOutcome;
        private boolean missingSuccessCriteria;
        private boolean missingReviewPeriod;
    }

    public static class ReplaceUnscheduledResult {
        private int cancelledCount;
        private int importedCount;

        public int getCancelledCount() {
            return cancelledCount;
        }

        public void setCancelledCount(int cancelledCount) {
            this.cancelledCount = cancelledCount;
        }

        public int getImportedCount() {
            return importedCount;
        }

        public void setImportedCount(int importedCount) {
            this.importedCount = importedCount;
        }
    }

    public static class ProjectHealthSnapshot {
        private Project project;
        private ProjectReportModel report;
        private List<ProjectHealthIssueModel> issues = new ArrayList<ProjectHealthIssueModel>();
        private int actionableIssueCount;

        public Project getProject() {
            return project;
        }

        public void setProject(Project project) {
            this.project = project;
        }

        public ProjectReportModel getReport() {
            return report;
        }

        public void setReport(ProjectReportModel report) {
            this.report = report;
        }

        public List<ProjectHealthIssueModel> getIssues() {
            return issues;
        }

        public void setIssues(List<ProjectHealthIssueModel> issues) {
            this.issues = issues;
        }

        public int getActionableIssueCount() {
            return actionableIssueCount;
        }

        public void setActionableIssueCount(int actionableIssueCount) {
            this.actionableIssueCount = actionableIssueCount;
        }
    }

    public ProjectHealthPageModel buildModel(AppReq appReq, Integer contextWorkspaceId,
            List<Workspace> accessiblePatchWorkspaces, String selectedPatchTagKey) {
        ProjectHealthPageModel model = new ProjectHealthPageModel();
        model.setOpenActionEditActionNextId(parseInteger(appReq.getRequest().getParameter("openActionEditId")));
        model.setContextWorkspaceId(contextWorkspaceId);
        model.setAccessiblePatchWorkspaces(accessiblePatchWorkspaces);
        model.setShowContextSelector(accessiblePatchWorkspaces != null && !accessiblePatchWorkspaces.isEmpty());

        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        List<Project> projects = loadProjects(webUser, dataSession, contextWorkspaceId);
        Map<Integer, String> displayNameByProjectId = buildPrivateDisplayNameMap(projects);
        Map<Integer, Integer> updateDueByProject = loadUpdateDueByProject(webUser, dataSession, projects);
        Map<Integer, ProjectStats> statsMap = buildStatsByProject(projects, webUser, dataSession, updateDueByProject);

        Project selectedProject;
        if (contextWorkspaceId == null) {
            selectedProject = buildPrivateLeftPanel(model, appReq, projects, displayNameByProjectId,
                    updateDueByProject, statsMap, dataSession, webUser);
        } else {
            String effectivePatchTagKey = selectedPatchTagKey;
            boolean patchTagParamPresent = appReq.getRequest() != null
                    && appReq.getRequest().getParameterMap() != null
                    && appReq.getRequest().getParameterMap().containsKey("patchTag");
            if (patchTagParamPresent && (effectivePatchTagKey == null || effectivePatchTagKey.trim().length() == 0)) {
                clearPatchSelectionState(appReq, contextWorkspaceId.intValue());
            } else if (effectivePatchTagKey == null || effectivePatchTagKey.trim().length() == 0) {
                effectivePatchTagKey = readPatchSelectedTag(appReq, contextWorkspaceId.intValue());
            }
            selectedProject = buildPatchLeftPanel(model, appReq, projects, displayNameByProjectId,
                    updateDueByProject, statsMap, dataSession, effectivePatchTagKey);
        }

        populateSelectedProjectData(model, appReq, selectedProject, statsMap, displayNameByProjectId,
                accessiblePatchWorkspaces, dataSession);

        return model;
    }

    public void populateFactDefinitions(ProjectHealthPageModel model, AppReq appReq, Integer selectedFactDefinitionId,
            String selectedFactGroup) {
        if (model == null || appReq == null || appReq.getActiveWorkspaceId() == null) {
            return;
        }

        ProjectFactDefinitionDao dao = new ProjectFactDefinitionDao(appReq.getDataSession());
        List<ProjectFactDefinition> factDefinitions = dao.listByWorkspaceId(appReq.getActiveWorkspaceId(), true);
        model.setFactDefinitions(factDefinitions);
        model.setSelectedFactDefinitionId(selectedFactDefinitionId);
        model.setSelectedFactGroup(selectedFactGroup);

        if (selectedFactDefinitionId != null) {
            ProjectFactDefinition selected = dao.getById(selectedFactDefinitionId.intValue());
            if (selected != null && selected.getWorkspaceId() == appReq.getActiveWorkspaceId()) {
                model.setSelectedFactDefinition(selected);
                if (model.getSelectedFactGroup() == null || model.getSelectedFactGroup().trim().length() == 0) {
                    model.setSelectedFactGroup(selected.getFactGroup());
                }
            }
        }
    }

    private Project buildPrivateLeftPanel(ProjectHealthPageModel model, AppReq appReq, List<Project> projects,
            Map<Integer, String> displayNameByProjectId, Map<Integer, Integer> updateDueByProject,
            Map<Integer, ProjectStats> statsMap, Session dataSession, WebUser webUser) {
        model.setLeftPanelMode(ProjectHealthPageModel.LEFT_PANEL_MODE_PRIVATE);
        int selectedProjectId = resolveSelectedProjectId(appReq, projects, webUser, dataSession);
        model.setSelectedProjectId(selectedProjectId);

        List<ProjectCadenceGroupModel> workProjectGroups = createCadenceGroups();
        List<ProjectCadenceGroupModel> personalProjectGroups = createCadenceGroups();
        List<ProjectListItemModel> pausedWorkProjects = new ArrayList<ProjectListItemModel>();
        List<ProjectListItemModel> completedWorkProjects = new ArrayList<ProjectListItemModel>();
        List<ProjectListItemModel> pausedPersonalProjects = new ArrayList<ProjectListItemModel>();
        List<ProjectListItemModel> completedPersonalProjects = new ArrayList<ProjectListItemModel>();
        Map<String, ProjectCadenceGroupModel> workGroupsByKey = toGroupMap(workProjectGroups);
        Map<String, ProjectCadenceGroupModel> personalGroupsByKey = toGroupMap(personalProjectGroups);

        Project selectedProject = null;
        for (Project project : projects) {
            ProjectStats stats = statsMap.get(project.getProjectId());
            ProjectListItemModel item = toListItem(project, stats, selectedProjectId, displayNameByProjectId);
            if (item.isSelected()) {
                selectedProject = project;
            }
            String normalizedProjectStatus = normalizeProjectStatus(project.getProjectStatus());
            boolean personalProject = isPersonalProject(project, dataSession);
            if (STATUS_PAUSED.equals(normalizedProjectStatus)) {
                if (personalProject) {
                    pausedPersonalProjects.add(item);
                } else {
                    pausedWorkProjects.add(item);
                }
                continue;
            }
            if (STATUS_COMPLETE.equals(normalizedProjectStatus)) {
                if (personalProject) {
                    completedPersonalProjects.add(item);
                } else {
                    completedWorkProjects.add(item);
                }
                continue;
            }
            String bucketKey = bucketKeyForUpdateDue(updateDueByProject.get(project.getProjectId()));
            if (personalProject) {
                personalGroupsByKey.get(bucketKey).getProjects().add(item);
            } else {
                workGroupsByKey.get(bucketKey).getProjects().add(item);
            }
        }

        model.setWorkProjectGroups(workProjectGroups);
        model.setPersonalProjectGroups(personalProjectGroups);
        model.setPausedWorkProjects(pausedWorkProjects);
        model.setCompletedWorkProjects(completedWorkProjects);
        model.setPausedPersonalProjects(pausedPersonalProjects);
        model.setCompletedPersonalProjects(completedPersonalProjects);
        return selectedProject;
    }

    private Project buildPatchLeftPanel(ProjectHealthPageModel model, AppReq appReq, List<Project> projects,
            Map<Integer, String> displayNameByProjectId, Map<Integer, Integer> updateDueByProject,
            Map<Integer, ProjectStats> statsMap, Session dataSession, String selectedPatchTagKey) {
        List<ProjectTagSummaryRowModel> summaryRows = buildPatchTagSummaryRows(dataSession,
                model.getContextWorkspaceId().intValue());
        model.setPatchTagSummaryRows(summaryRows);

        String resolvedTagKey = resolvePatchTagSelection(summaryRows, selectedPatchTagKey);
        if (resolvedTagKey == null) {
            clearPatchSelectionState(appReq, model.getContextWorkspaceId().intValue());
            model.setLeftPanelMode(ProjectHealthPageModel.LEFT_PANEL_MODE_PATCH_SUMMARY);
            model.setSelectedPatchTagKey(null);
            model.setSelectedPatchTagLabel(null);
            model.setSelectedProjectId(0);
            return null;
        }

        String selectedTagLabel = "";
        for (ProjectTagSummaryRowModel row : summaryRows) {
            if (resolvedTagKey.equals(row.getTagKey())) {
                selectedTagLabel = row.getTagLabel();
                break;
            }
        }

        model.setLeftPanelMode(ProjectHealthPageModel.LEFT_PANEL_MODE_PATCH_TAG);
        model.setSelectedPatchTagKey(resolvedTagKey);
        model.setSelectedPatchTagLabel(selectedTagLabel);
        writePatchSelectedTag(appReq, model.getContextWorkspaceId().intValue(), resolvedTagKey);

        List<Project> filteredProjects = filterProjectsForPatchTag(projects, dataSession,
                model.getContextWorkspaceId().intValue(), resolvedTagKey);
        int selectedProjectId = resolveSelectedPatchProjectId(appReq, filteredProjects,
                model.getContextWorkspaceId().intValue());
        model.setSelectedProjectId(selectedProjectId);

        List<ProjectCadenceGroupModel> groups = createCadenceGroups();
        Map<String, ProjectCadenceGroupModel> groupsByKey = toGroupMap(groups);

        Project selectedProject = null;
        for (Project project : filteredProjects) {
            ProjectStats stats = statsMap.get(project.getProjectId());
            ProjectListItemModel item = toListItem(project, stats, selectedProjectId, displayNameByProjectId);
            if (item.isSelected()) {
                selectedProject = project;
            }
            String bucketKey = bucketKeyForUpdateDue(updateDueByProject.get(project.getProjectId()));
            ProjectCadenceGroupModel group = groupsByKey.get(bucketKey);
            if (group != null) {
                group.getProjects().add(item);
            }
        }
        model.setPatchTagProjectGroups(groups);
        if (selectedProjectId > 0) {
            writePatchSelectedProjectId(appReq, model.getContextWorkspaceId().intValue(), selectedProjectId);
        } else {
            clearPatchSelectedProjectId(appReq, model.getContextWorkspaceId().intValue());
        }
        return selectedProject;
    }

    private void populateSelectedProjectData(ProjectHealthPageModel model, AppReq appReq, Project selectedProject,
            Map<Integer, ProjectStats> statsMap, Map<Integer, String> displayNameByProjectId,
            List<Workspace> accessiblePatchWorkspaces, Session dataSession) {
        if (selectedProject == null) {
            return;
        }

        appReq.setProject(selectedProject);
        ProjectStats selectedStats = statsMap.get(selectedProject.getProjectId());
        model.setSelectedProjectAvailable(true);
        model.setSelectedProjectName(resolveProjectDisplayName(selectedProject, displayNameByProjectId));
        model.setReport(buildReport(appReq, selectedProject, selectedStats, displayNameByProjectId));
        String selectedProjectStatus = normalizeProjectStatus(selectedProject.getProjectStatus());
        boolean healthCheckApplicable = STATUS_ACTIVE.equals(selectedProjectStatus);
        model.setHealthCheckApplicable(healthCheckApplicable);
        if (healthCheckApplicable) {
            model.setIssues(buildIssues(model.getReport(), selectedStats));
        } else {
            model.setIssues(new ArrayList<ProjectHealthIssueModel>());
        }

        boolean isPersonal = isPersonalProject(selectedProject, dataSession);
        model.setSelectedProjectIsPersonal(isPersonal);
        populateSharedProjectCockpitContext(model, appReq, selectedProject, dataSession);
        populateSharedOpenActions(model, appReq, selectedProject, dataSession);
        populateSharedProjectFacts(model, appReq, selectedProject, dataSession);
        boolean isInPrivateWorkspace = isProjectInPrivateWorkspace(selectedProject, dataSession);
        boolean patchLinksVisible = isInPrivateWorkspace
                && accessiblePatchWorkspaces != null && !accessiblePatchWorkspaces.isEmpty();
        model.setPatchLinksVisible(patchLinksVisible);

        if (!patchLinksVisible) {
            return;
        }

        Integer linkedPatchWorkspaceId = selectedProject.getLinkedPatchWorkspaceId();
        model.setSelectedProjectLinkedPatchWorkspaceId(linkedPatchWorkspaceId);
        if (linkedPatchWorkspaceId == null) {
            return;
        }

        Workspace linkedPatchWorkspace = (Workspace) dataSession.get(Workspace.class, linkedPatchWorkspaceId);
        model.setSelectedProjectLinkedPatchWorkspace(linkedPatchWorkspace);

        ProjectPatchLinkDao patchLinkDao = new ProjectPatchLinkDao(dataSession);
        model.setCanChangePatchWorkspace(!patchLinkDao.hasLinksForProject(selectedProject.getProjectId()));

        ProjectPatchLinkService patchLinkService = new ProjectPatchLinkService();
        model.setProjectPatchLinks(
                patchLinkService.buildLinkDisplayModels(dataSession, selectedProject.getProjectId(),
                        linkedPatchWorkspaceId));

        Query patchProjectQuery = dataSession.createQuery(
                "from Project where workspaceId = :wsId"
                        + " and (projectStatus is null or projectStatus <> :closedStatus)"
                        + " order by priorityLevel desc, projectName");
        patchProjectQuery.setParameter("wsId", linkedPatchWorkspaceId);
        patchProjectQuery.setParameter("closedStatus", STATUS_CLOSED);
        List<Project> patchProjects = new ArrayList<Project>();
        for (Object row : patchProjectQuery.list()) {
            if (row instanceof Project) {
                patchProjects.add((Project) row);
            }
        }
        model.setAvailablePatchProjects(patchProjects);

        Query patchTagQuery = dataSession.createQuery(
                "from ProjectTag where workspaceId = :wsId and tagStatus = :tagStatus"
                        + " order by sortOrder, tagName");
        patchTagQuery.setParameter("wsId", linkedPatchWorkspaceId);
        patchTagQuery.setParameter("tagStatus", ProjectTag.STATUS_ACTIVE);
        List<ProjectTag> patchTags = new ArrayList<ProjectTag>();
        for (Object row : patchTagQuery.list()) {
            if (row instanceof ProjectTag) {
                patchTags.add((ProjectTag) row);
            }
        }
        model.setAvailablePatchTags(patchTags);
    }

    private void populateSharedProjectCockpitContext(ProjectHealthPageModel model, AppReq appReq,
            Project selectedProject, Session dataSession) {
        if (model == null || appReq == null || appReq.getWebUser() == null || selectedProject == null
                || !model.isPatchContext() || model.isPatchSummaryMode()) {
            return;
        }
        if (selectedProject.getWorkspaceId() == null
                || !selectedProject.getWorkspaceId().equals(model.getContextWorkspaceId())) {
            return;
        }

        WebUser webUser = appReq.getWebUser();
        Integer privateWorkspaceId = WorkspaceRegistry.getWorkspaceIdForWebUserId(dataSession,
                webUser.getWebUserId());
        if (privateWorkspaceId == null) {
            return;
        }

        List<Project> candidatePrivateProjects = loadProjects(webUser, dataSession, privateWorkspaceId);
        model.setCandidatePrivateProjects(candidatePrivateProjects);
        Map<Integer, Project> candidateById = indexProjectsById(candidatePrivateProjects);

        List<Project> linkedPrivateProjects = loadLinkedPrivateProjectsForSharedProject(dataSession,
                selectedProject.getProjectId(), candidateById);
        model.setLinkedPrivateProjects(linkedPrivateProjects);

        Integer requestedPrivateProjectId = parseInteger(appReq.getRequest().getParameter("privateProjectId"));
        if (requestedPrivateProjectId == null) {
            requestedPrivateProjectId = readSharedCockpitSelectedPrivateProjectId(appReq,
                    selectedProject.getProjectId());
        }
        Project selectedPrivateProject = resolveSelectedPrivateProject(linkedPrivateProjects, candidateById,
                requestedPrivateProjectId);
        if (selectedPrivateProject != null) {
            model.setSelectedPrivateProject(selectedPrivateProject);
            model.setSelectedPrivateProjectId(Integer.valueOf(selectedPrivateProject.getProjectId()));
            writeSharedCockpitSelectedPrivateProjectId(appReq, selectedProject.getProjectId(),
                    selectedPrivateProject.getProjectId());
        } else if (requestedPrivateProjectId != null && candidateById.containsKey(requestedPrivateProjectId)) {
            model.setSelectedPrivateProjectId(requestedPrivateProjectId);
            writeSharedCockpitSelectedPrivateProjectId(appReq, selectedProject.getProjectId(),
                    requestedPrivateProjectId.intValue());
        } else {
            clearSharedCockpitSelectedPrivateProjectId(appReq, selectedProject.getProjectId());
        }
    }

    private void populateSharedOpenActions(ProjectHealthPageModel model, AppReq appReq,
            Project selectedProject, Session dataSession) {
        model.setOpenScheduledActions(new ArrayList<ProjectHealthPageModel.OpenActionItemModel>());
        model.setOpenUnscheduledActions(new ArrayList<ProjectHealthPageModel.OpenActionItemModel>());
        model.setOpenActionActorOptions(new ArrayList<ProjectHealthPageModel.OpenActionActorOptionModel>());
        model.setOpenActionsNeedsPrivateTargetSelection(false);

        if (model == null || appReq == null || appReq.getWebUser() == null || selectedProject == null
                || !model.isPatchContext() || model.isPatchSummaryMode()) {
            return;
        }
        if (selectedProject.getWorkspaceId() == null
                || !selectedProject.getWorkspaceId().equals(model.getContextWorkspaceId())) {
            return;
        }

        WebUser webUser = appReq.getWebUser();
        List<ActionNext> scheduledRows = listOpenActionsForSharedProject(dataSession, selectedProject.getProjectId(),
                true);
        List<ActionNext> unscheduledRows = listOpenActionsForSharedProject(dataSession, selectedProject.getProjectId(),
                false);

        Set<Integer> actionSetIds = new HashSet<Integer>();
        for (ActionNext row : scheduledRows) {
            if (row.getActionSet() != null) {
                actionSetIds.add(Integer.valueOf(row.getActionSet().getActionSetId()));
            }
        }
        for (ActionNext row : unscheduledRows) {
            if (row.getActionSet() != null) {
                actionSetIds.add(Integer.valueOf(row.getActionSet().getActionSetId()));
            }
        }

        Map<Integer, Integer> linkedPrivateByActionSetId = loadLinkedPrivateActionIdsForActionSets(dataSession,
                model.getSelectedPrivateProject(), actionSetIds);

        List<ProjectHealthPageModel.OpenActionItemModel> scheduledItems = new ArrayList<ProjectHealthPageModel.OpenActionItemModel>();
        List<ProjectHealthPageModel.OpenActionItemModel> unscheduledItems = new ArrayList<ProjectHealthPageModel.OpenActionItemModel>();
        for (ActionNext row : scheduledRows) {
            scheduledItems.add(toOpenActionItem(webUser, row, linkedPrivateByActionSetId));
        }
        for (ActionNext row : unscheduledRows) {
            unscheduledItems.add(toOpenActionItem(webUser, row, linkedPrivateByActionSetId));
        }
        model.setOpenScheduledActions(scheduledItems);
        model.setOpenUnscheduledActions(unscheduledItems);

        if (model.getOpenActionEditActionNextId() != null && model.getOpenActionEditActionNextId().intValue() > 0) {
            int editingActionId = model.getOpenActionEditActionNextId().intValue();
            boolean found = false;
            for (ProjectHealthPageModel.OpenActionItemModel item : scheduledItems) {
                if (item.getActionNextId() == editingActionId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (ProjectHealthPageModel.OpenActionItemModel item : unscheduledItems) {
                    if (item.getActionNextId() == editingActionId) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                model.setOpenActionEditActionNextId(null);
            }
        }

        model.setOpenActionActorOptions(loadOpenActionActorOptions(dataSession, selectedProject));
        model.setOpenActionsNeedsPrivateTargetSelection(model.getSelectedPrivateProject() == null);
    }

    private void populateSharedProjectFacts(ProjectHealthPageModel model, AppReq appReq,
            Project selectedProject, Session dataSession) {
        model.setSharedProjectFactGroups(new ArrayList<ProjectHealthPageModel.SharedProjectFactGroupModel>());

        if (model == null || appReq == null || selectedProject == null
                || !model.isPatchContext() || model.isPatchSummaryMode()) {
            return;
        }
        if (selectedProject.getWorkspaceId() == null
                || !selectedProject.getWorkspaceId().equals(model.getContextWorkspaceId())) {
            return;
        }

        ProjectFactDefinitionDao definitionDao = new ProjectFactDefinitionDao(dataSession);
        List<ProjectFactDefinition> definitions = definitionDao
                .listByWorkspaceId(selectedProject.getWorkspaceId().intValue(), false);
        if (definitions == null || definitions.isEmpty()) {
            return;
        }

        ProjectFactValueDao valueDao = new ProjectFactValueDao(dataSession);
        List<ProjectFactValue> values = valueDao.listByProjectId(selectedProject.getProjectId());
        Map<Integer, ProjectFactValue> valueByDefinitionId = new HashMap<Integer, ProjectFactValue>();
        for (ProjectFactValue value : values) {
            if (value == null) {
                continue;
            }
            valueByDefinitionId.put(Integer.valueOf(value.getProjectFactDefinitionId()), value);
        }

        List<ProjectHealthPageModel.SharedProjectFactGroupModel> groups = new ArrayList<ProjectHealthPageModel.SharedProjectFactGroupModel>();
        Map<String, ProjectHealthPageModel.SharedProjectFactGroupModel> groupByName = new LinkedHashMap<String, ProjectHealthPageModel.SharedProjectFactGroupModel>();

        for (ProjectFactDefinition definition : definitions) {
            if (definition == null) {
                continue;
            }
            if (!ProjectFactDefinition.INPUT_TYPE_BOOLEAN.equalsIgnoreCase(n(definition.getFactInputType()))) {
                continue;
            }

            String groupName = n(definition.getFactGroup()).trim();
            if (groupName.length() == 0) {
                groupName = "General";
            }

            ProjectHealthPageModel.SharedProjectFactGroupModel group = groupByName.get(groupName);
            if (group == null) {
                group = new ProjectHealthPageModel.SharedProjectFactGroupModel();
                group.setFactGroup(groupName);
                groupByName.put(groupName, group);
                groups.add(group);
            }

            ProjectFactValue value = valueByDefinitionId.get(Integer.valueOf(definition.getProjectFactDefinitionId()));
            boolean checked = value != null
                    && ProjectFactValue.BOOLEAN_YES.equalsIgnoreCase(n(value.getValueBoolean()));

            ProjectHealthPageModel.SharedProjectFactItemModel item = new ProjectHealthPageModel.SharedProjectFactItemModel();
            item.setProjectFactDefinitionId(definition.getProjectFactDefinitionId());
            item.setFactLabel(n(definition.getFactLabel()));
            item.setChecked(checked);
            group.getItems().add(item);
            group.setTotalCount(group.getTotalCount() + 1);
            if (checked) {
                group.setCheckedCount(group.getCheckedCount() + 1);
            }
        }

        model.setSharedProjectFactGroups(groups);
    }

    @SuppressWarnings("unchecked")
    private List<ActionNext> listOpenActionsForSharedProject(Session dataSession, int sharedProjectId,
            boolean scheduled) {
        StringBuilder hql = new StringBuilder();
        hql.append("select distinct an from ActionNext an ")
                .append("left join fetch an.contact ")
                .append("left join fetch an.nextProjectContact ")
                .append("where an.projectId = :projectId ")
                .append("and an.nextActionStatusString = :status ")
                .append("and an.nextDescription <> '' ");
        if (scheduled) {
            hql.append("and an.nextActionDate is not null ")
                    .append("order by an.nextActionDate, an.priorityLevel desc, an.completionOrder, an.nextChangeDate");
        } else {
            hql.append("and an.nextActionDate is null ")
                    .append("order by an.priorityLevel desc, an.completionOrder, an.nextChangeDate");
        }
        Query query = dataSession.createQuery(hql.toString());
        query.setParameter("projectId", sharedProjectId);
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        return query.list();
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> loadLinkedPrivateActionIdsForActionSets(Session dataSession,
            Project selectedPrivateProject, Set<Integer> actionSetIds) {
        Map<Integer, Integer> linkedByActionSet = new HashMap<Integer, Integer>();
        if (selectedPrivateProject == null || actionSetIds == null || actionSetIds.isEmpty()) {
            return linkedByActionSet;
        }
        Query query = dataSession.createQuery(
                "from ActionNext an where an.projectId = :projectId and an.actionSet.actionSetId in (:actionSetIds) "
                        + "order by an.nextChangeDate desc, an.actionNextId desc");
        query.setParameter("projectId", selectedPrivateProject.getProjectId());
        query.setParameterList("actionSetIds", actionSetIds);
        List<ActionNext> linkedRows = query.list();
        for (ActionNext linked : linkedRows) {
            if (linked == null || linked.getActionSet() == null) {
                continue;
            }
            Integer actionSetId = Integer.valueOf(linked.getActionSet().getActionSetId());
            if (!linkedByActionSet.containsKey(actionSetId)) {
                linkedByActionSet.put(actionSetId, Integer.valueOf(linked.getActionNextId()));
            }
        }
        return linkedByActionSet;
    }

    @SuppressWarnings("unchecked")
    private List<ProjectHealthPageModel.OpenActionActorOptionModel> loadOpenActionActorOptions(Session dataSession,
            Project selectedSharedProject) {
        List<ProjectHealthPageModel.OpenActionActorOptionModel> options = new ArrayList<ProjectHealthPageModel.OpenActionActorOptionModel>();
        ProjectHealthPageModel.OpenActionActorOptionModel someone = new ProjectHealthPageModel.OpenActionActorOptionModel();
        someone.setContactId(null);
        someone.setLabel("Someone");
        options.add(someone);

        if (selectedSharedProject == null || selectedSharedProject.getWorkspaceId() == null) {
            return options;
        }

        Query query = dataSession.createQuery(
                "from ProjectContact pc where pc.workspaceId = :workspaceId "
                        + "and pc.contactStatus = :contactStatus "
                        + "and exists (select wu.webUserId from WebUser wu "
                        + "where wu.contactId = pc.contactId and wu.registrationStatus = :registrationStatus) "
                        + "order by pc.nameLast, pc.nameFirst");
        query.setParameter("workspaceId", selectedSharedProject.getWorkspaceId());
        query.setParameter("contactStatus", ProjectContact.STATUS_ACTIVE);
        query.setParameter("registrationStatus", WebUser.REGISTRATION_STATUS_ACTIVE);
        List<ProjectContact> contacts = query.list();
        for (ProjectContact contact : contacts) {
            if (contact == null) {
                continue;
            }
            ProjectHealthPageModel.OpenActionActorOptionModel option = new ProjectHealthPageModel.OpenActionActorOptionModel();
            option.setContactId(Integer.valueOf(contact.getContactId()));
            option.setLabel(n(contact.getName(), "(unnamed contact)"));
            options.add(option);
        }
        return options;
    }

    private ProjectHealthPageModel.OpenActionItemModel toOpenActionItem(WebUser webUser, ActionNext row,
            Map<Integer, Integer> linkedPrivateByActionSetId) {
        ProjectHealthPageModel.OpenActionItemModel item = new ProjectHealthPageModel.OpenActionItemModel();
        item.setActionNextId(row.getActionNextId());
        item.setActorContactId(row.getContactId());
        item.setActorDisplay(row.getContact() == null ? "Someone" : n(row.getContact().getName(), "Someone"));
        item.setSentenceHtml(n(row.getNextDescriptionForDisplay(webUser.getProjectContact())));
        item.setNextActionType(n(row.getNextActionType()));
        item.setNextDescription(n(row.getNextDescription()));
        item.setNextActionDateLabel(formatDate(webUser, row.getNextActionDate()));
        if (row.getNextActionDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(webUser.getDateEntryPattern());
            item.setNextActionDateInput(sdf.format(row.getNextActionDate()));
        } else {
            item.setNextActionDateInput("");
        }
        item.setNextTimeEstimate(row.getNextTimeEstimate());
        item.setNextTimeEstimateLabel(
                row.getNextTimeEstimate() == null ? "" : ActionNext.getTimeForDisplay(row.getNextTimeEstimate()));
        item.setTargetDateLabel(formatDate(webUser, row.getNextTargetDate()));
        item.setDeadlineDateLabel(formatDate(webUser, row.getNextDeadlineDate()));
        item.setPriorityLevel(row.getPriorityLevel());
        item.setCompletionOrder(row.getCompletionOrder());
        if (row.getActionSet() != null && linkedPrivateByActionSetId != null) {
            Integer linkedActionId = linkedPrivateByActionSetId
                    .get(Integer.valueOf(row.getActionSet().getActionSetId()));
            item.setLinkedPrivateActionNextId(linkedActionId);
            item.setLinkedToSelectedPrivate(linkedActionId != null);
        }
        return item;
    }

    public String updateSharedOpenAction(AppReq appReq, int sharedProjectId, int actionNextId,
            String actorContactIdValue, String nextActionType, String nextDescription,
            String nextActionDateValue, String nextTimeEstimateValue,
            String priorityLevelValue, String completionOrderValue) {
        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();
        Project sharedProject = resolveSharedProjectForActionMutation(appReq, sharedProjectId);
        ActionNext selectedAction = resolveSharedActionForActionMutation(appReq, sharedProject, actionNextId);

        Integer actorContactId = parseInteger(actorContactIdValue);
        ProjectContact actorContact = null;
        if (actorContactId != null) {
            actorContact = (ProjectContact) dataSession.get(ProjectContact.class, actorContactId.intValue());
            if (actorContact == null || actorContact.getWorkspaceId() == null
                    || !actorContact.getWorkspaceId().equals(sharedProject.getWorkspaceId())) {
                throw new IllegalArgumentException("Selected actor is not available in this shared workspace.");
            }
        }

        Date nextActionDate = null;
        String trimmedDate = n(nextActionDateValue).trim();
        if (trimmedDate.length() > 0) {
            nextActionDate = webUser.parseDate(trimmedDate);
            if (nextActionDate == null) {
                throw new IllegalArgumentException("Action date must match your date format.");
            }
        }

        Integer nextTimeEstimate = null;
        String trimmedEstimate = n(nextTimeEstimateValue).trim();
        if (trimmedEstimate.length() > 0) {
            nextTimeEstimate = parseInteger(trimmedEstimate);
            if (nextTimeEstimate == null || nextTimeEstimate.intValue() < 0) {
                throw new IllegalArgumentException("Time estimate must be 0 or greater.");
            }
        }

        Integer priorityLevel = null;
        String trimmedPriority = n(priorityLevelValue).trim();
        if (trimmedPriority.length() > 0) {
            priorityLevel = parseInteger(trimmedPriority);
        }

        Integer completionOrder = null;
        String trimmedOrder = n(completionOrderValue).trim();
        if (trimmedOrder.length() > 0) {
            completionOrder = parseInteger(trimmedOrder);
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            Date now = new Date();
            List<ActionNext> siblings = resolveSharedActionSiblings(dataSession, selectedAction);
            for (ActionNext sibling : siblings) {
                if (actorContact == null) {
                    sibling.setContact(null);
                    sibling.setContactId(null);
                } else {
                    sibling.setContact(actorContact);
                }
                if (nextActionType != null && nextActionType.trim().length() > 0) {
                    sibling.setNextActionType(nextActionType.trim());
                }
                sibling.setNextDescription(n(nextDescription).trim());
                sibling.setNextActionDate(nextActionDate);
                sibling.setNextTimeEstimate(nextTimeEstimate);
                if (priorityLevel != null) {
                    sibling.setPriorityLevel(priorityLevel.intValue());
                }
                if (completionOrder != null) {
                    sibling.setCompletionOrder(completionOrder.intValue());
                }
                sibling.setNextChangeDate(now);
                dataSession.saveOrUpdate(sibling);
            }
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw new IllegalArgumentException("Unable to save shared action: " + e.getMessage());
        }

        return "Shared action updated.";
    }

    public String adoptSharedOpenAction(AppReq appReq, int sharedProjectId, int actionNextId,
            Integer requestedPrivateProjectId) {
        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();
        Project sharedProject = resolveSharedProjectForActionMutation(appReq, sharedProjectId);
        ActionNext sharedAction = resolveSharedActionForActionMutation(appReq, sharedProject, actionNextId);
        Project privateProject = resolveLinkedPrivateProject(webUser, dataSession, sharedProject,
                requestedPrivateProjectId);
        if (privateProject == null) {
            throw new IllegalArgumentException("Link/select a private project before adopting actions.");
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            ActionSet actionSet = sharedAction.getActionSet();
            if (actionSet == null || actionSet.getActionSetType() != ActionSetType.SHARED) {
                actionSet = new ActionSetDao(dataSession).createActionSet(webUser, ActionSetType.SHARED);
                sharedAction.setActionSet(actionSet);
                sharedAction.setNextChangeDate(new Date());
                dataSession.update(sharedAction);
            }

            if (sharedAction.getContactId() == null) {
                sharedAction.setContact(webUser.getProjectContact());
                sharedAction.setNextChangeDate(new Date());
                dataSession.update(sharedAction);
            }

            Query existingQuery = dataSession.createQuery(
                    "from ActionNext an where an.projectId = :projectId and an.actionSet.actionSetId = :actionSetId order by an.actionNextId");
            existingQuery.setParameter("projectId", privateProject.getProjectId());
            existingQuery.setParameter("actionSetId", actionSet.getActionSetId());
            @SuppressWarnings("unchecked")
            List<ActionNext> existingLinks = existingQuery.list();
            if (existingLinks != null && !existingLinks.isEmpty()) {
                Date now = new Date();
                boolean privateBillable = isWorkProject(privateProject, dataSession);
                for (ActionNext existing : existingLinks) {
                    existing.setWorkspaceId(privateProject.getWorkspaceId());
                    existing.setProject(privateProject);
                    existing.setProjectId(privateProject.getProjectId());
                    existing.setContact(webUser.getProjectContact());
                    existing.setContactId(Integer.valueOf(webUser.getContactId()));
                    existing.setNextActionType(sharedAction.getNextActionType());
                    existing.setNextDescription(sharedAction.getNextDescription());
                    existing.setNextActionDate(sharedAction.getNextActionDate());
                    existing.setNextTimeEstimate(sharedAction.getNextTimeEstimate());
                    existing.setNextContactId(sharedAction.getNextContactId());
                    existing.setPriorityLevel(sharedAction.getPriorityLevel());
                    existing.setCompletionOrder(sharedAction.getCompletionOrder());
                    existing.setNextActionStatus(ProjectNextActionStatus.READY);
                    existing.setNextChangeDate(now);
                    existing.setNextDeadlineDate(sharedAction.getNextDeadlineDate());
                    existing.setNextTargetDate(sharedAction.getNextTargetDate());
                    existing.setLinkUrl(sharedAction.getLinkUrl());
                    existing.setTimeSlot(sharedAction.getTimeSlot());
                    existing.setBillable(privateBillable);
                    existing.setActionSet(actionSet);
                    dataSession.saveOrUpdate(existing);
                }
                transaction.commit();
                return "Shared action linked and refreshed in private project.";
            }

            ActionNext privateAction = new ActionNext();
            privateAction.setWorkspaceId(privateProject.getWorkspaceId());
            privateAction.setProject(privateProject);
            privateAction.setProjectId(privateProject.getProjectId());
            privateAction.setContact(webUser.getProjectContact());
            privateAction.setContactId(Integer.valueOf(webUser.getContactId()));
            privateAction.setNextActionType(sharedAction.getNextActionType());
            privateAction.setNextDescription(sharedAction.getNextDescription());
            privateAction.setNextActionDate(sharedAction.getNextActionDate());
            privateAction.setNextTimeEstimate(sharedAction.getNextTimeEstimate());
            privateAction.setNextContactId(sharedAction.getNextContactId());
            privateAction.setPriorityLevel(sharedAction.getPriorityLevel());
            privateAction.setCompletionOrder(sharedAction.getCompletionOrder());
            privateAction.setNextActionStatus(ProjectNextActionStatus.READY);
            privateAction.setNextChangeDate(new Date());
            privateAction.setNextDeadlineDate(sharedAction.getNextDeadlineDate());
            privateAction.setNextTargetDate(sharedAction.getNextTargetDate());
            privateAction.setLinkUrl(sharedAction.getLinkUrl());
            privateAction.setTimeSlot(sharedAction.getTimeSlot());
            privateAction.setBillable(isWorkProject(privateProject, dataSession));
            privateAction.setActionSet(actionSet);
            dataSession.save(privateAction);

            transaction.commit();
            return "Shared action adopted into private project.";
        } catch (Exception e) {
            transaction.rollback();
            throw new IllegalArgumentException("Unable to adopt shared action: " + e.getMessage());
        }
    }

    public String cancelSharedOpenAction(AppReq appReq, int sharedProjectId, int actionNextId) {
        Session dataSession = appReq.getDataSession();
        Project sharedProject = resolveSharedProjectForActionMutation(appReq, sharedProjectId);
        ActionNext selectedAction = resolveSharedActionForActionMutation(appReq, sharedProject, actionNextId);

        Transaction transaction = dataSession.beginTransaction();
        try {
            Date now = new Date();
            List<ActionNext> siblings = resolveSharedActionSiblings(dataSession, selectedAction);
            for (ActionNext sibling : siblings) {
                sibling.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
                sibling.setNextChangeDate(now);
                dataSession.saveOrUpdate(sibling);
            }
            transaction.commit();
            return "Shared action cancelled.";
        } catch (Exception e) {
            transaction.rollback();
            throw new IllegalArgumentException("Unable to cancel shared action: " + e.getMessage());
        }
    }

    private Project resolveSharedProjectForActionMutation(AppReq appReq, int sharedProjectId) {
        Session dataSession = appReq.getDataSession();
        if (!isPatchContextWorkspace(appReq, dataSession)) {
            throw new IllegalArgumentException("Open Actions are only available for shared patch projects.");
        }
        Project sharedProject = (Project) dataSession.get(Project.class, sharedProjectId);
        if (sharedProject == null || sharedProject.getWorkspaceId() == null
                || appReq.getActiveWorkspaceId() == null
                || !sharedProject.getWorkspaceId().equals(appReq.getActiveWorkspaceId())) {
            throw new IllegalArgumentException("Shared project was not found.");
        }
        return sharedProject;
    }

    private ActionNext resolveSharedActionForActionMutation(AppReq appReq, Project sharedProject, int actionNextId) {
        Session dataSession = appReq.getDataSession();
        ActionNext action = (ActionNext) dataSession.get(ActionNext.class, actionNextId);
        if (action == null || action.getProjectId() != sharedProject.getProjectId()) {
            throw new IllegalArgumentException("Action was not found on the selected shared project.");
        }
        if (action.getWorkspaceId() == null || !action.getWorkspaceId().equals(sharedProject.getWorkspaceId())) {
            throw new IllegalArgumentException("Action is not available in this shared workspace.");
        }
        return action;
    }

    @SuppressWarnings("unchecked")
    private List<ActionNext> resolveSharedActionSiblings(Session dataSession, ActionNext selectedAction) {
        List<ActionNext> singleAction = new ArrayList<ActionNext>();
        if (selectedAction == null) {
            return singleAction;
        }
        singleAction.add(selectedAction);
        if (selectedAction.getActionSet() == null
                || selectedAction.getActionSet().getActionSetType() != ActionSetType.SHARED) {
            return singleAction;
        }
        int actionSetId = selectedAction.getActionSet().getActionSetId();
        Query siblingQuery = dataSession.createQuery(
                "from ActionNext an where an.actionSet.actionSetId = :actionSetId order by an.actionNextId");
        siblingQuery.setParameter("actionSetId", actionSetId);
        List<ActionNext> siblings = siblingQuery.list();
        if (siblings == null || siblings.isEmpty()) {
            return singleAction;
        }
        return siblings;
    }

    public ProjectHealthSnapshot buildProjectHealthSnapshot(AppReq appReq, Project project) {
        ProjectHealthSnapshot snapshot = new ProjectHealthSnapshot();
        if (project == null || appReq == null || appReq.getWebUser() == null || appReq.getDataSession() == null) {
            return snapshot;
        }

        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        List<Project> projects = new ArrayList<Project>();
        projects.add(project);

        Map<Integer, Integer> updateDueByProject = loadUpdateDueByProject(webUser, dataSession, projects);
        Map<Integer, ProjectStats> statsMap = buildStatsByProject(projects, webUser, dataSession, updateDueByProject);
        ProjectStats stats = statsMap.get(project.getProjectId());

        snapshot.setProject(project);
        if (stats == null) {
            return snapshot;
        }

        ProjectReportModel report = buildReport(appReq, project, stats, null);
        snapshot.setReport(report);

        String selectedProjectStatus = normalizeProjectStatus(project.getProjectStatus());
        boolean healthCheckApplicable = STATUS_ACTIVE.equals(selectedProjectStatus);
        if (!healthCheckApplicable) {
            snapshot.setIssues(new ArrayList<ProjectHealthIssueModel>());
            snapshot.setActionableIssueCount(0);
            return snapshot;
        }

        List<ProjectHealthIssueModel> issues = buildIssues(report, stats);
        snapshot.setIssues(issues);
        int actionableIssueCount = 0;
        for (ProjectHealthIssueModel issue : issues) {
            if (issue.getSeverity() == ProjectHealthIssueModel.Severity.CRITICAL
                    || issue.getSeverity() == ProjectHealthIssueModel.Severity.WARNING) {
                actionableIssueCount++;
            }
        }
        snapshot.setActionableIssueCount(actionableIssueCount);
        return snapshot;
    }

    public List<ProjectListItemModel> loadReprioritizeCandidates(AppReq appReq, int projectId) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        boolean patchContext = isPatchContextWorkspace(appReq, dataSession);

        List<Project> all = loadProjectsForCurrentWorkspace(appReq, webUser, dataSession);
        Map<Integer, String> displayNameByProjectId = buildPrivateDisplayNameMap(all);
        Map<Integer, Integer> updateDueByProject = loadUpdateDueByProject(webUser, dataSession, all);
        Project selected = null;
        for (Project project : all) {
            if (project.getProjectId() == projectId) {
                selected = project;
                break;
            }
        }
        if (selected == null) {
            return new ArrayList<ProjectListItemModel>();
        }

        String selectedBucket = bucketKeyForUpdateDue(updateDueByProject.get(selected.getProjectId()));
        List<ProjectListItemModel> candidates = new ArrayList<ProjectListItemModel>();
        for (Project project : all) {
            if (project.getProjectId() == projectId) {
                continue;
            }
            if (patchContext) {
                String status = normalizeProjectStatus(project.getProjectStatus());
                if (!STATUS_ACTIVE.equals(status)) {
                    continue;
                }
            } else {
                boolean personal = isPersonalProject(selected, dataSession);
                if (isPersonalProject(project, dataSession) != personal) {
                    continue;
                }
            }
            String projectBucket = bucketKeyForUpdateDue(updateDueByProject.get(project.getProjectId()));
            if (!selectedBucket.equals(projectBucket)) {
                continue;
            }
            ProjectListItemModel item = new ProjectListItemModel();
            item.setProjectId(project.getProjectId());
            item.setProjectName(resolveProjectDisplayName(project, displayNameByProjectId));
            item.setPriorityLevel(project.getPriorityLevel());
            candidates.add(item);
        }
        return candidates;
    }

    public String reprioritizeProject(AppReq appReq, int projectId, Integer beforeProjectId, String modeValue) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        boolean patchContext = isPatchContextWorkspace(appReq, dataSession);
        ReprioritizeMode mode = parseReprioritizeMode(modeValue);

        List<Project> all = loadProjectsForCurrentWorkspace(appReq, webUser, dataSession);
        Map<Integer, Integer> updateDueByProject = loadUpdateDueByProject(webUser, dataSession, all);
        Project selected = null;
        Project before = null;
        for (Project project : all) {
            if (project.getProjectId() == projectId) {
                selected = project;
            }
            if (beforeProjectId != null && project.getProjectId() == beforeProjectId.intValue()) {
                before = project;
            }
        }
        if (selected == null) {
            return "Project was not found";
        }
        if (mode == ReprioritizeMode.BEFORE && before == null) {
            return "Target project was not found";
        }

        String selectedBucket = bucketKeyForUpdateDue(updateDueByProject.get(selected.getProjectId()));
        if (before != null && patchContext) {
            String beforeStatus = normalizeProjectStatus(before.getProjectStatus());
            if (!STATUS_ACTIVE.equals(beforeStatus)) {
                return "Projects must be active";
            }
        }
        if (before != null && !patchContext) {
            boolean personal = isPersonalProject(selected, dataSession);
            if (isPersonalProject(before, dataSession) != personal) {
                return "Projects must be in the same section";
            }
        }
        if (before != null) {
            String beforeBucket = bucketKeyForUpdateDue(updateDueByProject.get(before.getProjectId()));
            if (!selectedBucket.equals(beforeBucket)) {
                return "Projects must be in the same review period";
            }
        }

        List<Project> bucket = new ArrayList<Project>();
        for (Project project : all) {
            if (!selectedBucket.equals(bucketKeyForUpdateDue(updateDueByProject.get(project.getProjectId())))) {
                continue;
            }
            if (patchContext) {
                String status = normalizeProjectStatus(project.getProjectStatus());
                if (!STATUS_ACTIVE.equals(status)) {
                    continue;
                }
            } else {
                boolean personal = isPersonalProject(selected, dataSession);
                if (isPersonalProject(project, dataSession) != personal) {
                    continue;
                }
            }
            bucket.add(project);
        }

        bucket.remove(selected);
        if (mode == ReprioritizeMode.FIRST) {
            bucket.add(0, selected);
        } else if (mode == ReprioritizeMode.LAST) {
            bucket.add(selected);
        } else {
            int target = bucket.indexOf(before);
            if (target < 0) {
                return "Could not determine target position";
            }
            bucket.add(target, selected);
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            int seedPriority = bucket.size() * 100;
            for (Project project : bucket) {
                project.setPriorityLevel(seedPriority);
                dataSession.update(project);
                seedPriority -= 1;
            }

            normalizeOpenProjectPriorities(webUser, dataSession);
            transaction.commit();
            return null;
        } catch (Exception e) {
            transaction.rollback();
            return "Unable to reprioritize project: " + e.getMessage();
        }
    }

    public void normalizeOpenProjectPriorities(WebUser webUser, Session dataSession) {
        List<Project> allOpenProjects = loadProjects(webUser, dataSession);
        Map<Integer, Integer> updateDueByProject = loadUpdateDueByProject(webUser, dataSession, allOpenProjects);

        final Map<Integer, Boolean> personalByProjectId = new HashMap<Integer, Boolean>();
        for (Project project : allOpenProjects) {
            personalByProjectId.put(project.getProjectId(), Boolean.valueOf(isPersonalProject(project, dataSession)));
        }

        Collections.sort(allOpenProjects, new Comparator<Project>() {
            @Override
            public int compare(Project a, Project b) {
                int aSectionRank = personalByProjectId.get(a.getProjectId()).booleanValue() ? 1 : 0;
                int bSectionRank = personalByProjectId.get(b.getProjectId()).booleanValue() ? 1 : 0;
                if (aSectionRank != bSectionRank) {
                    return aSectionRank - bSectionRank;
                }

                int aBucket = bucketRankForUpdateDue(updateDueByProject.get(a.getProjectId()));
                int bBucket = bucketRankForUpdateDue(updateDueByProject.get(b.getProjectId()));
                if (aBucket != bBucket) {
                    return aBucket - bBucket;
                }

                if (a.getPriorityLevel() != b.getPriorityLevel()) {
                    return b.getPriorityLevel() - a.getPriorityLevel();
                }

                String aName = n(a.getProjectName(), "").toLowerCase();
                String bName = n(b.getProjectName(), "").toLowerCase();
                int byName = aName.compareTo(bName);
                if (byName != 0) {
                    return byName;
                }

                return a.getProjectId() - b.getProjectId();
            }
        });

        int priority = allOpenProjects.size() * 10;
        for (Project project : allOpenProjects) {
            if (project.getPriorityLevel() != priority) {
                project.setPriorityLevel(priority);
                dataSession.update(project);
            }
            priority -= 10;
        }
    }

    public String scheduleProjectReview(AppReq appReq, int projectId, Date reviewDate) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        Integer activeWorkspaceId = appReq.getActiveWorkspaceId();

        Project project = (Project) dataSession.get(Project.class, projectId);
        if (project == null || project.getWorkspaceId() == null || webUser == null || activeWorkspaceId == null) {
            return "Project is not available";
        }
        if (!project.getWorkspaceId().equals(activeWorkspaceId)) {
            return "Project is not available";
        }

        if (reviewDate == null) {
            return "Review date is required";
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            ActionNext reviewAction = new ActionNext();
            reviewAction.setProject(project);
            reviewAction.setProjectId(project.getProjectId());
            reviewAction.setContact(webUser.getProjectContact());
            reviewAction.setContactId(webUser.getContactId());
            reviewAction.setWorkspaceId(activeWorkspaceId);
            reviewAction.setNextActionType(ProjectNextActionType.WILL);
            reviewAction.setNextActionDate(reviewDate);
            reviewAction.setNextDescription("review and update project");
            reviewAction.setNextSummary("");
            reviewAction.setNextTimeEstimate(30);
            reviewAction.setNextActionStatus(ProjectNextActionStatus.READY);
            reviewAction.setNextChangeDate(new Date());
            reviewAction.setBillable(isWorkProject(project, dataSession));

            dataSession.save(reviewAction);
            transaction.commit();
            return null;
        } catch (Exception e) {
            transaction.rollback();
            return "Unable to schedule project review: " + e.getMessage();
        }
    }

    public String saveQuickCaptureForSelectedProject(AppReq appReq, int projectId, Integer privateProjectId,
            String sentenceInput) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();

        if (webUser == null || dataSession == null) {
            throw new IllegalArgumentException("Quick capture is not available.");
        }
        if (!isPatchContextWorkspace(appReq, dataSession)) {
            throw new IllegalArgumentException("Quick capture is only available for shared patch projects.");
        }
        if (sentenceInput == null || sentenceInput.trim().length() == 0) {
            throw new IllegalArgumentException("Quick capture requires text.");
        }

        List<Project> projects = loadProjectsForCurrentWorkspace(appReq, webUser, dataSession);
        Project selectedProject = null;
        for (Project project : projects) {
            if (project != null && project.getProjectId() == projectId) {
                selectedProject = project;
                break;
            }
        }
        if (selectedProject == null || selectedProject.getWorkspaceId() == null) {
            throw new IllegalArgumentException("Project is not available.");
        }

        QuickCaptureActorResolution actorResolution = actionSentenceImportService
                .resolveProjectScopedActorForQuickCapture(webUser, dataSession, selectedProject, sentenceInput);
        if (actorResolution != null && actorResolution.isUnknownNamedContact()) {
            throw new IllegalArgumentException("Unknown workspace contact: "
                    + actorResolution.getUnknownNamedContact());
        }
        QuickCaptureActorKind actorKind = actorResolution == null ? QuickCaptureActorKind.IMPLICIT_CURRENT_USER
                : actorResolution.getActorKind();
        String normalizedSentenceInput = actorResolution == null ? sentenceInput
                : actorResolution.getNormalizedActionPart();
        boolean currentUserActor = QuickCaptureActorKind.CURRENT_USER.equals(actorKind)
                || QuickCaptureActorKind.IMPLICIT_CURRENT_USER.equals(actorKind)
                || isCurrentUserContact(webUser, actorResolution == null ? null : actorResolution.getContact());

        Project linkedPrivateProject = resolveLinkedPrivateProject(webUser, dataSession, selectedProject,
                privateProjectId);

        Transaction transaction = dataSession.beginTransaction();
        try {
            if (currentUserActor) {
                if (linkedPrivateProject == null) {
                    transaction.rollback();
                    throw new IllegalArgumentException(
                            "Link a private project before adding personal cockpit actions.");
                }
                ActionSet sharedActionSet = new ActionSetDao(dataSession).createActionSet(webUser,
                        ActionSetType.SHARED);
                ActionNext sharedAction = actionSentenceImportService.buildActionFromSentenceForProject(webUser,
                        dataSession, selectedProject, normalizedSentenceInput, selectedProject.getWorkspaceId(), false);
                ActionNext privateAction = actionSentenceImportService.buildActionFromSentenceForProject(webUser,
                        dataSession, linkedPrivateProject, normalizedSentenceInput,
                        linkedPrivateProject.getWorkspaceId(), false);
                if (sharedAction == null || privateAction == null) {
                    transaction.rollback();
                    throw new IllegalArgumentException("Unable to create action from quick capture sentence.");
                }
                prepareQuickCaptureAction(sharedAction, selectedProject, sharedActionSet);
                prepareQuickCaptureAction(privateAction, linkedPrivateProject, sharedActionSet);
                dataSession.save(sharedAction);
                dataSession.save(privateAction);
                transaction.commit();
                return "Quick capture saved.";
            }

            // Preserve the original actor token (for example "Someone will") so actor
            // assignment is
            // resolved correctly instead of being normalized back to current user phrasing.
            String sharedSentenceInput = sentenceInput;
            ActionNext action = actionSentenceImportService.buildActionFromSentenceForProject(webUser, dataSession,
                    selectedProject, sharedSentenceInput, selectedProject.getWorkspaceId(), false);
            if (action == null) {
                transaction.rollback();
                throw new IllegalArgumentException("Unable to create action from quick capture sentence.");
            }
            prepareQuickCaptureAction(action, selectedProject, action.getActionSet());
            dataSession.save(action);
            transaction.commit();
            return "Quick capture saved.";
        } catch (IllegalArgumentException iae) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw iae;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new IllegalArgumentException("Unable to save quick capture: " + e.getMessage());
        }
    }

    private void prepareQuickCaptureAction(ActionNext action, Project project, ActionSet actionSet) {
        action.setWorkspaceId(project.getWorkspaceId());
        action.setProject(project);
        action.setProjectId(project.getProjectId());
        action.setActionSet(actionSet);
    }

    private boolean isCurrentUserContact(WebUser webUser,
            org.openimmunizationsoftware.pt.model.ProjectContact contact) {
        return webUser != null && contact != null && contact.getContactId() == webUser.getContactId();
    }

    private Project resolveLinkedPrivateProject(WebUser webUser, Session dataSession, Project sharedProject,
            Integer requestedPrivateProjectId) {
        if (webUser == null || dataSession == null || sharedProject == null) {
            return null;
        }
        Integer privateWorkspaceId = WorkspaceRegistry.getWorkspaceIdForWebUserId(dataSession,
                webUser.getWebUserId());
        if (privateWorkspaceId == null) {
            return null;
        }
        List<Project> candidatePrivateProjects = loadProjects(webUser, dataSession, privateWorkspaceId);
        Map<Integer, Project> candidateById = indexProjectsById(candidatePrivateProjects);
        List<Project> linkedPrivateProjects = loadLinkedPrivateProjectsForSharedProject(dataSession,
                sharedProject.getProjectId(), candidateById);
        return resolveSelectedPrivateProject(linkedPrivateProjects, candidateById, requestedPrivateProjectId);
    }

    private Project resolveSelectedPrivateProject(List<Project> linkedPrivateProjects,
            Map<Integer, Project> candidateById,
            Integer requestedPrivateProjectId) {
        if (requestedPrivateProjectId != null) {
            for (Project linkedProject : linkedPrivateProjects) {
                if (linkedProject != null && linkedProject.getProjectId() == requestedPrivateProjectId.intValue()) {
                    return linkedProject;
                }
            }
        }
        if (linkedPrivateProjects.size() == 1) {
            return linkedPrivateProjects.get(0);
        }
        if (linkedPrivateProjects.size() > 1) {
            return linkedPrivateProjects.get(0);
        }
        if (requestedPrivateProjectId != null && candidateById.containsKey(requestedPrivateProjectId)) {
            return null;
        }
        return null;
    }

    private Map<Integer, Project> indexProjectsById(List<Project> projects) {
        Map<Integer, Project> projectsById = new LinkedHashMap<Integer, Project>();
        for (Project project : projects) {
            if (project != null) {
                projectsById.put(Integer.valueOf(project.getProjectId()), project);
            }
        }
        return projectsById;
    }

    private List<Project> loadLinkedPrivateProjectsForSharedProject(Session dataSession, int sharedProjectId,
            Map<Integer, Project> candidateById) {
        List<Project> linkedProjects = new ArrayList<Project>();
        if (candidateById == null || candidateById.isEmpty()) {
            return linkedProjects;
        }

        ProjectPatchLinkDao patchLinkDao = new ProjectPatchLinkDao(dataSession);
        List<ProjectPatchLink> links = patchLinkDao.listDirectLinksForPatchProject(sharedProjectId);
        Map<Integer, Project> linkedById = new LinkedHashMap<Integer, Project>();
        for (ProjectPatchLink link : links) {
            if (link == null) {
                continue;
            }
            Project privateProject = candidateById.get(Integer.valueOf(link.getPrivateProjectId()));
            if (privateProject != null) {
                linkedById.put(Integer.valueOf(privateProject.getProjectId()), privateProject);
            }
        }
        linkedProjects.addAll(linkedById.values());
        return linkedProjects;
    }

    public String updateLastReviewNow(AppReq appReq, int projectId) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        Project project = (Project) dataSession.get(Project.class, projectId);
        if (project == null) {
            return "Project was not found";
        }
        ProjectContactAssigned assigned = loadProjectContactAssigned(webUser, dataSession, project);
        Transaction transaction = dataSession.beginTransaction();
        try {
            if (assigned == null) {
                assigned = new ProjectContactAssigned();
                assigned.setId(new ProjectContactAssignedId());
                assigned.getId().setContactId(webUser.getContactId());
                assigned.getId().setProjectId(project.getProjectId());
                assigned.setEmailAlert("Y");
                assigned.setUpdateDue(0);
            }
            assigned.setUpdateLast(new Date());
            dataSession.saveOrUpdate(assigned);
            transaction.commit();
            return null;
        } catch (Exception e) {
            transaction.rollback();
            return "Unable to update review timestamp: " + e.getMessage();
        }
    }

    public int bulkImportActions(AppReq appReq, int projectId, String bulkImportText) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        if (bulkImportText == null || bulkImportText.trim().length() == 0) {
            throw new IllegalArgumentException("Bulk import text is required");
        }

        Project selectedProject = null;
        List<Project> projects = loadProjectsForCurrentWorkspace(appReq, webUser, dataSession);
        for (Project project : projects) {
            if (project.getProjectId() == projectId) {
                selectedProject = project;
                break;
            }
        }
        if (selectedProject == null) {
            throw new IllegalArgumentException("Project is not available");
        }

        return actionSentenceImportService.importActionsFromText(webUser, dataSession, selectedProject, projects,
                bulkImportText);
    }

    public List<ActionNext> loadUnscheduledReviewActions(AppReq appReq) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        Query query = dataSession.createQuery(
                "select distinct an from ActionNext an "
                        + "left join fetch an.project "
                        + "left join fetch an.contact "
                        + "left join fetch an.nextProjectContact "
                        + "where an.workspaceId = :workspaceId and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                        + "and an.nextDescription <> '' "
                        + "and an.nextActionStatusString = :status "
                        + "and an.nextActionDate is null "
                        + "order by an.projectId, an.priorityLevel desc, an.nextChangeDate");
        query.setParameter("workspaceId", appReq.getActiveWorkspaceId());
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        @SuppressWarnings("unchecked")
        List<ActionNext> actions = query.list();
        return actions;
    }

    public ReplaceUnscheduledResult replaceUnscheduledActions(AppReq appReq, int defaultProjectId,
            List<Integer> selectedActionIds, String bulkImportText) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();

        if (selectedActionIds == null || selectedActionIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one unscheduled action to replace");
        }
        if (bulkImportText == null || bulkImportText.trim().length() == 0) {
            throw new IllegalArgumentException("Bulk import text is required");
        }

        List<Project> projects = loadProjectsForCurrentWorkspace(appReq, webUser, dataSession);
        Project defaultProject = null;
        for (Project project : projects) {
            if (project.getProjectId() == defaultProjectId) {
                defaultProject = project;
                break;
            }
        }
        if (defaultProject == null) {
            throw new IllegalArgumentException("Default project is not available");
        }

        Query query = dataSession.createQuery(
                "select distinct an from ActionNext an "
                        + "where an.actionNextId in (:ids) "
                        + "and an.workspaceId = :workspaceId "
                        + "and (an.contactId = :contactId or an.nextContactId = :nextContactId) "
                        + "and an.nextActionStatusString = :status "
                        + "and an.nextActionDate is null");
        query.setParameterList("ids", selectedActionIds);
        query.setParameter("workspaceId", appReq.getActiveWorkspaceId());
        query.setParameter("contactId", webUser.getContactId());
        query.setParameter("nextContactId", webUser.getContactId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        @SuppressWarnings("unchecked")
        List<ActionNext> selectedActions = query.list();

        if (selectedActions.isEmpty()) {
            throw new IllegalArgumentException("Selected actions were not available for replacement");
        }

        Transaction cancelTransaction = dataSession.beginTransaction();
        int cancelledCount = 0;
        try {
            Date now = new Date();
            for (ActionNext action : selectedActions) {
                action.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
                action.setNextChangeDate(now);
                dataSession.update(action);
                cancelledCount++;
            }
            cancelTransaction.commit();
        } catch (Exception e) {
            cancelTransaction.rollback();
            throw new IllegalArgumentException("Unable to cancel selected actions: " + e.getMessage());
        }

        int importedCount = actionSentenceImportService.importActionsFromText(webUser, dataSession, defaultProject,
                projects, bulkImportText);
        if (importedCount <= 0) {
            throw new IllegalArgumentException("No actions were imported");
        }

        ReplaceUnscheduledResult result = new ReplaceUnscheduledResult();
        result.setCancelledCount(cancelledCount);
        result.setImportedCount(importedCount);
        return result;
    }

    private List<Project> loadProjects(WebUser webUser, Session dataSession) {
        return loadProjects(webUser, dataSession, null);
    }

    private List<Project> loadProjectsForCurrentWorkspace(AppReq appReq, WebUser webUser, Session dataSession) {
        Integer activeWorkspaceId = appReq == null ? null : appReq.getActiveWorkspaceId();
        return loadProjects(webUser, dataSession, activeWorkspaceId);
    }

    private List<Project> loadProjects(WebUser webUser, Session dataSession, Integer contextWorkspaceId) {
        Integer workspaceId = contextWorkspaceId;
        if (workspaceId == null && webUser != null) {
            workspaceId = org.openimmunizationsoftware.pt.WorkspaceRegistry.getWorkspaceIdForWebUserId(dataSession,
                    webUser.getWebUserId());
        }
        Query query = dataSession.createQuery(
                "from Project where workspaceId = :workspaceId and (projectStatus is null or projectStatus <> :closedStatus) order by priorityLevel desc, projectName");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("closedStatus", STATUS_CLOSED);
        @SuppressWarnings("unchecked")
        List<Project> projects = query.list();
        return projects;
    }

    private String resolvePatchTagSelection(List<ProjectTagSummaryRowModel> summaryRows, String selectedPatchTagKey) {
        if (selectedPatchTagKey == null || selectedPatchTagKey.trim().length() == 0) {
            return null;
        }
        String normalized = selectedPatchTagKey.trim();
        for (ProjectTagSummaryRowModel row : summaryRows) {
            if (normalized.equals(row.getTagKey())) {
                return normalized;
            }
        }
        return null;
    }

    private List<ProjectTagSummaryRowModel> buildPatchTagSummaryRows(Session dataSession, int workspaceId) {
        List<ProjectTagSummaryRowModel> rows = new ArrayList<ProjectTagSummaryRowModel>();

        Query tagQuery = dataSession.createQuery(
                "from ProjectTag where workspaceId = :workspaceId and tagStatus = :tagStatus"
                        + " order by sortOrder, tagName");
        tagQuery.setParameter("workspaceId", Integer.valueOf(workspaceId));
        tagQuery.setParameter("tagStatus", ProjectTag.STATUS_ACTIVE);

        @SuppressWarnings("unchecked")
        List<ProjectTag> tags = tagQuery.list();
        for (ProjectTag tag : tags) {
            Number count = (Number) dataSession.createQuery(
                    "select count(distinct p.projectId) from Project p "
                            + "where p.workspaceId = :workspaceId "
                            + "and (p.projectStatus is null or p.projectStatus = :activeStatus) "
                            + "and exists (select 1 from ProjectTagMap ptm where ptm.projectId = p.projectId and ptm.projectTagId = :tagId)")
                    .setParameter("workspaceId", Integer.valueOf(workspaceId))
                    .setParameter("activeStatus", STATUS_ACTIVE)
                    .setParameter("tagId", Integer.valueOf(tag.getProjectTagId()))
                    .uniqueResult();

            ProjectTagSummaryRowModel row = new ProjectTagSummaryRowModel();
            row.setTagKey(Integer.toString(tag.getProjectTagId()));
            row.setTagLabel(n(tag.getTagName(), "(unnamed tag)"));
            row.setActiveProjectCount(count == null ? 0 : count.intValue());
            rows.add(row);
        }

        Number untaggedCount = (Number) dataSession.createQuery(
                "select count(*) from Project p "
                        + "where p.workspaceId = :workspaceId "
                        + "and (p.projectStatus is null or p.projectStatus = :activeStatus) "
                        + "and not exists (select 1 from ProjectTagMap ptm where ptm.projectId = p.projectId)")
                .setParameter("workspaceId", Integer.valueOf(workspaceId))
                .setParameter("activeStatus", STATUS_ACTIVE)
                .uniqueResult();

        ProjectTagSummaryRowModel untaggedRow = new ProjectTagSummaryRowModel();
        untaggedRow.setTagKey(ProjectHealthPageModel.PATCH_TAG_KEY_UNTAGGED);
        untaggedRow.setTagLabel("Untagged");
        untaggedRow.setActiveProjectCount(untaggedCount == null ? 0 : untaggedCount.intValue());
        rows.add(untaggedRow);
        return rows;
    }

    private List<Project> filterProjectsForPatchTag(List<Project> projects, Session dataSession, int workspaceId,
            String patchTagKey) {
        List<Project> filtered = new ArrayList<Project>();
        Integer patchTagId = parseInteger(patchTagKey);
        for (Project project : projects) {
            if (project == null || project.getWorkspaceId() == null
                    || project.getWorkspaceId().intValue() != workspaceId) {
                continue;
            }
            String status = normalizeProjectStatus(project.getProjectStatus());
            if (!STATUS_ACTIVE.equals(status)) {
                continue;
            }

            if (ProjectHealthPageModel.PATCH_TAG_KEY_UNTAGGED.equals(patchTagKey)) {
                if (!hasTagMapping(dataSession, project.getProjectId(), null)) {
                    filtered.add(project);
                }
                continue;
            }

            if (patchTagId != null && hasTagMapping(dataSession, project.getProjectId(), patchTagId)) {
                filtered.add(project);
            }
        }
        return filtered;
    }

    private boolean hasTagMapping(Session dataSession, int projectId, Integer tagId) {
        Query query;
        if (tagId == null) {
            query = dataSession.createQuery("select count(*) from ProjectTagMap where projectId = :projectId");
            query.setParameter("projectId", Integer.valueOf(projectId));
        } else {
            query = dataSession.createQuery(
                    "select count(*) from ProjectTagMap where projectId = :projectId and projectTagId = :tagId");
            query.setParameter("projectId", Integer.valueOf(projectId));
            query.setParameter("tagId", tagId);
        }
        Number count = (Number) query.uniqueResult();
        return count != null && count.intValue() > 0;
    }

    private int resolveSelectedProjectIdFromRequest(AppReq appReq, List<Project> projects) {
        String selectedProjectIdStr = appReq.getRequest().getParameter(PARAM_PROJECT_ID);
        if (selectedProjectIdStr == null || selectedProjectIdStr.trim().length() == 0) {
            return 0;
        }
        Integer selectedProjectId = parseInteger(selectedProjectIdStr);
        if (selectedProjectId == null) {
            return 0;
        }
        for (Project project : projects) {
            if (project != null && project.getProjectId() == selectedProjectId.intValue()) {
                return selectedProjectId.intValue();
            }
        }
        return 0;
    }

    private int resolveSelectedPatchProjectId(AppReq appReq, List<Project> projects, int contextWorkspaceId) {
        int selectedFromRequest = resolveSelectedProjectIdFromRequest(appReq, projects);
        if (selectedFromRequest > 0) {
            return selectedFromRequest;
        }
        Integer selectedFromSession = readPatchSelectedProjectId(appReq, contextWorkspaceId);
        if (selectedFromSession != null) {
            for (Project project : projects) {
                if (project != null && project.getProjectId() == selectedFromSession.intValue()) {
                    return selectedFromSession.intValue();
                }
            }
        }
        return 0;
    }

    private boolean isPatchContextWorkspace(AppReq appReq, Session dataSession) {
        if (appReq == null || appReq.getActiveWorkspaceId() == null) {
            return false;
        }
        Workspace workspace = (Workspace) dataSession.get(Workspace.class, appReq.getActiveWorkspaceId());
        return workspace != null && Workspace.TYPE_PATCH.equals(workspace.getWorkspaceType());
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (Exception e) {
            return null;
        }
    }

    private int resolveSelectedProjectId(AppReq appReq, List<Project> projects, WebUser webUser,
            Session dataSession) {
        String selectedProjectIdStr = appReq.getRequest().getParameter(PARAM_PROJECT_ID);
        if (selectedProjectIdStr != null && selectedProjectIdStr.trim().length() > 0) {
            try {
                int selectedProjectId = Integer.parseInt(selectedProjectIdStr.trim());
                for (Project project : projects) {
                    if (project.getProjectId() == selectedProjectId) {
                        writePrivateSelectedProjectId(appReq, selectedProjectId);
                        return selectedProjectId;
                    }
                }
            } catch (NumberFormatException nfe) {
                // ignore invalid request parameter and use default selection logic
            }
        }

        Integer selectedFromSession = readPrivateSelectedProjectId(appReq);
        if (selectedFromSession != null) {
            for (Project project : projects) {
                if (project != null && project.getProjectId() == selectedFromSession.intValue()) {
                    return selectedFromSession.intValue();
                }
            }
        }

        for (Project project : projects) {
            if (!isPersonalProject(project, dataSession)) {
                writePrivateSelectedProjectId(appReq, project.getProjectId());
                return project.getProjectId();
            }
        }
        int fallbackProjectId = projects.isEmpty() ? 0 : projects.get(0).getProjectId();
        if (fallbackProjectId > 0) {
            writePrivateSelectedProjectId(appReq, fallbackProjectId);
        }
        return fallbackProjectId;
    }

    private String patchTagSessionKey(int contextWorkspaceId) {
        return SESSION_PATCH_SELECTED_TAG_PREFIX + contextWorkspaceId;
    }

    private String patchProjectSessionKey(int contextWorkspaceId) {
        return SESSION_PATCH_SELECTED_PROJECT_PREFIX + contextWorkspaceId;
    }

    private String sharedCockpitPrivateProjectSessionKey(int sharedProjectId) {
        return SESSION_SHARED_COCKPIT_PRIVATE_PROJECT_PREFIX + sharedProjectId;
    }

    private HttpSession getHttpSession(AppReq appReq) {
        if (appReq == null || appReq.getRequest() == null) {
            return null;
        }
        return appReq.getRequest().getSession(true);
    }

    private String readPatchSelectedTag(AppReq appReq, int contextWorkspaceId) {
        HttpSession session = getHttpSession(appReq);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(patchTagSessionKey(contextWorkspaceId));
        return value instanceof String ? (String) value : null;
    }

    private void writePatchSelectedTag(AppReq appReq, int contextWorkspaceId, String selectedTagKey) {
        HttpSession session = getHttpSession(appReq);
        if (session == null) {
            return;
        }
        if (selectedTagKey == null || selectedTagKey.trim().length() == 0) {
            session.removeAttribute(patchTagSessionKey(contextWorkspaceId));
            return;
        }
        session.setAttribute(patchTagSessionKey(contextWorkspaceId), selectedTagKey.trim());
    }

    private Integer readPatchSelectedProjectId(AppReq appReq, int contextWorkspaceId) {
        HttpSession session = getHttpSession(appReq);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(patchProjectSessionKey(contextWorkspaceId));
        return value instanceof Integer ? (Integer) value : null;
    }

    private void writePatchSelectedProjectId(AppReq appReq, int contextWorkspaceId, int projectId) {
        HttpSession session = getHttpSession(appReq);
        if (session == null) {
            return;
        }
        session.setAttribute(patchProjectSessionKey(contextWorkspaceId), Integer.valueOf(projectId));
    }

    private void clearPatchSelectedProjectId(AppReq appReq, int contextWorkspaceId) {
        HttpSession session = getHttpSession(appReq);
        if (session == null) {
            return;
        }
        session.removeAttribute(patchProjectSessionKey(contextWorkspaceId));
    }

    private Integer readPrivateSelectedProjectId(AppReq appReq) {
        HttpSession session = getHttpSession(appReq);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_PRIVATE_SELECTED_PROJECT_ID);
        return value instanceof Integer ? (Integer) value : null;
    }

    private void writePrivateSelectedProjectId(AppReq appReq, int projectId) {
        HttpSession session = getHttpSession(appReq);
        if (session == null) {
            return;
        }
        session.setAttribute(SESSION_PRIVATE_SELECTED_PROJECT_ID, Integer.valueOf(projectId));
    }

    private void clearPatchSelectionState(AppReq appReq, int contextWorkspaceId) {
        writePatchSelectedTag(appReq, contextWorkspaceId, null);
        clearPatchSelectedProjectId(appReq, contextWorkspaceId);
    }

    private Integer readSharedCockpitSelectedPrivateProjectId(AppReq appReq, int sharedProjectId) {
        HttpSession session = getHttpSession(appReq);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(sharedCockpitPrivateProjectSessionKey(sharedProjectId));
        return value instanceof Integer ? (Integer) value : null;
    }

    private void writeSharedCockpitSelectedPrivateProjectId(AppReq appReq, int sharedProjectId, int privateProjectId) {
        HttpSession session = getHttpSession(appReq);
        if (session == null) {
            return;
        }
        session.setAttribute(sharedCockpitPrivateProjectSessionKey(sharedProjectId), Integer.valueOf(privateProjectId));
    }

    private void clearSharedCockpitSelectedPrivateProjectId(AppReq appReq, int sharedProjectId) {
        HttpSession session = getHttpSession(appReq);
        if (session == null) {
            return;
        }
        session.removeAttribute(sharedCockpitPrivateProjectSessionKey(sharedProjectId));
    }

    private Map<Integer, ProjectStats> buildStatsByProject(List<Project> projects, WebUser webUser,
            Session dataSession, Map<Integer, Integer> updateDueByProject) {
        Map<Integer, ProjectStats> statsMap = new HashMap<Integer, ProjectStats>();

        LocalDate today = webUser.getLocalDateToday();
        Date todayDate = webUser.toDate(today);

        for (Project project : projects) {
            ProjectStats stats = new ProjectStats();
            stats.undatedOpen = countOpenUndated(dataSession, project);
            stats.overdueOpen = countOpenOverdue(dataSession, project, today);
            stats.lastReview = loadLastReview(dataSession, webUser, project);
            stats.reviewScheduledToday = hasReviewScheduledToday(dataSession, project, today);

            Integer updateDue = updateDueByProject.get(project.getProjectId());
            stats.updateDue = updateDue == null ? 0 : updateDue.intValue();

            stats.missingDescription = project.getDescription() == null
                    || project.getDescription().trim().length() == 0;
            stats.missingOutcome = project.getOutcomeText() == null
                    || project.getOutcomeText().trim().length() == 0;
            stats.missingSuccessCriteria = project.getSuccessCriteriaText() == null
                    || project.getSuccessCriteriaText().trim().length() == 0;
            stats.missingReviewPeriod = !hasReviewPeriod(stats.updateDue);

            if (stats.reviewScheduledToday) {
                stats.reviewOverdue = false;
            } else if (stats.updateDue > 0) {
                if (stats.lastReview == null) {
                    stats.reviewOverdue = true;
                } else {
                    Calendar dueDate = webUser.getCalendar();
                    dueDate.setTime(stats.lastReview);
                    dueDate.add(Calendar.DAY_OF_MONTH, stats.updateDue);
                    stats.reviewOverdue = todayDate.after(dueDate.getTime());
                }
            }

            statsMap.put(project.getProjectId(), stats);
        }

        return statsMap;
    }

    private ReprioritizeMode parseReprioritizeMode(String value) {
        if (value == null || value.trim().length() == 0) {
            return ReprioritizeMode.BEFORE;
        }
        String mode = value.trim().toUpperCase();
        if ("FIRST".equals(mode)) {
            return ReprioritizeMode.FIRST;
        }
        if ("LAST".equals(mode)) {
            return ReprioritizeMode.LAST;
        }
        return ReprioritizeMode.BEFORE;
    }

    private List<ProjectCadenceGroupModel> createCadenceGroups() {
        List<ProjectCadenceGroupModel> groups = new ArrayList<ProjectCadenceGroupModel>();
        for (ReviewInterval interval : ReviewInterval.values()) {
            ProjectCadenceGroupModel group = new ProjectCadenceGroupModel();
            group.setGroupKey(interval.name());
            group.setGroupLabel(interval.getDescription());
            groups.add(group);
        }
        ProjectCadenceGroupModel noneGroup = new ProjectCadenceGroupModel();
        noneGroup.setGroupKey(BUCKET_NONE);
        noneGroup.setGroupLabel("No Review Period");
        groups.add(noneGroup);
        return groups;
    }

    private Map<String, ProjectCadenceGroupModel> toGroupMap(List<ProjectCadenceGroupModel> groups) {
        Map<String, ProjectCadenceGroupModel> map = new LinkedHashMap<String, ProjectCadenceGroupModel>();
        for (ProjectCadenceGroupModel group : groups) {
            map.put(group.getGroupKey(), group);
        }
        return map;
    }

    private Map<Integer, Integer> loadUpdateDueByProject(WebUser webUser, Session dataSession, List<Project> projects) {
        Map<Integer, Integer> updateDueByProject = new HashMap<Integer, Integer>();
        if (projects == null || projects.isEmpty()) {
            return updateDueByProject;
        }

        List<Integer> projectIds = new ArrayList<Integer>();
        for (Project project : projects) {
            if (project != null) {
                projectIds.add(Integer.valueOf(project.getProjectId()));
            }
        }
        if (projectIds.isEmpty()) {
            return updateDueByProject;
        }

        Query query = dataSession.createQuery(
                "select pca.id.projectId, pca.updateDue from ProjectContactAssigned pca "
                        + "where pca.id.contactId = :contactId and pca.id.projectId in (:projectIds)");
        query.setParameter("contactId", webUser.getContactId());
        query.setParameterList("projectIds", projectIds);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.list();
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            Number projectId = (Number) row[0];
            Number updateDue = (Number) row[1];
            updateDueByProject.put(Integer.valueOf(projectId.intValue()),
                    Integer.valueOf(updateDue == null ? 0 : updateDue.intValue()));
        }
        return updateDueByProject;
    }

    private String bucketKeyForUpdateDue(Integer updateDue) {
        if (updateDue == null || updateDue.intValue() <= 0) {
            return BUCKET_NONE;
        }
        int days = updateDue.intValue();
        for (ReviewInterval interval : ReviewInterval.values()) {
            if (interval.getDays() == days) {
                return interval.name();
            }
        }
        return BUCKET_NONE;
    }

    private int bucketRankForUpdateDue(Integer updateDue) {
        String bucketKey = bucketKeyForUpdateDue(updateDue);
        int rank = 0;
        for (ReviewInterval interval : ReviewInterval.values()) {
            if (interval.name().equals(bucketKey)) {
                return rank;
            }
            rank++;
        }
        return ReviewInterval.values().length;
    }

    private ProjectListItemModel toListItem(Project project, ProjectStats stats, int selectedProjectId,
            Map<Integer, String> displayNameByProjectId) {
        ProjectListItemModel item = new ProjectListItemModel();
        item.setProjectId(project.getProjectId());
        item.setProjectName(resolveProjectDisplayName(project, displayNameByProjectId));
        item.setPriorityLevel(project.getPriorityLevel());
        item.setSelected(project.getProjectId() == selectedProjectId);
        item.setOverdueOpenCount(stats.overdueOpen);
        item.setUndatedOpenCount(stats.undatedOpen);
        item.setReviewOverdue(stats.reviewOverdue);

        if (stats.missingDescription || stats.missingOutcome || stats.missingSuccessCriteria
                || stats.missingReviewPeriod || stats.overdueOpen > 0
                || stats.reviewOverdue) {
            item.setHealthLevel(ProjectListItemModel.HealthLevel.ATTENTION_NEEDED);
            item.setHealthLabel("attention needed");
        } else if (stats.undatedOpen > 0) {
            item.setHealthLevel(ProjectListItemModel.HealthLevel.NEEDS_REVIEW);
            item.setHealthLabel("needs review");
        } else {
            item.setHealthLevel(ProjectListItemModel.HealthLevel.HEALTHY);
            item.setHealthLabel("healthy");
        }

        return item;
    }

    private ProjectReportModel buildReport(AppReq appReq, Project project, ProjectStats stats,
            Map<Integer, String> displayNameByProjectId) {
        ProjectReportModel report = new ProjectReportModel();
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();

        report.setProjectId(project.getProjectId());
        report.setProjectName(resolveProjectDisplayName(project, displayNameByProjectId));
        report.setDescription(n(project.getDescription()));
        report.setCategory(loadTagSummaryForProject(dataSession, project.getProjectId()));
        report.setPhase(normalizeProjectStatus(project.getProjectStatus()));
        report.setUndatedOpenCount(stats.undatedOpen);
        report.setOverdueOpenCount(stats.overdueOpen);
        report.setUpdateDueDays(stats.updateDue);
        report.setLastReviewLabel(formatDate(webUser, stats.lastReview));

        report.setRecentCompleted(loadCompletedLines(dataSession, webUser, project));
        report.setScheduledOpen(loadScheduledOpenLines(dataSession, webUser, project));
        report.setUnscheduledOpen(loadUnscheduledOpenLines(dataSession, webUser, project));
        report.setOpenProjectIssues(loadOpenProjectIssueLines(dataSession, project));
        report.setRecentNarratives(loadRecentNarrativeLines(dataSession, webUser, project));

        List<String> recommendations = new ArrayList<String>();
        if (stats.overdueOpen > 0) {
            recommendations.add("Replan overdue actions and move non-critical tasks out of today.");
        }
        if (stats.undatedOpen > 0) {
            recommendations.add("Schedule undated actions so project progress is visible on the calendar.");
        }
        if (stats.reviewOverdue) {
            recommendations.add("Run a formal project review this week and record updated priorities.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Continue current cadence and keep the backlog groomed.");
        }
        report.setNextRecommendations(recommendations);
        report.setReportText(buildReportText(report));

        return report;
    }

    private String resolveProjectDisplayName(Project project, Map<Integer, String> displayNameByProjectId) {
        if (project == null) {
            return "";
        }
        String displayName = displayNameByProjectId == null ? null : displayNameByProjectId.get(project.getProjectId());
        if (displayName != null) {
            return n(displayName);
        }
        return n(project.getProjectName());
    }

    private Map<Integer, String> buildPrivateDisplayNameMap(List<Project> projects) {
        Map<Integer, String> displayNameByProjectId = new HashMap<Integer, String>();
        if (projects == null) {
            return displayNameByProjectId;
        }
        for (Project project : projects) {
            if (project == null) {
                continue;
            }
            displayNameByProjectId.put(project.getProjectId(), n(project.getProjectName()));
        }
        return displayNameByProjectId;
    }

    private List<ProjectHealthIssueModel> buildIssues(ProjectReportModel report, ProjectStats stats) {
        List<ProjectHealthIssueModel> issues = new ArrayList<ProjectHealthIssueModel>();

        if (stats.missingDescription || stats.missingOutcome || stats.missingSuccessCriteria
                || stats.missingReviewPeriod) {
            StringBuilder detail = new StringBuilder();
            if (stats.missingDescription) {
                detail.append("Description is missing.");
            }
            if (stats.missingOutcome) {
                if (detail.length() > 0) {
                    detail.append(" ");
                }
                detail.append("Project outcome is missing.");
            }
            if (stats.missingSuccessCriteria) {
                if (detail.length() > 0) {
                    detail.append(" ");
                }
                detail.append("Success criteria are missing.");
            }
            if (stats.missingReviewPeriod) {
                if (detail.length() > 0) {
                    detail.append(" ");
                }
                detail.append("Review period is not configured (set Update Every).");
            }
            issues.add(makeIssue(ProjectHealthIssueModel.Severity.CRITICAL,
                    "Project setup incomplete",
                    detail.toString()));
        }

        if (stats.reviewOverdue) {
            issues.add(makeIssue(ProjectHealthIssueModel.Severity.CRITICAL,
                    "Project review overdue",
                    "Update cadence is set to every " + labelForDays(stats.updateDue) + " and last review was "
                            + n(report.getLastReviewLabel(), "not recorded") + "."));
        }
        if (stats.overdueOpen > 0) {
            issues.add(makeIssue(ProjectHealthIssueModel.Severity.CRITICAL,
                    "Overdue actions",
                    stats.overdueOpen + " open actions are past due."));
        }
        if (stats.undatedOpen > 0) {
            issues.add(makeIssue(ProjectHealthIssueModel.Severity.WARNING,
                    "Undated backlog",
                    stats.undatedOpen + " open actions have no date."));
        }
        if (issues.isEmpty()) {
            issues.add(makeIssue(ProjectHealthIssueModel.Severity.INFO,
                    "No major issues",
                    "Project appears healthy based on current review and action signals."));
        }

        return issues;
    }

    private ProjectHealthIssueModel makeIssue(ProjectHealthIssueModel.Severity severity, String title, String detail) {
        ProjectHealthIssueModel issue = new ProjectHealthIssueModel();
        issue.setSeverity(severity);
        issue.setTitle(title);
        issue.setDetail(detail);
        return issue;
    }

    private List<ProjectReportModel.ReportActionLine> loadCompletedLines(Session dataSession, WebUser webUser,
            Project project) {
        Query query = dataSession.createQuery(
                "from ActionTaken where projectId = :projectId and contactId = :contactId order by actionDate desc");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("contactId", webUser.getContactId());
        query.setMaxResults(8);
        @SuppressWarnings("unchecked")
        List<ActionTaken> rows = query.list();

        List<ProjectReportModel.ReportActionLine> lines = new ArrayList<ProjectReportModel.ReportActionLine>();
        for (ActionTaken row : rows) {
            ProjectReportModel.ReportActionLine line = new ProjectReportModel.ReportActionLine();
            line.setActionId(row.getActionTakenId());
            line.setDescription(n(row.getActionDescription()));
            line.setWhenLabel(formatDate(webUser, row.getActionDate()));
            lines.add(line);
        }
        return lines;
    }

    private List<ProjectReportModel.ReportActionLine> loadScheduledOpenLines(Session dataSession, WebUser webUser,
            Project project) {
        Query query = dataSession.createQuery(
                "from ActionNext an where an.projectId = :projectId and an.nextActionStatusString = :status and an.nextDescription <> '' and an.nextActionDate is not null order by an.nextActionDate, an.priorityLevel desc");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        query.setMaxResults(20);
        @SuppressWarnings("unchecked")
        List<ActionNext> rows = query.list();

        List<ProjectReportModel.ReportActionLine> lines = new ArrayList<ProjectReportModel.ReportActionLine>();
        for (ActionNext row : rows) {
            ProjectReportModel.ReportActionLine line = new ProjectReportModel.ReportActionLine();
            line.setActionId(row.getActionNextId());
            line.setDescription(n(row.getNextDescription()));
            line.setWhenLabel(formatDate(webUser, row.getNextActionDate()));
            lines.add(line);
        }
        return lines;
    }

    private List<ProjectReportModel.ReportActionLine> loadUnscheduledOpenLines(Session dataSession, WebUser webUser,
            Project project) {
        Query query = dataSession.createQuery(
                "from ActionNext an where an.projectId = :projectId and an.nextActionStatusString = :status and an.nextDescription <> '' and an.nextActionDate is null order by an.priorityLevel desc, an.nextChangeDate");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        query.setMaxResults(20);
        @SuppressWarnings("unchecked")
        List<ActionNext> rows = query.list();

        List<ProjectReportModel.ReportActionLine> lines = new ArrayList<ProjectReportModel.ReportActionLine>();
        for (ActionNext row : rows) {
            ProjectReportModel.ReportActionLine line = new ProjectReportModel.ReportActionLine();
            line.setActionId(row.getActionNextId());
            line.setDescription(n(row.getNextDescription()));
            line.setWhenLabel("undated");
            lines.add(line);
        }
        return lines;
    }

    private List<ProjectReportModel.ReportActionLine> loadOpenProjectIssueLines(Session dataSession, Project project) {
        ProjectIssueDao issueDao = new ProjectIssueDao(dataSession);
        List<ProjectIssue> issues = issueDao.listOpenIssuesForProject(project);
        List<ProjectReportModel.ReportActionLine> lines = new ArrayList<ProjectReportModel.ReportActionLine>();
        for (ProjectIssue issue : issues) {
            ProjectReportModel.ReportActionLine line = new ProjectReportModel.ReportActionLine();
            line.setActionId(issue.getProjectIssueId());
            line.setWhenLabel("");
            String issueType = issue.getIssueType() == null ? "Unknown" : issue.getIssueType().name();
            line.setDescription(issueType + ": " + n(issue.getIssueText()));
            lines.add(line);
        }
        return lines;
    }

    private List<ProjectReportModel.ReportActionLine> loadRecentNarrativeLines(Session dataSession, WebUser webUser,
            Project project) {
        Date now = new Date();
        LocalDate today = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Date ninetyDaysAgo = java.sql.Date.valueOf(today.minusDays(90));

        Query query = dataSession.createQuery(
                "from ProjectNarrative where projectId = :projectId "
                        + "and narrativeDate >= :start and narrativeDate <= :end "
                        + "order by narrativeDate desc, narrativeId desc");
        query.setParameter("projectId", project.getProjectId());
        query.setTimestamp("start", ninetyDaysAgo);
        query.setTimestamp("end", now);
        @SuppressWarnings("unchecked")
        List<ProjectNarrative> narratives = query.list();

        List<ProjectReportModel.ReportActionLine> lines = new ArrayList<ProjectReportModel.ReportActionLine>();
        for (ProjectNarrative narrative : narratives) {
            String text = n(narrative.getNarrativeText());
            if (text.trim().length() == 0) {
                continue;
            }
            ProjectReportModel.ReportActionLine line = new ProjectReportModel.ReportActionLine();
            line.setActionId(narrative.getNarrativeId());
            line.setWhenLabel(formatDate(webUser, narrative.getNarrativeDate()));
            String verbLabel = narrative.getNarrativeVerb() == null ? "Narrative"
                    : narrative.getNarrativeVerb().getLabel();
            line.setDescription(verbLabel + ": " + text);
            lines.add(line);
        }
        return lines;
    }

    private int countOpenUndated(Session dataSession, Project project) {
        Query query = dataSession.createQuery(
                "select count(*) from ActionNext an where an.projectId = :projectId and an.nextActionStatusString = :status and an.nextDescription <> '' and an.nextActionDate is null");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        Number result = (Number) query.uniqueResult();
        return result == null ? 0 : result.intValue();
    }

    private int countOpenOverdue(Session dataSession, Project project, LocalDate today) {
        Query query = dataSession.createQuery(
                "select count(*) from ActionNext an where an.projectId = :projectId and an.nextActionStatusString = :status and an.nextDescription <> '' and an.nextActionDate is not null and an.nextActionDate < :today");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        query.setParameter("today", java.sql.Date.valueOf(today));
        Number result = (Number) query.uniqueResult();
        return result == null ? 0 : result.intValue();
    }

    private Date loadLastReview(Session dataSession, WebUser webUser, Project project) {
        Query query = dataSession.createQuery(
                "select max(actionDate) from ActionTaken where projectId = :projectId and contactId = :contactId");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("contactId", webUser.getContactId());
        return (Date) query.uniqueResult();
    }

    private boolean hasReviewScheduledToday(Session dataSession, Project project, LocalDate today) {
        Query query = dataSession.createQuery(
                "select count(*) from ActionNext an "
                        + "where an.projectId = :projectId "
                        + "and an.nextActionStatusString = :status "
                        + "and an.nextActionDate = :today");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        query.setParameter("today", java.sql.Date.valueOf(today));
        Number result = (Number) query.uniqueResult();
        return result != null && result.intValue() > 0;
    }

    private String buildReportText(ProjectReportModel report) {
        StringBuilder text = new StringBuilder();
        text.append("Project Briefing\n");
        text.append("Project: ").append(n(report.getProjectName())).append("\n");
        text.append("Tags: ").append(n(report.getCategory(), "(none)")).append("\n");
        text.append("Status: ").append(n(report.getPhase(), "(unspecified)")).append("\n");
        text.append("Description: ").append(n(report.getDescription(), "(none)\n")).append("\n\n");

        text.append("Recent Completed Activity\n");
        if (report.getRecentCompleted().isEmpty()) {
            text.append("- none\n");
        } else {
            for (ProjectReportModel.ReportActionLine line : report.getRecentCompleted()) {
                text.append("- ").append(n(line.getWhenLabel(), "date unknown"))
                        .append(": ").append(n(line.getDescription())).append("\n");
            }
        }

        text.append("\nCurrent Open Scheduled Actions\n");
        if (report.getScheduledOpen().isEmpty()) {
            text.append("- none\n");
        } else {
            for (ProjectReportModel.ReportActionLine line : report.getScheduledOpen()) {
                text.append("- ").append(n(line.getWhenLabel(), "undated"))
                        .append(": ").append(n(line.getDescription())).append("\n");
            }
        }

        text.append("\nCurrent Unscheduled / Backlog Actions\n");
        if (report.getUnscheduledOpen().isEmpty()) {
            text.append("- none\n");
        } else {
            for (ProjectReportModel.ReportActionLine line : report.getUnscheduledOpen()) {
                text.append("- ").append(n(line.getDescription())).append("\n");
            }
        }

        text.append("\nOpen Project Issues\n");
        if (report.getOpenProjectIssues().isEmpty()) {
            text.append("- none\n");
        } else {
            for (ProjectReportModel.ReportActionLine line : report.getOpenProjectIssues()) {
                text.append("- ").append(n(line.getDescription())).append("\n");
            }
        }

        text.append("\nProject Narrative (Last 90 Days)\n");
        if (report.getRecentNarratives().isEmpty()) {
            text.append("- none\n");
        } else {
            for (ProjectReportModel.ReportActionLine line : report.getRecentNarratives()) {
                text.append("- ").append(n(line.getWhenLabel(), "date unknown"))
                        .append(": ").append(n(line.getDescription())).append("\n");
            }
        }

        text.append("\nHealth Notes\n");
        text.append("- Overdue open actions: ").append(report.getOverdueOpenCount()).append("\n");
        text.append("- Undated open actions: ").append(report.getUndatedOpenCount()).append("\n");
        if (report.getUpdateDueDays() > 0) {
            text.append("- Review cadence: every ").append(labelForDays(report.getUpdateDueDays())).append("\n");
            text.append("- Last review: ").append(n(report.getLastReviewLabel(), "not recorded")).append("\n");
        }

        text.append("\nWhat Needs To Be Done Next\n");
        for (String recommendation : report.getNextRecommendations()) {
            text.append("- ").append(recommendation).append("\n");
        }

        return text.toString();
    }

    private boolean isPersonalProject(Project project, Session dataSession) {
        return !isWorkProject(project, dataSession);
    }

    private boolean isWorkProject(Project project, Session dataSession) {
        if (project == null) {
            return false;
        }
        BillCode billCode = ClientServlet.resolveBillCode(dataSession, project);
        return billCode != null && "Y".equalsIgnoreCase(billCode.getBillable());
    }

    private boolean isProjectInPrivateWorkspace(Project project, Session dataSession) {
        if (project == null || project.getWorkspaceId() == null) {
            return false;
        }
        Workspace workspace = (Workspace) dataSession.get(Workspace.class, project.getWorkspaceId());
        return workspace != null && Workspace.TYPE_PRIVATE.equals(workspace.getWorkspaceType());
    }

    private String normalizeProjectStatus(String projectStatus) {
        return ProjectStatus.fromDatabaseValue(projectStatus).getDatabaseValue();
    }

    private String loadTagSummaryForProject(Session dataSession, int projectId) {
        Query query = dataSession.createQuery(
                "select pt.tagName from ProjectTagMap ptm, ProjectTag pt "
                        + "where ptm.projectId = :projectId and pt.projectTagId = ptm.projectTagId "
                        + "order by pt.sortOrder, pt.tagName");
        query.setParameter("projectId", projectId);
        @SuppressWarnings("unchecked")
        List<String> tagNames = query.list();
        if (tagNames == null || tagNames.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String tagName : tagNames) {
            if (tagName == null || tagName.trim().length() == 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(tagName.trim());
        }
        return sb.toString();
    }

    private String formatDate(WebUser webUser, Date date) {
        if (date == null) {
            return "";
        }
        return webUser.getDateFormatService().formatPattern(date, webUser.getDateDisplayPatternWithWeekdayShort(),
                webUser.getTimeZone());
    }

    private String labelForDays(int days) {
        if (days <= 0) {
            return "none";
        }
        for (ReviewInterval interval : ReviewInterval.values()) {
            if (days <= interval.getDays()) {
                return interval.getDescription();
            }
        }
        return Integer.toString(days) + " days";
    }

    private boolean hasReviewPeriod(int updateDue) {
        return updateDue > 0;
    }

    private String n(String value) {
        return n(value, "");
    }

    private String n(String value, String fallback) {
        if (value == null || value.trim().length() == 0) {
            return fallback;
        }
        return value;
    }

    private ProjectContactAssigned loadProjectContactAssigned(WebUser webUser, Session dataSession, Project project) {
        if (webUser == null || project == null) {
            return null;
        }
        ProjectContactAssignedId id = new ProjectContactAssignedId();
        id.setContactId(webUser.getContactId());
        id.setProjectId(project.getProjectId());
        return (ProjectContactAssigned) dataSession.get(ProjectContactAssigned.class, id);
    }

    public Date parseReviewDate(String dateValue) {
        if (dateValue == null || dateValue.trim().length() == 0) {
            return null;
        }
        try {
            return new SimpleDateFormat("MM/dd/yyyy").parse(dateValue.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
