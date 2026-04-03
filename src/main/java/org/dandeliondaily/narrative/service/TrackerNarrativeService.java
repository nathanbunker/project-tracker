package org.dandeliondaily.narrative.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

import org.dandeliondaily.narrative.model.TrackerNarrativeScope;
import org.dandeliondaily.narrative.model.TrackerNarrativeViewModel;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.TrackerNarrativeDao;
import org.openimmunizationsoftware.pt.manager.NarrativePeriods;
import org.openimmunizationsoftware.pt.manager.NarrativePeriods.PeriodRange;
import org.openimmunizationsoftware.pt.manager.TrackerNarrativeGenerator;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;
import org.openimmunizationsoftware.pt.model.TrackerNarrativeReviewStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

public class TrackerNarrativeService {

    // Shared narrative workflow used by both TrackerNarrativeServlet
    // (legacy/alternate view)
    // and ReviewDashboard (newer editor-first view).

    public static final String GENERATION_UNAVAILABLE_MESSAGE = TrackerNarrativeGenerator
            .getGenerationUnavailableMessage();

    public String normalizeType(String type) {
        if (type == null || type.trim().length() == 0) {
            return TrackerNarrativeScope.TYPE_DAILY;
        }
        String normalized = type.trim().toUpperCase();
        if (TrackerNarrativeScope.TYPE_WEEKLY.equals(normalized)
                || TrackerNarrativeScope.TYPE_MONTHLY.equals(normalized)
                || TrackerNarrativeScope.TYPE_DAILY.equals(normalized)) {
            return normalized;
        }
        return TrackerNarrativeScope.TYPE_DAILY;
    }

    public LocalDate resolveDate(String dateValue, AppReq appReq, WebUser webUser) {
        if (dateValue == null || dateValue.trim().length() == 0) {
            return webUser.getLocalDateToday();
        }
        try {
            return LocalDate.parse(dateValue.trim());
        } catch (DateTimeParseException e) {
            if (appReq != null) {
                appReq.setMessageProblem("Invalid date format. Use yyyy-MM-dd.");
            }
            return webUser.getLocalDateToday();
        }
    }

    public TrackerNarrativeScope resolveScope(String typeParam, LocalDate selectedDate) {
        TrackerNarrativeScope scope = new TrackerNarrativeScope();
        String type = normalizeType(typeParam);
        PeriodRange period = resolvePeriod(type, selectedDate);
        scope.setNarrativeType(type);
        scope.setSelectedDate(selectedDate);
        scope.setPeriodStart(period.getStart());
        scope.setPeriodEnd(period.getEnd());
        return scope;
    }

    public TrackerNarrativeViewModel loadViewModel(Session dataSession, TrackerNarrativeScope scope,
            long preferredNarrativeId) {
        TrackerNarrativeDao narrativeDao = new TrackerNarrativeDao(dataSession);
        TrackerNarrativeViewModel model = new TrackerNarrativeViewModel();
        model.setScope(scope);
        model.setGenerationAvailable(TrackerNarrativeGenerator.isGenerationAvailable());
        model.setGenerationUnavailableMessage(GENERATION_UNAVAILABLE_MESSAGE);

        List<TrackerNarrative> narratives = narrativeDao.findByTypeAndPeriod(scope.getNarrativeType(),
                scope.getPeriodStart(),
                scope.getPeriodEnd());
        model.getNarratives().addAll(narratives);

        TrackerNarrative approved = narrativeDao.findApprovedByTypeAndPeriod(scope.getNarrativeType(),
                scope.getPeriodStart(),
                scope.getPeriodEnd());
        model.setApprovedNarrative(approved);

        TrackerNarrative active = selectActiveNarrative(narratives, approved, preferredNarrativeId);
        model.setActiveNarrative(active);

        for (TrackerNarrative narrative : narratives) {
            if (active != null && narrative.getNarrativeId() == active.getNarrativeId()) {
                continue;
            }
            if (TrackerNarrativeReviewStatus.DELETED.equals(narrative.getReviewStatus())) {
                continue;
            }
            // History is intentionally secondary in the new ReviewDashboard UX.
            model.getHistoryItems().add(narrative);
        }
        return model;
    }

