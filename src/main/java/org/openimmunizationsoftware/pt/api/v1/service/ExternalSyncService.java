package org.openimmunizationsoftware.pt.api.v1.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.ApiRequestContext;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.SyncAssignmentMutationItem;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.SyncBatchItemResult;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.SyncBatchResponse;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.SyncContactUpsertItem;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.SyncProjectUpsertItem;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.WebUser;

public class ExternalSyncService {

    public static final String OPERATION_ADD = "add";
    public static final String OPERATION_REMOVE = "remove";

    public SyncBatchResponse upsertProjects(ApiRequestContext.ApiClientInfo client, int workspaceId,
            List<SyncProjectUpsertItem> items) {
        SyncBatchResponse response = new SyncBatchResponse();
        response.setTotal(items.size());
        List<SyncBatchItemResult> results = new ArrayList<SyncBatchItemResult>();

        Session session = HibernateRequestContext.getCurrentSession();
        String sourceKey = resolveSourceKey(client);
        WebUser apiUser = findApiUser(session, client.getUsername());

        for (SyncProjectUpsertItem item : items) {
            SyncBatchItemResult result = new SyncBatchItemResult();
            result.setOperation("project_upsert");
            result.setKey(item == null ? null : item.getExternalProjectId());
            try {
                String validationError = validateProjectItem(item);
                if (validationError != null) {
                    error(result, validationError);
                    results.add(result);
                    continue;
                }
                if (apiUser == null) {
                    error(result, "Unable to resolve API user for this key.");
                    results.add(result);
                    continue;
                }
                Project project = findProjectByExternalId(session, workspaceId, sourceKey,
                        item.getExternalProjectId());
                if (project == null) {
                    project = findFirstUnlinkedProjectByName(session, workspaceId, item.getProjectName());
                }
                boolean createMode = project == null;
                if (createMode) {
                    project = new Project();
                    project.setWorkspaceId(Integer.valueOf(workspaceId));
                    project.setWebUser(apiUser);
                    project.setCreatedByWebUserId(Integer.valueOf(apiUser.getWebUserId()));
                }
                project.setLastModifiedByWebUserId(Integer.valueOf(apiUser.getWebUserId()));
                project.setExternalSourceKey(sourceKey);
                project.setExternalProjectId(item.getExternalProjectId());
                project.setExternalManaged(true);
                project.setExternalLastSyncedAt(new Date());

                applyProjectFields(project, item);
                String stateError = validateProjectState(project);
                if (stateError != null) {
                    error(result, stateError);
                    results.add(result);
                    continue;
                }

                session.saveOrUpdate(project);
                result.setProjectId(Integer.valueOf(project.getProjectId()));
                result.setStatus(createMode ? "created" : "updated");
                result.setMessage(createMode ? "Project created." : "Project updated.");
                results.add(result);
            } catch (Exception ex) {
                error(result, rootCauseMessage(ex));
                results.add(result);
                session.clear();
            }
        }

        finalizeResponse(response, results);
        return response;
    }

    public SyncBatchResponse upsertContacts(ApiRequestContext.ApiClientInfo client, int workspaceId,
            List<SyncContactUpsertItem> items) {
        SyncBatchResponse response = new SyncBatchResponse();
        response.setTotal(items.size());
        List<SyncBatchItemResult> results = new ArrayList<SyncBatchItemResult>();

        Session session = HibernateRequestContext.getCurrentSession();
        String sourceKey = resolveSourceKey(client);

        for (SyncContactUpsertItem item : items) {
            SyncBatchItemResult result = new SyncBatchItemResult();
            result.setOperation("contact_upsert");
            result.setKey(item == null ? null : item.getExternalContactId());
            try {
                String validationError = validateContactItem(item);
                if (validationError != null) {
                    error(result, validationError);
                    results.add(result);
                    continue;
                }

                ProjectContact contact = findContactByExternalId(session, workspaceId, sourceKey,
                        item.getExternalContactId());
                if (contact == null && !isEmpty(item.getEmailAddress())) {
                    contact = findFirstUnlinkedContactByEmail(session, workspaceId, item.getEmailAddress());
                }
                boolean createMode = contact == null;
                if (createMode) {
                    contact = new ProjectContact();
                    contact.setWorkspaceId(Integer.valueOf(workspaceId));
                    contact.setEmailAlert("N");
                    contact.setPhoneTextable(false);
                    contact.setEmailConfirmed(false);
                }
                contact.setExternalSourceKey(sourceKey);
                contact.setExternalContactId(item.getExternalContactId());
                contact.setExternalManaged(true);
                contact.setExternalLastSyncedAt(new Date());

                applyContactFields(contact, item);
                String stateError = validateContactState(contact);
                if (stateError != null) {
                    error(result, stateError);
                    results.add(result);
                    continue;
                }

                session.saveOrUpdate(contact);
                result.setContactId(Integer.valueOf(contact.getContactId()));
                result.setStatus(createMode ? "created" : "updated");
                result.setMessage(createMode ? "Contact created." : "Contact updated.");
                results.add(result);
            } catch (Exception ex) {
                error(result, rootCauseMessage(ex));
                results.add(result);
                session.clear();
            }
        }

        finalizeResponse(response, results);
        return response;
    }

