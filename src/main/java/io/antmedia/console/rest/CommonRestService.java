package io.antmedia.console.rest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.FormParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Hex;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import ch.qos.logback.classic.Level;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.SystemUtils;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.console.AdminApplication;
import io.antmedia.console.AdminApplication.ApplicationInfo;
import io.antmedia.console.AdminApplication.BroadcastInfo;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.console.datastore.IConsoleDataStore;
import io.antmedia.datastore.db.types.Licence;
import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.licence.ILicenceService;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.User;
import io.antmedia.rest.model.UserType;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.StatsCollector;


public class CommonRestService {

	private static final String LOG_TYPE_ERROR = "error";

	private static final String FILE_NOT_EXIST = "There is no log yet";

	private static final String ERROR_LOG_LOCATION = "log/antmedia-error.log";

	private static final String SERVER_LOG_LOCATION = "log/ant-media-server.log";

	private static final String LOG_CONTENT = "logContent";

	private static final String LOG_CONTENT_SIZE = "logContentSize";

	private static final String LOG_FILE_SIZE = "logFileSize";

	private static final int MAX_CHAR_SIZE = 512000;

	private static final String LOG_LEVEL_ALL = "ALL";

	private static final String LOG_LEVEL_TRACE = "TRACE";

	private static final String LOG_LEVEL_DEBUG = "DEBUG";

	private static final String LOG_LEVEL_INFO = "INFO";

	private static final String LOG_LEVEL_WARN = "WARN";

	private static final String LOG_LEVEL_ERROR = "ERROR";

	private static final String LOG_LEVEL_OFF = "OFF";

	private static final String USER_PASSWORD = "user.password";

	public static final String USER_EMAIL = "user.email";

	public static final String IS_AUTHENTICATED = "isAuthenticated";

	public static final String SERVER_NAME = "server.name";

	public static final String LICENSE_KEY = "server.licence_key";

	public static final String MARKET_BUILD = "server.market_build";

	public static final String NODE_GROUP = "nodeGroup";

	Gson gson = new Gson();

	private IConsoleDataStore dataStore;

	private static final String LOG_LEVEL = "logLevel";

	private static final String RED5_PROPERTIES_PATH = "conf/red5.properties";

	protected static final Logger logger = LoggerFactory.getLogger(CommonRestService.class);

	private static final String LICENSE_STATUS = "license";

	protected ApplicationContext applicationContext;

	@Context
	private ServletContext servletContext;

	@Context
	private HttpServletRequest servletRequest;

	private ConsoleDataStoreFactory dataStoreFactory;
	private ServerSettings serverSettings;

	private ILicenceService licenceService;




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

	public Result addUser(User user) {
		boolean result = false;
		String message = "";
		HttpSession session = servletRequest.getSession();
		User currentUser = getDataStore().getUser(session.getAttribute(USER_EMAIL).toString());
		if (user != null) 
		{
			if (!getDataStore().doesUsernameExist(user.getEmail())) 
			{
				result = getDataStore().addUser(user.getEmail(), getMD5Hash(user.getPassword()), user.getUserType());
				logger.info("added user = {} password = {} user type = {}", user.getEmail(), user.getPassword()  ,user.getUserType());
				logger.info("current user = {} user type = {}", currentUser.getEmail(),  currentUser.getUserType());
			}
			else {
				message = "User with the same e-mail already exists";
			}
		}
		else {
			message = "User object is null";
		}


		Result operationResult = new Result(result);
		operationResult.setMessage(message);

		return operationResult;
	}




	public Result addInitialUser(User user) {
		boolean result = false;
		int errorId = -1;
		if (getDataStore().getNumberOfUserRecords() == 0) {
			result = getDataStore().addUser(user.getEmail(), getMD5Hash(user.getPassword()), UserType.ADMIN);
		}

		Result operationResult = new Result(result);
		operationResult.setErrorId(errorId);
		return operationResult;
	}


	public Result isFirstLogin() 
	{
		boolean result = false;
		if (getDataStore().getNumberOfUserRecords() == 0) {
			result = true;
		}
		return new Result(result);
	}

