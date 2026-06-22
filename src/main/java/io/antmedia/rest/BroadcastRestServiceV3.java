package io.antmedia.rest;

import static io.antmedia.AntMediaApplicationAdapter.IP_CAMERA;
import static io.antmedia.AntMediaApplicationAdapter.PLAY_LIST;
import static io.antmedia.AntMediaApplicationAdapter.STREAM_SOURCE;
import static io.antmedia.AntMediaApplicationAdapter.VOD;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
 * Broadcast REST API protected by the JWT authorization scheme ({@link JwtV3Secured} /
 * {@link JWTFilterV3}). Business logic is reused from {@link RestServiceBase}.
 *
 * <p>Ownership: creating a broadcast records the authenticated user as its owner. Only an
 * admin, or that owner, may update or delete it. Changing or removing the owner is admin only.
 */
@Path("/" + JWTFilterV3.VERSION + "/broadcasts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BroadcastRestServiceV3 extends RestServiceBase {

	@Operation(description = "Creates a broadcast and returns it. For IP cameras and stream sources the server pulls "
			+ "from the configured URL. If no owner is given, the authenticated user becomes the owner.")
	@POST
	@JwtV3Secured
	public Response createBroadcast(
			@Parameter(description = "The broadcast to create. May be null.", required = false) Broadcast broadcast,
			@Parameter(description = "Only for IP cameras and stream sources: start pulling the stream immediately.", required = false) @QueryParam("autoStart") boolean autoStart,
			@Context ContainerRequestContext requestContext) {

		if (broadcast != null && StringUtils.isBlank(broadcast.getOwnerId())) {
			String userId = authenticatedUserId(requestContext);
			if (userId != null) {
				broadcast.setOwnerId(userId);
			}
		}

		return createBroadcastInternal(broadcast, autoStart);
	}

	@Operation(description = "Returns a single broadcast by its id.")
	@GET
	@JwtV3Secured
	@Path("/{id}")
	public Response getBroadcast(@Parameter(description = "Id of the broadcast", required = true) @PathParam("id") String id) {
		Broadcast broadcast = getDataStore().get(id);
		if (broadcast == null) {
			return error(Status.NOT_FOUND, "Broadcast not found");
		}
		return Response.status(Status.OK).entity(broadcast).build();
	}

	@Operation(description = "Lists broadcasts with pagination and optional filtering.")
	@GET
	@JwtV3Secured
	public List<Broadcast> getBroadcastList(
			@Parameter(description = "Offset of the list for pagination", required = false) @QueryParam("offset") @DefaultValue("0") int offset,
			@Parameter(description = "Number of items to return", required = false) @QueryParam("size") @DefaultValue("50") int size,
			@Parameter(description = "Filter by stream type, e.g. liveStream, ipCamera, streamSource, VoD", required = false) @QueryParam("type") String type,
			@Parameter(description = "Field to sort by: name, date or status", required = false) @QueryParam("sort_by") String sortBy,
			@Parameter(description = "asc or desc", required = false) @QueryParam("order_by") String orderBy,
			@Parameter(description = "Search string", required = false) @QueryParam("search") String search) {
		return getDataStore().getBroadcastList(offset, size, type, sortBy, orderBy, search);
	}

	@Operation(description = "Updates a broadcast. Admins can update any broadcast; other users can update only broadcasts they own.")
	@PUT
	@JwtV3Secured
	@Path("/{id}")
	public Response updateBroadcast(
			@Parameter(description = "Id of the broadcast", required = true) @PathParam("id") String id,
			@Parameter(description = "The fields to update") BroadcastUpdate broadcast,
			@Context ContainerRequestContext requestContext) {

		if (broadcast == null) {
			return error(Status.BAD_REQUEST, "Broadcast update body is required");
		}

		Broadcast broadcastInDB = getDataStore().get(id);
		if (broadcastInDB == null) {
			return error(Status.NOT_FOUND, "Broadcast not found");
		}

		boolean admin = isAdmin(requestContext);
		if (!canModifyOrDelete(broadcastInDB, authenticatedUserId(requestContext), admin)) {
			return error(Status.FORBIDDEN, "You are not the owner of this broadcast");
		}

		// Only an admin may change or remove the owner (an empty ownerId clears it).
		if (broadcast.getOwnerId() != null && !admin) {
			return error(Status.FORBIDDEN, "Only an admin can change the owner of a broadcast");
		}

		Result result;
		String type = broadcastInDB.getType();
		if (IP_CAMERA.equals(type) || STREAM_SOURCE.equals(type) || VOD.equals(type) || PLAY_LIST.equals(type)) {
			result = updateStreamSource(id, broadcast, broadcastInDB);
		}
		else {
			result = updateBroadcast(id, broadcast);
		}
		return Response.status(result.isSuccess() ? Status.OK : Status.BAD_REQUEST).entity(result).build();
	}

	@Operation(description = "Deletes a broadcast. Admins can delete any broadcast; other users can delete only broadcasts they own.")
	@DELETE
	@JwtV3Secured
	@Path("/{id}")
	public Response deleteBroadcast(
			@Parameter(description = "Id of the broadcast", required = true) @PathParam("id") String id,
			@Parameter(description = "Delete the subtracks as well", required = false) @QueryParam("deleteSubtracks") boolean deleteSubtracks,
			@Context ContainerRequestContext requestContext) {

		Broadcast broadcastInDB = getDataStore().get(id);
		if (broadcastInDB == null) {
			return error(Status.NOT_FOUND, "Broadcast not found");
		}

		if (!canModifyOrDelete(broadcastInDB, authenticatedUserId(requestContext), isAdmin(requestContext))) {
			return error(Status.FORBIDDEN, "You are not the owner of this broadcast");
		}

		Result result = deleteBroadcast(id, deleteSubtracks);
		return Response.status(result.isSuccess() ? Status.OK : Status.BAD_REQUEST).entity(result).build();
	}

	/**
	 * Pure authorization rule for modifying or deleting a broadcast: an admin may act on any
	 * broadcast; any other caller only when they are the owner. A broadcast with no owner is
	 * open to any write-authorized caller (read_only is already blocked by the filter).
	 */
	public static boolean canModifyOrDelete(Broadcast broadcast, String userId, boolean admin) {
		if (StringUtils.isBlank(broadcast.getOwnerId())) {
			return true;
		}
		return admin || broadcast.getOwnerId().equals(userId);
	}

	private static String authenticatedUserId(ContainerRequestContext requestContext) {
		Object userId = requestContext.getProperty(JWTFilterV3.AUTHENTICATED_USER_ID);
		return userId != null ? userId.toString() : null;
	}

	private static boolean isAdmin(ContainerRequestContext requestContext) {
		return Boolean.TRUE.equals(requestContext.getProperty(JWTFilterV3.ADMIN_ACCESS));
	}

	private static Response error(Status status, String message) {
		return Response.status(status).entity(new Result(false, message)).build();
	}
}
