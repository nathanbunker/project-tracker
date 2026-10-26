package org.dandeliondaily.mcp.service;

import java.util.Date;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectTag;
import org.openimmunizationsoftware.pt.model.ProjectTagMap;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.Workspace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class McpProjectMutationServiceTest {

    @Test
    public void onlyActiveAndPausedProjectsAreOpenMoveDestinations() {
        Project project = new Project();
        project.setProjectStatus(ProjectStatus.ACTIVE.getDatabaseValue());
        Assert.assertTrue(McpProjectMutationService.isOpenProject(project));
        project.setProjectStatus(ProjectStatus.PAUSED.getDatabaseValue());
        Assert.assertTrue(McpProjectMutationService.isOpenProject(project));
        project.setProjectStatus(ProjectStatus.COMPLETE.getDatabaseValue());
        Assert.assertFalse(McpProjectMutationService.isOpenProject(project));
        project.setProjectStatus(ProjectStatus.CLOSED.getDatabaseValue());
        Assert.assertFalse(McpProjectMutationService.isOpenProject(project));
    }

    @Test
    public void projectNamesAndHandlesCompareWithoutCaseOrSurroundingWhitespace() {
        Assert.assertTrue(McpProjectMutationService.sameKey("  Alpha  ", "alpha"));
        Assert.assertFalse(McpProjectMutationService.sameKey("Alpha", "Alpha II"));
    }

    @Test
    public void rejectsOrderAndOtherActionMutationsThatAlsoTargetMovedAction() {
        ActionNext moved = new ActionNext();
        moved.setActionNextId(77);
        ActionNext other = new ActionNext();
        other.setActionNextId(88);
        Assert.assertTrue(McpProjectMutationService.conflictsWithMove(77, "order_day", null,
                Collections.singletonList(moved)));
        Assert.assertFalse(McpProjectMutationService.conflictsWithMove(77, "order_day", null,
                Collections.singletonList(other)));
        Assert.assertTrue(McpProjectMutationService.conflictsWithMove(77, "complete_action", Integer.valueOf(77),
                null));
        Assert.assertFalse(McpProjectMutationService.conflictsWithMove(77, "complete_action", Integer.valueOf(88),
                null));
    }

    @Test
    public void movedActionAsOfIsStrictlyLaterThanExistingValue() {
        Date previous = new Date(System.currentTimeMillis() + 120000L);
        Date next = McpProjectMutationService.monotonicChangeDate(previous);
        Assert.assertEquals(previous.getTime() + 1000L, next.getTime());
    }

    @Test
    public void moveUsesActualBillableDestinationCode() {
        verifyMoveBillability("Y", true);
    }

    @Test
    public void moveToNonbillableDestinationClearsPreviousBillableFlag() {
        verifyMoveBillability("N", false);
    }

    private void verifyMoveBillability(String billable, boolean expected) {
        BillCode code = new BillCode();
        code.setWorkspaceId(Integer.valueOf(5));
        code.setBillCode("OPS");
        code.setBillable(billable);
        Project destination = new Project();
        destination.setWorkspaceId(Integer.valueOf(5));
        destination.setProjectId(77);
        destination.setBillCode("OPS");
        ActionNext action = new ActionNext();
        action.setActionNextId(55);
        action.setProjectId(10);
        action.setBillable(!expected);
        Date changed = new Date(10000L);
        Session session = session(Collections.singletonList(code), Collections.<ProjectTag>emptyList(),
                Collections.<Project>emptyList(), new ArrayList<Object>());

        new McpProjectMutationService().applyMove(session, action, destination, changed);

        Assert.assertEquals(expected, action.isBillable());
        Assert.assertEquals(55, action.getActionNextId());
        Assert.assertEquals(77, action.getProjectId());
        Assert.assertSame(destination, action.getProject());
        Assert.assertEquals(changed, action.getNextChangeDate());
    }

    @Test
    public void createsPrivateProjectWithVerifiedBillCodeTagsAndOwnerAssignment() throws Exception {
        List<Object> saved = new ArrayList<Object>();
        BillCode billCode = new BillCode();
        billCode.setWorkspaceId(Integer.valueOf(5));
        billCode.setBillCode("OPS");
        ProjectTag tag = new ProjectTag();
        tag.setProjectTagId(22);
        tag.setWorkspaceId(5);
        tag.setTagName("Operations");
        tag.setTagHandle("Operations");
        tag.setTagStatus(ProjectTag.STATUS_ACTIVE);
        Session session = session(Collections.singletonList(billCode), Collections.singletonList(tag),
                Collections.<Project>emptyList(), saved);
        WebUser user = user();
        JsonNode item = new ObjectMapper().readTree("{\"type\":\"create_project\",\"ref\":\"repo\","
                + "\"name\":\"Repository\",\"description\":\"New project\",\"billCode\":\"OPS\","
                + "\"tags\":[\"operations\"],\"currentFocus\":\"Initial work\"}");

        McpProjectMutationService service = new McpProjectMutationService();
        McpProjectMutationService.ProjectDraft draft = service.prepareProject(session, 5, user, item,
                Collections.<McpProjectMutationService.ProjectDraft>emptyList());

        Assert.assertNull(draft.error);
        Assert.assertEquals("Repository", draft.project.getProjectName());
        Assert.assertEquals("Repository", draft.project.getProjectHandle());
        Assert.assertEquals("OPS", draft.project.getBillCode());
        Assert.assertFalse(draft.project.isExternalManaged());
        Assert.assertEquals("Initial work", draft.project.getCurrentFocusText());
        Assert.assertTrue(saved.isEmpty());

        service.applyProject(session, user, draft);
        Assert.assertEquals(3, saved.size());
        Assert.assertTrue(saved.get(1) instanceof ProjectContactAssigned);
        Assert.assertTrue(saved.get(2) instanceof ProjectTagMap);
        Assert.assertEquals(22, ((ProjectTagMap) saved.get(2)).getProjectTagId());
    }

    @Test
    public void rejectsUnknownBillCodeAndTagsBeforeAnyProjectWrite() throws Exception {
        List<Object> saved = new ArrayList<Object>();
        Session noBillCode = session(Collections.<BillCode>emptyList(), Collections.<ProjectTag>emptyList(),
                Collections.<Project>emptyList(), saved);
        JsonNode item = new ObjectMapper().readTree("{\"type\":\"create_project\",\"ref\":\"repo\","
                + "\"name\":\"Repository\",\"billCode\":\"MISSING\"}");
        McpProjectMutationService service = new McpProjectMutationService();
        McpProjectMutationService.ProjectDraft missingCode = service.prepareProject(noBillCode, 5, user(), item,
                Collections.<McpProjectMutationService.ProjectDraft>emptyList());
        Assert.assertNotNull(missingCode.error);
        Assert.assertTrue(saved.isEmpty());

        BillCode validBillCode = new BillCode();
        validBillCode.setWorkspaceId(Integer.valueOf(5));
        validBillCode.setBillCode("OPS");
        Session noTag = session(Collections.singletonList(validBillCode), Collections.<ProjectTag>emptyList(),
                Collections.<Project>emptyList(), saved);
        JsonNode withUnknownTag = new ObjectMapper().readTree("{\"type\":\"create_project\",\"ref\":\"repo\","
                + "\"name\":\"Repository\",\"billCode\":\"OPS\",\"tags\":[\"unknown\"]}");
        McpProjectMutationService.ProjectDraft missingTag = service.prepareProject(noTag, 5, user(), withUnknownTag,
                Collections.<McpProjectMutationService.ProjectDraft>emptyList());
        Assert.assertNotNull(missingTag.error);
        Assert.assertTrue(saved.isEmpty());
    }

    @Test
    public void createProjectBatchDoesNotInsertAnActionAuditWithZeroForeignKeys() throws Exception {
        List<Object> saved = new ArrayList<Object>();
        BillCode billCode = new BillCode();
        billCode.setWorkspaceId(Integer.valueOf(5));
        billCode.setBillCode("OPS");
        Session session = session(Collections.singletonList(billCode), Collections.<ProjectTag>emptyList(),
                Collections.<Project>emptyList(), saved);
        JsonNode create = new ObjectMapper().readTree("{\"type\":\"create_project\",\"ref\":\"repo\","
                + "\"name\":\"Repository\",\"billCode\":\"OPS\"}");

        // Exercise applyChanges, including audit dispatch, rather than only the
        // creation helper. There is intentionally no action to reference here.
        Map<String, Object> result = new McpApplyChangesService().applyChanges(session, 5, user(), "test",
                Collections.singletonList(create));

        Assert.assertEquals(Boolean.TRUE, result.get("applied"));
        Assert.assertEquals(2, saved.size());
        Assert.assertTrue(saved.get(0) instanceof Project);
        Assert.assertTrue(saved.get(1) instanceof ProjectContactAssigned);
        Assert.assertEquals(Integer.valueOf(99), ((Project) saved.get(0)).getCreatedByWebUserId());
        List<?> results = (List<?>) result.get("results");
        Map<?, ?> created = (Map<?, ?>) results.get(0);
        Assert.assertEquals("repo", created.get("ref"));
        Assert.assertEquals(Integer.valueOf(321), created.get("projectId"));
    }

    private Session session(final List<BillCode> billCodes, final List<ProjectTag> tags,
            final List<Project> visibleProjects, final List<Object> saved) {
        final Workspace workspace = new Workspace();
        workspace.setWorkspaceId(5);
        workspace.setWorkspaceType(Workspace.TYPE_PRIVATE);
        workspace.setWorkspaceStatus(Workspace.STATUS_ACTIVE);
        final Query[] currentQuery = new Query[1];
        final Session[] session = new Session[1];
        session[0] = proxy(Session.class, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("get".equals(name)) {
                    return args[0] == Workspace.class ? workspace : null;
                }
                if ("createQuery".equals(name)) {
                    currentQuery[0] = query(String.valueOf(args[0]), billCodes, tags, visibleProjects);
                    return currentQuery[0];
                }
                if ("save".equals(name)) {
                    Object object = args[0];
                    if (object instanceof Project) {
                        ((Project) object).setProjectId(321);
                    }
                    saved.add(object);
                    return Integer.valueOf(saved.size());
                }
                return defaultValue(method.getReturnType());
            }
        });
        return session[0];
    }

    private Query query(final String hql, final List<BillCode> billCodes, final List<ProjectTag> tags,
            final List<Project> visibleProjects) {
        final Query[] query = new Query[1];
        query[0] = proxy(Query.class, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("list".equals(name)) {
                    if (hql.startsWith("from BillCode")) {
                        return billCodes;
                    }
                    if (hql.startsWith("from ProjectTag")) {
                        return tags;
                    }
                    if (hql.startsWith("from Project p")) {
                        return visibleProjects;
                    }
                    return Collections.emptyList();
                }
                if ("uniqueResult".equals(name)) {
                    return hql.startsWith("select count(*) from WorkspaceMember") ? Integer.valueOf(1) : null;
                }
                if (Query.class.isAssignableFrom(method.getReturnType())) {
                    return query[0];
                }
                return defaultValue(method.getReturnType());
            }
        });
        return query[0];
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (type == Integer.TYPE) {
            return Integer.valueOf(0);
        }
        if (type == Long.TYPE) {
            return Long.valueOf(0);
        }
        return null;
    }

    private static WebUser user() {
        WebUser user = new WebUser();
        user.setWebUserId(99);
        user.setContactId(88);
        return user;
    }
}
