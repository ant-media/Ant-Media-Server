package io.antmedia.console.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Licence;
import io.antmedia.datastore.db.types.User;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;



@Api(value = "ManagementRestService")
@SwaggerDefinition(
		info = @Info(
				description = "Ant Media Server Management Panel REST API",
				version = "v2.0",
				title = "Ant Media Server Management Panel REST API",
				contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
				license = @License(name = "Apache 2.0", url = "http://www.apache.org")),
		consumes = {"application/json"},
		produces = {"application/json"},
		schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
		externalDocs = @ExternalDocs(value = "External Docs", url = "https://antmedia.io")
)
@Component
@Path("/v2")
public class RestServiceV2 extends CommonRestService {

	@ApiOperation(value = "Creates a new user. If user object is null or if user is not authenticated, new user won't be created.", response = Result.class)
	@POST
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result addUser(@ApiParam(value = "User object. If it is null, new user won't be created.", required = true, readOnly = true) User user) {
		return super.addUser(user);
	}

	@ApiOperation(value = "Edit the user in the server management panel's user list. It can change password or user type(admin, read only) ", response = List.class)
	@PUT
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result editUser(@ApiParam(value = "User to be edited. It finds the user with username.", required = true)User user) {
		return super.editUser(user);
	}

	@ApiOperation(value = "Delete the user from the server management panel's user list", response = List.class)
	@DELETE
	@Path("/users/{username}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Override
	public Result deleteUser(@ApiParam(value = "User name or e-mail of the user to be deleted", required = true) @PathParam("username") String userName) {
		return super.deleteUser(userName);
	}

	@ApiOperation(value = "Returns if user is blocked. User is blocked for a specific time if there are login attempts")
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/users/{usermail}/blocked")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result getBlockedStatus(@ApiParam(value="User name or e-mail of the user to check it status") @PathParam("usermail") String usermail) {
		return super.getBlockedStatus(usermail);
	}

	@ApiOperation(value = "Returns user list in the server management panel", response = List.class)
	@GET
	@Path("/user-list")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public List<User> getUserList() {
		return super.getUserList();
	}


	@ApiOperation(value = "Returns whether current user is admin or not. If user is admin, it can call POST/PUT/DELETE methods", response = Result.class)
	@GET
	@Path("/admin-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result isAdmin(){
		return super.isAdmin();
	}

	@ApiOperation(value = "Creates initial user. This is a one time scenario when initial user creation required and shouldn't be used otherwise. User object is required and can't be null", response = Result.class)
	@POST
	@Path("/users/initial")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result addInitialUser(@ApiParam(value = "User object. If it is null, new user won't be created.", required = true)User user) {
		return super.addInitialUser(user);
	}

