package org.openimmunizationsoftware.pt.api.common;

import static org.junit.Assert.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Assume;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.Project;
import org.dandeliondaily.mcp.service.McpDiscoveryService;
import org.dandeliondaily.mcp.service.McpProjectContextService;
import org.dandeliondaily.mcp.service.McpProjectAccess;
import org.dandeliondaily.mcp.service.McpApplyChangesService;
import org.dandeliondaily.mcp.service.McpActionContextSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Exercises discovery and atomic project writes using real mappings in disposable local MySQL. */
public class McpDiscoveryPersistenceTest {
    @Test
    public void discoveryAndProjectWritesPersistOnMysql() {
        String url = System.getenv("DANDELION_DISCOVERY_TEST_JDBC_URL");
        Assume.assumeTrue("Requires an isolated local mcp_discovery_test_* schema.", url != null);
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/mcp_discovery_test_[a-z0-9]+\\?.*"));
        Configuration config = new Configuration().configure();
        config.setProperty("hibernate.connection.url", url);
        config.setProperty("hibernate.connection.username", System.getenv("DANDELION_MEETING_TEST_USER"));
        config.setProperty("hibernate.connection.password", System.getenv("MYSQL_PWD"));
        config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        config.setProperty("hibernate.connection.provider_class", "org.hibernate.connection.DriverManagerConnectionProvider");
        SessionFactory factory = config.buildSessionFactory();
        Session session = factory.openSession();
        HibernateRequestContext.set(session);
        try {
            session.beginTransaction();
            WebUser user = new WebUser();
            user.setWebUserId(1);
            McpDiscoveryService service = new McpDiscoveryService();
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode args = mapper.createObjectNode().put("nameContains", "test%_").put("tag", "test")
                    .put("scope", "SHARED").put("billCode", "TEST");
            assertEquals(false, service.listProjects(session, 1, user, args).get("hasMore"));
            args = mapper.createObjectNode().put("text", "test%_").put("startDate", "2026-10-01")
                    .put("endDate", "2026-10-31").put("includeNotes", true).put("hasLinkedMeeting", true);
            args.putArray("actionTypes").add("REVIEW");
            args.putArray("projectIds").add(1);
            assertEquals(false, service.searchActions(session, 1, user, args).get("hasMore"));
            args.put("hasLinkedMeeting", false);
            service.searchActions(session, 1, user, args);
            seed(session);
            args = mapper.createObjectNode().put("status", "ALL").put("limit", 1);
            assertEquals(true, service.listProjects(session, 1, user, args).get("hasMore"));
            args.put("scope", "SHARED");
            assertEquals(1, ((java.util.List<?>) service.listProjects(session, 1, user, args).get("projects")).size());
            assertEquals(2, McpProjectAccess.requireReadable(session, 1, user, 2).getProjectId());
            ObjectNode selected = mapper.createObjectNode().put("workspaceId", 2).put("externalSourceKey", "InteropHub-ES");
            assertEquals(1, ((java.util.List<?>) service.listProjects(session, 1, user, selected).get("projects")).size());
            selected.put("workspaceId", 3);
            assertTrue(((java.util.List<?>) service.listProjects(session, 1, user, selected).get("projects")).isEmpty());
            for (int hidden : new int[] {3, 4, 5}) {
                try {
                    McpProjectAccess.requireReadable(session, 1, user, hidden);
                    fail("Unauthorized project must not be visible");
                } catch (org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException expected) {
                    assertEquals("not_found", expected.getCode());
                }
            }
            args = mapper.createObjectNode().put("text", "100%_").put("startDate", "2026-10-01")
                    .put("endDate", "2026-10-31");
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> actions = (java.util.List<java.util.Map<String, Object>>)
                    service.searchActions(session, 1, user, args).get("actions");
            assertEquals(1, actions.size());
            assertEquals(1, actions.get(0).get("actionNextId"));
            assertEquals(true, actions.get(0).get("completionDateKnown"));
            // A batch sibling in another project makes history ambiguous even after an action moves.
            sql(session, "insert into action_next (action_next_id,project_id,workspace_id,next_action_status,"
                    + "next_description,billable,priority_level,completion_order,reschedule_locked,action_set_id) "
                    + "values (2,2,2,'R','Sibling','N',0,0,'N',1)");
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> shared = (java.util.List<java.util.Map<String, Object>>)
                    new McpProjectContextService().getProjectContext(session, 1, 2, user).get("openActions");
            assertEquals(1, shared.size());
            assertFalse(shared.get(0).containsKey("linkedMeeting"));
            assertFalse(shared.get(0).containsKey("sharedWithProjects"));
            assertTrue(((java.util.List<?>) service.searchActions(session, 1, user, args).get("actions")).isEmpty());
            args.put("startDate", "2026-09-01");
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> ambiguous = (java.util.List<java.util.Map<String, Object>>)
                    service.searchActions(session, 1, user, args).get("actions");
            assertEquals(false, ambiguous.get(0).get("completionDateKnown"));
            assertNull(ambiguous.get(0).get("completionDate"));
            verifyProjectWrites(session, mapper);
            session.getTransaction().rollback();
        } finally {
            if (session.getTransaction().isActive()) { session.getTransaction().rollback(); }
            session.close();
            factory.close();
            HibernateRequestContext.clear();
        }
    }

