package io.antmedia.test.console;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.antmedia.console.AdminApplication;
import io.antmedia.plugin.api.PluginRecord;
import io.antmedia.plugin.api.PluginState;
import io.antmedia.rest.model.Result;
import org.red5.server.plugin.PluginDeployer;

public class AdminApplicationPluginTest {

    private AdminApplication adminApp;
    private PluginDeployer pluginDeployer;

    @Before
    public void before() {
        adminApp = Mockito.spy(new AdminApplication());
        pluginDeployer = Mockito.mock(PluginDeployer.class);
        adminApp.setPluginDeployer(pluginDeployer);

        File pluginsDir = new File(System.getProperty("java.io.tmpdir"), "ams-test-plugins-" + System.nanoTime());
        pluginsDir.mkdirs();
        doReturn(pluginsDir).when(adminApp).getPluginsDir();
    }

    @Test
    public void testIsValidPluginName_valid() {
        assertTrue(AdminApplication.isValidPluginName("clip-creator"));
        assertTrue(AdminApplication.isValidPluginName("my_plugin"));
        assertTrue(AdminApplication.isValidPluginName("plugin123"));
        assertTrue(AdminApplication.isValidPluginName("clip-creator-plugin-3.0.0-SNAPSHOT"));
    }

    @Test
    public void testIsValidPluginName_invalid() {
        assertFalse(AdminApplication.isValidPluginName(null));
        assertFalse(AdminApplication.isValidPluginName(""));
        assertFalse(AdminApplication.isValidPluginName("../etc/passwd"));
        assertFalse(AdminApplication.isValidPluginName("plugin name"));
        assertFalse(AdminApplication.isValidPluginName("plugin/name"));
    }

    @Test
    public void testDeployPlugin_noDeployer() {
        adminApp.setPluginDeployer(null);
        boolean result = adminApp.deployPlugin("test-plugin", new ByteArrayInputStream(new byte[0]));
        assertFalse(result);
    }

    @Test
    public void testDeployPlugin_duplicateBlocked() {
        when(pluginDeployer.getPluginNames()).thenReturn(java.util.Collections.singleton("test-plugin"));
        boolean result = adminApp.deployPlugin("test-plugin", new ByteArrayInputStream(new byte[0]));
        assertFalse(result);
    }

    @Test
    public void testDeployPlugin_success() {
        when(pluginDeployer.getPluginNames()).thenReturn(java.util.Collections.emptySet());
        when(pluginDeployer.loadPluginFromZip(any(File.class), any(File.class)))
                .thenReturn(new Result(true, "ok"));

        // Mock savePluginZip to return a temp file
        File fakeZip = new File(adminApp.getPluginsDir(), "test-plugin.zip");
        doReturn(fakeZip).when(adminApp).savePluginZip(eq("test-plugin"), any(InputStream.class));

        boolean result = adminApp.deployPlugin("test-plugin", new ByteArrayInputStream(new byte[]{1, 2}));
        assertTrue(result);
        verify(pluginDeployer).loadPluginFromZip(eq(fakeZip), any(File.class));
    }

    @Test
    public void testDeployPlugin_loadFails() {
        when(pluginDeployer.getPluginNames()).thenReturn(java.util.Collections.emptySet());
        when(pluginDeployer.loadPluginFromZip(any(File.class), any(File.class)))
                .thenReturn(new Result(false, "manifest error"));

        File fakeZip = new File(adminApp.getPluginsDir(), "test-plugin.zip");
        doReturn(fakeZip).when(adminApp).savePluginZip(eq("test-plugin"), any(InputStream.class));

        boolean result = adminApp.deployPlugin("test-plugin", new ByteArrayInputStream(new byte[]{1}));
        assertFalse(result);
    }

    @Test
    public void testUndeployPlugin_noDeployer() {
        adminApp.setPluginDeployer(null);
        boolean result = adminApp.undeployPlugin("test-plugin");
        assertFalse(result);
    }

    @Test
    public void testUndeployPlugin_success() {
        when(pluginDeployer.unloadPluginFromZip(eq("test-plugin"), any(File.class)))
                .thenReturn(new Result(true, "removed"));

        boolean result = adminApp.undeployPlugin("test-plugin");
        assertTrue(result);
        verify(pluginDeployer).unloadPluginFromZip(eq("test-plugin"), any(File.class));
    }

    @Test
    public void testUndeployPlugin_fails() {
        when(pluginDeployer.unloadPluginFromZip(eq("test-plugin"), any(File.class)))
                .thenReturn(new Result(false, "not found"));

        boolean result = adminApp.undeployPlugin("test-plugin");
        assertFalse(result);
    }

    @Test
    public void testGetAllPluginRecords_empty() {
        when(pluginDeployer.getAllPluginRecords()).thenReturn(java.util.Collections.emptyList());
        java.util.List<PluginRecord> records = adminApp.getAllPluginRecords();
        assertNotNull(records);
        // May have V1 plugins from PluginRegistry — but in test env it's empty
        assertTrue(records.isEmpty());
    }

    @Test
    public void testGetAllPluginRecords_withDeployerRecords() {
        PluginRecord record = new PluginRecord();
        record.setName("Test Plugin");
        record.setState(PluginState.ACTIVE);
        when(pluginDeployer.getAllPluginRecords())
                .thenReturn(java.util.Collections.singletonList(record));

        java.util.List<PluginRecord> records = adminApp.getAllPluginRecords();
        assertFalse(records.isEmpty());
        assertEquals("Test Plugin", records.get(0).getName());
    }

