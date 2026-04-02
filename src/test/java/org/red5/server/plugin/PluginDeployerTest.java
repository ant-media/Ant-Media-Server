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
import io.vertx.core.Vertx;

public class PluginDeployerTest {

	private PluginDeployer deployer;
	private static Vertx vertx;

	@Before
	public void before() {
		deployer = new PluginDeployer();
		vertx = Vertx.vertx();
	}

	/** Spy that bypasses system-CL check and JAR-adding; provides mock webapp contexts. */
	private PluginDeployer deployerSpy(Map<String, IApplicationContext> contexts) {
		PluginDeployer spy = Mockito.spy(deployer);
		doReturn(true).when(spy).isSystemClassLoaderServerClassLoader();
		doNothing().when(spy).addJarToSystemClassLoader(any());
		doReturn(contexts).when(spy).getApplicationContexts();
		return spy;
	}

	/** Creates a mock streaming webapp context with beans for DI injection. */
	private TomcatApplicationContext mockStreamingContext(String path) {
		TomcatApplicationContext tomcatCtx = mock(TomcatApplicationContext.class);
		Context catalinaCtx = mock(Context.class);
		when(tomcatCtx.getContext()).thenReturn(catalinaCtx);
		when(catalinaCtx.getPath()).thenReturn(path);

		ApplicationContext springCtx = mock(ApplicationContext.class);
		when(tomcatCtx.getSpringContext()).thenReturn(springCtx);
		lenient().when(springCtx.getId()).thenReturn(path);

		// Provide beans for @Inject resolution
		IAntMediaStreamHandler app = mock(AntMediaApplicationAdapter.class);
		when(springCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);
		when(springCtx.getBean(AppSettings.BEAN_NAME)).thenReturn(mock(AppSettings.class));
		when(springCtx.getBean(ServerSettings.BEAN_NAME)).thenReturn(mock(ServerSettings.class));
		when(springCtx.getBean("vertxCore")).thenReturn(vertx);

		return tomcatCtx;
	}
	
