package io.antmedia.console.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import io.antmedia.console.AdminApplication;
import io.antmedia.console.plugin.PluginService;
import io.antmedia.filter.JWTFilter;
import io.antmedia.plugin.api.PluginRecord;
import io.antmedia.rest.model.Result;
import jakarta.ws.rs.core.Response;

/**
 * The plugin endpoints must be nothing but a mapping to HTTP — every decision belongs to
 * {@link PluginService}. These tests assert exactly that, so any business logic creeping back
 * into the REST layer shows up as a failure.
 */
public class RestServiceV2PluginTest {

	/** Long enough for HS256, which rejects short keys outright. */
	private static final String CLUSTER_SECRET = "cluster-communication-key-for-unit-tests-0123456789";

	private RestServiceV2 rest;
	private PluginService pluginService;
	private AdminApplication adminApp;

	@Before
	public void setUp() {
		pluginService = mock(PluginService.class);
		adminApp = mock(AdminApplication.class);

		rest = new RestServiceV2();
		rest.setPluginService(pluginService);
		rest.setApplication(adminApp);
	}

	@Test
	public void testGetPlugins_delegates() {
		List<PluginRecord> records = List.of(new PluginRecord());
		when(pluginService.list()).thenReturn(records);

		assertSame(records, rest.getPlugins());
	}

	@Test
	public void testDeployPlugin_delegates() {
		InputStream zip = new ByteArrayInputStream(new byte[]{1});
		Result expected = new Result(true, "installed");
		when(pluginService.install("clip-creator", zip)).thenReturn(expected);

		assertSame(expected, rest.deployPlugin("clip-creator", zip));
	}

	@Test
	public void testUndeployPlugin_delegates() {
		Result expected = new Result(true, "removed");
		when(pluginService.uninstall("clip-creator")).thenReturn(expected);

		assertSame(expected, rest.undeployPlugin("clip-creator"));
	}

	@Test
	public void testInstallFromUrl_passesEveryFieldOfTheBody() {
		Result expected = new Result(true, "installed");
		when(pluginService.installFromUrl("clip-creator", "http://registry/p.zip", "abc123"))
				.thenReturn(expected);

		Result result = rest.installPluginFromUrl(
				Map.of("id", "clip-creator", "downloadUrl", "http://registry/p.zip", "sha256", "abc123"));

		assertSame(expected, result);
	}

	/** A body with no checksum is legal — the field is optional. */
	@Test
	public void testInstallFromUrl_missingChecksumIsPassedAsNull() {
		rest.installPluginFromUrl(Map.of("id", "clip-creator", "downloadUrl", "http://registry/p.zip"));

		verify(pluginService).installFromUrl("clip-creator", "http://registry/p.zip", null);
	}

	/** An entirely absent body must not NPE — it has to reach the service as nulls and be rejected there. */
	@Test
	public void testInstallFromUrl_nullBody() {
		rest.installPluginFromUrl(null);

		verify(pluginService).installFromUrl(isNull(), isNull(), isNull());
	}

	// --- cluster download endpoint ---

	@Test
	public void testDownloadPlugin_forbiddenWhenNoClusterSecret() {
		when(adminApp.getClusterCommunicationKey()).thenReturn(null);

		assertEquals(Response.Status.FORBIDDEN.getStatusCode(),
				rest.downloadPlugin("clip-creator", "any-token").getStatus());
	}

	@Test
	public void testDownloadPlugin_forbiddenWhenTokenMissing() {
		when(adminApp.getClusterCommunicationKey()).thenReturn(CLUSTER_SECRET);

		assertEquals(Response.Status.FORBIDDEN.getStatusCode(),
				rest.downloadPlugin("clip-creator", null).getStatus());
	}

	@Test
	public void testDownloadPlugin_forbiddenWhenTokenInvalid() {
		when(adminApp.getClusterCommunicationKey()).thenReturn(CLUSTER_SECRET);

		assertEquals(Response.Status.FORBIDDEN.getStatusCode(),
				rest.downloadPlugin("clip-creator", "not-a-jwt").getStatus());
	}

	@Test
	public void testDownloadPlugin_notFoundWhenNoZip() {
		when(adminApp.getClusterCommunicationKey()).thenReturn(CLUSTER_SECRET);
		when(pluginService.resolveZipForDownload("clip-creator")).thenReturn(null);

		assertEquals(Response.Status.NOT_FOUND.getStatusCode(),
				rest.downloadPlugin("clip-creator", validToken()).getStatus());
	}

	/** The ZIP existed when it was resolved but is gone by the time it is streamed. */
	@Test
	public void testDownloadPlugin_notFoundWhenZipDisappears() {
		when(adminApp.getClusterCommunicationKey()).thenReturn(CLUSTER_SECRET);
		when(pluginService.resolveZipForDownload("clip-creator"))
				.thenReturn(new File("/tmp/definitely-not-here-" + System.nanoTime() + ".zip"));

		assertEquals(Response.Status.NOT_FOUND.getStatusCode(),
				rest.downloadPlugin("clip-creator", validToken()).getStatus());
	}

	@Test
	public void testDownloadPlugin_streamsTheZip() throws Exception {
		File zip = Files.createTempFile("ams-plugin-download", ".zip").toFile();
		Files.write(zip.toPath(), new byte[]{1, 2, 3});

		when(adminApp.getClusterCommunicationKey()).thenReturn(CLUSTER_SECRET);
		when(pluginService.resolveZipForDownload("clip-creator")).thenReturn(zip);

		Response response = rest.downloadPlugin("clip-creator", validToken());

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertEquals(3L, Long.parseLong(response.getHeaderString("Content-Length")));
		assertTrue(response.getHeaderString("Content-Disposition").contains("clip-creator.zip"));

		Files.deleteIfExists(zip.toPath());
	}

	private static String validToken() {
		return JWTFilter.generateJwtToken(CLUSTER_SECRET,
				System.currentTimeMillis() + 60000, "pluginname", "clip-creator");
	}
}
