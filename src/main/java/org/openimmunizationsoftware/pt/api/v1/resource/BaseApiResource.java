package org.openimmunizationsoftware.pt.api.v1.resource;

import org.openimmunizationsoftware.pt.api.v1.resource.dto.ApiErrorResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.openimmunizationsoftware.pt.api.common.ApiRequestContext;

public abstract class BaseApiResource {

    protected ApiRequestContext.ApiClientInfo requireClient() {
        try {
            return ApiRequestContext.getCurrentClient();
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ApiErrorResponse("unauthorized", "Unauthorized.", null))
                    .build());
        }
    }

    protected String requireProviderId(ApiRequestContext.ApiClientInfo client) {
        if (client.getProviderId() == null || client.getProviderId().trim().length() == 0) {
            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                    .entity(new ApiErrorResponse("forbidden", "Provider scope missing.", null))
                    .build());
        }
        return client.getProviderId().trim();
    }

    protected void requirePositiveId(int value, String name) {
        if (value <= 0) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ApiErrorResponse("bad_request",
                                    name + " must be a positive integer.", null))
                            .build());
        }
    }
}
