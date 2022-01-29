package io.antmedia.console;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import io.antmedia.datastore.db.DataStore;
import io.antmedia.settings.ServerSettings;
import io.vertx.core.Vertx;


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
	private ServerSettings serverSettings;
	private Vertx vertx;
	private WarDeployer warDeployer;
	private boolean isCluster = false;


	private IClusterNotifier clusterNotifier;

	@Override
	public boolean appStart(IScope app) {
		isCluster = app.getContext().hasBean(IClusterNotifier.BEAN_NAME);

		vertx = (Vertx) scope.getContext().getBean("vertxCore");
		warDeployer = (WarDeployer) app.getContext().getBean("warDeployer");

		if(isCluster) {
			clusterNotifier = (IClusterNotifier) app.getContext().getBean(IClusterNotifier.BEAN_NAME);
			clusterNotifier.registerCreateAppListener( (appName, warFileName) -> {
				log.info("Creating application with name {}", appName);
				return createApplication(appName, warFileName);
			});
			clusterNotifier.registerDeleteAppListener(appName -> {
				log.info("Deleting application with name {}", appName);
				return deleteApplication(appName, false);
			});
			clusterNotifier.registerPullWarFileListener( (appName, warFileUrl) -> {
				log.info("Pulling war file for creating {} from URL: {}", appName, warFileUrl);
				return pullWarFile(appName, warFileUrl);
			});
		}

		return super.appStart(app);
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

	public boolean createApplication(String appName, String warFileName) {
		boolean success = false;
		logger.info("Running create app script, war file name (null if default): {}, app name: {} ", warFileName, appName);

		if(isCluster) {
			String mongoHost = getDataStoreFactory().getDbHost();
			String mongoUser = getDataStoreFactory().getDbUser();
			String mongoPass = getDataStoreFactory().getDbPassword();

			boolean result = runCreateAppScript(appName, true, mongoHost, mongoUser, mongoPass, warFileName);
			success = result;
		}
		else {
			boolean result = runCreateAppScript(appName, warFileName);
			success = result;
		}

		vertx.setTimer(3000, i -> warDeployer.deploy(true));

		return success;

	}

	public boolean pullWarFile(String appName, String warFileUrl) throws IOException{
		FileOutputStream fileOutputStream = null;
		try (BufferedInputStream in = new BufferedInputStream(new URL(warFileUrl).openStream())) {
			String fileExtension = "war";
			File savedFile = new File(String.format("%s/%s", System.getProperty("red5.root"), appName + "." + fileExtension));
			fileOutputStream = new FileOutputStream(savedFile);

			byte dataBuffer[] = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
				fileOutputStream.write(dataBuffer, 0, bytesRead);
			}
			fileOutputStream.flush();
			long fileSize = savedFile.length();

			String path = savedFile.getPath();

			logger.info("War file pulled from {} for creating application, filesize = {} path = {}", warFileUrl, fileSize, path);
			return true;
		}
		finally{
			fileOutputStream.close();
		}
	}

	public boolean deleteApplication(String appName, boolean deleteDB) {

		boolean success = false;
		WebScope appScope = (WebScope)getRootScope().getScope(appName);	
		
		if (appScope != null) 
		{
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
		}

		return success;
	}


	public boolean runCreateAppScript(String appName, String warFilePath) {
		return runCreateAppScript(appName, false, null, null, null, warFilePath);
	}

	public boolean runCreateAppScript(String appName) {
		return runCreateAppScript(appName, false, null, null, null, null);
	}

	public boolean runCreateAppScript(String appName, boolean isCluster, 
			String mongoHost, String mongoUser, String mongoPass, String warFileName) {
		Path currentRelativePath = Paths.get("");
		String webappsPath = currentRelativePath.toAbsolutePath().toString();

		String command;

		if(warFileName != null && !warFileName.isEmpty()){
			command = "/bin/bash create_app.sh"
					+ " -n "+appName
					+ " -w true"
					+ " -p "+webappsPath
					+ " -c "+isCluster
					+ " -f " +warFileName;

		}else{
			command = "/bin/bash create_app.sh"
					+ " -n "+appName
					+ " -w true"
					+ " -p "+webappsPath
					+ " -c "+isCluster;
		}
		if(isCluster) {
			command += " -m "+mongoHost
					+ " -u "+mongoUser
					+ " -s "+mongoPass;
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
			result = process.waitFor() == 0;
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
		ProcessBuilder pb = new ProcessBuilder(command.split(" "));
		pb.inheritIO().redirectOutput(ProcessBuilder.Redirect.INHERIT);
		pb.inheritIO().redirectError(ProcessBuilder.Redirect.INHERIT);
		return pb.start();

	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	public void setWarDeployer(WarDeployer warDeployer) {
		this.warDeployer = warDeployer;
	}
}
