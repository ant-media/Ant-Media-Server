package io.antmedia.plugin.api;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.red5.server.api.plugin.IRed5Plugin;

/**
 * Stores metadata and runtime state for a loaded V2 plugin.
 */
public class PluginRecord {

	private String name;
	private String version;
	private String author;
	private String description;
	private String requiresVersion;
	private String loadingMode;  // HOTLOAD or WEBAPP_LIB
	private boolean requiresRestart;
	private PluginState state;
	private String lastError;
	private String pluginId;
	private IRed5Plugin v1Instance;  // null for V2 plugins
	private URLClassLoader classLoader;
	private List<String> listenerClasses = new ArrayList<>();
	private List<String> restClasses = new ArrayList<>();
	private String configClass;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRequiresVersion() {
		return requiresVersion;
	}

	public void setRequiresVersion(String requiresVersion) {
		this.requiresVersion = requiresVersion;
	}

	public String getLoadingMode() {
		return loadingMode;
	}

	public void setLoadingMode(String loadingMode) {
		this.loadingMode = loadingMode;
	}

	public boolean isRequiresRestart() {
		return requiresRestart;
	}

	public void setRequiresRestart(boolean requiresRestart) {
		this.requiresRestart = requiresRestart;
	}

	public PluginState getState() {
		return state;
	}

	public void setState(PluginState state) {
		this.state = state;
	}

	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
	}

	public String getPluginId() {
		return pluginId;
	}

	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}

	public IRed5Plugin getV1Instance() {
		return v1Instance;
	}

	public void setV1Instance(IRed5Plugin v1Instance) {
		this.v1Instance = v1Instance;
	}

	public URLClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(URLClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public List<String> getListenerClasses() {
		return listenerClasses;
	}

	public void setListenerClasses(List<String> listenerClasses) {
		this.listenerClasses = listenerClasses;
	}

	public List<String> getRestClasses() {
		return restClasses;
	}

	public void setRestClasses(List<String> restClasses) {
		this.restClasses = restClasses;
	}

	public String getConfigClass() {
		return configClass;
	}

	public void setConfigClass(String configClass) {
		this.configClass = configClass;
	}
}
