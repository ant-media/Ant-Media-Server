package io.antmedia.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import io.antmedia.datastore.db.types.VoD;
import io.antmedia.muxer.Muxer;
import io.antmedia.rest.model.Result;
import io.antmedia.storage.StorageClient;
import io.antmedia.storage.StorageClient.FileType;
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
        externalDocs = @ExternalDocs(value = "External Docs", url = "https://antmedia.io"),
        basePath = "/v2/VoD"
)
@Component
@Path("/v2/vods")
public class VoDRestServiceV2 extends RestServiceBase{
	
	@ApiOperation(value = "VoD file from database", response = VoD.class)
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public VoD getVoD(@ApiParam(value = "id of the VoD", required = true) @PathParam("id") String id) {
		VoD vod = null;
		if (id != null) {
			vod = getDataStore().getVoD(id);
		}
		if (vod == null) {
			vod = new VoD();
		}
		return vod;
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
	
	
	@ApiOperation(value = " Get the VoD list from database", responseContainer = "List",response = VoD.class)
	@GET
	@Path("/list/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<VoD> getVodList(@ApiParam(value = "offset of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "Number of items that will be fetched", required = true) @PathParam("size") int size) {
		return getDataStore().getVodList(offset, size);
	}
	
	@ApiOperation(value = "Get the total number of VoDs", response = Long.class)
	@GET
	@Path("/count")
	@Produces(MediaType.APPLICATION_JSON)
	public long getTotalVodNumber() {
		return getDataStore().getTotalVodNumber();
	}
	
	@ApiOperation(value = "Delete specific VoD File", response = Result.class)
	@DELETE
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteVoD(@ApiParam(value = "the id of the VoD file", required = true) @PathParam("id") String id) {
		boolean success = false;
		String message = "";
		ApplicationContext appContext = getAppContext();
		if (appContext != null) {

			File videoFile = null;
			VoD voD = getDataStore().getVoD(id);
			if (voD != null) {
				try {
					String filePath = String.format("webapps/%s/%s", getScope().getName(), voD.getFilePath());
					videoFile = new File(filePath);
					boolean result = Files.deleteIfExists(videoFile.toPath());
					if (!result) {
						logger.warn("File is not deleted because it does not exist {}", videoFile.getAbsolutePath());
					}
					success = getDataStore().deleteVod(id);
					if (success) {
						message = "vod deleted";
					}

					String fileName = videoFile.getName();
					String[] splitFileName = StringUtils.split(fileName,".");
					//delete preview file if exists
					File previewFile = Muxer.getPreviewFile(getScope(), splitFileName[0], ".png");
					Files.deleteIfExists(previewFile.toPath());

					if (appContext.containsBean("app.storageClient")) {
						StorageClient storageClient = (StorageClient) appContext.getBean("app.storageClient");

						storageClient.delete(splitFileName[0] + ".mp4", FileType.TYPE_STREAM);
						storageClient.delete(splitFileName[0] + ".png", FileType.TYPE_PREVIEW);
					}
				}
				catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}

		}
		return new Result(success, message);
	}
	
	@ApiOperation(value = "Upload external VoD file to Ant Media Server", notes = "", response = Result.class)
	@POST
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Result uploadVoDFile(@ApiParam(value = "the name of the VoD File", required = true) @QueryParam("name") String fileName,
			@ApiParam(value = "file", required = true) @FormDataParam("file") InputStream inputStream) {
		boolean success = false;
		String message = "";
		String id= null;
		String appScopeName = getScope().getName();
		String fileExtension = FilenameUtils.getExtension(fileName);
		try {

			if ("mp4".equals(fileExtension)) {


				File streamsDirectory = new File(
						getStreamsDirectory(appScopeName));

				// if the directory does not exist, create it
				if (!streamsDirectory.exists()) {
					streamsDirectory.mkdirs();
				}
				String vodId = RandomStringUtils.randomNumeric(24);
				File savedFile = new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName,
						"streams/" + vodId + ".mp4"));

				int read = 0;
				byte[] bytes = new byte[2048];
				try (OutputStream outpuStream = new FileOutputStream(savedFile))
				{

					while ((read = inputStream.read(bytes)) != -1) {
						outpuStream.write(bytes, 0, read);
					}
					outpuStream.flush();

					long fileSize = savedFile.length();
					long unixTime = System.currentTimeMillis();

					String path = savedFile.getPath();

					String[] subDirs = path.split(Pattern.quote(File.separator));

					Integer pathLength = subDirs.length;

					String relativePath = subDirs[pathLength-2]+ File.separator +subDirs[pathLength-1];

					VoD newVod = new VoD(fileName, "file", relativePath, fileName, unixTime, 0, fileSize,
							VoD.UPLOADED_VOD, vodId);

					id = getDataStore().addVod(newVod);

					if(id != null) {
						success = true;
						message = id;
					} 
				}
			} 
			else {
				message = "notMp4File";
			}

		} 
		catch (IOException iox) {
			logger.error(iox.getMessage());
		} 


		return new Result(success, id, message);
	}
	
	public String getStreamsDirectory(String appScopeName) {
		return String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, "streams");
	}
	
	@ApiOperation(value = "Synchronize VoD Folder and add them to VoD database if any file exist and create symbolic links to that folder", notes = "Notes here", response = Result.class)
	@POST
	@Path("/synch-user-vod-list")
	@Produces(MediaType.APPLICATION_JSON)
	public Result synchUserVodList() {
		boolean result = false;
		int errorId = -1;
		String message = "";

		String vodFolder = getApplication().getAppSettings().getVodFolder();

		logger.info("synch user vod list vod folder is {}", vodFolder);

		if (vodFolder != null && vodFolder.length() > 0) {

			result = getApplication().synchUserVoDFolder(null, vodFolder);
		}
		else {
			errorId = 404;
			message = "no VodD folder defined";
		}

		return new Result(result, message, errorId);
	}
	
}