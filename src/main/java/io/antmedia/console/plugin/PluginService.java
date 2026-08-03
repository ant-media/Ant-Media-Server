package io.antmedia.console.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.red5.server.plugin.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.antmedia.filter.JWTFilter;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.plugin.PluginDeployer;
import io.antmedia.plugin.api.PluginId;
import io.antmedia.plugin.api.PluginRecord;
import io.antmedia.plugin.api.PluginState;
import io.antmedia.rest.model.Result;

/**
 * Owns plugin install, upgrade and removal for the console. The REST layer only maps to HTTP
 * and {@link io.antmedia.console.AdminApplication} only forwards cluster callbacks, so plugin
 * ids are validated and plugin files are located here and nowhere else.
 */
@Component
public class PluginService {

	private static final Logger logger = LoggerFactory.getLogger(PluginService.class);

	private static final int JWT_TOKEN_TIMEOUT_MS = 60000;

	private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
			.setConnectTimeout(5000)
			.setSocketTimeout(30000)
			.build();

	private PluginDeployer pluginDeployer;

	private CloseableHttpClient httpClient = HttpClients.createDefault();

	@Autowired
	public void setPluginDeployer(PluginDeployer pluginDeployer) {
		this.pluginDeployer = pluginDeployer;
	}

	/** Rebuilds the in-memory records from the jars on disk. Called once at startup. */
	public void scanInstalledPlugins() {
		pluginDeployer.scanInstalledPlugins();
	}

	/**
	 * Installs a plugin from an uploaded ZIP. The id must match the {@code AMS-Plugin-Id} the jar
	 * declares, which is what stops one plugin being installed twice under two different ids.
	 */
	public synchronized Result install(String pluginId, InputStream zipStream) {
		Result idCheck = validateId(pluginId);
		if (!idCheck.isSuccess()) {
			return idCheck;
		}
		if (zipStream == null) {
			return new Result(false, "No plugin ZIP uploaded");
		}
		if (isBundled(pluginId)) {
			return new Result(false, "A plugin with id '" + pluginId + "' is already bundled with the server");
		}

		File zipFile = savePluginZip(pluginId, zipStream);
		if (zipFile == null) {
			return new Result(false, "Could not save the uploaded ZIP for " + pluginId);
		}
		return deploy(pluginId, zipFile);
	}

	/** Installs a plugin by downloading its ZIP from the registry. */
	public synchronized Result installFromUrl(String pluginId, String downloadUrl) {
		Result idCheck = validateId(pluginId);
		if (!idCheck.isSuccess()) {
			return idCheck;
		}
		if (StringUtils.isBlank(downloadUrl)) {
			return new Result(false, "Download URL is required");
		}
		if (isBundled(pluginId)) {
			return new Result(false, "A plugin with id '" + pluginId + "' is already bundled with the server");
		}
		return downloadAndDeploy(pluginId, downloadUrl, null);
	}

	/** Installs a plugin whose ZIP lives on another cluster node. */
	public synchronized Result installFromClusterPeer(String pluginId, String zipUri, String clusterSecret) {
		Result idCheck = validateId(pluginId);
		if (!idCheck.isSuccess()) {
			return idCheck;
		}
		String jwtToken = JWTFilter.generateJwtToken(clusterSecret,
				System.currentTimeMillis() + JWT_TOKEN_TIMEOUT_MS, "pluginname", pluginId);
		return downloadAndDeploy(pluginId, zipUri, jwtToken);
	}

	/** Removes a plugin: unloads it, runs its uninstall script and deletes its files. */
	public synchronized Result uninstall(String pluginId) {
		Result idCheck = validateId(pluginId);
		if (!idCheck.isSuccess()) {
			return idCheck;
		}

		Result result = pluginDeployer.unloadPluginFromZip(pluginId, getPluginsDir());
		if (!result.isSuccess()) {
			logger.warn("Failed to uninstall plugin {}: {}", pluginId, result.getMessage());
			return result;
		}

		File zipFile = new File(getPluginsDir(), pluginId + ".zip");
		if (zipFile.exists() && !zipFile.delete()) {
			logger.warn("Failed to delete plugin ZIP: {}", zipFile.getAbsolutePath());
		}

		logger.info("Plugin {} uninstalled", pluginId);
		return result;
	}

