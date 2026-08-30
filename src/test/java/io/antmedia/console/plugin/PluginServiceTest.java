package io.antmedia.console.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.red5.server.plugin.PluginRegistry;

import io.antmedia.filter.TokenFilterManager;
import io.antmedia.plugin.PluginDeployer;
import io.antmedia.plugin.api.PluginRecord;
import io.antmedia.plugin.api.PluginState;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;

public class PluginServiceTest {

    private static final String REGISTRY_URL = "http://example.com/catalog.json";
    private static final String REGISTRY_ZIP_URL = "http://example.com/ClipCreatorPlugin/clip-creator.zip";

    private PluginService pluginService;
    private PluginDeployer pluginDeployer;
    private ServerSettings serverSettings;
    private File pluginsDir;
    private String previousRed5Root;

    @Before
    public void setUp() throws Exception {
        previousRed5Root = System.getProperty("red5.root");
        File root = Files.createTempDirectory("ams-plugin-service-test").toFile();
        System.setProperty("red5.root", root.getAbsolutePath());

        pluginDeployer = mock(PluginDeployer.class);
        pluginService = spy(new PluginService());
        pluginService.setPluginDeployer(pluginDeployer);

        // install-from-URL only accepts the configured registry host, so the tests need one.
        serverSettings = new ServerSettings();
        serverSettings.setPluginRegistryUrl(REGISTRY_URL);
        pluginService.setServerSettings(serverSettings);

        pluginsDir = pluginService.getPluginsDir();
    }

    @After
    public void tearDown() {
        if (previousRed5Root != null) {
            System.setProperty("red5.root", previousRed5Root);
        }
        else {
            System.clearProperty("red5.root");
        }
    }

    // --- id validation ---

