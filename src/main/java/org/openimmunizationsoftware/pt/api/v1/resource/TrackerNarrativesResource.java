package org.openimmunizationsoftware.pt.api.v1.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.openimmunizationsoftware.pt.api.common.ApiRequestContext;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.ApiErrorResponse;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.TrackerNarrativeDto;
import org.openimmunizationsoftware.pt.doa.TrackerNarrativeDao;
import org.openimmunizationsoftware.pt.format.DateFormatService;
import org.openimmunizationsoftware.pt.format.DefaultDateFormatService;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;

@Path("/v1/tracker-narratives")
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "ApiKeyAuth")
public class TrackerNarrativesResource extends BaseApiResource {

    private static final String LAST_UPDATED_FORMATS = "ISO-8601 (e.g. 2026-02-17T13:45:00Z), "
            + "yyyy-MM-dd HH:mm:ss, yyyy-MM-dd HH:mm, or yyyy-MM-dd";

    private final TrackerNarrativeDao narrativeDao = new TrackerNarrativeDao();
    private static final DateFormatService DATE_FORMAT_SERVICE = new DefaultDateFormatService();

    @GET
    @Operation(summary = "List tracker narratives updated after a timestamp")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid query parameters")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @ApiResponse(responseCode = "403", description = "Provider scope missing")
    public List<TrackerNarrativeDto> listTrackerNarratives(@QueryParam("contactId") Integer contactId,
            @QueryParam("lastUpdatedAfter") String lastUpdatedAfter) {
        ApiRequestContext.ApiClientInfo client = requireClient();
        requireProviderId(client);
        if (contactId == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiErrorResponse("bad_request", "contactId is required.", null))
                    .build());
        }
        requirePositiveId(contactId.intValue(), "contactId");
        Date updatedAfter = parseLastUpdated(lastUpdatedAfter);
        List<TrackerNarrative> narratives = narrativeDao.listByContactUpdatedAfter(
                contactId.intValue(), updatedAfter);
        List<TrackerNarrativeDto> result = new ArrayList<TrackerNarrativeDto>();
        for (TrackerNarrative narrative : narratives) {
            result.add(TrackerNarrativeDto.from(narrative));
        }
        return result;
    }

    private Date parseLastUpdated(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiErrorResponse("bad_request", "lastUpdatedAfter is required.", null))
                    .build());
        }
        String trimmed = value.trim();
        try {
            return DATE_FORMAT_SERVICE.parseApiDateTimeLenient(trimmed, TimeZone.getDefault());
        } catch (IllegalArgumentException ex) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiErrorResponse("bad_request",
                            "lastUpdatedAfter must match one of: " + LAST_UPDATED_FORMATS + ".", null))
                    .build());
        }
    }
}
