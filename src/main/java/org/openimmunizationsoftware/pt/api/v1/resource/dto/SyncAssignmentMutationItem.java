package org.openimmunizationsoftware.pt.api.v1.resource.dto;

public class SyncAssignmentMutationItem {

    private String externalProjectId;
    private String externalContactId;
    private String operation;

    public String getExternalProjectId() {
        return externalProjectId;
    }

    public void setExternalProjectId(String externalProjectId) {
        this.externalProjectId = externalProjectId;
    }

    public String getExternalContactId() {
        return externalContactId;
    }

    public void setExternalContactId(String externalContactId) {
        this.externalContactId = externalContactId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