    public long generate(AppReq appReq, TrackerNarrativeScope scope) {
        if (!TrackerNarrativeGenerator.isGenerationAvailable()) {
            return 0;
        }
        int projectId = resolveProjectId(appReq);
        int contactId = resolveContactId(appReq.getWebUser());
        if (projectId <= 0 || contactId <= 0) {
            appReq.setMessageProblem("Unable to determine project/contact for narrative.");
            return 0;
        }

        TrackerNarrative narrative = buildNewNarrative(scope.getNarrativeType(), scope.getPeriodStart(),
                scope.getPeriodEnd(),
                projectId, contactId, appReq.getWebUser());

        long newId = insertNarrative(appReq.getDataSession(), narrative);
        if (newId > 0) {
            TrackerNarrativeGenerator.enqueue(newId);
        }
        return newId;
    }

    public long regenerate(AppReq appReq, long narrativeId) {
        if (!TrackerNarrativeGenerator.isGenerationAvailable()) {
            return 0;
        }

        Session dataSession = appReq.getDataSession();
        TrackerNarrative existing = (TrackerNarrative) dataSession.get(TrackerNarrative.class, (int) narrativeId);
        if (existing == null) {
            appReq.setMessageProblem("Narrative not found.");
            return 0;
        }

        int projectId = existing.getProjectId();
        int contactId = existing.getContactId();
        if (projectId <= 0 || contactId <= 0) {
            projectId = resolveProjectId(appReq);
            contactId = resolveContactId(appReq.getWebUser());
        }
        if (projectId <= 0 || contactId <= 0) {
            appReq.setMessageProblem("Unable to determine project/contact for narrative.");
            return 0;
        }

        LocalDate periodStart = appReq.getWebUser().toLocalDate(existing.getPeriodStart());
        LocalDate periodEnd = appReq.getWebUser().toLocalDate(existing.getPeriodEnd());
        TrackerNarrative replacement = buildNewNarrative(existing.getNarrativeType(), periodStart, periodEnd, projectId,
                contactId, appReq.getWebUser());

        long newId = insertNarrative(dataSession, replacement);
        if (newId > 0) {
            TrackerNarrativeGenerator.enqueue(newId);
        }
        return newId;
    }

    public void approve(Session dataSession, WebUser webUser, long narrativeId) {
        if (narrativeId <= 0) {
            return;
        }
        TrackerNarrativeDao narrativeDao = new TrackerNarrativeDao(dataSession);
        narrativeDao.approve(narrativeId, webUser.getLocalDateTimeNow());
    }

