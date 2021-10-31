package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import io.antmedia.console.rest.SupportRestService;

public class SupportRestServiceTest {


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
	}

	@Test
	public void testAppendErrorFromJournal() {
		//This test passes in linux 
		
		List<String> files = new ArrayList<>();
		String logFile = "log/antmedia-error-journalctl.log";
		File f = new File(logFile);
		f.getParentFile().mkdirs();
		SupportRestService.appendErrorFromJournal(files, logFile);

		try {
			byte[] bytes = Files.readAllBytes(Paths.get(logFile));
			assertTrue(bytes.length > 0);
			assertTrue(f.isFile());
			assertEquals(1, files.size());

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
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

}