    private void verifyProjectWrites(Session session, ObjectMapper mapper) {
        sql(session, "insert into workspace_member (workspace_id,web_user_id,member_role,membership_status,created_date)"
                + " values (1,1,'OWNER','ACTIVE',now())");
        sql(session, "insert into bill_code (workspace_id,bill_code,billable,visible,estimate_min,bill_rate,bill_round)"
                + " values (1,'TEST','Y','Y',0,0,0)");
        sql(session, "update project set project_status='Active' where project_id=1");
        session.clear();
        sql(session, "insert into action_set (action_set_id,action_set_type,created_by_web_user_id,created_date)"
                + " values (2,'S',1,now())");
        sql(session, "insert into action_next (action_next_id,project_id,workspace_id,contact_id,next_action_status,"
                + "next_description,billable,priority_level,completion_order,reschedule_locked,action_set_id,"
                + "next_change_date,next_action_date,next_action_type,next_time_estimate)"
                + " values (3,1,1,1,'R','Move this meeting','N',1,7,'N',2,now(),'2026-10-26','WILL_MEET',30)");
        sql(session, "insert into linked_meeting (workspace_id,action_next_id,version,source_system,source_namespace,"
                + "external_event_uid,external_occurrence_id,original_subject,scheduled_start_epoch_ms,"
                + "scheduled_end_epoch_ms,source_status,last_synced_epoch_ms)"
                + " values (1,3,0,'TEST','','test-uid','','Original',1000,2000,'CONFIRMED',1000)");
        sql(session, "insert into bill_entry (bill_id,project_id,workspace_id,category_code,web_user_id,start_time,"
                + "end_time,billable,action_next_id) values (1,1,1,'WORK',1,'2026-09-01 09:00:00',"
                + "'2026-09-01 09:30:00','N',3)");
        WebUser owner = (WebUser) session.get(WebUser.class, 1);
        owner.setProjectContact((org.openimmunizationsoftware.pt.model.ProjectContact)
                session.get(org.openimmunizationsoftware.pt.model.ProjectContact.class, 1));
        org.openimmunizationsoftware.pt.model.ActionNext action = (org.openimmunizationsoftware.pt.model.ActionNext)
                session.get(org.openimmunizationsoftware.pt.model.ActionNext.class, 3);
        String oldAsOf = McpActionContextSupport.toIso(action.getNextChangeDate());
        ObjectNode create = mapper.createObjectNode().put("type", "create_project").put("ref", "repository")
                .put("name", "Repository test").put("billCode", "TEST");
        ObjectNode move = mapper.createObjectNode().put("type", "update_action").put("actionNextId", 3)
                .put("asOf", oldAsOf).put("projectRef", "repository");
        ObjectNode createAction = mapper.createObjectNode().put("type", "create_action")
                .put("projectRef", "repository").put("description", "First task").put("nextActionType", "WILL")
                .put("scheduledDate", "2026-10-26").put("estimateMinutes", 15);
        McpApplyChangesService writes = new McpApplyChangesService();
        ObjectNode invalid = createAction.deepCopy().put("projectRef", "unknown");
        java.util.Map<String, Object> rejected = writes.applyChanges(session, 1, owner, "test",
                java.util.Arrays.asList(create, move, invalid));
        assertEquals(false, rejected.get("applied"));
        session.flush();
        assertEquals(0L, ((Number) session.createQuery("select count(*) from Project where projectName='Repository test'")
                .uniqueResult()).longValue());
        assertEquals(1, action.getProjectId());
        java.util.Map<String, Object> result = writes.applyChanges(session, 1, owner, "test",
                java.util.Arrays.asList(create, move, createAction));
        assertEquals(result.toString(), true, result.get("applied"));
        session.flush();
        session.clear();
        action = (org.openimmunizationsoftware.pt.model.ActionNext)
                session.get(org.openimmunizationsoftware.pt.model.ActionNext.class, 3);
        assertNotEquals(1, action.getProjectId());
        assertEquals("Repository test", action.getProject().getProjectName());
        assertTrue(action.isBillable());
        assertEquals(Integer.valueOf(30), action.getNextTimeEstimate());
        assertEquals(7, action.getCompletionOrder());
        assertNotEquals(oldAsOf, McpActionContextSupport.toIso(action.getNextChangeDate()));
        org.openimmunizationsoftware.pt.model.BillEntry history = (org.openimmunizationsoftware.pt.model.BillEntry)
                session.get(org.openimmunizationsoftware.pt.model.BillEntry.class, 1);
        assertEquals(1, history.getProjectId());
        assertEquals(3, history.getAction().getActionNextId());
        org.openimmunizationsoftware.pt.model.LinkedMeeting meeting =
                new org.openimmunizationsoftware.pt.doa.LinkedMeetingDao(session).findByAction(1, 3);
        assertEquals("test-uid", meeting.getExternalEventUid());
        assertEquals(0, meeting.getVersion());
        assertEquals(1L, ((Number) session.createQuery("select count(*) from ProjectContactAssigned where id.projectId=:id")
                .setInteger("id", action.getProjectId()).uniqueResult()).longValue());
        verifyIndependentActionSets(session, mapper, owner, action.getProjectId());
    }

