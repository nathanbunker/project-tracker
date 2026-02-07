package org.openimmunizationsoftware.pt.api.common;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER)
public class ApiRequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(ApiRequestLoggingFilter.class.getName());
    private static final String START_TIME_PROPERTY = "apiStartTimeNs";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(START_TIME_PROPERTY, Long.valueOf(System.nanoTime()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        long durationMs = -1L;
        Object start = requestContext.getProperty(START_TIME_PROPERTY);
        if (start instanceof Long) {
            durationMs = (System.nanoTime() - ((Long) start).longValue()) / 1_000_000L;
        }
        String clientId = "unknown";
        String providerId = "unknown";
        try {
            ApiRequestContext.ApiClientInfo client = ApiRequestContext.getCurrentClient();
            clientId = String.valueOf(client.getClientId());
            if (client.getProviderId() != null) {
                providerId = client.getProviderId();
            }
        } catch (IllegalStateException ex) {
            // No authenticated client available.
        }
        String path = requestContext.getUriInfo().getPath();
        int status = responseContext.getStatus();
        String message = "api request client_id=" + clientId
                + " provider_id=" + providerId
                + " path=" + path
                + " status=" + status
                + " duration_ms=" + durationMs;
        LOGGER.log(Level.INFO, message);
    }
}
