package io.antmedia;

public interface IDeployPluginListener {
    /**
     * Called on non-origin cluster nodes to download and deploy a plugin.
     *
     * @param pluginName plugin name (used as filename: {pluginName}.jar)
     * @param jarFileURI HTTP URL to download the JAR from the origin node
     * @param secretKey  JWT secret for authenticated download
     */
    boolean deployPlugin(String pluginName, String jarFileURI, String secretKey);
}
