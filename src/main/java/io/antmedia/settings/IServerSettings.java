package io.antmedia.settings;

public interface IServerSettings {
	
	public static final String BEAN_NAME = "ant.media.server.settings";

	/**
	 * Server default HTTP port
	 * It's 5080 by default
	 * @return default HTTP port
	 */
	public int getDefaultHttpPort();

	/**
	 * The node group name that this node is in. 
	 * It's used in multi-level cluster
	 * 
	 * @return nodegroup name
	 */
	public String getNodeGroup();

	/**
	 * The IP address of the host. It returns the local or public IP address according to the
	 * configuration. If 'useGlobalIp' in configuration is true, then it returns publich IP address,
	 * If 'useGlobalIP' is false, it returns local IP address
	 * @return IP address of the host
	 */
	public String getHostAddress();
	
	
	
}
