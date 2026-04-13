package io.antmedia.test.console;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.antmedia.console.AdminApplication;
import io.antmedia.console.rest.CommonRestService;
import io.antmedia.plugin.api.PluginRecord;
import io.antmedia.plugin.api.PluginState;
import io.antmedia.rest.model.Result;

public class RestServiceV2PluginTest {

    private CommonRestService restService;
    private AdminApplication adminApp;

    @Before
    public void before() {
        restService = Mockito.spy(new CommonRestService());
        adminApp = Mockito.mock(AdminApplication.class);
        doReturn(adminApp).when(restService).getApplication();
    }

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

    @Test
    public void testDeployPlugin_invalidName() {
        Result result = restService.deployPlugin("../bad-name", new ByteArrayInputStream(new byte[0]));
        assertFalse(result.isSuccess());
    }

    @Test
    public void testDeployPlugin_nullStream() {
        Result result = restService.deployPlugin("valid-name", null);
        assertFalse(result.isSuccess());
    }

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
    public void testDeployPlugin_noAdminApp() {
        doReturn(null).when(restService).getApplication();
        Result result = restService.deployPlugin("test-plugin", new ByteArrayInputStream(new byte[]{1}));
        assertFalse(result.isSuccess());
    }

    @Test
    public void testUndeployPlugin_invalidName() {
        Result result = restService.undeployPlugin("../bad");
        assertFalse(result.isSuccess());
    }

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
    public void testUndeployPlugin_noAdminApp() {
        doReturn(null).when(restService).getApplication();
        Result result = restService.undeployPlugin("test-plugin");
        assertFalse(result.isSuccess());
    }

    @Test
    public void testInstallPluginFromUrl_invalidId() {
        Result result = restService.installPluginFromUrl("../bad", "http://example.com/test.zip");
        assertFalse(result.isSuccess());
    }

    @Test
    public void testInstallPluginFromUrl_nullUrl() {
        Result result = restService.installPluginFromUrl("test-plugin", null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void testInstallPluginFromUrl_emptyUrl() {
        Result result = restService.installPluginFromUrl("test-plugin", "");
        assertFalse(result.isSuccess());
    }

    @Test
    public void testInstallPluginFromUrl_success() {
        when(adminApp.installPluginFromUrl("test-plugin", "http://example.com/test.zip")).thenReturn(true);
        Result result = restService.installPluginFromUrl("test-plugin", "http://example.com/test.zip");
        assertTrue(result.isSuccess());
    }

    @Test
    public void testInstallPluginFromUrl_failure() {
        when(adminApp.installPluginFromUrl("test-plugin", "http://example.com/test.zip")).thenReturn(false);
        Result result = restService.installPluginFromUrl("test-plugin", "http://example.com/test.zip");
        assertFalse(result.isSuccess());
    }

    @Test
    public void testInstallPluginFromUrl_noAdminApp() {
        doReturn(null).when(restService).getApplication();
        Result result = restService.installPluginFromUrl("test-plugin", "http://example.com/test.zip");
        assertFalse(result.isSuccess());
    }
}
