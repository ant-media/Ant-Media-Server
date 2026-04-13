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
