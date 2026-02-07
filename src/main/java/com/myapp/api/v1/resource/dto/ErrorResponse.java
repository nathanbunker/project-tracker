package com.myapp.api.v1.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse")
public class ErrorResponse {

    private String message;

    public ErrorResponse() {
    }

    public ErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
