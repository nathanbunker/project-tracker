package org.openimmunizationsoftware.pt.api.v1.resource;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import java.util.Collections;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class OpenApiBootstrap implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        OpenAPI openApi = new OpenAPI()
                .info(new Info()
                        .title("Project Tracker API")
                        .version("v1")
                        .description("API endpoints for project tracking and proposal workflows."))
                .components(new Components()
                        .addSecuritySchemes("ApiKeyAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .name("X-Api-Key")
                                        .in(SecurityScheme.In.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"));

        SwaggerConfiguration config = new SwaggerConfiguration()
                .openAPI(openApi)
                .resourcePackages(Collections.singleton("org.openimmunizationsoftware.pt.api.v1.resource"))
                .prettyPrint(true);

        try {
            new JaxrsOpenApiContextBuilder<>()
                    .openApiConfiguration(config)
                    .buildContext(true);
        } catch (Exception ex) {
            System.err.println("OpenApiBootstrap: failed to initialize OpenAPI context");
            ex.printStackTrace(System.err);
            throw new IllegalStateException("Unable to initialize OpenAPI context.", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
