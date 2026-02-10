package org.openimmunizationsoftware.pt.api.common;

import org.openimmunizationsoftware.pt.api.v1.resource.dto.ApiErrorResponse;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.ErrorResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException) {
            WebApplicationException webEx = (WebApplicationException) exception;
            Response response = webEx.getResponse();
            Response.StatusType status = response.getStatusInfo();
            ApiErrorResponse error = buildError(status.getStatusCode(), extractMessage(response));
            return Response.status(status).entity(error).build();
        }
        ApiErrorResponse error = buildError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Unexpected server error.");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
    }

    private ApiErrorResponse buildError(int statusCode, String message) {
        return new ApiErrorResponse(mapErrorCode(statusCode), message, null);
    }

    private String extractMessage(Response response) {
        Object entity = response.getEntity();
        if (entity instanceof ApiErrorResponse) {
            ApiErrorResponse error = (ApiErrorResponse) entity;
            return error.getMessage();
        }
        if (entity instanceof ErrorResponse) {
            return ((ErrorResponse) entity).getMessage();
        }
        if (entity instanceof String) {
            return (String) entity;
        }
        return response.getStatusInfo().getReasonPhrase();
    }

    private String mapErrorCode(int statusCode) {
        switch (statusCode) {
            case 400:
                return "bad_request";
            case 401:
                return "unauthorized";
            case 403:
                return "forbidden";
            case 404:
                return "not_found";
            case 409:
                return "conflict";
            case 429:
                return "rate_limited";
            default:
                return "internal_error";
        }
    }
}
