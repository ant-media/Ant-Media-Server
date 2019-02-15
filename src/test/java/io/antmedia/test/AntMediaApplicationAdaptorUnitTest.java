package io.antmedia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.server.api.scope.IScope;

import com.jmatio.io.stream.ByteBufferInputStream;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.integration.AppFunctionalTest;
import io.vertx.core.Vertx;

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
		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		adapter.setDataStoreFactory(dsf);

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
	public void testMuxingFinished() {

		AppSettings appSettings = new AppSettings();
		appSettings.setMuxerFinishScript("src/test/resources/echo.sh");
		adapter.setAppSettings(appSettings);
		File f = new File ("src/test/resources/hello_script");

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		adapter.setDataStoreFactory(dsf);

		adapter.setVertx(Vertx.vertx());

		File anyFile = new File("src/test/resources/sample_MP4_480.mp4");

		{

			assertFalse(f.exists());

			adapter.muxingFinished("streamId", anyFile, 100, 480);

			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(()-> f.exists());

			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		}

		{
			appSettings.setMuxerFinishScript("");
			adapter.setAppSettings(appSettings);

			assertFalse(f.exists());

			adapter.muxingFinished("streamId", anyFile, 100, 480);

			Awaitility.await().pollDelay(3, TimeUnit.SECONDS).atMost(4, TimeUnit.SECONDS).until(()-> !f.exists());
		}

	}

	@Test
	public void testRunMuxerScript() {
		File f = new File ("src/test/resources/hello_script");
		assertFalse(f.exists());

		adapter.setVertx(Vertx.vertx());
		adapter.runScript("src/test/resources/echo.sh");

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(()-> f.exists());

		try {
			Files.delete(f.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	@Test
	public void testSendPost() {
		try {
			AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);

			CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
			Mockito.doReturn(httpClient).when(spyAdaptor).getHttpClient();

			CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
			Mockito.when(httpClient.execute(Mockito.any())).thenReturn(httpResponse);
			Mockito.when(httpResponse.getStatusLine()).thenReturn(Mockito.mock(StatusLine.class));
			
			Mockito.when(httpResponse.getEntity()).thenReturn(null);
			StringBuilder response = spyAdaptor.sendPOST("http://any_url", new HashMap());
			assertNull(response);
			
			HttpEntity entity = Mockito.mock(HttpEntity.class);
			InputStream is = new ByteBufferInputStream(ByteBuffer.allocate(10), 10);
			Mockito.when(entity.getContent()).thenReturn(is);
			Mockito.when(httpResponse.getEntity()).thenReturn(entity);
			HashMap map = new HashMap();
			map.put("action", "action_any");
			response = spyAdaptor.sendPOST("http://any_url", map);
			assertNotNull(response);
			assertEquals(10, response.length());
			
			
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testNotifyHook() {

		AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);

		StringBuilder notifyHook = spyAdaptor.notifyHook(null, null, null, null, null, null, null);
		assertNull(notifyHook);

		notifyHook = spyAdaptor.notifyHook("", null, null, null, null, null, null);
		assertNull(notifyHook);


		String id = String.valueOf((Math.random() * 10000));
		String action = "any_action";
		String streamName = String.valueOf((Math.random() * 10000));
		String category = "category";

		String vodName = "vod name" + String.valueOf((Math.random() * 10000));
		String vodId = String.valueOf((Math.random() * 10000));

		String url = "this is url";
		notifyHook = spyAdaptor.notifyHook(url, id, action, streamName, category, vodName, vodId);
		assertNull(notifyHook);

		try {
			ArgumentCaptor<String> captureUrl = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<Map> variables = ArgumentCaptor.forClass(Map.class);
			Mockito.verify(spyAdaptor).sendPOST(captureUrl.capture(), variables.capture());
			assertEquals(url, captureUrl.getValue());

			Map variablesMap = variables.getValue();
			assertEquals(id, variablesMap.get("id"));
			assertEquals(action, variablesMap.get("action"));
			assertEquals(streamName, variablesMap.get("streamName"));
			assertEquals(category, variablesMap.get("category"));
			assertEquals(vodName, variablesMap.get("vodName"));
			assertEquals(vodId, variablesMap.get("vodId"));

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


		url = "this is second  url";
		notifyHook = spyAdaptor.notifyHook(url, id, null, null, null, null, null);
		assertNull(notifyHook);

		try {
			ArgumentCaptor<String> captureUrl = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<Map> variables = ArgumentCaptor.forClass(Map.class);
			Mockito.verify(spyAdaptor, Mockito.times(2)).sendPOST(captureUrl.capture(), variables.capture());
			assertEquals(url, captureUrl.getValue());

			Map variablesMap = variables.getValue();
			assertEquals(id, variablesMap.get("id"));
			assertNull(variablesMap.get("action"));
			assertNull(variablesMap.get("streamName"));
			assertNull(variablesMap.get("category"));
			assertNull(variablesMap.get("vodName"));
			assertNull(variablesMap.get("vodId"));

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

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
