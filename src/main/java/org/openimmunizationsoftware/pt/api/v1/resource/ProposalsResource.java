package org.openimmunizationsoftware.pt.api.v1.resource;

import org.openimmunizationsoftware.pt.api.v1.resource.dto.ApiErrorResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.openimmunizationsoftware.pt.api.common.ApiRequestContext;
import org.openimmunizationsoftware.pt.api.v1.service.ProjectActionProposalService;

@Path("/v1/proposals")
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "ApiKeyAuth")
public class ProposalsResource extends BaseApiResource {

    private final ProjectActionProposalService proposalService = new ProjectActionProposalService();

    @POST
    @Path("/{proposalId}/supersede")
    @Operation(summary = "Supersede proposal", description = "Marks the proposal as superseded if it belongs to the authenticated client.")
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "400", description = "Invalid proposalId")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @ApiResponse(responseCode = "403", description = "Provider scope missing")
    @ApiResponse(responseCode = "404", description = "Proposal not found")
    public Response supersede(@PathParam("proposalId") int proposalId) {
        requirePositiveId(proposalId, "proposalId");
        ApiRequestContext.ApiClientInfo client = requireClient();
        String providerId = requireProviderId(client);
        try {
            proposalService.supersedeProposal(providerId, proposalId);
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiErrorResponse("not_found", "Proposal not found.", null))
                    .build());
        }
        return Response.noContent().build();
    }
}
