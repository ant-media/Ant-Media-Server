package io.antmedia.servlet.cmafutils;

public interface ICMAFChunkListener {

	public void chunkCompleted(byte[] completeChunk);

}
