package org.dandeliondaily.planahead.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionNextTemplateConfig;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.TemplateType;

/**
 * Generates and manages recurring template instances.
 *
 * Callers are responsible for wrapping calls in a Hibernate transaction.
 * This service performs DB reads and writes but does not open, commit,
 * or roll back transactions itself.
 */
public class TemplateGenerationService {

    private static final String STATUS_READY = ProjectNextActionStatus.READY.getId();
    private static final String STATUS_CANCELLED = ProjectNextActionStatus.CANCELLED.getId();

    private final SchedulePatternEvaluator patternEvaluator = new SchedulePatternEvaluator();

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Generate template instances from today through today+advanceDays for every
     * active template owned by the given workspace/contact. Idempotent: dates that
     * already have an instance (any status) are skipped.
     */
    public void generateForwardWindow(Session session, int workspaceId, int contactId,
            LocalDate today, int advanceDays) {
        LocalDate windowEnd = today.plusDays(advanceDays);
        List<ActionNext> templates = loadTemplateRoots(session, workspaceId, contactId);
        for (ActionNext template : templates) {
            try {
                generateForTemplate(session, template, today, windowEnd);
            } catch (Exception e) {
                System.err.println("[TemplateGenerationService] Error generating for template "
                        + template.getActionNextId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * After a template definition is saved, propagate changed fields to all future
     * READY instances (today and later). Today's instance is also updated unless it
     * has BillEntry records, in which case it is left untouched.
     *
     * If autoGenerate was just turned off, all future READY instances are
     * cancelled.
     */
    public void propagateTemplateEdit(Session session, ActionNext template,
            int workspaceId, int contactId, LocalDate today) {
        Date todayDate = toUtcDate(today);
        ActionNextTemplateConfig config = (ActionNextTemplateConfig) session.get(
                ActionNextTemplateConfig.class, template.getActionNextId());
        boolean rescheduleLocked = isRescheduleLocked(config);

        if (config != null && !config.isAutoGenerate()) {
            cancelFutureReadyInstances(session, template.getActionNextId(),
                    workspaceId, contactId, todayDate);
            return;
        }

        Query query = session.createQuery(
                "from ActionNext an where an.workspaceId = :workspaceId "
                        + "and (an.contactId = :contactId or an.nextContactId = :contactId) "
                        + "and an.templateActionNextId = :templateId "
                        + "and an.nextActionDate is not null and an.nextActionDate >= :today "
                        + "and an.nextActionStatusString = :ready");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", contactId);
        query.setParameter("templateId", template.getActionNextId());
        query.setParameter("today", todayDate);
        query.setParameter("ready", STATUS_READY);
        @SuppressWarnings("unchecked")
        List<ActionNext> instances = query.list();

        for (ActionNext instance : instances) {
            boolean isToday = isSameDay(instance.getNextActionDate(), todayDate);
            if (isToday && hasBillEntries(session, instance.getActionNextId())) {
                continue; // protect billed work
            }
            copyTemplateFieldsToInstance(template, instance, rescheduleLocked);
            session.update(instance);
        }
    }

    /**
     * Apply missed-action behavior to past READY template instances for all
     * templates owned by the given workspace/contact.
     *
     * AUTO_CANCEL: cancel the instance.
     * CARRY_FORWARD: move to today if no instance already exists for this template
     * today; otherwise cancel.
     * IGNORE: leave as-is.
     *
     * Should be called before generateForwardWindow so that carry-forward logic
     * can detect whether today already has an instance.
     */
    public void applyMissedActionBehavior(Session session, int workspaceId, int contactId,
            LocalDate today) {
        Date todayDate = toUtcDate(today);
        Query query = session.createQuery(
                "from ActionNext an where an.workspaceId = :workspaceId "
                        + "and (an.contactId = :contactId or an.nextContactId = :contactId) "
                        + "and an.templateActionNextId is not null "
                        + "and an.nextActionDate is not null and an.nextActionDate < :today "
                        + "and an.nextActionStatusString = :ready");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", contactId);
        query.setParameter("today", todayDate);
        query.setParameter("ready", STATUS_READY);
        @SuppressWarnings("unchecked")
        List<ActionNext> pastInstances = query.list();

        for (ActionNext instance : pastInstances) {
            ActionNext template = (ActionNext) session.get(ActionNext.class,
                    instance.getTemplateActionNextId());
            if (template == null) {
                continue;
            }
            ActionNextTemplateConfig config = (ActionNextTemplateConfig) session.get(
                    ActionNextTemplateConfig.class, template.getActionNextId());
            if (config == null) {
                continue;
            }
            String behavior = config.getMissedActionBehavior();
            if ("AUTO_CANCEL".equals(behavior)) {
                instance.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
                instance.setNextChangeDate(new Date());
                session.update(instance);
            } else if ("CARRY_FORWARD".equals(behavior)) {
                boolean todayAlreadyHasInstance = todayHasActiveInstance(session,
                        template.getActionNextId(), workspaceId, contactId, todayDate);
                if (todayAlreadyHasInstance) {
                    instance.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
                } else {
                    instance.setNextActionDate(todayDate);
                    instance.setRescheduleLocked(false);
                }
                instance.setNextChangeDate(new Date());
                session.update(instance);
            }
            // IGNORE: leave as-is
        }
    }

    /**
     * Called immediately after a template definition is saved via the UI.
     * Propagates field changes to future instances, cancels instances that no
     * longer match the updated schedule, and generates new instances for any
     * matching dates in [today, today+advanceDays] that don't yet have one.
     *
     * Callers must wrap this call in a Hibernate transaction.
     */
    public void syncAfterEdit(Session session, ActionNext template,
            int workspaceId, int contactId, LocalDate today, int advanceDays) {
        // Step 1 — propagate field changes and handle autoGenerate=N cancellation
        propagateTemplateEdit(session, template, workspaceId, contactId, today);

        ActionNextTemplateConfig config = (ActionNextTemplateConfig) session.get(
                ActionNextTemplateConfig.class, template.getActionNextId());
        if (config == null || !config.isAutoGenerate()) {
            return;
        }

        TemplateType type = template.getTemplateType();
        if (type == null) {
            return;
        }

        String pattern = resolvePattern(type, config);

        // Step 2 — cancel future READY instances whose dates no longer match the
        // (possibly changed) schedule pattern
        cancelInstancesNotMatchingSchedule(session, template.getActionNextId(),
                workspaceId, contactId, today, type, pattern);

        // Step 3 — generate new instances for matching dates that don't have one yet
        generateForTemplate(session, template, today, today.plusDays(advanceDays));
    }

    // =========================================================================
    // Private generation helpers
    // =========================================================================

    @SuppressWarnings("unchecked")
    private List<ActionNext> loadTemplateRoots(Session session, int workspaceId, int contactId) {
        Query query = session.createQuery(
                "from ActionNext an where an.workspaceId = :workspaceId "
                        + "and (an.contactId = :contactId or an.nextContactId = :contactId) "
                        + "and an.templateTypeString is not null and an.templateTypeString <> '' "
                        + "and an.templateActionNextId is null "
                        + "and an.nextActionStatusString <> :cancelled");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", contactId);
        query.setParameter("cancelled", STATUS_CANCELLED);
        return query.list();
    }

    private void generateForTemplate(Session session, ActionNext template,
            LocalDate today, LocalDate windowEnd) {
        ActionNextTemplateConfig config = (ActionNextTemplateConfig) session.get(
                ActionNextTemplateConfig.class, template.getActionNextId());
        if (config == null || !config.isAutoGenerate()) {
            return;
        }
        TemplateType type = template.getTemplateType();
        if (type == null) {
            return;
        }

        // Start from today or from the day after the last generated date
        LocalDate generationFrom = today;
        if (config.getLastGeneratedDate() != null) {
            LocalDate lastGen = toLocalDate(config.getLastGeneratedDate());
            if (lastGen.isAfter(today)) {
                // Already generated up through some future date; extend from there
                generationFrom = lastGen.plusDays(1);
            }
        }
        if (generationFrom.isAfter(windowEnd)) {
            return; // nothing new to generate
        }

        String pattern = resolvePattern(type, config);
        List<LocalDate> scheduledDates = patternEvaluator.matchingDates(
                type, pattern, generationFrom, windowEnd);

        Set<LocalDate> existingDates = loadExistingInstanceDates(session,
                template.getActionNextId(), workspaceId(template), contactId(template),
                generationFrom, windowEnd);

        boolean rescheduleLocked = isRescheduleLocked(config);
        for (LocalDate date : scheduledDates) {
            if (!existingDates.contains(date)) {
                session.save(createInstance(template, date, rescheduleLocked));
            }
        }

        config.setLastGeneratedDate(toUtcDate(windowEnd));
        session.update(config);
    }

    private Set<LocalDate> loadExistingInstanceDates(Session session, int templateActionNextId,
            int workspaceId, int contactId, LocalDate from, LocalDate to) {
        Date fromDate = toUtcDate(from);
        Date toDate = toUtcDate(to);
        Query query = session.createQuery(
                "select an.nextActionDate from ActionNext an "
                        + "where an.workspaceId = :workspaceId "
                        + "and (an.contactId = :contactId or an.nextContactId = :contactId) "
                        + "and an.templateActionNextId = :templateId "
                        + "and an.nextActionDate is not null "
                        + "and an.nextActionDate >= :fromDate and an.nextActionDate <= :toDate");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", contactId);
        query.setParameter("templateId", templateActionNextId);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        @SuppressWarnings("unchecked")
        List<Date> dates = query.list();
        Set<LocalDate> result = new HashSet<>();
        for (Date d : dates) {
            result.add(toLocalDate(d));
        }
        return result;
    }

    private ActionNext createInstance(ActionNext template, LocalDate date, boolean rescheduleLocked) {
        ActionNext instance = new ActionNext();
        instance.setProjectId(template.getProjectId());
        instance.setProject(template.getProject());
        instance.setContactId(template.getContactId());
        instance.setContact(template.getContact());
        instance.setWorkspaceId(template.getWorkspaceId());
        instance.setNextContactId(template.getNextContactId());
        instance.setNextProjectContact(template.getNextProjectContact());
        instance.setNextActionDate(toUtcDate(date));
        instance.setNextActionStatus(ProjectNextActionStatus.READY);
        instance.setTemplateActionNextId(template.getActionNextId());
        instance.setTemplateType(null); // generated instances are not templates
        instance.setNextActionType(template.getNextActionType());
        instance.setNextDescription(n(template.getNextDescription()));
        instance.setNextTimeEstimate(
                template.getNextTimeEstimate() == null ? 0 : template.getNextTimeEstimate());
        instance.setGamePoints(template.getGamePoints());
        instance.setTimeSlot(template.getTimeSlot());
        instance.setLinkUrl(n(template.getLinkUrl()));
        instance.setNextNotes(n(template.getNextNotes()));
        instance.setProcessStage(template.getProcessStage());
        instance.setBillable(template.isBillable());
        instance.setPriorityLevel(template.getPriorityLevel());
        instance.setCompletionOrder(0);
        instance.setRescheduleLocked(rescheduleLocked);
        instance.setNextChangeDate(new Date());
        return instance;
    }

    private void cancelFutureReadyInstances(Session session, int templateActionNextId,
            int workspaceId, int contactId, Date fromDate) {
        Query query = session.createQuery(
                "from ActionNext an where an.workspaceId = :workspaceId "
                        + "and (an.contactId = :contactId or an.nextContactId = :contactId) "
                        + "and an.templateActionNextId = :templateId "
                        + "and an.nextActionDate is not null and an.nextActionDate >= :fromDate "
                        + "and an.nextActionStatusString = :ready");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", contactId);
        query.setParameter("templateId", templateActionNextId);
        query.setParameter("fromDate", fromDate);
        query.setParameter("ready", STATUS_READY);
        @SuppressWarnings("unchecked")
        List<ActionNext> instances = query.list();
        for (ActionNext instance : instances) {
            instance.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
            instance.setNextChangeDate(new Date());
            session.update(instance);
        }
    }

    @SuppressWarnings("unchecked")
    private void cancelInstancesNotMatchingSchedule(Session session, int templateActionNextId,
            int workspaceId, int contactId, LocalDate today, TemplateType type, String pattern) {
        Date todayDate = toUtcDate(today);
        Query query = session.createQuery(
                "from ActionNext an where an.workspaceId = :workspaceId "
                        + "and (an.contactId = :contactId or an.nextContactId = :contactId) "
                        + "and an.templateActionNextId = :templateId "
                        + "and an.nextActionDate is not null and an.nextActionDate >= :today "
                        + "and an.nextActionStatusString = :ready");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("contactId", contactId);
        query.setParameter("templateId", templateActionNextId);
        query.setParameter("today", todayDate);
        query.setParameter("ready", STATUS_READY);
        List<ActionNext> instances = query.list();
        for (ActionNext instance : instances) {
            if (instance.getNextActionDate() == null) {
                continue;
            }
            LocalDate instanceDate = toLocalDate(instance.getNextActionDate());
            if (patternEvaluator.matchingDates(type, pattern, instanceDate, instanceDate).isEmpty()) {
                instance.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
                instance.setNextChangeDate(new Date());
                session.update(instance);
            }
        }
    }

    private void copyTemplateFieldsToInstance(ActionNext template, ActionNext instance,
            boolean rescheduleLocked) {
        instance.setNextActionType(template.getNextActionType());
        instance.setNextDescription(n(template.getNextDescription()));
        instance.setNextTimeEstimate(
                template.getNextTimeEstimate() == null ? 0 : template.getNextTimeEstimate());
        instance.setGamePoints(template.getGamePoints());
        instance.setTimeSlot(template.getTimeSlot());
        instance.setLinkUrl(n(template.getLinkUrl()));
        instance.setProcessStage(template.getProcessStage());
        instance.setRescheduleLocked(rescheduleLocked);
        instance.setNextChangeDate(new Date());
    }

    // =========================================================================
    // Small private helpers
    // =========================================================================

    private boolean hasBillEntries(Session session, int actionNextId) {
        Long count = (Long) session.createQuery(
                "select count(*) from BillEntry where action.actionNextId = :id")
                .setParameter("id", actionNextId)
                .uniqueResult();
        return count != null && count > 0;
    }

    private boolean todayHasActiveInstance(Session session, int templateActionNextId,
            int workspaceId, int contactId, Date todayDate) {
        Long count = (Long) session.createQuery(
                "select count(*) from ActionNext an "
                        + "where an.workspaceId = :workspaceId "
                        + "and (an.contactId = :contactId or an.nextContactId = :contactId) "
                        + "and an.templateActionNextId = :templateId "
                        + "and an.nextActionDate = :today "
                        + "and an.nextActionStatusString <> :cancelled")
                .setParameter("workspaceId", workspaceId)
                .setParameter("contactId", contactId)
                .setParameter("templateId", templateActionNextId)
                .setParameter("today", todayDate)
                .setParameter("cancelled", STATUS_CANCELLED)
                .uniqueResult();
        return count != null && count > 0;
    }

    private String resolvePattern(TemplateType type, ActionNextTemplateConfig config) {
        switch (type) {
            case WEEKLY:
                return config.getScheduleDaysOfWeek();
            case MONTHLY:
                return config.getScheduleDaysOfMonth();
            case QUARTERLY:
                return config.getScheduleDaysOfQuarter();
            case YEARLY:
                return config.getScheduleDaysOfYear();
            default:
                return null; // DAILY — pattern field is not used
        }
    }

    private boolean isRescheduleLocked(ActionNextTemplateConfig config) {
        return config != null && "AUTO_CANCEL".equals(config.getMissedActionBehavior());
    }

    private boolean isSameDay(Date a, Date b) {
        if (a == null || b == null) {
            return false;
        }
        return toLocalDate(a).equals(toLocalDate(b));
    }

    private int workspaceId(ActionNext template) {
        return template.getWorkspaceId() == null ? 0 : template.getWorkspaceId();
    }

    private int contactId(ActionNext template) {
        return template.getContactId();
    }

    static LocalDate toLocalDate(Date date) {
        if (date instanceof java.sql.Date) {
            // java.sql.Date.toLocalDate() is timezone-free — use it directly.
            return ((java.sql.Date) date).toLocalDate();
        }
        // Fall back: interpret the instant in the JVM's local timezone, which is
        // how JDBC will have set the time when reading a DATE column.
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    static Date toUtcDate(LocalDate localDate) {
        // java.sql.Date has no time/timezone component, so JDBC writes the date
        // value directly to a DATE column without any timezone shift.
        return java.sql.Date.valueOf(localDate);
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }
}
