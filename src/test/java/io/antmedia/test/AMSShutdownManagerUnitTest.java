package io.antmedia.test;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.red5.server.service.ShutdownServer;

import io.antmedia.shutdown.AMSShutdownManager;
import io.antmedia.shutdown.IShutdownListener;


public class AMSShutdownManagerUnitTest {
	
	@Test
	public void testShutdown() {
		AMSShutdownManager.getInstance().getListeners().clear();
		IShutdownListener listener1 = mock(IShutdownListener.class);
		IShutdownListener listener2 = mock(IShutdownListener.class);

		
		AMSShutdownManager.getInstance().subscribe(listener1);
		AMSShutdownManager.getInstance().subscribe(listener2);
		
		
		
		AMSShutdownManager.getInstance().notifyShutdown();
		verify(listener1, times(1)).serverShuttingdown();
		verify(listener2, times(1)).serverShuttingdown();

		
		//notification can be made only one times 
		
		AMSShutdownManager.getInstance().notifyShutdown();
		verify(listener1, times(1)).serverShuttingdown();
		verify(listener2, times(1)).serverShuttingdown();
		
		AMSShutdownManager.getInstance().getListeners().clear();
				
	}
}
