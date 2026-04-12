package org.red5.server.plugin;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.apache.catalina.Context;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.IApplicationContext;
import org.red5.server.tomcat.TomcatApplicationContext;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.plugin.api.IStreamListener;
import io.antmedia.plugin.api.PluginRecord;
import io.antmedia.plugin.api.PluginState;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.antmedia.test.plugin.MinimalSpringComponent;
import io.antmedia.test.plugin.MinimalSpringRestComponent;
import io.vertx.core.Vertx;

public class PluginDeployerTest {

    private PluginDeployer deployer;
    private static Vertx vertx;

    @Before
    public void before() {
        deployer = new PluginDeployer();
        vertx = Vertx.vertx();
    }

    private PluginDeployer deployerSpy(Map<String, IApplicationContext> contexts) {
        PluginDeployer spy = Mockito.spy(deployer);
        doReturn(true).when(spy).isSystemClassLoaderServerClassLoader();
        doNothing().when(spy).addJarToSystemClassLoader(any());
        doReturn(contexts).when(spy).getApplicationContexts();
        return spy;
    }

    private TomcatApplicationContext mockStreamingContext(String path) {
        TomcatApplicationContext tomcatCtx = mock(TomcatApplicationContext.class);
        Context catalinaCtx = mock(Context.class);
        when(tomcatCtx.getContext()).thenReturn(catalinaCtx);
        when(catalinaCtx.getPath()).thenReturn(path);

        ApplicationContext springCtx = mock(ApplicationContext.class);
        when(tomcatCtx.getSpringContext()).thenReturn(springCtx);

        IAntMediaStreamHandler app = mock(AntMediaApplicationAdapter.class);
        when(springCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);
        when(springCtx.getBean(AppSettings.BEAN_NAME)).thenReturn(mock(AppSettings.class));
        when(springCtx.getBean(ServerSettings.BEAN_NAME)).thenReturn(mock(ServerSettings.class));
        when(springCtx.getBean("vertxCore")).thenReturn(vertx);

        // AutowireCapableBeanFactory mock for createBean/registerSingleton
        org.springframework.beans.factory.support.DefaultListableBeanFactory beanFactory =
                mock(org.springframework.beans.factory.support.DefaultListableBeanFactory.class);
        when(springCtx.getAutowireCapableBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.createBean(any(Class.class))).thenAnswer(inv -> {
            Class<?> cls = inv.getArgument(0);
            Object instance = cls.getDeclaredConstructor().newInstance();
            if (instance instanceof org.springframework.context.ApplicationContextAware) {
                ((org.springframework.context.ApplicationContextAware) instance)
                        .setApplicationContext(springCtx);
            }
            return instance;
        });

        return tomcatCtx;
    }

    // loadPlugin — Spring @Component scanning

