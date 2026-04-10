package org.openimmunizationsoftware.pt.manager;

import java.time.LocalDate;
import java.time.ZoneId;
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
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;
import org.openimmunizationsoftware.pt.model.TrackerNarrativeReviewStatus;

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
                System.out.println("[TrackerNarrativeGenerator] " + getGenerationUnavailableMessage());
                return;
            }
            SessionFactory factory = CentralControl.getSessionFactory();
            Session session = factory.openSession();
            Transaction transaction = null;
            try {
                TrackerNarrative narrative = (TrackerNarrative) session.get(TrackerNarrative.class,
                        (int) trackerNarrativeId);
                if (narrative == null) {
                    return;
                }

                LocalDate periodStart = toLocalDate(narrative.getPeriodStart());
                LocalDate periodEnd = toLocalDate(narrative.getPeriodEnd());
                if (periodStart == null || periodEnd == null) {
                    return;
                }

                LocalDate endExclusive = periodEnd.plusDays(1);
                Date startDate = toDate(periodStart);
                Date endDate = toDate(endExclusive);

                List<ActionTaken> completedActions = loadCompletedActions(session, startDate, endDate);
                Map<Integer, Integer> timeByProject = loadMinutesByProject(session, startDate, endDate);
                Map<Integer, String> projectNames = loadProjectNames(session, timeByProject.keySet(), completedActions);
                List<ProjectNarrative> projectNarratives = loadProjectNarratives(session, startDate, endDate);
                List<ActionNext> waitingActions = loadWaitingActions(session, startDate, endDate);
                Set<Integer> projectIds = collectProjectIds(timeByProject, completedActions, projectNarratives,
                        waitingActions);
                Map<Integer, Project> projectsById = loadProjectsById(session, projectIds);
                Map<Integer, List<String>> openIssuesByProject = loadOpenIssuesByProject(session, projectIds);

                String prompt = buildPrompt(narrative, periodStart, periodEnd, completedActions, timeByProject,
                        projectNames, projectsById, openIssuesByProject, projectNarratives, waitingActions);
                GenerationContext context = new GenerationContext(periodStart, periodEnd, prompt, completedActions,
                        timeByProject, projectNames, projectsById, openIssuesByProject, projectNarratives,
                        waitingActions);
                String promptUsedText = OpenAiNarrativeGenerator.buildPromptForInspection(context);
                String markdownGenerated = createGenerator().generateDailyMarkdown(context);

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
                if (isEmpty(refresh.getMarkdownFinal())) {
                    refresh.setMarkdownFinal(markdownGenerated);
                }
                session.update(refresh);
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
                e.printStackTrace();
            } finally {
                session.close();
            }
        }
    }

    private static NarrativeGenerator createGenerator() {
        return new OpenAiNarrativeGenerator();
    }

    @SuppressWarnings("unchecked")
    private static List<ActionTaken> loadCompletedActions(Session session, Date startDate, Date endDate) {
        Query query = session.createQuery(
                "from ActionTaken atk left join fetch atk.project "
                        + "where atk.actionDate >= :start and atk.actionDate < :end "
                        + "and atk.actionDescription is not null and atk.actionDescription <> '' "
                        + "order by atk.actionDate asc");
        query.setTimestamp("start", startDate);
        query.setTimestamp("end", endDate);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Integer> loadMinutesByProject(Session session, Date startDate, Date endDate) {
        Query query = session.createQuery(
                "select be.projectId, sum(be.billMins) from BillEntry be "
                        + "where be.startTime >= :start and be.startTime < :end group by be.projectId");
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
            results.put(projectId.intValue(), minutes == null ? 0 : minutes.intValue());
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
    private static List<ProjectNarrative> loadProjectNarratives(Session session, Date startDate, Date endDate) {
        Query query = session.createQuery(
                "from ProjectNarrative pn left join fetch pn.project "
                        + "where pn.narrativeDate >= :start and pn.narrativeDate < :end "
                        + "order by pn.projectId, pn.narrativeVerbString, pn.narrativeDate asc");
        query.setTimestamp("start", startDate);
        query.setTimestamp("end", endDate);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    private static List<ActionNext> loadWaitingActions(Session session, Date startDate, Date endDate) {
        Query query = session.createQuery(
                "from ActionNext an left join fetch an.project "
                        + "where an.nextActionType = :waiting and an.nextDescription <> '' "
                        + "and an.nextChangeDate >= :start and an.nextChangeDate < :end "
                        + "order by an.nextChangeDate asc");
        query.setString("waiting", ProjectNextActionType.WAITING);
        query.setTimestamp("start", startDate);
        query.setTimestamp("end", endDate);
        return query.list();
    }

    private static String buildPrompt(TrackerNarrative narrative, LocalDate periodStart, LocalDate periodEnd,
            List<ActionTaken> completedActions, Map<Integer, Integer> timeByProject,
            Map<Integer, String> projectNames, Map<Integer, Project> projectsById,
            Map<Integer, List<String>> openIssuesByProject, List<ProjectNarrative> projectNarratives,
            List<ActionNext> waitingActions) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are writing a tracker narrative for period ")
                .append(periodStart).append(" to ").append(periodEnd).append(".\n");
        sb.append("Output Markdown with headings and bullets only. ")
                .append("No tables, no code blocks, no numbered lists.\n\n");

        sb.append("# Summary\n");
        sb.append("- Provide a concise overview of the period.\n\n");

        sb.append("# Time By Project\n");
        if (timeByProject.isEmpty()) {
            sb.append("- No time tracked.\n\n");
        } else {
            for (Map.Entry<Integer, Integer> entry : timeByProject.entrySet()) {
                String name = projectNames.get(entry.getKey());
                sb.append("- ").append(name == null ? "Project " + entry.getKey() : name)
                        .append(": ").append(TimeTracker.formatTime(entry.getValue())).append("\n");
            }
            sb.append("\n");
        }

        sb.append("# Completed Actions\n");
        if (completedActions.isEmpty()) {
            sb.append("- No completed actions recorded.\n\n");
        } else {
            for (ActionTaken action : completedActions) {
                String projectName = action.getProject() == null ? "" : action.getProject().getProjectName();
                sb.append("- ");
                if (!isEmpty(projectName)) {
                    sb.append(projectName).append(": ");
                }
                sb.append(action.getActionDescription()).append("\n");
            }
            sb.append("\n");
        }

        appendProjectContextSection(sb, timeByProject, projectNames, projectsById, openIssuesByProject);

        appendNarrativeSection(sb, "Notes", ProjectNarrativeVerb.NOTE, projectNarratives);
        appendNarrativeSection(sb, "Decisions", ProjectNarrativeVerb.DECISION, projectNarratives);
        appendNarrativeSection(sb, "Insights", ProjectNarrativeVerb.INSIGHT, projectNarratives);
        appendNarrativeSection(sb, "Risks", ProjectNarrativeVerb.RISK, projectNarratives);
        appendNarrativeSection(sb, "Opportunities", ProjectNarrativeVerb.OPPORTUNITY, projectNarratives);

        sb.append("# Waiting / Blocked\n");
        if (waitingActions.isEmpty()) {
            sb.append("- No waiting items recorded.\n");
        } else {
            for (ActionNext action : waitingActions) {
                String projectName = action.getProject() == null ? "" : action.getProject().getProjectName();
                sb.append("- ");
                if (!isEmpty(projectName)) {
                    sb.append(projectName).append(": ");
                }
                sb.append(action.getNextDescription()).append("\n");
            }
        }

        return sb.toString();
    }

    private static void appendProjectContextSection(StringBuilder sb, Map<Integer, Integer> timeByProject,
            Map<Integer, String> projectNames, Map<Integer, Project> projectsById,
            Map<Integer, List<String>> openIssuesByProject) {
        sb.append("# Project Context\n");
        boolean addedAny = false;

        for (Integer projectId : projectNames.keySet()) {
            Project project = projectsById.get(projectId);
            List<String> openIssues = openIssuesByProject.get(projectId);

            String description = project == null ? null : project.getDescription();
            String outcome = project == null ? null : project.getOutcomeText();
            String successCriteria = project == null ? null : project.getSuccessCriteriaText();

            boolean hasDescription = !isEmpty(description);
            boolean hasOutcome = !isEmpty(outcome);
            boolean hasSuccess = !splitNonEmptyLines(successCriteria).isEmpty();
            boolean hasOpenIssues = openIssues != null && !openIssues.isEmpty();

            if (!hasDescription && !hasOutcome && !hasSuccess && !hasOpenIssues) {
                continue;
            }

            addedAny = true;
            String projectName = projectNames.get(projectId);
            sb.append("## ").append(isEmpty(projectName) ? "Project " + projectId : projectName).append("\n");

            if (hasDescription) {
                sb.append("### Project Description\n");
                sb.append(description.trim()).append("\n\n");
            }
            if (hasOutcome) {
                sb.append("### Project Outcome\n");
                sb.append(outcome.trim()).append("\n\n");
            }
            if (hasSuccess) {
                sb.append("### Project Success Criteria\n");
                for (String line : splitNonEmptyLines(successCriteria)) {
                    sb.append("- ").append(line).append("\n");
                }
                sb.append("\n");
            }
            if (hasOpenIssues) {
                sb.append("### Open Issues\n");
                for (String issue : openIssues) {
                    if (isEmpty(issue)) {
                        continue;
                    }
                    sb.append("- ").append(issue.trim()).append("\n");
                }
                sb.append("\n");
            }
        }

        if (!addedAny) {
            sb.append("- None recorded.\n\n");
        }
    }

    private static List<String> splitNonEmptyLines(String value) {
        List<String> lines = new ArrayList<String>();
        if (isEmpty(value)) {
            return lines;
        }
        String[] parts = value.split("\\r?\\n");
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (trimmed.length() > 0) {
                lines.add(trimmed);
            }
        }
        return lines;
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

    private static void appendNarrativeSection(StringBuilder sb, String title, ProjectNarrativeVerb verb,
            List<ProjectNarrative> narratives) {
        sb.append("# ").append(title).append("\n");
        boolean added = false;
        for (ProjectNarrative narrative : narratives) {
            if (narrative.getNarrativeVerb() != verb) {
                continue;
            }
            String projectName = narrative.getProject() == null ? "" : narrative.getProject().getProjectName();
            sb.append("- ");
            if (!isEmpty(projectName)) {
                sb.append(projectName).append(": ");
            }
            sb.append(narrative.getNarrativeText()).append("\n");
            added = true;
        }
        if (!added) {
            sb.append("- None recorded.\n");
        }
        sb.append("\n");
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date toDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
