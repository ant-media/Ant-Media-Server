package io.antmedia.console;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.scope.Scope;
import org.red5.server.scope.WebScope;
import org.red5.server.tomcat.WarDeployer;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.net.HttpHeaders;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.filter.JWTFilter;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.plugin.api.PluginRecord;
import io.antmedia.plugin.api.PluginState;
import io.antmedia.rest.model.Result;
import io.vertx.core.Vertx;
import jakarta.annotation.Nullable;
import org.red5.server.plugin.PluginDeployer;
import org.red5.server.plugin.PluginRegistry;


/**
 * Sample application that uses the client manager.
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public class AdminApplication extends MultiThreadedApplicationAdapter {
	private static final int JWT_TOKEN_TIMEOUT_MS = 60000;


	private static final Logger log = LoggerFactory.getLogger(AdminApplication.class);


	public static final String APP_NAME = "ConsoleApp";
	private ConsoleDataStoreFactory dataStoreFactory;

	public static class ApplicationInfo {
		public String name;
		public int liveStreamCount;
		public int vodCount;
		public long storage;
	}

	public static class BroadcastInfo {
		public String name;
		public int watcherCount;

		public BroadcastInfo(String name, int watcherCount) {
			this.name = name;
			this.watcherCount = watcherCount;
		}
	}
	private IScope rootScope;
	private Vertx vertx;
	private WarDeployer warDeployer;
	private boolean isCluster = false;


	private IClusterNotifier clusterNotifier;

	private PluginDeployer pluginDeployer;

	private Queue<String> currentApplicationCreationProcesses = new ConcurrentLinkedQueue<>();

	/** Allowed characters in a plugin name when used as a path parameter or filename. Blocks
	 *  path traversal (`..`, `/`, `\`) and any non-alphanumeric character that could be
	 *  abused to escape {@code {AMS_HOME}/plugins/}. */
	private static final java.util.regex.Pattern PLUGIN_NAME_PATTERN =
			java.util.regex.Pattern.compile("[a-zA-Z0-9_.\\-]+");

	@Override
	public boolean appStart(IScope app) {
		isCluster = app.getContext().hasBean(IClusterNotifier.BEAN_NAME);

		vertx = (Vertx) scope.getContext().getBean("vertxCore");
		warDeployer = (WarDeployer) app.getContext().getBean("warDeployer");

		try {
			pluginDeployer = (PluginDeployer) app.getContext().getBean("pluginDeployer");
			pluginDeployer.scanInstalledPlugins();
		} catch (Exception e) {
			log.warn("PluginDeployer bean not found: {}", e.getMessage());
		}

		if(isCluster) {
			clusterNotifier = (IClusterNotifier) app.getContext().getBean(IClusterNotifier.BEAN_NAME);
			clusterNotifier.registerCreateAppListener( (appName, warFileURI, secretKey) ->
			createApplicationWithURL(appName, warFileURI, secretKey)
					);
			clusterNotifier.registerDeleteAppListener(appName -> {
				log.info("Deleting application with name {}", appName);
				return deleteApplication(appName, false);
			});

			// Plugin install/uninstall propagation across cluster nodes.
			clusterNotifier.registerDeployPluginListener((pluginName, jarURI, secretKey) ->
					deployPluginWithURL(pluginName, jarURI, secretKey));
			clusterNotifier.registerUndeployPluginListener(pluginName -> {
				log.info("Undeploying plugin with name {}", pluginName);
				return undeployPlugin(pluginName);
			});
		}

		return super.appStart(app);
	}

	public boolean createApplicationWithURL(String appName, String warFileURI, String secretKey) 
	{
		//If installation takes long, prevent redownloading war and starting installation again
		if(currentApplicationCreationProcesses.contains(appName)) {
			log.warn("{} application has already been installing", appName);
			return false;
		}

		log.info("Creating application with name {} and uri:{}", appName, warFileURI);
		boolean result = false;
		try {
			String warFileFullPath = null;
			if (StringUtils.isNotBlank(warFileURI)) 
			{
				if (warFileURI.startsWith("http"))  //covers both http and https
				{
					File file = downloadWarFile(appName, warFileURI, secretKey);
					if (file == null) {
						logger.error("War file cannot be downloaded from {}. App:{} will not be created", warFileURI, appName);
						return false;
					}
					warFileFullPath = file.getAbsolutePath();
				}
				else 
				{
					warFileFullPath = warFileURI;
				}
				logger.info("war full path: {}", warFileFullPath);

			}
			result = createApplication(appName, warFileFullPath);

		} 
		catch (Exception e) 
		{
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public boolean connect(IConnection conn, IScope scope, Object[] params) {
		this.scope = scope;
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect(IConnection conn, IScope scope) {

		super.disconnect(conn, scope);
	}

	public IScope getRootScope() {
		if (rootScope == null) {
			rootScope = ScopeUtils.findRoot(scope);
		}
		return rootScope;
	}

	public int getTotalLiveStreamSize() 
	{
		List<String> appNames = getApplications();
		int size = 0;
		for (String name : appNames) {
			IScope scope = getRootScope().getScope(name);
			size += getAppLiveStreamCount(scope);
		}
		return size;
	}

	public List<ApplicationInfo> getApplicationInfo() {
		List<String> appNames = getApplications();
		List<ApplicationInfo> appsInfo = new ArrayList<>();
		for (String name : appNames) {
			if (name.equals(APP_NAME)) {
				continue;
			}
			ApplicationInfo info = new ApplicationInfo();
			info.name = name;
			info.liveStreamCount = getAppLiveStreamCount(getRootScope().getScope(name));
			info.vodCount = getVoDCount(getRootScope().getScope(name));

			File appFolder = new File("webapps/"+name);
			info.storage = getDirectorySize(appFolder.toPath());
			appsInfo.add(info);
		}

		return appsInfo;
	}

	public AntMediaApplicationAdapter getApplicationAdaptor(IScope appScope) 
	{
		return (AntMediaApplicationAdapter) appScope.getContext().getApplicationContext().getBean(AntMediaApplicationAdapter.BEAN_NAME);
	}
	
	public static long getDirectorySize(Path dir) {
		//Pay Attenton: that we previously uses FileUtils.sizeOfDirectory(appFolder); which throws exception when the directory size is +20GB  and files are deleted
		//then we migrated to use getDirectorySize method which uses stream and parallel processing
		//@mekya
		
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk
                .parallel() // Enable parallel processing
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0; // Ignore files that can't be accessed
                    }
                })
                .sum();
        } catch (IOException e) {
        	logger.error("Error while calculating directory size: {}", ExceptionUtils.getMessage(e));
            return -1; // Handle or log the exception as needed
        }
    }

	public int getVoDCount(IScope appScope) {
		int size = 0;
		if (appScope != null ){
			size = (int)getApplicationAdaptor(appScope).getDataStore().getTotalVodNumber();
		}

		return size;
	}


	public List<BroadcastInfo> getAppLiveStreams(String name) {
		IScope root = getRootScope();
		IScope appScope = root.getScope(name);

		List<BroadcastInfo> broadcastInfoList = new ArrayList<>();
		Set<String> basicScopeNames = appScope.getBasicScopeNames(ScopeType.BROADCAST);
		for (String scopeName : basicScopeNames) {
			IBroadcastScope broadcastScope = appScope.getBroadcastScope(scopeName);
			BroadcastInfo info = new BroadcastInfo(broadcastScope.getName(), broadcastScope.getConsumers().size());
			broadcastInfoList.add(info);
		}
		return broadcastInfoList;
	}


	public boolean deleteVoDStream(String appname, String streamName) {
		File vodStream = new File("webapps/"+appname+"/streams/"+ streamName);
		boolean result = false;
		if (vodStream.exists()) {
			try {
				Files.delete(vodStream.toPath());
				result = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	public List<String> getApplications() {
		IScope root = getRootScope();

		java.util.Set<String> names = root.getScopeNames();
		List<String> apps = new ArrayList<>();
		for (String name : names) {

			IScope scope = root.getScope(name);

			if (scope instanceof Scope) {

				Scope appScope = (Scope) scope;
				if(!name.equals("root") && appScope.isRunning()) {
					apps.add(name);
				}

			}
		}

		/** Sorting applications alphabetically */
		Collections.sort(apps);

		return apps;
	}

	public int getTotalConnectionSize(){
		IScope root = getRootScope();
		return root.getStatistics().getActiveClients();
	}


	public ApplicationContext getApplicationContext(String scopeName) {
		IScope scope = getScope(scopeName);
		if (scope != null) {
			IContext context = scope.getContext();
			if (context != null) {
				return context.getApplicationContext();
			}
		}
		log.warn("Application:{} is not initilized", scopeName);
		return null;
	}


	private IScope getScope(String scopeName) {
		IGlobalScope root = (IGlobalScope) ScopeUtils.findRoot(scope);
		return getScopes(root, scopeName);
	}

	/**
	 * Gt only application scope
	 * 
	 * @param root
	 * @param scopeName
	 * @return IScope the requested scope
	 */
	private IScope getScopes(IGlobalScope root, String scopeName) {
		if (root.getName().equals(scopeName)) {
			return root;
		} else {
			if (root instanceof IScope) {
				try {
					IScope scope = root.getScope(scopeName);
					if (scope != null) {
						return scope;
					}
				} catch (NullPointerException npe) {
					log.debug(npe.toString());
				}

			}
		}
		return null;
	}

	public ConsoleDataStoreFactory getDataStoreFactory() {
		return dataStoreFactory;
	}

	public void setDataStoreFactory(ConsoleDataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}

	public int getAppLiveStreamCount(IScope appScope) {
		int size = 0;
		if (appScope != null) {
			size = (int)getApplicationAdaptor(appScope).getDataStore().getActiveBroadcastCount();
		}
		return size;
	}

	public boolean createApplication(String appName, String warFileFullPath) {
		if(currentApplicationCreationProcesses.contains(appName)) {
			log.warn("{} application has already been installing", appName);
			return false;
		}
		currentApplicationCreationProcesses.add(appName);
		boolean success = false;
		logger.info("Running create app script, war file name (null if default): {}, app name: {} ", warFileFullPath, appName);

		//check if there is a non-completed deployment 

		WebScope appScope = (WebScope)getRootScope().getScope(appName);	
		if (appScope != null && appScope.isRunning()) {
			logger.info("{} already exists and running", appName);
			currentApplicationCreationProcesses.remove(appName);
			return false;
		}

		String dbConnectionURL = getDataStoreFactory().getDbHost();
		success = runCreateAppScript(appName, isCluster, dbConnectionURL, warFileFullPath);


		vertx.executeBlocking(() -> {
			try {
				warDeployer.deploy(true);
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			finally {
				currentApplicationCreationProcesses.remove(appName);
			}
			return null;
		}, false);

		return success;

	}

	public Queue<String> getCurrentApplicationCreationProcesses() {
		return currentApplicationCreationProcesses;
	}

	public static String getJavaTmpDirectory() {
		return System.getProperty("java.io.tmpdir");
	}

	public static File getWarFileInTmpDirectory(String warFileName) 
	{
		String tmpsDirectory = getJavaTmpDirectory();
		File file = new File(tmpsDirectory + File.separator + warFileName);
		if (file.exists()) {
			return file;
		}
		return null;

	}

	public static String getWarName(String appName) {
		return appName + ".war";
	}

	@Nullable
	public static File saveWARFile(String appName, InputStream inputStream) 
	{
		File file = null;
		String fileExtension = "war";

		try {

			String tmpsDirectory =  getJavaTmpDirectory();

			File savedFile = new File(tmpsDirectory + File.separator + appName + "." + fileExtension);

			int read = 0;
			byte[] bytes = new byte[2048];
			try (OutputStream outpuStream = new FileOutputStream(savedFile))
			{

				while ((read = inputStream.read(bytes)) != -1) 
				{
					outpuStream.write(bytes, 0, read);
				}
				outpuStream.flush();

				logger.info("War file uploaded for application, filesize = {} path = {}", savedFile.length(),  savedFile.getPath());
			}

			file = savedFile;

		}
		catch (Exception iox) {
			logger.error(iox.getMessage());
		}

		return file;
	}

	public CloseableHttpClient getHttpClient() {
		return HttpClients.createDefault();
	}

	public File downloadWarFile(String appName, String warFileUrl, String jwtSecretKey) throws IOException
	{

		try (CloseableHttpClient client = getHttpClient()) 
		{
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(2 * 1000).setSocketTimeout(5*1000).build();

			String jwtToken = JWTFilter.generateJwtToken(jwtSecretKey, System.currentTimeMillis() + JWT_TOKEN_TIMEOUT_MS, "appname", appName);

			HttpRequestBase get = (HttpRequestBase) RequestBuilder.get().setUri(warFileUrl).addHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION, jwtToken).build();
			get.setConfig(requestConfig);

			HttpResponse response = client.execute(get);
			Header contentLengthHeader = response.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK || (contentLengthHeader != null && contentLengthHeader.getValue().equals("0"))) {
				logger.error("Cannot download war file from URL: {} Response code: {} length:{}", warFileUrl,
						response.getStatusLine().getStatusCode(), response.getFirstHeader(HttpHeaders.CONTENT_LENGTH).getValue());
				return null;
			}

			try (BufferedInputStream in = new BufferedInputStream(response.getEntity().getContent())) 
			{
				return saveWARFile(appName, in);
			}
		}
	}

	public synchronized boolean deleteApplication(String appName, boolean deleteDB) {

		boolean success = false;
		WebScope appScope = (WebScope)getRootScope().getScope(appName);	

		//appScope is running after application has started
		if (appScope != null && appScope.isRunning()) 
		{

			logger.info("Deleting app:{} and appscope is running:{}", 
					appName, appScope.isRunning());
			getApplicationAdaptor(appScope).stopApplication(deleteDB);

			success = runDeleteAppScript(appName);
			warDeployer.undeploy(appName);

			try {
				appScope.destroy();
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
				success = false;
			}
		}
		else {
			logger.info("Application scope for app:{} is not available to delete.", appName);
			Path currentPath = Paths.get("");
			File f = new File(currentPath.toAbsolutePath().toString() + "/webapps/" + appName);
			if (f.exists()) {
				logger.error("It detects an non-completed app deployment directory with name {}. It's being deleted.", appName);
				success = runDeleteAppScript(appName);
			}

		}

		return success;
	}

	public boolean runCreateAppScript(String appName) {
		return runCreateAppScript(appName, false, null, null);
	}

	public boolean runCreateAppScript(String appName, boolean isCluster, 
			String dbConnectionUrl, String warFileName) {
		Path currentRelativePath = Paths.get("");
		String webappsPath = currentRelativePath.toAbsolutePath().toString();

		appName = WarDeployer.getApplicationName(appName);
		String command = "/bin/bash create_app.sh"
				+ " -n " + appName
				+ " -w true"
				+ " -p " + webappsPath
				+ " -c " + isCluster;

		if 	(!DataStoreFactory.DB_TYPE_MAPDB.equals(getDataStoreFactory().getDbType())) {
			//add db connection url, user and pass if it's not mapdb
			if (StringUtils.isNotBlank(dbConnectionUrl)) {
				command +=  " -m " + dbConnectionUrl;
			}
		}

		if(StringUtils.isNotBlank(warFileName))
		{
			command += " -f " + warFileName;

		}

		log.info("Creating application with command: {}", command);
		return runCommand(command);
	}

	public boolean runDeleteAppScript(String appName) {
		Path currentRelativePath = Paths.get("");
		String webappsPath = currentRelativePath.toAbsolutePath().toString();

		String command = "/bin/bash delete_app.sh -n "+appName+" -p "+webappsPath;

		return runCommand(command);
	}

	public IClusterNotifier getClusterNotifier() {
		return clusterNotifier;
	}



	public boolean runCommand(String command) {

		boolean result = false;
		try {
			Process process = getProcess(command);
			if (process != null) 
			{
				new Thread() 
				{
					@Override
					public void run() 
					{
						InputStream inputStream = process.getInputStream();
						byte[] data = new byte[1024];
						int length;
						try 
						{
							while ((length = inputStream.read(data,0, data.length)) > 0) 
							{
								log.info(new String(data, 0, length));
							}
						} 
						catch (IOException e) 
						{	
							log.error(ExceptionUtils.getStackTrace(e));
						}
					}
				}.start();

				result = process.waitFor() == 0;
			}
		}
		catch (IOException e) {
			log.error(ExceptionUtils.getStackTrace(e));
		} 
		catch (InterruptedException e) {
			log.error(ExceptionUtils.getStackTrace(e));
			Thread.currentThread().interrupt();
		}
		return result;
	}

	public Process getProcess(String command) throws IOException {
		//This code uses a regular expression to check if the command string contains any special characters 
		// that may cause vulnerabilities, 
		//such as ;, &, |, <, >, (, ), $, , , \r, \n, \t, *, ?, {, }, [, ], \, ", ', or whitespace characters. 
		//If the command string contains any of these characters, it is considered unsafe to execute and the code prints an error message."

		String[] parameters = command.split(" ");
		String[] parametersToRun = new String[parameters.length];
		for (int i = 0; i < parameters.length; i++) 
		{
			String param = parameters[i];
			if (param.matches(".*[;&|<>()$`\\r\\n\\t*?{}\\[\\]\\\\\"'\\s].*")) 
			{
				logger.warn("Command includes special characters. Escaping the special characters. Argument:{} and full command:{}", param, command);
				param = "'" + param + "'";
			}
			parametersToRun[i] = param;	
		}


		ProcessBuilder pb = getProcessBuilder(parametersToRun);

		return pb.start();
	}

	public ProcessBuilder getProcessBuilder(String[] parametersToRun) {
		return new ProcessBuilder(parametersToRun);
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	public void setWarDeployer(WarDeployer warDeployer) {
		this.warDeployer = warDeployer;
	}

	public void setPluginDeployer(PluginDeployer pluginDeployer) {
		this.pluginDeployer = pluginDeployer;
	}

	public PluginDeployer getPluginDeployer() {
		return pluginDeployer;
	}

	public static boolean isValidPluginName(String pluginName) {
		return pluginName != null && PLUGIN_NAME_PATTERN.matcher(pluginName).matches();
	}

	public synchronized boolean deployPlugin(String pluginName, InputStream inputStream) {
		if (pluginDeployer == null) {
			log.error("PluginDeployer not initialized");
			return false;
		}
		if (PluginRegistry.getPluginNames().contains(pluginName)
				|| pluginDeployer.getPluginNames().contains(pluginName)) {
			log.warn("Plugin {} is already loaded", pluginName);
			return false;
		}

		File zipFile = savePluginZip(pluginName, inputStream);
		if (zipFile == null) {
			return false;
		}

		Result loadResult = pluginDeployer.loadPluginFromZip(zipFile, getPluginsDir());
		if (!loadResult.isSuccess()) {
			log.error("Failed to load plugin {}: {}", pluginName, loadResult.getMessage());
			return false;
		}

		log.info("Plugin {} deployed successfully", pluginName);
		// TODO: cluster propagation — implement in TcpCluster (Enterprise) and call here
		return true;
	}

	public boolean deployPluginWithURL(String pluginName, String jarFileURI, String secretKey) {
		log.info("Deploying plugin {} from cluster URI {}", pluginName, jarFileURI);
		try {
			File zipFile = downloadPluginZip(pluginName, jarFileURI, secretKey);
			if (zipFile == null) {
				return false;
			}
			Result loadResult = pluginDeployer.loadPluginFromZip(zipFile, getPluginsDir());
			if (!loadResult.isSuccess()) {
				log.error("Failed to load plugin {} from cluster: {}", pluginName, loadResult.getMessage());
				return false;
			}
			return true;
		} catch (Exception e) {
			log.error("Error deploying plugin {} from cluster: {}", pluginName, e.getMessage(), e);
			return false;
		}
	}

	public synchronized boolean installPluginFromUrl(String pluginId, String downloadUrl) {
		if (pluginDeployer == null) {
			log.error("PluginDeployer not initialized");
			return false;
		}

		try {
			File pluginsDir = getPluginsDir();
			File zipFile = new File(pluginsDir, pluginId + ".zip");

			// Download the ZIP from the registry
			try (CloseableHttpClient client = getHttpClient()) {
				RequestConfig config = RequestConfig.custom()
						.setConnectTimeout(5000).setSocketTimeout(30000).build();
				HttpRequestBase get = (HttpRequestBase) RequestBuilder.get()
						.setUri(downloadUrl).build();
				get.setConfig(config);

				HttpResponse response = client.execute(get);
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					log.error("Failed to download plugin from {}. Status: {}",
							downloadUrl, response.getStatusLine().getStatusCode());
					return false;
				}

				try (InputStream in = response.getEntity().getContent();
					 OutputStream out = new FileOutputStream(zipFile)) {
					byte[] buf = new byte[4096];
					int len;
					while ((len = in.read(buf)) != -1) {
						out.write(buf, 0, len);
					}
				}
			}

			log.info("Downloaded plugin ZIP from {} ({} bytes)", downloadUrl, zipFile.length());

			Result loadResult = pluginDeployer.loadPluginFromZip(zipFile, pluginsDir);
			if (!loadResult.isSuccess()) {
				log.error("Failed to load plugin {}: {}", pluginId, loadResult.getMessage());
				return false;
			}

			log.info("Plugin {} installed from URL successfully", pluginId);
			return true;

		} catch (Exception e) {
			log.error("Error installing plugin {} from {}: {}", pluginId, downloadUrl, e.getMessage(), e);
			return false;
		}
	}

	public synchronized boolean undeployPlugin(String pluginName) {
		if (pluginDeployer == null) {
			log.error("PluginDeployer not initialized");
			return false;
		}

		Result unloadResult = pluginDeployer.unloadPluginFromZip(pluginName, getPluginsDir());
		if (!unloadResult.isSuccess()) {
			log.warn("Failed to unload plugin {}: {}", pluginName, unloadResult.getMessage());
			return false;
		}

		File zipFile = new File(getPluginsDir(), pluginName + ".zip");
		if (zipFile.exists() && !zipFile.delete()) {
			log.warn("Failed to delete plugin ZIP: {}", zipFile.getAbsolutePath());
		}

		log.info("Plugin {} undeployed successfully", pluginName);
		// TODO: cluster propagation — implement in TcpCluster (Enterprise) and call here
		return true;
	}

	public List<String> getAllPluginNames() {
		Set<String> all = new HashSet<>();
		all.addAll(PluginRegistry.getPluginNames());
		if (pluginDeployer != null) {
			all.addAll(pluginDeployer.getPluginNames());
		}
		return Collections.unmodifiableList(new ArrayList<>(all));
	}

	public List<PluginRecord> getAllPluginRecords() {
		List<PluginRecord> records = new ArrayList<>();

		// V1 startup-loaded plugins — minimal records with name and ACTIVE state
		for (String name : PluginRegistry.getPluginNames()) {
			PluginRecord r = new PluginRecord();
			r.setName(name);
			r.setState(PluginState.ACTIVE);
			records.add(r);
		}

		// ZIP-installed plugins — full records from PluginDeployer
		if (pluginDeployer != null) {
			records.addAll(pluginDeployer.getAllPluginRecords());
		}

		return Collections.unmodifiableList(records);
	}

	public File getPluginsDir() {
		String amsHome = System.getProperty("red5.root", "/usr/local/antmedia");
		File dir = new File(amsHome, "plugins");
		if (!dir.exists() && !dir.mkdirs()) {
			log.warn("Could not create plugins directory: {}", dir.getAbsolutePath());
		}
		return dir;
	}

	@Nullable
	public File savePluginZip(String pluginName, InputStream inputStream) {
		if (inputStream == null) {
			log.error("Plugin upload stream is null for {}", pluginName);
			return null;
		}
		File saved = new File(getPluginsDir(), pluginName + ".zip");
		try (OutputStream out = new FileOutputStream(saved)) {
			byte[] buf = new byte[4096];
			int read;
			long total = 0;
			while ((read = inputStream.read(buf)) != -1) {
				out.write(buf, 0, read);
				total += read;
			}
			out.flush();
			log.info("Plugin ZIP saved: {} ({} bytes)", saved.getAbsolutePath(), total);
		} catch (IOException e) {
			log.error("Failed to save plugin ZIP for {}: {}", pluginName, e.getMessage());
			return null;
		}
		return saved;
	}

	@Nullable
	public File downloadPluginZip(String pluginName, String pluginFileUrl, String jwtSecretKey) throws IOException {
		try (CloseableHttpClient client = getHttpClient()) {
			RequestConfig requestConfig = RequestConfig.custom()
					.setConnectTimeout(2000).setSocketTimeout(5000).build();

			String jwtToken = JWTFilter.generateJwtToken(jwtSecretKey,
					System.currentTimeMillis() + JWT_TOKEN_TIMEOUT_MS, "pluginname", pluginName);

			HttpRequestBase get = (HttpRequestBase) RequestBuilder.get()
					.setUri(pluginFileUrl)
					.addHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION, jwtToken)
					.build();
			get.setConfig(requestConfig);

			HttpResponse response = client.execute(get);
			Header contentLengthHeader = response.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK
					|| (contentLengthHeader != null && "0".equals(contentLengthHeader.getValue()))) {
				log.error("Cannot download plugin ZIP from {}. Status: {}",
						pluginFileUrl, response.getStatusLine().getStatusCode());
				return null;
			}

			try (BufferedInputStream in = new BufferedInputStream(response.getEntity().getContent())) {
				return savePluginZip(pluginName, in);
			}
		}
	}

	public String buildPluginDownloadURI(String pluginName) {
		String host = System.getProperty("server.host", "localhost");
		String port = System.getProperty("server.http.port", "5080");
		return "http://" + host + ":" + port + "/rest/v2/plugins/" + pluginName + "/download";
	}

	@Nullable
	public String getClusterCommunicationKey() {
		for (String name : getApplications()) {
			if (APP_NAME.equals(name)) continue;
			IScope appScope = getRootScope().getScope(name);
			if (appScope == null) continue;
			try {
				AntMediaApplicationAdapter adapter = (AntMediaApplicationAdapter) appScope.getContext()
						.getBean(AntMediaApplicationAdapter.BEAN_NAME);
				if (adapter != null && adapter.getAppSettings() != null) {
					String key = adapter.getAppSettings().getClusterCommunicationKey();
					if (key != null && !key.isEmpty()) return key;
				}
			} catch (Exception e) {
				log.debug("Could not read cluster key from app {}: {}", name, e.getMessage());
			}
		}
		return null;
	}

}
