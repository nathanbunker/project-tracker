package org.openimmunizationsoftware.pt.doa;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.ProjectAiNote;

public class ProjectAiNoteDao {

    private final Session session;

    public ProjectAiNoteDao(Session session) {
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    public List<ProjectAiNote> listForProject(int projectId) {
        Query query = session.createQuery(
                "from ProjectAiNote where projectId = :projectId order by createdAt desc");
        query.setInteger("projectId", projectId);
        return query.list();
    }

    public ProjectAiNote getById(int noteId) {
        return (ProjectAiNote) session.get(ProjectAiNote.class, noteId);
    }

    public void save(ProjectAiNote note) {
        session.save(note);
    }

    public void update(ProjectAiNote note) {
        session.update(note);
    }

    public void delete(ProjectAiNote note) {
        session.delete(note);
    }
}
