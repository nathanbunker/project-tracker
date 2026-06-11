package org.dandeliondaily.planahead.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.CentralControl;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;

/**
 * Servlet context listener that schedules nightly (and immediate catch-up)
 * generation of template-based action_next instances.
 *
 * Registered in web.xml. Runs independently of any web request; opens and
 * closes its own Hibernate sessions.
 */
public class TemplateSchedulerListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(TemplateSchedulerListener.class.getName());

    /** Held so that {@link #triggerNow()} can submit an on-demand run. */
    private static volatile TemplateSchedulerListener instance;

    private ScheduledExecutorService scheduler;
    private final TemplateGenerationService generationService = new TemplateGenerationService();

    /**
     * Submits an immediate on-demand generation run (e.g. from the admin status
     * page).
     * No-ops silently if the listener has not yet started or has been destroyed.
     */
    public static void triggerNow() {
        TemplateSchedulerListener ref = instance;
        if (ref != null && ref.scheduler != null && !ref.scheduler.isShutdown()) {
            ref.scheduler.submit(() -> ref.runGenerationSafely("manual"));
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "TemplateScheduler");
                t.setDaemon(true);
                return t;
            }
        });

        instance = this;

        // Immediate catch-up run
        scheduler.submit(() -> runGenerationSafely("startup"));

        // Schedule to run every hour, aligned to :15 past the hour
        ZonedDateTime now = ZonedDateTime.now();
        int minuteNow = now.getMinute();
        long delayMinutes = (minuteNow < 15) ? (15 - minuteNow) : (60 - minuteNow + 15);
        long initialDelaySec = delayMinutes * 60L - now.getSecond();
        scheduler.scheduleAtFixedRate(() -> runGenerationSafely("hourly"), initialDelaySec, 3600L,
                TimeUnit.SECONDS);

        LOGGER.log(Level.INFO, "[TemplateScheduler] Scheduled. First hourly run in {0} min.",
                Long.valueOf(delayMinutes));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        instance = null;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    // =========================================================================
    // Core generation run
    // =========================================================================

    private void runGenerationSafely(String trigger) {
        try {
            runGeneration();
        } catch (Throwable t) {
            // Prevent ScheduledExecutorService from suppressing future runs
            // when a task throws.
            LOGGER.log(Level.SEVERE, "[TemplateScheduler] Unhandled error in " + trigger + " run", t);
        }
    }

    private void runGeneration() {
        LOGGER.info("[TemplateScheduler] Running template generation.");
        SchedulerRunRecord record = new SchedulerRunRecord();
        SchedulerStatusHolder.beginRun(record);
        SessionFactory factory = CentralControl.getSessionFactory();
        Session session = null;
        try {
            session = factory.openSession();
            int advanceDays = resolveAdvanceDays(session);

            List<Object[]> pairs = loadWorkspaceContactPairs(session);
            record.setPairsProcessed(pairs.size());
            LOGGER.info("[TemplateScheduler] Processing " + pairs.size() + " workspace/contact pairs.");

            for (Object[] pair : pairs) {
                int workspaceId = ((Number) pair[0]).intValue();
                if (pair[1] == null) {
                    continue;
                }
                int contactId = ((Number) pair[1]).intValue();
                try {
                    int created = processUser(factory, workspaceId, contactId, advanceDays);
                    record.addInstancesGenerated(created);
                    LOGGER.info("[TemplateScheduler] workspace=" + workspaceId
                            + " contact=" + contactId
                            + " created=" + created
                            + " advanceDays=" + advanceDays);
                } catch (Exception e) {
                    record.recordError(e);
                    LOGGER.log(Level.SEVERE,
                            "[TemplateScheduler] Error processing workspace=" + workspaceId
                                    + " contact=" + contactId,
                            e);
                }
            }

            SchedulerRunRecord.Outcome outcome = record.getPairsWithErrors() > 0
                    ? SchedulerRunRecord.Outcome.PARTIAL_ERRORS
                    : (pairs.isEmpty() ? SchedulerRunRecord.Outcome.NO_TEMPLATES : SchedulerRunRecord.Outcome.OK);
            record.finish(outcome);

        } catch (Exception e) {
            record.recordError(e);
            record.finish(SchedulerRunRecord.Outcome.FATAL);
            LOGGER.log(Level.SEVERE, "[TemplateScheduler] Fatal error during generation run", e);
        } finally {
            if (session != null) {
                session.close();
            }
            SchedulerStatusHolder.completeRun(record);
        }
    }

    /**
     * Runs missed-action behavior + forward generation for one workspace/contact
     * pair.
     */
    private int processUser(SessionFactory factory, int workspaceId, int contactId, int advanceDays) {
        Session session = factory.openSession();
        Transaction transaction = null;
        try {
            String username = resolveUsername(session, contactId);
            TimeZone userTz = resolveUserTimezone(session, username);
            LocalDate today = LocalDate.now(userTz.toZoneId());

            transaction = session.beginTransaction();
            generationService.applyMissedActionBehavior(session, workspaceId, contactId, today);
            int created = generationService.generateForwardWindow(session, workspaceId, contactId, today, advanceDays);
            generationService.cleanupPastActualTimes(session, workspaceId, contactId, today);
            transaction.commit();
            return created;
        } catch (RuntimeException re) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw re;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    @SuppressWarnings("unchecked")
    private List<Object[]> loadWorkspaceContactPairs(Session session) {
        Query query = session.createQuery(
                "select distinct an.workspaceId, coalesce(an.contactId, an.nextContactId) from ActionNext an "
                        + "where an.templateTypeString is not null and an.templateTypeString <> '' "
                        + "and (an.contactId is not null or an.nextContactId is not null) "
                        + "and (an.templateActionNextId is null or an.templateActionNextId = 0) "
                        + "and an.nextActionStatusString <> :cancelled");
        query.setParameter("cancelled", "X");
        return query.list();
    }

    private int resolveAdvanceDays(Session session) {
        String value = TrackerKeysManager.getKeyValue(
                TrackerKeysManager.KEY_TEMPLATE_ADVANCE_DAYS,
                TrackerKeysManager.KEY_TYPE_GLOBAL,
                TrackerKeysManager.KEY_ID_GLOBAL,
                "14",
                session);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 14;
        }
    }

    private String resolveUsername(Session session, int contactId) {
        Query query = session.createQuery(
                "select wu.username from WebUser wu where wu.contactId = :contactId");
        query.setParameter("contactId", contactId);
        query.setMaxResults(1);
        String username = (String) query.uniqueResult();
        return username != null ? username : "";
    }

    private TimeZone resolveUserTimezone(Session session, String username) {
        if (username == null || username.isEmpty()) {
            return TimeZone.getTimeZone(ZoneId.of("America/Denver"));
        }
        String tzName = TrackerKeysManager.getKeyValue(
                TrackerKeysManager.KEY_TIME_ZONE,
                TrackerKeysManager.KEY_TYPE_USER,
                username,
                "America/Denver",
                session);
        try {
            return TimeZone.getTimeZone(ZoneId.of(tzName));
        } catch (Exception e) {
            return TimeZone.getTimeZone(ZoneId.of("America/Denver"));
        }
    }
}