	/**
	 * Edit user account on db. 
	 * Username cannot be changed, password or userType can be changed
	 * userType = 0 means ready only account
	 * userType = 1 means read-write account
	 * 
	 * Post method should be used.
	 * 
	 * application/x-www-form-urlencoded
	 * 
	 * form parameters - case sensitive
	 * "userName", "password", "userType
	 * 
	 * @param userName
	 * @return JSON data
	 * if user is edited, success will be true
	 * if not, success will be false
	 * 	errorId = 2 means user does not exist
	 */
	/*
	@POST
	@Path("/editUser")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public OperationResult editUser(@FormParam("userName") String userName, @FormParam("password") String password, @FormParam("userType") Integer userType) {
		//TODO: check that request is coming from authorized user
		boolean result = false;
		int errorId = -1;
		if (userName != null && getDataStore().doesUsernameExist(userName)) {
			result = getDataStore().editUser(userName, password, userType);
		}
		else {
			errorId = 2;
		}

		OperationResult operationResult = new OperationResult(result);
		operationResult.setErrorId(errorId);
		return operationResult;
	}
	 */

	/**
	 * Deletes user account from db
	 * 
	 * Post method should be used.
	 * 
	 * application/x-www-form-urlencoded
	 * 
	 * form parameters - case sensitive
	 * "userName"
	 * 
	 * @param userName
	 * @return
	 */
	/*
	@POST
	@Path("/deleteUser")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public OperationResult deleteUser(@FormParam("userName") String userName) {
		//TODO: check that request is coming from authorized user
		boolean result = getDataStore().deleteUser(userName);
		return new OperationResult(result);
	}
	 */



	/**
	 * Authenticates user with userName and password
	 * 
	 * 
	 * @param user: The User object to be authenticated 
	 * @return json that shows user is authenticated or not
	 */

	public Result authenticateUser(User user) {
		boolean result=getDataStore().doesUserExist(user.getEmail(), user.getPassword()) ||
				getDataStore().doesUserExist(user.getEmail(), getMD5Hash(user.getPassword()));
		if (result) {
			HttpSession session = servletRequest.getSession();
			session.setAttribute(IS_AUTHENTICATED, true);
			session.setAttribute(USER_EMAIL, user.getEmail());
			session.setAttribute(USER_PASSWORD, getMD5Hash(user.getPassword()));
		}
		return new Result(result);
	}

	public Result isAdmin() {
		HttpSession session = servletRequest.getSession();
		if(isAuthenticated(session)) {
			User currentUser = getDataStore().getUser(session.getAttribute(USER_EMAIL).toString());
			if (currentUser.getUserType().equals(UserType.ADMIN)) {
				return new Result(true, "User is admin");
			}
		}
		return new Result(false, "User is not admin");
	}

	public Result editUser(User user) 
	{
		boolean result = false;
		String message = "";
		HttpSession session = servletRequest.getSession();
		String userEmail = (String)session.getAttribute(USER_EMAIL);

		if (user != null && user.getEmail() != null && getDataStore().doesUsernameExist(user.getEmail())) 
		{
			if (!userEmail.equals(user.getEmail())) 
			{
				User oldUser = getDataStore().getUser(user.getEmail());
				getDataStore().deleteUser(user.getEmail());
				if(user.getNewPassword() != null && !user.getNewPassword().isEmpty()) {
					logger.info("Changing password of user: {}",  user.getEmail());
					result = getDataStore().addUser(user.getEmail(), getMD5Hash(user.getNewPassword()), user.getUserType());
				}
				else {
					logger.info("Changing type of user: {}" , user.getEmail());
					result = getDataStore().addUser(user.getEmail(), oldUser.getPassword(), user.getUserType());
				}
			}
			else {
				message = "User cannot edit itself";
			}
			
		} 
		else {
			message = "Edited user is not found in database";
		}
		
		return new Result(result, message);
	}

	public Result deleteUser(String userName) {
		HttpSession session = servletRequest.getSession();
		String userEmail = (String) session.getAttribute(USER_EMAIL);
		boolean result = false;
		String message = "";

		if (!userEmail.equals(userName)) {
			result = getDataStore().deleteUser(userName);
		}
		else {
			message = "You cannot delete yourself";
		}

		if(result)
			logger.info("Deleted user: {} ", userName);
		else
			logger.info("Could not find and delete user: {}" , userName);

		return new Result(result, message);
	}