	@ApiOperation(value = "Checks first login status. If server being logged in first time, it returns true, otherwise false.", response = Result.class)
	@GET
	@Path("/first-login-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result isFirstLogin()
	{
		return super.isFirstLogin();
	}


	/**
	 * Authenticates user with userName and password
	 *
	 *
	 * @param user: The User object to be authenticated
	 * @return json that shows user is authenticated or not
	 */
	@ApiOperation(value = "Authenticates user with given username and password. Requires user object to authenticate.", response = Result.class)
	@POST
	@Path("/users/authenticate")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result authenticateUser(@ApiParam(value = "User object to authenticate", required = true) User user) {
		return super.authenticateUser(user);
	}

	@ApiOperation(value = "Changes the given user's password.", response = Result.class)
	@POST
	@Path("/users/password")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result changeUserPassword(@ApiParam(value = "User object to change the password", required = true)User user) {
		return super.changeUserPassword(user);
	}


	@ApiOperation(value = "Returns true if user is authenticated to call rest api operations.", response = Result.class)
	@GET
	@Path("/authentication-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result isAuthenticatedRest(){
		return super.isAuthenticatedRest();
	}


	/*
	 * 	os.name						:Operating System Name
	 * 	os.arch						: x86/x64/...
	 * 	java.specification.version	: Java Version (Required 1.5 or 1.6 and higher to run Red5)
	 * 	-------------------------------
	 * 	Runtime.getRuntime()._____  (Java Virtual Machine Memory)
	 * 	===============================
	 * 	maxMemory()					: Maximum limitation
	 * 	totalMemory()				: Total can be used
	 * 	freeMemory()				: Availability
	 * 	totalMemory()-freeMemory()	: In Use
	 * 	availableProcessors()		: Total Processors available
	 * 	-------------------------------
	 *  getOperatingSystemMXBean()	(Actual Operating System RAM)
	 *	===============================
	 *  osCommittedVirtualMemory()	: Virtual Memory
	 *  osTotalPhysicalMemory()		: Total Physical Memory
	 *  osFreePhysicalMemory()		: Available Physical Memory
	 *  osInUsePhysicalMemory()		: In Use Physical Memory
	 *  osTotalSwapSpace()			: Total Swap Space
	 *  osFreeSwapSpace()			: Available Swap Space
	 *  osInUseSwapSpace()			: In Use Swap Space
	 *  -------------------------------
	 *  File						(Actual Harddrive Info: Supported for JRE 1.6)
	 *	===============================
	 *	osHDUsableSpace()			: Usable Space
	 *	osHDTotalSpace()			: Total Space
	 *	osHDFreeSpace()				: Available Space
	 *	osHDInUseSpace()			: In Use Space
	 **/

	@ApiOperation(value = "Returns system information which includes many information such as JVM memory, OS information, Available File Space, Physical memory informations in detail.", response = Result.class)
	@GET
	@Path("/system-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getSystemInfo() {
		return super.getSystemInfo();
	}


	/*
	 * 	Runtime.getRuntime()._____  (Java Virtual Machine Memory)
	 * 	===============================
	 * 	maxMemory()					: Maximum limitation
	 * 	totalMemory()				: Total can be used
	 * 	freeMemory()				: Availability
	 * 	totalMemory()-freeMemory()	: In Use
	 * 	availableProcessors()		: Total Processors available
	 */
	@ApiOperation(value = "Returns JVM memory informations. Max, total, free, in-use and available processors are returned.", response = Result.class)
	@GET
	@Path("/jvm-memory-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getJVMMemoryInfo() {
		return super.getJVMMemoryInfo();
	}


	/*
	 *  osCommittedVirtualMemory()	: Virtual Memory
	 *  osTotalPhysicalMemory()		: Total Physical Memory
	 *  osFreePhysicalMemory()		: Available Physical Memory
	 *  osInUsePhysicalMemory()		: In Use Physical Memory
	 *  osTotalSwapSpace()			: Total Swap Space
	 *  osFreeSwapSpace()			: Available Swap Space
	 *  osInUseSwapSpace()			: In Use Swap Space
	 */
	@ApiOperation(value = "Gets system memory status. Returns Virtual, total physical, available physical, currently in use, total swap space, available swap space and in use swap space. ", response = Result.class)
	@GET
	@Path("/system-memory-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getSystemMemoryInfo() {
		return super.getSystemMemoryInfo();
	}


	/*
	 *  File						(Actual Harddrive Info: Supported for JRE 1.6)
	 *	===============================
	 *	osHDUsableSpace()			: Usable Space
	 *	osHDTotalSpace()			: Total Space
	 *	osHDFreeSpace()				: Available Space
	 *	osHDInUseSpace()			: In Use Space
	 **/
	@ApiOperation(value = "Gets system file status. Returns usable space, total space, available space and in use space.", response = Result.class)
	@GET
	@Path("/file-system-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getFileSystemInfo() {
		return super.getFileSystemInfo();
	}

	/**
	 * getProcessCpuTime:  microseconds CPU time used by the process
	 *
	 * getSystemCpuLoad:	"% recent cpu usage" for the whole system.
	 *
	 * getProcessCpuLoad: "% recent cpu usage" for the Java Virtual Machine process.
	 * @return the CPU load info
	 */
	@ApiOperation(value = "Returns system cpu load, process cpu load and process cpu time.", response = Result.class)
	@GET
	@Path("/cpu-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getCPUInfo() {
		return super.getCPUInfo();
	}

	@ApiOperation(value = "Gets thread dump in plain text.Includes very detailed information such as thread name, thread id, blocked time of thread, thread state and many more information are returned.", response = Result.class)
	@GET
	@Path("/thread-dump")
	@Produces(MediaType.TEXT_PLAIN)
	@Override
	public String getThreadDump() {
		return super.getThreadDump();
	}

	@ApiOperation(value = "Gets thread dump in json format. Includes very detailed information such as thread name, thread id, blocked time of thread, thread state and many more information are returned.", response = Result.class)
	@GET
	@Path("/thread-dump")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getThreadDumpJSON() {
		return super.getThreadDumpJSON();
	}

	@ApiOperation(value = "Returns processor's thread information. Includes number of dead locked threads, thread count, and thread peek count.", response = Result.class)
	@GET
	@Path("/threads")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getThreadsInfo() {
		return super.getThreadsInfo();
	}



	// method path was already Restful
	// v2 is added to prevent clashes with older RestService.java
	@ApiOperation(value = "Returns heap dump.", response = Result.class)
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Returns the heap dump")})
	@GET
	@Path("/heap-dump")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Override
	public Response getHeapDump() {

		return super.getHeapDump();
	}


	/**
	 * Return server uptime and startime in milliseconds
	 * @return JSON object contains the server uptime and start time
	 */

	// method path was already Restful
	// v2 is added to prevent clashes with older RestService.java
	@ApiOperation(value = "Gets server time. Returns server uptime and start time in milliseconds in JSON.", response = Result.class)
	@GET
	@Path("/server-time")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getServerTime() {
		return super.getServerTime();
	}

	@ApiOperation(value = "Gets system resource information. Returns number of total live streams, cpu usage, system information, jvm information, file system information, license status, gpu status etc. Basically returns most of the information in one package.", response = Result.class)
	@GET
	@Path("/system-resources")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getSystemResourcesInfo() {

		return super.getSystemResourcesInfo();
	}


	@ApiOperation(value = "Gets GPU information. Returns whether you have GPU or not. If yes, information of the gpu and the number of total gpus.", response = Result.class)
	@GET
	@Path("/gpu-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getGPUInfo()
	{
		return super.getGPUInfo();
	}

	@ApiOperation(value = "Returns the version of Ant Media Server.", response = Result.class)
	@GET
	@Path("/version")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getVersion() {
		return super.getVersion();
	}

	@ApiOperation(value = "Gets the applications in the server. Returns the name of the applications in JSON format.", response = Result.class)
	@GET
	@Path("/applications")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getApplications() {

		return super.getApplications();
	}

	/**
	 * Refactor name getTotalLiveStreamSize
	 * only return totalLiveStreamSize
	 * @return the number of live clients
	 */
	@ApiOperation(value = "Returns total number of live streams and total number of connections.", response = Result.class)
	@GET
	@Path("/live-clients-size")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getLiveClientsSize()
	{

		return super.getLiveClientsSize();
	}

	@ApiOperation(value = "Gets application info. Application info includes live stream count, vod count and application name.", response = Result.class)
	@GET
	@Path("/applications-info")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getApplicationInfo() {

		return super.getApplicationInfo();
	}

	/**
	 * Refactor remove this function and use ProxyServlet to get this info
	 * Before deleting check web panel does not use it
	 * @param name: application name
	 * @return live streams in the application
	 */
	@ApiOperation(value = "Returns live streams in the specified application. Retrieves broadcast names and the consumer size.", response = Result.class)
	@GET
	@Path("/applications/live-streams/{appname}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getAppLiveStreams(@ApiParam(value = "Application name", required = true) @PathParam("appname") String name) {

		return super.getAppLiveStreams(name);
	}

	@ApiOperation(value = "Changes the application settings with the given settings. Null fields will be set to default values.", response = Result.class)
	@POST
	@Path("/applications/settings/{appname}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public String changeSettings(@ApiParam(value = "Application name", required = true) @PathParam("appname") String appname, @ApiParam(value = "New application settings, null fields will be set to default values", required = true) AppSettings newSettings){

		return super.changeSettings(appname, newSettings);
	}


	@ApiOperation(value = "Checks whether application or applications have shutdown properly or not.", response = Result.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Returns the shutdown status of entered applications."),
			@ApiResponse(code = 400, message = "Either entered in wrong format or typed incorrectly application names")})
	@GET
	@Path("/shutdown-proper-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Response isShutdownProperly(@ApiParam(value = "Application name", required = true) @QueryParam("appNames") String appNamesArray)
	{
		return super.isShutdownProperly(appNamesArray);
	}

	@ApiOperation(value = "Set application or applications shutdown property to true", response = Result.class)
	@GET
	@Path("/shutdown-properly")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public boolean setShutdownStatus(@ApiParam(value = "Application name", required = true) @QueryParam("appNames") String appNamesArray){

		return super.setShutdownStatus(appNamesArray);
	}

	@ApiOperation(value = "Changes server settings. Sets Server Name, license key, market build status and node group.", response = Result.class)
	@POST
	@Path("/server-settings")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public String changeServerSettings(@ApiParam(value = "Server settings", required = true) ServerSettings serverSettings){
		return super.changeServerSettings(serverSettings);
	}

	@ApiOperation(value = "Changes ssl settings. Sets ssl configuration type. After this method is called, server will be restarted.", response = Result.class)
	@POST
	@Path("/ssl-settings")
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result configureSsl(@ApiParam(value = "SSL settings", required = true) @QueryParam("domain") String domain, @QueryParam("type") String type,
							   @FormDataParam("fullChainFile") InputStream fullChainFile,
							   @FormDataParam("fullChainFile") FormDataContentDisposition fullChainFileDetail,
							   @FormDataParam("privateKeyFile") InputStream privateKeyFile,
							   @FormDataParam("privateKeyFile") FormDataContentDisposition privateKeyFileDetail,
							   @FormDataParam("chainFile") InputStream chainFile,
							   @FormDataParam("chainFile") FormDataContentDisposition chainFileDetail)

	{
		return super.configureSsl(domain, type, fullChainFile, fullChainFileDetail, privateKeyFile, privateKeyFileDetail, chainFile, chainFileDetail);
	}

	@ApiOperation(value = "Returns true if the server is enterprise edition.", response = Result.class)
	@GET
	@Path("/enterprise-edition")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result isEnterpriseEdition(){

		return super.isEnterpriseEdition();
	}

	@ApiOperation(value = "Returns the specified application settings", response = Result.class)
	@GET
	@Path("/applications/settings/{appname}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public AppSettings getSettings(@ApiParam(value = "Application name", required = true) @PathParam("appname") String appname)
	{
		return super.getSettings(appname);
	}

	@ApiOperation(value = "Returns the server settings. From log level to measurement period of cpu, license key of the server host address,ssl configuration and many more settings are returned at once.", response = Result.class)
	@GET
	@Path("/server-settings")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public ServerSettings getServerSettings()
	{
		return super.getServerSettings();
	}

	@ApiOperation(value = "Returns license status. Includes license ID, status, owner, start date, end date, type and license count.", response = Result.class)
	@GET
	@Path("/licence-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Licence getLicenceStatus(@ApiParam(value = "License key", required = true) @QueryParam("key") String key)
	{
		return super.getLicenceStatus(key);
	}

	@ApiOperation(value = "Returns the last checked license status. Includes license ID, owner, start date, end date, type and license count.", response = Result.class)
	@GET
	@Path("/last-licence-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Licence getLicenceStatus()
	{
		return super.getLicenceStatus();
	}

	/**
	 * This method resets the viewers counts and broadcast status in the db.
	 * This should be used to recover db after server crashes.
	 * It's not intended to use to ignore the crash
	 * @param appname the application name that broadcasts will be reset
	 * @return
	 */
	@ApiOperation(value = "Resets the viewer counts and broadcasts statuses in the db. This can be used after server crashes to recover db. It's not intended to use to ignore the crash.", response = Result.class)
	@POST
	@Path("/applications/{appname}/reset")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result resetBroadcast(@ApiParam(value = "Application name", required = true) @PathParam("appname") String appname)
	{
		return super.resetBroadcast(appname);
	}

	@ApiOperation(value = "Returns the server mode. If it is in the cluster mode, result will be true.", response = Result.class)
	@GET
	@Path("/cluster-mode-status")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result isInClusterMode(){
		return super.isInClusterMode();
	}



	@ApiOperation(value = "Gets log file. Char size of the log, offset or log type can be specified.", response = Result.class)
	@GET
	@Path("/log-file/{offsetSize}/{charSize}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getLogFile(@ApiParam(value = "Char size of the log", required = true) @PathParam("charSize") int charSize, @ApiParam(value = "Log type. ERROR can be used to get only error logs", required = true) @QueryParam("logType") String logType,
							 @ApiParam(value = "Offset of the retrieved log", required = true) @PathParam("offsetSize") long offsetSize) throws IOException {
		return super.getLogFile(charSize,logType, offsetSize);
	}

	/**
	 * Create application. It supports both default or custom app
	 *
	 * How Custom App Creation works
	 * 1. Save the custom war file to tmp directory
	 * 2. Install the app from the tmp directory
	 * 3. If it's in cluster mode, create a symbolic link in root app to let other apps download the app
	 *
	 */
	@ApiOperation(value = "Creates a new application with given name. It just creates default app", response = Result.class)
	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Path("/applications/{appName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result createApplication(@ApiParam(value = "Name for the new application", required = true) @PathParam("appName") String appName) {
		return createApplication(appName, null);
	}

	@ApiOperation(value = "Creates a new application with given name. It supports uploading custom WAR files", response = Result.class)
	@PUT
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Path("/applications/{appName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result createApplication(@ApiParam(value = "Name for the new application", required = true) @PathParam("appName") String appName, @ApiParam(value = "file", required = true) @FormDataParam("file") InputStream inputStream)
	{
		Result result;
		if (appName != null && appName.matches("^[a-zA-Z0-9]*$"))
		{
			List<String> applications = getApplication().getApplications();

			boolean applicationAlreadyExist = false;
			for (String applicationName : applications)
			{
				if (applicationName.equalsIgnoreCase(appName))
				{
					applicationAlreadyExist = true;
					break;
				}
			}

			if (!applicationAlreadyExist)
			{
				result = super.createApplication(appName, inputStream);
			}
			else
			{
				result = new Result(false, "Application with the same name already exists");
			}
		}
		else
		{
			result = new Result(false, "Application name is not alphanumeric. Please provide alphanumeric characters");
		}

		return result;
	}


	@ApiOperation(value = "Deletes application with the given name.", response = Result.class)
	@DELETE
	@Path("/applications/{appName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result deleteApplication(@ApiParam(value = "Name of the application to delete", required = true) @PathParam("appName") String appName,
									@QueryParam("deleteDB") boolean deleteDB) {
		if (appName != null) {
			return super.deleteApplication(appName, deleteDB);
		}
		return new Result(false, "Application name is not defined");
	}


	@ApiOperation(value = "Returns the hostname to check liveness with HTTP type healthcheck.", response = Response.class)
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