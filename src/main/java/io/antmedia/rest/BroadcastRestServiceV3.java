package io.antmedia.rest;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.rest.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * v3 Broadcast REST API. Protected by the new v3 JWT authorization scheme
 * ({@link JwtV3Secured} / {@link JWTFilterV3}). Business logic is reused from
 * {@link RestServiceBase} so behavior matches v2.
 *
 * <p>Asset ownership: create stamps {@code ownerId} from the JWT sub; update/delete
 * enforce it (admin → any, user → only own, read_only is already blocked by the filter).
 */
@Path("/v3/broadcasts")
public class BroadcastRestServiceV3 extends RestServiceBase {

	@Operation(description = "Creates a Broadcast, IP Camera or Stream Source and returns the full broadcast object. "
			+ "Requires a v3 JWT granting admin or user access to this application. If no ownerId is provided, "
			+ "the broadcast is owned by the authenticated user (JWT sub).")
	@POST
	@JwtV3Secured
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/create")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createBroadcast(
			@Parameter(description = "Broadcast object. Set the required fields, it may be null as well.", required = false) Broadcast broadcast,
			@Parameter(description = "Only effective if stream is IP Camera or Stream Source. If true, it starts pulling the stream automatically.", required = false) @QueryParam("autoStart") boolean autoStart,
			@Context ContainerRequestContext requestContext) {

		// JWTFilterV3 already verified the token and put the user id (sub) here, so we don't re-parse it.
		if (broadcast != null && StringUtils.isBlank(broadcast.getOwnerId())) {
			Object ownerId = requestContext.getProperty(JWTFilterV3.AUTHENTICATED_USER_ID);
			if (ownerId != null) {
				broadcast.setOwnerId(ownerId.toString());
			}
		}

		return createBroadcastInternal(broadcast, autoStart);
	}

	@Operation(description = "Returns a single broadcast by id. Requires read access to this application.")
	@GET
	@JwtV3Secured
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBroadcast(@Parameter(description = "Id of the broadcast", required = true) @PathParam("id") String id) {
		Broadcast broadcast = getDataStore().get(id);
		if (broadcast == null) {
			return Response.status(Status.NOT_FOUND).entity(new Result(false, "Broadcast not found")).build();
		}
		return Response.status(Status.OK).entity(broadcast).build();
	}

	@Operation(description = "Lists broadcasts with pagination. Requires read access to this application.")
	@GET
	@JwtV3Secured
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> getBroadcastList(
			@Parameter(description = "Offset of the list for pagination", required = false) @QueryParam("offset") @DefaultValue("0") int offset,
			@Parameter(description = "Number of items to return", required = false) @QueryParam("size") @DefaultValue("50") int size,
			@Parameter(description = "Filter by stream type, e.g. liveStream, ipCamera, streamSource, VoD", required = false) @QueryParam("type") String type,
			@Parameter(description = "Field to sort by: name, date or status", required = false) @QueryParam("sort_by") String sortBy,
			@Parameter(description = "asc or desc", required = false) @QueryParam("order_by") String orderBy,
			@Parameter(description = "Search string", required = false) @QueryParam("search") String search) {
		return getDataStore().getBroadcastList(offset, size, type, sortBy, orderBy, search);
	}

	@Operation(description = "Updates a broadcast. Requires admin access, or user access while being the asset owner.")
	@PUT
	@JwtV3Secured
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateBroadcast(
			@Parameter(description = "Broadcast id", required = true) @PathParam("id") String id,
			@Parameter(description = "Broadcast object with the updates") BroadcastUpdate broadcast,
			@Context ContainerRequestContext requestContext) {

		if (broadcast == null) {
			return Response.status(Status.BAD_REQUEST).entity(new Result(false, "Broadcast update body is required")).build();
		}

		Broadcast broadcastInDB = getDataStore().get(id);
		Response denied = checkOwnership(broadcastInDB, requestContext);
		if (denied != null) {
			return denied;
		}

		Result result;
		String type = broadcastInDB.getType();
		if (AntMediaApplicationAdapter.IP_CAMERA.equals(type) || AntMediaApplicationAdapter.STREAM_SOURCE.equals(type)
				|| AntMediaApplicationAdapter.VOD.equals(type) || AntMediaApplicationAdapter.PLAY_LIST.equals(type)) {
			result = updateStreamSource(id, broadcast, broadcastInDB);
		}
		else {
			result = updateBroadcast(id, broadcast);
		}
		return Response.status(result.isSuccess() ? Status.OK : Status.BAD_REQUEST).entity(result).build();
	}

	@Operation(description = "Deletes a broadcast. Requires admin access, or user access while being the asset owner.")
	@DELETE
	@JwtV3Secured
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteBroadcast(
			@Parameter(description = "Id of the broadcast", required = true) @PathParam("id") String id,
			@Parameter(description = "Delete the subtracks as well", required = false) @QueryParam("deleteSubtracks") boolean deleteSubtracks,
			@Context ContainerRequestContext requestContext) {

		Broadcast broadcastInDB = getDataStore().get(id);
		Response denied = checkOwnership(broadcastInDB, requestContext);
		if (denied != null) {
			return denied;
		}

		Result result = deleteBroadcast(id, deleteSubtracks);
		return Response.status(result.isSuccess() ? Status.OK : Status.BAD_REQUEST).entity(result).build();
	}

	/**
	 * Asset ownership check for modify/delete. Returns an error Response when access is
	 * denied (404 if the broadcast is missing, 403 if the caller is not the owner), or
	 * {@code null} when the operation may proceed. read_only is already blocked by the filter.
	 */
	private Response checkOwnership(Broadcast broadcast, ContainerRequestContext requestContext) {
		if (broadcast == null) {
			return Response.status(Status.NOT_FOUND).entity(new Result(false, "Broadcast not found")).build();
		}
		// No owner set -> any write-authorized caller may proceed.
		if (StringUtils.isBlank(broadcast.getOwnerId())) {
			return null;
		}
		// Admins may modify any asset.
		if (Boolean.TRUE.equals(requestContext.getProperty(JWTFilterV3.ADMIN_ACCESS))) {
			return null;
		}
		// Otherwise the caller must be the owner.
		Object userId = requestContext.getProperty(JWTFilterV3.AUTHENTICATED_USER_ID);
		if (broadcast.getOwnerId().equals(userId)) {
			return null;
		}
		return Response.status(Status.FORBIDDEN).entity(new Result(false, "You are not the owner of this broadcast")).build();
	}
}
