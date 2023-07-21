package io.antmedia.plugin.api;

public interface IClusterStreamFetcher {
	public boolean register(String streamId, IPacketListener listener);
	
	public boolean remove(String streamId, IPacketListener listener);
}
