package org.dandeliondaily.projecthealth.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.dandeliondaily.projecthealth.model.ProjectHealthIssueModel;
import org.dandeliondaily.projecthealth.model.ProjectHealthPageModel;
import org.dandeliondaily.projecthealth.model.ProjectListItemModel;
import org.dandeliondaily.projecthealth.model.ProjectReportModel;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ProjectReviewServlet.Interval;

public class ProjectHealthPageService {

    public static final String PARAM_PROJECT_ID = "projectId";

    private static class ProjectStats {
        private int undatedOpen;
        private int overdueOpen;
        private Date lastReview;
        private int updateDue;
        private boolean reviewOverdue;
    }

    public ProjectHealthPageModel buildModel(AppReq appReq) {
        ProjectHealthPageModel model = new ProjectHealthPageModel();

        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        List<Project> projects = loadProjects(webUser, dataSession);
        int selectedProjectId = resolveSelectedProjectId(appReq, projects, webUser);
        model.setSelectedProjectId(selectedProjectId);

        Map<Integer, ProjectStats> statsMap = buildStatsByProject(projects, webUser, dataSession);

        List<ProjectListItemModel> workProjects = new ArrayList<ProjectListItemModel>();
        List<ProjectListItemModel> personalProjects = new ArrayList<ProjectListItemModel>();

        Project selectedProject = null;
        for (Project project : projects) {
            ProjectStats stats = statsMap.get(project.getProjectId());
            ProjectListItemModel item = toListItem(project, stats, selectedProjectId);
            if (item.isSelected()) {
                selectedProject = project;
            }
            if (isPersonalProject(project, webUser)) {
                personalProjects.add(item);
            } else {
                workProjects.add(item);
            }
        }

        model.setWorkProjects(workProjects);
        model.setPersonalProjects(personalProjects);

        if (selectedProject != null) {
            appReq.setProject(selectedProject);
            ProjectStats selectedStats = statsMap.get(selectedProject.getProjectId());
            model.setSelectedProjectAvailable(true);
            model.setSelectedProjectName(n(selectedProject.getProjectName()));
            model.setReport(buildReport(appReq, selectedProject, selectedStats));
            model.setIssues(buildIssues(model.getReport(), selectedStats));
        }

        return model;
    }

    public List<ProjectListItemModel> loadReprioritizeCandidates(AppReq appReq, int projectId) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();

        List<Project> all = loadProjects(webUser, dataSession);
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

