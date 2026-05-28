package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import java.util.List;

public class SyncAssignmentsApplyRequest {

    private List<SyncAssignmentMutationItem> items;

    public List<SyncAssignmentMutationItem> getItems() {
        return items;
    }

    public void setItems(List<SyncAssignmentMutationItem> items) {
        this.items = items;
    }
}
