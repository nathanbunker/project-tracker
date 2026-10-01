package org.dandeliondaily.mcp.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ReviewInterval;

/**
 * update_project_review_cadence (I-7): moves a project between the same
 * ReviewInterval groupings the Project Health page uses, by writing
 * ProjectContactAssigned.updateDue -- mirrors the "updateEvery" day-count
 * ProjectEditServlet already writes for this field, so both paths produce
 * the same value.
 */
public class McpProjectReviewCadenceService {

    public Map<String, Object> updateCadence(Session session, int workspaceId, int contactId, int projectId,
            String reviewIntervalName) {
        Project project = requireProjectInWorkspace(session, workspaceId, projectId);
        ProjectContactAssigned assigned = findAssignment(session, contactId, projectId);
        if (assigned == null) {
            throw new McpToolException("not_found",
                    "This account is not assigned to project " + projectId + "; cadence can only be set for "
                            + "projects you're assigned to.");
        }

        int newDays = toDays(reviewIntervalName);
        Integer previousDays = assigned.getUpdateDue();
        assigned.setUpdateDue(Integer.valueOf(newDays));
        session.update(assigned);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("projectId", project.getProjectId());
        result.put("reviewInterval", reviewIntervalName.toUpperCase());
        result.put("days", Integer.valueOf(newDays));
        result.put("label", ReviewInterval.makeLabel(newDays));
        result.put("previousDays", previousDays);
        return result;
    }

    private int toDays(String reviewIntervalName) {
        if (reviewIntervalName == null || reviewIntervalName.trim().length() == 0) {
            throw new McpToolException("invalid_arguments", "\"reviewInterval\" is required.");
        }
        String normalized = reviewIntervalName.trim().toUpperCase();
        if ("NONE".equals(normalized)) {
            return 0;
        }
        try {
            return ReviewInterval.valueOf(normalized).getDays();
        } catch (IllegalArgumentException invalid) {
            throw new McpToolException("invalid_arguments",
                    "\"reviewInterval\" must be one of NONE, WEEK, TWO_WEEKS, MONTH, TWO_MONTHS, FOUR_MONTHS, YEAR.");
        }
    }

    private ProjectContactAssigned findAssignment(Session session, int contactId, int projectId) {
        Query query = session.createQuery(
                "from ProjectContactAssigned where id.contactId = :contactId and id.projectId = :projectId");
        query.setInteger("contactId", contactId);
        query.setInteger("projectId", projectId);
        return (ProjectContactAssigned) query.uniqueResult();
    }

    private Project requireProjectInWorkspace(Session session, int workspaceId, int projectId) {
        Query query = session.createQuery(
                "from Project p where p.projectId = :projectId and p.workspaceId = :workspaceId");
        query.setInteger("projectId", projectId);
        query.setInteger("workspaceId", workspaceId);
        Project project = (Project) query.uniqueResult();
        if (project == null) {
            throw new McpToolException("not_found", "Project not found.");
        }
        return project;
    }
}
