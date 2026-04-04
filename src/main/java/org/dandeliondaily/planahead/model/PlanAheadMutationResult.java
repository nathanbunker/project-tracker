package org.dandeliondaily.planahead.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlanAheadMutationResult {

    private boolean success;
    private String message = "";
    private Map<String, String> affectedCellsHtml = new LinkedHashMap<String, String>();
    private Map<String, String> affectedHeadersHtml = new LinkedHashMap<String, String>();
    private Map<String, Object> data = new LinkedHashMap<String, Object>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getAffectedCellsHtml() {
        return affectedCellsHtml;
    }

    public void setAffectedCellsHtml(Map<String, String> affectedCellsHtml) {
        this.affectedCellsHtml = affectedCellsHtml;
    }

    public Map<String, String> getAffectedHeadersHtml() {
        return affectedHeadersHtml;
    }

    public void setAffectedHeadersHtml(Map<String, String> affectedHeadersHtml) {
        this.affectedHeadersHtml = affectedHeadersHtml;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
