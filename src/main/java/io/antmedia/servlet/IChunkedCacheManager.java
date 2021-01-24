package io.antmedia.servlet;

import io.antmedia.servlet.cmafutils.ICMAFChunkListener;

public interface IChunkedCacheManager 
{
	
	public static final String BEAN_NAME = "chunked.cache.manager";

	/**
	 * Add a new key to the cache map
	 * @param key
	 */
	public void addCache(String key);

	/**
	 * Remove key from the cache map
	 * @param key
	 */
	public void removeCache(String key);
	
	/**
	 * 
	 * Check that if there is a cache for the specific key
	 * @param key
	 * @return
	 */
	public boolean hasCache(String key);

	
	/**
	 * Add new data to the cache for the specific key
	 * @param key
	 * @param data
	 */
	public void append(String key, byte[] data);
	
	
	/**
	 * 
	 * @param key
	 * @param icmafChunkListener
	 */
	public void registerChunkListener(String key, ICMAFChunkListener icmafChunkListener);

	public void removeChunkListener(String key, ICMAFChunkListener icmafChunkListener);

}
