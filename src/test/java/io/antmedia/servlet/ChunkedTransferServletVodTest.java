package io.antmedia.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.test.UnitTestBase;
import jakarta.servlet.http.HttpServletRequest;

@Tag("fast")
class ChunkedTransferServletVodTest extends UnitTestBase<ChunkedTransferServlet> {

	@TempDir
	Path temporaryDirectory;

	@Test
	void testExternalFileHasPriorityForChunkedServletResources() throws Exception {
		Path externalFile = Files.writeString(temporaryDirectory.resolve("asset.mpd"), "external");
		AppSettings settings = new AppSettings();
		settings.setVodFolder(temporaryDirectory.toString());

		ApplicationContext applicationContext = mock(ApplicationContext.class);
		when(applicationContext.getBean(AppSettings.class)).thenReturn(settings);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getServletPath()).thenReturn("/streams/asset.mpd");
		when(request.getRequestURI()).thenReturn("/LiveApp/streams/asset.mpd");

		ChunkedTransferServlet servlet = new ChunkedTransferServlet();
		assertThat(servlet.resolveFileForRead(request, applicationContext)).isEqualTo(externalFile.toFile());
	}

	@Test
	void testDefaultAndMissingExternalFilesFallBackToApplicationStreams() {
		AppSettings settings = new AppSettings();
		settings.setVodFolder("streams");

		ApplicationContext applicationContext = mock(ApplicationContext.class);
		when(applicationContext.getBean(AppSettings.class)).thenReturn(settings);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getServletPath()).thenReturn("/streams/asset.m4s");
		when(request.getRequestURI()).thenReturn("/LiveApp/streams/asset.m4s");

		ChunkedTransferServlet servlet = new ChunkedTransferServlet();
		assertThat(servlet.resolveFileForRead(request, applicationContext))
				.isEqualTo(new File("webapps//LiveApp/streams/asset.m4s"));

		settings.setVodFolder(temporaryDirectory.toString());
		assertThat(servlet.resolveFileForRead(request, applicationContext))
				.isEqualTo(new File("webapps//LiveApp/streams/asset.m4s"));
		verify(applicationContext, times(2)).getBean(AppSettings.class);
	}

	@Test
	void testExternalResolutionRejectsTraversal() {
		ChunkedTransferServlet servlet = new ChunkedTransferServlet();
		assertThat(servlet.resolveExternalVodFile("/streams/../secret.mpd", temporaryDirectory.toString()))
				.isNull();
		assertThat(servlet.resolveExternalVodFile("/streams/asset.mpd", "relative/videos")).isNull();
		assertThat(servlet.resolveExternalVodFile("/preview/asset.mpd", temporaryDirectory.toString())).isNull();
	}
}
