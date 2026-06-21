package io.antmedia.rest;

import org.apache.commons.lang3.StringUtils;

import io.antmedia.datastore.db.types.Broadcast;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * v3 Broadcast REST API. Protected by the new v3 JWT authorization scheme
 * ({@link JwtV3Secured} / {@code JWTFilterV3}). This is the first slice and exposes
 * only broadcast creation; business logic is reused from {@link RestServiceBase} so
 * behavior matches v2.
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

		if (broadcast != null && StringUtils.isBlank(broadcast.getOwnerId())) {
			Object ownerId = requestContext.getProperty(JWTFilterV3.AUTHENTICATED_USER_ID);
			if (ownerId != null) {
				broadcast.setOwnerId(ownerId.toString());
			}
		}

		return createBroadcastInternal(broadcast, autoStart);
	}
}
