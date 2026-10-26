package org.dandeliondaily.mcp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dandeliondaily.shared.service.LinkedMeetingService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openimmunizationsoftware.pt.doa.LinkedMeetingDao;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.LinkedMeeting;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;

/** Opt-in: the caller supplies a disposable local schema with v5.12 already applied. */
public class LinkedMeetingPersistenceTest {
    @Test
    public void mysqlRoundTripIdentityPrecisionAndOptimisticVersion() {
        String url = System.getenv("DANDELION_MEETING_TEST_JDBC_URL");
        Assume.assumeTrue("Requires an isolated local linked_meeting_test_* schema.", url != null);
        Assert.assertTrue("Never run this test on the application database.",
                url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/linked_meeting_test_[a-z0-9]+\\?.*"));
        Configuration config = new Configuration();
        config.addResource("org/openimmunizationsoftware/pt/model/LinkedMeeting.hbm.xml");
        config.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
        config.setProperty("hibernate.connection.url", url);
        config.setProperty("hibernate.connection.username", System.getenv("DANDELION_MEETING_TEST_USER"));
        config.setProperty("hibernate.connection.password", System.getenv("MYSQL_PWD"));
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
        config.setProperty("hibernate.show_sql", "false");
        SessionFactory factory = config.buildSessionFactory();
        LinkedMeetingService service = new LinkedMeetingService();
        ActionNext action = new ActionNext();
        action.setActionNextId(1);
        action.setWorkspaceId(1);
        action.setNextActionType(ProjectNextActionType.WILL_MEET);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode change = mapper.createObjectNode();
        ObjectNode snapshot = change.putObject("meeting");
        snapshot.put("sourceSystem", "OUTLOOK_ICS").put("sourceNamespace", "calendar")
                .put("externalEventUid", "CaseSensitiveUid").put("externalOccurrenceId", "original-start")
                .put("originalSubject", "Original").put("scheduledStart", "2026-10-26T09:00:00.123-06:00")
                .put("scheduledEnd", "2026-10-26T10:00:00.456-06:00").put("sourceStatus", "CONFIRMED");
        Session session = factory.openSession();
        try {
            Transaction tx = session.beginTransaction();
            LinkedMeeting meeting = service.apply(session, service.prepare(session, 1, action, change, false), action);
            session.flush();
            Assert.assertEquals(0, meeting.getVersion());
            tx.commit();
            session.clear();
            tx = session.beginTransaction();
            meeting = new LinkedMeetingDao(session).findByIdentity(1, LinkedMeetingService.parseIdentity(snapshot));
            Assert.assertNotNull(meeting);
            Assert.assertEquals("2026-10-26T15:00:00.123Z", LinkedMeetingService.toMap(meeting).get("scheduledStart"));
            Assert.assertEquals("2026-10-26T16:00:00.456Z", LinkedMeetingService.toMap(meeting).get("scheduledEnd"));
            snapshot.put("externalEventUid", "casesensitiveuid");
            Assert.assertNull(new LinkedMeetingDao(session).findByIdentity(1, LinkedMeetingService.parseIdentity(snapshot)));
            snapshot.put("externalEventUid", "CaseSensitiveUid");
            change.put("expectedMeetingVersion", meeting.getVersion());
            snapshot.put("originalSubject", "Renamed");
            service.apply(session, service.prepare(session, 1, action, change, false), action);
            session.flush();
            Assert.assertEquals(1, meeting.getVersion());
            tx.commit();
            session.clear();

            Session stale = factory.openSession();
            try {
                Transaction staleTx = stale.beginTransaction();
                LinkedMeeting old = new LinkedMeetingDao(stale).findByAction(1, 1);
                tx = session.beginTransaction();
                LinkedMeeting current = new LinkedMeetingDao(session).findByAction(1, 1);
                current.setSourceStatus("CANCELLED");
                tx.commit();
                old.setOriginalSubject("Stale overwrite");
                try {
                    stale.flush();
                    Assert.fail("Expected optimistic concurrency failure.");
                } catch (StaleStateException expected) {
                    Assert.assertTrue(expected.getMessage().contains("updated or deleted")
                            || expected.getMessage().contains("row count"));
                } finally {
                    staleTx.rollback();
                }
            } finally {
                stale.close();
            }
        } finally {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            session.close();
            factory.close();
        }
    }
}
