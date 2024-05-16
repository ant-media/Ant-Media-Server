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
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpResponse;
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
import org.red5.server.scope.WebScope;
import org.red5.server.tomcat.WarDeployer;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.datastore.db.DataStoreFactory;
import io.vertx.core.Vertx;
import jakarta.annotation.Nullable;


/**
 * Sample application that uses the client manager.
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public class AdminApplication extends MultiThreadedApplicationAdapter {
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


	private Queue<String> currentApplicationCreationProcesses = new ConcurrentLinkedQueue<>();

	@Override
	public boolean appStart(IScope app) {
		isCluster = app.getContext().hasBean(IClusterNotifier.BEAN_NAME);

		vertx = (Vertx) scope.getContext().getBean("vertxCore");
		warDeployer = (WarDeployer) app.getContext().getBean("warDeployer");

		if(isCluster) {
			clusterNotifier = (IClusterNotifier) app.getContext().getBean(IClusterNotifier.BEAN_NAME);
			clusterNotifier.registerCreateAppListener( (appName, warFileURI) -> 
			createApplicationWithURL(appName, warFileURI)
					);
			clusterNotifier.registerDeleteAppListener(appName -> {
				log.info("Deleting application with name {}", appName);
				return deleteApplication(appName, false);
			});

		}

		return super.appStart(app);
	}

	public boolean createApplicationWithURL(String appName, String warFileURI) 
	{
		//If installation takes long, prevent redownloading war and starting installation again
		if(currentApplicationCreationProcesses.contains(appName)) {
			log.warn("{} application has already been installing", appName);
			return false;
		}

		log.info("Creating application with name {}", appName);
		boolean result = false;
		try {
			String warFileFullPath = null;
			if (warFileURI != null && !warFileURI.isEmpty()) 
			{
				warFileFullPath = downloadWarFile(appName, warFileURI).getAbsolutePath();
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

			info.storage = getStorage(name);
			appsInfo.add(info);
		}

		return appsInfo;
	}

	public AntMediaApplicationAdapter getApplicationAdaptor(IScope appScope) 
	{
		return (AntMediaApplicationAdapter) appScope.getContext().getApplicationContext().getBean(AntMediaApplicationAdapter.BEAN_NAME);
	}

	private long getStorage(String name) {
		File appFolder = new File("webapps/"+name);
		return FileUtils.sizeOfDirectory(appFolder);
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
			if(!name.equals("root")) {
				apps.add(name);
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
		String mongoUser = getDataStoreFactory().getDbUser();
		String mongoPass = getDataStoreFactory().getDbPassword();
		success = runCreateAppScript(appName, isCluster, dbConnectionURL, mongoUser, mongoPass, warFileFullPath);
		

		vertx.executeBlocking(() -> {
			try {
				warDeployer.deploy(true);
			}
			finally {
				currentApplicationCreationProcesses.remove(appName);
			}
			return null;
		});

		return success;

	}
	
	public Queue<String> getCurrentApplicationCreationProcesses() {
		return currentApplicationCreationProcesses;
	}

	@Nullable
	public static File saveWARFile(String appName, InputStream inputStream) 
	{
		File file = null;
		String fileExtension = "war";

		try {

			String tmpsDirectory = System.getProperty("java.io.tmpdir");

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

	public File downloadWarFile(String appName, String warFileUrl) throws IOException
	{

		try (CloseableHttpClient client = getHttpClient()) 
		{
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(2 * 1000).setSocketTimeout(5*1000).build();

			HttpRequestBase get = (HttpRequestBase) RequestBuilder.get().setUri(warFileUrl).build();
			get.setConfig(requestConfig);

			HttpResponse response = client.execute(get);

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
		return runCreateAppScript(appName, false, null, null, null, null);
	}

	public boolean runCreateAppScript(String appName, boolean isCluster, 
			String dbConnectionUrl, String dbUser, String dbPass, String warFileName) {
		Path currentRelativePath = Paths.get("");
		String webappsPath = currentRelativePath.toAbsolutePath().toString();


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
			if (StringUtils.isNotBlank(dbUser)) {
				command += " -u " + dbUser;
			}
			if (StringUtils.isNotBlank(dbPass)) {
				command += " -s " + dbPass;
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
}