    private void verifyIndependentActionSets(Session session, ObjectMapper mapper, WebUser owner, int projectId) {
        McpApplyChangesService writes = new McpApplyChangesService();
        ObjectNode first = mapper.createObjectNode().put("type", "create_action").put("projectId", projectId)
                .put("description", "Independent meeting first").put("nextActionType", "WILL_MEET")
                .put("scheduledDate", "2026-09-01");
        ObjectNode second = first.deepCopy().put("description", "Independent meeting second");
        java.util.Map<String, Object> batch = writes.applyChanges(session, 1, owner, "test",
                java.util.Arrays.asList(first, second));
        assertEquals(true, batch.get("applied"));
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> results =
                (java.util.List<java.util.Map<String, Object>>) batch.get("results");
        org.openimmunizationsoftware.pt.model.ActionNext a = (org.openimmunizationsoftware.pt.model.ActionNext)
                session.get(org.openimmunizationsoftware.pt.model.ActionNext.class,
                        ((Number) results.get(0).get("createdActionNextId")).intValue());
        org.openimmunizationsoftware.pt.model.ActionNext b = (org.openimmunizationsoftware.pt.model.ActionNext)
                session.get(org.openimmunizationsoftware.pt.model.ActionNext.class,
                        ((Number) results.get(1).get("createdActionNextId")).intValue());
        assertNotEquals(a.getActionSet().getActionSetId(), b.getActionSet().getActionSetId());
        ObjectNode split = mapper.createObjectNode().put("type", "split_action").put("actionNextId", a.getActionNextId())
                .put("asOf", McpActionContextSupport.toIso(a.getNextChangeDate()));
        split.putArray("newActions").addObject().put("description", "Independent meeting split one")
                .put("scheduledDate", "2026-09-01");
        ((com.fasterxml.jackson.databind.node.ArrayNode) split.get("newActions")).addObject()
                .put("description", "Independent meeting split two").put("scheduledDate", "2026-09-01");
        assertEquals(true, writes.applyChanges(session, 1, owner, "test", java.util.Arrays.asList(split)).get("applied"));
        @SuppressWarnings("unchecked")
        java.util.List<org.openimmunizationsoftware.pt.model.ActionNext> replacements = session.createQuery(
                "from ActionNext where nextDescription like 'Independent meeting split%' order by actionNextId").list();
        assertEquals(2, replacements.size());
        java.util.Set<Integer> sets = new java.util.HashSet<Integer>();
        sets.add(a.getActionSet().getActionSetId());
        sets.add(b.getActionSet().getActionSetId());
        for (org.openimmunizationsoftware.pt.model.ActionNext replacement : replacements) {
            assertTrue(sets.add(replacement.getActionSet().getActionSetId()));
        }
        java.util.Date workDate = java.util.Date.from(java.time.Instant.parse("2026-10-20T15:00:00Z"));
        org.dandeliondaily.shared.service.ActionCompletionService completion =
                new org.dandeliondaily.shared.service.ActionCompletionService();
        for (org.openimmunizationsoftware.pt.model.ActionNext completed : java.util.Arrays.asList(b,
                replacements.get(0), replacements.get(1))) {
            completion.applyCompletion(session, owner, completed, completed.getNextDescription(),
                    org.openimmunizationsoftware.pt.model.ProjectNextActionStatus.COMPLETED, workDate, 0);
        }
        session.flush();
        ObjectNode search = mapper.createObjectNode().put("text", "Independent meeting")
                .put("startDate", "2026-10-20").put("endDate", "2026-10-20");
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> found = (java.util.List<java.util.Map<String, Object>>)
                new McpDiscoveryService().searchActions(session, 1, owner, search).get("actions");
        assertEquals(3, found.size());
        for (java.util.Map<String, Object> item : found) {
            assertEquals(true, item.get("completionDateKnown"));
            assertNull(item.get("completionDateUnknownReason"));
            assertNull(item.get("dashboardBucket"));
            assertEquals("2026-09-01", item.get("scheduledDate"));
        }
    }

