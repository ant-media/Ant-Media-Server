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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.server.plugin.MinimalTestPlugin;
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

    @Test
    public void testDeployPlugin_success() throws Exception {
        String pluginName = "testDeploy";
        InputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});

        File fakeJar = File.createTempFile(pluginName, ".jar");
        fakeJar.deleteOnExit();

        doReturn(fakeJar).when(app).savePluginJar(eq(pluginName), any(InputStream.class));
        when(pluginDeployer.loadPlugin(fakeJar)).thenReturn(new Result(true));
        doReturn("http://localhost:5080/rest/v2/plugins/" + pluginName + "/download")
                .when(app).buildPluginDownloadURI(pluginName);

        boolean result = app.deployPlugin(pluginName, stream);

        assertTrue(result);
        verify(app).buildPluginDownloadURI(pluginName);
        verify(clusterNotifier).notifyDeployPlugin(
                eq(pluginName),
                eq("http://localhost:5080/rest/v2/plugins/" + pluginName + "/download"),
                anyString());
    }

    @Test
    public void testDeployPlugin_duplicate() throws Exception {
        // Pre-register plugin so duplicate check fires
        MinimalTestPlugin.PLUGIN_NAME = "dupDeploy";
        MinimalTestPlugin dummyPlugin = new MinimalTestPlugin();
        PluginRegistry.register(dummyPlugin);

        InputStream stream = new ByteArrayInputStream(new byte[]{1});
        boolean result = app.deployPlugin("dupDeploy", stream);

        assertFalse(result);
        verify(app, never()).savePluginJar(anyString(), any());
        verify(pluginDeployer, never()).loadPlugin(any());
        verify(clusterNotifier, never()).notifyDeployPlugin(anyString(), anyString(), anyString());
    }

    @Test
    public void testDeployPlugin_saveJarFails() throws Exception {
        String pluginName = "saveFailPlugin";
        InputStream stream = new ByteArrayInputStream(new byte[]{1});

        doReturn(null).when(app).savePluginJar(eq(pluginName), any());

        boolean result = app.deployPlugin(pluginName, stream);

        assertFalse(result);
        verify(pluginDeployer, never()).loadPlugin(any());
    }

    @Test
    public void testDeployPlugin_loadPluginFails() throws Exception {
        String pluginName = "loadFailPlugin";
        InputStream stream = new ByteArrayInputStream(new byte[]{1});

        File fakeJar = File.createTempFile(pluginName, ".jar");
        fakeJar.deleteOnExit();

        doReturn(fakeJar).when(app).savePluginJar(eq(pluginName), any());
        when(pluginDeployer.loadPlugin(fakeJar)).thenReturn(new Result(false, "load error"));

        boolean result = app.deployPlugin(pluginName, stream);

        assertFalse(result);
        verify(clusterNotifier, never()).notifyDeployPlugin(anyString(), anyString(), anyString());
    }

    @Test
    public void testDeployPlugin_noCluster() throws Exception {
        // Remove cluster notifier
        app.setClusterNotifier(null);
        String pluginName = "noClusterPlugin";
        InputStream stream = new ByteArrayInputStream(new byte[]{1});

        File fakeJar = File.createTempFile(pluginName, ".jar");
        fakeJar.deleteOnExit();

        doReturn(fakeJar).when(app).savePluginJar(eq(pluginName), any());
        when(pluginDeployer.loadPlugin(fakeJar)).thenReturn(new Result(true));

        boolean result = app.deployPlugin(pluginName, stream);

        assertTrue(result); // no NPE
    }

    @Test
    public void testDeployPlugin_jarSavedToPluginsDir() throws Exception {
        String pluginName = "jarPathPlugin";
        InputStream stream = new ByteArrayInputStream(new byte[]{0, 1, 2, 3});

        File tempRoot = Files.createTempDirectory("red5root").toFile();
        tempRoot.deleteOnExit();
        System.setProperty("red5.root", tempRoot.getAbsolutePath());

        doCallRealMethod().when(app).savePluginJar(eq(pluginName), any());
        when(pluginDeployer.loadPlugin(any())).thenReturn(new Result(true));
        doReturn("http://localhost:5080/rest/v2/plugins/jarPathPlugin/download")
                .when(app).buildPluginDownloadURI(anyString());

        app.deployPlugin(pluginName, stream);

        ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
        verify(pluginDeployer).loadPlugin(captor.capture());
        assertTrue(captor.getValue().getAbsolutePath()
                .contains("plugins" + File.separator + pluginName + ".jar"));
    }

    // -------------------------------------------------------------------------
    // deployPluginWithURL
    // -------------------------------------------------------------------------

    @Test
    public void testDeployPluginWithURL_success() throws Exception {
        String pluginName = "urlPlugin";
        File fakeJar = File.createTempFile(pluginName, ".jar");
        fakeJar.deleteOnExit();

        doReturn(fakeJar).when(app).downloadPluginJar(eq(pluginName), anyString(), anyString());
        when(pluginDeployer.loadPlugin(fakeJar)).thenReturn(new Result(true));

        boolean result = app.deployPluginWithURL(pluginName, "http://origin/plugins/urlPlugin/download", "secret");

        assertTrue(result);
        verify(pluginDeployer).loadPlugin(fakeJar);
    }

    @Test
    public void testDeployPluginWithURL_duplicate() throws Exception {
        MinimalTestPlugin.PLUGIN_NAME = "urlDupPlugin";
        PluginRegistry.register(new MinimalTestPlugin());

        boolean result = app.deployPluginWithURL("urlDupPlugin", "http://origin/...", "secret");

        assertFalse(result);
        verify(app, never()).downloadPluginJar(anyString(), anyString(), anyString());
    }

    @Test
    public void testDeployPluginWithURL_downloadFails() throws Exception {
        doReturn(null).when(app).downloadPluginJar(anyString(), anyString(), anyString());

        boolean result = app.deployPluginWithURL("failDl", "http://origin/...", "secret");

        assertFalse(result);
        verify(pluginDeployer, never()).loadPlugin(any());
    }

    @Test
    public void testDeployPluginWithURL_downloadThrows() throws Exception {
        doThrow(new java.io.IOException("network error"))
                .when(app).downloadPluginJar(anyString(), anyString(), anyString());

        boolean result = app.deployPluginWithURL("throwDl", "http://origin/...", "secret");

        assertFalse(result);
        verify(pluginDeployer, never()).loadPlugin(any());
    }

    @Test
    public void testUndeployPlugin_success() throws Exception {
        String pluginName = "undeployOk";
        when(pluginDeployer.unloadPlugin(pluginName)).thenReturn(new Result(true));

        File tempRoot = Files.createTempDirectory("red5root2").toFile();
        tempRoot.deleteOnExit();
        System.setProperty("red5.root", tempRoot.getAbsolutePath());
        File pluginsDir = new File(tempRoot, "plugins");
        pluginsDir.mkdirs();
        File jar = new File(pluginsDir, pluginName + ".jar");
        jar.createNewFile();

        boolean result = app.undeployPlugin(pluginName);

        assertTrue(result);
        verify(pluginDeployer).unloadPlugin(pluginName);
        assertFalse(jar.exists());
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
        verify(pluginDeployer).unloadPlugin("noClUp");
    }

    @Test
    public void testUndeployPlugin_jarFileNotOnDisk() throws Exception {
        when(pluginDeployer.unloadPlugin("noJar")).thenReturn(new Result(true));

        File tempRoot = Files.createTempDirectory("red5root3").toFile();
        tempRoot.deleteOnExit();
        System.setProperty("red5.root", tempRoot.getAbsolutePath());

        boolean result = app.undeployPlugin("noJar");

        assertTrue(result);
    }

    @Test
    public void testSavePluginJar_writesCorrectBytes() throws Exception {
        File tempRoot = Files.createTempDirectory("red5root4").toFile();
        tempRoot.deleteOnExit();
        System.setProperty("red5.root", tempRoot.getAbsolutePath());

        byte[] data = {10, 20, 30, 40, 50};
        InputStream stream = new ByteArrayInputStream(data);

        File saved = app.savePluginJar("byteCheckPlugin", stream);

        assertNotNull(saved);
        assertEquals(data.length, saved.length());
    }

    @Test
    public void testSavePluginJar_nullStream() throws Exception {
        File result = app.savePluginJar("nullStreamPlugin", null);
        assertNull(result);
    }

    // --- isValidPluginName coverage ---

    @Test
    public void testSavePluginJar_pathTraversalName() throws Exception {
        InputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});

        File result = app.savePluginJar("../evil", stream);

        assertNull(result);
    }

    @Test
    public void testSavePluginJar_nameWithSlash() throws Exception {
        InputStream stream = new ByteArrayInputStream(new byte[]{1});

        File result = app.savePluginJar("foo/bar", stream);

        assertNull(result);
    }

    @Test
    public void testUndeployPlugin_pathTraversalName() {
        boolean result = app.undeployPlugin("../../etc/passwd");

        assertFalse(result);
        verify(pluginDeployer, never()).unloadPlugin(anyString());
    }

    @Test
    public void testUndeployPlugin_nameWithDot() {
        boolean result = app.undeployPlugin("my.plugin");

        assertFalse(result);
        verify(pluginDeployer, never()).unloadPlugin(anyString());
    }

    @Test
    public void testDeployPlugin_pathTraversalName_rejectedBeforeSave() throws Exception {
        InputStream stream = new ByteArrayInputStream(new byte[]{1});

        boolean result = app.deployPlugin("../evil", stream);

        assertFalse(result);
        verify(pluginDeployer, never()).loadPlugin(any());
    }

    // --- buildPluginDownloadURI ---

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

    // --- downloadPluginJar ---

    @Test
    public void testDownloadPluginJar_successResponse_savesFile() throws Exception {
        File tempRoot = Files.createTempDirectory("red5root-dl-jar").toFile();
        tempRoot.deleteOnExit();
        System.setProperty("red5.root", tempRoot.getAbsolutePath());

        byte[] jarBytes = {1, 2, 3, 4, 5};

        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);
        HttpEntity mockEntity = mock(HttpEntity.class);

        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(jarBytes));
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockClient.execute(any())).thenReturn(mockResponse);
        doReturn(mockClient).when(app).getHttpClient();

        File result = app.downloadPluginJar("myPlugin", "http://origin/plugins/myPlugin/download", "secret");

        assertNotNull(result);
        assertEquals(jarBytes.length, result.length());
    }

    @Test
    public void testDownloadPluginJar_nonOkResponse_returnsNull() throws Exception {
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);

        when(mockStatusLine.getStatusCode()).thenReturn(403);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockClient.execute(any())).thenReturn(mockResponse);
        doReturn(mockClient).when(app).getHttpClient();

        File result = app.downloadPluginJar("myPlugin", "http://origin/...", "secret");

        assertNull(result);
    }

    @Test
    public void testDownloadPluginJar_ioException_propagates() throws Exception {
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        when(mockClient.execute(any())).thenThrow(new IOException("network error"));
        doReturn(mockClient).when(app).getHttpClient();

        try {
            app.downloadPluginJar("myPlugin", "http://origin/...", "secret");
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("network error", e.getMessage());
        }
    }

    // --- getAllPluginNames ---

    @Test
    public void testGetAllPluginNames_emptyBothSources() {
        when(pluginDeployer.getSpringPluginNames()).thenReturn(Set.of());

        List<String> names = app.getAllPluginNames();

        assertTrue(names.isEmpty());
    }

    @Test
    public void testGetAllPluginNames_springPluginsOnly() {
        when(pluginDeployer.getSpringPluginNames()).thenReturn(Set.of("spring-plugin"));

        List<String> names = app.getAllPluginNames();

        assertEquals(1, names.size());
        assertTrue(names.contains("spring-plugin"));
    }

    @Test
    public void testGetAllPluginNames_unionOfBothSources() throws Exception {
        MinimalTestPlugin.PLUGIN_NAME = "legacy-plugin";
        PluginRegistry.register(new MinimalTestPlugin());
        when(pluginDeployer.getSpringPluginNames()).thenReturn(Set.of("spring-plugin"));

        List<String> names = app.getAllPluginNames();

        assertTrue(names.contains("legacy-plugin"));
        assertTrue(names.contains("spring-plugin"));
        assertEquals(2, names.size());
    }

    @Test
    public void testGetAllPluginNames_nullDeployer() {
        app.setPluginDeployer(null);

        List<String> names = app.getAllPluginNames();

        assertTrue(names.isEmpty());
    }
}
