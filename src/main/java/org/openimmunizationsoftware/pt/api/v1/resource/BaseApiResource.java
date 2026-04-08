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
        if (client.getWorkspaceId() == null || client.getWorkspaceId().intValue() <= 0) {
            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                    .entity(new ApiErrorResponse("forbidden", "Workspace scope missing.", null))
                    .build());
        }
        return String.valueOf(client.getWorkspaceId());
    }

    protected int requireWorkspaceId(ApiRequestContext.ApiClientInfo client) {
        if (client.getWorkspaceId() == null || client.getWorkspaceId().intValue() <= 0) {
            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                    .entity(new ApiErrorResponse("forbidden", "Workspace scope missing or invalid.", null))
                    .build());
        }
        return client.getWorkspaceId().intValue();
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
