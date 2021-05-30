package io.antmedia.console.rest;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Licence;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.User;
import io.antmedia.settings.ServerSettings;

@Component
@Path("/")
public class RestService extends CommonRestService {


	/**
	 * Add user account on db. 
	 * Username must be unique,
	 * if there is a user with the same name, user will not be created
	 * 
	 * userType = 0 means ready only account
	 * userType = 1 means read-write account
	 * 
	 * Post method should be used.
	 * 
	 * application/json
	 * 
	 * form parameters - case sensitive
	 * "userName", "password", "userType
	 * 
	 * @param user: The user to be added
	 * @return JSON data
	 * if user is added success will be true
	 * if user is not added success will be false
	 * 	if user is not added, errorId = 1 means username already exist
	 */
	@POST
	@Path("/addUser")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result addUser(User user) {
		return super.addUser(user);
	}
	
	@GET
	@Path("/isAdmin")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result isAdmin(){
		return super.isAdmin();
	}

	@POST
	@Path("/addInitialUser")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result addInitialUser(User user) {

		return super.addInitialUser(user);
	}

	@GET
	@Path("/isFirstLogin")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result isFirstLogin() 
	{

		return super.isFirstLogin();
	}

	/**
	 * Edit user account on db. 
	 * Username cannot be changed, password or userType can be changed
	 * Post method should be used
	 * 
	 * @param user
	 * @return JSON data
	 * if user is edited, success will be true
	 * if not, success will be false
	 */

	@POST
	@Path("/editUser")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result editUser(User user) {
		return super.editUser(user);
	}

	/**
	 * Deletes user account from database
	 * @param username
	 * @return
	 */
	@DELETE
	@Path("/deleteUser/{username}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Override
	public Result deleteUser(@PathParam("username") String userName) {
		return super.deleteUser(userName);
	}



	/**
	 * Authenticates user with userName and password
	 * 
	 * 
	 * @param user: The User object to be authenticated 
	 * @return json that shows user is authenticated or not
	 */
	@POST
	@Path("/authenticateUser")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result authenticateUser(User user) {
		return super.authenticateUser(user);
	}


	@POST
	@Path("/changeUserPassword")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result changeUserPassword(User user) {

		return super.changeUserPassword(user);

	}
	@GET
	@Path("/userList")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public List<User> getUserList() {
		return super.getUserList();
	}


	@GET
	@Path("/isAuthenticated")
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


	@GET
	@Path("/getSystemInfo")
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
	@GET
	@Path("/getJVMMemoryInfo")
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
	@GET
	@Path("/getSystemMemoryInfo")
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
	@GET
	@Path("/getFileSystemInfo")
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
	@GET
	@Path("/getCPUInfo")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getCPUInfo() {
		return super.getCPUInfo();
	}
	
	@GET
	@Path("/thread-dump-raw")
	@Produces(MediaType.TEXT_PLAIN)
	@Override
	public String getThreadDump() {
		return super.getThreadDump();
	}
	
	@GET
	@Path("/thread-dump-json")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getThreadDumpJSON() {
		return super.getThreadDumpJSON();
	}
	
	
	@GET
	@Path("/threads-info")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getThreadsInfo() {
		return super.getThreadsInfo();
	}
	
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
	@GET
	@Path("/server-time")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getServerTime() {
		return super.getServerTime();
	}

	@GET
	@Path("/getSystemResourcesInfo")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getSystemResourcesInfo() {

		return super.getSystemResourcesInfo();
	}

	@GET
	@Path("/getGPUInfo")
	@Produces(MediaType.APPLICATION_JSON) 
	@Override
	public String getGPUInfo() 
	{
		return super.getGPUInfo();
	}


	@GET
	@Path("/getVersion")
	@Produces(MediaType.APPLICATION_JSON) 
	@Override
	public String getVersion() {
		return super.getVersion();
	}


	@GET
	@Path("/getApplications")
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
	@GET
	@Path("/getLiveClientsSize")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getLiveClientsSize() 
	{

		return super.getLiveClientsSize();
	}

	@GET
	@Path("/getApplicationsInfo")
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
	@Deprecated
	@GET
	@Path("/getAppLiveStreams/{appname}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getAppLiveStreams(@PathParam("appname") String name) {

		return super.getAppLiveStreams(name);
	}


	/**
	 * Refactor remove this function and use ProxyServlet to get this info
	 * Before deleting check web panel does not use it
	 * @param name application name
	 * @param streamName the stream name to be deleted
	 * @return operation value
	 */
	@Deprecated
	@POST
	@Path("/deleteVoDStream/{appname}")
	@Produces(MediaType.APPLICATION_JSON)
	public String deleteVoDStream(@PathParam("appname") String name, @FormParam("streamName") String streamName) {
		return super.deleteVoDStream(name, streamName);
	}


	@POST
	@Path("/changeSettings/{appname}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public String changeSettings(@PathParam("appname") String appname, AppSettings newSettings){
		return super.changeSettings(appname, newSettings);
	}
	
	
	@Deprecated
	@GET
	@Path("/isShutdownProperly")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean getShutdownStatus(@QueryParam("appNames") String appNamesArray){

		return super.getShutdownStatus(appNamesArray);
	}
	
	
	@GET
	@Path("/shutdown-properly")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Response isShutdownProperly(@QueryParam("appNames") String appNamesArray)
	{
		return super.isShutdownProperly(appNamesArray);
	}
	
	
	@GET
	@Path("/setShutdownProperly")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public boolean setShutdownStatus(@QueryParam("appNames") String appNamesArray){

		return super.setShutdownStatus(appNamesArray);
	}

	@POST
	@Path("/changeServerSettings")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public String changeServerSettings(ServerSettings serverSettings){
		return super.changeServerSettings(serverSettings);
	}

	@GET
	@Path("/isEnterpriseEdition")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result isEnterpriseEdition(){

		return super.isEnterpriseEdition();
	}

	@GET
	@Path("/getSettings/{appname}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public AppSettings getSettings(@PathParam("appname") String appname) 
	{

		return super.getSettings(appname);
	}

	@GET
	@Path("/getServerSettings")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public ServerSettings getServerSettings() 
	{
		return super.getServerSettings();
	}

	@GET
	@Path("/getLicenceStatus")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Licence getLicenceStatus(@QueryParam("key") String key) 
	{

		return super.getLicenceStatus(key);
	}

	@GET
	@Path("/getLastLicenceStatus")
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
	@POST
	@Path("/reset-broadcasts/{appname}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public Result resetBroadcast(@PathParam("appname") String appname) 
	{
		return super.resetBroadcast(appname);
	}

	
	@GET
	@Path("/isInClusterMode")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result isInClusterMode()
	{
		return super.isInClusterMode();
	}

	@GET
	@Path("/changeLogLevel/{level}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String changeLogSettings(@PathParam("level") String logLevel)
	{
		return super.changeLogSettings(logLevel);
	}

	@GET
	@Path("/getLogFile/{offsetSize}/{charSize}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public String getLogFile(@PathParam("charSize") int charSize, @QueryParam("logType") String logType,
			@PathParam("offsetSize") long offsetSize) throws IOException {

		return super.getLogFile(charSize,logType, offsetSize);
	}

	
	@POST
	@Path("/applications")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result createApplication(@QueryParam("appName") String appName) {

		return super.createApplication(appName);
	}
	
	@DELETE
	@Path("/applications/{appName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result deleteApplication(@PathParam("appName") String appName) {

		return super.deleteApplication(appName);
	}

}
