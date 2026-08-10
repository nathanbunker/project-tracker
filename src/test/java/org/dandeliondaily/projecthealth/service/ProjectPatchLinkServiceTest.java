package org.dandeliondaily.projecthealth.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.dashboard.service.ProjectDisplayLabelService;
import org.dandeliondaily.projecthealth.model.ProjectPatchLinkDisplayModel;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.doa.ProjectPatchLinkDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectPatchLink;
import org.openimmunizationsoftware.pt.model.ProjectStatus;

public class ProjectPatchLinkServiceTest {

    @Test
    public void buildLinkDisplayModels_resolvesAllCurrentNonClosedPatchProjects() {
        ProjectPatchLink link = new ProjectPatchLink();
        link.setProjectPatchLinkId(7);
        link.setPrivateProjectId(11);
        link.setPatchWorkspaceId(22);
        link.setLinkType(ProjectPatchLink.LINK_TYPE_PATCH_ALL);

        Project first = project(31, 22, "First");
        Project second = project(32, 22, "Second");
        RecordingSessionHandler handler = new RecordingSessionHandler(
                Collections.singletonList(link), Arrays.asList(first, second));

        List<ProjectPatchLinkDisplayModel> displays = new ProjectPatchLinkService()
                .buildLinkDisplayModels(handler.createSessionProxy(), 11, 22);

        Assert.assertEquals(1, displays.size());
        Assert.assertEquals(ProjectPatchLink.LINK_TYPE_PATCH_ALL, displays.get(0).getLinkType());
        Assert.assertEquals(Arrays.asList(first, second), displays.get(0).getResolvedProjects());

        QueryCall projectQuery = handler.findQuery("from Project p where");
        Assert.assertNotNull(projectQuery);
        Assert.assertTrue(projectQuery.hql.contains("p.projectStatus is null"));
        Assert.assertTrue(projectQuery.hql.contains("p.projectStatus <> :closedStatus"));
        Assert.assertEquals(Integer.valueOf(22), projectQuery.parameters.get("wsId"));
        Assert.assertEquals(ProjectStatus.CLOSED.getDatabaseValue(), projectQuery.parameters.get("closedStatus"));
    }

    @Test
    public void dao_detectsAndDeletesLinksForExclusivePatchAllMode() {
        RecordingSessionHandler handler = new RecordingSessionHandler(
                Collections.<ProjectPatchLink>emptyList(), Collections.<Project>emptyList());
        ProjectPatchLinkDao dao = new ProjectPatchLinkDao(handler.createSessionProxy());

        Assert.assertTrue(dao.patchAllLinkExists(11));
        Assert.assertEquals(2, dao.deleteLinksForProject(11));

        QueryCall existsQuery = handler.findQuery("select count(*) from ProjectPatchLink");
        Assert.assertEquals(Integer.valueOf(11), existsQuery.parameters.get("pid"));
        Assert.assertEquals(ProjectPatchLink.LINK_TYPE_PATCH_ALL, existsQuery.parameters.get("lt"));
        QueryCall deleteQuery = handler.findQuery("delete from ProjectPatchLink");
        Assert.assertEquals(Integer.valueOf(11), deleteQuery.parameters.get("pid"));
    }

    @Test
    public void dashboardResolver_usesPatchAllAndDeduplicatesProjects() {
        ProjectPatchLink link = new ProjectPatchLink();
        link.setPrivateProjectId(11);
        link.setPatchWorkspaceId(22);
        link.setLinkType(ProjectPatchLink.LINK_TYPE_PATCH_ALL);
        Project patchProject = project(31, 22, "Shared");
        RecordingSessionHandler handler = new RecordingSessionHandler(
                Collections.singletonList(link), Arrays.asList(patchProject, patchProject));
        Project privateProject = project(11, 5, "Private");
        privateProject.setLinkedPatchWorkspaceId(Integer.valueOf(22));

        List<Project> resolved = new ProjectDisplayLabelService()
                .resolveLinkedPatchProjects(handler.createSessionProxy(), privateProject);

        Assert.assertEquals(Collections.singletonList(patchProject), resolved);
        Assert.assertNotNull(handler.findQuery("from Project p where"));
    }

    private Project project(int projectId, int workspaceId, String projectName) {
        Project project = new Project();
        project.setProjectId(projectId);
        project.setWorkspaceId(Integer.valueOf(workspaceId));
        project.setProjectName(projectName);
        return project;
    }

    private static final class QueryCall {
        private final String hql;
        private final Map<String, Object> parameters = new LinkedHashMap<String, Object>();

        private QueryCall(String hql) {
            this.hql = hql;
        }
    }

    private static final class RecordingSessionHandler implements InvocationHandler {
        private final List<ProjectPatchLink> links;
        private final List<Project> projects;
        private final List<QueryCall> queryCalls = new ArrayList<QueryCall>();

        private RecordingSessionHandler(List<ProjectPatchLink> links, List<Project> projects) {
            this.links = links;
            this.projects = projects;
        }

        private Session createSessionProxy() {
            return (Session) Proxy.newProxyInstance(
                    Session.class.getClassLoader(), new Class<?>[] { Session.class }, this);
        }

        private QueryCall findQuery(String prefix) {
            for (QueryCall queryCall : queryCalls) {
                if (queryCall.hql.startsWith(prefix)) {
                    return queryCall;
                }
            }
            return null;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("createQuery".equals(method.getName())) {
                QueryCall queryCall = new QueryCall((String) args[0]);
                queryCalls.add(queryCall);
                return Proxy.newProxyInstance(Query.class.getClassLoader(), new Class<?>[] { Query.class },
                        new RecordingQueryHandler(queryCall));
            }
            if ("toString".equals(method.getName())) {
                return "RecordingSessionProxy";
            }
            return defaultValue(method.getReturnType());
        }

        private final class RecordingQueryHandler implements InvocationHandler {
            private final QueryCall queryCall;

            private RecordingQueryHandler(QueryCall queryCall) {
                this.queryCall = queryCall;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("setParameter".equals(method.getName())) {
                    queryCall.parameters.put((String) args[0], args[1]);
                    return proxy;
                }
                if ("list".equals(method.getName())) {
                    if (queryCall.hql.startsWith("from ProjectPatchLink")) {
                        return links;
                    }
                    if (queryCall.hql.startsWith("from Project p where")) {
                        return projects;
                    }
                    return Collections.emptyList();
                }
                if ("uniqueResult".equals(method.getName())) {
                    return Long.valueOf(1L);
                }
                if ("executeUpdate".equals(method.getName())) {
                    return Integer.valueOf(2);
                }
                if ("toString".equals(method.getName())) {
                    return "RecordingQueryProxy";
                }
                return defaultValue(method.getReturnType());
            }
        }

        private static Object defaultValue(Class<?> returnType) {
            if (!returnType.isPrimitive()) {
                return null;
            }
            if (returnType == boolean.class) {
                return Boolean.FALSE;
            }
            if (returnType == int.class) {
                return Integer.valueOf(0);
            }
            if (returnType == long.class) {
                return Long.valueOf(0L);
            }
            if (returnType == double.class) {
                return Double.valueOf(0D);
            }
            if (returnType == float.class) {
                return Float.valueOf(0F);
            }
            if (returnType == short.class) {
                return Short.valueOf((short) 0);
            }
            if (returnType == byte.class) {
                return Byte.valueOf((byte) 0);
            }
            if (returnType == char.class) {
                return Character.valueOf('\0');
            }
            return null;
        }
    }
}