    public SyncBatchResponse applyAssignments(ApiRequestContext.ApiClientInfo client, int workspaceId,
            List<SyncAssignmentMutationItem> items) {
        SyncBatchResponse response = new SyncBatchResponse();
        response.setTotal(items.size());
        List<SyncBatchItemResult> results = new ArrayList<SyncBatchItemResult>();

        Session session = HibernateRequestContext.getCurrentSession();
        String sourceKey = resolveSourceKey(client);

        for (SyncAssignmentMutationItem item : items) {
            SyncBatchItemResult result = new SyncBatchItemResult();
            result.setOperation("assignment_apply");
            result.setKey(item == null ? null
                    : item.getExternalProjectId() + "|" + item.getExternalContactId());
            try {
                String validationError = validateAssignmentItem(item);
                if (validationError != null) {
                    error(result, validationError);
                    results.add(result);
                    continue;
                }

                Project project = findProjectByExternalId(session, workspaceId, sourceKey,
                        item.getExternalProjectId());
                if (project == null) {
                    error(result, "Project not found for externalProjectId.");
                    results.add(result);
                    continue;
                }
                ProjectContact contact = findContactByExternalId(session, workspaceId, sourceKey,
                        item.getExternalContactId());
                if (contact == null) {
                    error(result, "Contact not found for externalContactId.");
                    results.add(result);
                    continue;
                }

                String op = item.getOperation();
                ProjectContactAssigned existing = findAssignment(session, contact.getContactId(),
                        project.getProjectId());
                if (OPERATION_ADD.equalsIgnoreCase(op)) {
                    if (existing == null) {
                        ProjectContactAssigned added = new ProjectContactAssigned();
                        ProjectContactAssignedId id = new ProjectContactAssignedId();
                        id.setContactId(contact.getContactId());
                        id.setProjectId(project.getProjectId());
                        added.setId(id);
                        added.setEmailAlert("N");
                        added.setUpdateDue(Integer.valueOf(0));
                        session.save(added);
                        result.setStatus("created");
                        result.setMessage("Assignment added.");
                    } else {
                        result.setStatus("noop");
                        result.setMessage("Assignment already present.");
                    }
                } else if (OPERATION_REMOVE.equalsIgnoreCase(op)) {
                    if (existing != null) {
                        session.delete(existing);
                        result.setStatus("removed");
                        result.setMessage("Assignment removed.");
                    } else {
                        result.setStatus("noop");
                        result.setMessage("Assignment already absent.");
                    }
                } else {
                    error(result, "operation must be 'add' or 'remove'.");
                    results.add(result);
                    continue;
                }
                result.setProjectId(Integer.valueOf(project.getProjectId()));
                result.setContactId(Integer.valueOf(contact.getContactId()));
                results.add(result);
            } catch (Exception ex) {
                error(result, rootCauseMessage(ex));
                results.add(result);
                session.clear();
            }
        }

        finalizeResponse(response, results);
        return response;
    }

