package io.antmedia.test;


import org.junit.Test;
import static org.mockito.Mockito.*;

import io.antmedia.shutdown.AMSShutdownManager;
import io.antmedia.shutdown.IShutdownListener;


public class AMSShutdownManagerUnitTest {
	
	@Test
	public void testShutdown() {
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
	}

}
