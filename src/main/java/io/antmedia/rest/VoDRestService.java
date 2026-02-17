package io.antmedia.rest;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

import io.antmedia.datastore.db.types.VoD;
import io.antmedia.rest.BroadcastRestService.SimpleStat;
import io.antmedia.rest.model.Result;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;



@OpenAPIDefinition(
		info = @Info(
				description = "Ant Media Server REST API Reference",
				version = "V2.0",
				title = "Ant Media Server REST API Reference",
				contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
				license = @License(name = "Apache 2.0", url = "http://www.apache.org")),
		servers = {
				@Server(
						description = "test server",
						url = "https://test.antmedia.io:5443/Sandbox/rest/"

						)},
		externalDocs = @ExternalDocumentation(url = "https://antmedia.io")
		)

@Component
@Path("/v2/vods")
public class VoDRestService extends RestServiceBase{

	@Operation(summary = "VoD file from database", description = "Retrieves a VoD file from the database by its ID.", responses = {
			@ApiResponse(responseCode = "200", description = "VoD file retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoD.class))),
			@ApiResponse(responseCode = "404", description = "VoD file not found")
	})	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public VoD getVoD(@Parameter(description = "ID of the VoD file", required = true) @PathParam("id") String id) {
		return super.getVoD(id);
	}

	@Operation(summary = "Import VoDs to Stalker Portal", description = "Imports VoDs to the Stalker Portal.", responses = {
			@ApiResponse(responseCode = "200", description = "VoDs imported successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
	})
	@POST
	@Path("/import-to-stalker")
	@Produces(MediaType.APPLICATION_JSON)
	public Result importVoDsToStalker() {
		return super.importVoDsToStalker();
	}


	@Operation(summary = "Get the VoD list from database", description = "Retrieves the list of VoD files from the database. It returns up to 50 items. You can use offset value to get result page by page.", responses = {
			@ApiResponse(responseCode = "200", description = "VoD list retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoD.class, type = "array")))
	})
	@GET
	@Path("/list/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<VoD> getVodList(
			@Parameter(description = "Offset of the list", required = true) @PathParam("offset") int offset,
			@Parameter(description = "Number of items that will be fetched", required = true) @PathParam("size") int size,
			@Parameter(description = "Field to sort. Possible values are 'name', 'date'") @QueryParam("sort_by") String sortBy,
			@Parameter(description = "'asc' for Ascending, 'desc' for Descending order") @QueryParam("order_by") String orderBy,
			@Parameter(description = "ID of the stream to filter the results by stream ID") @QueryParam("streamId") String streamId,
			@Parameter(description = "Search string") @QueryParam("search") String search) {
		return getDataStore().getVodList(offset, size, sortBy, orderBy, streamId, search);
	}

	@Operation(summary = "Get the total number of VoDs", description = "Retrieves the total number of VoD files in the database.", responses = {
			@ApiResponse(responseCode = "200", description = "Total number of VoDs retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SimpleStat.class)))
	})
	@GET
	@Path("/count")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleStat getTotalVodNumber() {
		return new SimpleStat(getDataStore().getTotalVodNumber());
	}

	@Operation(summary = "Get the partial number of VoDs depending on the searched items", description = "Retrieves the number of VoD files that include the specified search parameter.", responses = {
			@ApiResponse(responseCode = "200", description = "Partial number of VoDs retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SimpleStat.class)))
	})
	@GET
	@Path("/count/{search}")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleStat getTotalVodNumber(
			@Parameter(description = "Search parameter to get the number of items including it", required = true) @PathParam("search") String search) {
		return new SimpleStat(getDataStore().getPartialVodNumber(search));
	}

	@Operation(summary = "Delete specific VoD File", description = "Deletes a specific VoD file from the database by its ID.", responses = {
			@ApiResponse(responseCode = "200", description = "VoD file deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
			@ApiResponse(responseCode = "404", description = "VoD file not found")
	})
	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteVoD(
			@Parameter(description = "ID of the VoD file", required = true)
			@PathParam("id") String id) {
		return super.deleteVoD(id);
	}

	/**
	 * Use {@link #deleteVoDsBulk(String)}
	 */
	@Deprecated
    @Hidden
    @DELETE
    @Path("/bulk")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Result deleteVoDs(
            @Parameter(description = "IDs of the VoD files", required = true) String[] vodIds) {
        return super.deleteVoDs(vodIds);
    }
    
	
    @Operation(summary = "Delete bulk VoD Files based on Vod Id", description = "Deletes multiple VoD files from the database by their IDs.", responses = {
            @ApiResponse(responseCode = "200", description = "VoD files deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    })
    @DELETE
    @Path("/")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_JSON)
    public Result deleteVoDsBulk(
            @Parameter(description = "Comma-separated IDs of the VoD files", required = true) @QueryParam("ids") String vodIds) {
    	if (StringUtils.isNotBlank(vodIds)) {
    		return super.deleteVoDs(vodIds.split(","));
    	}
    	else {
    		return new Result(false, "ids parameter is blank. Please give comma-separated vod ids to the 'ids' as query parameter");
    	}
    }



    @Operation(summary = "Upload external VoD file to Ant Media Server", description = "Uploads an external VoD file to Ant Media Server.", responses = {
            @ApiResponse(responseCode = "200", description = "VoD file uploaded successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    })
    @POST
    @Path("/create")
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Result uploadVoDFile(
            @Parameter(description = "Name of the VoD File", required = true) @QueryParam("name") String fileName,
            @Parameter(description = "VoD file", required = true) @FormDataParam("file") InputStream inputStream,
            @Parameter(description = "Custom metadata for the VoD file", required = false) @FormDataParam("metadata") String metadata) {
        return super.uploadVoDFile(fileName, inputStream, metadata);
    }


    @Operation(summary = "Import VoD files from a directory and make them streamable.", description = "Imports VoD files from a directory to the datastore and links them to the streams.", responses = {
            @ApiResponse(responseCode = "200", description = "VoD files imported successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    })
    @POST
    @Path("/directory")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Result importVoDs(
            @Parameter(description = "The full path of the directory that VoD files will be imported to the datastore and linked to the streams", required = true) @QueryParam("directory") String directory) {
        return super.importVoDs(directory);
    }



    @Operation(summary = "Unlinks VoD path from streams directory and delete the database record.", description = "Deletes the database record associated with the specified directory, without deleting the files themselves.", responses = {
            @ApiResponse(responseCode = "200", description = "VoD records unlinked successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    })
    @DELETE
    @Path("/directory")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Result unlinksVoD(
            @Parameter(description = "The full path of the directory from which imported VoD files will be deleted from the database", required = true) @QueryParam("directory") String directory) {
        return super.unlinksVoD(directory);
    }



	@Hidden
    @Operation(summary = "Deprecated. Use import VoDs.", description = "Synchronizes VoD Folder and adds them to VoD database if any file exist and creates symbolic links to that folder.", deprecated = true, responses = {
            @ApiResponse(responseCode = "200", description = "VoD files synchronized successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    })
    @POST
    @Path("/synch-user-vod-list")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Result synchUserVodList() {
        return super.synchUserVodList();
    }

}
