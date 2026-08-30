package io.antmedia.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.antmedia.datastore.db.types.User;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.plugin.api.PluginRecord;
import io.antmedia.plugin.api.PluginState;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;

/**
 * Drives the plugin endpoints against a running server, installing the real Clip Creator
 * plugin from the public catalog. Assumes the server is already up, like the other tests here.
 */
@TestMethodOrder(MethodName.class)
public class PluginServiceIntegrationTests {

	private static final String SERVER_ADDR = ServerSettings.getLocalHostAddress();
	private static final String ROOT_SERVICE_URL = "http://" + SERVER_ADDR + ":5080/rest/v2";

	private static final String PLUGIN_ID = "clip-creator";
	private static final String PLUGIN_ZIP_URL =
			"https://antmedia-plugins.s3.eu-west-2.amazonaws.com/ClipCreatorPlugin/clip-creator.zip";

	private static final String TEST_USER_EMAIL = "test@antmedia.io";
	private static final String TEST_USER_PASS = "05a671c66aefea124cc08b76ea6d30bb";

	private static final Gson gson = new Gson();
	private static final Logger log = LoggerFactory.getLogger(PluginServiceIntegrationTests.class);

	private BasicCookieStore cookieStore;

	@BeforeEach
	public void before() throws Exception {
		cookieStore = new BasicCookieStore();
		login();
		uninstallQuietly();
	}

	@AfterEach
	public void after() throws Exception {
		uninstallQuietly();
	}

	/** Install from the catalog URL, prove the server really loaded it, then remove it. */
	@Test
	public void testInstallFromUrlThenUninstall() throws Exception {
		assertNull(findInstalled(PLUGIN_ID), "plugin should not be installed before the test");

		Result install = installFromUrl(PLUGIN_ID, PLUGIN_ZIP_URL);
		assertTrue(install.isSuccess(), "install failed: " + install.getMessage());

		PluginRecord record = findInstalled(PLUGIN_ID);
		assertNotNull(record, "plugin is missing from /plugins after a successful install");
		assertEquals(PluginState.ACTIVE, record.getState());
		// name and version only come from the jar's manifest, so this proves it was really read
		assertNotNull(record.getName());
		assertNotNull(record.getVersion());

		Result uninstall = uninstall(PLUGIN_ID);
		assertTrue(uninstall.isSuccess(), "uninstall failed: " + uninstall.getMessage());
		assertNull(findInstalled(PLUGIN_ID), "plugin still listed as installed after uninstall");

		// The record survives as a tombstone so the operator knows a restart is still owed.
		PluginRecord tombstone = findRecord(PLUGIN_ID);
		assertNotNull(tombstone, "the uninstalled plugin should still be reported until restart");
		assertEquals(PluginState.UNINSTALLED_PENDING_RESTART, tombstone.getState());
	}

	/** Same plugin, but uploaded as a multipart ZIP the way the dashboard does it. */
	@Test
	public void testInstallFromUploadedZipThenUninstall() throws Exception {
		File zip = downloadCatalogZip();

		Result install = installFromZip(PLUGIN_ID, zip);
		assertTrue(install.isSuccess(), "install failed: " + install.getMessage());

		PluginRecord record = findInstalled(PLUGIN_ID);
		assertNotNull(record);
		assertEquals(PluginState.ACTIVE, record.getState());

		assertTrue(uninstall(PLUGIN_ID).isSuccess());
		assertNull(findInstalled(PLUGIN_ID));

		Files.deleteIfExists(zip.toPath());
	}

	/** The jar declares its own id, so installing it under another one must be refused. */
	@Test
	public void testInstallRejectsIdMismatch() throws Exception {
		File zip = downloadCatalogZip();

		Result result = installFromZip("not-clip-creator", zip);

		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().toLowerCase().contains("mismatch"),
				"expected an id mismatch message, got: " + result.getMessage());
		assertNull(findInstalled("not-clip-creator"));

