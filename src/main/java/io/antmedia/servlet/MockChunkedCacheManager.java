package io.antmedia.servlet;

import io.antmedia.servlet.cmafutils.ICMAFChunkListener;

public class MockChunkedCacheManager implements IChunkedCacheManager {

	@Override
	public void addCache(String key) {
		// no need to implement
	}

	@Override
	public void removeCache(String key) {
		// no need to implement
	}

	@Override
	public void append(String key, byte[] data) {
		// no need to implement
	}
	
	@Override
	public boolean hasCache(String key) {
		return false;
	}

	@Override
	public void registerChunkListener(String key, ICMAFChunkListener icmafChunkListener) {
		// no need to implement
	}
	
	@Override
	public void removeChunkListener(String key, ICMAFChunkListener icmafChunkListener) {
		// no need to implement
	}
}
