package io.antmedia.test.checkserver;

import java.time.LocalDate;

import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.antmedia.checkserver.DiskSizeControl;

import io.vertx.core.Vertx;


public class DiskSizeControlTest {
	
	@Autowired
	private Vertx vertx;
	
	private Integer diskSizeover95 = 95;
	
	private Integer diskSizeover85 = 85;
	
	private Integer diskSizeover75 = 75;
	
	private Integer diskSizeover65 = 65;
	
	private String today = LocalDate.now().toString();
	 
	private String tomorrow = LocalDate.now().minusDays(1).toString();
	
	protected static Logger logger = LoggerFactory.getLogger(DiskSizeControlTest.class);
	
	 @Test
	    public void testDiskSizeCheck()  {
		 
		 DiskSizeControl diskSizeControl = Mockito.spy(new DiskSizeControl());		 
		 
		 when(diskSizeControl.getTodayStringDb()).thenReturn(today);
		 
		 when(diskSizeControl.getDiskSize()).thenReturn(diskSizeover95);
		 
		 diskSizeControl.startService();
		 
		 //verify send function 
		 verify(diskSizeControl,times(1)).startService();
		 
		 when(diskSizeControl.getTodayString()).thenReturn(tomorrow);
		 
		 when(diskSizeControl.getDiskSize()).thenReturn(diskSizeover95);
		 
		 //verify send function 
		 verify(diskSizeControl,times(1)).startService();
		 
		// doNothing().when(diskSizeControl).sendEmail.callSendEmail("Disk Usage Over %90 - Ant Media Server", "Disk Usage Over %90 - Ant Media Server \n\n The system will disable enablemMp4 Recording Setting in Applications.");
		 
		 when(diskSizeControl.getTodayString()).thenReturn(tomorrow);
		 
		 when(diskSizeControl.getDiskSize()).thenReturn(diskSizeover75);
		 
	//	 diskSizeControl.startService();
		 
	//	 doNothing().when(diskSizeControl).sendEmail.callSendEmail("Disk Usage Over %70 - Ant Media Server", "%70 above your Disk Size");
		 
		 //verify send function 
		 verify(diskSizeControl,times(1)).startService();
		 
		 when(diskSizeControl.getTodayString()).thenReturn(tomorrow);
		 
		 when(diskSizeControl.getDiskSize()).thenReturn(diskSizeover85);
		 
		// diskSizeControl.startService();
		 
	// doNothing().when(diskSizeControl).sendEmail.callSendEmail("Disk Usage Over %80 - Ant Media Server", "%80 above your Disk Size");
		 
		 //verify send function 
		 verify(diskSizeControl,times(1)).startService();
		 
		 when(diskSizeControl.getTodayString()).thenReturn(tomorrow);
		 
		 when(diskSizeControl.getDiskSize()).thenReturn(diskSizeover65);
		 
		// diskSizeControl.startService();
		 
		// doNothing().when(diskSizeControl).sendEmail.callSendEmail("test subject", "test context");
		 
	 }
	 
		public Vertx getVertx() {
			return vertx;
		}

}
