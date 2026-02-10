package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse")
public class ApiErrorResponse {

    private String errorCode;
    private String message;
    private Object details;

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(String errorCode, String message, Object details) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }
}
