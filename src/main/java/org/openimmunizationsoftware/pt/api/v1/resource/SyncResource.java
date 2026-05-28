package org.openimmunizationsoftware.pt.api.v1.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openimmunizationsoftware.pt.api.common.ApiRequestContext;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.ApiErrorResponse;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.SyncAssignmentsApplyRequest;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.SyncBatchResponse;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.SyncContactsUpsertRequest;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.SyncProjectsUpsertRequest;
import org.openimmunizationsoftware.pt.api.v1.service.ExternalSyncService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Path("/v1/sync")
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "ApiKeyAuth")
public class SyncResource extends BaseApiResource {

    private final ExternalSyncService syncService = new ExternalSyncService();

    @POST
    @Path("/projects/upsert")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upsert projects by external identifier", description = "Write-only endpoint. Upserts project rows in the API key workspace using (workspace, source, externalProjectId).")
    @ApiResponse(responseCode = "200", description = "Batch processed")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @ApiResponse(responseCode = "403", description = "Workspace scope missing")
    public SyncBatchResponse upsertProjects(SyncProjectsUpsertRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw badRequest("Request body must include a non-empty items array.");
        }
        ApiRequestContext.ApiClientInfo client = requireClient();
        int workspaceId = requireWorkspaceId(client);
        return syncService.upsertProjects(client, workspaceId, request.getItems());
    }

    @POST
    @Path("/contacts/upsert")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upsert contacts by external identifier", description = "Write-only endpoint. Upserts project_contact rows in the API key workspace using (workspace, source, externalContactId).")
    @ApiResponse(responseCode = "200", description = "Batch processed")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @ApiResponse(responseCode = "403", description = "Workspace scope missing")
    public SyncBatchResponse upsertContacts(SyncContactsUpsertRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw badRequest("Request body must include a non-empty items array.");
        }
        ApiRequestContext.ApiClientInfo client = requireClient();
        int workspaceId = requireWorkspaceId(client);
        return syncService.upsertContacts(client, workspaceId, request.getItems());
    }

    @POST
    @Path("/assignments/apply")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Apply assignment add/remove mutations", description = "Write-only endpoint. Applies add/remove operations against project_contact_assigned using external project/contact identifiers.")
    @ApiResponse(responseCode = "200", description = "Batch processed")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @ApiResponse(responseCode = "403", description = "Workspace scope missing")
    public SyncBatchResponse applyAssignments(SyncAssignmentsApplyRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw badRequest("Request body must include a non-empty items array.");
        }
        ApiRequestContext.ApiClientInfo client = requireClient();
        int workspaceId = requireWorkspaceId(client);
        return syncService.applyAssignments(client, workspaceId, request.getItems());
    }

    private WebApplicationException badRequest(String message) {
        return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiErrorResponse("bad_request", message, null))
                .build());
    }
}
