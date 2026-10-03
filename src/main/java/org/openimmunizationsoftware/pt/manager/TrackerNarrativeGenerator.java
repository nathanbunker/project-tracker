package org.openimmunizationsoftware.pt.manager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.dandeliondaily.outlook.service.PlanningOutlookService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.CentralControl;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectIssue;
import org.openimmunizationsoftware.pt.model.ProjectIssueStatus;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;
import org.openimmunizationsoftware.pt.model.TrackerNarrativeReviewStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.doa.PlanningOutlookDao;
import org.openimmunizationsoftware.pt.doa.TrackerNarrativeDao;
import org.openimmunizationsoftware.pt.model.PlanningOutlook;

public class TrackerNarrativeGenerator {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "TrackerNarrativeGenerator");
            thread.setDaemon(true);
            return thread;
        }
    });

    public static void enqueue(long trackerNarrativeId) {
        if (trackerNarrativeId <= 0) {
            return;
        }
        EXECUTOR.submit(new GenerateTask(trackerNarrativeId));
    }

    public static boolean isGenerationAvailable() {
        return OpenAiNarrativeGenerator.isConfigured();
    }

    public static String getGenerationUnavailableMessage() {
        return OpenAiNarrativeGenerator.getMissingConfigurationMessage();
    }

    private static class GenerateTask implements Runnable {

        private final long trackerNarrativeId;

        private GenerateTask(long trackerNarrativeId) {
            this.trackerNarrativeId = trackerNarrativeId;
        }

        @Override
        public void run() {
            if (!isGenerationAvailable()) {
                System.err.println("[TrackerNarrativeGenerator] Narrative " + trackerNarrativeId + ": "
                        + getGenerationUnavailableMessage());
                markGenerationFailed(trackerNarrativeId);
                return;
            }
            Session session = null;
            Transaction transaction = null;
            Exception generationFailure = null;
            try {
                SessionFactory factory = CentralControl.getSessionFactory();
                session = factory.openSession();
                TrackerNarrative narrative = (TrackerNarrative) session.get(TrackerNarrative.class,
                        (int) trackerNarrativeId);
                if (narrative == null) {
                    return;
                }

                WebUser owner = loadOwner(session, narrative.getContactId());
                if (owner == null) {
                    throw new IllegalStateException("Narrative owner was not found");
                }
                Project narrativeProject = (Project) session.get(Project.class, narrative.getProjectId());
                Integer workspaceId = narrativeProject == null ? null : narrativeProject.getWorkspaceId();

                LocalDate periodStart = owner.toLocalDate(narrative.getPeriodStart());
                LocalDate periodEnd = owner.toLocalDate(narrative.getPeriodEnd());
                if (periodStart == null || periodEnd == null) {
                    throw new IllegalStateException("Narrative period is incomplete");
                }

                LocalDate endExclusive = periodEnd.plusDays(1);
                Date startDate = owner.toDate(periodStart);
                Date endDate = owner.toDate(endExclusive);

                List<ActionTaken> completedActions = loadCompletedActions(session, narrative.getContactId(),
                        workspaceId, startDate, endDate);
                Map<Integer, Integer> timeByProject = loadMinutesByProject(session, owner.getWebUserId(), workspaceId,
                        startDate, endDate);
                Map<Integer, String> projectNames = loadProjectNames(session, timeByProject.keySet(), completedActions);
                List<ProjectNarrative> projectNarratives = loadProjectNarratives(session, narrative.getContactId(),
                        workspaceId, startDate, endDate);
                List<ActionNext> waitingActions = loadWaitingActions(session, narrative.getContactId(), workspaceId);
                List<ActionNext> completedActionDetails = loadCompletedActionDetails(session, narrative.getContactId(),
                        workspaceId, startDate, endDate);
                List<ActionNext> upcomingActions = loadUpcomingActions(session, narrative.getContactId(), workspaceId,
                        owner.toDate(endExclusive.plusWeeks(1)));
                List<TrackerNarrative> approvedDailyNarratives = "WEEKLY".equals(narrative.getNarrativeType())
                        ? new TrackerNarrativeDao(session).findApprovedByContactTypeAndPeriodRange(
                                narrative.getContactId(), "DAILY", periodStart, periodEnd)
                        : new ArrayList<TrackerNarrative>();
                Set<Integer> projectIds = collectProjectIds(timeByProject, completedActions, projectNarratives,
                        waitingActions);
                addActionProjectIds(projectIds, completedActionDetails);
                addActionProjectIds(projectIds, upcomingActions);
                Map<Integer, Project> projectsById = loadProjectsById(session, projectIds);
                for (Map.Entry<Integer, Project> entry : projectsById.entrySet()) {
                    projectNames.put(entry.getKey(), entry.getValue().getProjectName());
                }
                Map<Integer, List<String>> openIssuesByProject = loadOpenIssuesByProject(session, projectIds);

                boolean weekly = "WEEKLY".equals(narrative.getNarrativeType());
                PlanningOutlookService outlookService = new PlanningOutlookService();
                LocalDate weekStart = outlookService.periodStartFor(PlanningOutlookService.PERIOD_TYPE_WEEK,
                        periodStart);
                String weekOutlook = weekly ? loadOutlookText(session, owner.getWebUserId(),
                        PlanningOutlookService.PERIOD_TYPE_WEEK, weekStart) : null;
                String nextWeekOutlook = weekly ? loadOutlookText(session, owner.getWebUserId(),
                        PlanningOutlookService.PERIOD_TYPE_WEEK, weekStart.plusWeeks(1)) : null;
                Map<String, String> monthOutlooks = weekly
                        ? loadMonthOutlooks(session, owner.getWebUserId(), outlookService, periodStart, periodEnd)
                        : new LinkedHashMap<String, String>();

                GenerationContext context = new GenerationContext(periodStart, periodEnd, "", completedActions,
                        timeByProject, projectNames, projectsById, openIssuesByProject, projectNarratives,
                        waitingActions, completedActionDetails, upcomingActions, approvedDailyNarratives,
                        owner.getZoneId().getId(), weekOutlook, nextWeekOutlook, monthOutlooks);
                String promptUsedText = OpenAiNarrativeGenerator.buildPromptForInspection(
                        narrative.getNarrativeType(), context);
                String markdownGenerated = createGenerator().generateMarkdown(narrative.getNarrativeType(), context);

                transaction = session.beginTransaction();
                TrackerNarrative refresh = (TrackerNarrative) session.get(TrackerNarrative.class,
                        (int) trackerNarrativeId);
                if (refresh == null) {
                    return;
                }
                refresh.setMarkdownGenerated(markdownGenerated);
                refresh.setDateGenerated(new Date());
                refresh.setReviewStatus(TrackerNarrativeReviewStatus.GENERATED);
                refresh.setLastUpdated(new Date());
                refresh.setPromptUsedText(promptUsedText);
                refresh.setPromptVersion(OpenAiNarrativeGenerator.promptVersionFor(narrative.getNarrativeType()));
                refresh.setModelName(OpenAiNarrativeGenerator.MODEL_NAME);
                if (isEmpty(refresh.getMarkdownFinal())) {
                    refresh.setMarkdownFinal(markdownGenerated);
                }
                session.update(refresh);
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                generationFailure = e;
            } finally {
                if (session != null) {
                    session.close();
                }
            }
            if (generationFailure != null) {
                System.err.println("[TrackerNarrativeGenerator] Generation failed for narrative "
                        + trackerNarrativeId + ": " + generationFailure.getMessage());
                generationFailure.printStackTrace();
                markGenerationFailed(trackerNarrativeId);
            }
        }
    }

    private static void markGenerationFailed(long trackerNarrativeId) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = CentralControl.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            TrackerNarrative narrative = (TrackerNarrative) session.get(TrackerNarrative.class,
                    (int) trackerNarrativeId);
            if (narrative != null
                    && TrackerNarrativeReviewStatus.GENERATING.equals(narrative.getReviewStatus())) {
                narrative.setReviewStatus(TrackerNarrativeReviewStatus.FAILED);
                narrative.setLastUpdated(new Date());
                session.update(narrative);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            System.err.println("[TrackerNarrativeGenerator] Could not mark narrative " + trackerNarrativeId
                    + " as failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private static String loadOutlookText(Session session, int ownerUserId, String periodType,
            LocalDate periodStart) {
        PlanningOutlook outlook = new PlanningOutlookDao(session).findForPeriod(ownerUserId, periodType,
                periodStart);
        return outlook == null || isEmpty(outlook.getOutlookText()) ? null : outlook.getOutlookText().trim();
    }

    /** Outlooks for the month(s) the reported week falls in, keyed by "October 2026". */
    private static Map<String, String> loadMonthOutlooks(Session session, int ownerUserId,
            PlanningOutlookService outlookService, LocalDate periodStart, LocalDate periodEnd) {
        Map<String, String> monthOutlooks = new LinkedHashMap<String, String>();
        DateTimeFormatter label = DateTimeFormatter.ofPattern("MMMM uuuu");
        String month = PlanningOutlookService.PERIOD_TYPE_MONTH;
        LocalDate firstMonth = outlookService.periodStartFor(month, periodStart);
        LocalDate lastMonth = outlookService.periodStartFor(month, periodEnd);
        for (LocalDate m = firstMonth; !m.isAfter(lastMonth); m = m.plusMonths(1)) {
            String text = loadOutlookText(session, ownerUserId, month, m);
            if (text != null) {
                monthOutlooks.put(label.format(m), text);
            }
        }
        return monthOutlooks;
    }

    private static NarrativeGenerator createGenerator() {
        return new OpenAiNarrativeGenerator();
    }

    @SuppressWarnings("unchecked")
    private static WebUser loadOwner(Session session, int contactId) {
        Query query = session.createQuery("from WebUser where contactId = :contactId order by webUserId");
        query.setInteger("contactId", contactId);
        query.setMaxResults(1);
        List<WebUser> users = query.list();
        return users.isEmpty() ? null : users.get(0);
    }

    @SuppressWarnings("unchecked")
    private static List<ActionTaken> loadCompletedActions(Session session, int contactId, Integer workspaceId,
            Date startDate, Date endDate) {
        Query query = session.createQuery(
                "from ActionTaken atk left join fetch atk.project "
                        + "where atk.actionDate >= :start and atk.actionDate < :end "
                        + "and atk.contactId = :contactId "
                        + (workspaceId == null ? "" : "and atk.workspaceId = :workspaceId ")
                        + "and atk.actionDescription is not null and atk.actionDescription <> '' "
                        + "order by atk.actionDate asc");
        query.setInteger("contactId", contactId);
        if (workspaceId != null)
            query.setInteger("workspaceId", workspaceId.intValue());
        query.setTimestamp("start", startDate);
        query.setTimestamp("end", endDate);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Integer> loadMinutesByProject(Session session, int webUserId, Integer workspaceId,
            Date startDate, Date endDate) {
        Query query = session.createQuery(
                "select be.projectId, sum(be.billMins) from BillEntry be "
                        + "where be.webUser.webUserId = :webUserId and be.billable = 'Y' and be.billMins > 0 "
                        + (workspaceId == null ? "" : "and be.workspaceId = :workspaceId ")
                        + "and be.startTime >= :start and be.startTime < :end group by be.projectId");
        query.setInteger("webUserId", webUserId);
        if (workspaceId != null)
            query.setInteger("workspaceId", workspaceId.intValue());
        query.setTimestamp("start", startDate);
        query.setTimestamp("end", endDate);
        List<Object[]> rows = query.list();
        Map<Integer, Integer> results = new LinkedHashMap<Integer, Integer>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }
            Number projectId = (Number) row[0];
            Number minutes = (Number) row[1];
            if (projectId == null) {
                continue;
            }
            results.put(projectId.intValue(), minutes == null ? 0 : TimeEntry.adjustMinutes(minutes.intValue()));
        }
        return results;
    }

    private static Map<Integer, String> loadProjectNames(Session session, Iterable<Integer> projectIds,
            List<ActionTaken> completedActions) {
        Map<Integer, String> names = new LinkedHashMap<Integer, String>();
        for (ActionTaken action : completedActions) {
            Project project = action.getProject();
            if (project != null) {
                names.put(project.getProjectId(), project.getProjectName());
            }
        }
        for (Integer projectId : projectIds) {
            if (!names.containsKey(projectId)) {
                Project project = (Project) session.get(Project.class, projectId);
                if (project != null) {
                    names.put(projectId, project.getProjectName());
                }
            }
        }
        return names;
    }

    private static Map<Integer, Project> loadProjectsById(Session session, Set<Integer> projectIds) {
        Map<Integer, Project> projectsById = new LinkedHashMap<Integer, Project>();
        for (Integer projectId : projectIds) {
            if (projectId == null || projectId.intValue() <= 0 || projectsById.containsKey(projectId)) {
                continue;
            }
            Project project = (Project) session.get(Project.class, projectId);
            if (project != null) {
                projectsById.put(projectId, project);
            }
        }
        return projectsById;
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, List<String>> loadOpenIssuesByProject(Session session, Set<Integer> projectIds) {
        Map<Integer, List<String>> openIssuesByProject = new LinkedHashMap<Integer, List<String>>();
        if (projectIds == null || projectIds.isEmpty()) {
            return openIssuesByProject;
        }

        Query query = session.createQuery(
                "from ProjectIssue where issueStatusString = :status and project.projectId in (:projectIds) "
                        + "order by project.projectId asc, createdDate asc");
        query.setString("status", ProjectIssueStatus.OPEN.name());
        query.setParameterList("projectIds", projectIds);
        List<ProjectIssue> issues = query.list();

        for (ProjectIssue issue : issues) {
            if (issue == null || issue.getProject() == null || issue.getProject().getProjectId() <= 0
                    || isEmpty(issue.getIssueText())) {
                continue;
            }
            int projectId = issue.getProject().getProjectId();
            List<String> lines = openIssuesByProject.get(projectId);
            if (lines == null) {
                lines = new ArrayList<String>();
                openIssuesByProject.put(projectId, lines);
            }
            lines.add(issue.getIssueText().trim());
        }

        return openIssuesByProject;
    }

    @SuppressWarnings("unchecked")
    private static List<ProjectNarrative> loadProjectNarratives(Session session, int contactId, Integer workspaceId,
            Date startDate, Date endDate) {
        Query query = session.createQuery(
                "from ProjectNarrative pn left join fetch pn.project "
                        + "where pn.narrativeDate >= :start and pn.narrativeDate < :end "
                        + "and pn.contactId = :contactId "
                        + (workspaceId == null ? "" : "and pn.workspaceId = :workspaceId ")
                        + "order by pn.projectId, pn.narrativeVerbString, pn.narrativeDate asc");
        query.setInteger("contactId", contactId);
        if (workspaceId != null)
            query.setInteger("workspaceId", workspaceId.intValue());
        query.setTimestamp("start", startDate);
        query.setTimestamp("end", endDate);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    private static List<ActionNext> loadWaitingActions(Session session, int contactId, Integer workspaceId) {
        Query query = session.createQuery(
                "from ActionNext an left join fetch an.project "
                        + "where an.nextActionType = :waiting and an.nextDescription <> '' "
                        + "and an.contactId = :contactId "
                        + (workspaceId == null ? "" : "and an.workspaceId = :workspaceId ")
                        + "and an.nextActionStatusString in (:ready, :proposed) "
                        + "order by an.nextChangeDate asc");
        query.setString("waiting", ProjectNextActionType.WAITING);
        query.setInteger("contactId", contactId);
        query.setString("ready", ProjectNextActionStatus.READY.getId());
        query.setString("proposed", ProjectNextActionStatus.PROPOSED.getId());
        if (workspaceId != null)
            query.setInteger("workspaceId", workspaceId.intValue());
        return query.list();
    }

    @SuppressWarnings("unchecked")
    private static List<ActionNext> loadCompletedActionDetails(Session session, int contactId, Integer workspaceId,
            Date startDate, Date endDate) {
        Query query = session.createQuery(
                "from ActionNext an left join fetch an.project where an.contactId = :contactId "
                        + (workspaceId == null ? "" : "and an.workspaceId = :workspaceId ")
                        + "and an.nextActionStatusString = :completed and an.nextChangeDate >= :start "
                        + "and an.nextChangeDate < :end and an.nextSummary is not null and an.nextSummary <> '' "
                        + "order by an.nextChangeDate asc");
        query.setInteger("contactId", contactId);
        query.setString("completed", ProjectNextActionStatus.COMPLETED.getId());
        query.setTimestamp("start", startDate);
        query.setTimestamp("end", endDate);
        if (workspaceId != null)
            query.setInteger("workspaceId", workspaceId.intValue());
        return query.list();
    }

    @SuppressWarnings("unchecked")
    private static List<ActionNext> loadUpcomingActions(Session session, int contactId, Integer workspaceId,
            Date nextWeekEnd) {
        Query query = session.createQuery(
                "from ActionNext an left join fetch an.project where an.contactId = :contactId "
                        + (workspaceId == null ? "" : "and an.workspaceId = :workspaceId ")
                        + "and an.nextActionStatusString in (:ready, :proposed) and an.nextDescription <> '' "
                        + "and ((an.nextActionDate is not null and an.nextActionDate < :nextWeekEnd) "
                        + "or (an.nextTargetDate is not null and an.nextTargetDate < :nextWeekEnd) "
                        + "or (an.nextDeadlineDate is not null and an.nextDeadlineDate < :nextWeekEnd)) "
                        + "order by an.nextActionDate asc, an.priorityLevel desc");
        query.setInteger("contactId", contactId);
        query.setString("ready", ProjectNextActionStatus.READY.getId());
        query.setString("proposed", ProjectNextActionStatus.PROPOSED.getId());
        query.setDate("nextWeekEnd", nextWeekEnd);
        if (workspaceId != null)
            query.setInteger("workspaceId", workspaceId.intValue());
        query.setMaxResults(30);
        return query.list();
    }

    private static void addActionProjectIds(Set<Integer> projectIds, List<ActionNext> actions) {
        for (ActionNext action : actions) {
            if (action != null && action.getProjectId() > 0) {
                projectIds.add(Integer.valueOf(action.getProjectId()));
            }
        }
    }

    private static Set<Integer> collectProjectIds(Map<Integer, Integer> timeByProject,
            List<ActionTaken> completedActions, List<ProjectNarrative> projectNarratives,
            List<ActionNext> waitingActions) {
        Set<Integer> projectIds = new HashSet<Integer>();
        projectIds.addAll(timeByProject.keySet());

        for (ActionTaken action : completedActions) {
            if (action != null && action.getProject() != null) {
                projectIds.add(action.getProject().getProjectId());
            }
        }
        for (ProjectNarrative narrative : projectNarratives) {
            if (narrative != null && narrative.getProjectId() > 0) {
                projectIds.add(narrative.getProjectId());
            }
        }
        for (ActionNext action : waitingActions) {
            if (action != null && action.getProject() != null) {
                projectIds.add(action.getProject().getProjectId());
            }
        }

        return projectIds;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
