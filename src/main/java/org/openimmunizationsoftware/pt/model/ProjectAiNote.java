package org.openimmunizationsoftware.pt.model;

import java.io.Serializable;
import java.util.Date;

/**
 * An AI-authored observation, question, or idea about a project that should
 * persist across sessions -- distinct from ProjectNarrative (established
 * facts/decisions the user made) and from the project-health fact system.
 * See docs/Dandelion_Daily_AI_Integration_Assessment.md section 3, decision 5.
 */
public class ProjectAiNote implements Serializable {
    private static final long serialVersionUID = 1L;

    private int noteId;
    private int projectId;
    private String noteText;
    private String source;
    private Date createdAt;
    private Date updatedAt;

    public int getNoteId() {
        return noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