	public List<User> getUserList() {
		return getDataStore().getUserList();
	}


	public Result changeUserPassword(User user) {

		String userMail = (String)servletRequest.getSession().getAttribute(USER_EMAIL);

		return changeUserPasswordInternal(userMail, user);

	}

	public Result changeUserPasswordInternal(String userMail, User user) {
		boolean result = false;
		String message = null;
		if (userMail != null) {
			result = getDataStore().doesUserExist(userMail, user.getPassword()) || getDataStore().doesUserExist(userMail, getMD5Hash(user.getPassword()));
			if (result) {
				result = getDataStore().editUser(userMail, getMD5Hash(user.getNewPassword()), UserType.ADMIN);

				if (result) {
					HttpSession session = servletRequest.getSession();
					if (session != null) {
						session.setAttribute(IS_AUTHENTICATED, true);
						session.setAttribute(USER_EMAIL, userMail);
						session.setAttribute(USER_PASSWORD, getMD5Hash(user.getNewPassword()));
					}
				}
			}
			else {
				message = "User not exist with that name and pass";
			}
		}
		else {
			message = "User name does not exist in context";
		}

		return new Result(result, message);
	}

	public Result isAuthenticatedRest(){
		return new Result(isAuthenticated(servletRequest.getSession()));
	}

