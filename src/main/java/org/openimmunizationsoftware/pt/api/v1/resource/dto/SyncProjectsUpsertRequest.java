package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import java.util.List;

public class SyncProjectsUpsertRequest {

    private List<SyncProjectUpsertItem> items;

    public List<SyncProjectUpsertItem> getItems() {
        return items;
    }

    public void setItems(List<SyncProjectUpsertItem> items) {
        this.items = items;
    }
}
