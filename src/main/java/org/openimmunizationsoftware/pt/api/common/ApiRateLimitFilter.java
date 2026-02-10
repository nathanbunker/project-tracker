package org.openimmunizationsoftware.pt.api.common;

import org.openimmunizationsoftware.pt.api.v1.resource.dto.ApiErrorResponse;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION + 10)
public class ApiRateLimitFilter implements ContainerRequestFilter {

    private static final int LIMIT = 60;
    private static final long WINDOW_MS = 60_000L;
    private static final Map<Integer, Deque<Long>> REQUEST_LOG = new ConcurrentHashMap<Integer, Deque<Long>>();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        ApiRequestContext.ApiClientInfo client;
        try {
            client = ApiRequestContext.getCurrentClient();
        } catch (IllegalStateException ex) {
            return;
        }
        int clientId = client.getClientId();
        long now = System.currentTimeMillis();
        Deque<Long> window = REQUEST_LOG.get(clientId);
        if (window == null) {
            Deque<Long> created = new ArrayDeque<Long>();
            window = REQUEST_LOG.putIfAbsent(clientId, created);
            if (window == null) {
                window = created;
            }
        }
        boolean limited;
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst().longValue() > WINDOW_MS) {
                window.pollFirst();
            }
            limited = window.size() >= LIMIT;
            if (!limited) {
                window.addLast(Long.valueOf(now));
            }
        }
        if (limited) {
            ApiErrorResponse error = new ApiErrorResponse("rate_limited",
                    "Rate limit exceeded.", "limit=60 requests per minute");
            requestContext.abortWith(Response.status(429).entity(error).build());
        }
    }
}
