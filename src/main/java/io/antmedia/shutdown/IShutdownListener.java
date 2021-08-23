package io.antmedia.shutdown;

public interface IShutdownListener {

	public void serverShuttingdown(boolean deleteDB);

}
