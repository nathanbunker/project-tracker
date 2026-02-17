package org.openimmunizationsoftware.pt.api.v1.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.openimmunizationsoftware.pt.model.TrackerNarrative;

@Path("/v1/tracker-narratives")
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "ApiKeyAuth")
public class TrackerNarrativesResource extends BaseApiResource {

    private static final String LAST_UPDATED_FORMATS = "ISO-8601 (e.g. 2026-02-17T13:45:00Z), "
            + "yyyy-MM-dd HH:mm:ss, yyyy-MM-dd HH:mm, or yyyy-MM-dd";

    private final TrackerNarrativeDao narrativeDao = new TrackerNarrativeDao();

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
        Date parsed = parseInstant(trimmed);
        if (parsed != null) {
            return parsed;
        }
        parsed = parseOffsetDateTime(trimmed);
        if (parsed != null) {
            return parsed;
        }
        parsed = parseLocalDateTime(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        if (parsed != null) {
            return parsed;
        }
        parsed = parseLocalDateTime(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (parsed != null) {
            return parsed;
        }
        parsed = parseLocalDateTime(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        if (parsed != null) {
            return parsed;
        }
        parsed = parseLocalDate(trimmed);
        if (parsed != null) {
            return parsed;
        }
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiErrorResponse("bad_request",
                        "lastUpdatedAfter must match one of: " + LAST_UPDATED_FORMATS + ".", null))
                .build());
    }

    private Date parseInstant(String value) {
        try {
            return Date.from(Instant.parse(value));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private Date parseOffsetDateTime(String value) {
        try {
            OffsetDateTime parsed = OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return Date.from(parsed.toInstant());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private Date parseLocalDateTime(String value, DateTimeFormatter formatter) {
        try {
            LocalDateTime parsed = LocalDateTime.parse(value, formatter);
            return Date.from(parsed.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private Date parseLocalDate(String value) {
        try {
            LocalDate parsed = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return Date.from(parsed.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