    private void applyProjectFields(Project project, SyncProjectUpsertItem item) {
        if (item.isHasProjectName()) {
            project.setProjectName(item.getProjectName());
        }
        if (item.isHasDescription()) {
            project.setDescription(emptyToNull(item.getDescription()));
        }
        if (item.isHasProjectHandle()) {
            project.setProjectHandle(emptyToNull(item.getProjectHandle()));
        }
        if (item.isHasProjectStatus()) {
            String status = item.getProjectStatus();
            if (ProjectStatus.isKnownStatus(status)) {
                project.setProjectStatus(ProjectStatus.fromDatabaseValue(status).getDatabaseValue());
            }
        }
    }

    private void applyContactFields(ProjectContact contact, SyncContactUpsertItem item) {
        if (item.isHasNameLast()) {
            contact.setNameLast(item.getNameLast());
        }
        if (item.isHasNameFirst()) {
            contact.setNameFirst(item.getNameFirst());
        }
        if (item.isHasNameTitle()) {
            contact.setNameTitle(emptyToNull(item.getNameTitle()));
        }
        if (item.isHasOrganizationName()) {
            contact.setOrganizationName(emptyToNull(item.getOrganizationName()));
        }
        if (item.isHasEmailAddress()) {
            contact.setEmailAddress(emptyToNull(item.getEmailAddress()));
        }
        if (item.isHasTimeZone()) {
            contact.setTimeZone(emptyToNull(item.getTimeZone()));
        }
        if (item.isHasContactStatus()) {
            String status = item.getContactStatus();
            if (ProjectContact.isKnownStatus(status)) {
                contact.setContactStatus(status.toUpperCase());
            }
        }
    }

    private String validateProjectItem(SyncProjectUpsertItem item) {
        if (item == null) {
            return "Project item is required.";
        }
        if (isEmpty(item.getExternalProjectId())) {
            return "externalProjectId is required.";
        }
        if (!item.isHasProjectName() || isEmpty(item.getProjectName())) {
            return "projectName is required and cannot be empty.";
        }
        if (!item.isHasProjectStatus() || !ProjectStatus.isKnownStatus(item.getProjectStatus())) {
            return "projectStatus is required and must be one of: Active, Paused, Complete, Closed.";
        }
        return null;
    }

    private String validateProjectState(Project project) {
        if (isEmpty(project.getProjectName())) {
            return "projectName cannot be empty.";
        }
        String status = ProjectStatus.fromDatabaseValue(project.getProjectStatus()).getDatabaseValue();
        if (!ProjectStatus.CLOSED.getDatabaseValue().equalsIgnoreCase(status)
                && isEmpty(project.getProjectHandle())) {
            return "projectHandle is required for non-closed projects.";
        }
        return null;
    }

    private String validateContactItem(SyncContactUpsertItem item) {
        if (item == null) {
            return "Contact item is required.";
        }
        if (isEmpty(item.getExternalContactId())) {
            return "externalContactId is required.";
        }
        if (!item.isHasNameFirst() || isEmpty(item.getNameFirst())) {
            return "nameFirst is required and cannot be empty.";
        }
        if (!item.isHasNameLast() || isEmpty(item.getNameLast())) {
            return "nameLast is required and cannot be empty.";
        }
        if (!item.isHasContactStatus() || !ProjectContact.isKnownStatus(item.getContactStatus())) {
            return "contactStatus is required and must be ACTIVE or INACTIVE.";
        }
        return null;
    }

    private String validateContactState(ProjectContact contact) {
        if (isEmpty(contact.getNameFirst())) {
            return "nameFirst cannot be empty.";
        }
        if (isEmpty(contact.getNameLast())) {
            return "nameLast cannot be empty.";
        }
        if (!ProjectContact.isKnownStatus(contact.getContactStatus())) {
            return "contactStatus must be ACTIVE or INACTIVE.";
        }
        return null;
    }

    private String validateAssignmentItem(SyncAssignmentMutationItem item) {
        if (item == null) {
            return "Assignment item is required.";
        }
        if (isEmpty(item.getExternalProjectId())) {
            return "externalProjectId is required.";
        }
        if (isEmpty(item.getExternalContactId())) {
            return "externalContactId is required.";
        }
        if (isEmpty(item.getOperation())) {
            return "operation is required (add/remove).";
        }
        if (!OPERATION_ADD.equalsIgnoreCase(item.getOperation())
                && !OPERATION_REMOVE.equalsIgnoreCase(item.getOperation())) {
            return "operation must be 'add' or 'remove'.";
        }
        return null;
    }

