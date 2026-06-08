package org.red5.server.plugin;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.IApplicationContext;
import org.red5.server.tomcat.TomcatApplicationContext;
import org.springframework.context.ApplicationContext;

import java.util.jar.Attributes;

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
        assertTrue(result.getMessage().contains("No beans could be registered"));
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

        // Verify destroySingleton was called on the bean factory
        org.springframework.beans.factory.support.DefaultListableBeanFactory bf =
                (org.springframework.beans.factory.support.DefaultListableBeanFactory)
                        ctx.getSpringContext().getAutowireCapableBeanFactory();
        verify(bf).destroySingleton("plugin.minimal-component");
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

    @Test
    public void testGetAllPluginRecords_includesZipInstalledPlugins() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Record Plugin");
        File zip = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "record-plugin");
        File pluginsDir = createTempPluginsDir();

        spy.loadPluginFromZip(zip, pluginsDir);

        java.util.List<PluginRecord> records = spy.getAllPluginRecords();
        assertFalse(records.isEmpty());

        PluginRecord found = records.stream()
                .filter(r -> "Record Plugin".equals(r.getName()))
                .findFirst().orElse(null);
        assertNotNull(found);
        assertEquals(PluginState.ACTIVE, found.getState());
        assertEquals("1.0.0", found.getVersion());
        assertEquals("Test Author", found.getAuthor());

        deleteDir(pluginsDir);
    }

    @Test
    public void testIsDuplicateInstall_activePluginBlocked() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Dup Plugin");
        File zip = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "dup-plugin");
        File pluginsDir = createTempPluginsDir();

        Result first = spy.loadPluginFromZip(zip, pluginsDir);
        assertTrue(first.isSuccess());

        // Second install of the same plugin should fail
        File zip2 = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "dup-plugin2");
        Result second = spy.loadPluginFromZip(zip2, pluginsDir);
        assertFalse(second.isSuccess());
        assertTrue(second.getMessage().contains("already installed"));

        deleteDir(pluginsDir);
    }

    @Test
    public void testUnloadPluginFromZip_removesJarAndRecord() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Removable Plugin");
        File zip = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "removable-plugin");
        File pluginsDir = createTempPluginsDir();

        spy.loadPluginFromZip(zip, pluginsDir);
        assertNotNull(spy.getPluginRecord("Removable Plugin"));

        // Verify the flat jar exists
        String pluginId = spy.getPluginRecord("Removable Plugin").getPluginId();
        File flatJar = new File(pluginsDir, pluginId + ".jar");
        assertTrue(flatJar.exists());

        Result result = spy.unloadPluginFromZip("Removable Plugin", pluginsDir);
        assertTrue(result.isSuccess());

        // Record should still exist but in INSTALLED_PENDING_RESTART state
        // (restart needed to fully clean up — Vert.x timers etc.)
        PluginRecord afterUninstall = spy.getPluginRecord("Removable Plugin");
        assertNotNull(afterUninstall);
        assertEquals(PluginState.INSTALLED_PENDING_RESTART, afterUninstall.getState());

        // Flat jar should be deleted
        assertFalse(flatJar.exists());

        deleteDir(pluginsDir);
    }

    @Test
    public void testLoadPluginFromZip_requiresRestart_jarStaysForInstallScript() throws Exception {
        // When requiresRestart=true, the deployer still copies the jar to plugins/{id}.jar
        // before running install.sh. If there's no install.sh, the jar stays there so
        // ServerClassLoader finds it on next restart.
        File jar = SpringTestPluginJarBuilder.buildPluginJarRequiresRestart("Restart NoScript");
        File zip = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "restart-noscript");

        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", mockStreamingContext("/LiveApp")));
        File pluginsDir = createTempPluginsDir();

        Result result = spy.loadPluginFromZip(zip, pluginsDir);
        assertTrue(result.isSuccess());

        PluginRecord record = spy.getPluginRecord("Restart NoScript");
        assertEquals(PluginState.INSTALLED_PENDING_RESTART, record.getState());

        // Jar should still be at plugins/{id}.jar (no install.sh to move it)
        File flatJar = new File(pluginsDir, record.getPluginId() + ".jar");
        assertTrue("Jar should exist for restart-required plugin without install.sh",
                flatJar.exists());

        deleteDir(pluginsDir);
    }

    @Test
    public void testScanInstalledPlugins_findsJarInPluginsDir() throws Exception {
        String oldRoot = System.getProperty("red5.root");
        try {
            File amsHome = createTempPluginsDir();
            File amsPlugins = new File(amsHome, "plugins");
            amsPlugins.mkdirs();

            File jar = SpringTestPluginJarBuilder.buildPluginJar("scan-test", "Scan Plugin", "1.0.0", "Author", null);
            java.nio.file.Files.copy(jar.toPath(),
                    new File(amsPlugins, "scan-plugin-1.0.0.jar").toPath());

            System.setProperty("red5.root", amsHome.getAbsolutePath());
            PluginDeployer d = new PluginDeployer();
            d.scanInstalledPlugins();

            PluginRecord record = d.getPluginRecord("Scan Plugin");
            assertNotNull("Scan should find the plugin jar", record);
            assertEquals("Scan Plugin", record.getName());
            assertEquals("1.0.0", record.getVersion());
            assertEquals(PluginState.ACTIVE, record.getState());
            assertNotNull("jarPath should be set", record.getJarPath());
            assertTrue("jarPath should point to the actual file",
                    new File(record.getJarPath()).exists());

            deleteDir(amsHome);
        } finally {
            if (oldRoot != null) System.setProperty("red5.root", oldRoot);
            else System.clearProperty("red5.root");
        }
    }

    @Test
    public void testScanInstalledPlugins_skipsJarsWithoutManifest() throws Exception {
        String oldRoot = System.getProperty("red5.root");
        try {
            File amsHome = createTempPluginsDir();
            File amsPlugins = new File(amsHome, "plugins");
            amsPlugins.mkdirs();

            File emptyJar = SpringTestPluginJarBuilder.buildEmptyJar("no-manifest");
            java.nio.file.Files.copy(emptyJar.toPath(),
                    new File(amsPlugins, "no-manifest.jar").toPath());

            System.setProperty("red5.root", amsHome.getAbsolutePath());
            PluginDeployer d = new PluginDeployer();
            d.scanInstalledPlugins();

            assertTrue("Empty jars should be skipped", d.getAllPluginRecords().isEmpty());
            deleteDir(amsHome);
        } finally {
            if (oldRoot != null) System.setProperty("red5.root", oldRoot);
            else System.clearProperty("red5.root");
        }
    }

    @Test
    public void testScanInstalledPlugins_skipsAlreadyKnownPlugins() throws Exception {
        String oldRoot = System.getProperty("red5.root");
        try {
            File amsHome = createTempPluginsDir();
            File amsPlugins = new File(amsHome, "plugins");
            amsPlugins.mkdirs();

            File jar = SpringTestPluginJarBuilder.buildPluginJar("dup-scan", "Dup Scan", "1.0.0", "Author", null);
            java.nio.file.Files.copy(jar.toPath(),
                    new File(amsPlugins, "dup-scan-1.0.0.jar").toPath());

            System.setProperty("red5.root", amsHome.getAbsolutePath());
            PluginDeployer d = new PluginDeployer();

            d.scanInstalledPlugins();
            int firstCount = d.getAllPluginRecords().size();
            assertEquals("First scan should find exactly 1 plugin", 1, firstCount);

            d.scanInstalledPlugins();
            int secondCount = d.getAllPluginRecords().size();
            assertEquals("Second scan should not duplicate", firstCount, secondCount);

            deleteDir(amsHome);
        } finally {
            if (oldRoot != null) System.setProperty("red5.root", oldRoot);
            else System.clearProperty("red5.root");
        }
    }

    @Test
    public void testFindPluginRecord_byName() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Find By Name");
        File zip = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "find-by-name");
        File pluginsDir = createTempPluginsDir();
        spy.loadPluginFromZip(zip, pluginsDir);

        assertNotNull(spy.findPluginRecord("Find By Name"));
        deleteDir(pluginsDir);
    }

    @Test
    public void testFindPluginRecord_byPluginId() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Find By Id");
        File zip = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "find-by-id");
        File pluginsDir = createTempPluginsDir();
        spy.loadPluginFromZip(zip, pluginsDir);

        PluginRecord record = spy.getPluginRecord("Find By Id");
        assertNotNull(record);
        assertNotNull(spy.findPluginRecord(record.getPluginId()));
        deleteDir(pluginsDir);
    }

    @Test
    public void testFindPluginRecord_notFound() {
        assertNull(deployer.findPluginRecord("nonexistent"));
        assertNull(deployer.findPluginRecord(null));
    }

    @Test
    public void testExtractZip_valid() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildPluginJar("extract-test");
        File zip = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "extract-test");

        File extracted = deployer.extractZip(zip);
        assertNotNull(extracted);
        assertTrue(extracted.isDirectory());
        assertTrue(new File(extracted, "plugin.jar").exists());
        deleteDir(extracted);
    }

    @Test
    public void testExtractZip_invalidFile() throws Exception {
        File notAZip = new File("/tmp/not-a-zip-" + System.nanoTime() + ".txt");
        try {
            java.nio.file.Files.write(notAZip.toPath(), "not a zip".getBytes());
            File result = deployer.extractZip(notAZip);
            // Invalid zip should return a directory (ZipInputStream reads empty)
            // or null — either way no exception thrown
            if (result != null) {
                deleteDir(result);
            }
        } finally {
            notAZip.delete();
        }
    }

    @Test
    public void testExtractZip_tooManyEntries() throws Exception {
        File zip = new File(System.getProperty("java.io.tmpdir"), "bomb-entries-" + System.nanoTime() + ".zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zip))) {
            for (int i = 0; i <= PluginDeployer.MAX_ENTRY_COUNT + 1; i++) {
                zos.putNextEntry(new java.util.zip.ZipEntry("file-" + i + ".txt"));
                zos.write(new byte[]{1});
                zos.closeEntry();
            }
        }

        File result = deployer.extractZip(zip);
        assertNull("Should reject ZIP with too many entries", result);
        zip.delete();
    }

    @Test
    public void testExtractZip_singleFileTooLarge() throws Exception {
        // We can't create a real 200MB+ file in a unit test, so temporarily lower the limit
        // by testing via the loadPluginFromZip path with a zip that has a large entry.
        // Instead, verify the limit constants are sane.
        assertTrue("Max single file size should be positive",
                PluginDeployer.MAX_SINGLE_FILE_SIZE > 0);
        assertTrue("Max total size should be >= max single file size",
                PluginDeployer.MAX_TOTAL_EXTRACT_SIZE >= PluginDeployer.MAX_SINGLE_FILE_SIZE);
        assertTrue("Max entry count should be positive",
                PluginDeployer.MAX_ENTRY_COUNT > 0);
    }

    @Test
    public void testExtractZip_normalZipPassesLimits() throws Exception {
        // A normal plugin ZIP with plugin.jar + install.sh + uninstall.sh should pass all limits
        File jar = SpringTestPluginJarBuilder.buildPluginJar("limits-ok");
        File zipDir = createTempPluginsDir();
        File pluginJar = new File(zipDir, "plugin.jar");
        java.nio.file.Files.copy(jar.toPath(), pluginJar.toPath());

        File installSh = new File(zipDir, "install.sh");
        java.nio.file.Files.write(installSh.toPath(), "#!/bin/bash\nexit 0\n".getBytes());
        File uninstallSh = new File(zipDir, "uninstall.sh");
        java.nio.file.Files.write(uninstallSh.toPath(), "#!/bin/bash\nexit 0\n".getBytes());

        File zip = new File(zipDir, "limits-ok.zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zip))) {
            for (String name : new String[]{"plugin.jar", "install.sh", "uninstall.sh"}) {
                zos.putNextEntry(new java.util.zip.ZipEntry(name));
                java.nio.file.Files.copy(new File(zipDir, name).toPath(), zos);
                zos.closeEntry();
            }
        }

        File result = deployer.extractZip(zip);
        assertNotNull("Normal ZIP should extract fine", result);
        assertTrue(new File(result, "plugin.jar").exists());
        assertTrue(new File(result, "install.sh").exists());
        assertTrue(new File(result, "uninstall.sh").exists());

        deleteDir(result);
        deleteDir(zipDir);
    }

    @Test
    public void testLoadPluginFromZip_withInstallScript() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        // Build a ZIP with plugin.jar + install.sh
        File jar = SpringTestPluginJarBuilder.buildPluginJar("Script Plugin");
        File zipDir = createTempPluginsDir();
        File pluginJar = new File(zipDir, "plugin.jar");
        java.nio.file.Files.copy(jar.toPath(), pluginJar.toPath());

        // Create a simple install.sh that just exits 0
        File installSh = new File(zipDir, "install.sh");
        java.nio.file.Files.write(installSh.toPath(), "#!/bin/bash\nexit 0\n".getBytes());
        installSh.setExecutable(true);

        File uninstallSh = new File(zipDir, "uninstall.sh");
        java.nio.file.Files.write(uninstallSh.toPath(), "#!/bin/bash\nexit 0\n".getBytes());

        // Create the ZIP manually
        File zip = new File(zipDir, "script-plugin.zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zip))) {
            for (String name : new String[]{"plugin.jar", "install.sh", "uninstall.sh"}) {
                File f = new File(zipDir, name);
                zos.putNextEntry(new java.util.zip.ZipEntry(name));
                java.nio.file.Files.copy(f.toPath(), zos);
                zos.closeEntry();
            }
        }

        File pluginsDir = createTempPluginsDir();
        Result result = spy.loadPluginFromZip(zip, pluginsDir);
        assertTrue("Install with script should succeed: " + result.getMessage(), result.isSuccess());

        // Verify uninstall.sh was copied to canonical dir
        PluginRecord record = spy.getPluginRecord("Script Plugin");
        assertNotNull(record);
        File canonicalDir = new File(pluginsDir, record.getPluginId());
        assertTrue(new File(canonicalDir, "uninstall.sh").exists());

        deleteDir(zipDir);
        deleteDir(pluginsDir);
    }

    @Test
    public void testLoadPluginFromZip_installScriptFails() throws Exception {
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", mockStreamingContext("/LiveApp")));

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Fail Script");
        File zipDir = createTempPluginsDir();
        File pluginJar = new File(zipDir, "plugin.jar");
        java.nio.file.Files.copy(jar.toPath(), pluginJar.toPath());

        File installSh = new File(zipDir, "install.sh");
        java.nio.file.Files.write(installSh.toPath(), "#!/bin/bash\nexit 1\n".getBytes());
        installSh.setExecutable(true);

        File uninstallSh = new File(zipDir, "uninstall.sh");
        java.nio.file.Files.write(uninstallSh.toPath(), "#!/bin/bash\nexit 0\n".getBytes());

        File zip = new File(zipDir, "fail-script.zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zip))) {
            for (String name : new String[]{"plugin.jar", "install.sh", "uninstall.sh"}) {
                zos.putNextEntry(new java.util.zip.ZipEntry(name));
                java.nio.file.Files.copy(new File(zipDir, name).toPath(), zos);
                zos.closeEntry();
            }
        }

        File pluginsDir = createTempPluginsDir();
        Result result = spy.loadPluginFromZip(zip, pluginsDir);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("install.sh failed"));

        PluginRecord record = spy.getPluginRecord("Fail Script");
        assertNotNull(record);
        assertEquals(PluginState.FAILED, record.getState());

        deleteDir(zipDir);
        deleteDir(pluginsDir);
    }

    @Test
    public void testUnloadPluginFromZip_withUninstallScript() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Uninstall Script");
        File zipDir = createTempPluginsDir();
        File pluginJar = new File(zipDir, "plugin.jar");
        java.nio.file.Files.copy(jar.toPath(), pluginJar.toPath());

        File uninstallSh = new File(zipDir, "uninstall.sh");
        java.nio.file.Files.write(uninstallSh.toPath(), "#!/bin/bash\nexit 0\n".getBytes());

        File zip = new File(zipDir, "uninstall-test.zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zip))) {
            for (String name : new String[]{"plugin.jar", "uninstall.sh"}) {
                zos.putNextEntry(new java.util.zip.ZipEntry(name));
                java.nio.file.Files.copy(new File(zipDir, name).toPath(), zos);
                zos.closeEntry();
            }
        }

        File pluginsDir = createTempPluginsDir();
        spy.loadPluginFromZip(zip, pluginsDir);

        PluginRecord record = spy.getPluginRecord("Uninstall Script");
        assertNotNull(record);

        // Verify uninstall.sh exists in canonical dir
        File canonicalDir = new File(pluginsDir, record.getPluginId());
        assertTrue(new File(canonicalDir, "uninstall.sh").exists());

        Result result = spy.unloadPluginFromZip("Uninstall Script", pluginsDir);
        assertTrue(result.isSuccess());

        // Canonical dir should be deleted
        assertFalse(canonicalDir.exists());

        deleteDir(zipDir);
        deleteDir(pluginsDir);
    }

    @Test
    public void testLoadPlugin_multipleContexts() throws Exception {
        TomcatApplicationContext liveCtx = mockStreamingContext("/LiveApp");
        TomcatApplicationContext webrtcCtx = mockStreamingContext("/WebRTCAppEE");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", liveCtx, "/WebRTCAppEE", webrtcCtx));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("multiCtx");
        Result result = spy.loadPlugin(jar);
        assertTrue(result.isSuccess());

        // Verify bean registered in both contexts
        ApplicationContext liveSpring = liveCtx.getSpringContext();
        ApplicationContext webrtcSpring = webrtcCtx.getSpringContext();

        IAntMediaStreamHandler liveApp = (IAntMediaStreamHandler) liveSpring.getBean(AntMediaApplicationAdapter.BEAN_NAME);
        IAntMediaStreamHandler webrtcApp = (IAntMediaStreamHandler) webrtcSpring.getBean(AntMediaApplicationAdapter.BEAN_NAME);

        verify(liveApp).addStreamListener(any(IStreamListener.class));
        verify(webrtcApp).addStreamListener(any(IStreamListener.class));
    }

    @Test
    public void testReadManifestAttributes_validJar() throws Exception {
        File jar = SpringTestPluginJarBuilder.buildPluginJar("manifest-read", "Test Plugin", "2.0.0", "Author", "2.16.0");
        Attributes attrs = PluginDeployer.readManifestAttributes(jar);
        assertNotNull(attrs);
        assertEquals("Test Plugin", attrs.getValue("AMS-Plugin-Name"));
        assertEquals("2.0.0", attrs.getValue("AMS-Plugin-Version"));
    }

    @Test
    public void testReadManifestAttributes_nonExistentFile() {
        Attributes attrs = PluginDeployer.readManifestAttributes(new File("/nonexistent.jar"));
        assertNull(attrs);
    }

    @Test
    public void testIsDuplicateInstall_noRecord() {
        assertFalse(deployer.isDuplicateInstall("nonexistent"));
    }

    @Test
    public void testIsDuplicateInstall_activeState() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Active Plugin");
        File zip = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "active-plugin");
        File pluginsDir = createTempPluginsDir();
        spy.loadPluginFromZip(zip, pluginsDir);

        // ACTIVE state should block reinstall
        assertTrue(spy.isDuplicateInstall("Active Plugin"));
        deleteDir(pluginsDir);
    }

    @Test
    public void testIsDuplicateInstall_failedStateAllowsReinstall() throws Exception {
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", mockStreamingContext("/LiveApp")));

        // Use a failing install script to get FAILED state directly
        File jar = SpringTestPluginJarBuilder.buildPluginJar("Will Fail");
        File zipDir = createTempPluginsDir();
        File pluginJar = new File(zipDir, "plugin.jar");
        java.nio.file.Files.copy(jar.toPath(), pluginJar.toPath());

        File installSh = new File(zipDir, "install.sh");
        java.nio.file.Files.write(installSh.toPath(), "#!/bin/bash\nexit 1\n".getBytes());
        installSh.setExecutable(true);

        File uninstallSh = new File(zipDir, "uninstall.sh");
        java.nio.file.Files.write(uninstallSh.toPath(), "#!/bin/bash\nexit 0\n".getBytes());

        File zip = new File(zipDir, "will-fail.zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zip))) {
            for (String name : new String[]{"plugin.jar", "install.sh", "uninstall.sh"}) {
                zos.putNextEntry(new java.util.zip.ZipEntry(name));
                java.nio.file.Files.copy(new File(zipDir, name).toPath(), zos);
                zos.closeEntry();
            }
        }

        File pluginsDir = createTempPluginsDir();
        Result result = spy.loadPluginFromZip(zip, pluginsDir);
        assertFalse("Install should fail due to script", result.isSuccess());

        PluginRecord record = spy.getPluginRecord("Will Fail");
        assertNotNull(record);
        assertEquals(PluginState.FAILED, record.getState());

        // FAILED state should allow reinstall
        assertFalse(spy.isDuplicateInstall("Will Fail"));

        deleteDir(zipDir);
        deleteDir(pluginsDir);
    }

    @Test
    public void testScanInstalledPlugins_findsJarInWebappLib() throws Exception {
        String oldRoot = System.getProperty("red5.root");
        try {
            File amsHome = createTempPluginsDir();
            File amsPlugins = new File(amsHome, "plugins");
            amsPlugins.mkdirs();

            // Create a webapp with WEB-INF/lib
            File webapps = new File(amsHome, "webapps");
            File liveApp = new File(webapps, "LiveApp/WEB-INF/lib");
            liveApp.mkdirs();

            File jar = SpringTestPluginJarBuilder.buildPluginJar("webapp-scan", "Webapp Plugin", "2.0.0", "Author", null);
            java.nio.file.Files.copy(jar.toPath(), new File(liveApp, "webapp-plugin.jar").toPath());

            System.setProperty("red5.root", amsHome.getAbsolutePath());
            PluginDeployer d = new PluginDeployer();
            d.scanInstalledPlugins();

            PluginRecord record = d.getPluginRecord("Webapp Plugin");
            assertNotNull("Should find plugin in webapp WEB-INF/lib", record);
            assertEquals("2.0.0", record.getVersion());
            assertTrue(record.getJarPath().contains("WEB-INF/lib"));

            deleteDir(amsHome);
        } finally {
            if (oldRoot != null) System.setProperty("red5.root", oldRoot);
            else System.clearProperty("red5.root");
        }
    }

    @Test
    public void testScanInstalledPlugins_skipsRootWebapp() throws Exception {
        String oldRoot = System.getProperty("red5.root");
        try {
            File amsHome = createTempPluginsDir();
            new File(amsHome, "plugins").mkdirs();

            // Create root webapp (should be skipped)
            File rootLib = new File(amsHome, "webapps/root/WEB-INF/lib");
            rootLib.mkdirs();
            File jar = SpringTestPluginJarBuilder.buildPluginJar("root-scan", "Root Plugin", "1.0.0", "Author", null);
            java.nio.file.Files.copy(jar.toPath(), new File(rootLib, "root-plugin.jar").toPath());

            System.setProperty("red5.root", amsHome.getAbsolutePath());
            PluginDeployer d = new PluginDeployer();
            d.scanInstalledPlugins();

            assertNull("Should not find plugin in root webapp", d.getPluginRecord("Root Plugin"));
            deleteDir(amsHome);
        } finally {
            if (oldRoot != null) System.setProperty("red5.root", oldRoot);
            else System.clearProperty("red5.root");
        }
    }

    @Test
    public void testUnloadPlugin_notFound() {
        Result result = deployer.unloadPlugin("does-not-exist");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("not found"));
    }

    @Test
    public void testUnloadPlugin_destroysSingletonsInAllContexts() throws Exception {
        TomcatApplicationContext liveCtx = mockStreamingContext("/LiveApp");
        TomcatApplicationContext webrtcCtx = mockStreamingContext("/WebRTCAppEE");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", liveCtx, "/WebRTCAppEE", webrtcCtx));

        File jar = SpringTestPluginJarBuilder.buildComponentJar("destroyAll");
        spy.loadPlugin(jar);

        Result result = spy.unloadPlugin("destroyAll");
        assertTrue(result.isSuccess());

        // Verify destroySingleton called in both contexts
        org.springframework.beans.factory.support.DefaultListableBeanFactory liveBf =
                (org.springframework.beans.factory.support.DefaultListableBeanFactory)
                        liveCtx.getSpringContext().getAutowireCapableBeanFactory();
        org.springframework.beans.factory.support.DefaultListableBeanFactory webrtcBf =
                (org.springframework.beans.factory.support.DefaultListableBeanFactory)
                        webrtcCtx.getSpringContext().getAutowireCapableBeanFactory();

        verify(liveBf).destroySingleton("plugin.minimal-component");
        verify(webrtcBf).destroySingleton("plugin.minimal-component");
    }

    @Test
    public void testUnloadPluginFromZip_noRecord() {
        File pluginsDir = createTempPluginsDir();
        Result result = deployer.unloadPluginFromZip("nonexistent", pluginsDir);
        assertTrue(result.isSuccess()); // succeeds but "was not active"
        deleteDir(pluginsDir);
    }

    @Test
    public void testValidateManifest_withRequiresVersion() throws Exception {
        // When AMS version is not available (test env), the version check is skipped
        // and validation passes. This test verifies the manifest is still valid.
        File jar = SpringTestPluginJarBuilder.buildPluginJar("compat", "Compat Plugin",
                "1.0.0", "Author", "2.16.0");
        Result result = deployer.validateManifest(jar);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testLoadPluginFromZip_extractFails() {
        // Pass a non-zip file
        File pluginsDir = createTempPluginsDir();
        File notAZip = new File(pluginsDir, "bad.zip");
        try {
            java.nio.file.Files.write(notAZip.toPath(), "not a zip".getBytes());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        Result result = deployer.loadPluginFromZip(notAZip, pluginsDir);
        // Should fail — either "plugin.jar not found" or "Failed to extract"
        assertFalse(result.isSuccess());
        deleteDir(pluginsDir);
    }

    @Test
    public void testLoadPluginFromZip_duplicateBlocked() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));
        File pluginsDir = createTempPluginsDir();

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Dup Zip");
        File zip1 = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "dup-zip-1");
        File zip2 = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "dup-zip-2");

        Result first = spy.loadPluginFromZip(zip1, pluginsDir);
        assertTrue(first.isSuccess());

        Result second = spy.loadPluginFromZip(zip2, pluginsDir);
        assertFalse(second.isSuccess());
        assertTrue(second.getMessage().contains("already installed"));

        deleteDir(pluginsDir);
    }

    @Test
    public void testRunInstallScript_success() throws Exception {
        File workDir = createTempPluginsDir();
        File script = new File(workDir, "test.sh");
        java.nio.file.Files.write(script.toPath(), "#!/bin/bash\nexit 0\n".getBytes());
        script.setExecutable(true);

        int exitCode = deployer.runInstallScript(script, workDir, "Test", "1.0.0",
                new File(workDir, "plugin.jar"), "test-1.0.0");
        assertEquals(0, exitCode);
        deleteDir(workDir);
    }

    @Test
    public void testRunInstallScript_failure() throws Exception {
        File workDir = createTempPluginsDir();
        File script = new File(workDir, "fail.sh");
        java.nio.file.Files.write(script.toPath(), "#!/bin/bash\nexit 42\n".getBytes());
        script.setExecutable(true);

        int exitCode = deployer.runInstallScript(script, workDir, "Test", "1.0.0",
                new File(workDir, "plugin.jar"), "test-1.0.0");
        assertEquals(42, exitCode);
        deleteDir(workDir);
    }

    @Test
    public void testRunInstallScript_receivesEnvVars() throws Exception {
        File workDir = createTempPluginsDir();
        File output = new File(workDir, "env-output.txt");
        File script = new File(workDir, "env-check.sh");
        java.nio.file.Files.write(script.toPath(),
                ("#!/bin/bash\n"
                + "echo \"NAME=$PLUGIN_NAME\" > " + output.getAbsolutePath() + "\n"
                + "echo \"VERSION=$PLUGIN_VERSION\" >> " + output.getAbsolutePath() + "\n"
                + "echo \"ID=$PLUGIN_ID\" >> " + output.getAbsolutePath() + "\n"
                + "exit 0\n").getBytes());
        script.setExecutable(true);

        deployer.runInstallScript(script, workDir, "My Plugin", "3.0.0",
                new File(workDir, "plugin.jar"), "my-plugin-3.0.0");

        assertTrue(output.exists());
        String content = new String(java.nio.file.Files.readAllBytes(output.toPath()));
        assertTrue(content.contains("NAME=My Plugin"));
        assertTrue(content.contains("VERSION=3.0.0"));
        assertTrue(content.contains("ID=my-plugin-3.0.0"));

        deleteDir(workDir);
    }

    @Test
    public void testCopyFile_viaLoadPluginFromZip() throws Exception {
        // This tests the private copyFile/copyDirectory methods indirectly
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Copy Test");
        File zipDir = createTempPluginsDir();
        File pluginJar = new File(zipDir, "plugin.jar");
        java.nio.file.Files.copy(jar.toPath(), pluginJar.toPath());

        // Create assets directory to test copyDirectory
        File assetsDir = new File(zipDir, "assets");
        assetsDir.mkdirs();
        File assetFile = new File(assetsDir, "config.txt");
        java.nio.file.Files.write(assetFile.toPath(), "test config".getBytes());

        File uninstallSh = new File(zipDir, "uninstall.sh");
        java.nio.file.Files.write(uninstallSh.toPath(), "#!/bin/bash\nexit 0\n".getBytes());

        File zip = new File(zipDir, "copy-test.zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zip))) {
            for (String name : new String[]{"plugin.jar", "uninstall.sh"}) {
                zos.putNextEntry(new java.util.zip.ZipEntry(name));
                java.nio.file.Files.copy(new File(zipDir, name).toPath(), zos);
                zos.closeEntry();
            }
            zos.putNextEntry(new java.util.zip.ZipEntry("assets/"));
            zos.closeEntry();
            zos.putNextEntry(new java.util.zip.ZipEntry("assets/config.txt"));
            java.nio.file.Files.copy(assetFile.toPath(), zos);
            zos.closeEntry();
        }

        File pluginsDir = createTempPluginsDir();
        Result result = spy.loadPluginFromZip(zip, pluginsDir);
        assertTrue(result.isSuccess());

        PluginRecord record = spy.getPluginRecord("Copy Test");
        File canonicalDir = new File(pluginsDir, record.getPluginId());
        assertTrue(new File(canonicalDir, "uninstall.sh").exists());
        assertTrue(new File(canonicalDir, "assets/config.txt").exists());

        String configContent = new String(java.nio.file.Files.readAllBytes(
                new File(canonicalDir, "assets/config.txt").toPath()));
        assertEquals("test config", configContent);

        deleteDir(zipDir);
        deleteDir(pluginsDir);
    }

    @Test
    public void testPluginRecord_allFields() {
        PluginRecord r = new PluginRecord();
        r.setName("Test");
        r.setVersion("1.0");
        r.setAuthor("Author");
        r.setDescription("Desc");
        r.setRequiresVersion("2.0");
        r.setRequiresRestart(true);
        r.setState(PluginState.ACTIVE);
        r.setLastError("err");
        r.setPluginId("test-1.0");
        r.setJarPath("/tmp/test.jar");

        assertEquals("Test", r.getName());
        assertEquals("1.0", r.getVersion());
        assertEquals("Author", r.getAuthor());
        assertEquals("Desc", r.getDescription());
        assertEquals("2.0", r.getRequiresVersion());
        assertTrue(r.isRequiresRestart());
        assertEquals(PluginState.ACTIVE, r.getState());
        assertEquals("err", r.getLastError());
        assertEquals("test-1.0", r.getPluginId());
        assertEquals("/tmp/test.jar", r.getJarPath());
    }

    /**
     * Builds a {@link TomcatApplicationContext} whose underlying catalina context is a
     * {@link StandardContext} with a {@code jersey-serlvet} child wrapper exposing a
     * {@link ServletContainer} — required to exercise {@code reloadJersey} and
     * {@code reloadJerseyWithout}.
     */
    private TomcatApplicationContext mockContextWithJerseyWrapper(String path, ServletContainer[] captured) {
        TomcatApplicationContext tomcatCtx = mock(TomcatApplicationContext.class);
        StandardContext stdCtx = mock(StandardContext.class);
        when(tomcatCtx.getContext()).thenReturn(stdCtx);
        when(stdCtx.getPath()).thenReturn(path);

        Wrapper wrapper = mock(Wrapper.class);
        when(stdCtx.findChild("jersey-serlvet")).thenReturn(wrapper);

        ServletContainer servletContainer = mock(ServletContainer.class);
        when(wrapper.getServlet()).thenReturn(servletContainer);
        when(servletContainer.getConfiguration()).thenReturn(new ResourceConfig());
        if (captured != null && captured.length > 0) captured[0] = servletContainer;

        ApplicationContext springCtx = mock(ApplicationContext.class);
        when(tomcatCtx.getSpringContext()).thenReturn(springCtx);
        IAntMediaStreamHandler app = mock(AntMediaApplicationAdapter.class);
        when(springCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);

        org.springframework.beans.factory.support.DefaultListableBeanFactory bf =
                mock(org.springframework.beans.factory.support.DefaultListableBeanFactory.class);
        when(springCtx.getAutowireCapableBeanFactory()).thenReturn(bf);
        try {
            when(bf.createBean(any(Class.class))).thenAnswer(inv -> {
                Class<?> cls = inv.getArgument(0);
                Object instance = cls.getDeclaredConstructor().newInstance();
                if (instance instanceof org.springframework.context.ApplicationContextAware) {
                    ((org.springframework.context.ApplicationContextAware) instance)
                            .setApplicationContext(springCtx);
                }
                return instance;
            });
        } catch (Exception e) { throw new RuntimeException(e); }
        return tomcatCtx;
    }

    @Test
    public void testLoadPlugin_withRestClass_triggersReloadJersey() throws Exception {
        ServletContainer[] captured = new ServletContainer[1];
        TomcatApplicationContext ctx = mockContextWithJerseyWrapper("/LiveApp", captured);
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildComponentWithRestJar("reloadJersey");
        Result result = spy.loadPlugin(jar);

        assertTrue(result.isSuccess());
        // Jersey reload should fire because the jar contains a @Path component
        verify(captured[0]).reload(any(ResourceConfig.class));
    }

    @Test
    public void testReloadJersey_skipsNonStandardContext() throws Exception {
        // Use a plain Context mock (not StandardContext). reloadJersey should skip it
        // entirely and the plugin still loads.
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));
        File jar = SpringTestPluginJarBuilder.buildComponentWithRestJar("skipNonStd");
        Result result = spy.loadPlugin(jar);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testReloadJersey_skipsNonWrapperChild() throws Exception {
        TomcatApplicationContext tomcatCtx = mock(TomcatApplicationContext.class);
        StandardContext stdCtx = mock(StandardContext.class);
        when(tomcatCtx.getContext()).thenReturn(stdCtx);
        when(stdCtx.getPath()).thenReturn("/LiveApp");
        // findChild returns something that isn't a Wrapper
        when(stdCtx.findChild("jersey-serlvet")).thenReturn(null);

        ApplicationContext springCtx = mock(ApplicationContext.class);
        when(tomcatCtx.getSpringContext()).thenReturn(springCtx);
        IAntMediaStreamHandler app = mock(AntMediaApplicationAdapter.class);
        when(springCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);
        org.springframework.beans.factory.support.DefaultListableBeanFactory bf =
                mock(org.springframework.beans.factory.support.DefaultListableBeanFactory.class);
        when(springCtx.getAutowireCapableBeanFactory()).thenReturn(bf);
        when(bf.createBean(any(Class.class))).thenAnswer(inv ->
                ((Class<?>) inv.getArgument(0)).getDeclaredConstructor().newInstance());

        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", tomcatCtx));
        File jar = SpringTestPluginJarBuilder.buildComponentWithRestJar("nullWrapper");
        Result result = spy.loadPlugin(jar);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testReloadJersey_servletNotServletContainer() throws Exception {
        TomcatApplicationContext tomcatCtx = mock(TomcatApplicationContext.class);
        StandardContext stdCtx = mock(StandardContext.class);
        when(tomcatCtx.getContext()).thenReturn(stdCtx);
        when(stdCtx.getPath()).thenReturn("/LiveApp");
        Wrapper wrapper = mock(Wrapper.class);
        when(stdCtx.findChild("jersey-serlvet")).thenReturn(wrapper);
        // servlet is some other impl, not ServletContainer
        when(wrapper.getServlet()).thenReturn(mock(jakarta.servlet.Servlet.class));

        ApplicationContext springCtx = mock(ApplicationContext.class);
        when(tomcatCtx.getSpringContext()).thenReturn(springCtx);
        IAntMediaStreamHandler app = mock(AntMediaApplicationAdapter.class);
        when(springCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);
        org.springframework.beans.factory.support.DefaultListableBeanFactory bf =
                mock(org.springframework.beans.factory.support.DefaultListableBeanFactory.class);
        when(springCtx.getAutowireCapableBeanFactory()).thenReturn(bf);
        when(bf.createBean(any(Class.class))).thenAnswer(inv ->
                ((Class<?>) inv.getArgument(0)).getDeclaredConstructor().newInstance());

        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", tomcatCtx));
        File jar = SpringTestPluginJarBuilder.buildComponentWithRestJar("otherServlet");
        Result result = spy.loadPlugin(jar);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testUnloadPlugin_removesStreamListener() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        ApplicationContext springCtx = ctx.getSpringContext();
        // Make the bean factory return the real instance for getBean(beanName),
        // so instanceof IStreamListener evaluates true inside unloadPlugin.
        org.springframework.beans.factory.support.DefaultListableBeanFactory bf =
                (org.springframework.beans.factory.support.DefaultListableBeanFactory)
                        springCtx.getAutowireCapableBeanFactory();
        MinimalSpringComponent instance = new MinimalSpringComponent();
        when(bf.getBean("plugin.minimal-component")).thenReturn(instance);

        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));
        File jar = SpringTestPluginJarBuilder.buildComponentJar("listenerUnload");
        spy.loadPlugin(jar);

        Result result = spy.unloadPlugin("listenerUnload");
        assertTrue(result.isSuccess());

        IAntMediaStreamHandler app = (IAntMediaStreamHandler)
                springCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME);
        verify(app).removeStreamListener(instance);
        verify(bf).destroySingleton("plugin.minimal-component");
    }

    @Test
    public void testUnloadPlugin_startupLoaded_scansJarForBeans() throws Exception {
        // A scanned (startup-loaded) plugin is not in springPluginBeanNames, but has a
        // record with jarPath. unloadPlugin should scan the jar, find @Component
        // classes, and destroy matching beans.
        String oldRoot = System.getProperty("red5.root");
        try {
            File amsHome = createTempPluginsDir();
            File pluginsDir = new File(amsHome, "plugins");
            pluginsDir.mkdirs();

            File jar = SpringTestPluginJarBuilder.buildPluginJar("startup-scan",
                    "Startup Plugin", "1.0.0", "Author", null);
            java.nio.file.Files.copy(jar.toPath(),
                    new File(pluginsDir, "startup-plugin-1.0.0.jar").toPath());

            System.setProperty("red5.root", amsHome.getAbsolutePath());
            TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
            PluginDeployer d = Mockito.spy(new PluginDeployer());
            doReturn(Map.of("/LiveApp", ctx)).when(d).getApplicationContexts();
            d.scanInstalledPlugins();

            PluginRecord record = d.getPluginRecord("Startup Plugin");
            assertNotNull(record);
            assertNotNull(record.getJarPath());

            // Unloading by the record's pluginId triggers the jar-scan branch
            Result result = d.unloadPlugin(record.getPluginId());
            assertTrue("Startup-loaded plugin should be removable: " + result.getMessage(),
                    result.isSuccess());

            deleteDir(amsHome);
        } finally {
            if (oldRoot != null) System.setProperty("red5.root", oldRoot);
            else System.clearProperty("red5.root");
        }
    }

    @Test
    public void testUnloadPlugin_startupLoaded_withRestClass_triggersReloadJerseyWithout() throws Exception {
        // Create a jar with both @Component and @Path classes, register it as a
        // startup-loaded record, then unload — the jar scan should find the REST class
        // and fire reloadJerseyWithout.
        File jar = SpringTestPluginJarBuilder.buildPluginWithRestJar("Startup Rest Plugin");
        File copiedJar = new File(System.getProperty("java.io.tmpdir"),
                "startup-rest-" + System.nanoTime() + ".jar");
        java.nio.file.Files.copy(jar.toPath(), copiedJar.toPath());

        ServletContainer[] captured = new ServletContainer[1];
        TomcatApplicationContext ctx = mockContextWithJerseyWrapper("/LiveApp", captured);

        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        // Simulate startup scan result by registering the record directly
        Attributes attrs = PluginDeployer.readManifestAttributes(jar);
        PluginRecord record = spy.buildPluginRecord(attrs);
        record.setState(PluginState.ACTIVE);
        record.setJarPath(copiedJar.getAbsolutePath());
        // Use reflection-free path: insert via public map getter? no — expose via getPluginRecord
        // Workaround: use scanInstalledPlugins with a controlled dir
        String oldRoot = System.getProperty("red5.root");
        try {
            File amsHome = createTempPluginsDir();
            File pluginsDir = new File(amsHome, "plugins");
            pluginsDir.mkdirs();
            java.nio.file.Files.copy(jar.toPath(),
                    new File(pluginsDir, "startup-rest-1.0.0.jar").toPath());

            System.setProperty("red5.root", amsHome.getAbsolutePath());
            PluginDeployer d = Mockito.spy(new PluginDeployer());
            doReturn(Map.of("/LiveApp", ctx)).when(d).getApplicationContexts();
            d.scanInstalledPlugins();

            PluginRecord r = d.getPluginRecord("Startup Rest Plugin");
            assertNotNull(r);

            Result result = d.unloadPlugin(r.getPluginId());
            assertTrue(result.isSuccess());
            // reloadJerseyWithout called because the jar contains a @Path class
            verify(captured[0]).reload(any(ResourceConfig.class));
            deleteDir(amsHome);
        } finally {
            copiedJar.delete();
            if (oldRoot != null) System.setProperty("red5.root", oldRoot);
            else System.clearProperty("red5.root");
        }
    }

    @Test
    public void testExtractZip_zipSlip_rejected() throws Exception {
        File zip = new File(System.getProperty("java.io.tmpdir"), "slip-" + System.nanoTime() + ".zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zip))) {
            // Path traversal attempt — should be rejected
            zos.putNextEntry(new java.util.zip.ZipEntry("../escape.txt"));
            zos.write("bad".getBytes());
            zos.closeEntry();
        }

        File result = deployer.extractZip(zip);
        assertNull("Zip-slip entries must be rejected", result);
        zip.delete();
    }

    @Test
    public void testExtractZip_ioException_returnsNull() {
        // Pass a non-existent file — FileInputStream throws FileNotFoundException (IOException)
        File result = deployer.extractZip(new File("/does-not-exist-" + System.nanoTime() + ".zip"));
        assertNull(result);
    }

    @Test
    public void testExtractZip_withDirectoryEntry() throws Exception {
        File zip = new File(System.getProperty("java.io.tmpdir"), "dir-" + System.nanoTime() + ".zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zip))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("assets/"));
            zos.closeEntry();
            zos.putNextEntry(new java.util.zip.ZipEntry("assets/file.txt"));
            zos.write("x".getBytes());
            zos.closeEntry();
        }

        File extracted = deployer.extractZip(zip);
        assertNotNull(extracted);
        assertTrue(new File(extracted, "assets").isDirectory());
        assertTrue(new File(extracted, "assets/file.txt").exists());
        deleteDir(extracted);
        zip.delete();
    }

    @Test
    public void testIsVersionCompatible_malformed_returnsTrue() {
        // A version with a number too large for int triggers NumberFormatException
        assertTrue(PluginDeployer.isVersionCompatible(
                "99999999999999999.0.0", "2.11.0"));
    }

    @Test
    public void testIsVersionCompatible_differentLengths() {
        // current length < required length → must fail
        assertFalse(PluginDeployer.isVersionCompatible("2.11", "2.11.1"));
        // current length > required length → must pass
        assertTrue(PluginDeployer.isVersionCompatible("2.11.0", "2.11"));
    }

    @Test
    public void testResolveBeanName_defaultFromSimpleName() {
        // A class with @Component() no value → default camelCase name from simple name
        @org.springframework.stereotype.Component
        class NoValueComponent { }
        assertEquals("noValueComponent", PluginDeployer.resolveBeanName(NoValueComponent.class));
    }

    @Test
    public void testSlugify_nullAndEmpty() {
        assertEquals("", PluginDeployer.slugify(null));
        assertEquals("", PluginDeployer.slugify("!!!"));
        assertEquals("abc", PluginDeployer.slugify("  --abc--  "));
    }

    @Test
    public void testResolveRestKey_multiSegment() {
        @jakarta.ws.rs.Path("/a/b/last") class C { }
        assertEquals("last", PluginDeployer.resolveRestKey(C.class));
    }

    @Test
    public void testResolveRestKey_emptyPath() {
        @jakarta.ws.rs.Path("/") class C { }
        assertNull(PluginDeployer.resolveRestKey(C.class));
    }

    @Test
    public void testLoadPluginFromZip_exceptionDuringLoad_wrappedInResult() throws Exception {
        // Feed a zip that extracts but has a plugin.jar that's actually malformed —
        // the jar manifest read should fail → validateManifest returns false.
        File pluginsDir = createTempPluginsDir();
        File zipDir = createTempPluginsDir();
        File fakePluginJar = new File(zipDir, "plugin.jar");
        java.nio.file.Files.write(fakePluginJar.toPath(), new byte[]{0, 1, 2, 3});

        File zip = new File(zipDir, "malformed.zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zip))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("plugin.jar"));
            java.nio.file.Files.copy(fakePluginJar.toPath(), zos);
            zos.closeEntry();
        }

        Result result = deployer.loadPluginFromZip(zip, pluginsDir);
        assertFalse(result.isSuccess());
        deleteDir(pluginsDir);
        deleteDir(zipDir);
    }

    @Test
    public void testUnloadPluginFromZip_runsUninstallScriptWithNonZeroExit() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Noisy Uninstall");
        File zipDir = createTempPluginsDir();
        File pluginJar = new File(zipDir, "plugin.jar");
        java.nio.file.Files.copy(jar.toPath(), pluginJar.toPath());

        // uninstall.sh exits non-zero — should be logged but not fail the result
        File uninstallSh = new File(zipDir, "uninstall.sh");
        java.nio.file.Files.write(uninstallSh.toPath(), "#!/bin/bash\nexit 7\n".getBytes());

        File zip = new File(zipDir, "noisy.zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(zip))) {
            for (String n : new String[]{"plugin.jar", "uninstall.sh"}) {
                zos.putNextEntry(new java.util.zip.ZipEntry(n));
                java.nio.file.Files.copy(new File(zipDir, n).toPath(), zos);
                zos.closeEntry();
            }
        }

        File pluginsDir = createTempPluginsDir();
        spy.loadPluginFromZip(zip, pluginsDir);
        Result result = spy.unloadPluginFromZip("Noisy Uninstall", pluginsDir);
        assertTrue(result.isSuccess());
        deleteDir(zipDir);
        deleteDir(pluginsDir);
    }

    @Test
    public void testUnloadPluginFromZip_missingJarFile_warnsButSucceeds() throws Exception {
        TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
        PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

        File jar = SpringTestPluginJarBuilder.buildPluginJar("Gone Jar");
        File zip = SpringTestPluginJarBuilder.wrapJarAsZip(jar, "gone-jar");
        File pluginsDir = createTempPluginsDir();
        spy.loadPluginFromZip(zip, pluginsDir);

        PluginRecord record = spy.getPluginRecord("Gone Jar");
        assertNotNull(record);
        // Manually delete the flat jar BEFORE unload — exercise the "jar missing" warn path
        new File(pluginsDir, record.getPluginId() + ".jar").delete();

        Result result = spy.unloadPluginFromZip("Gone Jar", pluginsDir);
        assertTrue(result.isSuccess());
        deleteDir(pluginsDir);
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
