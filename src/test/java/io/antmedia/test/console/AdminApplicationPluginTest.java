package io.antmedia.test.console;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.plugin.PluginDeployer;
import org.red5.server.plugin.PluginRegistry;

import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.console.AdminApplication;
import io.antmedia.rest.model.Result;
import io.vertx.core.Vertx;

public class AdminApplicationPluginTest {

	static Vertx vertx;

	private AdminApplication app;
	private PluginDeployer pluginDeployer;
	private IClusterNotifier clusterNotifier;

	@Before
	public void before() throws Exception {
		if (vertx == null) {
			vertx = Vertx.vertx();
		}

		app = Mockito.spy(new AdminApplication());
		app.setVertx(vertx);

		pluginDeployer = Mockito.mock(PluginDeployer.class);
		app.setPluginDeployer(pluginDeployer);

		clusterNotifier = Mockito.mock(IClusterNotifier.class);
		app.setClusterNotifier(clusterNotifier);

		doReturn("test-secret").when(app).getClusterCommunicationKey();
	}

	@After
	public void after() throws Exception {
		PluginRegistry.shutdown();
	}

	// =========================================================================
	// deployPlugin (ZIP path)
	// =========================================================================

	@Test
	public void testDeployPlugin_success() throws Exception {
		String pluginName = "testDeploy";
		InputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});

		File fakeZip = File.createTempFile(pluginName, ".zip");
		fakeZip.deleteOnExit();

		doReturn(fakeZip).when(app).savePluginZip(eq(pluginName), any(InputStream.class));
		when(pluginDeployer.loadPluginFromZip(eq(fakeZip), any(File.class))).thenReturn(new Result(true));
		doReturn("http://localhost:5080/rest/v2/plugins/" + pluginName + "/download")
				.when(app).buildPluginDownloadURI(pluginName);

		boolean result = app.deployPlugin(pluginName, stream);

		assertTrue(result);
		verify(clusterNotifier).notifyDeployPlugin(
				eq(pluginName),
				contains("/download"),
				anyString());
	}

	@Test
	public void testDeployPlugin_saveZipFails() throws Exception {
		InputStream stream = new ByteArrayInputStream(new byte[]{1});
		doReturn(null).when(app).savePluginZip(eq("saveFail"), any());

		boolean result = app.deployPlugin("saveFail", stream);

		assertFalse(result);
		verify(pluginDeployer, never()).loadPluginFromZip(any(), any());
	}

	@Test
	public void testDeployPlugin_loadPluginFromZipFails() throws Exception {
		InputStream stream = new ByteArrayInputStream(new byte[]{1});
		File fakeZip = File.createTempFile("loadFail", ".zip");
		fakeZip.deleteOnExit();

		doReturn(fakeZip).when(app).savePluginZip(eq("loadFail"), any());
		when(pluginDeployer.loadPluginFromZip(eq(fakeZip), any())).thenReturn(new Result(false, "error"));

		boolean result = app.deployPlugin("loadFail", stream);

		assertFalse(result);
		verify(clusterNotifier, never()).notifyDeployPlugin(anyString(), anyString(), anyString());
	}

	@Test
	public void testDeployPlugin_noCluster() throws Exception {
		app.setClusterNotifier(null);
		InputStream stream = new ByteArrayInputStream(new byte[]{1});
		File fakeZip = File.createTempFile("noCluster", ".zip");
		fakeZip.deleteOnExit();

		doReturn(fakeZip).when(app).savePluginZip(eq("noCluster"), any());
		when(pluginDeployer.loadPluginFromZip(eq(fakeZip), any())).thenReturn(new Result(true));

		boolean result = app.deployPlugin("noCluster", stream);

		assertTrue(result); // no NPE
	}

	@Test
	public void testDeployPlugin_invalidName() throws Exception {
		InputStream stream = new ByteArrayInputStream(new byte[]{1});

		assertFalse(app.deployPlugin("../evil", stream));
		assertFalse(app.deployPlugin("foo/bar", stream));
		assertFalse(app.deployPlugin("my.plugin", stream));
		verify(pluginDeployer, never()).loadPluginFromZip(any(), any());
	}

	// =========================================================================
	// deployPluginWithURL
	// =========================================================================

	@Test
	public void testDeployPluginWithURL_success() throws Exception {
		File fakeZip = File.createTempFile("urlPlugin", ".zip");
		fakeZip.deleteOnExit();

		doReturn(fakeZip).when(app).downloadPluginZip(eq("urlPlugin"), anyString(), anyString());
		when(pluginDeployer.loadPluginFromZip(eq(fakeZip), any())).thenReturn(new Result(true));

		boolean result = app.deployPluginWithURL("urlPlugin", "http://origin/download", "secret");

		assertTrue(result);
	}

	@Test
	public void testDeployPluginWithURL_downloadFails() throws Exception {
		doReturn(null).when(app).downloadPluginZip(anyString(), anyString(), anyString());

		boolean result = app.deployPluginWithURL("failDl", "http://origin/...", "secret");

		assertFalse(result);
	}

	@Test
	public void testDeployPluginWithURL_downloadThrows() throws Exception {
		doThrow(new IOException("network error"))
				.when(app).downloadPluginZip(anyString(), anyString(), anyString());

		boolean result = app.deployPluginWithURL("throwDl", "http://origin/...", "secret");

		assertFalse(result);
	}

	// =========================================================================
	// undeployPlugin
	// =========================================================================

	@Test
	public void testUndeployPlugin_success() throws Exception {
		String pluginName = "undeployOk";
		when(pluginDeployer.unloadPlugin(pluginName)).thenReturn(new Result(true));

		File tempRoot = Files.createTempDirectory("red5root-undeploy").toFile();
		tempRoot.deleteOnExit();
		System.setProperty("red5.root", tempRoot.getAbsolutePath());
		File pluginsDir = new File(tempRoot, "plugins");
		pluginsDir.mkdirs();
		File zip = new File(pluginsDir, pluginName + ".zip");
		zip.createNewFile();

		boolean result = app.undeployPlugin(pluginName);

		assertTrue(result);
		verify(pluginDeployer).unloadPlugin(pluginName);
		verify(pluginDeployer).runUninstallScript(eq(pluginName), any(File.class));
		assertFalse(zip.exists());
		verify(clusterNotifier).notifyUndeployPlugin(pluginName);
	}

	@Test
	public void testUndeployPlugin_notFound() {
		when(pluginDeployer.unloadPlugin("missing")).thenReturn(new Result(false, "not found"));

		boolean result = app.undeployPlugin("missing");

		assertFalse(result);
		verify(clusterNotifier, never()).notifyUndeployPlugin(anyString());
	}

	@Test
	public void testUndeployPlugin_noCluster() throws Exception {
		app.setClusterNotifier(null);
		when(pluginDeployer.unloadPlugin("noClUp")).thenReturn(new Result(true));

		boolean result = app.undeployPlugin("noClUp");

		assertTrue(result);
	}

	@Test
	public void testUndeployPlugin_invalidName() {
		assertFalse(app.undeployPlugin("../../etc/passwd"));
		assertFalse(app.undeployPlugin("my.plugin"));
		verify(pluginDeployer, never()).unloadPlugin(anyString());
	}

	// =========================================================================
	// savePluginZip
	// =========================================================================

	@Test
	public void testSavePluginZip_writesCorrectBytes() throws Exception {
		File tempRoot = Files.createTempDirectory("red5root-save").toFile();
		tempRoot.deleteOnExit();
		System.setProperty("red5.root", tempRoot.getAbsolutePath());

		byte[] data = {10, 20, 30, 40, 50};
		InputStream stream = new ByteArrayInputStream(data);

		File saved = app.savePluginZip("byteCheck", stream);

		assertNotNull(saved);
		assertEquals(data.length, saved.length());
		assertTrue(saved.getName().endsWith(".zip"));
	}

	@Test
	public void testSavePluginZip_nullStream() {
		File result = app.savePluginZip("nullStream", null);
		assertNull(result);
	}

	@Test
	public void testSavePluginZip_invalidName() {
		InputStream stream = new ByteArrayInputStream(new byte[]{1});
		assertNull(app.savePluginZip("../evil", stream));
	}

	// =========================================================================
	// buildPluginDownloadURI
	// =========================================================================

	@Test
	public void testBuildPluginDownloadURI_defaultProperties() {
		System.clearProperty("red5.host");
		System.clearProperty("http.port");

		String uri = app.buildPluginDownloadURI("myPlugin");

		assertEquals("http://localhost:5080/rest/v2/plugins/myPlugin/download", uri);
	}

	@Test
	public void testBuildPluginDownloadURI_customProperties() {
		System.setProperty("red5.host", "10.0.0.1");
		System.setProperty("http.port", "8080");
		try {
			String uri = app.buildPluginDownloadURI("myPlugin");
			assertEquals("http://10.0.0.1:8080/rest/v2/plugins/myPlugin/download", uri);
		} finally {
			System.clearProperty("red5.host");
			System.clearProperty("http.port");
		}
	}

	// =========================================================================
	// downloadPluginZip
	// =========================================================================

	@Test
	public void testDownloadPluginZip_success() throws Exception {
		File tempRoot = Files.createTempDirectory("red5root-dl").toFile();
		tempRoot.deleteOnExit();
		System.setProperty("red5.root", tempRoot.getAbsolutePath());

		byte[] zipBytes = {1, 2, 3, 4, 5};

		CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
		CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
		StatusLine mockStatusLine = mock(StatusLine.class);
		HttpEntity mockEntity = mock(HttpEntity.class);

		when(mockStatusLine.getStatusCode()).thenReturn(200);
		when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
		when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(zipBytes));
		when(mockResponse.getEntity()).thenReturn(mockEntity);
		when(mockClient.execute(any())).thenReturn(mockResponse);
		doReturn(mockClient).when(app).getHttpClient();

		File result = app.downloadPluginZip("myPlugin", "http://origin/download", "secret");

		assertNotNull(result);
		assertEquals(zipBytes.length, result.length());
	}

	@Test
	public void testDownloadPluginZip_nonOkResponse() throws Exception {
		CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
		CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
		StatusLine mockStatusLine = mock(StatusLine.class);

		when(mockStatusLine.getStatusCode()).thenReturn(403);
		when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
		when(mockClient.execute(any())).thenReturn(mockResponse);
		doReturn(mockClient).when(app).getHttpClient();

		File result = app.downloadPluginZip("myPlugin", "http://origin/...", "secret");

		assertNull(result);
	}

	// =========================================================================
	// getAllPluginNames
	// =========================================================================

	@Test
	public void testGetAllPluginNames_empty() {
		when(pluginDeployer.getPluginNames()).thenReturn(Set.of());

		List<String> names = app.getAllPluginNames();

		assertTrue(names.isEmpty());
	}

	@Test
	public void testGetAllPluginNames_withPlugins() {
		when(pluginDeployer.getPluginNames()).thenReturn(Set.of("plugin-a", "plugin-b"));

		List<String> names = app.getAllPluginNames();

		assertEquals(2, names.size());
		assertTrue(names.contains("plugin-a"));
		assertTrue(names.contains("plugin-b"));
	}

	@Test
	public void testGetAllPluginNames_nullDeployer() {
		app.setPluginDeployer(null);

		List<String> names = app.getAllPluginNames();

		assertTrue(names.isEmpty());
	}
}
