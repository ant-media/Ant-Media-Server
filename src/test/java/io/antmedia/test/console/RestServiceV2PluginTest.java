package io.antmedia.test.console;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.antmedia.console.AdminApplication;
import io.antmedia.console.rest.RestServiceV2;
import io.antmedia.plugin.api.PluginRecord;
import io.antmedia.plugin.api.PluginState;
import io.antmedia.rest.model.Result;
import jakarta.ws.rs.core.Response;

public class RestServiceV2PluginTest {

    private RestServiceV2 restService;
    private AdminApplication adminApp;

    @Before
    public void before() {
        restService = Mockito.spy(new RestServiceV2());
        adminApp = Mockito.mock(AdminApplication.class);
        doReturn(adminApp).when(restService).getApplication();
    }

    // GET /rest/v2/plugins

    @Test
    public void testGetPlugins_empty() {
        when(adminApp.getAllPluginRecords()).thenReturn(new ArrayList<>());
        List<PluginRecord> result = restService.getPlugins();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetPlugins_withPlugins() {
        List<PluginRecord> records = new ArrayList<>();
        PluginRecord r = new PluginRecord();
        r.setName("Test Plugin");
        r.setState(PluginState.ACTIVE);
        records.add(r);
        when(adminApp.getAllPluginRecords()).thenReturn(records);

        List<PluginRecord> result = restService.getPlugins();
        assertEquals(1, result.size());
        assertEquals("Test Plugin", result.get(0).getName());
    }

    @Test
    public void testGetPlugins_noAdminApp() {
        doReturn(null).when(restService).getApplication();
        List<PluginRecord> result = restService.getPlugins();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // PUT /rest/v2/plugins/{name}

    @Test
    public void testDeployPlugin_success() {
        when(adminApp.deployPlugin(eq("test-plugin"), any(InputStream.class))).thenReturn(true);
        Result result = restService.deployPlugin("test-plugin", new ByteArrayInputStream(new byte[]{1}));
        assertTrue(result.isSuccess());
    }

    @Test
    public void testDeployPlugin_failure() {
        when(adminApp.deployPlugin(eq("test-plugin"), any(InputStream.class))).thenReturn(false);
        Result result = restService.deployPlugin("test-plugin", new ByteArrayInputStream(new byte[]{1}));
        assertFalse(result.isSuccess());
    }

    @Test
    public void testDeployPlugin_invalidName() {
        Result result = restService.deployPlugin("../bad", new ByteArrayInputStream(new byte[]{1}));
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("must match"));
    }

    @Test
    public void testDeployPlugin_nullStream() {
        Result result = restService.deployPlugin("test-plugin", null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void testDeployPlugin_noAdminApp() {
        doReturn(null).when(restService).getApplication();
        Result result = restService.deployPlugin("test-plugin", new ByteArrayInputStream(new byte[]{1}));
        assertFalse(result.isSuccess());
    }

    // POST /rest/v2/plugins/install-from-url

    @Test
    public void testInstallPluginFromUrl_success() {
        when(adminApp.installPluginFromUrl("test-plugin", "http://example.com/test.zip")).thenReturn(true);
        Map<String, String> body = new HashMap<>();
        body.put("id", "test-plugin");
        body.put("downloadUrl", "http://example.com/test.zip");
        Result result = restService.installPluginFromUrl(body);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testInstallPluginFromUrl_failure() {
        when(adminApp.installPluginFromUrl("test-plugin", "http://example.com/test.zip")).thenReturn(false);
        Map<String, String> body = new HashMap<>();
        body.put("id", "test-plugin");
        body.put("downloadUrl", "http://example.com/test.zip");
        Result result = restService.installPluginFromUrl(body);
        assertFalse(result.isSuccess());
    }

    @Test
    public void testInstallPluginFromUrl_invalidId() {
        Map<String, String> body = new HashMap<>();
        body.put("id", "../bad");
        body.put("downloadUrl", "http://example.com/test.zip");
        Result result = restService.installPluginFromUrl(body);
        assertFalse(result.isSuccess());
    }

    @Test
    public void testInstallPluginFromUrl_nullBody() {
        Result result = restService.installPluginFromUrl(null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void testInstallPluginFromUrl_nullUrl() {
        Map<String, String> body = new HashMap<>();
        body.put("id", "test-plugin");
        Result result = restService.installPluginFromUrl(body);
        assertFalse(result.isSuccess());
    }

    @Test
    public void testInstallPluginFromUrl_noAdminApp() {
        doReturn(null).when(restService).getApplication();
        Map<String, String> body = new HashMap<>();
        body.put("id", "test-plugin");
        body.put("downloadUrl", "http://example.com/test.zip");
        Result result = restService.installPluginFromUrl(body);
        assertFalse(result.isSuccess());
    }

    // DELETE /rest/v2/plugins/{name}

    @Test
    public void testUndeployPlugin_success() {
        when(adminApp.undeployPlugin("test-plugin")).thenReturn(true);
        Result result = restService.undeployPlugin("test-plugin");
        assertTrue(result.isSuccess());
    }

    @Test
    public void testUndeployPlugin_failure() {
        when(adminApp.undeployPlugin("test-plugin")).thenReturn(false);
        Result result = restService.undeployPlugin("test-plugin");
        assertFalse(result.isSuccess());
    }

    @Test
    public void testUndeployPlugin_invalidName() {
        Result result = restService.undeployPlugin("../bad");
        assertFalse(result.isSuccess());
    }

    @Test
    public void testUndeployPlugin_noAdminApp() {
        doReturn(null).when(restService).getApplication();
        Result result = restService.undeployPlugin("test-plugin");
        assertFalse(result.isSuccess());
    }

    // GET /rest/v2/plugins/{name}/download

    @Test
    public void testDownloadPlugin_noAdminApp() {
        doReturn(null).when(restService).getApplication();
        Response response = restService.downloadPlugin("test-plugin", "token");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDownloadPlugin_noAuthToken() {
        when(adminApp.getClusterCommunicationKey()).thenReturn("secret");
        Response response = restService.downloadPlugin("test-plugin", null);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testDownloadPlugin_invalidToken() {
        when(adminApp.getClusterCommunicationKey()).thenReturn("secret");
        Response response = restService.downloadPlugin("test-plugin", "wrong-token");
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testDownloadPlugin_invalidName() {
        when(adminApp.getClusterCommunicationKey()).thenReturn(null);
        Response response = restService.downloadPlugin("../bad", "token");
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testDownloadPlugin_fileNotFound() throws Exception {
        // Generate a valid JWT
        String secret = "test-secret-key-12345";
        when(adminApp.getClusterCommunicationKey()).thenReturn(secret);
        String validToken = io.antmedia.filter.JWTFilter.generateJwtToken(
                secret, System.currentTimeMillis() + 60000, "pluginname", "test-plugin");

        File pluginsDir = new File(System.getProperty("java.io.tmpdir"), "dl-test-" + System.nanoTime());
        pluginsDir.mkdirs();
        when(adminApp.getPluginsDir()).thenReturn(pluginsDir);

        Response response = restService.downloadPlugin("test-plugin", validToken);
        assertEquals(404, response.getStatus());

        pluginsDir.delete();
    }
}
