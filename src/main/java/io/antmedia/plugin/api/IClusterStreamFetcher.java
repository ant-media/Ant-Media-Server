package io.antmedia.plugin.api;

public interface IClusterStreamFetcher {
	public void register(String streamId, IPacketListener listener);
}
