package io.antmedia.servlet;

public interface IChunkedCacheManager {
	
	public static final String BEAN_NAME = "chunked.cache.manager";

	public void addCache(String key);

	public void removeCache(String key);

	public void append(String key, byte[] data);
	
	public byte[] getCache(String key, int index);
	
	public int size(String key);

}