	/** Every known plugin: the ones bundled with the server plus the ones installed from a ZIP. */
	public List<PluginRecord> list() {
		List<PluginRecord> records = new ArrayList<>();

		// Bundled plugins carry no manifest here, so name and state are all we know about them.
		for (String name : PluginRegistry.getPluginNames()) {
			PluginRecord record = new PluginRecord();
			record.setName(name);
			record.setPluginId(PluginId.fromName(name));
			record.setState(PluginState.ACTIVE);
			records.add(record);
		}

		records.addAll(pluginDeployer.getAllPluginRecords());
		return records;
	}

	/** The ZIP a cluster peer should download, or {@code null} when there isn't one. */
	public File resolveZipForDownload(String pluginId) {
		if (!PluginId.isValid(pluginId)) {
			return null;
		}
		File zipFile = new File(getPluginsDir(), pluginId + ".zip");
		return zipFile.exists() ? zipFile : null;
	}

	public File getPluginsDir() {
		String amsHome = System.getProperty("red5.root", "/usr/local/antmedia");
		File dir = new File(amsHome, "plugins");
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IllegalStateException("Could not create the plugins directory: " + dir.getAbsolutePath());
		}
		return dir;
	}

	private Result downloadAndDeploy(String pluginId, String zipUri, String jwtToken) {
		File zipFile;
		try {
			zipFile = downloadPluginZip(pluginId, zipUri, jwtToken);
		} catch (IOException e) {
			logger.error("Error downloading plugin {} from {}", pluginId, zipUri, e);
			return new Result(false, "Could not download plugin from " + zipUri + ": " + e.getMessage());
		}
		if (zipFile == null) {
			return new Result(false, "Could not save the plugin ZIP downloaded from " + zipUri);
		}
		return deploy(pluginId, zipFile);
	}

	private Result deploy(String pluginId, File zipFile) {
		Result result = pluginDeployer.loadPluginFromZip(zipFile, getPluginsDir(), pluginId);
		if (!result.isSuccess()) {
			logger.error("Failed to install plugin {}: {}", pluginId, result.getMessage());
			if (zipFile.exists() && !zipFile.delete()) {
				logger.warn("Failed to delete ZIP of failed install: {}", zipFile.getAbsolutePath());
			}
		}
		else {
			logger.info("Plugin {} installed", pluginId);
		}
		return result;
	}

	private static Result validateId(String pluginId) {
		if (!PluginId.isValid(pluginId)) {
			return new Result(false, "Plugin id '" + pluginId + "' is not valid. It must match " + PluginId.RULE);
		}
		return new Result(true);
	}

	/** Bundled plugins register under their display name, so compare on the derived id. */
	private static boolean isBundled(String pluginId) {
		return PluginRegistry.getPluginNames().stream()
				.anyMatch(name -> pluginId.equals(PluginId.fromName(name)));
	}

	File savePluginZip(String pluginId, InputStream inputStream) {
		File saved = new File(getPluginsDir(), pluginId + ".zip");
		try (OutputStream out = new FileOutputStream(saved)) {
			long total = IOUtils.copyLarge(inputStream, out);
			logger.info("Plugin ZIP saved: {} ({} bytes)", saved.getAbsolutePath(), total);
		} catch (IOException e) {
			logger.error("Failed to save plugin ZIP for {}", pluginId, e);
			return null;
		}
		return saved;
	}

	/** {@code jwtToken} is set only for cluster peer downloads, which are JWT-gated. */
	File downloadPluginZip(String pluginId, String zipUri, String jwtToken) throws IOException {
		HttpRequestBase get = (HttpRequestBase) RequestBuilder.get().setUri(zipUri).build();
		get.setConfig(REQUEST_CONFIG);
		if (jwtToken != null) {
			get.addHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION, jwtToken);
		}

		HttpResponse response = httpClient.execute(get);
		int status = response.getStatusLine().getStatusCode();
		Header contentLength = response.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
		if (status != HttpStatus.SC_OK || (contentLength != null && "0".equals(contentLength.getValue()))) {
			throw new IOException("HTTP " + status + " from " + zipUri);
		}

		try (InputStream in = response.getEntity().getContent()) {
			return savePluginZip(pluginId, in);
		}
	}

	void setHttpClient(CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
	}
}
