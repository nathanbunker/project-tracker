package org.dandeliondaily.projectnarrative.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class ProjectNarrativeService {

    private static final String DEFAULT_NOTE_TEXT = "Reviewed/no comments";

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
            summary.setProjectName(s(project.getProjectName()));
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
        Project project = (Project) dataSession.get(Project.class, (int) projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project is not available");
        }

        ProjectNarrativeDao narrativeDao = new ProjectNarrativeDao(dataSession);

        String noteText = s(entry == null ? null : entry.getNote()).trim();
        String decisionText = s(entry == null ? null : entry.getDecision()).trim();
        String insightText = s(entry == null ? null : entry.getInsight()).trim();
        String riskText = s(entry == null ? null : entry.getRisk()).trim();
        String opportunityText = s(entry == null ? null : entry.getOpportunity()).trim();

        if (noteText.length() == 0) {
            noteText = DEFAULT_NOTE_TEXT;
        }

        Transaction transaction = null;
        try {
            transaction = dataSession.beginTransaction();
            int offsetSeconds = 0;
            offsetSeconds = upsertNarrative(narrativeDao, appReq, project, reviewDate,
                    ProjectNarrativeVerb.NOTE, noteText, offsetSeconds);

            offsetSeconds = upsertIfPresent(narrativeDao, appReq, project, reviewDate,
                    ProjectNarrativeVerb.DECISION, decisionText, offsetSeconds);
            offsetSeconds = upsertIfPresent(narrativeDao, appReq, project, reviewDate,
                    ProjectNarrativeVerb.INSIGHT, insightText, offsetSeconds);
            offsetSeconds = upsertIfPresent(narrativeDao, appReq, project, reviewDate,
                    ProjectNarrativeVerb.RISK, riskText, offsetSeconds);
            upsertIfPresent(narrativeDao, appReq, project, reviewDate,
                    ProjectNarrativeVerb.OPPORTUNITY, opportunityText, offsetSeconds);

            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    private int upsertIfPresent(ProjectNarrativeDao narrativeDao, AppReq appReq, Project project,
            LocalDate reviewDate, ProjectNarrativeVerb verb, String text, int offsetSeconds) {
        if (text == null || text.length() == 0) {
            return offsetSeconds;
        }
        return upsertNarrative(narrativeDao, appReq, project, reviewDate, verb, text, offsetSeconds);
    }

    private int upsertNarrative(ProjectNarrativeDao narrativeDao, AppReq appReq, Project project,
            LocalDate reviewDate, ProjectNarrativeVerb verb, String text, int offsetSeconds) {
        ProjectNarrative narrative = narrativeDao.findNarrativeForProjectVerbOnDate(project.getProjectId(), verb,
                reviewDate);
        Date narrativeDate = buildNarrativeDate(reviewDate, offsetSeconds, appReq.getWebUser());
        if (narrative == null) {
            narrative = new ProjectNarrative();
            narrative.setProject(project);
            narrative.setContact(appReq.getWebUser().getProjectContact());
            narrative.setProvider(appReq.getWebUser().getProvider());
            narrative.setNarrativeVerb(verb);
            narrative.setNarrativeText(text);
            narrative.setNarrativeDate(narrativeDate);
            narrativeDao.insert(narrative);
        } else {
            narrativeDao.updateNarrativeTextIfChanged(narrative, text, narrativeDate);
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

    private String s(String value) {
        return value == null ? "" : value;
    }
}
