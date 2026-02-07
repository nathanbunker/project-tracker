package com.myapp.api.v1.resource;

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
        OpenApiContext context = OpenApiContextLocator.getInstance().getOpenApiContext(null);
        if (context == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }
        OpenAPI openApi = context.read();
        return Response.ok(Json.pretty(openApi)).build();
    }
}
