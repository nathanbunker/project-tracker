package org.dandeliondaily.projecthealth.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.doa.ProjectFactValueDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectFactDefinition;
import org.openimmunizationsoftware.pt.model.ProjectFactValue;

public class ProjectFactValueService {

    public static class ProjectFactValueUpdate {
        private String valueBoolean;
        private String valueText;
        private Date valueDate;
        private BigDecimal valueNumber;
        private String valueCode;
        private String notes;

        public String getValueBoolean() {
            return valueBoolean;
        }

        public void setValueBoolean(String valueBoolean) {
            this.valueBoolean = valueBoolean;
        }

        public String getValueText() {
            return valueText;
        }

        public void setValueText(String valueText) {
            this.valueText = valueText;
        }

        public Date getValueDate() {
            return valueDate;
        }

        public void setValueDate(Date valueDate) {
            this.valueDate = valueDate;
        }

        public BigDecimal getValueNumber() {
            return valueNumber;
        }

        public void setValueNumber(BigDecimal valueNumber) {
            this.valueNumber = valueNumber;
        }

        public String getValueCode() {
            return valueCode;
        }

        public void setValueCode(String valueCode) {
            this.valueCode = valueCode;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public ProjectFactValue getFactValue(Session session, int projectId, int projectFactDefinitionId) {
        validateProjectAndDefinitionWorkspace(session, projectId, projectFactDefinitionId);
        ProjectFactValueDao dao = new ProjectFactValueDao(session);
        return dao.getByProjectAndFactDefinition(projectId, projectFactDefinitionId);
    }

    public List<ProjectFactValue> listFactValuesForProject(Session session, int projectId) {
        requireProject(session, projectId);
        ProjectFactValueDao dao = new ProjectFactValueDao(session);
        return dao.listByProjectId(projectId);
    }

    public List<Object[]> listFactDefinitionsWithValuesForProject(Session session, int projectId,
            boolean includeInactiveDefinitions) {
        Project project = requireProject(session, projectId);
        if (project.getWorkspaceId() == null) {
            throw new IllegalArgumentException("Project workspace is required.");
        }
        ProjectFactValueDao dao = new ProjectFactValueDao(session);
        return dao.listByProjectWithDefinitions(project.getWorkspaceId().intValue(), projectId,
                includeInactiveDefinitions);
    }

    public ProjectFactValue setFactValue(Session session, int projectId, int projectFactDefinitionId,
            ProjectFactValueUpdate update, Integer webUserId, Date now) {
        validateProjectAndDefinitionWorkspace(session, projectId, projectFactDefinitionId);
        ProjectFactValueDao dao = new ProjectFactValueDao(session);

        if (update == null || !hasAnyValue(update)) {
            dao.clearByProjectAndFactDefinition(projectId, projectFactDefinitionId);
            return null;
        }

        ProjectFactValue value = new ProjectFactValue();
        value.setValueBoolean(normalizeBoolean(update.getValueBoolean()));
        value.setValueText(trimToNull(update.getValueText()));
        value.setValueDate(update.getValueDate());
        value.setValueNumber(update.getValueNumber());
        value.setValueCode(trimToNull(update.getValueCode()));
        value.setNotes(trimToNull(update.getNotes()));

        return dao.upsertByProjectAndFactDefinition(projectId, projectFactDefinitionId, value, webUserId,
                now == null ? new Date() : now);
    }

    public void clearFactValue(Session session, int projectId, int projectFactDefinitionId) {
        validateProjectAndDefinitionWorkspace(session, projectId, projectFactDefinitionId);
        ProjectFactValueDao dao = new ProjectFactValueDao(session);
        dao.clearByProjectAndFactDefinition(projectId, projectFactDefinitionId);
    }

    private void validateProjectAndDefinitionWorkspace(Session session, int projectId, int projectFactDefinitionId) {
        Project project = requireProject(session, projectId);
        ProjectFactDefinition definition = requireDefinition(session, projectFactDefinitionId);

        if (project.getWorkspaceId() == null) {
            throw new IllegalArgumentException("Project workspace is required.");
        }
        if (project.getWorkspaceId().intValue() != definition.getWorkspaceId()) {
            throw new IllegalArgumentException(
                    "Project and fact definition must belong to the same workspace.");
        }
    }

    private Project requireProject(Session session, int projectId) {
        Project project = (Project) session.get(Project.class, projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project not found.");
        }
        return project;
    }

    private ProjectFactDefinition requireDefinition(Session session, int projectFactDefinitionId) {
        ProjectFactDefinition definition = (ProjectFactDefinition) session.get(ProjectFactDefinition.class,
                projectFactDefinitionId);
        if (definition == null) {
            throw new IllegalArgumentException("Fact definition not found.");
        }
        return definition;
    }

    private String normalizeBoolean(String valueBoolean) {
        String normalized = trimToNull(valueBoolean);
        if (normalized == null) {
            return null;
        }
        if (ProjectFactValue.BOOLEAN_YES.equalsIgnoreCase(normalized)) {
            return ProjectFactValue.BOOLEAN_YES;
        }
        if (ProjectFactValue.BOOLEAN_NO.equalsIgnoreCase(normalized)) {
            return ProjectFactValue.BOOLEAN_NO;
        }
        throw new IllegalArgumentException("valueBoolean must be Y or N.");
    }

    private boolean hasAnyValue(ProjectFactValueUpdate update) {
        return trimToNull(update.getValueBoolean()) != null
                || trimToNull(update.getValueText()) != null
                || update.getValueDate() != null
                || update.getValueNumber() != null
                || trimToNull(update.getValueCode()) != null
                || trimToNull(update.getNotes()) != null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
