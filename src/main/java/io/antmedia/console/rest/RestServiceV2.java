package io.antmedia.console.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.red5.server.api.scope.IScope;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Licence;
import io.antmedia.datastore.db.types.User;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;


@OpenAPIDefinition(
	    info = @io.swagger.v3.oas.annotations.info.Info(
	        title = "Ant Media Server Management Panel REST API",
	        version = "v2.0",
	        description = "Ant Media Server Management Panel REST API",
	        contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
	        license = @License(name = "Apache 2.0", url = "http://www.apache.org")
	    ),
	    externalDocs = @ExternalDocumentation(description = "External Docs", url = "https://antmedia.io")
	)
@Component
@Path("/v2")
public class RestServiceV2 extends CommonRestService {

	@Operation(summary = "Creates a new user",
            description = "Creates a new user. If user object is null or if user is not authenticated, new user won't be created.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Successful operation", 
                             content = @Content(schema = @Schema(implementation = Result.class))),
                
            })	
	@POST
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result addUser(@Parameter(description = "User object. If it is null, new user won't be created.", required = true) User user) {
		return super.addUser(user);
	}

	@Operation(summary = "Edit the user",
	           description = "Edit the user in the server management panel's user list. It can change password or user type (admin, read-only).",
	           responses = {
	               @ApiResponse(responseCode = "200", description = "Successful operation", 
	                            content = @Content(schema = @Schema(implementation = Result.class)))
	           })	
	@PUT
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result editUser(@Parameter(description = "User to be edited. It finds the user with username.", required = true)User user) {
		return super.editUser(user);
	}

	@Operation(summary = "Delete the user",
	           description = "Delete the user from the server management panel's user list",
	           responses = {
	               @ApiResponse(responseCode = "200", description = "Successful operation", 
	                            content = @Content(schema = @Schema(implementation = Result.class)))
	           })
	@DELETE
	@Path("/users/{username}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Override
	public Result deleteUser(@Parameter(description = "User name or e-mail of the user to be deleted", required = true) @PathParam("username") String userName) {
		return super.deleteUser(userName);
	}

	@Operation(summary = "Returns if user is blocked",
	           description = "User is blocked for a specific time if there are login attempts",
	           responses = {
	               @ApiResponse(responseCode = "200", description = "Successful operation",
	                            content = @Content(schema = @Schema(implementation = Result.class)))
	           })
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/users/{usermail}/blocked")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getBlockedStatus(@Parameter(description="User name or e-mail of the user to check its status") @PathParam("usermail") String usermail) {
	    return super.getBlockedStatus(usermail);
	}


	@Operation(summary = "Returns user list",
	           description = "Returns user list in the server management panel",
	           responses = {
	               @ApiResponse(responseCode = "200", description = "Successful operation",
	                            content = @Content(schema = @Schema(implementation = List.class)))
	           })
	@GET
	@Path("/user-list")
	@Produces(MediaType.APPLICATION_JSON)
	public List<User> getUserList() {
	    return super.getUserList();
	}

	@Operation(summary = "Returns admin status",
	           description = "Returns whether current user is admin or not. If user is admin, it can call POST/PUT/DELETE methods",
	           responses = {
	               @ApiResponse(responseCode = "200", description = "Successful operation",
	                            content = @Content(schema = @Schema(implementation = Result.class)))
	           })
	@GET
	@Path("/admin-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Result isAdmin(){
	    return super.isAdmin();
	}

	@Operation(summary = "Creates initial user",
	           description = "Creates initial user. This is a one time scenario when initial user creation required and shouldn't be used otherwise. User object is required and can't be null",
	           responses = {
	               @ApiResponse(responseCode = "200", description = "Successful operation",
	                            content = @Content(schema = @Schema(implementation = Result.class)))
	           })
	@POST
	@Path("/users/initial")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Result addInitialUser(@Parameter(description = "User object. If it is null, new user won't be created.", required = true)User user) {
	    return super.addInitialUser(user);
	}

	@Operation(summary = "Checks first login status",
	           description = "Checks first login status. If server being logged in first time, it returns true, otherwise false.",
	           responses = {
	               @ApiResponse(responseCode = "200", description = "Successful operation",
	                            content = @Content(schema = @Schema(implementation = Result.class)))
	           })
	@GET
	@Path("/first-login-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Result isFirstLogin() {
	    return super.isFirstLogin();
	}

	@Operation(summary = "Authenticates user",
	           description = "Authenticates user with given username and password. Requires user object to authenticate.",
	           responses = {
	               @ApiResponse(responseCode = "200", description = "Successful operation",
	                            content = @Content(schema = @Schema(implementation = Result.class)))
	           })
	@POST
	@Path("/users/authenticate")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Result authenticateUser(@Parameter(description = "User object to authenticate", required = true) User user) {
	    return super.authenticateUser(user);
	}


	@Operation(summary = "Changes the given user's password",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@POST
	@Path("/users/password")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Result changeUserPassword(@Parameter(description = "User object to change the password", required = true) User user) {
	    return super.changeUserPassword(user);
	}

	@Operation(summary = "Returns true if user is authenticated to call rest api operations",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/authentication-status")
	@Produces(MediaType.APPLICATION_JSON)
	public Result isAuthenticatedRest() {
	    return super.isAuthenticatedRest();
	}

	@Operation(summary = "Returns system information",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/system-status")
	@Produces(MediaType.APPLICATION_JSON)
	public String getSystemInfo() {
	    return super.getSystemInfo();
	}

	@Operation(summary = "Returns JVM memory information",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/jvm-memory-status")
	@Produces(MediaType.APPLICATION_JSON)
	public String getJVMMemoryInfo() {
	    return super.getJVMMemoryInfo();
	}

	@Operation(summary = "Gets system memory status",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/system-memory-status")
	@Produces(MediaType.APPLICATION_JSON)
	public String getSystemMemoryInfo() {
	    return super.getSystemMemoryInfo();
	}

	@Operation(summary = "Gets system file status",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/file-system-status")
	@Produces(MediaType.APPLICATION_JSON)
	public String getFileSystemInfo() {
	    return super.getFileSystemInfo();
	}

	@Operation(summary = "Returns system cpu load, process cpu load and process cpu time",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/cpu-status")
	@Produces(MediaType.APPLICATION_JSON)
	public String getCPUInfo() {
	    return super.getCPUInfo();
	}

	@Operation(summary = "Gets thread dump in plain text",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/thread-dump")
	@Produces(MediaType.TEXT_PLAIN)
	public String getThreadDump() {
	    return super.getThreadDump();
	}

	@Operation(summary = "Gets thread dump in json format",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/thread-dump-json")
	@Produces(MediaType.APPLICATION_JSON)
	public String getThreadDumpJSON() {
	    return super.getThreadDumpJSON();
	}

	@Operation(summary = "Returns processor's thread information",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/threads")
	@Produces(MediaType.APPLICATION_JSON)
	public String getThreadsInfo() {
	    return super.getThreadsInfo();
	}

	@Operation(summary = "Returns heap dump",
	           responses = {@ApiResponse(responseCode = "200", description = "Returns the heap dump")})
	@GET
	@Path("/heap-dump")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getHeapDump() {
	    return super.getHeapDump();
	}

	@Operation(summary = "Gets server time",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/server-time")
	@Produces(MediaType.APPLICATION_JSON)
	public String getServerTime() {
	    return super.getServerTime();
	}

	@Operation(summary = "Gets system resource information",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/system-resources")
	@Produces(MediaType.APPLICATION_JSON)
	public String getSystemResourcesInfo() {
	    return super.getSystemResourcesInfo();
	}

	@Operation(summary = "Gets GPU information",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/gpu-status")
	@Produces(MediaType.APPLICATION_JSON)
	public String getGPUInfo() {
	    return super.getGPUInfo();
	}

	@Operation(summary = "Returns the version of Ant Media Server",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/version")
	@Produces(MediaType.APPLICATION_JSON)
	public String getVersion() {
	    return super.getVersion();
	}

	@Operation(summary = "Trigger garbage collector",
	           responses = {@ApiResponse(responseCode = "200", description = "Garbage collection triggered")})
	@POST
	@Path("/system/gc")
	@Produces(MediaType.APPLICATION_JSON)
	public Result triggerGc() {
	    System.gc();
	    return new Result(true);
	}

			
	@Operation(summary = "Gets the applications in the server",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/applications")
	@Produces(MediaType.APPLICATION_JSON)
	public String getApplications() {
	    return super.getApplications();
	}

	@Operation(summary = "Returns total number of live streams",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/live-clients-size")
	@Produces(MediaType.APPLICATION_JSON)
	public String getTotalLiveStreamSize() {
	    return super.getLiveClientsSize();
	}

	@Operation(summary = "Gets application info",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/applications-info")
	@Produces(MediaType.APPLICATION_JSON)
	public String getApplicationInfo() {
	    return super.getApplicationInfo();
	}

	@Operation(summary = "Returns live streams in the specified application",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@GET
	@Path("/applications/live-streams/{appname}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getAppLiveStreams(@Parameter(description = "Application name", required = true) @PathParam("appname") String name) {
	    return super.getAppLiveStreams(name);
	}

	@Operation(summary = "Changes the application settings",
	           responses = {@ApiResponse(responseCode = "200", description = "Successful operation")})
	@POST
	@Path("/applications/settings/{appname}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String changeSettings(@Parameter(description = "Application name", required = true) @PathParam("appname") String appname, @Parameter(description = "New application settings, null fields will be set to default values", required = true) AppSettings newSettings) {
	    return super.changeSettings(appname, newSettings);
	}


	@Operation(summary = "Checks whether application or applications have shutdown properly",
	           responses = {
	               @ApiResponse(responseCode = "200", description = "Returns the shutdown status of entered applications."),
	               @ApiResponse(responseCode = "400", description = "Either entered in wrong format or typed incorrectly application names")
	           })
	@GET
	@Path("/shutdown-proper-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response isShutdownProperly(@Parameter(description = "Application name", required = true) @QueryParam("appNames") String appNamesArray) {
	    return super.isShutdownProperly(appNamesArray);
	}

	@Operation(summary = "Set application or applications shutdown property to true",
	           responses = {@ApiResponse(responseCode = "200", description = "Shutdown status set")})
	@GET
	@Path("/shutdown-properly")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean setShutdownStatus(@Parameter(description = "Application name", required = true) @QueryParam("appNames") String appNamesArray) {
	    return super.setShutdownStatus(appNamesArray);
	}

	@Operation(summary = "Changes server settings",
	           responses = {@ApiResponse(responseCode = "200", description = "Server settings changed")})
	@POST
	@Path("/server-settings")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String changeServerSettings(@Parameter(description = "Server settings", required = true) ServerSettings serverSettings) {
	    return super.changeServerSettings(serverSettings);
	}

	@Operation(summary = "Changes ssl settings",
	           responses = {@ApiResponse(responseCode = "200", description = "SSL settings configured, server will be restarted")})
	@POST
	@Path("/ssl-settings")
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Produces(MediaType.APPLICATION_JSON)
	public Result configureSsl(@Parameter(description = "SSL settings", required = true) @QueryParam("domain") String domain, @QueryParam("type") String type,
	        @FormDataParam("fullChainFile") InputStream fullChainFile,
	        @FormDataParam("fullChainFile") FormDataContentDisposition fullChainFileDetail,
	        @FormDataParam("privateKeyFile") InputStream privateKeyFile,
	        @FormDataParam("privateKeyFile") FormDataContentDisposition privateKeyFileDetail,
	        @FormDataParam("chainFile") InputStream chainFile,
	        @FormDataParam("chainFile") FormDataContentDisposition chainFileDetail) {
	    return super.configureSsl(domain, type, fullChainFile, fullChainFileDetail, privateKeyFile, privateKeyFileDetail, chainFile, chainFileDetail);
	}

	@Operation(summary = "Returns true if the server is enterprise edition",
	           responses = {@ApiResponse(responseCode = "200", description = "Enterprise edition status")})
	@GET
	@Path("/enterprise-edition")
	@Produces(MediaType.APPLICATION_JSON)
	public Result isEnterpriseEdition() {
	    return super.isEnterpriseEdition();
	}

	@Operation(summary = "Returns the specified application settings",
	           responses = {@ApiResponse(responseCode = "200", description = "Application settings returned")})
	@GET
	@Path("/applications/settings/{appname}")
	@Produces(MediaType.APPLICATION_JSON)
	public AppSettings getSettings(@Parameter(description = "Application name", required = true) @PathParam("appname") String appname) {
	    return super.getSettings(appname);
	}

	@Operation(summary = "Returns the server settings",
	           responses = {@ApiResponse(responseCode = "200", description = "Server settings returned")})
	@GET
	@Path("/server-settings")
	@Produces(MediaType.APPLICATION_JSON)
	public ServerSettings getServerSettings() {
	    return super.getServerSettings();
	}

	@Operation(summary = "Returns license status",
	           responses = {@ApiResponse(responseCode = "200", description = "License status returned")})
	@GET
	@Path("/licence-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Licence getLicenceStatus(@Parameter(description = "License key", required = true) @QueryParam("key") String key) {
	    return super.getLicenceStatus(key);
	}

	@Operation(summary = "Returns the last checked license status",
	           responses = {@ApiResponse(responseCode = "200", description = "Last checked license status returned")})
	@GET
	@Path("/last-licence-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Licence getLicenceStatus() {
	    return super.getLicenceStatus();
	}

	
	@Operation(summary = "Resets the viewer counts and broadcasts statuses in the db",
	           responses = {@ApiResponse(responseCode = "200", description = "Broadcasts reset successfully")})
	@POST
	@Path("/applications/{appname}/reset")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Result resetBroadcast(@Parameter(description = "Application name", required = true) @PathParam("appname") String appname) {
	    return super.resetBroadcast(appname);
	}

	@Operation(summary = "Returns the server mode",
	           responses = {@ApiResponse(responseCode = "200", description = "Server mode returned")})
	@GET
	@Path("/cluster-mode-status")
	@Produces(MediaType.APPLICATION_JSON)
	public Result isInClusterMode() {
	    return super.isInClusterMode();
	}

	@Operation(summary = "Gets log file",
	           responses = {@ApiResponse(responseCode = "200", description = "Log file retrieved successfully")})
	@GET
	@Path("/log-file/{offsetSize}/{charSize}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getLogFile(@Parameter(description = "Char size of the log", required = true) @PathParam("charSize") int charSize, @Parameter(description = "Log type. ERROR can be used to get only error logs", required = true) @QueryParam("logType") String logType,
	        @Parameter(description = "Offset of the retrieved log", required = true) @PathParam("offsetSize") long offsetSize) throws IOException {
	    return super.getLogFile(charSize, logType, offsetSize);
	}

	@Operation(summary = "Creates a new application with given name",
	           responses = {@ApiResponse(responseCode = "200", description = "Application created successfully")})
	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Path("/applications/{appName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result createApplication(@Parameter(description = "Name for the new application", required = true) @PathParam("appName") String appName) {
	    return createApplication(appName, null);
	}

	@Operation(summary = "Creates a new application with given name and supports uploading custom WAR files",
	           responses = {@ApiResponse(responseCode = "200", description = "Custom application created successfully")})
	@PUT
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Path("/applications/{appName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result createApplication(@Parameter(description = "Name for the new application", required = true) @PathParam("appName") String appName, @Parameter(description = "file", required = true) @FormDataParam("file") InputStream inputStream) {
	    Result result;
	    if (appName != null && appName.matches("^[a-zA-Z0-9]*$")) {
	        boolean applicationAlreadyExist = isApplicationExists(appName);
	        if (!applicationAlreadyExist) {
	            result = super.createApplication(appName, inputStream);
	        } else {
	            result = new Result(false, "Application with the same name already exists");
	        }
	    } else {
	        result = new Result(false, "Application name is not alphanumeric. Please provide alphanumeric characters");
	    }
	    return result;
	}


	public boolean isApplicationExists(String appName) {
		return getApplication().getRootScope().getScope(appName)  != null;
	}


	@Operation(summary = "Deletes application with the given name",
	           responses = {@ApiResponse(responseCode = "200", description = "Application deleted successfully")})
	@DELETE
	@Path("/applications/{appName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteApplication(@Parameter(description = "Name of the application to delete", required = true) @PathParam("appName") String appName,
	        @Parameter(description = "Whether to delete associated database", required = true) @QueryParam("deleteDB") boolean deleteDB) {
	    if (appName != null) {
	        return super.deleteApplication(appName, deleteDB);
	    }
	    return new Result(false, "Application name is not defined");
	}

	@Operation(summary = "Returns the hostname to check liveness with HTTP type healthcheck",
	           responses = {@ApiResponse(responseCode = "200", description = "Liveness check response")})
	@GET
	@Path("/liveness")
	@Produces(MediaType.APPLICATION_JSON)
	public Response liveness() {
	    long startTimeMs = System.currentTimeMillis();
	    JsonObject jsonObject = new JsonObject();

	    //the following method may take some time to return

	    String status;
	    Status statusCode;
	    String hostname = getHostname();
	    if (hostname != null) {
	        status = "ok";
	        statusCode = Status.OK;
	    }
	    else {
	        hostname = "unknown";
	        status = "error";
	        statusCode = Status.INTERNAL_SERVER_ERROR;
	    }

	    jsonObject.addProperty("host", hostname);
	    jsonObject.addProperty("status", status);

	    Gson gson = new Gson();
	    long elapsedTimeMs = System.currentTimeMillis() - startTimeMs;
	    if (elapsedTimeMs > 1000) {
	        logger.warn("GET liveness method takes {}ms to return", elapsedTimeMs);
	    }
	    return Response.status(statusCode).entity(gson.toJson(jsonObject)).build();
	}


	public String getHostname() {
		String hostname = null;
		try {
			InetAddress inetAddress = InetAddress.getLocalHost();
			hostname = inetAddress.getHostName();
		} catch (UnknownHostException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return hostname;
	}

}