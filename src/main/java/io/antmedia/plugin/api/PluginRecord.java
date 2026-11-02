package io.antmedia.plugin.api;

public class PluginRecord {

    private String name;
    private String version;
    private String author;
    private String description;
    private String requiresVersion;
    private boolean requiresRestart;
    private PluginState state;
    private String lastError;
    private String pluginId;
    private String jarPath;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRequiresVersion() { return requiresVersion; }
    public void setRequiresVersion(String requiresVersion) { this.requiresVersion = requiresVersion; }

    public boolean isRequiresRestart() { return requiresRestart; }
    public void setRequiresRestart(boolean requiresRestart) { this.requiresRestart = requiresRestart; }

    public PluginState getState() { return state; }
    public void setState(PluginState state) { this.state = state; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public String getPluginId() { return pluginId; }
    public void setPluginId(String pluginId) { this.pluginId = pluginId; }

    public String getJarPath() { return jarPath; }
    public void setJarPath(String jarPath) { this.jarPath = jarPath; }
}
