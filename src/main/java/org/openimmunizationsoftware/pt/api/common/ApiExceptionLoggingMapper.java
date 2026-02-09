package org.openimmunizationsoftware.pt.api.common;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ApiExceptionLoggingMapper implements ExceptionMapper<Throwable> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        String path = "unknown";
        if (uriInfo != null) {
            path = uriInfo.getPath();
        }
        System.err.println("ApiExceptionLoggingMapper: unhandled exception for path=" + path);
        exception.printStackTrace(System.err);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"errorCode\":\"internal_error\",\"message\":\"Unexpected server error.\",\"details\":null}")
                .build();
    }
}