    @Test
    public void testInstall_invalidId() {
        Result result = pluginService.install("../etc/passwd", new ByteArrayInputStream(new byte[]{1}));

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("not valid"));
        verify(pluginDeployer, never()).loadPluginFromZip(any(), any(), anyString());
    }

    @Test
    public void testInstallFromUrl_invalidId() {
        Result result = pluginService.installFromUrl("Bad Id", "http://example.com/p.zip", null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("not valid"));
    }

    @Test
    public void testUninstall_invalidId() {
        Result result = pluginService.uninstall("../bad");

        assertFalse(result.isSuccess());
        verify(pluginDeployer, never()).unloadPluginFromZip(anyString(), any());
    }

    // --- install from upload ---

    @Test
    public void testInstall_nullStream() {
        Result result = pluginService.install("clip-creator", null);

        assertFalse(result.isSuccess());
        assertEquals("No plugin ZIP uploaded", result.getMessage());
    }

    @Test
    public void testInstall_rejectsBundledId() {
        try (MockedStatic<PluginRegistry> registry = Mockito.mockStatic(PluginRegistry.class)) {
            registry.when(PluginRegistry::getPluginNames).thenReturn(Set.of("Clip Creator Plugin"));

            Result result = pluginService.install("clip-creator", new ByteArrayInputStream(new byte[]{1}));

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("already bundled"));
        }
    }

    @Test
    public void testInstall_success() {
        when(pluginDeployer.loadPluginFromZip(any(), any(), eq("clip-creator")))
                .thenReturn(new Result(true, "installed"));

        Result result = pluginService.install("clip-creator", new ByteArrayInputStream(new byte[]{1, 2, 3}));

        assertTrue(result.isSuccess());
        verify(pluginDeployer).loadPluginFromZip(any(File.class), eq(pluginsDir), eq("clip-creator"));
    }

    @Test
    public void testInstall_saveFails() {
        doReturn(null).when(pluginService).savePluginZip(anyString(), any(InputStream.class));

        Result result = pluginService.install("clip-creator", new ByteArrayInputStream(new byte[]{1}));

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Could not save"));
    }

    /** A failed deploy must not leave the rejected ZIP behind for the next boot scan to pick up. */
    @Test
    public void testInstall_deployFails_deletesZip() {
        when(pluginDeployer.loadPluginFromZip(any(), any(), anyString()))
                .thenReturn(new Result(false, "Plugin id mismatch"));

        Result result = pluginService.install("clip-creator", new ByteArrayInputStream(new byte[]{1}));

        assertFalse(result.isSuccess());
        assertEquals("Plugin id mismatch", result.getMessage());
        assertFalse(new File(pluginsDir, "clip-creator.zip").exists());
    }

    /** The deployer's message is what tells the user why an install failed. */
    @Test
    public void testInstall_propagatesDeployerMessage() {
        when(pluginDeployer.loadPluginFromZip(any(), any(), anyString()))
                .thenReturn(new Result(false, "AMS-Plugin-Id 'x' is not valid"));

        Result result = pluginService.install("clip-creator", new ByteArrayInputStream(new byte[]{1}));

        assertEquals("AMS-Plugin-Id 'x' is not valid", result.getMessage());
    }

    // --- install from url ---

    @Test
    public void testInstallFromUrl_blankUrl() {
        assertFalse(pluginService.installFromUrl("clip-creator", null, null).isSuccess());
        assertFalse(pluginService.installFromUrl("clip-creator", "", null).isSuccess());
        assertFalse(pluginService.installFromUrl("clip-creator", "   ", null).isSuccess());
    }

    @Test
    public void testInstallFromUrl_success() throws Exception {
        pluginService.setHttpClient(clientReturning(200, new byte[]{1, 2, 3}));
        when(pluginDeployer.loadPluginFromZip(any(), any(), eq("clip-creator")))
                .thenReturn(new Result(true, "installed"));

        Result result = pluginService.installFromUrl("clip-creator", REGISTRY_ZIP_URL, null);

        assertTrue(result.isSuccess());
    }

    @Test
    public void testInstallFromUrl_httpError() throws Exception {
        pluginService.setHttpClient(clientReturning(404, new byte[0]));

        Result result = pluginService.installFromUrl("clip-creator", "http://example.com/missing.zip", null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("404"));
        verify(pluginDeployer, never()).loadPluginFromZip(any(), any(), anyString());
    }

    @Test
    public void testInstallFromUrl_noJwtHeader() throws Exception {
        CloseableHttpClient client = clientReturning(200, new byte[]{1});
        pluginService.setHttpClient(client);
        when(pluginDeployer.loadPluginFromZip(any(), any(), anyString())).thenReturn(new Result(true, ""));

        pluginService.installFromUrl("clip-creator", "http://example.com/p.zip", null);

        assertNull(capturedRequest(client).getFirstHeader(
                TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION));
    }

    // --- registry host restriction ---

    @Test
    public void testInstallFromUrl_rejectsHostOutsideRegistry() throws Exception {
        CloseableHttpClient client = clientReturning(200, new byte[]{1});
        pluginService.setHttpClient(client);

        Result result = pluginService.installFromUrl("clip-creator", "http://169.254.169.254/latest/meta-data", null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("registry host"));
        // nothing may be fetched at all — the point is that the server never issues the request
        verify(client, never()).execute(any(HttpRequestBase.class));
        verify(pluginDeployer, never()).loadPluginFromZip(any(), any(), anyString());
    }

    @Test
    public void testInstallFromUrl_rejectsNonHttpScheme() {
        Result result = pluginService.installFromUrl("clip-creator", "file:///etc/passwd", null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("http"));
    }

    @Test
    public void testInstallFromUrl_rejectsMalformedUrl() {
        Result result = pluginService.installFromUrl("clip-creator", "http://exa mple.com/p.zip", null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Malformed"));
    }

    @Test
    public void testInstallFromUrl_rejectedWhenNoRegistryConfigured() {
        serverSettings.setPluginRegistryUrl(null);

        Result result = pluginService.installFromUrl("clip-creator", REGISTRY_ZIP_URL, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("No plugin registry"));
    }

    // --- checksum verification ---

    @Test
    public void testInstallFromUrl_checksumMatches() throws Exception {
        byte[] payload = {1, 2, 3};
        pluginService.setHttpClient(clientReturning(200, payload));
        when(pluginDeployer.loadPluginFromZip(any(), any(), eq("clip-creator")))
                .thenReturn(new Result(true, "installed"));

        Result result = pluginService.installFromUrl("clip-creator", REGISTRY_ZIP_URL, sha256Hex(payload));

        assertTrue(result.getMessage(), result.isSuccess());
    }

    @Test
    public void testInstallFromUrl_checksumMismatchRejectsAndDeletesZip() throws Exception {
        pluginService.setHttpClient(clientReturning(200, new byte[]{1, 2, 3}));

        Result result = pluginService.installFromUrl("clip-creator", REGISTRY_ZIP_URL,
                "0000000000000000000000000000000000000000000000000000000000000000");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Checksum mismatch"));
        verify(pluginDeployer, never()).loadPluginFromZip(any(), any(), anyString());
        assertFalse("the unverified ZIP must not be left on disk",
                new File(pluginsDir, "clip-creator.zip").exists());
    }

    @Test
    public void testInstallFromUrl_checksumIsCaseInsensitive() throws Exception {
        byte[] payload = {9, 8, 7};
        pluginService.setHttpClient(clientReturning(200, payload));
        when(pluginDeployer.loadPluginFromZip(any(), any(), eq("clip-creator")))
                .thenReturn(new Result(true, "installed"));

        Result result = pluginService.installFromUrl("clip-creator", REGISTRY_ZIP_URL,
                sha256Hex(payload).toUpperCase());

        assertTrue(result.isSuccess());
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    // --- install from cluster peer ---

    @Test
    public void testInstallFromClusterPeer_sendsJwtHeader() throws Exception {
        CloseableHttpClient client = clientReturning(200, new byte[]{1});
        pluginService.setHttpClient(client);
        when(pluginDeployer.loadPluginFromZip(any(), any(), anyString())).thenReturn(new Result(true, ""));

        Result result = pluginService.installFromClusterPeer("clip-creator",
                "http://peer:5080/rest/v2/plugins/clip-creator/download", "cluster-secret");

        assertTrue(result.isSuccess());
        Header token = capturedRequest(client)
                .getFirstHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION);
        assertNotNull(token);
        assertFalse(token.getValue().isEmpty());
    }

    @Test
    public void testInstallFromClusterPeer_downloadFails() throws Exception {
        pluginService.setHttpClient(clientThrowing());

        Result result = pluginService.installFromClusterPeer("clip-creator", "http://peer/p.zip", "secret");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Could not download"));
    }

    /** A zero-length body means the peer has no such ZIP, even though it answered 200. */
    @Test
    public void testInstallFromClusterPeer_emptyBodyRejected() throws Exception {
        pluginService.setHttpClient(clientReturning(200, new byte[0], "0"));

        Result result = pluginService.installFromClusterPeer("clip-creator", "http://peer/p.zip", "secret");

        assertFalse(result.isSuccess());
        verify(pluginDeployer, never()).loadPluginFromZip(any(), any(), anyString());
    }

    // --- uninstall ---

    @Test
    public void testUninstall_success_deletesZip() throws Exception {
        File zip = new File(pluginsDir, "clip-creator.zip");
        assertTrue(zip.createNewFile());
        when(pluginDeployer.unloadPluginFromZip(eq("clip-creator"), eq(pluginsDir)))
                .thenReturn(new Result(true, "removed"));

        Result result = pluginService.uninstall("clip-creator");

        assertTrue(result.isSuccess());
        assertFalse(zip.exists());
    }

    /** A failed unload must leave the ZIP in place so the plugin still resolves after a restart. */
    @Test
    public void testUninstall_unloadFails_keepsZip() throws Exception {
        File zip = new File(pluginsDir, "clip-creator.zip");
        assertTrue(zip.createNewFile());
        when(pluginDeployer.unloadPluginFromZip(anyString(), any()))
                .thenReturn(new Result(false, "not found"));

        Result result = pluginService.uninstall("clip-creator");

        assertFalse(result.isSuccess());
        assertEquals("not found", result.getMessage());
        assertTrue(zip.exists());
    }

    @Test
    public void testUninstall_missingZipStillSucceeds() {
        when(pluginDeployer.unloadPluginFromZip(anyString(), any())).thenReturn(new Result(true, "removed"));

        assertTrue(pluginService.uninstall("clip-creator").isSuccess());
    }

    // --- listing ---

    @Test
    public void testList_combinesBundledAndInstalled() {
        PluginRecord installed = new PluginRecord();
        installed.setName("Filter Plugin");
        installed.setPluginId("filter");
        installed.setState(PluginState.ACTIVE);
        when(pluginDeployer.getAllPluginRecords()).thenReturn(Collections.singletonList(installed));

        try (MockedStatic<PluginRegistry> registry = Mockito.mockStatic(PluginRegistry.class)) {
            registry.when(PluginRegistry::getPluginNames).thenReturn(Set.of("Clip Creator Plugin"));

            List<PluginRecord> records = pluginService.list();

            assertEquals(2, records.size());
        }
    }

    /** Bundled plugins register under a display name, so the list must expose the derived id. */
    @Test
    public void testList_bundledPluginGetsDerivedId() {
        when(pluginDeployer.getAllPluginRecords()).thenReturn(Collections.emptyList());

        try (MockedStatic<PluginRegistry> registry = Mockito.mockStatic(PluginRegistry.class)) {
            registry.when(PluginRegistry::getPluginNames).thenReturn(Set.of("Clip Creator Plugin"));

            PluginRecord record = pluginService.list().get(0);

            assertEquals("Clip Creator Plugin", record.getName());
            assertEquals("clip-creator", record.getPluginId());
            assertEquals(PluginState.ACTIVE, record.getState());
        }
    }

    @Test
    public void testList_empty() {
        when(pluginDeployer.getAllPluginRecords()).thenReturn(Collections.emptyList());

        try (MockedStatic<PluginRegistry> registry = Mockito.mockStatic(PluginRegistry.class)) {
            registry.when(PluginRegistry::getPluginNames).thenReturn(Collections.emptySet());

            assertTrue(pluginService.list().isEmpty());
        }
    }

    // --- misc ---

    @Test
    public void testResolveZipForDownload() throws Exception {
        assertNull(pluginService.resolveZipForDownload("../bad"));
        assertNull(pluginService.resolveZipForDownload("clip-creator"));

        File zip = new File(pluginsDir, "clip-creator.zip");
        assertTrue(zip.createNewFile());
        assertEquals(zip, pluginService.resolveZipForDownload("clip-creator"));
    }

    @Test
    public void testGetPluginsDir_createsIfMissing() {
        assertTrue(pluginsDir.exists());
        assertTrue(pluginsDir.isDirectory());
        assertEquals("plugins", pluginsDir.getName());
    }

    @Test
    public void testSavePluginZip() {
        File saved = pluginService.savePluginZip("clip-creator", new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));

        assertNotNull(saved);
        assertTrue(saved.exists());
        assertEquals(5, saved.length());
    }

    @Test
    public void testScanInstalledPlugins_delegates() {
        pluginService.scanInstalledPlugins();
        verify(pluginDeployer).scanInstalledPlugins();
    }

    // --- helpers ---

    private static CloseableHttpClient clientReturning(int status, byte[] body) throws IOException {
        return clientReturning(status, body, null);
    }

    private static CloseableHttpClient clientReturning(int status, byte[] body, String contentLength)
            throws IOException {
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity entity = mock(HttpEntity.class);
        Header lengthHeader = contentLength == null ? null : mock(Header.class);

        when(statusLine.getStatusCode()).thenReturn(status);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(body));
        when(response.getEntity()).thenReturn(entity);
        if (lengthHeader != null) {
            when(lengthHeader.getValue()).thenReturn(contentLength);
        }
        when(response.getFirstHeader("Content-Length")).thenReturn(lengthHeader);
        when(client.execute(any(HttpRequestBase.class))).thenReturn(response);
        return client;
    }

    private static CloseableHttpClient clientThrowing() throws IOException {
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        when(client.execute(any(HttpRequestBase.class))).thenThrow(new IOException("connection refused"));
        return client;
    }

    private static HttpRequestBase capturedRequest(CloseableHttpClient client) throws IOException {
        ArgumentCaptor<HttpRequestBase> captor = ArgumentCaptor.forClass(HttpRequestBase.class);
        verify(client).execute(captor.capture());
        return captor.getValue();
    }
}
