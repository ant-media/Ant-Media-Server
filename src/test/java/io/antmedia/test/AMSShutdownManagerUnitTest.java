package io.antmedia.test;


import org.junit.Test;
import org.red5.server.service.ShutdownServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import io.antmedia.shutdown.AMSShutdownManager;
import io.antmedia.shutdown.IShutdownListener;


public class AMSShutdownManagerUnitTest {
	
	@Test
	public void testShutdown() {
		AMSShutdownManager.getInstance().getListeners().clear();
		IShutdownListener listener1 = mock(IShutdownListener.class);
		IShutdownListener listener2 = mock(IShutdownListener.class);
		IShutdownListener shutdownServer = mock(IShutdownListener.class);

		
		AMSShutdownManager.getInstance().subscribe(listener1);
		AMSShutdownManager.getInstance().subscribe(listener2);
		
		AMSShutdownManager.getInstance().setShutdownServer(shutdownServer);

		
		
		AMSShutdownManager.getInstance().notifyShutdown();
		verify(listener1, times(1)).serverShuttingdown();
		verify(listener2, times(1)).serverShuttingdown();
		verify(shutdownServer, times(1)).serverShuttingdown();

		
		//notification can be made only one times 
		
		AMSShutdownManager.getInstance().notifyShutdown();
		verify(listener1, times(1)).serverShuttingdown();
		verify(listener2, times(1)).serverShuttingdown();
		verify(shutdownServer, times(1)).serverShuttingdown();
		
		AMSShutdownManager.getInstance().getListeners().clear();
		
		AMSShutdownManager.getInstance().setShutdownServer(null);
		
	}

	@Test
	public void testShutdownServerRegister() {
		AMSShutdownManager.getInstance().setShutdownServer(null);
		assertNull(AMSShutdownManager.getInstance().getShutdownServer());
		ShutdownServer ss = new ShutdownServer();
		ss.start();
		assertNotNull(AMSShutdownManager.getInstance().getShutdownServer());
		
		AMSShutdownManager.getInstance().setShutdownServer(null);

	}
}
