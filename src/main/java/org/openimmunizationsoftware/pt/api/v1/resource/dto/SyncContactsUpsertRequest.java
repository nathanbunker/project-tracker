package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import java.util.List;

public class SyncContactsUpsertRequest {

    private List<SyncContactUpsertItem> items;

    public List<SyncContactUpsertItem> getItems() {
        return items;
    }

    public void setItems(List<SyncContactUpsertItem> items) {
        this.items = items;
    }
}
