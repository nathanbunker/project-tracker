package com.myapp.api.v1.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/swagger-ui")
public class SwaggerUiResource {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getSwaggerUi() {
        String html = "<!doctype html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "  <meta charset=\"utf-8\"/>\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>\n"
                + "  <title>Project Tracker API</title>\n"
                + "  <link rel=\"stylesheet\" href=\"https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui.css\"/>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <div id=\"swagger-ui\"></div>\n"
                + "  <script src=\"https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui-bundle.js\"></script>\n"
                + "  <script>\n"
                + "    window.onload = function() {\n"
                + "      SwaggerUIBundle({\n"
                + "        url: '/api/openapi.json',\n"
                + "        dom_id: '#swagger-ui'\n"
                + "      });\n"
                + "    };\n"
                + "  </script>\n"
                + "</body>\n"
                + "</html>\n";
        return Response.ok(html).build();
    }
}