    public void reject(Session dataSession, long narrativeId) {
        if (narrativeId <= 0) {
            return;
        }
        Transaction transaction = dataSession.beginTransaction();
        try {
            TrackerNarrativeDao narrativeDao = new TrackerNarrativeDao(dataSession);
            narrativeDao.reject(narrativeId);
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public void delete(Session dataSession, long narrativeId) {
        if (narrativeId <= 0) {
            return;
        }
        Transaction transaction = dataSession.beginTransaction();
        try {
            TrackerNarrativeDao narrativeDao = new TrackerNarrativeDao(dataSession);
            narrativeDao.softDelete(narrativeId);
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public void saveMarkdown(Session dataSession, long narrativeId, String markdownFinal) {
        if (narrativeId <= 0) {
            return;
        }
        Transaction transaction = dataSession.beginTransaction();
        try {
            TrackerNarrativeDao narrativeDao = new TrackerNarrativeDao(dataSession);
            narrativeDao.updateFinalText(narrativeId, normalizeMarkdown(markdownFinal));
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public static String normalizeMarkdown(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : value;
    }

    public static String buildDisplayTitle(String type, LocalDate periodStart, LocalDate periodEnd) {
        if (TrackerNarrativeScope.TYPE_WEEKLY.equals(type)) {
            return "Weekly Summary - " + periodStart + " to " + periodEnd;
        }
        if (TrackerNarrativeScope.TYPE_MONTHLY.equals(type)) {
            return "Monthly Summary - " + periodStart.getYear() + "-"
                    + String.format("%02d", periodStart.getMonthValue());
        }
        return "Daily Summary - " + periodStart;
    }

    private static PeriodRange resolvePeriod(String type, LocalDate date) {
        if (TrackerNarrativeScope.TYPE_WEEKLY.equals(type)) {
            return NarrativePeriods.forWeekly(date);
        }
        if (TrackerNarrativeScope.TYPE_MONTHLY.equals(type)) {
            return NarrativePeriods.forMonthly(YearMonth.from(date));
        }
        return NarrativePeriods.forDaily(date);
    }

    private static TrackerNarrative selectActiveNarrative(List<TrackerNarrative> narratives, TrackerNarrative approved,
            long preferredNarrativeId) {
        if (narratives == null || narratives.isEmpty()) {
            return null;
        }

        if (preferredNarrativeId > 0) {
            for (TrackerNarrative narrative : narratives) {
                if (narrative.getNarrativeId() == preferredNarrativeId
                        && !TrackerNarrativeReviewStatus.DELETED.equals(narrative.getReviewStatus())) {
                    return narrative;
                }
            }
        }

        if (approved != null && !TrackerNarrativeReviewStatus.DELETED.equals(approved.getReviewStatus())) {
            return approved;
        }

        for (TrackerNarrative narrative : narratives) {
            if (TrackerNarrativeReviewStatus.GENERATED.equals(narrative.getReviewStatus())
                    || TrackerNarrativeReviewStatus.GENERATING.equals(narrative.getReviewStatus())) {
                return narrative;
            }
        }

        for (TrackerNarrative narrative : narratives) {
            if (!TrackerNarrativeReviewStatus.DELETED.equals(narrative.getReviewStatus())) {
                return narrative;
            }
        }

        return narratives.get(0);
    }

    private static int resolveProjectId(AppReq appReq) {
        Project project = appReq.getProject();
        if (project == null) {
            project = appReq.getProjectSelected();
        }
        if (project == null) {
            project = appReq.getProjectTrackTime();
        }
        return project == null ? 0 : project.getProjectId();
    }

    private static int resolveContactId(WebUser webUser) {
        return webUser == null ? 0 : webUser.getContactId();
    }

    private static TrackerNarrative buildNewNarrative(String type, LocalDate periodStart, LocalDate periodEnd,
            int projectId, int contactId, WebUser webUser) {
        TrackerNarrative narrative = new TrackerNarrative();
        narrative.setProjectId(projectId);
        narrative.setContactId(contactId);
        narrative.setNarrativeType(type);
        narrative.setPeriodStart(webUser.toDate(periodStart));
        narrative.setPeriodEnd(webUser.toDate(periodEnd));
        narrative.setDisplayTitle(buildDisplayTitle(type, periodStart, periodEnd));
        narrative.setReviewStatus(TrackerNarrativeReviewStatus.GENERATING);
        narrative.setMarkdownGenerated(null);
        narrative.setMarkdownFinal(null);
        narrative.setLastUpdated(new Date());
        return narrative;
    }

    private static long insertNarrative(Session dataSession, TrackerNarrative narrative) {
        Transaction transaction = dataSession.beginTransaction();
        try {
            TrackerNarrativeDao narrativeDao = new TrackerNarrativeDao(dataSession);
            long newId = narrativeDao.insert(narrative);
            transaction.commit();
            return newId;
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }
}