    private Project findProjectByExternalId(Session session, int workspaceId, String sourceKey,
            String externalProjectId) {
        Query query = session.createQuery("from Project where workspaceId = :workspaceId "
                + "and externalSourceKey = :sourceKey and externalProjectId = :externalProjectId");
        query.setInteger("workspaceId", workspaceId);
        query.setString("sourceKey", sourceKey);
        query.setString("externalProjectId", externalProjectId);
        return (Project) query.uniqueResult();
    }

    private Project findFirstUnlinkedProjectByName(Session session, int workspaceId, String projectName) {
        if (isEmpty(projectName)) {
            return null;
        }
        Query query = session.createQuery("from Project where workspaceId = :workspaceId "
                + "and lower(projectName) = :projectNameLower "
                + "and (externalProjectId is null or trim(externalProjectId) = '') "
                + "order by projectId");
        query.setInteger("workspaceId", workspaceId);
        query.setString("projectNameLower", projectName.toLowerCase());
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<Project> matches = query.list();
        if (matches.isEmpty()) {
            return null;
        }
        return matches.get(0);
    }

    private ProjectContact findContactByExternalId(Session session, int workspaceId, String sourceKey,
            String externalContactId) {
        Query query = session.createQuery("from ProjectContact where workspaceId = :workspaceId "
                + "and externalSourceKey = :sourceKey and externalContactId = :externalContactId");
        query.setInteger("workspaceId", workspaceId);
        query.setString("sourceKey", sourceKey);
        query.setString("externalContactId", externalContactId);
        return (ProjectContact) query.uniqueResult();
    }

    private ProjectContact findFirstUnlinkedContactByEmail(Session session, int workspaceId,
            String emailAddress) {
        if (isEmpty(emailAddress)) {
            return null;
        }
        Query query = session.createQuery("from ProjectContact where workspaceId = :workspaceId "
                + "and emailAddress is not null and lower(emailAddress) = :emailLower "
                + "and (externalContactId is null or trim(externalContactId) = '') "
                + "order by contactId");
        query.setInteger("workspaceId", workspaceId);
        query.setString("emailLower", emailAddress.toLowerCase());
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<ProjectContact> matches = query.list();
        if (matches.isEmpty()) {
            return null;
        }
        return matches.get(0);
    }

    private ProjectContactAssigned findAssignment(Session session, int contactId, int projectId) {
        ProjectContactAssignedId id = new ProjectContactAssignedId(contactId, projectId);
        return (ProjectContactAssigned) session.get(ProjectContactAssigned.class, id);
    }

    private WebUser findApiUser(Session session, String username) {
        if (isEmpty(username)) {
            return null;
        }
        Query query = session.createQuery("from WebUser where username = :username");
        query.setString("username", username);
        return (WebUser) query.uniqueResult();
    }

    private void finalizeResponse(SyncBatchResponse response, List<SyncBatchItemResult> results) {
        int successCount = 0;
        int errorCount = 0;
        for (SyncBatchItemResult result : results) {
            if ("error".equals(result.getStatus())) {
                errorCount++;
            } else {
                successCount++;
            }
        }
        response.setSuccessCount(successCount);
        response.setErrorCount(errorCount);
        response.setResults(results);
    }

    private String resolveSourceKey(ApiRequestContext.ApiClientInfo client) {
        if (client.getAgentName() != null && client.getAgentName().length() > 0) {
            return client.getAgentName();
        }
        return "client-" + client.getClientId();
    }

    private String emptyToNull(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        return value;
    }

    private boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    private void error(SyncBatchItemResult result, String message) {
        result.setStatus("error");
        result.setMessage(message == null ? "Unexpected error." : message);
    }

    /**
     * Walks the full exception cause chain and returns the most specific
     * (deepest) non-null message, prefixed with the immediate exception class.
     * This surfaces the actual SQL or constraint error that Hibernate wraps.
     */
    private String rootCauseMessage(Exception ex) {
        String top = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        Throwable cause = ex.getCause();
        Throwable deepest = null;
        while (cause != null) {
            if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                deepest = cause;
            }
            cause = cause.getCause();
        }
        if (deepest != null) {
            return top + " | Caused by " + deepest.getClass().getSimpleName() + ": " + deepest.getMessage();
        }
        return top;
    }
}