	public static boolean isAuthenticated(HttpSession session) 
	{
		Object isAuthenticated = session.getAttribute(IS_AUTHENTICATED);
		Object userEmail = session.getAttribute(USER_EMAIL);
		Object userPassword = session.getAttribute(USER_PASSWORD);
		boolean result = false;
		if (isAuthenticated != null && userEmail != null && userPassword != null) {
			result = true;
		}
		return result;
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



	public String getSystemInfo() {
		return gson.toJson(StatsCollector.getSystemInfoJSObject());
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

	public String getJVMMemoryInfo() {
		return gson.toJson(StatsCollector.getJVMMemoryInfoJSObject());
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

	public String getSystemMemoryInfo() {
		return gson.toJson(StatsCollector.getSysteMemoryInfoJSObject());
	}


	/*
	 *  File						(Actual Harddrive Info: Supported for JRE 1.6)
	 *	===============================
	 *	osHDUsableSpace()			: Usable Space
	 *	osHDTotalSpace()			: Total Space
	 *	osHDFreeSpace()				: Available Space
	 *	osHDInUseSpace()			: In Use Space
	 **/

	public String getFileSystemInfo() {
		return gson.toJson(StatsCollector.getFileSystemInfoJSObject());
	}

	/**
	 * getProcessCpuTime:  microseconds CPU time used by the process
	 * 
	 * getSystemCpuLoad:	"% recent cpu usage" for the whole system. 
	 * 
	 * getProcessCpuLoad: "% recent cpu usage" for the Java Virtual Machine process. 
	 * @return the CPU load info
	 */

	public String getCPUInfo() {
		return gson.toJson(StatsCollector.getCPUInfoJSObject());
	}


	public String getThreadDump() {
		return Arrays.toString(StatsCollector.getThreadDump());
	}


	public String getThreadDumpJSON() {
		return gson.toJson(StatsCollector.getThreadDumpJSON());
	}



	public String getThreadsInfo() {
		return gson.toJson(StatsCollector.getThreadInfoJSONObject());
	}


	public Response getHeapDump() {
		SystemUtils.getHeapDump(SystemUtils.HEAPDUMP_HPROF);
		File file = new File(SystemUtils.HEAPDUMP_HPROF);
		return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
				.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"" ) //optional
				.build();
	}



	/**
	 * Return server uptime and startime in milliseconds
	 * @return JSON object contains the server uptime and start time
	 */

	public String getServerTime() {
		return gson.toJson(StatsCollector.getServerTime());
	}


	public String getSystemResourcesInfo() {

		AdminApplication application = getApplication();
		IScope rootScope = application.getRootScope();

		//add live stream size
		int totalLiveStreams = 0;
		Queue<IScope> scopes = new LinkedList<>();
		List<String> appNames = application.getApplications();
		for (String name : appNames) 
		{
			IScope scope = rootScope.getScope(name);
			scopes.add(scope);
			totalLiveStreams += application.getAppLiveStreamCount(scope);
		}

		JsonObject jsonObject = StatsCollector.getSystemResourcesInfo(scopes);

		jsonObject.addProperty(StatsCollector.TOTAL_LIVE_STREAMS, totalLiveStreams);

		jsonObject.add(LICENSE_STATUS, gson.toJsonTree(getLicenceStatus()));

		return gson.toJson(jsonObject);
	}


	public String getGPUInfo() 
	{
		return gson.toJson(StatsCollector.getGPUInfoJSObject());
	}



	public String getVersion() {
		return gson.toJson(RestServiceBase.getSoftwareVersion());
	}



	public String getApplications() {
		List<String> applications = getApplication().getApplications();
		JsonObject jsonObject = new JsonObject();
		JsonArray jsonArray = new JsonArray();

		for (String appName : applications) {
			if (!appName.equals(AdminApplication.APP_NAME)) {
				jsonArray.add(appName);
			}
		}
		jsonObject.add("applications", jsonArray);
		return gson.toJson(jsonObject);
	}

	/**
	 * Refactor name getTotalLiveStreamSize
	 * only return totalLiveStreamSize
	 * @return the number of live clients
	 */

	public String getLiveClientsSize() 
	{
		int totalConnectionSize = getApplication().getTotalConnectionSize();
		int totalLiveStreamSize = getApplication().getTotalLiveStreamSize();
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("totalConnectionSize", totalConnectionSize);
		jsonObject.addProperty(StatsCollector.TOTAL_LIVE_STREAMS, totalLiveStreamSize);

		return gson.toJson(jsonObject);
	}


	public String getApplicationInfo() {
		List<ApplicationInfo> info = getApplication().getApplicationInfo();
		return gson.toJson(info);
	}

	/**
	 * Refactor remove this function and use ProxyServlet to get this info
	 * Before deleting check web panel does not use it
	 * @param name: application name 
	 * @return live streams in the application
	 */

	public String getAppLiveStreams(@PathParam("appname") String name) {
		List<BroadcastInfo> appLiveStreams = getApplication().getAppLiveStreams(name);
		return gson.toJson(appLiveStreams);
	}


	/**
	 * Refactor remove this function and use ProxyServlet to get this info
	 * Before deleting check web panel does not use it
	 * @param name application name
	 * @param streamName the stream name to be deleted
	 * @return operation value
	 */

	public String deleteVoDStream(@PathParam("appname") String name, @FormParam("streamName") String streamName) {
		boolean deleteVoDStream = getApplication().deleteVoDStream(name, streamName);
		return gson.toJson(new Result(deleteVoDStream));
	}



	public String changeSettings(@PathParam("appname") String appname, AppSettings newSettings){
		AntMediaApplicationAdapter adapter = ((IApplicationAdaptorFactory) getApplication().getApplicationContext(appname).getBean(AntMediaApplicationAdapter.BEAN_NAME)).getAppAdaptor();
		return gson.toJson(new Result(adapter.updateSettings(newSettings, true)));
	}



	public boolean getShutdownStatus(@QueryParam("appNames") String appNamesArray){

		boolean appShutdownProblemExists = false;
		if (appNamesArray != null) 
		{
			String[] appNames = appNamesArray.split(",");

			if (appNames != null) 
			{
				for (String appName : appNames) 
				{
					//Check apps shutdown properly
					AntMediaApplicationAdapter appAdaptor = getAppAdaptor(appName);
					if (!appAdaptor.isShutdownProperly()) {
						appShutdownProblemExists = true;
						break;
					};

				}
			}
		}

		return !appShutdownProblemExists;
	}

	public AntMediaApplicationAdapter getAppAdaptor(String appName) {

		AntMediaApplicationAdapter appAdaptor = null;
		AdminApplication application = getApplication();
		if (application != null) 
		{
			ApplicationContext context = application.getApplicationContext(appName);
			if (context != null) 
			{
				IApplicationAdaptorFactory adaptorFactory = (IApplicationAdaptorFactory) context.getBean(AntMediaApplicationAdapter.BEAN_NAME);
				if (adaptorFactory != null) 
				{
					appAdaptor = adaptorFactory.getAppAdaptor();
				}
			}
		}
		return appAdaptor;
	}



	public Response isShutdownProperly(@QueryParam("appNames") String appNamesArray)
	{
		boolean appShutdownProblemExists = false;
		Response response = null;

		if (appNamesArray != null) 
		{
			String[] appNames = appNamesArray.split(",");

			if (appNames != null) 
			{
				for (String appName : appNames) 
				{
					//Check apps shutdown properly
					AntMediaApplicationAdapter appAdaptor = getAppAdaptor(appName);
					if (appAdaptor != null) 
					{
						if (!appAdaptor.isShutdownProperly()) {
							appShutdownProblemExists = true;
							break;
						}
					}
					else {
						response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Result(false, "Either server may not be initialized or application name that does not exist is requested. ")).build();
						break;
					}
				}
			}
			else {
				response = Response.status(Status.BAD_REQUEST).entity(new Result(false, "Bad parameter for appNames. ")).build();
			}

		}
		else {
			response = Response.status(Status.BAD_REQUEST).entity(new Result(false, "Bad parameter for appNames. ")).build();
		}

		if (response == null) {
			response = Response.status(Status.OK).entity(new Result(!appShutdownProblemExists)).build();
		}

		return response; 
	}



	public boolean setShutdownStatus(@QueryParam("appNames") String appNamesArray){

		String[] appNames = appNamesArray.split(",");
		boolean result = true;

		for (String appName : appNames) {
			//Check apps shutdown properly
			if(!((IApplicationAdaptorFactory) getApplication().getApplicationContext(appName).getBean(AntMediaApplicationAdapter.BEAN_NAME)).getAppAdaptor().isShutdownProperly()) {
				((IApplicationAdaptorFactory) getApplication().getApplicationContext(appName).getBean(AntMediaApplicationAdapter.BEAN_NAME)).getAppAdaptor().setShutdownProperly(true);
			}
		}

		return result;
	}


	public String changeServerSettings(ServerSettings serverSettings){

		PreferenceStore store = new PreferenceStore(RED5_PROPERTIES_PATH);

		String serverName = "";
		String licenceKey = "";
		if(serverSettings.getServerName() != null) {
			serverName = serverSettings.getServerName();
		}

		store.put(SERVER_NAME, serverName);
		getServerSettingsInternal().setServerName(serverName);

		if (serverSettings.getLicenceKey() != null) {
			licenceKey = serverSettings.getLicenceKey();
		}

		store.put(LICENSE_KEY, licenceKey);
		getServerSettingsInternal().setLicenceKey(licenceKey);

		store.put(MARKET_BUILD, String.valueOf(serverSettings.isBuildForMarket()));
		getServerSettingsInternal().setBuildForMarket(serverSettings.isBuildForMarket());

		store.put(NODE_GROUP, String.valueOf(serverSettings.getNodeGroup()));
		getServerSettingsInternal().setNodeGroup(serverSettings.getNodeGroup());
		
		ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

		if(LOG_LEVEL_ALL.equals(serverSettings.getLogLevel()) || LOG_LEVEL_TRACE.equals(serverSettings.getLogLevel()) 
				|| LOG_LEVEL_DEBUG.equals(serverSettings.getLogLevel()) || LOG_LEVEL_INFO.equals(serverSettings.getLogLevel()) 
				|| LOG_LEVEL_WARN.equals(serverSettings.getLogLevel())  || LOG_LEVEL_ERROR.equals(serverSettings.getLogLevel())
				|| LOG_LEVEL_OFF.equals(serverSettings.getLogLevel())) 
		{

			rootLogger.setLevel(currentLevelDetect(serverSettings.getLogLevel()));

			store.put(LOG_LEVEL, serverSettings.getLogLevel());
			getServerSettingsInternal().setLogLevel(serverSettings.getLogLevel());
		}

		return gson.toJson(new Result(store.save()));
		

	}


	public Result isEnterpriseEdition(){
		boolean isEnterprise = RestServiceBase.isEnterprise();
		return new Result(isEnterprise, "");
	}


	public AppSettings getSettings(@PathParam("appname") String appname) 
	{
		AdminApplication application = getApplication();
		if (application != null) {
			ApplicationContext context = application.getApplicationContext(appname);
			if (context != null) {
				IApplicationAdaptorFactory adaptorFactory = (IApplicationAdaptorFactory)context.getBean(AntMediaApplicationAdapter.BEAN_NAME);
				if (adaptorFactory != null) {
					AntMediaApplicationAdapter adapter = adaptorFactory.getAppAdaptor();
					if (adapter != null) {
						return adapter.getAppSettings();
					}
				}
			}
		}
		logger.warn("getSettings for app: {} returns null. It's likely not initialized.", appname);
		return null;
	}


	public ServerSettings getServerSettings() 
	{
		return getServerSettingsInternal();
	}


	public Licence getLicenceStatus(@QueryParam("key") String key) 
	{
		if(key == null) {
			return null;
		}
		return getLicenceServiceInstance().checkLicence(key);
	}


	public Licence getLicenceStatus() 
	{
		return getLicenceServiceInstance().getLastLicenseStatus();
	}

	/**
	 * This method resets the viewers counts and broadcast status in the db. 
	 * This should be used to recover db after server crashes. 
	 * It's not intended to use to ignore the crash
	 * @param appname the application name that broadcasts will be reset
	 * @return
	 */

	public Result resetBroadcast(@PathParam("appname") String appname) 
	{
		AntMediaApplicationAdapter appAdaptor = ((IApplicationAdaptorFactory) getApplication().getApplicationContext(appname).getBean(AntMediaApplicationAdapter.BEAN_NAME)).getAppAdaptor();
		return appAdaptor.resetBroadcasts();
	}

	public void setDataStore(IConsoleDataStore dataStore) {
		this.dataStore = dataStore;
	}

	public IConsoleDataStore getDataStore() {
		if (dataStore == null) {
			dataStore = getDataStoreFactory().getDataStore();
		}
		return dataStore;
	}

	private ServerSettings getServerSettingsInternal() {

		if(serverSettings == null) {

			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext); 
			serverSettings = (ServerSettings)ctxt.getBean(ServerSettings.BEAN_NAME);
		}
		return serverSettings;
	}



