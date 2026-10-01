package org.dandeliondaily.projectnarrative.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.dashboard.service.ProjectDisplayLabelService;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.ProjectNarrativeDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.dandeliondaily.projectnarrative.model.ProjectNarrativeEntry;
import org.dandeliondaily.projectnarrative.model.ProjectNarrativeSummary;

/**
 * Saves and summarizes ProjectNarrative rows. Used by the dashboard's
 * quick-review widgets (AppReq-based methods below) and, per
 * docs/MCP-Feedback.md ("the UI and the MCP should call the same service, so
 * behavior can't drift"), by the MCP work-day-review and narrative-CRUD tools
 * (the Session-based methods) and by ProjectNarrativeReviewServlet.
 */
public class ProjectNarrativeService {

    public static final String DEFAULT_NOTE_TEXT = "Reviewed/no comments";
    private final ProjectDisplayLabelService projectDisplayLabelService = new ProjectDisplayLabelService();

    public List<ProjectNarrativeSummary> listNarrativeSummariesForCompletedProjects(WebUser webUser,
            Session dataSession, LocalDate reviewDate, List<Integer> completedActionProjectIds) {
        List<ProjectNarrativeSummary> summaries = new ArrayList<ProjectNarrativeSummary>();
        if (completedActionProjectIds == null || completedActionProjectIds.isEmpty()) {
            return summaries;
        }

        ProjectNarrativeDao narrativeDao = new ProjectNarrativeDao(dataSession);

        Map<Long, Integer> completedCountByProject = new HashMap<Long, Integer>();
        for (Integer projectIdValue : completedActionProjectIds) {
            if (projectIdValue == null || projectIdValue.intValue() <= 0) {
                continue;
            }
            long projectId = projectIdValue.longValue();
            Integer existing = completedCountByProject.get(projectId);
            completedCountByProject.put(projectId, existing == null ? 1 : existing + 1);
        }

        Map<Long, Integer> minutesByProject = narrativeDao.getMinutesSpentByProjectOnDate(reviewDate);

        for (Map.Entry<Long, Integer> entry : completedCountByProject.entrySet()) {
            long projectId = entry.getKey().longValue();
            Project project = (Project) dataSession.get(Project.class, (int) projectId);
            if (project == null) {
                continue;
            }

            ProjectNarrativeSummary summary = new ProjectNarrativeSummary();
            summary.setProjectId(projectId);
            summary.setProjectName(s(projectDisplayLabelService.buildDisplayName(dataSession, project)));
            summary.setCompletedCount(entry.getValue().intValue());
            Integer minutesValue = minutesByProject.get(projectId);
            summary.setMinutesSpent(minutesValue == null ? 0 : Math.max(0, minutesValue.intValue()));
            summary.setReviewed(narrativeDao.hasNarrativeForProjectOnDate(projectId, reviewDate));
            summary.setNarrativeEntry(loadNarrativeEntry(narrativeDao, projectId, reviewDate));
            summaries.add(summary);
        }

        summaries.sort(new Comparator<ProjectNarrativeSummary>() {
            @Override
            public int compare(ProjectNarrativeSummary a, ProjectNarrativeSummary b) {
                int minutesCompare = Integer.compare(b.getMinutesSpent(), a.getMinutesSpent());
                if (minutesCompare != 0) {
                    return minutesCompare;
                }
                return s(a.getProjectName()).compareToIgnoreCase(s(b.getProjectName()));
            }
        });

        return summaries;
    }

