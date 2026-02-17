package org.openimmunizationsoftware.pt.api.v1.resource;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.models.OpenAPI;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/openapi.json")
@Produces(MediaType.APPLICATION_JSON)
public class OpenApiJsonResource {

    @GET
    public Response getOpenApi() {
        System.err.println("OpenApiJsonResource.getOpenApi: start");
        OpenApiContext context = OpenApiContextLocator.getInstance()
            .getOpenApiContext(OpenApiBootstrap.OPENAPI_CONTEXT_ID);
        if (context == null) {
            System.err.println("OpenApiJsonResource.getOpenApi: OpenApiContext is null");
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }
        try {
            OpenAPI openApi = context.read();
            String payload = Json.pretty(openApi);
            System.err.println("OpenApiJsonResource.getOpenApi: generated payload length=" + payload.length());
            return Response.ok(payload).build();
        } catch (Exception ex) {
            System.err.println("OpenApiJsonResource.getOpenApi: exception while generating OpenAPI");
            ex.printStackTrace(System.err);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON)
                    .entity("{\"errorCode\":\"internal_error\",\"message\":\"OpenAPI generation failed.\",\"details\":null}")
                    .build();
        }
    }
}
