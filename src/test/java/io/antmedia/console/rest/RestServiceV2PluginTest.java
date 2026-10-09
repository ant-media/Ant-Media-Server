package io.antmedia.console.rest;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import io.antmedia.console.plugin.PluginService;
import io.antmedia.plugin.api.PluginRecord;
import io.antmedia.rest.model.Result;

/**
 * The plugin endpoints must be nothing but a mapping to HTTP — every decision belongs to
 * {@link PluginService}. These tests assert exactly that, so any business logic creeping back
 * into the REST layer shows up as a failure.
 */
public class RestServiceV2PluginTest {

	private RestServiceV2 rest;
	private PluginService pluginService;

	@Before
	public void setUp() {
		pluginService = mock(PluginService.class);

		rest = new RestServiceV2();
		rest.setPluginService(pluginService);
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
}