    private void seed(Session session) {
        sql(session, "insert into project_contact (contact_id,name_last,name_first,email_alert,contact_status,external_managed,"
                + "phone_textable,email_confirmed) values (1,'Test','Discovery','N','ACTIVE','N','N','N')");
        sql(session, "insert into web_user (web_user_id,username,email_verified,contact_id,user_type,workflow_type,"
                + "registration_status,created_date) values (1,'discovery-test','N',1,'USER','NORMAL','ACTIVE',now())");
        sql(session, "insert into workspace (workspace_id,workspace_name,workspace_type,created_by_web_user_id,"
                + "workspace_status,created_date) values (1,'Private','PRIVATE',1,'ACTIVE',now()),"
                + "(2,'Shared','PATCH',1,'ACTIVE',now()),(3,'Foreign','PRIVATE',1,'ACTIVE',now()),"
                + "(4,'Inactive shared','PATCH',1,'INACTIVE',now()),(5,'Not a member','PATCH',1,'ACTIVE',now())");
        sql(session, "insert into workspace_member (workspace_id,web_user_id,member_role,membership_status,created_date)"
                + " values (2,1,'MEMBER','ACTIVE',now()),(3,1,'MEMBER','ACTIVE',now()),(4,1,'MEMBER','ACTIVE',now())");
        sql(session, "insert into project (project_id,project_name,workspace_id,web_user_id,external_managed,priority_level)"
                + " values (1,'Empty private',1,1,'N',0),(2,'Shared',2,1,'Y',0),(3,'Hidden',3,1,'N',0),"
                + "(4,'Hidden inactive',4,1,'Y',0),(5,'Hidden membership',5,1,'Y',0)");
        sql(session, "update project set external_source_key='InteropHub-ES' where project_id=2");
        sql(session, "insert into action_set (action_set_id,action_set_type,created_by_web_user_id,created_date)"
                + " values (1,'S',1,now())");
        sql(session, "insert into action_next (action_next_id,project_id,workspace_id,next_action_status,"
                + "next_description,billable,priority_level,completion_order,reschedule_locked,action_set_id,next_action_date)"
                + " values (1,1,1,'C','Exact 100%_ match','N',0,0,'N',1,'2026-09-01')");
        sql(session, "insert into action_taken (project_id,contact_id,workspace_id,action_date,action_description,action_set_id)"
                + " values (1,1,1,'2026-10-26 15:00:00','Recorded work',1)");
        assertNotNull(session.get(Project.class, 1));
    }

    private void sql(Session session, String statement) {
        session.createSQLQuery(statement).executeUpdate();
    }
}