        boolean personal = isPersonalProject(selected, webUser);
        List<ProjectListItemModel> candidates = new ArrayList<ProjectListItemModel>();
        for (Project project : all) {
            if (project.getProjectId() == projectId) {
                continue;
            }
            if (isPersonalProject(project, webUser) != personal) {
                continue;
            }
            ProjectListItemModel item = new ProjectListItemModel();
            item.setProjectId(project.getProjectId());
            item.setProjectName(n(project.getProjectName()));
            item.setPriorityLevel(project.getPriorityLevel());
            candidates.add(item);
        }
        return candidates;
    }

    public String reprioritizeBefore(AppReq appReq, int projectId, int beforeProjectId) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();

        List<Project> all = loadProjects(webUser, dataSession);
        Project selected = null;
        Project before = null;
        for (Project project : all) {
            if (project.getProjectId() == projectId) {
                selected = project;
            }
            if (project.getProjectId() == beforeProjectId) {
                before = project;
            }
        }
        if (selected == null || before == null) {
            return "Project was not found";
        }

        boolean personal = isPersonalProject(selected, webUser);
        if (isPersonalProject(before, webUser) != personal) {
            return "Projects must be in the same section";
        }

        List<Project> bucket = new ArrayList<Project>();
        for (Project project : all) {
            if (isPersonalProject(project, webUser) == personal) {
                bucket.add(project);
            }
        }

        bucket.remove(selected);
        int target = bucket.indexOf(before);
        if (target < 0) {
            return "Could not determine target position";
        }
        bucket.add(target, selected);

        Transaction transaction = dataSession.beginTransaction();
        try {
            int priority = bucket.size() * 10;
            for (Project project : bucket) {
                project.setPriorityLevel(priority);
                dataSession.update(project);
                priority -= 10;
            }
            transaction.commit();
            return null;
        } catch (Exception e) {
            transaction.rollback();
            return "Unable to reprioritize project: " + e.getMessage();
        }
    }

    public String scheduleProjectReview(AppReq appReq, int projectId, Date reviewDate) {
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();

        Project project = (Project) dataSession.get(Project.class, projectId);
        if (project == null || project.getProvider() == null
                || project.getProvider().getProviderId() != webUser.getProvider().getProviderId()) {
            return "Project is not available";
        }

        if (reviewDate == null) {
            return "Review date is required";
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            ProjectActionNext reviewAction = new ProjectActionNext();
            reviewAction.setProject(project);
            reviewAction.setProjectId(project.getProjectId());
            reviewAction.setContact(webUser.getProjectContact());
            reviewAction.setContactId(webUser.getContactId());
            reviewAction.setProvider(webUser.getProvider());
            reviewAction.setNextActionType(ProjectNextActionType.WILL);
            reviewAction.setNextActionDate(reviewDate);
            reviewAction.setNextDescription("project review");
            reviewAction.setNextSummary("Project health review");
            reviewAction.setNextTimeEstimate(30);
            reviewAction.setNextActionStatus(ProjectNextActionStatus.READY);
            reviewAction.setNextChangeDate(new Date());
            reviewAction.setBillable(project.getBillCode() != null && project.getBillCode().trim().length() > 0);

            dataSession.save(reviewAction);
            transaction.commit();
            return null;
        } catch (Exception e) {
            transaction.rollback();
            return "Unable to schedule project review: " + e.getMessage();
        }
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

    private List<Project> loadProjects(WebUser webUser, Session dataSession) {
        Query query = dataSession.createQuery(
                "from Project where provider = :provider and (phaseCode is null or phaseCode <> 'Clos') order by priorityLevel desc, projectName");
        query.setParameter("provider", webUser.getProvider());
        @SuppressWarnings("unchecked")
        List<Project> projects = query.list();
        return projects;
    }

    private int resolveSelectedProjectId(AppReq appReq, List<Project> projects, WebUser webUser) {
        String selectedProjectIdStr = appReq.getRequest().getParameter(PARAM_PROJECT_ID);
        if (selectedProjectIdStr != null && selectedProjectIdStr.trim().length() > 0) {
            try {
                int selectedProjectId = Integer.parseInt(selectedProjectIdStr.trim());
                for (Project project : projects) {
                    if (project.getProjectId() == selectedProjectId) {
                        return selectedProjectId;
                    }
                }
            } catch (NumberFormatException nfe) {
                // ignore invalid request parameter and use default selection logic
            }
        }

        for (Project project : projects) {
            if (!isPersonalProject(project, webUser)) {
                return project.getProjectId();
            }
        }
        return projects.isEmpty() ? 0 : projects.get(0).getProjectId();
    }

    private Map<Integer, ProjectStats> buildStatsByProject(List<Project> projects, WebUser webUser,
            Session dataSession) {
        Map<Integer, ProjectStats> statsMap = new HashMap<Integer, ProjectStats>();

        Date today = stripTime(webUser.getToday());
        Calendar staleThreshold = webUser.getCalendar();
        staleThreshold.setTime(today);
        staleThreshold.add(Calendar.DAY_OF_MONTH, -30);

        for (Project project : projects) {
            ProjectStats stats = new ProjectStats();
            stats.undatedOpen = countOpenUndated(dataSession, project);
            stats.overdueOpen = countOpenOverdue(dataSession, project, today);
            stats.lastReview = loadLastReview(dataSession, webUser, project);

            ProjectContactAssigned assigned = loadProjectContactAssigned(webUser, dataSession, project);
            if (assigned != null && assigned.getUpdateDue() != null) {
                stats.updateDue = assigned.getUpdateDue();
            }

            if (stats.updateDue > 0) {
                if (stats.lastReview == null) {
                    stats.reviewOverdue = true;
                } else {
                    Calendar dueDate = webUser.getCalendar();
                    dueDate.setTime(stats.lastReview);
                    dueDate.add(Calendar.DAY_OF_MONTH, stats.updateDue);
                    stats.reviewOverdue = today.after(dueDate.getTime());
                }
            }

            statsMap.put(project.getProjectId(), stats);
        }

        return statsMap;
    }

    private ProjectListItemModel toListItem(Project project, ProjectStats stats, int selectedProjectId) {
        ProjectListItemModel item = new ProjectListItemModel();
        item.setProjectId(project.getProjectId());
        item.setProjectName(n(project.getProjectName()));
        item.setPriorityLevel(project.getPriorityLevel());
        item.setSelected(project.getProjectId() == selectedProjectId);
        item.setOverdueOpenCount(stats.overdueOpen);
        item.setUndatedOpenCount(stats.undatedOpen);
        item.setReviewOverdue(stats.reviewOverdue);

        if (stats.overdueOpen > 0 || stats.reviewOverdue) {
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

    private ProjectReportModel buildReport(AppReq appReq, Project project, ProjectStats stats) {
        ProjectReportModel report = new ProjectReportModel();
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();

        report.setProjectId(project.getProjectId());
        report.setProjectName(n(project.getProjectName()));
        report.setDescription(n(project.getDescription()));
        report.setCategory(project.getProjectCategory() == null ? n(project.getCategoryCode())
                : n(project.getProjectCategory().getClientName()));
        report.setPhase(project.getProjectPhase() == null ? n(project.getPhaseCode())
                : n(project.getProjectPhase().getPhaseLabel()));
        report.setUndatedOpenCount(stats.undatedOpen);
        report.setOverdueOpenCount(stats.overdueOpen);
        report.setUpdateDueDays(stats.updateDue);
        report.setLastReviewLabel(formatDate(webUser, stats.lastReview));

        report.setRecentCompleted(loadCompletedLines(dataSession, webUser, project));
        report.setScheduledOpen(loadScheduledOpenLines(dataSession, webUser, project));
        report.setUnscheduledOpen(loadUnscheduledOpenLines(dataSession, webUser, project));

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

    private List<ProjectHealthIssueModel> buildIssues(ProjectReportModel report, ProjectStats stats) {
        List<ProjectHealthIssueModel> issues = new ArrayList<ProjectHealthIssueModel>();

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
                "from ProjectActionTaken where projectId = :projectId and contactId = :contactId order by actionDate desc");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("contactId", webUser.getContactId());
        query.setMaxResults(8);
        @SuppressWarnings("unchecked")
        List<ProjectActionTaken> rows = query.list();

        List<ProjectReportModel.ReportActionLine> lines = new ArrayList<ProjectReportModel.ReportActionLine>();
        for (ProjectActionTaken row : rows) {
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
                "from ProjectActionNext pan where pan.projectId = :projectId and pan.nextActionStatusString = :status and pan.nextDescription <> '' and pan.nextActionDate is not null order by pan.nextActionDate, pan.priorityLevel desc");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        query.setMaxResults(20);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> rows = query.list();

        List<ProjectReportModel.ReportActionLine> lines = new ArrayList<ProjectReportModel.ReportActionLine>();
        for (ProjectActionNext row : rows) {
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
                "from ProjectActionNext pan where pan.projectId = :projectId and pan.nextActionStatusString = :status and pan.nextDescription <> '' and pan.nextActionDate is null order by pan.priorityLevel desc, pan.nextChangeDate");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        query.setMaxResults(20);
        @SuppressWarnings("unchecked")
        List<ProjectActionNext> rows = query.list();

        List<ProjectReportModel.ReportActionLine> lines = new ArrayList<ProjectReportModel.ReportActionLine>();
        for (ProjectActionNext row : rows) {
            ProjectReportModel.ReportActionLine line = new ProjectReportModel.ReportActionLine();
            line.setActionId(row.getActionNextId());
            line.setDescription(n(row.getNextDescription()));
            line.setWhenLabel("undated");
            lines.add(line);
        }
        return lines;
    }

    private int countOpenUndated(Session dataSession, Project project) {
        Query query = dataSession.createQuery(
                "select count(*) from ProjectActionNext pan where pan.projectId = :projectId and pan.nextActionStatusString = :status and pan.nextDescription <> '' and pan.nextActionDate is null");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        Number result = (Number) query.uniqueResult();
        return result == null ? 0 : result.intValue();
    }

    private int countOpenOverdue(Session dataSession, Project project, Date today) {
        Query query = dataSession.createQuery(
                "select count(*) from ProjectActionNext pan where pan.projectId = :projectId and pan.nextActionStatusString = :status and pan.nextDescription <> '' and pan.nextActionDate is not null and pan.nextActionDate < :today");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("status", ProjectNextActionStatus.READY.getId());
        query.setParameter("today", today);
        Number result = (Number) query.uniqueResult();
        return result == null ? 0 : result.intValue();
    }

    private Date loadLastReview(Session dataSession, WebUser webUser, Project project) {
        Query query = dataSession.createQuery(
                "select max(actionDate) from ProjectActionTaken where projectId = :projectId and contactId = :contactId");
        query.setParameter("projectId", project.getProjectId());
        query.setParameter("contactId", webUser.getContactId());
        return (Date) query.uniqueResult();
    }

    private String buildReportText(ProjectReportModel report) {
        StringBuilder text = new StringBuilder();
        text.append("Project Briefing\n");
        text.append("Project: ").append(n(report.getProjectName())).append("\n");
        text.append("Category: ").append(n(report.getCategory(), "(unspecified)")).append("\n");
        text.append("Phase: ").append(n(report.getPhase(), "(unspecified)")).append("\n");
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

    private boolean isPersonalProject(Project project, WebUser webUser) {
        String categoryCode = project.getCategoryCode();
        if (categoryCode == null) {
            return false;
        }
        return categoryCode.equals("PER-" + webUser.getContactId());
    }

    private Date stripTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
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
        for (Interval interval : Interval.values()) {
            if (days <= interval.getDays()) {
                return interval.getDescription();
            }
        }
        return Integer.toString(days) + " days";
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