    @Test
    public void testLoadPlugin_systemCLNotServerCL_fails() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildComponentJar("sclCheck");
        Result result = deployer.loadPlugin(jar);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("ServerClassLoader"));
    }

    @Test
    public void testLoadPlugin_emptyJar_fails() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildEmptyJar("emptyJar");
        PluginDeployer spy = deployerSpy(Collections.emptyMap());
        Result result = spy.loadPlugin(jar);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("No @Component"));
    }

    @Test
    public void testLoadPlugin_noActiveContexts_fails() throws Exception {
        PluginDeployer spy = deployerSpy(Collections.emptyMap());
        File jar = SpringTestPluginJarBuilder.buildComponentJar("noCtx");
        Result result = spy.loadPlugin(jar);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("none of its beans"));
    }

    @Test
    public void testLoadPlugin_success() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("loadOk");
        Result result = spy.loadPlugin(jar);

        assertTrue(result.isSuccess());
        assertTrue(spy.getPluginNames().contains("loadOk"));

        // The @Component class calls app.addStreamListener(this) in setApplicationContext
        ApplicationContext springCtx = ctx.getSpringContext();
        IAntMediaStreamHandler app = (IAntMediaStreamHandler) springCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME);
        verify(app).addStreamListener(any(IStreamListener.class));
    }

    @Test
    public void testLoadPlugin_skipsRootContext() throws Exception {
        TomcatApplicationContext rootCtx = mockStreamingContext("");
        TomcatApplicationContext liveCtx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("", rootCtx, "/LiveApp", liveCtx));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("skipRoot");
        Result result = spy.loadPlugin(jar);
        assertTrue(result.isSuccess());

        // Root context's app should NOT have been touched
        IAntMediaStreamHandler rootApp = (IAntMediaStreamHandler)
                rootCtx.getSpringContext().getBean(AntMediaApplicationAdapter.BEAN_NAME);
        verify(rootApp, never()).addStreamListener(any());
    }

    @Test
    public void testLoadPlugin_duplicateRejected() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("dup");
        spy.loadPlugin(jar);
        Result second = spy.loadPlugin(jar);

        assertFalse(second.isSuccess());
        assertTrue(second.getMessage().contains("already loaded"));
    }

    @Test
    public void testLoadPlugin_withRestPath_success() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildComponentWithRestJar("restKey");
        Result result = spy.loadPlugin(jar);

        assertTrue(result.isSuccess());
        assertTrue(spy.getPluginNames().contains("restKey"));
    }

    @Test
    public void testUnloadPlugin_unknown_fails() {
        Result result = deployer.unloadPlugin("nonexistent");
        assertFalse(result.isSuccess());
    }

    @Test
    public void testUnloadPlugin_success() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("toUnload");
        spy.loadPlugin(jar);
        assertTrue(spy.getPluginNames().contains("toUnload"));

        Result result = spy.unloadPlugin("toUnload");
        assertTrue(result.isSuccess());
        assertFalse(spy.getPluginNames().contains("toUnload"));
    }


    @Test
    public void testValidateManifest_missingName() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildJarMissingName();
        Result result = deployer.validateManifest(jar);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("AMS-Plugin-Name"));
    }

    @Test
    public void testValidateManifest_missingVersion() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildJarMissingVersion();
        Result result = deployer.validateManifest(jar);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("AMS-Plugin-Version"));
    }

    @Test
    public void testValidateManifest_missingAuthor() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildJarMissingAuthor();
        Result result = deployer.validateManifest(jar);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("AMS-Plugin-Author"));
    }

    @Test
    public void testValidateManifest_valid() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildPluginJar("validManifest");
        Result result = deployer.validateManifest(jar);
        assertTrue(result.isSuccess());
    }


    @Test
    public void testBuildPluginRecord() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildPluginJar("recordTest", "My Plugin", "2.0.0",
                "Author", "2.11.0");
        java.util.jar.Attributes attrs = PluginDeployer.readManifestAttributes(jar);
        PluginRecord record = deployer.buildPluginRecord(attrs);

        assertEquals("My Plugin", record.getName());
        assertEquals("2.0.0", record.getVersion());
        assertEquals("Author", record.getAuthor());
        assertEquals("2.11.0", record.getRequiresVersion());
        assertEquals("my-plugin-2.0.0", record.getPluginId());
        assertFalse(record.isRequiresRestart());
    }

    @Test
    public void testBuildPluginRecord_requiresRestart() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildPluginJarRequiresRestart("restartTest");
        java.util.jar.Attributes attrs = PluginDeployer.readManifestAttributes(jar);
        PluginRecord record = deployer.buildPluginRecord(attrs);
        assertTrue(record.isRequiresRestart());
    }


    @Test
    public void testLoadPluginFromZip_noPluginJar_fails() throws Exception {
        File zip = SpringTestPluginJarBuilder.buildZipNoPluginJar();
        File pluginsDir = createTempPluginsDir();
        Result result = deployer.loadPluginFromZip(zip, pluginsDir);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("plugin.jar not found"));
        deleteDir(pluginsDir);
    }

    @Test
    public void testLoadPluginFromZip_requiresRestart_entersPendingState() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildPluginJarRequiresRestart("Restart Plugin");
        File zip = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "restart-plugin");

        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", mockStreamingContext("/LiveApp")));
        File pluginsDir = createTempPluginsDir();
        Result result = spy.loadPluginFromZip(zip, pluginsDir);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("restart required"));

        PluginRecord record = spy.getPluginRecord("Restart Plugin");
        assertNotNull(record);
        assertEquals(PluginState.INSTALLED_PENDING_RESTART, record.getState());
        deleteDir(pluginsDir);
    }

    @Test
    public void testLoadPluginFromZip_hotLoad_success() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Hot Plugin");
        File zip = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "hot-plugin");
        File pluginsDir = createTempPluginsDir();

        Result result = spy.loadPluginFromZip(zip, pluginsDir);
        assertTrue(result.isSuccess());

        PluginRecord record = spy.getPluginRecord("Hot Plugin");
        assertNotNull(record);
        assertEquals(PluginState.ACTIVE, record.getState());

        // Verify jar was copied to flat location in plugins dir (so ServerClassLoader
        // picks it up on the next AMS restart via its plugins/*.jar scan)
        File flatJar = new File(pluginsDir, record.getPluginId() + ".jar");
        assertTrue("Flat jar should exist at " + flatJar.getAbsolutePath(), flatJar.exists());

        deleteDir(pluginsDir);
    }


    @Test
    public void testVersionCompatible_sameVersion() {
        assertTrue(PluginDeployer.isVersionCompatible("2.11.0", "2.11.0"));
    }

    @Test
    public void testVersionCompatible_newerMajor() {
        assertTrue(PluginDeployer.isVersionCompatible("3.0.0", "2.11.0"));
    }

    @Test
    public void testVersionCompatible_olderVersion_fails() {
        assertFalse(PluginDeployer.isVersionCompatible("2.10.0", "2.11.0"));
    }

    @Test
    public void testVersionCompatible_nulls_pass() {
        assertTrue(PluginDeployer.isVersionCompatible(null, "2.11.0"));
        assertTrue(PluginDeployer.isVersionCompatible("2.11.0", null));
    }


    @Test
    public void testResolveBeanName_componentValue() {
        assertEquals("plugin.minimal-component",
                PluginDeployer.resolveBeanName(MinimalSpringComponent.class));
    }

    @Test
    public void testResolveRestKey_pathAnnotation() {
        assertEquals("test-plugin",
                PluginDeployer.resolveRestKey(MinimalSpringRestComponent.class));
    }

    @Test
    public void testResolveRestKey_noPath_returnsNull() {
        assertNull(PluginDeployer.resolveRestKey(MinimalSpringComponent.class));
    }


    @Test
    public void testSlugify() {
        assertEquals("clip-creator-plugin", PluginDeployer.slugify("Clip Creator Plugin"));
        assertEquals("my-plugin", PluginDeployer.slugify("My Plugin"));
    }

    // ---- helpers ----

    private File createTempPluginsDir() {
        File dir = new File(System.getProperty("java.io.tmpdir"),
                "ams-test-plugins-" + System.nanoTime());
        dir.mkdirs();
        return dir;
    }

    private void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}