    public void saveNarrativeForProjectDate(AppReq appReq, long projectId, LocalDate reviewDate,
            ProjectNarrativeEntry entry) {
        Session dataSession = appReq.getDataSession();
        Project project = requireProjectForWorkspace(dataSession, appReq.getActiveWorkspaceId(), projectId);

        Map<ProjectNarrativeVerb, String> fields = new EnumMap<ProjectNarrativeVerb, String>(
                ProjectNarrativeVerb.class);
        fields.put(ProjectNarrativeVerb.NOTE, entry == null ? null : entry.getNote());
        fields.put(ProjectNarrativeVerb.DECISION, entry == null ? null : entry.getDecision());
        fields.put(ProjectNarrativeVerb.INSIGHT, entry == null ? null : entry.getInsight());
        fields.put(ProjectNarrativeVerb.RISK, entry == null ? null : entry.getRisk());
        fields.put(ProjectNarrativeVerb.OPPORTUNITY, entry == null ? null : entry.getOpportunity());

        Transaction transaction = null;
        try {
            transaction = dataSession.beginTransaction();
            applyFields(dataSession, project, reviewDate, appReq.getWebUser(), fields);
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public void saveSingleNarrativeForProjectDate(AppReq appReq, long projectId, LocalDate reviewDate,
            ProjectNarrativeVerb verb, String text) {
        Session dataSession = appReq.getDataSession();
        Project project = requireProjectForWorkspace(dataSession, appReq.getActiveWorkspaceId(), projectId);
        if (verb == null) {
            throw new IllegalArgumentException("Narrative verb is required");
        }
        String normalizedText = s(text).trim();
        if (normalizedText.length() == 0) {
            throw new IllegalArgumentException("Narrative text is required");
        }

        Map<ProjectNarrativeVerb, String> fields = new EnumMap<ProjectNarrativeVerb, String>(
                ProjectNarrativeVerb.class);
        fields.put(verb, normalizedText);

        Transaction transaction = null;
        try {
            transaction = dataSession.beginTransaction();
            applyFields(dataSession, project, reviewDate, appReq.getWebUser(), fields);
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * MCP-facing save (docs/MCp-Feedback.md items 3 and 4/P-7): runs on the
     * caller's already-open session/transaction (the MCP resource wraps the
     * whole request in one), and -- unlike the two AppReq methods above,
     * which always carry all five verbs -- only touches verbs present in
     * "providedFields". A verb absent from the map is left alone; a verb
     * present with blank/empty text clears that day's entry (the P-7 fix);
     * NOTE, if provided blank, still gets DEFAULT_NOTE_TEXT rather than being
     * cleared, since it's what marks a project reviewed.
     */
    public void applyReviewFields(Session dataSession, Project project, LocalDate reviewDate, WebUser webUser,
            Map<ProjectNarrativeVerb, String> providedFields) {
        applyFields(dataSession, project, reviewDate, webUser, providedFields);
    }

    private void applyFields(Session dataSession, Project project, LocalDate reviewDate, WebUser webUser,
            Map<ProjectNarrativeVerb, String> fields) {
        ProjectNarrativeDao narrativeDao = new ProjectNarrativeDao(dataSession);
        int offsetSeconds = 0;

        if (fields.containsKey(ProjectNarrativeVerb.NOTE)) {
            String noteText = s(fields.get(ProjectNarrativeVerb.NOTE)).trim();
            if (noteText.length() == 0) {
                noteText = DEFAULT_NOTE_TEXT;
            }
            offsetSeconds = upsertOrClear(narrativeDao, project, reviewDate, webUser, ProjectNarrativeVerb.NOTE,
                    noteText, offsetSeconds);
        }
        for (ProjectNarrativeVerb verb : new ProjectNarrativeVerb[] { ProjectNarrativeVerb.DECISION,
                ProjectNarrativeVerb.INSIGHT, ProjectNarrativeVerb.RISK, ProjectNarrativeVerb.OPPORTUNITY }) {
            if (fields.containsKey(verb)) {
                offsetSeconds = upsertOrClear(narrativeDao, project, reviewDate, webUser, verb, fields.get(verb),
                        offsetSeconds);
            }
        }

        // Guarantee at least one narrative marks the day reviewed, even when the caller
        // only touched (and cleared) non-NOTE verbs and never mentioned NOTE at all.
        if (!fields.containsKey(ProjectNarrativeVerb.NOTE)
                && !narrativeDao.hasNarrativeForProjectOnDate(project.getProjectId(), reviewDate)) {
            upsertOrClear(narrativeDao, project, reviewDate, webUser, ProjectNarrativeVerb.NOTE, DEFAULT_NOTE_TEXT,
                    offsetSeconds);
        }
    }

    private int upsertOrClear(ProjectNarrativeDao narrativeDao, Project project, LocalDate reviewDate,
            WebUser webUser, ProjectNarrativeVerb verb, String text, int offsetSeconds) {
        String trimmed = s(text).trim();
        ProjectNarrative existing = narrativeDao.findNarrativeForProjectVerbOnDate(project.getProjectId(), verb,
                reviewDate);
        if (trimmed.length() == 0) {
            if (existing != null) {
                narrativeDao.delete(existing);
            }
            return offsetSeconds;
        }
        Date narrativeDate = buildNarrativeDate(reviewDate, offsetSeconds, webUser);
        if (existing == null) {
            ProjectNarrative narrative = new ProjectNarrative();
            narrative.setProject(project);
            narrative.setContact(webUser.getProjectContact());
            narrative.setWorkspaceId(project.getWorkspaceId());
            narrative.setNarrativeVerb(verb);
            narrative.setNarrativeText(trimmed);
            narrative.setNarrativeDate(narrativeDate);
            narrativeDao.insert(narrative);
        } else {
            narrativeDao.updateNarrativeTextIfChanged(existing, trimmed, narrativeDate);
        }
        return offsetSeconds + 1;
    }

    private Date buildNarrativeDate(LocalDate reviewDate, int offsetSeconds, WebUser webUser) {
        java.util.Calendar calendar = webUser.getCalendar(new Date());
        calendar.set(reviewDate.getYear(), reviewDate.getMonthValue() - 1, reviewDate.getDayOfMonth(), 12, 0, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        calendar.add(java.util.Calendar.SECOND, offsetSeconds);
        return calendar.getTime();
    }

    private ProjectNarrativeEntry loadNarrativeEntry(ProjectNarrativeDao narrativeDao, long projectId,
            LocalDate reviewDate) {
        List<ProjectNarrative> narratives = narrativeDao.findByProjectAndDateRange(projectId, reviewDate);
        Map<ProjectNarrativeVerb, String> textByVerb = new EnumMap<ProjectNarrativeVerb, String>(
                ProjectNarrativeVerb.class);
        for (ProjectNarrative narrative : narratives) {
            ProjectNarrativeVerb verb = narrative.getNarrativeVerb();
            if (verb == null) {
                continue;
            }
            textByVerb.put(verb, s(narrative.getNarrativeText()));
        }

        ProjectNarrativeEntry entry = new ProjectNarrativeEntry();
        entry.setNote(cleanNote(textByVerb.get(ProjectNarrativeVerb.NOTE)));
        entry.setDecision(s(textByVerb.get(ProjectNarrativeVerb.DECISION)));
        entry.setInsight(s(textByVerb.get(ProjectNarrativeVerb.INSIGHT)));
        entry.setRisk(s(textByVerb.get(ProjectNarrativeVerb.RISK)));
        entry.setOpportunity(s(textByVerb.get(ProjectNarrativeVerb.OPPORTUNITY)));
        return entry;
    }

    private String cleanNote(String value) {
        String note = s(value);
        if (DEFAULT_NOTE_TEXT.equals(note)) {
            return "";
        }
        return note;
    }

    public static Project requireProjectForWorkspace(Session dataSession, Integer workspaceId, long projectId) {
        Project project = (Project) dataSession.get(Project.class, (int) projectId);
        if (project == null || workspaceId == null || project.getWorkspaceId() == null
                || !workspaceId.equals(project.getWorkspaceId())) {
            throw new IllegalArgumentException("Project is not available");
        }
        return project;
    }

    private String s(String value) {
        return value == null ? "" : value;
    }
}
