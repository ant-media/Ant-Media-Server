package io.antmedia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.BasicScope;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.integration.AppFunctionalTest;

public class AntMediaApplicationAdaptorUnitTest {
	
	AntMediaApplicationAdapter adapter;
	String streamsFolderPath = "webapps/test/streams";
	
	@Before
	public void before() {
		adapter = new AntMediaApplicationAdapter();
		File f = new File(streamsFolderPath);
		try {
			AppFunctionalTest.delete(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@After
	public void after() {
		adapter = null;
		
		
	}
	
	@Test
	public void testSynchUserVoD() {
		File streamsFolder = new File(streamsFolderPath);
		if (!streamsFolder.exists()) {
			assertTrue(streamsFolder.mkdirs());
		}
		IDataStore dataStore = new InMemoryDataStore("dbname");
		adapter.setDataStore(dataStore);

		IScope scope = Mockito.mock(IScope.class);
		Mockito.when(scope.getName()).thenReturn("test");
		
		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		Mockito.doReturn(scope).when(spyAdapter).getScope();
		
		
		
		File realPath = new File("src/test/resources");
		assertTrue(realPath.exists());
		
		String linkFilePath = streamsFolder.getAbsolutePath() + "/resources";
		File linkFile = new File(linkFilePath);
		//Files.isSymbolicLink(linkFile.toPath());
		try {
			Files.deleteIfExists(linkFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		boolean result = spyAdapter.synchUserVoDFolder(null, realPath.getAbsolutePath());
		assertTrue(result);
		
		//we know there are 5 files in src/test/resources
		//test_short.flv
		//test_video_360p_subtitle.flv
		//test_Video_360p.flv
		//test.flv
		//sample_MP4_480.mp4
		List<VoD> vodList = dataStore.getVodList(0, 50);
		assertEquals(5, vodList.size());
		
		for (VoD voD : vodList) {
			assertEquals("streams/resources/" + voD.getVodName(), voD.getFilePath());
		}
		
		linkFile = new File(streamsFolder, "resources");
		assertTrue(linkFile.exists());
		
		
	}
	
	
	
	@Test
	public void testSynchUserVodThrowException() {
		File f = new File(streamsFolderPath);
		assertTrue(f.mkdirs());
		
		File emptyFile = new File(streamsFolderPath, "emptyfile");
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
			
			File oldDir = new File (streamsFolderPath, "dir");
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