    @Test
    public void testInstallPluginFromUrl_noDeployer() {
        adminApp.setPluginDeployer(null);
        boolean result = adminApp.installPluginFromUrl("test", "http://example.com/test.zip");
        assertFalse(result);
    }

    @Test
    public void testSavePluginZip_nullStream() {
        File result = adminApp.savePluginZip("test-plugin", null);
        assertNull(result);
    }

    @Test
    public void testSavePluginZip_success() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        File result = adminApp.savePluginZip("save-test", new ByteArrayInputStream(data));
        assertNotNull(result);
        assertTrue(result.exists());
        assertEquals(5, result.length());
        result.delete();
    }

    @Test
    public void testBuildPluginDownloadURI() {
        String uri = adminApp.buildPluginDownloadURI("my-plugin");
        assertNotNull(uri);
        assertTrue(uri.contains("my-plugin"));
        assertTrue(uri.contains("/rest/v2/plugins/"));
        assertTrue(uri.contains("/download"));
    }

    @Test
    public void testDeployPlugin_saveReturnsNull() {
        when(pluginDeployer.getPluginNames()).thenReturn(java.util.Collections.emptySet());
        doReturn(null).when(adminApp).savePluginZip(anyString(), any(InputStream.class));

        boolean result = adminApp.deployPlugin("test-plugin", new ByteArrayInputStream(new byte[]{1}));
        assertFalse(result);
    }

    @Test
    public void testUndeployPlugin_deletesZipFile() throws Exception {
        when(pluginDeployer.unloadPluginFromZip(eq("test-plugin"), any(File.class)))
                .thenReturn(new Result(true, "removed"));

        // Create a fake zip so the delete path is exercised
        File pluginsDir = adminApp.getPluginsDir();
        File fakeZip = new File(pluginsDir, "test-plugin.zip");
        fakeZip.createNewFile();
        assertTrue(fakeZip.exists());

        boolean result = adminApp.undeployPlugin("test-plugin");
        assertTrue(result);

        // The zip should be deleted
        assertFalse("ZIP file should be deleted after undeploy", fakeZip.exists());
    }

    @Test
    public void testGetAllPluginNames_combinesRegistryAndDeployer() {
        when(pluginDeployer.getPluginNames()).thenReturn(
                new java.util.HashSet<>(java.util.Arrays.asList("hot-loaded-plugin")));

        java.util.List<String> names = adminApp.getAllPluginNames();
        assertNotNull(names);
        assertTrue(names.contains("hot-loaded-plugin"));
    }

    @Test
    public void testGetAllPluginNames_noDeployer() {
        adminApp.setPluginDeployer(null);
        java.util.List<String> names = adminApp.getAllPluginNames();
        assertNotNull(names);
    }

    @Test
    public void testGetPluginsDir_createsIfNotExists() {
        // Reset spy to use real getPluginsDir
        AdminApplication realApp = new AdminApplication();
        System.setProperty("red5.root", System.getProperty("java.io.tmpdir") + "/ams-dir-test-" + System.nanoTime());
        File dir = realApp.getPluginsDir();
        assertNotNull(dir);
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        dir.delete();
        new File(System.getProperty("red5.root")).delete();
    }

    @Test
    public void testDeployPluginWithURL_invalidName() {
        boolean result = adminApp.deployPluginWithURL("../bad", "http://example.com/test.zip", "key");
        assertFalse(result);
    }

    @Test
    public void testDeployPluginWithURL_downloadFails() throws Exception {
        doReturn(null).when(adminApp).downloadPluginZip(anyString(), anyString(), anyString());
        boolean result = adminApp.deployPluginWithURL("test-plugin", "http://example.com/test.zip", "key");
        assertFalse(result);
    }

    @Test
    public void testDeployPluginWithURL_loadFails() throws Exception {
        File fakeZip = new File(adminApp.getPluginsDir(), "test.zip");
        fakeZip.createNewFile();
        doReturn(fakeZip).when(adminApp).downloadPluginZip(anyString(), anyString(), anyString());
        when(pluginDeployer.loadPluginFromZip(any(File.class), any(File.class)))
                .thenReturn(new Result(false, "bad manifest"));

        boolean result = adminApp.deployPluginWithURL("test-plugin", "http://example.com/test.zip", "key");
        assertFalse(result);
        fakeZip.delete();
    }

    @Test
    public void testDeployPluginWithURL_success() throws Exception {
        File fakeZip = new File(adminApp.getPluginsDir(), "test.zip");
        fakeZip.createNewFile();
        doReturn(fakeZip).when(adminApp).downloadPluginZip(anyString(), anyString(), anyString());
        when(pluginDeployer.loadPluginFromZip(any(File.class), any(File.class)))
                .thenReturn(new Result(true, "ok"));

        boolean result = adminApp.deployPluginWithURL("test-plugin", "http://example.com/test.zip", "key");
        assertTrue(result);
        fakeZip.delete();
    }

    @Test
    public void testDeployPluginWithURL_exceptionHandled() throws Exception {
        doThrow(new RuntimeException("network error")).when(adminApp)
                .downloadPluginZip(anyString(), anyString(), anyString());

        boolean result = adminApp.deployPluginWithURL("test-plugin", "http://example.com/test.zip", "key");
        assertFalse(result);
    }
}
