package org.openimmunizationsoftware.pt.manager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.CentralControl;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;
import org.openimmunizationsoftware.pt.model.TrackerNarrativeReviewStatus;

public class TrackerNarrativeGenerator {

    private static final NarrativeGenerator GENERATOR = new OpenAiNarrativeGenerator();

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

    private static class GenerateTask implements Runnable {

        private final long trackerNarrativeId;

        private GenerateTask(long trackerNarrativeId) {
            this.trackerNarrativeId = trackerNarrativeId;
        }

        @Override
        public void run() {
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

                List<ProjectActionTaken> completedActions = loadCompletedActions(session, startDate, endDate);
                Map<Integer, Integer> timeByProject = loadMinutesByProject(session, startDate, endDate);
                Map<Integer, String> projectNames = loadProjectNames(session, timeByProject.keySet(), completedActions);
                List<ProjectNarrative> projectNarratives = loadProjectNarratives(session, startDate, endDate);
                List<ProjectActionNext> waitingActions = loadWaitingActions(session, startDate, endDate);

                String prompt = buildPrompt(narrative, periodStart, periodEnd, completedActions, timeByProject,
                        projectNames, projectNarratives, waitingActions);
                GenerationContext context = new GenerationContext(periodStart, periodEnd, prompt, completedActions,
                        timeByProject, projectNames, projectNarratives, waitingActions);
                String markdownGenerated = GENERATOR.generateDailyMarkdown(context);

                transaction = session.beginTransaction();
                TrackerNarrative refresh = (TrackerNarrative) session.get(TrackerNarrative.class,
                        (int) trackerNarrativeId);
                if (refresh == null) {
                    return;
                }
                refresh.setMarkdownGenerated(markdownGenerated);
                refresh.setDateGenerated(new Date());
                refresh.setReviewStatus(TrackerNarrativeReviewStatus.GENERATED);
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

    @SuppressWarnings("unchecked")
    private static List<ProjectActionTaken> loadCompletedActions(Session session, Date startDate, Date endDate) {
        Query query = session.createQuery(
                "from ProjectActionTaken pat left join fetch pat.project "
                        + "where pat.actionDate >= :start and pat.actionDate < :end "
                        + "and pat.actionDescription is not null and pat.actionDescription <> '' "
                        + "order by pat.actionDate asc");
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
            List<ProjectActionTaken> completedActions) {
        Map<Integer, String> names = new LinkedHashMap<Integer, String>();
        for (ProjectActionTaken action : completedActions) {
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
    private static List<ProjectActionNext> loadWaitingActions(Session session, Date startDate, Date endDate) {
        Query query = session.createQuery(
                "from ProjectActionNext pan left join fetch pan.project "
                        + "where pan.nextActionType = :waiting and pan.nextDescription <> '' "
                        + "and pan.nextChangeDate >= :start and pan.nextChangeDate < :end "
                        + "order by pan.nextChangeDate asc");
        query.setString("waiting", ProjectNextActionType.WAITING);
        query.setTimestamp("start", startDate);
        query.setTimestamp("end", endDate);
        return query.list();
    }

    private static String buildPrompt(TrackerNarrative narrative, LocalDate periodStart, LocalDate periodEnd,
            List<ProjectActionTaken> completedActions, Map<Integer, Integer> timeByProject,
            Map<Integer, String> projectNames, List<ProjectNarrative> projectNarratives,
            List<ProjectActionNext> waitingActions) {
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
            for (ProjectActionTaken action : completedActions) {
                String projectName = action.getProject() == null ? "" : action.getProject().getProjectName();
                sb.append("- ");
                if (!isEmpty(projectName)) {
                    sb.append(projectName).append(": ");
                }
                sb.append(action.getActionDescription()).append("\n");
            }
            sb.append("\n");
        }

        appendNarrativeSection(sb, "Notes", ProjectNarrativeVerb.NOTE, projectNarratives);
        appendNarrativeSection(sb, "Decisions", ProjectNarrativeVerb.DECISION, projectNarratives);
        appendNarrativeSection(sb, "Insights", ProjectNarrativeVerb.INSIGHT, projectNarratives);
        appendNarrativeSection(sb, "Risks", ProjectNarrativeVerb.RISK, projectNarratives);
        appendNarrativeSection(sb, "Opportunities", ProjectNarrativeVerb.OPPORTUNITY, projectNarratives);

        sb.append("# Waiting / Blocked\n");
        if (waitingActions.isEmpty()) {
            sb.append("- No waiting items recorded.\n");
        } else {
            for (ProjectActionNext action : waitingActions) {
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
