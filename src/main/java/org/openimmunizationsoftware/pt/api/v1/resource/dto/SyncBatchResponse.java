package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import java.util.ArrayList;
import java.util.List;

public class SyncBatchResponse {

    private int total;
    private int successCount;
    private int errorCount;
    private List<SyncBatchItemResult> results = new ArrayList<SyncBatchItemResult>();

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public List<SyncBatchItemResult> getResults() {
        return results;
    }

    public void setResults(List<SyncBatchItemResult> results) {
        this.results = results;
    }
}
