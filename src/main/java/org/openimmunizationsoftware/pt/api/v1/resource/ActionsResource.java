package org.openimmunizationsoftware.pt.api.v1.resource;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.ApiRequestContext;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.api.v1.dao.ActionChangeLogDao;
import org.openimmunizationsoftware.pt.api.v1.service.ProjectActionProposalService;
import org.openimmunizationsoftware.pt.model.ProjectActionChangeLog;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionProposal;

import org.openimmunizationsoftware.pt.api.v1.resource.dto.ActionChangeLogDto;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.ApiErrorResponse;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.CreateProposalRequest;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.ProposalDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Path("/v1/actions")
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "ApiKeyAuth")
public class ActionsResource extends BaseApiResource {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final ProjectActionProposalService proposalService = new ProjectActionProposalService();
    private final ActionChangeLogDao changeLogDao = new ActionChangeLogDao();

    @GET
    @Path("/{actionId}/proposals")
    @Operation(summary = "List proposals for an action")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid actionId")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @ApiResponse(responseCode = "403", description = "Provider scope missing")
    @ApiResponse(responseCode = "404", description = "Action not found")
    public List<ProposalDto> listProposals(@PathParam("actionId") int actionId) {
        requirePositiveId(actionId, "actionId");
        ApiRequestContext.ApiClientInfo client = requireClient();
        String providerId = requireProviderId(client);
        requireAction(providerId, actionId);
        List<ProjectActionProposal> proposals = proposalService.listProposalsForAction(providerId, actionId);
        List<ProposalDto> result = new ArrayList<ProposalDto>();
        for (ProjectActionProposal proposal : proposals) {
            result.add(ProposalDto.from(proposal));
        }
        return result;
    }

    @POST
    @Path("/{actionId}/proposals")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create action-level proposal", description = "Creates a new proposal with status 'new' and supersedes any active proposal for the same client+action target.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @ApiResponse(responseCode = "403", description = "Provider scope missing")
    @ApiResponse(responseCode = "404", description = "Action not found")
    public Response createActionProposal(@PathParam("actionId") int actionId,
            CreateProposalRequest request) {
        requirePositiveId(actionId, "actionId");
        ApiRequestContext.ApiClientInfo client = requireClient();
        String providerId = requireProviderId(client);
        ProjectActionNext action = requireAction(providerId, actionId);
        validateProposalRequest(request);
        String agentName = client.getAgentName() != null ? client.getAgentName() : providerId;
        ProjectActionProposal proposal = proposalService.createProposal(providerId, agentName,
                action.getProjectId(), actionId, request.getProposedPatchJson(), request.getSummary(),
                request.getRationale(), request.getContactId());
        return Response.status(Response.Status.CREATED).entity(ProposalDto.from(proposal)).build();
    }

    @GET
    @Path("/{actionId}/changes")
    @Operation(summary = "List change logs for an action")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid actionId or limit")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @ApiResponse(responseCode = "403", description = "Provider scope missing")
    @ApiResponse(responseCode = "404", description = "Action not found")
    public List<ActionChangeLogDto> listChanges(@PathParam("actionId") int actionId,
            @QueryParam("limit") Integer limit) {
        requirePositiveId(actionId, "actionId");
        ApiRequestContext.ApiClientInfo client = requireClient();
        String providerId = requireProviderId(client);
        requireAction(providerId, actionId);
        int effectiveLimit = normalizeLimit(limit);
        List<ProjectActionChangeLog> changeLogs = changeLogDao.listByAction(providerId, actionId, effectiveLimit);
        List<ActionChangeLogDto> result = new ArrayList<ActionChangeLogDto>();
        for (ProjectActionChangeLog log : changeLogs) {
            result.add(ActionChangeLogDto.from(log));
        }
        return result;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit.intValue() <= 0) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiErrorResponse("bad_request",
                            "limit must be a positive integer.", null))
                    .build());
        }
        return Math.min(limit.intValue(), MAX_LIMIT);
    }

    private ProjectActionNext requireAction(String providerId, int actionId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from ProjectActionNext pan where pan.actionNextId = :actionId and pan.provider.providerId = :providerId");
        query.setInteger("actionId", actionId);
        query.setString("providerId", providerId);
        ProjectActionNext action = (ProjectActionNext) query.uniqueResult();
        if (action == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiErrorResponse("not_found", "Action not found.", null))
                    .build());
        }
        return action;
    }

    private void validateProposalRequest(CreateProposalRequest request) {
        if (request == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiErrorResponse("bad_request", "Request body is required.", null))
                    .build());
        }
        boolean hasContent = isNonEmpty(request.getProposedPatchJson())
                || isNonEmpty(request.getSummary())
                || isNonEmpty(request.getRationale());
        if (!hasContent) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiErrorResponse("bad_request", "Proposal content is required.", null))
                    .build());
        }
    }

    private boolean isNonEmpty(String value) {
        return value != null && value.trim().length() > 0;
    }
}
