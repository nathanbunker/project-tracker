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

    private ScheduledExecutorService scheduler;
    private final TemplateGenerationService generationService = new TemplateGenerationService();

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

        // Immediate catch-up run
        scheduler.submit(this::runGeneration);

        // Schedule to run every hour, aligned to :15 past the hour
        ZonedDateTime now = ZonedDateTime.now();
        int minuteNow = now.getMinute();
        long delayMinutes = (minuteNow < 15) ? (15 - minuteNow) : (60 - minuteNow + 15);
        long initialDelaySec = delayMinutes * 60L - now.getSecond();
        scheduler.scheduleAtFixedRate(this::runGeneration, initialDelaySec, 3600L, TimeUnit.SECONDS);

        System.out.println("[TemplateScheduler] Scheduled. First hourly run in " + delayMinutes + " min.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    // =========================================================================
    // Core generation run
    // =========================================================================

    private void runGeneration() {
        System.out.println("[TemplateScheduler] Running template generation.");
        SessionFactory factory = CentralControl.getSessionFactory();
        Session session = factory.openSession();
        try {
            // Look up global advance-days setting once per run
            int advanceDays = resolveAdvanceDays(session);

            // Find all distinct (workspaceId, contactId) pairs that own active templates
            List<Object[]> pairs = loadWorkspaceContactPairs(session);
            System.out.println("[TemplateScheduler] Processing " + pairs.size() + " workspace/contact pairs.");

            for (Object[] pair : pairs) {
                int workspaceId = ((Number) pair[0]).intValue();
                int contactId = ((Number) pair[1]).intValue();
                try {
                    processUser(factory, workspaceId, contactId, advanceDays);
                } catch (Exception e) {
                    System.err.println("[TemplateScheduler] Error processing workspace=" + workspaceId
                            + " contact=" + contactId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[TemplateScheduler] Fatal error during generation run: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    /**
     * Runs missed-action behavior + forward generation for one workspace/contact
     * pair.
     */
    private void processUser(SessionFactory factory, int workspaceId, int contactId, int advanceDays) {
        Session session = factory.openSession();
        Transaction transaction = null;
        try {
            String username = resolveUsername(session, contactId);
            TimeZone userTz = resolveUserTimezone(session, username);
            LocalDate today = LocalDate.now(userTz.toZoneId());

            transaction = session.beginTransaction();
            generationService.applyMissedActionBehavior(session, workspaceId, contactId, today);
            generationService.generateForwardWindow(session, workspaceId, contactId, today, advanceDays);
            transaction.commit();
        } catch (RuntimeException re) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw re;
        } finally {
            session.close();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    @SuppressWarnings("unchecked")
    private List<Object[]> loadWorkspaceContactPairs(Session session) {
        Query query = session.createQuery(
                "select distinct an.workspaceId, an.contactId from ActionNext an "
                        + "where an.templateTypeString is not null and an.templateTypeString <> '' "
                        + "and an.templateActionNextId is null "
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
