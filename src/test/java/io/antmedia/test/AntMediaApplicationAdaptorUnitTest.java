package io.antmedia.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.antmedia.AntMediaApplicationAdapter;

public class AntMediaApplicationAdaptorUnitTest {
	
	AntMediaApplicationAdapter adapter;
	String streamsFolder = "webapps/test/streams";
	
	@Before
	public void before() {
		adapter = new AntMediaApplicationAdapter();
		File f = new File(streamsFolder);
		f.delete();
	}
	
	@After
	public void after() {
		adapter = null;
		
		
	}
	
	
	@Test
	public void testSynchUserVodThrowException() {
		File f = new File(streamsFolder);
		assertTrue(f.mkdirs());
		
		File emptyFile = new File(streamsFolder, "emptyfile");
		emptyFile.deleteOnExit();
		try {
			assertTrue(emptyFile.createNewFile());
			boolean synchUserVoDFolder = adapter.deleteOldFolderPath("", f);
			assertFalse(synchUserVoDFolder);
			
			synchUserVoDFolder = adapter.deleteOldFolderPath(null, f);
			assertFalse(synchUserVoDFolder);
			
			synchUserVoDFolder = adapter.deleteOldFolderPath("anyfile", null);
			assertFalse(synchUserVoDFolder);
			
			
			synchUserVoDFolder = adapter.deleteOldFolderPath("notexist", f);
			assertFalse(synchUserVoDFolder);
			
			synchUserVoDFolder = adapter.deleteOldFolderPath(emptyFile.getName(), f);
			assertFalse(synchUserVoDFolder);
			
			File oldDir = new File (streamsFolder, "dir");
			oldDir.mkdirs();
			oldDir.deleteOnExit();
			
			synchUserVoDFolder = adapter.deleteOldFolderPath(oldDir.getName(), f);
			assertTrue(synchUserVoDFolder);
			
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
	}

}
