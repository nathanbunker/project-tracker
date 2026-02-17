package org.openimmunizationsoftware.pt.api.v1.resource;

import org.openimmunizationsoftware.pt.api.v1.resource.dto.ActionNextDto;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.ActionTakenDto;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.ApiErrorResponse;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.CreateProposalRequest;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.ProjectDto;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.ProposalDto;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.ApiRequestContext;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.api.v1.service.ProjectActionProposalService;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectActionProposal;

@Path("/v1/projects")
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "ApiKeyAuth")
public class ProjectsResource extends BaseApiResource {

    private final ProjectActionProposalService proposalService = new ProjectActionProposalService();

    @GET
    @Operation(summary = "List projects visible to client")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @ApiResponse(responseCode = "403", description = "Provider scope missing")
    public List<ProjectDto> listProjects() {
        ApiRequestContext.ApiClientInfo client = requireClient();
        String providerId = requireProviderId(client);
        List<Project> projects = proposalService.listProjectsVisibleToClient(providerId, client.getUsername());
        List<ProjectDto> result = new ArrayList<ProjectDto>();
        for (Project project : projects) {
            result.add(ProjectDto.from(project));
        }
        return result;
    }

    @GET
    @Path("/{projectId}/actions/next")
    @Operation(summary = "List next actions for a project")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid projectId")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @ApiResponse(responseCode = "403", description = "Provider scope missing")
    @ApiResponse(responseCode = "404", description = "Project not found")
    public List<ActionNextDto> listActionsNext(@PathParam("projectId") int projectId) {
        requirePositiveId(projectId, "projectId");
        ApiRequestContext.ApiClientInfo client = requireClient();
        String providerId = requireProviderId(client);
        requireProject(providerId, projectId);
        List<ProjectActionNext> actions = proposalService.listActionsNextForProject(providerId, projectId);
        List<ActionNextDto> result = new ArrayList<ActionNextDto>();
        for (ProjectActionNext action : actions) {
            result.add(ActionNextDto.from(action));
        }
        return result;
    }

    @GET
    @Path("/{projectId}/actions/taken")
    @Operation(summary = "List actions taken for a project")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid projectId")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @ApiResponse(responseCode = "403", description = "Provider scope missing")
    @ApiResponse(responseCode = "404", description = "Project not found")
    public List<ActionTakenDto> listActionsTaken(@PathParam("projectId") int projectId) {
        requirePositiveId(projectId, "projectId");
        ApiRequestContext.ApiClientInfo client = requireClient();
        String providerId = requireProviderId(client);
        requireProject(providerId, projectId);
        List<ProjectActionTaken> actions = proposalService.listActionsTakenForProject(providerId, projectId);
        List<ActionTakenDto> result = new ArrayList<ActionTakenDto>();
        for (ProjectActionTaken action : actions) {
            result.add(ActionTakenDto.from(action));
        }
        return result;
    }

    @POST
    @Path("/{projectId}/proposals")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create project-level proposal", description = "Creates a new proposal with status 'new' and supersedes any active proposal for the same client+project target. If actionNextId is provided, superseding is scoped to that next action.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    @ApiResponse(responseCode = "403", description = "Provider scope missing")
    @ApiResponse(responseCode = "404", description = "Project not found")
    public Response createProjectProposal(@PathParam("projectId") int projectId,
            CreateProposalRequest request) {
        requirePositiveId(projectId, "projectId");
        ApiRequestContext.ApiClientInfo client = requireClient();
        String providerId = requireProviderId(client);
        requireProject(providerId, projectId);
        validateProposalRequest(request);
        String agentName = client.getAgentName() != null ? client.getAgentName() : providerId;
        Integer actionNextId = request.getActionNextId();
        if (actionNextId != null && actionNextId.intValue() <= 0) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiErrorResponse("bad_request",
                    "actionNextId must be a positive integer.", null))
                .build());
        }
        ProjectActionProposal proposal = proposalService.createProposal(providerId, agentName, projectId,
            actionNextId, request.getProposedPatchJson(), request.getSummary(), request.getRationale(),
            request.getContactId());
        return Response.status(Response.Status.CREATED).entity(ProposalDto.from(proposal)).build();
    }

    private Project requireProject(String providerId, int projectId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from Project p where p.projectId = :projectId and p.provider.providerId = :providerId");
        query.setInteger("projectId", projectId);
        query.setString("providerId", providerId);
        Project project = (Project) query.uniqueResult();
        if (project == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiErrorResponse("not_found", "Project not found.", null))
                    .build());
        }
        return project;
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
