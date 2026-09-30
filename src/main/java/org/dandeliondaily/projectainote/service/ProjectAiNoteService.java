package org.dandeliondaily.projectainote.service;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.doa.ProjectAiNoteDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAiNote;

/**
 * Plain CRUD for "AI thoughts on a project" -- independent records, no
 * batch/staleness complexity, no review workflow (see
 * docs/Dandelion_Daily_AI_Integration_Assessment.md section 3, decision 5).
 * Shared by the MCP tools and the Dandelion project-page UI so both paths
 * enforce the same project/workspace scoping.
 */
public class ProjectAiNoteService {

    public List<ProjectAiNote> list(Session session, int projectId) {
        return new ProjectAiNoteDao(session).listForProject(projectId);
    }

    public ProjectAiNote create(Session session, int workspaceId, int projectId, String noteText, String source) {
        Project project = requireProject(session, workspaceId, projectId);
        String validatedText = requireNoteText(noteText);
        ProjectAiNote note = new ProjectAiNote();
        note.setProjectId(project.getProjectId());
        note.setNoteText(validatedText);
        note.setSource(source);
        Date now = new Date();
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        new ProjectAiNoteDao(session).save(note);
        return note;
    }

    public ProjectAiNote update(Session session, int workspaceId, int noteId, String noteText) {
        ProjectAiNoteDao dao = new ProjectAiNoteDao(session);
        ProjectAiNote note = requireNoteInWorkspace(session, dao, workspaceId, noteId);
        note.setNoteText(requireNoteText(noteText));
        note.setUpdatedAt(new Date());
        dao.update(note);
        return note;
    }

    public void delete(Session session, int workspaceId, int noteId) {
        ProjectAiNoteDao dao = new ProjectAiNoteDao(session);
        ProjectAiNote note = requireNoteInWorkspace(session, dao, workspaceId, noteId);
        dao.delete(note);
    }

    private ProjectAiNote requireNoteInWorkspace(Session session, ProjectAiNoteDao dao, int workspaceId,
            int noteId) {
        ProjectAiNote note = dao.getById(noteId);
        if (note == null) {
            throw new IllegalStateException("AI thought not found.");
        }
        Project project = (Project) session.get(Project.class, note.getProjectId());
        if (project == null || project.getWorkspaceId() == null
                || project.getWorkspaceId().intValue() != workspaceId) {
            throw new IllegalStateException("AI thought not found.");
        }
        return note;
    }

    private Project requireProject(Session session, int workspaceId, int projectId) {
        Query query = session.createQuery(
                "from Project p where p.projectId = :projectId and p.workspaceId = :workspaceId");
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        Project project = (Project) query.uniqueResult();
        if (project == null) {
            throw new IllegalStateException("Project not found.");
        }
        return project;
    }

    private String requireNoteText(String noteText) {
        if (noteText == null || noteText.trim().length() == 0) {
            throw new IllegalArgumentException("Note text is required.");
        }
        return noteText.trim();
    }
}
