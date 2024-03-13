package io.antmedia.rest;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.rest.model.Version;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;



@OpenAPIDefinition(
	    info = @Info(
	        description = "Ant Media Server REST API Reference",
	        version = "V2.0",
	        title = "Ant Media Server REST API Reference",
	        contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
	        license = @License(name = "Apache 2.0", url = "http://www.apache.org")),
	    servers = {@Server(	description = "test server",
				url = "https://test.antmedia.io:5443/Sandbox/rest/")},
	    externalDocs = @ExternalDocumentation(url = "https://antmedia.io")
	)

@Component
@Path("/v2")
public class RootRestService extends RestServiceBase {
	
	
	protected static Logger logger = LoggerFactory.getLogger(RootRestService.class);
	
	@Operation(summary = "Returns the Ant Media Server Version",
		    description = "Retrieves the version information of the Ant Media Server.",
		    responses = {
		        @ApiResponse(responseCode = "200", description = "Ant Media Server Version",
		                     content = @Content(
		                         mediaType = "application/json",
		                         schema = @Schema(implementation = Version.class)
		                     ))
		    }
		)
	@GET
	@Path("/version")
	@Produces(MediaType.APPLICATION_JSON)
	public Version getVersion() {
		return getSoftwareVersion();
	}

	public static class RoomInfo{
		private String roomId;
		private Map<String,String> streamDetailsMap;
		private long endDate = 0;
		private long startDate = 0;

		public RoomInfo(String roomId, Map<String, String> streamDetailsMap, ConferenceRoom room) {
			this.roomId = roomId;
			this.streamDetailsMap = streamDetailsMap;
			if(room != null) {
				this.endDate = room.getEndDate();
				this.startDate = room.getStartDate();
			}
		}

		public String getRoomId() {

			return roomId;
		}

		public long getEndDate() { return endDate; }

		public long getStartDate() { return startDate;}

		public void setRoomId(String roomId) {
			this.roomId = roomId;
		}

		public Map<String,String> getStreamDetailsMap() {
			return streamDetailsMap;
		}

		public void setStreamDetailsMap(Map<String,String> streamDetailsMap) {
			this.streamDetailsMap = streamDetailsMap;
		}
	}
}