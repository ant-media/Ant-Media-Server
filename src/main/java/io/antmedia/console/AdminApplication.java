package io.antmedia.console;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.tomcat.WarDeployer;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.SystemUtils;
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

	@Override
	public boolean appStart(IScope app) {
		isCluster = app.getContext().hasBean(IClusterNotifier.BEAN_NAME);

		vertx = (Vertx) scope.getContext().getBean("vertxCore");
		warDeployer = (WarDeployer) app.getContext().getBean("warDeployer");
		
		if(isCluster) {
			IClusterNotifier clusterNotifier = (IClusterNotifier) app.getContext().getBean(IClusterNotifier.BEAN_NAME);
			clusterNotifier.registerCreateAppListener(appName -> {
				log.info("Creating application with name {}", appName);
				return createApplication(appName);
			});
			clusterNotifier.registerDeleteAppListener(appName -> {
				log.info("Deleting application with name {}", appName);
				return deleteApplication(appName);
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

	private long getStorage(String name) {
		File appFolder = new File("webapps/"+name);
		return FileUtils.sizeOfDirectory(appFolder);
	}

	private int getVoDCount(IScope appScope) {
		int size = 0;
		if (appScope != null ){
			Object adapter = ((IApplicationAdaptorFactory) appScope.getContext().getApplicationContext().getBean(AntMediaApplicationAdapter.BEAN_NAME)).getAppAdaptor();
			if (adapter instanceof AntMediaApplicationAdapter)
			{
				DataStore dataStore = ((AntMediaApplicationAdapter)adapter).getDataStore();
				if (dataStore != null) {
					size =  (int) dataStore.getTotalVodNumber();
				}
			}
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
		List<String> apps = new ArrayList<String>();
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

	public HashMap<Integer, String> getConnections(String scopeName) {
		HashMap<Integer, String> connections = new HashMap<Integer, String>();
		IScope root = getScope(scopeName);
		if (root != null) {
			Set<IClient> clients = root.getClients();
			Iterator<IClient> client = clients.iterator();
			int id = 0;
			while (client.hasNext()) {
				IClient c = client.next();
				String user = c.getId();
				connections.put(id, user);
				id++;
			}
		}
		return connections;
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
		IScope root = ScopeUtils.findRoot(scope);
		return getScopes(root, scopeName);
	}

	/**
	 * Search through all the scopes in the given scope to a scope with the
	 * given name
	 * 
	 * @param root
	 * @param scopeName
	 * @return IScope the requested scope
	 */
	private IScope getScopes(IScope root, String scopeName) {
		if (root.getName().equals(scopeName)) {
			return root;
		} else {
			if (root instanceof IScope) {
				Set<String> names = root.getScopeNames();
				for (String name : names) {
					try {
						IScope parent = root.getScope(name);
						IScope scope = getScopes(parent, scopeName);
						if (scope != null) {
							return scope;
						}
					} catch (NullPointerException npe) {
						log.debug(npe.toString());
					}
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
			Object adapter = ((IApplicationAdaptorFactory) appScope.getContext().getApplicationContext().getBean(AntMediaApplicationAdapter.BEAN_NAME)).getAppAdaptor();
			if (adapter instanceof AntMediaApplicationAdapter) 
			{
				DataStore dataStore = ((AntMediaApplicationAdapter)adapter).getDataStore();
				if (dataStore != null) {
					size =  (int) dataStore.getActiveBroadcastCount();
				}
			}
		}
		return size;
	}

	public boolean createApplication(String appName) {
		boolean success = false;
		
		if(isCluster) {
			String mongoHost = getDataStoreFactory().getDbHost();
			String mongoUser = getDataStoreFactory().getDbUser();
			String mongoPass = getDataStoreFactory().getDbPassword();

			boolean result = runCreateAppScript(appName, true, mongoHost, mongoUser, mongoPass);
			success = result;
		}
		else {
			boolean result = runCreateAppScript(appName);
			success = result;
		}
		
		vertx.setTimer(3000, i -> warDeployer.deploy(true));

		return success;
		
	}

	public boolean deleteApplication(String appName) {
		boolean success = runDeleteAppScript(appName);
		warDeployer.undeploy(appName);
		
		IScope appScope = getRootScope().getScope(appName);	
		getRootScope().removeChildScope(appScope);
		
		return success;
	}
	
	
	public boolean runCreateAppScript(String appName) {
		return runCreateAppScript(appName, false, null, null, null);
	}
	
	public boolean runCreateAppScript(String appName, boolean isCluster, 
			String mongoHost, String mongoUser, String mongoPass) {
		Path currentRelativePath = Paths.get("");
		String webappsPath = currentRelativePath.toAbsolutePath().toString();
		
		String command = "/bin/bash create_app.sh"
				+ " -n "+appName
				+ " -w true"
				+ " -p "+webappsPath
				+ " -c "+isCluster;
		
		if(isCluster) {
			command += " -m "+mongoHost
					+ " -u "+mongoUser
					+ " -s "+mongoPass;
		}
		
		log.info("Creating application with command: {}", command);
		ProcessBuilder pb = new ProcessBuilder(command.split(" "));
		pb.inheritIO().redirectOutput(ProcessBuilder.Redirect.INHERIT);
		pb.inheritIO().redirectError(ProcessBuilder.Redirect.INHERIT);
		
		try {
			pb.start();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean runDeleteAppScript(String appName) {
		Path currentRelativePath = Paths.get("");
		String webappsPath = currentRelativePath.toAbsolutePath().toString();
		
		String command = "/bin/bash delete_app.sh -n "+appName+" -p "+webappsPath;

		ProcessBuilder pb = new ProcessBuilder(command.split(" "));
		pb.inheritIO().redirectOutput(ProcessBuilder.Redirect.INHERIT);
		pb.inheritIO().redirectError(ProcessBuilder.Redirect.INHERIT);
		
		try {
			pb.start();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
