package io.antmedia.test.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.console.rest.SupportRequest;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.StatsCollector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.antmedia.console.rest.SupportRestService;
import org.red5.server.scope.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

public class SupportRestServiceTest {

	SupportRestService restServiceSpy = null;
	
	@After 
	public void after() {
		File f = new File(SupportRestService.LOG_FILE);
		try { 
			Files.deleteIfExists(f.toPath()); 
		}
		catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
		restServiceSpy = null;
	}

	@Before
	public void before() {
		restServiceSpy = spy(SupportRestService.class);
	}

	@Test
	public void testZipFile() 
	{
		File f = new File(SupportRestService.LOG_FILE);
		try { 
			Files.deleteIfExists(f.toPath()); 
		}
		catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}

		{
			assertFalse(f.exists());
			SupportRestService.zipFile();
			assertTrue(f.exists());

		}		
		
		
		try { 
			Files.deleteIfExists(f.toPath()); 
		}
		catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}

		File f1 = new File("log/ant-media-server.log");
		f1.mkdirs();

		File f2 = new File("log/antmedia-error.log");
		f2.mkdirs();
		try {
			f2.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


		{
			assertFalse(f.exists());
			SupportRestService.zipFile();
			assertTrue(f.exists());
		}	
		
		try { 
			Files.deleteIfExists(f1.toPath()); 
			f1.createNewFile();
			Files.deleteIfExists(f.toPath()); 
		}
		catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		{
			try {
				Files.write(f1.toPath(), "hello world".getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
			assertFalse(f.exists());
			SupportRestService.zipFile();
			assertTrue(f.exists());
		}	
		
		
		try { 
			Files.deleteIfExists(f1.toPath()); 
			Files.deleteIfExists(f2.toPath()); 
		}
		catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSendSupportRequest(){

		ServerSettings serverSettings = mock(ServerSettings.class);
		StatsCollector statsCollector = mock(StatsCollector.class);

		doReturn(serverSettings).when(restServiceSpy).getServerSettingsInternal();
		doReturn("key").when(serverSettings).getLicenceKey();

		doReturn(statsCollector).when(restServiceSpy).getStatsCollector();
		doReturn(10).when(statsCollector).getCpuLoad();

		SupportRequest supportRequest = new SupportRequest();

		supportRequest.setName("testName");
		supportRequest.setEmail("test@antmedia.io");
		supportRequest.setTitle("test title");
		supportRequest.setDescription("test desc");
		supportRequest.setSendSystemInfo(true);

		try{
			assertTrue(restServiceSpy.sendSupport(supportRequest));
		}catch (Exception e){
			e.printStackTrace();
		}
	}

}