	@Test
	public void testLoadPlugin_systemCLNotServerCL_fails() throws Exception {
		File jar = TestPluginJarBuilder.buildPluginJar("sclCheck");
		Result result = deployer.loadPlugin(jar);
		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("ServerClassLoader"));
	}

	@Test
	public void testLoadPlugin_noAmsPluginAnnotation_fails() throws Exception {
		File jar = TestPluginJarBuilder.buildJarNoAmsPlugin();
		PluginDeployer spy = deployerSpy(Collections.emptyMap());
		Result result = spy.loadPlugin(jar);
		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("No @AmsPlugin"));
	}

	@Test
	public void testLoadPlugin_success() throws Exception {
		TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
		PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

		File jar = TestPluginJarBuilder.buildPluginJar("loadOk");
		Result result = spy.loadPlugin(jar);

		assertTrue(result.isSuccess());
		assertTrue(spy.getPluginNames().contains("loadOk"));

		// Verify stream listener was registered
		ApplicationContext springCtx = ctx.getSpringContext();
		IAntMediaStreamHandler app = (IAntMediaStreamHandler) springCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME);
		verify(app).addStreamListener(any(IStreamListener.class));
	}

	@Test
	public void testLoadPlugin_withRest_registersRestKey() throws Exception {
		TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
		PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

		File jar = TestPluginJarBuilder.buildPluginWithRestJar("restV2");
		Result result = spy.loadPlugin(jar);

		assertTrue(result.isSuccess());
		assertTrue(spy.getPluginRestKeys().containsKey("restV2"));
		assertTrue(spy.getPluginRestKeys().get("restV2").contains("test-v2-plugin"));
	}

	@Test
	public void testLoadPlugin_skipsRootContext() throws Exception {
		TomcatApplicationContext rootCtx = mockStreamingContext("");
		TomcatApplicationContext liveCtx = mockStreamingContext("/LiveApp");
		PluginDeployer spy = deployerSpy(Map.of("", rootCtx, "/LiveApp", liveCtx));

		File jar = TestPluginJarBuilder.buildPluginJar("skipRoot");
		Result result = spy.loadPlugin(jar);

		assertTrue(result.isSuccess());
		// Root context app should not have addStreamListener called
		ApplicationContext rootSpring = rootCtx.getSpringContext();
		verify(rootSpring, never()).getBean(AntMediaApplicationAdapter.BEAN_NAME);
	}

	@Test
	public void testLoadPlugin_multipleContexts() throws Exception {
		TomcatApplicationContext ctx1 = mockStreamingContext("/LiveApp");
		TomcatApplicationContext ctx2 = mockStreamingContext("/WebRTCAppEE");
		PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx1, "/WebRTCAppEE", ctx2));

		File jar = TestPluginJarBuilder.buildPluginJar("multiCtx");
		Result result = spy.loadPlugin(jar);

		assertTrue(result.isSuccess());
		// Both contexts should have listeners registered
		IAntMediaStreamHandler app1 = (IAntMediaStreamHandler) ctx1.getSpringContext().getBean(AntMediaApplicationAdapter.BEAN_NAME);
		IAntMediaStreamHandler app2 = (IAntMediaStreamHandler) ctx2.getSpringContext().getBean(AntMediaApplicationAdapter.BEAN_NAME);
		verify(app1).addStreamListener(any(IStreamListener.class));
		verify(app2).addStreamListener(any(IStreamListener.class));
	}

	@Test
	public void testLoadPlugin_injectsFields() throws Exception {
		TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
		PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

		File jar = TestPluginJarBuilder.buildPluginJar("injectTest");
		Result result = spy.loadPlugin(jar);

		assertTrue(result.isSuccess());
	}

	// ========================================================================
	// unloadPlugin
	// ========================================================================

	@Test
	public void testUnloadPlugin_notFound() {
		Result result = deployer.unloadPlugin("doesNotExist");
		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("not found"));
	}

	@Test
	public void testUnloadPlugin_success() throws Exception {
		TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
		PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

		File jar = TestPluginJarBuilder.buildPluginJar("unloadMe");
		spy.loadPlugin(jar);
		assertTrue(spy.getPluginNames().contains("unloadMe"));

		Result result = spy.unloadPlugin("unloadMe");

		assertTrue(result.isSuccess());
		assertFalse(spy.getPluginNames().contains("unloadMe"));
	}

	@Test
	public void testUnloadPlugin_removesStreamListeners() throws Exception {
		TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
		PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

		File jar = TestPluginJarBuilder.buildPluginJar("unloadListeners");
		spy.loadPlugin(jar);
		spy.unloadPlugin("unloadListeners");

		IAntMediaStreamHandler app = (IAntMediaStreamHandler) ctx.getSpringContext().getBean(AntMediaApplicationAdapter.BEAN_NAME);
		verify(app).removeStreamListener(any(IStreamListener.class));
	}

	@Test
	public void testUnloadPlugin_removesRestKeys() throws Exception {
		TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
		PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

		File jar = TestPluginJarBuilder.buildPluginWithRestJar("restUnload");
		spy.loadPlugin(jar);
		assertNotNull(spy.getPluginRestKeys().get("restUnload"));

		spy.unloadPlugin("restUnload");
		assertNull(spy.getPluginRestKeys().get("restUnload"));
	}

	// ========================================================================
	// Manifest validation
	// ========================================================================

	@Test
	public void testValidateManifest_missingName() throws Exception {
		File jar = TestPluginJarBuilder.buildJarMissingName();
		Result result = deployer.validateManifest(jar);
		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("AMS-Plugin-Name"));
	}

	@Test
	public void testValidateManifest_missingVersion() throws Exception {
		File jar = TestPluginJarBuilder.buildJarMissingVersion();
		Result result = deployer.validateManifest(jar);
		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("AMS-Plugin-Version"));
	}

	@Test
	public void testValidateManifest_missingAuthor() throws Exception {
		File jar = TestPluginJarBuilder.buildJarMissingAuthor();
		Result result = deployer.validateManifest(jar);
		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("AMS-Plugin-Author"));
	}

	@Test
	public void testValidateManifest_valid() throws Exception {
		File jar = TestPluginJarBuilder.buildPluginJar("validManifest");
		Result result = deployer.validateManifest(jar);
		assertTrue(result.isSuccess());
	}

	// ========================================================================
	// Version compatibility
	// ========================================================================

	@Test
	public void testVersionCompatible_sameVersion() {
		assertTrue(PluginDeployer.isVersionCompatible("2.11.0", "2.11.0"));
	}

	@Test
	public void testVersionCompatible_newerAms() {
		assertTrue(PluginDeployer.isVersionCompatible("2.12.0", "2.11.0"));
	}

	@Test
	public void testVersionCompatible_olderAms() {
		assertFalse(PluginDeployer.isVersionCompatible("2.10.0", "2.11.0"));
	}

	@Test
	public void testVersionCompatible_snapshotVersion() {
		assertTrue(PluginDeployer.isVersionCompatible("3.0.0-SNAPSHOT", "2.11.0"));
	}

	@Test
	public void testVersionCompatible_nulls() {
		assertTrue(PluginDeployer.isVersionCompatible(null, "2.11.0"));
		assertTrue(PluginDeployer.isVersionCompatible("2.11.0", null));
	}

	// ========================================================================
	// ZIP extraction and loadPluginFromZip
	// ========================================================================

	@Test
	public void testExtractZip_validZip() throws Exception {
		File jar = TestPluginJarBuilder.buildPluginJar("zipExtract");
		File zip = TestPluginJarBuilder.wrapJarAsZip(jar, "zipExtract");
		File extractDir = deployer.extractZip(zip);

		assertNotNull(extractDir);
		assertTrue(new File(extractDir, "plugin.jar").exists());

		// cleanup
		deleteDir(extractDir);
	}

	@Test
	public void testLoadPluginFromZip_noPluginJar_fails() throws Exception {
		File zip = TestPluginJarBuilder.buildZipNoPluginJar();
		File pluginsDir = createTempPluginsDir();

		Result result = deployer.loadPluginFromZip(zip, pluginsDir);

		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("plugin.jar not found"));

		deleteDir(pluginsDir);
	}

	@Test
	public void testLoadPluginFromZip_success() throws Exception {
		TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
		PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));

		File jar = TestPluginJarBuilder.buildPluginJar("zipSuccess");
		File zip = TestPluginJarBuilder.wrapJarAsZip(jar, "zipSuccess");
		File pluginsDir = createTempPluginsDir();

		Result result = spy.loadPluginFromZip(zip, pluginsDir);

		assertTrue(result.isSuccess());
		PluginRecord record = spy.getPluginRecord("zipSuccess");
		assertNotNull(record);
		assertEquals(PluginState.ACTIVE, record.getState());

		deleteDir(pluginsDir);
	}

	@Test
	public void testLoadPluginFromZip_manifestValidationFails() throws Exception {
		File jar = TestPluginJarBuilder.buildJarMissingName();
		File zip = TestPluginJarBuilder.wrapJarAsZip(jar, "badManifest");
		File pluginsDir = createTempPluginsDir();

		Result result = deployer.loadPluginFromZip(zip, pluginsDir);

		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("AMS-Plugin-Name"));

		deleteDir(pluginsDir);
	}

	@Test
	public void testLoadPluginFromZip_duplicate() throws Exception {
		TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
		PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));
		File pluginsDir = createTempPluginsDir();

		File jar = TestPluginJarBuilder.buildPluginJar("dupZip");
		File zip = TestPluginJarBuilder.wrapJarAsZip(jar, "dupZip");

		spy.loadPluginFromZip(zip, pluginsDir);
		Result second = spy.loadPluginFromZip(zip, pluginsDir);

		assertFalse(second.isSuccess());
		assertTrue(second.getMessage().contains("already loaded"));

		deleteDir(pluginsDir);
	}

	@Test
	public void testLoadPluginFromZip_installScriptSuccess() throws Exception {
		TomcatApplicationContext ctx = mockStreamingContext("/LiveApp");
		PluginDeployer spy = deployerSpy(Map.of("/LiveApp", ctx));
		File pluginsDir = createTempPluginsDir();

		File zip = TestPluginJarBuilder.buildZipWithInstallScript("scriptOk", "#!/bin/bash\nexit 0\n");
		Result result = spy.loadPluginFromZip(zip, pluginsDir);

		assertTrue(result.isSuccess());

		deleteDir(pluginsDir);
	}

	@Test
	public void testLoadPluginFromZip_installScriptFails() throws Exception {
		File pluginsDir = createTempPluginsDir();

		File zip = TestPluginJarBuilder.buildZipWithInstallScript("scriptFail", "#!/bin/bash\nexit 1\n");
		Result result = deployer.loadPluginFromZip(zip, pluginsDir);

		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("install.sh failed"));

		PluginRecord record = deployer.getPluginRecord("scriptFail");
		assertNotNull(record);
		assertEquals(PluginState.FAILED, record.getState());

		deleteDir(pluginsDir);
	}

	// ========================================================================
	// Uninstall script
	// ========================================================================

	@Test
	public void testRunUninstallScript_noRecord() {
		File pluginsDir = createTempPluginsDir();
		// Should not throw
		deployer.runUninstallScript("nonexistent", pluginsDir);
		deleteDir(pluginsDir);
	}

	// ========================================================================
	// buildPluginRecord
	// ========================================================================

	@Test
	public void testBuildPluginRecord() throws Exception {
		File jar = TestPluginJarBuilder.buildPluginJar("recordTest", "My Plugin", "2.0.0", "Author", "2.11.0");
		java.util.jar.Attributes attrs = PluginDeployer.readManifestAttributes(jar);

		PluginRecord record = deployer.buildPluginRecord(attrs);

		assertEquals("My Plugin", record.getName());
		assertEquals("2.0.0", record.getVersion());
		assertEquals("Author", record.getAuthor());
		assertEquals("2.11.0", record.getRequiresVersion());
		assertEquals("HOTLOAD", record.getLoadingMode());
		assertFalse(record.isRequiresRestart());
		assertEquals("my-plugin-2.0.0", record.getPluginId());
	}

	// ========================================================================
	// Slugify
	// ========================================================================

	@Test
	public void testSlugify() {
		assertEquals("clip-creator-plugin", PluginDeployer.slugify("Clip Creator Plugin"));
		assertEquals("my-plugin", PluginDeployer.slugify("My Plugin"));
		assertEquals("simple", PluginDeployer.slugify("simple"));
	}

	// ========================================================================
	// Plugin name listing
	// ========================================================================

	@Test
	public void testGetPluginNames_emptyInitially() {
		assertTrue(deployer.getPluginNames().isEmpty());
	}

	@Test
	public void testGetAllPluginRecords_emptyInitially() {
		assertTrue(deployer.getAllPluginRecords().isEmpty());
	}

	// ========================================================================
	// Helpers
	// ========================================================================

	private File createTempPluginsDir() {
		File dir = new File(System.getProperty("java.io.tmpdir"), "ams-test-plugins-" + System.nanoTime());
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
