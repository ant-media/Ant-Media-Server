package io.antmedia.webresource;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.catalina.core.StandardContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import io.antmedia.AppSettings;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class StreamWebRootTest extends UnitTestBase<StreamWebRoot> {

	@TempDir
	Path temporaryDirectory;

	@Test
	void testGetResourceType() {
		StreamWebRoot webroot = Mockito.spy(new StreamWebRoot());

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

		Mockito.doReturn(null).when(webroot).getResourceDefault(Mockito.anyString());

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

		Files.writeString(applicationStreams.resolve("shared.mp4"), "application", StandardCharsets.UTF_8);
		Files.writeString(applicationStreams.resolve("fallback.mp4"), "fallback", StandardCharsets.UTF_8);
		Files.writeString(externalVodFolder.resolve("shared.mp4"), "external", StandardCharsets.UTF_8);
		Files.writeString(externalVodFolder.resolve("playlist.m3u8"), "external playlist", StandardCharsets.UTF_8);

		StreamWebRoot webRoot = startWebRoot(applicationDirectory);
		try {
			setVodFolder(webRoot, externalVodFolder.toString());

			assertThat(webRoot.getResource("/streams/shared.mp4").getContent())
					.isEqualTo("external".getBytes(StandardCharsets.UTF_8));
			assertThat(webRoot.getResource("/streams/fallback.mp4").getContent())
					.isEqualTo("fallback".getBytes(StandardCharsets.UTF_8));
			assertThat(webRoot.getResource("/streams/playlist.m3u8").getContent())
					.isEqualTo("external playlist".getBytes(StandardCharsets.UTF_8));
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
