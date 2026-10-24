package org.dandeliondaily.narrative.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.dandeliondaily.narrative.model.TrackerNarrativeScope;
import org.dandeliondaily.narrative.model.TrackerNarrativeViewModel;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.LockMode;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.TrackerNarrativeDao;
import org.openimmunizationsoftware.pt.manager.NarrativePeriods;
import org.openimmunizationsoftware.pt.manager.NarrativePeriods.PeriodRange;
import org.openimmunizationsoftware.pt.manager.TrackerNarrativeGenerator;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContact;
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

    public TrackerNarrativeViewModel loadViewModel(Session dataSession, WebUser webUser,
            TrackerNarrativeScope scope, long preferredNarrativeId) {
        TrackerNarrativeDao narrativeDao = new TrackerNarrativeDao(dataSession);
        TrackerNarrativeViewModel model = new TrackerNarrativeViewModel();
        model.setScope(scope);
        model.setGenerationAvailable(TrackerNarrativeGenerator.isGenerationAvailable());
        model.setGenerationUnavailableMessage(GENERATION_UNAVAILABLE_MESSAGE);

        List<TrackerNarrative> narratives = narrativeDao.findByContactTypeAndPeriod(webUser.getContactId(),
                scope.getNarrativeType(),
                scope.getPeriodStart(),
                scope.getPeriodEnd());
        model.getNarratives().addAll(narratives);

        TrackerNarrative approved = narrativeDao.findApprovedByContactTypeAndPeriod(webUser.getContactId(),
                scope.getNarrativeType(),
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
        return generate(appReq.getDataSession(), appReq.getWebUser(), null, resolveProjectId(appReq), scope)
                .getNarrativeId();
    }

    public TrackerNarrative read(Session session, WebUser owner, Integer workspaceId, long id) {
        return requireNarrative(session, owner, workspaceId, id, false);
    }

    private TrackerNarrative requireNarrative(Session session, WebUser owner, Integer workspaceId, long id,
            boolean forUpdate) {
        if (id <= 0 || id > Integer.MAX_VALUE) {
            throw problem("invalid_arguments", "narrativeId must be a positive integer.");
        }
        TrackerNarrative narrative = forUpdate
                ? (TrackerNarrative) session.get(TrackerNarrative.class, (int) id, LockMode.UPGRADE)
                : (TrackerNarrative) session.get(TrackerNarrative.class, (int) id);
        if (narrative != null && forUpdate) {
            session.refresh(narrative, LockMode.UPGRADE);
        }
        if (narrative == null || narrative.getContactId() != owner.getContactId()) {
            throw problem("not_found", "Narrative not found.");
        }
        requireProject(session, narrative.getProjectId(), workspaceId);
        return narrative;
    }

    public TrackerNarrative submit(Session session, WebUser owner, Integer workspaceId, int projectId,
            TrackerNarrativeScope scope, String markdown, String modelName, String promptVersion, String promptText) {
        requireMarkdown(markdown);
        requireLength(modelName, 50, "modelName");
        requireLength(promptVersion, 50, "promptVersion");
        return write(session, owner, () -> {
            requireProject(session, projectId, workspaceId);
            // Matching the immutable source makes retries safe even after editing or approving the draft.
            for (TrackerNarrative existing : lockedPeriod(session, owner, scope)) {
                if (existing.getProjectId() == projectId && Objects.equals(markdown, existing.getMarkdownGenerated())
                        && Objects.equals(modelName, existing.getModelName())
                        && Objects.equals(promptVersion, existing.getPromptVersion())
                        && Objects.equals(promptText, existing.getPromptUsedText())) {
                    return existing;
                }
            }
            TrackerNarrative narrative = buildNewNarrative(scope.getNarrativeType(), scope.getPeriodStart(),
                    scope.getPeriodEnd(), projectId, owner.getContactId());
            narrative.setReviewStatus(TrackerNarrativeReviewStatus.GENERATED);
            narrative.setMarkdownGenerated(markdown);
            narrative.setMarkdownFinal(markdown);
            narrative.setDateGenerated(new Date());
            narrative.setModelName(modelName);
            narrative.setPromptVersion(promptVersion);
            narrative.setPromptUsedText(promptText);
            new TrackerNarrativeDao(session).insert(narrative);
            return narrative;
        });
    }

    public TrackerNarrative updateDraft(Session session, WebUser owner, Integer workspaceId, long id,
            String lastUpdated, String markdown) {
        requireMarkdown(markdown);
        return write(session, owner, () -> {
            TrackerNarrative narrative = current(session, owner, workspaceId, id, lastUpdated);
            requireEditable(narrative);
            if (TrackerNarrativeReviewStatus.APPROVED.equals(narrative.getReviewStatus())) {
                TrackerNarrative revision = buildNewNarrative(narrative.getNarrativeType(),
                        owner.toLocalDate(narrative.getPeriodStart()), owner.toLocalDate(narrative.getPeriodEnd()),
                        narrative.getProjectId(), owner.getContactId());
                revision.setDisplayTitle(narrative.getDisplayTitle());
                revision.setMarkdownGenerated(narrative.getMarkdownGenerated());
                revision.setDateGenerated(narrative.getDateGenerated());
                revision.setModelName(narrative.getModelName());
                revision.setPromptVersion(narrative.getPromptVersion());
                revision.setPromptUsedText(narrative.getPromptUsedText());
                revision.setMarkdownFinal(markdown);
                revision.setReviewStatus(TrackerNarrativeReviewStatus.GENERATED);
                new TrackerNarrativeDao(session).insert(revision);
                return revision;
            }
            narrative.setMarkdownFinal(markdown);
            narrative.setReviewStatus(TrackerNarrativeReviewStatus.GENERATED);
            narrative.setDateApproved(null);
            markUpdated(narrative);
            session.update(narrative);
            return narrative;
        });
    }

    public List<Integer> approve(Session session, WebUser owner, Integer workspaceId, long id, String lastUpdated) {
        return write(session, owner, () -> {
            TrackerNarrative narrative = current(session, owner, workspaceId, id, lastUpdated);
            if (TrackerNarrativeReviewStatus.APPROVED.equals(narrative.getReviewStatus())) {
                return new ArrayList<Integer>();
            }
            if (!TrackerNarrativeReviewStatus.GENERATED.equals(narrative.getReviewStatus())) {
                throw problem("invalid_state", "Only a generated draft can be approved.");
            }
            requireMarkdown(narrative.getMarkdownFinal());
            TrackerNarrativeScope scope = resolveScope(narrative.getNarrativeType(),
                    owner.toLocalDate(narrative.getPeriodStart()));
            List<Integer> displaced = new ArrayList<Integer>();
            for (TrackerNarrative previous : lockedPeriod(session, owner, scope)) {
                if (previous.getNarrativeId() != id
                        && TrackerNarrativeReviewStatus.APPROVED.equals(previous.getReviewStatus())) {
                    previous.setReviewStatus(TrackerNarrativeReviewStatus.REJECTED);
                    markUpdated(previous);
                    session.update(previous);
                    displaced.add(Integer.valueOf(previous.getNarrativeId()));
                }
            }
            narrative.setReviewStatus(TrackerNarrativeReviewStatus.APPROVED);
            narrative.setDateApproved(new Date());
            markUpdated(narrative);
            session.update(narrative);
            return displaced;
        });
    }

    public TrackerNarrative changeStatus(Session session, WebUser owner, Integer workspaceId, long id,
            String lastUpdated, TrackerNarrativeReviewStatus status) {
        if (status != TrackerNarrativeReviewStatus.REJECTED && status != TrackerNarrativeReviewStatus.DELETED) {
            throw problem("invalid_arguments", "Unsupported status change.");
        }
        return write(session, owner, () -> {
            TrackerNarrative narrative = current(session, owner, workspaceId, id, lastUpdated);
            if (narrative.getReviewStatus() == status) {
                return narrative;
            }
            if (narrative.getReviewStatus() == TrackerNarrativeReviewStatus.GENERATING
                    || narrative.getReviewStatus() == TrackerNarrativeReviewStatus.DELETED) {
                throw problem("invalid_state", "A generating or deleted narrative cannot be changed.");
            }
            narrative.setReviewStatus(status);
            markUpdated(narrative);
            session.update(narrative);
            return narrative;
        });
    }

    public TrackerNarrative generate(Session session, WebUser owner, Integer workspaceId, int projectId,
            TrackerNarrativeScope scope) {
        requireGeneration();
        return write(session, owner, () -> {
            requireProject(session, projectId, workspaceId);
            return queueGeneration(session, owner, projectId, scope);
        });
    }

    public TrackerNarrative regenerate(Session session, WebUser owner, Integer workspaceId, long id,
            String lastUpdated) {
        requireGeneration();
        return write(session, owner, () -> {
            TrackerNarrative source = current(session, owner, workspaceId, id, lastUpdated);
            if (source.getReviewStatus() == TrackerNarrativeReviewStatus.GENERATING
                    || source.getReviewStatus() == TrackerNarrativeReviewStatus.DELETED) {
                throw problem("invalid_state", "A generating or deleted narrative cannot be regenerated.");
            }
            return queueGeneration(session, owner, source.getProjectId(),
                    resolveScope(source.getNarrativeType(), owner.toLocalDate(source.getPeriodStart())));
        });
    }

    protected boolean isGenerationAvailable() {
        return TrackerNarrativeGenerator.isGenerationAvailable();
    }

    protected void enqueueGeneration(long id) {
        TrackerNarrativeGenerator.enqueue(id);
    }

    private void requireGeneration() {
        if (!isGenerationAvailable()) {
            throw problem("generation_unavailable", GENERATION_UNAVAILABLE_MESSAGE);
        }
    }

    private TrackerNarrative queueGeneration(Session session, WebUser owner, int projectId,
            TrackerNarrativeScope scope) {
        TrackerNarrative narrative = buildNewNarrative(scope.getNarrativeType(), scope.getPeriodStart(),
                scope.getPeriodEnd(), projectId, owner.getContactId());
        new TrackerNarrativeDao(session).insert(narrative);
        session.getTransaction().registerSynchronization(new Synchronization() {
            public void beforeCompletion() {
            }

            public void afterCompletion(int status) {
                if (status == Status.STATUS_COMMITTED) {
                    enqueueGeneration(narrative.getNarrativeId());
                }
            }
        });
        return narrative;
    }

    private TrackerNarrative current(Session session, WebUser owner, Integer workspaceId, long id,
            String lastUpdated) {
        TrackerNarrative narrative = requireNarrative(session, owner, workspaceId, id, true);
        if (lastUpdated == null || lastUpdated.trim().isEmpty()) {
            throw problem("invalid_arguments", "lastUpdated is required; reload the report before modifying it.");
        }
        if (!lastUpdated.equals(version(narrative))) {
            throw problem("stale", "This narrative has changed; read it again before modifying it.");
        }
        return narrative;
    }

    public static String version(TrackerNarrative narrative) {
        return narrative.getLastUpdated() == null ? null
                : java.time.Instant.ofEpochMilli(narrative.getLastUpdated().getTime()).toString();
    }

    public static void markUpdated(TrackerNarrative narrative) {
        // MySQL's legacy timestamp columns have second precision. Every write must change the token.
        long previous = narrative.getLastUpdated() == null ? 0 : narrative.getLastUpdated().getTime();
        narrative.setLastUpdated(new Date(Math.max(System.currentTimeMillis() / 1000 * 1000,
                previous / 1000 * 1000 + 1000)));
    }

    private static Project requireProject(Session session, int projectId, Integer workspaceId) {
        Project project = projectId > 0 ? (Project) session.get(Project.class, projectId) : null;
        if (project == null || (workspaceId != null && !workspaceId.equals(project.getWorkspaceId()))) {
            throw problem("not_found", "Project/narrative not found in this workspace.");
        }
        return project;
    }

    private List<TrackerNarrative> lockedPeriod(Session session, WebUser owner, TrackerNarrativeScope scope) {
        List<TrackerNarrative> narratives = new TrackerNarrativeDao(session).findByContactTypeAndPeriodForUpdate(
                owner.getContactId(), scope.getNarrativeType(), scope.getPeriodStart(), scope.getPeriodEnd());
        for (TrackerNarrative narrative : narratives) {
            session.refresh(narrative, LockMode.UPGRADE);
        }
        return narratives;
    }

    private static void requireEditable(TrackerNarrative narrative) {
        TrackerNarrativeReviewStatus status = narrative.getReviewStatus();
        if (status != TrackerNarrativeReviewStatus.GENERATED && status != TrackerNarrativeReviewStatus.APPROVED
                && status != TrackerNarrativeReviewStatus.REJECTED) {
            throw problem("invalid_state", "Only a generated, approved, or rejected report can be edited.");
        }
    }

    private static void requireMarkdown(String markdown) {
        if (normalizeMarkdown(markdown) == null) {
            throw problem("invalid_arguments", "Report Markdown cannot be empty.");
        }
    }

    private static void requireLength(String value, int max, String field) {
        if (value != null && value.length() > max) {
            throw problem("invalid_arguments", field + " must not exceed " + max + " characters.");
        }
    }

    private interface Work<T> {
        T run();
    }

    private <T> T write(Session session, WebUser owner, Work<T> work) {
        Transaction transaction = session.getTransaction();
        boolean ownTransaction = transaction == null || !transaction.isActive();
        if (ownTransaction) {
            transaction = session.beginTransaction();
        }
        try {
            // Serialize all narrative writes for this contact, including concurrent period approvals/submissions.
            if (owner.getContactId() <= 0 || session.get(ProjectContact.class, owner.getContactId(),
                    LockMode.UPGRADE) == null) {
                throw problem("not_found", "Narrative owner not found.");
            }
            T result = work.run();
            session.flush();
            if (ownTransaction) {
                transaction.commit();
            }
            return result;
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    private static NarrativeWorkflowException problem(String code, String message) {
        return new NarrativeWorkflowException(code, message);
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

    private static TrackerNarrative buildNewNarrative(String type, LocalDate periodStart, LocalDate periodEnd,
            int projectId, int contactId) {
        TrackerNarrative narrative = new TrackerNarrative();
        narrative.setProjectId(projectId);
        narrative.setContactId(contactId);
        narrative.setNarrativeType(type);
        narrative.setPeriodStart(java.sql.Date.valueOf(periodStart));
        narrative.setPeriodEnd(java.sql.Date.valueOf(periodEnd));
        narrative.setDisplayTitle(buildDisplayTitle(type, periodStart, periodEnd));
        narrative.setReviewStatus(TrackerNarrativeReviewStatus.GENERATING);
        narrative.setMarkdownGenerated(null);
        narrative.setMarkdownFinal(null);
        narrative.setLastUpdated(new Date(System.currentTimeMillis() / 1000 * 1000));
        return narrative;
    }
}