	public ILicenceService getLicenceServiceInstance () {
		if(licenceService == null) {

			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext); 
			licenceService = (ILicenceService)ctxt.getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString());
		}
		return licenceService;
	}


	public AdminApplication getApplication() {
		WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext); 
		return (AdminApplication)ctxt.getBean("web.handler");
	}

	public ConsoleDataStoreFactory getDataStoreFactory() {
		if(dataStoreFactory == null)
		{
			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext); 
			dataStoreFactory = (ConsoleDataStoreFactory) ctxt.getBean("dataStoreFactory");
		}
		return dataStoreFactory;
	}

	public void setDataStoreFactory(ConsoleDataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}


	public Result isInClusterMode()
	{
		return new Result(isClusterMode(), "");
	}

	public String changeLogSettings(@PathParam("level") String logLevel){

		ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

		PreferenceStore store = new PreferenceStore(RED5_PROPERTIES_PATH);

		if(logLevel.equals(LOG_LEVEL_ALL) || logLevel.equals(LOG_LEVEL_TRACE) 
				|| logLevel.equals(LOG_LEVEL_DEBUG) || logLevel.equals(LOG_LEVEL_INFO) 
				|| logLevel.equals(LOG_LEVEL_WARN)  || logLevel.equals(LOG_LEVEL_ERROR)
				|| logLevel.equals(LOG_LEVEL_OFF)) {

			rootLogger.setLevel(currentLevelDetect(logLevel));

			store.put(LOG_LEVEL, logLevel);

	
		}

		return gson.toJson(new Result(store.save()));
	}

	public Level currentLevelDetect(String logLevel) {

		Level currentLevel;
		if( logLevel.equals(LOG_LEVEL_OFF)) {
			currentLevel = Level.OFF;
			return currentLevel;
		}
		if( logLevel.equals(LOG_LEVEL_ERROR)) {
			currentLevel = Level.ERROR;
			return currentLevel;
		}
		if( logLevel.equals(LOG_LEVEL_WARN)) {
			currentLevel = Level.WARN;
			return currentLevel;
		}
		if( logLevel.equals(LOG_LEVEL_DEBUG)) {
			currentLevel = Level.DEBUG;
			return currentLevel;
		}
		if( logLevel.equals(LOG_LEVEL_TRACE)) {
			currentLevel = Level.ALL;
			return currentLevel;
		}
		if( logLevel.equals(LOG_LEVEL_ALL)) {
			currentLevel = Level.ALL;
			return currentLevel;
		}
		else {
			currentLevel = Level.INFO;
			return currentLevel;
		}

	}


	public String getLogFile(@PathParam("charSize") int charSize, @QueryParam("logType") String logType,
			@PathParam("offsetSize") long offsetSize) throws IOException {

		long skipValue = 0;
		int countKb = 0;
		int maxCount = 500;
		//default log 
		String logLocation = SERVER_LOG_LOCATION;

		if (logType.equals(LOG_TYPE_ERROR)) {
			logLocation = ERROR_LOG_LOCATION;
		} 

		JsonObject jsonObject = new JsonObject();
		String logContent = "";
		File file = new File(logLocation);

		if (!file.isFile()) {
			logContent = FILE_NOT_EXIST;

			jsonObject.addProperty(LOG_CONTENT, logContent);

			return jsonObject.toString();
		}

		// check charSize > 500kb
		if (charSize > MAX_CHAR_SIZE) {
			charSize = MAX_CHAR_SIZE;
		}

		if (offsetSize != -1) { 			
			skipValue = offsetSize;
			maxCount = charSize / 1024;
		} 
		else if (file.length() > charSize) {
			skipValue = file.length() - charSize;
		}

		int contentSize = 0;
		if (file.length() > skipValue) {

			ByteArrayOutputStream ous = null;
			InputStream ios = null;


			try {

				byte[] buffer = new byte[1024];
				ous = new ByteArrayOutputStream();
				ios = new FileInputStream(file);

				ios.skip(skipValue);

				int read = 0;

				while ((read = ios.read(buffer)) != -1) {

					ous.write(buffer, 0, read);
					countKb++;
					contentSize += read;
					if (countKb == maxCount) { // max read 500kb
						break;
					}

				}
			} finally {
				try {
					if (ous != null)
						ous.close();
				} catch (IOException e) { 
					logger.error(e.toString());
				}

				try {
					if (ios != null)
						ios.close();
				} catch (IOException e) {
					logger.error(e.toString());
				}
			}

			logContent = ous.toString("UTF-8");
		}
		jsonObject.addProperty(LOG_CONTENT, logContent);
		jsonObject.addProperty(LOG_CONTENT_SIZE, contentSize);
		jsonObject.addProperty(LOG_FILE_SIZE, file.length());

		return jsonObject.toString();
	}

	public String getMD5Hash(String pass){
		String passResult= "";
		try {
			MessageDigest m=MessageDigest.getInstance("MD5");
			m.reset();
			m.update(pass.getBytes(Charset.forName("UTF8")));
			byte[] digestResult=m.digest();
			passResult= Hex.encodeHexString(digestResult);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return passResult;
	}


	public Result createApplication(@QueryParam("appName") String appName) {
		return new Result(getApplication().createApplication(appName));
	}


	public Result deleteApplication(@PathParam("appName") String appName) {
		logger.info("delete application http request:{}", appName);
		AppSettings appSettings = getSettings(appName);
		appSettings.setToBeDeleted(true);
		changeSettings(appName, appSettings);
		boolean result = true;
		if (!isClusterMode()) {
			//if it's not in cluster mode, delete application
			//In cluster mode, it's deleted by synchronization
			result = getApplication().deleteApplication(appName);
		}
		return new Result(result);
	}

	public boolean isClusterMode() {
		WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		return ctxt.containsBean(IClusterNotifier.BEAN_NAME);
	}

}
