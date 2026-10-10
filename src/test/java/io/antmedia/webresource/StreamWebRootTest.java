package io.antmedia.webresource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.catalina.core.StandardContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class StreamWebRootTest extends UnitTestBase<StreamWebRoot> {

	@TempDir
	Path temporaryDirectory;

	@Test
	void testGetResourceType() {
		StreamWebRoot webroot = spy(new StreamWebRoot());

		webroot.getResource("test.mpd");
		assertThat(webroot.isStreamingResource()).isTrue();

		webroot.getResource("test.m3u8");
		assertThat(webroot.isStreamingResource()).isTrue();

		webroot.getResource("test.ts");
		assertThat(webroot.isStreamingResource()).isTrue();

		webroot.getResource("test.m4s");
		assertThat(webroot.isStreamingResource()).isTrue();

		webroot.getResource("/previews/test.png");
		assertThat(webroot.isStreamingResource()).isTrue();

		doReturn(null).when(webroot).getResourceDefault(anyString());

		webroot.getResource("/anydir/test.png");
		assertThat(webroot.isStreamingResource()).isFalse();

		webroot.getResource("/previews/test.html");
		assertThat(webroot.isStreamingResource()).isFalse();

		webroot.getResource("test.html");
		assertThat(webroot.isStreamingResource()).isFalse();
	}

	@Test
	void testExternalVodFolderHasPriorityAndApplicationFolderIsFallback() throws Exception {
		Path applicationDirectory = Files.createDirectory(temporaryDirectory.resolve("application"));
		Path applicationStreams = Files.createDirectory(applicationDirectory.resolve("streams"));
		Path externalVodFolder = Files.createDirectory(temporaryDirectory.resolve("external"));
		Path replacementVodFolder = Files.createDirectory(temporaryDirectory.resolve("replacement"));

		Files.writeString(applicationStreams.resolve("shared.mp4"), "application", StandardCharsets.UTF_8);
		Files.writeString(applicationStreams.resolve("fallback.mp4"), "fallback", StandardCharsets.UTF_8);
		Files.writeString(externalVodFolder.resolve("shared.mp4"), "external", StandardCharsets.UTF_8);
		Files.writeString(externalVodFolder.resolve("playlist.m3u8"), "external playlist", StandardCharsets.UTF_8);
		Files.writeString(replacementVodFolder.resolve("replacement.mp4"), "replacement", StandardCharsets.UTF_8);

		StreamWebRoot webRoot = startWebRoot(applicationDirectory);
		try {
			setVodFolder(webRoot, externalVodFolder.toString());

			assertThat(webRoot.getResource("/streams/shared.mp4").getContent())
					.isEqualTo("external".getBytes(StandardCharsets.UTF_8));
			assertThat(webRoot.getResource("/streams/fallback.mp4").getContent())
					.isEqualTo("fallback".getBytes(StandardCharsets.UTF_8));
			assertThat(webRoot.getResource("/streams/playlist.m3u8").getContent())
					.isEqualTo("external playlist".getBytes(StandardCharsets.UTF_8));

			setVodFolder(webRoot, replacementVodFolder.toString());
			assertThat(webRoot.getResource("/streams/replacement.mp4").getContent())
					.isEqualTo("replacement".getBytes(StandardCharsets.UTF_8));
		}
		finally {
			webRoot.stop();
			webRoot.destroy();
		}
	}

	@Test
	void testDefaultVodFolderPreservesApplicationResourceLookup() throws Exception {
		Path applicationDirectory = Files.createDirectory(temporaryDirectory.resolve("default-application"));
		Path applicationStreams = Files.createDirectory(applicationDirectory.resolve("streams"));
		Files.writeString(applicationStreams.resolve("asset.mp4"), "application", StandardCharsets.UTF_8);

		StreamWebRoot webRoot = startWebRoot(applicationDirectory);
		try {
			webRoot.configureVodFolder(null);
			webRoot.configureVodFolder(" ");
			setVodFolder(webRoot, "streams");
			assertThat(webRoot.getResource("/streams/asset.mp4").getContent())
					.isEqualTo("application".getBytes(StandardCharsets.UTF_8));

			setVodFolder(webRoot, "/streams");
			assertThat(webRoot.getResource("/streams/asset.mp4").getContent())
					.isEqualTo("application".getBytes(StandardCharsets.UTF_8));
		}
		finally {
			webRoot.stop();
			webRoot.destroy();
		}
	}

	@Test
	void testDiscoversSettingsFromApplicationContextAndRejectsInvalidFolder() {
		AppSettings settings = new AppSettings();
		settings.setVodFolder(temporaryDirectory.resolve("missing").toString());
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		when(applicationContext.containsBean(AppSettings.BEAN_NAME)).thenReturn(true);
		when(applicationContext.getBean(AppSettings.BEAN_NAME, AppSettings.class)).thenReturn(settings);

		org.apache.catalina.Context context = mock(org.apache.catalina.Context.class);
		jakarta.servlet.ServletContext servletContext = mock(jakarta.servlet.ServletContext.class);
		when(context.getServletContext()).thenReturn(servletContext);
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
				.thenReturn(applicationContext);

		StreamWebRoot webRoot = spy(new StreamWebRoot());
		webRoot.setContext(context);
		doReturn(mock(org.apache.catalina.WebResource.class))
				.when(webRoot).getResourceDefault(anyString());
		webRoot.getResource("/streams/missing.mp4");

		verify(applicationContext).getBean(AppSettings.BEAN_NAME, AppSettings.class);
	}

	private StreamWebRoot startWebRoot(Path applicationDirectory) throws Exception {
		StandardContext context = new StandardContext();
		context.setPath("/test");
		context.setName("/test");
		context.setDocBase(applicationDirectory.toString());

		StreamWebRoot webRoot = new StreamWebRoot();
		webRoot.setContext(context);
		context.setResources(webRoot);
		webRoot.start();
		return webRoot;
	}

	private void setVodFolder(StreamWebRoot webRoot, String vodFolder) {
		AppSettings settings = new AppSettings();
		settings.setVodFolder(vodFolder);
		webRoot.setAppSettings(settings);
		webRoot.configureVodFolder(vodFolder);
	}
}
