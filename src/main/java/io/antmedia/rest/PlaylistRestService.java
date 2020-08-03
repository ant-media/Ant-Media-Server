package io.antmedia.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.StreamIdValidator;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Playlist;
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
		basePath = "/v2/playlist"
		)
@Component
@Path("/v2/playlists")
public class PlaylistRestService extends RestServiceBase{


	@ApiOperation(value = "Playlist list from database", response = Playlist.class)
	@GET
	@Path("/{playlistId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Playlist getPlaylist(@ApiParam(value = "id of the Playlist", required = true) @PathParam("playlistId") String playlistId) {
		return getOrCreatePlaylist(playlistId);
	}

	public Playlist getOrCreatePlaylist(String playlistId) {
		Playlist playlist = null;
		if (playlistId != null) {
			playlist  = getDataStore().getPlaylist(playlistId);
		}
		else {
			playlist = new Playlist();
		}
		return playlist;
	}

	@ApiOperation(value = "Playlist list from database", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{playlistId}/stop")
	@Produces(MediaType.APPLICATION_JSON)
	public Result stopPlaylist(@ApiParam(value = "id of the Playlist", required = true) @PathParam("playlistId") String playlistId) {
		Result result = new Result(false);

		//Check playlistId is not null
		if(playlistId != null) {

			// Get playlist from Datastore
			Playlist playlist = getDataStore().getPlaylist(playlistId);

			// Check playlist is not null
			if(playlist != null) {

				if(playlist.getBroadcastItemList().size() > playlist.getCurrentPlayIndex()) {

					// Get current broadcast from playlist
					Broadcast broadcast = playlist.getBroadcastItemList().get(playlist.getCurrentPlayIndex());

					if(!broadcast.getStreamId().isEmpty() && broadcast.getStreamId() != null) {

						result = getApplication().stopStreaming(broadcast);
						playlist.setPlaylistStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
						getDataStore().editPlaylist(playlistId, playlist);

					}
				}
				else {
					result.setMessage("Playlist Current Broadcast not found. Playlist Broadcast Size: " + playlist.getBroadcastItemList().size() + " Playlist Current Broadcast Index: "+playlist.getCurrentPlayIndex());
				}
			}
			else {
				result.setMessage("Playlist not found");
			}

		}

		return result;
	}

	@ApiOperation(value = "Playlist list from database", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{playlistId}/start")
	@Produces(MediaType.APPLICATION_JSON)
	public Result startPlaylist(@ApiParam(value = "id of the Playlist", required = true) @PathParam("playlistId") String playlistId) {
		Result result = new Result(false);

		//Check playlistId is not null
		if(playlistId != null) {

			// Get playlist from Datastore
			Playlist playlist = getDataStore().getPlaylist(playlistId);

			// Check playlist is not null
			if(playlist != null) {

				if(playlist.getBroadcastItemList().size() > playlist.getCurrentPlayIndex()) {

					// Get current broadcast from playlist
					Broadcast broadcast = playlist.getBroadcastItemList().get(playlist.getCurrentPlayIndex());

					if(!broadcast.getStreamId().isEmpty() && broadcast.getStreamId() != null) {
						result = startPlaylistService(playlist);
					}
				}
				else {
					result.setMessage("Playlist Current Broadcast not found. Playlist Broadcast Size: " + playlist.getBroadcastItemList().size() + " Playlist Current Broadcast Index: "+playlist.getCurrentPlayIndex());	}
			}
			else {
				result.setMessage("Playlist not found");
			}
		}
		return result;
	}


	@ApiOperation(value = "Delete specific Playlist", response = Result.class)
	@DELETE
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/{playlistId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deletePlaylist(@ApiParam(value = "the playlistId of the Playlist", required = true) @PathParam("playlistId") String playlistId) {

		Result result = new Result(false);

		if(playlistId != null) {

			Broadcast broadcast = getDataStore().get(playlistId);

			result.setSuccess(getDataStore().deletePlaylist(playlistId));	
			getDataStore().delete(playlistId);
			getApplication().stopStreaming(broadcast);

			return result;
		}
		return result;
	}


	@ApiOperation(value = "Create Playlist", notes = "", response = Result.class)
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/create")
	@Produces(MediaType.APPLICATION_JSON)
	public Result createPlaylist(@ApiParam(value = "the name of the Playlist File", required = false) Playlist playlist,
			@ApiParam(value = "If it's true, it starts automatically pulling playlist broadcasts. Default value is false by default", required = false, defaultValue="false") @QueryParam("autoStart") boolean autoStart
			) {

		Result result = new Result(false);

		// If Playlist ID is entered, check is not null & valid

		if(playlist.getPlaylistId() != null && !playlist.getPlaylistId().isEmpty()) {

			// Check Playlist ID for already there
			Playlist playlistTmp = getDataStore().getPlaylist(playlist.getPlaylistId());

			if (playlistTmp != null) 
			{
				result.setMessage("Playlist id is already being used");
				return result;
			}
			else if (!StreamIdValidator.isStreamIdValid(playlist.getPlaylistId())) 
			{
				result.setMessage("Playlist id is not valid");
				return result;
			}
		}
		else {
			playlist.setPlaylistId(RandomStringUtils.randomNumeric(24));
		}

		// Check playlist ID and all stream IDs are same?		

		checkBroadcastIdsInPlaylist(playlist);

		result.setSuccess(getDataStore().createPlaylist(playlist));

		// If create Playlist is succeed, add broadcast and check autostart
		if(result.isSuccess()) {

			// Add Broadcast for the list in Broadcasts list
			saveBroadcast(playlist.getBroadcastItemList().get(playlist.getCurrentPlayIndex()), AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, getScope().getName(), getDataStore(), getAppSettings().getListenerHookURL(), getServerSettings(), 0);

			if(autoStart) {
				result = startPlaylistService(playlist);
			}
		}

		return result;
	}

	@ApiOperation(value = "Edit Playlist", notes = "", response = Result.class)
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/edit/{playlistId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result editPlaylist(@ApiParam(value="id of the Playlist") @PathParam("playlistId") String playlistId, @ApiParam(value = "the name of the Playlist File", required = true) Playlist playlist) {

		Result result = new Result(false);

		if(playlistId != null && playlist.getPlaylistId() != null) {

			// Check playlist ID and all stream IDs are same?		
			checkBroadcastIdsInPlaylist(playlist);
			result.setSuccess(getDataStore().editPlaylist(playlistId,playlist));

			return result;
		}

		return result;
	}

}