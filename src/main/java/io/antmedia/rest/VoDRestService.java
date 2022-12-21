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

import io.antmedia.datastore.db.types.VoD;
import io.antmedia.rest.BroadcastRestService.SimpleStat;
import io.antmedia.rest.model.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Contact;
import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

@Api(value = "VoD Rest Service")
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
        externalDocs = @ExternalDocs(value = "External Docs", url = "https://antmedia.io")
)
@Component
@Path("/v2/vods")
public class VoDRestService extends RestServiceBase{
	
	@ApiOperation(value = "VoD file from database", response = VoD.class)
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public VoD getVoD(@ApiParam(value = "id of the VoD", required = true) @PathParam("id") String id) {
		return super.getVoD(id);
	}

	@ApiOperation(value = "Import VoDs to Stalker Portal", response = Result.class)
	@POST
	@Path("/import-to-stalker")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result importVoDsToStalker() 
	{
		return super.importVoDsToStalker();
	}
	
	
	@ApiOperation(value = " Get the VoD list from database. It returns 50 items at max. You can use offset value to get result page by page ", responseContainer = "List",response = VoD.class)
	@GET
	@Path("/list/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<VoD> getVodList(@ApiParam(value = "Offset of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "Number of items that will be fetched", required = true) @PathParam("size") int size,
			@ApiParam(value = "Field to sort. Possible values are \"name\", \"date\"", required = false) @QueryParam("sort_by") String sortBy,
			@ApiParam(value ="\"asc\" for Ascending, \"desc\" Descening order", required = false) @QueryParam("order_by") String orderBy,
			@ApiParam(value = "Id of the stream to filter the results by stream id", required = false) @QueryParam("streamId") String streamId,
			@ApiParam(value = "Search string", required = false) @QueryParam("search") String search)
	{
		return getDataStore().getVodList(offset, size, sortBy, orderBy, streamId, search);
	}
	
	@ApiOperation(value = "Get the total number of VoDs", response = Long.class)
	@GET
	@Path("/count")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleStat getTotalVodNumber() {
		return new SimpleStat(getDataStore().getTotalVodNumber());
	}

	@ApiOperation(value = "Get the partial number of VoDs depending on the searched items", response = Long.class)
	@GET
	@Path("/count/{search}")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleStat getTotalVodNumber(
			@ApiParam(value = "Search parameter to get the number of items including it ", required = true) @PathParam("search") String search)
	{
		return new SimpleStat(getDataStore().getPartialVodNumber(search));
	}
	
	@ApiOperation(value = "Delete specific VoD File", response = Result.class)
	@DELETE
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result deleteVoD(@ApiParam(value = "the id of the VoD file", required = true) @PathParam("id") String id) {
		return super.deleteVoD(id);
	}

	@ApiOperation(value = "Delete bulk VoD Files based on Vod Id", response = Result.class)
	@DELETE
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/bulk")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result deleteVoDs(@ApiParam(value = "the ids of the VoD file", required = true) String[] vodIds) {
		return super.deleteVoDs(vodIds);
	}
	
	@ApiOperation(value = "Upload external VoD file to Ant Media Server", notes = "", response = Result.class)
	@POST
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Path("/create")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result uploadVoDFile(@ApiParam(value = "the name of the VoD File", required = true) @QueryParam("name") String fileName,
			@ApiParam(value = "file", required = true) @FormDataParam("file") InputStream inputStream) {
		return super.uploadVoDFile(fileName, inputStream);
	}
	
	@ApiOperation(value = "Import VoD files from a directory and make it streamable.", response = Result.class)
	@POST
	@Path("/directory")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result importVoDs(@ApiParam(value="the full path of the directory that VoD files will be imported to datastore and linked to the streams") @QueryParam("directory") String directory) {
		return super.importVoDs(directory);
	}
	
	
	@ApiOperation(value = "Unlinks VoD path from streams directory and delete the database record. It does not delete the files. It only unlinks folder and delete database records", response = Result.class)
	@DELETE
	@Path("/directory")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result unlinksVoD(@ApiParam(value="the full path of the directory that imported VoD files will be deleted from database. ") @QueryParam("directory") String directory) {
		return super.unlinksVoD(directory);
	}
	
	
	
	@Deprecated
	@ApiOperation(value = "Deprecated. Use import VoDs. Synchronize VoD Folder and add them to VoD database if any file exist and create symbolic links to that folder", notes = "Notes here", response = Result.class)
	@POST
	@Path("/synch-user-vod-list")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result synchUserVodList() {
		return super.synchUserVodList();
	}
}
