package org.openimmunizationsoftware.pt.doa;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.ProjectPatchLink;

public class ProjectPatchLinkDao {

    private final Session session;

    public ProjectPatchLinkDao(Session session) {
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    public List<ProjectPatchLink> listLinksForProject(int privateProjectId) {
        Query query = session.createQuery(
                "from ProjectPatchLink where privateProjectId = :pid order by projectPatchLinkId");
        query.setParameter("pid", privateProjectId);
        return query.list();
    }

    public boolean hasLinksForProject(int privateProjectId) {
        Query query = session.createQuery(
                "select count(*) from ProjectPatchLink where privateProjectId = :pid");
        query.setParameter("pid", privateProjectId);
        Number count = (Number) query.uniqueResult();
        return count != null && count.intValue() > 0;
    }

    public boolean directLinkExists(int privateProjectId, int patchProjectId) {
        Query query = session.createQuery(
                "select count(*) from ProjectPatchLink where privateProjectId = :pid"
                        + " and linkType = :lt and linkedPatchProjectId = :ppid");
        query.setParameter("pid", privateProjectId);
        query.setParameter("lt", ProjectPatchLink.LINK_TYPE_DIRECT_PROJECT);
        query.setParameter("ppid", patchProjectId);
        Number count = (Number) query.uniqueResult();
        return count != null && count.intValue() > 0;
    }

    public boolean categoryLinkExists(int privateProjectId, int patchCategoryId) {
        Query query = session.createQuery(
                "select count(*) from ProjectPatchLink where privateProjectId = :pid"
                        + " and linkType = :lt and linkedPatchCategoryId = :pcid");
        query.setParameter("pid", privateProjectId);
        query.setParameter("lt", ProjectPatchLink.LINK_TYPE_PATCH_CATEGORY);
        query.setParameter("pcid", patchCategoryId);
        Number count = (Number) query.uniqueResult();
        return count != null && count.intValue() > 0;
    }

    public ProjectPatchLink getById(int projectPatchLinkId) {
        return (ProjectPatchLink) session.get(ProjectPatchLink.class, projectPatchLinkId);
    }

    public void save(ProjectPatchLink link) {
        session.save(link);
    }

    public void delete(ProjectPatchLink link) {
        session.delete(link);
    }
}