		Files.deleteIfExists(zip.toPath());
	}

	@Test
	public void testInstallRejectsInvalidId() throws Exception {
		Result result = installFromUrl("Not A Valid Id", PLUGIN_ZIP_URL);

		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("not valid"), result.getMessage());
	}

	/** Same host as the registry, but nothing there — the download itself has to fail cleanly. */
	@Test
	public void testInstallRejectsUnreachableUrl() throws Exception {
		Result result = installFromUrl(PLUGIN_ID,
				"https://antmedia-plugins.s3.eu-west-2.amazonaws.com/no-such-plugin-here.zip");

		assertFalse(result.isSuccess());
		assertNull(findInstalled(PLUGIN_ID));
	}

	/**
	 * The server must refuse to fetch from anywhere but the configured registry. Otherwise the
	 * endpoint is an SSRF primitive — an arbitrary URL would have AMS issue a GET from inside
	 * the network and write the response to disk.
	 */
	@Test
	public void testInstallRejectsUrlOutsideRegistry() throws Exception {
		Result result = installFromUrl(PLUGIN_ID, "http://127.0.0.1:1/nope.zip");

		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("registry host"),
				"expected the registry host restriction, got: " + result.getMessage());
		assertNull(findInstalled(PLUGIN_ID));
	}

	@Test
	public void testDuplicateInstallRejected() throws Exception {
		assertTrue(installFromUrl(PLUGIN_ID, PLUGIN_ZIP_URL).isSuccess());

		Result second = installFromUrl(PLUGIN_ID, PLUGIN_ZIP_URL);

		assertFalse(second.isSuccess(), "the same plugin was installed twice");
		// the first install must survive the rejected second one
		assertNotNull(findInstalled(PLUGIN_ID));
		assertTrue(uninstall(PLUGIN_ID).isSuccess());
	}

	@Test
	public void testUninstallUnknownPluginFails() throws Exception {
		Result result = uninstall("no-such-plugin");

		assertFalse(result.isSuccess());
	}

	/** The cluster download endpoint must not serve plugin files without a valid JWT. */
	@Test
	public void testDownloadRequiresJwt() throws Exception {
		assertTrue(installFromUrl(PLUGIN_ID, PLUGIN_ZIP_URL).isSuccess());

		try (CloseableHttpClient client = client()) {
			HttpResponse noToken = client.execute(
					new HttpGet(ROOT_SERVICE_URL + "/plugins/" + PLUGIN_ID + "/download"));
			assertEquals(403, noToken.getStatusLine().getStatusCode());
			EntityUtils.consumeQuietly(noToken.getEntity());

			HttpGet badToken = new HttpGet(ROOT_SERVICE_URL + "/plugins/" + PLUGIN_ID + "/download");
			badToken.addHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION, "not-a-jwt");
			HttpResponse rejected = client.execute(badToken);
			assertEquals(403, rejected.getStatusLine().getStatusCode());
			EntityUtils.consumeQuietly(rejected.getEntity());
		}

		assertTrue(uninstall(PLUGIN_ID).isSuccess());
	}

	// --- REST helpers ---

	private List<PluginRecord> getPlugins() throws Exception {
		try (CloseableHttpClient client = client()) {
			HttpResponse response = client.execute(new HttpGet(ROOT_SERVICE_URL + "/plugins"));
			String body = EntityUtils.toString(response.getEntity());
			assertEquals(200, response.getStatusLine().getStatusCode(), body);
			Type listType = new TypeToken<List<PluginRecord>>() {}.getType();
			return gson.fromJson(body, listType);
		}
	}

	/**
	 * An uninstalled plugin keeps a record in state {@code UNINSTALLED_PENDING_RESTART} so the
	 * operator can see a restart is still owed — its files are gone but its classes are still
	 * resident. That tombstone does not count as installed, which is the same rule the
	 * management console applies when deciding whether to offer Install or Uninstall.
	 */
	private PluginRecord findInstalled(String pluginId) throws Exception {
		PluginRecord record = findRecord(pluginId);
		return (record != null && record.getState() != PluginState.UNINSTALLED_PENDING_RESTART)
				? record : null;
	}

	/** Any record for this id, tombstones included. */
	private PluginRecord findRecord(String pluginId) throws Exception {
		return getPlugins().stream()
				.filter(record -> pluginId.equals(record.getPluginId()))
				.findFirst()
				.orElse(null);
	}

	private Result installFromUrl(String pluginId, String downloadUrl) throws Exception {
		try (CloseableHttpClient client = client()) {
			HttpUriRequest post = RequestBuilder.post()
					.setUri(ROOT_SERVICE_URL + "/plugins/install-from-url")
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.setEntity(new StringEntity(gson.toJson(
							java.util.Map.of("id", pluginId, "downloadUrl", downloadUrl))))
					.build();
			return toResult(client.execute(post));
		}
	}

	private Result installFromZip(String pluginId, File zip) throws Exception {
		HttpEntity entity = MultipartEntityBuilder.create()
				.setMode(HttpMultipartMode.STRICT)
				.addBinaryBody("file", zip, ContentType.DEFAULT_BINARY, zip.getName())
				.build();

		HttpPut put = new HttpPut(ROOT_SERVICE_URL + "/plugins/" + pluginId);
		put.setEntity(entity);
		try (CloseableHttpClient client = client()) {
			return toResult(client.execute(put));
		}
	}

	private Result uninstall(String pluginId) throws Exception {
		try (CloseableHttpClient client = client()) {
			return toResult(client.execute(new HttpDelete(ROOT_SERVICE_URL + "/plugins/" + pluginId)));
		}
	}

	private void uninstallQuietly() {
		try {
			if (findInstalled(PLUGIN_ID) != null) {
				uninstall(PLUGIN_ID);
			}
		} catch (Exception e) {
			log.warn("Could not clean up {}: {}", PLUGIN_ID, e.getMessage());
		}
	}

	private Result toResult(HttpResponse response) throws Exception {
		String body = EntityUtils.toString(response.getEntity());
		assertEquals(200, response.getStatusLine().getStatusCode(), body);
		return gson.fromJson(body, Result.class);
	}

	/** Fetches the catalog ZIP once per test that needs a real upload body. */
	private File downloadCatalogZip() throws Exception {
		File zip = File.createTempFile(PLUGIN_ID, ".zip");
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpResponse response = client.execute(new HttpGet(PLUGIN_ZIP_URL));
			assertEquals(200, response.getStatusLine().getStatusCode(),
					"could not fetch the plugin ZIP from the catalog");
			try (InputStream in = response.getEntity().getContent();
				 OutputStream out = new FileOutputStream(zip)) {
				in.transferTo(out);
			}
		}
		assertTrue(zip.length() > 0, "downloaded plugin ZIP is empty");
		return zip;
	}

	private CloseableHttpClient client() {
		return HttpClients.custom()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(cookieStore)
				.build();
	}

	private void login() throws Exception {
		User user = new User();
		user.setEmail(TEST_USER_EMAIL);
		user.setPassword(TEST_USER_PASS);

		if (callIsFirstLogin()) {
			assertTrue(callCreateInitialUser(user), "could not create the initial user");
		}
		assertTrue(callAuthenticateUser(user), "could not authenticate the test user");
	}

	private boolean callIsFirstLogin() throws Exception {
		try (CloseableHttpClient client = client()) {
			HttpResponse response = client.execute(new HttpGet(ROOT_SERVICE_URL + "/first-login-status"));
			return gson.fromJson(EntityUtils.toString(response.getEntity()), Result.class).isSuccess();
		}
	}

	private boolean callCreateInitialUser(User user) throws Exception {
		return postJson("/users/initial", user);
	}

	private boolean callAuthenticateUser(User user) throws Exception {
		return postJson("/users/authenticate", user);
	}

	private boolean postJson(String path, Object body) throws Exception {
		try (CloseableHttpClient client = client()) {
			HttpUriRequest post = RequestBuilder.post()
					.setUri(ROOT_SERVICE_URL + path)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.setEntity(new StringEntity(gson.toJson(body)))
					.build();
			HttpResponse response = client.execute(post);
			String content = EntityUtils.toString(response.getEntity());
			if (response.getStatusLine().getStatusCode() != 200) {
				fail(path + " returned " + response.getStatusLine().getStatusCode() + ": " + content);
			}
			return gson.fromJson(content, Result.class).isSuccess();
		}
	}
}
