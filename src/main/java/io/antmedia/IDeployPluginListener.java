package io.antmedia;

public interface IDeployPluginListener {
    boolean deployPlugin(String pluginName, String jarFileURI, String secretKey);
}
