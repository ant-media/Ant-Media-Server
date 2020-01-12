package io.antmedia.rest;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

import io.antmedia.datastore.db.types.Playlist;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.rest.BroadcastRestServiceV2.SimpleStat;
import io.antmedia.rest.model.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Contact;
import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

@Api(value = "Playlist Rest Service")
@SwaggerDefinition(
        info = @Info(
                description = "Ant Media Server REST API Reference",
                version = "V2.0",
                title = "Ant Media Server REST API Reference",
                contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
                license = @License(name = "Apache 2.0", url = "http://www.apache.org")),
        consumes = {"application/json"},
        produces = {"application/json"},
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
        externalDocs = @ExternalDocs(value = "External Docs", url = "https://antmedia.io"),
        basePath = "/v2/Playlist"
)
@Component
@Path("/v2/playlists")
public class PlaylistRestServiceV2 extends RestServiceBase{
	
	
 	@ApiOperation(value = "Playlist list from database", response = Playlist.class)
	@GET
	@Path("/{playlistId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Playlist getPlaylist(@ApiParam(value = "id of the Playlist", required = true) @PathParam("playlistId") String playlistId) {
		return super.getPlaylist(playlistId);
	}
	
	@ApiOperation(value = "Delete specific Playlist", response = Result.class)
	@DELETE
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/{playlistId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result deleteVoD(@ApiParam(value = "the playlistId of the Playlist", required = true) @PathParam("playlistId") String playlistId) {
		return super.deletePlaylist(playlistId);
	}
	
	
	@ApiOperation(value = "Create Playlist", notes = "", response = Result.class)
	@POST
	@Path("/create")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result createPlaylist(@ApiParam(value = "the name of the Playlist File", required = true) @QueryParam("playlist") Playlist playlist) {
		return super.createPlaylist(playlist);
	}

	
}