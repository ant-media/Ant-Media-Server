package io.antmedia.servlet;

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
	public byte[] getCache(String key, int index) {
		return null;
	}

	@Override
	public int size(String key) {
		return 0;
	}

}
