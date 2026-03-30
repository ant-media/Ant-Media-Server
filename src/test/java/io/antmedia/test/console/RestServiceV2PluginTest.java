package io.antmedia.test.console;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.plugin.PluginRegistry;

import java.io.File;
import java.nio.file.Files;

import io.antmedia.console.AdminApplication;
import io.antmedia.console.rest.RestServiceV2;
import io.antmedia.filter.JWTFilter;
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

    @After
    public void after() throws Exception {
        PluginRegistry.shutdown();
    }

    @Test
    public void testGetPlugins_empty() {
        when(adminApp.getAllPluginNames()).thenReturn(List.of());

        List<String> plugins = restService.getPlugins();

        assertTrue(plugins.isEmpty());
    }

    @Test
    public void testGetPlugins_withPlugins() throws Exception {
        when(adminApp.getAllPluginNames()).thenReturn(List.of("pluginA", "pluginB"));

        List<String> plugins = restService.getPlugins();

        assertEquals(2, plugins.size());
        assertTrue(plugins.contains("pluginA"));
        assertTrue(plugins.contains("pluginB"));
    }

    @Test
    public void testDeployPlugin_success() {
        InputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(adminApp.deployPlugin(eq("myPlugin"), any())).thenReturn(true);

        Result result = restService.deployPlugin("myPlugin", stream);

        assertTrue(result.isSuccess());
    }

    @Test
    public void testDeployPlugin_adminReturnsFailure() {
        InputStream stream = new ByteArrayInputStream(new byte[]{1});
        when(adminApp.deployPlugin(eq("dupPlugin"), any())).thenReturn(false);

        Result result = restService.deployPlugin("dupPlugin", stream);

        assertFalse(result.isSuccess());
    }

    @Test
    public void testDeployPlugin_nullInputStream() {
        Result result = restService.deployPlugin("somePlugin", null);

        assertFalse(result.isSuccess());
        verify(adminApp, never()).deployPlugin(anyString(), any());
    }

    @Test
    public void testDeployPlugin_blankPluginName() {
        InputStream stream = new ByteArrayInputStream(new byte[]{1});

        Result result = restService.deployPlugin("", stream);

        assertFalse(result.isSuccess());
        verify(adminApp, never()).deployPlugin(anyString(), any());
    }

    @Test
    public void testDeployPlugin_nullPluginName() {
        InputStream stream = new ByteArrayInputStream(new byte[]{1});

        Result result = restService.deployPlugin(null, stream);

        assertFalse(result.isSuccess());
        verify(adminApp, never()).deployPlugin(anyString(), any());
    }

    @Test
    public void testUndeployPlugin_success() {
        when(adminApp.undeployPlugin("myPlugin")).thenReturn(true);

        Result result = restService.undeployPlugin("myPlugin");

        assertTrue(result.isSuccess());
    }

    @Test
    public void testUndeployPlugin_notFound() {
        when(adminApp.undeployPlugin("ghost")).thenReturn(false);

        Result result = restService.undeployPlugin("ghost");

        assertFalse(result.isSuccess());
    }

    @Test
    public void testUndeployPlugin_blankName() {
        Result result = restService.undeployPlugin("  ");

        assertFalse(result.isSuccess());
        verify(adminApp, never()).undeployPlugin(anyString());
    }

    // --- isValidPluginName coverage ---

    @Test
    public void testDeployPlugin_pathTraversalName() {
        InputStream stream = new ByteArrayInputStream(new byte[]{1});

        Result result = restService.deployPlugin("../evil", stream);

        assertFalse(result.isSuccess());
        verify(adminApp, never()).deployPlugin(anyString(), any());
    }

    @Test
    public void testDeployPlugin_validName_accepted() {
        InputStream stream = new ByteArrayInputStream(new byte[]{1});
        when(adminApp.deployPlugin(eq("my-plugin_123"), any())).thenReturn(true);

        Result result = restService.deployPlugin("my-plugin_123", stream);

        assertTrue(result.isSuccess());
    }

    @Test
    public void testUndeployPlugin_pathTraversalName() {
        Result result = restService.undeployPlugin("../../etc/passwd");

        assertFalse(result.isSuccess());
        verify(adminApp, never()).undeployPlugin(anyString());
    }

    @Test
    public void testDownloadPlugin_pathTraversalName_rejectedAfterValidJwt() throws Exception {
        String secret = "dl-secret";
        when(adminApp.getClusterCommunicationKey()).thenReturn(secret);
        // Generate a valid JWT for the malicious name — server must still reject it
        String token = JWTFilter.generateJwtToken(
                secret, System.currentTimeMillis() + 60_000, "pluginname", "../evil");

        Response response = restService.downloadPlugin("../evil", token);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDownloadPlugin_invalidToken() throws Exception {
        when(adminApp.getClusterCommunicationKey()).thenReturn("real-secret");

        Response response = restService.downloadPlugin("somePlugin", "bad-token");

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDownloadPlugin_nullToken() throws Exception {
        when(adminApp.getClusterCommunicationKey()).thenReturn("real-secret");

        Response response = restService.downloadPlugin("somePlugin", null); // must not NPE

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDownloadPlugin_validToken_fileExists() throws Exception {
        File tempRoot = Files.createTempDirectory("red5root-dl").toFile();
        tempRoot.deleteOnExit();
        System.setProperty("red5.root", tempRoot.getAbsolutePath());
        File pluginsDir = new File(tempRoot, "plugins");
        pluginsDir.mkdirs();
        File jar = new File(pluginsDir, "myPlugin.jar");
        Files.write(jar.toPath(), new byte[]{1, 2, 3, 4, 5});

        String secret = "download-secret";
        when(adminApp.getClusterCommunicationKey()).thenReturn(secret);

        String validToken = JWTFilter.generateJwtToken(
                secret, System.currentTimeMillis() + 60_000, "pluginname", "myPlugin");

        Response response = restService.downloadPlugin("myPlugin", validToken);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(5L, response.getLength());
    }

    @Test
    public void testDownloadPlugin_validToken_fileMissing() throws Exception {
        File tempRoot = Files.createTempDirectory("red5root-dl2").toFile();
        tempRoot.deleteOnExit();
        System.setProperty("red5.root", tempRoot.getAbsolutePath());
        String secret = "download-secret2";
        when(adminApp.getClusterCommunicationKey()).thenReturn(secret);

        String validToken = JWTFilter.generateJwtToken(
                secret, System.currentTimeMillis() + 60_000, "pluginname", "ghostPlugin");

        Response response = restService.downloadPlugin("ghostPlugin", validToken);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
